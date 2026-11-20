package pw.yami.client.module.misc;

import pw.yami.client.event.render.RenderEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.eventbus.annotation.EventListener;

public class Module2 extends ClientModule {
   private static final long a = 0L;
   private static final String b = null;

   public Module2() {
      super(b, "", ModuleCategory.MISC);
   }

   @EventListener
   public void onRender(RenderEvent renderEvent) {
   }

   @Override
   protected void awd() {
   }
}
