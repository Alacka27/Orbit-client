package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.ClickMenuController;
import dev.alacka.orbit.input.ModuleBinds;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class ClickMenuKeyMixin {
    @Shadow @Final private Minecraft minecraft;
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void orbit$toggle(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (window != minecraft.getWindow().handle()) return;
        if (ClickMenuController.handle(minecraft, action, event)) ci.cancel();
        else ModuleBinds.key(minecraft, action, event);
    }
}
