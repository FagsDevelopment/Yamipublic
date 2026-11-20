package pw.yami.client.module;

import pw.yami.client.event.CanInsertEvent;
import pw.yami.eventbus.annotation.EventListener;

public class Module4 extends ClientModule {
   public static boolean cn;

   public Module4() {
      super("ShulkerInsert", "Allows any item to be inserted into shulker boxes", ModuleCategory.MISC);
   }

   @EventListener
   public void onCanInsert(CanInsertEvent canInsertEvent) {
      canInsertEvent.aim();
   }
}
