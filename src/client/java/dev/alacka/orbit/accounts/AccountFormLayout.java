package dev.alacka.orbit.accounts;

public record AccountFormLayout(int x, int y, int width, int height) {
    public static AccountFormLayout of(int sw, int sh, int preferredHeight, int dx, int dy) {
        int width = Math.min(440, sw - 24);
        int height = Math.min(preferredHeight, sh - 20);
        return new AccountFormLayout(place(sw, width, dx), place(sh, height, dy), width, height);
    }
    private static int place(int screen, int size, int offset) {
        return Math.max(0, Math.min(Math.max(0, screen - size), (screen - size) / 2 + offset));
    }
    public int inner() { return width - 24; }
    public int fieldY(int row) { return y + 42 + row * 28; }
    public int statusY() { return y + height - (height < 210 ? 56 : 66); }
    public int footerY() { return y + height - 30; }
}