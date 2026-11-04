package dev.alacka.orbit.menu;

/** Decoration only: native slot positions and their 16px hit targets never move. */
public record ContainerLayout(int x, int y, int width, int height, int closeX, int closeY, int hintY) {
    public static ContainerLayout of(int left, int top, int imageWidth, int imageHeight, int screenWidth, int screenHeight) {
        int x = Math.max(3, left - 12), y = Math.max(3, top - 12);
        int right = Math.min(screenWidth - 3, left + imageWidth + 12);
        int bottom = Math.min(screenHeight - 3, top + imageHeight + 21);
        return new ContainerLayout(x, y, Math.max(1, right - x), Math.max(1, bottom - y),
                right - 16, y + 5, Math.min(bottom - 12, top + imageHeight + 5));
    }
    public boolean closes(double mx, double my) { return mx >= closeX && mx < closeX + 11 && my >= closeY && my < closeY + 11; }
}
