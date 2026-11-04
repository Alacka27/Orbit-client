package dev.alacka.orbit.accounts;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/** HTTPS only, fixed providers, no redirects, bounded replies and no body logging. */
public final class AuthHttp implements AuthTransport {
    private static final int MAX_REPLY = 131_072;
    private static final Set<String> HOSTS = Set.of(
            "authserver.ely.by", "skinsystem.ely.by", "login.microsoftonline.com",
            "user.auth.xboxlive.com", "xsts.auth.xboxlive.com", "api.minecraftservices.com");
    private final Proxy proxy;

    public AuthHttp(Proxy proxy) { this.proxy = proxy == null ? Proxy.NO_PROXY : proxy; }

    public static boolean allowed(URI uri) {
        return uri != null && "https".equals(uri.getScheme()) && uri.getUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getFragment() == null
                && HOSTS.contains(uri.getHost());
    }

    @Override public Reply request(String method, URI uri, String body, Map<String, String> headers) throws AuthFailure {
        if (!allowed(uri) || !(method.equals("GET") || method.equals("POST"))) {
            throw new AuthFailure(AuthFailure.Reason.RESPONSE);
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection(proxy);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(15_000);
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "Orbit-Accounts/0.4.10");
            connection.setUseCaches(false);
            for (var header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (method.equals("POST")) {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (var out = connection.getOutputStream()) { out.write(bytes); }
            }
            int status = connection.getResponseCode();
            if (status >= 300 && status < 400) throw new AuthFailure(AuthFailure.Reason.RESPONSE);
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            byte[] bytes = new byte[0];
            if (stream != null) {
                try (stream) { bytes = stream.readNBytes(MAX_REPLY + 1); }
            }
            return decodeReply(status, bytes);
        } catch (AuthFailure error) {
            throw error;
        } catch (java.io.IOException error) {
            throw new AuthFailure(AuthFailure.Reason.NETWORK);
        } catch (RuntimeException error) {
            throw new AuthFailure(AuthFailure.Reason.RESPONSE);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    static Reply decodeReply(int status, byte[] bytes) throws AuthFailure {
        if (bytes.length > MAX_REPLY) throw new AuthFailure(AuthFailure.Reason.RESPONSE);
        JsonObject json = new JsonObject();
        if (bytes.length > 0) {
            try {
                var parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
                if (!parsed.isJsonObject()) throw new IllegalArgumentException();
                json = parsed.getAsJsonObject();
            } catch (RuntimeException error) {
                // Providers/proxies may return HTML for rate limits or outages. Keep the
                // status for a safe, useful error; a malformed successful reply still fails closed.
                if (status < 400) throw new AuthFailure(AuthFailure.Reason.RESPONSE);
            }
        }
        return new Reply(status, json);
    }
}