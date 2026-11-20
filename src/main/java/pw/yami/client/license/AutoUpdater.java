package pw.yami.client.license;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.loader.api.FabricLoader;
import pw.yami.client.Bootstrap;
import pw.yami.client.utils.chat.ChatUtils;

/**
 * Checks the signed update manifest, downloads the jar and verifies both the
 * SHA-256 hash and the Ed25519 signature before staging it.
 */
public final class AutoUpdater {
   private static final AutoUpdater INSTANCE = new AutoUpdater();
   private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

   private final AtomicBoolean running = new AtomicBoolean(false);
   private volatile boolean updateRequired = false;
   private volatile String latestVersion = "";
   private volatile String downloadUrl = "";
   private volatile int downloadProgress = 0;
   private volatile long downloadedBytes = 0L;
   private volatile long totalBytes = 0L;
   private volatile String updateStatus = "Guncelleme bekleniyor...";
   private volatile boolean downloadSuccess = false;
   private volatile boolean downloadFailed = false;
   private volatile String errorMessage = "";

   private AutoUpdater() {
   }

   public static AutoUpdater get() {
      return INSTANCE;
   }

   public boolean isUpdateRequired() {
      return this.updateRequired;
   }

   public String getLatestVersion() {
      return this.latestVersion;
   }

   public String getDownloadUrl() {
      return this.downloadUrl;
   }

   public int getDownloadProgress() {
      return this.downloadProgress;
   }

   public long getDownloadedBytes() {
      return this.downloadedBytes;
   }

   public long getTotalBytes() {
      return this.totalBytes;
   }

   public String getUpdateStatus() {
      return this.updateStatus;
   }

   public boolean isDownloadSuccess() {
      return this.downloadSuccess;
   }

   public boolean isDownloadFailed() {
      return this.downloadFailed;
   }

   public String getErrorMessage() {
      return this.errorMessage;
   }

   public boolean isDownloading() {
      return this.running.get();
   }

   public void triggerDownloadAsync() {
      LicenseConfig cfg = LicenseManager.get().getConfig();
      if (cfg != null) {
         this.checkAsync(cfg);
      }
   }

   public void checkAsync(LicenseConfig config) {
      if (config == null || !config.autoUpdate || config.updateRepo == null || config.updateRepo.isBlank()) {
         return;
      }
      if (!this.running.compareAndSet(false, true)) {
         return;
      }
      Thread thread = new Thread(() -> {
         try {
            this.check(config);
         } catch (Exception exception) {
            Bootstrap.Ob("Guncelleme kontrolu basarisiz: " + exception.getMessage());
         } finally {
            this.running.set(false);
         }
      }, "Yami-Update");
      thread.setDaemon(true);
      thread.start();
   }

   private void check(LicenseConfig config) {
      JsonObject manifest = this.fetchManifest(config);
      String remoteVersion = string(manifest, "version");
      String currentVersion = currentVersion();
      String sha256 = string(manifest, "sha256");
      String signature = string(manifest, "signature");
      String jarName = string(manifest, "jar");
      if (remoteVersion.isEmpty() || sha256.isEmpty() || signature.isEmpty() || jarName.isEmpty()) {
         Bootstrap.Ob("Guncelleme manifesti eksik alanlar iceriyor.");
         return;
      }

      boolean mandatory = bool(manifest, "mandatory");
      String proxyUrl = string(manifest, "proxy_download_url");
      if (!proxyUrl.isEmpty() && proxyUrl.startsWith("/")) {
         proxyUrl = config.apiBaseUrl + proxyUrl;
      }
      String downloadUrl = string(manifest, "download_url");
      if (downloadUrl.isEmpty()) {
         String tag = string(manifest, "tag");
         if (tag.isEmpty()) {
            tag = "v" + remoteVersion;
         }
         downloadUrl = "https://github.com/" + config.updateRepo + "/releases/download/" + tag + "/" + jarName;
      }

      String primaryUrl = !proxyUrl.isEmpty() ? proxyUrl : downloadUrl;
      String fallbackUrl = primaryUrl.equals(downloadUrl) ? "" : downloadUrl;

      boolean isNewerVer = isNewer(remoteVersion, currentVersion);
      boolean sameVersion = remoteVersion.equalsIgnoreCase(currentVersion);
      String localHash = localJarHash();
      boolean hashDiffers = !localHash.isEmpty() && !sha256.equalsIgnoreCase(localHash);

      if (isNewerVer || (sameVersion && hashDiffers) || mandatory) {
         this.latestVersion = remoteVersion;
         this.downloadUrl = primaryUrl;
         this.updateRequired = true;
         Bootstrap.NZ("Yeni guncelleme yayinlandi: " + remoteVersion + " (Zorunlu mod aktif)");
      } else {
         Bootstrap.NZ("Guncelleme yok (mevcut " + currentVersion + ", uzak " + remoteVersion + ").");
         return;
      }

      long expectedSize = getLong(manifest, "size");
      this.totalBytes = expectedSize;
      this.downloadedBytes = 0L;
      this.downloadProgress = 1;
      this.downloadSuccess = false;
      this.downloadFailed = false;
      this.updateStatus = "Yeni surum indiriliyor...";

      Path updateDir = FabricLoader.getInstance().getGameDir().resolve("Yami").resolve("update");
      try {
         Files.createDirectories(updateDir);
      } catch (Exception exception) {
         this.downloadFailed = true;
         this.errorMessage = exception.getMessage();
         this.updateStatus = "Guncelleme klasoru olusturulamadi: " + exception.getMessage();
         Bootstrap.Ob(this.updateStatus);
         return;
      }

      Path temporary = updateDir.resolve(jarName + ".part");
      Path staged = updateDir.resolve(jarName);
      try {
         Bootstrap.NZ("Guncelleme indiriliyor: " + remoteVersion + " (" + jarName + ")");
         try {
            this.download(primaryUrl, temporary, expectedSize);
         } catch (Exception directFailure) {
            if (fallbackUrl.isEmpty() || fallbackUrl.equals(primaryUrl)) {
               throw directFailure;
            }
            Bootstrap.NZ("Render indirmesi basarisiz, alternatif adres deneniyor: " + fallbackUrl);
            this.download(fallbackUrl, temporary, expectedSize);
         }
         this.downloadProgress = 96;
         this.updateStatus = "SHA-256 dogrulaniyor...";
         String actualHash = CryptoUtil.sha256(temporary);
         if (!actualHash.equalsIgnoreCase(sha256)) {
            Files.deleteIfExists(temporary);
            this.downloadFailed = true;
            this.errorMessage = "SHA-256 dogrulamasi basarisiz.";
            this.updateStatus = this.errorMessage;
            Bootstrap.Ob(this.errorMessage);
            return;
         }
         this.downloadProgress = 97;
         this.updateStatus = "Ed25519 dijital imza dogrulaniyor...";
         if (!CryptoUtil.verifyEd25519(temporary, signature, Keys.updatePublicKey())) {
            Files.deleteIfExists(temporary);
            this.downloadFailed = true;
            this.errorMessage = "Ed25519 imza dogrulamasi basarisiz.";
            this.updateStatus = this.errorMessage;
            Bootstrap.Ob(this.errorMessage);
            return;
         }
         this.downloadProgress = 98;
         this.updateStatus = "Mods klasorune aktariliyor...";
         Files.move(temporary, staged, StandardCopyOption.REPLACE_EXISTING);
         UpdateApplier.stage(staged, UpdateApplier.currentJarName(), remoteVersion);
         UpdateApplier.applyPending();
         UpdateApplier.scheduleInstallAfterExit(staged, UpdateApplier.currentJarName());
         this.downloadProgress = 100;
         this.downloadSuccess = true;
         this.updateStatus = "Guncelleme tamamlandi! Oyunu yeniden baslatin.";
         Bootstrap.NZ("Guncelleme hazir: " + remoteVersion + ". Mods klasorune kuruldu.");
         ChatUtils.UB("§b[Yami] §aGuncelleme kuruldu (v" + remoteVersion + ")! Oyunu yeniden baslattiginizda aktif olacak.");
      } catch (Exception exception) {
         this.downloadFailed = true;
         this.errorMessage = exception.getMessage();
         this.updateStatus = "Guncelleme indirilemedi: " + exception.getMessage();
         try {
            Files.deleteIfExists(temporary);
         } catch (Exception ignored) {
         }
         Bootstrap.Ob(this.updateStatus);
      }
   }

   private JsonObject fetchManifest(LicenseConfig config) {
      try {
         JsonObject local = this.getJson(config.apiBaseUrl + "/update/latest?channel=" + config.updateChannel, "application/json");
         if (local != null) {
            return local;
         }
      } catch (Exception exception) {
         Bootstrap.NZ("Backend guncelleme kontrolu basarisiz, GitHub deneniyor: " + exception.getMessage());
      }

      try {
         JsonElement releasesElement = this.getJsonElement(
            "https://api.github.com/repos/" + config.updateRepo + "/releases?per_page=20",
            "application/vnd.github+json"
         );
         if (releasesElement == null || !releasesElement.isJsonArray()) {
            throw new IllegalStateException("GitHub release listesi alinamadi.");
         }
         JsonArray releases = releasesElement.getAsJsonArray();
         for (JsonElement element : releases) {
            if (!element.isJsonObject()) {
               continue;
            }
            JsonObject release = element.getAsJsonObject();
            if (bool(release, "draft")) {
               continue;
            }
            if (bool(release, "prerelease") && !"beta".equalsIgnoreCase(config.updateChannel)) {
               continue;
            }
            JsonObject manifest = this.manifestFromRelease(release);
            if (manifest != null) {
               return manifest;
            }
         }
      } catch (Exception exception) {
         Bootstrap.Ob("GitHub guncelleme kontrolu basarisiz: " + exception.getMessage());
      }
      return new JsonObject();
   }

   private JsonObject manifestFromRelease(JsonObject release) {
      JsonElement assetsElement = release.get("assets");
      if (assetsElement == null || !assetsElement.isJsonArray()) {
         return null;
      }
      JsonArray assets = assetsElement.getAsJsonArray();
      JsonObject manifestAsset = null;
      for (JsonElement element : assets) {
         if (element.isJsonObject() && "update.json".equals(string(element.getAsJsonObject(), "name"))) {
            manifestAsset = element.getAsJsonObject();
            break;
         }
      }
      if (manifestAsset == null) {
         return null;
      }
      try {
         String manifestUrl = string(manifestAsset, "browser_download_url");
         JsonObject manifest = this.getJson(manifestUrl, "application/octet-stream");
         if (manifest == null) {
            return null;
         }
         String jarName = string(manifest, "jar");
         for (JsonElement element : assets) {
            if (element.isJsonObject() && jarName.equals(string(element.getAsJsonObject(), "name"))) {
               manifest.addProperty("download_url", string(element.getAsJsonObject(), "browser_download_url"));
               break;
            }
         }
         manifest.addProperty("tag", string(release, "tag_name"));
         manifest.addProperty("html_url", string(release, "html_url"));
         return manifest;
      } catch (Exception exception) {
         Bootstrap.Ob("Release manifesti okunamadi: " + exception.getMessage());
         return null;
      }
   }

   private JsonObject getJson(String url, String accept) throws Exception {
      JsonElement element = this.getJsonElement(url, accept);
      return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
   }

   private JsonElement getJsonElement(String url, String accept) throws Exception {
      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
         .timeout(Duration.ofSeconds(30))
         .header("Accept", accept)
         .header("User-Agent", "YamiClient/" + Bootstrap.b)
         .GET()
         .build();
      HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() >= 400) {
         throw new IllegalStateException("HTTP " + response.statusCode());
      }
      return JsonParser.parseString(response.body());
   }

   private void download(String url, Path target, long expectedSize) throws Exception {
      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
         .timeout(Duration.ofSeconds(300))
         .header("User-Agent", "YamiClient/" + Bootstrap.b)
         .GET()
         .build();
      HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() >= 400) {
         try (InputStream ignored = response.body()) {
         }
         throw new IllegalStateException("HTTP " + response.statusCode());
      }
      long length = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
      if (length <= 0) {
         length = expectedSize;
      }
      this.totalBytes = length > 0 ? length : expectedSize;
      this.downloadedBytes = 0L;
      this.downloadProgress = 1;

      try (InputStream input = response.body();
           java.io.OutputStream output = Files.newOutputStream(
              target,
              java.nio.file.StandardOpenOption.CREATE,
              java.nio.file.StandardOpenOption.WRITE,
              java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
         byte[] buffer = new byte[65536];
         int bytesRead;
         while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
            this.downloadedBytes += bytesRead;
            if (this.totalBytes > 0) {
               int pct = (int) Math.min(95, Math.max(1, (this.downloadedBytes * 100) / this.totalBytes));
               this.downloadProgress = pct;
               this.updateStatus = String.format("Yukleniyor: %%%d (%.1f MB / %.1f MB)", pct, this.downloadedBytes / 1048576.0, this.totalBytes / 1048576.0);
            } else {
               this.updateStatus = String.format("Yukleniyor: %.1f MB", this.downloadedBytes / 1048576.0);
            }
         }
      }
   }

   private static long getLong(JsonObject json, String key) {
      if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
         try {
            return json.get(key).getAsLong();
         } catch (Exception ignored) {
         }
      }
      return 0L;
   }

   public static String currentVersion() {
      try {
         return FabricLoader.getInstance()
            .getModContainer("yami")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse(Bootstrap.b);
      } catch (Exception exception) {
         return Bootstrap.b;
      }
   }

   private static String localJarHash() {
      try {
         Path jar = UpdateApplier.currentJarPath();
         if (jar != null && Files.isRegularFile(jar)) {
            return CryptoUtil.sha256(jar);
         }
      } catch (Exception ignored) {
      }
      return "";
   }

   private static boolean isNewer(String remote, String current) {
      String[] remoteParts = remote.split("[._+-]");
      String[] currentParts = current.split("[._+-]");
      int length = Math.max(remoteParts.length, currentParts.length);
      for (int index = 0; index < length; index++) {
         int left = index < remoteParts.length ? number(remoteParts[index]) : 0;
         int right = index < currentParts.length ? number(currentParts[index]) : 0;
         if (left != right) {
            return left > right;
         }
      }
      return false;
   }

   private static int number(String value) {
      try {
         StringBuilder digits = new StringBuilder();
         for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!Character.isDigit(character)) {
               break;
            }
            digits.append(character);
         }
         return digits.isEmpty() ? 0 : Integer.parseInt(digits.toString());
      } catch (Exception exception) {
         return 0;
      }
   }

   private static String string(JsonObject json, String key) {
      return json != null && json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
   }

   private static boolean bool(JsonObject json, String key) {
      return json.has(key) && !json.get(key).isJsonNull() && json.get(key).getAsBoolean();
   }
}
