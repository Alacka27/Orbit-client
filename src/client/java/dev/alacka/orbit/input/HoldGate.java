package dev.alacka.orbit.input;

/** A held input must be released after a menu, focus loss, identity or bind change. */
public final class HoldGate {
    private boolean releaseRequired = true;

    public boolean update(boolean allowed, boolean down) {
        if (!allowed) { reset(); return false; }
        if (releaseRequired) {
            if (!down) releaseRequired = false;
            return false;
        }
        return down;
    }

    public void reset() { releaseRequired = true; }
}