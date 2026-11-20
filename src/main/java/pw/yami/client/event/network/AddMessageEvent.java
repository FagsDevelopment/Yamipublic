package pw.yami.client.event.network;

import net.minecraft.text.Text;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class AddMessageEvent extends Event {
   private Text text;

   public AddMessageEvent(Text text2) {
      this.text = text2;
   }

   public Text getText() {
      return this.text;
   }

   public void setText(Text text2) {
      this.text = text2;
   }
}
