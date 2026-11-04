package dev.alacka.orbit.mixin;

import dev.alacka.orbit.modules.VisualModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe received events after vanilla processing; never cancel or rewrite them. */
@Mixin(ClientPacketListener.class)
public abstract class VisualEventsMixin {
    @Inject(method="handleDamageEvent", at=@At("TAIL"))
    private void orbit$damage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        var level = Minecraft.getInstance().level;
        if (level != null) VisualModules.damage(level.getEntity(packet.entityId()));
    }
    @Inject(method="handleAnimate", at=@At("TAIL"))
    private void orbit$critical(ClientboundAnimatePacket packet, CallbackInfo ci) {
        var level = Minecraft.getInstance().level;
        if (level != null && packet.getAction() == ClientboundAnimatePacket.CRITICAL_HIT)
            VisualModules.critical(level.getEntity(packet.getId()));
    }
    @Inject(method="handleEntityEvent", at=@At("TAIL"))
    private void orbit$entityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        var level = Minecraft.getInstance().level;
        if (level != null) VisualModules.entityEvent(packet.getEntity(level), packet.getEventId());
    }
}