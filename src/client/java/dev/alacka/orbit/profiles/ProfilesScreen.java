package dev.alacka.orbit.profiles;
import dev.alacka.orbit.menu.GlassMenu;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** List presentation; existing offline launch/restart semantics and safe storage are unchanged. */
public final class ProfilesScreen extends Screen {
    private final Screen parent;private ProfileStore.Data data;private int index,offset,rows;
    private String notice="",editText="";private EditBox input;
    private int left,top,panelWidth,panelHeight;private dev.alacka.orbit.menu.ProfileListLayout layout;
    public ProfilesScreen(Screen parent){super(Component.literal("Profiles"));this.parent=parent;}
    @Override protected void init(){
        try{data=ProfileStore.read(ProfileLaunch.path());}catch(Exception e){data=null;notice="Ошибка чтения; файл профилей сохранён без изменений.";}
        buildControls();
    }
    private String nameAt(int i){return i==0?ProfileStore.LAUNCHER:data.names().get(i-1);}
    private boolean persist(ProfileStore.Data next,String message){
        try{ProfileStore.write(ProfileLaunch.path(),next);data=next;notice=message;return true;}
        catch(IOException e){notice="Ошибка сохранения; существующий файл не изменён.";return false;}
    }
    private void buildControls(){
        if(input!=null)editText=input.getValue();input=null;clearWidgets();
        layout=dev.alacka.orbit.menu.ProfileListLayout.of(width,height);
        panelWidth=layout.width();panelHeight=layout.height();left=layout.x();top=layout.y();
        int x=left+12,inner=panelWidth-24;
        if(data!=null){
            index=Math.max(0,Math.min(index,data.names().size()));rows=layout.rows();
            offset=Math.max(0,Math.min(offset,Math.max(0,data.names().size()+1-rows)));
            if(index<offset)offset=index;if(index>=offset+rows)offset=index-rows+1;
            for(int i=offset;i<Math.min(data.names().size()+1,offset+rows);i++){
                int chosen=i;String key=nameAt(i),name=i==0?minecraft.getUser().getName()+" · Launcher":key;
                String status=ProfileLaunch.active().equals(key)?"Сейчас":data.selected().equals(key)?"След. запуск":"";
                addRenderableWidget(new ProfileRowButton(x,top+layout.rowY()+(i-offset)*layout.step(),inner,layout.rowHeight(),name,i==0,index==i,status,b->{index=chosen;buildControls();}));
            }
            if(data.names().size()+1>rows){
                addRenderableWidget(Button.builder(Component.literal("‹"),b->{index=Math.max(0,index-rows);offset=index;buildControls();}).bounds(left+panelWidth-62,top+10,22,20).build());
                addRenderableWidget(Button.builder(Component.literal("›"),b->{index=Math.min(data.names().size(),index+rows);offset=Math.max(0,index-rows+1);buildControls();}).bounds(left+panelWidth-36,top+10,22,20).build());
            }
            int editY=top+panelHeight-87;
            input=addRenderableWidget(new EditBox(font,x,editY,inner-142,22,Component.literal("Ник offline-профиля")));
            input.setMaxLength(16);input.setHint(Component.literal("Ник offline-профиля"));input.setValue(editText);
            addRenderableWidget(Button.builder(Component.literal("Добавить"),b->{
                try{var next=ProfileStore.add(data,input.getValue().trim());if(persist(next,"Профиль добавлен. Выбери его и перезапусти игру.")){index=data.names().size();editText="";input.setValue("");}}
                catch(IllegalArgumentException e){notice=e.getMessage();}buildControls();
            }).bounds(x+inner-136,editY,64,22).build());
            Button rename=addRenderableWidget(Button.builder(Component.literal("Изменить"),b->{
                try{persist(ProfileStore.rename(data,nameAt(index),input.getValue().trim()),"Имя сохранено. Применение — после перезапуска.");}catch(IllegalArgumentException e){notice=e.getMessage();}buildControls();
            }).bounds(x+inner-68,editY,68,22).build());rename.active=index>0;
            int third=(inner-12)/3,y=top+panelHeight-30;
            addRenderableWidget(Button.builder(Component.literal("Выбрать"),b->{persist(ProfileStore.select(data,nameAt(index)),"Сохранено. Перезапусти Minecraft для применения.");buildControls();}).bounds(x,y,third,22).build());
            Button delete=addRenderableWidget(Button.builder(Component.literal("Удалить"),b->{if(persist(ProfileStore.delete(data,nameAt(index)),"Профиль удалён."))index=Math.max(0,index-1);buildControls();}).bounds(x+third+6,y,third,22).build());delete.active=index>0;
            addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(x+2*(third+6),y,inner-2*(third+6),22).build());
        }else addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(x,top+panelHeight-30,inner,22).build());
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(data!=null&&x>=left&&x<left+panelWidth&&y>=top+45&&y<top+panelHeight-92){index=Math.max(0,Math.min(data.names().size(),index+(vertical<0?1:vertical>0?-1:0)));buildControls();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        renderBackground(g,mx,my,delta);GlassMenu.shadow(g,left,top,panelWidth,panelHeight);
        GlassMenu.panel(g,left,top,panelWidth,panelHeight,12,GlassMenu.WINDOW_FILL,GlassMenu.WINDOW_BORDER);
        GlassMenu.heading(g,"Profiles",left+12,top+12,0xffffffff);
        GlassMenu.text(g,"Выбор применяется после перезапуска игры",left+12,top+29,0xffe6d6f1);
        String line=notice.isEmpty()?"Offline — только одиночная игра и серверы без проверки аккаунтов.":notice;
        int y=top+panelHeight-59;
        for(var text:font.split(Component.literal(line),panelWidth-24)){if(y+9>top+panelHeight-34)break;g.drawString(font,text,left+12,y,0xffe9dbf5,false);y+=11;}
        super.render(g,mx,my,delta);
    }
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent);}
}
