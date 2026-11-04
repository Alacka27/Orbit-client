package dev.alacka.orbit.accounts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Only a public application ID. Never stores account/password/token data. */
public final class AccountPreferences {
    private AccountPreferences() {}

    public static boolean validClientId(String id) {
        return id != null && id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    public static String read(Path path) throws IOException {
        Properties properties = load(path);
        String id = properties.getProperty("microsoft.clientId", "").trim();
        if (!id.isEmpty() && !validClientId(id)) throw new IOException("Invalid application ID");
        return id;
    }

    private static Properties load(Path path) throws IOException {
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { properties.load(reader); }
            catch (IllegalArgumentException error) { throw new IOException("Invalid account preferences"); }
        }
        return properties;
    }

    public static void write(Path path, String id) throws IOException {
        if (!id.isEmpty() && !validClientId(id)) throw new IOException("Invalid application ID");
        read(path); // A malformed existing file is preserved, not silently overwritten.
        Properties properties = load(path);
        if (id.isEmpty()) properties.remove("microsoft.clientId");
        else properties.setProperty("microsoft.clientId", id);
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, "accounts-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                properties.store(writer, "Orbit public Microsoft application ID; NO passwords or tokens");
            }
            try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}