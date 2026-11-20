package pw.yami.client.event.render;

import net.minecraft.entity.Entity;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class RenderEntityEvent extends Event {
   private final Entity entity;
   public static boolean fp;

   public RenderEntityEvent(Entity entity2) {
      this.entity = entity2;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
