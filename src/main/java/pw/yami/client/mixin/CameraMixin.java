package pw.yami.client.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraMixin {
   @Accessor("cameraY")
   float getCameraY();

   @Accessor("lastCameraY")
   float getLastCameraY();

   @Invoker("setPos")
   void invokeSetPos(double x, double y, double z);
}
