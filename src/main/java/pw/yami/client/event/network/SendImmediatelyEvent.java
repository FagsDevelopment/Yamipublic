package pw.yami.client.event.network;

import net.minecraft.network.packet.Packet;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.manager.Managers;

@UnidentifiedInterface2
public class SendImmediatelyEvent extends PacketEvent {
   private final boolean a;
   public static boolean ag;

   public SendImmediatelyEvent(Packet packet2) {
      super(packet2);
      this.a = Managers.serverManager.jc(packet2);
   }

   public boolean apr() {
      return this.a;
   }
}
