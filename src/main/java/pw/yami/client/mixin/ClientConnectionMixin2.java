package pw.yami.client.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.yami.client.event.DisconnectEvent;
import pw.yami.client.event.network.ChannelRead0Event;
import pw.yami.client.event.network.DecodeEvent;
import pw.yami.client.event.network.SendImmediatelyEvent;
import pw.yami.client.event.network.SendImmediately_2Event;
import pw.yami.eventbus.EventBusImpl;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin2 {
   @Shadow
   @Final
   private static Logger LOGGER;
   @Shadow
   @Nullable
   private volatile PacketListener packetListener;

   @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
   private void hookExceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable error2, CallbackInfo callbackInfo) {
      DecodeEvent decodeEvent = new DecodeEvent();
      EventBusImpl.INSTANCE.rC(decodeEvent);
      if (decodeEvent.aik()) {
         LOGGER.error("Exception caught on network thread:", error2);
         callbackInfo.cancel();
      }
   }

   @Inject(method = "sendImmediately(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", at = @At("HEAD"), cancellable = true)
   private void hookSendImmediately(Packet packet2, @Nullable PacketCallbacks packetCallbacks, boolean flag, CallbackInfo callbackInfo) {
      SendImmediatelyEvent sendImmediatelyEvent = new SendImmediatelyEvent(packet2);
      EventBusImpl.INSTANCE.rC(sendImmediatelyEvent);
      if (sendImmediatelyEvent.aik()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "sendImmediately(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", at = @At("TAIL"), cancellable = true)
   private void hookSendImmediately$2(Packet packet2, @Nullable PacketCallbacks packetCallbacks, boolean flag, CallbackInfo callbackInfo) {
      SendImmediately_2Event sendImmediately2Event = new SendImmediately_2Event(packet2);
      EventBusImpl.INSTANCE.rC(sendImmediately2Event);
      if (sendImmediately2Event.aik()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
   private void hookChannelRead0(ChannelHandlerContext channelHandlerContext, Packet packet2, CallbackInfo callbackInfo) {
      PacketListener packetListener = this.packetListener;
      if (packet2 != null && packetListener != null && packetListener.accepts(packet2)) {
         if (packet2 instanceof net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket) {
            if (pw.yami.client.utils.world.PlacementUtils.isPlacing || pw.yami.client.utils.world.PlacementUtils.shouldReleaseSneak) {
               callbackInfo.cancel();
               int syncId = ((net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket)packet2).getSyncId();
               if (pw.yami.client.Mc.client.player != null && pw.yami.client.Mc.client.player.networkHandler != null) {
                  pw.yami.client.Mc.client.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket(syncId));
               }
               return;
            }
         }
         ChannelRead0Event channelRead0Event = new ChannelRead0Event(this.packetListener, packet2);
         EventBusImpl.INSTANCE.rC(channelRead0Event);
         if (channelRead0Event.aik()) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "disconnect(Lnet/minecraft/network/DisconnectionInfo;)V", at = @At("HEAD"))
   private void hookDisconnect(DisconnectionInfo disconnectionInfo, CallbackInfo callbackInfo) {
      DisconnectEvent disconnectEvent = new DisconnectEvent();
      EventBusImpl.INSTANCE.rC(disconnectEvent);
   }
}
