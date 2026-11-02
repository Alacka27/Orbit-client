package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Checkbox.class)
public abstract class MenuCheckboxMixin {
    @Redirect(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
    private void orbit$checkbox(GuiGraphics g, RenderPipeline pipeline, Identifier sprite,
                                 int x, int y, int width, int height, int color) {
        if (!GlassMenu.active()) { g.blitSprite(pipeline, sprite, x, y, width, height, color); return; }
        Checkbox self = (Checkbox) (Object) this;
        GlassMenu.panel(g, x, y, width, height, 5,
                self.selected() ? 0xf17f4cba : 0xd0261c33,
                self.isHoveredOrFocused() ? 0xffd5b5ff : 0x897d629a);
        if (self.selected()) {
            int cx = x + width / 2, cy = y + height / 2;
            g.fill(cx - 4, cy, cx - 2, cy + 2, 0xfff9f1ff);
            g.fill(cx - 2, cy + 2, cx, cy + 4, 0xfff9f1ff);
            for (int i = 0; i < 4; i++) g.fill(cx + i, cy + 2 - i, cx + i + 2, cy + 4 - i, 0xfff9f1ff);
        }
    }
}
