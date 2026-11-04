package dev.alacka.orbit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.alacka.orbit.accounts.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AccountsSelfTest {
    private static int checks;
    private static final String APP = "01234567-1234-4321-abcd-123456789012";
    private static final String ID = "00112233445566778899aabbccddeeff";
    private static final String SECRET = "dummy-password-not-a-real-credential";
    private static final String TOKEN = "dummy-minecraft-token";
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void failure(AuthFailure.Reason reason, Action action) throws Exception {
        try { action.run(); throw new AssertionError("Expected " + reason); }
        catch (AuthFailure error) {
            check(error.reason() == reason, "Safe failure reason " + reason);
            check(!error.getMessage().contains(SECRET) && !error.getMessage().contains(TOKEN), "No credentials in error message");
        }
    }
    private record Call(String method, URI uri, String body, Map<String, String> headers) {
        JsonObject json() { return JsonParser.parseString(body).getAsJsonObject(); }
        Map<String,String> form() {
            Map<String,String> fields = new HashMap<>();
            for (String pair : body.split("&")) {
                String[] p = pair.split("=", 2);
                fields.put(URLDecoder.decode(p[0], StandardCharsets.UTF_8), URLDecoder.decode(p[1], StandardCharsets.UTF_8));
            }
            return fields;
        }
    }
    @FunctionalInterface private interface Handler { AuthTransport.Reply reply(Call call) throws AuthFailure; }
    private static final class Fake implements AuthTransport {
        final List<Call> calls = new ArrayList<>();
        Handler handler;
        Fake(Handler handler) { this.handler = handler; }
        public Reply request(String method, URI uri, String body, Map<String,String> headers) throws AuthFailure {
            check(AuthHttp.allowed(uri), "All protocol requests stay on fixed HTTPS hosts");
            Call call = new Call(method, uri, body, headers); calls.add(call); return handler.reply(call);
        }
    }
    private static AuthTransport.Reply reply(int code, JsonObject body) { return new AuthTransport.Reply(code, body); }
    private static JsonObject json(Object... values) { return AuthProtocol.json(values); }
    private static JsonObject ely(String token, String client) {
        return json("accessToken", token, "clientToken", client, "selectedProfile", json("id", ID, "name", "OrbitTest"));
    }
    private static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-04T12:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
        void sleep(long millis) { now = now.plusMillis(millis); }
    }
    private static JsonObject device(int seconds) {
        return json("device_code", "dummy-device-token", "user_code", "ABCD-EFGH", "verification_uri", "https://www.microsoft.com/link", "expires_in", seconds, "interval", 5);
    }
    private static AuthTransport.Reply chain(Call call) {
        String path = call.uri.getPath(), host = call.uri.getHost();
        if (host.equals("user.auth.xboxlive.com")) {
            check(call.json().getAsJsonObject("Properties").get("RpsTicket").getAsString().equals("d=dummy-msa-token"), "MSA token goes only to XBL HTTPS");
            return reply(200, JsonParser.parseString("{\"Token\":\"dummy-xbl-token\",\"DisplayClaims\":{\"xui\":[{\"uhs\":\"12345\",\"xid\":\"67890\"}]}}").getAsJsonObject());
        }
        if (host.equals("xsts.auth.xboxlive.com")) {
            check(call.json().get("RelyingParty").getAsString().equals("rp://api.minecraftservices.com/"), "XSTS audience is Java Minecraft");
            return reply(200, JsonParser.parseString("{\"Token\":\"dummy-xsts-token\",\"DisplayClaims\":{\"xui\":[{\"uhs\":\"12345\"}]}}").getAsJsonObject());
        }
        if (path.equals("/authentication/login_with_xbox")) {
            check(call.json().get("identityToken").getAsString().equals("XBL3.0 x=12345;dummy-xsts-token"), "Correct XSTS identity token");
            return reply(200, json("access_token", TOKEN, "expires_in", 86400));
        }
        check(call.headers.get("Authorization").equals("Bearer " + TOKEN), "Only Minecraft token sent to Minecraft API");
        if (path.equals("/entitlements/mcstore")) return reply(200, JsonParser.parseString("{\"items\":[{\"name\":\"game_minecraft\"}]}").getAsJsonObject());
        if (path.equals("/minecraft/profile")) return reply(200, json("id", ID, "name", "OrbitTest"));
        throw new AssertionError("Unexpected fixed API route");
    }

    private static void elyTests() throws Exception {
        Fake fake = new Fake(call -> {
            check(call.uri.getHost().equals("authserver.ely.by"), "Ely credentials never leave Ely");
            check(call.uri.getPath().equals("/auth/authenticate"), "Ely direct sign in route");
            check(call.method.equals("POST") && call.headers.get("Content-Type").equals("application/json"), "Ely POST JSON");
            JsonObject body = call.json();
            check(body.get("username").getAsString().equals("test@example.invalid"), "Trim Ely login only");
            check(body.get("password").getAsString().equals(SECRET + ":123456"), "2FA combined only inside provider request");
            check(AccountPreferences.validClientId(body.get("clientToken").getAsString()), "Random per-session client token");
            return reply(200, ely(TOKEN, body.get("clientToken").getAsString()));
        });
        AuthSession session = new AuthProtocol(fake).elyLogin(" test@example.invalid ", SECRET, "123456");
        check(session.name().equals("OrbitTest") && session.id().toString().equals("00112233-4455-6677-8899-aabbccddeeff"), "Authoritative Ely name and UUID");
        check(session.provider() == AuthSession.Provider.ELY && session.key().startsWith("ely:"), "Provider-scoped identity");
        check(!session.toString().contains(TOKEN), "Session string redacts token");
        check(!reply(200, ely(TOKEN, "client")).toString().contains(TOKEN), "Reply string redacts body");
        fake.handler = call -> {
            check(call.uri.getPath().equals("/auth/refresh"), "Ely refresh route");
            check(call.json().get("clientToken").getAsString().equals(session.clientToken()), "Ely client token retained");
            check(!call.body.contains(SECRET), "No password during refresh");
            return reply(200, ely("new-dummy-token", session.clientToken()));
        };
        AuthSession refreshed = new AuthProtocol(fake).refresh(session, () -> false);
        check(refreshed.accessToken().equals("new-dummy-token"), "Refresh replaces access token");
        fake.handler = call -> {
            check(call.uri.getPath().equals("/auth/invalidate") && !call.uri.getPath().equals("/auth/signout"), "Remove only Orbit's session, never all user sessions");
            return reply(204, json());
        };
        new AuthProtocol(fake).invalidateEly(refreshed);
        for (int status : new int[]{400,401,403}) {
            Fake denied = new Fake(call -> reply(status, json("errorMessage", SECRET + TOKEN)));
            failure(AuthFailure.Reason.CREDENTIALS, () -> new AuthProtocol(denied).elyLogin("test", SECRET, ""));
        }
        failure(AuthFailure.Reason.TWO_FACTOR, () -> new AuthProtocol(new Fake(c -> reply(401, json("errorMessage", "Account protected with two factor auth.")))).elyLogin("test", SECRET, ""));
        failure(AuthFailure.Reason.RATE_LIMIT, () -> new AuthProtocol(new Fake(c -> reply(429, json()))).elyLogin("test", SECRET, ""));
        failure(AuthFailure.Reason.NETWORK, () -> new AuthProtocol(new Fake(c -> reply(503, json()))).elyLogin("test", SECRET, ""));
        Fake untouched = new Fake(c -> { throw new AssertionError("Invalid credentials must not be sent"); });
        failure(AuthFailure.Reason.CREDENTIALS, () -> new AuthProtocol(untouched).elyLogin("", SECRET, ""));
        failure(AuthFailure.Reason.CREDENTIALS, () -> new AuthProtocol(untouched).elyLogin("test", SECRET, "12345x"));
        for (String bad : new String[]{"zero", "00000000000000000000000000000000", "1-1-1-1-1", "00112233445566778899aabbccddeefg"}) {
            failure(AuthFailure.Reason.RESPONSE, () -> AuthProtocol.uuid(bad));
        }
        failure(AuthFailure.Reason.RESPONSE, () -> new AuthProtocol(new Fake(c -> reply(200, ely(TOKEN, "wrong-client")))).elyLogin("test", SECRET, ""));
    }

    private static void microsoftTests() throws Exception {
        TestClock clock = new TestClock();
        List<Long> waits = new ArrayList<>();
        int[] polls = {0};
        Fake fake = new Fake(call -> {
            if (call.uri.getPath().endsWith("/devicecode")) {
                check(call.form().get("client_id").equals(APP), "Own configured public client ID");
                check(call.form().get("scope").equals("XboxLive.signin offline_access"), "Consumer Xbox scopes");
                check(!call.body.contains("client_secret"), "No embedded OAuth secret");
                return reply(200, device(900));
            }
            if (call.uri.getPath().endsWith("/token")) {
                check(call.uri.getPath().contains("/consumers/"), "Personal Microsoft tenant");
                Map<String,String> fields = call.form();
                if (fields.get("grant_type").equals("refresh_token")) {
                    check(fields.get("refresh_token").equals("dummy-refresh-token"), "Memory-only refresh token");
                    return reply(200, json("access_token", "dummy-msa-token", "refresh_token", "rotated-refresh-token"));
                }
                check(fields.get("device_code").equals("dummy-device-token"), "Device code posted only to Microsoft");
                polls[0]++;
                if (polls[0] == 1) return reply(400, json("error", "authorization_pending"));
                if (polls[0] == 2) return reply(400, json("error", "slow_down"));
                return reply(200, json("access_token", "dummy-msa-token", "refresh_token", "dummy-refresh-token"));
            }
            return chain(call);
        });
        AuthProtocol protocol = new AuthProtocol(fake, clock, millis -> { waits.add(millis); clock.sleep(millis); });
        var code = protocol.microsoftCode(APP);
        check(code.userCode().equals("ABCD-EFGH") && !code.toString().contains("dummy-device-token"), "Only user code exposed for display");
        AuthSession session = protocol.microsoftPoll(APP, code, () -> false);
        check(waits.equals(List.of(5000L,5000L,10000L)), "Poll interval and slow_down respected");
        check(session.provider() == AuthSession.Provider.MICROSOFT && session.accessToken().equals(TOKEN), "Full Microsoft-Xbox-Minecraft flow");
        check(session.xuid().equals("67890") && session.clientId().equals(APP), "Own account metadata retained");
        check(session.expiresAt().equals(clock.instant().plusSeconds(86400)), "Minecraft expiry tracked");
        AuthSession refreshed = protocol.refresh(session, () -> false);
        check(refreshed.refreshToken().equals("rotated-refresh-token"), "Rotating Microsoft refresh tokens retained only in memory");
        check(fake.calls.stream().noneMatch(c -> c.uri.getHost().endsWith("ely.by")), "Microsoft never routed through Ely");
        Fake noId = new Fake(c -> { throw new AssertionError("No request without app ID"); });
        failure(AuthFailure.Reason.CLIENT_ID, () -> new AuthProtocol(noId).microsoftCode(""));
        failure(AuthFailure.Reason.CLIENT_ID, () -> new AuthProtocol(noId).microsoftCode("borrow-a-launcher-id"));
        for (String remote : List.of("https://attacker.invalid/login", "http://www.microsoft.com/link", "https://attacker@www.microsoft.com/link", "https://www.microsoft.com:444/link")) {
            Fake badUri = new Fake(c -> { JsonObject d = device(900); d.addProperty("verification_uri", remote); return reply(200, d); });
            failure(AuthFailure.Reason.RESPONSE, () -> new AuthProtocol(badUri).microsoftCode(APP));
        }
        for (String error : List.of("authorization_declined", "access_denied", "expired_token", "invalid_client")) {
            TestClock c = new TestClock();
            Fake denied = new Fake(call -> call.uri.getPath().endsWith("/devicecode") ? reply(200,device(900)) : reply(400,json("error",error,"error_description", SECRET)));
            AuthProtocol p = new AuthProtocol(denied,c,c::sleep);
            var d = p.microsoftCode(APP);
            AuthFailure.Reason reason = error.equals("expired_token") ? AuthFailure.Reason.EXPIRED
                    : error.equals("invalid_client") ? AuthFailure.Reason.CLIENT_ID : AuthFailure.Reason.CANCELLED;
            failure(reason, () -> p.microsoftPoll(APP,d,()->false));
        }
        TestClock expiry = new TestClock();
        Fake expiring = new Fake(call -> { check(call.uri.getPath().endsWith("/devicecode"), "Do not poll expired codes"); return reply(200,device(5)); });
        AuthProtocol p = new AuthProtocol(expiring,expiry,expiry::sleep);
        var d = p.microsoftCode(APP);
        failure(AuthFailure.Reason.EXPIRED, () -> p.microsoftPoll(APP,d,()->false));
        try { p.microsoftPoll(APP,d,()->true); throw new AssertionError("Cancellation expected"); }
        catch (CancellationException expected) { check(true,"Cancelled jobs do not sign in"); }
        for (String route : List.of("app", "licence", "profile", "family")) {
            TestClock c = new TestClock();
            Fake denied = new Fake(call -> {
                if (call.uri.getPath().endsWith("/devicecode")) return reply(200,device(900));
                if (call.uri.getPath().endsWith("/token")) return reply(200,json("access_token","dummy-msa-token"));
                if (route.equals("app") && call.uri.getPath().equals("/authentication/login_with_xbox")) return reply(403,json("errorMessage", SECRET));
                if (route.equals("licence") && call.uri.getPath().equals("/entitlements/mcstore")) return reply(200,JsonParser.parseString("{\"items\":[]}").getAsJsonObject());
                if (route.equals("profile") && call.uri.getPath().equals("/minecraft/profile")) return reply(404,json());
                if (route.equals("family") && call.uri.getHost().equals("xsts.auth.xboxlive.com")) return reply(401,json("XErr",2148916238L));
                return chain(call);
            });
            AuthProtocol q = new AuthProtocol(denied,c,c::sleep); var dc = q.microsoftCode(APP);
            AuthFailure.Reason reason = switch(route) { case "app" -> AuthFailure.Reason.APP_APPROVAL; case "licence" -> AuthFailure.Reason.NO_GAME; case "profile" -> AuthFailure.Reason.NO_PROFILE; default -> AuthFailure.Reason.FAMILY; };
            failure(reason, () -> q.microsoftPoll(APP,dc,()->false));
        }
    }

    private static void persistenceAndGuardTests() throws Exception {
        var root = Files.createTempDirectory("orbit-account-test-");
        var path = root.resolve("accounts.properties");
        check(AccountPreferences.read(path).isEmpty(), "Missing config does not invent an ID");
        AccountPreferences.write(path,APP);
        String content = Files.readString(path);
        check(AccountPreferences.read(path).equals(APP), "Public app ID round trip");
        check(!content.contains(TOKEN) && !content.contains(SECRET), "No credential persistence");
        Files.writeString(path,"microsoft.clientId=broken\nkeep=original\n");
        byte[] before = Files.readAllBytes(path);
        try { AccountPreferences.write(path,APP); throw new AssertionError("Preserve malformed preferences"); }
        catch (java.io.IOException expected) { check(Arrays.equals(before,Files.readAllBytes(path)), "Malformed file preserved byte-for-byte"); }
        Files.delete(path);
        try { AccountPreferences.write(path,APP + "\naccessToken=" + TOKEN); throw new AssertionError("Reject injection"); }
        catch (java.io.IOException expected) { check(!Files.exists(path), "Invalid ID never creates a config"); }
        Files.delete(root);
        for (int flags=0;flags<128;flags++) {
            boolean allowed = SessionGuard.canSwitch((flags&1)!=0,(flags&2)!=0,(flags&4)!=0,(flags&8)!=0,(flags&16)!=0,(flags&32)!=0,(flags&64)!=0);
            check(allowed == (flags==1), "Only the disconnected main client thread may switch");
        }
        for (String host : List.of("https://authserver.ely.by/auth/authenticate", "https://login.microsoftonline.com/consumers/oauth2/v2.0/token", "https://api.minecraftservices.com/minecraft/profile"))
            check(AuthHttp.allowed(URI.create(host)), "Documented HTTPS API allowed");
        for (String bad : List.of("http://authserver.ely.by/auth/authenticate", "https://authserver.ely.by.attacker.invalid/", "https://attacker@authserver.ely.by/", "https://authserver.ely.by:444/", "https://127.0.0.1/", "https://authserver.ely.by/#fragment"))
            check(!AuthHttp.allowed(URI.create(bad)), "Untrusted transport destination rejected");
        for (int w=320;w<=2048;w+=37) for (int h=200;h<=1200;h+=29) for (int preferred : new int[]{230,270}) for (int shift : new int[]{-10000,0,10000}) {
            var form=AccountFormLayout.of(w,h,preferred,shift,-shift);
            check(form.x()>=0 && form.y()>=0 && form.x()+form.width()<=w && form.y()+form.height()<=h, "Account window clamped on screen");
            check(form.fieldY(2)+22<=form.statusY(), "Three fields never overlap status");
            check(form.statusY()+9<=form.footerY()-4 && form.footerY()+22<=form.y()+form.height()-6, "Status and action buttons fit");
        }
    }

    public static void main(String[] args) throws Exception {
        elyTests(); microsoftTests(); persistenceAndGuardTests();
        System.out.println("PASS: accounts " + checks + " assertions; fake transports only, no real credentials or API sign-ins.");
    }
}
