package dev.alacka.orbit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.alacka.orbit.freelook.FreeLookController;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Do not update the player's yaw/pitch while the FreeLook key is held. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void orbit$mouseTurn(LocalPlayer player, double horizontal, double vertical,
                                 Operation<Void> original) {
        if (!FreeLookController.turnCamera(player, horizontal, vertical)) {
            original.call(player, horizontal, vertical);
        }
    }
}
