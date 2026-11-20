package pw.yami.client.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.yami.client.event.render.RenderEvent9;
import pw.yami.client.module.render.FPSBoost;
import pw.yami.eventbus.EventBusImpl;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
   @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At("HEAD"), cancellable = true)
   private void hookRender(BlockEntity blockEntity, float f, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, CallbackInfo callbackInfo) {
      FPSBoost fpsBoost = FPSBoost.getInstance();
      if (fpsBoost != null && fpsBoost.awg() && (Boolean)fpsBoost.cullBlockEntities.atx() && MinecraftClient.getInstance().player != null && blockEntity != null) {
         float maxDist = (Float)fpsBoost.blockEntityDist.atx();
         double maxDistSq = maxDist * maxDist;
         if (blockEntity.getPos().getSquaredDistance(MinecraftClient.getInstance().player.getPos()) > maxDistSq) {
            callbackInfo.cancel();
            return;
         }
      }

      RenderEvent9 renderEvent9 = new RenderEvent9(blockEntity);
      EventBusImpl.INSTANCE.rC(renderEvent9);
   }
}
