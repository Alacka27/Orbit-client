package dev.alacka.orbit.menu;

/** Responsive, continuous grid; content is never split into pages. */
public record ModuleGrid(int columns, int rows, int cardWidth, int cardHeight, int contentHeight, int viewportHeight) {
    public static final int GAP = 6;
    public static ModuleGrid of(int width, int height, int items) {
        if (width < 1 || height < 1 || items < 0) throw new IllegalArgumentException("grid");
        int columns = Math.max(1, Math.min(3, (width + GAP) / (124 + GAP)));
        int rows = (items + columns - 1) / columns;
        int card = 32;
        return new ModuleGrid(columns, rows, (width - (columns - 1) * GAP) / columns, card,
                rows == 0 ? 0 : rows * card + (rows - 1) * GAP, height);
    }
    public int x(int index) { return index % columns * (cardWidth + GAP); }
    public int y(int index) { return index / columns * (cardHeight + GAP); }
    public int maxScroll() { return Math.max(0, contentHeight - viewportHeight); }
    public double clampScroll(double value) { return Math.max(0, Math.min(maxScroll(), value)); }
}
