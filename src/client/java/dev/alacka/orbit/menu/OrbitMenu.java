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
    public static void useVanillaForSession() { enabled = false; current = null; }
    private void hideUnmanaged() {
        for (var child : screen.children()) {
            if (child instanceof AbstractWidget widget && !buttons.contains(widget) && widget != fallback) {
                widget.visible = false;
            }
        }
    }

    public void renderBackground(GuiGraphics g) {
        hideUnmanaged();
        int w = screen.width, h = screen.height;
        for (int y = 0; y < h; y += 4) {
            double t = (double) y / h;
            int red = (int) (7 - 4 * t), green = (int) (20 - 11 * t), blue = (int) (41 - 20 * t);
            g.fill(0, y, w, Math.min(y + 4, h), 0xff000000 | red << 16 | green << 8 | blue);
        }
        double time = animation ? System.nanoTime() / 1_000_000_000.0 : 0;
        // Deliberately subtle, deterministic star field; no network or random allocations.
        for (int i = 0; i < 36; i++) {
            int x = (i * 97 + 29) % w, y = (i * 53 + 17) % h;
            int alpha = 45 + (int) (20 * Math.sin(time * 0.5 + i));
            g.fill(x, y, x + 1, y + 1, alpha << 24 | 0x72acff);
        }
        double angle = -0.28 + Math.sin(time * 0.08) * 0.035;
        int lastX = 0, lastY = 0;
        for (int i = 0; i <= 180; i++) {
            double a = i * Math.PI * 2 / 180;
            double x = Math.cos(a) * w * 0.47, y = Math.sin(a) * h * 0.30;
            int px = (int) (w / 2.0 + x * Math.cos(angle) - y * Math.sin(angle));
            int py = (int) (h * 0.44 + x * Math.sin(angle) + y * Math.cos(angle));
            if (i > 0) drawLine(g, lastX, lastY, px, py, 0x203d8df2);
            lastX = px; lastY = py;
        }
        int logoX = (w - logoSize) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, ICON, logoX, top, 0f, 0f,
                logoSize, logoSize, 512, 512, 512, 512, 0xffffffff);
        g.pose().pushMatrix();
        g.pose().scale(2f, 2f);
        g.drawCenteredString(Minecraft.getInstance().font, "O R B I T", w / 4, (top + logoSize + 1) / 2, 0xffe3efff);
        g.pose().popMatrix();
        g.drawCenteredString(Minecraft.getInstance().font, "YOUR WORLD. YOUR ORBIT.", w / 2,
                firstButtonY - 13, 0xff8ba6ce);
        var font = Minecraft.getInstance().font;
        String user = Minecraft.getInstance().getUser().getName();
        int maxWidth = Math.max(40, (w - logoSize) / 2 - 24);
        while (user.length() > 1 && font.width(user) > maxWidth) user = user.substring(0, user.length() - 1);
        g.drawString(font, user, w - font.width(user) - 12, 12, 0xffb9d7ff, false);
        g.fill(12, 12, 15, 21, 0xff409cff);
        g.drawString(font, "ORBIT / 0.3.1", 21, 12, 0xff86acdf, false);
        g.drawString(font, "Minecraft 1.21.11", 12, h - 11, 0xff8198b8, false);
        String rights = "Not affiliated with Mojang";
        g.drawString(font, rights, w - font.width(rights) - 12, h - 11, 0xff8198b8, false);
    }

    private static void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            int x = (int) (x1 + (x2 - x1) * t), y = (int) (y1 + (y2 - y1) * t);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    public static boolean drawButton(GuiGraphics g, AbstractButton button) {
        OrbitMenu menu = current;
        if (menu == null || Minecraft.getInstance().screen != menu.screen) return false;
        int index = menu.buttons.indexOf(button);
        if (index < 0 && button != menu.fallback) return false;
        boolean hover = button.active && button.isHoveredOrFocused();
        boolean primary = index == 0;
        int x = button.getX(), y = button.getY(), w = button.getWidth(), h = button.getHeight();
        int border = hover ? 0xff88c8ff : primary ? 0xff438fe0 : 0xff284b76;
        int fill = !button.active ? 0xff111e30 : primary
                ? (hover ? 0xff2676c9 : 0xff1c5da8) : (hover ? 0xff173957 : 0xff0b2038);
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        if (hover) g.fill(x + 1, y + 1, x + 3, y + h - 1, 0xff8cd5ff);
        g.drawCenteredString(Minecraft.getInstance().font, button.getMessage(), x + w / 2,
                y + (h - 8) / 2, button.active ? 0xffeef6ff : 0xff71829b);
        return true;
    }
}
