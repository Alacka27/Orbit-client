package dev.alacka.orbit.config;

import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.menu.OrbitMenu;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class OrbitSettings {
   private static UUID id;
   private static ClientSettings saved = ClientSettings.DEFAULT;
   private static ClientSettings preview;
   private static HudPrefs savedHud = HudPrefs.DEFAULT;
   private static HudPrefs previewHud;

   private OrbitSettings() {
   }

   private static Path path() {
      return FabricLoader.getInstance().getConfigDir().resolve("orbit/settings/" + id + ".properties");
   }

   public static ClientSettings get() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getUser() != null && !mc.getUser().getProfileId().equals(id)) {
         id = mc.getUser().getProfileId();
         preview = null;
         previewHud = null;

         try {
            saved = ClientSettings.load(path());
            if (!Files.exists(path())) {
               saved = saved.theme(saved.accent(), saved.windowOpacity(), saved.buttonOpacity(), OrbitMenu.legacyAnimation());
            }
         } catch (IOException var3) {
            saved = ClientSettings.DEFAULT;
            OrbitClient.LOGGER.error("[Orbit] Invalid profile preferences preserved", var3);
         }

         try {
            savedHud = HudPrefs.load(path());
         } catch (IOException var2) {
            savedHud = HudPrefs.DEFAULT;
            OrbitClient.LOGGER.error("[Orbit] Invalid HUD preferences preserved", var2);
         }
      }

      return saved;
   }

   public static ClientSettings theme() {
      get();
      return preview == null ? saved : preview;
   }

   public static HudPrefs hud() {
      get();
      return previewHud == null ? savedHud : previewHud;
   }

   public static HudPrefs savedHud() {
      get();
      return savedHud;
   }

   public static void preview(ClientSettings draft) {
      get();
      preview = draft;
   }

   public static void previewHud(HudPrefs draft) {
      get();
      previewHud = draft;
   }

   public static void cancelHudPreview() { previewHud = null; }

   public static void cancelPreview() {
      preview = null;
      previewHud = null;
   }

   public static void save(ClientSettings value) throws IOException {
      get();
      if (id == null) {
         throw new IOException("Launch identity is not available");
      } else {
         ClientSettings.saveTogether(path(), value, null);
         saved = value;
         preview = null;
      }
   }

   public static void saveHud(HudPrefs value) throws IOException {
      get();
      if (id == null) {
         throw new IOException("Launch identity is not available");
      } else {
         ClientSettings.saveTogether(path(), null, value);
         savedHud = value;
         previewHud = null;
      }
   }

   public static void saveAll(ClientSettings value, HudPrefs hud) throws IOException {
      get();
      if (id == null) {
         throw new IOException("Launch identity is not available");
      } else {
         ClientSettings.saveTogether(path(), value, hud);
         saved = value;
         savedHud = hud;
         preview = null;
         previewHud = null;
      }
   }

   public static void saveWindow(String window, HudPrefs.Offset offset) throws IOException {
      HudPrefs next = savedHud().withWindow(window, offset);
      HudPrefs draft = previewHud;
      ClientSettings themeDraft = preview;
      saveHud(next);
      if (draft != null) {
         previewHud = draft.withWindows(next.windows());
      }

      preview = themeDraft;
   }
}
