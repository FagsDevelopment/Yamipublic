package pw.yami.client.module.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import pw.yami.client.event.EventPhase;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.manager.Managers;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class AutoRelease extends ClientModule {
   Setting crossbow = this.registerSetting(new BooleanSetting("Crossbow", "Automatically releases crossbow when fully charged", false));
   Setting ticks = this.registerSetting(new NumberSetting("Ticks", "Ticks before releasing the bow charge", 3, 5, 20));
   Setting tPSSync = this.registerSetting(new BooleanSetting("TPS-Sync", "Sync bow release to server ticks", false));
   public static boolean af;

   public AutoRelease() {
      super("AutoBowRelease", "Automatically releases a charged bow", ModuleCategory.COMBAT);
   }

   @EventListener
   public void onTickPre(TickPreEvent tickPreEvent) {
      if (!SelfBow.aiZ().awg()) {
         if (tickPreEvent.getEventPhase() == EventPhase.POST) {
            ItemStack stack = client.player.getMainHandStack();
            if (stack.getItem() == Items.BOW) {
               float f;
               if ((Boolean)this.tPSSync.atx()) {
                  f = 20.0F - Managers.entityManager.rW();
               } else {
                  f = 0.0F;
               }

               if (client.player.getItemUseTime() + f >= ((Integer)this.ticks.atx()).intValue()) {
                  Managers.serverManager.iO('\uf560', new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
                  client.player.stopUsingItem();
               }
            } else if ((Boolean)this.crossbow.atx() && stack.getItem() == Items.CROSSBOW) {
               float temp = client.player.getItemUseTime();
               ItemStack temp2 = client.player.getMainHandStack();
               ClientPlayerEntity player2 = client.player;
               if (temp / CrossbowItem.getPullTime(temp2, (LivingEntity)player2) > 1.0F) {
                  Managers.serverManager.iO('\uf560', new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
                  client.player.stopUsingItem();
                  Hand hand = Hand.MAIN_HAND;
                  ClientPlayerEntity player3 = client.player;
                  client.interactionManager.interactItem((PlayerEntity)player3, hand);
               }
            }
         }
      }
   }
}
