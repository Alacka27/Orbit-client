package dev.alacka.orbit.modules;

import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.config.SupportSettings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Native player-menu clicks and normal item use only. Every full equipment swap
 * finishes in one client tick; the cursor must be empty before automation starts.
 */
public final class InventoryModules {
    private static final SupportGate GATE=new SupportGate();
    private static final ConsumeSession CONSUME=new ConsumeSession();
    private static final UseKeyLease USE=new UseKeyLease();
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static ClientLevel world,consumeWorld;
    private static LocalPlayer consumePlayer;
    private static long tick,busyTick=-1,resumeConsumeAt,suspendedUntil,pendingSwapUntil;
    private static boolean initialized;
    private InventoryModules() {}

    public static void initialize() {
        if (initialized) return;
        initialized=true;
        ClientTickEvents.START_CLIENT_TICK.register(InventoryModules::startTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> stopConsume(mc,false));
    }
    /** Also blocks attacks when the user disabled the combat module's ordinary item-use pause. */
    public static boolean blocksCombat() { return CONSUME.active()||busyTick==tick; }
    private static boolean active(Minecraft mc) {
        return mc.player!=null&&mc.level!=null&&mc.gameMode!=null&&mc.screen==null&&mc.getOverlay()==null
                &&!mc.isPaused()&&mc.isWindowActive()&&mc.player.isAlive()&&!mc.player.isSpectator()
                &&!mc.player.isSleeping()&&mc.player.containerMenu==mc.player.inventoryMenu
                &&mc.player.inventoryMenu.getCarried().isEmpty();
    }
    private static void startTick(Minecraft mc) {
        tick++;
        boolean requested=dev.alacka.orbit.input.ModuleBinds.consumeSwap(active(mc));
        if(world!=mc.level) {
            stopConsume(mc,false); world=mc.level; GATE.reset();
            pendingSwapUntil=0; resumeConsumeAt=0; suspendedUntil=0;
        }
        if(!active(mc)) {
            stopConsume(mc,mc.isWindowActive()); pendingSwapUntil=0; GATE.reset(); return;
        }
        if(tick<suspendedUntil) return;
        try { update(mc,OrbitSettings.get().support(),requested); }
        catch(RuntimeException error) {
            stopConsume(mc,true); pendingSwapUntil=0; suspendedUntil=tick+100;
            OrbitClient.LOGGER.error("[Orbit] Item automation paused after an unexpected interaction failure",error);
            message(mc,"Ошибка действия с предметом; автоматика приостановлена. Подробности в latest.log.");
        }
    }
    private static void update(Minecraft mc,SupportSettings settings,boolean requested) {
        boolean ended=false;
        if(requested&&settings.elytra().enabled()) {
            stopConsume(mc,true); pendingSwapUntil=tick+20;
        }
        if(CONSUME.active()) {
            var c=settings.consume();
            var reason=CONSUME.observe(tick,mc.player.getInventory().getSelectedSlot(),mc.player.getHealth(),
                    mc.player.isUsingItem(),c.enabled()&&!USE.releasedByUser(mc),
                    c.safeOnly()&&danger(mc,c.dangerRadius()),mc.player.hurtTime>0,c.stopOnDamage());
            if(reason!=ConsumeSession.Stop.NONE) {
                stopConsume(mc,true); resumeConsumeAt=tick+(reason==ConsumeSession.Stop.FINISHED?2:20); ended=true;
            } else USE.hold(mc);
        }
        // An offhand replacement does not change a food/potion being used in the main hand.
        if(tryTotem(mc,settings.totem())) return;
        if(CONSUME.active()||ended) return;
        if(!settings.elytra().enabled()) pendingSwapUntil=0;
        if(pendingSwapUntil>=tick&&pendingSwapUntil!=0) {
            if(tryElytra(mc,settings.elytra(),settings.armor().protection())) return;
        } else pendingSwapUntil=0;
        if(mc.player.isUsingItem()) return; // Never take over a manual food/shield use.
        if(tick>=resumeConsumeAt&&tryConsume(mc,settings.consume())) return;
        tryArmor(mc,settings.armor());
    }
    private static boolean tryTotem(Minecraft mc,SupportSettings.Totem s) {
        if(!s.enabled()||mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return false;
        if(s.healthOnly()&&!SupportMath.lowHealth(mc.player.getHealth(),mc.player.getAbsorptionAmount(),s.health())) return false;
        int source=-1;
        for(int n=0;n<36;n++) {
            int slot=s.hotbarFirst()?n:(n+9)%36;
            if(mc.player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING)) { source=slot; break; }
        }
        if(source<0||!GATE.claim(tick,SupportGate.Action.TOTEM,s.delay())) return false;
        busyTick=tick;
        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId,SupportMath.menuSlot(source),40,ClickType.SWAP,mc.player);
        return true;
    }
    private static boolean tryArmor(Minecraft mc,SupportSettings.Armor s) {
        if(!s.enabled()) return false;
        for(var slot:ARMOR) {
            ItemStack current=mc.player.getItemBySlot(slot);
            if(bound(current)||slot==EquipmentSlot.CHEST&&current.has(DataComponents.GLIDER)) continue;
            int source=bestArmor(mc,slot,s.protection(),score(mc,current,slot,s.protection()));
            if(source<0) continue;
            if(!GATE.claim(tick,SupportGate.Action.ARMOR,s.delay())) return false;
            busyTick=tick; exchangeEquipment(mc,source,slot); return true;
        }
        return false;
    }
    private static int bestArmor(Minecraft mc,EquipmentSlot slot,boolean protection,double baseline) {
        int best=-1; double value=baseline;
        for(int i=0;i<36;i++) {
            ItemStack stack=mc.player.getInventory().getItem(i);
            if(!equips(mc,stack,slot)||bound(stack)||stack.has(DataComponents.GLIDER)) continue;
            double candidate=score(mc,stack,slot,protection);
            if(candidate>value+0.000001) { value=candidate; best=i; }
        }
        return best;
    }
    private static boolean equips(Minecraft mc,ItemStack stack,EquipmentSlot slot) {
        var e=stack.get(DataComponents.EQUIPPABLE);
        return !stack.isEmpty()&&stack.getCount()==1&&e!=null&&e.slot()==slot&&e.canBeEquippedBy(mc.player.getType());
    }
    private static boolean bound(ItemStack stack) {
        return !stack.isEmpty()&&EnchantmentHelper.has(stack,EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
    }
    private static double durability(ItemStack stack) {
        return !stack.isDamageableItem()?1.0:Math.max(0.0,(stack.getMaxDamage()-stack.getDamageValue())/(double)Math.max(1,stack.getMaxDamage()));
    }
    private static double score(Minecraft mc,ItemStack stack,EquipmentSlot slot,boolean preferProtection) {
        if(stack.isEmpty()) return 0;
        if(!equips(mc,stack,slot)||stack.has(DataComponents.GLIDER)) return 0;
        double[] values={0,0};
        stack.forEachModifier(slot,(attribute,modifier) -> {
            if(attribute.equals(Attributes.ARMOR)) values[0]+=modifier.amount();
            if(attribute.equals(Attributes.ARMOR_TOUGHNESS)) values[1]+=modifier.amount();
        });
        if(values[0]<=0) return 0; // Pumpkins/heads are not armor upgrades.
        var protection=mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
        return SupportMath.armorScore(values[0],values[1],EnchantmentHelper.getItemEnchantmentLevel(protection,stack),durability(stack),preferProtection);
    }
    private static boolean tryElytra(Minecraft mc,SupportSettings.Elytra s,boolean protection) {
        ItemStack chest=mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if(bound(chest)) {
            pendingSwapUntil=0; message(mc,"Нагрудный слот нельзя снять: Проклятие привязки."); return false;
        }
        boolean glidingItem=chest.has(DataComponents.GLIDER);
        if(glidingItem&&s.protectFlight()&&mc.player.isFallFlying()) {
            pendingSwapUntil=0; message(mc,"Снятие элитр в полёте запрещено настройкой."); return false;
        }
        int source=-1;
        if(glidingItem) source=bestArmor(mc,EquipmentSlot.CHEST,protection,0);
        else {
            double best=-1;
            for(int i=0;i<36;i++) {
                var stack=mc.player.getInventory().getItem(i);
                if(stack.has(DataComponents.GLIDER)&&equips(mc,stack,EquipmentSlot.CHEST)&&!bound(stack)
                        &&(!stack.isDamageableItem()||stack.getMaxDamage()-stack.getDamageValue()>1)&&durability(stack)>best) {
                    source=i; best=durability(stack);
                }
            }
        }
        if(source<0) {
            pendingSwapUntil=0; message(mc,glidingItem?"Нагрудник в инвентаре не найден.":"Исправные элитры в инвентаре не найдены."); return false;
        }
        if(!GATE.claim(tick,SupportGate.Action.ELYTRA,s.delay())) return false;
        pendingSwapUntil=0; busyTick=tick; exchangeEquipment(mc,source,EquipmentSlot.CHEST); return true;
    }
    /** A regular cursor swap, completed before another module may touch the inventory. */
    private static void exchangeEquipment(Minecraft mc,int source,EquipmentSlot armor) {
        var menu=mc.player.inventoryMenu;
        int from=SupportMath.menuSlot(source);
        ItemStack next=mc.player.getInventory().getItem(source).copy();
        try {
            mc.gameMode.handleInventoryMouseClick(menu.containerId,from,0,ClickType.PICKUP,mc.player);
            mc.gameMode.handleInventoryMouseClick(menu.containerId,SupportMath.armorSlot(armor.name()),0,ClickType.PICKUP,mc.player);
        } finally {
            // Restore either the previous armor or an unplaced replacement. Never drop it.
            if(mc.player.containerMenu==menu&&!menu.getCarried().isEmpty()&&menu.getSlot(from).getItem().isEmpty())
                mc.gameMode.handleInventoryMouseClick(menu.containerId,from,0,ClickType.PICKUP,mc.player);
        }
        if(!menu.getCarried().isEmpty()||!ItemStack.isSameItemSameComponents(mc.player.getItemBySlot(armor),next)) {
            suspendedUntil=tick+100;
            message(mc,"Обмен не подтверждён клиентом. Проверь инвентарь; автоматика приостановлена.");
            OrbitClient.LOGGER.warn("[Orbit] Unexpected equipment-swap result; no item was dropped");
        }
    }
    private static boolean tryConsume(Minecraft mc,SupportSettings.Consume s) {
        if(!s.enabled()||s.stopOnDamage()&&mc.player.hurtTime>0||s.safeOnly()&&danger(mc,s.dangerRadius())) return false;
        int selected=-1,rank=-1;
        for(int i=0;i<9;i++) {
            int value=consumeRank(mc,mc.player.getInventory().getItem(i),s);
            if(value>rank) { selected=i; rank=value; }
        }
        if(selected<0||rank<0||!GATE.claim(tick,SupportGate.Action.CONSUME,s.delay())||!USE.acquire(mc)) return false;
        int original=mc.player.getInventory().getSelectedSlot();
        ItemStack item=mc.player.getInventory().getItem(selected);
        CONSUME.begin(tick,original,selected,Math.max(1,item.getUseDuration(mc.player)),mc.player.getHealth());
        consumePlayer=mc.player; consumeWorld=mc.level; busyTick=tick;
        mc.player.getInventory().setSelectedSlot(selected);
        mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
        if(!mc.player.isUsingItem()) { stopConsume(mc,true); resumeConsumeAt=tick+20; }
        return true;
    }
    private static int consumeRank(Minecraft mc,ItemStack stack,SupportSettings.Consume s) {
        if(stack.isEmpty()||mc.player.getCooldowns().isOnCooldown(stack)) return -1;
        boolean health=mc.player.getHealth()<=s.health();
        if(stack.is(Items.GOLDEN_APPLE)||stack.is(Items.ENCHANTED_GOLDEN_APPLE))
            return s.gapples()&&health&&!mc.player.hasEffect(MobEffects.REGENERATION)&&mc.player.getAbsorptionAmount()<=0
                    ? (stack.is(Items.GOLDEN_APPLE)?900:890) : -1;
        if(stack.is(Items.POTION)) {
            if(!s.potions()) return -1;
            var contents=stack.get(DataComponents.POTION_CONTENTS);
            if(contents==null) return -1;
            boolean needed=false,healing=false;
            for(var effect:contents.getAllEffects()) {
                var kind=effect.getEffect();
                if(!kind.value().isBeneficial()) return -1;
                if(kind.equals(MobEffects.INSTANT_HEALTH)) { if(health) { needed=true; healing=true; } }
                else if(!kind.value().isInstantenous()) {
                    var current=mc.player.getEffect(kind);
                    if(current==null||current.getAmplifier()<effect.getAmplifier()
                            ||current.getAmplifier()==effect.getAmplifier()&&current.getDuration()>=0&&current.getDuration()<=40) needed=true;
                }
            }
            return needed?(healing?1100:200):-1;
        }
        var food=stack.get(DataComponents.FOOD);
        return s.food()&&food!=null&&food.nutrition()>0&&mc.player.canEat(false)&&mc.player.getFoodData().getFoodLevel()<=s.hunger()
                ? 500+food.nutrition() : -1;
    }
    private static boolean danger(Minecraft mc,double radius) {
        return !mc.level.getEntitiesOfClass(LivingEntity.class,mc.player.getBoundingBox().inflate(radius),e -> {
            if(e==mc.player||!e.isAlive()||e.isSpectator()||e.distanceToSqr(mc.player)>radius*radius||mc.player.isAlliedTo(e)) return false;
            return e instanceof Player||e instanceof Enemy||e instanceof Mob mob&&mob.getTarget()==mc.player;
        }).isEmpty();
    }
    private static void stopConsume(Minecraft mc,boolean restorePhysical) {
        if(CONSUME.active()&&mc.player==consumePlayer&&mc.level==consumeWorld) {
            if(mc.gameMode!=null&&mc.player.isUsingItem()) mc.gameMode.releaseUsingItem(mc.player);
            mc.player.getInventory().setSelectedSlot(CONSUME.restoreSlot(mc.player.getInventory().getSelectedSlot()));
            busyTick=tick;
        }
        USE.release(mc,restorePhysical);
        CONSUME.clear(); consumePlayer=null; consumeWorld=null;
    }
    private static void message(Minecraft mc,String text) {
        mc.player.displayClientMessage(Component.literal("Orbit: "+text),true);
    }
}