package dev.alacka.orbit.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** Internal per-profile controls. Never register these with Minecraft's KeyMapping registry. */
public record BindPrefs(Map<String, Integer> keys) {
    public static final List<String> MODULES = List.of("Menu", "FreeLook", "Zoom", "KillAura", "TriggerBot",
            "NoFall", "Jesus", "AutoTotem", "AutoArmor", "AutoConsume", "ElytraSwap", "Trajectories",
            "TargetHUD", "DamageNumbers", "HitParticles", "CriticalHitEffect", "ShieldStatus", "TotemCounter", "SeeInvisibles");
    public static final BindPrefs DEFAULT = new BindPrefs(Map.of());
    public static final int NONE = -1;
    public static final int MOUSE = 1000;

    public BindPrefs {
        Objects.requireNonNull(keys, "keys");
        for (var entry : keys.entrySet()) {
            if (!MODULES.contains(entry.getKey()) || entry.getValue() == null || !allowed(entry.getKey(), entry.getValue()))
                throw new IllegalArgumentException("Invalid module binding");
        }
        keys = Map.copyOf(keys);
    }

    public BindPrefs with(String module, int code) {
        if (!MODULES.contains(module) || !allowed(module, code)) throw new IllegalArgumentException("Invalid binding");
        Map<String, Integer> next = new LinkedHashMap<>(keys);
        next.put(module, code);
        return new BindPrefs(next);
    }

    public BindPrefs without(String module) {
        if (!MODULES.contains(module)) throw new IllegalArgumentException("Invalid module");
        Map<String, Integer> next = new LinkedHashMap<>(keys);
        next.remove(module);
        return new BindPrefs(next);
    }

    public static boolean validCode(int code) {
        return code == NONE || code >= MOUSE && code < MOUSE + 8
                || code == 32 || code == 39 || code >= 44 && code <= 57 || code == 59 || code == 61
                || code >= 65 && code <= 93 || code == 96 || code == 161 || code == 162
                || code >= 256 && code <= 269 || code >= 280 && code <= 284
                || code >= 290 && code <= 314 || code >= 320 && code <= 336 || code >= 340 && code <= 348;
    }

    public static boolean required(String action) { return List.of("Menu", "FreeLook", "Zoom").contains(action); }
    public static boolean allowed(String action, int code) {
        return validCode(code) && !(required(action) && code == NONE) && !(action.equals("Menu") && code >= MOUSE);
    }
    public static int defaultCode(String action) {
        return switch (action) {
            case "Menu" -> 344; // GLFW right shift, without constructing a Minecraft KeyMapping.
            case "FreeLook" -> 342;
            case "Zoom" -> 67;
            case "ElytraSwap" -> 71;
            default -> NONE;
        };
    }

    public static BindPrefs read(Properties p) {
        Map<String, Integer> keys = new LinkedHashMap<>();
        for (String module : MODULES) {
            String value = p.getProperty("bind." + module);
            if (value != null) {
                int code = Integer.parseInt(value.strip());
                // Old "unbound" built-ins migrate to their always-available defaults.
                if (required(module) && code == NONE) continue;
                keys.put(module, code);
            }
        }
        return new BindPrefs(keys);
    }

    public void write(Properties p) {
        for (String module : MODULES) p.remove("bind." + module);
        keys.forEach((name, code) -> p.setProperty("bind." + name, Integer.toString(code)));
    }
}