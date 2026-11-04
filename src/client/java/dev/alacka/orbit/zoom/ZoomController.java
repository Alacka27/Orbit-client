package dev.alacka.orbit.zoom;

import dev.alacka.orbit.input.ModuleBinds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/** Render-only lens and proportional mouse sensitivity; Options.fov is never changed. */
public final class ZoomController {
    private static final ZoomState STATE = new ZoomState();
    private static Object lastPlayer, lastWorld;
    private static long lastNanos;

    private ZoomController() {}

    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            if (mc.player != lastPlayer || mc.level != lastWorld || !allowed(mc)) reset();
            lastPlayer = mc.player; lastWorld = mc.level;
        });
    }

    private static boolean allowed(Minecraft mc) { return ModuleBinds.active(mc); }
    private static void reset() { STATE.reset(); lastNanos = 0; }

    public static float fov(float original) {
        Minecraft mc = Minecraft.getInstance();
        if (!allowed(mc)) { ModuleBinds.held("Zoom", false); reset(); return original; }
        long now = System.nanoTime();
        double seconds = lastNanos == 0 ? 0 : (now - lastNanos) / 1e9;
        lastNanos = now;
        STATE.update(ModuleBinds.held("Zoom", true), seconds);
        return (float) STATE.fov(original);
    }

    public static boolean scroll(double amount) {
        if (!ModuleBinds.held("Zoom", true)) return false;
        STATE.scroll(amount);
        return true;
    }

    public static double sensitivity() { return allowed(Minecraft.getInstance()) ? 1 / STATE.multiplier() : 1; }
    public static double magnification() { return STATE.multiplier(); }
    public static double selected() { return STATE.selected(); }
}