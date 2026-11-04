package dev.alacka.orbit.modules;

import dev.alacka.orbit.config.CombatSettings.Priority;
import java.util.Comparator;
import java.util.List;

/** Pure selection/rotation logic; no packets, game state or randomised bypass behaviour. */
public final class CombatMath {
    private CombatMath() {}

    public record Angles(float yaw, float pitch) {}
    public record Candidate(int id, double distanceSquared, double angle, Double knownHealth) {}

    public static double wrapDegrees(double value) {
        double result = value % 360.0;
        if (result >= 180.0) result -= 360.0;
        if (result < -180.0) result += 360.0;
        return result;
    }

    public static Angles angles(double dx, double dy, double dz) {
        return new Angles((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0),
                (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
    }

    public static float stepYaw(float current, float target, float step) {
        return current + (float) Math.clamp(wrapDegrees(target - current), -step, step);
    }

    public static float stepPitch(float current, float target, float step) {
        return Math.clamp(current + Math.clamp(target - current, -step, step), -90.0f, 90.0f);
    }

    public static double angularDistance(float yaw, float pitch, Angles target) {
        return Math.hypot(wrapDegrees(target.yaw - yaw), target.pitch - pitch);
    }

    public static Comparator<Candidate> order(Priority priority) {
        Comparator<Candidate> nearest = Comparator.comparingDouble(Candidate::distanceSquared);
        Comparator<Candidate> angle = Comparator.comparingDouble(Candidate::angle);
        return switch (priority) {
            case CLOSEST -> nearest.thenComparing(angle).thenComparingInt(Candidate::id);
            case ANGLE -> angle.thenComparing(nearest).thenComparingInt(Candidate::id);
            // Unknown remote-player HP is never replaced with a fabricated "20".
            case HEALTH -> Comparator.comparingDouble((Candidate c) ->
                    c.knownHealth == null ? Double.POSITIVE_INFINITY : c.knownHealth)
                    .thenComparing(nearest).thenComparing(angle).thenComparingInt(Candidate::id);
        };
    }

    /** Keep an aimed target until the hit, then cycle inside the priority-selected pool. */
    public static int cycleIndex(List<Integer> ids, Integer currentId, Integer lastHitId) {
        if (ids.isEmpty()) return -1;
        int current = currentId == null ? -1 : ids.indexOf(currentId);
        if (current >= 0) return current;
        int last = lastHitId == null ? -1 : ids.indexOf(lastHitId);
        return last < 0 ? 0 : (last + 1) % ids.size();
    }
}