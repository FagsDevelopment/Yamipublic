package pw.yami.client.license;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

public final class LicenseConfig {
   public static final String HARDCODED_API_URL = "https://yami-license-2yrt.onrender.com";
   public static final String HARDCODED_DISCORD = "https://discord.gg/KcREybKEbp";

   public int version = 1;
   public String apiBaseUrl = HARDCODED_API_URL;
   public String updateRepo = "imocix/yami";
   public String updateChannel = "stable";
   public boolean autoUpdate = true;
   public int offlineGraceHours = 24;
   public String discordInviteUrl = HARDCODED_DISCORD;
   public String token = "";
   public long tokenExpiresAt = 0L;
   public long lastCheckAt = 0L;
   public String hwidHash = "";
   public String lastKnownVersion = "";
   /** Path to license-system directory for auto-starting backend on host machine. */
   public String hostBackendDir = "";

   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path PATH = FabricLoader.getInstance().getGameDir().resolve("Yami").resolve("license.json");

   public static LicenseConfig load() {
      try {
         if (Files.isRegularFile(PATH)) {
            String json = Files.readString(PATH, StandardCharsets.UTF_8);
            LicenseConfig config = GSON.fromJson(json, LicenseConfig.class);
            if (config != null) {
               config.normalize();
               return config;
            }
         }
      } catch (Exception exception) {
         try {
            Files.move(PATH, PATH.resolveSibling("license.json.bak"), StandardCopyOption.REPLACE_EXISTING);
         } catch (Exception ignored) {
         }
      }
      LicenseConfig config = new LicenseConfig();
      config.normalize();
      config.save();
      return config;
   }

   public synchronized void save() {
      try {
         Files.createDirectories(PATH.getParent());
         Path temporary = PATH.resolveSibling("license.json.tmp");
         Files.writeString(temporary, GSON.toJson(this), StandardCharsets.UTF_8);
         try {
            Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (Exception exception) {
            Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (Exception ignored) {
      }
   }

   private void normalize() {
      this.apiBaseUrl = HARDCODED_API_URL;
      this.discordInviteUrl = HARDCODED_DISCORD;
      this.updateRepo = trim(this.updateRepo, "imocix/yami");
      this.updateChannel = trim(this.updateChannel, "stable");
      this.token = this.token == null ? "" : this.token.trim();
      this.hwidHash = this.hwidHash == null ? "" : this.hwidHash.trim();
      this.lastKnownVersion = this.lastKnownVersion == null ? "" : this.lastKnownVersion.trim();
      this.hostBackendDir = this.hostBackendDir == null ? "" : this.hostBackendDir.trim();
      if (this.offlineGraceHours < 0) {
         this.offlineGraceHours = 0;
      }
      if (this.offlineGraceHours > 168) {
         this.offlineGraceHours = 168;
      }
      if (!"beta".equalsIgnoreCase(this.updateChannel)) {
         this.updateChannel = "stable";
      }
      while (this.apiBaseUrl.endsWith("/")) {
         this.apiBaseUrl = this.apiBaseUrl.substring(0, this.apiBaseUrl.length() - 1);
      }
   }

   private static String trim(String value, String fallback) {
      String cleaned = value == null ? "" : value.trim();
      return cleaned.isEmpty() ? fallback : cleaned;
   }
}
