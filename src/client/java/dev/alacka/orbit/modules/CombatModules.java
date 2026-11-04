package dev.alacka.orbit.modules;

import dev.alacka.orbit.config.CombatSettings;
import dev.alacka.orbit.config.CombatSettings.AttackMode;
import dev.alacka.orbit.config.CombatSettings.Targets;
import dev.alacka.orbit.config.OrbitSettings;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Visible, ordinary client rotations and normal game-mode attacks. No silent/bypass modes. */
public final class CombatModules {
    private static final CombatScheduler SCHEDULER = new CombatScheduler();
    private static ClientLevel world;
    private static CombatSettings previous;
    private static LivingEntity auraTarget;
    private static Integer lastAuraHit;
    private static long tick;
    private static boolean initialized;

    private CombatModules() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        // Rotate before LocalPlayer's normal movement update; attack after that update.
        // This avoids inventing a separate stream of fake movement packets.
        ClientTickEvents.START_CLIENT_TICK.register(CombatModules::startTick);
        ClientTickEvents.END_CLIENT_TICK.register(CombatModules::endTick);
    }

    private static boolean active(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.gameMode != null
                && mc.screen == null && mc.getOverlay() == null && !mc.isPaused() && mc.isWindowActive()
                && mc.player.isAlive() && !mc.player.isSpectator() && !mc.player.isSleeping();
    }

    private static void clearTargets() {
        auraTarget = null;
        lastAuraHit = null;
    }

    private static void startTick(Minecraft mc) {
        tick++;
        if (world != mc.level) {
            world = mc.level;
            previous = null;
            clearTargets();
            SCHEDULER.reset();
        }
        if (!active(mc)) {
            clearTargets();
            SCHEDULER.reset();
            return;
        }
        CombatSettings settings = OrbitSettings.get().combat();
        if (!settings.equals(previous)) {
            clearTargets();
            SCHEDULER.reset();
            previous = settings;
        }
        CombatSettings.Aura aura = settings.aura();
        if (!aura.enabled() || !canAct(mc, aura.onlyWeapon(), aura.pauseUsingItem())) {
            clearTargets();
            return;
        }

        Comparator<LivingEntity> order = Comparator.comparing(e -> snapshot(mc, e), CombatMath.order(aura.priority()));
        List<LivingEntity> pool = mc.level.getEntitiesOfClass(LivingEntity.class,
                        mc.player.getBoundingBox().inflate(aura.range() + 1.0),
                        e -> eligible(mc, e, aura.targets(), aura.ignoreTeam(), aura.range()) && visible(mc, e))
                .stream().sorted(order).limit(aura.maxTargets()).toList();
        int index = CombatMath.cycleIndex(pool.stream().map(Entity::getId).toList(),
                auraTarget == null ? null : auraTarget.getId(), lastAuraHit);
        auraTarget = index < 0 ? null : pool.get(index);
        if (auraTarget == null || aura.rotation() == CombatSettings.Rotation.NONE) return;

        Vec3 delta = aimPoint(mc, auraTarget).subtract(mc.player.getEyePosition());
        CombatMath.Angles angles = CombatMath.angles(delta.x, delta.y, delta.z);
        float step = aura.rotation() == CombatSettings.Rotation.INSTANT ? 360.0f : aura.rotationStep();
        mc.player.setYRot(CombatMath.stepYaw(mc.player.getYRot(), angles.yaw(), step));
        mc.player.setXRot(CombatMath.stepPitch(mc.player.getXRot(), angles.pitch(), step));
    }

    private static void endTick(Minecraft mc) {
        if (!active(mc) || mc.level != world) return;
        CombatSettings settings = OrbitSettings.get().combat();
        float strength = mc.player.getAttackStrengthScale(0.0f);
        CombatSettings.Trigger trigger = settings.trigger();

        // A real crosshair target takes priority. A fresh ray is used instead of a stale render-frame hitResult.
        if (trigger.enabled() && canAct(mc, trigger.onlyWeapon(), trigger.pauseUsingItem())
                && SCHEDULER.ready(tick, strength, trigger.mode())) {
            LivingEntity hit = aimedEntity(mc, trigger.range());
            if (hit != null && eligible(mc, hit, trigger.targets(), trigger.ignoreTeam(), trigger.range())) {
                if (attack(mc, hit, trigger.cps())) {
                    if (auraTarget == hit) {
                        lastAuraHit = hit.getId();
                        auraTarget = null;
                    }
                    return;
                }
            }
        }

        CombatSettings.Aura aura = settings.aura();
        if (!aura.enabled() || auraTarget == null || !canAct(mc, aura.onlyWeapon(), aura.pauseUsingItem())
                || !SCHEDULER.ready(tick, strength, AttackMode.COOLDOWN)) return;
        if (!eligible(mc, auraTarget, aura.targets(), aura.ignoreTeam(), aura.range()) || !visible(mc, auraTarget)) {
            auraTarget = null;
            return;
        }
        LivingEntity hit = aimedEntity(mc, aura.range());
        if (hit != auraTarget) return; // No hit until the actual view ray reaches this target.
        if (attack(mc, auraTarget, trigger.cps())) {
            lastAuraHit = auraTarget.getId();
            auraTarget = null;
        }
    }

    private static boolean attack(Minecraft mc, LivingEntity target, int cps) {
        // Use the 1.21.11 weapon's actual attack-range component, not a fabricated client reach.
        if (!mc.player.isWithinAttackRange(target.getBoundingBox(), 0.0)) return false;
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
        SCHEDULER.attacked(tick, cps);
        return true;
    }

    private static boolean canAct(Minecraft mc, boolean onlyWeapon, boolean pauseUsing) {
        if (InventoryModules.blocksCombat()) return false;
        if (pauseUsing && mc.player.isUsingItem()) return false;
        var stack = mc.player.getMainHandItem();
        // Spears have a dedicated attack path; stage 1 supports ordinary melee weapons/tools.
        if (stack.has(DataComponents.PIERCING_WEAPON) || stack.has(DataComponents.KINETIC_WEAPON)) return false;
        return !onlyWeapon || stack.has(DataComponents.WEAPON);
    }

    private static boolean eligible(Minecraft mc, LivingEntity entity, Targets targets, boolean ignoreTeam, double range) {
        if (entity == mc.player || entity.level() != mc.level || entity.isRemoved() || !entity.isAlive()
                || entity instanceof ArmorStand || entity.isSpectator() || entity.isInvulnerable() || entity.isInvisibleTo(mc.player)
                || !entity.isAttackable() || !entity.isPickable()) return false;
        if (entity instanceof Player p && p.isCreative()) return false;
        if (entity instanceof TamableAnimal pet && pet.isOwnedBy(mc.player)) return false;
        boolean player = entity instanceof Player;
        boolean mob = entity instanceof Mob;
        if (!(targets == Targets.ALL && (player || mob) || targets == Targets.PLAYERS && player
                || targets == Targets.MOBS && mob)) return false;
        if (ignoreTeam && mc.player.isAlliedTo(entity)) return false;
        return entity.getBoundingBox().distanceToSqr(mc.player.getEyePosition()) <= range * range
                && mc.player.isWithinAttackRange(entity.getBoundingBox(), 0.0);
    }

    private static Vec3 aimPoint(Minecraft mc, LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        double margin = Math.min(0.1, box.getYsize() * 0.25);
        return new Vec3((box.minX + box.maxX) * 0.5,
                Math.clamp(mc.player.getEyeY(), box.minY + margin, box.maxY - margin),
                (box.minZ + box.maxZ) * 0.5);
    }

    private static boolean visible(Minecraft mc, LivingEntity entity) {
        Vec3 eye = mc.player.getEyePosition(), point = aimPoint(mc, entity);
        HitResult block = mc.level.clip(new ClipContext(eye, point, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        return block.getType() == HitResult.Type.MISS || block.getLocation().distanceToSqr(eye) + 1.0e-7 >= point.distanceToSqr(eye);
    }

    /** The closest pickable entity wins, including non-target entities which can obstruct a hit. */
    private static LivingEntity aimedEntity(Minecraft mc, double range) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 end = eye.add(mc.player.getViewVector(1.0f).scale(range));
        HitResult block = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        double best = block.getType() == HitResult.Type.MISS ? range * range : eye.distanceToSqr(block.getLocation());
        Entity nearest = null;
        AABB search = new AABB(eye, end).inflate(1.0);
        for (Entity entity : mc.level.getEntities(mc.player, search, e -> e.isPickable() && !e.isSpectator())) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
            Vec3 hit = box.contains(eye) ? eye : box.clip(eye, end).orElse(null);
            if (hit == null) continue;
            double distance = eye.distanceToSqr(hit);
            if (distance < best || nearest == null && block.getType() == HitResult.Type.MISS && distance <= best) {
                best = distance;
                nearest = entity;
            }
        }
        return nearest instanceof LivingEntity living ? living : null;
    }

    private static CombatMath.Candidate snapshot(Minecraft mc, LivingEntity entity) {
        Vec3 delta = aimPoint(mc, entity).subtract(mc.player.getEyePosition());
        CombatMath.Angles angles = CombatMath.angles(delta.x, delta.y, delta.z);
        // Vanilla remote-player health is not generally synchronized. Only mob HP is used.
        Double health = entity instanceof Player ? null : (double) entity.getHealth();
        return new CombatMath.Candidate(entity.getId(),
                entity.getBoundingBox().distanceToSqr(mc.player.getEyePosition()),
                CombatMath.angularDistance(mc.player.getYRot(), mc.player.getXRot(), angles), health);
    }
}