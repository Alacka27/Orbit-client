package dev.alacka.orbit.mixin;
import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(TooltipRenderUtil.class)
public abstract class TooltipSkinMixin {
    @Inject(method = "renderTooltipBackground", at = @At("HEAD"), cancellable = true)
    private static void orbit$tooltip(GuiGraphics g, int x, int y, int w, int h, Identifier style, CallbackInfo ci) {
        // Respect tooltip styles supplied by items/resource packs.
        if (!GlassMenu.active() || style != null) return;
        GlassMenu.shadow(g, x - 5, y - 5, w + 10, h + 10);
        GlassMenu.panel(g, x - 5, y - 5, w + 10, h + 10, 5, 0xee22162f, 0xd5b887e1);
        ci.cancel();
    }
}
