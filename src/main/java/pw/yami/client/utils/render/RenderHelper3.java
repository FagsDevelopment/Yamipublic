package pw.yami.client.utils.render;

import java.awt.Color;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pw.yami.client.gui.CategoryPanel;
import pw.yami.client.gui.ScreenDrawScreen;
import pw.yami.client.manager.Managers;
import pw.yami.client.manager.RenderUtils;
import pw.yami.client.manager.SoundManager;
import pw.yami.client.model.render.AstolfoMioMode;
import pw.yami.client.module.client.UI;
import pw.yami.client.render.DrawBase;
import pw.yami.client.setting.BlockListSetting;
import pw.yami.client.setting.Setting;

public class RenderHelper3 extends DrawBase {
   public RenderHelper3(CategoryPanel categoryPanel, DrawMatrixHelper drawMatrixHelper, Setting setting2, float f, float f2) {
      super(categoryPanel, drawMatrixHelper, setting2, f, f2);
   }

   @Override
   public void Cs(DrawContext context, float f, float f2, int n, int n2) {
      this.vy(RenderUtils.acG() + UI.atK().atT());
      this.j = f;
      this.k = f2;
      boolean flag = this.vC(n, n2);
      if (UI.aum() == AstolfoMioMode.YamiV2) {
         int nAccent = UI.atK().atW(this.k);
         if (flag) {
            vi(context, f, f2, this.l, this.m, new Color(255, 255, 255, 12).getRGB());
         }
         this.ve(context, vf(this.setting.atu()), f + 4.0F, f2 + UI.atK().atU(), -1);
         float editW = RenderUtils.acF("edit");
         float badgeX = f + this.l - editW - 8.0F;
         float badgeY = f2 + 1.5F;
         float badgeW = editW + 5.0F;
         float badgeH = this.m - 3.0F;
         vi(context, badgeX, badgeY, badgeW, badgeH, new Color(28, 31, 44, 220).getRGB());
         vi(context, badgeX, badgeY, badgeW, 0.5, ColorUtils.iG(nAccent, 80));
         vi(context, badgeX, badgeY + badgeH - 0.5, badgeW, 0.5, ColorUtils.iG(nAccent, 80));
         this.ve(context, "edit", badgeX + 2.5F, f2 + UI.atK().atU(), nAccent);
         return;
      }
      this.vg(context, this.jj(true, flag).getRGB());
      int n3 = UI.auf() ? -16777216 : -1;
      String str = vf(this.setting.atu());
      this.ve(context, str, f + 2.0F, f2 + UI.atK().atU(), n3);
      this.ve(context, "§7[edit]", f + this.l - 2.0F - RenderUtils.acF("[edit]"), f2 + UI.atK().atU(), n3);
   }

   @Override
   public void UU(int n, int n2, int n3) {
      if (this.vC(n, n2) && n3 == 0) {
         if (UI.atK().auc()) {
            Managers.soundManager.jg(SoundManager.a);
         }

         MinecraftClient temp = client;
         ScreenDrawScreen screenDrawScreen = new ScreenDrawScreen(new DrawBufferHelper((List)this.setting.atx(), this.setting instanceof BlockListSetting));
         temp.setScreen(screenDrawScreen);
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
