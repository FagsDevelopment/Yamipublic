package pw.yami.client.event.render;

import net.minecraft.entity.ItemEntity;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class RenderEvent7 extends Event {
   private final ItemEntity itemEntity;
   public static boolean gv;

   public RenderEvent7(ItemEntity itemEntity2) {
      this.itemEntity = itemEntity2;
   }

   public ItemEntity getItemEntity() {
      return this.itemEntity;
   }
}
