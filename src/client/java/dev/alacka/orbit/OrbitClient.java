package dev.alacka.orbit;

import dev.alacka.orbit.freelook.FreeLookController;
import dev.alacka.orbit.menu.OrbitMenu;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OrbitClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("orbit");
    @Override
    public void onInitializeClient() {
        FreeLookController.initialize();
        OrbitMenu.initialize();
        LOGGER.info("[Orbit] 0.4.3 Unified purple menus and FreeLook initialized.");
    }
}
