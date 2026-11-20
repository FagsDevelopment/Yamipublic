package pw.yami.client.event.player;

import net.minecraft.util.Hand;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class SwingHandEvent extends Event {
   private final Hand hand;
   public static boolean gb;

   public SwingHandEvent(Hand hand2) {
      this.hand = hand2;
   }

   public Hand getHand() {
      return this.hand;
   }
}
