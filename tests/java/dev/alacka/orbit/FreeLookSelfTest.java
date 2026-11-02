package dev.alacka.orbit;

import dev.alacka.orbit.config.FreeLookConfig;
import dev.alacka.orbit.freelook.FreeLookState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Dependency-free tests. These cannot validate Mixin application or rendering in Minecraft. */
public final class FreeLookSelfTest {
    private static int assertions;
    private static void check(boolean value, String label) {
        assertions++;
        if (!value) throw new AssertionError(label);
    }
    private static void near(double actual, double expected, double tolerance, String label) {
        check(Math.abs(actual - expected) <= tolerance, label + ": " + actual + " != " + expected);
    }
    private static FreeLookState turned() {
        FreeLookState state = new FreeLookState();
        state.update(true, 0, 12);
        state.turn(600, 200, 0);
        return state;
    }
    public static void main(String[] args) throws Exception {
        FreeLookState state = new FreeLookState();
        check(!state.isActive(), "Starts inactive");
        state.turn(100, 100, 0);
        near(state.cameraYaw(10), 10, 0, "Inactive input ignored");
        state.update(true, 0, 12);
        state.turn(100, 100, 0);
        check(state.isHeld(), "Hold activates");
        near(state.cameraYaw(30), 45, 0.001, "Yaw offset and sensitivity");
        near(state.cameraPitch(0), 15, 0.001, "Pitch sensitivity");
        state.turn(0, 100000, 0);
        near(state.cameraPitch(0), 90, 0, "Upper pitch limit");
        state.turn(0, -100000, 0);
        near(state.cameraPitch(0), -90, 0, "Lower pitch limit");
        state.reset();
        near(state.cameraYaw(123), 123, 0, "Reset restores original yaw");
        near(state.cameraPitch(42), 42, 0, "Reset restores original pitch");
        check(!state.isActive(), "Reset ends state");

        state = new FreeLookState();
        state.update(true, 0, 12);
        state.turn(1600, 0, 0); // +240 degrees => shortest offset -120 degrees
        near(state.cameraYaw(0), -120, 0.001, "Yaw wraps to shortest return");
        state.update(false, 0.05, 12);
        check(state.phase() == FreeLookState.Phase.RETURNING, "Release starts return");
        check(state.cameraYaw(0) > -120 && state.cameraYaw(0) < 0, "Return moves toward back");
        double beforeRehold = state.cameraYaw(0);
        state.update(true, 0.1, 12);
        near(state.cameraYaw(0), beforeRehold, 0.001, "Rehold does not snap");
        state.turn(Double.NaN, 0, 0);
        near(state.cameraYaw(0), beforeRehold, 0.001, "Bad mouse values ignored");
        for (int i = 0; i < 200; i++) state.update(false, 0.016, 12);
        check(!state.isActive(), "Return completes exactly");
        near(state.cameraYaw(75), 75, 0, "Return follows current player direction");

        FreeLookState a = turned();
        FreeLookState b = turned();
        for (int i = 0; i < 30; i++) a.update(false, 1.0 / 60.0, 12);
        for (int i = 0; i < 60; i++) b.update(false, 1.0 / 120.0, 12);
        near(a.cameraYaw(0), b.cameraYaw(0), 0.00001, "Return independent of frame rate");
        near(a.cameraPitch(0), b.cameraPitch(0), 0.00001, "Pitch return independent of frame rate");
        double offset = a.cameraYaw(0);
        a.update(false, -1, 12);
        near(a.cameraYaw(0), offset, 0.00001, "Negative time ignored");
        a.reset();
        check(!a.isActive(), "Forced reset clears return");

        Path directory = Files.createTempDirectory("orbit-config-test-");
        try {
            Path config = directory.resolve("orbit/freelook.properties");
            FreeLookConfig defaults = FreeLookConfig.load(config);
            check(defaults.enabled(), "Config enabled by default");
            near(defaults.returnSpeed(), 12, 0, "Default return speed");
            check(Files.exists(config), "Config saved on first load");
            String custom = "enabled=false\nreturnSpeed=7.5\n";
            Files.writeString(config, custom);
            FreeLookConfig saved = FreeLookConfig.load(config);
            check(!saved.enabled(), "Disabled setting reloads");
            near(saved.returnSpeed(), 7.5, 0, "Custom speed reloads");
            check(Files.readString(config).equals(custom), "Valid config not overwritten");
            String[] invalid = {"NaN", "Infinity", "0", "41", "oops"};
            for (String invalidSpeed : invalid) {
                String invalidConfig = "enabled=true\nreturnSpeed=" + invalidSpeed + "\n";
                Files.writeString(config, invalidConfig);
                try {
                    FreeLookConfig.load(config);
                    throw new AssertionError("Accepted invalid speed: " + invalidSpeed);
                } catch (java.io.IOException expected) {
                    check(Files.readString(config).equals(invalidConfig), "Bad config preserved: " + invalidSpeed);
                }
            }
            Files.writeString(config, "enabled=maybe\nreturnSpeed=12\n");
            try {
                FreeLookConfig.load(config);
                throw new AssertionError("Accepted invalid boolean");
            } catch (java.io.IOException expected) { check(true, "Bad boolean rejected"); }
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("PASS: " + assertions + " FreeLook state/config checks. Minecraft integration not tested here.");
    }
}
