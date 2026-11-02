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
import net.minecraft.resources.Identifier;
import dev.alacka.orbit.mixin.CycleButtonAccessor;

/** Lightweight translucent styling, not a blur/shader effect. No in-world GUI changes. */
public final class GlassMenu {
    private static final Identifier CLOUDS = Identifier.fromNamespaceAndPath("orbit", "clouds.png");
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

    /** Scanline-rounded fill and 1px border, without layering fill under the border. */
    public static void panel(GuiGraphics g, int x, int y, int width, int height, int radius, int fill, int border) {
        if (width <= 0 || height <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        for (int row = 0; row < height; row++) {
            int inset = inset(row, height, r);
            int left = x + inset, right = x + width - inset;
            if (row == 0 || row == height - 1) {
                g.fill(left, y + row, right, y + row + 1, border);
            } else {
                int inner = inset(row - 1, height - 2, Math.max(0, r - 1)) + 1;
                int il = Math.min(right, x + inner), ir = Math.max(il, x + width - inner);
                g.fill(left, y + row, il, y + row + 1, border);
                g.fill(il, y + row, ir, y + row + 1, fill);
                g.fill(ir, y + row, right, y + row + 1, border);
            }
        }
    }
    private static int inset(int y, int height, int r) {
        if (r == 0) return 0;
        double distance = y < r ? r - y - 0.5 : y >= height - r ? y - (height - r) + 0.5 : 0;
        return distance == 0 ? 0 : Math.max(0, (int) Math.ceil(r - Math.sqrt(Math.max(0, r * r - distance * distance))));
    }

    public static void button(GuiGraphics g, AbstractButton button, boolean primary) {
        Hover state = HOVERS.computeIfAbsent(button, key -> new Hover());
        long now = System.nanoTime();
        float dt = state.last == 0 ? 1 : Math.min(0.2f, (now - state.last) / 1_000_000_000f);
        state.last = now;
        float target = button.active && button.isHoveredOrFocused() ? 1 : 0;
        state.value += (target - state.value) * (OrbitMenu.isAnimated() ? 1 - (float) Math.exp(-dt * 16) : 1);
        boolean title = Minecraft.getInstance().screen instanceof TitleScreen;
        int fillAlpha = !button.active ? 25 : title ? (primary ? 24 : 14) + (int) (state.value * 24) : 112 + (int) (state.value * 18);
        int borderAlpha = !button.active ? 24 : 72 + (int) (state.value * 85);
        panel(g, button.getX(), button.getY(), button.getWidth(), button.getHeight(), 7,
                fillAlpha << 24 | (title ? 0xffffff : 0x190d36), borderAlpha << 24 | 0xffffff);
        var font = Minecraft.getInstance().font;
        String label = button.getMessage().getString();
        if (font.width(label) > button.getWidth() - 14) {
            while (label.length() > 1 && font.width(label + "...") > button.getWidth() - 14) label = label.substring(0, label.length() - 1);
            label += "...";
        }
        g.drawCenteredString(font, label, button.getX() + button.getWidth() / 2,
                button.getY() + (button.getHeight() - 8) / 2, button.active ? 0xfffaf8ff : 0xffb2a5c8);
    }
}
