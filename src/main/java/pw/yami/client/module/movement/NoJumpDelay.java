package pw.yami.client.module.movement;

import pw.yami.client.event.movement.TickMovementEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.eventbus.annotation.EventListener;

public class NoJumpDelay extends ClientModule {
   public NoJumpDelay() {
      super("NoJumpDelay", "Removes the vanilla jump delay", ModuleCategory.MOVEMENT);
   }

   @EventListener(priority = -999)
   public void onTickMovement(TickMovementEvent tickMovementEvent) {
      tickMovementEvent.aim();
   }
}
