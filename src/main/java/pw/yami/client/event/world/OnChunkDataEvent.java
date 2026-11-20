package pw.yami.client.event.world;

import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import pw.yami.client.event.Event;

public class OnChunkDataEvent extends Event {
   private final ChunkDataS2CPacket chunkDataS2CPacket;
   public static boolean dj;

   public OnChunkDataEvent(ChunkDataS2CPacket chunkDataS2CPacket2) {
      this.chunkDataS2CPacket = chunkDataS2CPacket2;
   }

   public ChunkDataS2CPacket getChunkDataS2CPacket() {
      return this.chunkDataS2CPacket;
   }
}
