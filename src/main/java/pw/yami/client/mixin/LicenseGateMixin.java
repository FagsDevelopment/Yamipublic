package pw.yami.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class LicenseGateMixin {
   @Inject(method = "joinWorld", at = @At("HEAD"))
   private void yami$blockJoin(ClientWorld world, DownloadingTerrainScreen.WorldEntryReason reason, CallbackInfo callbackInfo) {
      // Auth completely removed - allow all world joins freely
   }

   @Inject(method = "tick", at = @At("HEAD"))
   private void yami$licenseTick(CallbackInfo callbackInfo) {
      // Auth completely removed - never disconnect
   }
}
