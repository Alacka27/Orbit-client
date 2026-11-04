package dev.alacka.orbit.config;

import dev.alacka.orbit.hud.HudLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public record HudPrefs(
   int opacity, boolean status, boolean coords, boolean modules, Map<String, HudLayout.Placement> placements, Map<String, HudPrefs.Offset> windows
) {
   public static final int MIN_OPACITY = 20;
   public static final int MAX_OPACITY = 95;
   public static final HudPrefs DEFAULT = new HudPrefs(70, true, true, true, Map.of(), Map.of());

   public HudPrefs(
      int opacity, boolean status, boolean coords, boolean modules, Map<String, HudLayout.Placement> placements, Map<String, HudPrefs.Offset> windows
   ) {
      if (opacity >= 20 && opacity <= 95) {
         placements = Map.copyOf(placements);
         windows = Map.copyOf(windows);
         this.opacity = opacity;
         this.status = status;
         this.coords = coords;
         this.modules = modules;
         this.placements = placements;
         this.windows = windows;
      } else {
         throw new IllegalArgumentException("Invalid HUD opacity");
      }
   }

   public HudPrefs withOpacity(int value) {
      return new HudPrefs(value, this.status, this.coords, this.modules, this.placements, this.windows);
   }

   public HudPrefs withVisibility(boolean s, boolean c, boolean m) {
      return new HudPrefs(this.opacity, s, c, m, this.placements, this.windows);
   }

   public HudPrefs withPlacements(Map<String, HudLayout.Placement> value) {
      return new HudPrefs(this.opacity, this.status, this.coords, this.modules, value, this.windows);
   }

   public HudPrefs withPlacement(String id, HudLayout.Placement value) {
      Map<String, HudLayout.Placement> next = new LinkedHashMap<>(this.placements);
      if (value == null) {
         next.remove(id);
      } else {
         next.put(id, value);
      }

      return this.withPlacements(next);
   }

   public HudPrefs withWindows(Map<String, HudPrefs.Offset> value) {
      return new HudPrefs(this.opacity, this.status, this.coords, this.modules, this.placements, value);
   }

   public HudPrefs withWindow(String id, HudPrefs.Offset value) {
      Map<String, HudPrefs.Offset> next = new LinkedHashMap<>(this.windows);
      if (value != null && !value.equals(HudPrefs.Offset.ZERO)) {
         next.put(id, value);
      } else {
         next.remove(id);
      }

      return this.withWindows(next);
   }

   public HudLayout.Placement placement(String id) {
      return this.placements.get(id);
   }

   public HudPrefs.Offset window(String id) {
      return this.windows.getOrDefault(id, HudPrefs.Offset.ZERO);
   }

   public static HudPrefs read(Properties p) {
      int opacity;
      try {
         opacity = Integer.parseInt(p.getProperty("hud.opacity", "70").strip());
      } catch (NumberFormatException var7) {
         throw new IllegalArgumentException("hud.opacity");
      }

      Map<String, HudLayout.Placement> placed = new TreeMap<>();
      Map<String, HudPrefs.Offset> moved = new TreeMap<>();

      for (String key : p.stringPropertyNames()) {
         if (key.startsWith("hud.pos.")) {
            HudLayout.Placement value = HudLayout.Placement.decode(p.getProperty(key));
            if (value != null) {
               placed.put(key.substring(8), value);
            }
         } else if (key.startsWith("window.")) {
            HudPrefs.Offset value = HudPrefs.Offset.decode(p.getProperty(key));
            if (value != null) {
               moved.put(key.substring(7), value);
            }
         }
      }

      return new HudPrefs(opacity, bool(p, "hud.status"), bool(p, "hud.coords"), bool(p, "hud.modules"), placed, moved);
   }

   private static boolean bool(Properties p, String key) {
      String s = p.getProperty(key, "true").strip();
      if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false")) {
         throw new IllegalArgumentException(key);
      } else {
         return Boolean.parseBoolean(s);
      }
   }

   public void write(Properties p) {
      for (String key : p.stringPropertyNames()) {
         if (key.startsWith("hud.pos.") || key.startsWith("window.")) {
            p.remove(key);
         }
      }

      p.setProperty("hud.opacity", Integer.toString(this.opacity));
      p.setProperty("hud.status", Boolean.toString(this.status));
      p.setProperty("hud.coords", Boolean.toString(this.coords));
      p.setProperty("hud.modules", Boolean.toString(this.modules));
      new TreeMap<>(this.placements).forEach((id, value) -> p.setProperty("hud.pos." + id, value.encode()));
      new TreeMap<>(this.windows).forEach((id, value) -> p.setProperty("window." + id, value.encode()));
   }

   public static HudPrefs load(Path file) throws IOException {
      if (!Files.exists(file)) {
         return DEFAULT;
      } else {
         try {
            return read(ClientSettings.read(file));
         } catch (IllegalArgumentException var2) {
            throw new IOException("Invalid HUD settings; existing file preserved", var2);
         }
      }
   }

   public record Offset(int x, int y) {
      public static final HudPrefs.Offset ZERO = new HudPrefs.Offset(0, 0);

      public Offset(int x, int y) {
         if (Math.abs(x) <= 16384 && Math.abs(y) <= 16384) {
            this.x = x;
            this.y = y;
         } else {
            throw new IllegalArgumentException("offset");
         }
      }

      public String encode() {
         return this.x + "," + this.y;
      }

      public static HudPrefs.Offset decode(String text) {
         if (text == null) {
            return null;
         } else {
            String[] parts = text.strip().split(",");
            if (parts.length != 2) {
               return null;
            } else {
               try {
                  return new HudPrefs.Offset(Integer.parseInt(parts[0].strip()), Integer.parseInt(parts[1].strip()));
               } catch (IllegalArgumentException var3) {
                  return null;
               }
            }
         }
      }
   }
}
