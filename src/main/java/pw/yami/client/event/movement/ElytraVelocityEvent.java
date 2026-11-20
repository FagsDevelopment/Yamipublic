package pw.yami.client.event.movement;

import net.minecraft.util.math.Vec3d;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class ElytraVelocityEvent extends Event {
   private final Vec3d vec;
   public static boolean dq;

   public ElytraVelocityEvent(Vec3d vec2) {
      this.vec = vec2;
   }

   public Vec3d getVec() {
      return this.vec;
   }
}
