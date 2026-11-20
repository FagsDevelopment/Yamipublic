package pw.yami.client.event;

import pw.yami.client.api.UnidentifiedInterface2;

@UnidentifiedInterface2
public class RecastOnLandEvent extends Event {
   private boolean a;

   public RecastOnLandEvent(boolean flag) {
      this.a = flag;
   }

   public boolean yh() {
      return this.a;
   }

   public void yi() {
      this.a = true;
   }
}
