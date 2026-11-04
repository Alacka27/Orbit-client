package dev.alacka.orbit.modules;

import dev.alacka.orbit.config.CombatSettings.AttackMode;

/** One shared attack budget: both modules together still issue at most one attack per tick. */
public final class CombatScheduler {
    private long lastAttackTick = Long.MIN_VALUE;
    private double nextCpsTick = Double.NaN;

    public boolean ready(long tick, float strength, AttackMode mode) {
        if (tick <= lastAttackTick) return false;
        if (mode == AttackMode.COOLDOWN) return Float.isFinite(strength) && strength >= 0.999f;
        return !Double.isFinite(nextCpsTick) || tick + 1.0e-8 >= nextCpsTick;
    }

    public void attacked(long tick, int cps) {
        if (cps < 1 || cps > 20) throw new IllegalArgumentException("cps");
        lastAttackTick = tick;
        // Retain fractional phase (e.g. 13 CPS); do not accumulate a burst after idle time.
        double base = Double.isFinite(nextCpsTick) && tick - nextCpsTick <= 1.0
                ? nextCpsTick : tick;
        nextCpsTick = base + 20.0 / cps;
    }

    public void reset() {
        lastAttackTick = Long.MIN_VALUE;
        nextCpsTick = Double.NaN;
    }
}