package dev.alacka.orbit.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import dev.alacka.orbit.accounts.SessionAccess;
import dev.alacka.orbit.accounts.SessionState;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.network.Connection;
import net.minecraft.server.Services;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Minecraft.class)
public abstract class MinecraftSessionMixin implements SessionAccess {
    @Shadow @Final @Mutable private User user;
    @Shadow @Final @Mutable private Services services;
    @Shadow @Final @Mutable private UserApiService userApiService;
    @Shadow @Final @Mutable private CompletableFuture<ProfileResult> profileFuture;
    @Shadow @Final @Mutable private CompletableFuture<UserApiService.UserProperties> userPropertiesFuture;
    @Shadow @Final @Mutable private SkinManager skinManager;
    @Shadow @Final @Mutable private PlayerSkinRenderCache playerSkinRenderCache;
    @Shadow @Final @Mutable private PlayerSocialManager playerSocialManager;
    @Shadow @Final @Mutable private ClientTelemetryManager telemetryManager;
    @Shadow @Final @Mutable private ProfileKeyPairManager profileKeyPairManager;
    @Shadow @Final @Mutable private RealmsDataFetcher realmsDataFetcher;
    @Shadow private ReportingContext reportingContext;
    @Shadow private Connection pendingConnection;

    @Override public SessionState orbit$captureSession() {
        return new SessionState(user, services, userApiService, profileFuture, userPropertiesFuture,
                skinManager, playerSkinRenderCache, playerSocialManager, telemetryManager,
                profileKeyPairManager, realmsDataFetcher, reportingContext);
    }

    /** No allocation, callbacks or network calls here: commit as one main-thread operation. */
    @Override public void orbit$applySession(SessionState next) {
        user = next.user();
        services = next.services();
        userApiService = next.api();
        profileFuture = next.profile();
        userPropertiesFuture = next.properties();
        skinManager = next.skins();
        playerSkinRenderCache = next.skinRender();
        playerSocialManager = next.social();
        telemetryManager = next.telemetry();
        profileKeyPairManager = next.keys();
        realmsDataFetcher = next.realms();
        reportingContext = next.reporting();
    }

    @Override public boolean orbit$hasPendingConnection() { return pendingConnection != null; }
}