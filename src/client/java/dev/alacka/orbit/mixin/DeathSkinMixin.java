package dev.alacka.orbit.mixin;
import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(DeathScreen.class)
public abstract class DeathSkinMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void orbit$death(GuiGraphics g, int mx, int my, float delta, CallbackInfo ci) {
        if (!GlassMenu.active()) return;
        DeathScreen s = (DeathScreen) (Object) this;
        GlassMenu.backdrop(g, s.width, s.height);
        int w = Math.min(s.width - 20, 260), y = Math.min(36, Math.max(8, s.height / 8));
        int bottom = Math.min(s.height - 8, Math.max(124, s.height / 4 + 124));
        GlassMenu.shadow(g, (s.width - w) / 2, y, w, bottom - y);
        GlassMenu.panel(g, (s.width - w) / 2, y, w, bottom - y, 12, GlassMenu.WINDOW_FILL, GlassMenu.WINDOW_BORDER);
        ci.cancel();
    }
}
