package pw.yami.client.module.movement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import pw.yami.client.Bootstrap;
import pw.yami.client.event.TickEntitiesPreEvent;
import pw.yami.client.event.Tick_PreEvent;
import pw.yami.client.event.network.SendImmediatelyEvent;
import pw.yami.client.handler.EntityHandler;
import pw.yami.client.manager.Managers;
import pw.yami.client.model.combat.TargetPriority;
import pw.yami.client.model.movement.FollowerNormalMaceMode;
import pw.yami.client.model.world.EntityInteractType;
import pw.yami.client.module.HitResultWorldHelper;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.module.Multitask;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.Stopwatch;
import pw.yami.client.utils.StopwatchImpl;
import pw.yami.client.utils.combat.TargetUtils;
import pw.yami.client.utils.movement.UnidentifiedUtils;
import pw.yami.client.utils.player.RotationUtils;
import pw.yami.client.utils.render.WorldUtils;
import pw.yami.eventbus.annotation.EventListener;

public class Follower extends Multitask {
   private static Follower INSTANCE;
   private final List list = new ArrayList();
   private final Stopwatch stopwatch = new StopwatchImpl();
   Setting mode = this.registerSetting(new EnumSetting("Mode", "mode", FollowerNormalMaceMode.Normal, FollowerNormalMaceMode.values()));
   Setting smart = this.registerSetting(new BooleanSetting("Smart", "Plan the best dive path and time the mace hit", false));
   Setting sortPriority = this.registerSetting(new EnumSetting("SortPriority", "How to filter targets within range", TargetPriority.ClosestAngle, TargetPriority.values()));
   Setting numberSetting1 = this.registerSetting(new NumberSetting("Range", "The maximum range the entity can be to attack it", 0.0, 40.0, 300.0));
   Setting booleanSetting1_1 = this.registerSetting(new BooleanSetting("Friend", "Follow your friend", false));
   Setting booleanSetting2_1 = this.registerSetting(new BooleanSetting("OnlyFlying", "Only working when you fly", false));
   Setting fallDistance = this.registerSetting(new NumberSetting("FallDistance", "Rise height (Mace) / dive height (Smart, max 15)", 0.0, 40.0, 300.0, this::isFallDistanceVisible));
   Setting attackDelay = this.registerSetting(new NumberSetting("AttackDelay", "Add delays between attacks", 0.0, 1000.0, 3000.0, this::isAttackDelayVisible));
   Setting predictTicks = this.registerSetting(new NumberSetting("PredictTicks", "Lead target ticks ahead", 0.0, 2.0, 10.0));
   Setting autoSprint = this.registerSetting(new BooleanSetting("AutoSprint", "Sprint while following", true));
   Setting ignoreNakeds = this.registerSetting(new BooleanSetting("IgnoreNakeds", "Ignore players with no items", false));
   Setting autoAuraMace = this.registerSetting(new BooleanSetting("AutoAuraMace", "Switch Aura mode to Mace while following and restore on disable", true));
   Setting autoFakeFly = this.registerSetting(new BooleanSetting("AutoFakeFly", "Automatically fake fly while following and exit on disable", true));

   private pw.yami.client.model.combat.AuraSwordAxeMode savedAuraWeapon = null;
   private boolean enabledFakeFlyByFollower = false;
   private final AutoMaceStrikePlanner smartPlanner = new AutoMaceStrikePlanner();
   private boolean smartStriking = false;
   private Entity smartTarget = null;
   private int smartLastAttackAge = -100;
   private int smartClimbTicks = 0;
   private int smartDiveTicks = 0;
   private boolean smartRecovering = false;
   private boolean smartHorizontal = false;
   private float smartYaw = Float.NaN;
   private float smartPitch = Float.NaN;

   public Follower() {
      super("AutoMace", "Follow your enemy and attack with Mace", ModuleCategory.MOVEMENT, 4000);
      INSTANCE = this;
   }

   public static Follower dM() {
      return INSTANCE;
   }

   @Override
   public void awb() {
      super.awb();
      if ((Boolean)this.autoAuraMace.atx()) {
         pw.yami.client.module.combat.Aura aura = pw.yami.client.module.combat.Aura.aoS();
         if (aura != null) {
            pw.yami.client.model.combat.AuraSwordAxeMode current = aura.getWeaponMode();
            if (current != null && current != pw.yami.client.model.combat.AuraSwordAxeMode.Mace) {
               this.savedAuraWeapon = current;
            } else {
               this.savedAuraWeapon = pw.yami.client.model.combat.AuraSwordAxeMode.Sword;
            }
            aura.setWeaponMode(pw.yami.client.model.combat.AuraSwordAxeMode.Mace);
         }
      }
      if ((Boolean)this.autoFakeFly.atx()) {
         FakeFly fakeFly = FakeFly.qr();
         if (fakeFly != null && !fakeFly.awg()) {
            fakeFly.awb();
            this.enabledFakeFlyByFollower = true;
         }
      }
   }

   @Override
   public void awc() {
      super.awc();
      if ((Boolean)this.autoAuraMace.atx()) {
         pw.yami.client.module.combat.Aura aura = pw.yami.client.module.combat.Aura.aoS();
         if (aura != null) {
            pw.yami.client.model.combat.AuraSwordAxeMode targetMode = this.savedAuraWeapon != null && this.savedAuraWeapon != pw.yami.client.model.combat.AuraSwordAxeMode.Mace
               ? this.savedAuraWeapon
               : pw.yami.client.model.combat.AuraSwordAxeMode.Sword;
            aura.setWeaponMode(targetMode);
         }
         this.savedAuraWeapon = null;
      }
      if (this.enabledFakeFlyByFollower) {
         FakeFly fakeFly = FakeFly.qr();
         if (fakeFly != null && fakeFly.awg()) {
            fakeFly.awc();
         }
         this.enabledFakeFlyByFollower = false;
      }
      this.resetSmart();
   }

   public Entity getTarget() {
      return !this.list.isEmpty() ? (Entity) this.list.getFirst() : null;
   }

   @EventListener
   public void dN(TickEntitiesPreEvent tickEntitiesPreEvent) {
      this.list.clear();
      if (!(Boolean)this.booleanSetting2_1.atx() || ElytraFly.di().dt()) {
         final Follower follower = this;
         TargetUtils.WY(this.list, new Predicate() {
            @Override
            public boolean test(Object tickEntitiesPreEvent) {
               return follower.dT((Entity)tickEntitiesPreEvent);
            }
         }, (TargetPriority)this.sortPriority.atx(), 1);
         boolean canAttack = HitResultWorldHelper.Sq().SK();
         if (!(Boolean)this.smart.atx() || this.list.isEmpty() || !canAttack) {
            this.resetSmart();
         }
         if (!this.list.isEmpty() && canAttack) {
            if ((Boolean)this.smart.atx() && this.updateSmart((Entity)this.list.getFirst())) {
               return;
            }
            int n;
            if ((n = ((FollowerNormalMaceMode)this.mode.atx()).ordinal()) != 0) {
               if (n == 1) {
                  Iterator iterator2 = this.list.iterator();

                  while (iterator2.hasNext()) {
                     Object obj = iterator2.next();
                     Vec3d temp = ((Entity)obj).getEyePos();
                     double temp2 = (((Entity)obj).getX() - ((Entity)obj).prevX) * (Double)this.predictTicks.atx();
                     double temp3 = (((Entity)obj).getY() - ((Entity)obj).prevY) * (Double)this.predictTicks.atx();
                     double d = (((Entity)obj).getZ() - ((Entity)obj).prevZ) * (Double)this.predictTicks.atx();
                     double d2 = temp3;
                     double d3 = temp2;
                     Vec3d vec = temp.add(d3, d2, d);
                     Vec3d vec2 = vec;
                     if (obj instanceof PlayerEntity && this.dU((PlayerEntity)obj)) {
                        Vec3d vec3 = this.dV((PlayerEntity)obj);
                        vec2 = vec.add(vec3);
                     }

                     Vec3d vec4 = this.dO(vec2);
                     Vec3d targetPos = (vec4 != null) ? vec4 : vec2;
                     boolean attackReady = this.stopwatch.afq((Number)this.attackDelay.atx());
                     if (!attackReady) {
                        double d4 = 0.5 + (Double)this.fallDistance.atx();
                        temp = targetPos.add(0.0, d4, 0.0);
                     } else {
                        temp = targetPos;
                     }

                     float[] fs = RotationUtils.nM(temp);
                     this.ajM(fs[0], fs[1]);

                     if (attackReady && obj instanceof Entity targetEntity) {
                        double dist = client.player.getEyePos().distanceTo(targetEntity.getBoundingBox().getCenter());
                        if (dist <= 4.8) {
                           performMaceAttack(targetEntity);
                           this.stopwatch.afr(29432, 5265492, (byte)40);
                        }
                     }
                     return;
                  }

                  iterator2 = this.list.iterator();

                  while (iterator2.hasNext()) {
                     Object obj2 = iterator2.next();
                     Vec3d vec5 = ((Entity)obj2).getEyePos();
                     double d5 = (((Entity)obj2).getX() - ((Entity)obj2).prevX) * (Double)this.predictTicks.atx();
                     double d6 = (((Entity)obj2).getY() - ((Entity)obj2).prevY) * (Double)this.predictTicks.atx();
                     double d7 = (((Entity)obj2).getZ() - ((Entity)obj2).prevZ) * (Double)this.predictTicks.atx();
                     double d8 = d6;
                     double d9 = d5;
                     Vec3d vec6 = vec5.add(d9, d8, d7);
                     Vec3d vec7 = vec6;
                     if (obj2 instanceof PlayerEntity && this.dU((PlayerEntity)obj2)) {
                        Vec3d vec8 = this.dV((PlayerEntity)obj2);
                        vec7 = vec6.add(vec8);
                     }

                     double d10 = 0.5 + (Double)this.fallDistance.atx();
                     Vec3d vec9 = vec7.add(0.0, d10, 0.0);
                     if (!this.dP(client.player.getPos(), vec9)) {
                        float[] fs2 = RotationUtils.nM(vec9);
                        this.ajM(fs2[0], fs2[1]);
                        return;
                     }
                  }

                  Vec3d vec10 = client.player.getEyePos().add(0.0, 10.0, 0.0);
                  if (!this.dP(client.player.getPos(), vec10)) {
                     float[] fs3 = RotationUtils.nM(vec10);
                     this.ajM(fs3[0], fs3[1]);
                  }
               }
            } else {
               Object obj3 = this.list.getFirst();
               Vec3d vec11 = ((Entity)obj3).getEyePos();
               double d11 = (((Entity)obj3).getX() - ((Entity)obj3).prevX) * (Double)this.predictTicks.atx();
               double d12 = (((Entity)obj3).getY() - ((Entity)obj3).prevY) * (Double)this.predictTicks.atx();
               double d13 = (((Entity)obj3).getZ() - ((Entity)obj3).prevZ) * (Double)this.predictTicks.atx();
               double d14 = d12;
               double d15 = d11;
               float[] fs4 = RotationUtils.nM(vec11.add(d15, d14, d13));
               this.ajM(fs4[0], fs4[1]);
            }
         }
      }
   }

   @Override
   protected int ajP() {
      return 114514;
   }

   public Vec3d dO(Vec3d vec) {
      Vec3d vec2 = client.player.getPos();
      if (!this.dP(vec, vec2)) {
         return vec;
      }

      for (int n = -1; n <= 1; n++) {
         for (int n2 = -1; n2 <= 1; n2++) {
            double temp = n;
            double d = n2;
            double d2 = temp;
            Vec3d vec3 = vec.add(d2, 0.5, d);
            if (!this.dP(vec3, vec2)) {
               return vec3;
            }
         }
      }

      return null;
   }

   public boolean dP(Vec3d vec, Vec3d vec2) {
      ClientWorld temp = client.world;
      RaycastContext raycastContext = new RaycastContext(vec, vec2, ShapeType.COLLIDER, FluidHandling.NONE, client.player);
      boolean flag;
      if (temp.raycast(raycastContext).getType() == Type.BLOCK) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   @EventListener
   public void onSendImmediately(SendImmediatelyEvent sendImmediatelyEvent) {
      if (client.world != null && client.player != null) {
         Packet packet2 = sendImmediatelyEvent.getPacket();
         if (packet2 instanceof EntityHandler && ((EntityHandler)packet2).Qd() == EntityInteractType.ATTACK) {
            this.stopwatch.afr(29432, 5265492, (byte)40);
         }
      }
   }

   @EventListener(priority = -99)
   public void onTick_Pre(Tick_PreEvent tickPreEvent) {
      this.updateAutoFakeFly();
      if (!Bootstrap.NV() || !UnidentifiedUtils.eP()) {
         if (HitResultWorldHelper.Sq().SK() && !this.list.isEmpty()) {
            client.player.input.movementForward = 1.0F;
            client.player.input.movementSideways = 0.0F;
            if ((Boolean)this.autoSprint.atx()) {
               client.player.setSprinting(true);
            }
         }
      }
   }

   public boolean dS() {
      boolean flag;
      if (this.awg() && !this.list.isEmpty()) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   public void updateAutoFakeFly() {
      FakeFly fakeFly = FakeFly.qr();
      if (fakeFly == null) {
         return;
      }

      if (this.awg()) {
         if ((Boolean)this.autoFakeFly.atx() && !fakeFly.awg()) {
            fakeFly.awb();
         }
      } else if (fakeFly.awg()) {
         fakeFly.awc();
      }
   }

   boolean dT(Entity entity2) {
      ClientPlayerEntity player2 = client.player;
      if (!entity2.equals(player2)) {
         Entity entity3 = client.cameraEntity;
         if (!entity2.equals(entity3)) {
            if (entity2 instanceof PlayerEntity) {
               if (((PlayerEntity)entity2).isDead()) {
                  return false;
               }

               if (!((PlayerEntity)entity2).isAlive()) {
                  return false;
               }

               if (((PlayerEntity)entity2).isCreative()) {
                  return false;
               }

               Vec3d temp = client.player.getEyePos();
               Vec3d vec = ((PlayerEntity)entity2).getPos();
               if (temp.distanceTo(vec) > (Double)this.numberSetting1.atx()) {
                  return false;
               }

               if ((Boolean)this.ignoreNakeds.atx() && WorldUtils.NO((PlayerEntity)entity2)) {
                  return false;
               }

               boolean flag;
               if (!(Boolean)this.booleanSetting1_1.atx() && Managers.friendManager.awr((PlayerEntity)entity2)) {
                  flag = false;
               } else {
                  flag = true;
               }

               return flag;
            }

            return false;
         }
      }

      return false;
   }

   private boolean dU(PlayerEntity player2) {
      ItemStack temp = player2.getMainHandStack();
      Item item2 = Items.SHIELD;
      if (!temp.isOf(item2)) {
         temp = player2.getOffHandStack();
         Item item3 = Items.SHIELD;
         if (!temp.isOf(item3)) {
            return false;
         }
      }

      return true;
   }

   private Vec3d dV(PlayerEntity player2) {
      Vec3d vec = RotationUtils.nS(0.0F, player2.getYaw());
      Vec3d vec2 = t(-vec.x, -vec.z);
      return vec2.lengthSquared() == 0.0 ? Vec3d.ZERO : vec2.normalize().multiply(0.8);
   }

   private Boolean isAttackDelayVisible() {
      boolean flag;
      if (this.mode.atx() == FollowerNormalMaceMode.Mace || (Boolean)this.smart.atx()) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isFallDistanceVisible() {
      boolean flag;
      if (this.mode.atx() == FollowerNormalMaceMode.Mace || (Boolean)this.smart.atx()) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isAutoFakeFlyVisible() {
      return true;
   }

   private boolean updateSmart(Entity target) {
      ClientPlayerEntity player = client.player;
      if (player == null || client.world == null) {
         this.resetSmart();
         return false;
      }

      if (this.smartTarget != target) {
         this.smartPlanner.reset();
         this.smartStriking = false;
         this.smartTarget = target;
         this.smartClimbTicks = 0;
         this.smartDiveTicks = 0;
         this.smartRecovering = false;
         this.smartHorizontal = false;
         this.smartYaw = Float.NaN;
         this.smartPitch = Float.NaN;
      }

      boolean wearingElytra = player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
      boolean flightReady = player.isFallFlying() || wearingElytra && !player.isOnGround();
      if (player.isTouchingWater() || !flightReady) {
         this.smartStriking = false;
         this.smartClimbTicks = 0;
         this.smartDiveTicks = 0;
         this.smartRecovering = false;
         this.smartHorizontal = false;
         this.smartYaw = Float.NaN;
         this.smartPitch = Float.NaN;
         this.smartPlanner.reset();
         return false;
      }

      double heightAboveTarget = player.getY() - target.getBoundingBox().maxY;
      double diveHeight = MathHelper.clamp(((Number)this.fallDistance.atx()).doubleValue(), 5.0, 15.0);
      if (!this.smartStriking) {
         this.smartClimbTicks++;
         boolean climbTimeout = this.smartClimbTicks > 40 && heightAboveTarget > 5.0;
         if (heightAboveTarget > diveHeight || climbTimeout || this.hasOverheadCollision() || this.isEnclosed(target)) {
            this.smartStriking = true;
            this.smartClimbTicks = 0;
            this.smartDiveTicks = 0;
            this.smartPlanner.reset();
         }
      }

      if (this.smartStriking && (player.isOnGround() || player.getY() < target.getY() - target.getHeight())) {
         this.smartStriking = false;
         this.smartClimbTicks = 0;
         this.smartDiveTicks = 0;
         this.smartHorizontal = false;
         this.smartPlanner.reset();
      }

      if (this.smartStriking) {
         this.smartDiveTicks++;
         this.smartPlanner.updateNormal(target, 3.0);
         double distance = player.getEyePos().distanceTo(target.getBoundingBox().getCenter());
         this.smartHorizontal = this.isEnclosed(target);
         if (this.smartHorizontal && this.tryHorizontalAttack(target, distance)) {
            return true;
         }

         float[] rotation = this.smartPlanner.rotation();
         if (rotation == null) {
            Vec3d aim = this.smartPlanner.aimPoint();
            if (aim == null) {
               aim = target.getBoundingBox().getCenter();
            }
            rotation = RotationUtils.nM(aim);
            if (heightAboveTarget > 0.0 && aim.y >= target.getBoundingBox().maxY) {
               rotation[1] = Math.max(0.0F, rotation[1]);
            }
         }

         if (distance > 6.0) {
            Vec3d velocity = player.getVelocity();
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (horizontalSpeed < 0.25) {
               rotation[1] = Math.max(rotation[1], 20.0F);
            }
         }
         boolean precise = this.smartPlanner.hitTicks() <= 3 || distance <= 5.0;
         if (this.smartDiveTicks <= 6) {
            rotation = this.smoothSmartRotation(rotation[0], rotation[1], precise);
         } else {
            this.smartYaw = rotation[0];
            this.smartPitch = rotation[1];
         }
         this.ajM(rotation[0], rotation[1]);

         boolean canHit = this.smartPlanner.hitTicks() <= 0;
         if (!canHit && !this.smartPlanner.isActive()) {
            canHit = player.fallDistance > 1.0 && this.smartPlanner.canHitNow(target, 4.8);
         }
         if (canHit) {
            int cooldownTicks = Math.max(4, (int)(((Number)this.attackDelay.atx()).doubleValue() / 50.0));
            if (distance <= 4.8 && player.age - this.smartLastAttackAge >= cooldownTicks) {
               this.performMaceAttack(target);
               this.smartLastAttackAge = player.age;
               this.stopwatch.afr(29432, 5265492, (byte)40);
               this.smartStriking = false;
               this.smartClimbTicks = 0;
               this.smartDiveTicks = 0;
               this.smartHorizontal = false;
               this.smartPlanner.reset();
            }
         }
         return true;
      }

      double deltaX = target.getX() - player.getX();
      double deltaZ = target.getZ() - player.getZ();
      double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      float yaw = (float)(MathHelper.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F;
      if (horizontalDistance < 12.0) {
         yaw = MathHelper.wrapDegrees(yaw + 180.0F);
      }
      float climbPitch = -75.0F;
      Vec3d velocity = player.getVelocity();
      double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
      if (horizontalSpeed < 0.3 && velocity.y <= 0.02) {
         this.smartRecovering = true;
      } else if (horizontalSpeed > 0.7) {
         this.smartRecovering = false;
      }
      if (this.smartRecovering) {
         climbPitch = 20.0F;
      }
      float[] rotation = this.smoothSmartRotation(yaw, climbPitch, false);
      this.ajM(rotation[0], rotation[1]);
      return true;
   }

   private float[] smoothSmartRotation(float desiredYaw, float desiredPitch, boolean precise) {
      if (Float.isNaN(this.smartYaw) || Float.isNaN(this.smartPitch)) {
         ClientPlayerEntity player = client.player;
         this.smartYaw = player != null ? player.getYaw() : desiredYaw;
         this.smartPitch = player != null ? player.getPitch() : desiredPitch;
      }

      float maxYaw = precise ? 90.0F : 55.0F;
      float maxPitch = precise ? 55.0F : 32.0F;
      float yawDelta = (float)RotationUtils.nQ(desiredYaw, this.smartYaw);
      float pitchDelta = desiredPitch - this.smartPitch;
      this.smartYaw = MathHelper.wrapDegrees(this.smartYaw + MathHelper.clamp(yawDelta, -maxYaw, maxYaw));
      this.smartPitch = MathHelper.clamp(this.smartPitch + MathHelper.clamp(pitchDelta, -maxPitch, maxPitch), -90.0F, 90.0F);
      return new float[]{this.smartYaw, this.smartPitch};
   }

   private void resetSmart() {
      this.smartPlanner.reset();
      this.smartStriking = false;
      this.smartTarget = null;
      this.smartLastAttackAge = -100;
      this.smartClimbTicks = 0;
      this.smartDiveTicks = 0;
      this.smartRecovering = false;
      this.smartHorizontal = false;
      this.smartYaw = Float.NaN;
      this.smartPitch = Float.NaN;
   }

   private boolean hasOverheadCollision() {
      if (client.player == null || client.world == null) {
         return false;
      }

      Box bounds = client.player.getBoundingBox();
      int minX = MathHelper.floor(bounds.minX) - 1;
      int maxX = MathHelper.floor(bounds.maxX) + 1;
      int minZ = MathHelper.floor(bounds.minZ) - 1;
      int maxZ = MathHelper.floor(bounds.maxZ) + 1;
      int y = MathHelper.floor(bounds.maxY) + 1;
      int solidBlocks = 0;
      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            BlockPos position = new BlockPos(x, y, z);
            if (!client.world.getBlockState(position).getCollisionShape(client.world, position).isEmpty()) {
               if (++solidBlocks > 3) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   private boolean tryHorizontalAttack(Entity target, double distance) {
      // Keep the approach horizontal even at range; the actual attack still uses 4.8.
      Vec3d aim = this.smartPlanner.horizontalAim(target, 12.0);
      if (aim == null) {
         return false;
      }

      ClientPlayerEntity player = client.player;
      float[] rotation = RotationUtils.nM(aim);
      if (player.getY() > target.getBoundingBox().maxY) {
         rotation[1] = Math.max(0.0F, rotation[1]);
      }
      this.smartYaw = rotation[0];
      this.smartPitch = rotation[1];
      this.ajM(rotation[0], rotation[1]);

      int cooldownTicks = Math.max(4, (int)(((Number)this.attackDelay.atx()).doubleValue() / 50.0));
      if (distance <= 4.8 && player.age - this.smartLastAttackAge >= cooldownTicks) {
         this.performMaceAttack(target);
         this.smartLastAttackAge = player.age;
         this.stopwatch.afr(29432, 5265492, (byte)40);
         this.smartStriking = false;
         this.smartClimbTicks = 0;
         this.smartDiveTicks = 0;
         this.smartHorizontal = false;
         this.smartPlanner.reset();
      }
      return true;
   }

   private boolean isEnclosed(Entity target) {
      if (target == null || client.world == null) {
         return false;
      }
      return this.hasCeilingAbove(target.getBoundingBox());
   }

   private boolean hasCeilingAbove(Box bounds) {
      if (bounds == null) {
         return false;
      }

      int minX = MathHelper.floor(bounds.minX);
      int maxX = MathHelper.floor(bounds.maxX);
      int minZ = MathHelper.floor(bounds.minZ);
      int maxZ = MathHelper.floor(bounds.maxZ);
      int startY = MathHelper.floor(bounds.maxY) + 1;
      int endY = startY + 6;
      for (int y = startY; y <= endY; y++) {
         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               BlockPos position = new BlockPos(x, y, z);
               if (!client.world.getBlockState(position).getCollisionShape(client.world, position).isEmpty()) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   private void performMaceAttack(Entity targetEntity) {
      if (client.player == null || client.interactionManager == null) return;
      int maceSlot = -1;
      for (int i = 0; i < 9; i++) {
         ItemStack stack = client.player.getInventory().getStack(i);
         if (stack != null && stack.getItem() instanceof net.minecraft.item.MaceItem) {
            maceSlot = i;
            break;
         }
      }
      if (maceSlot != -1 && client.player.getInventory().selectedSlot != maceSlot) {
         client.player.getInventory().selectedSlot = maceSlot;
         if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(maceSlot));
         }
      }
      client.interactionManager.attackEntity(client.player, targetEntity);
      client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
   }

   private static Vec3d t(double d, double d2) {
      return new Vec3d(d, 0.0, d2);
   }
}
