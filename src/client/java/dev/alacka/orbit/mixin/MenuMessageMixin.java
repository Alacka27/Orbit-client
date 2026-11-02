package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.FocusableTextWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FocusableTextWidget.class)
public abstract class MenuMessageMixin {
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void orbit$messageBackground(GuiGraphics g, int x, int y, int right, int bottom, int color) {
        if (!GlassMenu.active()) { g.fill(x, y, right, bottom, color); return; }
        GlassMenu.panel(g, x, y, right - x, bottom - y, 8, 0xd0291e37, 0x00000000);
    }
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderOutline(IIIII)V"))
    private void orbit$messageBorder(GuiGraphics g, int x, int y, int width, int height, int color) {
        if (!GlassMenu.active()) { g.renderOutline(x, y, width, height, color); return; }
        GlassMenu.panel(g, x, y, width, height, 8, 0x00000000, 0x9fc69aef);
    }
}
