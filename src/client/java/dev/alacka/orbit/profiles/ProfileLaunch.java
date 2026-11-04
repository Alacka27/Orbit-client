package dev.alacka.orbit.profiles;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.User;
import net.minecraft.client.main.GameConfig;
import dev.alacka.orbit.accounts.AccountSessions;

/** Capture the unmodified launcher identity. Saved offline choices apply safely at the main menu. */
public final class ProfileLaunch {
    private static User launcher;
    private ProfileLaunch() {}
    public static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("orbit/profiles.properties"); }
    public static String active() { return AccountSessions.active(); }
    public static User launcherUser() { return launcher; }
    public static GameConfig apply(GameConfig config) {
        launcher = config.user.user;
        return config;
    }
}
