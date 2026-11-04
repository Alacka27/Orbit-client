package dev.alacka.orbit.mixin;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.HudTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(PlayerTabOverlay.class)
public abstract class TabGlassMixin {
    @Unique private boolean orbit$glass, orbit$positioned;
    @Inject(method="render",at=@At("HEAD"))
    private void orbit$begin(GuiGraphics g,int width,Scoreboard scores,Objective objective,CallbackInfo ci){
        orbit$glass=HudTheme.enabled();orbit$positioned=false;if(orbit$glass)g.pose().pushMatrix();
    }
    @Inject(method="render",at=@At("RETURN"))
    private void orbit$end(GuiGraphics g,int width,Scoreboard scores,Objective objective,CallbackInfo ci){if(orbit$glass)g.pose().popMatrix();}

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void orbit$rows(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        if (!HudTheme.enabled()) { g.fill(x1, y1, x2, y2, color); return; }
        if (!orbit$positioned) {
            g.pose().translate(Math.max(0,g.guiWidth()-8-x2),22);orbit$positioned=true;
        }
        boolean outer = color == Integer.MIN_VALUE;
        GlassMenu.panel(g, x1, y1, x2 - x1, y2 - y1, outer ? 4 : 2,
                outer ? 0xd02a1d3a : 0x784b365d, outer ? 0x689d79ba : 0x1e9b7bb4);
    }
}
