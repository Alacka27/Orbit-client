package dev.alacka.orbit.menu;

import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;

public final class ColorSwatch extends Button {
   private final int color;
   private final IntSupplier selected;

   public ColorSwatch(int x, int y, int color, IntSupplier selected, OnPress action) {
      super(x, y, 17, 17, Component.literal(String.format("Цвет #%06X", color)), action, DEFAULT_NARRATION);
      this.color = color;
      this.selected = selected;
   }

   @Override
   protected void renderContents(GuiGraphics g, int mx, int my, float delta) {
      GlassMenu.panel(
         g, this.getX(), this.getY(), 17, 17, 5, 1964971052, this.selected.getAsInt() != this.color && !this.isHoveredOrFocused() ? 1617712766 : -2049793
      );
      GlassMenu.pill(g, this.getX() + 4, this.getY() + 4, 9.0F, 9.0F, 2.5F, 0xFF000000 | this.color);
   }
}
