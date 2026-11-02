package dev.alacka.orbit.profiles;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Stores only offline names and the chosen launch mode; never credentials. */
public final class ProfileStore {
    public static final String LAUNCHER = "@launcher";
    private static final int MAX_PROFILES = 20;
    public record Data(List<String> names, String selected) {
        public Data { names = List.copyOf(names); }
    }
    private ProfileStore() {}
    public static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{3,16}");
    }
    public static Data read(Path path) throws IOException {
        if (!Files.exists(path)) return new Data(List.of(), LAUNCHER);
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { p.load(reader); }
        int count;
        try { count = Integer.parseInt(p.getProperty("count", "0")); }
        catch (NumberFormatException ex) { throw new IOException("Invalid profile count", ex); }
        if (count < 0 || count > MAX_PROFILES) throw new IOException("Invalid profile count");
        List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name = p.getProperty("profile." + i);
            if (!validName(name) || containsIgnoreCase(names, name)) throw new IOException("Invalid or duplicate profile");
            names.add(name);
        }
        String selected = p.getProperty("selected", LAUNCHER);
        if (!selected.equals(LAUNCHER) && !names.contains(selected)) throw new IOException("Invalid selected profile");
        return new Data(names, selected);
    }
    public static Data add(Data data, String name) {
        if (!validName(name)) throw new IllegalArgumentException("Nickname: 3-16 letters, digits or underscore");
        if (containsIgnoreCase(data.names(), name)) throw new IllegalArgumentException("Profile already exists");
        if (data.names().size() >= MAX_PROFILES) throw new IllegalArgumentException("Maximum 20 profiles");
        List<String> names = new ArrayList<>(data.names()); names.add(name);
        return new Data(names, data.selected());
    }
    public static Data rename(Data data, String oldName, String newName) {
        if (!data.names().contains(oldName)) throw new IllegalArgumentException("Unknown profile");
        if (!validName(newName)) throw new IllegalArgumentException("Nickname: 3-16 letters, digits or underscore");
        List<String> names = new ArrayList<>(data.names());
        int position = names.indexOf(oldName); names.remove(position);
        if (containsIgnoreCase(names, newName)) throw new IllegalArgumentException("Profile already exists");
        names.add(position, newName);
        return new Data(names, data.selected().equals(oldName) ? newName : data.selected());
    }
    public static Data delete(Data data, String name) {
        List<String> names = new ArrayList<>(data.names()); names.remove(name);
        return new Data(names, data.selected().equals(name) ? LAUNCHER : data.selected());
    }
    public static Data select(Data data, String name) {
        if (!name.equals(LAUNCHER) && !data.names().contains(name)) throw new IllegalArgumentException("Unknown profile");
        return new Data(data.names(), name);
    }
    public static void write(Path path, Data data) throws IOException {
        // Validate before replacing any existing file.
        if (data.names().size() > MAX_PROFILES) throw new IOException("Too many profiles");
        List<String> seen = new ArrayList<>();
        for (String name : data.names()) {
            if (!validName(name) || containsIgnoreCase(seen, name)) throw new IOException("Invalid profile");
            seen.add(name);
        }
        if (!data.selected().equals(LAUNCHER) && !seen.contains(data.selected())) throw new IOException("Invalid selection");
        Properties p = new Properties(); p.setProperty("count", Integer.toString(seen.size()));
        p.setProperty("selected", data.selected());
        for (int i = 0; i < seen.size(); i++) p.setProperty("profile." + i, seen.get(i));
        Path parent = path.toAbsolutePath().getParent(); Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, "profiles-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) { p.store(writer, "Orbit offline profiles; no passwords or tokens"); }
            try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    private static boolean containsIgnoreCase(List<String> values, String value) {
        return values.stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).equals(value.toLowerCase(Locale.ROOT)));
    }
}
