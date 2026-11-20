package pw.yami.client.event.render;

import net.minecraft.client.gui.DrawContext;
import pw.yami.client.api.UnidentifiedInterface2;

@UnidentifiedInterface2
public class RenderStatusEffectOverlayEvent extends DrawContextEvent {
   public RenderStatusEffectOverlayEvent(DrawContext context) {
      super(context);
   }

   public static boolean be;
}
