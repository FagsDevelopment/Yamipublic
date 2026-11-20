package pw.yami.client.module.movement;

import net.minecraft.client.option.KeyBinding;
import pw.yami.client.Bootstrap;
import pw.yami.client.event.EventPhase;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.model.movement.UnidentifiedInterface14;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.movement.UnidentifiedUtils;
import pw.yami.eventbus.annotation.EventListener;

public class AutoWalk extends ClientModule {
   private static AutoWalk INSTANCE;
   Setting lock = this.registerSetting(new BooleanSetting("Lock", "Stops movement when sneaking or jumping", false));

   public AutoWalk() {
      super("AutoWalk", "Automatically moves forward", ModuleCategory.MOVEMENT);
      INSTANCE = this;
   }

   public static AutoWalk eV() {
      return INSTANCE;
   }

   @Override
   public void awe() {
      if (client.options != null) {
         ((UnidentifiedInterface14)client.options.forwardKey).WK();
      }
   }

   @EventListener
   public void onTickPre(TickPreEvent tickPreEvent) {
      if (tickPreEvent.getEventPhase() == EventPhase.PRE) {
         if (Bootstrap.NV() && UnidentifiedUtils.eP()) {
            return;
         }

         KeyBinding keyBinding;
         boolean flag;
         label26: {
            KeyBinding keyBinding2;
            label25: {
               KeyBinding temp = client.options.forwardKey;
               flag = client.options.sneakKey.isPressed();
               keyBinding = temp;
               if (!flag) {
                  temp = keyBinding;
                  Setting setting2 = this.lock;
                  keyBinding2 = temp;
                  if (!(Boolean)setting2.atx()) {
                     break label25;
                  }

                  temp = keyBinding2;
                  flag = client.options.jumpKey.isPressed();
                  keyBinding = temp;
                  if (!flag) {
                     temp = keyBinding2;
                     flag = client.player.isOnGround();
                     keyBinding = temp;
                     if (flag) {
                        break label25;
                     }
                  }
               }

               temp = keyBinding;
               flag = false;
               keyBinding = temp;
               break label26;
            }

            KeyBinding keyBinding3 = keyBinding2;
            flag = true;
            keyBinding = keyBinding3;
         }

         KeyBinding keyBinding4 = keyBinding;
         boolean flag2 = flag;
         keyBinding4.setPressed(flag2);
      }
   }
}
