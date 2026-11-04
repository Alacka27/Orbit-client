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
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.joml.Quaternionf;

/** Immutable extracted geometry; vanilla launch parameters without random spread. */
public final class Trajectories {
    public record Impact(Vec3 position, double seconds) {}
    private record Path(List<Vec3> points, Vec3 impact, double seconds, int color, boolean gradient) {}
    private static volatile List<Path> paths = List.of();
    private Trajectories() {}
    public static void initialize() {
        WorldRenderEvents.END_EXTRACTION.register(Trajectories::extract);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(Trajectories::draw);
    }
    public static int pathCount() { return paths.size(); }
    public static int pointCount() { return paths.stream().mapToInt(p -> p.points.size()).sum(); }
    public static Impact impact() {
        var snapshot = paths;
        return snapshot.isEmpty() || snapshot.getFirst().impact == null ? null
                : new Impact(snapshot.getFirst().impact, snapshot.getFirst().seconds);
    }
    private static void extract(WorldExtractionContext context) {
        Minecraft mc = Minecraft.getInstance(); var player = mc.player;
        paths = List.of();
        if (!OrbitSettings.get().trajectories() || player == null || !player.isAlive() || mc.options.hideGui) return;
        ItemStack stack = player.isUsingItem() ? player.getUseItem() : player.getMainHandItem();
        if (!supported(stack)) stack = player.getOffhandItem();
        if (!supported(stack)) return;
        boolean bow = stack.is(Items.BOW), crossbow = stack.is(Items.CROSSBOW), trident = stack.is(Items.TRIDENT);
        boolean arrow = bow || crossbow || trident, potion = stack.getItem() instanceof ThrowablePotionItem;
        double speed = bow ? 3 : crossbow ? 3.15 : trident ? 2.5 : potion ? .5 : 1.5;
        double gravity = arrow || potion ? .05 : .03;
        int count = 1;
        if (bow && player.isUsingItem()) {
            speed = BowItem.getPowerForTime(player.getTicksUsingItem()) * 3;
            if (speed < .3) return;
        }
        if (trident && (EnchantmentHelper.getTridentSpinAttackStrength(stack, player) > 0
                || player.isUsingItem() && player.getTicksUsingItem() < 10)) return;
        if (crossbow) {
            var charged = stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).getItems();
            // Firework acceleration/lifetime and custom projectiles are not modeled as arrows.
            if (charged.isEmpty() || charged.size() > 3 || charged.stream().anyMatch(s -> !(s.getItem() instanceof ArrowItem))) return;
            count = charged.size();
        }
        float partial = context.tickCounter().getGameTimeDeltaPartialTick(false);
        Vec3 origin = player.getEyePosition(partial).add(0, -.1, 0);
        double yaw = Math.toRadians(player.getYRot()), pitch = Math.toRadians(player.getXRot());
        Vec3 direction = new Vec3(-Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch + Math.toRadians(potion ? -20 : 0)), Math.cos(yaw) * Math.cos(pitch)).normalize();
        var result = new ArrayList<Path>();
        for (int shot = 0; shot < count; shot++) {
            Vec3 aim = direction;
            if (shot > 0) {
                double angle = Math.toRadians(shot == 1 ? -10 : 10);
                Vec3 up = player.getUpVector(partial);
                var rotated = direction.toVector3f().rotate(new Quaternionf().setAngleAxis(angle,up.x,up.y,up.z));
                aim = new Vec3(rotated);
            }
            Vec3 velocity = aim.scale(speed);
            // Crossbow uses shoot(), not shootFromRotation(): it does not inherit walking velocity.
            if (!crossbow) {
                Vec3 inherited = player.getKnownMovement();
                velocity = velocity.add(inherited.x, player.onGround() ? 0 : inherited.y, inherited.z);
            }
            List<Vec3> points = new ArrayList<>(); points.add(origin);
            Vec3 pos = origin, impact = null; double seconds = 0;
            for (int step = 0; step < 200; step++) {
                if (!context.world().hasChunkAt(BlockPos.containing(pos))) break;
                boolean water = context.world().getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER);
                double drag = water ? trident ? .99 : arrow ? .6 : .8 : .99;
                if (!arrow) velocity = stepVelocity(velocity, drag, gravity, true);
                Vec3 fullEnd = pos.add(velocity), end = fullEnd;
                HitResult block = context.world().clip(new ClipContext(pos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
                var entity = ProjectileUtil.getEntityHitResult(context.world(), player, pos, end, new AABB(pos,end).inflate(.3),
                        e -> e != player && !e.isSpectator() && e.isPickable() && !e.isPassengerOfSameVehicle(player), .3f);
                if (entity != null) { end = entity.getLocation(); impact = end; }
                else if (block.getType() != HitResult.Type.MISS) impact = end;
                seconds = (step + (impact == null ? 1 : Math.clamp(pos.distanceTo(end) / Math.max(1e-9,pos.distanceTo(fullEnd)),0,1))) / 20.0;
                points.add(end); pos = end;
                if (impact != null || pos.y < context.world().getMinY()-8 || origin.distanceToSqr(pos) > 65536) break;
                if (arrow) velocity = stepVelocity(velocity, drag, gravity, false);
            }
            if (points.size() > 1) result.add(new Path(List.copyOf(points), impact, seconds,
                    UiTheme.accent(), OrbitSettings.get().visual().trajectoryGradient()));
        }
        paths = List.copyOf(result);
    }
    private static boolean supported(ItemStack stack) {
        return stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.TRIDENT)
                || stack.is(Items.SNOWBALL) || stack.is(Items.EGG) || stack.is(Items.ENDER_PEARL)
                || stack.getItem() instanceof ThrowablePotionItem;
    }
    private static Vec3 stepVelocity(Vec3 v, double drag, double gravity, boolean thrown) {
        var next = Ballistics.nextVelocity(new Ballistics.Vector(v.x,v.y,v.z),drag,gravity,thrown);
        return new Vec3(next.x(),next.y(),next.z());
    }
    private static void draw(WorldRenderContext ctx) {
        var snapshot = paths;
        if (snapshot.isEmpty()) return;
        var pose = ctx.matrices().last(); var vertices = ctx.consumers().getBuffer(RenderTypes.lines());
        Vec3 camera = ctx.worldState().cameraRenderState.pos;
        for (var state : snapshot) {
            for (int i = 1; i < state.points.size(); i++) {
                int color = state.gradient ? VisualLedger.gradient(state.color,(i-1.0)/Math.max(1,state.points.size()-2)) : state.color;
                line(vertices,pose,state.points.get(i-1).subtract(camera),state.points.get(i).subtract(camera),color);
            }
            if (state.impact != null) {
                Vec3 c = state.impact.subtract(camera); double r = .15;
                line(vertices,pose,c.add(-r,0,0),c.add(r,0,0),state.color);
                line(vertices,pose,c.add(0,-r,0),c.add(0,r,0),state.color);
                line(vertices,pose,c.add(0,0,-r),c.add(0,0,r),state.color);
            }
        }
    }
    private static void line(VertexConsumer out, PoseStack.Pose pose, Vec3 a, Vec3 b, int color) {
        Vec3 normal = b.subtract(a).normalize(); if (normal.lengthSqr() < 1e-8) return;
        out.addVertex(pose,(float)a.x,(float)a.y,(float)a.z).setColor(color).setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z).setLineWidth(2);
        out.addVertex(pose,(float)b.x,(float)b.y,(float)b.z).setColor(color).setNormal(pose,(float)normal.x,(float)normal.y,(float)normal.z).setLineWidth(2);
    }
}
