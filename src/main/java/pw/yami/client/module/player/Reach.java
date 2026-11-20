package pw.yami.client.module.player;

import pw.yami.client.event.combat.UpdateTargetedEntity_2Event;
import pw.yami.client.event.world.GetBlockInteractionRangeEvent;
import pw.yami.client.event.world.GetEntityInteractionRangeEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class Reach extends ClientModule {
   Setting blockReach = this.registerSetting(new NumberSetting("BlockReach", "The extended block reach distance", 4.0F, 4.5F, 6.0F));
   Setting entityReach = this.registerSetting(new NumberSetting("EntityReach", "The extended entity reach distance", 2.5F, 3.0F, 6.0F));
   Setting noHitbox = this.registerSetting(new BooleanSetting("NoHitbox", "Ignores hitboxes allowing player to interact through entities", false));

   public Reach() {
      super("Reach", "Extends player reach", ModuleCategory.PLAYER);
   }

   @Override
   public String hy() {
      return Float.toString((Float)this.entityReach.atx());
   }

   @EventListener
   public void onGetEntityInteractionRange(GetEntityInteractionRangeEvent getEntityInteractionRangeEvent) {
      getEntityInteractionRangeEvent.aim();
      getEntityInteractionRangeEvent.ME(((Float)this.entityReach.atx()).floatValue());
   }

   @EventListener
   public void onGetBlockInteractionRange(GetBlockInteractionRangeEvent getBlockInteractionRangeEvent) {
      getBlockInteractionRangeEvent.aim();
      getBlockInteractionRangeEvent.ME(((Float)this.blockReach.atx()).floatValue());
   }

   @EventListener
   public void onUpdateTargetedEntity_2(UpdateTargetedEntity_2Event updateTargetedEntity2Event) {
      updateTargetedEntity2Event.ail((Boolean)this.noHitbox.atx());
   }
}
