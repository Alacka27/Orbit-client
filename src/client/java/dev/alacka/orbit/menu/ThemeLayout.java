package dev.alacka.orbit.menu;

/** Compact settings omit RGB sliders, while the HEX field always remains available. */
public record ThemeLayout(int x, int y, int width, int height, boolean compact) {
    public static ThemeLayout of(int width, int height) {
        int w = Math.min(420, width - 24), h = Math.min(306, height - 24);
        return new ThemeLayout((width-w)/2, (height-h)/2, w, h, h < 300);
    }
    public boolean tiny() { return height < 200; }
    public int hexY() { return y + (tiny() ? 28 : 43); }
    public int opacityY() { return y + (tiny() ? 52 : compact ? 75 : 157); }
    public int controlHeight() { return tiny() ? 18 : 22; }
    public int step() { return tiny() ? 22 : 26; }
    public int footerY() { return y + height - (tiny() ? 28 : 32); }
    public int noticeY() { return footerY() - (tiny() ? 12 : 17); }
}
