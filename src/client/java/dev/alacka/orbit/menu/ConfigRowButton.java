package dev.alacka.orbit.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;

public final class ConfigRowButton extends Button {
   private final String name;
   private final String detail;
   private final String status;
   private final boolean selected;
   private final boolean valid;

   public ConfigRowButton(int x, int y, int w, int h, String name, String detail, String status, boolean selected, boolean valid, OnPress action) {
      super(x, y, w, h, Component.literal(name + ". " + detail + (status.isEmpty() ? "" : ". " + status)), action, DEFAULT_NARRATION);
      this.name = name;
      this.detail = detail;
      this.status = status;
      this.selected = selected;
      this.valid = valid;
   }

   @Override
   protected void renderContents(GuiGraphics g, int mx, int my, float delta) {
      boolean hover = this.isHoveredOrFocused();
      GlassMenu.panel(
         g,
         this.getX(),
         this.getY(),
         this.getWidth(),
         this.getHeight(),
         6,
         this.selected ? -1687993727 : (hover ? -2007942041 : 1749628510),
         this.selected ? -1447194398 : (hover ? 1890158540 : 1083734960)
      );
      Font font = Minecraft.getInstance().font;
      int statusWidth = this.status.isEmpty() ? 0 : font.width(this.status) + 14;
      int textWidth = Math.max(30, this.getWidth() - 20 - statusWidth);
      boolean twoLines = this.getHeight() >= 28;
      int titleY = twoLines ? this.getY() + 6 : this.getY() + (this.getHeight() - 9) / 2;
      GlassMenu.heading(g, font.plainSubstrByWidth(this.name, textWidth), this.getX() + 10, titleY, this.valid ? -199425 : -1586737);
      if (twoLines) {
         GlassMenu.text(g, font.plainSubstrByWidth(this.detail, textWidth), this.getX() + 10, this.getY() + 17, this.valid ? -2504728 : -19276);
      }

      if (!this.status.isEmpty()) {
         int sx = this.getX() + this.getWidth() - statusWidth - 4;
         int sy = this.getY() + (this.getHeight() - 14) / 2;
         GlassMenu.panel(g, sx, sy, statusWidth, 14, 7, -2142094226, 1889501136);
         GlassMenu.text(g, this.status, sx + 7, sy + 3, -792321);
      }
   }
}
