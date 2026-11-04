package dev.alacka.orbit.accounts;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

/** Native editing/IME, but masked rendering and no password in narration/copy/cut. */
public final class SecretEditBox extends EditBox {
    private final Component label;
    public SecretEditBox(Font font, int x, int y, int width, int height, Component label) {
        super(font, x, y, width, height, label);
        this.label = label;
        setMaxLength(1024);
        setHint(label);
        addFormatter((text, offset) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
    }
    @Override protected net.minecraft.network.chat.MutableComponent createNarrationMessage() {
        return label.copy(); // EditBox's default narration includes its unmasked value.
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if ((event.modifiers() & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0
                && (event.key() == GLFW.GLFW_KEY_C || event.key() == GLFW.GLFW_KEY_X)) return true;
        return super.keyPressed(event);
    }
    public String takeAndClear() { String result = getValue(); setValue(""); return result; }
    public void erase() { setValue(""); }
}