package dev.alacka.orbit.modules;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded observation history. Unknown health is NOT a zero or a vanilla 20 HP guess. */
public final class VisualLedger {
    public static final int LIMIT = 256;
    private record Health(double value, long tick) {}
    private record Pops(int count, long tick) {}
    private final Map<UUID, Health> health = new LinkedHashMap<>();
    private final Map<UUID, Pops> pops = new LinkedHashMap<>();
    private final Map<UUID, Long> critical = new LinkedHashMap<>();

    /**
     * Vanilla sends an objective's HEARTS render declaration, not its server-side
     * criterion. Ordinary integer scores and absent entries are never guessed HP.
     * Even HEARTS is the server's reported score, not proof of an entity's max HP.
     */
    public static double declaredHealthScore(boolean hearts, Integer value) {
        return hearts && value != null && value >= 0 && value <= 4096 ? value : Double.NaN;
    }

    /** Positive = observed damage; negative = observed healing; NaN = no trustworthy delta. */
    public double observe(UUID id, double totalHealth, boolean known, long tick) {
        if (id == null) throw new IllegalArgumentException("entity");
        Health before = health.remove(id);
        if (!known || !Double.isFinite(totalHealth) || totalHealth < 0 || totalHealth > 4096)
            return Double.NaN;
        health.put(id, new Health(totalHealth, tick));
        trim(health);
        if (before == null || tick <= before.tick || tick - before.tick > 40) return Double.NaN;
        return before.value - totalHealth;
    }
    public boolean critical(UUID id, long tick) {
        Long previous = critical.remove(id);
        critical.put(id, tick); trim(critical);
        return previous == null || tick < previous || tick - previous > 2;
    }
    public boolean isCritical(UUID id, long tick) {
        Long at = critical.get(id);
        return at != null && tick >= at && tick - at <= 3;
    }
    public int pop(UUID id, long tick) {
        Pops previous = pops.remove(id);
        int count = previous == null ? 1 : previous.count + (previous.tick == tick ? 0 : 1);
        pops.put(id, new Pops(Math.min(9999, count), tick)); trim(pops);
        return Math.min(9999, count);
    }
    public int pops(UUID id) { Pops p = pops.get(id); return p == null ? 0 : p.count; }
    public int trackedHealth() { return health.size(); }
    public int trackedPops() { return pops.size(); }
    public void clearHealth() { health.clear(); }
    public void clearPops() { pops.clear(); }
    public void prune(long tick) {
        health.entrySet().removeIf(e -> tick < e.getValue().tick || tick - e.getValue().tick > 40);
        critical.entrySet().removeIf(e -> tick < e.getValue() || tick - e.getValue() > 4);
    }
    public void clear() { health.clear(); pops.clear(); critical.clear(); }
    private static void trim(Map<?, ?> map) {
        while (map.size() > LIMIT) map.remove(map.keySet().iterator().next());
    }
    public static double alpha(double age, int duration) {
        if (!Double.isFinite(age) || age < 0 || duration <= 0 || age >= duration) return 0;
        return Math.min(1, (duration - age) / Math.max(1, duration * .4));
    }
    public static int gradient(int argb, double progress) {
        double t = Double.isFinite(progress) ? Math.clamp(progress, 0, 1) : 0;
        int result = argb & 0xff000000;
        for (int shift : new int[]{16, 8, 0}) {
            int c = argb >>> shift & 255;
            int near = (int) Math.round(c + (255 - c) * .32);
            int far = (int) Math.round(c * .68);
            result |= (int) Math.round(near + (far - near) * t) << shift;
        }
        return result;
    }
}