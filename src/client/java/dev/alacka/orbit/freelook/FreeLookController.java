package dev.alacka.orbit.freelook;

import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.config.FreeLookConfig;
import java.io.IOException;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartTick;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class FreeLookController {
   private static final FreeLookState STATE = new FreeLookState();
   private static FreeLookConfig config = FreeLookConfig.DEFAULT;
   private static Object lastPlayer;
   private static Object lastLevel;
   private static boolean releaseRequired = true;
   private static long lastNanos;

   private FreeLookController() {
   }

   public static void initialize() {
      try {
         config = FreeLookConfig.load(FabricLoader.getInstance().getConfigDir().resolve("orbit").resolve("freelook.properties"));
      } catch (Exception var1) {
         config = FreeLookConfig.DEFAULT;
         OrbitClient.LOGGER.error("[Orbit] FreeLook uses safe defaults: invalid config preserved.", var1);
      }

      ClientTickEvents.START_CLIENT_TICK.register((StartTick)client -> synchronize());
   }

   public static boolean enabled() {
      return true;
   }

   public static double returnSpeed() {
      return config.returnSpeed();
   }

   public static void apply(FreeLookConfig next) throws IOException {
      FreeLookConfig builtIn = new FreeLookConfig(true, next.returnSpeed());
      builtIn.save(FabricLoader.getInstance().getConfigDir().resolve("orbit/freelook.properties"));
      config = builtIn;
      STATE.reset();
      releaseRequired = true;
   }

   public static String keyLabel() {
      return dev.alacka.orbit.input.ModuleBinds.label("FreeLook");
   }

   public static void synchronize() {
      Minecraft client = Minecraft.getInstance();
      long now = System.nanoTime();
      double seconds = lastNanos == 0L ? 0.0 : (now - lastNanos) / 1.0E9;
      lastNanos = now;
      if (client.player != lastPlayer || client.level != lastLevel) {
         STATE.reset();
         releaseRequired = true;
         lastPlayer = client.player;
         lastLevel = client.level;
      }

      boolean allowed = client.player != null
         && client.level != null
         && client.player.isAlive()
         && client.getCameraEntity() == client.player
         && client.screen == null
         && client.isWindowActive()
         && client.options.getCameraType() == CameraType.THIRD_PERSON_BACK;
      if (!allowed) {
         STATE.reset();
         releaseRequired = true;
      } else {
         boolean held = dev.alacka.orbit.input.ModuleBinds.held("FreeLook", true);
         if (releaseRequired) {
            if (held) {
               return;
            }

            releaseRequired = false;
         }

         STATE.update(held, seconds, config.returnSpeed());
      }
   }

   public static boolean turnCamera(Entity player, double horizontal, double vertical) {
      synchronize();
      Minecraft client = Minecraft.getInstance();
      if (player == client.player && STATE.isHeld()) {
         STATE.turn(horizontal, vertical, player.getXRot());
         return true;
      } else {
         return false;
      }
   }

   public static float cameraYaw(Entity entity, float original) {
      return entity == Minecraft.getInstance().player && STATE.isActive() ? STATE.cameraYaw(original) : original;
   }

   public static float cameraPitch(Entity entity, float original) {
      return entity == Minecraft.getInstance().player && STATE.isActive() ? STATE.cameraPitch(original) : original;
   }
}
