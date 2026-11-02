package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.TabButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TabButton.class)
public abstract class MenuTabMixin {
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void orbit$tab(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!GlassMenu.active()) return;
        TabButton self = (TabButton) (Object) this;
        GlassMenu.panel(g, self.getX() + 2, self.getY() + 3, self.getWidth() - 4, self.getHeight() - 4,
                6, self.isSelected() ? 0xe57d4bb3 : 0xc42b2038,
                self.isSelected() ? 0xddca9cf5 : self.isHoveredOrFocused() ? 0xaaa885ca : 0x506f5488);
        var font = Minecraft.getInstance().font;
        var label = self.getMessage();
        g.drawString(font, label, self.getX() + (self.getWidth() - font.width(label)) / 2,
                self.getY() + (self.getHeight() - 8) / 2, self.active ? 0xfff5edff : 0xff9f8fad, false);
        ci.cancel();
    }
}
