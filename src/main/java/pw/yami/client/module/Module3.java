package pw.yami.client.module;

import pw.yami.eventbus.EventBusImpl;

public class Module3 extends ModuleBase {
   public Module3(String str, String str2, ModuleCategory moduleCategory) {
      super(str, str2, moduleCategory);
      EventBusImpl.INSTANCE.rD(this);
   }
}
