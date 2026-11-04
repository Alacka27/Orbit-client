package dev.alacka.orbit.hud;

import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.config.HudPrefs;
import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.HudTheme;
import dev.alacka.orbit.menu.UiTheme;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag the actual visible HUD while the normal chat screen owns the cursor. */
public final class HudChatDrag {
    private String active;
    private UUID owner;
    private HudPrefs candidate;
    private int grabX,grabY,startX,startY,guideX=-1,guideY=-1;
    public boolean active(){return active!=null;}
    private boolean allowed(){
        Minecraft m=Minecraft.getInstance();
        return HudTheme.enabled()&&!m.options.hideGui&&!m.getDebugOverlay().showDebugScreen();
    }
    private String at(double x,double y){
        Minecraft m=Minecraft.getInstance();
        if(y>=m.getWindow().getGuiScaledHeight()-14)return null; // Native input always wins.
        var plates=HudElements.plates();
        for(int i=HudElements.IDS.size()-1;i>=0;i--){String id=HudElements.IDS.get(i);var rect=plates.get(id);if(rect!=null&&rect.contains(x,y))return id;}
        return null;
    }
    public boolean press(MouseButtonEvent e){
        if(!allowed())return false;
        String id=at(e.x(),e.y());if(id==null||e.button()!=0&&e.button()!=1)return false;
        if(e.button()==1){save(OrbitSettings.savedHud().withPlacement(id,null));return true;}
        var r=HudElements.plates().get(id);active=id;owner=Minecraft.getInstance().getUser().getProfileId();candidate=null;
        grabX=(int)Math.round(e.x())-r.x();grabY=(int)Math.round(e.y())-r.y();startX=r.x();startY=r.y();return true;
    }
    public boolean move(MouseButtonEvent e){
        if(active==null||e.button()!=0)return false;
        var r=HudElements.plates().get(active);if(r==null)return true;
        Minecraft m=Minecraft.getInstance();int sw=m.getWindow().getGuiScaledWidth(),sh=m.getWindow().getGuiScaledHeight();
        int x=(int)Math.round(e.x())-grabX,y=(int)Math.round(e.y())-grabY;
        if(candidate==null&&Math.abs(x-startX)+Math.abs(y-startY)<2)return true;
        if(e.hasShiftDown()){x=HudLayout.clamp(x,0,sw-r.width());y=HudLayout.clamp(y,0,sh-r.height());guideX=guideY=-1;}
        else{var snap=HudLayout.snap(x,y,r.width(),r.height(),sw,sh,6,5);x=snap.x();y=snap.y();guideX=snap.guideX();guideY=snap.guideY();}
        int safeY=HudLayout.clamp(y,0,sh-16-r.height());if(safeY!=y)guideY=-1;y=safeY;
        candidate=OrbitSettings.savedHud().withPlacement(active,HudLayout.capture(x,y,r.width(),r.height(),sw,sh));OrbitSettings.previewHud(candidate);return true;
    }
    public boolean release(MouseButtonEvent e){if(active==null||e.button()!=0)return false;move(e);finish();return true;}
    public void finish(){
        if(active==null)return;
        if(candidate!=null&&owner.equals(Minecraft.getInstance().getUser().getProfileId()))save(candidate);
        else OrbitSettings.cancelHudPreview();
        active=null;candidate=null;owner=null;guideX=guideY=-1;
    }
    private void save(HudPrefs next){
        try{OrbitSettings.saveHud(next);}
        catch(Exception e){
            OrbitSettings.cancelHudPreview();OrbitClient.LOGGER.error("[Orbit] HUD position not saved",e);
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Не удалось сохранить позицию HUD"));
        }
    }
    public void render(GuiGraphics g,int mx,int my){
        if(!allowed())return;
        int sw=g.guiWidth(),sh=g.guiHeight();g.enableScissor(0,0,sw,Math.max(0,sh-14));
        HudElements.renderInChat(g);
        String hovered=active!=null?active:at(mx,my);
        if(hovered!=null){var r=HudElements.plates().get(hovered);if(r!=null)GlassMenu.outline(g,r.x()-1,r.y()-1,r.width()+2,r.height()+2,4,0x99000000|(UiTheme.accent()&0xffffff));}
        if(active!=null){
            int c=0x66000000|(UiTheme.accent()&0xffffff);
            if(guideX>=0)g.fill(guideX,0,guideX+1,sh-14,c);
            if(guideY>=0)g.fill(0,guideY,sw,guideY+1,c);
        }
        g.disableScissor();
    }
}
