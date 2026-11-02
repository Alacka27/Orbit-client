package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.OrbitMenu;
import dev.alacka.orbit.profiles.ProfilesScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    @Unique private OrbitMenu orbit$menu;
    protected TitleScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void orbit$installMenu(CallbackInfo ci) {
        orbit$menu = OrbitMenu.install(this);
        if (orbit$menu != null) {
            Button fallback = Button.builder(Component.literal("Minecraft menu"), button -> {
                OrbitMenu.useVanillaForSession();
                Minecraft.getInstance().setScreen(new TitleScreen());
            }).bounds(12, height - 30, 104, 16).build();
            addRenderableWidget(fallback);
            orbit$menu.setFallback(fallback);
            Button profiles = Button.builder(Component.literal("Profiles"), button ->
                    Minecraft.getInstance().setScreen(new ProfilesScreen(this)))
                    .bounds(width - 116, height - 30, 104, 16).build();
            addRenderableWidget(profiles);
            orbit$menu.setProfiles(profiles);
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void orbit$renderMenu(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (orbit$menu == null) return;
        orbit$menu.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        ci.cancel();
    }
}
