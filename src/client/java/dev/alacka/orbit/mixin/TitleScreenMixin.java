package dev.alacka.orbit.mixin;

import dev.alacka.orbit.menu.OrbitMenu;
import dev.alacka.orbit.menu.ThemeSettingsScreen;
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

@Mixin({TitleScreen.class})
public abstract class TitleScreenMixin extends Screen {
   @Unique
   private OrbitMenu orbit$menu;

   protected TitleScreenMixin(Component title) {
      super(title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void orbit$installMenu(CallbackInfo ci) {
      this.orbit$menu = OrbitMenu.install(this);
      if (this.orbit$menu != null) {
         Button settings = Button.builder(Component.literal("Настройки Orbit"), button -> Minecraft.getInstance().setScreen(new ThemeSettingsScreen(this)))
            .bounds(12, this.height - 40, 114, 26)
            .build();
         this.addRenderableWidget(settings);
         this.orbit$menu.setSettings(settings);
         Button profiles = Button.builder(Component.literal("Profiles"), button -> Minecraft.getInstance().setScreen(new ProfilesScreen(this)))
            .bounds(this.width - 126, this.height - 40, 114, 26)
            .build();
         this.addRenderableWidget(profiles);
         this.orbit$menu.setProfiles(profiles);
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void orbit$renderMenu(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.orbit$menu != null) {
         this.orbit$menu.renderBackground(graphics);
         super.render(graphics, mouseX, mouseY, delta);
         ci.cancel();
      }
   }
}
