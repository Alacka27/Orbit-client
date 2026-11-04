package dev.alacka.orbit.modules;

import java.util.EnumMap;

/** One complete item operation per tick; each module also retains its own cooldown. */
public final class SupportGate {
    public enum Action { TOTEM, ELYTRA, CONSUME, ARMOR }
    private final EnumMap<Action,Long> previous=new EnumMap<>(Action.class);
    private long lastTick=Long.MIN_VALUE;
    public boolean claim(long tick,Action action,int delay) {
        if (tick<0||action==null||delay<1) throw new IllegalArgumentException("action timing");
        if (tick==lastTick||tick<lastTick) return false;
        Long before=previous.get(action);
        if (before!=null&&tick-before<delay) return false;
        previous.put(action,tick); lastTick=tick; return true;
    }
    public void reset() { previous.clear(); lastTick=Long.MIN_VALUE; }
}