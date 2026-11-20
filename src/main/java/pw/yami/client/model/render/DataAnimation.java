package pw.yami.client.model.render;

import pw.yami.client.utils.UnidentifiedHelper7;
import pw.yami.client.utils.world.BlockPosDirectionHelper;

public record DataAnimation(BlockPosDirectionHelper data, UnidentifiedHelper7 animation) {
   public static boolean q;

   public BlockPosDirectionHelper getData() {
      return this.data;
   }
}
