package dev.alacka.orbit.accounts;

import dev.alacka.orbit.profiles.ProfilesScreen;
import java.util.concurrent.CancellationException;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class ElyLoginScreen extends AccountFormScreen {
    private String username = "";
    private EditBox login, totp;
    private SecretEditBox password;

    public ElyLoginScreen(ProfilesScreen profiles) {
        super("Ely.by", "Пароль не сохраняется", profiles, 270, "account-ely");
        status = "Для серверов с поддержкой Ely.by.";
    }
    @Override protected void controls() {
        if (password != null) password.erase();
        login = input("E-mail или ник Ely.by", 0, 254);
        login.setValue(username); login.setResponder(text -> username = text);
        password = addRenderableWidget(new SecretEditBox(font, layout.x() + 12, layout.fieldY(1), layout.inner(), 22, Component.literal("Пароль Ely.by")));
        totp = input("Код 2FA — если включён", 2, 6);
        totp.setFilter(text -> text.matches("\\d{0,6}"));
        int primaryY = layout.height() >= 220 ? layout.fieldY(3) : layout.footerY();
        if (layout.height() >= 220) {
            primary = button(busy ? "Вход..." : "Войти и применить", layout.x() + 12, primaryY, layout.inner(), b -> login());
            backAndRegister("https://account.ely.by/register");
        } else {
            int third = (layout.inner() - 12) / 3;
            primary = button(busy ? "Вход..." : "Войти", layout.x() + 12, primaryY, third, b -> login());
            button("Создать", layout.x() + 18 + third, primaryY, third,
                    b -> net.minecraft.util.Util.getPlatform().openUri("https://account.ely.by/register"));
            button("Назад", layout.x() + 24 + 2 * third, primaryY, layout.inner() - 2 * third - 12, b -> onClose());
        }
        primary.active = !busy && AccountSessions.canSwitch(minecraft);
        login.setEditable(!busy); password.setEditable(!busy); totp.setEditable(!busy);
        setInitialFocus(login);
    }

    @Override protected void setBusy(boolean value) {
        super.setBusy(value);
        if (login != null) login.setEditable(!value);
        if (password != null) password.setEditable(!value);
        if (totp != null) totp.setEditable(!value);
    }

    private void login() {
        if (busy || !AccountSessions.canSwitch(minecraft)) return;
        String email = login.getValue().trim();
        String secret = password.takeAndClear();
        String code = totp.getValue(); totp.setValue("");
        setBusy(true);
        status = "Проверка входа Ely.by...";
        task = new AuthTask();
        AuthProtocol protocol = new AuthProtocol(new AuthHttp(minecraft.getProxy()));
        task.start(minecraft, this, job -> {
            AuthSession session = protocol.elyLogin(email, secret, code);
            try {
                job.check();
                AccountSessions.Prepared prepared = AccountSessions.prepareOnline(minecraft, session);
                job.check();
                return prepared;
            } catch (CancellationException | AuthFailure error) {
                AuthTask.invalidateLater(minecraft, session);
                throw error;
            }
        }, this::authenticated, this::failed);
    }

    @Override public void removed() {
        if (password != null) password.erase();
        if (totp != null) totp.setValue("");
        super.removed();
    }
}