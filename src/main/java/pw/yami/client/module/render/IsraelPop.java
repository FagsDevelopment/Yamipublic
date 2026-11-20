package pw.yami.client.module.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import pw.yami.client.event.network.ChannelRead0Event;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.render.Render3DContext;
import pw.yami.eventbus.annotation.EventListener;

public class IsraelPop extends ClientModule {
   private static final Identifier TEXTURE = Identifier.of("yamiclient", "textures/israelpop.png");

   public Setting fadeInTime = this.registerSetting(new NumberSetting("Fade In Time", "Fade in duration in ms", 50, 200, 1000));
   public Setting stayTime = this.registerSetting(new NumberSetting("Stay Time", "Full opacity duration in ms", 200, 1000, 3000));
   public Setting fadeTime = this.registerSetting(new NumberSetting("Fade Time", "Fade out duration in ms", 100, 500, 2000));
   public Setting scale = this.registerSetting(new NumberSetting("Scale", "Size of the popup image", 0.2F, 0.9F, 2.5F));
   public Setting yOffset = this.registerSetting(new NumberSetting("Y-Offset", "Offset above head", 0.0F, 0.6F, 2.0F));
   public Setting self = this.registerSetting(new BooleanSetting("Self", "Trigger on self pops", false));

   private final List<PopData> pops = new ArrayList<>();

   public IsraelPop() {
      super("Israel Pop", "Renders funny Israel pop meme over player heads on totem pop", ModuleCategory.RENDER);
   }

   @Override
   public String Kn() {
      return "Israel Pop";
   }

   @Override
   public void awe() {
      synchronized (this.pops) {
         this.pops.clear();
      }
   }

   public void triggerPop(PlayerEntity player) {
      if (player == null) return;
      if (!(Boolean)this.self.atx() && player == client.player) return;
      synchronized (this.pops) {
         this.pops.add(new PopData(player));
      }
   }

   @EventListener
   public void onPacket(ChannelRead0Event event) {
      Packet packet = event.getPacket();
      if (packet instanceof EntityStatusS2CPacket && ((EntityStatusS2CPacket)packet).getStatus() == 35) {
         if (client.world != null) {
            Entity entity = ((EntityStatusS2CPacket)packet).getEntity(client.world);
            if (entity == null && pw.yami.client.module.misc.FakePlayer.INSTANCE != null) {
               pw.yami.client.player.PlayerWorldOtherClientPlayerEntity fp = pw.yami.client.module.misc.FakePlayer.INSTANCE.getFakePlayer();
               if (fp != null) {
                  entity = fp;
               }
            }
            if (entity instanceof PlayerEntity) {
               this.triggerPop((PlayerEntity)entity);
            }
         }
      }
   }

   @EventListener(priority = -1)
   public void onRender3D(Render3DContext render3DContext) {
      if (this.pops.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      long fadeInMs = ((Number)this.fadeInTime.atx()).longValue();
      long stayMs = ((Number)this.stayTime.atx()).longValue();
      long fadeMs = ((Number)this.fadeTime.atx()).longValue();
      long totalMs = fadeInMs + stayMs + fadeMs;
      float sc = ((Number)this.scale.atx()).floatValue();
      float yOff = ((Number)this.yOffset.atx()).floatValue();

      synchronized (this.pops) {
         this.pops.removeIf(pop -> {
            long elapsed = now - pop.startTime;
            if (elapsed >= totalMs) {
               return true;
            }

            float alpha;
            float currentScale = sc;
            float floatOffset = 0.0F;

            if (elapsed < fadeInMs) {
               float inProgress = (float)elapsed / (float)fadeInMs;
               alpha = MathHelper.clamp(inProgress, 0.0F, 1.0F);
               float sFactor = (float)Math.sin(inProgress * Math.PI / 2.0);
               currentScale = sc * (0.35F + 0.65F * sFactor);
            } else if (elapsed <= (fadeInMs + stayMs)) {
               alpha = 1.0F;
               currentScale = sc;
            } else {
               float fadeProgress = (float)(elapsed - (fadeInMs + stayMs)) / (float)fadeMs;
               alpha = MathHelper.clamp(1.0F - fadeProgress, 0.0F, 1.0F);
               floatOffset = fadeProgress * 0.35F;
               currentScale = sc;
            }

            pop.render(render3DContext, currentScale, yOff + floatOffset, alpha);
            return false;
         });
      }
   }

   private static class PopData {
      private final PlayerEntity player;
      private double lastX, lastY, lastZ;
      private final long startTime;

      public PopData(PlayerEntity player) {
         this.player = player;
         this.lastX = player.getX();
         this.lastY = player.getY() + player.getHeight();
         this.lastZ = player.getZ();
         this.startTime = System.currentTimeMillis();
      }

      public void render(Render3DContext context, float scale, float yOffset, float alpha) {
         if (this.player.isAlive() && this.player.getWorld() != null) {
            this.lastX = this.player.getX();
            this.lastY = this.player.getY() + this.player.getHeight();
            this.lastZ = this.player.getZ();
         }

         Camera camera = client.gameRenderer.getCamera();
         Vec3d camPos = camera.getPos();

         context.matrices.push();
         context.matrices.translate(this.lastX - camPos.x, this.lastY + yOffset - camPos.y, this.lastZ - camPos.z);
         context.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
         context.matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

         float halfW = 0.45F * scale * 1.294F;
         float halfH = 0.45F * scale;

         RenderSystem.setShaderTexture(0, TEXTURE);
         GlStateManager._texParameter(3553, 10241, 9729); // GL_LINEAR
         GlStateManager._texParameter(3553, 10240, 9729);
         RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);

         int colorAlpha = MathHelper.clamp((int)(alpha * 255.0F), 0, 255);
         int argb = (colorAlpha << 24) | 0x00FFFFFF;

         Matrix4f matrix4f = context.matrices.peek().getPositionMatrix();
         BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         bufferBuilder.vertex(matrix4f, -halfW, halfH, 0.0F).texture(0.0F, 0.0F).color(argb);
         bufferBuilder.vertex(matrix4f, -halfW, -halfH, 0.0F).texture(0.0F, 1.0F).color(argb);
         bufferBuilder.vertex(matrix4f, halfW, -halfH, 0.0F).texture(1.0F, 1.0F).color(argb);
         bufferBuilder.vertex(matrix4f, halfW, halfH, 0.0F).texture(1.0F, 0.0F).color(argb);
         BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         context.matrices.pop();
      }
   }
}
