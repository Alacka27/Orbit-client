package dev.alacka.orbit.accounts;

import com.google.gson.JsonObject;
import java.net.URI;
import java.util.Map;

/** Injectable transport, so protocol tests never need a real password/account. */
@FunctionalInterface
public interface AuthTransport {
    Reply request(String method, URI uri, String body, Map<String, String> headers) throws AuthFailure;

    final class Reply {
        private final int status;
        private final JsonObject body;
        public Reply(int status, JsonObject body) { this.status = status; this.body = body; }
        public int status() { return status; }
        public JsonObject body() { return body; }
        public boolean ok() { return status >= 200 && status < 300; }
        @Override public String toString() { return "AuthReply[HTTP " + status + ", body=REDACTED]"; }
    }
}