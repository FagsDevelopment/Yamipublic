package pw.yami.client.event.combat;

import net.minecraft.entity.Entity;
import pw.yami.client.event.Event;

public class AttackEntityEvent extends Event {
   private final Entity target;

   public AttackEntityEvent(Entity target) {
      this.target = target;
   }

   public Entity getTarget() {
      return this.target;
   }
}
