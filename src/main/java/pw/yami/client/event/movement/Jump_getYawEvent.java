package pw.yami.client.event.movement;

import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public final class Jump_getYawEvent extends Event {
   private float a;

   public Jump_getYawEvent(float f) {
      this.a = f;
   }

   public float On() {
      return this.a;
   }

   public void Oo(float f) {
      this.a = f;
   }
}
