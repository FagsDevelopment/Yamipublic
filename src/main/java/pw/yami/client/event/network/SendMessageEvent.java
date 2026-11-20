package pw.yami.client.event.network;

import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.UnknownEvent10;

@UnidentifiedInterface2
public class SendMessageEvent extends UnknownEvent10 {
   public SendMessageEvent(String str) {
      super(str);
   }
}
