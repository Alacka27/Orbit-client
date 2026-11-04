package dev.alacka.orbit.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public final class SmoothShapes {
   private SmoothShapes() {
   }

   public static ShapeMath.Mesh mesh(GuiGraphics g) {
      Matrix3x2fc pose = g.pose();
      float poseScale = (float)Math.sqrt(Math.abs(pose.m00() * pose.m11() - pose.m01() * pose.m10()));
      int gui = 1;
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft != null && minecraft.getWindow() != null) {
         gui = Math.max(1, minecraft.getWindow().getGuiScale());
      }

      return new ShapeMath.Mesh(gui * (poseScale > 1.0E-4F ? poseScale : 1.0F));
   }

   public static void submit(GuiGraphics g, ShapeMath.Mesh mesh) {
      if (!mesh.isEmpty()) {
         Matrix3x2f pose = new Matrix3x2f(g.pose());
         ScreenRectangle scissor = g.scissorStack.peek();
         ScreenRectangle bounds = bounds(mesh, pose, scissor);
         if (bounds != null) {
            g.guiRenderState.submitGuiElement(new SmoothShapes.Shape(pose, mesh.xy, mesh.argb, mesh.count, scissor, bounds));
         }
      }
   }

   private static ScreenRectangle bounds(ShapeMath.Mesh mesh, Matrix3x2f pose, ScreenRectangle scissor) {
      int x0 = (int)Math.floor(mesh.minX) - 1;
      int y0 = (int)Math.floor(mesh.minY) - 1;
      int x1 = (int)Math.ceil(mesh.maxX) + 1;
      int y1 = (int)Math.ceil(mesh.maxY) + 1;
      ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
      return scissor != null ? scissor.intersection(bounds) : bounds;
   }

   public static void image(GuiGraphics g, Identifier texture, float x, float y, float w, float h, float radius, int textureWidth, int textureHeight, int argb) {
      if (!(w <= 0.0F) && !(h <= 0.0F)) {
         ShapeMath.Mesh mesh = mesh(g);
         ShapeMath.fill(mesh, x, y, w, h, radius, argb);
         if (!mesh.isEmpty()) {
            float box = w / h;
            float source = (float)textureWidth / textureHeight;
            float uw = box < source ? box / source : 1.0F;
            float vh = box < source ? 1.0F : source / box;
            float u0 = (1.0F - uw) / 2.0F;
            float v0 = (1.0F - vh) / 2.0F;
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
            Matrix3x2f pose = new Matrix3x2f(g.pose());
            ScreenRectangle scissor = g.scissorStack.peek();
            ScreenRectangle bounds = bounds(mesh, pose, scissor);
            if (bounds != null) {
               g.guiRenderState
                  .submitGuiElement(
                     new SmoothShapes.Picture(
                        pose,
                        mesh.xy,
                        mesh.argb,
                        mesh.count,
                        TextureSetup.singleTexture(tex.getTextureView(), tex.getSampler()),
                        x,
                        y,
                        w,
                        h,
                        u0,
                        v0,
                        u0 + uw,
                        v0 + vh,
                        scissor,
                        bounds
                     )
                  );
            }
         }
      }
   }

   public static void fill(GuiGraphics g, float x, float y, float w, float h, float radius, int argb) {
      ShapeMath.Mesh mesh = mesh(g);
      ShapeMath.fill(mesh, x, y, w, h, radius, argb);
      submit(g, mesh);
   }

   public static void line(GuiGraphics g, float x0, float y0, float x1, float y1, float thickness, int argb) {
      ShapeMath.Mesh mesh = mesh(g);
      ShapeMath.line(mesh, x0, y0, x1, y1, thickness, argb);
      submit(g, mesh);
   }

   private record Picture(
      Matrix3x2fc pose,
      float[] xy,
      int[] argb,
      int count,
      TextureSetup textureSetup,
      float x,
      float y,
      float w,
      float h,
      float u0,
      float v0,
      float u1,
      float v1,
      ScreenRectangle scissorArea,
      ScreenRectangle bounds
   ) implements GuiElementRenderState {
      @Override
      public RenderPipeline pipeline() {
         return RenderPipelines.GUI_TEXTURED;
      }

      @Override
      public void buildVertices(VertexConsumer consumer) {
         for (int i = 0; i < this.count; i++) {
            float px = this.xy[i * 2];
            float py = this.xy[i * 2 + 1];
            float tx = Math.max(0.0F, Math.min(1.0F, (px - this.x) / this.w));
            float ty = Math.max(0.0F, Math.min(1.0F, (py - this.y) / this.h));
            consumer.addVertexWith2DPose(this.pose, px, py)
               .setUv(this.u0 + (this.u1 - this.u0) * tx, this.v0 + (this.v1 - this.v0) * ty)
               .setColor(this.argb[i]);
         }
      }
   }

   private record Shape(Matrix3x2fc pose, float[] xy, int[] argb, int count, ScreenRectangle scissorArea, ScreenRectangle bounds)
      implements GuiElementRenderState {
      @Override
      public RenderPipeline pipeline() {
         return RenderPipelines.GUI;
      }

      @Override
      public TextureSetup textureSetup() {
         return TextureSetup.noTexture();
      }

      @Override
      public void buildVertices(VertexConsumer consumer) {
         for (int i = 0; i < this.count; i++) {
            consumer.addVertexWith2DPose(this.pose, this.xy[i * 2], this.xy[i * 2 + 1]).setColor(this.argb[i]);
         }
      }
   }
}
