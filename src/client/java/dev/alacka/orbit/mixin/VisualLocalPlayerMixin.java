package dev.alacka.orbit.mixin;

import dev.alacka.orbit.modules.VisualModules;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses vanilla's own critical-hit signal, not a guessed fall-distance condition. */
@Mixin(LocalPlayer.class)
public abstract class VisualLocalPlayerMixin {
    @Inject(method="crit", at=@At("TAIL"))
    private void orbit$critical(Entity target, CallbackInfo ci) { VisualModules.critical(target); }
}