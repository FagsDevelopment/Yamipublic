package pw.yami.client.utils.movement;

import pw.yami.client.event.TickEntitiesPreEvent;
import pw.yami.client.module.movement.ElytraFly;
import pw.yami.eventbus.EventBusImpl;
import pw.yami.eventbus.annotation.EventListener;

public class ElytraFlyUtils {
   public static int a_;
   public static boolean a;

   @EventListener
   public static void auN(TickEntitiesPreEvent tickEntitiesPreEvent) {
      if (a_ <= 0) {
         EventBusImpl.INSTANCE.rH(ElytraFlyUtils.class);
      } else {
         ElytraFly.di().do_();
         a_--;
      }
   }
}
