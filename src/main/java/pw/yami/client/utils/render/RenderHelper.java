package pw.yami.client.utils.render;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import pw.yami.client.gui.CategoryPanel;
import pw.yami.client.manager.Managers;
import pw.yami.client.manager.RenderUtils;
import pw.yami.client.manager.SoundManager;
import pw.yami.client.model.render.AstolfoMioMode;
import pw.yami.client.module.client.UI;
import pw.yami.client.render.DrawBase;
import pw.yami.client.setting.Setting;

public class RenderHelper extends DrawBase {
   public RenderHelper(CategoryPanel categoryPanel, DrawMatrixHelper drawMatrixHelper, Setting setting2, float f, float f2) {
      super(categoryPanel, drawMatrixHelper, setting2, f, f2);
   }

   @Override
   public void Cs(DrawContext context, float f, float f2, int n, int n2) {
      this.vy(RenderUtils.acG() + UI.atK().atT());
      this.j = f;
      this.k = f2;
      boolean flag = this.vC(n, n2);
      if (UI.aum() == AstolfoMioMode.YamiV2) {
         boolean enabled = (Boolean)this.setting.atx();
         int nAccent = UI.atK().atW(this.k);
         if (flag) {
            vi(context, f, f2, this.l, this.m, new Color(255, 255, 255, 12).getRGB());
         }
         int n3 = UI.auf() ? -16777216 : (enabled ? -1 : new Color(175, 180, 195).getRGB());
         this.ve(context, vf(this.setting.atu()), f + 4.0F, f2 + UI.atK().atU(), n3);

         // Modern toggle switch on right
         float switchWidth = 14.0F;
         float switchHeight = 7.0F;
         float switchX = f + this.l - switchWidth - 4.0F;
         float switchY = f2 + (this.m - switchHeight) / 2.0F;

         int trackColor = enabled ? ColorUtils.iG(nAccent, 200) : new Color(36, 40, 56).getRGB();
         vi(context, switchX, switchY, switchWidth, switchHeight, trackColor);

         float thumbSize = 5.0F;
         float thumbX = enabled ? (switchX + switchWidth - thumbSize - 1.0F) : (switchX + 1.0F);
         float thumbY = switchY + 1.0F;
         int thumbColor = enabled ? -1 : new Color(160, 165, 180).getRGB();
         vi(context, thumbX, thumbY, thumbSize, thumbSize, thumbColor);
         return;
      }
      this.vg(context, this.jj((Boolean)this.setting.atx(), flag).getRGB());
      int n3 = UI.auf() ? -16777216 : -1;
      this.ve(context, vf(this.setting.atu()), f + 2.0F, f2 + UI.atK().atU(), n3);
   }

   @Override
   public void UU(int n, int n2, int n3) {
      if (this.vC(n, n2) && n3 == 0) {
         boolean flag = (Boolean)this.setting.atx();
         this.setting.aty(!flag);
         if (UI.atK().auc()) {
            Managers.soundManager.jg(SoundManager.a);
         }
      }
   }

   @Override
   public void UV() {
   }

   @Override
   public void UW(int n) {
   }

   @Override
   public void UX(char c) {
   }
}
