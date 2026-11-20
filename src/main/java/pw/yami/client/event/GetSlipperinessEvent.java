package pw.yami.client.event;

import net.minecraft.block.Block;
import pw.yami.client.api.UnidentifiedInterface2;

@UnidentifiedInterface2
public class GetSlipperinessEvent extends Event {
   private final Block block;
   private float b;

   public GetSlipperinessEvent(Block block2, float f) {
      this.block = block2;
      this.b = f;
   }

   public Block getBlock() {
      return this.block;
   }

   public float lU() {
      return this.b;
   }

   public void lV() {
      this.b = 0.6F;
   }
}
