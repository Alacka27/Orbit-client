package dev.alacka.orbit.mixin;

import com.mojang.realmsclient.client.RealmsClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RealmsClient.class)
public interface RealmsClientAccessor {
    @Invoker("<init>")
    static RealmsClient orbit$create(String sessionId, String username, Minecraft minecraft) {
        throw new AssertionError("Mixin constructor bridge");
    }
}