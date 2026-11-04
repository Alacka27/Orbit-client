package dev.alacka.orbit.menu;

import com.mojang.authlib.GameProfile;
import dev.alacka.orbit.profiles.ProfileLaunch;
import dev.alacka.orbit.accounts.AccountSessions;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

public final class PlayerIdentity {
   private static UUID id;
   private static Supplier<PlayerSkin> lookup;

   private PlayerIdentity() {
   }

   public static void reset() { id = null; lookup = null; }

   public static PlayerSkin skin() {
      Minecraft m = Minecraft.getInstance();
      if (m.player != null) {
         return m.player.getSkin();
      } else {
         User user = m.getUser();
         if (!user.getProfileId().equals(id) || lookup == null) {
            id = user.getProfileId();
            GameProfile profile = new GameProfile(id, user.getName());
            lookup = AccountSessions.offline() ? () -> DefaultPlayerSkin.get(profile) : m.getSkinManager().createLookup(profile, true);
         }

         return lookup.get();
      }
   }

   public static void drawCompact(GuiGraphics g, int x, int y, int width) {
      Minecraft m = Minecraft.getInstance();
      PlayerFaceRenderer.draw(g, skin(), x, y, 14);
      GlassMenu.text(g, fit(m.font, m.getUser().getName(), Math.max(12, width - 20)), x + 20, y + 3, -198913);
   }

   public static void draw(GuiGraphics g, int x, int y, int width) {
      Minecraft m = Minecraft.getInstance();
      PlayerFaceRenderer.draw(g, skin(), x, y + 2, 20);
      GlassMenu.text(g, fit(m.font, m.getUser().getName(), Math.max(20, width - 26)), x + 26, y + 2, -198913);
      String mode = AccountSessions.modeLabel();
      g.pose().pushMatrix();
      g.pose().translate(x + 26, y + 14);
      g.pose().scale(0.8F, 0.8F);
      GlassMenu.text(g, fit(m.font, mode, Math.max(20, (int)((width - 26) / 0.8))), 0, 0, -2503705);
      g.pose().popMatrix();
   }

   private static String fit(Font font, String value, int width) {
      if (font.width(value) <= width) {
         return value;
      } else {
         String cut = value;

         while (!cut.isEmpty() && font.width(cut.stripTrailing() + "…") > width) {
            cut = cut.substring(0, cut.length() - 1);
         }

         return cut.stripTrailing() + "…";
      }
   }
}
