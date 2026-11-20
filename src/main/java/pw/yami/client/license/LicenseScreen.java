package pw.yami.client.license;

import java.net.URI;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * License waiting screen.
 * Shows "Yonetici tarafindan onaylatilmaniz bekleniyor" and Discord support link.
 * Automatically polls the backend every 2.5 seconds.
 * As soon as the admin runs `!license add <hwid>`, this screen closes
 * automatically and the game starts without needing a restart!
 */
public final class LicenseScreen extends Screen {
   private static final String DISCORD_URL = "https://discord.gg/KcREybKEbp";

   private static final int WHITE   = 0xFFFFFFFF;
   private static final int GRAY    = 0xFFAAAAAA;
   private static final int GREEN   = 0xFF57F287;
   private static final int RED     = 0xFFED4245;
   private static final int YELLOW  = 0xFFFEE75C;
   private static final int BLUE    = 0xFF5865F2;
   private static final int BLACK   = 0xFF000000;

   private ButtonWidget discordButton;
   private ButtonWidget copyButton;
   private ButtonWidget quitButton;
   private String status = "";
   private boolean busy = false;
   private long lastAutoPoll = 0L;
   private int dotAnimation = 0;
   private long lastDotTick = 0L;

   public LicenseScreen() {
      super(Text.literal("Yami Lisans"));
   }

   @Override
   protected void init() {
      int buttonWidth = Math.min(260, Math.max(180, this.width - 80));
      int left = this.width / 2 - buttonWidth / 2;
      int centerY = this.height / 2;

      this.discordButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Discord Destek"), button -> this.openDiscord())
            .dimensions(left, centerY + 30, buttonWidth, 20)
            .build()
      );

      this.copyButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal("HWID Kopyala"), button -> this.copyHwid())
            .dimensions(left, centerY + 54, buttonWidth, 20)
            .build()
      );

      this.quitButton = this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Oyunu Kapat"), button -> this.quit())
            .dimensions(left, centerY + 78, buttonWidth, 20)
            .build()
      );

      if (this.status.isBlank()) {
         this.status = LicenseManager.get().getMessage();
      }
   }

   @Override
   public void tick() {
      if (AutoUpdater.get().isUpdateRequired()) {
         if (this.client != null) {
            this.client.setScreen(new UpdateScreen());
         }
         return;
      }
      LicenseManager manager = LicenseManager.get();
      if (manager.isLicensed()) {
         this.close();
         return;
      }
      if (!this.busy) {
         this.status = manager.getMessage();
         long now = System.currentTimeMillis();

         // Dot animation every 500ms
         if (now - this.lastDotTick >= 500L) {
            this.lastDotTick = now;
            this.dotAnimation = (this.dotAnimation + 1) % 4;
         }

         // Auto-check backend every 2.5 seconds
         if (now - this.lastAutoPoll >= 2500L) {
            this.lastAutoPoll = now;
            manager.checkHwidAsync().whenComplete((unused, throwable) -> {
               if (this.client != null) {
                  this.client.execute(() -> {
                     if (LicenseManager.get().isLicensed()) {
                        this.close();
                     }
                  });
               }
            });
         }
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      // Pure black background
      context.fill(0, 0, this.width, this.height, BLACK);

      int centerX = this.width / 2;
      int centerY = this.height / 2;

      // Brand Title
      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("YAMI CLIENT"),
         centerX, centerY - 90,
         0xFFBB86FC
      );

      // Animated dots: "", ".", "..", "..."
      String dots = ".".repeat(this.dotAnimation);
      LicenseState state = LicenseManager.get().getState();

      String waitText;
      int statusColor;
      if (state == LicenseState.INVALID || state == LicenseState.ERROR) {
         waitText = "Lisans Reddedildi";
         statusColor = RED;
      } else {
         waitText = "Yonetici tarafindan onaylatilmaniz bekleniyor" + dots;
         statusColor = YELLOW;
      }

      // Main message
      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal(waitText),
         centerX, centerY - 55,
         statusColor
      );

      // Discord Support Info
      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("destek icin discord:  " + DISCORD_URL),
         centerX, centerY - 35,
         BLUE
      );

      // HWID line
      String hwidShort = LicenseManager.get().getHwidShort();
      if (hwidShort != null && !hwidShort.isEmpty()) {
         context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal("HWID: " + hwidShort),
            centerX, centerY - 10,
            GRAY
         );
      }

      // If user copied or custom message
      if (this.status != null && !this.status.isBlank() && !this.status.contains("onaylatilmaniz")) {
         context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal(this.status),
            centerX, centerY + 8,
            GREEN
         );
      }

      // Render buttons
      super.render(context, mouseX, mouseY, delta);

      // Bottom info
      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("Yonetici onayladiginda oyun otomatik acilacaktir"),
         centerX, this.height - 30,
         0xFF666666
      );
      context.drawCenteredTextWithShadow(
         this.textRenderer,
         Text.literal("Yami " + pw.yami.client.Bootstrap.b),
         centerX, this.height - 16,
         0xFF444444
      );
   }

   private void openDiscord() {
      try {
         Util.getOperatingSystem().open(URI.create(DISCORD_URL));
      } catch (Exception exception) {
         this.copyHwid();
         this.status = "Discord linki acilamadi, panoya kopyalandi.";
      }
   }

   private void copyHwid() {
      String hwid = LicenseManager.get().getHwid();
      if (hwid == null || hwid.isEmpty()) {
         this.status = "HWID henuz hazir degil, birkac saniye bekleyin.";
         return;
      }
      if (this.client != null) {
         this.client.keyboard.setClipboard(hwid);
      }
      this.status = "HWID panoya kopyalandi!";
   }

   private void quit() {
      if (this.client != null) {
         this.client.scheduleStop();
      }
   }

   @Override
   public void close() {
      if (this.client != null) {
         this.client.setScreen(new TitleScreen());
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }
}
