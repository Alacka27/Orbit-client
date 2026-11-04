package dev.alacka.orbit.accounts;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** An isolated provider, not a global replacement of Mojang authlib URLs/keys. */
public final class ElySessionService implements MinecraftSessionService, GameProfileRepository {
    private static final String AUTH = "https://authserver.ely.by/session/";
    private static final String SKINS = "https://skinsystem.ely.by/profile/";
    private static final PublicKey ELY_TEXTURES_KEY = loadKey();
    private final AuthTransport transport;
    private final PublicKey texturesKey;

    public ElySessionService(AuthTransport transport) { this(transport, ELY_TEXTURES_KEY); }

    /** Package-private key injection for signed-fixture tests; signature checks are never disabled. */
    ElySessionService(AuthTransport transport, PublicKey texturesKey) {
        this.transport = Objects.requireNonNull(transport);
        this.texturesKey = Objects.requireNonNull(texturesKey);
    }

    private static PublicKey loadKey() {
        try (var in = ElySessionService.class.getResourceAsStream("/assets/orbit/auth/ely-textures-public-key.der")) {
            if (in == null) throw new IllegalStateException("Missing Ely public key");
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(in.readAllBytes()));
        } catch (Exception error) { throw new IllegalStateException("Invalid Ely public key"); }
    }

    @Override public void joinServer(UUID profile, String accessToken, String serverId) throws AuthenticationException {
        try {
            var reply = transport.request("POST", URI.create(AUTH + "join"),
                    AuthProtocol.json("accessToken", accessToken, "selectedProfile", profile.toString().replace("-", ""),
                            "serverId", serverId).toString(), Map.of("Content-Type", "application/json"));
            if (!reply.ok()) {
                if (reply.status() >= 500 || reply.status() == 429) throw new AuthenticationUnavailableException("Ely.by unavailable");
                throw new AuthenticationException("Ely.by session expired; sign in again in Orbit Profiles.");
            }
        } catch (AuthFailure error) { throw new AuthenticationUnavailableException("Ely.by unavailable"); }
    }

    @Override public ProfileResult hasJoinedServer(String name, String serverId, InetAddress ip) throws AuthenticationUnavailableException {
        String url = AUTH + "hasJoined?username=" + AuthProtocol.encode(name) + "&serverId=" + AuthProtocol.encode(serverId);
        if (ip != null) url += "&ip=" + AuthProtocol.encode(ip.getHostAddress());
        try {
            var reply = transport.request("GET", URI.create(url), "", Map.of());
            if (reply.status() >= 500 || reply.status() == 429) throw new AuthenticationUnavailableException("Ely.by unavailable");
            return reply.ok() && reply.body().has("id") ? new ProfileResult(parseProfile(reply.body())) : null;
        } catch (AuthFailure | RuntimeException error) { throw new AuthenticationUnavailableException("Ely.by unavailable"); }
    }

    @Override public ProfileResult fetchProfile(UUID id, boolean requireSecure) {
        try {
            var reply = transport.request("GET", URI.create(AUTH + "profile/" + id.toString().replace("-", "") + "?unsigned=false"), "", Map.of());
            if (!reply.ok() || !reply.body().has("id")) return null;
            GameProfile profile = parseProfile(reply.body());
            return profile.id().equals(id) ? new ProfileResult(profile) : null;
        } catch (AuthFailure | RuntimeException error) { return null; }
    }

    private GameProfile byName(String name) throws AuthFailure {
        if (!name.matches("[A-Za-z0-9_]{3,16}")) throw new AuthFailure(AuthFailure.Reason.RESPONSE);
        var reply = transport.request("GET", URI.create(SKINS + AuthProtocol.encode(name) + "?unsigned=false"), "", Map.of());
        if (!reply.ok()) throw new AuthFailure(AuthFailure.Reason.NETWORK);
        return parseProfile(reply.body());
    }

    @Override public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
        for (String name : names) {
            try { GameProfile profile = byName(name); callback.onProfileLookupSucceeded(profile.name(), profile.id()); }
            catch (AuthFailure | RuntimeException error) {
                callback.onProfileLookupFailed(name, new AuthenticationException("Ely.by profile unavailable"));
            }
        }
    }

    @Override public Optional<NameAndId> findProfileByName(String name) {
        try { GameProfile profile = byName(name); return Optional.of(new NameAndId(profile.id(), profile.name())); }
        catch (AuthFailure | RuntimeException error) { return Optional.empty(); }
    }

    private static GameProfile parseProfile(JsonObject body) throws AuthFailure {
        UUID id = AuthProtocol.uuid(AuthProtocol.required(body, "id"));
        String name = AuthProtocol.required(body, "name");
        if (!name.matches("[A-Za-z0-9_]{3,16}")) throw new AuthFailure(AuthFailure.Reason.RESPONSE);
        var properties = ImmutableMultimap.<String, Property>builder();
        var values = body.get("properties");
        if (values != null) {
            if (!values.isJsonArray() || values.getAsJsonArray().size() > 16) throw new AuthFailure(AuthFailure.Reason.RESPONSE);
            for (var value : values.getAsJsonArray()) {
                if (!value.isJsonObject()) throw new AuthFailure(AuthFailure.Reason.RESPONSE);
                JsonObject property = value.getAsJsonObject();
                if (!"textures".equals(AuthProtocol.required(property, "name"))) continue;
                String packed = AuthProtocol.required(property, "value");
                String signature = AuthProtocol.optional(property, "signature");
                properties.put("textures", signature.isEmpty() ? new Property("textures", packed) : new Property("textures", packed, signature));
            }
        }
        return new GameProfile(id, name, new PropertyMap(properties.build()));
    }

    @Override public Property getPackedTextures(GameProfile profile) {
        return profile.properties().get("textures").stream().findFirst().orElse(null);
    }

    @Override public String getSecurePropertyValue(Property property) {
        if (!validSignature(property)) throw new InsecurePublicKeyException("Invalid Ely texture signature");
        return property.value();
    }

    private boolean validSignature(Property property) {
        if (property == null || !"textures".equals(property.name()) || property.value() == null || !property.hasSignature()
                || property.value().length() > 32768 || property.signature().length() > 8192) return false;
        try {
            // Authlib's helper prints invalid-signature stack traces; verify quietly instead.
            Signature verifier = Signature.getInstance("SHA1withRSA");
            verifier.initVerify(texturesKey);
            verifier.update(property.value().getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(property.signature()));
        } catch (GeneralSecurityException | IllegalArgumentException error) { return false; }
    }

    @Override public MinecraftProfileTextures unpackTextures(Property property) {
        // Placeholder/unsigned/foreign-provider signatures fail closed to the default skin.
        if (!validSignature(property)) return MinecraftProfileTextures.EMPTY;
        try {
            byte[] decoded = Base64.getDecoder().decode(property.value());
            JsonObject payload = JsonParser.parseString(new String(decoded, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject textures = AuthProtocol.object(payload, "textures");
            MinecraftProfileTexture skin = texture(textures, "SKIN");
            MinecraftProfileTexture cape = texture(textures, "CAPE");
            MinecraftProfileTexture elytra = texture(textures, "ELYTRA");
            return new MinecraftProfileTextures(skin, cape, elytra, SignatureState.SIGNED);
        } catch (AuthFailure | RuntimeException error) { return MinecraftProfileTextures.EMPTY; }
    }

    private static MinecraftProfileTexture texture(JsonObject textures, String kind) throws AuthFailure {
        if (!textures.has(kind)) return null;
        JsonObject value = AuthProtocol.object(textures, kind);
        String url = AuthProtocol.required(value, "url");
        URI uri = URI.create(url);
        String host = uri.getHost();
        if (host == null || !Set.of("skinsystem.ely.by", "textures.ely.by", "ely.by", "textures.minecraft.net").contains(host)
                || uri.getUserInfo() != null || uri.getFragment() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                || !(uri.getScheme().equals("https") || uri.getScheme().equals("http"))) {
            throw new AuthFailure(AuthFailure.Reason.RESPONSE);
        }
        // Both official texture hosts support HTTPS; never download cleartext image URLs.
        if (url.startsWith("http://")) url = "https://" + url.substring(7);
        String model = "";
        if (value.has("metadata")) model = AuthProtocol.optional(AuthProtocol.object(value, "metadata"), "model");
        if (!model.isEmpty() && !model.equals("slim")) throw new AuthFailure(AuthFailure.Reason.RESPONSE);
        return new MinecraftProfileTexture(url, model.isEmpty() ? Map.of() : Map.of("model", model));
    }
}