package dev.alacka.orbit.menu;

import dev.alacka.orbit.input.ModuleBinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** Edge-triggered hotkey; never steals input from chat, text fields or Controls. */
public final class ClickMenuController {
    private ClickMenuController() {}
    public static void initialize() {}
    public static String keyLabel() { return ModuleBinds.label("Menu"); }
    public static boolean matches(KeyEvent event) { return event.key() == ModuleBinds.code("Menu"); }
    public static boolean handle(Minecraft client, int action, KeyEvent event) {
        if (action != GLFW.GLFW_PRESS || !matches(event) || !client.isWindowActive()) return false;
        if (client.screen instanceof OrbitClickScreen menu) { menu.onClose(); return true; }
        if (client.screen == null || client.screen instanceof TitleScreen || client.screen instanceof PauseScreen) {
            client.setScreen(new OrbitClickScreen(client.screen));
            return true;
        }
        return false;
    }
}
