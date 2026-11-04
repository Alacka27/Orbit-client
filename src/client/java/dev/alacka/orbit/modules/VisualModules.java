package dev.alacka.orbit.modules;

import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.config.VisualSettings;
import dev.alacka.orbit.menu.UiTheme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Client-only observation and effects. Does not change health, invisibility, attacks or packets. */
public final class VisualModules {
    public record Health(double value, double maximum, double absorption, boolean known, String source) {
        public static final Health UNKNOWN = new Health(0, 0, 0, false, "");
        public boolean scoreboard() { return source.startsWith("scoreboard:"); }
        public double total() { return value + absorption; }
    }
    public record Target(String name, PlayerSkin skin, Health health, int armor, double distance,
                         int pops, String shield, boolean preview) {
        public static Target sample() { return new Target("Предпросмотр", null, Health.UNKNOWN, 0, 0, 0, "", true); }
    }
    public record Shield(boolean present, boolean blocking, int remaining, int maximum, float cooldown) {
        static final Shield EMPTY = new Shield(false, false, 0, 0, 0);
    }
    public record Label(float x, float y, String text, int color, float scale) {}
    public record Frame(Target target, List<Label> labels, Shield shield, int ownTotems, int selfPops) {
        static final Frame EMPTY = new Frame(null, List.of(), Shield.EMPTY, 0, 0);
    }
    private record Floating(Vec3 position, String text, int color, long born, int life, String module) {}
    private static final VisualLedger LEDGER = new VisualLedger();
    private static final List<Floating> FLOATING = new ArrayList<>();
    private static final Map<UUID, String> HEALTH_SOURCES = new HashMap<>();
    private static final Map<UUID, Entity> HURT = new LinkedHashMap<>();
    private static final Map<UUID, Long> SHIELD_DISABLED = new LinkedHashMap<>();
    private static Object world, player;
    private static UUID profile, targetUuid;
    private static int targetId = -1;
    private static long tick, targetUntil;
    private static volatile Frame frame = Frame.EMPTY;

    private VisualModules() {}
    public static Frame frame() { return frame; }
    public static int floatingCount() { return FLOATING.size(); }
    public static int observedPops(UUID id) { return LEDGER.pops(id); }
    public static void clearPopCounts() { LEDGER.clearPops(); }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(VisualModules::tick);
        AttackEntityCallback.EVENT.register((who, level, hand, target, hit) -> {
            Minecraft mc = Minecraft.getInstance();
            if (who == mc.player && level == mc.level && target instanceof LivingEntity) {
                context(mc);
                targetId = target.getId(); targetUuid = target.getUUID();
                targetUntil = tick + OrbitSettings.get().visual().targetSeconds() * 20L;
            }
            return InteractionResult.PASS;
        });
        WorldRenderEvents.END_EXTRACTION.register(VisualModules::extract);
    }
    private static void context(Minecraft mc) {
        UUID id = mc.getUser() == null ? null : mc.getUser().getProfileId();
        if (world != mc.level || player != mc.player || !java.util.Objects.equals(profile, id)) {
            world = mc.level; player = mc.player; profile = id;
            LEDGER.clear(); FLOATING.clear(); HEALTH_SOURCES.clear(); HURT.clear(); SHIELD_DISABLED.clear();
            targetId = -1; targetUuid = null; targetUntil = 0; tick = 0; frame = Frame.EMPTY;
        }
    }
    private static boolean near(Entity e, Minecraft mc) {
        return e != null && !e.isRemoved() && mc.level != null && e.level() == mc.level && mc.player != null
                && mc.player.distanceToSqr(e) <= Math.pow(OrbitSettings.get().visual().range(), 2);
    }
    public static boolean reveal(LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        var settings = OrbitSettings.get().visual();
        return settings.enabled("SeeInvisibles") && entity.isInvisible() && near(entity, mc)
                && (entity instanceof Player || settings.seeMobs() && !(entity instanceof net.minecraft.world.entity.decoration.ArmorStand));
    }
    public static Health health(LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (entity instanceof Player && entity != mc.player) {
            if (mc.level == null) return Health.UNKNOWN;
            var board = mc.level.getScoreboard();
            for (DisplaySlot slot : new DisplaySlot[]{DisplaySlot.BELOW_NAME, DisplaySlot.LIST, DisplaySlot.SIDEBAR}) {
                var objective = board.getDisplayObjective(slot);
                if (objective == null) continue;
                var score = board.getPlayerScoreInfo((Player) entity, objective);
                double value = VisualLedger.declaredHealthScore(
                        objective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS,
                        score == null ? null : score.value());
                if (Double.isFinite(value))
                    return new Health(value, 0, 0, true, "scoreboard:" + objective.getName());
            }
            return Health.UNKNOWN;
        }
        double value = entity.getHealth(), max = entity.getMaxHealth(), absorption = entity.getAbsorptionAmount();
        return Double.isFinite(value) && Double.isFinite(max) && Double.isFinite(absorption)
                && value >= 0 && max > 0 && max <= 4096 && absorption >= 0
                ? new Health(value, max, absorption, true, "entity") : Health.UNKNOWN;
    }

    private static void tick(Minecraft mc) {
        context(mc);
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        tick++;
        var settings = OrbitSettings.get().visual();
        LEDGER.prune(tick);
        FLOATING.removeIf(f -> tick - f.born >= f.life || !settings.enabled(f.module));
        SHIELD_DISABLED.entrySet().removeIf(e -> tick - e.getValue() > 40);
        if (settings.enabled("DamageNumbers")) {
            var nearby = mc.level.getEntitiesOfClass(LivingEntity.class,
                    mc.player.getBoundingBox().inflate(settings.range()), e -> near(e, mc));
            int sampled = 0;
            for (LivingEntity entity : nearby) {
                if (sampled++ >= VisualLedger.LIMIT) break;
                Health h = health(entity);
                String previousSource = HEALTH_SOURCES.put(entity.getUUID(), h.source);
                if (!java.util.Objects.equals(previousSource, h.source))
                    LEDGER.observe(entity.getUUID(), 0, false, tick);
                double change = LEDGER.observe(entity.getUUID(), h.total(), h.known, tick);
                if (Double.isFinite(change) && Math.abs(change) >= .05
                        && (change > 0 || settings.healing())) {
                    boolean critical = change > 0 && LEDGER.isCritical(entity.getUUID(), tick);
                    int color = change < 0 ? 0xff88e0a0 : critical ? 0xffffcf72 : 0xfff1c0d5;
                    String text = (change < 0 ? "+" : "−") + (h.scoreboard() ? "≈" : "") + number(Math.abs(change));
                    add(new Floating(entity.position().add(0, entity.getBbHeight() * .75, 0),
                            text, color, tick, settings.numberTicks(), "DamageNumbers"));
                }
            }
            HEALTH_SOURCES.keySet().removeIf(id -> nearby.stream().noneMatch(e -> e.getUUID().equals(id)));
        } else {
            HEALTH_SOURCES.clear();
            LEDGER.clearHealth();
        }
        if (settings.enabled("HitParticles")) for (Entity entity : List.copyOf(HURT.values()))
            if (near(entity, mc)) particles(entity, LEDGER.isCritical(entity.getUUID(), tick) ? 0xffcf72 : UiTheme.accent() & 0xffffff,
                    settings.particleCount(), .7f);
        HURT.clear();
    }
    public static void damage(Entity entity) {
        Minecraft mc = Minecraft.getInstance(); context(mc);
        if (entity instanceof LivingEntity && near(entity, mc) && OrbitSettings.get().visual().enabled("HitParticles")) {
            HURT.put(entity.getUUID(), entity);
            while (HURT.size() > 64) HURT.remove(HURT.keySet().iterator().next());
        }
    }
    public static void critical(Entity entity) {
        Minecraft mc = Minecraft.getInstance(); context(mc);
        if (!(entity instanceof LivingEntity) || !near(entity, mc)) return;
        boolean fresh = LEDGER.critical(entity.getUUID(), tick);
        if (fresh && OrbitSettings.get().visual().enabled("CriticalHitEffect")) {
            particles(entity, 0xffcf72, OrbitSettings.get().visual().particleCount(), 1.1f);
            add(new Floating(entity.position().add(0, entity.getBbHeight() + .15, 0),
                    "КРИТ", 0xffffcf72, tick, 18, "CriticalHitEffect"));
        }
    }
    public static void entityEvent(Entity entity, byte event) {
        Minecraft mc = Minecraft.getInstance(); context(mc);
        if (!near(entity, mc)) return;
        if (event == 35 && OrbitSettings.get().visual().enabled("TotemCounter")) LEDGER.pop(entity.getUUID(), tick);
        if (event == 30 && OrbitSettings.get().visual().enabled("ShieldStatus")) {
            SHIELD_DISABLED.put(entity.getUUID(), tick);
            while (SHIELD_DISABLED.size() > 64) SHIELD_DISABLED.remove(SHIELD_DISABLED.keySet().iterator().next());
        }
    }
    private static void add(Floating effect) {
        if (FLOATING.size() >= 128) FLOATING.removeFirst();
        FLOATING.add(effect);
    }
    private static void particles(Entity entity, int rgb, int count, float size) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) return;
        var random = mc.level.random;
        var type = new DustParticleOptions(rgb, size);
        for (int i = 0; i < count; i++) mc.level.addParticle(type,
                entity.getX() + (random.nextDouble() - .5) * entity.getBbWidth(),
                entity.getY() + entity.getBbHeight() * (.35 + random.nextDouble() * .5),
                entity.getZ() + (random.nextDouble() - .5) * entity.getBbWidth(),
                (random.nextDouble() - .5) * .12, .03 + random.nextDouble() * .08, (random.nextDouble() - .5) * .12);
    }

    private static Target target(Minecraft mc) {
        Entity focused = tick <= targetUntil ? mc.level.getEntity(targetId) : null;
        if (focused != null && !focused.getUUID().equals(targetUuid)) focused = null;
        if (!(focused instanceof LivingEntity) && mc.hitResult instanceof EntityHitResult hit) focused = hit.getEntity();
        if (!(focused instanceof LivingEntity living) || !near(living, mc) || living == mc.player) return null;
        return new Target(living.getName().getString(),
                living instanceof AbstractClientPlayer p ? p.getSkin() : null, health(living),
                living.getArmorValue(), mc.player.distanceTo(living), LEDGER.pops(living.getUUID()), shieldLabel(living), false);
    }
    private static Shield ownShield(Minecraft mc) {
        ItemStack stack = mc.player.getUseItem();
        if (!stack.is(Items.SHIELD)) stack = mc.player.getOffhandItem().is(Items.SHIELD)
                ? mc.player.getOffhandItem() : mc.player.getMainHandItem();
        if (!stack.is(Items.SHIELD)) return Shield.EMPTY;
        return new Shield(true, mc.player.isBlocking(), Math.max(0, stack.getMaxDamage() - stack.getDamageValue()),
                stack.getMaxDamage(), mc.player.getCooldowns().getCooldownPercent(stack, 0));
    }
    private static String shieldLabel(LivingEntity entity) {
        if (!entity.getOffhandItem().is(Items.SHIELD) && !entity.getMainHandItem().is(Items.SHIELD)) return "";
        Long disabled = SHIELD_DISABLED.get(entity.getUUID());
        if (disabled != null && tick - disabled < 40) return "Щит: сбит";
        return entity.isBlocking() ? "Щит: блокирует" : "Щит: в руке";
    }
    private static int ownTotems(Minecraft mc) {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) count += stack.getCount();
        }
        return count;
    }
    private static void extract(WorldExtractionContext context) {
        Minecraft mc = Minecraft.getInstance(); context(mc);
        if (mc.player == null || mc.level == null || mc.options.hideGui) { frame = Frame.EMPTY; return; }
        var settings = OrbitSettings.get().visual();
        List<Label> labels = new ArrayList<>();
        float partial = context.tickCounter().getGameTimeDeltaPartialTick(false);
        for (Floating effect : List.copyOf(FLOATING)) {
            double age = tick - effect.born + partial;
            float alpha = (float) VisualLedger.alpha(age, effect.life);
            if (alpha <= 0 || !settings.enabled(effect.module)) continue;
            Vec3 pos = effect.position.add(0, age * .025, 0);
            project(mc, labels, pos, effect.text, (Math.round(alpha * 255) << 24) | effect.color & 0xffffff, 1.05f);
        }
        if (settings.enabled("ShieldStatus") || settings.enabled("TotemCounter")) {
            int count = 0;
            for (Player other : mc.level.players()) {
                if (count >= 24) break;
                if (other == mc.player || !near(other, mc) || other.isInvisible() && !reveal(other)) continue;
                String text = settings.enabled("ShieldStatus") ? shieldLabel(other) : "";
                int pops = LEDGER.pops(other.getUUID());
                if (settings.enabled("TotemCounter") && pops > 0)
                    text += (text.isEmpty() ? "" : " · ") + "Тотем: " + pops + " сраб.";
                if (!text.isEmpty()) {
                    project(mc, labels, other.getPosition(partial).add(0, other.getBbHeight() + .55, 0), text, 0xffe4d2f3, .8f);
                    count++;
                }
            }
        }
        if (OrbitSettings.get().trajectories() && settings.trajectoryTime()) {
            var impact = Trajectories.impact();
            if (impact != null) project(mc, labels, impact.position().add(0, .2, 0),
                    String.format(Locale.ROOT, "%.2f с", impact.seconds()), 0xffe5c6ff, .8f);
        }
        frame = new Frame(settings.enabled("TargetHUD") ? target(mc) : null, List.copyOf(labels),
                settings.enabled("ShieldStatus") ? ownShield(mc) : Shield.EMPTY,
                settings.enabled("TotemCounter") ? ownTotems(mc) : 0, LEDGER.pops(mc.player.getUUID()));
    }
    private static void project(Minecraft mc, List<Label> labels, Vec3 position, String text, int color, float scale) {
        var camera = mc.gameRenderer.getMainCamera();
        Vec3 relative = position.subtract(camera.position());
        var forward = camera.forwardVector();
        if (relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z() <= .1) return;
        HitResult obstruction = mc.level.clip(new ClipContext(camera.position(), position, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mc.player));
        if (obstruction.getType() != HitResult.Type.MISS && obstruction.getLocation().distanceToSqr(position) > .12) return;
        Vec3 ndc = mc.gameRenderer.projectPointToScreen(position);
        if (!Double.isFinite(ndc.x) || !Double.isFinite(ndc.y) || Math.abs(ndc.x) > 1.1 || Math.abs(ndc.y) > 1.1) return;
        float x = (float) ((ndc.x + 1) * .5 * mc.getWindow().getGuiScaledWidth());
        float y = (float) ((1 - ndc.y) * .5 * mc.getWindow().getGuiScaledHeight());
        labels.add(new Label(x, y, text, color, scale));
    }
    public static String number(double value) {
        return Math.abs(value - Math.rint(value)) < .05 ? Long.toString(Math.round(value))
                : String.format(Locale.ROOT, "%.1f", value);
    }
}