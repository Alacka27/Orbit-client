package dev.alacka.orbit.config;

import dev.alacka.orbit.OrbitClient;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Preferences are isolated by the current launch identity, without changing its session. */
public final class OrbitSettings {
    private static UUID id;
    private static ClientSettings saved = ClientSettings.DEFAULT;
    private static ClientSettings preview;
    private OrbitSettings() {}
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("orbit/settings/" + id + ".properties"); }
    public static ClientSettings get() {
        var mc = Minecraft.getInstance();
        if (mc != null && mc.getUser() != null && !mc.getUser().getProfileId().equals(id)) {
            id = mc.getUser().getProfileId(); preview = null;
            try {
                saved = ClientSettings.load(path());
                if (!java.nio.file.Files.exists(path())) saved = saved.theme(saved.accent(),saved.windowOpacity(),saved.buttonOpacity(),dev.alacka.orbit.menu.OrbitMenu.legacyAnimation());
            }
            catch (IOException e) { saved = ClientSettings.DEFAULT; OrbitClient.LOGGER.error("[Orbit] Invalid profile preferences preserved", e); }
        }
        return saved;
    }
    public static ClientSettings theme() { get(); return preview == null ? saved : preview; }
    public static void preview(ClientSettings draft) { get(); preview = draft; }
    public static void cancelPreview() { preview = null; }
    public static void save(ClientSettings value) throws IOException {
        get();
        if (id == null) throw new IOException("Launch identity is not available");
        value.save(path()); saved = value; preview = null;
    }
}
