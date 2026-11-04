package dev.alacka.orbit.render;

import java.util.Arrays;

public final class ShapeMath {
   private static final float[] NO_LEVELS = new float[0];
   private static final float[][] COS = new float[33][];
   private static final float[][] SIN = new float[33][];

   private ShapeMath() {
   }

   public static int segments(float radiusPixels) {
      return !(radiusPixels > 0.0F) ? 2 : Math.max(2, Math.min(32, (int)Math.ceil(1.756 * Math.sqrt(radiusPixels))));
   }

   private static float[] cos(int n) {
      if (COS[n] == null) {
         table(n);
      }

      return COS[n];
   }

   private static float[] sin(int n) {
      if (SIN[n] == null) {
         table(n);
      }

      return SIN[n];
   }

   private static synchronized void table(int n) {
      if (COS[n] == null) {
         float[] c = new float[n + 1];
         float[] s = new float[n + 1];

         for (int k = 0; k <= n; k++) {
            double a = (Math.PI / 2) * k / n;
            c[k] = (float)Math.cos(a);
            s[k] = (float)Math.sin(a);
         }

         c[n] = 0.0F;
         s[n] = 1.0F;
         SIN[n] = s;
         COS[n] = c;
      }
   }

   public static int alpha(int argb, float factor) {
      if (factor >= 1.0F) {
         return argb;
      } else {
         return !(factor > 0.0F) ? argb & 16777215 : Math.round((argb >>> 24) * factor) << 24 | argb & 16777215;
      }
   }

   public static int scaleRgb(int argb, float factor) {
      int r = Math.min(255, Math.round((argb >> 16 & 0xFF) * factor));
      int g = Math.min(255, Math.round((argb >> 8 & 0xFF) * factor));
      int b = Math.min(255, Math.round((argb & 0xFF) * factor));
      return argb & 0xFF000000 | r << 16 | g << 8 | b;
   }

   private static float[] outline(float cxL, float cxR, float cyT, float cyB, int n, float[] side) {
      float[] c = cos(n);
      float[] s = sin(n);
      float[] o = new float[(4 * (n + 1) + 2 * side.length) * 4];
      int p = 0;

      for (int k = 0; k <= n; k++) {
         p = put(o, p, cxL, cyT, -c[k], -s[k]);
      }

      for (int k = n; k >= 0; k--) {
         p = put(o, p, cxR, cyT, c[k], -s[k]);
      }

      for (float level : side) {
         p = put(o, p, cxR, level, 1.0F, 0.0F);
      }

      for (int k = 0; k <= n; k++) {
         p = put(o, p, cxR, cyB, c[k], s[k]);
      }

      for (int k = n; k >= 0; k--) {
         p = put(o, p, cxL, cyB, -c[k], s[k]);
      }

      for (int i = side.length - 1; i >= 0; i--) {
         p = put(o, p, cxL, side[i], -1.0F, 0.0F);
      }

      return o;
   }

   private static int put(float[] o, int p, float cx, float cy, float nx, float ny) {
      o[p] = cx;
      o[p + 1] = cy;
      o[p + 2] = nx;
      o[p + 3] = ny;
      return p + 4;
   }

   private static void strip(ShapeMath.Mesh m, float[] o, float ra, float rb, ShapeMath.Gradient g, float alphaA, float alphaB) {
      if (!(alphaA <= 0.0F) || !(alphaB <= 0.0F)) {
         int points = o.length / 4;

         for (int i = 0; i < points; i++) {
            int a = i * 4;
            int b = (i + 1) % points * 4;
            float ax = o[a] + ra * o[a + 2];
            float ay = o[a + 1] + ra * o[a + 3];
            float bx = o[b] + ra * o[b + 2];
            float by = o[b + 1] + ra * o[b + 3];
            float cx = o[b] + rb * o[b + 2];
            float cy = o[b + 1] + rb * o[b + 3];
            float dx = o[a] + rb * o[a + 2];
            float dy = o[a + 1] + rb * o[a + 3];
            m.quad(ax, ay, alpha(g.at(ay), alphaA), bx, by, alpha(g.at(by), alphaA), cx, cy, alpha(g.at(cy), alphaB), dx, dy, alpha(g.at(dy), alphaB));
         }
      }
   }

   private static void core(ShapeMath.Mesh m, float cxL, float cxR, float cyT, float cyB, float rho, int n, float[] side, ShapeMath.Gradient g) {
      float[] c = cos(n);
      float[] s = sin(n);
      int total = 2 * (n + 1) + side.length;
      float[] ys = new float[total];
      float[] es = new float[total];
      int p = 0;

      for (int k = n; k >= 0; k--) {
         ys[p] = cyT - rho * s[k];
         es[p++] = rho * c[k];
      }

      for (float level : side) {
         ys[p] = level;
         es[p++] = rho;
      }

      for (int k = 0; k <= n; k++) {
         ys[p] = cyB + rho * s[k];
         es[p++] = rho * c[k];
      }

      for (int i = 0; i + 1 < total; i++) {
         float ya = ys[i];
         float yb = ys[i + 1];
         if (!(yb - ya <= 1.0E-6F)) {
            int ca = g.at(ya);
            int cb = g.at(yb);
            m.quad(cxL - es[i], ya, ca, cxL - es[i + 1], yb, cb, cxR + es[i + 1], yb, cb, cxR + es[i], ya, ca);
         }
      }
   }

   private static float[] inside(float[] levels, float top, float bottom) {
      if (levels != null && levels.length != 0) {
         float[] copy = (float[])levels.clone();
         Arrays.sort(copy);
         int n = 0;

         for (float level : copy) {
            if (level > top + 1.0E-4F && level < bottom - 1.0E-4F && (n == 0 || level - copy[n - 1] > 1.0E-4F)) {
               copy[n++] = level;
            }
         }

         return Arrays.copyOf(copy, n);
      } else {
         return NO_LEVELS;
      }
   }

   private static float radius(float requested, float w, float h, float aa) {
      float half = Math.min(w, h) / 2.0F;
      return Math.max(Math.min(Math.max(0.0F, requested), half), Math.min(aa * 0.5F, half));
   }

   public static void fill(ShapeMath.Mesh m, float x, float y, float w, float h, float radius, ShapeMath.Gradient g, float[] levels) {
      if (w > 0.0F && h > 0.0F) {
         float aa = m.aa;
         float r = radius(radius, w, h, aa);
         float cxL = x + r;
         float cxR = x + w - r;
         float cyT = y + r;
         float cyB = y + h - r;
         int n = segments((r + aa * 0.5F) * m.px);
         float inner = Math.max(0.0F, r - aa * 0.5F);
         float outer = r + aa * 0.5F;
         float[] side = inside(levels, cyT, cyB);
         core(m, cxL, cxR, cyT, cyB, inner, n, side, g);
         strip(m, outline(cxL, cxR, cyT, cyB, n, side), inner, outer, g, 1.0F, 0.0F);
      }
   }

   public static void fill(ShapeMath.Mesh m, float x, float y, float w, float h, float radius, int argb) {
      fill(m, x, y, w, h, radius, yy -> argb, NO_LEVELS);
   }

   public static void ring(ShapeMath.Mesh m, float x, float y, float w, float h, float radius, float thickness, int argb) {
      if (w > 0.0F && h > 0.0F && thickness > 0.0F && argb >>> 24 != 0) {
         float aa = m.aa;
         float r = radius(radius, w, h, aa);
         float half = Math.min(w, h) / 2.0F;
         float cxL = x + r;
         float cxR = x + w - r;
         float cyT = y + r;
         float cyB = y + h - r;
         int n = segments((r + aa * 0.5F) * m.px);
         float[] o = outline(cxL, cxR, cyT, cyB, n, NO_LEVELS);
         ShapeMath.Gradient g = yy -> argb;
         float t = Math.min(thickness, half);
         if (t > aa) {
            float r0 = r + aa * 0.5F;
            float r1 = r - aa * 0.5F;
            float r2 = Math.max(0.0F, r - t + aa * 0.5F);
            float r3 = Math.max(0.0F, r - t - aa * 0.5F);
            strip(m, o, r0, r1, g, 0.0F, 1.0F);
            strip(m, o, r1, r2, g, 1.0F, 1.0F);
            strip(m, o, r2, r3, g, 1.0F, 0.0F);
         } else {
            float mid = r - t * 0.5F;
            float peak = t / aa;
            strip(m, o, mid + aa * 0.5F, mid, g, 0.0F, peak);
            strip(m, o, mid, Math.max(0.0F, mid - aa * 0.5F), g, peak, 0.0F);
         }
      }
   }

   public static void shadow(ShapeMath.Mesh m, float x, float y, float w, float h, float radius, int argb, float spread) {
      if (w > 0.0F && h > 0.0F && argb >>> 24 != 0) {
         float r = Math.max(0.0F, Math.min(radius, Math.min(w, h) / 2.0F));
         float cxL = x + r;
         float cxR = x + w - r;
         float cyT = y + r;
         float cyB = y + h - r;
         int n = Math.max(2, segments((r + spread) * m.px) * 2 / 3);
         ShapeMath.Gradient g = yy -> argb;
         core(m, cxL, cxR, cyT, cyB, r, n, NO_LEVELS, g);
         float[] o = outline(cxL, cxR, cyT, cyB, n, NO_LEVELS);
         int steps = 5;

         for (int i = 0; i < steps; i++) {
            float d0 = spread * i / steps;
            float d1 = spread * (i + 1) / steps;
            float a0 = (float)Math.exp(-d0 * d0 / 12.0F);
            float a1 = i + 1 == steps ? 0.0F : (float)Math.exp(-d1 * d1 / 12.0F);
            strip(m, o, r + d0, r + d1, g, a0, a1);
         }
      }
   }

   public static void line(ShapeMath.Mesh m, float x0, float y0, float x1, float y1, float thickness, int argb) {
      if (thickness > 0.0F && argb >>> 24 != 0) {
         float dx = x1 - x0;
         float dy = y1 - y0;
         float len = (float)Math.hypot(dx, dy);
         float ux = len < 1.0E-4F ? 1.0F : dx / len;
         float uy = len < 1.0E-4F ? 0.0F : dy / len;
         float vx = -uy;
         float vy = ux;
         float aa = m.aa;
         float half = thickness * 0.5F;
         float peak = 1.0F;
         if (thickness < aa) {
            peak = thickness / aa;
            half = aa * 0.5F;
         }

         float inner = Math.max(0.0F, half - aa * 0.5F);
         float outer = half + aa * 0.5F;
         int n = segments(outer * m.px) * 2;
         float[] o = new float[(2 * n + 2) * 4];
         int p = 0;

         for (int k = 0; k <= n; k++) {
            double a = (-Math.PI / 2) + Math.PI * k / n;
            float c = (float)Math.cos(a);
            float s = (float)Math.sin(a);
            p = put(o, p, x1, y1, ux * c + vx * s, uy * c + vy * s);
         }

         for (int k = 0; k <= n; k++) {
            double a = (Math.PI / 2) + Math.PI * k / n;
            float c = (float)Math.cos(a);
            float s = (float)Math.sin(a);
            p = put(o, p, x0, y0, ux * c + vx * s, uy * c + vy * s);
         }

         int solid = alpha(argb, peak);
         ShapeMath.Gradient g = yy -> solid;
         if (inner > 0.0F) {
            float mx = (x0 + x1) * 0.5F;
            float my = (y0 + y1) * 0.5F;
            int points = o.length / 4;

            for (int i = 0; i < points; i++) {
               int a = i * 4;
               int b = (i + 1) % points * 4;
               float ax = o[a] + inner * o[a + 2];
               float ay = o[a + 1] + inner * o[a + 3];
               float bx = o[b] + inner * o[b + 2];
               float by = o[b + 1] + inner * o[b + 3];
               m.quad(mx, my, solid, ax, ay, solid, bx, by, solid, bx, by, solid);
            }
         }

         strip(m, o, inner, outer, g, 1.0F, 0.0F);
      }
   }

   public static int windowColor(int baseRgb, int shadeRgb, int alpha, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      float a = Math.max(0, Math.min(255, alpha)) / 255.0F;
      float b = a * (float)Math.pow(t, 1.35);
      float out = 1.0F - (1.0F - a) * (1.0F - b) * (1.0F - b);
      if (out <= 1.0E-5F) {
         return 0;
      } else {
         float fill = a * (1.0F - b) * (1.0F - b) * (1.0F - 0.729F * t);
         float shade = b * (2.0F - b);
         int r = Math.round(Math.min(255.0F, ((baseRgb >> 16 & 0xFF) * fill + (shadeRgb >> 16 & 0xFF) * shade) / out));
         int g = Math.round(Math.min(255.0F, ((baseRgb >> 8 & 0xFF) * fill + (shadeRgb >> 8 & 0xFF) * shade) / out));
         int bl = Math.round(Math.min(255.0F, ((baseRgb & 0xFF) * fill + (shadeRgb & 0xFF) * shade) / out));
         return Math.round(out * 255.0F) << 24 | r << 16 | g << 8 | bl;
      }
   }

   public static float sliceRow(float local, float height, float border) {
      if (!(height > 0.0F)) {
         return 0.0F;
      } else {
         local = Math.max(0.0F, Math.min(height, local));
         float row;
         if (height <= 2.0F * border) {
            row = local / height * 63.0F;
         } else if (local < border) {
            row = local;
         } else if (local > height - border) {
            row = 63.0F - (height - local);
         } else {
            row = border + (local - border) * (63.0F - 2.0F * border) / (height - 2.0F * border);
         }

         return Math.max(0.0F, Math.min(1.0F, row / 63.0F));
      }
   }

   public interface Gradient {
      int at(float var1);
   }

   public static final class Mesh {
      public float[] xy = new float[256];
      public int[] argb = new int[128];
      public int count;
      public float minX = Float.POSITIVE_INFINITY;
      public float minY = Float.POSITIVE_INFINITY;
      public float maxX = Float.NEGATIVE_INFINITY;
      public float maxY = Float.NEGATIVE_INFINITY;
      public final float px;
      public final float aa;

      public Mesh(float pixelsPerUnit) {
         this.px = Float.isFinite(pixelsPerUnit) ? Math.max(0.25F, Math.min(64.0F, pixelsPerUnit)) : 1.0F;
         this.aa = 1.0F / this.px;
      }

      private void vertex(float x, float y, int c) {
         if (this.count == this.argb.length) {
            this.xy = Arrays.copyOf(this.xy, this.xy.length * 2);
            this.argb = Arrays.copyOf(this.argb, this.argb.length * 2);
         }

         this.xy[this.count * 2] = x;
         this.xy[this.count * 2 + 1] = y;
         this.argb[this.count++] = c;
         if (x < this.minX) {
            this.minX = x;
         }

         if (y < this.minY) {
            this.minY = y;
         }

         if (x > this.maxX) {
            this.maxX = x;
         }

         if (y > this.maxY) {
            this.maxY = y;
         }
      }

      public void quad(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
         if ((c0 | c1 | c2 | c3) >>> 24 != 0) {
            float area = x0 * y1 - x1 * y0 + x1 * y2 - x2 * y1 + x2 * y3 - x3 * y2 + x3 * y0 - x0 * y3;
            if (!(Math.abs(area) < 1.0E-7F)) {
               if (area > 0.0F) {
                  this.vertex(x0, y0, c0);
                  this.vertex(x3, y3, c3);
                  this.vertex(x2, y2, c2);
                  this.vertex(x1, y1, c1);
               } else {
                  this.vertex(x0, y0, c0);
                  this.vertex(x1, y1, c1);
                  this.vertex(x2, y2, c2);
                  this.vertex(x3, y3, c3);
               }
            }
         }
      }

      public boolean isEmpty() {
         return this.count == 0;
      }
   }
}
