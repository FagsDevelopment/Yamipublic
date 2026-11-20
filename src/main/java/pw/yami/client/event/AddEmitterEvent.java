package pw.yami.client.event;

import net.minecraft.particle.ParticleEffect;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.render.AddParticleEvent;

@UnidentifiedInterface2
public class AddEmitterEvent extends AddParticleEvent {
   public static boolean cj;

   public AddEmitterEvent(ParticleEffect particleEffect) {
      super(particleEffect);
   }
}
