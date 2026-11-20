/*
 * ─────────────────────────────────────────────
 * Based on Zio's AutoMaceStrikePlanner.
 *
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package pw.yami.client.module.movement;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import pw.yami.client.Mc;

/**
 * Smart dive planner for AutoMace.
 *
 * <p>Instead of a pathfinder it simulates a small set of approach points around
 * the target with simplified vanilla elytra physics and picks the rotation
 * that gives the best (earliest, cleanest) hit.</p>
 */
final class AutoMaceStrikePlanner {
   private static final double[] RADII = {2.5, 4.5, 7.0, 10.0};
   private static final double[] HEIGHTS = {0.5, 2.0, 4.0};
   private static final double[] CENTER_HEIGHTS = {5.0, 9.0};
   private static final double GRAVITY = 0.08;
   private static final int MAX_TICKS = 60;
   private static final int MAX_SIMULATIONS = 900;
   private static final int COLLISION_HORIZON = 10;
   private static final double COLLISION_RADIUS = 12.0;

   private final Long2ObjectOpenHashMap<List<Box>> collisionCache = new Long2ObjectOpenHashMap<>();
   private final List<Vec3d> path = new ArrayList<>();
   private final List<Vec3d> route = new ArrayList<>();
   private static final int ROUTE_MODE = 2;

   private Vec3d routeAimPoint;
   private Vec3d selectedAimPoint;
   private Vec3d visibleAimPoint;
   private Vec3d simulationAimPoint;
   private Vec3d simulationAimOrigin;
   private Vec3d failedRouteGoal;
   private Vec3d failedRouteOrigin;
   private int simulationAimTick;
   private int routeCooldown;
   private Box pathBody;
   private double ceilingY = Double.POSITIVE_INFINITY;
   private boolean simulatingRoute;
   private boolean routeHorizontal;

   private ShapeContext collisionContext;
   private Box playerBody;
   private Box targetBox;
   private Vec3d playerPosition;
   private Vec3d initialVelocity;
   private Vec3d position;
   private Vec3d velocity;
   private Vec3d look;

   private double playerX;
   private double playerZ;
   private double targetX;
   private double targetY;
   private double targetZ;
   private double targetMotionX;
   private double targetMotionZ;
   private double attackRange;
   private double eyeHeight;
   private double initialFallDistance;

   private float initialYaw;
   private float initialPitch;
   private float yaw;
   private float pitch;
   private float simulationYaw;
   private float simulationPitch;

   private boolean active;
   private boolean simulationDown;
   private boolean selectedDown;
   private int simulationStage;
   private int selectedHitTicks = Integer.MAX_VALUE;
   private float[] selectedRotation;

   private Vec3d selectedOffset;
   private int selectedMode;
   private int refreshTicks;
   private int searchCursor;
   private boolean solution;
   private int simulationCount;
   private double score;
   private boolean limitReached;
   private boolean pathBlocked;
   private int simulationHitTicks;
   private double simulationFallDistance;

   void reset() {
      active = false;
      solution = false;
      selectedOffset = null;
      selectedMode = 0;
      selectedRotation = null;
      selectedHitTicks = Integer.MAX_VALUE;
      selectedDown = false;
      refreshTicks = 0;
      searchCursor = 0;
      path.clear();
      route.clear();
      routeAimPoint = null;
      selectedAimPoint = null;
      visibleAimPoint = null;
      simulationAimPoint = null;
      simulationAimOrigin = null;
      simulationAimTick = 0;
      failedRouteGoal = null;
      failedRouteOrigin = null;
      routeCooldown = 0;
      simulatingRoute = false;
      routeHorizontal = false;
      ceilingY = Double.POSITIVE_INFINITY;
      collisionCache.clear();
   }

   boolean isActive() {
      return active && solution && selectedRotation != null && selectedHitTicks != Integer.MAX_VALUE;
   }

   float[] rotation() {
      return isActive() ? selectedRotation : null;
   }

   int hitTicks() {
      return isActive() ? selectedHitTicks : Integer.MAX_VALUE;
   }

   boolean shouldDescend() {
      return isActive() && selectedDown;
   }

   Vec3d aimPoint() {
      return selectedAimPoint != null ? selectedAimPoint : visibleAimPoint;
   }

   Vec3d visibleAim(Entity target, double range) {
      ClientPlayerEntity player = Mc.client == null ? null : Mc.client.player;
      if (player == null || target == null) {
         return null;
      }
      if (collisionContext == null) {
         collisionContext = ShapeContext.of(player);
      }
      return findVisibleAim(player.getEyePos(), target.getBoundingBox(), range);
   }

   Vec3d horizontalAim(Entity target, double range) {
      ClientPlayerEntity player = Mc.client == null ? null : Mc.client.player;
      if (player == null || target == null) {
         return null;
      }
      if (collisionContext == null) {
         collisionContext = ShapeContext.of(player);
      }
      Vec3d eye = player.getEyePos();
      Box box = target.getBoundingBox();
      Vec3d center = box.getCenter();
      double aimY = MathHelper.clamp(eye.y, box.minY + 0.1, box.maxY - 0.1);
      Vec3d aim = new Vec3d(center.x, aimY, center.z);
      if (eye.squaredDistanceTo(aim) <= range * range && lineClear(eye, aim)) {
         return aim;
      }
      return findVisibleAim(eye, box, range);
   }

   boolean canHitNow(Entity target, double range) {
      return visibleAim(target, range) != null;
   }

   void updateNormal(Entity target, double range) {
      update(target, range);
   }

   private void update(Entity target, double range) {
      ClientPlayerEntity player = Mc.client == null ? null : Mc.client.player;
      if (player == null || Mc.client.world == null || target == null) {
         reset();
         return;
      }

      active = true;
      collisionCache.clear();
      collisionContext = ShapeContext.of(player);
      EntityDimensions dimensions = player.getDimensions(EntityPose.FALL_FLYING);
      playerBody = dimensions.getBoxAt(Vec3d.ZERO).expand(0.02);
      pathBody = playerBody.expand(0.02);
      eyeHeight = dimensions.eyeHeight();

      targetBox = target.getBoundingBox();
      targetX = target.getX();
      targetY = target.getY();
      targetZ = target.getZ();
      targetMotionX = targetX - target.prevX;
      targetMotionZ = targetZ - target.prevZ;
      double targetSpeed = Math.sqrt(targetMotionX * targetMotionX + targetMotionZ * targetMotionZ);
      if (targetSpeed > 1.0) {
         targetMotionX /= targetSpeed;
         targetMotionZ /= targetSpeed;
      }

      playerPosition = player.getPos();
      playerX = playerPosition.x;
      playerZ = playerPosition.z;
      initialVelocity = new Vec3d(
         player.getX() - player.prevX,
         player.getY() - player.prevY,
         player.getZ() - player.prevZ
      );
      initialFallDistance = player.fallDistance;
      initialYaw = player.lastYaw;
      initialPitch = player.lastPitch;
      attackRange = range;
      simulationCount = 0;
      path.clear();
      ceilingY = computeCeiling();
      visibleAimPoint = findVisibleAim(playerPosition.add(0.0, eyeHeight, 0.0), targetBox, attackRange);

      boolean previousWorks = solution && simulate(selectedOffset, selectedMode, path);
      boolean good = previousWorks && acceptable();
      boolean finalWorks = good;

      if (!good && (selectedOffset != null || !solution)) {
         finalWorks = false;
         if (simulate(null, 1, null) && acceptable()) {
            selectedOffset = null;
            selectedMode = 1;
            good = true;
            previousWorks = true;
            solution = true;
            refreshTicks = 0;
            searchCursor = 0;
         }
      }

      boolean search = false;
      if (previousWorks) {
         if (!good && ++refreshTicks >= 10) {
            search = true;
         }
      } else if (!solution && searchCursor == 0) {
         if (++refreshTicks >= 5) {
            search = true;
         }
      } else {
         search = true;
      }

      if (search) {
         refreshSearch(previousWorks);
         finalWorks = false;
      }

      if (!finalWorks) {
         path.clear();
         finalWorks = solution && simulate(selectedOffset, selectedMode, path) && acceptable();
      }

      if (!finalWorks
         && this.ceilingY != Double.POSITIVE_INFINITY
         && MathHelper.floor(playerPosition.y + 0.3) >= MathHelper.floor(targetBox.minY)) {
         finalWorks = tryRoute();
      }

      if (finalWorks) {
         selectedMode = simulationStage;
         selectedHitTicks = simulationHitTicks;
         selectedRotation = new float[]{simulationYaw, simulationPitch};
         selectedDown = simulationDown;
         selectedAimPoint = simulationAimPoint;
         if (selectedMode != ROUTE_MODE) {
            route.clear();
            routeAimPoint = null;
         }
      } else {
         solution = false;
         selectedRotation = null;
         selectedHitTicks = Integer.MAX_VALUE;
         selectedAimPoint = null;
         path.clear();
      }
   }

   private List<Vec3d> candidateOffsets() {
      List<Vec3d> candidates = new ArrayList<>(194);
      for (double height : CENTER_HEIGHTS) {
         if (targetY + height + 0.6 <= ceilingY) {
            candidates.add(new Vec3d(0.0, height, 0.0));
         }
      }

      double bearing = MathHelper.atan2(playerZ - targetZ, playerX - targetX);
      for (int index = 0; index < 16; index++) {
         int offset = (index + 1) / 2 * (index % 2 == 1 ? 1 : -1);
         double angle = bearing + offset * Math.PI * 2.0 / 16.0;
         for (double radius : RADII) {
            for (double height : HEIGHTS) {
               candidates.add(new Vec3d(Math.cos(angle) * radius, height, Math.sin(angle) * radius));
            }
         }
      }

      return candidates;
   }

   private List<Box> collisionBoxes(int x, int y, int z) {
      long key = BlockPos.asLong(x, y, z);
      List<Box> cached = collisionCache.get(key);
      if (cached != null) {
         return cached;
      }

      BlockPos position = new BlockPos(x, y, z);
      VoxelShape shape = Mc.client.world.getBlockState(position)
         .getCollisionShape(Mc.client.world, position, collisionContext);
      if (shape.isEmpty()) {
         collisionCache.put(key, Collections.emptyList());
         return Collections.emptyList();
      }
      List<Box> boxes = new ArrayList<>();
      for (Box box : shape.getBoundingBoxes()) {
         boxes.add(box.offset(x, y, z));
      }
      collisionCache.put(key, boxes);
      return boxes;
   }

   private boolean candidateClear(Vec3d offset) {
      Box bounds = playerBody.offset(targetX + offset.x, targetY + offset.y, targetZ + offset.z);
      for (int x = MathHelper.floor(bounds.minX); x < MathHelper.ceil(bounds.maxX); x++) {
         for (int z = MathHelper.floor(bounds.minZ); z < MathHelper.ceil(bounds.maxZ); z++) {
            for (int y = MathHelper.floor(bounds.minY) - 1; y < MathHelper.ceil(bounds.maxY); y++) {
               for (Box box : collisionBoxes(x, y, z)) {
                  if (box.intersects(bounds)) {
                     return false;
                  }
               }
            }
         }
      }
      return true;
   }

   private boolean pointClear(double x, double y, double z) {
      int bx = MathHelper.floor(x);
      int by = MathHelper.floor(y);
      int bz = MathHelper.floor(z);
      for (int yy = by - 1; yy <= by; yy++) {
         for (Box box : collisionBoxes(bx, yy, bz)) {
            if (box.intersects(x, y, z, x, y, z)) {
               return false;
            }
         }
      }
      return true;
   }

   private boolean lineClear(Vec3d from, Vec3d to) {
      Vec3d delta = to.subtract(from);
      int steps = Math.max(1, MathHelper.ceil(delta.length() / 0.25));
      for (int i = 0; i < steps; i++) {
         Vec3d point = from.add(delta.multiply((double)i / steps));
         if (!pointClear(point.x, point.y, point.z)) {
            return false;
         }
      }
      return true;
   }

   private boolean bodyClear(Vec3d position) {
      return bodyClear(playerBody, position);
   }

   private boolean bodyClear(Box body, Vec3d position) {
      Box bounds = body.offset(position.x, position.y, position.z);
      for (int x = MathHelper.floor(bounds.minX); x < MathHelper.ceil(bounds.maxX); x++) {
         for (int z = MathHelper.floor(bounds.minZ); z < MathHelper.ceil(bounds.maxZ); z++) {
            for (int y = MathHelper.floor(bounds.minY) - 1; y < MathHelper.ceil(bounds.maxY); y++) {
               for (Box box : collisionBoxes(x, y, z)) {
                  if (box.intersects(bounds)) {
                     return false;
                  }
               }
            }
         }
      }
      return true;
   }

   private boolean movementClear(Vec3d position, Vec3d velocity) {
      int steps = Math.max(1, MathHelper.ceil(velocity.length() / 0.5));
      for (int i = 1; i <= steps; i++) {
         if (!bodyClear(position.add(velocity.multiply((double)i / steps)))) {
            return false;
         }
      }
      return true;
   }

   private boolean inAttackRange(Vec3d position, int tick) {
      Vec3d eye = position.add(0.0, eyeHeight, 0.0);
      Box movingTarget = targetBox.offset(targetMotionX * tick, 0.0, targetMotionZ * tick);
      Vec3d nearest = clampToBounds(eye, movingTarget);
      if (eye.squaredDistanceTo(nearest) > attackRange * attackRange) {
         return false;
      }

      Vec3d center = movingTarget.getCenter();
      if (lineClear(eye, center)) {
         simulationAimPoint = center;
         simulationAimTick = tick;
         return true;
      }

      if (lineClear(eye, nearest)) {
         simulationAimPoint = nearest;
         simulationAimTick = tick;
         return true;
      }

      if (simulationAimPoint != null) {
         Vec3d predicted = simulationAimPoint.add(
            targetMotionX * (tick - simulationAimTick),
            0.0,
            targetMotionZ * (tick - simulationAimTick)
         );
         if (eye.squaredDistanceTo(predicted) <= attackRange * attackRange && lineClear(eye, predicted)) {
            simulationAimPoint = predicted;
            simulationAimTick = tick;
            return true;
         }
      }

      if (simulationAimOrigin == null || simulationAimOrigin.squaredDistanceTo(position) > 4.0) {
         simulationAimOrigin = position;
         Vec3d visible = findVisibleAim(eye, movingTarget, attackRange);
         if (visible != null) {
            simulationAimPoint = visible;
            simulationAimTick = tick;
            return true;
         }
      }

      return false;
   }

   private boolean followOnHit(Vec3d position, Vec3d velocity, int tick) {
      for (int i = 0; i < 6; i++) {
         if (inAttackRange(position, tick + i)) {
            return true;
         }
         if (!movementClear(position, velocity)) {
            return false;
         }
         position = position.add(velocity);
         velocity = new Vec3d(velocity.x * 0.91, (velocity.y - GRAVITY) * 0.98, velocity.z * 0.91);
      }
      return false;
   }

   private void setAim(Vec3d direction, boolean clampPitch) {
      yaw = MathHelper.wrapDegrees((float)(MathHelper.atan2(direction.z, direction.x) * (180.0 / Math.PI)) - 90.0F);
      pitch = (float)(MathHelper.atan2(-direction.y,
         Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * (180.0 / Math.PI));
      if (clampPitch) {
         pitch = Math.max(0.0F, pitch);
      }

      float pitchRadians = pitch * (float)(Math.PI / 180.0);
      float yawRadians = -yaw * (float)(Math.PI / 180.0);
      float cosPitch = (float)Math.cos(pitchRadians);
      look = new Vec3d(
         (float)Math.sin(yawRadians) * cosPitch,
         -(float)Math.sin(pitchRadians),
         (float)Math.cos(yawRadians) * cosPitch
      );
   }

   private Vec3d elytraVelocity(Vec3d current) {
      float pitchRadians = pitch * (float)(Math.PI / 180.0);
      double cos = Math.cos(pitchRadians);
      double cosSquared = cos * cos;
      double lookHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
      double horizontal = Math.sqrt(current.x * current.x + current.z * current.z);
      double x = current.x;
      double y = current.y + GRAVITY * (-1.0 + cosSquared * 0.75);
      double z = current.z;

      if (lookHorizontal > 0.0) {
         if (y < 0.0) {
            double lift = y * -0.1 * cosSquared;
            x += look.x * lift / lookHorizontal;
            y += lift;
            z += look.z * lift / lookHorizontal;
         }
         if (pitchRadians < 0.0) {
            double lift = horizontal * -Math.sin(pitchRadians) * 0.04;
            x -= look.x * lift / lookHorizontal;
            y += lift * 3.2;
            z -= look.z * lift / lookHorizontal;
         }
         x += (look.x / lookHorizontal * horizontal - x) * 0.1;
         z += (look.z / lookHorizontal * horizontal - z) * 0.1;
      }

      return new Vec3d(x * 0.99, y * 0.98, z * 0.99);
   }

   private void aimAt(Vec3d target, int tick) {
      Vec3d delta = target.subtract(position);
      double length = delta.length();
      Vec3d desired = length < 0.0001 ? new Vec3d(0.0, -1.0, 0.0) : delta.multiply(1.0 / length);
      setAim(desired, true);
   }

   private boolean simulate(Vec3d offset, int requestedStage, List<Vec3d> path) {
      if (requestedStage == ROUTE_MODE) {
         return simulateRoute(path);
      }

      simulatingRoute = false;
      simulationAimPoint = null;
      simulationAimOrigin = null;
      simulationAimTick = 0;
      position = playerPosition;
      velocity = initialVelocity;
      double fallDistance = initialFallDistance;
      limitReached = false;
      pathBlocked = false;
      int stage = offset == null ? 1 : requestedStage;
      Vec3d waypoint = offset == null
         ? new Vec3d(targetX, targetY, targetZ)
         : new Vec3d(targetX + offset.x, targetY + offset.y, targetZ + offset.z);

      if (inAttackRange(position, 0)) {
         Vec3d initialAim = simulationAimPoint != null
            ? simulationAimPoint
            : targetBox.getCenter();
         aimAt(initialAim, 0);
         simulationStage = 1;
         simulationYaw = yaw;
         simulationPitch = pitch;
         simulationDown = false;
         simulationHitTicks = 0;
         score = fallDistance;
         simulationFallDistance = fallDistance;
         return true;
      }

      for (int tick = 0; tick < MAX_TICKS; tick++) {
         simulationCount++;
         double motionX = targetMotionX * tick;
         double motionZ = targetMotionZ * tick;
         Vec3d movedTarget = new Vec3d(targetX + motionX, targetY, targetZ + motionZ);
         Vec3d movedWaypoint = waypoint.add(motionX, 0.0, motionZ);
         if (stage == 0 && (position.y < waypoint.y || position.squaredDistanceTo(movedWaypoint) <= 9.0)) {
            stage = 1;
         }

         if (stage != 0 && position.squaredDistanceTo(targetX, targetY + 0.5, targetZ) < 100.0) {
            updateSimulationAim(tick);
         }

         Vec3d aimTarget = movedTarget.add(0.0, targetBox.getLengthY() * 0.5, 0.0);
         aimAt(stage == 0 ? movedWaypoint : aimTarget, tick);
         if (tick == 0) {
            simulationStage = stage;
            simulationYaw = yaw;
            simulationPitch = pitch;
            simulationDown = false;
         }

         if (velocity.y > -0.5 && fallDistance > 1.0) {
            fallDistance = 1.0;
            limitReached = true;
         }

         Vec3d previousPosition = position;
         Vec3d previousVelocity = velocity;
         velocity = elytraVelocity(velocity);
         if (tick < COLLISION_HORIZON
            && position.squaredDistanceTo(playerPosition) <= COLLISION_RADIUS * COLLISION_RADIUS
            && !movementClear(position, velocity)) {
            return false;
         }

         position = position.add(velocity);
         if (velocity.y < 0.0) {
            fallDistance -= velocity.y;
         }
         if (path != null) {
            path.add(position);
         }

         if (inAttackRange(position, tick + 1)) {
            simulationHitTicks = tick + 1;
            score = fallDistance - 0.15 * simulationHitTicks;
            simulationFallDistance = fallDistance;
            if (!followOnHit(previousPosition, previousVelocity, tick)) {
               score -= 10.0;
               pathBlocked = true;
            }
            return true;
         }
      }

      simulationHitTicks = Integer.MAX_VALUE;
      score = fallDistance - 100.0;
      simulationFallDistance = 0.0;
      return true;
   }

   private void updateSimulationAim(int tick) {
      if (simulationAimOrigin != null && simulationAimOrigin.squaredDistanceTo(position) <= 4.0) {
         return;
      }

      simulationAimOrigin = position;
      Box movingTarget = targetBox.offset(targetMotionX * tick, 0.0, targetMotionZ * tick);
      simulationAimPoint = findVisibleAim(
         position.add(0.0, eyeHeight, 0.0),
         movingTarget,
         attackRange + 1.5
      );
      simulationAimTick = tick;
   }

   private boolean simulateRoute(List<Vec3d> path) {
      simulatingRoute = true;
      routeHorizontal = playerPosition.y <= targetBox.maxY + 0.5;
      simulationAimPoint = null;
      simulationAimOrigin = null;
      simulationAimTick = 0;
      position = playerPosition;
      velocity = initialVelocity;
      double fallDistance = initialFallDistance;
      limitReached = false;
      pathBlocked = false;

      if (route.isEmpty()) {
         simulationHitTicks = Integer.MAX_VALUE;
         score = -100.0;
         simulationFallDistance = 0.0;
         return false;
      }

      if (inAttackRange(position, 0) && (routeHorizontal || initialFallDistance > 1.5)) {
         Vec3d initialAim = simulationAimPoint != null ? simulationAimPoint : targetBox.getCenter();
         aimAt(initialAim, 0);
         simulationStage = ROUTE_MODE;
         simulationYaw = yaw;
         simulationPitch = pitch;
         simulationDown = false;
         simulationHitTicks = 0;
         score = fallDistance;
         simulationFallDistance = fallDistance;
         return true;
      }

      int index = 0;
      for (int tick = 0; tick < MAX_TICKS; tick++) {
         simulationCount++;
         while (index < route.size() - 1) {
            Vec3d waypoint = route.get(index);
            double distanceSq = position.squaredDistanceTo(waypoint);
            if (distanceSq < 2.25
               || (waypoint.subtract(position).dotProduct(velocity) < 0.0 && distanceSq < 12.25)) {
               index++;
            } else {
               break;
            }
         }

         boolean nearTarget = position.squaredDistanceTo(targetX, targetY + 0.5, targetZ) < 64.0;
         if (nearTarget) {
            updateSimulationAim(tick);
         }

         Vec3d waypoint = route.get(Math.min(index, route.size() - 1));
         Vec3d aimTarget = waypoint;
         if (nearTarget && simulationAimPoint != null) {
            aimTarget = simulationAimPoint.add(
               targetMotionX * (tick - simulationAimTick),
               0.0,
               targetMotionZ * (tick - simulationAimTick)
            );
         }
         aimAt(aimTarget, tick);

         if (tick == 0) {
            simulationStage = ROUTE_MODE;
            simulationYaw = yaw;
            simulationPitch = pitch;
            simulationDown = false;
         }

         if (velocity.y > -0.5 && fallDistance > 1.0 && nearTarget) {
            fallDistance = 1.0;
            limitReached = true;
         }

         Vec3d previousPosition = position;
         Vec3d previousVelocity = velocity;
         velocity = elytraVelocity(velocity);
         if (!movementClear(position, velocity)) {
            return false;
         }

         position = position.add(velocity);
         if (velocity.y < 0.0) {
            fallDistance -= velocity.y;
         }
         if (path != null) {
            path.add(position);
         }

         if (inAttackRange(position, tick + 1)) {
            simulationHitTicks = tick + 1;
            score = fallDistance - 0.15 * simulationHitTicks;
            simulationFallDistance = fallDistance;
            if (!followOnHit(previousPosition, previousVelocity, tick)) {
               score -= 10.0;
               pathBlocked = true;
            }
            return true;
         }
      }

      simulationHitTicks = Integer.MAX_VALUE;
      score = fallDistance - 100.0;
      simulationFallDistance = 0.0;
      return true;
   }

   private boolean tryRoute() {
      if (routeCooldown > 0) {
         routeCooldown--;
         return false;
      }

      boolean fresh = route.isEmpty();
      if (fresh && !buildRoute()) {
         routeCooldown = 4;
         return false;
      }

      path.clear();
      if (!simulateRoute(path) || !acceptable()) {
         if (fresh) {
            failedRouteGoal = route.isEmpty() ? null : route.get(route.size() - 1);
            failedRouteOrigin = playerPosition;
         }
         route.clear();
         routeAimPoint = null;
         routeCooldown = 3;
         return false;
      }

      failedRouteGoal = null;
      failedRouteOrigin = null;
      solution = true;
      selectedOffset = null;
      selectedMode = ROUTE_MODE;
      return true;
   }

   private boolean buildRoute() {
      route.clear();
      routeAimPoint = null;
      if (Mc.client == null || Mc.client.world == null || playerPosition == null || targetBox == null) {
         return false;
      }

      double horizontalDistance = Math.sqrt(
         (targetX - playerPosition.x) * (targetX - playerPosition.x)
            + (targetZ - playerPosition.z) * (targetZ - playerPosition.z)
      );
      if (horizontalDistance > 16.0) {
         return false;
      }

      int startX = MathHelper.floor(playerPosition.x);
      int startY = MathHelper.floor(playerPosition.y + 0.3);
      int startZ = MathHelper.floor(playerPosition.z);
      int targetCellX = MathHelper.floor(targetX);
      int targetCellZ = MathHelper.floor(targetZ);
      int minX = Math.min(startX, targetCellX) - 8;
      int maxX = Math.max(startX, targetCellX) + 8;
      int minZ = Math.min(startZ, targetCellZ) - 8;
      int maxZ = Math.max(startZ, targetCellZ) + 8;
      int minY = Math.max(Mc.client.world.getBottomY() + 1, MathHelper.floor(targetBox.minY));
      int maxY = startY;
      if (minY > maxY) {
         minY = maxY;
      }

      Vec3d startCenter = playerPosition.add(0.0, 0.3, 0.0);
      Vec3d heuristic = new Vec3d(targetX, targetY + 0.5, targetZ);
      AutoMacePathFinder.Result result = AutoMacePathFinder.find(
         startCenter,
         startX,
         startY,
         startZ,
         minX,
         maxX,
         minY,
         maxY,
         minZ,
         maxZ,
         heuristic,
         this::pathCellClear,
         this::pathGoal,
         this::routeSegmentClear,
         800
      );

      if (result == null || result.waypoints.isEmpty()) {
         return false;
      }

      double routeLength = 0.0;
      Vec3d previous = startCenter;
      for (Vec3d waypoint : result.waypoints) {
         routeLength += previous.distanceTo(waypoint);
         previous = waypoint;
      }
      if (routeLength > 20.0) {
         return false;
      }

      Vec3d first = result.waypoints.get(0);
      Vec3d toFirst = new Vec3d(first.x - playerPosition.x, 0.0, first.z - playerPosition.z);
      Vec3d toTarget = new Vec3d(targetX - playerPosition.x, 0.0, targetZ - playerPosition.z);
      if (toFirst.length() > 8.0) {
         return false;
      }
      if (toFirst.lengthSquared() > 1.0E-4
         && toTarget.lengthSquared() > 1.0E-4
         && toFirst.dotProduct(toTarget) < 0.0) {
         return false;
      }

      route.addAll(result.waypoints);
      routeAimPoint = result.aimPoint;
      return true;
   }

   private boolean pathCellClear(int x, int y, int z) {
      if (Mc.client == null || Mc.client.world == null || pathBody == null) {
         return false;
      }
      if (y <= Mc.client.world.getBottomY() || y >= Mc.client.world.getTopY()) {
         return false;
      }
      return bodyClear(pathBody, new Vec3d(x + 0.5, y + 0.2, z + 0.5));
   }

   private Vec3d pathGoal(int x, int y, int z) {
      if (failedRouteGoal != null) {
         if (failedRouteOrigin != null && failedRouteOrigin.squaredDistanceTo(playerPosition) > 9.0) {
            failedRouteGoal = null;
            failedRouteOrigin = null;
         } else if (failedRouteGoal.squaredDistanceTo(x + 0.5, y + 0.5, z + 0.5) < 6.25) {
            return null;
         }
      }

      Vec3d eye = new Vec3d(x + 0.5, y + 0.2 + eyeHeight, z + 0.5);
      Vec3d cellCenter = new Vec3d(x + 0.5, y + 0.5, z + 0.5);
      if (targetBox.expand(0.35).contains(cellCenter)) {
         return null;
      }
      Vec3d center = targetBox.getCenter();
      double limit = attackRange + 0.3;
      if (eye.squaredDistanceTo(center) > limit * limit || !lineClear(eye, center)) {
         return null;
      }
      return center;
   }

   private boolean routeSegmentClear(Vec3d fromCenter, Vec3d toCenter) {
      Vec3d delta = toCenter.subtract(fromCenter);
      int steps = Math.max(1, MathHelper.ceil(delta.length() / 0.35));
      for (int i = 0; i <= steps; i++) {
         Vec3d center = fromCenter.add(delta.multiply((double)i / steps));
         if (!bodyClear(pathBody, center.add(0.0, -0.3, 0.0))) {
            return false;
         }
      }
      return true;
   }

   private Vec3d findVisibleAim(Vec3d eye, Box box, double range) {
      double rangeSq = range * range;
      Vec3d center = box.getCenter();
      if (eye.squaredDistanceTo(center) <= rangeSq && lineClear(eye, center)) {
         return center;
      }

      Vec3d nearest = clampToBounds(eye, box);
      if (eye.squaredDistanceTo(nearest) <= rangeSq && lineClear(eye, nearest)) {
         return nearest;
      }

      double minX = box.minX;
      double maxX = box.maxX;
      double minY = box.minY;
      double maxY = box.maxY;
      double minZ = box.minZ;
      double maxZ = box.maxZ;
      double centerX = (minX + maxX) * 0.5;
      double centerZ = (minZ + maxZ) * 0.5;
      double width = maxX - minX;
      double height = maxY - minY;
      double depth = maxZ - minZ;

      List<Vec3d> points = new ArrayList<>(24);
      for (double heightFactor : new double[]{0.5, 0.25, 0.75, 0.1}) {
         double y = minY + height * heightFactor;
         points.add(new Vec3d(centerX, y, centerZ));
         for (double sideFactor : new double[]{0.2, 0.8}) {
            points.add(new Vec3d(minX + width * sideFactor, y, minZ));
            points.add(new Vec3d(minX + width * sideFactor, y, maxZ));
            points.add(new Vec3d(minX, y, minZ + depth * sideFactor));
            points.add(new Vec3d(maxX, y, minZ + depth * sideFactor));
         }
      }

      points.sort(Comparator.comparingDouble(point -> eye.squaredDistanceTo(point)));
      for (Vec3d point : points) {
         if (eye.squaredDistanceTo(point) > rangeSq) {
            continue;
         }
         if (lineClear(eye, point)) {
            return point;
         }
      }

      return null;
   }

   private double computeCeiling() {
      if (Mc.client == null || Mc.client.world == null || targetBox == null) {
         return Double.POSITIVE_INFINITY;
      }

      int minX = MathHelper.floor(targetBox.minX);
      int maxX = MathHelper.floor(targetBox.maxX);
      int minZ = MathHelper.floor(targetBox.minZ);
      int maxZ = MathHelper.floor(targetBox.maxZ);
      int startY = MathHelper.floor(targetBox.maxY);
      int endY = startY + 16;
      double ceiling = Double.POSITIVE_INFINITY;
      for (int y = startY; y <= endY; y++) {
         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               if (!collisionBoxes(x, y, z).isEmpty()) {
                  ceiling = Math.min(ceiling, y);
               }
            }
         }
      }
      return ceiling;
   }

   private boolean acceptable() {
      if (simulatingRoute && routeHorizontal) {
         return !pathBlocked && simulationHitTicks != Integer.MAX_VALUE;
      }
      return !limitReached && !pathBlocked && simulationHitTicks != Integer.MAX_VALUE
         && simulationFallDistance > 1.5;
   }

   private void refreshSearch(boolean previousStillWorks) {
      refreshTicks = 0;
      List<Vec3d> candidates = candidateOffsets();
      boolean found = false;
      Vec3d bestOffset = null;
      int bestMode = 0;
      double bestScore = Double.NEGATIVE_INFINITY;

      if (previousStillWorks && simulate(selectedOffset, selectedMode, null) && acceptable()) {
         found = true;
         bestOffset = selectedOffset;
         bestMode = selectedMode;
         bestScore = score + 1.0;
      }

      if (simulate(null, 1, null) && acceptable() && (!found || score > bestScore)) {
         found = true;
         bestOffset = null;
         bestMode = 1;
         bestScore = score;
      }

      int start = searchCursor;
      searchCursor = 0;
      for (int i = 0; i < candidates.size(); i++) {
         if (simulationCount >= MAX_SIMULATIONS) {
            searchCursor = (start + i) % candidates.size();
            break;
         }

         Vec3d offset = candidates.get((start + i) % candidates.size());
         Vec3d lineStart = new Vec3d(targetX + offset.x, targetY + offset.y + eyeHeight, targetZ + offset.z);
         if (candidateClear(offset)
            && lineClear(lineStart, targetBox.getCenter())
            && simulate(offset, 0, null)
            && acceptable()
            && (!found || score > bestScore)) {
            found = true;
            bestOffset = offset;
            bestMode = 0;
            bestScore = score;
            if (acceptable()) {
               break;
            }
         }
      }

      solution = found;
      if (found && (bestOffset != selectedOffset || bestMode != selectedMode)) {
         selectedOffset = bestOffset;
         selectedMode = bestMode;
      }
   }

   private static Vec3d clampToBounds(Vec3d point, Box bounds) {
      return new Vec3d(
         MathHelper.clamp(point.x, bounds.minX, bounds.maxX),
         MathHelper.clamp(point.y, bounds.minY, bounds.maxY),
         MathHelper.clamp(point.z, bounds.minZ, bounds.maxZ)
      );
   }
}
