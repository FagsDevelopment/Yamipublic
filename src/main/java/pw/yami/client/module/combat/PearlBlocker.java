package pw.yami.client.module.combat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import pw.yami.client.event.TickEntitiesPreEvent;
import pw.yami.client.manager.Managers;
import pw.yami.client.model.player.SwapMode;
import pw.yami.client.model.world.PosNeedPlaceSide;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.module.Multitask;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.Stopwatch;
import pw.yami.client.utils.StopwatchImpl;
import pw.yami.client.utils.world.BlockPosBlockItemHelper;
import pw.yami.client.utils.world.CollisionUtils;
import pw.yami.client.utils.world.PlacementUtils;
import pw.yami.client.world.ProjectileEntityVec3dProjectileEntity;
import pw.yami.eventbus.annotation.EventListener;

public class PearlBlocker extends Multitask {
   Setting numberSetting1 = this.registerSetting(new NumberSetting("ExtrapolationTicks", "Accounts for motion when calculating ender pearl positions", 0, 20, 500));
   Setting numberSetting2 = this.registerSetting(new NumberSetting("MinExtrapolationTicks", "Min motion predict ticks", 0, 4, 20));
   Setting numberSetting3 = this.registerSetting(new NumberSetting("Delay", "Delay between placements (ms)", 0, 60, 1000));
   Setting bpt = this.registerSetting(new NumberSetting("BPT", "in places, number of blocks", 1, 1, 8));
   Setting swapAction = this.registerSetting(new EnumSetting("SwapAction", "Decide which swap mode to use", SwapMode.Hotbar, SwapMode.values()));
   Setting rotate = this.registerSetting(new BooleanSetting("Rotate", "Rotates to block before placing", false));
   Setting pearlRange = this.registerSetting(new NumberSetting("PearlRange", "Valid ender pearl distance", 0.0, 4.5, 6.0));
   Setting ignoreRange = this.registerSetting(new NumberSetting("IgnoreRange", "Ignore ender pearl distance", 0.0, 0.0, 6.0));
   Setting placeRange = this.registerSetting(new NumberSetting("PlaceRange", "The distance of your block reach", 0.0, 4.5, 6.0));
   Setting breakCrystal = this.registerSetting(new BooleanSetting("BreakCrystal", "Break crystals when placing blocks", true));
   Setting breakRange = this.registerSetting(new NumberSetting("BreakRange", "The distance of your entity reach", 0.0, 3.0, 6.0));
   Setting airPlace = this.registerSetting(new BooleanSetting("AirPlace", "Replace block exploit allow you place block at air", false));
   Setting support = this.registerSetting(new NumberSetting("Support", "The maximum number of blocks for helper can place", 0, 2, 8));
   Setting multi = this.registerSetting(new BooleanSetting("Multi", "Allow multiple assignblocks", true, this::isMultiVisible));
   Setting ignoreFriends = this.registerSetting(new BooleanSetting("IgnoreFriends", "Ignore friend's ender pearl", true));

   private final Map<Entity, BlockPos> map = new HashMap<>();
   private final Stopwatch stopwatch = new StopwatchImpl();
   public static boolean cd;

   public PearlBlocker() {
      super("PearlBlocker", "Block enemy's ender pearl", ModuleCategory.COMBAT);
   }

   @Override
   public void awe() {
      this.map.clear();
   }

   @EventListener
   public void avs(TickEntitiesPreEvent tickEntitiesPreEvent) {
      if (client.player == null || client.world == null) {
         return;
      }
      if (!((Boolean)this.breakSetting.atx() || !this.RG())) {
         return;
      }
      if (!this.stopwatch.afq((Number)this.numberSetting3.atx())) {
         return;
      }

      this.map.entrySet().removeIf(PearlBlocker::avt);

      int placedCount = 0;
      int maxBpt = ((Number)this.bpt.atx()).intValue();
      Vec3d eyePos = client.player.getEyePos();

      double pRange = ((Number)this.pearlRange.atx()).doubleValue();
      double ignRange = ((Number)this.ignoreRange.atx()).doubleValue();
      double plcRange = ((Number)this.placeRange.atx()).doubleValue();
      double brkRange = ((Number)this.breakRange.atx()).doubleValue();
      boolean brkCrystal = (Boolean)this.breakCrystal.atx();
      boolean rot = (Boolean)this.rotate.atx();
      boolean air = (Boolean)this.airPlace.atx();
      SwapMode swap = (SwapMode)this.swapAction.atx();
      int supp = ((Number)this.support.atx()).intValue();
      boolean multiPlace = (Boolean)this.multi.atx();
      boolean ignFriends = (Boolean)this.ignoreFriends.atx();
      int minTicks = ((Number)this.numberSetting2.atx()).intValue();
      int maxTicks = ((Number)this.numberSetting1.atx()).intValue();

      for (Entity entity : client.world.getEntities()) {
         if (placedCount >= maxBpt) {
            break;
         }
         if (!(entity instanceof EnderPearlEntity pearl)) {
            continue;
         }

         Entity owner = pearl.getOwner();
         if (!(owner instanceof PlayerEntity) || owner == client.player || (ignFriends && Managers.friendManager.awr((PlayerEntity)owner))) {
            continue;
         }

         ProjectileEntityVec3dProjectileEntity sim = new ProjectileEntityVec3dProjectileEntity(pearl);
         BlockPos existingBlock = this.map.get(pearl);
         if (existingBlock != null) {
            boolean alreadyBlocked = false;
            for (int t = minTicks; t < maxTicks; t++) {
               HitResult hit = sim.ajv();
               if (hit != null) {
                  if (existingBlock.toCenterPos().distanceTo(hit.getPos()) < 1.0) {
                     alreadyBlocked = true;
                  }
                  break;
               }
               BlockPos predictedPos = BlockPos.ofFloored(sim.prevX, sim.prevY, sim.prevZ);
               if (existingBlock.equals(predictedPos)) {
                  alreadyBlocked = true;
                  break;
               }
            }
            if (alreadyBlocked) {
               continue;
            }
            sim = new ProjectileEntityVec3dProjectileEntity(pearl);
         }

         BlockPos bestPos = null;
         BlockPosBlockItemHelper bestHelper = null;
         List<PosNeedPlaceSide> bestSupport = null;

         for (int t = minTicks; t < maxTicks; t++) {
            HitResult hit = sim.ajv();
            if (hit != null) {
               if (hit.getPos().distanceTo(eyePos) <= ignRange) {
                  bestPos = null;
               }
               break;
            }

            Vec3d pearlPos = new Vec3d(sim.prevX, sim.prevY, sim.prevZ);
            if (pearlPos.distanceTo(eyePos) > pRange) {
               continue;
            }

            BlockPos candidate = BlockPos.ofFloored(pearlPos.x, pearlPos.y, pearlPos.z);
            if (bestPos != null) {
               double distNew = eyePos.distanceTo(candidate.toCenterPos());
               double distOld = eyePos.distanceTo(bestPos.toCenterPos());
               if (distNew >= distOld) {
                  continue;
               }
            }

            BlockPosBlockItemHelper helper = new BlockPosBlockItemHelper(candidate, Items.OBSIDIAN, air, swap);
            if (helper.avK(plcRange, brkCrystal)) {
               bestPos = candidate;
               bestHelper = helper;
               bestSupport = null;
            } else if (bestPos == null && helper.m && helper.n && (helper.h || helper.o) && helper.stackSlotHelper.LL() && supp > 0) {
               List<PosNeedPlaceSide> supportList = PlacementUtils.Fs(candidate, plcRange, supp);
               if (supportList != null && !supportList.isEmpty() && CollisionUtils.aiD(supportList.getLast().getPos())) {
                  bestPos = candidate;
                  bestHelper = helper;
                  bestSupport = supportList;
               }
            }
         }

         if (bestPos != null && bestHelper != null) {
            this.map.put(pearl, bestPos);
            this.stopwatch.afr(29432, 5265492, (byte)40);

            if (bestSupport == null) {
               bestHelper.avN(rot, brkCrystal, brkRange);
               placedCount++;
            } else {
               if (!multiPlace) {
                  PosNeedPlaceSide first = bestSupport.getFirst();
                  new BlockPosBlockItemHelper(first.getPos(), Items.OBSIDIAN, air, swap)
                     .avM(plcRange, rot, brkCrystal, brkRange);
                  if (++placedCount >= maxBpt) {
                     return;
                  }
               } else {
                  for (PosNeedPlaceSide side : bestSupport) {
                     new BlockPosBlockItemHelper(side.getPos(), Items.OBSIDIAN, air, swap)
                        .avM(plcRange, rot, brkCrystal, brkRange);
                     if (++placedCount >= maxBpt) {
                        return;
                     }
                  }
               }

               bestHelper.avM(plcRange, rot, brkCrystal, brkRange);
               if (++placedCount >= maxBpt) {
                  return;
               }
            }
         }
      }
   }

   static boolean avt(Entry<?, ?> entry) {
      Object key = entry.getKey();
      return !(key instanceof Entity entity) || !entity.isAlive();
   }

   private Boolean isMultiVisible() {
      return ((Number)this.support.atx()).intValue() > 0;
   }
}
