package dev.alacka.orbit.mixin;
import dev.alacka.orbit.menu.GlassMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin({WorldSelectionList.class,ServerSelectionList.class})
public abstract class SelectionWidthMixin {
    @Inject(method="getRowWidth",at=@At("HEAD"),cancellable=true)
    private void orbit$wideCards(CallbackInfoReturnable<Integer> ci){
        if(GlassMenu.selectionScreen())ci.setReturnValue(Math.max(200,Math.min(516,Minecraft.getInstance().screen.width-64)));
    }
}
