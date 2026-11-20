/*
 * ─────────────────────────────────────────────
 * Based on Phobos' BlockPathFinder architecture (A* over a bounded block grid).
 *
 * Licensed under the MIT License.
 *
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package pw.yami.client.module.movement;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Small bounded 3D A* used by the Smart dive planner.
 *
 * <p>It searches a block grid around the player for a cell from which the
 * target is actually visible (not merely close), then smooths the raw block
 * path into a few straight, body-clear segments. This is what lets Smart go
 * around a roof/tree instead of hovering on top of it.</p>
 */
final class AutoMacePathFinder {
   interface CellTest {
      boolean test(int x, int y, int z);
   }

   interface GoalTest {
      /** Returns the aim point if this cell can attack, otherwise null. */
      Vec3d test(int x, int y, int z);
   }

   interface SegmentTest {
      boolean test(Vec3d fromCenter, Vec3d toCenter);
   }

   static final class Result {
      final List<Vec3d> waypoints;
      final Vec3d aimPoint;

      Result(List<Vec3d> waypoints, Vec3d aimPoint) {
         this.waypoints = waypoints;
         this.aimPoint = aimPoint;
      }
   }

   private static final int[][] DIRECTIONS = {
      {0, -1, 0}, {0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0},
      {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
      {1, 1, 0}, {1, -1, 0}, {-1, 1, 0}, {-1, -1, 0},
      {0, 1, 1}, {0, 1, -1}, {0, -1, 1}, {0, -1, -1}
   };

   private static final int SMOOTH_LOOKAHEAD = 10;

   private AutoMacePathFinder() {
   }

   static Result find(
      Vec3d startCenter,
      int startX,
      int startY,
      int startZ,
      int minX,
      int maxX,
      int minY,
      int maxY,
      int minZ,
      int maxZ,
      Vec3d heuristicTarget,
      CellTest cellTest,
      GoalTest goalTest,
      SegmentTest segmentTest,
      int maxExpansions
   ) {
      if (maxExpansions <= 0) {
         return null;
      }

      Long2ByteOpenHashMap passable = new Long2ByteOpenHashMap();
      passable.defaultReturnValue((byte)0);

      Long2DoubleOpenHashMap best = new Long2DoubleOpenHashMap();
      best.defaultReturnValue(Double.POSITIVE_INFINITY);

      LongOpenHashSet closed = new LongOpenHashSet();
      PriorityQueue<Node> open = new PriorityQueue<>(
         Comparator.comparingDouble((Node node) -> node.f).thenComparingDouble(node -> node.g)
      );

      long startKey = BlockPos.asLong(startX, startY, startZ);
      best.put(startKey, 0.0);
      open.add(new Node(startX, startY, startZ, startKey, 0.0, heuristic(startX, startY, startZ, heuristicTarget)));

      int expansions = 0;
      while (!open.isEmpty() && expansions < maxExpansions) {
         Node current = open.poll();
         if (!closed.add(current.key)) {
            continue;
         }
         if (current.g > best.get(current.key) + 1.0E-7) {
            continue;
         }

         expansions++;
         Vec3d aim = goalTest.test(current.x, current.y, current.z);
         if (aim != null) {
            List<Vec3d> raw = reconstruct(current);
            return new Result(smooth(raw, startCenter, segmentTest), aim);
         }

         for (int[] direction : DIRECTIONS) {
            int dx = direction[0];
            int dy = direction[1];
            int dz = direction[2];
            int nx = current.x + dx;
            int ny = current.y + dy;
            int nz = current.z + dz;

            if (!inside(nx, ny, nz, minX, maxX, minY, maxY, minZ, maxZ)) {
               continue;
            }

            long neighborKey = BlockPos.asLong(nx, ny, nz);
            if (closed.contains(neighborKey)) {
               continue;
            }

            // Strict corner checks: never squeeze the body diagonally through a corner.
            if (dx != 0 && !clear(passable, current.x + dx, current.y, current.z, cellTest)) {
               continue;
            }
            if (dy != 0 && !clear(passable, current.x, current.y + dy, current.z, cellTest)) {
               continue;
            }
            if (dz != 0 && !clear(passable, current.x, current.y, current.z + dz, cellTest)) {
               continue;
            }
            if (!clear(passable, nx, ny, nz, cellTest)) {
               continue;
            }

            double step = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double tentative = current.g + step;
            if (dy > 0) {
               tentative += 0.75;
            }

            if (tentative >= best.get(neighborKey)) {
               continue;
            }

            best.put(neighborKey, tentative);
            open.add(new Node(
               nx,
               ny,
               nz,
               neighborKey,
               tentative,
               tentative + heuristic(nx, ny, nz, heuristicTarget),
               current
            ));
         }
      }

      return null;
   }

   private static boolean inside(
      int x, int y, int z,
      int minX, int maxX, int minY, int maxY, int minZ, int maxZ
   ) {
      return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
   }

   private static boolean clear(
      Long2ByteOpenHashMap passable,
      int x,
      int y,
      int z,
      CellTest cellTest
   ) {
      long key = BlockPos.asLong(x, y, z);
      byte value = passable.get(key);
      if (value == 0) {
         value = cellTest.test(x, y, z) ? (byte)1 : (byte)2;
         passable.put(key, value);
      }
      return value == 1;
   }

   private static double heuristic(int x, int y, int z, Vec3d target) {
      double dx = x + 0.5 - target.x;
      double dy = y + 0.5 - target.y;
      double dz = z + 0.5 - target.z;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private static List<Vec3d> reconstruct(Node goal) {
      List<Vec3d> raw = new ArrayList<>();
      Node current = goal;
      while (current != null) {
         raw.add(new Vec3d(current.x + 0.5, current.y + 0.5, current.z + 0.5));
         current = current.parent;
      }
      Collections.reverse(raw);
      return raw;
   }

   private static List<Vec3d> smooth(List<Vec3d> raw, Vec3d startCenter, SegmentTest segmentTest) {
      if (raw.size() <= 1) {
         return raw;
      }

      List<Vec3d> waypoints = new ArrayList<>();
      Vec3d anchor = startCenter;
      int index = 0;
      while (index < raw.size() - 1) {
         int last = Math.min(index + SMOOTH_LOOKAHEAD, raw.size() - 1);
         int chosen = index + 1;
         for (int candidate = last; candidate > index; candidate--) {
            if (segmentTest.test(anchor, raw.get(candidate))) {
               chosen = candidate;
               break;
            }
         }

         Vec3d next = raw.get(chosen);
         waypoints.add(next);
         anchor = next;
         index = chosen;
      }

      return waypoints;
   }

   private static final class Node {
      final int x;
      final int y;
      final int z;
      final long key;
      final double g;
      final double f;
      final Node parent;

      Node(int x, int y, int z, long key, double g, double f) {
         this(x, y, z, key, g, f, null);
      }

      Node(int x, int y, int z, long key, double g, double f, Node parent) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.key = key;
         this.g = g;
         this.f = f;
         this.parent = parent;
      }
   }
}
