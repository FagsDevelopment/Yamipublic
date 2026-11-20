package pw.yami.client.module.player;

import net.minecraft.client.gui.screen.DeathScreen;
import pw.yami.client.event.EventPhase;
import pw.yami.client.event.SetScreenEvent;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.eventbus.annotation.EventListener;

public class AutoRespawn extends ClientModule {
   private boolean a;
   public static boolean dq;

   public AutoRespawn() {
      super("AutoRespawn", "Respawns automatically after a death", ModuleCategory.PLAYER);
   }

   @EventListener
   public void onTickPre(TickPreEvent tickPreEvent) {
      if (tickPreEvent.getEventPhase() == EventPhase.PRE && this.a && client.player.isDead()) {
         client.player.requestRespawn();
         this.a = false;
      }
   }

   @EventListener
   public void onSetScreen(SetScreenEvent setScreenEvent) {
      if (setScreenEvent.getScreen() instanceof DeathScreen) {
         this.a = true;
      }
   }
}
