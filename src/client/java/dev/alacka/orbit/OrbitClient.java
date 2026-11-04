package dev.alacka.orbit;

import dev.alacka.orbit.freelook.FreeLookController;
import dev.alacka.orbit.menu.ClickMenuController;
import dev.alacka.orbit.menu.OrbitMenu;
import dev.alacka.orbit.modules.Trajectories;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OrbitClient implements ClientModInitializer {
   public static final Logger LOGGER = LoggerFactory.getLogger("orbit");
   private static String version;

   public static String version() {
      if (version == null) {
         String raw = FabricLoader.getInstance().getModContainer("orbit").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
         int dash = raw.indexOf(45);
         version = dash > 0 ? raw.substring(0, dash) : raw;
      }

      return version;
   }

   public void onInitializeClient() {
      dev.alacka.orbit.accounts.AccountSessions.initialize();
      FreeLookController.initialize();
      dev.alacka.orbit.zoom.ZoomController.initialize();
      dev.alacka.orbit.input.ModuleBinds.initialize();
      OrbitMenu.initialize();
      dev.alacka.orbit.menu.ThemeBackdrop.initialize();
      ClickMenuController.initialize();
      Trajectories.initialize();
      dev.alacka.orbit.modules.InventoryModules.initialize();
      dev.alacka.orbit.modules.CombatModules.initialize();
      dev.alacka.orbit.modules.VisualModules.initialize();
      LOGGER.info("[Orbit] {} Violet Glass UI; visual/combat/item modules opt-in; built-in controls are internal.", version());
   }
}
