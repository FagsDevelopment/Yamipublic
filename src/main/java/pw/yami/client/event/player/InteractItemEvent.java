package pw.yami.client.event.player;

import net.minecraft.util.Hand;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class InteractItemEvent extends Event {
   public final Hand hand;
   public static boolean bm;

   public InteractItemEvent(Hand hand2) {
      this.hand = hand2;
   }
}
