package dev.alacka.orbit.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/** Settings are read at startup. Existing files are never silently overwritten. */
public record FreeLookConfig(boolean enabled, double returnSpeed) {
    public static final FreeLookConfig DEFAULT = new FreeLookConfig(true, 12.0);

    public static FreeLookConfig load(Path file) throws IOException {
        if (!Files.exists(file)) {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Properties defaults = new Properties();
            defaults.setProperty("enabled", "true");
            defaults.setProperty("returnSpeed", "12.0");
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                defaults.store(writer, "Orbit FreeLook: returnSpeed 1..40, higher = faster. Keybind is saved in Minecraft controls.");
            }
            return DEFAULT;
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        String value = props.getProperty("enabled", "true").strip();
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IOException("enabled must be true or false; config preserved: " + file);
        }
        double speed;
        try {
            speed = Double.parseDouble(props.getProperty("returnSpeed", "12.0").strip());
        } catch (NumberFormatException error) {
            throw new IOException("returnSpeed must be a number; config preserved: " + file, error);
        }
        if (!Double.isFinite(speed) || speed < 1 || speed > 40) {
            throw new IOException("returnSpeed must be between 1 and 40; config preserved: " + file);
        }
        return new FreeLookConfig(Boolean.parseBoolean(value), speed);
    }
}
