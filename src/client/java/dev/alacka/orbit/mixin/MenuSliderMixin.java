package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rendering only: native dragging, keyboard controls and narration are retained. */
@Mixin(AbstractSliderButton.class)
public abstract class MenuSliderMixin {
    @Shadow protected double value;
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void orbit$slider(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!GlassMenu.active()) return;
        AbstractSliderButton self = (AbstractSliderButton) (Object) this;
        int x = self.getX(), y = self.getY(), width = self.getWidth(), height = self.getHeight();
        GlassMenu.panel(g, x, y, width, height, 8, 0xd3282034,
                self.isHoveredOrFocused() ? 0xc2be91ec : 0x546b557e);
        int end = x + 5 + (int) ((width - 10) * value);
        if (end > x + 5) g.fill(x + 5, y + height - 4, end, y + height - 2, dev.alacka.orbit.menu.UiTheme.tint(0x989f73d6, false));
        int thumb = x + 1 + (int) ((width - 8) * value);
        g.fill(thumb, y + height - 4, thumb + 6, y + height - 1, dev.alacka.orbit.menu.UiTheme.tint(0xffdfc4ff, false));
        var font = Minecraft.getInstance().font;
        String label = self.getMessage().getString();
        int available = Math.max(0, width - 12);
        if (font.width(label) > available) {
            while (!label.isEmpty() && font.width(label + "…") > available) label = label.substring(0, label.length() - 1);
            label += "…";
        }
        g.drawString(font, label, x + (width - font.width(label)) / 2,
                y + (height - 9) / 2, self.active ? 0xfff7efff : 0xffa295b1, false);
        if (self.isHovered()) g.requestCursor(CursorTypes.RESIZE_EW);
        ci.cancel();
    }
}
