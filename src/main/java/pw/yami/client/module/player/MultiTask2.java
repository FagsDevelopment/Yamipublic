package pw.yami.client.module.player;

import pw.yami.client.event.combat.AttackEvent;
import pw.yami.client.event.player.IsUsingItemEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class MultiTask2 extends ClientModule {
   Setting booleanSetting1_1 = this.registerSetting(new BooleanSetting("Break", "Allows you break when using", true));
   Setting booleanSetting2_1 = this.registerSetting(new BooleanSetting("Attack", "Allows you attack when using", false));

   public MultiTask2() {
      super("MultiTask", "Allows you to mine and use items simultaneously", ModuleCategory.PLAYER);
   }

   @EventListener
   public void onIsUsingItem(IsUsingItemEvent isUsingItemEvent) {
      if ((Boolean)this.booleanSetting1_1.atx()) {
         isUsingItemEvent.aim();
      }
   }

   @EventListener
   public void onAttack(AttackEvent attackEvent) {
      if ((Boolean)this.booleanSetting2_1.atx()) {
         attackEvent.aim();
      }
   }
}
