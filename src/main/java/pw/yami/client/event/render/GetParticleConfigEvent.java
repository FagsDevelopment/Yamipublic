package pw.yami.client.event.render;

import net.minecraft.world.biome.BiomeParticleConfig;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class GetParticleConfigEvent extends Event {
   private BiomeParticleConfig biomeParticleConfig;
   public static boolean fa;

   public BiomeParticleConfig getBiomeParticleConfig() {
      return this.biomeParticleConfig;
   }

   public void setBiomeParticleConfig(BiomeParticleConfig biomeParticleConfig2) {
      this.biomeParticleConfig = biomeParticleConfig2;
   }
}
