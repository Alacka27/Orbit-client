package dev.alacka.orbit.menu;

import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.config.BindPrefs;
import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.input.ModuleBinds;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Binding edits are drafts. Capture never reaches gameplay or the parent screen. */
public final class BindScreen extends Screen implements OrbitWindow {
    private final Screen parent;
    private final String module;
    private Integer draft;
    private boolean capturing;
    private String notice = "";
    private int x, y, w, h;
    private Button save, cancel, clear, defaults;

    public BindScreen(Screen parent, String module) {
        super(Component.literal(module + " · бинд"));
        if (!BindPrefs.MODULES.contains(module)) throw new IllegalArgumentException("Unknown module");
        this.parent = parent; this.module = module;
        draft = OrbitSettings.get().binds().keys().get(module);
    }

    private int selected() { return draft == null ? ModuleBinds.baseCode(module) : draft; }

    @Override protected void init() {
        clearWidgets();
        w = Math.min(336, width - 24); h = Math.min(188, height - 20);
        x = (width - w) / 2; y = (height - h) / 2;
        addRenderableWidget(Button.builder(Component.literal(capturing ? "Нажмите клавишу или кнопку мыши…" : "Клавиша: " + ModuleBinds.label(selected())), b -> {
            capturing = true; notice = ""; init();
        }).bounds(x + 12, y + 47, w - 24, 22).build());
        int half = (w - 30) / 2;
        clear = addRenderableWidget(Button.builder(Component.literal("Без бинда"), b -> {
            draft = BindPrefs.NONE; capturing = false; notice = ""; init();
        }).bounds(x + 12, y + 77, half, 20).build());
        clear.active = !BindPrefs.required(module);
        defaults = addRenderableWidget(Button.builder(Component.literal("По умолчанию"), b -> {
            draft = null; capturing = false; notice = ""; init();
        }).bounds(x + 18 + half, y + 77, half, 20).build());
        cancel = addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> onClose())
                .bounds(x + 12, y + h - 30, half, 20).build());
        save = addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> apply())
                .bounds(x + 18 + half, y + h - 30, half, 20).build());
        save.active = !capturing;
    }

    private void choose(int code) {
        if (!BindPrefs.validCode(code)) { notice = "Эта клавиша не поддерживается"; return; }
        if (!BindPrefs.allowed(module, code)) {
            notice = module.equals("Menu") ? "Для открытия Orbit нужна клавиша" : "Для встроенного действия нужен бинд";
            return;
        }
        if (!module.equals("Menu") && code == ModuleBinds.code("Menu")) {
            notice = "Клавиша открытия Orbit уже занята"; return;
        }
        if (module.equals("Menu") && BindPrefs.MODULES.stream().anyMatch(name ->
                !name.equals("Menu") && ModuleBinds.code(name) == code)) {
            notice = "Эта клавиша уже назначена в Orbit"; return;
        }
        draft = code; capturing = false; notice = ""; init();
    }

    @Override public boolean keyPressed(KeyEvent e) {
        if (capturing) {
            if (e.key() == GLFW.GLFW_KEY_ESCAPE) { capturing = false; notice = ""; init(); }
            else if (e.key() == GLFW.GLFW_KEY_DELETE || e.key() == GLFW.GLFW_KEY_BACKSPACE) choose(BindPrefs.NONE);
            else if (!module.equals("Menu") && ClickMenuController.matches(e)) notice = "Клавиша открытия Orbit уже занята";
            else if (e.key() < 0) notice = "Эта клавиша не поддерживается";
            else choose(e.key());
            return true;
        }
        return super.keyPressed(e);
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (capturing) {
            // Keep the visible escape/reset actions usable while listening for mouse binds.
            if (e.button() == 0 && (cancel.isMouseOver(e.x(), e.y())
                    || clear.isMouseOver(e.x(), e.y()) || defaults.isMouseOver(e.x(), e.y())))
                return super.mouseClicked(e, doubleClick);
            // AbstractWidget.isMouseOver() excludes inactive widgets, so test its box directly.
            if (e.button() == 0 && (inside(save,e) || !clear.active && inside(clear,e))) return true;
            choose(BindPrefs.MOUSE + e.button()); return true;
        }
        return super.mouseClicked(e, doubleClick);
    }
    private static boolean inside(Button button, MouseButtonEvent e) {
        return e.x() >= button.getX() && e.x() < button.getX() + button.getWidth()
                && e.y() >= button.getY() && e.y() < button.getY() + button.getHeight();
    }

    private void apply() {
        try {
            var current = OrbitSettings.get();
            var next = draft == null ? current.binds().without(module) : current.binds().with(module, draft);
            OrbitSettings.save(current.withBinds(next));
            onClose();
        } catch (Exception error) {
            notice = "Ошибка сохранения; файл не изменён";
            OrbitClient.LOGGER.error("[Orbit] Cannot save module binding", error);
        }
    }

    @Override public boolean accent(AbstractButton button) { return button == save; }

    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        GlassMenu.shadow(g, x, y, w, h);
        GlassMenu.panel(g, x, y, w, h, 10, GlassMenu.WINDOW_FILL, GlassMenu.WINDOW_BORDER);
        GlassMenu.heading(g, getTitle().getString(), x + 12, y + 12, 0xfffcf7ff);
        GlassMenu.text(g, font.plainSubstrByWidth(ModuleBinds.mode(module), w - 24), x + 12, y + 29, 0xffdccae8);
        String shared = selected() == BindPrefs.NONE ? "" : BindPrefs.MODULES.stream()
                .filter(name -> !name.equals(module) && ModuleBinds.code(name) == selected())
                .collect(Collectors.joining(", "));
        String help = capturing ? (BindPrefs.required(module) ? "Esc — отменить · встроенное управление" : "Esc — отменить · Del — снять бинд")
                : shared.isEmpty() ? "Клавиша сохраняется для текущего профиля"
                : "Также: " + shared + " · сработают вместе";
        int lineY = y + 108;
        for (var line : font.split(Component.literal(help), w - 24)) {
            if (lineY + 9 > y + h - 52) break;
            g.drawString(font, line, x + 12, lineY, shared.isEmpty() ? 0xffcdbfd9 : 0xffffd596, false);
            lineY += 11;
        }
        if (!notice.isEmpty()) GlassMenu.text(g, font.plainSubstrByWidth(notice, w - 24), x + 12, y + h - 46, 0xffffb5c1);
        super.render(g, mx, my, delta);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}