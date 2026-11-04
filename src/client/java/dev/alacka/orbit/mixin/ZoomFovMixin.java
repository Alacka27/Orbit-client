package dev.alacka.orbit.mixin;

import dev.alacka.orbit.zoom.ZoomController;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class ZoomFovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void orbit$zoom(Camera camera, float delta, boolean worldFov, CallbackInfoReturnable<Float> ci) {
        if (worldFov) ci.setReturnValue(ZoomController.fov(ci.getReturnValue()));
    }
}