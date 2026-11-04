package dev.alacka.orbit.accounts;

public interface SessionAccess {
    SessionState orbit$captureSession();
    void orbit$applySession(SessionState state);
    boolean orbit$hasPendingConnection();
}