package dev.alacka.orbit.menu;

/** Pure layout in scaled GUI coordinates, including small and HiDPI windows. */
public record ClickMenuLayout(int x, int y, int width, int height, int sidebar, int tabHeight) {
    public static ClickMenuLayout of(int screenWidth, int screenHeight) {
        int w = Math.min(552, screenWidth - 20);
        int h = Math.min(304, screenHeight - 20);
        int side = w < 380 ? 100 : 120;
        int th = Math.min(24, Math.max(14, (h - 76 - 16) / 5));
        return new ClickMenuLayout((screenWidth - w) / 2, (screenHeight - h) / 2, w, h, side, th);
    }
    public int tabsY() { return y + 32; }
    public int tabY(int index) { return tabsY() + index * (tabHeight + 4); }
    public int footerY() { return y + height - 40; }
    public int contentX() { return x + sidebar + 16; }
    public int contentWidth() { return width - sidebar - 32; }
}
