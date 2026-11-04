package dev.alacka.orbit.modules;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Owns only the artificial use-key hold; real user input is restored on release. */
final class UseKeyLease {
    private boolean owned;
    boolean acquire(Minecraft mc) {
        if (owned) return true;
        if (mc.options.keyUse.isDown()||physicalDown(mc)) return false;
        owned=true; mc.options.keyUse.setDown(true); return true;
    }
    boolean releasedByUser(Minecraft mc) { return owned&&!mc.options.keyUse.isDown(); }
    void hold(Minecraft mc) { if (owned) mc.options.keyUse.setDown(true); }
    void release(Minecraft mc,boolean restorePhysical) {
        if (!owned) return;
        owned=false;
        mc.options.keyUse.setDown(restorePhysical&&mc.isWindowActive()&&physicalDown(mc));
    }
    private static boolean physicalDown(Minecraft mc) {
        if (!mc.isWindowActive()||mc.options.keyUse.isUnbound()) return false;
        InputConstants.Key key=InputConstants.getKey(mc.options.keyUse.saveString());
        long window=mc.getWindow().handle();
        if (key.getType()==InputConstants.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window,key.getValue())==GLFW.GLFW_PRESS;
        if (key.getType()==InputConstants.Type.KEYSYM)
            return InputConstants.isKeyDown(mc.getWindow(),key.getValue());
        // Scancode bindings also respect manual input. Only valid GLFW key symbols are queried.
        int[][] ranges={{32,32},{39,39},{44,57},{59,59},{61,61},{65,93},{96,96},{161,162},
                {256,269},{280,284},{290,314},{320,336},{340,348}};
        for (int[] range:ranges) for(int symbol=range[0];symbol<=range[1];symbol++)
            if (GLFW.glfwGetKeyScancode(symbol)==key.getValue())
                return GLFW.glfwGetKey(window,symbol)==GLFW.GLFW_PRESS;
        return false;
    }
}