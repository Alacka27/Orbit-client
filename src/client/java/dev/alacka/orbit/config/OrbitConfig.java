package dev.alacka.orbit.config;

import java.util.Properties;

public record OrbitConfig(ClientSettings settings, HudPrefs hud, boolean freeLook, double returnSpeed) {
   public static final String FORMAT = "orbit-config-1";

   public OrbitConfig(ClientSettings settings, HudPrefs hud, boolean freeLook, double returnSpeed) {
      if (settings == null || hud == null) {
         throw new IllegalArgumentException("config");
      } else if (Double.isFinite(returnSpeed) && !(returnSpeed < 1.0) && !(returnSpeed > 40.0)) {
         this.settings = settings;
         this.hud = hud;
         this.freeLook = true; // Kept for reading old configurations; never disables the built-in.
         this.returnSpeed = returnSpeed;
      } else {
         throw new IllegalArgumentException("freelook.returnSpeed");
      }
   }

   public Properties toProperties(String version) {
      Properties p = new Properties();
      p.setProperty("format", "orbit-config-1");
      p.setProperty("version", version == null ? "" : version);
      p.setProperty("theme.accent", String.format("%06x", this.settings.accent()));
      p.setProperty("theme.windowOpacity", Integer.toString(this.settings.windowOpacity()));
      p.setProperty("theme.buttonOpacity", Integer.toString(this.settings.buttonOpacity()));
      p.setProperty("theme.animations", Boolean.toString(this.settings.animations()));
      p.setProperty("theme.transparentChat", Boolean.toString(settings.transparentChat()));
      p.setProperty("module.NoFall", Boolean.toString(this.settings.noFall()));
      p.setProperty("module.Jesus", Boolean.toString(this.settings.jesus()));
      p.setProperty("module.Trajectories", Boolean.toString(this.settings.trajectories()));
      settings.binds().write(p);
      settings.visual().write(p);
      this.settings.combat().write(p);
      this.settings.support().write(p);
      p.setProperty("freelook.returnSpeed", Double.toString(this.returnSpeed));
      this.hud.write(p);
      return p;
   }

   public static OrbitConfig fromProperties(Properties p) {
      String format = p.getProperty("format");
      if (format != null && format.strip().equals("orbit-config-1")) {
         ClientSettings d = ClientSettings.DEFAULT;
         String accent = p.getProperty("theme.accent", String.format("%06x", d.accent())).strip();
         if (accent.startsWith("#")) {
            accent = accent.substring(1);
         }

         if (!accent.matches("[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("theme.accent");
         } else {
            ClientSettings settings = new ClientSettings(
               Integer.parseInt(accent, 16),
               integer(p, "theme.windowOpacity", d.windowOpacity()),
               integer(p, "theme.buttonOpacity", d.buttonOpacity()),
               bool(p, "theme.animations", d.animations()),
               bool(p, "module.NoFall", d.noFall()),
               bool(p, "module.Jesus", d.jesus()),
               bool(p, "module.Trajectories", d.trajectories()),
               CombatSettings.read(p),
               SupportSettings.read(p),
               bool(p, "theme.transparentChat", false),
               true,
               BindPrefs.read(p),
               VisualSettings.read(p)
            );

            double speed;
            try {
               speed = Double.parseDouble(p.getProperty("freelook.returnSpeed", "12.0").strip());
            } catch (NumberFormatException var8) {
               throw new IllegalArgumentException("freelook.returnSpeed");
            }

            return new OrbitConfig(settings, HudPrefs.read(p), true, speed);
         }
      } else {
         throw new IllegalArgumentException("format");
      }
   }

   private static int integer(Properties p, String key, int fallback) {
      try {
         return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).strip());
      } catch (NumberFormatException var4) {
         throw new IllegalArgumentException(key);
      }
   }

   private static boolean bool(Properties p, String key, boolean fallback) {
      String s = p.getProperty(key, Boolean.toString(fallback)).strip();
      if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false")) {
         throw new IllegalArgumentException(key);
      } else {
         return Boolean.parseBoolean(s);
      }
   }
}
