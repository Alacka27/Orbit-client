package dev.alacka.orbit.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({Checkbox.class})
public abstract class MenuCheckboxMixin {
   @Redirect(
      method = {"renderContents"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"
      )}
   )
   private void orbit$checkbox(GuiGraphics g, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color) {
      if (!GlassMenu.active()) {
         g.blitSprite(pipeline, sprite, x, y, width, height, color);
      } else {
         Checkbox self = (Checkbox)(Object)this;
         GlassMenu.panel(g, x, y, width, height, 5, self.selected() ? -243315526 : -802808781, self.isHoveredOrFocused() ? -2771457 : -1988271462);
         if (self.selected()) {
            float fx = x + width / 2.0F;
            float fy = y + height / 2.0F;
            GlassMenu.line(g, fx - 3.6F, fy + 0.2F, fx - 1.1F, fy + 2.7F, 1.9F, -396801);
            GlassMenu.line(g, fx - 1.1F, fy + 2.7F, fx + 3.8F, fy - 2.6F, 1.9F, -396801);
         }
      }
   }
}
