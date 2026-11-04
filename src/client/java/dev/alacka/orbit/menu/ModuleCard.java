package dev.alacka.orbit.menu;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
/** A real vanilla Button with a glass card and switch; keyboard/narration/callbacks stay native. */
public final class ModuleCard extends Button {
    private final String label;
    private final BooleanSupplier enabled;
    private final Runnable settingsAction;
    private int clipX, clipY, clipWidth, clipHeight;
    public ModuleCard viewport(int x, int y, int w, int h) {
        clipX=x;clipY=y;clipWidth=w;clipHeight=h;return this;
    }
    private boolean withinClip(double x,double y) {
        return clipWidth==0 || x>=clipX && x<clipX+clipWidth && y>=clipY && y<clipY+clipHeight;
    }
    @Override public boolean isMouseOver(double x,double y) { return withinClip(x,y) && super.isMouseOver(x,y); }

    public ModuleCard(int x, int y, int w, int h, String label, BooleanSupplier enabled, OnPress action) {
        this(x,y,w,h,label,enabled,action,null);
    }
    public ModuleCard(int x, int y, int w, int h, String label, BooleanSupplier enabled, OnPress action, Runnable settingsAction) {
        super(x,y,w,h,Component.literal(label + ": " + (enabled.getAsBoolean() ? "ON" : "OFF")),action,DEFAULT_NARRATION);
        this.label=label;this.enabled=enabled;
        this.settingsAction=settingsAction;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (settingsAction != null && active && visible && isFocused()
                && (event.key() == 348 || event.key() == 299 && (event.modifiers() & 1) != 0)) {
            settingsAction.run(); return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!withinClip(event.x(),event.y())) return false;
        if (event.button()==1 && settingsAction!=null && active && visible && isMouseOver(event.x(),event.y())) {
            settingsAction.run();
            return true;
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override protected void renderContents(GuiGraphics g,int mx,int my,float delta) {
        if (clipWidth>0 && (getY()+getHeight()<=clipY || getY()>=clipY+clipHeight)) return;
        if (clipWidth>0) g.enableScissor(clipX,clipY,clipX+clipWidth,clipY+clipHeight);
        boolean on=enabled.getAsBoolean(), hover=isHoveredOrFocused() && (isFocused() || withinClip(mx,my));
        GlassMenu.panel(g,getX(),getY(),getWidth(),getHeight(),5,hover?0x8b49355e:0x57332745,hover?0x719979b4:0x189979b4);
        var font=Minecraft.getInstance().font;
        int titleY=getY()+(getHeight()-9)/2;
        GlassMenu.heading(g,font.plainSubstrByWidth(label,getWidth()-40),getX()+8,titleY,0xfffcf7ff);
        int sx=getX()+getWidth()-26,sy=getY()+(getHeight()-10)/2;
        GlassMenu.panel(g,sx,sy,18,10,5,on?0xf39e59de:0x986c5b7c,on?0xc9ca9df3:0x436e5d7e);
        GlassMenu.panel(g,sx+(on?9:1),sy+1,8,8,4,0xffffffff,0x30ffffff);
        if (clipWidth>0) g.disableScissor();
    }
}
