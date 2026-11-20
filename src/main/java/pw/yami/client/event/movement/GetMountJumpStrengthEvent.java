package pw.yami.client.event.movement;

import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class GetMountJumpStrengthEvent extends Event {
   private float a;

   public float Dc() {
      return this.a;
   }

   public void Dd(float f) {
      this.a = f;
   }
}
