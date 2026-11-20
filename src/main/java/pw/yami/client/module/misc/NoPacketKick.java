package pw.yami.client.module.misc;

import pw.yami.client.event.network.DecodeEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.eventbus.annotation.EventListener;

public class NoPacketKick extends ClientModule {
   public static boolean ca;

   public NoPacketKick() {
      super("NoPacketKick", "Prevents getting kicked by packets", ModuleCategory.MISC);
   }

   @EventListener
   public void onDecode(DecodeEvent decodeEvent) {
      decodeEvent.aim();
   }
}
