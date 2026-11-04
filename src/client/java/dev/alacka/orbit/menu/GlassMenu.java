package dev.alacka.orbit.menu;

import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.mixin.CycleButtonAccessor;
import dev.alacka.orbit.profiles.ProfileRowButton;
import dev.alacka.orbit.render.ShapeMath;
import dev.alacka.orbit.render.SmoothShapes;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Button.Plain;
import net.minecraft.client.gui.components.CycleButton.DisplayState;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.FontDescription.Resource;
import net.minecraft.resources.Identifier;

public final class GlassMenu {
   public static final int WINDOW_FILL = 773328934;
   public static final int WINDOW_BORDER = 1604948418;
   private static final WeakHashMap<AbstractButton, GlassMenu.Hover> HOVERS = new WeakHashMap<>();

   private GlassMenu() {
   }

   public static boolean active() {
      Minecraft client = Minecraft.getInstance();
      return (OrbitMenu.isEnabled() || client.screen instanceof OrbitWindow)
         && client.screen != null
         && !(client.screen instanceof ChatScreen)
         && (!(client.screen instanceof AbstractContainerScreen) || GameUiTheme.supported(client.screen));
   }

   public static boolean supports(AbstractButton button) {
      if (GameUiTheme.active()
         || button instanceof ModuleCard
         || button instanceof ColorSwatch
         || button instanceof ProfileRowButton
         || button instanceof ConfigRowButton) {
         return false;
      } else if (button instanceof ImageButton || button instanceof SpriteIconButton || button instanceof PlainTextButton) {
         return false;
      } else {
         return button instanceof CycleButton<?> cycle
            ? ((CycleButtonAccessor)cycle).orbit$displayState() != DisplayState.HIDE
            : button instanceof Plain || button instanceof LockIconButton;
      }
   }

   public static void backdrop(GuiGraphics g, int width, int height) {
      if (Minecraft.getInstance().level != null) {
         if (Minecraft.getInstance().options.getMenuBackgroundBlurriness() >= 1) {
            g.blurBeforeThisStratum();
         }

         g.fill(0, 0, width, height, UiTheme.tint(1175456538, false));
      } else {
         double scale = Math.max(width / 1600.0, height / 900.0);
         int w = (int)Math.ceil(1600.0 * scale);
         int h = (int)Math.ceil(900.0 * scale);
         g.blit(RenderPipelines.GUI_TEXTURED, ThemeBackdrop.texture(), (width - w) / 2, (height - h) / 2, 0.0F, 0.0F, w, h, 1600, 900, 1600, 900, -1);

         for (int row = 0; row < 32; row++) {
            int alpha = row * 120 / 31;
            g.fill(0, height - 32 + row, width, height - 31 + row, UiTheme.tint(alpha << 24 | 1050910, false));
         }
      }
   }

   public static float radius(int requested, int width, int height) {
      float half = Math.min(width, height) / 2.0F;
      if (requested >= half - 1.0F) {
         return half;
      } else {
         boolean small = Math.min(width, height) < 16 || requested <= 4;
         boolean medium = !small && (requested <= 8 || Math.min(width, height) < 24);
         return Math.min(small ? 3.0F : (medium ? 6.0F : 10.0F), half);
      }
   }

   public static void panel(GuiGraphics g, int x, int y, int width, int height, int radius, int fill, int border) {
      surface(g, x, y, width, height, radius, fill, border, false);
   }

   public static void hudPanel(GuiGraphics g, int x, int y, int width, int height, int radius, int fill, int border) {
      surface(g, x, y, width, height, radius, fill, border, true);
   }

   private static void surface(GuiGraphics g, int x, int y, int width, int height, int radius, int fill, int border, boolean hud) {
      if (width > 0 && height > 0) {
         boolean window = fill == 773328934;
         fill = window ? UiTheme.tint(fill, true) : (hud ? UiTheme.hudTint(fill) : UiTheme.tint(fill, true));
         border = UiTheme.tint(border, false);
         float r = radius(radius, width, height);
         ShapeMath.Mesh mesh = SmoothShapes.mesh(g);
         if (fill >>> 24 != 0) {
            if (window) {
               int alpha = fill >>> 24;
               int base = UiTheme.tint(-12046244, false) & 16777215;
               int shade = UiTheme.tint(-15726056, false) & 16777215;
               float[] levels = new float[7];

               for (int i = 0; i < levels.length; i++) {
                  levels[i] = y + r + (height - 2.0F * r) * (i + 1) / 8.0F;
               }

               ShapeMath.fill(mesh, x, y, width, height, r, yy -> ShapeMath.windowColor(base, shade, alpha, ShapeMath.sliceRow(yy - y, height, 10.0F)), levels);
            } else {
               int surfaceFill = fill;
               ShapeMath.fill(mesh, x, y, width, height, r, yy -> ShapeMath.scaleRgb(surfaceFill, 1.0F - 0.08627451F * ShapeMath.sliceRow(yy - y, height, r)), null);
            }
         }

         if (border >>> 24 != 0) {
            ShapeMath.ring(mesh, x, y, width, height, r, 1.0F, border);
         }

         SmoothShapes.submit(g, mesh);
      }
   }

   public static void shadow(GuiGraphics g, int x, int y, int width, int height) {
      if (width > 0 && height > 0) {
         ShapeMath.Mesh mesh = SmoothShapes.mesh(g);
         ShapeMath.shadow(mesh, x, y + 2, width, height, Math.min(10.0F, Math.min(width, height) / 2.0F), 301989888, 6.0F);
         SmoothShapes.submit(g, mesh);
      }
   }

   public static void pill(GuiGraphics g, float x, float y, float width, float height, float radius, int argb) {
      SmoothShapes.fill(g, x, y, width, height, radius, argb);
   }

   public static void outline(GuiGraphics g, float x, float y, float width, float height, float radius, int argb) {
      ShapeMath.Mesh mesh = SmoothShapes.mesh(g);
      ShapeMath.ring(mesh, x, y, width, height, Math.min(radius, Math.min(width, height) / 2.0F), 1.0F, argb);
      SmoothShapes.submit(g, mesh);
   }

   public static void line(GuiGraphics g, float x0, float y0, float x1, float y1, float thickness, int argb) {
      SmoothShapes.line(g, x0, y0, x1, y1, thickness, argb);
   }

   public static void grip(GuiGraphics g, int centreX, int y, boolean hovered) {
      pill(g, centreX - 12, y, 24.0F, 3.0F, 1.5F, UiTheme.tint(hovered ? -924198657 : 1490667758, false));
   }

   public static Component text(String label) {
      return Component.literal(label);
   }

   public static void text(GuiGraphics g, String label, int x, int y, int color) {
      g.drawString(Minecraft.getInstance().font, text(label), x, y, color, false);
   }

   public static Component boldText(String label) {
      MutableComponent text = Component.literal(label);
      if (Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath("orbit", "font/ui_bold.json")).isPresent()) {
         text.withStyle(s -> s.withFont(new Resource(UiFonts.BOLD)));
      }

      return text;
   }

   public static Component boldText(String label, int poseScale) {
      int scale = UiFonts.scale() * Math.max(1, poseScale);
      if (scale <= 8 && UiFonts.available(true, scale)) {
         MutableComponent text = Component.literal(label);
         text.withStyle(s -> s.withFont(UiFonts.variant(true, scale)));
         return text;
      } else {
         return boldText(label);
      }
   }

   public static void centeredHeading(GuiGraphics g, String label, int centreX, int y, int color) {
      Font font = Minecraft.getInstance().font;
      Component content = boldText(label);
      g.drawString(font, content, centreX - font.width(content) / 2, y, color, false);
   }

   public static void heading(GuiGraphics g, String label, int x, int y, int color) {
      g.drawString(Minecraft.getInstance().font, boldText(label), x, y, color, false);
   }

   public static boolean selectionScreen() {
      Screen screen = Minecraft.getInstance().screen;
      return active() && (screen instanceof SelectWorldScreen || screen instanceof JoinMultiplayerScreen);
   }

   public static void screenPanel(GuiGraphics g, int width, int height) {
      if (active()
         && !(Minecraft.getInstance().screen instanceof AbstractContainerScreen)
         && !(Minecraft.getInstance().screen instanceof AdvancementsScreen)
         && !(Minecraft.getInstance().screen instanceof TitleScreen)
         && !(Minecraft.getInstance().screen instanceof OrbitWindow)) {
         if (Minecraft.getInstance().screen instanceof PauseScreen pause) {
            if (pause.showsPauseMenu()) {
               int count = (int)pause.children().stream().filter(c -> c instanceof Button b && b.visible).count();
               PauseLayout p = PauseLayout.of(width, height, count);
               shadow(g, p.x(), p.y(), p.width(), p.height());
               panel(g, p.x(), p.y(), p.width(), p.height(), 12, 773328934, 1604948418);
            }
         } else {
            int w = Math.min(552, Math.max(width - 16, Math.min(width - 4, 320)));
            int x = (width - w) / 2;
            shadow(g, x, 7, w, height - 14);
            panel(g, x, 7, w, height - 14, 12, 773328934, 1604948418);
         }
      }
   }

   public static void card(GuiGraphics g, LayoutElement entry, boolean selected, boolean hovered) {
      int fill = selected ? -1219480969 : (hovered ? -1739570079 : 1883387984);
      panel(g, entry.getX(), entry.getY(), entry.getWidth(), entry.getHeight() - 1, 5, fill, selected ? -793539861 : (hovered ? 1892398822 : 685626094));
   }

   private static int mix(int a, int b, float t) {
      int out = 0;

      for (int shift = 0; shift <= 24; shift += 8) {
         int av = a >>> shift & 0xFF;
         int bv = b >>> shift & 0xFF;
         out |= Math.round(av + (bv - av) * t) << shift;
      }

      return out;
   }

   public static void button(GuiGraphics g, AbstractButton button, boolean primary) {
      GlassMenu.Hover state = HOVERS.computeIfAbsent(button, key -> new GlassMenu.Hover());
      long now = System.nanoTime();
      float dt = state.last == 0L ? 1.0F : Math.min(0.2F, (float)(now - state.last) / 1.0E9F);
      state.last = now;
      float target = button.active && button.isHoveredOrFocused() ? 1.0F : 0.0F;
      state.value = state.value + (target - state.value) * (OrbitSettings.theme().animations() ? 1.0F - (float)Math.exp(-dt * 16.0F) : 1.0F);
      boolean title = Minecraft.getInstance().screen instanceof TitleScreen;
      String raw = button.getMessage().getString();
      boolean accent = primary
         || button.getMessage().equals(Component.translatable("menu.returnToGame"))
         || button.getMessage().equals(Component.translatable("deathScreen.respawn"))
         || button.getMessage().equals(Component.translatable("selectWorld.select"))
         || button.getMessage().equals(Component.translatable("selectServer.select"))
         || button.getMessage().equals(Component.translatable("lanServer.start"))
         || Minecraft.getInstance().screen instanceof CreateWorldScreen && button.getMessage().equals(Component.translatable("selectWorld.create"))
         || Minecraft.getInstance().screen instanceof OrbitWindow window && window.accent(button)
         || button.getMessage().equals(Component.translatable("gui.back"))
         || button.getMessage().equals(Component.translatable("gui.done"));
      int fill = !button.active
         ? 1479879747
         : (accent ? mix(-1401403721, -761633315, state.value) : mix(title ? 1932337979 : 1932206902, -1454688673, state.value));
      int border = !button.active ? 625951582 : (accent ? mix(-1312646675, -2243329, state.value) : mix(1096698994, -1229680164, state.value));
      shadow(g, button.getX(), button.getY(), button.getWidth(), button.getHeight());
      panel(g, button.getX(), button.getY(), button.getWidth(), button.getHeight(), 8, fill, border);
      if (button instanceof LockIconButton lock) {
         lockIcon(g, button, lock.isLocked(), button.active ? -461569 : -7240289);
      } else {
         Font font = Minecraft.getInstance().font;
         String label = raw;
         int available = Math.max(0, button.getWidth() - 12);
         if (font.width(boldText(raw)) > available) {
            while (!label.isEmpty() && font.width(boldText(label + "…")) > available) {
               label = label.substring(0, label.length() - 1);
            }

            label = label + "…";
         }

         Component content = boldText(label);
         g.drawString(
            font,
            content,
            button.getX() + (button.getWidth() - font.width(content)) / 2,
            button.getY() + (button.getHeight() - 9) / 2,
            button.active ? -461569 : -7240289,
            false
         );
      }
   }

   private static void lockIcon(GuiGraphics g, AbstractButton button, boolean locked, int argb) {
      float cx = button.getX() + button.getWidth() / 2.0F;
      float cy = button.getY() + button.getHeight() / 2.0F;
      float r = 2.5F;
      float t = 1.4F;
      float legTop = locked ? cy - 2.5F : cy - 4.0F;
      ShapeMath.Mesh mesh = SmoothShapes.mesh(g);
      float px = cx - r;
      float py = legTop;

      for (int i = 1; i <= 10; i++) {
         double a = Math.PI + Math.PI * i / 10.0;
         float qx = cx + r * (float)Math.cos(a);
         float qy = legTop + r * (float)Math.sin(a);
         ShapeMath.line(mesh, px, py, qx, qy, t, argb);
         px = qx;
         py = qy;
      }

      ShapeMath.line(mesh, cx - r, legTop, cx - r, cy + 0.5F, t, argb);
      ShapeMath.line(mesh, cx + r, legTop, cx + r, locked ? cy + 0.5F : legTop + 0.8F, t, argb);
      ShapeMath.fill(mesh, cx - 4.0F, cy - 0.5F, 8.0F, 6.0F, 1.6F, argb);
      ShapeMath.fill(mesh, cx - 0.9F, cy + 1.2F, 1.8F, 1.8F, 0.9F, -802939856);
      SmoothShapes.submit(g, mesh);
   }

   private static final class Hover {
      float value;
      long last;
   }
}
