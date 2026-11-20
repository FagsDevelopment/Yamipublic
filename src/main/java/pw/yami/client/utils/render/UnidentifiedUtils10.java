package pw.yami.client.utils.render;

import pw.yami.client.render.ColorManager;

public class UnidentifiedUtils10 {
   public static ColorManager colorManager;
   public static ColorManager colorManager2;
   public static ColorManager colorManager3;

   private UnidentifiedUtils10() {
   }

   public static void atH() {
      colorManager = new ColorManager("pos_color.vert", "pos_color.frag");
      colorManager2 = new ColorManager("pos_tex_color.vert", "pos_tex_color.frag");
      colorManager3 = new ColorManager("text.vert", "text.frag");
   }
}
