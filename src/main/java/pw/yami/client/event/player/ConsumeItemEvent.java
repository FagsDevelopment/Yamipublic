package pw.yami.client.event.player;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import pw.yami.client.event.Event;

public class ConsumeItemEvent extends Event {
   private final ItemStack stack;
   public static boolean fd;

   public ConsumeItemEvent(ItemStack stack2) {
      this.stack = stack2;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public Item ks() {
      return this.stack.getItem();
   }
}
