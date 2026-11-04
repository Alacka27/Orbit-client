package dev.alacka.orbit.menu;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
/** A real vanilla Button with a glass card and switch; keyboard/narration/callbacks stay native. */
public final class ModuleCard extends Button {
    private final String label, description;
    private final BooleanSupplier enabled;
    public ModuleCard(int x, int y, int w, int h, String label, String description, BooleanSupplier enabled, OnPress action) {
        super(x,y,w,h,Component.literal(label + ": " + (enabled.getAsBoolean() ? "ON" : "OFF") + ". " + description),action,DEFAULT_NARRATION);
        this.label=label;this.description=description;this.enabled=enabled;
    }
    @Override protected void renderContents(GuiGraphics g,int mx,int my,float delta) {
        boolean on=enabled.getAsBoolean(), hover=isHoveredOrFocused();
        GlassMenu.panel(g,getX(),getY(),getWidth(),getHeight(),6,hover?0x98694a80:0x7656406a,hover?0xafa982cb:0x4d9979b4);
        var font=Minecraft.getInstance().font;
        int titleY=getY()+(getHeight()<36?(getHeight()-9)/2:10);
        GlassMenu.heading(g,font.plainSubstrByWidth(label,getWidth()-52),getX()+10,titleY,0xfffcf7ff);
        int sx=getX()+getWidth()-32,sy=getY()+(getHeight()<36?(getHeight()-11)/2:10);
        GlassMenu.panel(g,sx,sy,22,11,5,on?0xf39e59de:0x986c5b7c,on?0xc9ca9df3:0x436e5d7e);
        GlassMenu.panel(g,sx+(on?12:1),sy+1,9,9,4,0xffffffff,0x30ffffff);
        if(getHeight()>=42) {
            int y=getY()+26;
            for(var line:font.split(Component.literal(description),getWidth()-22)) {
                if(y+9>getY()+getHeight()-5)break;
                g.drawString(font,line,getX()+10,y,0xffe4d7f0,false);y+=11;
            }
        }
    }
}
