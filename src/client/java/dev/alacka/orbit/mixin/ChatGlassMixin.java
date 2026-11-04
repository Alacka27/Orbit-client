package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.ChatGlass;
import dev.alacka.orbit.menu.HudTheme;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"net/minecraft/client/gui/components/ChatComponent$DrawingBackgroundGraphicsAccess", "net/minecraft/client/gui/components/ChatComponent$DrawingFocusedGraphicsAccess"}
)
public abstract class ChatGlassMixin {
   @Redirect(
      method = {"fill"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"
      )}
   )
   private void orbit$line(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
      if (HudTheme.enabled() && x2 - x1 >= 12 && y2 > y1 && (color & 16777215) == 0) {
         ChatGlass.add(graphics, x1, y1, x2, y2, color);
      } else {
         graphics.fill(x1, y1, x2, y2, color);
      }
   }

   @Inject(
      method = {"handleMessage"},
      at = {@At("HEAD")}
   )
   private void orbit$beforeText(int y, float opacity, FormattedCharSequence text, CallbackInfoReturnable<Boolean> ci) {
      ChatGlass.flushPending();
   }

   @Inject(
      method = {"handleTag"},
      at = {@At("HEAD")}
   )
   private void orbit$beforeTag(int x1, int y1, int x2, int y2, float opacity, GuiMessageTag tag, CallbackInfo ci) {
      ChatGlass.flushPending();
   }
}
