package dev.alacka.orbit.mixin;
import dev.alacka.orbit.menu.GameUiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerSkinMixin {
    @Inject(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"))
    private void orbit$glass(GuiGraphics g, int mx, int my, float delta, CallbackInfo ci) {
        GameUiTheme.background(g, (AbstractContainerScreen<?>) (Object) this);
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void orbit$close(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> ci) {
        var self = (AbstractContainerScreen<?>) (Object) this;
        if (GameUiTheme.active() && !(self instanceof CreativeModeInventoryScreen) && event.button() == 0
                && GameUiTheme.layout(self).closes(event.x(), event.y())) {
            self.onClose(); ci.setReturnValue(true);
        }
    }
}
