package dev.alacka.orbit.menu;

/** Responsive settings pages, including GUI scale 4 at a 1280x800 window. */
public record ModuleSettingsLayout(int x, int y, int width, int height, int rowHeight,
                                   int step, int rows, int contentY, int helpY, int footerY) {
    public static ModuleSettingsLayout of(int screenWidth, int screenHeight) {
        return of(screenWidth, screenHeight, 0, 0);
    }

    public static ModuleSettingsLayout of(int screenWidth, int screenHeight, int dx, int dy) {
        int w = Math.max(1, Math.min(520, screenWidth - 16));
        int h = Math.max(1, Math.min(392, screenHeight - 16));
        int x = Math.clamp((screenWidth - w) / 2 + dx, 0, Math.max(0, screenWidth - w));
        int y = Math.clamp((screenHeight - h) / 2 + dy, 0, Math.max(0, screenHeight - h));
        boolean compact = h < 260;
        int row = compact ? 20 : 24, gap = compact ? 4 : 6;
        int content = y + (compact ? 40 : 56);
        int help = y + h - (compact ? 44 : 66);
        int count = Math.max(1, Math.min(8, (help - content - 5 + gap) / (row + gap)));
        return new ModuleSettingsLayout(x, y, w, h, row, row + gap, count, content, help, y + h - 26);
    }

    public int pages(int controls) { return Math.max(1, (controls + rows - 1) / rows); }
    public int controlY(int indexOnPage) { return contentY + indexOnPage * step; }
}