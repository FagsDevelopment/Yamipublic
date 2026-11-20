package pw.yami.client.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import pw.yami.client.Mc;
import pw.yami.client.manager.RenderUtils;
import pw.yami.client.model.render.AstolfoMioMode;
import pw.yami.client.model.render.Easing;
import pw.yami.client.model.render.GuiGifMode;
import pw.yami.client.model.render.GuiShaderMode;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleBase;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.module.client.UI;
import pw.yami.client.render.BufferVertexBase;
import pw.yami.client.render.ColorBase;
import pw.yami.client.render.DrawBase;
import pw.yami.client.render.ScreenRectManager;
import pw.yami.client.utils.UnidentifiedHelper7;
import pw.yami.client.utils.render.ColorUtils;
import pw.yami.client.utils.render.DrawHelper;
import pw.yami.client.utils.render.DrawHelper2;
import pw.yami.client.utils.render.DrawMatrixHelper;
import pw.yami.client.utils.render.MathHelperHelper;
import pw.yami.client.utils.render.MathHelperIdentifierHelper;
import pw.yami.client.utils.render.RenderHelper5;

public class DrawScreenScreen extends Screen {
   public static final ScreenRectManager a = new ScreenRectManager();
   public static float b;
   public static int c;
   public static int d;
   public static ColorBase colorBase;
   public static boolean f;
   public static boolean g;
   public static boolean h;
   public static boolean i;
   public static boolean j;
   private final List list = new CopyOnWriteArrayList();
   private final UI ui;
   private final UnidentifiedHelper7 unidentifiedHelper7 = new UnidentifiedHelper7(false, '\u0000', 200.0F, 496665058, '楚', Easing.LINEAR);
   private static final Identifier n = Identifier.of("yamiclient", "textures/custom_hud_icon.png");
   private static final Identifier SONIC_TEX = Identifier.of("yamiclient", "textures/sonic_anim.png");
   private static final Identifier POPCAT1_TEX = Identifier.of("yamiclient", "textures/popcat1.png");
   private static final Identifier POPCAT2_TEX = Identifier.of("yamiclient", "textures/popcat2.png");
   private static final Identifier PEPE_TEX = Identifier.of("yamiclient", "textures/pepe.png");
   private static final Identifier ISRAEL_TEX = Identifier.of("yamiclient", "textures/davut_star.png");
   private static final Identifier FAGS_TEX = Identifier.of("yamiclient", "textures/fags.png");
   private String text;
   private boolean p = true;
   private long openTime = System.currentTimeMillis();
   private boolean searchOpen = false;
   private String searchQuery = "";
   private int searchSelectedIndex = 0;
   private int searchScrollOffset = 0;

   public static class SearchResult {
      public final DrawMatrixHelper matrixHelper;
      public final CategoryPanel panel;
      public final ModuleCategory category;
      public final ClientModule module;
      public final String name;

      public SearchResult(DrawMatrixHelper matrixHelper, CategoryPanel panel, ModuleCategory category, ClientModule module, String name) {
         this.matrixHelper = matrixHelper;
         this.panel = panel;
         this.category = category;
         this.module = module;
         this.name = name;
      }
   }

   public DrawScreenScreen(UI ui2) {
      super(Text.literal("ClickGui"));
      this.ui = ui2;
      float f2 = 15.0F;
      ModuleCategory[] moduleCategorys = ModuleCategory.values();
      int temp = moduleCategorys.length;
      int n2 = 0;

      do {
         ModuleCategory moduleCategory = moduleCategorys[n2];
         if (moduleCategory != ModuleCategory.HUD) {
            CategoryPanel categoryPanel = new CategoryPanel(moduleCategory, f2, 15.0F, 14936090835792L);
            this.list.add(categoryPanel);
            f2 += categoryPanel.vz() + 3.0F;
         }
      } while (++n2 < 8);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      if (UI.atK().aub()) {
         Mc.client.gameRenderer.renderBlur(delta);
         Mc.client.getFramebuffer().beginWrite(false);
      }

      int screenW = context.getScaledWindowWidth();
      int screenH = context.getScaledWindowHeight();
      float timeMs = (float)(System.currentTimeMillis() % 60000L) / 1000.0F;
      int accentColor = UI.atK().atW(0.0);
      int accentR = (accentColor >> 16) & 0xFF;
      int accentG = (accentColor >> 8) & 0xFF;
      int accentB = accentColor & 0xFF;

      GuiShaderMode shader = UI.atK().bgShader != null ? (GuiShaderMode)UI.atK().bgShader.atx() : GuiShaderMode.Waves;

      if (UI.aum() == AstolfoMioMode.YamiV2) {
         // YamiV2: Clean, sleek minimalist dark matte background ("sade" tema)
         int bgTop = new Color(13, 14, 19, 235).getRGB();
         int bgBot = new Color(7, 8, 12, 245).getRGB();
         context.fillGradient(0, 0, screenW, screenH, bgTop, bgBot);
      } else {
         context.fillGradient(
            0,
            0,
            screenW,
            (int)(screenH / 2.0),
            UI.atK().atV(UI.atM().getRGB()),
            UI.atK().atV(UI.atN().getRGB())
         );
         context.fillGradient(
            0,
            (int)(screenH * 0.5),
            screenW,
            screenH,
            UI.atK().atV(UI.atN().getRGB()),
            UI.atK().atV(UI.atO().getRGB())
         );
      }

      // === Background Shader Effects Layer ===
      if (shader == GuiShaderMode.CyberGrid || shader == GuiShaderMode.All) {
         int gridSpacing = 28;
         int gridAlpha = (int)(12 + Math.sin(timeMs * 0.4) * 4);
         int gridColor = new Color(accentR, accentG, accentB, Math.max(0, Math.min(255, gridAlpha))).getRGB();
         for (int gx = 0; gx < screenW; gx += gridSpacing) {
            context.fill(gx, 0, gx + 1, screenH, UI.atK().atV(gridColor));
         }
         for (int gy = 0; gy < screenH; gy += gridSpacing) {
            context.fill(0, gy, screenW, gy + 1, UI.atK().atV(gridColor));
         }
      }

      if (shader == GuiShaderMode.Waves || shader == GuiShaderMode.All) {
         for (int wi = 0; wi < 3; wi++) {
            float wavePhase = timeMs * 0.28F + wi * 1.5F;
            float waveY = screenH * (0.24F + wi * 0.26F) + (float)Math.sin(wavePhase) * (screenH * 0.045F);
            float waveAlpha = 0.03F + (float)Math.sin(wavePhase * 0.6) * 0.012F;
            int waveColor = new Color(
               accentR, accentG, accentB,
               Math.max(0, Math.min(255, (int)(waveAlpha * 255)))
            ).getRGB();
            int bandH = 28 + wi * 6;
            context.fillGradient(0, (int)(waveY - bandH / 2), screenW, (int)waveY, 0, UI.atK().atV(waveColor));
            context.fillGradient(0, (int)waveY, screenW, (int)(waveY + bandH / 2), UI.atK().atV(waveColor), 0);
         }
      }

      if (shader == GuiShaderMode.Stars || shader == GuiShaderMode.All) {
         for (int si = 0; si < 40; si++) {
            int seed = si * 7919;
            float sx = (float)((seed % screenW) + Math.sin(timeMs * 0.4 + si) * 14.0) % screenW;
            if (sx < 0) sx += screenW;
            float sy = (float)((screenH - (timeMs * (12 + (seed % 20)) + seed) % screenH)) % screenH;
            if (sy < 0) sy += screenH;
            float starAlpha = (float)(0.2 + 0.18 * Math.sin(timeMs * 2.2 + si));
            int starColor = new Color(accentR, accentG, accentB, Math.max(0, Math.min(255, (int)(starAlpha * 255)))).getRGB();
            context.fill((int)sx, (int)sy, (int)sx + 2, (int)sy + 2, UI.atK().atV(starColor));
         }
      }

      if (shader == GuiShaderMode.Vignette || shader == GuiShaderMode.Waves || shader == GuiShaderMode.All) {
         int vigSize = Math.min(screenW, screenH) / 3;
         int vigColor = new Color(0, 0, 0, 55).getRGB();
         context.fillGradient(0, 0, vigSize, vigSize, UI.atK().atV(vigColor), 0);
         context.fillGradient(screenW - vigSize, 0, screenW, vigSize, 0, UI.atK().atV(vigColor));
         context.fillGradient(0, screenH - vigSize, vigSize, screenH, 0, UI.atK().atV(vigColor));
         context.fillGradient(screenW - vigSize, screenH - vigSize, screenW, screenH, 0, UI.atK().atV(vigColor));
      }

      // Smooth opening animation
      float animProgress = 1.0F;
      if (UI.atK().animation != null && (Boolean)UI.atK().animation.atx()) {
         long elapsed = System.currentTimeMillis() - this.openTime;
         if (elapsed < 240L) {
            float t = (float)elapsed / 240.0F;
            animProgress = (float)(1.0 - Math.pow(1.0 - t, 3.0));
         }
      }
      float animScale = 0.88F + 0.12F * animProgress;
      float centerX = screenW / 2.0F;
      float centerY = screenH / 2.0F;

      context.getMatrices().push();
      context.getMatrices().translate(centerX, centerY, 0.0F);
      context.getMatrices().scale(animScale, animScale, 1.0F);
      context.getMatrices().translate(-centerX, -centerY, 0.0F);

      mouseX = (int)(mouseX / b);
      mouseY = (int)(mouseY / b);
      context.getMatrices().push();
      context.getMatrices().scale(b, b, 1.0F);
      context.getMatrices().push();
      int n = UI.atK().atV(-1);
      if (UI.atK().auh()) {
         RenderUtils.acE(context, "shift + f1 to reset clickgui", 1.0F, 1.0F, n);
         RenderUtils.acE(context, "shift + scroll to scroll horizontally", 1.0F, 1 + RenderUtils.acG(), n);
         RenderUtils.acE(context, "shift + click a bind to switch between hold and toggle", 1.0F, 1 + RenderUtils.acG() * 2, n);
         RenderUtils.acE(context, "shift + click a module to hide it in the module list", 1.0F, 1 + RenderUtils.acG() * 3, n);
      }

      this.KE(context);
      boolean found = false;
      Iterator iterator2 = this.list.iterator();

      while (iterator2.hasNext()) {
         CategoryPanel categoryPanel = (CategoryPanel)iterator2.next();
         if (categoryPanel.vC((float)mouseX, (float)mouseY) && i && this.KF()) {
            categoryPanel.bL(true);
         }

         categoryPanel.aeU(context, mouseX, mouseY);
         if (!categoryPanel.bG()) {
            continue;
         }

         Iterator iterator3 = categoryPanel.getList().iterator();

         while (iterator3.hasNext()) {
            DrawMatrixHelper drawMatrixHelper = (DrawMatrixHelper)iterator3.next();
            if (DrawHelper2.text != null && !drawMatrixHelper.getModuleBase().Kn().toLowerCase().contains(DrawHelper2.text.toLowerCase())) {
               continue;
            }

            if (drawMatrixHelper.vC((float)mouseX, (float)mouseY) && !drawMatrixHelper.getModuleBase().hw().isEmpty()) {
               this.text = drawMatrixHelper.getModuleBase().hw();
               found = true;
               break;
            }

            if (!drawMatrixHelper.anS()) {
               continue;
            }

            Iterator iterator4 = drawMatrixHelper.getList().iterator();

            while (iterator4.hasNext()) {
               DrawBase drawBase = (DrawBase)iterator4.next();
               if (drawBase.getSetting().isVisible()
                  && !drawBase.getSetting().atw().isEmpty()
                  && drawBase.vC((float)mouseX, (float)mouseY)) {
                  this.text = drawBase.getSetting().atw();
                  found = true;
                  break;
               }
            }
         }
      }

      context.getMatrices().pop();
      if (UI.atK().aui()) {
         this.unidentifiedHelper7.qV(found && !j && colorBase == null);
         if (this.unidentifiedHelper7.qW() > 0.01F && this.text != null) {
            int n2 = RenderUtils.acF(this.text);
            int n3 = (int)(60.0 * this.unidentifiedHelper7.qW());
            int n4 = UI.atK().atV(new Color(0, 0, 0, 50 + n3).getRGB());
            BufferVertexBase.vi(context, (double)(mouseX + 10.0F), (double)(mouseY - 8.0F), (double)(n2 + 4.0F), (double)(RenderUtils.acG() + 2.0F), n4);
            if (UI.aum() == AstolfoMioMode.YamiV2) {
               BufferVertexBase.vi(context, (double)(mouseX + 10.0F), (double)(mouseY - 8.0F), 1.5, (double)(RenderUtils.acG() + 2.0F), UI.atK().atW(mouseY));
            }
            RenderUtils.acE(context, this.text, mouseX + 13.0F, mouseY - 6.0F, ColorUtils.iH(-1, (float)this.unidentifiedHelper7.qW()));
         }
      }

      DrawScreenScreen.h = false;
      f = false;
      c = mouseX;
      d = mouseY;
      context.getMatrices().pop();
      context.getMatrices().pop(); // Pop opening animation matrix

      // Corner animated character GIF (sol alt)
      if (UI.atK().gif.atx() != GuiGifMode.Off) {
         this.renderCornerGif(context);
      }

      // ClickGUI Search Bar (Ctrl+F)
      int screenMouseX = (int)(Mc.client.mouse.getX() * (double)context.getScaledWindowWidth() / (double)Mc.client.getWindow().getWidth());
      int screenMouseY = (int)(Mc.client.mouse.getY() * (double)context.getScaledWindowHeight() / (double)Mc.client.getWindow().getHeight());
      this.renderSearchBar(context, screenMouseX, screenMouseY);
   }

   private void renderCornerGif(DrawContext context) {
      int screenW = context.getScaledWindowWidth();
      int screenH = context.getScaledWindowHeight();
      GuiGifMode mode = (GuiGifMode)UI.atK().gif.atx();
      if (mode == GuiGifMode.Off) {
         return;
      }
      boolean spin = (Boolean)UI.atK().spin.atx();
      long now = System.currentTimeMillis();

      int size = 52;
      int w = size;
      int h = size;

      float centerX = 16.0F + w / 2.0F;
      float centerY = screenH - 16.0F - h / 2.0F;

      context.getMatrices().push();
      context.getMatrices().translate(centerX, centerY, 0.0F);

      if (spin) {
         float rot = (float)((now * 0.15F) % 360.0F);
         context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));
      }

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      GlStateManager._texParameter(3553, 10241, 9729);
      GlStateManager._texParameter(3553, 10240, 9729);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      if (mode == GuiGifMode.Sonic) {
         int frame = (int)((now / 65L) % 8L);
         int u = frame * 128;
         context.drawTexture(SONIC_TEX, -w / 2, -h / 2, (float)u, 0.0F, w, h, 1024, 128);
      } else if (mode == GuiGifMode.PopCat) {
         boolean open = ((now / 150L) % 2L) == 1L;
         Identifier popTex = open ? POPCAT2_TEX : POPCAT1_TEX;
         context.drawTexture(popTex, -w / 2, -h / 2, 0.0F, 0.0F, w, h, w, h);
      } else if (mode == GuiGifMode.Pepe) {
         context.drawTexture(PEPE_TEX, -w / 2, -h / 2, 0.0F, 0.0F, w, h, w, h);
      } else if (mode == GuiGifMode.Israel) {
         context.drawTexture(ISRAEL_TEX, -w / 2, -h / 2, 0.0F, 0.0F, w, h, w, h);
      } else if (mode == GuiGifMode.Fags) {
         context.drawTexture(FAGS_TEX, -w / 2, -h / 2, 0.0F, 0.0F, w, h, w, h);
      }

      GlStateManager._texParameter(3553, 10241, 9728);
      GlStateManager._texParameter(3553, 10240, 9728);
      RenderSystem.disableBlend();
      context.getMatrices().pop();
   }

   private void onQueryChanged() {
      this.searchSelectedIndex = 0;
      this.searchScrollOffset = 0;
      if (this.searchQuery.trim().isEmpty()) {
         DrawHelper2.text = null;
      } else {
         DrawHelper2.text = this.searchQuery.trim();
      }
   }

   private void openSearch() {
      this.searchOpen = true;
      this.searchQuery = "";
      this.searchSelectedIndex = 0;
      this.searchScrollOffset = 0;
      DrawHelper2.text = null;
   }

   private void closeSearch() {
      this.searchOpen = false;
      this.searchQuery = "";
      this.searchSelectedIndex = 0;
      this.searchScrollOffset = 0;
      DrawHelper2.text = null;
   }

   private void toggleSearch() {
      if (this.searchOpen) {
         this.closeSearch();
      } else {
         this.openSearch();
      }
   }

   private List<SearchResult> getSearchResults() {
      List<SearchResult> results = new ArrayList<>();
      String query = this.searchQuery == null ? "" : this.searchQuery.trim().toLowerCase();

      for (Object panelObj : this.list) {
         if (!(panelObj instanceof CategoryPanel)) continue;
         CategoryPanel panel = (CategoryPanel)panelObj;
         ModuleCategory cat = panel.getModuleCategory();
         for (Object modObj : panel.getList()) {
            if (!(modObj instanceof DrawMatrixHelper)) continue;
            DrawMatrixHelper helper = (DrawMatrixHelper)modObj;
            ModuleBase mb = helper.getModuleBase();
            if (mb instanceof ClientModule) {
               ClientModule cm = (ClientModule)mb;
               String name = cm.OO();
               String id = cm.Kn() != null ? cm.Kn() : "";
               String desc = cm.getText3() != null ? cm.getText3() : "";
               if (query.isEmpty() || name.toLowerCase().contains(query) || id.toLowerCase().contains(query) || desc.toLowerCase().contains(query)) {
                  results.add(new SearchResult(helper, panel, cat, cm, name));
               }
            }
         }
      }

      if (!query.isEmpty()) {
         results.sort((a, b) -> {
            boolean aExact = a.name.equalsIgnoreCase(query);
            boolean bExact = b.name.equalsIgnoreCase(query);
            if (aExact && !bExact) return -1;
            if (!aExact && bExact) return 1;
            boolean aStarts = a.name.toLowerCase().startsWith(query);
            boolean bStarts = b.name.toLowerCase().startsWith(query);
            if (aStarts && !bStarts) return -1;
            if (!aStarts && bStarts) return 1;
            return a.name.compareToIgnoreCase(b.name);
         });
      }

      return results;
   }

   private int getCategoryColor(ModuleCategory cat) {
      if (cat == null) return 0xFF95A5A6;
      return switch (cat) {
         case COMBAT -> 0xFFE74C3C;
         case RENDER -> 0xFF9B59B6;
         case MOVEMENT -> 0xFF3498DB;
         case PLAYER -> 0xFF2ECC71;
         case EXPLOIT -> 0xFFE67E22;
         case CLIENT -> 0xFF1ABC9C;
         case MISC -> 0xFF95A5A6;
         default -> 0xFFBDC3C7;
      };
   }

   private void renderSearchBar(DrawContext context, int mouseX, int mouseY) {
      // In-place search: no overlay screen. Modules filtered via DrawHelper2.text.
      if (!this.searchOpen) return;
      String indicator = this.searchQuery.isEmpty() ? "Search: type to filter... (ESC to cancel)" : ("Search: " + this.searchQuery + (((System.currentTimeMillis() / 450L) % 2L == 0L) ? "|" : ""));
      float tw = RenderUtils.acF(indicator);
      float x = (context.getScaledWindowWidth() - tw) / 2.0F;
      RenderUtils.acE(context, indicator, x, 4.0F, this.searchQuery.isEmpty() ? 0xFF656F84 : 0xFFADB5BD);
   }

   private boolean handleSearchClick(double mouseX, double mouseY, int button) {
      int screenW = Mc.client.getWindow().getScaledWidth();

      if (!this.searchOpen) {
         return false; // search hidden; let ClickGUI handle clicks
      }


      float boxW = 430.0F;
      float boxX = (screenW - boxW) / 2.0F;
      float boxY = 22.0F;
      float inputH = 30.0F;

      // Clear [x] click
      if (!this.searchQuery.isEmpty()) {
         float clearX = boxX + boxW - 56.0F;
         if (mouseX >= clearX && mouseX <= clearX + 16.0F && mouseY >= boxY + 7.0F && mouseY <= boxY + 23.0F) {
            this.searchQuery = "";
            this.onQueryChanged();
            if (Mc.client.player != null) {
               Mc.client.player.playSound(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
            }
            return true;
         }
      }

      // ESC click
      float escX = boxX + boxW - 36.0F;
      if (mouseX >= escX && mouseX <= escX + 28.0F && mouseY >= boxY + 7.0F && mouseY <= boxY + 23.0F) {
         this.closeSearch();
         if (Mc.client.player != null) {
            Mc.client.player.playSound(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
         }
         return true;
      }

      float dropY = boxY + inputH + 2.0F;
      List<SearchResult> results = this.getSearchResults();

      if (this.searchQuery.isEmpty()) {
         float dropH = 58.0F;
         if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= dropY + dropH) {
            return true;
         }
         this.closeSearch();
         return true;
      }

      if (results.isEmpty()) {
         float dropH = 48.0F;
         if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= dropY + dropH) {
            return true;
         }
         this.closeSearch();
         return true;
      }

      int maxVisible = 7;
      int visibleCount = Math.min(results.size() - this.searchScrollOffset, maxVisible);
      float itemH = 24.0F;
      float headerH = 20.0F;
      float dropTotalH = headerH + visibleCount * (itemH + 2.0F) + 6.0F;

      for (int i = 0; i < visibleCount; i++) {
         int actualIdx = this.searchScrollOffset + i;
         if (actualIdx >= results.size()) break;
         SearchResult item = results.get(actualIdx);
         float itemY = dropY + headerH + 3.0F + i * (itemH + 2.0F);

         if (mouseX >= boxX + 4.0F && mouseX <= boxX + boxW - 4.0F && mouseY >= itemY && mouseY <= itemY + itemH) {
            this.searchSelectedIndex = actualIdx;
            if (button == 0) { // Left click: toggle module!
               item.module.awa();
               item.panel.bH(true);
               if (Mc.client.player != null) {
                  Mc.client.player.playSound(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
               }
               return true;
            } else if (button == 1) { // Right click: open category panel
               item.panel.bH(true);
               this.closeSearch();
               if (Mc.client.player != null) {
                  Mc.client.player.playSound(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.2F);
               }
               return true;
            }
         }
      }

      if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= dropY + dropTotalH) {
         return true;
      }

      this.closeSearch();
      return true;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.handleSearchClick(mouseX, mouseY, button)) {
         return true;
      }

      mouseX /= b;
      mouseY /= b;
      if (button == 0) {
         h = true;
         i = true;
      } else if (button == 1) {
         f = true;
         g = true;
      }

      Iterator iterator2 = this.list.iterator();

      while (iterator2.hasNext()) {
         ((CategoryPanel)iterator2.next()).UU((int)mouseX, (int)mouseY, button);
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      mouseX /= b;
      mouseY /= b;
      if (button == 0) {
         i = false;
         colorBase = null;
      } else if (button == 1) {
         g = false;
      }

      Iterator iterator2 = this.list.iterator();

      while (iterator2.hasNext()) {
         ((CategoryPanel)iterator2.next()).UV();
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.searchOpen) {
         int screenW = Mc.client.getWindow().getScaledWidth();
         float boxW = 430.0F;
         float boxX = (screenW - boxW) / 2.0F;
         List<SearchResult> results = this.getSearchResults();
         int maxVisible = 7;
         if (results.size() > maxVisible && mouseX >= boxX && mouseX <= boxX + boxW) {
            this.searchScrollOffset = MathHelper.clamp(this.searchScrollOffset - (int)Math.signum(verticalAmount), 0, results.size() - maxVisible);
            return true;
         }
      }

      double scaledMouseX = mouseX / b;
      double scaledMouseY = mouseY / b;
      boolean hoveredAny = false;
      for (Object obj : this.list) {
         CategoryPanel cp = (CategoryPanel) obj;
         if (scaledMouseX >= cp.vv() && scaledMouseX <= cp.vv() + cp.vz()) {
            hoveredAny = true;
            break;
         }
      }

      for (Object obj : this.list) {
         CategoryPanel categoryPanel = (CategoryPanel) obj;
         boolean isHovered = (scaledMouseX >= categoryPanel.vv() && scaledMouseX <= categoryPanel.vv() + categoryPanel.vz());
         if (!hoveredAny || isHovered) {
            if (j) {
               float f3 = (float)(categoryPanel.vv() + verticalAmount * UI.atK().aul());
               categoryPanel.vE(f3, categoryPanel.vw());
            } else {
               float f2 = (float)(categoryPanel.vw() + verticalAmount * UI.atK().aul());
               categoryPanel.vE(categoryPanel.vv(), f2);
            }
         }
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean isCtrl = hasControlDown() || (modifiers & 2) != 0;
      if (isCtrl && (keyCode == 70 || keyCode == 102)) { // Ctrl+F
         this.toggleSearch();
         return true;
      }

      if (this.searchOpen) {
         if (keyCode == 256) { // ESCAPE
            this.closeSearch();
            return true;
         }
         if (keyCode == 259) { // BACKSPACE
            if (isCtrl) {
               this.searchQuery = "";
            } else if (!this.searchQuery.isEmpty()) {
               this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
            }
            this.onQueryChanged();
            return true;
         }
         if (keyCode == 257) { // ENTER
            List<SearchResult> results = this.getSearchResults();
            if (!results.isEmpty()) {
               int idx = MathHelper.clamp(this.searchSelectedIndex, 0, results.size() - 1);
               SearchResult res = results.get(idx);
               res.module.awa();
               res.panel.bH(true);
               if (Mc.client.player != null) {
                  Mc.client.player.playSound(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
               }
            }
            return true;
         }
         if (keyCode == 265) { // UP
            List<SearchResult> results = this.getSearchResults();
            if (!results.isEmpty()) {
               this.searchSelectedIndex = Math.max(0, this.searchSelectedIndex - 1);
               if (this.searchSelectedIndex < this.searchScrollOffset) {
                  this.searchScrollOffset = this.searchSelectedIndex;
               }
            }
            return true;
         }
         if (keyCode == 264) { // DOWN
            List<SearchResult> results = this.getSearchResults();
            if (!results.isEmpty()) {
               this.searchSelectedIndex = Math.min(results.size() - 1, this.searchSelectedIndex + 1);
               if (this.searchSelectedIndex >= this.searchScrollOffset + 7) {
                  this.searchScrollOffset = this.searchSelectedIndex - 6;
               }
            }
            return true;
         }
         return true;
      }

      // Ctrl+C / Ctrl+V module settings copy & paste
      if (isCtrl) {
         if (keyCode == 67) { // C key
            DrawMatrixHelper hovered = this.getHoveredModule();
            if (hovered != null) {
               pw.yami.client.module.ModuleBase mod = hovered.getModuleBase();
               try {
                  com.google.gson.JsonObject json = mod.LT(25681, 44222, '\u218e');
                  String exportStr = "YAMI_CFG:" + json.toString();
                  Mc.client.keyboard.setClipboard(exportStr);
                  pw.yami.client.utils.chat.ChatUtils.UB("Copied settings for \u00a7s" + mod.getText2() + "\u00a7f to clipboard!");
                  if (Mc.client.player != null) {
                     Mc.client.player.playSound(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
                  }
               } catch (Exception ex) {
                  pw.yami.client.utils.chat.ChatUtils.UL("Failed to copy settings: " + ex.getMessage());
               }
               return true;
            } else {
               pw.yami.client.utils.chat.ChatUtils.UL("Hover your mouse over a module to copy its settings!");
               return true;
            }
         } else if (keyCode == 86) { // V key
            try {
               String clip = Mc.client.keyboard.getClipboard();
               if (clip != null && !clip.isEmpty()) {
                  String jsonStr = clip.trim();
                  if (jsonStr.startsWith("YAMI_CFG:")) {
                     jsonStr = jsonStr.substring("YAMI_CFG:".length()).trim();
                  }
                  com.google.gson.JsonElement elem = com.google.gson.JsonParser.parseString(jsonStr);
                  if (elem.isJsonObject()) {
                     com.google.gson.JsonObject json = elem.getAsJsonObject();
                     DrawMatrixHelper hovered = this.getHoveredModule();
                     pw.yami.client.module.ModuleBase targetMod = null;
                     if (hovered != null) {
                        targetMod = hovered.getModuleBase();
                     } else if (json.has("name")) {
                        String name = json.get("name").getAsString();
                        for (Object obj : pw.yami.client.manager.Managers.moduleManager.XM()) {
                           pw.yami.client.module.ModuleBase m = (pw.yami.client.module.ModuleBase)obj;
                           if (m.getText2().equalsIgnoreCase(name) || m.Kn().equalsIgnoreCase(name) || m.OO().equalsIgnoreCase(name)) {
                              targetMod = m;
                              break;
                           }
                        }
                     }
                     if (targetMod != null) {
                        targetMod.OM(json);
                        pw.yami.client.utils.chat.ChatUtils.UB("Applied settings to \u00a7s" + targetMod.getText2() + "\u00a7f!");
                        if (Mc.client.player != null) {
                           Mc.client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
                        }
                        return true;
                     } else {
                        pw.yami.client.utils.chat.ChatUtils.UL("Hover over a module in ClickGUI to paste settings!");
                     }
                  } else {
                     pw.yami.client.utils.chat.ChatUtils.UL("Clipboard does not contain valid module settings!");
                  }
               }
            } catch (Exception ex) {
               pw.yami.client.utils.chat.ChatUtils.UL("Failed to paste settings: " + ex.getMessage());
            }
            return true;
         }
      }
      if (keyCode == 340) {
         j = true;
      }

      if (keyCode == 290 && j) {
         UI.drawScreenScreen = new DrawScreenScreen(this.ui);
         this.ui.awc();
         this.ui.awb();
      }

      this.p = true;
      Iterator iterator2 = this.list.iterator();

      while (iterator2.hasNext()) {
         CategoryPanel categoryPanel = (CategoryPanel)iterator2.next();
         if (categoryPanel.getDrawHelper2() != null && categoryPanel.getDrawHelper2().afc()) {
            this.p = false;
         } else {
            Iterator iterator3 = categoryPanel.getList().iterator();

            while (iterator3.hasNext()) {
               Iterator iterator4 = ((DrawMatrixHelper)iterator3.next()).getList().iterator();

               while (iterator4.hasNext()) {
                  DrawBase drawBase = (DrawBase)iterator4.next();
                  if (drawBase instanceof DrawHelper && ((DrawHelper)drawBase).lW()
                     || drawBase instanceof RenderHelper5 && ((RenderHelper5)drawBase).afG()
                     || drawBase instanceof MathHelperIdentifierHelper && ((MathHelperIdentifierHelper)drawBase).aA()
                     || drawBase instanceof MathHelperHelper && ((MathHelperHelper)drawBase).nX()) {
                     this.p = false;
                     break;
                  }
               }
            }
         }
      }

      iterator2 = this.list.iterator();

      while (iterator2.hasNext()) {
         ((CategoryPanel)iterator2.next()).UW(keyCode);
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 340) {
         j = false;
      }

      return super.keyReleased(keyCode, scanCode, modifiers);
   }

   public boolean charTyped(char chr, int modifiers) {
      if (this.searchOpen) {
         if (chr >= 32 && chr != 127) {
            if (this.searchQuery.length() < 35) {
               this.searchQuery += chr;
               this.onQueryChanged();
            }
            return true;
         }
      }

      Iterator iterator2 = this.list.iterator();

      while (iterator2.hasNext()) {
         ((CategoryPanel)iterator2.next()).UX(chr);
      }

      return super.charTyped(chr, modifiers);
   }

   public boolean shouldPause() {
      return false;
   }

   public void close() {
      this.closeSearch();
      this.ui.awc();
      super.close();
   }

   public void onDisplayed() {
      this.openTime = System.currentTimeMillis();
      h = false;
      i = false;
      f = false;
      g = false;
      j = false;
      colorBase = null;
   }

   public boolean shouldCloseOnEsc() {
      if (this.searchOpen) {
         return false;
      }
      return this.p;
   }

   private void KE(DrawContext context2) {
      float f2 = UI.atK().auk();
      if (f2 != 1.0F) {
         float f3 = context2.getScaledWindowHeight() / 8.0F * (f2 - 1.0F);
         context2.getMatrices().translate(0.0F, f3, 0.0F);
      }
   }

   private boolean KF() {
      Iterator iterator2 = this.list.iterator();

      while (iterator2.hasNext()) {
         if (((CategoryPanel)iterator2.next()).bK()) {
            return false;
         }
      }

      return true;
   }

   public List getList() {
      return this.list;
   }

   public DrawMatrixHelper getHoveredModule() {
      double rawMouseX = Mc.client.mouse.getX() * (double)Mc.client.getWindow().getScaledWidth() / (double)Mc.client.getWindow().getWidth();
      double rawMouseY = Mc.client.mouse.getY() * (double)Mc.client.getWindow().getScaledHeight() / (double)Mc.client.getWindow().getHeight();
      float scale = b > 0.001F ? b : 1.0F;
      float smX = (float)(rawMouseX / scale);
      float smY = (float)(rawMouseY / scale);
      float auk = UI.atK().auk();
      if (auk != 1.0F) {
         float f3 = (Mc.client.getWindow().getScaledHeight() / 8.0F * (auk - 1.0F)) / scale;
         smY -= f3;
      }

      for (Object panelObj : this.list) {
         CategoryPanel panel = (CategoryPanel)panelObj;
         if (!panel.bG()) {
            continue;
         }
         for (Object modObj : panel.getList()) {
            DrawMatrixHelper mod = (DrawMatrixHelper)modObj;
            if (mod.vC(smX, smY)) {
               return mod;
            }
            if (mod.anS()) {
               for (Object setObj : mod.getList()) {
                  if (setObj instanceof DrawBase) {
                     DrawBase set = (DrawBase) setObj;
                     if (set.getSetting().isVisible() && set.vC(smX, smY)) {
                        return mod;
                     }
                  }
               }
            }
         }
      }
      return null;
   }
}
