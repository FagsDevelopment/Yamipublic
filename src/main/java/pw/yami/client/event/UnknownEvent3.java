package pw.yami.client.event;

import pw.yami.client.setting.Setting;

public class UnknownEvent3 extends UnknownEvent8 {
   private final Setting setting;

   public UnknownEvent3(Setting setting2) {
      this.setting = setting2;
   }

   public Setting getSetting() {
      return this.setting;
   }
}
