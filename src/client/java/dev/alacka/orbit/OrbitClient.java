package dev.alacka.orbit;

import dev.alacka.orbit.freelook.FreeLookController;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OrbitClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("orbit");

    @Override
    public void onInitializeClient() {
        FreeLookController.initialize();
        LOGGER.info("[Orbit] 0.2.0 FreeLook initialized. Hold Left Alt in third-person back view. Other modules are not implemented yet.");
    }
}
