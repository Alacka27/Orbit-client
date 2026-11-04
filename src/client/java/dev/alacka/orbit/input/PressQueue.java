package dev.alacka.orbit.input;

/** A one-shot action, not a repeated key state. Menus and identity changes discard it. */
public final class PressQueue {
    private boolean pending;
    public void request(boolean allowed) { if (allowed) pending = true; }
    public boolean consume(boolean allowed) {
        boolean result = allowed && pending;
        pending = false;
        return result;
    }
    public void clear() { pending = false; }
}