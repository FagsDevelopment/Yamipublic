package pw.yami.client.event.network;

import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class VisitFormattedEvent extends Event {
   private String text;
   public static boolean dw;

   public VisitFormattedEvent(String str) {
      this.text = str;
   }

   public String getText() {
      return this.text;
   }

   public void setText(String str) {
      this.text = str;
   }
}
