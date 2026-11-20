package pw.yami.client.module.movement;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import pw.yami.client.event.EventPhase;
import pw.yami.client.event.TickEvent5;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.event.movement.MoveEvent;
import pw.yami.client.manager.Managers;
import pw.yami.client.manager.PlayerStateManager;
import pw.yami.client.model.movement.FastFallStepShiftMode;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.module.exploit.PacketFly;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.Stopwatch;
import pw.yami.client.utils.StopwatchImpl;
import pw.yami.eventbus.annotation.EventListener;

public class FastFall extends ClientModule {
   private final Stopwatch stopwatch = new StopwatchImpl();
   Setting height = this.registerSetting(new NumberSetting("Height", "The maximum fall height", 1.0F, 3.0F, 10.0F));
   Setting mode = this.registerSetting(new EnumSetting("Mode", "The mode for falling down blocks", FastFallStepShiftMode.STEP, FastFallStepShiftMode.values()));
   Setting accelerate = this.registerSetting(new BooleanSetting("Accelerate", "Accels the fall speed", false, this::isAccelerateVisible));
   Setting shiftTicks = this.registerSetting(new NumberSetting("ShiftTicks", "Number of ticks to shift ahead", 1, 3, 5, this::isShiftTicksVisible));
   private boolean f_;
   private boolean g;
   private int h_;

   public FastFall() {
      super("FastFall", "Falls down blocks faster", ModuleCategory.MOVEMENT);
   }

   @Override
   public void awe() {
      this.g = false;
      this.h_ = 0;
   }

   @EventListener
   public void onTickPre(TickPreEvent tickPreEvent) {
      if (tickPreEvent.getEventPhase() == EventPhase.PRE) {
         this.f_ = client.player.isOnGround();
         if (this.mode.atx() == FastFallStepShiftMode.STEP) {
            if (client.player.isRiding()
               || client.player.isFallFlying()
               || client.player.isHoldingOntoLadder()
               || client.player.isInLava()
               || client.player.isTouchingWater()
               || client.player.input.jumping
               || client.player.input.sneaking
               || !Managers.antiCheatManager.Uh(1000L)) {
               return;
            }

            if (Speed.lq().awg() || LongJump.rs().awg() || Flight.adS().awg() || PacketFly.Gj().awg()) {
               return;
            }

            double d = this.apR();
            if (d > 0.01 && d <= ((Float)this.height.atx()).floatValue() && client.player.isOnGround()) {
               Managers.playerStateManager.rK(client.player.getVelocity().x * 0.05, client.player.getVelocity().z * 0.05);
               PlayerStateManager temp = Managers.playerStateManager;
               boolean flag = (Boolean)this.accelerate.atx();
               PlayerStateManager playerStateManager2 = temp;
               double d2;
               if (flag) {
                  temp = playerStateManager2;
                  d2 = client.player.getVelocity().y - 0.62F;
                  playerStateManager2 = temp;
               } else {
                  temp = playerStateManager2;
                  d2 = -3.0;
                  playerStateManager2 = temp;
               }

               playerStateManager2.rJ(d2);
            }
         }
      }
   }

   @EventListener
   public void onTickEvent5(TickEvent5 tickEvent5) {
      if (this.mode.atx() == FastFallStepShiftMode.SHIFT) {
         if (client.player.isRiding()
            || client.player.isFallFlying()
            || client.player.isHoldingOntoLadder()
            || client.player.isInLava()
            || client.player.isTouchingWater()
            || client.player.input.jumping
            || client.player.input.sneaking
            || !Managers.antiCheatManager.Uh(1000L)) {
            return;
         }

         if (!Managers.antiCheatManager.Uh(1000L)
            || !this.stopwatch.afq(1000)
            || Speed.lq().awg()
            || LongJump.rs().awg()
            || Flight.adS().awg()
            || PacketFly.Gj().awg()) {
            return;
         }

         double d = this.apR();
         if (d > 0.01 && d <= ((Float)this.height.atx()).floatValue() + 0.01) {
            if (client.player.isOnGround()) {
               Managers.playerStateManager.rK(client.player.getVelocity().x * 0.05, client.player.getVelocity().z * 0.05);
            }

            if (client.player.getVelocity().y < 0.0 && this.f_ && !client.player.isOnGround()) {
               this.stopwatch.afr(29432, 5265492, (byte)40);
               tickEvent5.aim();
               tickEvent5.arF((Integer)this.shiftTicks.atx());
               this.g = true;
               this.h_ = 0;
            }
         }
      }
   }

   @EventListener
   public void onMove(MoveEvent moveEvent) {
      if (!Flight.adS().awg() && !PacketFly.Gj().awg()) {
         if (this.g && this.mode.atx() == FastFallStepShiftMode.SHIFT) {
            moveEvent.ym(0.0);
            moveEvent.yq(0.0);
            Managers.playerStateManager.rK(0.0, 0.0);
            this.h_++;
            if (this.h_ > (Integer)this.shiftTicks.atx()) {
               this.g = false;
               this.h_ = 0;
            }
         }
      }
   }

   private double apR() {
      Box box = client.player.getBoundingBox();

      for (double d = 0.0; d < ((Float)this.height.atx()).floatValue() + 0.5; d += 0.01) {
         ClientWorld temp = client.world;
         ClientPlayerEntity temp2 = client.player;
         Box temp3 = box;
         double d2 = -d;
         Box box2 = temp3.offset(0.0, d2, 0.0);
         ClientPlayerEntity player2 = temp2;
         if (!temp.isSpaceEmpty((Entity)player2, box2)) {
            return d;
         }
      }

      return -1.0;
   }

   private Boolean isShiftTicksVisible() {
      boolean flag;
      if (this.mode.atx() == FastFallStepShiftMode.SHIFT) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isAccelerateVisible() {
      boolean flag;
      if (this.mode.atx() == FastFallStepShiftMode.STEP) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }
}
