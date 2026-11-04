package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.HudTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerTabOverlay.class})
public abstract class TabGlassMixin {
   @Unique
   private boolean orbit$glass;
   @Unique
   private boolean orbit$positioned;

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void orbit$begin(GuiGraphics g, int width, Scoreboard scores, Objective objective, CallbackInfo ci) {
      this.orbit$glass = HudTheme.enabled();
      this.orbit$positioned = false;
      if (this.orbit$glass) {
         g.pose().pushMatrix();
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void orbit$end(GuiGraphics g, int width, Scoreboard scores, Objective objective, CallbackInfo ci) {
      if (this.orbit$glass) {
         g.pose().popMatrix();
      }
   }

   @Redirect(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"
      )}
   )
   private void orbit$rows(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
      if (!HudTheme.enabled()) {
         g.fill(x1, y1, x2, y2, color);
      } else {
         if (!this.orbit$positioned) {
            g.pose().translate(Math.max(0, g.guiWidth() - 8 - x2), 22.0F);
            this.orbit$positioned = true;
         }

         boolean outer = color == Integer.MIN_VALUE;
         GlassMenu.hudPanel(g, x1, y1, x2 - x1, y2 - y1, outer ? 4 : 2, outer ? -802546374 : 2018195037, outer ? 1755150778 : 513506228);
      }
   }
}
