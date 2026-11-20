package pw.yami.client.event.player;

import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class InitItemEvent extends Event {
   private int a;

   public InitItemEvent(int n) {
      this.a = n;
   }

   public int abL() {
      return Math.max(0, this.a);
   }

   public int abM() {
      return this.a;
   }

   public void abN(int n) {
      this.a = n;
   }
}
