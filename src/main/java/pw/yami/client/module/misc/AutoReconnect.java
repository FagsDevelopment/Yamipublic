package pw.yami.client.module.misc;

import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;

public class AutoReconnect extends ClientModule {
   Setting delay = this.registerSetting(new NumberSetting("Delay", "The delay between reconnects to a server", 0, 5, 100));

   public AutoReconnect() {
      super("AutoReconnect", "Automatically reconnects to a server immediately after disconnecting", ModuleCategory.MISC);
   }

   public int agA() {
      return (Integer)this.delay.atx();
   }
}
