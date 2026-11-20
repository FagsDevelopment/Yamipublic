package pw.yami.client.module.movement;

import pw.yami.client.event.HasStatusEffectEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.eventbus.annotation.EventListener;

public class AntiLevitation extends ClientModule {
   public static boolean fw;

   public AntiLevitation() {
      super("AntiLevitation", "Prevents the player from being levitated", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onHasStatusEffect(HasStatusEffectEvent hasStatusEffectEvent) {
      hasStatusEffectEvent.aim();
   }
}
