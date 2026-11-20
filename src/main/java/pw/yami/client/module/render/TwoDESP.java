package pw.yami.client.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Iterator;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pw.yami.client.manager.Managers;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ColorHelper;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.ColorSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.render.Render3DContext;
import pw.yami.client.utils.world.EntityBoxUtils;
import pw.yami.eventbus.annotation.EventListener;

public class TwoDESP extends ClientModule {
   public enum Mode {
      Corners,
      Box
   }

   public enum Shape {
      Outline,
      Fill,
      Both
   }

   public enum ColorMode {
      Static,
      Gradient,
      Rainbow,
      Astolfo,
      Health,
      Cyberpunk,
      Fire,
      Matrix,
      Galactic,
      Sunset,
      Vaporwave
   }

   private static TwoDESP INSTANCE;
   Setting mode = this.registerSetting(new EnumSetting("Mode", "Render style of the 2D box", Mode.Corners, Mode.values()));
   Setting shape = this.registerSetting(new EnumSetting("Shape", "Box fill mode", Shape.Both, Shape.values()));
   Setting colorMode = this.registerSetting(new EnumSetting("ColorMode", "Color animation and theme style", ColorMode.Rainbow, ColorMode.values()));
   Setting color = this.registerSetting(new ColorSetting("Color", "Main box color", new Color(0, 220, 255, 255), this::isColorVisible));
   Setting colorBottom = this.registerSetting(new ColorSetting("ColorBottom", "Bottom gradient color", new Color(255, 0, 128, 255), this::isColorBottomVisible));
   Setting colorSpeed = this.registerSetting(new NumberSetting("ColorSpeed", "Color animation speed", 0.2F, 2.0F, 8.0F, this::isAnimatedColor));
   Setting saturation = this.registerSetting(new NumberSetting("Saturation", "Color saturation", 0.1F, 0.85F, 1.0F, this::isAnimatedColor));
   Setting brightness = this.registerSetting(new NumberSetting("Brightness", "Color brightness", 0.1F, 1.0F, 1.0F, this::isAnimatedColor));
   Setting fillOpacity = this.registerSetting(new NumberSetting("FillOpacity", "Opacity of the inner box fill", 0.0F, 0.22F, 1.0F, this::isFillVisible));
   Setting padding = this.registerSetting(new NumberSetting("Padding", "Hitbox padding around entity", 0.0F, 0.04F, 0.20F));
   Setting thickness = this.registerSetting(new NumberSetting("Thickness", "Line thickness", 0.01F, 0.025F, 0.08F));
   Setting cornerLength = this.registerSetting(new NumberSetting("CornerLength", "Length of corner brackets", 0.10F, 0.25F, 0.50F, this::isCornerLengthVisible));
   Setting range = this.registerSetting(new NumberSetting("Range", "Render range", 10.0F, 64.0F, 256.0F));
   Setting healthBar = this.registerSetting(new BooleanSetting("HealthBar", "Render dynamic health bar on the left", true));
   Setting armorBar = this.registerSetting(new BooleanSetting("ArmorBar", "Render armor bar on the bottom", true));
   Setting throughWalls = this.registerSetting(new BooleanSetting("ThroughWalls", "Render through walls (X-Ray)", true));
   Setting players = this.registerSetting(new BooleanSetting("Players", "Render on players", true));
   Setting self = this.registerSetting(new BooleanSetting("Self", "Render on yourself in third person", true));
   Setting monsters = this.registerSetting(new BooleanSetting("Monsters", "Render on hostile mobs", false));
   Setting animals = this.registerSetting(new BooleanSetting("Animals", "Render on passive mobs", false));
   Setting friends = this.registerSetting(new BooleanSetting("Friends", "Use friend color for friends", true));

   public TwoDESP() {
      super("2DESP", "Renders precise hitbox-aligned 2D bounding boxes and health bars around entities", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static TwoDESP getInstance() {
      return INSTANCE;
   }

   private Boolean isColorVisible() {
      ColorMode cm = (ColorMode) this.colorMode.atx();
      return cm == ColorMode.Static || cm == ColorMode.Gradient;
   }

   private Boolean isColorBottomVisible() {
      return this.colorMode.atx() == ColorMode.Gradient;
   }

   private Boolean isAnimatedColor() {
      ColorMode cm = (ColorMode) this.colorMode.atx();
      return cm != ColorMode.Static && cm != ColorMode.Gradient && cm != ColorMode.Health;
   }

   private Boolean isFillVisible() {
      return this.shape.atx() != Shape.Outline;
   }

   private Boolean isCornerLengthVisible() {
      return this.mode.atx() == Mode.Corners;
   }

   @EventListener(priority = -2)
   public void onRender3D(Render3DContext render3DContext) {
      if (client.world == null || client.player == null || client.gameRenderer == null) return;

      Camera camera = client.gameRenderer.getCamera();
      Vec3d camPos = camera.getPos();
      float maxRange = ((Number)this.range.atx()).floatValue();
      float maxRangeSq = maxRange * maxRange;

      float thick = ((Number)this.thickness.atx()).floatValue();
      float cRatio = ((Number)this.cornerLength.atx()).floatValue();
      float pad = ((Number)this.padding.atx()).floatValue();
      Mode renderMode = (Mode)this.mode.atx();
      Shape fillMode = (Shape)this.shape.atx();
      boolean showHealth = (Boolean)this.healthBar.atx();
      boolean showArmor = (Boolean)this.armorBar.atx();
      boolean seeThrough = (Boolean)this.throughWalls.atx();

      float yaw = camera.getYaw();
      float pitch = camera.getPitch();

      // Camera inverse rotation quaternion: transforms world-relative vectors into billboard plane coordinates
      Quaternionf invRot = new Quaternionf()
         .rotationY((float) Math.toRadians(-yaw))
         .rotateX((float) Math.toRadians(pitch))
         .conjugate();

      // Forward gaze vector for depth culling (entities behind camera)
      float yawRad = (float) Math.toRadians(yaw);
      float pitchRad = (float) Math.toRadians(pitch);
      float fx = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
      float fy = -MathHelper.sin(pitchRad);
      float fz = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);

      Vector3f tempVec = new Vector3f();
      Iterator<Entity> entities = client.world.getEntities().iterator();
      int entityIndex = 0;

      while (entities.hasNext()) {
         Entity entity = entities.next();
         if (entity == null || !entity.isAlive()) continue;

         if (entity instanceof PlayerEntity player) {
            if (player == client.player) {
               if (!(Boolean)this.self.atx() || client.options.getPerspective().isFirstPerson()) {
                  continue;
               }
            } else if (!(Boolean)this.players.atx()) {
               continue;
            }
         } else if (entity instanceof Monster) {
            if (!(Boolean)this.monsters.atx()) continue;
         } else if (entity instanceof AnimalEntity) {
            if (!(Boolean)this.animals.atx()) continue;
         } else {
            continue;
         }

         // Get exact interpolated 3D bounding box
         Box box = entity.getBoundingBox();
         if (box == null) continue;
         try {
            box = EntityBoxUtils.BE(entity);
         } catch (Exception ignored) {}

         double cX = (box.minX + box.maxX) * 0.5;
         double cY = (box.minY + box.maxY) * 0.5;
         double cZ = (box.minZ + box.maxZ) * 0.5;

         double transX = cX - camPos.x;
         double transY = cY - camPos.y;
         double transZ = cZ - camPos.z;

         // Check range
         double distSq = transX * transX + transY * transY + transZ * transZ;
         if (distSq > maxRangeSq) continue;

         // Depth culling: entity must be in front of the camera
         double depth = transX * fx + transY * fy + transZ * fz;
         if (depth <= 0.25) continue;

         // 8 Corners of the 3D bounding box relative to center
         double[][] corners = new double[][] {
            {box.minX - cX, box.minY - cY, box.minZ - cZ},
            {box.maxX - cX, box.minY - cY, box.minZ - cZ},
            {box.minX - cX, box.maxY - cY, box.minZ - cZ},
            {box.maxX - cX, box.maxY - cY, box.minZ - cZ},
            {box.minX - cX, box.minY - cY, box.maxZ - cZ},
            {box.maxX - cX, box.minY - cY, box.maxZ - cZ},
            {box.minX - cX, box.maxY - cY, box.maxZ - cZ},
            {box.maxX - cX, box.maxY - cY, box.maxZ - cZ}
         };

         float minX = Float.MAX_VALUE;
         float maxX = -Float.MAX_VALUE;
         float minY = Float.MAX_VALUE;
         float maxY = -Float.MAX_VALUE;

         for (double[] c : corners) {
            tempVec.set((float)c[0], (float)c[1], (float)c[2]);
            invRot.transform(tempVec);

            if (tempVec.x < minX) minX = tempVec.x;
            if (tempVec.x > maxX) maxX = tempVec.x;
            if (tempVec.y < minY) minY = tempVec.y;
            if (tempVec.y > maxY) maxY = tempVec.y;
         }

         // Apply user padding
         minX -= pad;
         maxX += pad;
         minY -= pad;
         maxY += pad;

         // Theme colors for this entity
         Color[] colors = getColors(entity, entityIndex * 0.35F);
         Color topCol = colors[0];
         Color botCol = colors[1];
         entityIndex++;

         // Matrix billboard setup centered at the entity's hitbox center
         render3DContext.matrices.push();
         render3DContext.matrices.translate(transX, transY, transZ);
         render3DContext.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
         render3DContext.matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

         Matrix4f matrix = render3DContext.matrices.peek().getPositionMatrix();

         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);

         if (seeThrough) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
         } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
         }

         BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

         // 1. Fill quad with vertical gradient
         if (fillMode == Shape.Fill || fillMode == Shape.Both) {
            int fillAlpha = Math.round(((Number)this.fillOpacity.atx()).floatValue() * 255.0F);
            if (fillAlpha > 0) {
               drawQuadGradient(matrix, buf, minX, minY, maxX, maxY, topCol, botCol, fillAlpha);
            }
         }

         // 2. Box or Corners outline with vertical gradient and black shadow backing
         if (fillMode == Shape.Outline || fillMode == Shape.Both) {
            if (renderMode == Mode.Box) {
               renderBox(matrix, buf, minX, minY, maxX, maxY, thick, topCol, botCol);
            } else {
               renderCorners(matrix, buf, minX, minY, maxX, maxY, thick, cRatio, topCol, botCol);
            }
         }

         // 3. Health bar on left side
         if (showHealth && entity instanceof LivingEntity living) {
            float hpRatio = MathHelper.clamp(living.getHealth() / living.getMaxHealth(), 0.0F, 1.0F);
            renderHealthBar(matrix, buf, minX - 0.04F, minY, maxY, hpRatio);
         }

         // 4. Armor bar on bottom edge
         if (showArmor && entity instanceof PlayerEntity player) {
            float armorRatio = MathHelper.clamp((float)player.getArmor() / 20.0F, 0.0F, 1.0F);
            if (armorRatio > 0.0F) {
               renderArmorBar(matrix, buf, minX, maxX, minY - 0.04F, armorRatio);
            }
         }

         BufferRenderer.drawWithGlobalProgram(buf.end());

         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();

         render3DContext.matrices.pop();
      }
   }

   private Color[] getColors(Entity entity, float offset) {
      ColorMode cMode = (ColorMode) this.colorMode.atx();
      long now = System.currentTimeMillis();
      float speed = ((Number)this.colorSpeed.atx()).floatValue();
      float time = (now % 1000000L) * 0.001F * speed;
      float sat = ((Number)this.saturation.atx()).floatValue();
      float bri = ((Number)this.brightness.atx()).floatValue();

      Color top;
      Color bot;

      switch (cMode) {
         case Static -> {
            top = (Color) this.color.atx();
            bot = top;
         }
         case Gradient -> {
            top = (Color) this.color.atx();
            bot = (Color) this.colorBottom.atx();
         }
         case Rainbow -> {
            float hueTop = (time * 0.15F + offset * 0.2F) % 1.0F;
            if (hueTop < 0) hueTop += 1.0F;
            float hueBot = (hueTop + 0.14F) % 1.0F;
            top = Color.getHSBColor(hueTop, sat, bri);
            bot = Color.getHSBColor(hueBot, sat, bri);
         }
         case Astolfo -> {
            float waveTop = (float) Math.sin(time * 2.0F + offset);
            float hueTop = 0.52F + 0.38F * (waveTop * 0.5F + 0.5F);
            float waveBot = (float) Math.sin(time * 2.0F + offset + 0.8F);
            float hueBot = 0.52F + 0.38F * (waveBot * 0.5F + 0.5F);
            top = Color.getHSBColor(hueTop, sat * 0.85F, bri);
            bot = Color.getHSBColor(hueBot, sat * 0.85F, bri);
         }
         case Health -> {
            if (entity instanceof LivingEntity living) {
               float hpRatio = MathHelper.clamp(living.getHealth() / living.getMaxHealth(), 0.0F, 1.0F);
               float hue = hpRatio * 0.33F;
               top = Color.getHSBColor(hue, sat, bri);
               bot = Color.getHSBColor(hue, sat, bri * 0.85F);
            } else {
               top = (Color) this.color.atx();
               bot = top;
            }
         }
         case Cyberpunk -> {
            float pulse = (float) Math.sin(time * 3.0F + offset) * 0.15F;
            int rT = MathHelper.clamp((int)(255 * (0.85F + pulse)), 0, 255);
            int bT = MathHelper.clamp((int)(220 * (0.85F + pulse)), 0, 255);
            int gB = MathHelper.clamp((int)(240 * (0.85F - pulse)), 0, 255);
            top = new Color(rT, 0, bT);
            bot = new Color(0, gB, 255);
         }
         case Fire -> {
            float flicker = (float) Math.sin(time * 5.0F + offset * 2.0F) * 0.12F;
            int gT = MathHelper.clamp((int)(210 + flicker * 45), 140, 255);
            int rB = MathHelper.clamp((int)(220 + flicker * 35), 160, 255);
            top = new Color(255, gT, 10);
            bot = new Color(rB, 30, 0);
         }
         case Matrix -> {
            float wave = (float) Math.sin(time * 4.0F + offset) * 0.25F + 0.75F;
            top = new Color(30, MathHelper.clamp((int)(255 * wave), 100, 255), 70);
            bot = new Color(5, MathHelper.clamp((int)(130 * wave), 30, 200), 20);
         }
         case Galactic -> {
            float wave = (float) Math.sin(time * 2.2F + offset) * 0.2F;
            top = new Color(MathHelper.clamp((int)(180 + wave * 50), 100, 255), 40, 255);
            bot = new Color(60, 10, MathHelper.clamp((int)(190 - wave * 50), 80, 255));
         }
         case Sunset -> {
            float wave = (float) Math.sin(time * 2.0F + offset) * 0.2F;
            top = new Color(255, MathHelper.clamp((int)(90 + wave * 40), 50, 160), 120);
            bot = new Color(MathHelper.clamp((int)(130 + wave * 40), 90, 200), 40, 220);
         }
         case Vaporwave -> {
            float wave = (float) Math.sin(time * 2.5F + offset) * 0.2F;
            top = new Color(MathHelper.clamp((int)(230 + wave * 25), 180, 255), 90, 240);
            bot = new Color(40, MathHelper.clamp((int)(230 - wave * 25), 170, 255), 200);
         }
         default -> {
            top = (Color) this.color.atx();
            bot = top;
         }
      }

      if ((Boolean)this.friends.atx() && entity instanceof PlayerEntity player && Managers.friendManager.awt(player.getName())) {
         Color friendCol = ColorHelper.wx().wB();
         top = friendCol;
         bot = friendCol;
      }

      return new Color[]{top, bot};
   }

   private void drawQuadGradient(Matrix4f matrix, BufferBuilder buf, float x1, float y1, float x2, float y2, Color top, Color bot, int alpha) {
      int rT = top.getRed(), gT = top.getGreen(), bT = top.getBlue();
      int rB = bot.getRed(), gB = bot.getGreen(), bB = bot.getBlue();
      buf.vertex(matrix, x1, y1, 0.0F).color(rB, gB, bB, alpha);
      buf.vertex(matrix, x1, y2, 0.0F).color(rT, gT, bT, alpha);
      buf.vertex(matrix, x2, y2, 0.0F).color(rT, gT, bT, alpha);
      buf.vertex(matrix, x2, y1, 0.0F).color(rB, gB, bB, alpha);
   }

   private void drawLineVGradient(Matrix4f matrix, BufferBuilder buf, float x1, float y1, float x2, float y2, float thick, Color top, Color bot, int alpha) {
      float ht = thick * 0.5F;
      if (Math.abs(x1 - x2) < 0.0001F) {
         float minY = Math.min(y1, y2);
         float maxY = Math.max(y1, y2);
         drawQuadGradient(matrix, buf, x1 - ht, minY, x1 + ht, maxY, top, bot, alpha);
      } else {
         float minX = Math.min(x1, x2);
         float maxX = Math.max(x1, x2);
         Color lineCol = (y1 >= (y1 + y2) * 0.5F) ? top : bot;
         drawQuadGradient(matrix, buf, minX, y1 - ht, maxX, y1 + ht, lineCol, lineCol, alpha);
      }
   }

   private void renderBox(Matrix4f matrix, BufferBuilder buf, float x1, float y1, float x2, float y2, float thick, Color top, Color bot) {
      float shadowThick = thick + 0.012F;
      Color black = new Color(0, 0, 0);

      // Shadow backing
      drawLineVGradient(matrix, buf, x1, y1, x2, y1, shadowThick, black, black, 200);
      drawLineVGradient(matrix, buf, x1, y2, x2, y2, shadowThick, black, black, 200);
      drawLineVGradient(matrix, buf, x1, y1, x1, y2, shadowThick, black, black, 200);
      drawLineVGradient(matrix, buf, x2, y1, x2, y2, shadowThick, black, black, 200);

      // Gradient color lines
      drawLineVGradient(matrix, buf, x1, y1, x2, y1, thick, bot, bot, 255);
      drawLineVGradient(matrix, buf, x1, y2, x2, y2, thick, top, top, 255);
      drawLineVGradient(matrix, buf, x1, y1, x1, y2, thick, top, bot, 255);
      drawLineVGradient(matrix, buf, x2, y1, x2, y2, thick, top, bot, 255);
   }

   private void renderCorners(Matrix4f matrix, BufferBuilder buf, float x1, float y1, float x2, float y2, float thick, float cRatio, Color top, Color bot) {
      float w = x2 - x1;
      float h = y2 - y1;
      float cW = w * cRatio;
      float cH = h * cRatio;
      float shadowThick = thick + 0.012F;
      Color black = new Color(0, 0, 0);

      // Shadow corners
      drawLineVGradient(matrix, buf, x1, y2, x1 + cW, y2, shadowThick, black, black, 200);
      drawLineVGradient(matrix, buf, x1, y2, x1, y2 - cH, shadowThick, black, black, 200);
      drawLineVGradient(matrix, buf, x2, y2, x2 - cW, y2, shadowThick, black, black, 200);
      drawLineVGradient(matrix, buf, x2, y2, x2, y2 - cH, shadowThick, black, black, 200);
      drawLineVGradient(matrix, buf, x1, y1, x1 + cW, y1, shadowThick, black, black, 200);
      drawLineVGradient(matrix, buf, x1, y1, x1, y1 + cH, shadowThick, black, black, 200);
      drawLineVGradient(matrix, buf, x2, y1, x2 - cW, y1, shadowThick, black, black, 200);
      drawLineVGradient(matrix, buf, x2, y1, x2, y1 + cH, shadowThick, black, black, 200);

      // Color corners with gradient
      drawLineVGradient(matrix, buf, x1, y2, x1 + cW, y2, thick, top, top, 255);
      drawLineVGradient(matrix, buf, x1, y2, x1, y2 - cH, thick, top, top, 255);
      drawLineVGradient(matrix, buf, x2, y2, x2 - cW, y2, thick, top, top, 255);
      drawLineVGradient(matrix, buf, x2, y2, x2, y2 - cH, thick, top, top, 255);
      drawLineVGradient(matrix, buf, x1, y1, x1 + cW, y1, thick, bot, bot, 255);
      drawLineVGradient(matrix, buf, x1, y1, x1, y1 + cH, thick, bot, bot, 255);
      drawLineVGradient(matrix, buf, x2, y1, x2 - cW, y1, thick, bot, bot, 255);
      drawLineVGradient(matrix, buf, x2, y1, x2, y1 + cH, thick, bot, bot, 255);
   }

   private void renderHealthBar(Matrix4f matrix, BufferBuilder buf, float x, float y1, float y2, float hpRatio) {
      float barW = 0.035F;
      float shadow = 0.008F;

      // Dark background
      drawQuadGradient(matrix, buf, x - barW - shadow, y1 - shadow, x + shadow, y2 + shadow, new Color(15, 15, 15), new Color(15, 15, 15), 200);

      // Dynamic health gradient
      float curY2 = y1 + (y2 - y1) * hpRatio;
      Color hpColor;
      if (hpRatio > 0.65F) {
         hpColor = new Color(40, 240, 60);
      } else if (hpRatio > 0.35F) {
         hpColor = new Color(255, 200, 20);
      } else {
         hpColor = new Color(255, 40, 40);
      }
      Color hpBottomColor = new Color(180, 20, 20);

      drawQuadGradient(matrix, buf, x - barW, y1, x, curY2, hpColor, hpBottomColor, 245);
   }

   private void renderArmorBar(Matrix4f matrix, BufferBuilder buf, float x1, float x2, float y, float armorRatio) {
      float barH = 0.03F;
      float shadow = 0.008F;

      // Dark background
      drawQuadGradient(matrix, buf, x1 - shadow, y - barH - shadow, x2 + shadow, y + shadow, new Color(15, 15, 15), new Color(15, 15, 15), 200);

      // Cyan to Royal Blue armor gradient
      float curX2 = x1 + (x2 - x1) * armorRatio;
      Color armorTop = new Color(0, 230, 255);
      Color armorBot = new Color(0, 140, 255);
      drawQuadGradient(matrix, buf, x1, y - barH, curX2, y, armorTop, armorBot, 245);
   }
}