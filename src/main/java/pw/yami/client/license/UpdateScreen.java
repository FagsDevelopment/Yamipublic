package pw.yami.client.license;

import java.net.URI;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import pw.yami.client.Bootstrap;

/**
 * Modern auto-updating screen with 1% - 100% progress bar.
 * Appears when the game starts with an outdated jar.
 * Automatically downloads the new release in background, verifies SHA-256 and Ed25519 signature,
 * installs it directly into the mods folder, and provides an instant restart action.
 */
public final class UpdateScreen extends Screen {
   private static final String DISCORD_URL = "https://discord.gg/KcREybKEbp";

   private static final int WHITE   = 0xFFFFFFFF;
   private static final int GRAY    = 0xFFAAAAAA;
   private static final int GREEN   = 0xFF57F287;
   private static final int RED     = 0xFFED4245;
   private static final int CYAN    = 0xFF00E5FF;
   private static final int PURPLE  = 0xFFBB86FC;
   private static final int DARK_BG = 0xF012121C;

   private ButtonWidget actionButton;
   private ButtonWidget discordButton;
   private ButtonWidget quitButton;

   public UpdateScreen() {
      super(Text.literal("Yami Guncelleme"));
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   protected void init() {
      // Trigger automatic background download immediately on screen open
      if (!AutoUpdater.get().isDownloading() && !AutoUpdater.get().isDownloadSuccess()) {
         AutoUpdater.get().triggerDownloadAsync();
      }

      int buttonWidth = Math.min(260, Math.max(180, this.width - 80));
      int left = this.width / 2 - buttonWidth / 2;
      int centerY = this.height / 2;

      this.actionButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Guncelleniyor..."), button -> this.handleAction())
            .dimensions(left, centerY + 40, buttonWidth, 20)
            .build()
      );

      this.discordButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Discord Destek"), button -> this.openDiscord())
            .dimensions(left, centerY + 64, buttonWidth, 20)
            .build()
      );

      this.quitButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Oyunu Kapat"), button -> this.quit())
            .dimensions(left, centerY + 88, buttonWidth, 20)
            .build()
      );

      this.updateButtonStates();
   }

   private void updateButtonStates() {
      if (this.actionButton == null) {
         return;
      }
      boolean success = AutoUpdater.get().isDownloadSuccess();
      boolean failed = AutoUpdater.get().isDownloadFailed();
      if (success) {
         this.actionButton.setMessage(Text.literal("✔ Oyunu Yeniden Baslat"));
         this.actionButton.active = true;
      } else if (failed) {
         this.actionButton.setMessage(Text.literal("Tekrar Dene"));
         this.actionButton.active = true;
      } else {
         int pct = AutoUpdater.get().getDownloadProgress();
         this.actionButton.setMessage(Text.literal(String.format("Guncelleniyor... %%%d", Math.max(1, pct))));
         this.actionButton.active = false;
      }
   }

   private void handleAction() {
      if (AutoUpdater.get().isDownloadSuccess()) {
         this.quit();
      } else if (AutoUpdater.get().isDownloadFailed()) {
         AutoUpdater.get().triggerDownloadAsync();
         this.updateButtonStates();
      }
   }

   private void openDiscord() {
      try {
         Util.getOperatingSystem().open(URI.create(DISCORD_URL));
      } catch (Exception ignored) {
      }
   }

   private void quit() {
      if (this.client != null) {
         this.client.scheduleStop();
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.updateButtonStates();
      this.renderBackground(context, mouseX, mouseY, delta);

      int centerX = this.width / 2;
      int centerY = this.height / 2;

      // Card Background
      int cardWidth = 340;
      int cardHeight = 224;
      int cardX = centerX - cardWidth / 2;
      int cardY = centerY - 116;

      // Dark card container
      context.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, DARK_BG);
      // Card border
      context.fill(cardX, cardY, cardX + cardWidth, cardY + 1, 0xFF38384E);
      context.fill(cardX, cardY + cardHeight - 1, cardX + cardWidth, cardY + cardHeight, 0xFF38384E);
      context.fill(cardX, cardY, cardX + 1, cardY + cardHeight, 0xFF38384E);
      context.fill(cardX + cardWidth - 1, cardY, cardX + cardWidth, cardY + cardHeight, 0xFF38384E);

      // Brand Title
      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("✦ YAMI CLIENT ✦"),
         centerX, centerY - 102,
         PURPLE
      );

      boolean success = AutoUpdater.get().isDownloadSuccess();
      boolean failed = AutoUpdater.get().isDownloadFailed();
      String titleText;
      int titleColor;
      if (success) {
         titleText = "Guncelleme Basariyla Tamamlandi!";
         titleColor = GREEN;
      } else if (failed) {
         titleText = "Guncelleme Indirilemedi";
         titleColor = RED;
      } else {
         titleText = "Yeni Surum Otomatik Guncelleniyor...";
         titleColor = CYAN;
      }

      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal(titleText),
         centerX, centerY - 82,
         titleColor
      );

      // Version Information
      String currentVer = Bootstrap.b;
      String newVer = AutoUpdater.get().getLatestVersion();
      if (newVer == null || newVer.isBlank()) {
         newVer = currentVer;
      }
      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("v" + currentVer + "   ➔   v" + newVer),
         centerX, centerY - 64,
         WHITE
      );

      // Progress Bar Dimensions
      int barWidth = 280;
      int barHeight = 18;
      int barX = centerX - barWidth / 2;
      int barY = centerY - 44;

      // Bar Border & Background
      context.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF45455E);
      context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF14141E);

      int progress = AutoUpdater.get().getDownloadProgress();
      if (success) {
         progress = 100;
      } else if (progress < 1 && AutoUpdater.get().isDownloading()) {
         progress = 1;
      }
      int fillWidth = Math.max(0, Math.min(barWidth, (barWidth * progress) / 100));

      if (fillWidth > 0) {
         int fillColor = success ? 0xFF57F287 : 0xFF00C9A7;
         context.fill(barX, barY, barX + fillWidth, barY + barHeight, fillColor);
      }

      // Percentage label centered inside the bar
      String percentLabel;
      if (success) {
         percentLabel = "%100 Tamamlandi";
      } else if (failed) {
         percentLabel = "Hata";
      } else {
         percentLabel = String.format("%%%d Yukleniyor", Math.max(1, progress));
      }
      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal(percentLabel),
         centerX, barY + 5,
         WHITE
      );

      // Sub-status text
      String statusMsg = AutoUpdater.get().getUpdateStatus();
      if (statusMsg == null || statusMsg.isBlank()) {
         statusMsg = "Lutfen bekleyin, dosya mods klasorune aktariliyor...";
      }
      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal(statusMsg),
         centerX, centerY - 18,
         GRAY
      );

      // Hint line
      if (success) {
         context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("Mods klasorundeki dosya guncellendi. Oyunu yeniden baslatin."),
            centerX, centerY + 3,
            GREEN
         );
      } else if (!failed) {
         context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("Yeni surum mods klasorune otomatik kuruluyor..."),
            centerX, centerY + 3,
            0xFF8A8AA5
         );
      }

      super.render(context, mouseX, mouseY, delta);
   }
}
