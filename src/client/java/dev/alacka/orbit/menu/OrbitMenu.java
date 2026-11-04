package dev.alacka.orbit.menu;

import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.config.ClientSettings;
import dev.alacka.orbit.config.OrbitSettings;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class OrbitMenu {
   private static final Identifier ICON = Identifier.fromNamespaceAndPath("orbit", "icon.png");
   private static boolean enabled = true;
   private static boolean animation = true;
   private static OrbitMenu current;
   private final Screen screen;
   private final List<AbstractWidget> buttons;
   private Button settings;
   private Button profiles;
   private final int logoSize;
   private final int top;
   private final int firstButtonY;

   public static void initialize() {
      Path path = FabricLoader.getInstance().getConfigDir().resolve("orbit/mainmenu.properties");

      try {
         Properties values = new Properties();
         if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
               values.load(reader);
            }
         } else {
            Files.createDirectories(path.getParent());
            values.setProperty("enabled", "true");
            values.setProperty("animation", "true");

            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
               values.store(writer, "Orbit violet glass main menu. Set enabled=false for vanilla. Restart to reload.");
            }
         }

         enabled = booleanSetting(values, "enabled", true);
         animation = booleanSetting(values, "animation", true);
      } catch (Exception var9) {
         enabled = false;
         OrbitClient.LOGGER.error("[Orbit] Main menu disabled: config error. Existing file preserved.", var9);
      }
   }

   private static boolean booleanSetting(Properties props, String key, boolean defaultValue) {
      String value = props.getProperty(key, Boolean.toString(defaultValue)).strip();
      if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
         throw new IllegalArgumentException(key + " must be true or false");
      } else {
         return Boolean.parseBoolean(value);
      }
   }

   private OrbitMenu(Screen screen, List<AbstractWidget> buttons) {
      this.screen = screen;
      this.buttons = buttons;
      boolean compact = screen.height < 300;
      this.logoSize = compact ? 52 : 82;
      int h = compact ? 22 : 28;
      int contentHeight = this.logoSize + 39 + h * 3 + 12;
      this.top = Math.max(8, (screen.height - contentHeight - 44) / 2);
      this.firstButtonY = this.top + this.logoSize + 39;
      int w = Math.min(220, screen.width - 48);

      for (int i = 0; i < buttons.size(); i++) {
         AbstractWidget button = buttons.get(i);
         int row = Math.min(i, 2);
         int bw = i < 2 ? w : (w - 6) / 2;
         int x = (screen.width - w) / 2 + (i == 3 ? (w + 6) / 2 : 0);
         button.setPosition(x, this.firstButtonY + row * (h + 6));
         button.setWidth(bw);
         button.setHeight(h);
      }
   }

   public static OrbitMenu install(Screen screen) {
      current = null;
      if (enabled && screen.width >= 320 && screen.height >= 240) {
         String[] keys = new String[]{"menu.singleplayer", "menu.multiplayer", "menu.options", "menu.quit"};
         List<AbstractWidget> selected = new ArrayList<>();

         for (int i = 0; i < keys.length; i++) {
            String label = Component.translatable(keys[i]).getString();
            AbstractWidget found = null;

            for (GuiEventListener child : screen.children()) {
               if (child instanceof Button button
                  && button.visible
                  && (
                     button.getMessage().getString().equals(label)
                        || i == 0 && button.getMessage().getString().equals(Component.translatable("menu.playdemo").getString())
                  )) {
                  found = button;
                  break;
               }
            }

            if (found == null) {
               return null;
            }

            selected.add(found);
         }

         Collections.swap(selected, 0, 1);
         current = new OrbitMenu(screen, selected);
         current.hideUnmanaged();
         return current;
      } else {
         return null;
      }
   }

   public void setSettings(Button button) {
      this.settings = button;
   }

   public void setProfiles(Button button) {
      this.profiles = button;
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static boolean legacyAnimation() {
      return animation;
   }

   public static boolean isAnimated() {
      return OrbitSettings.theme().animations();
   }

   public static void setAnimation(boolean value) throws IOException {
      ClientSettings p = OrbitSettings.get();
      OrbitSettings.save(p.theme(p.accent(), p.windowOpacity(), p.buttonOpacity(), value));
   }

   private void hideUnmanaged() {
      for (GuiEventListener child : this.screen.children()) {
         if (child instanceof AbstractWidget widget && !this.buttons.contains(widget) && widget != this.settings && widget != this.profiles) {
            widget.visible = false;
         }
      }
   }

   public void renderBackground(GuiGraphics g) {
      this.hideUnmanaged();
      int w = this.screen.width;
      int h = this.screen.height;
      GlassMenu.backdrop(g, w, h);
      int panelWidth = this.buttons.get(0).getWidth() + 28;
      int bottom = this.buttons.get(this.buttons.size() - 1).getY() + this.buttons.get(this.buttons.size() - 1).getHeight();
      int panelBottom = Math.min(bottom + 12, h - 31);
      GlassMenu.shadow(g, (w - panelWidth) / 2, this.top - 4, panelWidth, panelBottom - (this.top - 4));
      GlassMenu.panel(g, (w - panelWidth) / 2, this.top - 4, panelWidth, panelBottom - (this.top - 4), 12, 773328934, 1604948418);
      int logoX = (w - this.logoSize) / 2;
      g.blit(RenderPipelines.GUI_TEXTURED, ICON, logoX, this.top, 0.0F, 0.0F, this.logoSize, this.logoSize, 512, 512, 512, 512, -1);
      g.pose().pushMatrix();
      g.pose().scale(2.0F, 2.0F);
      Component word = GlassMenu.boldText("O R B I T", 2);
      g.drawString(Minecraft.getInstance().font, word, w / 4 - Minecraft.getInstance().font.width(word) / 2, (this.top + this.logoSize + 1) / 2, -330241, false);
      g.pose().popMatrix();
      Component subtitle = GlassMenu.text("YOUR WORLD. YOUR ORBIT.");
      g.drawString(Minecraft.getInstance().font, subtitle, (w - Minecraft.getInstance().font.width(subtitle)) / 2, this.firstButtonY - 15, -989958, true);
      Font font = Minecraft.getInstance().font;
      String version = "ORBIT / " + OrbitClient.version();
      boolean corner = 21 + font.width(version) + 6 <= (w - panelWidth) / 2 || this.top - 4 >= 25;
      if (corner) {
         GlassMenu.pill(g, 12.0F, 12.0F, 3.0F, 9.0F, 1.5F, UiTheme.tint(-2179585, false));
         g.drawString(font, version, 21, 12, -1516038, false);
      }

      String footer = corner ? "Minecraft 1.21.11" : "ORBIT " + OrbitClient.version() + " / Minecraft 1.21.11";
      g.drawString(font, footer, 12, h - 11, -1120262, false);
      String rights = "Not affiliated with Mojang";
      g.drawString(font, rights, w - font.width(rights) - 12, h - 11, -1120262, false);
   }

   public static boolean drawButton(GuiGraphics g, AbstractButton button) {
      OrbitMenu menu = current;
      if (menu != null && Minecraft.getInstance().screen == menu.screen) {
         int index = menu.buttons.indexOf(button);
         if (index < 0 && button != menu.settings && button != menu.profiles) {
            return false;
         } else {
            GlassMenu.button(g, button, index == 0);
            return true;
         }
      } else {
         return false;
      }
   }
}
