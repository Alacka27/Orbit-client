package dev.alacka.orbit.menu;

import dev.alacka.orbit.mixin.ContainerScreenAccessor;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

/** Skins vanilla presentation only; no Slot mutation, packet, recipe or input-coordinate replacement. */
public final class GameUiTheme {
    private static final Set<String> SUPPORTED = Set.of("InventoryScreen", "CreativeModeInventoryScreen", "ContainerScreen",
            "CraftingScreen", "FurnaceScreen", "BlastFurnaceScreen", "SmokerScreen", "EnchantmentScreen", "AnvilScreen",
            "MerchantScreen", "BrewingStandScreen", "SmithingScreen", "CartographyTableScreen", "GrindstoneScreen",
            "LoomScreen", "StonecutterScreen", "BeaconScreen", "HopperScreen", "DispenserScreen", "ShulkerBoxScreen",
            "HorseInventoryScreen", "NautilusInventoryScreen", "CrafterScreen");
    private GameUiTheme() {}
    public static boolean supported(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> && screen.getClass().getPackageName().equals("net.minecraft.client.gui.screens.inventory")
                && SUPPORTED.contains(screen.getClass().getSimpleName());
    }
    public static boolean active() { return OrbitMenu.isEnabled() && supported(Minecraft.getInstance().screen); }
    public static ContainerLayout layout(AbstractContainerScreen<?> screen) {
        var a = (ContainerScreenAccessor) screen;
        return ContainerLayout.of(a.orbit$left(), a.orbit$top(), a.orbit$imageWidth(), a.orbit$imageHeight(), screen.width, screen.height);
    }
    public static void background(GuiGraphics g, AbstractContainerScreen<?> screen) {
        if (!active()) return;
        var a = (ContainerScreenAccessor) screen;
        var b = layout(screen);
        GlassMenu.shadow(g, b.x(), b.y(), b.width(), b.height());
        GlassMenu.panel(g, b.x(), b.y(), b.width(), b.height(), 12, GlassMenu.WINDOW_FILL, GlassMenu.WINDOW_BORDER);
        if (screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)
            GlassMenu.heading(g, "Инвентарь", b.x() + 10, b.y() + 7, 0xfffaf3ff);
        // Draw frames underneath the native stacks, counts, durability, ghost recipes and hover logic.
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive() || slot.x < 0 || slot.y < 0 || slot.x + 16 > a.orbit$imageWidth() || slot.y + 16 > a.orbit$imageHeight()) continue;
            GlassMenu.panel(g, a.orbit$left() + slot.x - 1, a.orbit$top() + slot.y - 1, 18, 18, 3, 0x751a1424, 0x4b9e83b8);
        }
        if (!(screen instanceof CreativeModeInventoryScreen)) {
            GlassMenu.panel(g, b.closeX(), b.closeY(), 11, 11, 3, 0x3a685275, 0x547d638c);
            GlassMenu.text(g, "×", b.closeX() + 3, b.closeY() + 1, 0xfff1e8fa);
            if (b.hintY() >= a.orbit$top() + a.orbit$imageHeight()) {
                var hint = GlassMenu.text("Shift + клик — переместить  ·  Esc — закрыть");
                g.pose().pushMatrix();
                g.pose().translate(b.x() + b.width() / 2f, b.hintY());
                g.pose().scale(.75f, .75f);
                g.drawString(Minecraft.getInstance().font, hint, -Minecraft.getInstance().font.width(hint) / 2, 0, 0xffd9cce8, false);
                g.pose().popMatrix();
            }
        }
        diagram(g, screen, a.orbit$left(), a.orbit$top());
    }
    private static void diagram(GuiGraphics g, AbstractContainerScreen<?> screen, int x, int y) {
        int ax = -1, ay = 0;
        switch (screen.getClass().getSimpleName()) {
            case "InventoryScreen" -> { ax = 136; ay = 32; }
            case "CraftingScreen" -> { ax = 100; ay = 38; }
            case "FurnaceScreen", "SmokerScreen", "BlastFurnaceScreen" -> { ax = 82; ay = 39; }
            case "AnvilScreen" -> { ax = 110; ay = 51; }
            case "SmithingScreen" -> { ax = 121; ay = 52; }
            case "GrindstoneScreen" -> { ax = 105; ay = 38; }
            case "CartographyTableScreen" -> { ax = 121; ay = 43; }
            case "MerchantScreen" -> { ax = 197; ay = 39; }
        }
        if (ax >= 0) {
            int color = UiTheme.tint(0xb8ccafe7, false);
            g.fill(x + ax, y + ay, x + ax + 12, y + ay + 1, color);
            for (int i = 0; i < 4; i++) {
                g.fill(x + ax + 8 + i, y + ay - 3 + i, x + ax + 9 + i, y + ay - 2 + i, color);
                g.fill(x + ax + 8 + i, y + ay + 3 - i, x + ax + 9 + i, y + ay + 4 - i, color);
            }
        }
    }
    public static boolean hideTexture(GuiGraphics g, Identifier texture, int x, int y, int width, int height) {
        if (GlassMenu.active() && texture.getNamespace().equals("minecraft")) {
            String p = texture.getPath();
            if (Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.advancements.AdvancementsScreen) {
                if (p.equals("textures/gui/advancements/window.png")) {
                    // This frame is drawn AFTER the tree: keep the centre transparent.
                    GlassMenu.panel(g, x, y, width, height, 8, 0x00000000, 0x8db18bcf); return true;
                }
                if (width == 16 && height == 16 && (p.startsWith("textures/gui/advancements/backgrounds/") || p.startsWith("textures/block/"))) return true;
            }
            if (p.equals("textures/gui/recipe_book.png") && width == 147 && height == 166) {
                GlassMenu.panel(g, x, y, width, height, 8, GlassMenu.WINDOW_FILL, GlassMenu.WINDOW_BORDER); return true;
            }
        }
        if (!active() || !texture.getNamespace().equals("minecraft") || !texture.getPath().startsWith("textures/gui/container/")) return false;
        var screen = (AbstractContainerScreen<?>) Minecraft.getInstance().screen;
        var a = (ContainerScreenAccessor) screen;
        // The two chest halves and creative tab bodies use the same full-width test. Small live overlays remain native.
        return x == a.orbit$left() && width == a.orbit$imageWidth() && height > 40
                && y >= a.orbit$top() && y < a.orbit$top() + a.orbit$imageHeight();
    }
    public static boolean sprite(GuiGraphics g, Identifier id, int x, int y, int w, int h, int color) {
        if (!id.getNamespace().equals("minecraft")) return false;
        if (HudTheme.sprite(g, id.getPath(), x, y, w, h, color)) return true;
        String p = id.getPath();
        if (GlassMenu.active() && p.startsWith("advancements/")) {
            boolean selected = p.contains("selected") || p.contains("obtained") && !p.contains("unobtained");
            GlassMenu.panel(g, x, y, w, h, 5, selected ? 0xd38950ba : 0xa42e203e, selected ? 0xcdd6a5fa : 0x659b7cba); return true;
        }
        if (!active()) return false;
        // Native icon-bearing buttons (notably trade rows) still render their contents.
        // Replace only the vanilla background sprite, not Button.renderContents.
        if (p.startsWith("widget/button")) {
            boolean selected = p.contains("highlighted"), disabled = p.contains("disabled");
            GlassMenu.panel(g, x, y, w, h, 4, disabled ? 0x55281f33 : selected ? 0xbd7747a5 : 0x8a493357,
                    selected ? 0xcdd0a4f6 : 0x5d9b80b3); return true;
        }
        if (p.contains("slot_highlight_back")) {
            GlassMenu.panel(g, x + 3, y + 3, w - 6, h - 6, 3, 0x506e4299, 0x80be90ed); return true;
        }
        if (p.contains("slot_highlight_front")) {
            GlassMenu.panel(g, x + 3, y + 3, w - 6, h - 6, 3, 0x00322147, 0xc9cf9dff); return true;
        }
        if (p.startsWith("container/") && (p.contains("enchantment_slot") || p.contains("/button")
                || p.contains("/tab_") || p.contains("text_field") || p.contains("recipe_selected")
                || p.contains("recipe_highlighted") || p.contains("pattern_selected") || p.contains("pattern_highlighted"))) {
            boolean selected = p.contains("highlighted") || p.contains("selected") && !p.contains("unselected");
            boolean disabled = p.contains("disabled");
            GlassMenu.panel(g, x, y, w, h, 4, disabled ? 0x55281f33 : selected ? 0xb36c3f9d : 0x73503a65,
                    selected ? 0xccd0a4f6 : 0x4b9b80b3); return true;
        }
        if (p.startsWith("container/") && p.contains("scroller") && w <= 14) {
            GlassMenu.panel(g, x, y, w, h, 3, 0xb48d5cbb, 0x75d0a6ee); return true;
        }
        if (p.equals("recipe_book/recipe_book")) {
            GlassMenu.panel(g, x, y, w, h, 8, GlassMenu.WINDOW_FILL, GlassMenu.WINDOW_BORDER); return true;
        }
        return false;
    }
    public static int labelColor(int color) {
        if (GlassMenu.active() && ((color & 0xffffff) == 0x404040 || (color & 0xffffff) == 0x3f3f3f)) return (color & 0xff000000) | 0xf2e9fb;
        return color;
    }
}
