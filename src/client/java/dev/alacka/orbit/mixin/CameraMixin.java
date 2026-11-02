package dev.alacka.orbit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.alacka.orbit.freelook.FreeLookController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Change view angles before vanilla performs third-person positioning and collision. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "setup", at = @At("HEAD"))
    private void orbit$prepareCamera(CallbackInfo ci) {
        FreeLookController.synchronize();
    }

    @WrapOperation(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float orbit$viewYaw(Entity entity, float partialTick, Operation<Float> original) {
        return FreeLookController.cameraYaw(entity, original.call(entity, partialTick));
    }

    @WrapOperation(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float orbit$viewPitch(Entity entity, float partialTick, Operation<Float> original) {
        return FreeLookController.cameraPitch(entity, original.call(entity, partialTick));
    }
}
