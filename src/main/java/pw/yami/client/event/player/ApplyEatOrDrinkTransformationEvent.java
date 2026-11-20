package pw.yami.client.event.player;

import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class ApplyEatOrDrinkTransformationEvent extends Event {
   private float a;

   public float Fx() {
      return this.a;
   }

   public void Fy(float f) {
      this.a = f;
   }
}
