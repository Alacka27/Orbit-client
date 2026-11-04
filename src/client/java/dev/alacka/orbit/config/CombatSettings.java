package dev.alacka.orbit.config;

import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/** Immutable, opt-in combat preferences. Missing keys keep old Orbit files compatible. */
public record CombatSettings(Aura aura, Trigger trigger) {
    public static final CombatSettings DEFAULT = new CombatSettings(Aura.DEFAULT, Trigger.DEFAULT);

    public CombatSettings {
        Objects.requireNonNull(aura, "aura");
        Objects.requireNonNull(trigger, "trigger");
    }

    public CombatSettings withAura(Aura value) { return new CombatSettings(value, trigger); }
    public CombatSettings withTrigger(Trigger value) { return new CombatSettings(aura, value); }

    public enum Priority {
        CLOSEST("Ближайшая"), HEALTH("HP мобов"), ANGLE("Угол прицела");
        public final String label;
        Priority(String label) { this.label = label; }
    }

    public enum Rotation {
        SMOOTH("Плавный"), INSTANT("Мгновенный"), NONE("Без поворота");
        public final String label;
        Rotation(String label) { this.label = label; }
    }

    public enum Targets {
        PLAYERS("Игроки"), MOBS("Мобы"), ALL("Игроки и мобы");
        public final String label;
        Targets(String label) { this.label = label; }
    }

    public enum AttackMode {
        COOLDOWN("1.9+ · кулдаун"), CPS("1.8 · CPS");
        public final String label;
        AttackMode(String label) { this.label = label; }
    }

    public record Aura(boolean enabled, double range, Priority priority, int maxTargets,
                       Rotation rotation, int rotationStep, Targets targets,
                       boolean ignoreTeam, boolean onlyWeapon, boolean pauseUsingItem) {
        public static final Aura DEFAULT = new Aura(false, 3.0, Priority.CLOSEST, 1,
                Rotation.SMOOTH, 20, Targets.PLAYERS, true, true, true);

        public Aura {
            validateRange(range);
            Objects.requireNonNull(priority, "priority");
            Objects.requireNonNull(rotation, "rotation");
            Objects.requireNonNull(targets, "targets");
            if (maxTargets < 1 || maxTargets > 5) throw new IllegalArgumentException("maxTargets: 1..5");
            if (rotationStep < 5 || rotationStep > 90) throw new IllegalArgumentException("rotationStep: 5..90");
        }

        public Aura enabled(boolean v) { return new Aura(v, range, priority, maxTargets, rotation, rotationStep, targets, ignoreTeam, onlyWeapon, pauseUsingItem); }
        public Aura range(double v) { return new Aura(enabled, v, priority, maxTargets, rotation, rotationStep, targets, ignoreTeam, onlyWeapon, pauseUsingItem); }
        public Aura priority(Priority v) { return new Aura(enabled, range, v, maxTargets, rotation, rotationStep, targets, ignoreTeam, onlyWeapon, pauseUsingItem); }
        public Aura maxTargets(int v) { return new Aura(enabled, range, priority, v, rotation, rotationStep, targets, ignoreTeam, onlyWeapon, pauseUsingItem); }
        public Aura rotation(Rotation v) { return new Aura(enabled, range, priority, maxTargets, v, rotationStep, targets, ignoreTeam, onlyWeapon, pauseUsingItem); }
        public Aura rotationStep(int v) { return new Aura(enabled, range, priority, maxTargets, rotation, v, targets, ignoreTeam, onlyWeapon, pauseUsingItem); }
        public Aura targets(Targets v) { return new Aura(enabled, range, priority, maxTargets, rotation, rotationStep, v, ignoreTeam, onlyWeapon, pauseUsingItem); }
        public Aura ignoreTeam(boolean v) { return new Aura(enabled, range, priority, maxTargets, rotation, rotationStep, targets, v, onlyWeapon, pauseUsingItem); }
        public Aura onlyWeapon(boolean v) { return new Aura(enabled, range, priority, maxTargets, rotation, rotationStep, targets, ignoreTeam, v, pauseUsingItem); }
        public Aura pauseUsingItem(boolean v) { return new Aura(enabled, range, priority, maxTargets, rotation, rotationStep, targets, ignoreTeam, onlyWeapon, v); }
    }

    public record Trigger(boolean enabled, AttackMode mode, int cps, double range, Targets targets,
                          boolean ignoreTeam, boolean onlyWeapon, boolean pauseUsingItem) {
        public static final Trigger DEFAULT = new Trigger(false, AttackMode.COOLDOWN, 10, 3.0,
                Targets.PLAYERS, true, true, true);

        public Trigger {
            validateRange(range);
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(targets, "targets");
            if (cps < 1 || cps > 20) throw new IllegalArgumentException("cps: 1..20");
        }

        public Trigger enabled(boolean v) { return new Trigger(v, mode, cps, range, targets, ignoreTeam, onlyWeapon, pauseUsingItem); }
        public Trigger mode(AttackMode v) { return new Trigger(enabled, v, cps, range, targets, ignoreTeam, onlyWeapon, pauseUsingItem); }
        public Trigger cps(int v) { return new Trigger(enabled, mode, v, range, targets, ignoreTeam, onlyWeapon, pauseUsingItem); }
        public Trigger range(double v) { return new Trigger(enabled, mode, cps, v, targets, ignoreTeam, onlyWeapon, pauseUsingItem); }
        public Trigger targets(Targets v) { return new Trigger(enabled, mode, cps, range, v, ignoreTeam, onlyWeapon, pauseUsingItem); }
        public Trigger ignoreTeam(boolean v) { return new Trigger(enabled, mode, cps, range, targets, v, onlyWeapon, pauseUsingItem); }
        public Trigger onlyWeapon(boolean v) { return new Trigger(enabled, mode, cps, range, targets, ignoreTeam, v, pauseUsingItem); }
        public Trigger pauseUsingItem(boolean v) { return new Trigger(enabled, mode, cps, range, targets, ignoreTeam, onlyWeapon, v); }
    }

    private static void validateRange(double range) {
        if (!Double.isFinite(range) || range < 1.0 || range > 6.0)
            throw new IllegalArgumentException("range: 1..6");
    }

    public static CombatSettings read(Properties p) {
        Aura a = Aura.DEFAULT;
        Trigger t = Trigger.DEFAULT;
        return new CombatSettings(
                new Aura(bool(p, "aura.enabled", a.enabled), number(p, "aura.range", a.range),
                        option(p, "aura.priority", a.priority), integer(p, "aura.maxTargets", a.maxTargets),
                        option(p, "aura.rotation", a.rotation), integer(p, "aura.rotationStep", a.rotationStep),
                        option(p, "aura.targets", a.targets), bool(p, "aura.ignoreTeam", a.ignoreTeam),
                        bool(p, "aura.onlyWeapon", a.onlyWeapon), bool(p, "aura.pauseUsingItem", a.pauseUsingItem)),
                new Trigger(bool(p, "trigger.enabled", t.enabled), option(p, "trigger.mode", t.mode),
                        integer(p, "trigger.cps", t.cps), number(p, "trigger.range", t.range),
                        option(p, "trigger.targets", t.targets), bool(p, "trigger.ignoreTeam", t.ignoreTeam),
                        bool(p, "trigger.onlyWeapon", t.onlyWeapon), bool(p, "trigger.pauseUsingItem", t.pauseUsingItem)));
    }

    public void write(Properties p) {
        put(p, "aura.enabled", aura.enabled);
        put(p, "aura.range", aura.range);
        put(p, "aura.priority", aura.priority);
        put(p, "aura.maxTargets", aura.maxTargets);
        put(p, "aura.rotation", aura.rotation);
        put(p, "aura.rotationStep", aura.rotationStep);
        put(p, "aura.targets", aura.targets);
        put(p, "aura.ignoreTeam", aura.ignoreTeam);
        put(p, "aura.onlyWeapon", aura.onlyWeapon);
        put(p, "aura.pauseUsingItem", aura.pauseUsingItem);
        put(p, "trigger.enabled", trigger.enabled);
        put(p, "trigger.mode", trigger.mode);
        put(p, "trigger.cps", trigger.cps);
        put(p, "trigger.range", trigger.range);
        put(p, "trigger.targets", trigger.targets);
        put(p, "trigger.ignoreTeam", trigger.ignoreTeam);
        put(p, "trigger.onlyWeapon", trigger.onlyWeapon);
        put(p, "trigger.pauseUsingItem", trigger.pauseUsingItem);
    }

    private static void put(Properties p, String key, Object value) { p.setProperty("combat." + key, value.toString()); }
    private static String text(Properties p, String key, Object fallback) { return p.getProperty("combat." + key, fallback.toString()).strip(); }
    private static boolean bool(Properties p, String key, boolean fallback) {
        String value = text(p, key, fallback);
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
            throw new IllegalArgumentException("combat." + key);
        return Boolean.parseBoolean(value);
    }
    private static int integer(Properties p, String key, int fallback) { return Integer.parseInt(text(p, key, fallback)); }
    private static double number(Properties p, String key, double fallback) { return Double.parseDouble(text(p, key, fallback)); }
    private static <E extends Enum<E>> E option(Properties p, String key, E fallback) {
        return Enum.valueOf(fallback.getDeclaringClass(), text(p, key, fallback).toUpperCase(Locale.ROOT));
    }
}