package pw.yami.client.event.render;

import net.minecraft.entity.LivingEntity;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class Render_isInvisibleToEvent extends Event {
   private final LivingEntity living;
   public static boolean bc;

   public Render_isInvisibleToEvent(LivingEntity living2) {
      this.living = living2;
   }

   public LivingEntity getLiving() {
      return this.living;
   }
}
