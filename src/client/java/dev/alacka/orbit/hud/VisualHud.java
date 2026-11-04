package dev.alacka.orbit.hud;

import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.HudTheme;
import dev.alacka.orbit.menu.UiTheme;
import dev.alacka.orbit.modules.VisualModules;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;

/** Small movable panels and projected labels, rendered from immutable extraction snapshots. */
public final class VisualHud {
    public static final int TARGET_WIDTH = 160, TARGET_HEIGHT = 76, INFO_WIDTH = 140;
    private VisualHud() {}

    private static void text(GuiGraphics g, Font font, String value, int x, int y, int width, int color, boolean bold) {
        Component text = bold ? HudTheme.boldText(value) : HudTheme.text(value);
        g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(text, width)), x, y, color, false);
    }
    public static void target(GuiGraphics g, Font font, VisualModules.Target target, int x, int y) {
        GlassMenu.hudPanel(g,x,y,TARGET_WIDTH,TARGET_HEIGHT,6,0xba251c32,0x609979b4);
        if (target.skin() != null) PlayerFaceRenderer.draw(g,target.skin(),x+8,y+8,24);
        else {
            GlassMenu.pill(g,x+8,y+8,24,24,4,0x509979b4);
            String initial = target.name().isEmpty() ? "?" : new String(Character.toChars(target.name().codePointAt(0))).toUpperCase(java.util.Locale.ROOT);
            Component letter = HudTheme.boldText(initial);
            g.drawString(font,letter,x+20-font.width(letter)/2,y+16,0xffefddff,false);
        }
        text(g,font,target.name(),x+40,y+8,112,0xfffbf6ff,true);
        text(g,font,target.preview() ? "Перетаскивай в чате"
                : String.format(java.util.Locale.ROOT,"%.1f м · броня %d",target.distance(),target.armor()),
                x+40,y+22,112,0xffd2bfdc,false);
        var health = target.health();
        String hp = !health.known() ? "HP: нет данных"
                : "HP: " + VisualModules.number(health.value())
                + (health.maximum()>0 ? " / " + VisualModules.number(health.maximum()) : " · scoreboard")
                + (health.absorption()>0 ? " +" + VisualModules.number(health.absorption()) : "");
        text(g,font,hp,x+8,y+38,144,health.known()?0xfff6eafa:0xffbdb0c8,false);
        GlassMenu.pill(g,x+8,y+51,144,4,2,0x50433a50);
        if (health.known() && health.maximum()>0) {
            float progress=(float)Math.clamp(health.value()/health.maximum(),0,1);
            if (progress>0) GlassMenu.pill(g,x+8,y+51,Math.max(2,144*progress),4,2,UiTheme.accent());
        }
        String footer = target.preview() ? "Нет выбранной цели"
                : OrbitSettings.get().visual().enabled("TotemCounter") ? "Тотемы: " + target.pops() + " срабатываний"
                : !target.shield().isEmpty() && OrbitSettings.get().visual().enabled("ShieldStatus") ? target.shield()
                : health.scoreboard() ? "HP переданы health-scoreboard" : "Наблюдаемые данные";
        text(g,font,footer,x+8,y+62,144,0xffc5b3d2,false);
    }
    public static int infoHeight() {
        var settings=OrbitSettings.get().visual();
        return 10 + (settings.enabled("ShieldStatus")?26:0) + (settings.enabled("TotemCounter")?13:0);
    }
    public static void info(GuiGraphics g, Font font, VisualModules.Frame frame, int x, int y) {
        GlassMenu.hudPanel(g,x,y,INFO_WIDTH,infoHeight(),5,0xba251c32,0x509979b4);
        int row=y+6;
        if (OrbitSettings.get().visual().enabled("ShieldStatus")) {
            var shield=frame.shield();
            String state=!shield.present()?"Щит: нет в руках":shield.cooldown()>0
                    ?"Щит: откат "+Math.round(shield.cooldown()*100)+"%":shield.blocking()?"Щит: блокирует":"Щит: готов";
            text(g,font,state,x+7,row,126,0xffe4d2f3,true);
            text(g,font,shield.present()?"Прочность "+shield.remaining()+"/"+shield.maximum():"Данные своего щита",
                    x+7,row+13,126,0xffc5b3d2,false);
            row+=26;
        }
        if (OrbitSettings.get().visual().enabled("TotemCounter"))
            text(g,font,"Тотемы: "+frame.ownTotems()+" · сраб. "+frame.selfPops(),x+7,row,126,0xffffdc91,false);
    }
    public static void labels(GuiGraphics g, Font font, VisualModules.Frame frame) {
        for (var label:frame.labels()) {
            var component=HudTheme.boldText(label.text());
            int width=font.width(component);
            float scale=label.scale(), half=width*scale/2;
            // Never let a projected label overflow the window when its point reaches an edge.
            if (width*scale>g.guiWidth()-12) continue;
            float x=Math.clamp(label.x(),half+6,g.guiWidth()-half-6);
            float y=Math.clamp(label.y(),4,g.guiHeight()-12*scale);
            g.pose().pushMatrix();
            g.pose().translate(x,y);g.pose().scale(scale,scale);
            g.drawString(font,component,-width/2,0,label.color(),true);
            g.pose().popMatrix();
        }
    }
}