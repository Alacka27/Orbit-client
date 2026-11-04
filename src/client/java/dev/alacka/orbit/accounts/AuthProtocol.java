package dev.alacka.orbit.accounts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

import static dev.alacka.orbit.accounts.AuthFailure.Reason.*;

/** Provider-specific protocol. No Minecraft dependency and no authentication logs. */
public final class AuthProtocol {
    private static final String ELY = "https://authserver.ely.by";
    private static final String MSA = "https://login.microsoftonline.com/consumers/oauth2/v2.0/";
    private static final String MC = "https://api.minecraftservices.com";
    private static final String SCOPE = "XboxLive.signin offline_access";
    private final AuthTransport transport;
    private final Clock clock;
    private final Sleeper sleeper;
    @FunctionalInterface public interface Sleeper { void sleep(long millis) throws InterruptedException; }

    public AuthProtocol(AuthTransport transport) { this(transport, Clock.systemUTC(), Thread::sleep); }
    public AuthProtocol(AuthTransport transport, Clock clock, Sleeper sleeper) {
        this.transport = transport; this.clock = clock; this.sleeper = sleeper;
    }

    public static final class DeviceCode {
        private final String deviceCode, userCode;
        private final URI verificationUri;
        private final Instant expiresAt;
        private final int interval;
        private DeviceCode(String deviceCode, String userCode, URI verificationUri, Instant expiresAt, int interval) {
            this.deviceCode = deviceCode; this.userCode = userCode; this.verificationUri = verificationUri;
            this.expiresAt = expiresAt; this.interval = interval;
        }
        public String userCode() { return userCode; }
        public URI verificationUri() { return verificationUri; }
        public Instant expiresAt() { return expiresAt; }
        @Override public String toString() { return "DeviceCode[REDACTED]"; }
    }

    public AuthSession elyLogin(String username, String password, String totp) throws AuthFailure {
        if (username == null || username.isBlank() || username.length() > 254 || password == null
                || password.isEmpty() || password.length() > 1024 || (totp != null && !totp.isEmpty() && !totp.matches("\\d{6}"))) {
            throw new AuthFailure(CREDENTIALS);
        }
        String clientToken = UUID.randomUUID().toString();
        String secret = totp == null || totp.isEmpty() ? password : password + ":" + totp;
        var reply = post(ELY + "/auth/authenticate",
                json("username", username.trim(), "password", secret, "clientToken", clientToken, "requestUser", false));
        requireEly(reply);
        return elySession(reply.body(), clientToken);
    }

    public AuthSession refresh(AuthSession session, BooleanSupplier cancelled) throws AuthFailure {
        check(cancelled);
        if (session.provider() == AuthSession.Provider.ELY) {
            var reply = post(ELY + "/auth/refresh",
                    json("accessToken", session.accessToken(), "clientToken", session.clientToken(), "requestUser", false));
            requireEly(reply);
            check(cancelled);
            return elySession(reply.body(), session.clientToken());
        }
        if (!AccountPreferences.validClientId(session.clientId()) || session.refreshToken().isBlank()) throw new AuthFailure(EXPIRED);
        var reply = form(MSA + "token", Map.of("client_id", session.clientId(), "grant_type", "refresh_token",
                "refresh_token", session.refreshToken(), "scope", SCOPE));
        requireMicrosoft(reply);
        check(cancelled);
        return minecraftSession(session.clientId(), reply.body(), cancelled, session.refreshToken());
    }

    /** Invalidates only Orbit's Ely session, NOT every session belonging to the account. */
    public void invalidateEly(AuthSession session) throws AuthFailure {
        if (session.provider() != AuthSession.Provider.ELY) return;
        var reply = post(ELY + "/auth/invalidate",
                json("accessToken", session.accessToken(), "clientToken", session.clientToken()));
        if (!reply.ok()) throw new AuthFailure(reply.status() == 429 ? RATE_LIMIT : NETWORK);
    }

    private AuthSession elySession(JsonObject body, String expectedClientToken) throws AuthFailure {
        JsonObject profile = object(body, "selectedProfile");
        String name = profileName(profile);
        String echoed = optional(body, "clientToken");
        if (!echoed.isEmpty() && !echoed.equals(expectedClientToken)) throw new AuthFailure(RESPONSE);
        return new AuthSession(AuthSession.Provider.ELY, uuid(required(profile, "id")), name,
                required(body, "accessToken"), "", expectedClientToken, "", "", Instant.MAX);
    }

    public DeviceCode microsoftCode(String clientId) throws AuthFailure {
        if (!AccountPreferences.validClientId(clientId)) throw new AuthFailure(CLIENT_ID);
        var reply = form(MSA + "devicecode", Map.of("client_id", clientId, "scope", SCOPE));
        requireMicrosoft(reply);
        JsonObject body = reply.body();
        URI verification;
        try { verification = URI.create(required(body, "verification_uri")); }
        catch (IllegalArgumentException error) { throw new AuthFailure(RESPONSE); }
        if (!safeVerificationUri(verification)) throw new AuthFailure(RESPONSE);
        String code = required(body, "user_code");
        if (!code.matches("[A-Za-z0-9-]{4,32}")) throw new AuthFailure(RESPONSE);
        int seconds = integer(body, "expires_in", 5, 1800);
        int interval = body.has("interval") ? integer(body, "interval", 1, 60) : 5;
        return new DeviceCode(required(body, "device_code"), code, verification, clock.instant().plusSeconds(seconds), interval);
    }

    public static boolean safeVerificationUri(URI uri) {
        return uri != null && "https".equals(uri.getScheme()) && uri.getUserInfo() == null
                && uri.getFragment() == null && (uri.getPort() == -1 || uri.getPort() == 443)
                && Set.of("microsoft.com", "www.microsoft.com", "login.microsoftonline.com", "login.live.com").contains(uri.getHost());
    }

    public AuthSession microsoftPoll(String clientId, DeviceCode code, BooleanSupplier cancelled) throws AuthFailure {
        if (!AccountPreferences.validClientId(clientId)) throw new AuthFailure(CLIENT_ID);
        check(cancelled);
        int interval = code.interval;
        while (clock.instant().isBefore(code.expiresAt)) {
            check(cancelled);
            try { sleeper.sleep(interval * 1000L); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new CancellationException(); }
            check(cancelled);
            if (!clock.instant().isBefore(code.expiresAt)) break;
            var reply = form(MSA + "token", Map.of("client_id", clientId,
                    "grant_type", "urn:ietf:params:oauth:grant-type:device_code", "device_code", code.deviceCode));
            check(cancelled);
            if (reply.ok()) return minecraftSession(clientId, reply.body(), cancelled, "");
            String error = optional(reply.body(), "error");
            if (error.equals("authorization_pending")) continue;
            if (error.equals("slow_down")) { interval = Math.min(60, interval + 5); continue; }
            requireMicrosoft(reply);
        }
        throw new AuthFailure(EXPIRED);
    }

    private AuthSession minecraftSession(String clientId, JsonObject oauth, BooleanSupplier cancelled,
                                         String oldRefreshToken) throws AuthFailure {
        String msaToken = required(oauth, "access_token");
        check(cancelled);
        var xbl = post("https://user.auth.xboxlive.com/user/authenticate",
                json("Properties", json("AuthMethod", "RPS", "SiteName", "user.auth.xboxlive.com", "RpsTicket", "d=" + msaToken),
                        "RelyingParty", "http://auth.xboxlive.com", "TokenType", "JWT"));
        requireXbox(xbl);
        String xblToken = required(xbl.body(), "Token");
        String xuid = xboxClaim(xbl.body(), "xid", false);
        check(cancelled);
        JsonArray userTokens = new JsonArray(); userTokens.add(xblToken);
        var xsts = post("https://xsts.auth.xboxlive.com/xsts/authorize",
                json("Properties", json("SandboxId", "RETAIL", "UserTokens", userTokens),
                        "RelyingParty", "rp://api.minecraftservices.com/", "TokenType", "JWT"));
        requireXbox(xsts);
        String hash = xboxClaim(xsts.body(), "uhs", true);
        String xstsToken = required(xsts.body(), "Token");
        check(cancelled);
        var login = post(MC + "/authentication/login_with_xbox", json("identityToken", "XBL3.0 x=" + hash + ";" + xstsToken));
        if (login.status() == 403) throw new AuthFailure(APP_APPROVAL);
        requireMicrosoft(login);
        String token = required(login.body(), "access_token");
        int seconds = integer(login.body(), "expires_in", 1, 604800);
        check(cancelled);
        var entitlement = get(MC + "/entitlements/mcstore", token);
        requireMicrosoft(entitlement);
        boolean ownsJava = false;
        JsonElement items = entitlement.body().get("items");
        if (items == null || !items.isJsonArray()) throw new AuthFailure(RESPONSE);
        for (JsonElement item : items.getAsJsonArray()) {
            if (!item.isJsonObject()) throw new AuthFailure(RESPONSE);
            String name = optional(item.getAsJsonObject(), "name");
            if (Set.of("product_minecraft", "game_minecraft", "product_minecraft_java", "game_minecraft_java").contains(name)) ownsJava = true;
        }
        if (!ownsJava) throw new AuthFailure(NO_GAME);
        check(cancelled);
        var profile = get(MC + "/minecraft/profile", token);
        if (profile.status() == 404) throw new AuthFailure(NO_PROFILE);
        requireMicrosoft(profile);
        check(cancelled);
        String refresh = optional(oauth, "refresh_token");
        if (refresh.isEmpty()) refresh = oldRefreshToken;
        return new AuthSession(AuthSession.Provider.MICROSOFT, uuid(required(profile.body(), "id")),
                profileName(profile.body()), token, refresh, "", clientId, xuid, clock.instant().plusSeconds(seconds));
    }

    private static String xboxClaim(JsonObject body, String key, boolean required) throws AuthFailure {
        JsonObject claims = object(body, "DisplayClaims");
        JsonElement array = claims.get("xui");
        if (array == null || !array.isJsonArray() || array.getAsJsonArray().isEmpty()
                || !array.getAsJsonArray().get(0).isJsonObject()) throw new AuthFailure(RESPONSE);
        JsonObject first = array.getAsJsonArray().get(0).getAsJsonObject();
        return required ? required(first, key) : optional(first, key);
    }

    private static void requireEly(AuthTransport.Reply reply) throws AuthFailure {
        if (reply.ok()) return;
        if (reply.status() == 429) throw new AuthFailure(RATE_LIMIT);
        if (reply.status() >= 500) throw new AuthFailure(NETWORK);
        if (optional(reply.body(), "errorMessage").equals("Account protected with two factor auth.")) throw new AuthFailure(TWO_FACTOR);
        throw new AuthFailure(CREDENTIALS);
    }

    private static void requireMicrosoft(AuthTransport.Reply reply) throws AuthFailure {
        if (reply.ok()) return;
        if (reply.status() == 429) throw new AuthFailure(RATE_LIMIT);
        if (reply.status() >= 500) throw new AuthFailure(NETWORK);
        String error = optional(reply.body(), "error");
        throw new AuthFailure(switch (error) {
            case "authorization_declined", "access_denied" -> CANCELLED;
            case "expired_token", "bad_verification_code", "invalid_grant" -> EXPIRED;
            case "invalid_client", "unauthorized_client", "invalid_scope" -> CLIENT_ID;
            default -> reply.status() == 403 ? APP_APPROVAL : CREDENTIALS;
        });
    }

    private static void requireXbox(AuthTransport.Reply reply) throws AuthFailure {
        if (reply.ok()) return;
        if (reply.status() == 429) throw new AuthFailure(RATE_LIMIT);
        if (reply.status() >= 500) throw new AuthFailure(NETWORK);
        String code = optional(reply.body(), "XErr");
        throw new AuthFailure(switch (code) {
            case "2148916233" -> XBOX_PROFILE;
            case "2148916236", "2148916237", "2148916238" -> FAMILY;
            default -> XBOX_DENIED;
        });
    }

    private AuthTransport.Reply post(String url, JsonObject body) throws AuthFailure {
        return transport.request("POST", URI.create(url), body.toString(), Map.of("Content-Type", "application/json"));
    }
    private AuthTransport.Reply form(String url, Map<String, String> fields) throws AuthFailure {
        String body = fields.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
        return transport.request("POST", URI.create(url), body, Map.of("Content-Type", "application/x-www-form-urlencoded"));
    }
    private AuthTransport.Reply get(String url, String token) throws AuthFailure {
        return transport.request("GET", URI.create(url), "", Map.of("Authorization", "Bearer " + token));
    }
    public static String encode(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8); }
    public static JsonObject json(Object... pairs) {
        JsonObject object = new JsonObject();
        for (int i = 0; i < pairs.length; i += 2) {
            String key = (String) pairs[i]; Object value = pairs[i + 1];
            if (value instanceof JsonElement element) object.add(key, element);
            else if (value instanceof Boolean bool) object.addProperty(key, bool);
            else if (value instanceof Number number) object.addProperty(key, number);
            else object.addProperty(key, (String) value);
        }
        return object;
    }
    public static String required(JsonObject body, String key) throws AuthFailure {
        String value = optional(body, key);
        if (value.isBlank()) throw new AuthFailure(RESPONSE);
        return value;
    }
    public static String optional(JsonObject body, String key) throws AuthFailure {
        var value = body.get(key);
        if (value == null || value.isJsonNull()) return "";
        if (!value.isJsonPrimitive()) throw new AuthFailure(RESPONSE);
        String text = value.getAsString();
        if (text.length() > 32768) throw new AuthFailure(RESPONSE);
        return text;
    }
    public static JsonObject object(JsonObject body, String key) throws AuthFailure {
        var value = body.get(key);
        if (value == null || !value.isJsonObject()) throw new AuthFailure(RESPONSE);
        return value.getAsJsonObject();
    }
    public static int integer(JsonObject body, String key, int min, int max) throws AuthFailure {
        try {
            String text = required(body, key);
            if (!text.matches("\\d{1,10}")) throw new NumberFormatException();
            int n = Integer.parseInt(text);
            if (n < min || n > max) throw new NumberFormatException();
            return n;
        } catch (RuntimeException error) { throw new AuthFailure(RESPONSE); }
    }
    public static UUID uuid(String text) throws AuthFailure {
        if (text.matches("[0-9a-fA-F]{32}")) text = text.substring(0, 8) + "-" + text.substring(8, 12) + "-"
                + text.substring(12, 16) + "-" + text.substring(16, 20) + "-" + text.substring(20);
        if (!AccountPreferences.validClientId(text)) throw new AuthFailure(RESPONSE);
        UUID id = UUID.fromString(text);
        if (id.equals(new UUID(0, 0))) throw new AuthFailure(RESPONSE);
        return id;
    }
    private static String profileName(JsonObject profile) throws AuthFailure {
        String name = required(profile, "name");
        if (!name.matches("[A-Za-z0-9_]{3,16}")) throw new AuthFailure(RESPONSE);
        return name;
    }
    public static void check(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) throw new CancellationException();
    }
}