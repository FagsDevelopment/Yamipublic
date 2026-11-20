package pw.yami.client.event.render;

import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class RenderEvent2 extends Event {
   private int a;

   public int gI() {
      return this.a;
   }

   public void gJ(int n) {
      this.a = n;
   }
}
