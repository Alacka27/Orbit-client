package dev.alacka.orbit.accounts;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.server.Services;

/** Complete candidate built before committing a main-thread identity change. */
public record SessionState(
        User user, Services services, UserApiService api,
        CompletableFuture<ProfileResult> profile, CompletableFuture<UserApiService.UserProperties> properties,
        SkinManager skins, PlayerSkinRenderCache skinRender, PlayerSocialManager social,
        ClientTelemetryManager telemetry, ProfileKeyPairManager keys, RealmsDataFetcher realms,
        ReportingContext reporting) {
    @Override public String toString() { return "SessionState[credentials=REDACTED]"; }
}