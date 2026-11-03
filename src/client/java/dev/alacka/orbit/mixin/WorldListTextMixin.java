package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.GlassMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Lift only vanilla neutral-gray labels; warning/error component styles are retained. */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.WorldSelectionList$WorldListEntry")
public abstract class WorldListTextMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = -8355712))
    private int orbit$readableGray(int original) {
        return GlassMenu.active() ? 0xffeee4f8 : original;
    }
}
