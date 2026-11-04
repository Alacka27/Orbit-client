# 48 референсов → рабочие экраны 0.4.6

Это карта оформления, **не утверждение, что каждый экран отдельно протестирован в игре или повторяет макет пиксель-в-пиксель**. Реальные значения, доступные действия и vanilla-геометрия сохраняются. Облака/панели/виджеты/типографика общие; специализированные хуки нужны для контейнеров, HUD, чата, смерти и достижений.

| № | Референс | Оформление | Что остаётся настоящим |
|---|---|---|---|
| 01 | Главное меню | `OrbitMenu / TitleScreenMixin` | native callbacks |
| 02 | Выбор мира | `GlassMenu / SelectionWidthMixin` | native world list |
| 03 | Создание мира | `GlassMenu / tabs and widgets` | native creation form |
| 04 | Генерация мира | `GlassMenu / tabs and widgets` | native generation controls |
| 05 | Настройка мира | `GlassMenu` | native world actions |
| 06 | Мультиплеер | `GlassMenu / SelectionWidthMixin` | native server list and status |
| 07 | Добавление сервера | `GlassMenu / MenuEditBoxMixin` | native server form |
| 08 | Прямое подключение | `GlassMenu / MenuEditBoxMixin` | native connect form |
| 09 | Настройки | `GlassMenu / menu widgets` | native settings |
| 10 | Видеонастройки | `GlassMenu / slider/cycle widgets` | native video settings |
| 11 | Музыка и звуки | `GlassMenu / slider widgets` | native sound settings |
| 12 | Управление | `GlassMenu / menu widgets` | native controls |
| 13 | Назначение клавиш | `GlassMenu / menu widgets` | native key bindings |
| 14 | Язык | `GlassMenu / menu lists` | native language list |
| 15 | Специальные возможности | `GlassMenu / menu widgets` | native accessibility |
| 16 | Внешний вид | `GlassMenu / menu widgets` | native skin settings and preview |
| 17 | Наборы ресурсов | `GlassMenu / menu lists` | native resource packs |
| 18 | RShift · General | `OrbitClickScreen / ModuleCard` | real FreeLook |
| 19 | RShift · Pvp | `OrbitClickScreen / ModuleCard` | real NoFall / Jesus |
| 20 | RShift · Visual | `OrbitClickScreen / ModuleCard` | real Trajectories |
| 21 | RShift · Fun | `OrbitClickScreen` | honest empty state |
| 22 | RShift · Интерфейс | `ThemeSettingsScreen` | live preview / safe Apply-Cancel |
| 23 | Profiles | `ProfilesScreen / ProfileRowButton` | existing restart-based identity model |
| 24 | Пауза | `PauseSkinMixin` | all existing actions retained |
| 25 | Хотбар и HUD | `HudSkinMixin / HudTheme` | native hotbar and status logic |
| 26 | Инвентарь | `ContainerSkinMixin / GameUiTheme` | native inventory slots |
| 27 | Творческий инвентарь | `ContainerSkinMixin / GameUiTheme` | native creative tabs/search/scroll |
| 28 | Большой сундук | `ContainerSkinMixin / GameUiTheme` | native chest slots; both texture halves |
| 29 | Верстак | `ContainerSkinMixin / GameUiTheme` | native 3×3 recipes |
| 30 | Печь | `ContainerSkinMixin / GameUiTheme` | native furnace / progress / recipe book |
| 31 | Зачарование | `ContainerSkinMixin / GameUiTheme` | native book / costs / offers |
| 32 | Наковальня | `ContainerSkinMixin / GameUiTheme` | native rename field / costs |
| 33 | Торговля | `ContainerSkinMixin / GameUiTheme` | native trade list and availability |
| 34 | Зельеварение | `ContainerSkinMixin / GameUiTheme` | native brewing state |
| 35 | Кузнечный стол | `ContainerSkinMixin / GameUiTheme` | native smithing templates / preview |
| 36 | Картографический стол | `ContainerSkinMixin / GameUiTheme` | native map result previews |
| 37 | Камнерез | `ContainerSkinMixin / GameUiTheme` | native recipe grid / scroll |
| 38 | Ткацкий станок | `ContainerSkinMixin / GameUiTheme` | native patterns / banner preview |
| 39 | Точило | `ContainerSkinMixin / GameUiTheme` | native repair / disenchant |
| 40 | Маяк | `ContainerSkinMixin / GameUiTheme` | native powers / payment / availability |
| 41 | Достижения | `AdvancementSkinMixin / GameUiTheme` | native graph / tabs / scroll / tooltips |
| 42 | Статистика | `GlassMenu / menu lists` | native statistics |
| 43 | Чат и список игроков | `ChatGlassMixin / TabGlassMixin` | native messages / links / players / scores |
| 44 | Экран смерти | `DeathSkinMixin / GlassMenu` | native cause / score / respawn delay |
| 45 | Ошибка подключения | `GlassMenu / MenuMessageMixin` | native connection error |
| 46 | Подключение к серверу | `GlassMenu / MenuBackdropMixin` | native connection/loading state |
| 47 | Открытие для сети | `GlassMenu / MenuEditBoxMixin` | native LAN options |
| 48 | Подтверждение удаления | `GlassMenu / MenuMessageMixin` | native confirmation callback |
