package pw.yami.client.module.render;

import net.minecraft.entity.player.PlayerEntity;
import pw.yami.client.event.render.Render_isInvisibleToEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public final class TrueSight extends ClientModule {
   Setting onlyPlayers = this.registerSetting(new BooleanSetting("OnlyPlayers", "Only reveal invisible players", true));
   public static boolean eq;

   public TrueSight() {
      super("TrueSight", "Allows you to see invisible entities", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRender_isInvisibleTo(Render_isInvisibleToEvent renderisInvisibleToEvent) {
      if (renderisInvisibleToEvent.getLiving().isInvisible() && (!(Boolean)this.onlyPlayers.atx() || renderisInvisibleToEvent.getLiving() instanceof PlayerEntity)) {
         renderisInvisibleToEvent.aim();
      }
   }
}
