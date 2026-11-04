package dev.alacka.orbit.mixin;

import dev.alacka.orbit.hud.HudChatDrag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatHudMixin extends Screen {
    @Unique private final HudChatDrag orbit$hudDrag=new HudChatDrag();
    protected ChatHudMixin(Component title){super(title);}
    @Redirect(method="mouseClicked",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/CommandSuggestions;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;)Z"))
    private boolean orbit$hudClick(CommandSuggestions suggestions,MouseButtonEvent event){
        if(suggestions.mouseClicked(event))return true;
        boolean handled=orbit$hudDrag.press(event);
        if(handled&&event.button()==0)setDragging(true);
        return handled;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        return orbit$hudDrag.move(event)||super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event){
        if(orbit$hudDrag.release(event)){setDragging(false);return true;}
        return super.mouseReleased(event);
    }
    @Inject(method="render",at=@At("RETURN"))
    private void orbit$drawHud(GuiGraphics g,int mx,int my,float delta,CallbackInfo ci){orbit$hudDrag.render(g,mx,my);}
    @Inject(method="removed",at=@At("HEAD"))
    private void orbit$finishHud(CallbackInfo ci){orbit$hudDrag.finish();}
}
