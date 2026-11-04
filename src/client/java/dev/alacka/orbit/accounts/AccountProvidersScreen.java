package dev.alacka.orbit.accounts;

import dev.alacka.orbit.profiles.ProfilesScreen;

public final class AccountProvidersScreen extends AccountFormScreen {
    public AccountProvidersScreen(ProfilesScreen profiles) {
        super("Добавить аккаунт", "", profiles, 230, "account-providers");
        status = "Онлайн-сессии — до закрытия игры.";
    }
    @Override protected void controls() {
        int x = layout.x() + 12;
        primary = button("Offline — свой ник", x, layout.fieldY(0), layout.inner(), b -> {
            profiles.focusOfflineInput(); minecraft.setScreen(profiles);
        });
        button("Ely.by — вход", x, layout.fieldY(1), layout.inner(), b -> minecraft.setScreen(new ElyLoginScreen(profiles)));
        button("Microsoft — официальный аккаунт", x, layout.fieldY(2), layout.inner(), b -> minecraft.setScreen(new MicrosoftLoginScreen(profiles)));
        button("Назад", x, layout.footerY(), layout.inner(), b -> onClose());
    }
}