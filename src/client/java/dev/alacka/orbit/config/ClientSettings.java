package dev.alacka.orbit.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public record ClientSettings(int accent, int windowOpacity, int buttonOpacity, boolean animations, boolean noFall, boolean jesus, boolean trajectories,
                             CombatSettings combat, SupportSettings support, boolean transparentChat, boolean zoom, BindPrefs binds, VisualSettings visual) {
   public static final ClientSettings DEFAULT = new ClientSettings(10574816, 55, 70, true, false, false, false);

   public ClientSettings(int accent, int windowOpacity, int buttonOpacity, boolean animations, boolean noFall, boolean jesus, boolean trajectories) {
      this(accent, windowOpacity, buttonOpacity, animations, noFall, jesus, trajectories, CombatSettings.DEFAULT, SupportSettings.DEFAULT);
   }

   public ClientSettings(int accent, int windowOpacity, int buttonOpacity, boolean animations, boolean noFall, boolean jesus, boolean trajectories,
                         CombatSettings combat) {
      this(accent,windowOpacity,buttonOpacity,animations,noFall,jesus,trajectories,combat,SupportSettings.DEFAULT);
   }

   public ClientSettings(int accent, int windowOpacity, int buttonOpacity, boolean animations, boolean noFall, boolean jesus, boolean trajectories,
                         CombatSettings combat, SupportSettings support) {
      this(accent, windowOpacity, buttonOpacity, animations, noFall, jesus, trajectories, combat, support, false);
   }

   public ClientSettings(int accent, int windowOpacity, int buttonOpacity, boolean animations, boolean noFall, boolean jesus, boolean trajectories,
                         CombatSettings combat, SupportSettings support, boolean transparentChat) {
      this(accent, windowOpacity, buttonOpacity, animations, noFall, jesus, trajectories, combat, support, transparentChat, true, BindPrefs.DEFAULT);
   }

   public ClientSettings(int accent, int windowOpacity, int buttonOpacity, boolean animations, boolean noFall, boolean jesus, boolean trajectories,
                         CombatSettings combat, SupportSettings support, boolean transparentChat, boolean zoom, BindPrefs binds) {
      this(accent, windowOpacity, buttonOpacity, animations, noFall, jesus, trajectories, combat, support,
              transparentChat, true, binds, VisualSettings.DEFAULT);
   }

   public ClientSettings(int accent, int windowOpacity, int buttonOpacity, boolean animations, boolean noFall, boolean jesus, boolean trajectories,
                         CombatSettings combat, SupportSettings support, boolean transparentChat, boolean zoom, BindPrefs binds, VisualSettings visual) {
      java.util.Objects.requireNonNull(binds, "binds");
      java.util.Objects.requireNonNull(combat, "combat");
      java.util.Objects.requireNonNull(support, "support");
      java.util.Objects.requireNonNull(visual, "visual");
      if (accent >= 0 && accent <= 16777215 && windowOpacity >= 0 && windowOpacity <= 85 && buttonOpacity >= 35 && buttonOpacity <= 95) {
         this.accent = accent;
         this.windowOpacity = windowOpacity;
         this.buttonOpacity = buttonOpacity;
         this.animations = animations;
         this.noFall = noFall;
         this.jesus = jesus;
         this.trajectories = trajectories;
         this.combat = combat;
         this.support = support;
         this.transparentChat = transparentChat;
         this.zoom = true; // Legacy field: Zoom is now a built-in, never a switchable module.
         this.binds = binds;
         this.visual = visual;
      } else {
         throw new IllegalArgumentException("Invalid theme values");
      }
   }

   public ClientSettings theme(int color, int window, int button, boolean motion) {
      return new ClientSettings(color, window, button, motion, this.noFall, this.jesus, this.trajectories, this.combat, this.support, this.transparentChat, true, this.binds, visual);
   }

   public ClientSettings combat(CombatSettings value) {
      return new ClientSettings(accent, windowOpacity, buttonOpacity, animations, noFall, jesus, trajectories, value, support, transparentChat, true, binds, visual);
   }

   public ClientSettings support(SupportSettings value) {
      return new ClientSettings(accent,windowOpacity,buttonOpacity,animations,noFall,jesus,trajectories,combat,value,transparentChat,true,binds,visual);
   }

   public ClientSettings withTransparentChat(boolean value) {
      return new ClientSettings(accent, windowOpacity, buttonOpacity, animations, noFall, jesus, trajectories, combat, support, value, true, binds, visual);
   }

   public ClientSettings module(String name, boolean enabled) {
      if (VisualSettings.MODULES.contains(name)) return withVisual(visual.enabled(name, enabled));
      return switch (name) {
         case "NoFall" -> new ClientSettings(this.accent, this.windowOpacity, this.buttonOpacity, this.animations, enabled, this.jesus, this.trajectories, this.combat, this.support, this.transparentChat, true, this.binds, visual);
         case "Jesus" -> new ClientSettings(this.accent, this.windowOpacity, this.buttonOpacity, this.animations, this.noFall, enabled, this.trajectories, this.combat, this.support, this.transparentChat, true, this.binds, visual);
         case "Trajectories" -> new ClientSettings(this.accent, this.windowOpacity, this.buttonOpacity, this.animations, this.noFall, this.jesus, enabled, this.combat, this.support, this.transparentChat, true, this.binds, visual);
         case "KillAura", "AttackAura" -> combat(combat.withAura(combat.aura().enabled(enabled)));
         case "TriggerBot" -> combat(combat.withTrigger(combat.trigger().enabled(enabled)));
         case "AutoTotem", "AutoArmor", "AutoConsume", "ElytraSwap" -> support(support.enabled(name,enabled));
         default -> throw new IllegalArgumentException("Unknown module: " + name);
      };
   }

   public ClientSettings withBinds(BindPrefs value) {
      return new ClientSettings(accent, windowOpacity, buttonOpacity, animations, noFall, jesus, trajectories, combat, support, transparentChat, true, value, visual);
   }

   public ClientSettings withVisual(VisualSettings value) {
      return new ClientSettings(accent, windowOpacity, buttonOpacity, animations, noFall, jesus, trajectories,
              combat, support, transparentChat, true, binds, value);
   }

   public static ClientSettings load(Path file) throws IOException {
      if (!Files.exists(file)) {
         return DEFAULT;
      } else {
         Properties p = read(file);

         try {
            String color = p.getProperty("accent", "945cda").strip();
            if (!color.matches("[0-9a-fA-F]{6}")) {
               throw new IllegalArgumentException("accent must be 6 hex digits");
            } else {
               return new ClientSettings(
                  Integer.parseInt(color, 16),
                  Integer.parseInt(p.getProperty("windowOpacity", "18")),
                  Integer.parseInt(p.getProperty("buttonOpacity", "60")),
                  bool(p, "animations", true),
                  bool(p, "NoFall", false),
                  bool(p, "Jesus", false),
                  bool(p, "Trajectories", false),
                  CombatSettings.read(p),
                  SupportSettings.read(p),
                  bool(p, "chat.transparent", false),
                  true,
                  BindPrefs.read(p),
                  VisualSettings.read(p)
               );
            }
         } catch (IllegalArgumentException var3) {
            throw new IOException("Invalid settings; existing file preserved", var3);
         }
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

   public static Properties read(Path file) throws IOException {
      Properties p = new Properties();

      try {
         try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(r);
         }

         return p;
      } catch (IllegalArgumentException var7) {
         throw new IOException("Invalid properties; file preserved", var7);
      }
   }

   public void write(Properties p) {
      p.setProperty("accent", String.format("%06x", this.accent));
      p.setProperty("windowOpacity", this.windowOpacity + "");
      p.setProperty("buttonOpacity", this.buttonOpacity + "");
      p.setProperty("animations", this.animations + "");
      p.setProperty("NoFall", this.noFall + "");
      p.setProperty("Jesus", this.jesus + "");
      p.setProperty("Trajectories", this.trajectories + "");
      p.setProperty("chat.transparent", Boolean.toString(transparentChat));
      p.remove("Zoom");
      p.remove("module.FreeLook");
      p.remove("module.Zoom");
      binds.write(p);
      visual.write(p);
      this.combat.write(p);
      this.support.write(p);
   }

   public void save(Path file) throws IOException {
      saveTogether(file, this, null);
   }

   public static void saveTogether(Path file, ClientSettings settings, HudPrefs hud) throws IOException {
      Properties p = new Properties();
      if (Files.exists(file)) {
         load(file);
         HudPrefs.load(file);
         p = read(file);
      }

      if (settings != null) {
         settings.write(p);
      }

      if (hud != null) {
         hud.write(p);
      }

      store(file, p, "Orbit local per-profile preferences. No credentials.");
   }

   public static void store(Path file, Properties p, String comment) throws IOException {
      Path dir = file.toAbsolutePath().getParent();
      Files.createDirectories(dir);
      Path tmp = Files.createTempFile(dir, "orbit-", ".tmp");

      try {
         try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            p.store(w, comment);
         }

         try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
         } catch (AtomicMoveNotSupportedException var14) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
         }
      } finally {
         Files.deleteIfExists(tmp);
      }
   }
}
