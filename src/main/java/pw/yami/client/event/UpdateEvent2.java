package pw.yami.client.event;

import java.awt.Color;
import pw.yami.client.api.UnidentifiedInterface2;

@UnidentifiedInterface2
public class UpdateEvent2 extends Event {
   private Color color;

   public Color getColor() {
      return this.color;
   }

   public void setColor(Color color2) {
      this.color = color2;
   }
}
