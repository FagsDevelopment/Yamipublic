package pw.yami.client.world;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import pw.yami.client.Mc;
import pw.yami.client.model.world.PosState;
import pw.yami.client.model.world.VanillaNcpMode;
import pw.yami.client.utils.player.DirectionPlayerHelper;
import pw.yami.client.utils.world.BlockPosDirectionHelper2;
import pw.yami.client.utils.world.BoxBlockPosHelper;

public abstract class BlockPosDirectionBase {
   public static BlockPosDirectionBase Hp(VanillaNcpMode vanillaNcpMode) {
      switch ((VanillaNcpMode)vanillaNcpMode) {
         case NCP:
            return BlockPosDirectionHelper2.INSTANCE;
         case Grim:
            return BoxBlockPosHelper.INSTANCE;
         default:
            return DirectionPlayerHelper.INSTANCE;
      }
   }

   @Nullable
   public Direction Hq(BlockPos pos, long l) {
      return this.Hs(pos, Mc.client.player, 110176316437589L, Mc.client.world);
   }

   public boolean Hr(BlockPos pos, Direction direction) {
      return this.Ht(pos, direction, Mc.client.world, Mc.client.player);
   }

   @Nullable
   public abstract Direction Hs(BlockPos var1, PlayerEntity var2, long var3, ClientWorld var5);

   public abstract boolean Ht(BlockPos var1, Direction var2, ClientWorld var3, PlayerEntity var4);

   public abstract boolean Hu(BlockPos var1, Direction var2, ClientWorld var3, PlayerEntity var4, PosState var5);
}
