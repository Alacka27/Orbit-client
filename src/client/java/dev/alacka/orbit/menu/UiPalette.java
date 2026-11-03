package dev.alacka.orbit.menu;

import java.awt.Color;

/** Pure colour math, independently tested without Minecraft. */
public final class UiPalette {
    private UiPalette() {}
    public static int tint(int argb, boolean surface, dev.alacka.orbit.config.ClientSettings prefs) {
        if (argb == 0x2e181026) return prefs.windowOpacity() * 255 / 100 << 24 | 0x181026;
        int alpha = argb >>> 24;
        if (alpha == 0) return argb;
        int rgb = argb & 0xffffff;
        float[] hsv = Color.RGBtoHSB(rgb >> 16, rgb >> 8 & 255, rgb & 255, null);
        float[] target = Color.RGBtoHSB(prefs.accent() >> 16, prefs.accent() >> 8 & 255, prefs.accent() & 255, null);
        // Only palette purples; white text and server warning colours stay untouched.
        if (hsv[0] > .59f && hsv[0] < .87f && hsv[1] > .04f) {
            float saturation = Math.min(1, hsv[1] * (target[1] / .58f));
            float brightness = Math.min(1, hsv[2] * (.7f + .3f * target[2]));
            rgb = Color.HSBtoRGB(target[0], saturation, brightness) & 0xffffff;
            if (surface) rgb = readableSurface(rgb);
        }
        if (surface) alpha = Math.min(245, Math.round(alpha * prefs.buttonOpacity() / 60f));
        return alpha << 24 | rgb;
    }
    /** White labels keep >= 4.5:1 contrast against the solid button colour. */
    private static int readableSurface(int rgb) {
        while (luminance(rgb) > .18) {
            int r = (rgb >> 16) * 95 / 100, g = (rgb >> 8 & 255) * 95 / 100, b = (rgb & 255) * 95 / 100;
            rgb = r << 16 | g << 8 | b;
        }
        return rgb;
    }
    private static double luminance(int rgb) { return .2126*channel(rgb >> 16) + .7152*channel(rgb >> 8 & 255) + .0722*channel(rgb & 255); }
    private static double channel(int b) { double c = b / 255.; return c <= .04045 ? c / 12.92 : Math.pow((c+.055)/1.055,2.4); }
}
