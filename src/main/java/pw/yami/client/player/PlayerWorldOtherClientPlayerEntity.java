package pw.yami.client.player;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.explosion.Explosion;
import pw.yami.client.Mc;
import pw.yami.client.event.network.ChannelRead0Event;
import pw.yami.client.module.misc.FakePlayer;
import pw.yami.eventbus.EventBusImpl;

public class PlayerWorldOtherClientPlayerEntity extends OtherClientPlayerEntity {
   public static final int FAKE_PLAYER_ID = -9344;
   private final PlayerEntity player;
   private boolean killed = false;
   private int deathTicks = 0;
   public static boolean eu;

   public PlayerWorldOtherClientPlayerEntity(PlayerEntity playerEntity, String str, boolean autoSpawn) {
      super(Mc.client.world, a(str));
      this.player = playerEntity;
      this.setId(FAKE_PLAYER_ID);

      if (this.player != null) {
         this.copyPositionAndRotation(this.player);
         this.setYaw(this.player.getYaw());
         this.setPitch(this.player.getPitch());
         this.setHeadYaw(this.player.headYaw);
         this.setBodyYaw(this.player.bodyYaw);
         this.prevYaw = this.player.getYaw();
         this.prevPitch = this.player.getPitch();
         this.prevHeadYaw = this.player.headYaw;
         this.prevBodyYaw = this.player.bodyYaw;
         this.setPose(this.player.getPose());
         this.getInventory().clone(this.player.getInventory());
         try {
            this.getDataTracker().set(PLAYER_MODEL_PARTS, this.player.getDataTracker().get(PLAYER_MODEL_PARTS));
         } catch (Throwable ignored) {
         }
         if (this.player.isFallFlying()) {
            this.startFallFlying();
         }
      } else {
         this.setPose(EntityPose.STANDING);
      }

      if (!this.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
         this.getInventory().setStack(40, new ItemStack(Items.TOTEM_OF_UNDYING));
      }

      EntityAttributeInstance absorptionAttr = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_ABSORPTION);
      if (absorptionAttr != null) {
         absorptionAttr.setBaseValue(16.0);
      }
      this.setHealth(20.0F);
      this.setAbsorptionAmount(16.0F);
      this.setInvulnerable(false);

      if (autoSpawn) {
         this.spawn();
      }
   }

   public PlayerWorldOtherClientPlayerEntity(PlayerEntity playerEntity, String str) {
      this(playerEntity, str, false);
   }

   public PlayerWorldOtherClientPlayerEntity(PlayerEntity playerEntity) {
      this(playerEntity, "FakePlayer", false);
   }

   public PlayerWorldOtherClientPlayerEntity(String str) {
      this((PlayerEntity)Mc.client.player, str, false);
   }

   public PlayerWorldOtherClientPlayerEntity() {
      this((PlayerEntity)Mc.client.player, "FakePlayer", false);
   }

   private static GameProfile a(String str) {
      String finalName = "FakePlayer";
      if (str != null && !str.trim().isEmpty()) {
         finalName = str;
      }
      return new GameProfile(UUID.randomUUID(), finalName);
   }

   public void spawn() {
      if (Mc.client.world != null) {
         this.unsetRemoved();
         this.setId(FAKE_PLAYER_ID);
         Mc.client.world.addEntity(this);
      }
   }

   public void despawn() {
      if (Mc.client.world != null) {
         Mc.client.world.removeEntity(this.getId(), RemovalReason.DISCARDED);
         this.setRemoved(RemovalReason.DISCARDED);
      }
   }

   public void Cp() {
      this.spawn();
   }

   public void Cq() {
      this.despawn();
   }

   public void kill() {
      this.killed = true;
      this.setHealth(0.0F);
      this.setAbsorptionAmount(0.0F);
   }

   public boolean isKilled() {
      return this.killed;
   }

   public int getDeathTicks() {
      return this.deathTicks;
   }

   @Override
   public boolean isDead() {
      return this.killed || this.isRemoved();
   }

   @Override
   public boolean isAlive() {
      return !this.killed && !this.isRemoved();
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   public boolean isAttackable() {
      return true;
   }

   @Override
   public boolean isFireImmune() {
      return true;
   }

   @Override
   public boolean isCreative() {
      return false;
   }

   @Override
   public boolean isSpectator() {
      return false;
   }

   @Override
   public boolean isImmuneToExplosion(Explosion explosion) {
      return false;
   }

   @Override
   protected void pushAway(Entity entity) {
   }

   @Override
   public void pushAwayFrom(Entity entity) {
   }

   @Override
   public SkinTextures getSkinTextures() {
      if (Mc.client.player != null) {
         return Mc.client.player.getSkinTextures();
      }
      return super.getSkinTextures();
   }

   @Override
   public void tick() {
      if (this.killed) {
         if (++this.deathTicks > 6) {
            this.despawn();
         }
         this.setAbsorptionAmount(0.0F);
         super.setHealth(0.0F);
         return;
      }

      EntityAttributeInstance absorptionAttr = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_ABSORPTION);
      if (absorptionAttr != null) {
         absorptionAttr.setBaseValue(16.0);
      }

      this.age++;
      if (this.age % 80 == 0) {
         this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 900, 1));
         this.setHealth(20.0F);
         this.setAbsorptionAmount(16.0F);
      }

      if (!this.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
         this.getInventory().setStack(40, new ItemStack(Items.TOTEM_OF_UNDYING));
      }

      super.tick();
   }

   @Override
   public void setHealth(float health) {
      super.setHealth(health);
      if (this.getHealth() <= 0.0F && !this.killed) {
         this.popTotem();
      }
   }

   public void popTotem() {
      super.setHealth(20.0F);
      this.clearStatusEffects();
      this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
      this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 900, 1));
      this.setAbsorptionAmount(16.0F);

      if (Mc.client.particleManager != null) {
         Mc.client.particleManager.addEmitter(this, ParticleTypes.TOTEM_OF_UNDYING, 30);
      }

      if (Mc.client.world != null && Mc.client.player != null) {
         Mc.client.world.playSound(
            Mc.client.player,
            this.getBlockPos(),
            SoundEvents.ITEM_TOTEM_USE,
            SoundCategory.PLAYERS,
            1.0F,
            1.0F
         );
      }

      EntityStatusS2CPacket packet = new EntityStatusS2CPacket(this, (byte)35);
      ChannelRead0Event event = new ChannelRead0Event(Mc.client.getNetworkHandler(), packet);
      EventBusImpl.INSTANCE.rC(event);
      if (!event.aik() && Mc.client.getNetworkHandler() != null) {
         try {
            packet.apply(Mc.client.getNetworkHandler());
         } catch (Throwable ignored) {
         }
      }

      try {
         pw.yami.client.module.render.IsraelPop israelPop = (pw.yami.client.module.render.IsraelPop)pw.yami.client.manager.Managers.moduleManager.XL("Israel Pop");
         if (israelPop != null && israelPop.awg()) {
            israelPop.triggerPop(this);
         }
      } catch (Throwable ignored) {
      }
   }

   @Override
   public boolean damage(DamageSource source, float amount) {
      try {
         FakePlayer fp = FakePlayer.INSTANCE;
         if (fp != null && fp.awg() && fp.isDamageEnabled()) {
            fp.GE((double)amount);
            return true;
         }
         this.animateDamage(this.getYaw());
         this.hurtTime = 10;
         this.maxHurtTime = 10;
         if (this.getHealth() > amount) {
            this.setHealth(this.getHealth() - amount);
         } else {
            this.popTotem();
         }
         return true;
      } catch (Throwable ignored) {
         return false;
      }
   }

   public PlayerEntity getPlayer() {
      return this.player;
   }
}
