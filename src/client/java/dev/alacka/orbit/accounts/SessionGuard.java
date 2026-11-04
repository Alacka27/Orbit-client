package dev.alacka.orbit.accounts;

/** Kept separate for exhaustive tests; never change an identity during a connection. */
public final class SessionGuard {
    private SessionGuard() {}
    public static boolean canSwitch(boolean clientThread, boolean level, boolean player, boolean connection,
                                    boolean pendingConnection, boolean integratedServer, boolean overlay) {
        return clientThread && !level && !player && !connection && !pendingConnection && !integratedServer && !overlay;
    }
}