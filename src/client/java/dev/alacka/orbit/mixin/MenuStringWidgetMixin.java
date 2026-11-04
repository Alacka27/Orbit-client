package dev.alacka.orbit.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({StringWidget.class})
public abstract class MenuStringWidgetMixin {
   @Unique
   private static final int ORBIT$DRIFT = 4;
   @Shadow
   private int maxWidth;
   @Unique
   private boolean orbit$sizedToText;

   @Inject(
      method = {"<init>(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/Font;)V"},
      at = {@At("TAIL")}
   )
   private void orbit$sized(Component message, Font font, CallbackInfo ci) {
      this.orbit$sizedToText = true;
   }

   @Inject(
      method = {"<init>(IILnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/Font;)V"},
      at = {@At("TAIL")}
   )
   private void orbit$sizedAt(int x, int y, Component message, Font font, CallbackInfo ci) {
      this.orbit$sizedToText = true;
   }

   @Redirect(
      method = {"visitLines"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/network/chat/FormattedText;)I"
      )}
   )
   private int orbit$textWidth(Font font, FormattedText text) {
      int width = font.width(text);
      if (this.orbit$sizedToText && this.maxWidth <= 0) {
         int box = ((StringWidget)(Object)this).getWidth();
         return width > box && width - box <= 4 ? box : width;
      } else {
         return width;
      }
   }
}
