package pw.yami.client.module.render;

import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;

public class AspectRatio extends ClientModule {
   private static AspectRatio INSTANCE;
   public final Setting ratio = this.registerSetting(new NumberSetting("Ratio", "Ratio for aspect", 0.0F, 1.78F, 5.0F));

   public AspectRatio() {
      super("AspectRatio", "Change your screen aspect ratio", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static AspectRatio YA() {
      return INSTANCE;
   }
}
