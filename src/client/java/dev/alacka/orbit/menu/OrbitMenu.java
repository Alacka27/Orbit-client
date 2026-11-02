package dev.alacka.orbit.menu;

import dev.alacka.orbit.OrbitClient;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Styles the original title-screen actions. Does not replace their permission checks or callbacks. */
public final class OrbitMenu {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("orbit", "icon.png");
    private static boolean enabled = true;
    private static boolean animation = true;
    private static OrbitMenu current;
    private final Screen screen;
    private final List<AbstractWidget> buttons;
    private Button fallback;
    private Button profiles;
    private final int logoSize;
    private final int top;
    private final int firstButtonY;

    public static void initialize() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("orbit/mainmenu.properties");
        try {
            Properties values = new Properties();
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { values.load(reader); }
            } else {
                Files.createDirectories(path.getParent());
                values.setProperty("enabled", "true");
                values.setProperty("animation", "true");
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    values.store(writer, "Orbit blue main menu. Set enabled=false for vanilla. Restart to reload.");
                }
            }
            enabled = booleanSetting(values, "enabled", true);
            animation = booleanSetting(values, "animation", true);
        } catch (Exception error) {
            enabled = false;
            OrbitClient.LOGGER.error("[Orbit] Main menu disabled: config error. Existing file preserved.", error);
        }
    }

    private static boolean booleanSetting(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key, Boolean.toString(defaultValue)).strip();
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException(key + " must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    private OrbitMenu(Screen screen, List<AbstractWidget> buttons) {
        this.screen = screen;
        this.buttons = buttons;
        boolean compact = screen.height < 300;
        logoSize = compact ? 52 : 82;
        int h = compact ? 22 : 28;
        int contentHeight = logoSize + 39 + h * 4 + 6 * 3;
        top = Math.max(8, (screen.height - contentHeight - 22) / 2);
        firstButtonY = top + logoSize + 39;
        int w = Math.min(220, screen.width - 48);
        for (int i = 0; i < buttons.size(); i++) {
            AbstractWidget button = buttons.get(i);
            button.setPosition((screen.width - w) / 2, firstButtonY + i * (h + 6));
            button.setWidth(w);
            button.setHeight(h);
        }
    }

    public static OrbitMenu install(Screen screen) {
        current = null;
        if (!enabled || screen.width < 320 || screen.height < 240) return null;
        String[] keys = {"menu.singleplayer", "menu.multiplayer", "menu.options", "menu.quit"};
        List<AbstractWidget> selected = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            String label = Component.translatable(keys[i]).getString();
            AbstractWidget found = null;
            for (var child : screen.children()) {
                if (child instanceof Button button && button.visible
                        && (button.getMessage().getString().equals(label)
                        || (i == 0 && button.getMessage().getString()
                        .equals(Component.translatable("menu.playdemo").getString())))) {
                    found = button;
                    break;
                }
            }
            // Unsupported layouts (including demo variants without Multiplayer) keep vanilla intact.
            if (found == null) return null;
            selected.add(found);
        }
        current = new OrbitMenu(screen, selected);
        current.hideUnmanaged();
        return current;
    }

    public void setFallback(Button button) { fallback = button; }
    public void setProfiles(Button button) { profiles = button; }
    public static boolean isEnabled() { return enabled; }
    public static boolean isAnimated() { return animation; }
    public static void useVanillaForSession() { enabled = false; current = null; }
    private void hideUnmanaged() {
        for (var child : screen.children()) {
            if (child instanceof AbstractWidget widget && !buttons.contains(widget) && widget != fallback && widget != profiles) {
                widget.visible = false;
            }
        }
    }

    public void renderBackground(GuiGraphics g) {
        hideUnmanaged();
        int w = screen.width, h = screen.height;
        GlassMenu.backdrop(g, w, h);
        int panelWidth = buttons.get(0).getWidth() + 28;
        int bottom = buttons.get(buttons.size() - 1).getY() + buttons.get(buttons.size() - 1).getHeight();
        int panelBottom = Math.min(bottom + 12, h - 31);
        GlassMenu.panel(g, (w - panelWidth) / 2, top - 4, panelWidth, panelBottom - (top - 4),
                14, 0x60180c35, 0x1dffffff);
        int logoX = (w - logoSize) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, ICON, logoX, top, 0f, 0f,
                logoSize, logoSize, 512, 512, 512, 512, 0xffffffff);
        g.pose().pushMatrix();
        g.pose().scale(2f, 2f);
        g.drawCenteredString(Minecraft.getInstance().font, "O R B I T", w / 4, (top + logoSize + 1) / 2, 0xffe3efff);
        g.pose().popMatrix();
        g.drawCenteredString(Minecraft.getInstance().font, "YOUR WORLD. YOUR ORBIT.", w / 2,
                firstButtonY - 13, 0xffded3f5);
        var font = Minecraft.getInstance().font;
        String user = Minecraft.getInstance().getUser().getName();
        int maxWidth = Math.max(40, (w - logoSize) / 2 - 24);
        while (user.length() > 1 && font.width(user) > maxWidth) user = user.substring(0, user.length() - 1);
        g.drawString(font, user, w - font.width(user) - 12, 12, 0xfff0eaff, false);
        g.fill(12, 12, 15, 21, 0xffdebdff);
        g.drawString(font, "ORBIT / 0.4.1", 21, 12, 0xffe8ddfa, false);
        g.drawString(font, "Minecraft 1.21.11", 12, h - 11, 0xffeee7fa, false);
        String rights = "Not affiliated with Mojang";
        g.drawString(font, rights, w - font.width(rights) - 12, h - 11, 0xffeee7fa, false);
    }

    public static boolean drawButton(GuiGraphics g, AbstractButton button) {
        OrbitMenu menu = current;
        if (menu == null || Minecraft.getInstance().screen != menu.screen) return false;
        int index = menu.buttons.indexOf(button);
        if (index < 0 && button != menu.fallback && button != menu.profiles) return false;
        GlassMenu.button(g, button, index == 0);
        return true;
    }
}
