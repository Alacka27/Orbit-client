package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.OrbitMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scope is identity-based: all non-Orbit title widgets and all other screens stay vanilla. */
@Mixin(AbstractButton.class)
public abstract class MenuButtonMixin {
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void orbit$blueButton(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (OrbitMenu.drawButton(graphics, (AbstractButton) (Object) this)) ci.cancel();
    }
}
