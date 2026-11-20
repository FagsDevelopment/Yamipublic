package pw.yami.client.module.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import pw.yami.client.event.render.RenderEvent;
import pw.yami.client.event.render.RenderWorld_2Event;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.render.FramebufferFramebuffer3;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class MotionBlur extends ClientModule {
   private static MotionBlur INSTANCE;
   Setting strength = this.registerSetting(new NumberSetting("Strength", "Blur accumulation strength", 1.0F, 5.0F, 9.5F));
   Setting hudBlur = this.registerSetting(new BooleanSetting("HUDBlur", "Apply motion blur to HUD/GUI elements as well", false));

   private Framebuffer blurBuffer;

   public MotionBlur() {
      super("MotionBlur", "Smooth cinematic motion blur using accumulation buffer", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static MotionBlur getInstance() {
      return INSTANCE;
   }

   @Override
   public void awe() {
      super.awe();
      if (this.blurBuffer != null) {
         this.blurBuffer.delete();
         this.blurBuffer = null;
      }
   }

   @EventListener
   public void onRenderWorld_2(RenderWorld_2Event event) {
      if (!(Boolean)this.hudBlur.atx()) {
         this.applyMotionBlur();
      }
   }

   @EventListener
   public void onRender(RenderEvent event) {
      if ((Boolean)this.hudBlur.atx()) {
         this.applyMotionBlur();
      }
   }

   private void applyMotionBlur() {
      if (client.world == null || client.player == null) return;

      Framebuffer mainFbo = client.getFramebuffer();
      if (mainFbo == null) return;

      int width = mainFbo.textureWidth;
      int height = mainFbo.textureHeight;
      if (width <= 0 || height <= 0) return;

      if (this.blurBuffer == null) {
         this.blurBuffer = new FramebufferFramebuffer3();
      }

      if (this.blurBuffer.textureWidth != width || this.blurBuffer.textureHeight != height) {
         this.blurBuffer.resize(width, height, false);
         copyFramebuffer(mainFbo, this.blurBuffer, width, height);
         return;
      }

      float strengthVal = ((Number)this.strength.atx()).floatValue();
      float alpha = strengthVal / 10.0F;
      if (alpha < 0.05F) return;
      if (alpha > 0.95F) alpha = 0.95F;

      // Blend previous accumulated frame over current frame with constant alpha
      mainFbo.beginWrite(false);
      RenderSystem.enableBlend();
      GL14.glBlendColor(0.0F, 0.0F, 0.0F, alpha);
      GL14.glBlendFunc(GL14.GL_CONSTANT_ALPHA, GL14.GL_ONE_MINUS_CONSTANT_ALPHA);
      RenderSystem.backupProjectionMatrix();
      this.blurBuffer.draw(width, height, false);
      RenderSystem.restoreProjectionMatrix();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();

      // Copy resulting blended frame to accumulation buffer for next frame
      copyFramebuffer(mainFbo, this.blurBuffer, width, height);

      mainFbo.beginWrite(false);
   }

   private void copyFramebuffer(Framebuffer src, Framebuffer dst, int width, int height) {
      GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src.fbo);
      GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst.fbo);
      GlStateManager._glBlitFrameBuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
      GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, src.fbo);
   }
}