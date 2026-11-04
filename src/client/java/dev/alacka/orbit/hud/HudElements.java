package dev.alacka.orbit.hud;

import dev.alacka.orbit.config.HudPrefs;
import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.HudTheme;
import dev.alacka.orbit.menu.UiTheme;
import dev.alacka.orbit.modules.Modules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.scores.DisplaySlot;

public final class HudElements {
   public static final String STATUS = "status";
   public static final String COORDS = "coords";
   public static final String MODULES = "modules";
   public static final List<String> IDS = List.of("status", "coords", "modules", "target", "combat");
   public static final int STATUS_HEIGHT = 18;
   public static final int COORDS_HEIGHT = 15;
   public static final int ROW_HEIGHT = 13;
   public static final int ROW_STEP = 15;
   private static final Map<String, Float> ANIMATION = new HashMap<>();
   private static long lastFrame;
   private static final Map<String, HudLayout.Rect> PLATES = new HashMap<>();
   public static Map<String, HudLayout.Rect> plates() { return Map.copyOf(PLATES); }

   private HudElements() {
   }

   public static HudLayout.Placement defaults(String id, int screenWidth) {
      return switch (id) {
         case "status" -> new HudLayout.Placement(HudLayout.Anchor.TOP_LEFT, 6, 6);
         case "coords" -> screenWidth < 500
            ? new HudLayout.Placement(HudLayout.Anchor.TOP_LEFT, 6, 30)
            : new HudLayout.Placement(HudLayout.Anchor.BOTTOM_LEFT, 6, 6);
         // Recent chat messages occupy the lower left. Keep a new target panel above
         // them, leaving room for the compact-window coordinates under the watermark.
         case "target" -> new HudLayout.Placement(HudLayout.Anchor.TOP_LEFT, 6, screenWidth < 500 ? 52 : 32);
         case "combat" -> new HudLayout.Placement(HudLayout.Anchor.BOTTOM_RIGHT, 6, 60);
         default -> new HudLayout.Placement(HudLayout.Anchor.TOP_RIGHT, 6, 6);
      };
   }

   public static HudLayout.Placement placement(Map<String, HudLayout.Placement> placements, String id, int screenWidth) {
      HudLayout.Placement p = placements.get(id);
      return p == null ? defaults(id, screenWidth) : p;
   }

   public static String label(String id) {
      return switch (id) {
         case "status" -> "FPS и пинг";
         case "coords" -> "Координаты";
         case "target" -> "Панель цели";
         case "combat" -> "Щит и тотемы";
         default -> "Список модулей";
      };
   }

   public static boolean visible(HudPrefs prefs, String id) {
      return switch (id) {
         case "status" -> prefs.status();
         case "coords" -> prefs.coords();
         case "target" -> OrbitSettings.get().visual().enabled("TargetHUD");
         case "combat" -> OrbitSettings.get().visual().enabled("ShieldStatus") || OrbitSettings.get().visual().enabled("TotemCounter");
         default -> prefs.modules();
      };
   }

   public static Component statusText(Minecraft m) {
      ClientPacketListener connection = m.getConnection();
      PlayerInfo info = connection != null && m.player != null ? connection.getPlayerInfo(m.player.getUUID()) : null;
      String latency = m.hasSingleplayerServer() ? "Singleplayer" : (info == null ? "— ms" : info.getLatency() + " ms");
      return HudTheme.boldText("ORBIT   |   " + m.getFps() + " FPS   |   " + latency);
   }

   public static Component coordsText(Minecraft m) {
      return m.player == null
         ? HudTheme.text("XYZ  128 / 64 / -42")
         : HudTheme.text("XYZ  " + m.player.getBlockX() + " / " + m.player.getBlockY() + " / " + m.player.getBlockZ());
   }

   public static List<HudElements.Row> rows(Font font, boolean preview) {
      long now = System.nanoTime();
      float dt = lastFrame == 0L ? 1.0F : Math.min(0.25F, (float)(now - lastFrame) / 1.0E9F);
      lastFrame = now;
      boolean animate = OrbitSettings.theme().animations();
      float step = animate ? 1.0F - (float)Math.exp(-dt * 12.0F) : 1.0F;
      List<HudElements.Row> rows = new ArrayList<>();

      for (Modules.Module module : Modules.ALL) {
         float target = module.enabled().getAsBoolean() ? 1.0F : 0.0F;
         float value = ANIMATION.getOrDefault(module.name(), target);
         value += (target - value) * step;
         if (Math.abs(target - value) < 0.002F) {
            value = target;
         }

         ANIMATION.put(module.name(), value);
         if (value > 0.01F) {
            rows.add(new HudElements.Row(module.name(), value, false));
         }
      }

      if (rows.isEmpty() && preview) {
         for (Modules.Module module : Modules.ALL.subList(0, 2)) {
            rows.add(new HudElements.Row(module.name(), 1.0F, true));
         }
      }

      rows.sort(Comparator.<HudElements.Row>comparingInt(r -> font.width(HudTheme.text(r.name()))).reversed().thenComparing(HudElements.Row::name));
      return rows;
   }

   public static int statusWidth(Font font, Component text) {
      return font.width(text) + 14;
   }

   public static int coordsWidth(Font font, Component text) {
      return font.width(text) + 12;
   }

   public static int rowWidth(Font font, String name) {
      return font.width(HudTheme.text(name)) + 15;
   }

   public static int modulesWidth(Font font, List<HudElements.Row> rows) {
      int width = 0;

      for (HudElements.Row row : rows) {
         width = Math.max(width, rowWidth(font, row.name()));
      }

      return Math.max(width, 40);
   }

   public static int modulesHeight(List<HudElements.Row> rows) {
      float height = 0.0F;

      for (HudElements.Row row : rows) {
         height += 15.0F * row.visibility();
      }

      return Math.max(13, Math.round(height - 2.0F));
   }

   public static void drawStatus(GuiGraphics g, Font font, Component text, int x, int y, float alpha) {
      GlassMenu.hudPanel(g, x, y, statusWidth(font, text), 18, 5, fade(-1138025157, alpha), fade(1620802235, alpha));
      g.drawString(font, text, x + 7, y + 5, fade(-593921, alpha), false);
   }

   public static void drawCoords(GuiGraphics g, Font font, Component text, int x, int y, float alpha) {
      GlassMenu.hudPanel(g, x, y, coordsWidth(font, text), 15, 4, fade(-1205134021, alpha), fade(1182555804, alpha));
      g.drawString(font, text, x + 6, y + 4, fade(-1120776, alpha), false);
   }

   public static void drawModules(GuiGraphics g, Font font, List<HudElements.Row> rows, int x, int y, int width, int column, float alpha) {
      float cursor = y;
      int accent = UiTheme.accent();
      int text = UiTheme.accentText();

      for (HudElements.Row row : rows) {
         float v = row.visibility();
         int w = rowWidth(font, row.name());
         int slide = Math.round((1.0F - v) * 10.0F);
         int rx = column == 2 ? x + width - w + slide : (column == 1 ? x + (width - w) / 2 : x - slide);
         int ry = Math.round(cursor);
         float a = alpha * v * (row.sample() ? 0.5F : 1.0F);
         GlassMenu.hudPanel(g, rx, ry, w, 13, 4, fade(-1205134021, a), fade(1066035886, a));
         int barX = column == 2 ? rx + w - 5 : rx + 3;
         GlassMenu.pill(g, barX, ry + 3, 2.0F, 7.0F, 1.0F, fade(accent, a));
         g.drawString(font, HudTheme.text(row.name()), column == 2 ? rx + 5 : rx + 8, ry + 3, fade(text, a), false);
         cursor += 15.0F * v;
      }
   }

   public static int fade(int argb, float alpha) {
      int a = Math.round((argb >>> 24) * Math.max(0.0F, Math.min(1.0F, alpha)));
      return a << 24 | argb & 16777215;
   }

   public static HudLayout.Rect effects(Minecraft m, int screenWidth) {
      if (m.player == null) {
         return null;
      } else {
         int beneficial = 0;
         int harmful = 0;

         for (MobEffectInstance effect : m.player.getActiveEffects()) {
            if (effect.showIcon()) {
               if (effect.getEffect().value().isBeneficial()) {
                  beneficial++;
               } else {
                  harmful++;
               }
            }
         }

         return HudLayout.effects(screenWidth, beneficial, harmful, m.isDemo());
      }
   }

   public static boolean tabListVisible(Minecraft m) {
      return m.level != null && m.player != null && m.options.keyPlayerList.isDown()
         ? !m.isLocalServer()
            || m.player.connection.getListedOnlinePlayers().size() > 1
            || m.level.getScoreboard().getDisplayObjective(DisplaySlot.LIST) != null
         : false;
   }

   public static HudLayout.Rect resolve(String id, Map<String, HudLayout.Placement> placements, int width, int height, int screenWidth, int screenHeight) {
      return HudLayout.resolve(placement(placements, id, screenWidth), screenWidth, screenHeight, width, height);
   }

   public static void render(GuiGraphics g) { renderNow(g, false); }
   public static void renderInChat(GuiGraphics g) { renderNow(g, true); }
   private static void renderNow(GuiGraphics g, boolean chat) {
      Minecraft m = Minecraft.getInstance();
      if ((m.screen instanceof ChatScreen) != chat) return;
      PLATES.clear();
      if (HudTheme.enabled()) {
         Font font = m.font;
         List<HudElements.Row> rows = rows(font, false);
         if (!m.options.hideGui && !m.getDebugOverlay().showDebugScreen() && (m.screen == null || m.screen instanceof ChatScreen)) {
            HudPrefs prefs = OrbitSettings.hud();
            if (chat && rows.isEmpty() && prefs.modules()) rows = List.of(new Row("Модули", 1, true));
            int sw = g.guiWidth();
            int sh = g.guiHeight();
            HudLayout.Rect effects = effects(m, sw);
            if (prefs.status()) {
               Component text = statusText(m);
               HudLayout.Rect r = place("status", prefs, statusWidth(font, text), 18, sw, sh, effects);
               PLATES.put("status", r);
               drawStatus(g, font, text, r.x(), r.y(), 1.0F);
            }

            if (prefs.coords()) {
               Component text = coordsText(m);
               HudLayout.Rect r = place("coords", prefs, coordsWidth(font, text), 15, sw, sh, effects);
               PLATES.put("coords", r);
               drawCoords(g, font, text, r.x(), r.y(), 1.0F);
            }

            if (prefs.modules() && !rows.isEmpty() && !tabListVisible(m)) {
               int width = modulesWidth(font, rows);
               HudLayout.Rect r = place("modules", prefs, width, modulesHeight(rows), sw, sh, effects);
               PLATES.put("modules", r);
               drawModules(g, font, rows, r.x(), r.y(), width, placement(prefs.placements(), "modules", sw).anchor().column, 1.0F);
            }
            var visual = dev.alacka.orbit.modules.VisualModules.frame();
            if (OrbitSettings.get().visual().enabled("TargetHUD") && (visual.target() != null || chat)) {
               var r = place("target", prefs, VisualHud.TARGET_WIDTH, VisualHud.TARGET_HEIGHT, sw, sh, effects);
               PLATES.put("target",r);
               VisualHud.target(g,font,visual.target()==null?dev.alacka.orbit.modules.VisualModules.Target.sample():visual.target(),r.x(),r.y());
            }
            if (OrbitSettings.get().visual().enabled("ShieldStatus") || OrbitSettings.get().visual().enabled("TotemCounter")) {
               var r=place("combat",prefs,VisualHud.INFO_WIDTH,VisualHud.infoHeight(),sw,sh,effects);
               PLATES.put("combat",r);
               VisualHud.info(g,font,visual,r.x(),r.y());
            }
            VisualHud.labels(g,font,visual);
         }
      }
   }

   private static HudLayout.Rect place(String id, HudPrefs prefs, int width, int height, int sw, int sh, HudLayout.Rect effects) {
      HudLayout.Placement p = placement(prefs.placements(), id, sw);
      HudLayout.Rect r = HudLayout.resolve(p, sw, sh, width, height);
      if (Minecraft.getInstance().screen instanceof ChatScreen) {
         // Keep bottom-anchored plates fully grabbable without covering native text input.
         return r.withY(HudLayout.clamp(r.y(), 0, sh - 16 - height));
      }
      return p.anchor().row == 0 ? HudLayout.avoid(r, effects, 4, sh) : r;
   }

   public record Row(String name, float visibility, boolean sample) {
   }
}
