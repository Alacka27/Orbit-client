package dev.alacka.orbit.zoom;

/** Frame-rate-independent optical zoom. Never writes the player's FOV option. */
public final class ZoomState {
    public static final double MIN = 1.25, MAX = 30, DEFAULT = 3;
    private double selected = DEFAULT, current = 1;

    public void scroll(double amount) {
        if (!Double.isFinite(amount)) return;
        selected = Math.clamp(selected * Math.pow(1.2, Math.clamp(amount, -20, 20)), MIN, MAX);
    }

    public double update(boolean held, double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) seconds = 0;
        double target = held ? selected : 1;
        double blend = -Math.expm1(-16 * Math.min(.1, seconds));
        current += (target - current) * blend;
        if (Math.abs(current - target) < .001) current = target;
        return current;
    }

    public double multiplier() { return current; }
    public double selected() { return selected; }
    public double fov(double original) {
        if (current == 1 || !Double.isFinite(original) || original <= 0 || original >= 180) return original;
        return Math.toDegrees(2 * Math.atan(Math.tan(Math.toRadians(original) / 2) / current));
    }
    public void reset() { current = 1; selected = DEFAULT; }
}