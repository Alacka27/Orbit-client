package dev.alacka.orbit.config;

import java.util.Objects;
import java.util.Properties;

/** Opt-in item automation. The persisted format is shared by profiles and named configs. */
public record SupportSettings(Totem totem, Armor armor, Consume consume, Elytra elytra) {
    public static final SupportSettings DEFAULT = new SupportSettings(Totem.DEFAULT, Armor.DEFAULT, Consume.DEFAULT, Elytra.DEFAULT);
    public SupportSettings {
        Objects.requireNonNull(totem); Objects.requireNonNull(armor);
        Objects.requireNonNull(consume); Objects.requireNonNull(elytra);
    }
    public SupportSettings withTotem(Totem v) { return new SupportSettings(v,armor,consume,elytra); }
    public SupportSettings withArmor(Armor v) { return new SupportSettings(totem,v,consume,elytra); }
    public SupportSettings withConsume(Consume v) { return new SupportSettings(totem,armor,v,elytra); }
    public SupportSettings withElytra(Elytra v) { return new SupportSettings(totem,armor,consume,v); }
    public boolean enabled(String module) {
        return switch(module) {
            case "AutoTotem" -> totem.enabled();
            case "AutoArmor" -> armor.enabled();
            case "AutoConsume" -> consume.enabled();
            case "ElytraSwap" -> elytra.enabled();
            default -> throw new IllegalArgumentException("Unknown support module: "+module);
        };
    }
    public SupportSettings enabled(String module,boolean value) {
        return switch(module) {
            case "AutoTotem" -> withTotem(totem.enabled(value));
            case "AutoArmor" -> withArmor(armor.enabled(value));
            case "AutoConsume" -> withConsume(consume.enabled(value));
            case "ElytraSwap" -> withElytra(elytra.enabled(value));
            default -> throw new IllegalArgumentException("Unknown support module: "+module);
        };
    }
    /** Settings dialogs edit one module without changing its current on/off state. */
    public SupportSettings settingsFrom(String module,SupportSettings source) {
        Objects.requireNonNull(source);
        return switch(module) {
            case "AutoTotem" -> withTotem(source.totem.enabled(totem.enabled));
            case "AutoArmor" -> withArmor(source.armor.enabled(armor.enabled));
            case "AutoConsume" -> withConsume(source.consume.enabled(consume.enabled));
            case "ElytraSwap" -> withElytra(source.elytra.enabled(elytra.enabled));
            default -> throw new IllegalArgumentException("Unknown support module: "+module);
        };
    }
    public record Totem(boolean enabled, boolean healthOnly, double health, int delay, boolean hotbarFirst) {
        public static final Totem DEFAULT = new Totem(false,false,8,2,true);
        public Totem { decimal(health,1,20,"totem.health"); integer(delay,1,20,"totem.delay"); }
        public Totem enabled(boolean v) { return new Totem(v,healthOnly,health,delay,hotbarFirst); }
        public Totem healthOnly(boolean v) { return new Totem(enabled,v,health,delay,hotbarFirst); }
        public Totem health(double v) { return new Totem(enabled,healthOnly,v,delay,hotbarFirst); }
        public Totem delay(int v) { return new Totem(enabled,healthOnly,health,v,hotbarFirst); }
        public Totem hotbarFirst(boolean v) { return new Totem(enabled,healthOnly,health,delay,v); }
    }
    public record Armor(boolean enabled, int delay, boolean protection) {
        public static final Armor DEFAULT = new Armor(false,6,true);
        public Armor { integer(delay,1,40,"armor.delay"); }
        public Armor enabled(boolean v) { return new Armor(v,delay,protection); }
        public Armor delay(int v) { return new Armor(enabled,v,protection); }
        public Armor protection(boolean v) { return new Armor(enabled,delay,v); }
    }
    public record Consume(boolean enabled, boolean food, boolean potions, boolean gapples, boolean safeOnly,
                          double health, int hunger, double dangerRadius, boolean stopOnDamage, int delay) {
        // Valuable consumables require an explicit opt-in in the RMB menu.
        public static final Consume DEFAULT = new Consume(false,true,false,false,true,10,14,8,true,8);
        public Consume {
            decimal(health,1,20,"consume.health"); integer(hunger,1,20,"consume.hunger");
            decimal(dangerRadius,2,16,"consume.dangerRadius"); integer(delay,1,40,"consume.delay");
        }
        public Consume enabled(boolean v) { return new Consume(v,food,potions,gapples,safeOnly,health,hunger,dangerRadius,stopOnDamage,delay); }
        public Consume option(String key,boolean v) {
            return switch(key) {
                case "food" -> new Consume(enabled,v,potions,gapples,safeOnly,health,hunger,dangerRadius,stopOnDamage,delay);
                case "potions" -> new Consume(enabled,food,v,gapples,safeOnly,health,hunger,dangerRadius,stopOnDamage,delay);
                case "gapples" -> new Consume(enabled,food,potions,v,safeOnly,health,hunger,dangerRadius,stopOnDamage,delay);
                case "safeOnly" -> new Consume(enabled,food,potions,gapples,v,health,hunger,dangerRadius,stopOnDamage,delay);
                case "stopOnDamage" -> new Consume(enabled,food,potions,gapples,safeOnly,health,hunger,dangerRadius,v,delay);
                default -> throw new IllegalArgumentException("Unknown consumption option: "+key);
            };
        }
        public Consume health(double v) { return new Consume(enabled,food,potions,gapples,safeOnly,v,hunger,dangerRadius,stopOnDamage,delay); }
        public Consume hunger(int v) { return new Consume(enabled,food,potions,gapples,safeOnly,health,v,dangerRadius,stopOnDamage,delay); }
        public Consume dangerRadius(double v) { return new Consume(enabled,food,potions,gapples,safeOnly,health,hunger,v,stopOnDamage,delay); }
        public Consume delay(int v) { return new Consume(enabled,food,potions,gapples,safeOnly,health,hunger,dangerRadius,stopOnDamage,v); }
    }
    public record Elytra(boolean enabled, int delay, boolean protectFlight) {
        public static final Elytra DEFAULT = new Elytra(false,4,true);
        public Elytra { integer(delay,1,40,"elytra.delay"); }
        public Elytra enabled(boolean v) { return new Elytra(v,delay,protectFlight); }
        public Elytra delay(int v) { return new Elytra(enabled,v,protectFlight); }
        public Elytra protectFlight(boolean v) { return new Elytra(enabled,delay,v); }
    }
    private static void decimal(double v,double min,double max,String key) {
        if (!Double.isFinite(v)||v<min||v>max) throw new IllegalArgumentException(key);
    }
    private static void integer(int v,int min,int max,String key) {
        if (v<min||v>max) throw new IllegalArgumentException(key);
    }
    private static boolean flag(Properties p,String k,boolean fallback) {
        String v=p.getProperty(k,Boolean.toString(fallback)).strip();
        if (!v.equalsIgnoreCase("true")&&!v.equalsIgnoreCase("false")) throw new IllegalArgumentException(k);
        return Boolean.parseBoolean(v);
    }
    private static int number(Properties p,String k,int fallback) { return Integer.parseInt(p.getProperty(k,""+fallback).strip()); }
    private static double real(Properties p,String k,double fallback) { return Double.parseDouble(p.getProperty(k,""+fallback).strip()); }
    public static SupportSettings read(Properties p) {
        var t=Totem.DEFAULT; var a=Armor.DEFAULT; var c=Consume.DEFAULT; var e=Elytra.DEFAULT;
        return new SupportSettings(
            new Totem(flag(p,"support.totem.enabled",t.enabled),flag(p,"support.totem.healthOnly",t.healthOnly),
                real(p,"support.totem.health",t.health),number(p,"support.totem.delay",t.delay),flag(p,"support.totem.hotbarFirst",t.hotbarFirst)),
            new Armor(flag(p,"support.armor.enabled",a.enabled),number(p,"support.armor.delay",a.delay),flag(p,"support.armor.protection",a.protection)),
            new Consume(flag(p,"support.consume.enabled",c.enabled),flag(p,"support.consume.food",c.food),flag(p,"support.consume.potions",c.potions),
                flag(p,"support.consume.gapples",c.gapples),flag(p,"support.consume.safeOnly",c.safeOnly),real(p,"support.consume.health",c.health),
                number(p,"support.consume.hunger",c.hunger),real(p,"support.consume.dangerRadius",c.dangerRadius),
                flag(p,"support.consume.stopOnDamage",c.stopOnDamage),number(p,"support.consume.delay",c.delay)),
            new Elytra(flag(p,"support.elytra.enabled",e.enabled),number(p,"support.elytra.delay",e.delay),flag(p,"support.elytra.protectFlight",e.protectFlight))
        );
    }
    private static void put(Properties p,String k,Object v) { p.setProperty("support."+k,v.toString()); }
    public void write(Properties p) {
        put(p,"totem.enabled",totem.enabled); put(p,"totem.healthOnly",totem.healthOnly); put(p,"totem.health",totem.health);
        put(p,"totem.delay",totem.delay); put(p,"totem.hotbarFirst",totem.hotbarFirst);
        put(p,"armor.enabled",armor.enabled); put(p,"armor.delay",armor.delay); put(p,"armor.protection",armor.protection);
        put(p,"consume.enabled",consume.enabled); put(p,"consume.food",consume.food); put(p,"consume.potions",consume.potions);
        put(p,"consume.gapples",consume.gapples); put(p,"consume.safeOnly",consume.safeOnly); put(p,"consume.health",consume.health);
        put(p,"consume.hunger",consume.hunger); put(p,"consume.dangerRadius",consume.dangerRadius);
        put(p,"consume.stopOnDamage",consume.stopOnDamage); put(p,"consume.delay",consume.delay);
        put(p,"elytra.enabled",elytra.enabled); put(p,"elytra.delay",elytra.delay); put(p,"elytra.protectFlight",elytra.protectFlight);
    }
}