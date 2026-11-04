package dev.alacka.orbit.hud;

public final class HudLayout {
   private HudLayout() {
   }

   public static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(Math.max(min, max), value));
   }

   public static HudLayout.Rect resolve(HudLayout.Placement p, int screenWidth, int screenHeight, int width, int height) {
      int x = switch (p.anchor().column) {
         case 0 -> p.dx();
         case 1 -> (screenWidth - width) / 2 + p.dx();
         default -> screenWidth - width - p.dx();
      };

      int y = switch (p.anchor().row) {
         case 0 -> p.dy();
         case 1 -> (screenHeight - height) / 2 + p.dy();
         default -> screenHeight - height - p.dy();
      };
      return new HudLayout.Rect(clamp(x, 0, screenWidth - width), clamp(y, 0, screenHeight - height), width, height);
   }

   public static HudLayout.Placement capture(int x, int y, int width, int height, int screenWidth, int screenHeight) {
      x = clamp(x, 0, screenWidth - width);
      y = clamp(y, 0, screenHeight - height);
      int column = third(x + width / 2.0, screenWidth);
      int row = third(y + height / 2.0, screenHeight);

      int dx = switch (column) {
         case 0 -> x;
         case 1 -> x - (screenWidth - width) / 2;
         default -> screenWidth - width - x;
      };

      int dy = switch (row) {
         case 0 -> y;
         case 1 -> y - (screenHeight - height) / 2;
         default -> screenHeight - height - y;
      };
      return new HudLayout.Placement(HudLayout.Anchor.of(column, row), dx, dy);
   }

   private static int third(double centre, int size) {
      if (centre < size / 3.0) {
         return 0;
      } else {
         return centre > size * 2 / 3.0 ? 2 : 1;
      }
   }

   public static HudLayout.Snap snap(int x, int y, int width, int height, int screenWidth, int screenHeight, int margin, int threshold) {
      x = inside(x, width, screenWidth, margin);
      y = inside(y, height, screenHeight, margin);
      int guideX = -1;
      int guideY = -1;
      int bestX = threshold + 1;
      int bestY = threshold + 1;
      int sx = x;
      int sy = y;
      int[][] xs = new int[][]{{margin, margin}, {(screenWidth - width) / 2, screenWidth / 2}, {screenWidth - width - margin, screenWidth - margin}};
      int[][] ys = new int[][]{{margin, margin}, {(screenHeight - height) / 2, screenHeight / 2}, {screenHeight - height - margin, screenHeight - margin}};

      for (int[] c : xs) {
         int d = Math.abs(x - c[0]);
         if (d < bestX) {
            bestX = d;
            sx = c[0];
            guideX = c[1];
         }
      }

      for (int[] cx : ys) {
         int d = Math.abs(y - cx[0]);
         if (d < bestY) {
            bestY = d;
            sy = cx[0];
            guideY = cx[1];
         }
      }

      if (bestX > threshold) {
         sx = x;
         guideX = -1;
      }

      if (bestY > threshold) {
         sy = y;
         guideY = -1;
      }

      return new HudLayout.Snap(clamp(sx, 0, screenWidth - width), clamp(sy, 0, screenHeight - height), guideX, guideY);
   }

   private static int inside(int value, int size, int screen, int margin) {
      int min = margin;
      int max = screen - size - margin;
      if (max < margin) {
         min = 0;
         max = Math.max(0, screen - size);
      }

      return clamp(value, min, max);
   }

   public static HudLayout.Rect avoid(HudLayout.Rect element, HudLayout.Rect obstacle, int gap, int screenHeight) {
      return obstacle != null && element.intersects(obstacle) ? element.withY(clamp(obstacle.bottom() + gap, 0, screenHeight - element.height())) : element;
   }

   public static HudLayout.Rect effects(int screenWidth, int beneficial, int harmful, boolean demo) {
      int columns = Math.max(beneficial, harmful);
      if (columns == 0) {
         return null;
      } else {
         int top = demo ? 16 : 1;
         int bottom = harmful > 0 ? top + 26 + 24 : top + 24;
         int left = screenWidth - 25 * columns;
         return new HudLayout.Rect(left, top, screenWidth - left, bottom - top);
      }
   }

   public static enum Anchor {
      TOP_LEFT(0, 0),
      TOP(1, 0),
      TOP_RIGHT(2, 0),
      LEFT(0, 1),
      CENTER(1, 1),
      RIGHT(2, 1),
      BOTTOM_LEFT(0, 2),
      BOTTOM(1, 2),
      BOTTOM_RIGHT(2, 2);

      public final int column;
      public final int row;

      private Anchor(int column, int row) {
         this.column = column;
         this.row = row;
      }

      public static HudLayout.Anchor of(int column, int row) {
         for (HudLayout.Anchor anchor : values()) {
            if (anchor.column == column && anchor.row == row) {
               return anchor;
            }
         }

         throw new IllegalArgumentException("anchor");
      }
   }

   public record Placement(HudLayout.Anchor anchor, int dx, int dy) {
      public static final int LIMIT = 16384;

      public Placement(HudLayout.Anchor anchor, int dx, int dy) {
         if (anchor != null && Math.abs(dx) <= 16384 && Math.abs(dy) <= 16384) {
            this.anchor = anchor;
            this.dx = dx;
            this.dy = dy;
         } else {
            throw new IllegalArgumentException("placement");
         }
      }

      public String encode() {
         return this.anchor.name() + "," + this.dx + "," + this.dy;
      }

      public static HudLayout.Placement decode(String text) {
         if (text == null) {
            return null;
         } else {
            String[] parts = text.strip().split(",");
            if (parts.length != 3) {
               return null;
            } else {
               try {
                  return new HudLayout.Placement(
                     HudLayout.Anchor.valueOf(parts[0].strip()), Integer.parseInt(parts[1].strip()), Integer.parseInt(parts[2].strip())
                  );
               } catch (IllegalArgumentException var3) {
                  return null;
               }
            }
         }
      }
   }

   public record Rect(int x, int y, int width, int height) {
      public int right() {
         return this.x + this.width;
      }

      public int bottom() {
         return this.y + this.height;
      }

      public boolean contains(double px, double py) {
         return px >= this.x && px < this.x + this.width && py >= this.y && py < this.y + this.height;
      }

      public boolean intersects(HudLayout.Rect o) {
         return this.x < o.right() && o.x < this.right() && this.y < o.bottom() && o.y < this.bottom();
      }

      public HudLayout.Rect withY(int value) {
         return new HudLayout.Rect(this.x, value, this.width, this.height);
      }
   }

   public record Snap(int x, int y, int guideX, int guideY) {
   }
}
