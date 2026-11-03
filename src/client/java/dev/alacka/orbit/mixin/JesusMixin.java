package dev.alacka.orbit.mixin;

import dev.alacka.orbit.modules.MovementModules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlock.class)
public abstract class JesusMixin {
    @Inject(method="getCollisionShape",at=@At("HEAD"),cancellable=true)
    private void orbit$waterSurface(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx,CallbackInfoReturnable<VoxelShape> cir) {
        double h=MovementModules.waterSurface(level,pos,state.getFluidState(),ctx);
        if (h>0) cir.setReturnValue(Shapes.box(0,0,0,1,h,1));
    }
}
