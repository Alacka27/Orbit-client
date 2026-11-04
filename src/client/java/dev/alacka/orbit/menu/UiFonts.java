package dev.alacka.orbit.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FontDescription.Resource;
import net.minecraft.resources.Identifier;

public final class UiFonts {
   public static final Identifier UI = Identifier.fromNamespaceAndPath("orbit", "ui");
   public static final Identifier BOLD = Identifier.fromNamespaceAndPath("orbit", "ui_bold");
   public static final int MAX = 8;
   private static final FontDescription[] REGULAR = new FontDescription[9];
   private static final FontDescription[] HEAVY = new FontDescription[9];

   private UiFonts() {
   }

   public static int scale() {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft != null && minecraft.getWindow() != null ? Math.max(1, Math.min(8, minecraft.getWindow().getGuiScale())) : 2;
   }

   public static FontDescription variant(boolean bold, int scale) {
      int s = Math.max(1, Math.min(8, scale));
      return bold ? HEAVY[s] : REGULAR[s];
   }

   public static boolean available(boolean bold, int scale) {
      int s = Math.max(1, Math.min(8, scale));
      return Minecraft.getInstance()
         .getResourceManager()
         .getResource(Identifier.fromNamespaceAndPath("orbit", "font/" + (bold ? "ui_bold_x" : "ui_x") + s + ".json"))
         .isPresent();
   }

   static {
      for (int i = 1; i <= 8; i++) {
         REGULAR[i] = new Resource(Identifier.fromNamespaceAndPath("orbit", "ui_x" + i));
         HEAVY[i] = new Resource(Identifier.fromNamespaceAndPath("orbit", "ui_bold_x" + i));
      }
   }
}
