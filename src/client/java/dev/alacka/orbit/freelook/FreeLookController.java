package dev.alacka.orbit.freelook;

import com.mojang.blaze3d.platform.InputConstants;
import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.config.FreeLookConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

public final class FreeLookController {
    private static final FreeLookState STATE = new FreeLookState();
    private static KeyMapping holdKey;
    private static FreeLookConfig config = FreeLookConfig.DEFAULT;
    private static Object lastPlayer;
    private static Object lastLevel;
    private static boolean releaseRequired = true;
    private static long lastNanos;

    private FreeLookController() {}

    public static void initialize() {
        try {
            config = FreeLookConfig.load(FabricLoader.getInstance().getConfigDir()
                    .resolve("orbit").resolve("freelook.properties"));
        } catch (Exception error) {
            // On a bad config, disable the feature rather than silently replacing user settings.
            config = new FreeLookConfig(false, 12);
            OrbitClient.LOGGER.error("[Orbit] FreeLook disabled: could not load config. Existing file preserved.", error);
        }
        holdKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.orbit.freelook", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT, KeyMapping.Category.MISC));
        ClientTickEvents.START_CLIENT_TICK.register(client -> synchronize());
    }

    public static boolean enabled() { return config.enabled(); }
    public static String keyLabel() { return holdKey == null ? "Left Alt" : holdKey.getTranslatedKeyMessage().getString(); }
    public static void setEnabled(boolean value) throws java.io.IOException {
        FreeLookConfig next = new FreeLookConfig(value, config.returnSpeed());
        next.save(FabricLoader.getInstance().getConfigDir().resolve("orbit/freelook.properties"));
        config = next; STATE.reset(); releaseRequired = true;
    }

    /** Called by ticks, camera setup and mouse input; all run on the client thread. */
    public static void synchronize() {
        Minecraft client = Minecraft.getInstance();
        long now = System.nanoTime();
        double seconds = lastNanos == 0 ? 0 : (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        if (client.player != lastPlayer || client.level != lastLevel) {
            STATE.reset();
            releaseRequired = true;
            lastPlayer = client.player;
            lastLevel = client.level;
        }
        boolean allowed = holdKey != null && config.enabled()
                && client.player != null && client.level != null
                && client.player.isAlive() && client.getCameraEntity() == client.player
                && client.screen == null && client.isWindowActive()
                && client.options.getCameraType() == CameraType.THIRD_PERSON_BACK;
        if (!allowed) {
            STATE.reset();
            releaseRequired = true;
            return;
        }
        boolean held = holdKey.isDown();
        if (releaseRequired) {
            if (held) return; // Require release after menus, death, world changes or focus loss.
            releaseRequired = false;
        }
        STATE.update(held, seconds, config.returnSpeed());
    }

    /** Receives vanilla sensitivity/inversion-adjusted deltas, before LocalPlayer.turn. */
    public static boolean turnCamera(Entity player, double horizontal, double vertical) {
        synchronize();
        Minecraft client = Minecraft.getInstance();
        if (player != client.player || !STATE.isHeld()) return false;
        STATE.turn(horizontal, vertical, player.getXRot());
        return true;
    }

    public static float cameraYaw(Entity entity, float original) {
        return entity == Minecraft.getInstance().player && STATE.isActive()
                ? STATE.cameraYaw(original) : original;
    }

    public static float cameraPitch(Entity entity, float original) {
        return entity == Minecraft.getInstance().player && STATE.isActive()
                ? STATE.cameraPitch(original) : original;
    }
}
