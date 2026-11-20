package pw.yami.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Iterator;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import pw.yami.client.Bootstrap;
import pw.yami.client.Mc;
import pw.yami.client.manager.Managers;
import pw.yami.client.manager.RenderUtils;
import pw.yami.client.module.ColorHelper3;
import pw.yami.client.module.render.PhantomView;
import pw.yami.client.render.FramebufferFramebuffer;
import pw.yami.client.utils.Stopwatch;
import pw.yami.client.utils.StopwatchImpl;
import pw.yami.client.utils.UnidentifiedUtils2;
import pw.yami.client.render.BufferVertexBase;
import pw.yami.client.utils.render.ColorUtils;
import pw.yami.satin.ManagedShaderEffect;

public class TextDrawScreen extends Screen {
   private static final double a = 1.7777777777777777;
   private static final Identifier b = Identifier.of("yamiclient", "textures/background.png");
   private static final Identifier c = Identifier.of("yamiclient", "textures/logo.png");
   private static final Stopwatch d = new StopwatchImpl();
   public static boolean e;
   private static double f = 0.5;
   private static double g = 0.5;
   private boolean h;
   private long i;
   private long starFadeStart = 0;
   private static boolean musicMuted = loadMuteState();
   private PositionedSoundInstance musicInstance;
   private ButtonWidget muteButton;
   private boolean musicStarted = false;

   public TextDrawScreen(boolean flag) {
      super(Text.of("CustomTitleScreen"));
      this.h = flag;
   }

   public static void ey(int n, int n2, int n3, int n4) {
      if (n != 0 || n2 != 0) {
         double d2;
         if (ColorHelper3.auO().auP()) {
            d2 = 0.01;
         } else {
            d2 = 0.02;
         }

         double d3 = d2;
         f = PhantomView.DH(f, (double)n / n3, d3);
         g = PhantomView.DH(g, (float)n2 / n4, d3);
         f = Math.min(f, 1.0);
         g = Math.min(g, 1.0);
         f = Math.max(f, 0.0);
         g = Math.max(g, 0.0);
      }
   }

   public static void ez(DrawContext context2, float f2, float f3) {
      int w = (int)f2;
      int h = (int)f3;
      Color themeColor = ColorHelper3.auO() != null ? ColorHelper3.auO().auU() : new Color(181, 156, 255);
      int accentR = themeColor.getRed();
      int accentG = themeColor.getGreen();
      int accentB = themeColor.getBlue();

      // Deep dark sleek background gradient (clean, modern, no green tint)
      int topBg = new Color(10, 11, 18).getRGB();
      int midBg = new Color(
         Math.max(0, accentR / 16),
         Math.max(0, accentG / 16),
         Math.max(0, accentB / 16),
         255
      ).getRGB();
      int botBg = new Color(5, 6, 10).getRGB();
      context2.fillGradient(0, 0, w, h / 2, topBg, midBg);
      context2.fillGradient(0, h / 2, w, h, midBg, botBg);

      // Subtle background grid pattern
      int gridSpacing = 32;
      int gridColor = new Color(accentR, accentG, accentB, 10).getRGB();
      for (int gx = 0; gx < w; gx += gridSpacing) {
         context2.fill(gx, 0, gx + 1, h, gridColor);
      }
      for (int gy = 0; gy < h; gy += gridSpacing) {
         context2.fill(0, gy, w, gy + 1, gridColor);
      }

      // Ambient center glow using glow.png (static, no pulsing)
      RenderSystem.enableBlend();
      RenderSystem.setShaderTexture(0, Identifier.of("yamiclient", "textures/glow.png"));
      context2.setShaderColor(accentR / 255.0F, accentG / 255.0F, accentB / 255.0F, 0.12F);
      int glowSize = Math.min(w, h);
      eA(context2, (w - glowSize) / 2.0F, (h - glowSize) / 2.0F, glowSize, glowSize, glowSize, glowSize);
      context2.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();

      // Soft dark vignette around screen edges
      int vigSize = Math.min(w, h) / 3;
      int vigColor = new Color(0, 0, 0, 110).getRGB();
      context2.fillGradient(0, 0, vigSize, vigSize, vigColor, 0);
      context2.fillGradient(w - vigSize, 0, w, vigSize, 0, vigColor);
      context2.fillGradient(0, h - vigSize, vigSize, h, 0, vigColor);
      context2.fillGradient(w - vigSize, h - vigSize, w, h, 0, vigColor);
   }

   public static void eA(DrawContext context2, float f2, float f3, float f4, float f5, float f6, float f7) {
      eB(context2, f2, f3, f4, f5, f4, f5, f6, f7);
   }

   private static void eB(DrawContext context2, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9) {
      eC(context2, f2, f2 + f4, f3, f3 + f5, f6, f7, f8, f9);
   }

   private static void eC(DrawContext context2, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9) {
      eD(context2.getMatrices().peek().getPositionMatrix(), f2, f3, f4, f5, 0.0F / f8, (0.0F + f6) / f8, 0.0F / f9, (0.0F + f7) / f9);
   }

   private static void eD(Matrix4f matrix4f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9) {
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      Tessellator temp = Tessellator.getInstance();
      VertexFormat vertexFormat = VertexFormats.POSITION_TEXTURE;
      DrawMode drawMode = DrawMode.QUADS;
      BufferBuilder bufferBuilder = temp.begin(drawMode, vertexFormat);
      bufferBuilder.vertex(matrix4f, f2, f4, 0.0F).texture(f6, f8);
      bufferBuilder.vertex(matrix4f, f2, f5, 0.0F).texture(f6, f9);
      bufferBuilder.vertex(matrix4f, f3, f5, 0.0F).texture(f7, f9);
      bufferBuilder.vertex(matrix4f, f3, f4, 0.0F).texture(f7, f8);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
   }

   public boolean shouldPause() {
      return false;
   }

   @Override
   public void removed() {
      super.removed();
      stopMusic();
   }

   private static volatile javazoom.jl.player.Player activeAudioPlayer;
   private static Thread activeAudioThread;
   private static volatile boolean activeAudioRunning = false;
   private static final Object AUDIO_LOCK = new Object();

   public static void startMusic() {
      if (musicMuted) return;
      synchronized (AUDIO_LOCK) {
         // Eger sarki zaten caliyorsa kesinlikle ikinci bir thread baslatma
         if (activeAudioThread != null && activeAudioThread.isAlive()) {
            return;
         }
         stopMusicInternal();

         // Minecraft'in varsayilan ana menu muzigini durdur (ust uste calmasin)
         try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getMusicTracker() != null) {
               mc.getMusicTracker().stop();
            }
         } catch (Throwable ignored) {}

         activeAudioRunning = true;
         activeAudioThread = new Thread(() -> {
            while (activeAudioRunning) {
               MinecraftClient mc = MinecraftClient.getInstance();
               if (mc == null || !(mc.currentScreen instanceof TextDrawScreen)) {
                  activeAudioRunning = false;
                  break;
               }
               java.io.InputStream is = null;
               try {
                  java.io.File file = new java.io.File(MP3_PATH);
                  if (file.exists()) {
                     is = new java.io.BufferedInputStream(new java.io.FileInputStream(file));
                  } else {
                     is = TextDrawScreen.class.getResourceAsStream("/assets/yamiclient/sounds/israel.mp3");
                  }
                  if (is == null) {
                     break;
                  }
                  synchronized (AUDIO_LOCK) {
                     if (!activeAudioRunning) {
                        try { is.close(); } catch (Exception ignored) {}
                        break;
                     }
                     activeAudioPlayer = new javazoom.jl.player.Player(is);
                  }
                  activeAudioPlayer.play();
               } catch (Exception ignored) {
               } finally {
                  if (is != null) {
                     try { is.close(); } catch (Exception ignored) {}
                  }
               }
               if (!activeAudioRunning) break;
            }
         }, "Yami-MenuMusic");
         activeAudioThread.setDaemon(true);
         activeAudioThread.start();
      }
   }

   public static void stopMusic() {
      synchronized (AUDIO_LOCK) {
         stopMusicInternal();
      }
   }

   private static void stopMusicInternal() {
      activeAudioRunning = false;
      if (activeAudioPlayer != null) {
         try { activeAudioPlayer.close(); } catch (Exception ignored) {}
         activeAudioPlayer = null;
      }
      if (activeAudioThread != null) {
         activeAudioThread.interrupt();
         try { activeAudioThread.join(300); } catch (InterruptedException ignored) {}
         activeAudioThread = null;
      }
   }

   private void toggleMute() {
      musicMuted = !musicMuted;
      if (musicMuted) {
         stopMusic();
      } else {
         startMusic();
      }
      if (muteButton != null) {
         muteButton.setMessage(Text.of(musicMuted ? "\u266A Muted" : "\u266A Music"));
      }
      saveMuteState(musicMuted);
   }

   private static final String MP3_PATH = "C:\\Users\\imo34\\Desktop\\openyami\\OpenHachimi-main\\src\\main\\resources\\assets\\yamiclient\\sounds\\israel.mp3";

   private static boolean loadMuteState() {
      try {
         java.io.File f = new java.io.File(MinecraftClient.getInstance().runDirectory, "config/yami_menu_music.txt");
         if (f.exists()) {
            java.util.Scanner sc = new java.util.Scanner(f);
            String v = sc.hasNext() ? sc.next() : "false";
            sc.close();
            return Boolean.parseBoolean(v);
         }
      } catch (Exception ignored) {}
      return false;
   }

   private static void saveMuteState(boolean muted) {
      try {
         java.io.File f = new java.io.File(MinecraftClient.getInstance().runDirectory, "config/yami_menu_music.txt");
         f.getParentFile().mkdirs();
         java.io.PrintWriter pw = new java.io.PrintWriter(f);
         pw.print(muted);
         pw.close();
      } catch (Exception ignored) {}
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }


   protected void init() {
      Text text = Text.of(("Minecraft " + SharedConstants.getGameVersion().getName()).toLowerCase());
      int n = RenderUtils.acF(text.getString());
      int n2 = this.width / 2 - 200 / 2;
      int n3 = this.height / 2 - 48 + 30;
      boolean flag = Bootstrap.NY();
      if (flag) {
         try {
            Class.forName("ru.vidtu.ias.screen.AccountScreen");
            n3 += -12;
         } catch (Exception exception) {
            Bootstrap.Ob("Not found IAS");
            flag = false;
         }
      }

      this.addDrawableChild(DrawTextPressableWidget.apm(Text.of("singleplayer"), this::eK).EE("5").EB(n2, n3, 200).EF());
      this.addDrawableChild(DrawTextPressableWidget.apm(Text.of("multiplayer"), this::eJ).EE("4").EB(n2, n3 + 24, 200).EF());
      this.addDrawableChild(DrawTextPressableWidget.apm(Text.of("options"), this::eI).EE("7").EB(n2, n3 + 72, 98).EF());
      this.addDrawableChild(DrawTextPressableWidget.apm(Text.of("quit"), this::eH).EE("X").EB(n2 + 102, n3 + 72, 98).EF());
      if (flag) {
         this.addDrawableChild(DrawTextPressableWidget.apm(Text.of("altmanager"), this::eG).EE("c").EB(n2, n3 + 48, 200).EF());
      }

      int n4 = RenderUtils.acG();
      this.addDrawableChild(new TextDrawButtonWidget(this.height - n4 - 4, n, text, this::eF));

      muteButton = ButtonWidget.builder(Text.of(musicMuted ? "\u266A Muted" : "\u266A Music"), btn -> toggleMute()).dimensions(this.width - 110, 10, 100, 20).build();
      this.addDrawableChild(muteButton);

      if (!musicMuted) {
         startMusic();
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      ey(mouseX, mouseY, this.width, this.height);

      if (this.i == 0L && this.h) {
         this.i = Util.getMeasuringTimeMs();
      }

      float f2 = 1.0F;
      if (this.h) {
         float f3 = (float)(Util.getMeasuringTimeMs() - this.i) / 2000.0F;
         if (f3 > 1.0F) {
            this.h = false;
         } else {
            f2 = MathHelper.clampedMap(MathHelper.clamp(f3, 0.0F, 1.0F), 0.5F, 1.0F, 0.0F, 1.0F);
         }

         this.eE(f2);
      }

      this.renderPanoramaBackground(context, delta);

      Color themeColor = ColorHelper3.auO() != null ? ColorHelper3.auO().auU() : new Color(181, 156, 255);
      int centerX = this.width / 2;
      int n4 = this.height / 2 - 48 - 30;


      // Frosted backing behind buttons
      int n2 = this.width / 2 - 200 / 2;
      int n3 = this.height / 2 - 48 + 30;
      BufferVertexBase.vi(context, n2 - 8, n3 - 6, 216, 106, new Color(11, 13, 20, (int)(160 * f2)).getRGB());
      BufferVertexBase.vn(context, n2 - 8, n3 - 6, 216, 1.0, ColorUtils.iG(themeColor.getRGB(), (int)(160 * f2)), ColorUtils.iG(themeColor.getRGB(), (int)(40 * f2)));
      BufferVertexBase.vi(context, n2 - 8, n3 - 5, 0.5, 105, new Color(45, 50, 75, (int)(100 * f2)).getRGB());
      BufferVertexBase.vi(context, n2 + 207.5, n3 - 5, 0.5, 105, new Color(45, 50, 75, (int)(100 * f2)).getRGB());

      int n = MathHelper.ceil(f2 * 255.0F) << 24;
      if ((n & -67108864) != 0) {
         Iterator iterator2 = this.drawables.iterator();

         while (iterator2.hasNext()) {
            ((Drawable)iterator2.next()).render(context, mouseX, mouseY, delta);
         }

         int n2Height = RenderUtils.acG();
         Object[] objs = new Object[]{"Yami", Bootstrap.b};
         String temp = String.format("%s %s", objs).toLowerCase();
         float temp2 = this.height - n2Height * 2 - 4;
         int n3Color = 16777215 | n;
         RenderUtils.acE(context, temp, 5.0F, temp2, n3Color);
      }

      // Logo
      int logoX = (int)(this.width / 2.0 - 76.0);
      int logoY = n4 - 14;

      RenderSystem.enableBlend();
      com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10241, 9729);
      com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10240, 9729);
      float f5 = themeColor.getRed() / 255.0F;
      float temp3 = themeColor.getGreen() / 255.0F;
      float temp4 = themeColor.getBlue() / 255.0F;
      context.setShaderColor(f5, temp3, temp4, f2);
      RenderSystem.setShaderTexture(0, c);
      eA(context, logoX, logoY, 150.0F, 54.0F, 150.0F, 54.0F);
      // Rotating Fags emblem on top of the Yami logo (fade-in, texture-based)
      {
         long now = Util.getMeasuringTimeMs();
         if (starFadeStart == 0) starFadeStart = now;
         float fade = MathHelper.clamp((now - starFadeStart) / 1500.0F, 0.0F, 1.0F);
         if (fade > 0.01F) {
            float angle = (now % 3600L) / 10.0F;
            int starCx = logoX + 75;
            int starCy = logoY - 80;
            float R = 48.0F;
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, Identifier.of("yamiclient", "textures/fags.png"));
            com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10241, 9729);
            com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10240, 9729);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fade);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder bb = tess.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Matrix4f mat = context.getMatrices().peek().getPositionMatrix();
            // rotate around star center
            context.getMatrices().push();
            context.getMatrices().translate(starCx, starCy, 0);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float)Math.toRadians(angle)));
            context.getMatrices().scale(R, R, 1.0F);
            Matrix4f m = context.getMatrices().peek().getPositionMatrix();
            bb.vertex(m, -1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).color(255, 255, 255, (int)(255 * fade));
            bb.vertex(m, -1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).color(255, 255, 255, (int)(255 * fade));
            bb.vertex(m, 1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).color(255, 255, 255, (int)(255 * fade));
            bb.vertex(m, 1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).color(255, 255, 255, (int)(255 * fade));
            context.getMatrices().pop();
            BufferRenderer.drawWithGlobalProgram(bb.end());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();

            // Sweep (shimmer) effect: white gradient band passing across the emblem
            // (only visible where the emblem texture has alpha > 0)
            {
               float sweep = ((now % 3000) / 3000.0F);
               float bandCenterLocal = -1.0F + 2.0F * sweep; // -1..1 local space
               float bandHalfLocal = 0.06F; // daha ince
               float bandLeftLocal = bandCenterLocal - bandHalfLocal;
               float bandRightLocal = bandCenterLocal + bandHalfLocal;
               // clamp to emblem bounds
               if (bandLeftLocal < -1.0F) bandLeftLocal = -1.0F;
               if (bandRightLocal > 1.0F) bandRightLocal = 1.0F;
               if (bandLeftLocal < bandRightLocal) {
                  float u1 = (bandLeftLocal + 1.0F) / 2.0F;
                  float u2 = (bandRightLocal + 1.0F) / 2.0F;
                  RenderSystem.enableBlend();
                  RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
                  RenderSystem.setShaderTexture(0, Identifier.of("yamiclient", "textures/fags.png"));
                  com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10241, 9729);
                  com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10240, 9729);
                  RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fade);
                  Tessellator tess2 = Tessellator.getInstance();
                  BufferBuilder bb2 = tess2.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                  Matrix4f m2 = context.getMatrices().peek().getPositionMatrix();
                  // apply star transform (translate + rotate + scale) for the band
                  // (reuse the same transform by re-pushing)
                  context.getMatrices().push();
                  context.getMatrices().translate(starCx, starCy, 0);
                  context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float)Math.toRadians(angle)));
                  context.getMatrices().scale(R, R, 1.0F);
                  Matrix4f mBand = context.getMatrices().peek().getPositionMatrix();
                  int white = new Color(255, 255, 255, (int)(180 * fade)).getRGB();
                  bb2.vertex(mBand, bandLeftLocal, -1.0F, 0).color(white).texture(u1, 0);
                  bb2.vertex(mBand, bandLeftLocal, 1.0F, 0).color(white).texture(u1, 1);
                  bb2.vertex(mBand, bandRightLocal, 1.0F, 0).color(white).texture(u2, 1);
                  bb2.vertex(mBand, bandRightLocal, -1.0F, 0).color(white).texture(u2, 0);
                  context.getMatrices().pop();
                  BufferRenderer.drawWithGlobalProgram(bb2.end());
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  RenderSystem.disableBlend();
               }
            }
         }
      }
   }

   private void eE(float f2) {
      Iterator iterator2 = this.children().iterator();

      while (iterator2.hasNext()) {
         Element element = (Element)iterator2.next();
         if (element instanceof ClickableWidget) {
            ((ClickableWidget)element).setAlpha(f2);
         }
      }
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   protected void renderPanoramaBackground(DrawContext context, float delta) {
      if (ColorHelper3.auO().auQ()) {
         ez(context, this.width, this.height);
      } else {
         int n = this.height;
         int n2 = this.width;
         ROTATING_PANORAMA_RENDERER.render(context, n2, n, 1.0F, delta);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void eF(ButtonWidget buttonWidget) {
      stopMusic();
      e = true;
      MinecraftClient temp = this.client;
      TitleScreen titleScreen = new TitleScreen();
      temp.setScreen((Screen)titleScreen);
      e = false;
   }

   private void eG(DrawTextPressableWidget drawTextPressableWidget) {
      stopMusic();
      UnidentifiedUtils2.mM(this);
   }

   private void eH(DrawTextPressableWidget drawTextPressableWidget) {
      stopMusic();
      this.client.scheduleStop();
   }

   private void eI(DrawTextPressableWidget drawTextPressableWidget) {
      stopMusic();
      MinecraftClient temp = this.client;
      OptionsScreen optionsScreen = new OptionsScreen(this, this.client.options);
      temp.setScreen((Screen)optionsScreen);
   }

   private void eJ(DrawTextPressableWidget drawTextPressableWidget) {
      stopMusic();
      MinecraftClient temp = this.client;
      MultiplayerScreen multiplayerScreen = new MultiplayerScreen(this);
      temp.setScreen((Screen)multiplayerScreen);
   }

   private void eK(DrawTextPressableWidget drawTextPressableWidget) {
      stopMusic();
      MinecraftClient temp = this.client;
      SelectWorldScreen selectWorldScreen = new SelectWorldScreen(this);
      temp.setScreen((Screen)selectWorldScreen);
   }

   static void eL(DrawContext context2, float f2, float f3, float f4, float f5) {
      eA(context2, (float)(-f2 * f), (float)(-f3 * g), f4 + f2, f5 + f3, f4 + f2, f5 + f3);
   }
}
