package pw.yami.client.module.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import pw.yami.client.event.GetFramelimitEvent;
import pw.yami.client.event.OnKeyEvent;
import pw.yami.client.event.UpdateEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.StopwatchImpl;
import pw.yami.eventbus.annotation.EventListener;

public class AdaptiveFPS extends ClientModule {
   Setting unfocused = this.registerSetting(new NumberSetting("Unfocused", "The FPS limit when game is in the background", 5, 30, 120));
   Setting idle = this.registerSetting(new NumberSetting("Idle", "The FPS limit when game is idle (0 is unlimited)", 5, 30, 120));
   Setting idleTime = this.registerSetting(new NumberSetting("IdleTime", "Idle time (0 is disabled)", 0, 1, 10));
   Setting fps = this.registerSetting(new NumberSetting("FPS", "The max FPS for game (0 is unlimited)", 0, 274, 1000));
   private static final StopwatchImpl e = new StopwatchImpl();

   public AdaptiveFPS() {
      super("AdaptiveFPS", "Reduces FPS when game is in the background", ModuleCategory.MISC);
   }

   @EventListener(priority = 1)
   public void onUpdate(UpdateEvent updateEvent) {
      if (updateEvent.lR() != 0.0 || updateEvent.lS() != 0.0) {
         e.afr(29432, 5265492, (byte)40);
      }
   }

   @EventListener(priority = 1)
   public void onOnKey(OnKeyEvent onKeyEvent) {
      e.afr(29432, 5265492, (byte)40);
   }

   @EventListener
   public void onGetFramelimit(GetFramelimitEvent getFramelimitEvent) {
      if (!client.isWindowFocused()) {
         getFramelimitEvent.aim();
         getFramelimitEvent.arI((Integer)this.unfocused.atx());
      } else if ((Integer)this.idleTime.atx() > 0 && e.afq((Integer)this.idleTime.atx() * 60000)) {
         getFramelimitEvent.aim();
         getFramelimitEvent.arI((Integer)this.idle.atx());
      } else {
         getFramelimitEvent.aim();
         getFramelimitEvent.arI(260);
         if ((Integer)this.fps.atx() > 0) {
            RenderSystem.limitDisplayFPS((Integer)this.fps.atx());
         }
      }
   }
}
