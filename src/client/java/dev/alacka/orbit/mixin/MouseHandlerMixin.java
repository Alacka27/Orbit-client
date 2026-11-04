package dev.alacka.orbit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.alacka.orbit.freelook.FreeLookController;
import dev.alacka.orbit.input.ModuleBinds;
import dev.alacka.orbit.zoom.ZoomController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Do not update the player's yaw/pitch while the FreeLook key is held. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"))
    private void orbit$moduleButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (window == minecraft.getWindow().handle()) ModuleBinds.mouse(minecraft, action, info.button());
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void orbit$zoomScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window == minecraft.getWindow().handle() && ZoomController.scroll(vertical)) ci.cancel();
    }

    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void orbit$mouseTurn(LocalPlayer player, double horizontal, double vertical,
                                 Operation<Void> original) {
        double scale = ZoomController.sensitivity();
        horizontal *= scale; vertical *= scale;
        if (!FreeLookController.turnCamera(player, horizontal, vertical)) {
            original.call(player, horizontal, vertical);
        }
    }
}
