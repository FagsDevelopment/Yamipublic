package pw.yami.client.event.render;

import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class BeginRenderTickEvent extends Event {
   private float a;
   public static boolean ew;

   public float qS() {
      return this.a;
   }

   public void qT(float f) {
      this.a = f;
   }
}
