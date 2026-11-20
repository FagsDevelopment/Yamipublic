package pw.yami.client.event.render;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class AddParticleEvent extends Event {
   private final ParticleEffect particleEffect;
   public static boolean hh;

   public AddParticleEvent(ParticleEffect particleEffect2) {
      this.particleEffect = particleEffect2;
   }

   public ParticleEffect getParticleEffect() {
      return this.particleEffect;
   }

   public ParticleType Le() {
      return this.particleEffect.getType();
   }
}
