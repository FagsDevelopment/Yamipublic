package pw.yami.client.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.SlimeBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.yami.client.Mc;
import pw.yami.client.event.movement.OnSteppedOnEvent;
import pw.yami.eventbus.EventBusImpl;

@Mixin(SlimeBlock.class)
public class SlimeBlockMixin {
   @Inject(method = "onSteppedOn(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void hookOnSteppedOn(World world2, BlockPos pos, BlockState state, Entity entity2, CallbackInfo callbackInfo) {
      OnSteppedOnEvent onSteppedOnEvent = new OnSteppedOnEvent();
      EventBusImpl.INSTANCE.rC(onSteppedOnEvent);
      if (onSteppedOnEvent.aik() && entity2 == Mc.client.player) {
         callbackInfo.cancel();
      }
   }
}
