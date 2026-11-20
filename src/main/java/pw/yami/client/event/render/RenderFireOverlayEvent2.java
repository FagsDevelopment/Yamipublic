package pw.yami.client.event.render;

import net.minecraft.client.gui.DrawContext;

import pw.yami.client.api.UnidentifiedInterface2;

@UnidentifiedInterface2
public class RenderFireOverlayEvent2 extends DrawContextEvent {
   public RenderFireOverlayEvent2(DrawContext context) {
      super(context);
   }

}
