package pw.yami.client.module;

import pw.yami.client.manager.Managers;
import pw.yami.client.module.exploit.FastLatency;

public class Ping extends DrawRenderModule {
   public Ping() {
      super("Ping", "");
   }

   @Override
   public void adb() {
      int n = FastLatency.Ov().awg() ? (int)FastLatency.Ov().Oz() : Managers.serverManager.iS();
      Object[] objs = new Object[]{n};
      this.text = String.format("Ping §f%dms", objs);
      super.adb();
   }
}
