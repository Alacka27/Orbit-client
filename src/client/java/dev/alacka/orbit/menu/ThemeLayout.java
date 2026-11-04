package dev.alacka.orbit.menu;

public record ThemeLayout(int x, int y, int width, int height, boolean compact) {
   public static ThemeLayout of(int width, int height) {
      return of(width, height, 0, 0);
   }

   public static ThemeLayout of(int width, int height, int dx, int dy) {
      int w = Math.min(552, width - 24);
      int h = Math.min(306, height - 24);
      return new ThemeLayout(ClickMenuLayout.place(width, w, dx), ClickMenuLayout.place(height, h, dy), w, h, h < 300);
   }

   public boolean preview() {
      return this.width >= 480 && this.height >= 200;
   }

   public int controlWidth() {
      return this.preview() ? (this.width - 36) * 56 / 100 : this.width - 24;
   }

   public int previewX() {
      return this.x + this.controlWidth() + 24;
   }

   public int previewWidth() {
      return this.width - this.controlWidth() - 36;
   }

   public boolean tiny() {
      return this.height < 200;
   }

   public int hexY() {
      return this.y + (this.tiny() ? 28 : 43);
   }

   public int opacityY() {
      return this.y + (this.tiny() ? 52 : (this.compact ? 75 : 157));
   }

   public int controlHeight() {
      return this.tiny() ? 18 : 22;
   }

   public int step() {
      return this.tiny() ? 22 : 26;
   }

   public int footerY() {
      return this.y + this.height - (this.tiny() ? 28 : 32);
   }

   public int noticeY() {
      return this.footerY() - (this.tiny() ? 12 : 17);
   }
}
