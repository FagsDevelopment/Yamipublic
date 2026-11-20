package pw.yami.client.utils.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PathResult {
   public final Vec3d exactStart;
   public final Vec3d exactGoal;
   public final BlockPos startBlock;
   public final BlockPos goalBlock;
   public final List<BlockNode> nodes;
   public final List<Vec3d> waypoints;
   public boolean valid;

   public PathResult(Vec3d exactStart, Vec3d exactGoal, BlockPos startBlock, BlockPos goalBlock, List<BlockNode> nodes, boolean valid) {
      this.exactStart = exactStart;
      this.exactGoal = exactGoal;
      this.startBlock = startBlock;
      this.goalBlock = goalBlock;
      this.nodes = nodes != null ? nodes : Collections.emptyList();
      this.valid = valid;

      this.waypoints = new ArrayList<>();
      for (BlockNode node : this.nodes) {
         this.waypoints.add(node.getCenterVec());
      }
      if (!this.waypoints.isEmpty()) {
         this.waypoints.set(this.waypoints.size() - 1, exactGoal);
      }
   }

   public static PathResult empty(Vec3d start, Vec3d goal) {
      BlockPos s = BlockPos.ofFloored(start.x, start.y, start.z);
      BlockPos g = BlockPos.ofFloored(goal.x, goal.y, goal.z);
      return new PathResult(start, goal, s, g, Collections.emptyList(), false);
   }

   public boolean isValid() {
      return this.valid && !this.waypoints.isEmpty();
   }

   public List<Vec3d> getWaypoints() {
      return this.waypoints;
   }

   public List<BlockNode> getNodes() {
      return this.nodes;
   }

   public int size() {
      return this.waypoints.size();
   }

   public Vec3d get(int index) {
      return this.waypoints.get(index);
   }

   public Vec3d getEndGoal() {
      return this.exactGoal;
   }

   @Override
   public String toString() {
      return "PathResult(nodes=" + this.nodes.size() + ", valid=" + this.valid + ", start=" + this.startBlock + ", goal=" + this.goalBlock + ")";
   }
}