package dev.alacka.orbit.menu;

import net.minecraft.client.gui.GuiGraphics;

public final class ChatGlass {
   private static final int MAX = 128;
   private static final int[] RECTS = new int[640];
   private static int count;
   private static GuiGraphics owner;

   private ChatGlass() {
   }

   public static void add(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
      if (owner != g) {
         count = 0;
         owner = g;
      }

      if (count == 128) {
         flush(g);
      }

      int i = count++ * 5;
      RECTS[i] = x1;
      RECTS[i + 1] = y1;
      RECTS[i + 2] = x2;
      RECTS[i + 3] = y2;
      RECTS[i + 4] = color;
   }

   public static void flushPending() {
      if (owner != null && count > 0) {
         flush(owner);
      }
   }

   public static void flush(GuiGraphics g) {
      if (owner != g) {
         count = 0;
         owner = g;
      } else {
         int i = 0;

         while (i < count) {
            int x1 = RECTS[i * 5];
            int top = RECTS[i * 5 + 1];
            int x2 = RECTS[i * 5 + 2];
            int bottom = RECTS[i * 5 + 3];
            int color = RECTS[i * 5 + 4];

            int j;
            for (j = i + 1; j < count && RECTS[j * 5] == x1 && RECTS[j * 5 + 2] == x2 && RECTS[j * 5 + 4] == color && RECTS[j * 5 + 1] == bottom; j++) {
               bottom = RECTS[j * 5 + 3];
            }

            draw(g, x1, top, x2, bottom, color);
            i = j;
         }

         count = 0;
      }
   }

   private static void draw(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
      int alpha = color >>> 24;
      if (alpha != 0 && y2 > y1) {
         int border = Math.round((HudTheme.transparentChat() ? 92.0F : 20.0F) * Math.min(1.0F, alpha / 128.0F));
         GlassMenu.hudPanel(g, x1, y1, x2 - x1, y2 - y1, 3, HudTheme.transparentChat() ? 0 : alpha << 24 | 2562614, border << 24 | 6178676);
      }
   }
}
