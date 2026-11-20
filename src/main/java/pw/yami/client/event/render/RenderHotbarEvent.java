package pw.yami.client.event.render;

import net.minecraft.client.gui.DrawContext;

import pw.yami.client.api.UnidentifiedInterface2;

@UnidentifiedInterface2
public class RenderHotbarEvent extends DrawContextEvent {
   public RenderHotbarEvent(DrawContext context) {
      super(context);
   }

}
