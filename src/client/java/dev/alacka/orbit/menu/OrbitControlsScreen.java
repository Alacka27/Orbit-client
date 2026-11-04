package dev.alacka.orbit.menu;

import dev.alacka.orbit.config.BindPrefs;
import dev.alacka.orbit.input.ModuleBinds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The sole control directory. Opening Minecraft Controls never adds Orbit entries. */
public final class OrbitControlsScreen extends Screen implements OrbitWindow {
    private final Screen parent;
    private ModuleSettingsLayout box;
    private int page;
    public OrbitControlsScreen(Screen parent) { super(Component.literal("Управление Orbit")); this.parent = parent; }

    @Override protected void init() {
        clearWidgets();
        box = ModuleSettingsLayout.of(width, height);
        int pages = box.pages(BindPrefs.MODULES.size());
        page = Math.clamp(page, 0, pages - 1);
        Button previous = Button.builder(Component.literal("<"), b -> { page--; init(); })
                .bounds(box.x()+box.width()-92,box.y()+10,22,20).build();
        previous.active = page > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal(">"), b -> { page++; init(); })
                .bounds(box.x()+box.width()-32,box.y()+10,22,20).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);
        for (int row = 0; row < box.rows(); row++) {
            int index = page * box.rows() + row;
            if (index >= BindPrefs.MODULES.size()) break;
            String name = BindPrefs.MODULES.get(index);
            String label = name.equals("Menu") ? "Открыть Orbit" : name;
            if (name.equals("FreeLook") || name.equals("Zoom")) label += " · удержание";
            addRenderableWidget(Button.builder(Component.literal(label + ": " + ModuleBinds.label(name)),
                    b -> minecraft.setScreen(new BindScreen(this,name)))
                    .bounds(box.x()+12,box.controlY(row),box.width()-24,box.rowHeight()).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose())
                .bounds(box.x()+12,box.footerY(),box.width()-24,20).build());
    }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        GlassMenu.shadow(g,box.x(),box.y(),box.width(),box.height());
        GlassMenu.panel(g,box.x(),box.y(),box.width(),box.height(),12,GlassMenu.WINDOW_FILL,GlassMenu.WINDOW_BORDER);
        GlassMenu.heading(g,"Управление",box.x()+12,box.y()+12,0xfffcf7ff);
        g.drawCenteredString(font,(page+1)+"/"+box.pages(BindPrefs.MODULES.size()),
                box.x()+box.width()-51,box.y()+16,0xfff5e9ff);
        GlassMenu.text(g,"Бинды сохраняются отдельно для профиля.",box.x()+12,box.helpY(),0xffdccae8);
        super.render(g,mx,my,delta);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}