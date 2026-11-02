package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.OrbitMenu;
import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public abstract class MenuButtonMixin {
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void orbit$glassButton(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AbstractButton self = (AbstractButton) (Object) this;
        if (OrbitMenu.drawButton(graphics, self)) {
            ci.cancel();
        } else if (GlassMenu.active() && GlassMenu.supports(self)) {
            GlassMenu.button(graphics, self, false);
            ci.cancel();
        }
    }
}
