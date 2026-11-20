package pw.yami.client.event.movement;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class GetVelocityMultiplierEvent extends Event {
   private final BlockState state;
   public static boolean fh;

   public GetVelocityMultiplierEvent(BlockState state2) {
      this.state = state2;
   }

   public Block aqh() {
      return this.state.getBlock();
   }
}
