package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replace just the input background. Keep native text, caret, selection and IME. */
@Mixin(EditBox.class)
public abstract class MenuEditBoxMixin {
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void orbit$input(GuiGraphics g, com.mojang.blaze3d.pipeline.RenderPipeline pipeline,
                              Identifier sprite, int x, int y, int width, int height) {
        if (!GlassMenu.active()) { g.blitSprite(pipeline, sprite, x, y, width, height); return; }
        EditBox self = (EditBox) (Object) this;
        GlassMenu.panel(g, x, y, width, height, 8, 0xb31c1626,
                self.isFocused() ? 0xe3be94f0 : 0x506d587f);
    }
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private void orbit$readableHint(GuiGraphics g, Font font, Component hint, int x, int y, int color) {
        if (!GlassMenu.active()) { g.drawString(font, hint, x, y, color); return; }
        g.drawString(font, hint.copy().withStyle(style -> style.withColor(0xbfb0d1)), x, y, 0xffbfb0d1, false);
    }

}
