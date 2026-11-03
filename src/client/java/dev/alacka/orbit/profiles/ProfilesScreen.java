package dev.alacka.orbit.profiles;

import dev.alacka.orbit.menu.GlassMenu;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Offline profiles and the launcher session; changes apply after game restart. */
public final class ProfilesScreen extends Screen {
    private final Screen parent;
    private ProfileStore.Data data;
    private int index;
    private String notice = "";
    private EditBox input;
    private int left, top, panelWidth, panelHeight;

    public ProfilesScreen(Screen parent) { super(Component.literal("Profiles")); this.parent = parent; }
    @Override protected void init() {
        try { data = ProfileStore.read(ProfileLaunch.path()); }
        catch (Exception e) { data = null; notice = "Cannot read profiles; file preserved."; }
        buildControls();
    }
    private String nameAt(int i) { return i == 0 ? ProfileStore.LAUNCHER : data.names().get(i - 1); }
    private void persist(ProfileStore.Data next, String message) {
        try { ProfileStore.write(ProfileLaunch.path(), next); data = next; notice = message; }
        catch (IOException e) { notice = "Could not save profiles; existing file preserved."; }
    }
    private void buildControls() {
        clearWidgets();
        panelWidth = Math.min(360, width - 24); panelHeight = Math.min(216, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        int inner = panelWidth - 24, x = left + 12;
        if (data != null) {
            index = Math.max(0, Math.min(index, data.names().size()));
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { index = Math.max(0, index - 1); buildControls(); }).bounds(x, top + 50, 28, 22).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { index = Math.min(data.names().size(), index + 1); buildControls(); }).bounds(x + inner - 28, top + 50, 28, 22).build());
            addRenderableWidget(Button.builder(Component.literal("Select"), b -> {
                persist(ProfileStore.select(data, nameAt(index)), "Saved. Restart Minecraft to apply."); buildControls();
            }).bounds(x, top + 77, (inner - 6) / 2, 22).build());
            Button delete = addRenderableWidget(Button.builder(Component.literal("Delete"), b -> {
                persist(ProfileStore.delete(data, nameAt(index)), "Profile removed. Restart to apply changes.");
                index = Math.max(0, index - 1); buildControls();
            }).bounds(x + (inner + 6) / 2, top + 77, (inner - 6) / 2, 22).build());
            delete.active = index > 0;
            input = addRenderableWidget(new EditBox(font, x, top + 113, inner - 142, 22, Component.literal("Offline nickname")));
            input.setMaxLength(16); input.setHint(Component.literal("Offline nickname"));
            addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
                try { ProfileStore.Data next = ProfileStore.add(data, input.getValue().trim()); persist(next, "Profile saved. Select it, then restart."); index = data.names().size(); }
                catch (IllegalArgumentException e) { notice = e.getMessage(); }
                buildControls();
            }).bounds(x + inner - 136, top + 113, 64, 22).build());
            Button rename = addRenderableWidget(Button.builder(Component.literal("Rename"), b -> {
                try { persist(ProfileStore.rename(data, nameAt(index), input.getValue().trim()), "Renamed. Restart to apply if selected."); }
                catch (IllegalArgumentException e) { notice = e.getMessage(); }
                buildControls();
            }).bounds(x + inner - 68, top + 113, 68, 22).build());
            rename.active = index > 0;
        }
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(x + inner - 76, top + panelHeight - 27, 76, 20).build());
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        GlassMenu.shadow(g, left, top, panelWidth, panelHeight);
        GlassMenu.panel(g, left, top, panelWidth, panelHeight, 12, GlassMenu.WINDOW_FILL, GlassMenu.WINDOW_BORDER);
        GlassMenu.text(g, "Profiles", left + 12, top + 10, 0xffffffff);
        GlassMenu.panel(g, left + 46, top + 48, panelWidth - 92, 26, 8, 0x754e346c, 0x709674bb);
        if (data != null) {
            String chosen = nameAt(index);
            String label = index == 0 ? "Launcher account" : chosen + " (offline)";
            g.drawString(font, GlassMenu.text(label), (width - font.width(GlassMenu.text(label))) / 2, top + 57, 0xfff6edff, false);
            String active = ProfileLaunch.active().equals(ProfileStore.LAUNCHER) ? "Launcher" : ProfileLaunch.active() + " (offline)";
            g.drawString(font, font.plainSubstrByWidth("Active: " + active, panelWidth - 24), left + 12, top + 27, 0xfff4edff, true);
            String next = data.selected().equals(ProfileStore.LAUNCHER) ? "Launcher" : data.selected() + " (offline)";
            g.drawString(font, font.plainSubstrByWidth("Next launch: " + next, panelWidth - 24), left + 12, top + 140, 0xfff4edff, true);
        }
        g.drawString(font, font.plainSubstrByWidth(notice, panelWidth - 24), left + 12, top + 154, 0xfff1ddff, true);
        g.drawString(font, font.plainSubstrByWidth("Offline: servers without account verification only.", panelWidth - 24), left + 12, top + 167, 0xfff4edff, true);
        super.render(g, mouseX, mouseY, delta);
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
