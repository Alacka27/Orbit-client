package dev.alacka.orbit.menu;

import dev.alacka.orbit.config.OrbitSettings;

public final class UiTheme {
   private UiTheme() {
   }

   public static int tint(int argb, boolean surface) {
      return UiPalette.tint(argb, surface, OrbitSettings.theme());
   }

   public static int hudTint(int argb) {
      return UiPalette.tint(argb, true, OrbitSettings.theme(), OrbitSettings.hud().opacity());
   }

   public static int accent() {
      return 0xFF000000 | OrbitSettings.theme().accent();
   }

   public static int accentText() {
      return UiPalette.light(OrbitSettings.theme().accent(), 0.42F);
   }
}
