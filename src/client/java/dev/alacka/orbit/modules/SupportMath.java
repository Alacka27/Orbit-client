package dev.alacka.orbit.modules;

/** Item-selection and slot rules independent of Minecraft, used by runtime and tests. */
public final class SupportMath {
    private SupportMath() {}
    public static int menuSlot(int inventoryIndex) {
        if (inventoryIndex<0||inventoryIndex>=36) throw new IllegalArgumentException("main inventory index");
        return inventoryIndex<9 ? inventoryIndex+36 : inventoryIndex;
    }
    public static int armorSlot(String equipmentSlot) {
        return switch(equipmentSlot) {
            case "HEAD" -> 5; case "CHEST" -> 6; case "LEGS" -> 7; case "FEET" -> 8;
            default -> throw new IllegalArgumentException("not a player armor slot");
        };
    }
    /** A documented general-purpose heuristic, not a prediction of every damage type. */
    public static double armorScore(double armor,double toughness,int protection,double durability,boolean preferProtection) {
        if (!Double.isFinite(armor)||!Double.isFinite(toughness)||!Double.isFinite(durability)
                ||armor<0||toughness<0||protection<0) return Double.NEGATIVE_INFINITY;
        return armor*1000+toughness*100+(preferProtection?protection*750:0)+Math.clamp(durability,0,1)*0.01;
    }
    public static boolean lowHealth(double health,double absorption,double threshold) {
        return Double.isFinite(health)&&Double.isFinite(absorption)&&Double.isFinite(threshold)
                &&health>0&&health+Math.max(0,absorption)<=threshold;
    }
}