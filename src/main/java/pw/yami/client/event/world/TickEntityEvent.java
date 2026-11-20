package pw.yami.client.event.world;

import net.minecraft.entity.Entity;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class TickEntityEvent extends Event {
   private final Entity entity;
   public static boolean em;

   public TickEntityEvent(Entity entity2) {
      this.entity = entity2;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
