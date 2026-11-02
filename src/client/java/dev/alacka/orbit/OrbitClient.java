package dev.alacka.orbit;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Minimal entrypoint: proves compilation and provides a runtime smoke-test marker. */
public final class OrbitClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("orbit");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Orbit] Bootstrap initialized. Gameplay modules are NOT implemented yet.");
    }
}
