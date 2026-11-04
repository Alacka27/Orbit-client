package dev.alacka.orbit.profiles;

import dev.alacka.orbit.accounts.AccountProvidersScreen;
import dev.alacka.orbit.accounts.AccountSessions;
import dev.alacka.orbit.accounts.AuthHttp;
import dev.alacka.orbit.accounts.AuthProtocol;
import dev.alacka.orbit.accounts.AuthSession;
import dev.alacka.orbit.accounts.AuthTask;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.OrbitWindow;
import dev.alacka.orbit.menu.ProfileListLayout;
import dev.alacka.orbit.menu.WindowDrag;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ProfilesScreen extends Screen implements OrbitWindow {
    private record Entry(String key, String name, String kind, UUID id, boolean offline, AuthSession online) {}
    private final Screen parent;
    private ProfileStore.Data data;
    private List<Entry> entries = List.of();
    private int index, offset, rows;
    private String notice = "", editText = "";
    private EditBox input;
    private ProfileListLayout layout;
    private final WindowDrag drag = new WindowDrag("profiles");
    private Button choose;
    private boolean focusOffline, selectionInitialized, busy;
    private AuthTask task;

    public ProfilesScreen(Screen parent) { super(Component.literal("Profiles")); this.parent = parent; }

    @Override protected void init() {
        try { data = ProfileStore.read(ProfileLaunch.path()); }
        catch (Exception error) { data = null; notice = "Ошибка чтения; файл профилей сохранён без изменений."; }
        buildControls();
        if (focusOffline && input != null) { setInitialFocus(input); focusOffline = false; }
    }

    public void focusOfflineInput() { focusOffline = true; }

    private void refreshEntries() {
        List<Entry> next = new ArrayList<>();
        var launcher = AccountSessions.launcherUser();
        if (launcher == null) launcher = minecraft.getUser();
        next.add(new Entry(ProfileStore.LAUNCHER, launcher.getName() + " · Launcher", "Аккаунт лаунчера", launcher.getProfileId(), false, null));
        if (data != null) for (String name : data.names()) {
            UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            next.add(new Entry(name, name, "Offline-профиль", id, true, null));
        }
        for (AuthSession session : AccountSessions.online()) next.add(new Entry(session.key(), session.name(),
                session.provider() == AuthSession.Provider.ELY ? "Ely.by · до закрытия игры" : "Microsoft · до закрытия игры",
                session.id(), false, session));
        entries = List.copyOf(next);
        if (!selectionInitialized) {
            for (int i = 0; i < entries.size(); i++) if (entries.get(i).key().equals(AccountSessions.active())) index = i;
            selectionInitialized = true;
        }
    }

    private boolean persist(ProfileStore.Data next, String message) {
        try { ProfileStore.write(ProfileLaunch.path(), next); data = next; notice = message; return true; }
        catch (IOException error) { notice = "Ошибка сохранения; существующий файл не изменён."; return false; }
    }

    private void buildControls() {
        if (input != null) editText = input.getValue();
        input = null;
        clearWidgets();
        layout = ProfileListLayout.of(width, height, drag.dx(), drag.dy());
        refreshEntries();
        index = Math.max(0, Math.min(index, entries.size() - 1));
        rows = layout.rows();
        offset = Math.max(0, Math.min(offset, Math.max(0, entries.size() - rows)));
        if (index < offset) offset = index;
        if (index >= offset + rows) offset = index - rows + 1;
        int x = layout.x() + 12, inner = layout.width() - 24;
        for (int i = offset; i < Math.min(entries.size(), offset + rows); i++) {
            int selected = i;
            Entry entry = entries.get(i);
            String status = entry.key().equals(AccountSessions.active()) ? "Сейчас"
                    : data != null && data.selected().equals(entry.key()) ? "Автовход" : "";
            Button row = addRenderableWidget(new ProfileRowButton(x, layout.y() + layout.rowY() + (i - offset) * layout.step(),
                    inner, layout.rowHeight(), entry.name(), entry.kind(), entry.id(), entry.key().equals(AccountSessions.active()),
                    index == i, status, b -> { index = selected; buildControls(); }));
            row.active = !busy;
        }
        Button login = addRenderableWidget(Button.builder(Component.literal("+ Аккаунт"), b -> minecraft.setScreen(new AccountProvidersScreen(this)))
                .bounds(x + 75, layout.y() + 7, Math.min(116, Math.max(82, inner - 145)), 20).build());
        login.active = !busy && AccountSessions.canSwitch(minecraft);
        if (entries.size() > rows) {
            Button previous = addRenderableWidget(Button.builder(Component.literal("‹"), b -> {
                index = Math.max(0, index - rows); offset = index; buildControls();
            }).bounds(layout.x() + layout.width() - 62, layout.y() + 7, 22, 20).build());
            Button next = addRenderableWidget(Button.builder(Component.literal("›"), b -> {
                index = Math.min(entries.size() - 1, index + rows); offset = Math.max(0, index - rows + 1); buildControls();
            }).bounds(layout.x() + layout.width() - 36, layout.y() + 7, 22, 20).build());
            previous.active = !busy && index > 0; next.active = !busy && index < entries.size() - 1;
        }
        Entry entry = entries.get(index);
        if (data != null) {
            input = addRenderableWidget(new EditBox(font, x, layout.editY(), inner - 142, 22, Component.literal("Ник offline-профиля")));
            input.setMaxLength(16); input.setHint(Component.literal("Ник offline-профиля")); input.setValue(editText); input.setEditable(!busy);
            Button add = addRenderableWidget(Button.builder(Component.literal("Добавить"), b -> {
                try {
                    if (persist(ProfileStore.add(data, input.getValue().trim()), "Ник добавлен. Нажми «Применить» — без перезапуска.")) {
                        index = data.names().size(); editText = ""; input.setValue("");
                    }
                } catch (IllegalArgumentException error) { notice = error.getMessage(); }
                buildControls();
            }).bounds(x + inner - 136, layout.editY(), 64, 22).build());
            add.active = !busy;
            Button rename = addRenderableWidget(Button.builder(Component.literal("Изменить"), b -> {
                try { persist(ProfileStore.rename(data, entry.key(), input.getValue().trim()), "Имя записи сохранено. Нажми «Применить» для смены ника."); }
                catch (IllegalArgumentException error) { notice = error.getMessage(); }
                buildControls();
            }).bounds(x + inner - 68, layout.editY(), 68, 22).build());
            rename.active = !busy && entry.offline();
        }
        int third = (inner - 12) / 3;
        choose = addRenderableWidget(Button.builder(Component.literal(busy ? "Вход..." : "Применить"), b -> apply(entry))
                .bounds(x, layout.footerY(), third, 22).build());
        choose.active = !busy && AccountSessions.canSwitch(minecraft) && !entry.key().equals(AccountSessions.active());
        Button delete = addRenderableWidget(Button.builder(Component.literal("Удалить"), b -> {
            if (entry.online() != null) {
                try {
                    AuthSession forgotten = AccountSessions.forget(entry.key());
                    AuthTask.invalidateLater(minecraft, forgotten);
                    notice = "Онлайн-сессия удалена из памяти.";
                    index = Math.max(0, index - 1);
                } catch (IllegalStateException error) { notice = "Сначала переключись на другой аккаунт."; }
            } else if (entry.offline() && data != null) {
                if (persist(ProfileStore.delete(data, entry.key()), entry.key().equals(AccountSessions.active())
                        ? "Запись удалена; текущий ник не изменён." : "Профиль удалён.")) index = Math.max(0, index - 1);
            }
            buildControls();
        }).bounds(x + third + 6, layout.footerY(), third, 22).build());
        delete.active = !busy && (entry.offline() || (entry.online() != null && !entry.key().equals(AccountSessions.active())));
        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose())
                .bounds(x + 2 * (third + 6), layout.footerY(), inner - 2 * (third + 6), 22).build());
    }

    private void apply(Entry entry) {
        if (busy || !AccountSessions.canSwitch(minecraft)) { notice = "Сначала выйди из мира/сервера в главное меню."; return; }
        if (entry.online() != null) {
            busy = true; notice = "Обновление онлайн-сессии..."; buildControls();
            task = new AuthTask();
            task.start(minecraft, this, job -> {
                AuthSession refreshed = new AuthProtocol(new AuthHttp(minecraft.getProxy())).refresh(entry.online(), job::cancelled);
                job.check();
                AccountSessions.Prepared prepared = AccountSessions.prepareOnline(minecraft, refreshed);
                job.check(); return prepared;
            }, prepared -> {
                try { acceptOnline(prepared); }
                catch (Exception error) { busy = false; notice = "Не удалось применить сессию. Войди заново."; buildControls(); }
            }, error -> { busy = false; notice = error.getMessage(); buildControls(); });
            return;
        }
        try {
            if (entry.offline()) AccountSessions.applyOffline(minecraft, entry.key());
            else AccountSessions.applyLauncher(minecraft);
            notice = "Активен: " + minecraft.getUser().getName();
            if (data != null && !persist(ProfileStore.select(data, entry.key()), notice)) notice = "Сессия применена, но автозапуск профиля не сохранён.";
        } catch (Exception error) { notice = "Не удалось сменить аккаунт. Дождись главного меню и попробуй снова."; }
        buildControls();
    }

    public void acceptOnline(AccountSessions.Prepared prepared) throws IOException {
        AccountSessions.applyOnline(minecraft, prepared);
        busy = false;
        notice = "Активен: " + minecraft.getUser().getName();
        if (data != null && !persist(ProfileStore.select(data, ProfileStore.LAUNCHER), notice))
            notice = "Сессия применена. Не удалось сохранить выбор для следующего запуска.";
        refreshEntries();
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).key().equals(AccountSessions.active())) index = i;
        buildControls();
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (!busy && x >= layout.x() && x < layout.x() + layout.width() && y >= layout.y() + layout.rowY() && y < layout.editY() - 5) {
            index = Math.max(0, Math.min(entries.size() - 1, index + (vertical < 0 ? 1 : vertical > 0 ? -1 : 0)));
            buildControls(); return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        GlassMenu.shadow(graphics, layout.x(), layout.y(), layout.width(), layout.height());
        GlassMenu.panel(graphics, layout.x(), layout.y(), layout.width(), layout.height(), 12, GlassMenu.WINDOW_FILL, GlassMenu.WINDOW_BORDER);
        drag.render(this, graphics, mouseX, mouseY, layout.x(), layout.y(), layout.width());
        GlassMenu.heading(graphics, "Profiles", layout.x() + 12, layout.y() + 12, 0xfffcf4ff);
        String line = notice;
        int y = layout.noticeY();
        for (var text : font.split(Component.literal(line), layout.width() - 24)) {
            if (y + 9 > layout.footerY() - 4) break;
            graphics.drawString(font, text, layout.x() + 12, y, 0xffdfd0e9, false); y += 11;
        }
        super.render(graphics, mouseX, mouseY, delta);
    }
    @Override public boolean accent(AbstractButton button) { return button == choose; }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return drag.press(this, event, layout.x(), layout.y(), layout.width()) || super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!drag.dragging()) return super.mouseDragged(event, dx, dy);
        ProfileListLayout next = ProfileListLayout.of(width, height, drag.targetX(event.x()), drag.targetY(event.y()));
        ProfileListLayout center = ProfileListLayout.of(width, height);
        WindowDrag.shift(this, next.x() - layout.x(), next.y() - layout.y());
        drag.applied(next.x() - center.x(), next.y() - center.y()); layout = next; return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (!drag.dragging()) return super.mouseReleased(event);
        mouseDragged(event, 0, 0); drag.release(); return true;
    }
    @Override public void removed() { if (task != null) task.close(); busy = false; }
    @Override public void onClose() {
        if (task != null) task.close();
        // Recreate the title so its account-dependent multiplayer/Realms widgets cannot be stale.
        minecraft.setScreen(parent instanceof TitleScreen || parent == null ? new TitleScreen() : parent);
    }
}
