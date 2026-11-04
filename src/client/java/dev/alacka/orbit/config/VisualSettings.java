package dev.alacka.orbit.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Opt-in visual modules. No server-side state, packets or invented player statistics. */
public record VisualSettings(Map<String, Boolean> modules, int range, int targetSeconds,
                             int particleCount, int numberTicks, boolean healing,
                             boolean seeMobs, boolean trajectoryGradient, boolean trajectoryTime) {
    public static final List<String> MODULES = List.of("TargetHUD", "DamageNumbers", "HitParticles",
            "CriticalHitEffect", "ShieldStatus", "TotemCounter", "SeeInvisibles");
    public static final VisualSettings DEFAULT = new VisualSettings(Map.of(), 32, 4, 8, 24,
            true, false, true, true);

    public VisualSettings {
        if (modules == null || modules.entrySet().stream().anyMatch(e ->
                !MODULES.contains(e.getKey()) || e.getValue() == null)
                || range < 8 || range > 64 || targetSeconds < 1 || targetSeconds > 10
                || particleCount < 2 || particleCount > 32 || numberTicks < 12 || numberTicks > 60)
            throw new IllegalArgumentException("Invalid visual preferences");
        // Canonicalize false entries so old/new files have the same defaults.
        var copy = new LinkedHashMap<String, Boolean>();
        modules.forEach((name, enabled) -> { if (enabled) copy.put(name, true); });
        modules = Map.copyOf(copy);
    }

    public boolean enabled(String module) {
        if (!MODULES.contains(module)) throw new IllegalArgumentException("Unknown visual module: " + module);
        return modules.getOrDefault(module, false);
    }
    public boolean anyEnabled() { return !modules.isEmpty(); }
    public VisualSettings enabled(String module, boolean value) {
        if (!MODULES.contains(module)) throw new IllegalArgumentException("Unknown visual module: " + module);
        var next = new LinkedHashMap<>(modules);
        if (value) next.put(module, true); else next.remove(module);
        return new VisualSettings(next, range, targetSeconds, particleCount, numberTicks, healing,
                seeMobs, trajectoryGradient, trajectoryTime);
    }
    public VisualSettings number(String option, int value) {
        return new VisualSettings(modules,
                option.equals("range") ? value : range,
                option.equals("targetSeconds") ? value : targetSeconds,
                option.equals("particleCount") ? value : particleCount,
                option.equals("numberTicks") ? value : numberTicks,
                healing, seeMobs, trajectoryGradient, trajectoryTime).knownNumber(option);
    }
    private VisualSettings knownNumber(String option) {
        if (!List.of("range", "targetSeconds", "particleCount", "numberTicks").contains(option))
            throw new IllegalArgumentException("Unknown visual option");
        return this;
    }
    public VisualSettings option(String option, boolean value) {
        if (!List.of("healing", "seeMobs", "trajectoryGradient", "trajectoryTime").contains(option))
            throw new IllegalArgumentException("Unknown visual option");
        return new VisualSettings(modules, range, targetSeconds, particleCount, numberTicks,
                option.equals("healing") ? value : healing,
                option.equals("seeMobs") ? value : seeMobs,
                option.equals("trajectoryGradient") ? value : trajectoryGradient,
                option.equals("trajectoryTime") ? value : trajectoryTime);
    }

    /** Rebase a dialog's options, never its stale enabled states or another module's options. */
    public VisualSettings settingsFrom(String module, VisualSettings source) {
        if (!MODULES.contains(module) && !module.equals("Trajectories"))
            throw new IllegalArgumentException("Unknown visual module");
        return new VisualSettings(modules, module.equals("Trajectories") ? range : source.range,
                module.equals("TargetHUD") ? source.targetSeconds : targetSeconds,
                List.of("HitParticles", "CriticalHitEffect").contains(module) ? source.particleCount : particleCount,
                module.equals("DamageNumbers") ? source.numberTicks : numberTicks,
                module.equals("DamageNumbers") ? source.healing : healing,
                module.equals("SeeInvisibles") ? source.seeMobs : seeMobs,
                module.equals("Trajectories") ? source.trajectoryGradient : trajectoryGradient,
                module.equals("Trajectories") ? source.trajectoryTime : trajectoryTime);
    }

    public static VisualSettings read(Properties p) {
        var states = new LinkedHashMap<String, Boolean>();
        for (String module : MODULES) if (bool(p, "visual." + module, false)) states.put(module, true);
        return new VisualSettings(states, integer(p, "range", 32), integer(p, "targetSeconds", 4),
                integer(p, "particleCount", 8), integer(p, "numberTicks", 24),
                bool(p, "visual.healing", true), bool(p, "visual.seeMobs", false),
                bool(p, "visual.trajectoryGradient", true), bool(p, "visual.trajectoryTime", true));
    }
    private static int integer(Properties p, String key, int fallback) {
        return Integer.parseInt(p.getProperty("visual." + key, Integer.toString(fallback)).strip());
    }
    private static boolean bool(Properties p, String key, boolean fallback) {
        String s = p.getProperty(key, Boolean.toString(fallback)).strip();
        if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false")) throw new IllegalArgumentException(key);
        return Boolean.parseBoolean(s);
    }
    public void write(Properties p) {
        for (String module : MODULES) p.setProperty("visual." + module, Boolean.toString(enabled(module)));
        p.setProperty("visual.range", Integer.toString(range));
        p.setProperty("visual.targetSeconds", Integer.toString(targetSeconds));
        p.setProperty("visual.particleCount", Integer.toString(particleCount));
        p.setProperty("visual.numberTicks", Integer.toString(numberTicks));
        p.setProperty("visual.healing", Boolean.toString(healing));
        p.setProperty("visual.seeMobs", Boolean.toString(seeMobs));
        p.setProperty("visual.trajectoryGradient", Boolean.toString(trajectoryGradient));
        p.setProperty("visual.trajectoryTime", Boolean.toString(trajectoryTime));
    }
}