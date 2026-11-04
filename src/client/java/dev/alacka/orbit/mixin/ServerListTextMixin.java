package dev.alacka.orbit.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.render.SmoothShapes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   targets = {"net/minecraft/client/gui/screens/multiplayer/ServerSelectionList$OnlineServerEntry"}
)
public abstract class ServerListTextMixin {
   @ModifyConstant(
      method = {"renderContent"},
      constant = {@Constant(
         intValue = -8355712
      )}
   )
   private int orbit$readableGray(int original) {
      return GlassMenu.active() ? -1121032 : original;
   }

   @Redirect(
      method = {"drawIcon"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"
      )}
   )
   private void orbit$roundIcon(GuiGraphics g, RenderPipeline pipeline, Identifier id, int x, int y, float u, float v, int w, int h, int tw, int th) {
      if (GlassMenu.active() && u == 0.0F && v == 0.0F && w == tw && h == th) {
         SmoothShapes.image(g, id, x, y, w, h, 4.0F, tw, th, -1);
      } else {
         g.blit(pipeline, id, x, y, u, v, w, h, tw, th);
      }
   }

   @Redirect(
      method = {"renderContent"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"
      )}
   )
   private void orbit$roundShade(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
      if (!GlassMenu.active()) {
         g.fill(x0, y0, x1, y1, color);
      } else {
         SmoothShapes.fill(g, x0, y0, x1 - x0, y1 - y0, 4.0F, color);
      }
   }
}
