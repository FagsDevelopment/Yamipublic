package pw.yami.client.utils.render;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.ArrayUtils;
import pw.yami.client.manager.Managers;
import pw.yami.client.manager.RenderUtils;
import pw.yami.client.manager.SoundManager;
import pw.yami.client.model.render.AstolfoMioMode;
import pw.yami.client.module.client.UI;
import pw.yami.client.render.DrawBase;
import pw.yami.client.gui.CategoryPanel;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.Stopwatch;
import pw.yami.client.utils.StopwatchImpl;

public class RenderHelper5 extends DrawBase {
   private final Stopwatch stopwatch;
   private char[] b;
   private boolean c;
   private boolean d;

   public RenderHelper5(CategoryPanel categoryPanel, DrawMatrixHelper drawMatrixHelper2, Setting setting2, float f, float f2) {
      super(categoryPanel, drawMatrixHelper2, setting2, f, f2);
      this.stopwatch = new StopwatchImpl();
      this.b = ((String)setting2.atx()).toCharArray();
   }

   @Override
   public void Cs(DrawContext context, float f, float f2, int n, int n2) {
      this.vy(RenderUtils.acG() + UI.atK().atT());
      this.j = f;
      this.k = f2;
      boolean flag = this.vC(n, n2);
      if (UI.aum() == AstolfoMioMode.YamiV2) {
         if (flag) {
            vi(context, f, f2, this.l, this.m, new Color(255, 255, 255, 12).getRGB());
         }
         String str = this.c ? new String(this.b) + this.afH() : this.setting.atu() + Formatting.GRAY + " " + new String(this.b);
         this.ve(context, str, f + 4.0F, f2 + UI.atK().atU(), -1);
         return;
      }
      int n3 = UI.auf() ? -16777216 : -1;
      this.vg(context, this.jj(false, flag).getRGB());
      String str = this.c ? new String(this.b) + this.afH() : this.setting.atu() + Formatting.GRAY + " " + new String(this.b);
      this.ve(context, str, f + 2.0F, f2 + UI.atK().atU(), n3);
   }

   @Override
   public void UU(int n, int n2, int n3) {
      if (this.vC(n, n2)) {
         if (n3 == 0) {
            if (this.c) {
               this.setting.aty(new String(this.b));
               this.c = false;
            } else {
               this.c = true;
            }

            if (UI.atK().auc()) {
               Managers.soundManager.jg(SoundManager.a);
            }
         }
      } else {
         this.afF();
      }
   }

   @Override
   public void UV() {
   }

   @Override
   public void UW(int n) {
      if (this.c) {
         switch (n) {
            case 256:
               this.b = ((String)this.setting.atx()).toCharArray();
               this.c = false;
               break;
            case 257:
               this.setting.aty(new String(this.b));
               this.c = false;
            case 258:
            default:
               break;
            case 259:
               if (this.b.length != 0) {
                  this.b = ArrayUtils.remove(this.b, this.b.length - 1);
               }
         }
      }
   }

   @Override
   public void UX(char c2) {
      if (this.c) {
         this.b = ArrayUtils.add(this.b, c2);
      }
   }

   public void afF() {
      this.c = false;
   }

   public boolean afG() {
      return this.c;
   }

   public String afH() {
      if (this.stopwatch.afq(250)) {
         this.d = !this.d;
         this.stopwatch.afr(29432, 5265492, (byte)40);
      }

      return this.d && this.c ? "_" : "";
   }
}
