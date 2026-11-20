package pw.yami.client.module.misc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import pw.yami.client.event.DisconnectEvent;
import pw.yami.client.event.EventPhase;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.event.movement.PushAwayFromEvent;
import pw.yami.client.event.network.ChannelRead0Event;
import pw.yami.client.event.network.SendImmediatelyEvent;
import pw.yami.client.handler.EntityHandler;
import pw.yami.client.mixin.PlayerInteractEntityC2SPacketMixin;
import pw.yami.client.model.world.EntityInteractType;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.player.PlayerWorldOtherClientPlayerEntity;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.setting.StringSetting;
import pw.yami.client.utils.world.Vec3dEntityUtils;
import pw.yami.eventbus.annotation.EventListener;

public class FakePlayer extends ClientModule {
   public static class MovementFrame {
      public final double x, y, z;
      public final float yaw, pitch, headYaw;
      public final boolean sneaking, onGround;

      public MovementFrame(double x, double y, double z, float yaw, float pitch, float headYaw, boolean sneaking, boolean onGround) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.yaw = yaw;
         this.pitch = pitch;
         this.headYaw = headYaw;
         this.sneaking = sneaking;
         this.onGround = onGround;
      }
   }

   public final Setting name = this.registerSetting(new StringSetting("Name", "Fake player's name", "FakePlayer"));
   public final Setting damage = this.registerSetting(new BooleanSetting("Damage", "Allow you to damage fake player", true));
   public final Setting assumeBestArmor = this.registerSetting(new BooleanSetting("AssumeBestArmor", "Assumes Prot 4 armor is max armor", false));
   public final Setting recordSetting = this.registerSetting(new BooleanSetting("Record", "Record your movement", false));
   public final Setting play = this.registerSetting(new BooleanSetting("Play", "Play the saved movement", false));

   private final List<MovementFrame> recordedFrames = new ArrayList<>();
   private int playbackIndex = 0;
   private boolean wasRecording = false;
   private PlayerWorldOtherClientPlayerEntity playerWorldOtherClientPlayerEntity;
   private boolean isApplyingDamage = false;

   public static FakePlayer INSTANCE;

   public FakePlayer() {
      super("FakePlayer", "Spawns an attackable client-side test player", ModuleCategory.MISC);
      INSTANCE = this;
   }

   public boolean isDamageEnabled() {
      return (Boolean)this.damage.atx();
   }

   public PlayerWorldOtherClientPlayerEntity getFakePlayer() {
      return this.playerWorldOtherClientPlayerEntity;
   }

   public void spawnFakePlayer(String customName) {
      if (client.player == null || client.world == null) return;
      if (this.playerWorldOtherClientPlayerEntity != null) {
         this.playerWorldOtherClientPlayerEntity.despawn();
         this.playerWorldOtherClientPlayerEntity = null;
      }
      String targetName = (String)this.name.atx();
      if (customName != null && !customName.trim().isEmpty()) {
         targetName = customName;
      }
      if (targetName == null || targetName.trim().isEmpty()) {
         targetName = "FakePlayer";
      }
      this.playerWorldOtherClientPlayerEntity = new PlayerWorldOtherClientPlayerEntity(client.player, targetName, false);
      this.playerWorldOtherClientPlayerEntity.spawn();
      if (!this.awg()) {
         this.awa(); // enable module
      }
   }

   public void removeFakePlayer() {
      if (this.playerWorldOtherClientPlayerEntity != null) {
         this.playerWorldOtherClientPlayerEntity.despawn();
         this.playerWorldOtherClientPlayerEntity = null;
      }
      if (this.awg()) {
         this.awb(); // disable module
      }
   }

   public void killFakePlayer() {
      if (this.playerWorldOtherClientPlayerEntity != null) {
         this.playerWorldOtherClientPlayerEntity.kill();
      }
   }

   public boolean toggleRecord() {
      boolean newState = !(Boolean)this.recordSetting.atx();
      this.recordSetting.aty(newState);
      if (newState) {
         this.recordedFrames.clear();
         this.play.aty(false);
      }
      return newState;
   }

   public boolean togglePlay() {
      boolean newState = !(Boolean)this.play.atx();
      this.play.aty(newState);
      if (newState) {
         this.playbackIndex = 0;
         this.recordSetting.aty(false);
      }
      return newState;
   }

   @EventListener
   public void onTickPre(TickPreEvent tickPreEvent) {
      if (tickPreEvent.getEventPhase() != EventPhase.PRE || client.player == null) return;

      boolean isRecording = (Boolean)this.recordSetting.atx();
      if (isRecording != this.wasRecording && isRecording) {
         this.recordedFrames.clear();
      }
      this.wasRecording = isRecording;

      if (isRecording) {
         this.recordedFrames.add(new MovementFrame(
            client.player.getX(),
            client.player.getY(),
            client.player.getZ(),
            client.player.getYaw(),
            client.player.getPitch(),
            client.player.headYaw,
            client.player.isSneaking(),
            client.player.isOnGround()
         ));
      }

      boolean isPlaying = (Boolean)this.play.atx();
      if (isPlaying && this.playerWorldOtherClientPlayerEntity != null && !this.recordedFrames.isEmpty()) {
         this.playbackIndex = (this.playbackIndex + 1) % this.recordedFrames.size();
         MovementFrame frame = this.recordedFrames.get(this.playbackIndex);

         this.playerWorldOtherClientPlayerEntity.updateTrackedPositionAndAngles(
            frame.x, frame.y, frame.z, frame.yaw, frame.pitch, 3
         );
         this.playerWorldOtherClientPlayerEntity.setYaw(frame.yaw);
         this.playerWorldOtherClientPlayerEntity.setPitch(frame.pitch);
         this.playerWorldOtherClientPlayerEntity.setHeadYaw(frame.headYaw);
         this.playerWorldOtherClientPlayerEntity.setOnGround(frame.onGround);
         if (frame.sneaking) {
            this.playerWorldOtherClientPlayerEntity.setPose(EntityPose.CROUCHING);
         } else {
            this.playerWorldOtherClientPlayerEntity.setPose(EntityPose.STANDING);
         }
      }
   }

   @Override
   public void awd() {
      if (client.player != null && client.world != null) {
         if (this.playerWorldOtherClientPlayerEntity != null) {
            this.playerWorldOtherClientPlayerEntity.despawn();
         }
         String fakeName = (String)this.name.atx();
         if (fakeName == null || fakeName.trim().isEmpty()) {
            fakeName = "FakePlayer";
         }
         this.playerWorldOtherClientPlayerEntity = new PlayerWorldOtherClientPlayerEntity(client.player, fakeName, false);
         this.playerWorldOtherClientPlayerEntity.spawn();
      }
   }

   @Override
   public void awe() {
      if (this.playerWorldOtherClientPlayerEntity != null) {
         this.playerWorldOtherClientPlayerEntity.despawn();
         this.playerWorldOtherClientPlayerEntity = null;
      }
   }

   @EventListener
   public void onDisconnect(DisconnectEvent disconnectEvent) {
      if (this.playerWorldOtherClientPlayerEntity != null) {
         this.playerWorldOtherClientPlayerEntity.despawn();
         this.playerWorldOtherClientPlayerEntity = null;
      }
      this.awc();
   }

   @EventListener
   public void onPushAwayFrom(PushAwayFromEvent pushAwayFromEvent) {
      Entity temp = pushAwayFromEvent.getEntity();
      ClientPlayerEntity player2 = client.player;
      if (temp != null && temp.equals(player2)) {
         temp = pushAwayFromEvent.getEntity2();
         if (temp != null && temp.equals(this.playerWorldOtherClientPlayerEntity)) {
            pushAwayFromEvent.ail(true);
         }
      }
   }

   @EventListener
   public void onSendImmediately(SendImmediatelyEvent event) {
      if (this.playerWorldOtherClientPlayerEntity == null) return;
      Packet<?> packet = event.getPacket();
      if (packet instanceof PlayerInteractEntityC2SPacket) {
         try {
            int targetId = ((PlayerInteractEntityC2SPacketMixin)packet).hookGetEntityId();
            if (targetId == this.playerWorldOtherClientPlayerEntity.getId()) {
               if (packet instanceof EntityHandler handler && handler.Qd() == EntityInteractType.ATTACK) {
                  MinecraftClient.getInstance().execute(() -> {
                     if (this.playerWorldOtherClientPlayerEntity != null && this.playerWorldOtherClientPlayerEntity.isAlive()) {
                        this.playerWorldOtherClientPlayerEntity.animateDamage(this.playerWorldOtherClientPlayerEntity.getYaw());
                        this.playerWorldOtherClientPlayerEntity.hurtTime = 10;
                        this.playerWorldOtherClientPlayerEntity.maxHurtTime = 10;
                        float curHealth = this.playerWorldOtherClientPlayerEntity.getHealth();
                        float dmg = 2.0F;
                        if (client.player != null) {
                           try {
                              dmg = Math.max(2.0F, (float)client.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                           } catch (Throwable ignored) {
                           }
                        }
                        if (curHealth > dmg) {
                           this.playerWorldOtherClientPlayerEntity.setHealth(curHealth - dmg);
                        } else {
                           this.playerWorldOtherClientPlayerEntity.popTotem();
                        }
                     }
                  });
               }
               event.ail(true);
            }
         } catch (Throwable ignored) {
         }
      }
   }

   @EventListener
   public void onChannelRead0(ChannelRead0Event channelRead0Event) {
      Packet<?> packet = channelRead0Event.getPacket();
      if (packet instanceof DisconnectS2CPacket || packet instanceof GameJoinS2CPacket) {
         if (this.playerWorldOtherClientPlayerEntity != null) {
            this.playerWorldOtherClientPlayerEntity.despawn();
            this.playerWorldOtherClientPlayerEntity = null;
         }
         this.awc();
         return;
      }

      if ((Boolean)this.damage.atx() && this.playerWorldOtherClientPlayerEntity != null && this.playerWorldOtherClientPlayerEntity.isAlive()) {
         if (packet instanceof ExplosionS2CPacket explosionPacket) {
            Vec3d explosionPos = new Vec3d(explosionPacket.getX(), explosionPacket.getY(), explosionPacket.getZ());
            Vec3d fakePos = this.playerWorldOtherClientPlayerEntity.getPos();
            if (explosionPos.distanceTo(fakePos) > 10.0) {
               return;
            }

            ClientWorld world2 = client.world;
            if (world2 == null) return;
            Mutable mutable = new Mutable(explosionPacket.getX(), explosionPacket.getY(), explosionPacket.getZ());
            double calculatedDamage;
            if (world2.getBlockState(mutable).getBlock() == Blocks.RESPAWN_ANCHOR) {
               calculatedDamage = Vec3dEntityUtils.vX(
                  this.playerWorldOtherClientPlayerEntity,
                  explosionPos,
                  false,
                  10.0F,
                  0,
                  (Boolean)this.assumeBestArmor.atx()
               );
            } else {
               calculatedDamage = Vec3dEntityUtils.vK(
                  this.playerWorldOtherClientPlayerEntity,
                  explosionPos,
                  (Boolean)this.assumeBestArmor.atx()
               );
            }

            final double finalDamage = calculatedDamage;
            MinecraftClient.getInstance().execute(() -> {
               this.GE(finalDamage);
            });
         }
      }
   }

   public void GE(double d) {
      if (this.playerWorldOtherClientPlayerEntity == null || !this.playerWorldOtherClientPlayerEntity.isAlive()) return;
      if (this.isApplyingDamage) return;
      this.isApplyingDamage = true;
      try {
         PlayerWorldOtherClientPlayerEntity fp = this.playerWorldOtherClientPlayerEntity;
         try {
            fp.animateDamage(fp.getYaw());
            fp.hurtTime = 10;
            fp.maxHurtTime = 10;
         } catch (Throwable ignored) {
         }
         float currentHealth = fp.getHealth();
         if (currentHealth > d) {
            fp.setHealth((float)(currentHealth - d));
         } else {
            fp.popTotem();
         }
      } catch (Throwable ignored) {
      } finally {
         this.isApplyingDamage = false;
      }
   }
}
