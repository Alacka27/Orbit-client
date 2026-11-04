package dev.alacka.orbit.menu;

import net.minecraft.client.gui.components.AbstractButton;

public interface OrbitWindow {
   default boolean accent(AbstractButton button) {
      return false;
   }
}
