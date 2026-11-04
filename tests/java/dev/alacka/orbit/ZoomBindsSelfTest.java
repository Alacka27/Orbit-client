package dev.alacka.orbit;

import dev.alacka.orbit.config.*;
import dev.alacka.orbit.input.HoldGate;
import dev.alacka.orbit.input.PressQueue;
import dev.alacka.orbit.zoom.ZoomState;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class ZoomBindsSelfTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++; if (!value) throw new AssertionError(message);
    }
    private static void reject(Runnable action) {
        try { action.run(); throw new AssertionError("Invalid bind accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        bindings(); persistence(); holds(); zoom();
        System.out.println("PASS Zoom, bindings and one-shot input: " + checks + " assertions");
    }
    private static void bindings() {
        check(BindPrefs.MODULES.size() == 19, "every module has a binding");
        var mutable = new HashMap<String, Integer>(); mutable.put("Zoom", 67);
        var binds = new BindPrefs(mutable); mutable.put("Zoom", 66);
        check(binds.keys().get("Zoom") == 67, "binds are immutable");
        try { binds.keys().put("Zoom", 66); throw new AssertionError("mutable map"); }
        catch (UnsupportedOperationException expected) { checks++; }
        for (String name : BindPrefs.MODULES) for (int code : new int[]{-1, 32, 67, 265, 299, 342, 1000, 1007}) {
            if (!BindPrefs.allowed(name, code)) { reject(() -> binds.with(name, code)); continue; }
            var next = binds.with(name, code);
            check(next.keys().get(name) == code, "keyboard/mouse/none accepted");
            var p = new Properties(); next.write(p);
            check(BindPrefs.read(p).equals(next), "property codec");
            next.without(name).write(p);
            check(!p.containsKey("bind." + name), "reset removes stale override");
        }
        for (int code : new int[]{-2, 0, 1, 31, 40, 100, 255, 349, 999, 1008, Integer.MAX_VALUE})
            reject(() -> binds.with("Zoom", code));
        reject(() -> binds.with("Unknown", 67));
        reject(() -> new BindPrefs(Map.of("Unknown", 67)));
        check(BindPrefs.DEFAULT.without("Zoom").equals(BindPrefs.DEFAULT), "absent means internal default");
        check(!BindPrefs.DEFAULT.with("NoFall", -1).equals(BindPrefs.DEFAULT), "none differs from default");
    }
    private static void persistence() throws Exception {
        Path dir = Files.createTempDirectory("orbit-zoom-binds-");
        try {
            Path a = dir.resolve("profile-a.properties"), b = dir.resolve("profile-b.properties");
            check(ClientSettings.load(a).zoom(), "Zoom defaults on for new profile");
            check(ClientSettings.load(a).binds().equals(BindPrefs.DEFAULT), "internal defaults retained");
            var binds = BindPrefs.DEFAULT.with("Zoom", 1003).with("NoFall", 75).with("FreeLook", 72);
            var settings = ClientSettings.DEFAULT.withBinds(binds);
            for (String module : BindPrefs.MODULES) if (!BindPrefs.required(module))
                check(settings.module(module, true).binds().equals(binds), "module edit preserves binds");
            check(settings.theme(0x42b983, 50, 70, false).binds().equals(binds), "theme preserves binds");
            check(settings.combat(settings.combat()).binds().equals(binds), "combat preserves binds");
            check(settings.support(settings.support()).binds().equals(binds), "support preserves binds");
            check(settings.withTransparentChat(true).zoom(), "Zoom stays built-in");
            reject(() -> settings.module("Zoom", false));
            reject(() -> settings.module("FreeLook", false));
            var hud = HudPrefs.DEFAULT.withOpacity(80);
            ClientSettings.saveTogether(a, settings, hud);
            check(ClientSettings.load(a).equals(settings), "profile disk roundtrip");
            ClientSettings.saveTogether(a, null, hud.withOpacity(60));
            check(ClientSettings.load(a).equals(settings), "HUD save retains binds and Zoom");
            ClientSettings.DEFAULT.save(b);
            check(ClientSettings.load(b).binds().equals(BindPrefs.DEFAULT), "UUID profiles are isolated");
            var named = new OrbitConfig(settings, hud, true, 12);
            check(OrbitConfig.fromProperties(named.toProperties("test")).equals(named), "named-config roundtrip");
            var old = named.toProperties("0.4.12"); old.remove("module.Zoom");
            BindPrefs.MODULES.forEach(name -> old.remove("bind." + name));
            check(OrbitConfig.fromProperties(old).settings().zoom(), "old configs enable Zoom");
            check(OrbitConfig.fromProperties(old).settings().binds().equals(BindPrefs.DEFAULT), "old configs use defaults");
            var p = ClientSettings.read(a); p.remove("Zoom");
            BindPrefs.MODULES.forEach(name -> p.remove("bind." + name));
            ClientSettings.store(a, p, "legacy");
            check(ClientSettings.load(a).zoom(), "legacy settings load");
            p.setProperty("bind.Zoom", "broken"); ClientSettings.store(a, p, "malformed");
            byte[] bytes = Files.readAllBytes(a);
            try { settings.save(a); throw new AssertionError("malformed file overwritten"); }
            catch (IOException expected) { checks++; }
            check(Arrays.equals(bytes, Files.readAllBytes(a)), "invalid file preserved");
            p.setProperty("bind.Zoom", "1008"); ClientSettings.store(a, p, "malformed");
            try { ClientSettings.load(a); throw new AssertionError("invalid mouse accepted"); }
            catch (IOException expected) { checks++; }
            old.setProperty("module.Zoom", "perhaps");
            check(OrbitConfig.fromProperties(old).settings().zoom(), "obsolete Zoom flag cannot disable built-in");
        } finally {
            try (var paths = Files.walk(dir)) {
                for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
    }
    private static void holds() {
        var gate = new HoldGate();
        check(!gate.update(true, true), "held on entry is blocked");
        check(!gate.update(true, false), "release arms hold");
        check(gate.update(true, true), "new hold accepted");
        check(gate.update(true, true), "hold remains active");
        check(!gate.update(false, true), "menu/focus loss deactivates");
        check(!gate.update(true, true), "refocus while held does not resume");
        gate.update(true, false);
        check(gate.update(true, true), "press after release works");
        gate.reset();
        check(!gate.update(true, true), "identity/binding change re-arms");
        var queue = new PressQueue();
        queue.request(false); check(!queue.consume(true), "menus do not queue a swap");
        queue.request(true); check(queue.consume(true), "one action");
        check(!queue.consume(true), "not repeated");
        queue.request(true); check(!queue.consume(false), "ineligible action discarded");
        check(!queue.consume(true), "discarded action does not resume");
        queue.request(true); queue.clear(); check(!queue.consume(true), "context reset clears action");
    }
    private static void zoom() {
        var z = new ZoomState();
        check(z.multiplier() == 1 && z.selected() == 3, "default magnification");
        check(z.fov(70) == 70, "unzoomed FOV exact");
        double reference = 0;
        for (int fps : new int[]{15, 30, 60, 120, 240, 360}) {
            z.reset();
            for (int i = 0; i < fps; i++) z.update(true, 1.0 / fps);
            check(Math.abs(z.multiplier() - 3) < .001, "smooth zoom converges");
            if (reference == 0) reference = z.multiplier();
            check(Math.abs(reference - z.multiplier()) < .001, "frame independent");
            for (int fov = 30; fov <= 120; fov++) {
                double value = z.fov(fov);
                check(value > 0 && value < fov, "optical FOV finite and narrower");
            }
            for (int i = 0; i < fps; i++) z.update(false, 1.0 / fps);
            check(z.multiplier() == 1 && z.fov(70) == 70, "release restores exact FOV");
        }
        for (int i = 0; i < 100; i++) z.scroll(100);
        check(z.selected() == ZoomState.MAX, "upper scroll clamp");
        for (int i = 0; i < 100; i++) z.scroll(-100);
        check(z.selected() == ZoomState.MIN, "lower scroll clamp");
        z.scroll(Double.NaN); z.scroll(Double.POSITIVE_INFINITY);
        check(z.selected() == ZoomState.MIN, "invalid wheel values ignored");
        double before = z.multiplier();
        z.update(true, Double.NaN); z.update(true, -1);
        check(z.multiplier() == before, "invalid time ignored");
        for (int i = 0; i < 1000; i++) {
            z.scroll(i % 2 == 0 ? .125 : -.125);
            z.update(i % 3 != 0, .016);
            check(Double.isFinite(z.fov(70)) && z.multiplier() >= 1 && z.multiplier() <= 30, "stress finite/bounded");
        }
        z.reset(); check(z.fov(70) == 70 && z.selected() == 3, "full reset");
    }
}