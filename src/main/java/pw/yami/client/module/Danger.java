package pw.yami.client.module;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import pw.yami.client.manager.RenderUtils;
import pw.yami.client.model.render.DangerMode;
import pw.yami.client.module.client.HUD;
import pw.yami.client.module.hud.RenderModule;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;

public class Danger extends RenderModule {
   private static final Identifier ISRAEL_TEX = Identifier.of("yamiclient", "textures/davut_star.png");
   private static final Identifier HAZARD_TEX = Identifier.of("yamiclient", "textures/danger_hazard.png");
   private static final Identifier FAGS_TEX = Identifier.of("yamiclient", "textures/fags.png");

   public Setting mode = this.registerSetting(new EnumSetting("Mode", "Texture emblem", DangerMode.Israel, DangerMode.values()));
   public Setting speed = this.registerSetting(new NumberSetting("Speed", "Rotation and orbit speed", 0.5F, 3.0F, 10.0F));
   public Setting scale = this.registerSetting(new NumberSetting("Scale", "Size scale", 0.3F, 1.0F, 2.5F));

   public Danger() {
      super("Danger", "Displays rotating emblem around screen like DCD");
      this.awb();
      this.numberSetting1.aty(20.0F);
      this.numberSetting2.aty(20.0F);
   }

   @Override
   public String Kn() {
      return "Danger";
   }

   @Override
   public void adb() {
   }

   @Override
   public void adc(DrawContext context) {
      DangerMode m = (DangerMode) this.mode.atx();
      Identifier tex;
      if (m == DangerMode.Hazard) {
         tex = HAZARD_TEX;
      } else if (m == DangerMode.Fags) {
         tex = FAGS_TEX;
      } else {
         tex = ISRAEL_TEX;
      }

      float sc = (Float) this.scale.atx();
      if (sc < 0.2F) {
         sc = 1.0F;
      }
      int baseSize = 42;
      int size = (int)(baseSize * sc);

      float spd = (Float) this.speed.atx();
      float time = (float)(System.currentTimeMillis() % 10000000L) * 0.001F * spd;

      int screenW = client.getWindow().getScaledWidth();
      int screenH = client.getWindow().getScaledHeight();
      float marginX = size * 0.8F + 15.0F;
      float marginY = size * 0.8F + 15.0F;
      float rx = Math.max(30.0F, (screenW / 2.0F) - marginX);
      float ry = Math.max(30.0F, (screenH / 2.0F) - marginY);
      float orbitAngle = time * 0.75F;
      float centerX = (screenW / 2.0F) + (float)Math.cos(orbitAngle) * rx;
      float centerY = (screenH / 2.0F) + (float)Math.sin(orbitAngle) * ry;

      float rotDegrees = (time * 120.0F) % 360.0F;

      int n = HUD.zg().zo((centerY + 18.0) / RenderUtils.acG());
      Color color = new Color(n, true);

      context.getMatrices().push();
      context.getMatrices().translate(centerX, centerY, 0.0F);
      context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotDegrees));
      context.getMatrices().translate(-size / 2.0F, -size / 2.0F, 0.0F);

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      GlStateManager._texParameter(3553, 10241, 9729);
      GlStateManager._texParameter(3553, 10240, 9729);

      if (m == DangerMode.Hazard) {
         // 3D hazard symbol: render natural colors
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      } else {
         float rCol = color.getRed() / 255.0F;
         float gCol = color.getGreen() / 255.0F;
         float bCol = color.getBlue() / 255.0F;
         RenderSystem.setShaderColor(rCol, gCol, bCol, 1.0F);
      }

      context.drawTexture(tex, 0, 0, 0.0F, 0.0F, size, size, size, size);

      GlStateManager._texParameter(3553, 10241, 9728);
      GlStateManager._texParameter(3553, 10240, 9728);
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      context.getMatrices().pop();
   }

   @Override
   public float adl() {
      float sc = (Float) this.scale.atx();
      if (sc < 0.2F) {
         sc = 1.0F;
      }
      return super.adl() + 42.0F * sc;
   }

   @Override
   public float adm() {
      float sc = (Float) this.scale.atx();
      if (sc < 0.2F) {
         sc = 1.0F;
      }
      return super.adm() + 42.0F * sc;
   }
}
