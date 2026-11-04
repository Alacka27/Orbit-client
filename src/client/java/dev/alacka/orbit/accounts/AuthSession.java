package dev.alacka.orbit.accounts;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Online credentials live only in RAM. Deliberately not a record/serializable DTO. */
public final class AuthSession {
    public enum Provider { ELY, MICROSOFT }
    private final Provider provider;
    private final UUID id;
    private final String name;
    private final String accessToken;
    private final String refreshToken;
    private final String clientToken;
    private final String clientId;
    private final String xuid;
    private final Instant expiresAt;

    public AuthSession(Provider provider, UUID id, String name, String accessToken,
                       String refreshToken, String clientToken, String clientId,
                       String xuid, Instant expiresAt) {
        this.provider = Objects.requireNonNull(provider);
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.accessToken = Objects.requireNonNull(accessToken);
        this.refreshToken = Objects.requireNonNull(refreshToken);
        this.clientToken = Objects.requireNonNull(clientToken);
        this.clientId = Objects.requireNonNull(clientId);
        this.xuid = Objects.requireNonNull(xuid);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    public Provider provider() { return provider; }
    public UUID id() { return id; }
    public String name() { return name; }
    public String key() { return provider.name().toLowerCase(java.util.Locale.ROOT) + ":" + id; }
    public String accessToken() { return accessToken; }
    public String refreshToken() { return refreshToken; }
    public String clientToken() { return clientToken; }
    public String clientId() { return clientId; }
    public String xuid() { return xuid; }
    public Instant expiresAt() { return expiresAt; }

    @Override public String toString() { return "AuthSession[" + provider + ", credentials=REDACTED]"; }
}