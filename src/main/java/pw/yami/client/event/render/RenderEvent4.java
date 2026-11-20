package pw.yami.client.event.render;

import net.minecraft.client.gui.DrawContext;

import pw.yami.client.api.UnidentifiedInterface2;

@UnidentifiedInterface2
public class RenderEvent4 extends DrawContextEvent {
   public RenderEvent4(DrawContext context) {
      super(context);
   }

   public static boolean i;
}
