package dev.alacka.orbit.menu;

public final class ModuleHelp {
    private ModuleHelp() {}
    public static String text(String module) {
        return switch (module) {
            case "KillAura" -> "Наведение и атака в пределах оружия.";
            case "TriggerBot" -> "Атака цели под прицелом.";
            case "NoFall" -> "Защита от падения. Зависит от сервера.";
            case "Jesus" -> "Ходьба по воде. Shift — нырнуть.";
            case "AutoTotem" -> "Тотем в левую руку.";
            case "AutoArmor" -> "Подбирает лучшую броню.";
            case "AutoConsume" -> "Еда и зелья из хотбара.";
            case "ElytraSwap" -> "Обмен элитр и нагрудника по клавише.";
            case "Trajectories" -> "Траектория и точка попадания.";
            case "TargetHUD" -> "Панель цели: имя, броня и дистанция.";
            case "DamageNumbers" -> "Наблюдаемый урон и лечение.";
            case "HitParticles" -> "Цветные частицы при попаданиях.";
            case "CriticalHitEffect" -> "Дополнительный эффект обычного крита.";
            case "ShieldStatus" -> "Состояние видимых щитов.";
            case "TotemCounter" -> "Свои тотемы и замеченные срабатывания.";
            case "SeeInvisibles" -> "Полупрозрачные невидимые игроки.";
            default -> throw new IllegalArgumentException("Unknown module: " + module);
        };
    }
    public static String limit(String module) {
        return switch (module) {
            case "TargetHUD" -> "Чужие HP — лишь из scoreboard в формате сердец.";
            case "DamageNumbers" -> "Числа — только по известному здоровью.";
            case "HitParticles" -> "Реальные события урона; только визуал.";
            case "CriticalHitEffect" -> "Не изменяет атаку и не создаёт крит.";
            case "ShieldStatus" -> "Чужой откат и прочность не угадываются.";
            case "TotemCounter" -> "У других — срабатывания, не остаток.";
            case "SeeInvisibles" -> "Только известные клиенту сущности, без X-ray.";
            case "Trajectories" -> "Без разброса, фейерверков и Riptide.";
            default -> "";
        };
    }
}