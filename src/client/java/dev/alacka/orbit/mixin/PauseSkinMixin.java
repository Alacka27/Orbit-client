package dev.alacka.orbit.mixin;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.OrbitClickScreen;
import dev.alacka.orbit.menu.PauseLayout;
import java.util.ArrayList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PauseScreen.class)
public abstract class PauseSkinMixin extends Screen {
    protected PauseSkinMixin(Component title){super(title);}
    @Inject(method="init",at=@At("TAIL"))
    private void orbit$layout(CallbackInfo ci){
        var self=(PauseScreen)(Object)this;
        if(!GlassMenu.active()||!self.showsPauseMenu())return;
        var buttons=new ArrayList<Button>();for(var child:children())if(child instanceof Button b&&b.visible)buttons.add(b);
        if(buttons.size()<2)return;
        Button orbit=addRenderableWidget(Button.builder(Component.literal("Меню Orbit"),b->minecraft.setScreen(new OrbitClickScreen(this))).bounds(0,0,100,20).build());
        var p=PauseLayout.of(width,height,buttons.size()+1);int x=p.x()+14,w=p.width()-28,y=p.y()+38,h=p.buttonHeight();
        var first=buttons.getFirst();first.setRectangle(w,h,x,y);y+=p.step();
        int middle=buttons.size()-2;
        for(int i=0;i<middle;i++){var b=buttons.get(i+1);int bw=(w-6)/2;b.setRectangle(bw,h,x+(i%2)*(bw+6),y+(i/2)*p.step());}
        y+=((middle+1)/2)*p.step();orbit.setRectangle(w,h,x,y);y+=p.step();
        buttons.getLast().setRectangle(w,h,x,y);
        for(var child:children())if(child instanceof StringWidget text)text.setPosition((width-text.getWidth())/2,p.y()+15);
    }
}
