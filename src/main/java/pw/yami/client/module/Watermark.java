package pw.yami.client.module;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import pw.yami.client.manager.RenderUtils;
import pw.yami.client.module.client.HUD;
import pw.yami.client.module.hud.RenderModule;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;

public class Watermark extends RenderModule {
   private static final Identifier a = Identifier.of("yamiclient", "textures/logo.png");
   Setting booleanSetting1_1 = this.registerSetting(new BooleanSetting("Logo", "Replace watermark with logo", false));
   Setting numberSetting1_1 = this.registerSetting(new NumberSetting("Scale", "", 0.1F, 1.0F, 3.0F, this::isNumberSetting1Visible));
   public String text;

   public Watermark() {
      super("Watermark", "");
      Object[] objs = new Object[]{"Yami", pw.yami.client.Bootstrap.b};
      this.text = String.format("%s §fv%s", objs);
      this.awb();
      this.numberSetting1.aty(0.0F);
      this.numberSetting2.aty(0.0F);
      this.numberSetting1_1.aty(1.0F);
   }

   @Override
   public void adb() {
   }

   @Override
   public void adc(DrawContext context) {
      if ((Boolean)this.booleanSetting1_1.atx()) {
         float sc = (Float)this.numberSetting1_1.atx();
         if (sc < 0.2F) {
            sc = 1.0F;
         }
         int w = (int)(75.0F * sc);
         int h = (int)(27.0F * sc);
         int bx = (int)this.add();
         int by = (int)this.ade();

         int n = HUD.zg().zo((this.ade() + 18.0) / RenderUtils.acG());
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         GlStateManager._texParameter(3553, 10241, 9729);
         GlStateManager._texParameter(3553, 10240, 9729);

         // Subtle black outline behind logo texture
         RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 0.4F);
         context.drawTexture(a, bx - 1, by, 0.0F, 0.0F, w, h, w, h);
         context.drawTexture(a, bx + 1, by, 0.0F, 0.0F, w, h, w, h);
         context.drawTexture(a, bx, by - 1, 0.0F, 0.0F, w, h, w, h);
         context.drawTexture(a, bx, by + 1, 0.0F, 0.0F, w, h, w, h);

         RenderSystem.setShaderColor((n >> 16 & 0xFF) / 255.0F, (n >> 8 & 0xFF) / 255.0F, (n & 0xFF) / 255.0F, 1.0F);
         context.drawTexture(a, bx, by, 0.0F, 0.0F, w, h, w, h);
         GlStateManager._texParameter(3553, 10241, 9728);
         GlStateManager._texParameter(3553, 10240, 9728);
         RenderSystem.disableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      } else {
         this.adq(context, this.text, this.add(), this.ade());
      }
   }

   @Override
   public float adl() {
      float sc = (Float)this.numberSetting1_1.atx();
      if (sc < 0.2F) {
         sc = 1.0F;
      }
      return super.adl() + ((Boolean)this.booleanSetting1_1.atx() ? 75.0F * sc : RenderUtils.acF(this.text));
   }

   @Override
   public float adm() {
      float sc = (Float)this.numberSetting1_1.atx();
      if (sc < 0.2F) {
         sc = 1.0F;
      }
      return super.adm() + ((Boolean)this.booleanSetting1_1.atx() ? 27.0F * sc : RenderUtils.acG());
   }

   private Boolean isNumberSetting1Visible() {
      return (Boolean)this.booleanSetting1_1.atx();
   }
}
