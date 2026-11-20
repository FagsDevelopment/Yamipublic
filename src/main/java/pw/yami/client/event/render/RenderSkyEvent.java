package pw.yami.client.event.render;

import net.minecraft.client.render.Camera;
import org.joml.Matrix4f;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class RenderSkyEvent extends Event {
   private final Matrix4f matrix4f;
   private final Matrix4f projectionMatrix;
   private final float tickDelta;
   private final Camera camera;
   private final boolean thickFog;

   public RenderSkyEvent(Matrix4f matrix4f, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog) {
      this.matrix4f = matrix4f;
      this.projectionMatrix = projectionMatrix;
      this.tickDelta = tickDelta;
      this.camera = camera;
      this.thickFog = thickFog;
   }

   public Matrix4f getMatrix4f() {
      return this.matrix4f;
   }

   public Matrix4f getProjectionMatrix() {
      return this.projectionMatrix;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public Camera getCamera() {
      return this.camera;
   }

   public boolean isThickFog() {
      return this.thickFog;
   }
}
