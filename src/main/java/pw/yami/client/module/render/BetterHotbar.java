package pw.yami.client.module.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import pw.yami.client.event.player.SelectedSlotXEvent;
import pw.yami.client.model.render.Easing;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.render.UnidentifiedHelper9;
import pw.yami.eventbus.annotation.EventListener;

public class BetterHotbar extends ClientModule {
   private static BetterHotbar INSTANCE;
   Setting animation = this.registerSetting(new BooleanSetting("Animation", "", true));
   Setting length = this.registerSetting(new NumberSetting("Length", "Timer for the animation", 0, 300, 350));
   Setting easing = this.registerSetting(new EnumSetting("Easing", "Animation easing", Easing.CUBIC_OUT, Easing.values()));
   UnidentifiedHelper9 unidentifiedHelper9_1 = new UnidentifiedHelper9();

   public BetterHotbar() {
      super("BetterHotbar", "Some renders for hotbar", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static BetterHotbar ry() {
      return INSTANCE;
   }

   @EventListener
   public void rz(SelectedSlotXEvent selectedSlotXEvent) {
      if ((Boolean)this.animation.atx()) {
         selectedSlotXEvent.aim();
         double d = this.unidentifiedHelper9_1.MH(selectedSlotXEvent.nB(), ((Integer)this.length.atx()).intValue(), (Easing)this.easing.atx());
         DrawContext temp = selectedSlotXEvent.getContext();
         Identifier temp2 = selectedSlotXEvent.nx();
         int temp3 = (int)d;
         int temp4 = selectedSlotXEvent.nA();
         int n = selectedSlotXEvent.ny();
         int n2 = selectedSlotXEvent.nz();
         int n3 = temp4;
         int n4 = temp3;
         Identifier id = temp2;
         temp.drawGuiTexture(id, n4, n3, n2, n);
      }
   }
}
