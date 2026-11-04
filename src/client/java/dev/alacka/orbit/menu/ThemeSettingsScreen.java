package dev.alacka.orbit.menu;

import dev.alacka.orbit.config.ClientSettings;
import dev.alacka.orbit.config.HudPrefs;
import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.hud.HudElements;
import dev.alacka.orbit.render.SmoothShapes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ThemeSettingsScreen extends Screen implements OrbitWindow {
   private static final int[] SWATCHES = new int[]{0xa15be0, 0x6488e8, 0x48b9b2, 0x58a69d, 0xd2a454, 0xde729c, 0xd75b63, 0xa3a5af};
   private final Screen parent;
   private final WindowDrag drag = new WindowDrag("settings");
   private ClientSettings draft;
   private HudPrefs hudDraft;
   private ThemeSettingsScreen.Page page = ThemeSettingsScreen.Page.THEME;
   private ThemeLayout box;
   private EditBox hex;
   private String hexText;
   private boolean updating;
   private final List<ThemeSettingsScreen.ColorSlider> rgbSliders = new ArrayList<>();
   private Button apply;
   private Button themeTab;
   private Button hudTab;
   private String notice = "";

   public ThemeSettingsScreen(Screen parent) {
      super(Component.literal("Настройки Orbit"));
      this.parent = parent;
      this.draft = OrbitSettings.get();
      this.hudDraft = OrbitSettings.savedHud();
   }

   private HudPrefs hudPreview() {
      HudPrefs saved = OrbitSettings.savedHud();
      return this.hudDraft.withPlacements(saved.placements()).withWindows(saved.windows());
   }

   private void previewAll() {
      OrbitSettings.preview(this.draft);
      OrbitSettings.previewHud(this.hudPreview());
   }

   @Override
   protected void init() {
      this.rgbSliders.clear();
      this.box = ThemeLayout.of(this.width, this.height, this.drag.dx(), this.drag.dy());
      this.previewAll();
      int x = this.box.x() + 12;
      int w = this.box.controlWidth();
      int tabW = this.box.width() < 360 ? 54 : 62;
      int tabsX = this.box.x() + this.box.width() - 12 - 2 * tabW - 4;
      this.themeTab = this.addRenderableWidget(Button.builder(Component.literal("Тема"), b -> {
         this.page = ThemeSettingsScreen.Page.THEME;
         this.notice = "";
         this.rebuildWidgets();
      }).bounds(tabsX, this.box.y() + 8, tabW, 18).build());
      this.hudTab = this.addRenderableWidget(Button.builder(Component.literal("HUD / чат"), b -> {
         this.page = ThemeSettingsScreen.Page.HUD;
         this.notice = "";
         this.rebuildWidgets();
      }).bounds(tabsX + tabW + 4, this.box.y() + 8, tabW, 18).build());
      if (this.page == ThemeSettingsScreen.Page.THEME) {
         this.themePage(x, w);
      } else {
         this.hudPage(x, w);
      }

      int footerWidth = this.box.width() - 24;
      int third = (footerWidth - 12) / 3;
      int y = this.box.footerY();
      this.apply = this.addRenderableWidget(Button.builder(Component.literal("Применить"), b -> this.applyAll()).bounds(x, y, third, 22).build());
      this.addRenderableWidget(Button.builder(Component.literal("Сбросить"), b -> {
         if (this.page == ThemeSettingsScreen.Page.THEME) {
            ClientSettings d = ClientSettings.DEFAULT;
            this.draft = this.draft.theme(d.accent(), d.windowOpacity(), d.buttonOpacity(), d.animations());
            this.hexText = null;
         } else {
            HudPrefs d = HudPrefs.DEFAULT;
            this.hudDraft = this.hudDraft.withOpacity(d.opacity()).withVisibility(d.status(), d.coords(), d.modules());
            this.draft = this.draft.withTransparentChat(false);
         }

         this.notice = "Примените, чтобы сохранить";
         this.rebuildWidgets();
      }).bounds(x + third + 6, y, third, 22).build());
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.back"), b -> this.onClose()).bounds(x + 2 * (third + 6), y, footerWidth - 2 * (third + 6), 22).build()
      );
   }

   private void themePage(int x, int w) {
      this.hex = new EditBox(this.font, x, this.box.hexY(), w - 60, this.box.controlHeight(), Component.literal("Цвет HEX"));
      this.hex.setMaxLength(7);
      this.hex.setValue(this.hexText != null ? this.hexText : String.format("#%06X", this.draft.accent()));
      this.hex
         .setResponder(
            s -> {
               if (!this.updating) {
                  this.hexText = s;
                  if (s.matches("#?[a-fA-F0-9]{6}")) {
                     this.draft = this.draft
                        .theme(Integer.parseInt(s.replace("#", ""), 16), this.draft.windowOpacity(), this.draft.buttonOpacity(), this.draft.animations());
                     OrbitSettings.preview(this.draft);
                     this.syncColors();
                     this.notice = "";
                  } else {
                     this.notice = "Цвет: #RRGGBB, например #945CDA";
                  }
               }
            }
         );
      this.addRenderableWidget(this.hex);
      if (!this.box.compact()) {
         String[] labels = new String[]{"Красный", "Зелёный", "Синий"};

         for (int i = 0; i < 3; i++) {
            int shift = (2 - i) * 8;
            this.rgbSliders
               .add(this.slider(x, this.box.y() + 75 + i * 26, w, this.box.controlHeight(), labels[i], this.draft.accent() >> shift & 0xFF, 0, 255, "", v -> {
                  int colorx = this.draft.accent() & ~(255 << shift) | v << shift;
                  this.draft = this.draft.theme(colorx, this.draft.windowOpacity(), this.draft.buttonOpacity(), this.draft.animations());
                  this.setHex(colorx);
                  OrbitSettings.preview(this.draft);
                  this.notice = "";
               }));
         }
      }

      this.slider(x, this.box.opacityY(), w, this.box.controlHeight(), "Плотность окна", this.draft.windowOpacity(), 0, 85, "%", v -> {
         this.draft = this.draft.theme(this.draft.accent(), v, this.draft.buttonOpacity(), this.draft.animations());
         OrbitSettings.preview(this.draft);
      });
      this.slider(x, this.box.opacityY() + this.box.step(), w, this.box.controlHeight(), "Плотность кнопок", this.draft.buttonOpacity(), 35, 95, "%", v -> {
         this.draft = this.draft.theme(this.draft.accent(), this.draft.windowOpacity(), v, this.draft.animations());
         OrbitSettings.preview(this.draft);
      });
      this.addRenderableWidget(Button.builder(Component.literal("Анимации: " + onOff(this.draft.animations())), b -> {
         this.draft = this.draft.theme(this.draft.accent(), this.draft.windowOpacity(), this.draft.buttonOpacity(), !this.draft.animations());
         OrbitSettings.preview(this.draft);
         b.setMessage(Component.literal("Анимации: " + onOff(this.draft.animations())));
      }).bounds(x, this.box.opacityY() + 2 * this.box.step(), w, this.box.controlHeight()).build());
      if (this.box.preview()) {
         for (int i = 0; i < SWATCHES.length; i++) {
            int color = SWATCHES[i];
            this.addRenderableWidget(new ColorSwatch(this.box.previewX() + i * 23, this.swatchY(), color, () -> this.draft.accent(), b -> {
               this.draft = this.draft.theme(color, this.draft.windowOpacity(), this.draft.buttonOpacity(), this.draft.animations());
               this.setHex(color);
               this.syncColors();
               OrbitSettings.preview(this.draft);
               this.notice = "";
            }));
         }
      }
   }

   private void hudPage(int x, int w) {
      int step = this.box.tiny() ? 22 : (this.box.compact() ? 24 : 26);
      int h = this.box.tiny() ? 18 : (this.box.compact() ? 20 : 22);
      int y = this.box.hexY();
      boolean chips = this.box.noticeY() - 2 - y < 4 * step + h;
      this.slider(x, y, w, h, "Плотность HUD", this.hudDraft.opacity(), 20, 95, "%", v -> {
         this.hudDraft = this.hudDraft.withOpacity(v);
         OrbitSettings.previewHud(this.hudPreview());
      });
      String[] full = new String[]{"FPS и пинг", "Координаты", "Список модулей"};
      String[] shortNames = new String[]{"FPS", "XYZ", "Модули"};
      int chipW = (w - 8) / 3;

      for (int i = 0; i < 3; i++) {
         int index = i;
         String name = chips ? shortNames[i] : full[i];
         Button toggle = Button.builder(Component.literal(name + ": " + onOff(this.state(i))), b -> {
            boolean s = this.hudDraft.status();
            boolean c = this.hudDraft.coords();
            boolean m = this.hudDraft.modules();
            if (index == 0) {
               s = !s;
            } else if (index == 1) {
               c = !c;
            } else {
               m = !m;
            }

            this.hudDraft = this.hudDraft.withVisibility(s, c, m);
            OrbitSettings.previewHud(this.hudPreview());
            b.setMessage(Component.literal(name + ": " + onOff(this.state(index))));
         }).bounds(chips ? x + i * (chipW + 4) : x, chips ? y + step : y + (i + 1) * step, chips ? (i == 2 ? w - 2 * (chipW + 4) : chipW) : w, h).build();
         this.addRenderableWidget(toggle);
      }

      int chatY = chips ? y + 2 * step : y + 4 * step;
      this.addRenderableWidget(Button.builder(Component.literal("Прозрачный чат: " + onOff(draft.transparentChat())), b -> {
         draft = draft.withTransparentChat(!draft.transparentChat());
         OrbitSettings.preview(draft);
         b.setMessage(Component.literal("Прозрачный чат: " + onOff(draft.transparentChat())));
      }).bounds(x, chatY, w, h).build());
   }

   private boolean state(int index) {
      return index == 0 ? this.hudDraft.status() : (index == 1 ? this.hudDraft.coords() : this.hudDraft.modules());
   }

   private static String onOff(boolean value) {
      return value ? "ВКЛ" : "ВЫКЛ";
   }

   private void applyAll() {
      if (this.page == ThemeSettingsScreen.Page.THEME && this.hex != null && !this.hex.getValue().matches("#?[a-fA-F0-9]{6}")) {
         this.notice = "Цвет: #RRGGBB";
      } else {
         try {
            OrbitSettings.saveAll(this.draft, this.hudPreview());
            this.minecraft.setScreen(this.parent);
         } catch (Exception var2) {
            this.notice = "Ошибка сохранения; файл не изменён";
         }
      }
   }

   @Override
   public boolean accent(AbstractButton button) {
      return button == this.apply || button == (this.page == ThemeSettingsScreen.Page.THEME ? this.themeTab : this.hudTab);
   }

   private void setHex(int color) {
      this.hexText = String.format("#%06X", color);
      if (this.hex != null) {
         this.updating = true;
         this.hex.setValue(this.hexText);
         this.updating = false;
      }
   }

   private void syncColors() {
      for (int i = 0; i < this.rgbSliders.size(); i++) {
         this.rgbSliders.get(i).sync(this.draft.accent() >> (2 - i) * 8 & 0xFF);
      }
   }

   private ThemeSettingsScreen.ColorSlider slider(
      int x, int y, int width, int height, String label, int initial, int min, int max, String unit, IntConsumer change
   ) {
      return this.addRenderableWidget(new ThemeSettingsScreen.ColorSlider(x, y, width, height, label, initial, min, max, unit, change));
   }

   private int previewHeight() {
      return Math.max(60, Math.min(142, this.box.noticeY() - (this.box.y() + 49) - 36));
   }

   private int swatchY() {
      return this.box.y() + 49 + this.previewHeight() + 16;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
      return this.drag.press(this, e, this.box.x(), this.box.y(), this.box.width()) ? true : super.mouseClicked(e, doubleClick);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
      if (!this.drag.dragging()) {
         return super.mouseDragged(e, dx, dy);
      } else {
         ThemeLayout next = ThemeLayout.of(this.width, this.height, this.drag.targetX(e.x()), this.drag.targetY(e.y()));
         ThemeLayout centre = ThemeLayout.of(this.width, this.height);
         WindowDrag.shift(this, next.x() - this.box.x(), next.y() - this.box.y());
         this.drag.applied(next.x() - centre.x(), next.y() - centre.y());
         this.box = next;
         return true;
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent e) {
      if (this.drag.dragging()) {
         this.mouseDragged(e, 0.0, 0.0);
         this.drag.release();
         return true;
      } else {
         return super.mouseReleased(e);
      }
   }

   @Override
   public void render(GuiGraphics g, int mx, int my, float delta) {
      GlassMenu.shadow(g, this.box.x(), this.box.y(), this.box.width(), this.box.height());
      GlassMenu.panel(g, this.box.x(), this.box.y(), this.box.width(), this.box.height(), 12, 773328934, 1604948418);
      this.drag.render(this, g, mx, my, this.box.x(), this.box.y(), this.box.width());
      GlassMenu.heading(g, "Настройки Orbit", this.box.x() + 12, this.box.y() + 12, -1);
      if (!this.box.tiny()) {
         String sub = this.minecraft.getUser().getName();
         GlassMenu.text(g, this.font.plainSubstrByWidth(sub, this.box.width() - 24), this.box.x() + 12, this.box.y() + 27, -1121032);
      }

      if (this.page == ThemeSettingsScreen.Page.THEME && this.hex != null) {
         GlassMenu.pill(
            g, this.box.x() + this.box.controlWidth() - 46, this.box.hexY() + 3, 46.0F, this.box.controlHeight() - 6, 4.0F, 0xFF000000 | this.draft.accent()
         );
      }

      if (this.box.preview()) {
         this.renderPreview(g);
      }

      if (!this.notice.isEmpty()) {
         GlassMenu.text(g, this.font.plainSubstrByWidth(this.notice, this.box.width() - 24), this.box.x() + 12, this.box.noticeY(), -1);
      }

      super.render(g, mx, my, delta);
   }

   private void renderPreview(GuiGraphics g) {
      int px = this.box.previewX();
      int py = this.box.y() + 49;
      int pw = this.box.previewWidth();
      int ph = this.previewHeight();
      GlassMenu.text(g, "Предпросмотр", px, py - 15, -1450508);
      SmoothShapes.image(g, ThemeBackdrop.texture(), px, py, pw, ph, 6.0F, 1600, 900, -1);
      g.enableScissor(px, py, px + pw, py + ph);
      if (this.page == ThemeSettingsScreen.Page.THEME) {
         this.previewTheme(g, px, py, pw, ph);
      } else {
         this.previewHud(g, px, py, pw, ph);
      }

      g.disableScissor();
      GlassMenu.outline(g, px, py, pw, ph, 6.0F, UiTheme.tint(-1986298956, false));
   }

   private void previewTheme(GuiGraphics g, int px, int py, int pw, int ph) {
      int bw = (pw - 48) / 2;
      int wy = ph >= 100 ? py + (ph - 90) / 2 : py + 4;
      int wh = ph >= 100 ? 90 : ph - 8;
      GlassMenu.panel(g, px + 10, wy, pw - 20, wh, 8, 773328934, 1604948418);
      if (wh >= 60) {
         GlassMenu.text(g, "Стеклянное окно", px + 20, wy + 14, -1);
      }

      int by = wy + wh - 26;
      GlassMenu.panel(g, px + 20, by, bw, 22, 5, -577547573, -1379429400);
      GlassMenu.panel(g, px + 28 + bw, by, bw, 22, 5, -1722729630, 1954439828);
      GlassMenu.text(g, "Кнопка", px + 25, by + 7, -1);
      GlassMenu.text(g, "Назад", px + 33 + bw, by + 7, -1);
   }

   private void previewHud(GuiGraphics g, int px, int py, int pw, int ph) {
      HudPrefs prefs = this.hudPreview();
      if (prefs.status()) {
         HudElements.drawStatus(g, this.font, HudTheme.boldText("ORBIT  |  60 FPS"), px + 6, py + 6, 1.0F);
      }

      if (prefs.coords()) {
         HudElements.drawCoords(g, this.font, HudTheme.text("XYZ  128 / 64 / -42"), px + 6, py + ph - 21, 1.0F);
      }

      if (prefs.modules()) {
         List<HudElements.Row> rows = List.of(new HudElements.Row("Trajectories", 1.0F, false), new HudElements.Row("FreeLook", 1.0F, false));
         int w = HudElements.modulesWidth(this.font, rows);
         HudElements.drawModules(g, this.font, rows, px + pw - 6 - w, py + (prefs.status() ? 30 : 6), w, 2, 1.0F);
      }

      if (!prefs.status() && !prefs.coords() && !prefs.modules()) {
         GlassMenu.centeredHeading(g, "HUD Orbit скрыт", px + pw / 2, py + ph / 2 - 4, -725761);
      }
   }

   @Override
   public void removed() {
      OrbitSettings.cancelPreview();
   }

   @Override
   public void onClose() {
      OrbitSettings.cancelPreview();
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private static final class ColorSlider extends AbstractSliderButton {
      private final String label;
      private final String unit;
      private final int min;
      private final int max;
      private final IntConsumer change;

      ColorSlider(int x, int y, int width, int height, String label, int initial, int min, int max, String unit, IntConsumer change) {
         super(x, y, width, height, Component.empty(), (double)(initial - min) / (max - min));
         this.label = label;
         this.min = min;
         this.max = max;
         this.unit = unit;
         this.change = change;
         this.updateMessage();
      }

      void sync(int n) {
         this.value = (double)(n - this.min) / (this.max - this.min);
         this.updateMessage();
      }

      @Override
      protected void updateMessage() {
         this.setMessage(Component.literal(this.label + ": " + Math.round(this.min + this.value * (this.max - this.min)) + this.unit));
      }

      @Override
      protected void applyValue() {
         this.change.accept((int)Math.round(this.min + this.value * (this.max - this.min)));
      }
   }

   private static enum Page {
      THEME,
      HUD;
   }
}
