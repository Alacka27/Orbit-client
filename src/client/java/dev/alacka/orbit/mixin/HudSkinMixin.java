package dev.alacka.orbit.mixin;
import dev.alacka.orbit.menu.HudTheme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
public abstract class HudSkinMixin {
    @Unique private boolean orbit$hotbarScaled;
    @Inject(method="renderHotbarAndDecorations",at=@At("HEAD"))
    private void orbit$hotbarScale(GuiGraphics g,DeltaTracker delta,CallbackInfo ci){
        orbit$hotbarScaled=HudTheme.enabled();if(!orbit$hotbarScaled)return;
        float scale=(float)Math.min(1.45,Math.max(1,(g.guiWidth()-70)/390.0));
        g.pose().pushMatrix();g.pose().translate(g.guiWidth()/2f,g.guiHeight());g.pose().scale(scale,scale);g.pose().translate(-g.guiWidth()/2f,-g.guiHeight());
    }
    @Inject(method="renderHotbarAndDecorations",at=@At("RETURN"))
    private void orbit$hotbarPop(GuiGraphics g,DeltaTracker delta,CallbackInfo ci){if(orbit$hotbarScaled)g.pose().popMatrix();}
    @Inject(method="render",at=@At("TAIL"))
    private void orbit$status(GuiGraphics g,DeltaTracker delta,CallbackInfo ci){HudTheme.render(g);}
}
