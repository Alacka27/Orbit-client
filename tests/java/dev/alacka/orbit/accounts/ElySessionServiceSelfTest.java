package dev.alacka.orbit.accounts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.properties.Property;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/** Synthetic keys and dummy credentials only; no request leaves this process. */
public final class ElySessionServiceSelfTest {
    private static final UUID ID = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
    private static int checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void responseFailure(Action action) throws Exception {
        try { action.run(); throw new AssertionError("Expected malformed-reply failure"); }
        catch (AuthFailure error) { check(error.reason() == AuthFailure.Reason.RESPONSE, "Safe malformed-reply reason"); }
    }
    private static final class Fake implements AuthTransport {
        int status = 200, calls;
        JsonObject reply = new JsonObject();
        String method, body;
        URI uri;
        Map<String, String> headers;
        boolean networkFailure;
        public Reply request(String method, URI uri, String body, Map<String, String> headers) throws AuthFailure {
            check(AuthHttp.allowed(uri), "Provider request uses a fixed HTTPS host");
            check(uri.getHost().equals("authserver.ely.by") || uri.getHost().equals("skinsystem.ely.by"), "No Ely token sent to Microsoft");
            calls++;
            this.method = method; this.uri = uri; this.body = body; this.headers = headers;
            if (networkFailure) throw new AuthFailure(AuthFailure.Reason.NETWORK);
            return new Reply(status, reply);
        }
    }
    private static Property signed(KeyPair keys, String json) throws Exception {
        String value = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initSign(keys.getPrivate());
        signer.update(value.getBytes(StandardCharsets.UTF_8));
        return new Property("textures", value, Base64.getEncoder().encodeToString(signer.sign()));
    }
    private static String skin(String url) {
        return AuthProtocol.json("textures", AuthProtocol.json("SKIN", AuthProtocol.json(
                "url", url, "metadata", AuthProtocol.json("model", "slim")))).toString();
    }
    private static JsonObject profile() {
        return AuthProtocol.json("id", ID.toString().replace("-", ""), "name", "OrbitTest");
    }
    private static void textures() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair fixture = generator.generateKeyPair();
        ElySessionService service = new ElySessionService(new Fake(), fixture.getPublic());
        Property valid = signed(fixture, skin("https://textures.ely.by/skins/test.png"));
        MinecraftProfileTextures decoded = service.unpackTextures(valid);
        check(decoded.signatureState() == SignatureState.SIGNED && decoded.skin() != null, "Real synthetic RSA signature verified");
        check(decoded.skin().getUrl().equals("https://textures.ely.by/skins/test.png"), "Texture URL preserved");
        check(decoded.skin().getMetadata("model").equals("slim"), "Slim metadata preserved");
        check(service.getSecurePropertyValue(valid).equals(valid.value()), "Secure property requires the correct signature");
        check(new ElySessionService(new Fake()).unpackTextures(valid) == MinecraftProfileTextures.EMPTY, "Production pinned Ely key rejects foreign fixture");
        check(new ElySessionService(new Fake(), generator.generateKeyPair().getPublic()).unpackTextures(valid)
                == MinecraftProfileTextures.EMPTY, "Wrong RSA key fails closed");
        for (Property invalid : new Property[]{
                new Property("textures", valid.value()),
                new Property("textures", valid.value(), "Cg=="),
                new Property("textures", valid.value() + "x", valid.signature()),
                new Property("other", valid.value(), valid.signature()),
                new Property("textures", "x".repeat(32769), valid.signature()),
                new Property("textures", valid.value(), "x".repeat(8193))}) {
            check(service.unpackTextures(invalid) == MinecraftProfileTextures.EMPTY, "Unsigned/placeholder/tampered/oversized texture rejected");
            try { service.getSecurePropertyValue(invalid); throw new AssertionError("Invalid signature accepted"); }
            catch (InsecurePublicKeyException expected) { checks++; }
        }
        check(service.unpackTextures(null) == MinecraftProfileTextures.EMPTY, "Missing texture uses default skin");
        for (String host : new String[]{"skinsystem.ely.by", "textures.ely.by", "ely.by", "textures.minecraft.net"}) {
            var texture = service.unpackTextures(signed(fixture, skin("http://" + host + "/texture/test")));
            check(texture.skin() != null && texture.skin().getUrl().startsWith("https://"), "Known cleartext texture is upgraded to HTTPS");
        }
        for (String url : new String[]{
                "https://textures.ely.by.evil.invalid/test", "https://evil.invalid/test",
                "https://textures.ely.by@evil.invalid/test", "https://user@textures.ely.by/test",
                "https://textures.ely.by:444/test", "https://textures.ely.by/test#fragment",
                "file:///tmp/test.png", "data:image/png;base64,AAAA", "ftp://textures.ely.by/test",
                "//textures.ely.by/test"}) {
            check(service.unpackTextures(signed(fixture, skin(url))) == MinecraftProfileTextures.EMPTY, "Unsafe texture URL rejected");
        }
        for (String payload : new String[]{"not-json", "[]", "{}", "{\"textures\":[]}",
                "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.ely.by/test\",\"metadata\":{\"model\":\"unknown\"}}}}"}) {
            check(service.unpackTextures(signed(fixture, payload)) == MinecraftProfileTextures.EMPTY, "Malformed signed payload fails closed");
        }
        String all = AuthProtocol.json("textures", AuthProtocol.json(
                "SKIN", AuthProtocol.json("url", "https://textures.ely.by/skin"),
                "CAPE", AuthProtocol.json("url", "https://textures.ely.by/cape"),
                "ELYTRA", AuthProtocol.json("url", "https://textures.ely.by/elytra"))).toString();
        var textures = service.unpackTextures(signed(fixture, all));
        check(textures.skin() != null && textures.cape() != null && textures.elytra() != null, "All three signed texture types");
    }
    private static void profilesAndSessions() throws Exception {
        Fake fake = new Fake();
        ElySessionService service = new ElySessionService(fake);
        fake.reply = profile();
        var result = service.fetchProfile(ID, true);
        check(result != null && result.profile().id().equals(ID), "Fetch binds profile to the requested UUID");
        check(fake.method.equals("GET") && fake.uri.getPath().equals("/session/profile/" + ID.toString().replace("-", ""))
                && fake.uri.getQuery().equals("unsigned=false"), "Signed Ely profile route");
        check(fake.body.isEmpty() && fake.headers.isEmpty(), "Public profile request has no access token");
        fake.reply.addProperty("id", UUID.randomUUID().toString());
        check(service.fetchProfile(ID, false) == null, "Mismatched UUID rejected even if secure flag is false");
        fake.reply = profile(); fake.reply.addProperty("name", "invalid name");
        check(service.fetchProfile(ID, true) == null, "Invalid provider name rejected");
        fake.reply = profile();
        check(service.findProfileByName("OrbitTest").orElseThrow().id().equals(ID), "Name lookup uses Ely repository");
        check(fake.uri.toString().equals("https://skinsystem.ely.by/profile/OrbitTest?unsigned=false"), "Name lookup fixed public route");
        int previous = fake.calls;
        for (String bad : new String[]{"../admin", "ab", "a".repeat(17), "with space", "name?x=y"})
            check(service.findProfileByName(bad).isEmpty(), "Malformed profile name rejected before networking");
        check(previous == fake.calls, "Invalid names never make network requests");
        int[] callbacks = {0, 0};
        service.findProfilesByNames(new String[]{"OrbitTest", "../bad"}, new ProfileLookupCallback() {
            public void onProfileLookupSucceeded(String name, UUID id) { callbacks[0]++; check(id.equals(ID), "Callback UUID"); }
            public void onProfileLookupFailed(String name, Exception error) { callbacks[1]++; }
        });
        check(callbacks[0] == 1 && callbacks[1] == 1, "Repository callback handles each result independently");
        JsonArray properties = new JsonArray();
        properties.add(AuthProtocol.json("name", "textures", "value", "dummy-packed-texture", "signature", "Cg=="));
        fake.reply = profile(); fake.reply.add("properties", properties);
        var packed = service.getPackedTextures(service.fetchProfile(ID, true).profile());
        check(packed != null && packed.value().equals("dummy-packed-texture"), "Texture property extracted without trusting its signature");
        check(service.unpackTextures(packed) == MinecraftProfileTextures.EMPTY, "Extraction does not bypass signature validation");
        JsonArray oversized = new JsonArray();
        for (int i = 0; i < 17; i++) oversized.add(properties.get(0));
        fake.reply.add("properties", oversized);
        check(service.fetchProfile(ID, true) == null, "Property count is bounded");
        fake.reply = profile(); fake.status = 204;
        service.joinServer(ID, "dummy-ely-session-token", "dummy-server-hash");
        JsonObject sent = com.google.gson.JsonParser.parseString(fake.body).getAsJsonObject();
        check(fake.method.equals("POST") && fake.uri.toString().equals("https://authserver.ely.by/session/join"), "Join is provider-isolated");
        check(sent.get("accessToken").getAsString().equals("dummy-ely-session-token")
                && sent.get("selectedProfile").getAsString().equals(ID.toString().replace("-", ""))
                && sent.get("serverId").getAsString().equals("dummy-server-hash"), "Join uses this session identity only");
        check(!fake.uri.toString().contains("token"), "No credentials in URLs");
        fake.status = 401;
        try { service.joinServer(ID, "dummy-ely-session-token", "hash"); throw new AssertionError("Expired session accepted"); }
        catch (AuthenticationUnavailableException wrong) { throw new AssertionError("Wrong expired-session classification"); }
        catch (AuthenticationException expected) { check(!expected.getMessage().contains("dummy-ely-session-token"), "Safe expired-session error"); }
        for (int status : new int[]{429, 500, 503}) {
            fake.status = status;
            try { service.joinServer(ID, "dummy-ely-session-token", "hash"); throw new AssertionError("Unavailable provider accepted"); }
            catch (AuthenticationUnavailableException expected) { checks++; }
        }
        fake.status = 200; fake.reply = profile();
        check(service.hasJoinedServer("OrbitTest", "hash +", java.net.InetAddress.getByName("127.0.0.1")) != null, "Ely server validation parses profile");
        check(fake.uri.getRawQuery().contains("serverId=hash+%2B") && fake.uri.getRawQuery().contains("ip=127.0.0.1"), "Server validation query encoded");
        fake.status = 204; fake.reply = new JsonObject();
        check(service.hasJoinedServer("OrbitTest", "hash", null) == null, "Missing joined profile is not invented");
        fake.networkFailure = true;
        check(service.fetchProfile(ID, true) == null, "Profile outage safely falls back");
        try { service.joinServer(ID, "dummy-ely-session-token", "hash"); throw new AssertionError("Network outage accepted"); }
        catch (AuthenticationUnavailableException expected) { checks++; }
    }
    private static void httpReplies() throws Exception {
        for (int status : new int[]{400, 401, 403, 404, 429, 500, 502, 503}) {
            var reply = AuthHttp.decodeReply(status, "<html>upstream unavailable</html>".getBytes(StandardCharsets.UTF_8));
            check(reply.status() == status && reply.body().size() == 0, "Non-JSON error preserves HTTP status only");
        }
        check(AuthHttp.decodeReply(204, new byte[0]).body().size() == 0, "Empty successful reply allowed");
        check(AuthHttp.decodeReply(200, "{\"ok\":true}".getBytes(StandardCharsets.UTF_8)).body().get("ok").getAsBoolean(), "Successful object decoded");
        check(AuthHttp.decodeReply(400, "{\"error\":\"invalid_grant\"}".getBytes(StandardCharsets.UTF_8)).body().has("error"), "Structured provider error retained");
        for (String bad : new String[]{"not-json", "[]", "null", "<html>dummy-secret</html>"})
            responseFailure(() -> AuthHttp.decodeReply(200, bad.getBytes(StandardCharsets.UTF_8)));
        responseFailure(() -> AuthHttp.decodeReply(503, new byte[131073]));
        responseFailure(() -> new AuthHttp(null).request("DELETE", URI.create("https://authserver.ely.by/auth/invalidate"), "", Map.of()));
        responseFailure(() -> new AuthHttp(null).request("POST", URI.create("http://authserver.ely.by/auth/authenticate"), "", Map.of()));
    }
    public static void main(String[] args) throws Exception {
        textures(); profilesAndSessions(); httpReplies();
        System.out.println("PASS account skins, provider isolation and HTTP replies: " + checks + " assertions; synthetic data only");
    }
}