package dev.alacka.orbit.menu;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.config.HudPrefs;
import dev.alacka.orbit.config.OrbitSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public final class WindowDrag {
   public static final int STRIP = 28;
   private final String id;
   private int dx;
   private int dy;
   private int startDx;
   private int startDy;
   private double startX;
   private double startY;
   private boolean dragging;

   public WindowDrag(String id) {
      this.id = id;
      HudPrefs.Offset saved = OrbitSettings.hud().window(id);
      this.dx = saved.x();
      this.dy = saved.y();
   }

   public void reload() {
      HudPrefs.Offset saved = OrbitSettings.hud().window(this.id);
      this.dx = saved.x();
      this.dy = saved.y();
      this.dragging = false;
   }

   public int dx() {
      return this.dx;
   }

   public int dy() {
      return this.dy;
   }

   public boolean dragging() {
      return this.dragging;
   }

   public static boolean inStrip(double mx, double my, int x, int y, int width) {
      return mx >= x && mx < x + width && my >= y && my < y + 28;
   }

   public boolean press(Screen screen, MouseButtonEvent e, int x, int y, int width) {
      if (e.button() == 0 && inStrip(e.x(), e.y(), x, y, width) && !screen.getChildAt(e.x(), e.y()).isPresent()) {
         this.dragging = true;
         this.startX = e.x();
         this.startY = e.y();
         this.startDx = this.dx;
         this.startDy = this.dy;
         return true;
      } else {
         return false;
      }
   }

   public int targetX(double mouseX) {
      return this.startDx + (int)Math.round(mouseX - this.startX);
   }

   public int targetY(double mouseY) {
      return this.startDy + (int)Math.round(mouseY - this.startY);
   }

   public void applied(int x, int y) {
      this.dx = x;
      this.dy = y;
   }

   public void release() {
      if (this.dragging) {
         this.dragging = false;

         try {
            OrbitSettings.saveWindow(this.id, new HudPrefs.Offset(this.dx, this.dy));
         } catch (Exception var2) {
            OrbitClient.LOGGER.error("[Orbit] Window position not saved", var2);
         }
      }
   }

   public static void shift(Screen screen, int ddx, int ddy) {
      if (ddx != 0 || ddy != 0) {
         for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget) {
               widget.setPosition(widget.getX() + ddx, widget.getY() + ddy);
            }
         }
      }
   }

   public void render(Screen screen, GuiGraphics g, int mouseX, int mouseY, int x, int y, int width) {
      boolean hover = this.dragging || inStrip(mouseX, mouseY, x, y, width) && screen.getChildAt(mouseX, mouseY).isEmpty();
      GlassMenu.grip(g, x + width / 2, y + 5, hover);
      if (hover) {
         g.requestCursor(CursorTypes.RESIZE_ALL);
      }
   }
}
