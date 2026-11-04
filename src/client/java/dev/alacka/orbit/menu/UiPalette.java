package dev.alacka.orbit.menu;

import dev.alacka.orbit.config.ClientSettings;
import java.awt.Color;

public final class UiPalette {
   private UiPalette() {
   }

   public static int tint(int argb, boolean surface, ClientSettings prefs) {
      return tint(argb, surface, prefs, prefs.buttonOpacity());
   }

   public static int tint(int argb, boolean surface, ClientSettings prefs, int density) {
      if (argb == 773328934) {
         return prefs.windowOpacity() * 255 / 100 << 24 | (tint(0xff181026, false, prefs, density) & 0xffffff);
      } else {
         int alpha = argb >>> 24;
         if (alpha == 0) {
            return argb;
         } else {
            int rgb = argb & 16777215;
            float[] hsv = Color.RGBtoHSB(rgb >> 16, rgb >> 8 & 0xFF, rgb & 0xFF, null);
            float[] target = Color.RGBtoHSB(prefs.accent() >> 16, prefs.accent() >> 8 & 0xFF, prefs.accent() & 0xFF, null);
            if (hsv[0] > 0.59F && hsv[0] < 0.87F && hsv[1] > 0.04F) {
               float saturation = Math.min(1.0F, hsv[1] * (target[1] / 0.58F));
               float brightness = Math.min(1.0F, hsv[2] * (0.7F + 0.3F * target[2]));
               rgb = Color.HSBtoRGB(target[0], saturation, brightness) & 16777215;
               if (surface) {
                  rgb = readableSurface(rgb);
               }
            }

            if (surface) {
               alpha = Math.min(245, Math.round(alpha * density / 60.0F));
            }

            return alpha << 24 | rgb;
         }
      }
   }

   /** Recolour the original cloud palette without changing its shapes or alpha. */
   public static int cloudColor(int argb, int accent) {
      if (accent == ClientSettings.DEFAULT.accent()) return argb;
      float[] source = Color.RGBtoHSB(argb >> 16 & 255, argb >> 8 & 255, argb & 255, null);
      float[] target = Color.RGBtoHSB(accent >> 16 & 255, accent >> 8 & 255, accent & 255, null);
      int base = ClientSettings.DEFAULT.accent();
      float[] reference = Color.RGBtoHSB(base >> 16 & 255, base >> 8 & 255, base & 255, null);
      float hue = (source[0] + target[0] - reference[0] + 1) % 1;
      float saturation = Math.min(1, source[1] * target[1] / reference[1]);
      float brightness = Math.min(1, source[2] * (.8f + .2f * target[2] / reference[2]));
      return argb & 0xff000000 | Color.HSBtoRGB(hue, saturation, brightness) & 0xffffff;
   }

   private static int readableSurface(int rgb) {
      while (luminance(rgb) > 0.18) {
         int r = (rgb >> 16) * 95 / 100;
         int g = (rgb >> 8 & 0xFF) * 95 / 100;
         int b = (rgb & 0xFF) * 95 / 100;
         rgb = r << 16 | g << 8 | b;
      }

      return rgb;
   }

   private static double luminance(int rgb) {
      return 0.2126 * channel(rgb >> 16) + 0.7152 * channel(rgb >> 8 & 0xFF) + 0.0722 * channel(rgb & 0xFF);
   }

   public static int light(int rgb, float amount) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      r += Math.round((255 - r) * amount);
      g += Math.round((255 - g) * amount);
      b += Math.round((255 - b) * amount);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private static double channel(int b) {
      double c = b / 255.0;
      return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
   }
}
