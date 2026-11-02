package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Decoration only: scrolling, narration, selection and entry callbacks stay vanilla. */
@Mixin(AbstractSelectionList.class)
public abstract class MenuListMixin {
    @Inject(method = "renderListBackground", at = @At("HEAD"), cancellable = true)
    private void orbit$listBackground(GuiGraphics g, CallbackInfo ci) {
        if (GlassMenu.selectionScreen()) ci.cancel();
    }
    @Inject(method = "renderListSeparators", at = @At("HEAD"), cancellable = true)
    private void orbit$separators(GuiGraphics g, CallbackInfo ci) {
        if (GlassMenu.selectionScreen()) ci.cancel();
    }
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void orbit$card(GuiGraphics g, int mouseX, int mouseY, float delta,
                            @Coerce Object entry, CallbackInfo ci) {
        if (!GlassMenu.selectionScreen()) return;
        AbstractSelectionList<?> self = (AbstractSelectionList<?>) (Object) this;
        GlassMenu.card(g, (LayoutElement) entry, self.getSelected() == entry, ((GuiEventListener) entry).isMouseOver(mouseX, mouseY));
    }
    @Inject(method = "renderSelection", at = @At("HEAD"), cancellable = true)
    private void orbit$selection(GuiGraphics g, @Coerce Object entry, int color, CallbackInfo ci) {
        if (GlassMenu.selectionScreen()) ci.cancel();
    }
}
