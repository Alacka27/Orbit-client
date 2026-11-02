package dev.alacka.orbit.freelook;

/** Pure camera-offset state. Never writes player angles or movement. */
public final class FreeLookState {
    public enum Phase { INACTIVE, HELD, RETURNING }
    private Phase phase = Phase.INACTIVE;
    private double yawOffset;
    private double pitchOffset;
    private static final double EPSILON = 0.03;

    public void update(boolean held, double seconds, double returnSpeed) {
        if (held) {
            phase = Phase.HELD;
            return;
        }
        if (phase == Phase.INACTIVE) return;
        phase = Phase.RETURNING;
        double dt = Double.isFinite(seconds) ? Math.max(0, Math.min(0.25, seconds)) : 0;
        double speed = Double.isFinite(returnSpeed) ? Math.max(1, Math.min(40, returnSpeed)) : 12;
        double decay = Math.exp(-speed * dt);
        yawOffset *= decay;
        pitchOffset *= decay;
        if (Math.abs(yawOffset) < EPSILON && Math.abs(pitchOffset) < EPSILON) reset();
    }

    public void turn(double horizontal, double vertical, float playerPitch) {
        if (!isHeld() || !Double.isFinite(horizontal) || !Double.isFinite(vertical)) return;
        yawOffset = wrap(yawOffset + horizontal * 0.15);
        pitchOffset = clamp(playerPitch + pitchOffset + vertical * 0.15, -90, 90) - playerPitch;
    }

    public float cameraYaw(float vanillaYaw) { return (float) (vanillaYaw + yawOffset); }
    public float cameraPitch(float vanillaPitch) {
        return (float) clamp(vanillaPitch + pitchOffset, -90, 90);
    }
    public boolean isHeld() { return phase == Phase.HELD; }
    public boolean isActive() { return phase != Phase.INACTIVE; }
    public Phase phase() { return phase; }
    public void reset() {
        phase = Phase.INACTIVE;
        yawOffset = 0;
        pitchOffset = 0;
    }
    private static double wrap(double angle) {
        double value = angle % 360;
        if (value >= 180) value -= 360;
        if (value < -180) value += 360;
        return value;
    }
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
