package pw.yami.client.module.combat;

import pw.yami.client.event.combat.DoAttackEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.eventbus.annotation.EventListener;

public class NoHitDelay extends ClientModule {
   public NoHitDelay() {
      super("NoHitDelay", "Removes vanilla attack delay", ModuleCategory.COMBAT);
   }

   @EventListener
   public void onDoAttack(DoAttackEvent doAttackEvent) {
      client.attackCooldown = 0;
   }
}
