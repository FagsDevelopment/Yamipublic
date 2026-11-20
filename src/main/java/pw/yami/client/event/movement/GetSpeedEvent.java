package pw.yami.client.event.movement;

import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public final class GetSpeedEvent extends Event {
   float a;

   public float ar() {
      return this.a;
   }

   public void as() {
      this.a = 0.0F;
   }
}
