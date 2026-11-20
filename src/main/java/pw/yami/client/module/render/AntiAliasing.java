package pw.yami.client.module.render;

import pw.yami.client.model.render.AntiAliasingOption;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;

public class AntiAliasing extends ClientModule {
   private static AntiAliasing INSTANCE;
   Setting mode = this.registerSetting(new EnumSetting("Mode", "mode", AntiAliasingOption.a_, AntiAliasingOption.values()));
   Setting sample = this.registerSetting(new NumberSetting("Sample", "MSAA sampling number", 0, 8, 32));
   public static boolean bi;

   public AntiAliasing() {
      super("AntiAliasing", "Combat the problems of aliasing in a sampled signal such as a digital image", ModuleCategory.RENDER);
      INSTANCE = this;
      this.WT(true);
   }

   public static AntiAliasing Dj() {
      return INSTANCE;
   }

   public int Dk() {
      return (Integer)this.sample.atx();
   }
}
