package dev.alacka.orbit.menu;

public record ClickMenuLayout(int x, int y, int width, int height, int sidebar, int tabHeight) {
   public static ClickMenuLayout of(int screenWidth, int screenHeight) {
      return of(screenWidth, screenHeight, 0, 0);
   }

   public static ClickMenuLayout of(int screenWidth, int screenHeight, int dx, int dy) {
      int w = Math.min(620, screenWidth - 20);
      int h = Math.min(250, screenHeight - 20);
      int side = w < 380 ? 80 : 92;
      int th = Math.min(24, Math.max(14, (h - 76 - 16) / 5));
      return new ClickMenuLayout(place(screenWidth, w, dx), place(screenHeight, h, dy), w, h, side, th);
   }

   static int place(int screen, int size, int offset) {
      return Math.max(0, Math.min(Math.max(0, screen - size), (screen - size) / 2 + offset));
   }

   public int tabsY() {
      return this.y + 34;
   }

   public int tabY(int index) {
      return this.tabsY() + index * (this.tabHeight + 4);
   }

   public int footerY() {
      return this.y + this.height - 28;
   }

   public int contentX() {
      return this.x + this.sidebar + 12;
   }

   public int contentWidth() {
      return this.width - this.sidebar - 24;
   }
}
