package pw.yami.client.event.render;

import net.minecraft.client.gui.DrawContext;
import pw.yami.client.api.UnidentifiedInterface2;

@UnidentifiedInterface2
public class RenderSpyglassOverlayEvent extends DrawContextEvent {
   public RenderSpyglassOverlayEvent(DrawContext context) {
      super(context);
   }

   public static boolean au;
}
