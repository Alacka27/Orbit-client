package dev.alacka.orbit.profiles;

import com.mojang.authlib.GameProfile;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.PlayerIdentity;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;

public final class ProfileRowButton extends Button {
    private final String name, kind, status;
    private final UUID id;
    private final boolean current, selected;
    public ProfileRowButton(int x, int y, int width, int height, String name, String kind, UUID id,
                            boolean current, boolean selected, String status, OnPress action) {
        super(x, y, width, height, Component.literal(name + ". " + kind + ". " + status), action, DEFAULT_NARRATION);
        this.name = name; this.kind = kind; this.id = id; this.current = current; this.selected = selected; this.status = status;
    }
    @Override protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        GlassMenu.panel(graphics, getX(), getY(), getWidth(), getHeight(), 6,
                selected ? 0x9b634281 : isHoveredOrFocused() ? 0x88513c67 : 0x6849365e, selected ? 0xa9bd90e2 : 0x40987bb0);
        var skin = current ? PlayerIdentity.skin() : DefaultPlayerSkin.get(new GameProfile(id, name));
        PlayerFaceRenderer.draw(graphics, skin, getX() + 8, getY() + (getHeight() - 24) / 2, 24);
        var font = Minecraft.getInstance().font;
        int textY = getY() + (getHeight() < 35 ? 5 : 8);
        GlassMenu.heading(graphics, font.plainSubstrByWidth(name, Math.max(40, getWidth() - 117)), getX() + 42, textY, 0xfffcf4ff);
        GlassMenu.text(graphics, font.plainSubstrByWidth(kind, Math.max(40, getWidth() - 117)), getX() + 42, textY + 12, 0xffd9c7e8);
        GlassMenu.text(graphics, status, getX() + getWidth() - font.width(status) - 9, getY() + (getHeight() - 9) / 2, 0xffe9dcf4);
    }
}
