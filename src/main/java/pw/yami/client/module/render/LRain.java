package pw.yami.client.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.event.network.ChannelRead0Event;
import pw.yami.client.event.player.UpdateDeathEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.ColorSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.render.Render3DContext;
import pw.yami.eventbus.annotation.EventListener;

public class LRain extends ClientModule {
   public Setting duration = this.registerSetting(new NumberSetting("Duration", "How long L's rain in ms", 1000, 3500, 10000));
   public Setting count = this.registerSetting(new NumberSetting("Count", "Number of L letters", 5, 25, 60));
   public Setting scale = this.registerSetting(new NumberSetting("Scale", "Size of L letters", 0.2F, 0.65F, 2.0F));
   public Setting speed = this.registerSetting(new NumberSetting("Speed", "Fall speed multiplier", 0.2F, 1.2F, 4.0F));
   public Setting spread = this.registerSetting(new NumberSetting("Spread", "Horizontal spread radius", 0.5F, 2.5F, 8.0F));
   public Setting outline = this.registerSetting(new BooleanSetting("Outline", "Thick black outline around letters", true));
   public Setting outlineWidth = this.registerSetting(new NumberSetting("Outline Width", "Black border thickness", 0.02F, 0.05F, 0.15F, this::isOutlineVisible));
   public Setting gradient = this.registerSetting(new BooleanSetting("Gradient", "Vertical color gradient", true));
   public Setting rainbow = this.registerSetting(new BooleanSetting("Rainbow", "Rainbow colored gradient L's", true));
   public Setting topColor = this.registerSetting(new ColorSetting("Top Color", "Top gradient color", new Color(255, 60, 100), this::isColorVisible));
   public Setting bottomColor = this.registerSetting(new ColorSetting("Bottom Color", "Bottom gradient color", new Color(170, 30, 255), this::isColorVisible));
   public Setting ignoreSelf = this.registerSetting(new BooleanSetting("Ignore Self", "Don't trigger on own death", true));
   public Setting test = this.registerSetting(new BooleanSetting("Test", "Spawn L rain around you now", false));

   private final List<LRainEvent> events = new ArrayList<>();
   private final Set<Integer> recentDeaths = new HashSet<>();
   private final Random random = new Random();

   public LRain() {
      super("LRain", "Rains L letters on killed players", ModuleCategory.RENDER);
   }

   private Boolean isOutlineVisible() {
      return (Boolean)this.outline.atx();
   }

   private Boolean isColorVisible() {
      return !(Boolean)this.rainbow.atx();
   }

   @Override
   public void awe() {
      synchronized (this.events) {
         this.events.clear();
      }
      this.recentDeaths.clear();
   }

   public void triggerAt(double x, double y, double z) {
      int letterCount = ((Number)this.count.atx()).intValue();
      float spreadRadius = ((Number)this.spread.atx()).floatValue();
      synchronized (this.events) {
         this.events.add(new LRainEvent(x, y + 2.5, z, letterCount, spreadRadius, System.currentTimeMillis()));
      }
   }

   @EventListener
   public void onTick(TickPreEvent event) {
      if ((Boolean)this.test.atx()) {
         this.test.aty(false);
         if (client.player != null) {
            this.triggerAt(client.player.getX(), client.player.getY(), client.player.getZ());
         }
      }

      if (client.world != null) {
         for (Entity entity : client.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity != client.player) {
               PlayerEntity p = (PlayerEntity)entity;
               if (p.isDead() || p.getHealth() <= 0.0F || p.deathTime > 0) {
                  if (this.recentDeaths.add(p.getId())) {
                     this.triggerAt(p.getX(), p.getY(), p.getZ());
                  }
               } else {
                  this.recentDeaths.remove(p.getId());
               }
            }
         }
      }
   }

   @EventListener
   public void onDeath(UpdateDeathEvent updateDeathEvent) {
      LivingEntity living = updateDeathEvent.getLiving();
      if (!(living instanceof PlayerEntity)) return;
      if ((Boolean)this.ignoreSelf.atx() && living == client.player) return;

      this.triggerAt(living.getX(), living.getY(), living.getZ());
   }

   @EventListener
   public void onPacket(ChannelRead0Event event) {
      Packet packet = event.getPacket();
      if (packet instanceof EntityStatusS2CPacket) {
         EntityStatusS2CPacket p = (EntityStatusS2CPacket)packet;
         if (p.getStatus() == 3 && client.world != null) {
            Entity ent = p.getEntity(client.world);
            if (ent instanceof PlayerEntity) {
               if ((Boolean)this.ignoreSelf.atx() && ent == client.player) return;
               this.triggerAt(ent.getX(), ent.getY(), ent.getZ());
            }
         }
      }
   }

   @EventListener(priority = -1)
   public void onRender3D(Render3DContext render3DContext) {
      if (this.events.isEmpty() || client.gameRenderer == null) return;

      Camera camera = client.gameRenderer.getCamera();
      Vec3d camPos = camera.getPos();
      long now = System.currentTimeMillis();
      long durationMs = ((Number)this.duration.atx()).longValue();
      float fallSpeed = ((Number)this.speed.atx()).floatValue();
      float baseScale = ((Number)this.scale.atx()).floatValue();
      boolean isRainbow = (Boolean)this.rainbow.atx();
      boolean useGradient = (Boolean)this.gradient.atx();
      boolean hasOutline = (Boolean)this.outline.atx();
      float outWidth = ((Number)this.outlineWidth.atx()).floatValue();

      Color colTopStatic = (Color)this.topColor.atx();
      Color colBotStatic = (Color)this.bottomColor.atx();

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);

      synchronized (this.events) {
         Iterator<LRainEvent> it = this.events.iterator();
         while (it.hasNext()) {
            LRainEvent ev = it.next();
            long elapsed = now - ev.startTime;
            if (elapsed >= durationMs) {
               it.remove();
               continue;
            }

            float progress = (float)elapsed / (float)durationMs;
            float alpha = MathHelper.clamp(1.0F - progress, 0.0F, 1.0F);
            int a = (int)(alpha * 255.0F);
            int outlineA = (int)(alpha * 240.0F);

            for (int i = 0; i < ev.letters.length; i++) {
               LLetter letter = ev.letters[i];
               float elapsedSec = elapsed / 1000.0F;

               double lx = ev.x + letter.offsetX;
               double ly = ev.y - (elapsedSec * fallSpeed * letter.fallMultiplier) + letter.offsetY;
               double lz = ev.z + letter.offsetZ;

               float rot = letter.rotationSpeed * elapsedSec * 360.0F;

               // Compute gradient colors
               int rTop, gTop, bTop;
               int rBot, gBot, bBot;

               if (isRainbow) {
                  float hueTop = (float)((now / 8L + i * 25L) % 360L) / 360.0F;
                  float hueBot = (float)((now / 8L + i * 25L + 55L) % 360L) / 360.0F;
                  Color c1 = Color.getHSBColor(hueTop, 0.85F, 1.0F);
                  Color c2 = Color.getHSBColor(hueBot, 0.90F, 1.0F);
                  rTop = c1.getRed(); gTop = c1.getGreen(); bTop = c1.getBlue();
                  rBot = c2.getRed(); gBot = c2.getGreen(); bBot = c2.getBlue();
               } else if (useGradient) {
                  rTop = colTopStatic.getRed(); gTop = colTopStatic.getGreen(); bTop = colTopStatic.getBlue();
                  rBot = colBotStatic.getRed(); gBot = colBotStatic.getGreen(); bBot = colBotStatic.getBlue();
               } else {
                  rTop = colTopStatic.getRed(); gTop = colTopStatic.getGreen(); bTop = colTopStatic.getBlue();
                  rBot = rTop; gBot = gTop; bBot = bTop;
               }

               // Mid color for the horizontal foot top edge
               int rMid = (int)(rBot * 0.77F + rTop * 0.23F);
               int gMid = (int)(gBot * 0.77F + gTop * 0.23F);
               int bMid = (int)(bBot * 0.77F + bTop * 0.23F);

               render3DContext.matrices.push();
               render3DContext.matrices.translate(lx - camPos.x, ly - camPos.y, lz - camPos.z);
               render3DContext.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
               render3DContext.matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
               render3DContext.matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));

               float s = baseScale * letter.scaleMultiplier;
               render3DContext.matrices.scale(s, s, s);

               Matrix4f matrix = render3DContext.matrices.peek().getPositionMatrix();

               BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

               // 1. Black Outline (slightly behind at z = -0.002F)
               if (hasOutline) {
                  float w = outWidth;

                  // Outline Vertical stem
                  buf.vertex(matrix, -0.22F - w,  0.35F + w, -0.002F).color(0, 0, 0, outlineA);
                  buf.vertex(matrix, -0.22F - w, -0.35F - w, -0.002F).color(0, 0, 0, outlineA);
                  buf.vertex(matrix, -0.06F + w, -0.35F - w, -0.002F).color(0, 0, 0, outlineA);
                  buf.vertex(matrix, -0.06F + w,  0.35F + w, -0.002F).color(0, 0, 0, outlineA);

                  // Outline Horizontal foot
                  buf.vertex(matrix, -0.06F,     -0.19F + w, -0.002F).color(0, 0, 0, outlineA);
                  buf.vertex(matrix, -0.06F,     -0.35F - w, -0.002F).color(0, 0, 0, outlineA);
                  buf.vertex(matrix,  0.22F + w, -0.35F - w, -0.002F).color(0, 0, 0, outlineA);
                  buf.vertex(matrix,  0.22F + w, -0.19F + w, -0.002F).color(0, 0, 0, outlineA);
               }

               // 2. Inner Gradient "L" (slightly in front at z = 0.002F)
               // Vertical bar with smooth vertical gradient from rTop to rBot
               buf.vertex(matrix, -0.22F,  0.35F, 0.002F).color(rTop, gTop, bTop, a);
               buf.vertex(matrix, -0.22F, -0.35F, 0.002F).color(rBot, gBot, bBot, a);
               buf.vertex(matrix, -0.06F, -0.35F, 0.002F).color(rBot, gBot, bBot, a);
               buf.vertex(matrix, -0.06F,  0.35F, 0.002F).color(rTop, gTop, bTop, a);

               // Horizontal foot with smooth vertical gradient from rMid to rBot
               buf.vertex(matrix, -0.06F, -0.19F, 0.002F).color(rMid, gMid, bMid, a);
               buf.vertex(matrix, -0.06F, -0.35F, 0.002F).color(rBot, gBot, bBot, a);
               buf.vertex(matrix,  0.22F, -0.35F, 0.002F).color(rBot, gBot, bBot, a);
               buf.vertex(matrix,  0.22F, -0.19F, 0.002F).color(rMid, gMid, bMid, a);

               BufferRenderer.drawWithGlobalProgram(buf.end());

               render3DContext.matrices.pop();
            }
         }
      }

      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   private class LRainEvent {
      final double x, y, z;
      final long startTime;
      final LLetter[] letters;

      LRainEvent(double x, double y, double z, int count, float spread, long startTime) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.startTime = startTime;
         this.letters = new LLetter[count];
         for (int i = 0; i < count; i++) {
            this.letters[i] = new LLetter(
               (random.nextFloat() - 0.5F) * 2.0F * spread,
               random.nextFloat() * 2.5F,
               (random.nextFloat() - 0.5F) * 2.0F * spread,
               0.6F + random.nextFloat() * 0.8F,
               0.7F + random.nextFloat() * 0.6F,
               (random.nextFloat() - 0.5F) * 2.5F
            );
         }
      }
   }

   private static class LLetter {
      final float offsetX, offsetY, offsetZ;
      final float fallMultiplier;
      final float scaleMultiplier;
      final float rotationSpeed;

      LLetter(float offsetX, float offsetY, float offsetZ, float fallMultiplier, float scaleMultiplier, float rotationSpeed) {
         this.offsetX = offsetX;
         this.offsetY = offsetY;
         this.offsetZ = offsetZ;
         this.fallMultiplier = fallMultiplier;
         this.scaleMultiplier = scaleMultiplier;
         this.rotationSpeed = rotationSpeed;
      }
   }
}
