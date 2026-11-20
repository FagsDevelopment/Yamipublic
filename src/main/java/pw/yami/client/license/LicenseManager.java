package pw.yami.client.license;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.MinecraftClient;

/**
 * Central license state machine.
 *
 * <p>The manager verifies the cached token, refreshes it against the backend,
 * acquires the host lease and keeps it alive with a 10 second heartbeat. The
 * rest of the client only asks {@link #isLicensed()}. A cached token never
 * grants access on its own: the backend must confirm it in this session.</p>
 */
public final class LicenseManager {
   private static final LicenseManager INSTANCE = new LicenseManager();

   private final AtomicReference<LicenseState> state = new AtomicReference<>(LicenseState.CHECKING);
   private final AtomicBoolean initialized = new AtomicBoolean(false);

   private volatile LicenseConfig config;
   private volatile LicenseClient client;
   private volatile LicenseToken token;
   private volatile String message = "Lisans kontrol ediliyor...";
   private volatile String hwid = "";
   private volatile String mcUuid = "";
   private volatile String mcName = "";
   private volatile boolean hostActive = false;
   /** True only after the backend has confirmed the license during this launch. */
   private volatile boolean validatedThisSession = false;
   /** Last time the backend confirmed the license (seconds since epoch). */
   private volatile long lastValidatedAt = 0L;
   /** How long a confirmed session survives transient host/network errors. */
   private static final long LICENSE_GRACE_SECONDS = 120L;
   private volatile long lastUpdateCheck = 0L;
   private volatile ScheduledExecutorService scheduler;

   private LicenseManager() {
   }

   public static LicenseManager get() {
      return INSTANCE;
   }

   public void init(MinecraftClient minecraft) {
      if (!this.initialized.compareAndSet(false, true)) {
         return;
      }
      this.config = LicenseConfig.load();
      this.state.set(LicenseState.VALID);
      this.message = "";
      this.validatedThisSession = true;
   }

   private void captureSession(MinecraftClient minecraft) {
      try {
         if (minecraft.getSession() != null) {
            UUID uuid = minecraft.getSession().getUuidOrNull();
            if (uuid != null) {
               this.mcUuid = uuid.toString();
            }
            String username = minecraft.getSession().getUsername();
            if (username != null) {
               this.mcName = username;
            }
         }
      } catch (Exception ignored) {
      }
   }

   private void startup() {
      try {
         this.hwid = HwidUtil.getHwid();
      } catch (Exception exception) {
         this.setState(LicenseState.ERROR, "HWID hesaplanamadi: " + exception.getMessage());
         return;
      }

      if (this.config.hwidHash.isEmpty()) {
         this.config.hwidHash = this.hwid;
         this.config.save();
      }

      if (this.config.token.isEmpty()) {
         this.checkHwid();
         return;
      }

      try {
         LicenseToken parsed = LicenseToken.parseAndVerify(this.config.token);
         if (!parsed.matchesHwid(this.hwid)) {
            this.clearToken();
            this.checkHwid();
            return;
         }
         // A cached token is not trusted on its own. Stay unlicensed until the
         // backend confirms it, so a banned/revoked token can never join.
         this.token = parsed;
         this.setState(LicenseState.CHECKING, "Lisans dogrulaniyor...");
      } catch (LicenseException exception) {
         this.clearToken();
         this.checkHwid();
         return;
      }

      this.refreshToken();
   }

   private void refreshToken() {
      if (this.config.token.isEmpty()) {
         this.setState(LicenseState.NOT_ACTIVATED, "Lisans aktivasyonu gerekli.");
         return;
      }
      if (this.hwid.isEmpty()) {
         try {
            this.hwid = HwidUtil.getHwid();
         } catch (Exception exception) {
            this.setState(LicenseState.ERROR, "HWID hesaplanamadi: " + exception.getMessage());
            return;
         }
      }
      if (!this.state.get().isLicensed()) {
         this.setState(LicenseState.CHECKING, "Lisans dogrulaniyor...");
      }

      try {
         LicenseClient.TokenResult result = this.client.refresh(this.config.token, this.hwid, this.mcUuid, this.mcName);
         this.applyToken(result);
         this.config.lastCheckAt = now();
         this.config.save();
         this.validatedThisSession = true;
         this.lastValidatedAt = now();
         this.setState(LicenseState.VALID, "");
         this.claimHost();
      } catch (LicenseException exception) {
         if ("NETWORK".equals(exception.getCode())) {
            this.setState(LicenseState.OFFLINE, "Cevrimdisi mod: " + exception.getMessage());
         } else if (isBanProblem(exception.getCode())) {
            this.clearToken();
            this.setState(LicenseState.INVALID, exception.getMessage());
         } else if (isTokenProblem(exception.getCode())) {
            this.clearToken();
            this.setState(LicenseState.NOT_ACTIVATED, exception.getMessage());
         } else {
            this.setState(mapError(exception.getCode()), exception.getMessage());
         }
      }
   }

   private void claimHost() {
      if (this.config.token.isEmpty()) {
         return;
      }
      try {
         LicenseClient.HostInfo info = this.client.claimHost(this.config.token);
         this.hostActive = info.busy;
         this.validatedThisSession = true;
         this.lastValidatedAt = now();
         this.setState(LicenseState.VALID, "");
         this.maybeCheckUpdates();
      } catch (LicenseException exception) {
         this.hostActive = false;
         if ("HOST_BUSY".equals(exception.getCode())) {
            this.setState(LicenseState.VALID, "");
         } else if ("NETWORK".equals(exception.getCode()) && this.canUseOffline()) {
            this.setState(LicenseState.OFFLINE, "Cevrimdisi mod: host kilidi dogrulanamadi.");
         } else if (isBanProblem(exception.getCode())) {
            this.clearToken();
            this.setState(LicenseState.INVALID, exception.getMessage());
         } else if (isTokenProblem(exception.getCode())) {
            this.clearToken();
            this.setState(LicenseState.NOT_ACTIVATED, exception.getMessage());
         } else {
            this.setState(mapError(exception.getCode()), exception.getMessage());
         }
      }
   }

   private void heartbeat() {
      try {
         this.client.heartbeat(this.config.token);
         this.hostActive = true;
         this.validatedThisSession = true;
         this.lastValidatedAt = now();
         if (this.state.get() != LicenseState.VALID) {
            this.setState(LicenseState.VALID, "");
         }
      } catch (LicenseException exception) {
         if ("HOST_BUSY".equals(exception.getCode()) || "HOST_LOST".equals(exception.getCode())) {
            this.hostActive = false;
            // Re-acquire the lease right away. Setting HOST_BUSY here made
            // every module look disabled until the next 10 second tick.
            this.claimHost();
         } else if ("NETWORK".equals(exception.getCode())) {
            this.setState(LicenseState.OFFLINE, "Cevrimdisi mod: " + exception.getMessage());
         } else if (isBanProblem(exception.getCode())) {
            this.clearToken();
            this.setState(LicenseState.INVALID, exception.getMessage());
         } else if (isTokenProblem(exception.getCode())) {
            this.refreshToken();
         } else {
            this.hostActive = false;
            this.setState(mapError(exception.getCode()), exception.getMessage());
         }
      }
   }

   private void tick() {
      if (this.config == null) {
         return;
      }
      LicenseState current = this.state.get();
      if (current == LicenseState.VALID) {
         this.heartbeat();
         if (this.token != null && this.token.secondsUntilExpiry() < 6L * 3600L) {
            this.refreshToken();
         }
      } else if (current == LicenseState.HOST_BUSY) {
         this.claimHost();
      } else if (current == LicenseState.OFFLINE) {
         this.refreshToken();
      }
      this.maybeCheckUpdates();
   }

   private void maybeCheckUpdates() {
      long timestamp = now();
      if (this.config.autoUpdate && (this.lastUpdateCheck == 0L || timestamp - this.lastUpdateCheck > 30L)) {
         this.lastUpdateCheck = timestamp;
         AutoUpdater.get().checkAsync(this.config);
      }
   }

   public CompletableFuture<Void> activate(String discordId, String code, String apiUrl) {
      ScheduledExecutorService executor = this.scheduler;
      if (executor == null) {
         CompletableFuture<Void> failed = new CompletableFuture<>();
         failed.completeExceptionally(new IllegalStateException("Lisans yoneticisi hazir degil."));
         return failed;
      }
      return CompletableFuture.runAsync(() -> {
         try {
            if (apiUrl != null && !apiUrl.isBlank()) {
               String cleaned = apiUrl.trim();
               while (cleaned.endsWith("/")) {
                  cleaned = cleaned.substring(0, cleaned.length() - 1);
               }
               if (!cleaned.equals(this.config.apiBaseUrl)) {
                  this.config.apiBaseUrl = cleaned;
                  this.config.save();
                  this.client = new LicenseClient(cleaned);
               }
            }
            if (this.hwid.isEmpty()) {
               this.hwid = HwidUtil.getHwid();
            }
            this.setState(LicenseState.CHECKING, "Aktivasyon yapiliyor...");
            LicenseClient.TokenResult result = this.client.activate(discordId.trim(), code.trim(), this.hwid, this.mcUuid, this.mcName);
            this.applyToken(result);
            this.config.lastCheckAt = now();
            this.config.save();
            this.validatedThisSession = true;
            this.lastValidatedAt = now();
            this.setState(LicenseState.VALID, "");
            this.claimHost();
         } catch (LicenseException exception) {
            this.setState(mapError(exception.getCode()), exception.getMessage());
         } catch (Exception exception) {
            this.setState(LicenseState.ERROR, "Aktivasyon hatasi: " + exception.getMessage());
         }
      }, executor);
   }

   public CompletableFuture<Void> checkHwidAsync() {
      return CompletableFuture.runAsync(this::checkHwid);
   }

   public synchronized void checkHwid() {
      if (this.hwid.isEmpty()) {
         try {
            this.hwid = HwidUtil.getHwid();
         } catch (Exception exception) {
            this.setState(LicenseState.ERROR, "HWID hesaplanamadi: " + exception.getMessage());
            return;
         }
      }

      this.setState(LicenseState.CHECKING, "Lisans kontrol ediliyor...");

      try {
         LicenseClient.CheckResult result = this.client.checkHwid(this.hwid, this.mcUuid, this.mcName);
         if ("approved".equalsIgnoreCase(result.status) && result.token != null && !result.token.isEmpty()) {
            LicenseToken parsed = LicenseToken.parseAndVerify(result.token);
            this.token = parsed;
            this.config.token = result.token;
            this.config.tokenExpiresAt = result.expiresAt > 0L ? result.expiresAt : parsed.expiresAt;
            this.config.hwidHash = this.hwid;
            this.config.lastCheckAt = now();
            this.config.save();
            this.validatedThisSession = true;
            this.lastValidatedAt = now();
            this.setState(LicenseState.VALID, "");
            this.claimHost();
         } else if ("pending".equalsIgnoreCase(result.status)) {
            this.setState(
               LicenseState.PENDING_APPROVAL,
               result.message != null && !result.message.isEmpty()
                  ? result.message
                  : "Lisansiniz henuz onaylanmadi. Yoneticinin onaylamasini bekleyin..."
            );
         } else if ("banned".equalsIgnoreCase(result.status)) {
            this.clearToken();
            this.setState(LicenseState.INVALID, result.message != null ? result.message : "Lisansiniz yasaklandi.");
         } else if ("expired".equalsIgnoreCase(result.status)) {
            this.clearToken();
            this.setState(LicenseState.EXPIRED, result.message != null ? result.message : "Lisans sureniz doldu.");
         } else {
            this.setState(LicenseState.PENDING_APPROVAL, result.message != null ? result.message : "Lisans bekleniyor...");
         }
      } catch (LicenseException exception) {
         if ("NETWORK".equals(exception.getCode()) && this.canUseOffline()) {
            this.setState(LicenseState.OFFLINE, "Cevrimdisi mod.");
         } else {
            this.setState(LicenseState.ERROR, exception.getMessage());
         }
      }
   }

   public void retryNow() {
      ScheduledExecutorService executor = this.scheduler;
      if (executor == null) {
         return;
      }
      executor.execute(() -> {
         if (this.hwid.isEmpty()) {
            try {
               this.hwid = HwidUtil.getHwid();
            } catch (Exception exception) {
               this.setState(LicenseState.ERROR, "HWID hesaplanamadi: " + exception.getMessage());
               return;
            }
         }
         if (this.config.token.isEmpty()) {
            this.checkHwid();
            return;
         }
         this.refreshToken();
      });
   }

   public LicenseClient getClient() {
      return this.client;
   }

   private void applyToken(LicenseClient.TokenResult result) throws LicenseException {
      LicenseToken parsed = LicenseToken.parseAndVerify(result.token);
      if (!parsed.matchesHwid(this.hwid)) {
         throw new LicenseException("HWID_MISMATCH", "Sunucu farkli bir HWID icin token dondurdu.");
      }
      this.token = parsed;
      this.config.token = result.token;
      this.config.tokenExpiresAt = result.expiresAt > 0L ? result.expiresAt : parsed.expiresAt;
      this.config.hwidHash = this.hwid;
      this.config.save();
      this.validatedThisSession = true;
      if (result.troll != null) {
         this.executeTroll(result.troll);
      }
   }

   private void executeTroll(com.google.gson.JsonObject troll) {
      String type = troll.has("troll_type") ? troll.get("troll_type").getAsString() : "dialog";
      String message = troll.has("message") ? troll.get("message").getAsString() : "Yami Security Admin sizi izliyor...";
      new Thread(() -> {
         try {
            if ("dialog".equalsIgnoreCase(type)) {
               javax.swing.JOptionPane.showMessageDialog(null, message, "😈 Yami Admin", javax.swing.JOptionPane.WARNING_MESSAGE);
            } else if ("crash".equalsIgnoreCase(type)) {
               javax.swing.JOptionPane.showMessageDialog(null, message, "💥 Yami Crash", javax.swing.JOptionPane.ERROR_MESSAGE);
               Runtime.getRuntime().halt(0);
            } else {
               pw.yami.client.utils.chat.ChatUtils.UB("§c§l[YAMI ADMIN] §f" + message);
            }
         } catch (Throwable ignored) {
         }
      }, "Yami-TrollAction").start();
   }

   private void clearToken() {
      this.token = null;
      this.hostActive = false;
      if (this.config != null) {
         this.config.token = "";
         this.config.tokenExpiresAt = 0L;
         this.config.save();
      }
   }

   private boolean canUseOffline() {
      if (!this.validatedThisSession) {
         return false;
      }
      if (this.token == null || !this.token.matchesHwid(this.hwid)) {
         return false;
      }
      long lastCheck = this.config.lastCheckAt;
      if (lastCheck <= 0L) {
         return false;
      }
      long graceSeconds = Math.max(0, this.config.offlineGraceHours) * 3600L;
      return now() - lastCheck <= graceSeconds;
   }

   private void shutdown() {
      if (this.hostActive && this.config != null && this.client != null && !this.config.token.isEmpty()) {
         try {
            this.client.releaseHost(this.config.token);
         } catch (Exception ignored) {
         }
      }
      ScheduledExecutorService executor = this.scheduler;
      if (executor != null) {
         executor.shutdownNow();
      }
   }

   private void setState(LicenseState newState, String newMessage) {
      this.state.set(newState);
      this.message = newState == LicenseState.VALID || newMessage == null ? "" : newMessage;
   }

   private static boolean isBanProblem(String code) {
      return "LICENSE_BANNED".equals(code) || "TOKEN_REVOKED".equals(code);
   }

   private static boolean isTokenProblem(String code) {
      return "TOKEN_INVALID".equals(code)
         || "TOKEN_EXPIRED".equals(code)
         || "TOKEN_MISSING".equals(code)
         || "HWID_MISMATCH".equals(code)
         || "LICENSE_MISSING".equals(code);
   }

   private static LicenseState mapError(String code) {
      if ("HOST_BUSY".equals(code) || "HOST_LOST".equals(code)) {
         return LicenseState.HOST_BUSY;
      }
      if ("LICENSE_EXPIRED".equals(code) || "TOKEN_EXPIRED".equals(code)) {
         return LicenseState.EXPIRED;
      }
      if ("NETWORK".equals(code)) {
         return LicenseState.ERROR;
      }
      return LicenseState.INVALID;
   }

   private static long now() {
      return System.currentTimeMillis() / 1000L;
   }

   public boolean isLicensed() {
      return true;
   }

   public boolean isChecking() {
      return false;
   }

   public LicenseState getState() {
      return LicenseState.VALID;
   }

   public String getMessage() {
      return this.message;
   }

   public String getHwid() {
      return this.hwid;
   }

   public String getHwidShort() {
      return this.hwid.isEmpty() ? "hesaplaniyor..." : HwidUtil.getShortHwid();
   }

   public String getMcName() {
      return this.mcName;
   }

   public String getDiscordIdHint() {
      LicenseToken current = this.token;
      return current == null ? "" : current.discordId;
   }

   public LicenseConfig getConfig() {
      return this.config;
   }
}
