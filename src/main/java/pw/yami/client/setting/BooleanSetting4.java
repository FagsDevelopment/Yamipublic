package pw.yami.client.setting;

import pw.yami.client.config.ConfigContainer;
import pw.yami.client.module.ClientModule;
import pw.yami.eventbus.EventBusImpl;

public class BooleanSetting4 extends BooleanSetting {
   public static boolean fk;

   public BooleanSetting4(String str, String str2, Boolean flag) {
      super(str, str2, flag);
   }

   @Override
   public void Kl(Boolean flag) {
      super.Kl(flag);
      ConfigContainer configContainer = this.getConfigContainer();
      if (configContainer instanceof ClientModule) {
         if (flag) {
            EventBusImpl.INSTANCE.rD((ClientModule)configContainer);
         } else {
            EventBusImpl.INSTANCE.rG((ClientModule)configContainer);
         }
      }
   }

   public void NR() {
      ConfigContainer configContainer = this.getConfigContainer();
      if (configContainer instanceof ClientModule) {
         ((ClientModule)configContainer).awb();
      }
   }

   public void NS() {
      ConfigContainer configContainer = this.getConfigContainer();
      if (configContainer instanceof ClientModule) {
         ((ClientModule)configContainer).awc();
      }
   }

   @Override
   public void aty(Object obj) {
      this.Kl((Boolean)obj);
   }
}
