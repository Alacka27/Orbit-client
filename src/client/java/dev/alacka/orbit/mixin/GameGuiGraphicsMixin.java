package dev.alacka.orbit.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.alacka.orbit.menu.GameUiTheme;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.HudTheme;
import dev.alacka.orbit.menu.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiGraphics.class})
public abstract class GameGuiGraphicsMixin {
   @Inject(
      method = {"innerBlit"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void orbit$nativeBackground(
      RenderPipeline pipeline, Identifier texture, int x1, int x2, int y1, int y2, float u1, float u2, float v1, float v2, int color, CallbackInfo ci
   ) {
      if (GameUiTheme.hideTexture((GuiGraphics)(Object)this, texture, x1, y1, x2 - x1, y2 - y1)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void orbit$sprite(RenderPipeline pipeline, Identifier id, int x, int y, int w, int h, int color, CallbackInfo ci) {
      if (GameUiTheme.sprite((GuiGraphics)(Object)this, id, x, y, w, h, color)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIIII)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void orbit$experience(RenderPipeline pipeline, Identifier id, int tw, int th, int u, int v, int x, int y, int w, int h, int color, CallbackInfo ci) {
      if (GameUiTheme.sprite((GuiGraphics)(Object)this, id, x, y, w, h, color)) {
         ci.cancel();
      } else {
         if (HudTheme.enabled() && id.getNamespace().equals("minecraft") && id.getPath().equals("hud/experience_bar_progress")) {
            GlassMenu.pill((GuiGraphics)(Object)this, x, y, w, h, Math.min(w, h) / 2.0F, UiTheme.accent());
            ci.cancel();
         }
      }
   }

   @ModifyVariable(
      method = {"drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V"},
      at = {@At("HEAD")},
      argsOnly = true,
      ordinal = 2
   )
   private int orbit$labels(int color) {
      return GameUiTheme.labelColor(color);
   }

   @Inject(
      method = {"fill(IIIII)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void orbit$chatInput(int x1, int y1, int x2, int y2, int color, CallbackInfo ci) {
      Minecraft m = Minecraft.getInstance();
      GuiGraphics g = (GuiGraphics)(Object)this;
      if (HudTheme.enabled()
         && m.screen instanceof ChatScreen
         && x2 - x1 > g.guiWidth() / 2
         && y1 == g.guiHeight() - 14
         && y2 == g.guiHeight() - 2
         && y2 - y1 <= 18
         && (color & 16777215) == 0) {
         GlassMenu.panel(g, x1, y1, x2 - x1, y2 - y1, 4, HudTheme.transparentChat() ? 0 : -585034449, -1933734685);
         ci.cancel();
      }
   }
}
