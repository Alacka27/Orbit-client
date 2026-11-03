package dev.alacka.orbit.mixin;

import dev.alacka.orbit.modules.MovementModules;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Change only the ground flag assembled by the local movement sender. */
@Mixin(LocalPlayer.class)
public abstract class NoFallMixin {
    @Redirect(method="sendPosition",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean orbit$fallGround(LocalPlayer player) { return player.onGround() || MovementModules.noFall(); }
}
