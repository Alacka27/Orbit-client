package dev.alacka.orbit;

import dev.alacka.orbit.hud.HudGeometry;
import dev.alacka.orbit.menu.ModuleHelp;

public final class HudGeometrySelfTest {
    private static int checks;
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        for (int width : new int[]{320,360,427,480,640,854,960,1280,1920}) {
            float scale = HudGeometry.scale(width);
            check(scale >= 1 && scale <= 1.45f, "HUD scale retained");
            for (int height : new int[]{180,200,240,300,400,540,1080}) {
                int center = width / 2;
                var bar = HudGeometry.hotbar(center - 91, height - 22, 182, 22);
                var left = HudGeometry.offhand(center - 120, height - 23, false);
                var right = HudGeometry.offhand(center + 91, height - 23, true);
                check(bar.y() == height - 24 && bar.y() + bar.height() == height, "Lower frame, no bottom clipping");
                check(left.width() == 18 && left.height() == 18 && right.width() == 18 && right.height() == 18, "Offhand matches normal slots");
                check(left.y() == height - 20 && right.y() == left.y(), "All slot baselines match");
                check(bar.x() - left.x() - left.width() == 5 && right.x() - bar.x() - bar.width() == 5, "Symmetric offhand gap");
                check(center - 117 == left.x() + 1 && center + 101 == right.x() + 1, "Vanilla item anchors preserved");
                check(height - 19 == left.y() + 1 && left.y() + left.height() <= height - 2, "Icons keep padding and counts are not moved");
                check(height - 12 > bar.y(), "Native chat input is allowed to overlap the hotbar");
            }
        }
        for (String name : new String[]{"KillAura","TriggerBot","NoFall","Jesus","AutoTotem",
                "AutoArmor","AutoConsume","ElytraSwap","Trajectories","TargetHUD","DamageNumbers","HitParticles","CriticalHitEffect","ShieldStatus","TotemCounter","SeeInvisibles"}) {
            String help = ModuleHelp.text(name);
            check(!help.isBlank() && help.length() < 55 && !help.contains("\n"), "Short right-click text");
        }
        System.out.println("PASS HUD/chat geometry and concise help: " + checks + " assertions");
    }
}