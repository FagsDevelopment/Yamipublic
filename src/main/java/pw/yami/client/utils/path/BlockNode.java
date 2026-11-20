package pw.yami.client.utils.path;

import java.util.Objects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BlockNode implements Comparable<BlockNode> {
   public final BlockPos blockPos;
   public double gCost;
   public double fCost;
   public BlockNode parent;

   public BlockNode(BlockPos blockPos, double gCost, double fCost, BlockNode parent) {
      this.blockPos = blockPos;
      this.gCost = gCost;
      this.fCost = fCost;
      this.parent = parent;
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public Vec3d getCenterVec() {
      return new Vec3d(this.blockPos.getX() + 0.5, this.blockPos.getY() + 0.1, this.blockPos.getZ() + 0.5);
   }

   public double getSquaredDistance(BlockNode other) {
      return this.blockPos.getSquaredDistance(other.blockPos);
   }

   public double getSquaredDistance(BlockPos pos) {
      return this.blockPos.getSquaredDistance(pos);
   }

   @Override
   public int compareTo(BlockNode other) {
      int cmp = Double.compare(this.fCost, other.fCost);
      if (cmp == 0) {
         return Double.compare(this.gCost, other.gCost);
      }
      return cmp;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj instanceof BlockNode other) {
         return Objects.equals(this.blockPos, other.blockPos);
      }
      return false;
   }

   @Override
   public int hashCode() {
      return this.blockPos.hashCode();
   }

   @Override
   public String toString() {
      return "BlockNode(pos=" + this.blockPos + ", fCost=" + this.fCost + ", gCost=" + this.gCost + ")";
   }
}