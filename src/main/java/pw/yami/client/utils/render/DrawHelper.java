package pw.yami.client.utils.render;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import pw.yami.client.gui.CategoryPanel;
import pw.yami.client.gui.DrawScreenScreen;
import pw.yami.client.manager.Managers;
import pw.yami.client.manager.RenderUtils;
import pw.yami.client.manager.SoundManager;
import pw.yami.client.model.render.AstolfoMioMode;
import pw.yami.client.module.client.UI;
import pw.yami.client.render.DrawBase;
import pw.yami.client.setting.KeybindSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.UnidentifiedHelper17;

public class DrawHelper extends DrawBase {
   private boolean a;

   public DrawHelper(CategoryPanel categoryPanel, DrawMatrixHelper drawMatrixHelper, Setting setting2, float f, float f2) {
      super(categoryPanel, drawMatrixHelper, setting2, f, f2);
   }

   @Override
   public void Cs(DrawContext context, float f, float f2, int n, int n2) {
      this.vy(RenderUtils.acG() + UI.atK().atT());
      this.j = f;
      this.k = f2;
      UnidentifiedHelper17 unidentifiedHelper17 = (UnidentifiedHelper17)this.setting.atx();
      String str = this.a ? "..." : unidentifiedHelper17.agv();
      boolean flag = this.vC(n, n2);
      if (UI.aum() == AstolfoMioMode.YamiV2) {
         int nAccent = UI.atK().atW(this.k);
         if (flag) {
            vi(context, f, f2, this.l, this.m, new Color(255, 255, 255, 12).getRGB());
         }
         FontLoader.fontRenderer3.aaS(context.getMatrices(), "A", f + 3.0F, f2 + UI.atK().atU(), nAccent);
         String str2 = (flag && DrawScreenScreen.j) ? (unidentifiedHelper17.d ? "Hold" : "Toggle") : vf(this.setting.atu());
         this.ve(context, str2, f + 14.0F, f2 + UI.atK().atU(), -1);

         String str3 = str.length() != 1 ? vf(str) : str;
         float f3 = RenderUtils.acF(str3);
         float badgeX = f + this.l - f3 - 7.0F;
         float badgeY = f2 + 1.5F;
         float badgeW = f3 + 5.0F;
         float badgeH = this.m - 3.0F;
         vi(context, badgeX, badgeY, badgeW, badgeH, new Color(28, 31, 44, 220).getRGB());
         vi(context, badgeX, badgeY, badgeW, 0.5, this.a ? nAccent : ColorUtils.iG(nAccent, 80));
         vi(context, badgeX, badgeY + badgeH - 0.5, badgeW, 0.5, this.a ? nAccent : ColorUtils.iG(nAccent, 80));
         this.ve(context, str3, badgeX + 2.5F, f2 + UI.atK().atU(), this.a ? nAccent : -1);
         return;
      }

      this.vg(context, this.jj(false, flag).getRGB());
      int n3 = UI.auf() ? -16777216 : -1;
      FontLoader.fontRenderer3.aaS(context.getMatrices(), "A", f + 2.0F, f2 + UI.atK().atU(), n3);
      String str2;
      if (flag && DrawScreenScreen.j) {
         str2 = unidentifiedHelper17.d ? "Hold" : "Toggle";
      } else {
         str2 = vf(this.setting.atu());
      }

      String str3 = str.length() != 1 ? vf(str) : str;
      float f3 = RenderUtils.acF(str3);
      this.ve(context, str2, f + 13.0F, f2 + UI.atK().atU(), n3);
      this.ve(context, str3, f + this.l - f3 - 2.0F, f2 + UI.atK().atU(), -5592406);
   }

   @Override
   public void UU(int n, int n2, int n3) {
      if (this.a) {
         ((KeybindSetting)this.setting).aT(1000 + n3);
         this.a = false;
      } else if (this.vC(n, n2)) {
         if (this.a) {
            if (n3 == 0) {
               this.a = false;
            }
         } else if (n3 == 0) {
            if (DrawScreenScreen.j) {
               UnidentifiedHelper17 unidentifiedHelper17 = (UnidentifiedHelper17)this.setting.atx();
               unidentifiedHelper17.d = !unidentifiedHelper17.d;
            } else {
               this.a = true;
            }
         } else if (n3 == 2) {
            ((KeybindSetting)this.setting).aT(-1);
         }

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
      if (this.a) {
         if (n != 256 && n != 259 && n != 261) {
            ((KeybindSetting)this.setting).aT(n);
         } else {
            ((KeybindSetting)this.setting).aT(-1);
         }

         this.a = false;
      }
   }

   @Override
   public void UX(char c) {
   }

   public boolean lW() {
      return this.a;
   }

   public void lX() {
      this.a = false;
   }
}
