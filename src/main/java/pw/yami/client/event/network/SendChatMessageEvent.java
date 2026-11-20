package pw.yami.client.event.network;

import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.UnknownEvent10;

@UnidentifiedInterface2
public class SendChatMessageEvent extends UnknownEvent10 {
   public SendChatMessageEvent(String str) {
      super(str);
   }

   public static boolean dc;
}
