package dev.alacka.orbit.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.ChatScreen;

/** Native hotbar slots, hands, attack indicator, hearts, XP and keybinds are retained. */
public final class HudTheme {
    private static final FontDescription FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("orbit", "ui"));
    private HudTheme() {}
    private static Component text(String s) {
        var text = Component.literal(s);
        if (Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath("orbit", "font/ui.json")).isPresent()) text.withStyle(st -> st.withFont(FONT));
        return text;
    }
    public static boolean enabled() { return OrbitMenu.isEnabled() && Minecraft.getInstance().player != null; }
    public static boolean sprite(GuiGraphics g, String p, int x, int y, int w, int h, int color) {
        if (!enabled()) return false;
        if (p.equals("hud/hotbar")) {
            GlassMenu.panel(g, x - 4, y - 4, w + 8, h + 4, 7, 0xc3281b38, 0x80ad8cc7);
            for (int i = 0; i < 9; i++) GlassMenu.panel(g, x + 2 + i * 20, y + 2, 18, 18, 3, 0x5c150e20, 0x3b9b7bb4);
            return true;
        }
        if (p.equals("hud/hotbar_selection")) {
            GlassMenu.panel(g, x + 3, y + 3, 18, 18, 3, 0x42763ba2, 0xffd4a3ff); return true;
        }
        if (p.equals("hud/hotbar_offhand_left") || p.equals("hud/hotbar_offhand_right")) {
            GlassMenu.panel(g, x + 1, y + 1, w - 2, h - 1, 5, 0xb92b1d3b, 0x789b7db5); return true;
        }
        if (p.equals("hud/experience_bar_background")) {
            GlassMenu.panel(g, x, y, w, h, 3, 0xb21a1126, 0x4b9b7ac3); return true;
        }
        if (p.startsWith("hud/effect_background")) {
            GlassMenu.panel(g, x, y, w, h, 4, 0xb4291d38, 0x709a78bd); return true;
        }
        return false;
    }
    public static void render(GuiGraphics g) {
        Minecraft m = Minecraft.getInstance();
        if (!enabled() || m.options.hideGui || m.getDebugOverlay().showDebugScreen()
                || m.screen != null && !(m.screen instanceof ChatScreen)) return;
        var font = m.font;
        var connection = m.getConnection();
        var info = connection == null ? null : connection.getPlayerInfo(m.player.getUUID());
        String latency = m.hasSingleplayerServer() ? "Singleplayer" : info == null ? "— ms" : info.getLatency() + " ms";
        Component label = text("O R B I T   |   " + m.getFps() + " FPS   |   " + latency);
        int width = font.width(label) + 14;
        GlassMenu.panel(g, 6, 6, width, 18, 5, 0xbc2b1d3b, 0x609b7abb);
        g.drawString(font, label, 13, 11, 0xfff6efff, false);
        if (!(m.screen instanceof ChatScreen)) {
            Component xyz = text("XYZ  " + m.player.getBlockX() + " / " + m.player.getBlockY() + " / " + m.player.getBlockZ());
            // A compact window has no safe lower-left lane beside the native hotbar/chat.
            int xyzY = g.guiWidth() < 500 ? 30 : g.guiHeight() - 21;
            GlassMenu.panel(g, 6, xyzY, font.width(xyz) + 12, 15, 4, 0xb82b1d3b, 0x467c5e9c);
            g.drawString(font, xyz, 12, xyzY + 4, 0xffeee5f8, false);
        }
    }
}
