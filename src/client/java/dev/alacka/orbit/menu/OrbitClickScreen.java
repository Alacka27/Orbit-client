package dev.alacka.orbit.menu;

import dev.alacka.orbit.config.OrbitSettings;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class OrbitClickScreen extends Screen implements OrbitWindow {
    private final Screen parent;
    private Tab selected = Tab.Pvp;
    private final EnumMap<Tab, Button> tabs = new EnumMap<>(Tab.class);
    private final EnumMap<Tab, Double> offsets = new EnumMap<>(Tab.class);
    private final List<ModuleCard> cards = new ArrayList<>();
    private final WindowDrag drag = new WindowDrag("click");
    private ClickMenuLayout box;
    private ModuleGrid grid;
    private int viewX, viewY, viewW, viewH;
    private String notice = "";
    private boolean scrolling;

    public OrbitClickScreen(Screen parent) { super(Component.literal("Orbit")); this.parent=parent; }

    @Override protected void init() {
        clearWidgets();tabs.clear();cards.clear();grid=null;scrolling=false;
        box=ClickMenuLayout.of(width,height,drag.dx(),drag.dy());
        updateViewport();
        for (Tab tab:Tab.values()) tabs.put(tab,addRenderableWidget(Button.builder(Component.literal(tab.name()),b->{
            selected=tab;notice="";init();
        }).bounds(box.x()+8,box.tabY(tab.ordinal()),box.sidebar()-16,box.tabHeight()).build()));
        String[] entries=switch(selected){
            case Pvp -> new String[]{"KillAura","TriggerBot","NoFall","Jesus","AutoTotem","AutoArmor","AutoConsume","ElytraSwap"};
            case Visual -> new String[]{"TargetHUD","DamageNumbers","HitParticles","CriticalHitEffect",
                    "ShieldStatus","TotemCounter","SeeInvisibles","Trajectories"};
            default -> new String[0];
        };
        if(entries.length>0){
            grid=ModuleGrid.of(Math.max(1,viewW-8),viewH,entries.length);
            offsets.put(selected,grid.clampScroll(offsets.getOrDefault(selected,0.0)));
            for(int i=0;i<entries.length;i++) module(entries[i],viewX+grid.x(i),viewY+grid.y(i)-(int)Math.round(scroll()),grid.cardWidth(),grid.cardHeight());
        } else if(selected==Tab.Settings){
            int h=Math.min(24,Math.max(18,(viewH-18)/4));
            addRenderableWidget(Button.builder(Component.literal("Тема и HUD"),b->minecraft.setScreen(new ThemeSettingsScreen(this)))
                    .bounds(viewX,viewY,viewW,h).build());
            addRenderableWidget(Button.builder(Component.literal("Управление Orbit"),b->minecraft.setScreen(new OrbitControlsScreen(this)))
                    .bounds(viewX,viewY+h+6,viewW,h).build());
            addRenderableWidget(Button.builder(Component.literal(chatLabel()),b->{
                try{OrbitSettings.save(OrbitSettings.get().withTransparentChat(!OrbitSettings.get().transparentChat()));b.setMessage(Component.literal(chatLabel()));}
                catch(Exception e){notice="Не удалось сохранить настройку";}
            }).bounds(viewX,viewY+2*(h+6),viewW,h).build());
            addRenderableWidget(Button.builder(Component.literal("Настройки Minecraft"),b->minecraft.setScreen(new OptionsScreen(this,minecraft.options)))
                    .bounds(viewX,viewY+3*(h+6),viewW,h).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Конфиги"),b->minecraft.setScreen(new ConfigsScreen(this)))
                .bounds(box.x()+box.width()-84,box.y()+box.height()-28,72,20).build());
    }
    private String chatLabel(){return "Прозрачный чат: "+(OrbitSettings.get().transparentChat()?"ВКЛ":"ВЫКЛ");}
    private void updateViewport(){
        viewX=box.contentX();viewY=box.y()+38;viewW=box.contentWidth();viewH=Math.max(1,box.height()-76);
        for(ModuleCard card:cards)card.viewport(viewX,viewY,viewW-7,viewH);
    }
    private double scroll(){return offsets.getOrDefault(selected,0.0);}
    private void scrollTo(double value){
        if(grid==null)return;
        int previous=(int)Math.round(scroll());double next=grid.clampScroll(value);offsets.put(selected,next);
        int dy=previous-(int)Math.round(next);for(ModuleCard card:cards)card.setY(card.getY()+dy);
    }
    private boolean inView(double x,double y){return x>=viewX&&x<viewX+viewW&&y>=viewY&&y<viewY+viewH;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(grid!=null&&inView(x,y)){scrollTo(scroll()-vertical*32);return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    private void module(String name,int x,int y,int w,int h){
        BooleanSupplier state=()->switch(name){
            case "NoFall" -> OrbitSettings.get().noFall();
            case "Jesus" -> OrbitSettings.get().jesus();
            case "Trajectories" -> OrbitSettings.get().trajectories();
            case "KillAura" -> OrbitSettings.get().combat().aura().enabled();
            case "TriggerBot" -> OrbitSettings.get().combat().trigger().enabled();
            default -> dev.alacka.orbit.config.VisualSettings.MODULES.contains(name)
                    ? OrbitSettings.get().visual().enabled(name) : OrbitSettings.get().support().enabled(name);
        };
        boolean combat=name.equals("KillAura")||name.equals("TriggerBot");
        boolean support=List.of("AutoTotem","AutoArmor","AutoConsume","ElytraSwap").contains(name);
        boolean visual=dev.alacka.orbit.config.VisualSettings.MODULES.contains(name)||name.equals("Trajectories");
        String help=ModuleHelp.text(name);
        Runnable settings=()->minecraft.setScreen(combat?new ModuleSettingsScreen(this,name.equals("KillAura"))
                :support?new SupportSettingsScreen(this,name):visual?new VisualSettingsScreen(this,name):new ModuleInfoScreen(this,name,help));
        ModuleCard card=new ModuleCard(x,y,w,h,name,state,b->{
            try{
                OrbitSettings.save(OrbitSettings.get().module(name,!state.getAsBoolean()));
                notice="";
            }catch(Exception e){notice="Ошибка сохранения; файл не изменён";}
            init();
        },settings).viewport(viewX,viewY,viewW-7,viewH);
        cards.add(addRenderableWidget(card));
    }
    @Override public boolean accent(AbstractButton button){return tabs.get(selected)==button;}
    public boolean selectedButton(AbstractButton button){return accent(button);}
    private int thumbHeight(){return Math.max(16,Math.min(viewH,viewH*viewH/Math.max(1,grid.contentHeight())));}
    private void scrollFromMouse(double y){scrollTo((y-viewY-thumbHeight()/2.0)/Math.max(1,viewH-thumbHeight())*grid.maxScroll());}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        if(e.button()==0&&grid!=null&&grid.maxScroll()>0&&e.x()>=viewX+viewW-6&&e.x()<viewX+viewW+2&&e.y()>=viewY&&e.y()<viewY+viewH){
            scrolling=true;scrollFromMouse(e.y());return true;
        }
        return drag.press(this,e,box.x(),box.y(),box.width())||super.mouseClicked(e,doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
        if(scrolling){scrollFromMouse(e.y());return true;}
        if(!drag.dragging())return super.mouseDragged(e,dx,dy);
        ClickMenuLayout next=ClickMenuLayout.of(width,height,drag.targetX(e.x()),drag.targetY(e.y()));
        ClickMenuLayout center=ClickMenuLayout.of(width,height);
        WindowDrag.shift(this,next.x()-box.x(),next.y()-box.y());drag.applied(next.x()-center.x(),next.y()-center.y());box=next;updateViewport();return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){
        if(scrolling){scrolling=false;return true;}
        if(drag.dragging()){mouseDragged(e,0,0);drag.release();return true;}
        return super.mouseReleased(e);
    }
    @Override public boolean keyPressed(KeyEvent e){
        if(grid!=null&&(e.key()==266||e.key()==267)){scrollTo(scroll()+(e.key()==267?1:-1)*Math.max(32,viewH-32));return true;}
        boolean handled=super.keyPressed(e);
        if(getFocused() instanceof ModuleCard c){
            if(c.getY()<viewY)scrollTo(scroll()+c.getY()-viewY);
            else if(c.getY()+c.getHeight()>viewY+viewH)scrollTo(scroll()+c.getY()+c.getHeight()-viewY-viewH);
        }
        return handled;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        GlassMenu.shadow(g,box.x(),box.y(),box.width(),box.height());
        GlassMenu.panel(g,box.x(),box.y(),box.width(),box.height(),10,GlassMenu.WINDOW_FILL,0x3a9979b4);
        GlassMenu.pill(g,box.x()+box.sidebar(),box.y()+12,1,box.height()-24,0,UiTheme.tint(0x249979b4,false));
        GlassMenu.text(g,"O R B I T",box.x()+12,box.y()+14,-1);
        GlassMenu.heading(g,selected.name(),viewX,box.y()+14,-1);
        drag.render(this,g,mx,my,box.x(),box.y(),box.width());
        PlayerIdentity.drawCompact(g,box.x()+9,box.y()+box.height()-24,box.sidebar()-17);
        if(selected==Tab.Fun)GlassMenu.text(g,"Пусто",viewX,viewY+8,0xffc7bdce);
        if(!notice.isEmpty())GlassMenu.text(g,font.plainSubstrByWidth(notice,Math.max(30,viewW-84)),viewX,box.y()+box.height()-23,0xffffc2cf);
        if(grid!=null&&grid.maxScroll()>0){
            int th=thumbHeight();int ty=viewY+(int)Math.round((viewH-th)*scroll()/grid.maxScroll());
            GlassMenu.pill(g,viewX+viewW-3,viewY,2,viewH,1,UiTheme.tint(0x289979b4,false));
            GlassMenu.pill(g,viewX+viewW-3,ty,2,th,1,UiTheme.tint(scrolling?0xffc9a5ed:0xb09979b4,false));
        }
        super.render(g,mx,my,delta);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    public enum Tab{Pvp,Visual,Fun,Settings}
}
