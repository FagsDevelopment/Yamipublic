package pw.yami.client.module.movement;

import java.util.List;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.LlamaEntity;
import pw.yami.client.event.IsAiDisabledEvent;
import pw.yami.client.event.IsSaddledEvent;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.event.movement.GetMountJumpStrengthEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class EntityControl extends ClientModule {
   Setting jumpStrength = this.registerSetting(new NumberSetting("JumpStrength", "The fixed jump strength of the mounted entity", 0.1F, 0.7F, 2.0F));
   Setting noPigAI = this.registerSetting(new BooleanSetting("NoPigAI", "Prevents the pig movement when controlling pigs", false));

   public EntityControl() {
      super("EntityControl", "Allows you to steer entities without a saddle", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onTickPre(TickPreEvent tickPreEvent) {
      Entity entity2 = client.player.getVehicle();
      if (entity2 != null) {
         Entity temp = entity2;
         float f = client.player.getYaw();
         temp.setYaw(f);
         if (entity2 instanceof LlamaEntity) {
            LlamaEntity llamaEntity = (LlamaEntity)entity2;
            float f2 = client.player.getYaw();
            llamaEntity.headYaw = f2;
         }
      }
   }

   @EventListener
   public void onIsSaddled(IsSaddledEvent isSaddledEvent) {
      isSaddledEvent.aim();
   }

   @EventListener
   public void onGetMountJumpStrength(GetMountJumpStrengthEvent getMountJumpStrengthEvent) {
      getMountJumpStrengthEvent.aim();
      getMountJumpStrengthEvent.Dd((Float)this.jumpStrength.atx());
   }

   @EventListener
   public void onIsAiDisabled(IsAiDisabledEvent isAiDisabledEvent) {
      if (client.player != null) {
         if ((Boolean)this.noPigAI.atx()) {
            List temp = isAiDisabledEvent.getPigEntity().getPassengerList();
            ClientPlayerEntity player2 = client.player;
            if (temp.contains(player2)) {
               isAiDisabledEvent.aim();
            }
         }
      }
   }
}
