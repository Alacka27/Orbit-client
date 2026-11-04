package dev.alacka.orbit.accounts;

import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.OrbitWindow;
import dev.alacka.orbit.menu.WindowDrag;
import dev.alacka.orbit.profiles.ProfilesScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class AccountFormScreen extends Screen implements OrbitWindow {
    protected final ProfilesScreen profiles;
    protected AccountFormLayout layout;
    protected String status = "";
    protected String subtitle;
    protected Button primary;
    protected AuthTask task;
    protected boolean busy;
    private final int preferredHeight;
    private final WindowDrag drag;

    protected AccountFormScreen(String title, String subtitle, ProfilesScreen profiles, int height, String dragKey) {
        super(Component.literal(title)); this.subtitle = subtitle; this.profiles = profiles;
        this.preferredHeight = height; this.drag = new WindowDrag(dragKey);
    }

    @Override protected final void init() {
        clearWidgets();
        layout = AccountFormLayout.of(width, height, preferredHeight, drag.dx(), drag.dy());
        controls();
    }
    protected abstract void controls();
    protected Button button(String label, int x, int y, int w, Button.OnPress press) {
        return addRenderableWidget(Button.builder(Component.literal(label), press).bounds(x, y, w, 22).build());
    }
    protected EditBox input(String label, int row, int max) {
        EditBox input = addRenderableWidget(new EditBox(font, layout.x() + 12, layout.fieldY(row), layout.inner(), 22, Component.literal(label)));
        input.setMaxLength(max); input.setHint(Component.literal(label)); return input;
    }
    protected void backAndRegister(String registration) {
        int half = (layout.inner() - 6) / 2;
        button("Создать аккаунт", layout.x() + 12, layout.footerY(), half,
                b -> net.minecraft.util.Util.getPlatform().openUri(registration));
        button("Назад", layout.x() + 18 + half, layout.footerY(), layout.inner() - half - 6, b -> onClose());
    }
    protected void setBusy(boolean value) {
        busy = value;
        if (primary != null) primary.active = !value;
    }
    protected void failed(AuthFailure error) { setBusy(false); status = error.getMessage(); }
    protected void authenticated(AccountSessions.Prepared prepared) {
        try {
            profiles.acceptOnline(prepared);
            minecraft.setScreen(profiles);
        } catch (Exception error) {
            setBusy(false);
            status = "Не удалось применить сессию. Выйди в главное меню и попробуй ещё раз.";
        }
    }
    protected void paragraph(GuiGraphics graphics, String text, int y, int bottom, int color) {
        for (var line : font.split(Component.literal(text), layout.inner())) {
            if (y + 9 > bottom) break;
            graphics.drawString(font, line, layout.x() + 12, y, color, false);
            y += 11;
        }
    }
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        GlassMenu.shadow(graphics, layout.x(), layout.y(), layout.width(), layout.height());
        GlassMenu.panel(graphics, layout.x(), layout.y(), layout.width(), layout.height(), 12, GlassMenu.WINDOW_FILL, GlassMenu.WINDOW_BORDER);
        drag.render(this, graphics, mouseX, mouseY, layout.x(), layout.y(), layout.width());
        GlassMenu.heading(graphics, getTitle().getString(), layout.x() + 12, layout.y() + 12, 0xfffcf4ff);
        GlassMenu.text(graphics, font.plainSubstrByWidth(subtitle, layout.inner()), layout.x() + 12, layout.y() + 28, 0xffd9c7e8);
        renderContent(graphics, mouseX, mouseY, delta);
        paragraph(graphics, status, layout.statusY(), layout.footerY() - 5, 0xffdfd0e9);
        super.render(graphics, mouseX, mouseY, delta);
    }
    @Override public boolean accent(AbstractButton button) { return button == primary; }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return drag.press(this, event, layout.x(), layout.y(), layout.width()) || super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!drag.dragging()) return super.mouseDragged(event, dx, dy);
        AccountFormLayout next = AccountFormLayout.of(width, height, preferredHeight, drag.targetX(event.x()), drag.targetY(event.y()));
        AccountFormLayout center = AccountFormLayout.of(width, height, preferredHeight, 0, 0);
        WindowDrag.shift(this, next.x() - layout.x(), next.y() - layout.y());
        drag.applied(next.x() - center.x(), next.y() - center.y());
        layout = next;
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (!drag.dragging()) return super.mouseReleased(event);
        mouseDragged(event, 0, 0); drag.release(); return true;
    }
    @Override public void removed() { if (task != null) task.close(); }
    @Override public void onClose() { if (task != null) task.close(); minecraft.setScreen(profiles); }
}