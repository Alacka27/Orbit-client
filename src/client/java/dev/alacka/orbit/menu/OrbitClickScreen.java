package dev.alacka.orbit.menu;

import com.mojang.authlib.GameProfile;
import dev.alacka.orbit.freelook.FreeLookController;
import dev.alacka.orbit.profiles.ProfileLaunch;
import dev.alacka.orbit.profiles.ProfileStore;
import dev.alacka.orbit.profiles.ProfilesScreen;
import java.util.EnumMap;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

/** Functional widgets; empty categories do not pretend to contain working modules. */
public final class OrbitClickScreen extends Screen {
    public enum Tab { General, Pvp, Visual, Fun, Settings }
    private final Screen parent;
    private Tab selected = Tab.General;
    private final EnumMap<Tab, Button> tabs = new EnumMap<>(Tab.class);
    private ClickMenuLayout box;
    private String notice = "";
    private Supplier<PlayerSkin> skin;
    private UUID skinId;

    public OrbitClickScreen(Screen parent) { super(Component.literal("Orbit")); this.parent = parent; }
    @Override protected void init() {
        clearWidgets(); tabs.clear(); box = ClickMenuLayout.of(width, height);
        for (Tab tab : Tab.values()) {
            Button button = Button.builder(Component.literal(tab.name()), b -> { selected = tab; notice = ""; init(); })
                    .bounds(box.x() + 8, box.tabY(tab.ordinal()), box.sidebar() - 16, box.tabHeight()).build();
            tabs.put(tab, addRenderableWidget(button));
        }
        int x = box.contentX(), w = box.contentWidth(), y = box.y() + 59;
        int row = box.height() < 230 ? 20 : 24;
        if (selected == Tab.General) {
            addRenderableWidget(Button.builder(Component.literal("FreeLook: " + (FreeLookController.enabled() ? "ON" : "OFF")), b -> {
                try { FreeLookController.setEnabled(!FreeLookController.enabled()); notice = "Сохранено"; }
                catch (Exception e) { notice = "Не удалось сохранить настройки"; }
                init();
            }).bounds(x, y, w, row).build());
        } else if (selected == Tab.Visual) {
            addRenderableWidget(Button.builder(Component.literal("Анимации: " + (OrbitMenu.isAnimated() ? "ON" : "OFF")), b -> {
                try { OrbitMenu.setAnimation(!OrbitMenu.isAnimated()); notice = "Сохранено"; }
                catch (Exception e) { notice = "Не удалось сохранить настройки"; }
                init();
            }).bounds(x, y, w, row).build());
        } else if (selected == Tab.Settings) {
            addRenderableWidget(Button.builder(Component.literal("Настройки Minecraft"), b ->
                    minecraft.setScreen(new OptionsScreen(this, minecraft.options))).bounds(x, y, w, row).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Profiles"), b -> minecraft.setScreen(new ProfilesScreen(this)))
                .bounds(box.x() + box.width() - 92, box.y() + box.height() - 32, 80, 22).build());
    }
    public boolean selectedButton(AbstractButton button) { return tabs.get(selected) == button; }
    private void line(GuiGraphics g, String value, int y, int color) {
        GlassMenu.text(g, font.plainSubstrByWidth(value, box.contentWidth()), box.contentX(), y, color);
    }
    private void lines(GuiGraphics g, String value, int y) {
        int limit = box.y() + box.height() - 59;
        for (var text : font.split(Component.literal(value), box.contentWidth())) {
            if (y + 9 > limit) break;
            g.drawString(font, text, box.contentX(), y, 0xfff2eaf9, false); y += 13;
        }
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        GlassMenu.shadow(g, box.x(), box.y(), box.width(), box.height());
        GlassMenu.panel(g, box.x(), box.y(), box.width(), box.height(), 12, GlassMenu.WINDOW_FILL, GlassMenu.WINDOW_BORDER);
        GlassMenu.panel(g, box.x() + 5, box.y() + 5, box.sidebar() - 10, box.height() - 10, 10, 0x26180e28, 0x188f6eac);
        GlassMenu.text(g, "O R B I T", box.x() + 12, box.y() + 12, 0xffffffff);
        line(g, selected.name(), box.y() + 15, 0xffffffff);
        line(g, "Orbit / 0.4.4", box.y() + 31, 0xffeee1fb);
        int body = box.y() + (box.height() < 230 ? 86 : 94);
        switch (selected) {
            case General -> lines(g, box.height() < 200 ? FreeLookController.keyLabel() + " · вид сзади" : "Свободный обзор: удерживай " + FreeLookController.keyLabel() + " в виде от третьего лица сзади. Профили доступны справа снизу.", body);
            case Visual -> lines(g, box.height() < 200 ? "Плавная подсветка кнопок." : "Плавная подсветка кнопок. Прозрачные панели, фиолетовый акцент и исходный фон с облаками.", body);
            case Settings -> lines(g, box.height() < 200 ? "Esc — закрыть меню" : "Меню: " + ClickMenuController.keyLabel() + ". Клавишу можно изменить в управлении Minecraft. Esc закрывает меню.", body);
            case Pvp, Fun -> lines(g, "В этой вкладке пока нет модулей. Рабочие функции появятся здесь по мере добавления.", box.y() + 63);
        }
        if (!notice.isEmpty()) line(g, notice, box.y() + box.height() - 49, 0xfff1dfff);
        drawPlayer(g);
        super.render(g, mouseX, mouseY, delta);
    }
    private void drawPlayer(GuiGraphics g) {
        int x = box.x() + 8, y = box.footerY(), w = box.sidebar() - 16;
        GlassMenu.panel(g, x, y, w, 32, 5, 0x591c132c, 0x399d7fbe);
        PlayerSkin currentSkin;
        String name;
        if (minecraft.player != null) {
            currentSkin = minecraft.player.getSkin(); name = minecraft.player.getGameProfile().name();
        } else {
            var user = minecraft.getUser(); name = user.getName();
            if (!user.getProfileId().equals(skinId)) {
                skinId = user.getProfileId();
                GameProfile profile = new GameProfile(skinId, name);
                skin = ProfileLaunch.active().equals(ProfileStore.LAUNCHER)
                        ? minecraft.getSkinManager().createLookup(profile, true)
                        : () -> DefaultPlayerSkin.get(profile);
            }
            currentSkin = skin.get();
        }
        PlayerFaceRenderer.draw(g, currentSkin, x + 5, y + 6, 20);
        GlassMenu.text(g, font.plainSubstrByWidth(name, w - 35), x + 31, y + 6, 0xffffffff);
        GlassMenu.text(g, "Игрок", x + 31, y + 19, 0xffe4d2f6);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
