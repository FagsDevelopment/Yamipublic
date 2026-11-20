package pw.yami.client.model.combat;

import net.minecraft.util.math.BlockPos;

public record PosDamage(BlockPos pos, double damage) {
   public BlockPos getPos() {
      return this.pos;
   }
}
