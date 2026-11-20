package pw.yami.client.event;

import pw.yami.client.api.UnidentifiedInterface2;

@UnidentifiedInterface2
public class GetFramelimitEvent extends Event {
   private int a;
   public static boolean gk;

   public int arH() {
      return this.a;
   }

   public void arI(int n) {
      this.a = n;
   }
}
