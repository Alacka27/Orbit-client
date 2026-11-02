package dev.alacka.orbit.profiles;

import dev.alacka.orbit.OrbitClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.User;
import net.minecraft.client.main.GameConfig;

/** Applied once, before Minecraft builds its session-dependent services. */
public final class ProfileLaunch {
    private static String active = ProfileStore.LAUNCHER;
    private ProfileLaunch() {}
    public static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("orbit/profiles.properties"); }
    public static String active() { return active; }
    public static GameConfig apply(GameConfig config) {
        try {
            String selected = ProfileStore.read(path()).selected();
            if (selected.equals(ProfileStore.LAUNCHER)) return config;
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + selected).getBytes(StandardCharsets.UTF_8));
            User user = new User(selected, uuid, "", Optional.empty(), Optional.empty());
            active = selected;
            return new GameConfig(new GameConfig.UserData(user, config.user.proxy), config.display,
                    config.location, config.game, config.quickPlay);
        } catch (Exception error) {
            OrbitClient.LOGGER.error("[Orbit] Could not load profiles; using launcher session; file preserved.", error);
            return config;
        }
    }
}
