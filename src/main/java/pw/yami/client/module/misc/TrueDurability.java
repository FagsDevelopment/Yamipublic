package pw.yami.client.module.misc;

import pw.yami.client.event.player.InitItemEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.eventbus.annotation.EventListener;

public class TrueDurability extends ClientModule {
   public static boolean ba;

   public TrueDurability() {
      super("TrueDurability", "Displays the true durability of unbreakable items", ModuleCategory.MISC);
   }

   @EventListener
   public void onInitItem(InitItemEvent initItemEvent) {
      int n = initItemEvent.abL();
      if (initItemEvent.abM() < 0) {
         n = initItemEvent.abM();
      }

      initItemEvent.aim();
      initItemEvent.abN(n);
   }
}
