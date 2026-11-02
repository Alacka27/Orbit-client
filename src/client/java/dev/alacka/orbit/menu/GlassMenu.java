package dev.alacka.orbit.menu;

import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.resources.Identifier;
import dev.alacka.orbit.mixin.CycleButtonAccessor;
import dev.alacka.orbit.profiles.ProfilesScreen;

/** Lightweight translucent styling, not a blur/shader effect. No in-world GUI changes. */
public final class GlassMenu {
    private static final Identifier CLOUDS = Identifier.fromNamespaceAndPath("orbit", "clouds.png");
    private static final Identifier SURFACE = Identifier.fromNamespaceAndPath("orbit", "ui/surface");
    private static final Identifier BORDER = Identifier.fromNamespaceAndPath("orbit", "ui/border");
    private static final Identifier SHADOW = Identifier.fromNamespaceAndPath("orbit", "ui/shadow");
    private static final FontDescription UI_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("orbit", "ui"));
    private static final WeakHashMap<AbstractButton, Hover> HOVERS = new WeakHashMap<>();
    private static final class Hover { float value; long last; }
    private GlassMenu() {}

    public static boolean active() {
        Minecraft client = Minecraft.getInstance();
        return OrbitMenu.isEnabled() && client.screen != null && client.level == null;
    }

    public static boolean supports(AbstractButton button) {
        if (button instanceof ImageButton || button instanceof SpriteIconButton || button instanceof PlainTextButton) return false;
        if (button instanceof CycleButton<?> cycle) {
            return ((CycleButtonAccessor) cycle).orbit$displayState() != CycleButton.DisplayState.HIDE;
        }
        return button instanceof Button;
    }

    public static void backdrop(GuiGraphics g, int width, int height) {
        double scale = Math.max(width / 1600.0, height / 900.0);
        int w = (int) Math.ceil(1600 * scale), h = (int) Math.ceil(900 * scale);
        g.blit(RenderPipelines.GUI_TEXTURED, CLOUDS, (width - w) / 2, (height - h) / 2,
                0f, 0f, w, h, 1600, 900, 1600, 900, 0xffffffff);
        for (int row = 0; row < 32; row++) {
            int alpha = row * 120 / 31;
            g.fill(0, height - 32 + row, width, height - 31 + row, alpha << 24 | 0x10091e);
        }
    }

    /** Antialiased nine-slice sprites keep their corner radius at every GUI scale. */
    public static void panel(GuiGraphics g, int x, int y, int width, int height, int radius, int fill, int border) {
        if (width <= 0 || height <= 0) return;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, SURFACE, x, y, width, height, fill);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, BORDER, x, y, width, height, border);
    }
    public static void shadow(GuiGraphics g, int x, int y, int width, int height) {
        if (width > 0 && height > 0) g.blitSprite(RenderPipelines.GUI_TEXTURED, SHADOW, x - 6, y - 4, width + 12, height + 12);
    }
    public static Component text(String label) {
        return Component.literal(label).withStyle(style -> style.withFont(UI_FONT));
    }
    public static void text(GuiGraphics g, String label, int x, int y, int color) {
        g.drawString(Minecraft.getInstance().font, text(label), x, y, color, false);
    }
    public static boolean selectionScreen() {
        var screen = Minecraft.getInstance().screen;
        return active() && (screen instanceof SelectWorldScreen || screen instanceof JoinMultiplayerScreen);
    }
    public static void screenPanel(GuiGraphics g, int width, int height) {
        if (!selectionScreen()) return;
        int w = Math.min(width - 16, 552), x = (width - w) / 2;
        shadow(g, x, 7, w, height - 14);
        panel(g, x, 7, w, height - 14, 12, 0xd51c1528, 0x36d6c9ec);
    }
    public static void card(GuiGraphics g, net.minecraft.client.gui.layouts.LayoutElement entry, boolean selected, boolean hovered) {
        int fill = selected ? 0xb7503277 : hovered ? 0x98504461 : 0x70423850;
        panel(g, entry.getX(), entry.getY(), entry.getWidth(), entry.getHeight() - 3, 8,
                fill, selected ? 0xd0b38aeb : hovered ? 0x70cbb6e6 : 0x28ddd2ee);
    }
    private static int mix(int a, int b, float t) {
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int av = (a >>> shift) & 255, bv = (b >>> shift) & 255;
            out |= Math.round(av + (bv - av) * t) << shift;
        }
        return out;
    }

    public static void button(GuiGraphics g, AbstractButton button, boolean primary) {
        Hover state = HOVERS.computeIfAbsent(button, key -> new Hover());
        long now = System.nanoTime();
        float dt = state.last == 0 ? 1 : Math.min(0.2f, (now - state.last) / 1_000_000_000f);
        state.last = now;
        float target = button.active && button.isHoveredOrFocused() ? 1 : 0;
        state.value += (target - state.value) * (OrbitMenu.isAnimated() ? 1 - (float) Math.exp(-dt * 16) : 1);
        boolean title = Minecraft.getInstance().screen instanceof TitleScreen;
        String raw = button.getMessage().getString();
        boolean accent = primary || (Minecraft.getInstance().screen instanceof ProfilesScreen
                && (raw.equals("Select") || raw.equals("Back")))
                || button.getMessage().equals(Component.translatable("gui.back"))
                || button.getMessage().equals(Component.translatable("gui.done"));
        int fill = !button.active ? 0x58352c43 : accent
                ? mix(0xe57846b7, 0xfa9a65dd, state.value)
                : mix(title ? 0xb72d233b : 0xa52b2336, 0xd94b365f, state.value);
        int border = !button.active ? 0x254f435e : accent
                ? mix(0xb1c299ed, 0xffddc4ff, state.value)
                : mix(0x415e4c72, 0xb6b491dc, state.value);
        shadow(g, button.getX(), button.getY(), button.getWidth(), button.getHeight());
        panel(g, button.getX(), button.getY(), button.getWidth(), button.getHeight(), 8, fill, border);
        var font = Minecraft.getInstance().font;
        String label = raw;
        int available = Math.max(0, button.getWidth() - 18);
        if (font.width(text(label)) > available) {
            while (!label.isEmpty() && font.width(text(label + "…")) > available) label = label.substring(0, label.length() - 1);
            label += "…";
        }
        Component content = text(label);
        g.drawString(font, content, button.getX() + (button.getWidth() - font.width(content)) / 2,
                button.getY() + (button.getHeight() - 9) / 2,
                button.active ? 0xfff8f4ff : 0xff91859f, false);

    }
}
