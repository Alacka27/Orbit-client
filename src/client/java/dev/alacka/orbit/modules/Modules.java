package dev.alacka.orbit.modules;

import dev.alacka.orbit.config.OrbitSettings;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class Modules {
   public static final List<Modules.Module> ALL = List.of(
      new Modules.Module("KillAura", () -> OrbitSettings.get().combat().aura().enabled()),
      new Modules.Module("TriggerBot", () -> OrbitSettings.get().combat().trigger().enabled()),
      new Modules.Module("NoFall", () -> OrbitSettings.get().noFall()),
      new Modules.Module("Jesus", () -> OrbitSettings.get().jesus()),
      new Modules.Module("AutoTotem", () -> OrbitSettings.get().support().totem().enabled()),
      new Modules.Module("AutoArmor", () -> OrbitSettings.get().support().armor().enabled()),
      new Modules.Module("AutoConsume", () -> OrbitSettings.get().support().consume().enabled()),
      new Modules.Module("ElytraSwap", () -> OrbitSettings.get().support().elytra().enabled()),
      new Modules.Module("Trajectories", () -> OrbitSettings.get().trajectories()),
      new Modules.Module("TargetHUD", () -> OrbitSettings.get().visual().enabled("TargetHUD")),
      new Modules.Module("DamageNumbers", () -> OrbitSettings.get().visual().enabled("DamageNumbers")),
      new Modules.Module("HitParticles", () -> OrbitSettings.get().visual().enabled("HitParticles")),
      new Modules.Module("CriticalHitEffect", () -> OrbitSettings.get().visual().enabled("CriticalHitEffect")),
      new Modules.Module("ShieldStatus", () -> OrbitSettings.get().visual().enabled("ShieldStatus")),
      new Modules.Module("TotemCounter", () -> OrbitSettings.get().visual().enabled("TotemCounter")),
      new Modules.Module("SeeInvisibles", () -> OrbitSettings.get().visual().enabled("SeeInvisibles"))
   );

   private Modules() {
   }

   public static boolean enabled(String name) {
      return ALL.stream().filter(module -> module.name().equals(name)).findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Unknown module: " + name)).enabled().getAsBoolean();
   }

   public record Module(String name, BooleanSupplier enabled) {
   }
}
