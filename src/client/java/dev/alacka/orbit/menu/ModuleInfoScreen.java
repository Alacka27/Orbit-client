package dev.alacka.orbit.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Small right-click help for modules without adjustable settings. */
public final class ModuleInfoScreen extends Screen implements OrbitWindow {
    private final Screen parent;
    private final String description;
    private int x, y, w, h;
    public ModuleInfoScreen(Screen parent, String module, String description) {
        super(Component.literal(module));
        this.parent = parent;
        this.description = description;
    }
    @Override protected void init() {
        w = Math.min(320, width - 24); h = Math.min(156, height - 20);
        x = (width - w) / 2; y = (height - h) / 2;
        addRenderableWidget(Button.builder(Component.literal("Бинд: " + dev.alacka.orbit.input.ModuleBinds.label(getTitle().getString())),
                b -> minecraft.setScreen(new BindScreen(this, getTitle().getString())))
                .bounds(x + 12, y + 32, w - 24, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose())
                .bounds(x + 12, y + h - 30, w - 24, 20).build());
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        GlassMenu.shadow(g, x, y, w, h);
        GlassMenu.panel(g, x, y, w, h, 10, GlassMenu.WINDOW_FILL, GlassMenu.WINDOW_BORDER);
        GlassMenu.heading(g, getTitle().getString(), x + 12, y + 12, 0xfffcf7ff);
        int lineY = y + 65;
        for (var line : font.split(Component.literal(description), w - 24)) {
            if (lineY + 9 > y + h - 38) break;
            g.drawString(font, line, x + 12, lineY, 0xffdccae8, false); lineY += 11;
        }
        super.render(g, mouseX, mouseY, delta);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}