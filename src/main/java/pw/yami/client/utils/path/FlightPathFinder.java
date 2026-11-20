package pw.yami.client.utils.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiPredicate;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

/**
 * High-performance 3D Voxel A* Pathfinder inspired by Phobos's BlockPathFinder architecture.
 * Adapted for 3D Flight (Elytra/FakeFly), obstacle penetration, and Mace Smash top-down trajectories.
 */
public class FlightPathFinder {
   public static final int[][] DIRECTIONS = new int[][]{
      // 6 Orthogonal (Phobos directionArray: DOWN, NORTH, SOUTH, EAST, WEST, UP)
      {0, -1, 0}, {0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0},
      // 12 Planar Diagonals
      {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
      {1, 1, 0}, {1, -1, 0}, {-1, 1, 0}, {-1, -1, 0},
      {0, 1, 1}, {0, 1, -1}, {0, -1, 1}, {0, -1, -1},
      // 8 3D Diagonals
      {1, 1, 1}, {1, 1, -1}, {1, -1, 1}, {1, -1, -1},
      {-1, 1, 1}, {-1, 1, -1}, {-1, -1, 1}, {-1, -1, -1}
   };

   public final ClientWorld world;
   public final ClientPlayerEntity player;
   public final BiPredicate<BlockPos, BlockPos> stepPredicate;
   public final BiPredicate<BlockPos, BlockPos> goalPredicate;
   public final int maxBudget;
   public final boolean maceAltitudeBias;

   public FlightPathFinder(ClientWorld world, ClientPlayerEntity player, BiPredicate<BlockPos, BlockPos> stepPredicate, BiPredicate<BlockPos, BlockPos> goalPredicate, int maxBudget, boolean maceAltitudeBias) {
      this.world = world;
      this.player = player;
      this.stepPredicate = stepPredicate != null ? stepPredicate : this::defaultStepPredicate;
      this.goalPredicate = goalPredicate != null ? goalPredicate : this::defaultGoalPredicate;
      this.maxBudget = maxBudget > 0 ? maxBudget : 400;
      this.maceAltitudeBias = maceAltitudeBias;
   }

   public PathResult findPath(Vec3d exactStart, Vec3d exactGoal) {
      if (this.world == null) {
         return PathResult.empty(exactStart, exactGoal);
      }

      BlockPos startPos = BlockPos.ofFloored(exactStart.x, exactStart.y, exactStart.z);
      BlockPos goalPos = BlockPos.ofFloored(exactGoal.x, exactGoal.y, exactGoal.z);

      if (startPos.equals(goalPos)) {
         List<BlockNode> single = new ArrayList<>();
         single.add(new BlockNode(startPos, 0.0, 0.0, null));
         return new PathResult(exactStart, exactGoal, startPos, goalPos, single, true);
      }

      // PriorityQueue (OpenSet) ordered by fCost (f = g + h)
      PriorityQueue<BlockNode> openSet = new PriorityQueue<>();
      // Closed set to avoid re-evaluating nodes
      Set<BlockPos> closedSet = new HashSet<>();
      // gCost map (Phobos map2)
      Map<BlockPos, Double> gCostMap = new HashMap<>();

      double startH = Math.sqrt(startPos.getSquaredDistance(goalPos));
      BlockNode startNode = new BlockNode(startPos, 0.0, startH, null);
      openSet.add(startNode);
      gCostMap.put(startPos, 0.0);

      BlockNode bestNode = startNode;
      double bestDistance = startH;
      int iterations = 0;

      while (!openSet.isEmpty() && iterations < this.maxBudget) {
         iterations++;
         BlockNode current = openSet.poll();
         BlockPos currentPos = current.blockPos;

         if (closedSet.contains(currentPos)) continue;
         closedSet.add(currentPos);

         double distToGoal = Math.sqrt(currentPos.getSquaredDistance(goalPos));
         if (distToGoal < bestDistance) {
            bestDistance = distToGoal;
            bestNode = current;
         }

         // Goal check
         if (this.goalPredicate.test(currentPos, goalPos) || distToGoal <= 1.42) {
            bestNode = current;
            break;
         }

         // Expand 26 directions
         for (int[] dir : DIRECTIONS) {
            int nx = currentPos.getX() + dir[0];
            int ny = currentPos.getY() + dir[1];
            int nz = currentPos.getZ() + dir[2];

            if (ny < -64 || ny > 319) continue;

            BlockPos neighborPos = new BlockPos(nx, ny, nz);
            if (closedSet.contains(neighborPos)) continue;

            // Validate step via stepPredicate (Phobos blockPosBiPredicate2)
            if (!this.stepPredicate.test(currentPos, neighborPos)) continue;

            double stepDist = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
            double tentativeG = current.gCost + stepDist;

            // Mace Smash altitude bias: discount nodes that fly higher than the goal
            if (this.maceAltitudeBias) {
               if (ny > goalPos.getY() + 1) {
                  tentativeG -= 0.15 * Math.min(8.0, ny - goalPos.getY());
               } else if (ny < goalPos.getY()) {
                  tentativeG += 0.25 * (goalPos.getY() - ny);
               }
            }

            if (tentativeG < gCostMap.getOrDefault(neighborPos, Double.POSITIVE_INFINITY)) {
               gCostMap.put(neighborPos, tentativeG);
               double h = Math.sqrt(neighborPos.getSquaredDistance(goalPos));
               BlockNode neighborNode = new BlockNode(neighborPos, tentativeG, tentativeG + h, current);
               openSet.add(neighborNode);
            }
         }
      }

      // Backtrack path from goal to start (Phobos resolveObject logic)
      List<BlockNode> path = new ArrayList<>();
      BlockNode curr = bestNode;
      while (curr != null) {
         path.add(curr);
         curr = curr.parent;
      }
      Collections.reverse(path);

      boolean reachedGoal = bestDistance <= 2.2;
      return new PathResult(exactStart, exactGoal, startPos, goalPos, path, reachedGoal);
   }

   public boolean defaultStepPredicate(BlockPos from, BlockPos to) {
      if (!isPassable(this.world, to)) return false;

      // Diagonal corner-cutting check
      int dx = to.getX() - from.getX();
      int dy = to.getY() - from.getY();
      int dz = to.getZ() - from.getZ();

      if (dx != 0 && dz != 0) {
         if (!isPassable(this.world, new BlockPos(from.getX() + dx, from.getY(), from.getZ()))
            && !isPassable(this.world, new BlockPos(from.getX(), from.getY(), from.getZ() + dz))) {
            return false;
         }
      }
      if (dy != 0 && dx != 0) {
         if (!isPassable(this.world, new BlockPos(from.getX() + dx, from.getY(), from.getZ()))
            && !isPassable(this.world, new BlockPos(from.getX(), from.getY() + dy, from.getZ()))) {
            return false;
         }
      }
      if (dy != 0 && dz != 0) {
         if (!isPassable(this.world, new BlockPos(from.getX(), from.getY(), from.getZ() + dz))
            && !isPassable(this.world, new BlockPos(from.getX(), from.getY() + dy, from.getZ()))) {
            return false;
         }
      }

      return true;
   }

   public boolean defaultGoalPredicate(BlockPos current, BlockPos goal) {
      return current.equals(goal);
   }

   public static boolean isPassable(ClientWorld world, BlockPos pos) {
      if (world == null) return false;
      if (pos.getY() < -64 || pos.getY() > 319) return false;

      BlockState state = world.getBlockState(pos);
      if (!state.isAir() && state.blocksMovement()) {
         return false;
      }

      // Player headroom check (needs 2 blocks vertical clearance)
      BlockState stateUp = world.getBlockState(pos.up());
      return stateUp.isAir() || !stateUp.blocksMovement();
   }

   public static boolean isCorridorClear(ClientWorld world, ClientPlayerEntity player, Vec3d from, Vec3d to) {
      if (world == null) return false;

      RaycastContext ctx = new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, player);
      if (world.raycast(ctx).getType() == Type.BLOCK) {
         return false;
      }

      double dist = from.distanceTo(to);
      if (dist > 1.5) {
         Vec3d mid = from.add(to).multiply(0.5);
         BlockPos bpMid = BlockPos.ofFloored(mid.x, mid.y, mid.z);
         if (!isPassable(world, bpMid)) return false;

         if (dist > 4.0) {
            Vec3d p25 = from.add(to.subtract(from).multiply(0.25));
            Vec3d p75 = from.add(to.subtract(from).multiply(0.75));
            if (!isPassable(world, BlockPos.ofFloored(p25.x, p25.y, p25.z)) ||
                !isPassable(world, BlockPos.ofFloored(p75.x, p75.y, p75.z))) {
               return false;
            }
         }
      }

      return true;
   }

   /**
    * Line-of-sight shortcutting (string-pulling):
    * Scans from the end of the path backwards to find the furthest directly visible node from playerPos.
    */
   public static Vec3d getSmoothedWaypoint(ClientWorld world, ClientPlayerEntity player, Vec3d playerPos, PathResult pathResult) {
      if (!pathResult.isValid() || pathResult.waypoints.isEmpty()) {
         return pathResult.exactGoal;
      }

      List<Vec3d> waypoints = pathResult.getWaypoints();
      for (int i = waypoints.size() - 1; i >= 0; i--) {
         Vec3d wp = waypoints.get(i);
         if (isCorridorClear(world, player, playerPos, wp)) {
            return wp;
         }
      }

      return waypoints.get(0);
   }
}