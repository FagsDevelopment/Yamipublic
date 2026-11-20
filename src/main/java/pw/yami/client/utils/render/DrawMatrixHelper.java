package pw.yami.client.utils.render;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import pw.yami.client.gui.CategoryPanel;
import pw.yami.client.gui.DrawScreenScreen;
import pw.yami.client.manager.Managers;
import pw.yami.client.manager.RenderUtils;
import pw.yami.client.manager.SoundManager;
import pw.yami.client.model.render.AstolfoMioMode;
import pw.yami.client.model.render.Easing;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleBase;
import pw.yami.client.module.client.UI;
import pw.yami.client.render.ColorBase;
import pw.yami.client.render.DrawBase;
import pw.yami.client.setting.BlockListSetting;
import pw.yami.client.setting.ItemListSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.UnidentifiedHelper17;
import pw.yami.client.utils.UnidentifiedHelper7;
import pw.yami.client.utils.world.SoundEventHelper;

public class DrawMatrixHelper extends ColorBase {
   private final ModuleBase moduleBase;
   private final List list = new CopyOnWriteArrayList();
   private final UnidentifiedHelper7 unidentifiedHelper7 = new UnidentifiedHelper7(false, '\u0000', 200.0F, 496665058, '楚', Easing.CUBIC_OUT);
   private final UnidentifiedHelper9 unidentifiedHelper9 = new UnidentifiedHelper9();
   private static final Color e_ = new Color(-5592406);
   private float f_;
   private boolean g;
   private static final String d = "Enabled";

   public DrawMatrixHelper(ModuleBase moduleBase2, CategoryPanel categoryPanel, float f2, float f3) {
      super(categoryPanel, f2, f3, 102.0F);
      this.moduleBase = moduleBase2;
      Iterator iterator2 = moduleBase2.OQ().iterator();

      while (iterator2.hasNext()) {
         Setting setting2 = (Setting)iterator2.next();
         if (!setting2.getText().equalsIgnoreCase("Enabled")) {
            if (setting2.atx() instanceof Boolean) {
               List list2 = this.list;
               RenderHelper renderHelper = new RenderHelper(categoryPanel, this, setting2, f2, f3);
               list2.add(renderHelper);
            } else if (setting2.atx() instanceof Double) {
               List list3 = this.list;
               MathHelperHelper mathHelperHelper = new MathHelperHelper(categoryPanel, this, setting2, f2, f3);
               list3.add(mathHelperHelper);
            } else if (setting2.atx() instanceof Float) {
               List list4 = this.list;
               MathHelperHelper mathHelperHelper2 = new MathHelperHelper(categoryPanel, this, setting2, f2, f3);
               list4.add(mathHelperHelper2);
            } else if (setting2.atx() instanceof Integer) {
               List list5 = this.list;
               MathHelperHelper mathHelperHelper3 = new MathHelperHelper(categoryPanel, this, setting2, f2, f3);
               list5.add(mathHelperHelper3);
            } else if (setting2.atx() instanceof Enum) {
               List list6 = this.list;
               SoundEventHelper soundEventHelper = new SoundEventHelper(categoryPanel, this, setting2, f2, f3);
               list6.add(soundEventHelper);
            } else if (setting2.atx() instanceof String) {
               List list7 = this.list;
               RenderHelper5 renderHelper5 = new RenderHelper5(categoryPanel, this, setting2, f2, f3);
               list7.add(renderHelper5);
            } else if (setting2.atx() instanceof UnidentifiedHelper17) {
               List list8 = this.list;
               DrawHelper drawHelper = new DrawHelper(categoryPanel, this, setting2, f2, f3);
               list8.add(drawHelper);
            } else if (setting2.atx() instanceof Color) {
               List list9 = this.list;
               MathHelperIdentifierHelper mathHelperIdentifierHelper = new MathHelperIdentifierHelper(categoryPanel, this, setting2, f2, f3);
               list9.add(mathHelperIdentifierHelper);
            } else if (setting2 instanceof BlockListSetting || setting2 instanceof ItemListSetting) {
               List temp = this.list;
               RenderHelper3 renderHelper3 = new RenderHelper3(categoryPanel, this, setting2, f2, f3);
               temp.add(renderHelper3);
            }
         }
      }

      this.g = false;
   }

   @Override
   public void aeU(DrawContext context, int n, int n2) {
      this.anQ(context, this.j, this.k, n, n2);
   }

   public void anQ(DrawContext context, float f2, float f3, int n, int n2) {
      if (UI.aum() == AstolfoMioMode.YamiV2) {
         this.vy(RenderUtils.acG() + UI.atK().atT());
         this.j = f2;
         this.k = f3;
         ModuleBase moduleBase2 = this.moduleBase;
         boolean flag2 = false;
         if (moduleBase2 instanceof ClientModule) {
            flag2 = ((ClientModule)moduleBase2).awg();
         }
         boolean flag = this.vC(n, n2);
         int n3 = UI.auf() ? -16777216 : -1;
         int nAccent = UI.atK().atW(this.k);

         // Background card rendering
         if (UI.aum() == AstolfoMioMode.YamiV2) {
            // YamiV2: Clean, minimalist flat theme ("sade" tema)
            if (flag2) {
               vi(context, f2, f3, 2.0, this.m, nAccent);
               vi(context, f2 + 2.0, f3, this.l - 2.0, this.m, ColorUtils.iG(nAccent, flag ? 45 : 32));
            } else if (flag) {
               vi(context, f2, f3, this.l, this.m, new Color(255, 255, 255, 12).getRGB());
            } else {
               vi(context, f2, f3, this.l, this.m, new Color(16, 17, 23, 140).getRGB());
            }
         } else {
            float cardTime = (float)(System.currentTimeMillis() % 60000L) / 1000.0F;
            if (flag2) {
               // Active state: Animated gradient left accent pill
               float accentPulse = 0.8F + (float)Math.sin(cardTime * 2.0 + f3 * 0.05) * 0.2F;
               int accentPulsed = ColorUtils.iG(nAccent, (int)(255 * accentPulse));
               vi(context, f2, f3, 2.5, this.m, accentPulsed);
               // Multi-gradient horizontal highlight with animated shimmer
               float shimmer = (float)(Math.sin(cardTime * 1.2 + f2 * 0.02) * 0.5 + 0.5);
               int shimmerAlpha1 = (int)(85 + shimmer * 40 + (flag ? 30 : 0));
               int shimmerAlpha2 = (int)(12 + shimmer * 18 + (flag ? 18 : 0));
               vn(context, f2 + 2.5, f3, this.l - 2.5, this.m, 
                  ColorUtils.iG(nAccent, Math.min(255, shimmerAlpha1)), 
                  ColorUtils.iG(nAccent, Math.min(255, shimmerAlpha2)));
               // Top accent glow line with gradient fade
               vn(context, f2, f3, this.l, 0.5, ColorUtils.iG(nAccent, 90), ColorUtils.iG(nAccent, 30));
               // Bottom accent glow line
               vn(context, f2, f3 + this.m - 0.5, this.l, 0.5, ColorUtils.iG(nAccent, 70), ColorUtils.iG(nAccent, 20));
               // Right edge subtle glow
               vi(context, f2 + this.l - 0.5, f3 + 1.0, 0.5, this.m - 2.0, ColorUtils.iG(nAccent, 25));
            } else if (flag) {
               // Hovered state: Gradient card with animated side indicator
               vn(context, f2, f3, this.l, this.m,
                  new Color(255, 255, 255, 24).getRGB(),
                  new Color(255, 255, 255, 8).getRGB());
               // Left hover indicator with gradient
               float hoverPulse = 0.7F + (float)Math.sin(cardTime * 3.0) * 0.3F;
               int hoverAccent = ColorUtils.iG(nAccent, (int)(180 * hoverPulse));
               vi(context, f2, f3 + 1.5F, 1.5, this.m - 3.0F, hoverAccent);
               // Top subtle line
               vi(context, f2, f3, this.l, 0.5, new Color(255, 255, 255, 10).getRGB());
            } else {
               // Disabled normal state: gradient dark card
               vk(context, f2, f3, this.l, this.m,
                  new Color(18, 20, 30, 140).getRGB(),
                  new Color(24, 26, 38, 120).getRGB());
            }
         }

         // Text color animation
         Color targetTextColor;
         if (flag2) {
            targetTextColor = new Color(-1);
         } else if (flag) {
            targetTextColor = new Color(-1);
         } else if (UI.atK().aue()) {
            targetTextColor = e_;
         } else {
            targetTextColor = new Color(165, 172, 192);
         }
         int animatedTextColor = this.colorHelper2.arQ(targetTextColor, 150L, Easing.QUINT_OUT).getRGB();

         // Smooth text slide animation on hover or enabled
         double slide = this.unidentifiedHelper9.MH((flag || flag2) ? 1.0 : 0.0, 100L, Easing.CUBIC_OUT);
         float textX = (float)(f2 + 4.0F + slide * 2.0);
         this.ve(context, vf(this.moduleBase.OO()), textX, f3 + UI.atK().atU(), animatedTextColor);

         // Chevron expand indicator on right
         if (UI.atK().getArrows()) {
            if (this.anS()) {
               String arrow = "v";
               this.ve(context, arrow, f2 + this.l - RenderUtils.acF(arrow) - 4.0F, f3 + UI.atK().atU(), ColorUtils.iG(nAccent, 220));
            } else if (!this.list.isEmpty()) {
               String arrow = ">";
               this.ve(context, arrow, f2 + this.l - RenderUtils.acF(arrow) - 4.0F, f3 + UI.atK().atU(), new Color(110, 115, 130).getRGB());
            }
         }

         if (UI.atK().aug()) {
            this.ve(context, "*", f2 + this.l - 12.0F - RenderUtils.acF("*"), f3 + 1.0F + UI.atK().atU(), nAccent);
         }

         // Draw expanded settings if open
         double d4 = this.unidentifiedHelper7.qW();
         if (d4 > 0.01F) {
            this.f_ = this.k + this.m + 0.5F;
            float f4 = 0.0F;
            Iterator iterator2 = this.list.iterator();
            while (iterator2.hasNext()) {
               DrawBase drawBase = (DrawBase)iterator2.next();
               if (drawBase.getSetting().isVisible()) {
                  f4 += drawBase.vx();
                  f4 += 0.5F;
                  if (drawBase instanceof MathHelperIdentifierHelper && ((MathHelperIdentifierHelper)drawBase).aB() > 0.01F) {
                     f4 += ((MathHelperIdentifierHelper)drawBase).ax() * ((MathHelperIdentifierHelper)drawBase).aB() * this.anT();
                  } else if (drawBase instanceof SoundEventHelper && ((SoundEventHelper)drawBase).xS() > 0.01F) {
                     f4 += ((SoundEventHelper)drawBase).xT() * ((SoundEventHelper)drawBase).xS() * this.anT();
                  }
               }
            }

            this.vF(this.j, this.f_ - 1.0F, this.j + this.l, this.f_ + (1.0F + f4) * d4);

            if (UI.aum() == AstolfoMioMode.YamiV2) {
               vi(context, f2, this.f_ - 1.0F, this.l, (1.0F + f4) * d4, new Color(11, 12, 17, 240).getRGB());
               vi(context, f2 + 1.0F, this.f_ - 1.0F, 1.0, (1.0F + f4) * d4, ColorUtils.iG(nAccent, 130));
            } else {
               // Container background with gradient
               vk(context, f2, this.f_ - 1.0F, this.l, (1.0F + f4) * d4,
                  new Color(8, 9, 14, 230).getRGB(),
                  new Color(14, 15, 22, 210).getRGB());
               // Left guide connector line with animated gradient
               float guideTime = (float)(System.currentTimeMillis() % 60000L) / 1000.0F;
               float guidePulse = 0.7F + (float)Math.sin(guideTime * 1.5) * 0.3F;
               int guideAlpha = (int)(140 * guidePulse);
               vk(context, f2 + 1.0F, this.f_ - 1.0F, 1.5, (1.0F + f4) * d4,
                  ColorUtils.iG(nAccent, Math.min(255, guideAlpha + 40)),
                  ColorUtils.iG(nAccent, Math.min(255, guideAlpha - 30)));
               // Bottom guide cap with gradient
               vn(context, f2 + 1.0F, this.f_ + (1.0F + f4) * d4 - 0.5, this.l - 2.0F, 0.5,
                  ColorUtils.iG(nAccent, 100), ColorUtils.iG(nAccent, 20));
               // Right border subtle line
               vi(context, f2 + this.l - 0.5, this.f_ - 1.0F, 0.5, (1.0F + f4) * d4,
                  new Color(40, 44, 60, 100).getRGB());
            }

            iterator2 = this.list.iterator();
            while (iterator2.hasNext()) {
               DrawBase drawBase2 = (DrawBase)iterator2.next();
               drawBase2.vE(f2 + 2.0F, this.f_);
               if (drawBase2.getSetting().isVisible()) {
                  drawBase2.Cs(context, f2 + 1.0F, this.f_, n, n2);
                  ((CategoryPanel)this.renderHelper2).bE((float)(drawBase2.vx() * d4));
                  ((CategoryPanel)this.renderHelper2).bE(0.5F);
                  this.anR(drawBase2.vx());
                  this.anR(0.5F);
               }
            }

            this.vG();
            ((CategoryPanel)this.renderHelper2).bE((float)(2.0 * d4));
         } else {
            Iterator iterator3 = this.list.iterator();
            while (iterator3.hasNext()) {
               DrawBase drawBase3 = (DrawBase)iterator3.next();
               if (drawBase3 instanceof DrawHelper) {
                  ((DrawHelper)drawBase3).lX();
               }

               if (drawBase3 instanceof RenderHelper5) {
                  ((RenderHelper5)drawBase3).afF();
               }

               if (drawBase3 instanceof MathHelperIdentifierHelper) {
                  ((MathHelperIdentifierHelper)drawBase3).ay();
               }

               if (drawBase3 instanceof MathHelperHelper) {
                  ((MathHelperHelper)drawBase3).nW();
               }
            }
         }
         return;
      }

      int n3;
      Color color;
      label131: {
         int temp;
         label134: {
            boolean flag;
            Color color2;
            label129: {
               label128: {
                  this.vy(RenderUtils.acG() + UI.atK().atT());
                  this.j = f2;
                  this.k = f3;
                  ModuleBase moduleBase2 = this.moduleBase;
                  if (moduleBase2 instanceof ClientModule) {
                     boolean flag2 = ((ClientModule)moduleBase2).awg();
                     flag = this.vC(n, n2);
                     color = null;
                     n3 = UI.auf() ? -16777216 : -1;
                     if (UI.aum().Qx()) {
                        color = this.jj(flag2, flag);
                        this.vg(context, color.getRGB());
                        if (UI.atK().aue() && !flag2) {
                           temp = -5592406;
                           break label134;
                        }
                        break label128;
                     }

                     if (!flag2) {
                        if (UI.atK().aue()) {
                           color2 = e_;
                        } else {
                           color2 = new Color(n3);
                        }
                        break label129;
                     }
                  } else {
                     flag = this.vC(n, n2);
                     color = null;
                     n3 = UI.auf() ? -16777216 : -1;
                     if (UI.aum().Qx()) {
                        color = this.jj(true, flag);
                        this.vg(context, color.getRGB());
                        if (UI.atK().aue()) {
                        }
                        break label128;
                     }
                  }

                  color2 = new Color(UI.atK().atW(this.k));
                  break label129;
               }

               temp = n3;
               break label134;
            }

            int n4 = this.colorHelper2.arQ(color2, 150L, Easing.QUINT_OUT).getRGB();
            context.getMatrices().push();
            MatrixStack matrices = context.getMatrices();
            double temp2 = 0.0;
            DecimalFormat temp3 = ModuleBase.bo;
            double temp4 = flag ? 1.0 : 0.0;
            Easing easing = Easing.QUAD_OUT;
            double d2 = -1.0 * this.unidentifiedHelper9.MH(temp4, 50L, easing);
            double d3 = Double.parseDouble(temp3.format(d2));
            matrices.translate(0.0, d3, 0.0);
            this.ve(context, vf(this.moduleBase.OO()), f2 + 3.0F, f3 + UI.atK().atU(), n4);
            context.getMatrices().pop();
            break label131;
         }

         int n5 = temp;
         this.ve(context, vf(this.moduleBase.OO()), f2 + 3.0F, f3 + UI.atK().atU(), n5);
      }

      double d4 = this.unidentifiedHelper7.qW();
      if (DrawScreenScreen.j && this.moduleBase instanceof ClientModule) {
         int n6 = ((ClientModule)this.moduleBase).WS() ? -8355712 : n3;
         FontLoader.fontRenderer3.aaR(context.getMatrices(), "a", f2 + this.l - 11.5, f3 + UI.atK().atU(), ColorUtils.iG(n6, 255));
      }

      if (UI.atK().aug()) {
         this.ve(context, "*", f2 + this.l - 4.0F - RenderUtils.acF("*"), f3 + 1.0F + UI.atK().atU(), n3);
      }

      if (d4 > 0.01F) {
         this.f_ = this.k + this.m + 0.5F;
         float f4 = 0.0F;
         Iterator iterator2 = this.list.iterator();

         while (iterator2.hasNext()) {
            DrawBase drawBase = (DrawBase)iterator2.next();
            if (drawBase.getSetting().isVisible()) {
               f4 += drawBase.vx();
               if (UI.aum().Qz()) {
                  f4 += 0.5F;
               }

               if (drawBase instanceof MathHelperIdentifierHelper && ((MathHelperIdentifierHelper)drawBase).aB() > 0.01F) {
                  f4 += ((MathHelperIdentifierHelper)drawBase).ax() * ((MathHelperIdentifierHelper)drawBase).aB() * this.anT();
               } else if (drawBase instanceof SoundEventHelper && ((SoundEventHelper)drawBase).xS() > 0.01F) {
                  f4 += ((SoundEventHelper)drawBase).xT() * ((SoundEventHelper)drawBase).xS() * this.anT();
               }
            }
         }

         this.vF(this.j, this.f_ - 1.0F, this.j + this.l, this.f_ + (1.0F + f4) * d4);
         iterator2 = this.list.iterator();

         while (iterator2.hasNext()) {
            DrawBase drawBase2 = (DrawBase)iterator2.next();
            drawBase2.vE(f2 + 2.0F, this.f_);
            if (drawBase2.getSetting().isVisible()) {
               drawBase2.Cs(context, f2 + 1.0F, this.f_, n, n2);
               ((CategoryPanel)this.renderHelper2).bE((float)(drawBase2.vx() * d4));
               if (UI.aum().Qz()) {
                  ((CategoryPanel)this.renderHelper2).bE(0.5F);
               }

               this.anR(drawBase2.vx());
               if (UI.aum().Qz()) {
                  this.anR(0.5F);
               }
            }
         }

         if (UI.aum().Qx()) {
            vi(context, f2, this.k + this.m, 0.5, this.f_ - (this.k + this.m) + 0.5, color.getRGB());
            vi(context, f2 + this.l - 0.5, this.k + this.m, 0.5, this.f_ - (this.k + this.m) + 0.5, color.getRGB());
            vi(context, f2, this.f_ + 0.5, this.l, 0.5, color.getRGB());
         }

         this.vG();
         ((CategoryPanel)this.renderHelper2).bE((float)(2.0 * d4));
      } else {
         Iterator iterator3 = this.list.iterator();

         while (iterator3.hasNext()) {
            DrawBase drawBase3 = (DrawBase)iterator3.next();
            if (drawBase3 instanceof DrawHelper) {
               ((DrawHelper)drawBase3).lX();
            }

            if (drawBase3 instanceof RenderHelper5) {
               ((RenderHelper5)drawBase3).afF();
            }

            if (drawBase3 instanceof MathHelperIdentifierHelper) {
               ((MathHelperIdentifierHelper)drawBase3).ay();
            }

            if (drawBase3 instanceof MathHelperHelper) {
               ((MathHelperHelper)drawBase3).nW();
            }
         }
      }
   }

   @Override
   public void UU(int n, int n2, int n3) {
      if (this.vC(n, n2)) {
         label30: {
            if (n3 == 0) {
               ModuleBase moduleBase2 = this.moduleBase;
               if (moduleBase2 instanceof ClientModule) {
                  if (DrawScreenScreen.j) {
                     ((ClientModule)moduleBase2).WT(!((ClientModule)moduleBase2).WS());
                  } else {
                     ((ClientModule)moduleBase2).awa();
                  }

                  break label30;
               }
            }

            if (n3 == 1) {
               this.g = !this.g;
               this.unidentifiedHelper7.qV(this.g);
            }
         }

         if (UI.atK().auc()) {
            Managers.soundManager.jg(SoundManager.a);
         }
      } else if (this.g) {
         Iterator iterator2 = this.list.iterator();

         while (iterator2.hasNext()) {
            DrawBase drawBase = (DrawBase)iterator2.next();
            if (drawBase.getSetting().isVisible()) {
               drawBase.UU(n, n2, n3);
            }
         }
      }
   }

   @Override
   public void UV() {
      if (this.g) {
         Iterator iterator2 = this.list.iterator();

         while (iterator2.hasNext()) {
            ((DrawBase)iterator2.next()).UV();
         }
      }
   }

   @Override
   public void UW(int n) {
      if (this.g) {
         Iterator iterator2 = this.list.iterator();

         while (iterator2.hasNext()) {
            ((DrawBase)iterator2.next()).UW(n);
         }
      }
   }

   @Override
   public void UX(char c) {
      if (this.g) {
         Iterator iterator2 = this.list.iterator();

         while (iterator2.hasNext()) {
            ((DrawBase)iterator2.next()).UX(c);
         }
      }
   }

   public void anR(float f2) {
      this.f_ += f2;
   }

   public boolean anS() {
      return this.g;
   }

   public float anT() {
      return (float)this.unidentifiedHelper7.qW();
   }

   public ModuleBase getModuleBase() {
      return this.moduleBase;
   }

   public List getList() {
      return this.list;
   }
}
