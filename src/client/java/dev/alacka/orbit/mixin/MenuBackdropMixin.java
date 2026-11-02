package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MenuBackdropMixin {
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
    private void orbit$clouds(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!GlassMenu.active()) return;
        Screen self = (Screen) (Object) this;
        GlassMenu.backdrop(graphics, self.width, self.height);
        ci.cancel();
    }

    @Inject(method = "renderMenuBackgroundTexture", at = @At("HEAD"), cancellable = true)
    private static void orbit$glassList(GuiGraphics graphics, Identifier texture, int x, int y,
                                        float u, float v, int width, int height, CallbackInfo ci) {
        if (!GlassMenu.active()) return;
        GlassMenu.panel(graphics, x, y, width, height, 10, 0x60150b2c, 0x25ffffff);
        ci.cancel();
    }
}
