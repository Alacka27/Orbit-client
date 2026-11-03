package dev.alacka.orbit.modules;

/** Deterministic centre-line physics; random spread is deliberately not predicted. */
public final class Ballistics {
    private Ballistics() {}
    public record Vector(double x,double y,double z) {}
    public static Vector nextVelocity(Vector v,double drag,double gravity,boolean throwable) {
        return new Vector(v.x*drag,throwable?(v.y-gravity)*drag:v.y*drag-gravity,v.z*drag);
    }
}
