package pw.yami.client.event;

import pw.yami.client.api.UnidentifiedInterface2;

@UnidentifiedInterface2
public class OnKeyEvent extends Event {
   private final int a;
   private final int b;
   public static boolean bg;

   public OnKeyEvent(int n, int n2) {
      this.a = n;
      this.b = n2;
   }

   public int KC() {
      return this.a;
   }

   public int KD() {
      return this.b;
   }
}
