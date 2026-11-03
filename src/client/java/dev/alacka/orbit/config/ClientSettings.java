package dev.alacka.orbit.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

/** Local preferences only: UUID-named files never contain account credentials. */
public record ClientSettings(int accent, int windowOpacity, int buttonOpacity, boolean animations,
                             boolean noFall, boolean jesus, boolean trajectories) {
    public static final ClientSettings DEFAULT = new ClientSettings(0x945cda, 18, 60, true, false, false, false);
    public ClientSettings {
        if (accent < 0 || accent > 0xffffff || windowOpacity < 0 || windowOpacity > 85
                || buttonOpacity < 35 || buttonOpacity > 95) throw new IllegalArgumentException("Invalid theme values");
    }
    public ClientSettings theme(int color, int window, int button, boolean motion) {
        return new ClientSettings(color, window, button, motion, noFall, jesus, trajectories);
    }
    public ClientSettings module(String name, boolean enabled) {
        return switch (name) {
            case "NoFall" -> new ClientSettings(accent, windowOpacity, buttonOpacity, animations, enabled, jesus, trajectories);
            case "Jesus" -> new ClientSettings(accent, windowOpacity, buttonOpacity, animations, noFall, enabled, trajectories);
            case "Trajectories" -> new ClientSettings(accent, windowOpacity, buttonOpacity, animations, noFall, jesus, enabled);
            default -> throw new IllegalArgumentException("Unknown module: " + name);
        };
    }
    public static ClientSettings load(Path file) throws IOException {
        if (!Files.exists(file)) return DEFAULT;
        Properties p = read(file);
        try {
            String color = p.getProperty("accent", "945cda").strip();
            if (!color.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("accent must be 6 hex digits");
            return new ClientSettings(Integer.parseInt(color, 16), Integer.parseInt(p.getProperty("windowOpacity", "18")),
                    Integer.parseInt(p.getProperty("buttonOpacity", "60")), bool(p,"animations",true),
                    bool(p,"NoFall",false), bool(p,"Jesus",false), bool(p,"Trajectories",false));
        } catch (IllegalArgumentException e) { throw new IOException("Invalid settings; existing file preserved", e); }
    }
    private static boolean bool(Properties p, String key, boolean fallback) {
        String s = p.getProperty(key, Boolean.toString(fallback)).strip();
        if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false")) throw new IllegalArgumentException(key);
        return Boolean.parseBoolean(s);
    }
    private static Properties read(Path file) throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { p.load(r); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid properties; file preserved", e); }
        return p;
    }
    public void save(Path file) throws IOException {
        Properties p = new Properties();
        if (Files.exists(file)) { load(file); p = read(file); }
        p.setProperty("accent", String.format("%06x", accent));
        p.setProperty("windowOpacity", "" + windowOpacity); p.setProperty("buttonOpacity", "" + buttonOpacity);
        p.setProperty("animations", "" + animations); p.setProperty("NoFall", "" + noFall);
        p.setProperty("Jesus", "" + jesus); p.setProperty("Trajectories", "" + trajectories);
        Path dir = file.toAbsolutePath().getParent(); Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, "settings-", ".tmp");
        try {
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) { p.store(w, "Orbit local per-profile preferences. No credentials."); }
            try { Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }
}
