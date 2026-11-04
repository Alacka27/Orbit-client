package dev.alacka.orbit.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({TabNavigationBar.class, CreateWorldScreen.class, StatsScreen.class})
public abstract class MenuSeparatorMixin {
   @Redirect(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"
      )}
   )
   private void orbit$separator(
      GuiGraphics g, RenderPipeline pipeline, Identifier sprite, int x, int y, float u, float v, int width, int height, int tw, int th
   ) {
      if (!GlassMenu.active()) {
         g.blit(pipeline, sprite, x, y, u, v, width, height, tw, th);
      }
   }
}
