package dev.alacka.orbit.accounts;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.menu.PlayerIdentity;
import dev.alacka.orbit.mixin.RealmsClientAccessor;
import dev.alacka.orbit.profiles.ProfileLaunch;
import dev.alacka.orbit.profiles.ProfileStore;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.player.LocalPlayerResolver;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.server.Services;
import net.minecraft.server.players.CachedUserNameToIdResolver;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.Util;

/** No credentials on disk. All commits are main-menu-only, on the client thread. */
public final class AccountSessions {
    private static final Map<String, AuthSession> ONLINE = new LinkedHashMap<>();
    private static SessionState launcher;
    private static volatile String active = ProfileStore.LAUNCHER;
    private static volatile long revision;
    private static String pendingStartup;
    private static final UserApiService.UserProperties LOCAL_PROPERTIES = new UserApiService.UserProperties(
            Set.of(UserApiService.UserFlag.SERVERS_ALLOWED, UserApiService.UserFlag.CHAT_ALLOWED), Map.of());

    private AccountSessions() {}
    public static String active() { return active; }
    public static long revision() { return revision; }
    public static boolean mojangServicesAllowed() { return active.equals(ProfileStore.LAUNCHER) || active.startsWith("microsoft:"); }
    public static boolean offline() { return !active.equals(ProfileStore.LAUNCHER) && !active.contains(":"); }
    public static String modeLabel() {
        return active.equals(ProfileStore.LAUNCHER) ? "Аккаунт лаунчера" : active.startsWith("ely:") ? "Ely.by"
                : active.startsWith("microsoft:") ? "Microsoft" : "Offline-профиль";
    }
    public static Path preferencesPath() { return FabricLoader.getInstance().getConfigDir().resolve("orbit/accounts.properties"); }
    public static List<AuthSession> online() { return List.copyOf(ONLINE.values()); }
    public static User launcherUser() {
        return launcher == null ? ProfileLaunch.launcherUser() : launcher.user();
    }

    public static void initialize() {
        ClientLifecycleEvents.CLIENT_STARTED.register(minecraft -> {
            launcher = access(minecraft).orbit$captureSession();
            try {
                String selected = ProfileStore.read(ProfileLaunch.path()).selected();
                if (!selected.equals(ProfileStore.LAUNCHER)) pendingStartup = selected;
            } catch (Exception error) {
                OrbitClient.LOGGER.warn("[Orbit] Invalid offline profile selection preserved; launcher identity retained.");
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (pendingStartup == null || !(minecraft.screen instanceof TitleScreen) || !canSwitch(minecraft)) return;
            String name = pendingStartup;
            pendingStartup = null;
            try { applyOffline(minecraft, name); }
            catch (Exception error) { OrbitClient.LOGGER.warn("[Orbit] Could not apply offline profile; launcher identity retained."); }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> ONLINE.clear());
    }

    private static SessionAccess access(Minecraft minecraft) { return (SessionAccess) (Object) minecraft; }
    public static boolean canSwitch(Minecraft minecraft) {
        return minecraft != null && SessionGuard.canSwitch(minecraft.isSameThread(), minecraft.level != null,
                minecraft.player != null, minecraft.getConnection() != null, access(minecraft).orbit$hasPendingConnection(),
                minecraft.hasSingleplayerServer() || minecraft.isLocalServer(), minecraft.getOverlay() != null);
    }
    private static void requireMenu(Minecraft minecraft) {
        if (!canSwitch(minecraft)) throw new IllegalStateException("Сначала выйди из мира/сервера и дождись главного меню.");
    }

    public static void applyOffline(Minecraft minecraft, String name) throws java.io.IOException {
        requireMenu(minecraft);
        if (!ProfileStore.validName(name)) throw new IllegalArgumentException("Ник: 3–16 латинских букв, цифр или _.");
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        User user = new User(name, uuid, "", Optional.empty(), Optional.empty());
        Path cache = cache(minecraft, "offline");
        Services services = Services.create(YggdrasilAuthenticationService.createOffline(minecraft.getProxy()), cache.toFile());
        SessionState state = build(minecraft, user, services, UserApiService.OFFLINE, LOCAL_PROPERTIES,
                CompletableFuture.completedFuture(new ProfileResult(new GameProfile(uuid, name))),
                ProfileKeyPairManager.EMPTY_KEY_MANAGER, false, "offline");
        commit(minecraft, state, name);
    }

    public static void applyLauncher(Minecraft minecraft) throws java.io.IOException {
        requireMenu(minecraft);
        if (launcher == null) throw new IllegalStateException("Аккаунт лаунчера ещё загружается.");
        SessionState state = build(minecraft, launcher.user(), launcher.services(), launcher.api(), null,
                launcher.profile(), launcher.keys(), true, "launcher");
        // Retain the original provider's permission response rather than inventing permissions.
        state = new SessionState(state.user(), state.services(), state.api(), state.profile(), launcher.properties(),
                state.skins(), state.skinRender(), state.social(), state.telemetry(), state.keys(), state.realms(), state.reporting());
        commit(minecraft, state, ProfileStore.LAUNCHER);
    }

    public static final class Prepared {
        private final AuthSession session;
        private final Services services;
        private final UserApiService api;
        private final UserApiService.UserProperties properties;
        private Prepared(AuthSession session, Services services, UserApiService api, UserApiService.UserProperties properties) {
            this.session = session; this.services = services; this.api = api; this.properties = properties;
        }
        @Override public String toString() { return "PreparedAccount[REDACTED]"; }
    }

    /** Worker thread: fetch real Microsoft permissions before enabling any online features. */
    public static Prepared prepareOnline(Minecraft minecraft, AuthSession session) throws AuthFailure {
        try {
            if (session.provider() == AuthSession.Provider.ELY) {
                Path cache = cache(minecraft, "ely");
                ElySessionService provider = new ElySessionService(new AuthHttp(minecraft.getProxy()));
                var names = new CachedUserNameToIdResolver(provider, cache.resolve("usercache.json").toFile());
                Services services = new Services(provider, ServicesKeySet.EMPTY, provider, names, new ProfileResolver.Cached(provider, names));
                return new Prepared(session, services, UserApiService.OFFLINE, LOCAL_PROPERTIES);
            }
            // A new Microsoft session must not inherit a launcher's custom auth environment.
            var authentication = new YggdrasilAuthenticationService(
                    minecraft.getProxy(), YggdrasilEnvironment.PROD.getEnvironment());
            Services services = Services.create(authentication, cache(minecraft, "microsoft").toFile());
            UserApiService api = authentication.createUserApiService(session.accessToken());
            // Do not substitute permissive offline flags on a network/parental-permission failure.
            UserApiService.UserProperties properties = api.fetchProperties();
            return new Prepared(session, services, api, properties);
        } catch (Exception error) { throw new AuthFailure(AuthFailure.Reason.NETWORK); }
    }

    public static void applyOnline(Minecraft minecraft, Prepared prepared) throws java.io.IOException {
        requireMenu(minecraft);
        AuthSession session = prepared.session;
        if (ONLINE.size() >= 20 && !ONLINE.containsKey(session.key())) throw new IllegalStateException("Максимум 20 онлайн-сессий за запуск.");
        boolean microsoft = session.provider() == AuthSession.Provider.MICROSOFT;
        User user = new User(session.name(), session.id(), session.accessToken(),
                session.xuid().isEmpty() ? Optional.empty() : Optional.of(session.xuid()),
                microsoft ? Optional.of(session.clientId()) : Optional.empty());
        CompletableFuture<ProfileResult> profile = CompletableFuture.supplyAsync(() -> {
            try {
                ProfileResult fetched = prepared.services.sessionService().fetchProfile(session.id(), true);
                if (fetched != null && fetched.profile().id().equals(session.id())) return fetched;
            } catch (RuntimeException ignored) {}
            return new ProfileResult(new GameProfile(session.id(), session.name()));
        }, Util.ioPool());
        SessionState state = build(minecraft, user, prepared.services, prepared.api, prepared.properties, profile,
                microsoft ? ProfileKeyPairManager.create(prepared.api, user, minecraft.gameDirectory.toPath()) : ProfileKeyPairManager.EMPTY_KEY_MANAGER,
                microsoft, microsoft ? "microsoft" : "ely");
        commit(minecraft, state, session.key());
        ONLINE.put(session.key(), session);
    }

    public static AuthSession forget(String key) {
        if (active.equals(key)) throw new IllegalStateException("Сначала переключись на другой аккаунт.");
        return ONLINE.remove(key);
    }

    private static Path cache(Minecraft minecraft, String provider) throws java.io.IOException {
        Path path = minecraft.gameDirectory.toPath().resolve("orbit/account-cache").resolve(provider);
        Files.createDirectories(path);
        return path;
    }

    private static SessionState build(Minecraft minecraft, User user, Services services, UserApiService api,
                                      UserApiService.UserProperties properties, CompletableFuture<ProfileResult> profile,
                                      ProfileKeyPairManager keys, boolean mojang, String provider) throws java.io.IOException {
        SkinTextureDownloader downloader = new SkinTextureDownloader(minecraft.getProxy(), minecraft.getTextureManager(), minecraft);
        SkinManager skins = new SkinManager(cache(minecraft, provider).resolve("skins"), services, downloader, minecraft);
        PlayerSkinRenderCache render = new PlayerSkinRenderCache(minecraft.getTextureManager(), skins, new LocalPlayerResolver(minecraft, services.profileResolver()));
        var realmsClient = RealmsClientAccessor.orbit$create(mojang ? user.getSessionId() : "", user.getName(), minecraft);
        return new SessionState(user, services, api, profile,
                CompletableFuture.completedFuture(properties == null ? LOCAL_PROPERTIES : properties),
                skins, render, new PlayerSocialManager(minecraft, api),
                new ClientTelemetryManager(minecraft, api, user), keys, new RealmsDataFetcher(realmsClient),
                ReportingContext.create(ReportEnvironment.local(), api));
    }

    private static void commit(Minecraft minecraft, SessionState state, String key) {
        requireMenu(minecraft); // Recheck after preparation; a connection must never race the commit.
        SessionState previous = access(minecraft).orbit$captureSession();
        access(minecraft).orbit$applySession(state);
        active = key;
        pendingStartup = null;
        revision++;
        PlayerIdentity.reset();
        try { previous.social().stopOnlineMode(); } catch (RuntimeException ignored) {}
        try { previous.telemetry().close(); } catch (RuntimeException ignored) {}
        OrbitClient.LOGGER.info("[Orbit] Account session switched; provider={}, revision={}.", modeLabel(), revision);
    }
}