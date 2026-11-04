package dev.alacka.orbit.mixin;

import com.mojang.realmsclient.client.RealmsClient;
import dev.alacka.orbit.accounts.AccountSessions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RealmsClient.class)
public abstract class RealmsIdentityMixin {
    @Shadow private static volatile RealmsClient realmsClientInstance;
    @Unique private static User orbit$realmsUser;

    @Inject(method = "getOrCreate(Lnet/minecraft/client/Minecraft;)Lcom/mojang/realmsclient/client/RealmsClient;",
            at = @At("HEAD"), cancellable = true)
    private static void orbit$accountScopedClient(Minecraft minecraft, CallbackInfoReturnable<RealmsClient> ci) {
        User current = minecraft.getUser();
        if (orbit$realmsUser != current) {
            realmsClientInstance = null;
            orbit$realmsUser = current;
        }
        if (!AccountSessions.mojangServicesAllowed()) {
            // Never send an Ely.by access token to Microsoft's Realms endpoint.
            if (realmsClientInstance == null) realmsClientInstance = RealmsClientAccessor.orbit$create("", current.getName(), minecraft);
            ci.setReturnValue(realmsClientInstance);
        }
    }
}