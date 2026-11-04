package dev.alacka.orbit.accounts;

import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.profiles.ProfilesScreen;
import java.time.Instant;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

public final class MicrosoftLoginScreen extends AccountFormScreen {
    private String clientId = "";
    private EditBox appId;
    private AuthProtocol.DeviceCode device;
    private boolean preferencesValid = true;

    public MicrosoftLoginScreen(ProfilesScreen profiles) {
        super("Microsoft", "Вход через браузер", profiles, 270, "account-microsoft");
        try { clientId = AccountPreferences.read(AccountSessions.preferencesPath()); }
        catch (java.io.IOException error) { preferencesValid = false; }
        status = preferencesValid
                ? "Нужен Client ID, одобренный для Minecraft API."
                : "accounts.properties повреждён. Вход недоступен.";
    }
    @Override protected void controls() {
        int x = layout.x() + 12;
        if (device == null) {
            appId = input("Client ID приложения Microsoft", 0, 36);
            appId.setValue(clientId); appId.setEditable(!busy && preferencesValid);
            appId.setResponder(text -> {
                clientId = text.trim();
                if (primary != null) primary.active = !busy && preferencesValid && AccountPreferences.validClientId(clientId);
            });
            primary = button(busy ? "Запрос кода..." : "Получить код входа", x, layout.fieldY(1), layout.inner(), b -> startLogin());
            primary.active = !busy && preferencesValid && AccountPreferences.validClientId(clientId) && AccountSessions.canSwitch(minecraft);
            button("Одобрение Minecraft API", x, layout.fieldY(2), layout.inner(),
                    b -> net.minecraft.util.Util.getPlatform().openUri("https://aka.ms/mce-reviewappid"));
            backAndRegister("https://signup.live.com/");
        } else {
            appId = null;
            int half = (layout.inner() - 6) / 2;
            primary = button("Открыть Microsoft", x, layout.fieldY(2), half,
                    b -> net.minecraft.util.Util.getPlatform().openUri(device.verificationUri()));
            button("Копировать код", x + half + 6, layout.fieldY(2), layout.inner() - half - 6,
                    b -> minecraft.keyboardHandler.setClipboard(device.userCode()));
            button("Заново", x, layout.footerY(), half, b -> {
                if (task != null) task.close();
                busy = false; device = null; status = "Запроси новый код для входа."; rebuild();
            });
            button("Отмена", x + half + 6, layout.footerY(), layout.inner() - half - 6, b -> onClose());
        }
    }

    private void rebuild() { clearWidgets(); controls(); }

    private void startLogin() {
        if (busy || !AccountSessions.canSwitch(minecraft)) return;
        try { AccountPreferences.write(AccountSessions.preferencesPath(), clientId); }
        catch (java.io.IOException error) { status = "Не удалось сохранить Client ID. Файл настроек не изменён."; return; }
        String id = clientId;
        setBusy(true);
        status = "Запрос одноразового кода у Microsoft...";
        task = new AuthTask();
        AuthProtocol protocol = new AuthProtocol(new AuthHttp(minecraft.getProxy()));
        task.start(minecraft, this, job -> {
            AuthProtocol.DeviceCode issued = protocol.microsoftCode(id);
            job.check();
            minecraft.execute(() -> {
                if (minecraft.screen != this || job.cancelled()) return;
                device = issued; status = "Подтверди код в браузере."; rebuild();
            });
            AuthSession session = protocol.microsoftPoll(id, issued, job::cancelled);
            job.check();
            AccountSessions.Prepared prepared = AccountSessions.prepareOnline(minecraft, session);
            job.check();
            return prepared;
        }, this::authenticated, error -> {
            setBusy(false); status = error.getMessage();
            if (device == null) rebuild();
        });
    }

    @Override protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (device == null) return;
        GlassMenu.heading(graphics, device.userCode(), layout.x() + 12, layout.fieldY(0) + 4, 0xfffcf4ff);
        long seconds = Math.max(0, java.time.Duration.between(Instant.now(), device.expiresAt()).getSeconds());
        String line = device.verificationUri().getHost() + " · " + (seconds / 60) + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds % 60);
        GlassMenu.text(graphics, line, layout.x() + 12, layout.fieldY(1) + 2, 0xffd9c7e8);
    }
}