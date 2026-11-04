package dev.alacka.orbit.menu;

public record ProfileListLayout(int x, int y, int width, int height, int rows, int rowY, int rowHeight, int step) {
   public static ProfileListLayout of(int sw, int sh) {
      return of(sw, sh, 0, 0);
   }

   public static ProfileListLayout of(int sw, int sh, int dx, int dy) {
      int w = Math.min(460, sw - 24);
      int h = Math.min(304, sh - 20);
      boolean tiny = h < 200;
      int ry = tiny ? 41 : 47;
      int rh = tiny ? 30 : 37;
      int step = tiny ? 36 : 42;
      int rows = Math.max(1, Math.min(3, (h - 87 - ry + step - rh - 3) / step));
      return new ProfileListLayout(ClickMenuLayout.place(sw, w, dx), ClickMenuLayout.place(sh, h, dy), w, h, rows, ry, rh, step);
   }

   public static ProfileListLayout configs(int sw, int sh, int dx, int dy) {
      int w = Math.min(460, sw - 24);
      int h = Math.min(304, sh - 20);
      boolean tiny = h < 200;
      int ry = tiny ? 37 : 45;
      int rh = tiny ? 24 : 30;
      int step = tiny ? 27 : 34;
      int rows = Math.max(1, (h - 87 - ry + step - rh - 3) / step);
      return new ProfileListLayout(ClickMenuLayout.place(sw, w, dx), ClickMenuLayout.place(sh, h, dy), w, h, rows, ry, rh, step);
   }

   public int editY() {
      return this.y + this.height - 87;
   }

   public int noticeY() {
      return this.y + this.height - 59;
   }

   public int footerY() {
      return this.y + this.height - 30;
   }
}
