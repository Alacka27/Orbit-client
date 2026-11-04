package dev.alacka.orbit.mixin;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.HudTheme;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
/** Keep native wrapping, fade, scroll, links and signatures. No multi-target field shadow/remap ambiguity. */
@Mixin(targets = {"net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess", "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"})
public abstract class ChatGlassMixin {
    @Redirect(method="fill",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void orbit$line(GuiGraphics graphics,int x1,int y1,int x2,int y2,int color){
        if(!HudTheme.enabled()||x2-x1<12||y2<=y1||(color&0xffffff)!=0){graphics.fill(x1,y1,x2,y2,color);return;}
        GlassMenu.panel(graphics,x1,y1,x2-x1,y2-y1,3,(color&0xff000000)|0x271a36,0x145e4774);
    }
}
