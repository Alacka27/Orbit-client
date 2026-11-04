package dev.alacka.orbit.accounts;

/** Only fixed, non-sensitive messages may cross from authentication to the UI/log. */
public final class AuthFailure extends Exception {
    public enum Reason {
        NETWORK, RESPONSE, CREDENTIALS, TWO_FACTOR, RATE_LIMIT, CANCELLED, EXPIRED,
        CLIENT_ID, APP_APPROVAL, NO_GAME, NO_PROFILE, XBOX_PROFILE, FAMILY, XBOX_DENIED
    }

    private final Reason reason;

    public AuthFailure(Reason reason) {
        super(message(reason));
        this.reason = reason;
    }

    public Reason reason() { return reason; }

    private static String message(Reason reason) {
        return switch (reason) {
            case NETWORK -> "Нет связи с сервисом входа. Проверь подключение и попробуй ещё раз.";
            case RESPONSE -> "Сервис вернул неожиданный ответ. Активный аккаунт не изменён.";
            case CREDENTIALS -> "Неверные данные входа или код 2FA. Проверь их и повтори вход.";
            case TWO_FACTOR -> "Ely.by требует код 2FA. Введи пароль и текущий код из приложения.";
            case RATE_LIMIT -> "Слишком много запросов. Подожди перед следующей попыткой.";
            case CANCELLED -> "Вход отменён. Активный аккаунт не изменён.";
            case EXPIRED -> "Код или сессия истекли. Начни вход заново.";
            case CLIENT_ID -> "Нужен Client ID собственного приложения Microsoft с доступом к Minecraft API.";
            case APP_APPROVAL -> "Microsoft/Minecraft отклонил приложение. Проверь Client ID и одобрение Minecraft API.";
            case NO_GAME -> "На этом аккаунте не найдено право на Minecraft Java Edition.";
            case NO_PROFILE -> "Профиль Minecraft Java не найден. Сначала настрой его в официальном лаунчере.";
            case XBOX_PROFILE -> "Сначала создай профиль Xbox на официальном сайте Microsoft.";
            case FAMILY -> "Для входа нужно разрешение семьи Microsoft или проверка возраста.";
            case XBOX_DENIED -> "Xbox Live не разрешил вход для этого аккаунта.";
        };
    }
}