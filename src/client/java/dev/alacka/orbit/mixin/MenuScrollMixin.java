package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractScrollArea.class)
public abstract class MenuScrollMixin {
    @Redirect(method = "renderScrollbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void orbit$scroll(GuiGraphics g, RenderPipeline pipeline, Identifier sprite,
                               int x, int y, int width, int height) {
        if (!GlassMenu.active()) { g.blitSprite(pipeline, sprite, x, y, width, height); return; }
        boolean track = sprite.getPath().contains("background");
        GlassMenu.panel(g, x, y, width, height, 3, track ? 0x80241a31 : 0xe2a175d3,
                track ? 0x202b1c3a : 0x90d1b2f2);
    }
}
