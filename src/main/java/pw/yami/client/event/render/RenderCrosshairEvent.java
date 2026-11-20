package pw.yami.client.event.render;

import net.minecraft.client.gui.DrawContext;
import pw.yami.client.api.UnidentifiedInterface2;
import pw.yami.client.event.Event;

@UnidentifiedInterface2
public class RenderCrosshairEvent extends Event {
   private final DrawContext context;
   public static boolean l;

   public RenderCrosshairEvent(DrawContext context2) {
      this.context = context2;
   }

   public DrawContext getContext() {
      return this.context;
   }
}
