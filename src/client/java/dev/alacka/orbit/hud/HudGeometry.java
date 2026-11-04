package dev.alacka.orbit.hud;

/** Logical GUI coordinates shared by hotbar drawing and hit testing. */
public final class HudGeometry {
    private HudGeometry() {}
    public record Rect(int x, int y, int width, int height) {}

    public static float scale(int screenWidth) {
        return (float) Math.min(1.45, Math.max(1, (screenWidth - 70) / 390.0));
    }
    public static Rect hotbar(int x, int y, int width, int height) {
        return new Rect(x - 4, y - 2, width + 8, height + 2);
    }
    public static Rect offhand(int x, int y, boolean right) {
        // Vanilla item anchors are x+3 (left), x+10 (right), y+4.
        return new Rect(x + (right ? 9 : 2), y + 3, 18, 18);
    }
}