package dev.alacka.orbit.mixin;

import dev.alacka.orbit.modules.VisualModules;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla translucent teammate-style body. Keeps depth testing and the actual invisibility flag. */
@Mixin(LivingEntityRenderer.class)
public abstract class SeeInvisiblesMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at=@At("TAIL"))
    private void orbit$ghost(LivingEntity entity, LivingEntityRenderState state, float partial, CallbackInfo ci) {
        if (VisualModules.reveal(entity)) state.isInvisibleToPlayer = false;
    }
}