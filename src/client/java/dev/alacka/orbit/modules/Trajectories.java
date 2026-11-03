package dev.alacka.orbit.modules;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.menu.UiTheme;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Extract immutable geometry; drawing never reads the live level or player. */
public final class Trajectories {
    private record Path(List<Vec3> points,Vec3 impact,int color) {}
    private static volatile Path path=new Path(List.of(),null,0xffffffff);
    private Trajectories() {}
    public static void initialize() {
        WorldRenderEvents.END_EXTRACTION.register(Trajectories::extract);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(Trajectories::draw);
    }
    private static void extract(WorldExtractionContext context) {
        Minecraft mc=Minecraft.getInstance(); var player=mc.player;
        path=new Path(List.of(),null,0xffffffff);
        if (!OrbitSettings.get().trajectories() || player==null || !player.isAlive() || mc.options.hideGui) return;
        ItemStack stack=player.isUsingItem()?player.getUseItem():player.getMainHandItem();
        if (!supported(stack)) stack=player.getOffhandItem();
        if (!supported(stack)) return;
        boolean bow=stack.is(Items.BOW), potion=stack.getItem() instanceof ThrowablePotionItem;
        double speed=bow?3: potion?.5:1.5, gravity=bow||potion?.05:.03;
        if (bow && player.isUsingItem()) {
            speed=BowItem.getPowerForTime(player.getTicksUsingItem())*3;
            if (speed<.3) return;
        }
        float partial=context.tickCounter().getGameTimeDeltaPartialTick(false);
        Vec3 pos=player.getEyePosition(partial).add(0,-.1,0);
        double yaw=Math.toRadians(player.getYRot()), pitch=Math.toRadians(player.getXRot());
        Vec3 velocity=new Vec3(-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch+Math.toRadians(potion?-20:0)),Math.cos(yaw)*Math.cos(pitch)).normalize().scale(speed);
        Vec3 inherited=player.getKnownMovement();
        velocity=velocity.add(inherited.x,player.onGround()?0:inherited.y,inherited.z);
        List<Vec3> points=new ArrayList<>(); points.add(pos); Vec3 impact=null;
        for(int step=0;step<200;step++) {
            if (!context.world().hasChunkAt(BlockPos.containing(pos))) break;
            double drag=context.world().getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER)?(bow?.6:.8):.99;
            if (!bow) velocity=stepVelocity(velocity,drag,gravity,true);
            Vec3 end=pos.add(velocity);
            HitResult block=context.world().clip(new ClipContext(pos,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            if (block.getType()!=HitResult.Type.MISS) end=block.getLocation();
            var entity=ProjectileUtil.getEntityHitResult(context.world(),player,pos,end,new AABB(pos,end).inflate(.3),
                    e->e!=player && !e.isSpectator() && e.isPickable() && !e.isPassengerOfSameVehicle(player),.3f);
            if (entity!=null) { end=entity.getLocation(); impact=end; }
            else if (block.getType()!=HitResult.Type.MISS) impact=end;
            points.add(end); pos=end;
            if (impact!=null || pos.y<context.world().getMinY()-8 || points.getFirst().distanceToSqr(pos)>65536) break;
            if (bow) velocity=stepVelocity(velocity,drag,gravity,false);
        }
        path=new Path(List.copyOf(points),impact,UiTheme.accent());
    }
    private static boolean supported(ItemStack s) {
        return s.is(Items.BOW)||s.is(Items.SNOWBALL)||s.is(Items.EGG)||s.is(Items.ENDER_PEARL)||s.getItem() instanceof ThrowablePotionItem;
    }
    private static Vec3 stepVelocity(Vec3 v,double drag,double gravity,boolean thrown) {
        var n=Ballistics.nextVelocity(new Ballistics.Vector(v.x,v.y,v.z),drag,gravity,thrown);
        return new Vec3(n.x(),n.y(),n.z());
    }
    private static void draw(WorldRenderContext ctx) {
        Path state=path; if(state.points.size()<2)return;
        var pose=ctx.matrices().last(); var vertices=ctx.consumers().getBuffer(RenderTypes.lines());
        Vec3 camera=ctx.worldState().cameraRenderState.pos;
        for(int i=1;i<state.points.size();i++) line(vertices,pose,state.points.get(i-1).subtract(camera),state.points.get(i).subtract(camera),state.color);
        if(state.impact!=null) {
            Vec3 c=state.impact.subtract(camera); double r=.15;
            line(vertices,pose,c.add(-r,0,0),c.add(r,0,0),state.color);
            line(vertices,pose,c.add(0,-r,0),c.add(0,r,0),state.color);
            line(vertices,pose,c.add(0,0,-r),c.add(0,0,r),state.color);
        }
    }
    private static void line(VertexConsumer out,PoseStack.Pose pose,Vec3 a,Vec3 b,int color) {
        Vec3 normal=b.subtract(a).normalize();
        if(normal.lengthSqr()<1e-8)return;
        out.addVertex(pose,(float)a.x,(float)a.y,(float)a.z).setColor(color).setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z).setLineWidth(2);
        out.addVertex(pose,(float)b.x,(float)b.y,(float)b.z).setColor(color).setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z).setLineWidth(2);
    }
}
