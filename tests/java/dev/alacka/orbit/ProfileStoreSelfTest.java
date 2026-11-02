package dev.alacka.orbit;

import dev.alacka.orbit.profiles.ProfileStore;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ProfileStoreSelfTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("orbit-profile-test");
        Path file = root.resolve("profiles.properties");
        var data = ProfileStore.read(file);
        assert data.selected().equals(ProfileStore.LAUNCHER) && data.names().isEmpty();
        data = ProfileStore.add(data, "Player_123");
        data = ProfileStore.add(data, "launcher"); // Not confused with the launcher sentinel.
        try { ProfileStore.add(data, "player_123"); throw new AssertionError("duplicate accepted"); }
        catch (IllegalArgumentException expected) { }
        for (String invalid : new String[]{"ab", "invalid!", "12345678901234567", "русский"}) {
            try { ProfileStore.add(data, invalid); throw new AssertionError("invalid name accepted"); }
            catch (IllegalArgumentException expected) { }
        }
        data = ProfileStore.select(data, "launcher");
        data = ProfileStore.rename(data, "launcher", "New_Name");
        assert data.selected().equals("New_Name");
        data = ProfileStore.rename(data, "New_Name", "launcher");
        try { ProfileStore.rename(data, "launcher", "player_123"); throw new AssertionError("duplicate rename accepted"); }
        catch (IllegalArgumentException expected) { }
        ProfileStore.write(file, data);
        assert ProfileStore.read(file).equals(data);
        assert ProfileStore.delete(data, "launcher").selected().equals(ProfileStore.LAUNCHER);
        var content = Files.readString(file);
        assert !content.toLowerCase().contains("accesstoken");
        Files.writeString(file, "count=999\n");
        try { ProfileStore.read(file); throw new AssertionError("corrupt file accepted"); }
        catch (java.io.IOException expected) { }
        assert Files.readString(file).equals("count=999\n");
        System.out.println("ProfileStore tests passed");
    }
}
