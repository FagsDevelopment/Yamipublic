package pw.yami.client.manager;

import pw.yami.client.event.OnInitFinishedEvent;
import pw.yami.client.utils.render.FontLoader;
import pw.yami.eventbus.EventBusImpl;
import pw.yami.eventbus.annotation.EventListener;

public class UnidentifiedHelper3 {
   public UnidentifiedHelper3() {
      EventBusImpl.INSTANCE.rD(this);
   }

   @EventListener
   public void onOnInitFinished(OnInitFinishedEvent onInitFinishedEvent) {
      FontLoader.aor();
   }
}
