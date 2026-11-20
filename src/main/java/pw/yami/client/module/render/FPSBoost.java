package pw.yami.client.module.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.event.render.AddExplosionParticleEvent;
import pw.yami.client.event.render.AddParticleEvent;
import pw.yami.client.event.render.ApplyFogEvent;
import pw.yami.client.event.render.RenderArmorGlintEvent;
import pw.yami.client.event.render.RenderEntityEvent;
import pw.yami.client.event.render.TiltViewWhenHurtEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class FPSBoost extends ClientModule {
   private static FPSBoost INSTANCE;

   public Setting noShadows = this.registerSetting(new BooleanSetting("NoShadows", "Disables entity shadows for significant GPU geometry savings", true));
   public Setting particles = this.registerSetting(new BooleanSetting("Particles", "Removes explosions, campfires, portals, and heavy particles", true));
   public Setting noWeather = this.registerSetting(new BooleanSetting("NoWeather", "Disables rain and snow rendering to prevent frame drops", true));
   public Setting noFog = this.registerSetting(new BooleanSetting("NoFog", "Disables terrain and atmospheric fog rendering to boost GPU throughput", true));
   public Setting noGlint = this.registerSetting(new BooleanSetting("NoGlint", "Disables heavy multi-pass enchantment glint shader rendering", false));
   public Setting noHurtCam = this.registerSetting(new BooleanSetting("NoHurtCam", "Removes camera shaking on damage for ultra smooth combat", true));
   public Setting cullEntities = this.registerSetting(new BooleanSetting("CullEntities", "Skips rendering distant non-player entities", true));
   public Setting cullDistance = this.registerSetting(new NumberSetting("CullDistance", "Distance beyond which non-player entities are culled", 16.0F, 48.0F, 128.0F, this.cullEntities::atx));
   public Setting cullBlockEntities = this.registerSetting(new BooleanSetting("CullBlockEntities", "Skips rendering distant chests, banners, and signs", true));
   public Setting blockEntityDist = this.registerSetting(new NumberSetting("BlockEntityDist", "Distance beyond which block entities are culled", 8.0F, 32.0F, 96.0F, this.cullBlockEntities::atx));
   public Setting cullItems = this.registerSetting(new BooleanSetting("CullGroundItems", "Skips rendering distant dropped items to eliminate CPVP lag", true));
   public Setting itemDist = this.registerSetting(new NumberSetting("ItemDistance", "Maximum render distance for dropped items", 8.0F, 24.0F, 64.0F, this.cullItems::atx));
   public Setting cullArmorStands = this.registerSetting(new BooleanSetting("CullArmorStands", "Skips rendering armor stands to boost FPS in lobbies", true));
   public Setting noDead = this.registerSetting(new BooleanSetting("NoDeadEntities", "Immediately stops rendering dead entities to prevent animation lag", true));
   public Setting memoryTrim = this.registerSetting(new BooleanSetting("MemoryTrim", "Periodically cleans up unused memory to prevent sudden stutter spikes", true));

   private boolean originalShadows = true;
   private int trimTickCounter = 0;

   public FPSBoost() {
      super("FPSBoost", "Dramatically boosts FPS and game smoothness for low-end PCs", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static FPSBoost getInstance() {
      return INSTANCE;
   }

   @Override
   public void awd() {
      if (client.options != null && (Boolean)this.noShadows.atx()) {
         try {
            this.originalShadows = (Boolean)client.options.getEntityShadows().getValue();
            client.options.getEntityShadows().setValue(false);
         } catch (Throwable ignored) {
         }
      }
      this.trimTickCounter = 0;
   }

   @Override
   public void awe() {
      if (client.options != null) {
         try {
            client.options.getEntityShadows().setValue(this.originalShadows);
         } catch (Throwable ignored) {
         }
      }
   }

   @EventListener
   public void onTickPre(TickPreEvent event) {
      if (client.world != null) {
         if ((Boolean)this.noWeather.atx()) {
            try {
               client.world.setRainGradient(0.0F);
               client.world.setThunderGradient(0.0F);
            } catch (Throwable ignored) {
            }
         }
      }

      if (client.options != null && (Boolean)this.noShadows.atx()) {
         try {
            if ((Boolean)client.options.getEntityShadows().getValue()) {
               client.options.getEntityShadows().setValue(false);
            }
         } catch (Throwable ignored) {
         }
      }

      if ((Boolean)this.memoryTrim.atx()) {
         if (++this.trimTickCounter >= 1200) {
            this.trimTickCounter = 0;
            try {
               System.gc();
            } catch (Throwable ignored) {
            }
         }
      }
   }

   @EventListener
   public void onTiltViewWhenHurt(TiltViewWhenHurtEvent event) {
      if ((Boolean)this.noHurtCam.atx()) {
         event.aim();
      }
   }

   @EventListener
   public void onAddParticle(AddParticleEvent event) {
      if ((Boolean)this.particles.atx()) {
         ParticleType type = event.Le();
         if (type == ParticleTypes.EXPLOSION
               || type == ParticleTypes.EXPLOSION_EMITTER
               || type == ParticleTypes.FIREWORK
               || type == ParticleTypes.CAMPFIRE_COSY_SMOKE
               || type == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE
               || type == ParticleTypes.PORTAL
               || type == ParticleTypes.FALLING_LAVA
               || type == ParticleTypes.FALLING_WATER
               || type == ParticleTypes.SPLASH
               || type == ParticleTypes.BUBBLE
               || type == ParticleTypes.SMOKE
               || type == ParticleTypes.LARGE_SMOKE
               || type == ParticleTypes.DRIPPING_LAVA
               || type == ParticleTypes.DRIPPING_WATER
               || type == ParticleTypes.POOF
               || type == ParticleTypes.SPIT
               || type == ParticleTypes.SNEEZE) {
            event.aim();
         }
      }
   }

   @EventListener
   public void onAddExplosionParticle(AddExplosionParticleEvent event) {
      if ((Boolean)this.particles.atx()) {
         event.aim();
      }
   }

   @EventListener
   public void onApplyFog(ApplyFogEvent event) {
      if ((Boolean)this.noFog.atx()) {
         event.aim();
         event.VT(event.VR() * 8.0F);
         event.VV(event.VR() * 10.0F);
      }
   }

   @EventListener
   public void onRenderArmorGlint(RenderArmorGlintEvent event) {
      if ((Boolean)this.noGlint.atx()) {
         event.aim();
      }
   }

   @EventListener
   public void onRenderEntity(RenderEntityEvent event) {
      if (client.player == null) {
         return;
      }
      Entity entity = event.getEntity();
      if (entity == null) {
         return;
      }

      if ((Boolean)this.noDead.atx()) {
         if (!entity.isAlive() || (entity instanceof LivingEntity && ((LivingEntity)entity).deathTime > 0)) {
            event.aim();
            return;
         }
      }

      if (entity instanceof ItemEntity) {
         if ((Boolean)this.cullItems.atx()) {
            float dist = (Float)this.itemDist.atx();
            if (entity.squaredDistanceTo(client.player) > dist * dist) {
               event.aim();
               return;
            }
         }
         return;
      }

      if (entity instanceof ArmorStandEntity) {
         if ((Boolean)this.cullArmorStands.atx()) {
            event.aim();
            return;
         }
      }

      if ((Boolean)this.cullEntities.atx() && !(entity instanceof PlayerEntity)) {
         float dist = (Float)this.cullDistance.atx();
         double maxDistSq = dist * dist;
         if (entity.squaredDistanceTo(client.player) > maxDistSq) {
            event.aim();
         }
      }
   }
}
