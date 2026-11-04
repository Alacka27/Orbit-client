package dev.alacka.orbit.mixin;

import com.mojang.realmsclient.RealmsAvailability;
import dev.alacka.orbit.accounts.AccountSessions;
import java.util.concurrent.CompletableFuture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RealmsAvailability.class)
public abstract class RealmsAvailabilityMixin {
    @Shadow private static CompletableFuture<RealmsAvailability.Result> future;
    @Unique private static long orbit$sessionRevision = -1;

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void orbit$accountScopedAvailability(CallbackInfoReturnable<CompletableFuture<RealmsAvailability.Result>> ci) {
        long revision = AccountSessions.revision();
        if (orbit$sessionRevision != revision) {
            future = null;
            orbit$sessionRevision = revision;
        }
        if (!AccountSessions.mojangServicesAllowed()) {
            ci.setReturnValue(CompletableFuture.completedFuture(
                    new RealmsAvailability.Result(RealmsAvailability.Type.AUTHENTICATION_ERROR)));
        }
    }
}