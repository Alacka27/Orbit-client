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
        LOGGER.info("[Orbit] 0.4.4 Transparent menus, RShift GUI and FreeLook initialized.");
    }
}
