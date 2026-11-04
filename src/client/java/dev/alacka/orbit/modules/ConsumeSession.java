package dev.alacka.orbit.modules;

/** Tracks only automation-owned state: manual hotbar changes are never undone. */
public final class ConsumeSession {
    public enum Stop { NONE, CONTEXT, SLOT, DANGER, DAMAGE, TIMEOUT, FINISHED }
    private boolean active;
    private int originalSlot,ownedSlot;
    private long start,deadline;
    private double lastHealth;
    public boolean active() { return active; }
    public int ownedSlot() { return ownedSlot; }
    public void begin(long tick,int original,int selected,int duration,double health) {
        if (active||tick<0||original<0||original>8||selected<0||selected>8||duration<1||!Double.isFinite(health)||health<=0)
            throw new IllegalArgumentException("consume session");
        active=true; originalSlot=original; ownedSlot=selected; start=tick; deadline=tick+duration+20; lastHealth=health;
    }
    public Stop observe(long tick,int selected,double health,boolean using,boolean context,boolean danger,boolean damaged,boolean stopOnDamage) {
        if (!active) return Stop.NONE;
        Stop reason= !context ? Stop.CONTEXT : selected!=ownedSlot ? Stop.SLOT : danger ? Stop.DANGER
                : stopOnDamage&&(damaged||!Double.isFinite(health)||health<lastHealth-0.0001) ? Stop.DAMAGE
                : tick>=deadline ? Stop.TIMEOUT : tick>start&&!using ? Stop.FINISHED : Stop.NONE;
        lastHealth=health; return reason;
    }
    public int restoreSlot(int current) { return active&&current==ownedSlot ? originalSlot : current; }
    public void clear() { active=false; }
}