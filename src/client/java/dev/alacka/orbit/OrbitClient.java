package dev.alacka.orbit;

import dev.alacka.orbit.freelook.FreeLookController;
import dev.alacka.orbit.menu.OrbitMenu;
import dev.alacka.orbit.menu.ClickMenuController;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OrbitClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("orbit");
    @Override
    public void onInitializeClient() {
        FreeLookController.initialize();
        OrbitMenu.initialize();
        ClickMenuController.initialize();
        dev.alacka.orbit.modules.Trajectories.initialize();
        LOGGER.info("[Orbit] 0.4.6 Violet Glass menus, vanilla containers and HUD initialized; FreeLook and modules retained.");
    }
}
