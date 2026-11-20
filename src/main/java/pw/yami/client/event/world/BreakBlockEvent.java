package pw.yami.client.event.world;

import net.minecraft.util.math.BlockPos;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class BreakBlockEvent extends Event {
   private final BlockPos pos;

   public BreakBlockEvent(BlockPos pos2) {
      this.pos = pos2;
   }

   public BlockPos getPos() {
      return this.pos;
   }
}
