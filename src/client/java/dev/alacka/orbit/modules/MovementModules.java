package dev.alacka.orbit.modules;

import dev.alacka.orbit.config.OrbitSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;

/** Classic client-side behaviours, not an anti-cheat bypass or a server-side guarantee. */
public final class MovementModules {
    private MovementModules() {}
    public static boolean noFall() {
        Minecraft mc=Minecraft.getInstance(); var p=mc==null?null:mc.player;
        return OrbitSettings.get().noFall() && p!=null && p.isAlive() && !p.isSpectator()
                && !p.getAbilities().flying && !p.isFallFlying() && !p.isPassenger()
                && !p.onClimbable() && !p.isInWater() && !p.isInLava()
                && p.fallDistance>2.5 && p.getDeltaMovement().y<0;
    }
    /** Only the client player's water surface collision changes; other entities and lava do not. */
    public static double waterSurface(BlockGetter level,BlockPos pos,FluidState fluid,CollisionContext context) {
        Minecraft mc=Minecraft.getInstance();
        if (mc==null || !mc.isSameThread() || !OrbitSettings.get().jesus() || mc.player==null
                || !(context instanceof EntityCollisionContext ec) || ec.getEntity()!=mc.player) return -1;
        var p=mc==null?null:mc.player;
        if (!p.isAlive() || p.isSpectator() || p.getAbilities().flying || p.isPassenger()
                || p.isShiftKeyDown() || p.isUnderWater() || !fluid.is(FluidTags.WATER)
                || level.getFluidState(pos.above()).is(FluidTags.WATER)) return -1;
        double height=fluid.getHeight(level,pos);
        return p.getBoundingBox().minY>=pos.getY()+height-.04 ? height : -1;
    }
}
