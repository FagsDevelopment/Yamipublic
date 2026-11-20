package pw.yami.client.event.player;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import pw.yami.client.Mc;
import pw.yami.client.event.Event;

public class SetCurrentHandEvent extends Event {
   private final Hand hand;
   public static boolean cp;

   public SetCurrentHandEvent(Hand hand2) {
      this.hand = hand2;
   }

   public Hand getHand() {
      return this.hand;
   }

   public ItemStack VX() {
      Hand hand2 = this.hand;
      return Mc.client.player.getStackInHand(hand2);
   }
}
