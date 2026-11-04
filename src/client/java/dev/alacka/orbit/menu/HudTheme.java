package dev.alacka.orbit.menu;

import dev.alacka.orbit.hud.HudElements;
import dev.alacka.orbit.hud.HudGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.FontDescription.Resource;
import net.minecraft.resources.Identifier;

public final class HudTheme {
   private static final FontDescription FONT = new Resource(Identifier.fromNamespaceAndPath("orbit", "ui"));

   private HudTheme() {
   }

   public static Component text(String s) {
      MutableComponent text = Component.literal(s);
      if (Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath("orbit", "font/ui.json")).isPresent()) {
         text.withStyle(st -> st.withFont(FONT));
      }

      return text;
   }

   /** Real 700-weight companion font; the same styled component supplies width and glyphs. */
   public static Component boldText(String s) {
      MutableComponent text = Component.literal(s);
      if (Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath("orbit", "font/ui_bold.json")).isPresent()) {
         text.withStyle(st -> st.withFont(new Resource(UiFonts.BOLD)));
      } else {
         text.withStyle(st -> st.withBold(true));
      }
      return text;
   }

   public static boolean enabled() {
      return OrbitMenu.isEnabled() && Minecraft.getInstance().player != null;
   }

   public static boolean transparentChat() {
      return enabled() && dev.alacka.orbit.config.OrbitSettings.theme().transparentChat();
   }

   public static boolean sprite(GuiGraphics g, String p, int x, int y, int w, int h, int color) {
      if (!enabled()) {
         return false;
      } else if (!p.equals("hud/hotbar")) {
         if (p.equals("hud/hotbar_selection")) {
            GlassMenu.hudPanel(g, x + 3, y + 3, 18, 18, 3, 1115044770, -2841601);
            return true;
         } else if (p.equals("hud/hotbar_offhand_left") || p.equals("hud/hotbar_offhand_right")) {
            var slot = HudGeometry.offhand(x, y, p.endsWith("_right"));
            GlassMenu.hudPanel(g, slot.x(), slot.y(), slot.width(), slot.height(), 3, 1544883744, 1000045492);
            return true;
         } else if (p.equals("hud/experience_bar_background")) {
            GlassMenu.hudPanel(g, x, y, w, h, 3, -1306914522, 1268480707);
            return true;
         } else if (p.startsWith("hud/effect_background")) {
            GlassMenu.hudPanel(g, x, y, w, h, 4, -1272373960, 1889171645);
            return true;
         } else {
            return false;
         }
      } else {
         var frame = HudGeometry.hotbar(x, y, w, h);
         GlassMenu.hudPanel(g, frame.x(), frame.y(), frame.width(), frame.height(), 6, -1020781768, -2136109881);

         for (int i = 0; i < 9; i++) {
            GlassMenu.hudPanel(g, x + 2 + i * 20, y + 2, 18, 18, 3, 1544883744, 1000045492);
         }

         return true;
      }
   }

   public static void render(GuiGraphics g) {
      HudElements.render(g);
   }
}
