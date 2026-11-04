package dev.alacka.orbit.mixin;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos") int orbit$left();
    @Accessor("topPos") int orbit$top();
    @Accessor("imageWidth") int orbit$imageWidth();
    @Accessor("imageHeight") int orbit$imageHeight();
}
