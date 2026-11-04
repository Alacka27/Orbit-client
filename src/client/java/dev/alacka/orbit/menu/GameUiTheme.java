package dev.alacka.orbit.menu;

import dev.alacka.orbit.mixin.ContainerScreenAccessor;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

public final class GameUiTheme {
   private static final Set<String> SUPPORTED = Set.of(
      "InventoryScreen",
      "CreativeModeInventoryScreen",
      "ContainerScreen",
      "CraftingScreen",
      "FurnaceScreen",
      "BlastFurnaceScreen",
      "SmokerScreen",
      "EnchantmentScreen",
      "AnvilScreen",
      "MerchantScreen",
      "BrewingStandScreen",
      "SmithingScreen",
      "CartographyTableScreen",
      "GrindstoneScreen",
      "LoomScreen",
      "StonecutterScreen",
      "BeaconScreen",
      "HopperScreen",
      "DispenserScreen",
      "ShulkerBoxScreen",
      "HorseInventoryScreen",
      "NautilusInventoryScreen",
      "CrafterScreen"
   );
   private GameUiTheme() {
   }

   public static boolean supported(Screen screen) {
      return screen instanceof AbstractContainerScreen
         && screen.getClass().getPackageName().equals("net.minecraft.client.gui.screens.inventory")
         && SUPPORTED.contains(screen.getClass().getSimpleName());
   }

   public static boolean active() {
      return OrbitMenu.isEnabled() && supported(Minecraft.getInstance().screen);
   }

   public static ContainerLayout layout(AbstractContainerScreen<?> screen) {
      ContainerScreenAccessor a = (ContainerScreenAccessor)screen;
      return ContainerLayout.of(a.orbit$left(), a.orbit$top(), a.orbit$imageWidth(), a.orbit$imageHeight(), screen.width, screen.height);
   }

   public static void background(GuiGraphics g, AbstractContainerScreen<?> screen) {
      if (active()) {
         ContainerScreenAccessor a = (ContainerScreenAccessor)screen;
         ContainerLayout b = layout(screen);
         GlassMenu.shadow(g, b.x(), b.y(), b.width(), b.height());
         GlassMenu.panel(g, b.x(), b.y(), b.width(), b.height(), 12, 773328934, 1604948418);
         if (screen instanceof InventoryScreen) {
            GlassMenu.heading(g, "Инвентарь", b.x() + 10, b.y() + 7, -330753);
         }

         for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive() && slot.x >= 0 && slot.y >= 0 && slot.x + 16 <= a.orbit$imageWidth() && slot.y + 16 <= a.orbit$imageHeight()) {
               GlassMenu.panel(g, a.orbit$left() + slot.x - 1, a.orbit$top() + slot.y - 1, 18, 18, 3, 1964643364, 1268679608);
            }
         }

         if (!(screen instanceof CreativeModeInventoryScreen)) {
            GlassMenu.panel(g, b.closeX(), b.closeY(), 11, 11, 3, 979915381, 1417503628);
            GlassMenu.text(g, "×", b.closeX() + 3, b.closeY() + 1, -923398);
         }

         diagram(g, screen, a.orbit$left(), a.orbit$top());
      }
   }

   private static void diagram(GuiGraphics g, AbstractContainerScreen<?> screen, int x, int y) {
      int ax = -1;
      int ay = 0;
      String color = screen.getClass().getSimpleName();
      switch (color) {
         case "InventoryScreen":
            ax = 136;
            ay = 32;
            break;
         case "CraftingScreen":
            ax = 100;
            ay = 38;
            break;
         case "FurnaceScreen":
         case "SmokerScreen":
         case "BlastFurnaceScreen":
            ax = 82;
            ay = 39;
            break;
         case "AnvilScreen":
            ax = 110;
            ay = 51;
            break;
         case "SmithingScreen":
            ax = 121;
            ay = 52;
            break;
         case "GrindstoneScreen":
            ax = 105;
            ay = 38;
            break;
         case "CartographyTableScreen":
            ax = 121;
            ay = 43;
            break;
         case "MerchantScreen":
            ax = 197;
            ay = 39;
      }

      if (ax >= 0) {
         int arrowColor = UiTheme.tint(-1194545177, false);
         float sx = x + ax;
         float sy = y + ay + 0.5F;
         GlassMenu.line(g, sx + 0.6F, sy, sx + 11.2F, sy, 1.3F, arrowColor);
         GlassMenu.line(g, sx + 7.6F, sy - 3.4F, sx + 11.2F, sy, 1.3F, arrowColor);
         GlassMenu.line(g, sx + 7.6F, sy + 3.4F, sx + 11.2F, sy, 1.3F, arrowColor);
      }
   }

   public static boolean hideTexture(GuiGraphics g, Identifier texture, int x, int y, int width, int height) {
      if (GlassMenu.active() && texture.getNamespace().equals("minecraft")) {
         String p = texture.getPath();
         if (Minecraft.getInstance().screen instanceof AdvancementsScreen) {
            if (p.equals("textures/gui/advancements/window.png")) {
               GlassMenu.panel(g, x, y, width, height, 8, 0, -1917744177);
               return true;
            }

            if (width == 16 && height == 16 && (p.startsWith("textures/gui/advancements/backgrounds/") || p.startsWith("textures/block/"))) {
               return true;
            }
         }

         if (p.equals("textures/gui/recipe_book.png") && width == 147 && height == 166) {
            GlassMenu.panel(g, x, y, width, height, 8, 773328934, 1604948418);
            return true;
         }
      }

      if (active() && texture.getNamespace().equals("minecraft") && texture.getPath().startsWith("textures/gui/container/")) {
         AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)Minecraft.getInstance().screen;
         ContainerScreenAccessor a = (ContainerScreenAccessor)screen;
         return x == a.orbit$left() && width == a.orbit$imageWidth() && height > 40 && y >= a.orbit$top() && y < a.orbit$top() + a.orbit$imageHeight();
      } else {
         return false;
      }
   }

   public static boolean sprite(GuiGraphics g, Identifier id, int x, int y, int w, int h, int color) {
      if (!id.getNamespace().equals("minecraft")) {
         return false;
      } else if (HudTheme.sprite(g, id.getPath(), x, y, w, h, color)) {
         return true;
      } else {
         String p = id.getPath();
         if (GlassMenu.active() && p.startsWith("advancements/")) {
            boolean selected = p.contains("selected") || p.contains("obtained") && !p.contains("unobtained");
            GlassMenu.panel(g, x, y, w, h, 5, selected ? -745975622 : -1540480962, selected ? -841570822 : 1704688826);
            return true;
         } else if (!active()) {
            return false;
         } else if (p.startsWith("container/inventory/effect_background")) {
            boolean ambient = p.contains("ambient");
            GlassMenu.panel(g, x, y, w, h, 6, ambient ? -1673909172 : -1037558985, ambient ? 1555536592 : 2057141440);
            return true;
         } else if (p.startsWith("widget/button")) {
            boolean selected = p.contains("highlighted");
            boolean disabled = p.contains("disabled");
            GlassMenu.panel(g, x, y, w, h, 4, disabled ? 1428692787 : (selected ? -1116256347 : -1974914217), selected ? -841964298 : 1570472115);
            return true;
         } else if (p.contains("slot_highlight_back")) {
            GlassMenu.panel(g, x + 3, y + 3, w - 6, h - 6, 3, 1349403289, -2134994707);
            return true;
         } else if (p.contains("slot_highlight_front")) {
            GlassMenu.panel(g, x + 3, y + 3, w - 6, h - 6, 3, 3285319, -909140481);
            return true;
         } else if (p.startsWith("container/")
            && (
               p.contains("enchantment_slot")
                  || p.contains("/button")
                  || p.contains("/tab_")
                  || p.contains("text_field")
                  || p.contains("recipe_selected")
                  || p.contains("recipe_highlighted")
                  || p.contains("pattern_selected")
                  || p.contains("pattern_highlighted")
            )) {
            boolean selected = p.contains("highlighted") || p.contains("selected") && !p.contains("unselected");
            boolean disabled = p.contains("disabled");
            GlassMenu.panel(g, x, y, w, h, 4, disabled ? 1428692787 : (selected ? -1284751459 : 1934637669), selected ? -858741514 : 1268482227);
            return true;
         } else if (p.startsWith("container/") && p.contains("scroller") && w <= 14) {
            GlassMenu.panel(g, x, y, w, h, 3, -1265804101, 1976608494);
            return true;
         } else if (p.equals("recipe_book/recipe_book")) {
            GlassMenu.panel(g, x, y, w, h, 8, 773328934, 1604948418);
            return true;
         } else {
            return false;
         }
      }
   }

   public static int labelColor(int color) {
      return !GlassMenu.active() || (color & 16777215) != 4210752 && (color & 16777215) != 4144959 ? color : color & 0xFF000000 | 15919611;
   }
}
