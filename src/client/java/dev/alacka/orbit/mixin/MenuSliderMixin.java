package dev.alacka.orbit.mixin;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractSliderButton.class})
public abstract class MenuSliderMixin {
   @Shadow
   protected double value;

   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void orbit$slider(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (GlassMenu.active()) {
         AbstractSliderButton self = (AbstractSliderButton)(Object)this;
         int x = self.getX();
         int y = self.getY();
         int width = self.getWidth();
         int height = self.getHeight();
         GlassMenu.panel(g, x, y, width, height, 8, -752345036, self.isHoveredOrFocused() ? -1027698196 : 1416320382);
         float end = x + 5 + (float)((width - 10) * this.value);
         if (end > x + 6) {
            GlassMenu.pill(g, x + 5, y + height - 4, end - x - 5.0F, 2.0F, 1.0F, UiTheme.tint(-1734380586, false));
         }

         float thumb = x + 1 + (float)((width - 8) * this.value);
         GlassMenu.pill(g, thumb, y + height - 4.5F, 6.0F, 3.0F, 1.5F, UiTheme.tint(-2112257, false));
         Font font = Minecraft.getInstance().font;
         String label = self.getMessage().getString();
         int available = Math.max(0, width - 12);
         if (font.width(label) > available) {
            while (!label.isEmpty() && font.width(label + "…") > available) {
               label = label.substring(0, label.length() - 1);
            }

            label = label + "…";
         }

         g.drawString(font, label, x + (width - font.width(label)) / 2, y + (height - 9) / 2, self.active ? -528385 : -6122063, false);
         if (self.isHovered()) {
            g.requestCursor(CursorTypes.RESIZE_EW);
         }

         ci.cancel();
      }
   }
}
