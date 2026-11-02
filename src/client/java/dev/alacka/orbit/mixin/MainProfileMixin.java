package dev.alacka.orbit.mixin;

import dev.alacka.orbit.profiles.ProfileLaunch;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Select profile before Minecraft initializes identity-dependent services. */
@Mixin(Main.class)
public abstract class MainProfileMixin {
    @ModifyArg(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;<init>(Lnet/minecraft/client/main/GameConfig;)V"), index = 0)
    private static GameConfig orbit$selectProfile(GameConfig config) {
        return ProfileLaunch.apply(config);
    }
}
