package dev.alacka.orbit.menu;

import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.config.ConfigStore;
import dev.alacka.orbit.config.DesktopLocator;
import dev.alacka.orbit.config.FreeLookConfig;
import dev.alacka.orbit.config.OrbitConfig;
import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.freelook.FreeLookController;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

public final class ConfigsScreen extends Screen implements OrbitWindow {
   private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
   private static Path folder;
   private static boolean desktop;
   private static String lastLoaded = "";
   private static final Map<Path, ConfigsScreen.Check> CHECKS = new HashMap<>();
   private final Screen parent;
   private final WindowDrag drag = new WindowDrag("configs");
   private ProfileListLayout layout;
   private List<ConfigStore.Entry> entries = List.of();
   private String selected;
   private String notice = "";
   private String editText = "";
   private int offset;
   private int ticks;
   private EditBox input;
   private Button load;
   private Button overwrite;
   private Button delete;

   public ConfigsScreen(Screen parent) {
      super(Component.literal("Конфиги"));
      this.parent = parent;
   }

   public static Path folder() {
      if (folder == null) {
         Path fallback = FabricLoader.getInstance().getConfigDir().resolve("orbit/configs");

         try {
            Path found = DesktopLocator.desktop(DesktopLocator.system());
            desktop = found != null;
            folder = found == null ? fallback : found.resolve("Orbit Configs");
         } catch (Exception var2) {
            folder = fallback;
            OrbitClient.LOGGER.warn("[Orbit] Desktop not found; configs use {}", fallback, var2);
         }

         OrbitClient.LOGGER.info("[Orbit] Config folder: {}", folder);
      }

      return folder;
   }

   @Override
   protected void init() {
      this.rescan();
      this.buildControls();
   }

   private boolean valid(ConfigStore.Entry entry) {
      ConfigsScreen.Check check = CHECKS.get(entry.file());
      if (check == null || check.modified() != entry.modified() || check.size() != entry.size()) {
         boolean ok;
         try {
            ConfigStore.read(entry.file());
            ok = true;
         } catch (Exception var5) {
            ok = false;
         }

         check = new ConfigsScreen.Check(entry.modified(), entry.size(), ok);
         CHECKS.put(entry.file(), check);
      }

      return check.valid();
   }

   private boolean rescan() {
      List<ConfigStore.Entry> next;
      try {
         next = ConfigStore.list(folder());
      } catch (Exception var3) {
         next = List.of();
      }

      boolean changed = !next.equals(this.entries);
      this.entries = next;
      if (this.selected != null && ConfigStore.find(this.entries, this.selected) == null) {
         this.selected = null;
      }

      return changed;
   }

   private ConfigStore.Entry current() {
      return this.selected == null ? null : ConfigStore.find(this.entries, this.selected);
   }

   private void buildControls() {
      boolean focused = this.input != null && this.input.isFocused();
      int cursor = this.input == null ? 0 : this.input.getCursorPosition();
      if (this.input != null) {
         this.editText = this.input.getValue();
      }

      this.clearWidgets();
      this.layout = ProfileListLayout.configs(this.width, this.height, this.drag.dx(), this.drag.dy());
      int x = this.layout.x() + 12;
      int inner = this.layout.width() - 24;
      int rows = this.layout.rows();
      this.offset = Math.max(0, Math.min(this.offset, Math.max(0, this.entries.size() - rows)));
      int openWidth = this.layout.width() < 360 ? 70 : 96;
      this.addRenderableWidget(
         Button.builder(Component.literal(this.layout.width() < 360 ? "Папка" : "Открыть папку"), b -> this.openFolder())
            .bounds(this.layout.x() + this.layout.width() - 12 - openWidth, this.layout.y() + 10, openWidth, 20)
            .build()
      );
      if (this.entries.size() > rows) {
         int px = this.layout.x() + this.layout.width() - 12 - openWidth - 52;
         Button prev = this.addRenderableWidget(Button.builder(Component.literal("‹"), b -> {
            this.offset = Math.max(0, this.offset - rows);
            this.buildControls();
         }).bounds(px, this.layout.y() + 10, 22, 20).build());
         Button next = this.addRenderableWidget(Button.builder(Component.literal("›"), b -> {
            this.offset = Math.min(this.entries.size() - rows, this.offset + rows);
            this.buildControls();
         }).bounds(px + 26, this.layout.y() + 10, 22, 20).build());
         prev.active = this.offset > 0;
         next.active = this.offset + rows < this.entries.size();
      }

      for (int i = this.offset; i < Math.min(this.entries.size(), this.offset + rows); i++) {
         ConfigStore.Entry entry = this.entries.get(i);
         boolean ok = this.valid(entry);
         String detail = ok
            ? "Изменён " + DATE.format(Instant.ofEpochMilli(entry.modified()).atZone(ZoneId.systemDefault()))
            : "Файл повреждён — загрузить нельзя";
         String status = entry.name().equals(lastLoaded) ? "Загружен" : "";
         this.addRenderableWidget(
            new ConfigRowButton(
               x,
               this.layout.y() + this.layout.rowY() + (i - this.offset) * this.layout.step(),
               inner,
               this.layout.rowHeight(),
               entry.name(),
               detail,
               status,
               entry.name().equals(this.selected),
               ok,
               b -> {
                  this.selected = entry.name();
                  this.notice = "";
                  this.buildControls();
               }
            )
         );
      }

      int editY = this.layout.editY();
      this.input = this.addRenderableWidget(new EditBox(this.font, x, editY, inner - 112, 22, Component.literal("Название конфига")));
      this.input.setMaxLength(96);
      this.input.setFilter(s -> s.codePoints().allMatch(ConfigStore::allowed) && s.codePointCount(0, s.length()) <= 48);
      this.input.setHint(Component.literal("Название конфига"));
      this.input.setValue(this.editText);
      this.input.setCursorPosition(Math.min(cursor, this.editText.length()));
      this.input.setHighlightPos(this.input.getCursorPosition());
      if (focused) {
         this.setFocused(this.input);
      }

      this.addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> this.saveNew()).bounds(x + inner - 106, editY, 106, 22).build());
      int quarter = (inner - 18) / 4;
      int y = this.layout.footerY();
      ConfigStore.Entry entry = this.current();
      this.load = this.addRenderableWidget(Button.builder(Component.literal("Загрузить"), b -> this.loadSelected()).bounds(x, y, quarter, 22).build());
      this.overwrite = this.addRenderableWidget(
         Button.builder(Component.literal("Перезаписать"), b -> this.confirmOverwrite()).bounds(x + quarter + 6, y, quarter, 22).build()
      );
      this.delete = this.addRenderableWidget(
         Button.builder(Component.literal("Удалить"), b -> this.confirmDelete()).bounds(x + 2 * (quarter + 6), y, quarter, 22).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.back"), b -> this.onClose()).bounds(x + 3 * (quarter + 6), y, inner - 3 * (quarter + 6), 22).build()
      );
      this.load.active = entry != null && this.valid(entry);
      this.overwrite.active = entry != null;
      this.delete.active = entry != null;
   }

   private OrbitConfig snapshot() {
      return new OrbitConfig(OrbitSettings.get(), OrbitSettings.savedHud(), FreeLookController.enabled(), FreeLookController.returnSpeed());
   }

   private void saveNew() {
      String name = ConfigStore.sanitize(this.input.getValue());
      if (name.isEmpty()) {
         this.notice = "Введи название конфига";
      } else if (ConfigStore.find(this.entries, name) != null) {
         this.notice = "«" + name + "» уже есть — выбери его и нажми «Перезаписать»";
         this.selected = ConfigStore.find(this.entries, name).name();
         this.buildControls();
      } else {
         try {
            ConfigStore.create(folder(), name, this.snapshot(), OrbitClient.version());
            this.notice = "Сохранено: " + name + " · " + folderLabel();
            this.selected = name;
            this.editText = "";
            this.input.setValue("");
         } catch (FileAlreadyExistsException var3) {
            this.notice = "«" + name + "» уже есть — выбери его и нажми «Перезаписать»";
         } catch (Exception var4) {
            this.notice = "Не удалось сохранить: нет доступа к папке";
            OrbitClient.LOGGER.error("[Orbit] Config not saved", var4);
         }

         this.rescan();
         int index = this.indexOf(this.selected);
         if (index >= 0 && (index < this.offset || index >= this.offset + this.layout.rows())) {
            this.offset = index;
         }

         this.buildControls();
      }
   }

   private int indexOf(String name) {
      for (int i = 0; i < this.entries.size(); i++) {
         if (this.entries.get(i).name().equals(name)) {
            return i;
         }
      }

      return -1;
   }

   private void loadSelected() {
      ConfigStore.Entry entry = this.current();
      if (entry != null) {
         try {
            OrbitConfig config = ConfigStore.read(entry.file());
            OrbitSettings.saveAll(config.settings(), config.hud());

            try {
               FreeLookController.apply(new FreeLookConfig(config.freeLook(), config.returnSpeed()));
            } catch (Exception var4) {
               this.notice = "Загружено, но FreeLook не сохранён";
               OrbitClient.LOGGER.error("[Orbit] FreeLook from config not saved", var4);
            }

            lastLoaded = entry.name();
            if (this.notice.isEmpty() || !this.notice.startsWith("Загружено,")) {
               this.notice = "Загружено: " + entry.name();
            }

            this.drag.reload();
         } catch (Exception var5) {
            this.notice = "Не удалось загрузить: файл повреждён или недоступен";
            OrbitClient.LOGGER.error("[Orbit] Config not loaded", var5);
         }

         this.rescan();
         this.buildControls();
      }
   }

   private void confirmOverwrite() {
      ConfigStore.Entry entry = this.current();
      if (entry != null) {
         this.minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
               try {
                  ConfigStore.overwrite(entry.file(), entry.name(), this.snapshot(), OrbitClient.version());
                  this.notice = "Перезаписано: " + entry.name();
               } catch (Exception var4) {
                  this.notice = "Не удалось перезаписать: файл не изменён";
                  OrbitClient.LOGGER.error("[Orbit] Config not overwritten", var4);
               }
            }

            this.minecraft.setScreen(this);
         }, Component.literal("Перезаписать «" + entry.name() + "»?"), Component.literal("Текущие настройки Orbit заменят содержимое этого конфига.")));
      }
   }

   private void confirmDelete() {
      ConfigStore.Entry entry = this.current();
      if (entry != null) {
         this.minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
               try {
                  ConfigStore.delete(entry.file());
                  this.notice = "Удалено: " + entry.name();
                  if (entry.name().equals(lastLoaded)) {
                     lastLoaded = "";
                  }

                  this.selected = null;
               } catch (Exception var4) {
                  this.notice = "Не удалось удалить файл";
                  OrbitClient.LOGGER.error("[Orbit] Config not deleted", var4);
               }
            }

            this.minecraft.setScreen(this);
         }, Component.literal("Удалить «" + entry.name() + "»?"), Component.literal("Файл будет удалён из папки Orbit Configs.")));
      }
   }

   private void openFolder() {
      try {
         Files.createDirectories(folder());
         Util.getPlatform().openPath(folder());
         this.notice = "Открываю папку " + folderLabel();
      } catch (Exception var2) {
         this.notice = "Не удалось открыть папку: " + folder();
         OrbitClient.LOGGER.error("[Orbit] Config folder not opened", var2);
      }
   }

   private static String folderLabel() {
      return desktop ? "Рабочий стол/Orbit Configs" : "config/orbit/configs";
   }

   @Override
   public void onFilesDrop(List<Path> files) {
      List<String> added = new ArrayList<>();
      List<String> failed = new ArrayList<>();

      for (Path file : files) {
         try {
            String name = ConfigStore.importFile(folder(), file);
            if (name != null) {
               added.add(name);
            } else {
               failed.add(file.getFileName() + " (не .orbit)");
            }
         } catch (Exception var7) {
            failed.add(String.valueOf(file.getFileName()));
         }
      }

      if (!added.isEmpty()) {
         this.selected = added.get(added.size() - 1);
      }

      this.notice = (added.isEmpty() ? "" : "Добавлено: " + String.join(", ", added))
         + (failed.isEmpty() ? "" : (added.isEmpty() ? "" : " · ") + "Не добавлено: " + String.join(", ", failed));
      this.rescan();
      int index = this.indexOf(this.selected);
      if (index >= 0 && (index < this.offset || index >= this.offset + this.layout.rows())) {
         this.offset = index;
      }

      this.buildControls();
   }

   @Override
   public void tick() {
      super.tick();
      if (++this.ticks % 30 == 0 && this.rescan()) {
         this.buildControls();
      }
   }

   @Override
   public boolean accent(AbstractButton button) {
      return button == this.load;
   }

   @Override
   public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      int listTop = this.layout.y() + this.layout.rowY();
      int listBottom = listTop + this.layout.rows() * this.layout.step();
      if (x >= this.layout.x() && x < this.layout.x() + this.layout.width() && y >= listTop && y < listBottom && this.entries.size() > this.layout.rows()) {
         int next = Math.max(0, Math.min(this.entries.size() - this.layout.rows(), this.offset + (vertical < 0.0 ? 1 : (vertical > 0.0 ? -1 : 0))));
         if (next != this.offset) {
            this.offset = next;
            this.buildControls();
         }

         return true;
      } else {
         return super.mouseScrolled(x, y, horizontal, vertical);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
      return this.drag.press(this, e, this.layout.x(), this.layout.y(), this.layout.width()) ? true : super.mouseClicked(e, doubleClick);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
      if (!this.drag.dragging()) {
         return super.mouseDragged(e, dx, dy);
      } else {
         ProfileListLayout next = ProfileListLayout.configs(this.width, this.height, this.drag.targetX(e.x()), this.drag.targetY(e.y()));
         ProfileListLayout centre = ProfileListLayout.configs(this.width, this.height, 0, 0);
         WindowDrag.shift(this, next.x() - this.layout.x(), next.y() - this.layout.y());
         this.drag.applied(next.x() - centre.x(), next.y() - centre.y());
         this.layout = next;
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
      int left = this.layout.x();
      int top = this.layout.y();
      int w = this.layout.width();
      int h = this.layout.height();
      GlassMenu.shadow(g, left, top, w, h);
      GlassMenu.panel(g, left, top, w, h, 12, 773328934, 1604948418);
      this.drag.render(this, g, mx, my, left, top, w);
      GlassMenu.heading(g, "Конфиги", left + 12, top + 12, -1);
      int headerRoom = w - 24 - (w < 360 ? 70 : 96) - (this.entries.size() > this.layout.rows() ? 56 : 4);
      GlassMenu.text(g, this.font.plainSubstrByWidth("Папка: " + folderLabel(), headerRoom), left + 12, top + 27, -1648911);
      if (this.entries.isEmpty()) {
         int cy = top + this.layout.rowY() + Math.min(this.layout.rows() * this.layout.step(), 80) / 2 - 10;
         GlassMenu.centeredHeading(g, "Нет конфигов", left + w / 2, cy, -660225);
      }

      String line = this.notice;
      int y = this.layout.noticeY();

      for (FormattedCharSequence text : this.font.split(Component.literal(line), w - 24)) {
         if (y + 9 > top + h - 34) {
            break;
         }

         g.drawString(this.font, text, left + 12, y, this.notice.isEmpty() ? -2043666 : -595201, false);
         y += 11;
      }

      super.render(g, mx, my, delta);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private record Check(long modified, long size, boolean valid) {
   }
}
