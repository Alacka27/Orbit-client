package dev.alacka.orbit.menu;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** Edge-triggered hotkey; never steals input from chat, text fields or Controls. */
public final class ClickMenuController {
    private static KeyMapping key;
    private ClickMenuController() {}
    public static void initialize() {
        key = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.orbit.menu",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));
    }
    public static String keyLabel() { return key == null ? "RShift" : key.getTranslatedKeyMessage().getString(); }
    public static boolean handle(Minecraft client, int action, KeyEvent event) {
        if (key == null || action != GLFW.GLFW_PRESS || !key.matches(event) || !client.isWindowActive()) return false;
        if (client.screen instanceof OrbitClickScreen menu) { menu.onClose(); return true; }
        if (client.screen == null || client.screen instanceof TitleScreen || client.screen instanceof PauseScreen) {
            client.setScreen(new OrbitClickScreen(client.screen));
            return true;
        }
        return false;
    }
}
