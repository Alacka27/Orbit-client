package dev.alacka.orbit.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.config.BindPrefs;
import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.modules.Modules;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Internal controls only: no vanilla KeyMapping instances, registration or options writes. */
public final class ModuleBinds {
    private static final Map<String, HoldGate> HOLDS = new HashMap<>();
    private static final PressQueue SWAP = new PressQueue();
    private static Map<String, Integer> codes = Map.of();
    private static Object lastPlayer, lastWorld;
    private static UUID lastId;

    private ModuleBinds() {}

    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            synchronize(mc);
            held("FreeLook", true);
            held("Zoom", true);
            if (!active(mc)) SWAP.clear();
        });
    }

    public static boolean active(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.player.isAlive() && !mc.player.isSleeping()
                && mc.screen == null && mc.getOverlay() == null && mc.isWindowActive() && !mc.isPaused();
    }

    public static int baseCode(String module) {
        return BindPrefs.defaultCode(module);
    }

    public static int code(String module) {
        return OrbitSettings.get().binds().keys().getOrDefault(module, baseCode(module));
    }

    public static String label(String module) { return label(code(module)); }

    public static String label(int code) {
        if (code == BindPrefs.NONE) return "Нет";
        if (code >= BindPrefs.MOUSE) return "Мышь " + (code - BindPrefs.MOUSE + 1);
        return switch (code) {
            case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
            default -> InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
        };
    }

    public static String mode(String module) {
        return switch (module) {
            case "Menu" -> "Открыть / закрыть Orbit · только клавиатура";
            case "FreeLook", "Zoom" -> "Работает при удержании";
            case "ElytraSwap" -> "Обмен по нажатию · модуль должен быть включён";
            default -> "Нажатие — включить / выключить";
        };
    }

    private static void synchronize(Minecraft mc) {
        UUID id = mc.getUser() == null ? null : mc.getUser().getProfileId();
        Map<String, Integer> next = new LinkedHashMap<>();
        for (String name : BindPrefs.MODULES) next.put(name, code(name));
        if (mc.player != lastPlayer || mc.level != lastWorld || !java.util.Objects.equals(lastId, id)
                || !next.equals(codes)) {
            HOLDS.values().forEach(HoldGate::reset);
            SWAP.clear();
            lastPlayer = mc.player; lastWorld = mc.level; lastId = id;
            codes = Map.copyOf(next);
        }
    }

    private static boolean down(Minecraft mc, int code) {
        if (code == BindPrefs.NONE) return false;
        return code >= BindPrefs.MOUSE
                ? GLFW.glfwGetMouseButton(mc.getWindow().handle(), code - BindPrefs.MOUSE) == GLFW.GLFW_PRESS
                : InputConstants.isKeyDown(mc.getWindow(), code);
    }

    public static boolean held(String module, boolean enabled) {
        Minecraft mc = Minecraft.getInstance();
        synchronize(mc);
        return HOLDS.computeIfAbsent(module, ignored -> new HoldGate())
                .update(active(mc) && enabled, down(mc, codes.getOrDefault(module, BindPrefs.NONE)));
    }

    public static void key(Minecraft mc, int action, KeyEvent event) {
        if (action == GLFW.GLFW_PRESS && event.key() >= 0 && BindPrefs.validCode(event.key())) press(mc, event.key());
    }

    public static void mouse(Minecraft mc, int action, int button) {
        if (action == GLFW.GLFW_PRESS && button >= 0 && button < 8) press(mc, BindPrefs.MOUSE + button);
    }

    private static void press(Minecraft mc, int code) {
        synchronize(mc);
        if (!active(mc)) { SWAP.clear(); return; }
        for (String module : BindPrefs.MODULES) {
            if (codes.get(module) != code || BindPrefs.required(module)) continue;
            if (module.equals("ElytraSwap")) {
                SWAP.request(OrbitSettings.get().support().elytra().enabled());
                continue;
            }
            try {
                boolean enabled = !Modules.enabled(module);
                OrbitSettings.save(OrbitSettings.get().module(module, enabled));
                mc.player.displayClientMessage(Component.literal("Orbit · " + module + ": " + (enabled ? "ВКЛ" : "ВЫКЛ")), true);
            } catch (Exception error) {
                OrbitClient.LOGGER.error("[Orbit] Cannot apply module binding: {}", module, error);
                mc.player.displayClientMessage(Component.literal("Orbit: не удалось сохранить настройку"), true);
            }
        }
    }

    public static boolean consumeSwap(boolean allowed) {
        synchronize(Minecraft.getInstance());
        return SWAP.consume(allowed);
    }
}