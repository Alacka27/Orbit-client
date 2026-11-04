package dev.alacka.orbit.mixin;
import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementSkinMixin {
    @Inject(method="renderInside",at=@At("HEAD"))
    private void orbit$treeWindow(GuiGraphics g,int x,int y,CallbackInfo ci){
        if(!GlassMenu.active())return;
        GlassMenu.shadow(g,x,y,252,140);
        GlassMenu.panel(g,x,y,252,140,8,GlassMenu.WINDOW_FILL,GlassMenu.WINDOW_BORDER);
    }
}
