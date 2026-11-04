package dev.alacka.orbit.mixin;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The vanilla pack downloader captures the launch User; refresh its public HTTP headers. */
@Mixin(targets = "net.minecraft.client.resources.server.DownloadedPackSource$4")
public abstract class ResourcePackIdentityMixin {
    @Redirect(method = "createDownloadHeaders", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/User;getName()Ljava/lang/String;"))
    private String orbit$currentPackName(User launchUser) { return Minecraft.getInstance().getUser().getName(); }

    @Redirect(method = "createDownloadHeaders", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/User;getProfileId()Ljava/util/UUID;"))
    private UUID orbit$currentPackId(User launchUser) { return Minecraft.getInstance().getUser().getProfileId(); }
}