package dev.alacka.orbit.menu;

import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.freelook.FreeLookController;
import dev.alacka.orbit.profiles.ProfilesScreen;
import java.util.EnumMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/** All visible toggles act on existing modules; no placeholder switches. */
public final class OrbitClickScreen extends Screen {
    public enum Tab { General,Pvp,Visual,Fun,Settings }
    private final Screen parent; private Tab selected=Tab.General;
    private final EnumMap<Tab,Button> tabs=new EnumMap<>(Tab.class);
    private ClickMenuLayout box; private String notice="";
    public OrbitClickScreen(Screen parent){super(Component.literal("Orbit"));this.parent=parent;}
    @Override protected void init(){
        clearWidgets();tabs.clear();box=ClickMenuLayout.of(width,height);
        for(Tab tab:Tab.values())tabs.put(tab,addRenderableWidget(Button.builder(Component.literal(tab.name()),b->{selected=tab;notice="";init();})
                .bounds(box.x()+8,box.tabY(tab.ordinal()),box.sidebar()-16,box.tabHeight()).build()));
        int x=box.contentX(),w=box.contentWidth(),y=box.y()+54;
        int room=box.height()-106,card=Math.min(72,room),half=Math.min(68,(room-8)/2);
        if(selected==Tab.General){
            addRenderableWidget(new ModuleCard(x,y,w,card,"FreeLook","Свободный обзор без изменения направления движения. Клавиша: "+FreeLookController.keyLabel(),FreeLookController::enabled,b->{
                try{FreeLookController.setEnabled(!FreeLookController.enabled());notice="Сохранено";}catch(Exception e){notice="Не удалось сохранить настройки";}init();
            }));
        }else if(selected==Tab.Pvp){
            module("NoFall","Флаг земли при падении. Сервер может отклонить действие.",x,y,w,half);
            module("Jesus","Поверхность воды. Удерживай Shift, чтобы нырнуть.",x,y+half+8,w,half);
        }else if(selected==Tab.Visual){
            module("Trajectories","Прогноз полёта снаряда и метка первого столкновения.",x,y,w,card);
        }else if(selected==Tab.Settings){
            addRenderableWidget(Button.builder(Component.literal("Интерфейс · цвет и прозрачность"),b->minecraft.setScreen(new ThemeSettingsScreen(this))).bounds(x,y,w,26).build());
            addRenderableWidget(Button.builder(Component.literal("Настройки Minecraft"),b->minecraft.setScreen(new OptionsScreen(this,minecraft.options))).bounds(x,y+34,w,26).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Profiles"),b->minecraft.setScreen(new ProfilesScreen(this)))
                .bounds(box.x()+box.width()-92,box.y()+box.height()-32,80,22).build());
    }
    private void module(String name,String description,int x,int y,int w,int h){
        java.util.function.BooleanSupplier state=()->switch(name){case "NoFall"->OrbitSettings.get().noFall();case "Jesus"->OrbitSettings.get().jesus();default->OrbitSettings.get().trajectories();};
        addRenderableWidget(new ModuleCard(x,y,w,h,name,description,state,b->{
            try{OrbitSettings.save(OrbitSettings.get().module(name,!state.getAsBoolean()));notice="Сохранено";}catch(Exception e){notice="Ошибка сохранения; файл не изменён";}init();
        }));
    }
    public boolean selectedButton(AbstractButton button){return tabs.get(selected)==button;}
    private void lines(GuiGraphics g,String value,int y,int color){
        for(var line:font.split(Component.literal(value),box.contentWidth())){if(y+9>box.y()+box.height()-55)break;g.drawString(font,line,box.contentX(),y,color,false);y+=12;}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        renderBackground(g,mx,my,delta);
        GlassMenu.shadow(g,box.x(),box.y(),box.width(),box.height());
        GlassMenu.panel(g,box.x(),box.y(),box.width(),box.height(),12,GlassMenu.WINDOW_FILL,GlassMenu.WINDOW_BORDER);
        GlassMenu.panel(g,box.x()+1,box.y()+1,box.sidebar()-1,box.height()-2,10,0x451a1028,0x248765a4);
        GlassMenu.text(g,"O R B I T",box.x()+13,box.y()+13,0xffffffff);
        GlassMenu.heading(g,selected.name(),box.contentX(),box.y()+13,0xffffffff);
        GlassMenu.text(g,"Модули Orbit",box.contentX(),box.y()+29,0xffdecee9);
        int helper=box.y()+54+Math.min(72,box.height()-106)+10;
        if(selected==Tab.General&&box.height()>245)lines(g,"Удерживай Alt в виде от третьего лица сзади, чтобы осмотреться вокруг.",helper,0xffe3d5ef);
        if(selected==Tab.Visual&&box.height()>245){
            GlassMenu.panel(g,box.contentX(),helper,box.contentWidth(),47,5,0x60523c67,0x329a79b4);
            GlassMenu.text(g,"Поддерживаемые предметы",box.contentX()+8,helper+7,0xffe9dcf5);
            var items=new net.minecraft.world.item.Item[]{Items.BOW,Items.SNOWBALL,Items.EGG,Items.ENDER_PEARL,Items.SPLASH_POTION};
            for(int i=0;i<items.length;i++){int sx=box.contentX()+8+i*25;GlassMenu.panel(g,sx,helper+21,20,20,3,0x6d22182e,0x3b9375af);g.renderItem(new ItemStack(items[i]),sx+2,helper+23);}
        }
        if(selected==Tab.Fun){int cy=box.y()+box.height()/2;g.drawCenteredString(font,"Пока нет модулей",box.contentX()+box.contentWidth()/2,cy-12,0xfff5ecff);lines(g,"Новые функции появятся здесь позже.",cy+9,0xffd5c3e4);}
        if(selected==Tab.Settings&&box.height()>245)lines(g,"Личная тема сохраняется отдельно для текущего профиля. Esc отменяет неприменённые изменения.",box.y()+132,0xffe0d1ed);
        if(!notice.isEmpty())lines(g,notice,box.y()+box.height()-53,0xfff6eaff);
        g.fill(box.x()+9,box.footerY()-6,box.x()+box.sidebar()-9,box.footerY()-5,0x3b886ba4);
        PlayerIdentity.draw(g,box.x()+9,box.footerY()+3,box.sidebar()-18);
        GlassMenu.text(g,"Esc — назад",box.contentX(),box.y()+box.height()-24,0xffd5c2e5);
        super.render(g,mx,my,delta);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
