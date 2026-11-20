package pw.yami.client.license;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class LicenseClient {
   private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(8))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

   private final String baseUrl;

   public LicenseClient(String baseUrl) {
      String cleaned = baseUrl == null ? "" : baseUrl.trim();
      while (cleaned.endsWith("/")) {
         cleaned = cleaned.substring(0, cleaned.length() - 1);
      }
      this.baseUrl = cleaned.isEmpty() ? LicenseConfig.HARDCODED_API_URL : cleaned;
   }

   public String getBaseUrl() {
      return this.baseUrl;
   }

   public CheckResult checkHwid(String hwid, String mcUuid, String mcName) throws LicenseException {
      JsonObject body = new JsonObject();
      body.addProperty("hwid", hwid);
      body.addProperty("mc_uuid", mcUuid == null ? "" : mcUuid);
      body.addProperty("mc_name", mcName == null ? "" : mcName);
      JsonObject response = this.send("POST", "/license/check-hwid", body, null);
      return CheckResult.from(response);
   }

   public void sendSecurityAlert(String reason, String detail) {
      try {
         JsonObject body = new JsonObject();
         body.addProperty("hwid", HwidUtil.getHwid());
         String mcName = "";
         String mcUuid = "";
         try {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null && mc.getSession() != null) {
               mcName = mc.getSession().getUsername();
               if (mc.getSession().getUuidOrNull() != null) {
                  mcUuid = mc.getSession().getUuidOrNull().toString();
               }
            }
         } catch (Throwable ignored) {
         }
         body.addProperty("mc_name", mcName);
         body.addProperty("mc_uuid", mcUuid);
         body.addProperty("reason", reason);
         body.addProperty("detail", detail);
         this.send("POST", "/license/security-alert", body, null);
      } catch (Throwable ignored) {
      }
   }

   public TokenResult activate(String discordId, String code, String hwid, String mcUuid, String mcName) throws LicenseException {
      JsonObject body = new JsonObject();
      body.addProperty("discord_id", discordId);
      body.addProperty("code", code);
      body.addProperty("hwid", hwid);
      body.addProperty("mc_uuid", mcUuid == null ? "" : mcUuid);
      body.addProperty("mc_name", mcName == null ? "" : mcName);
      JsonObject response = this.send("POST", "/license/activate", body, null);
      return TokenResult.from(response);
   }

   public TokenResult refresh(String token, String hwid, String mcUuid, String mcName) throws LicenseException {
      JsonObject body = new JsonObject();
      body.addProperty("token", token);
      body.addProperty("hwid", hwid);
      body.addProperty("mc_uuid", mcUuid == null ? "" : mcUuid);
      body.addProperty("mc_name", mcName == null ? "" : mcName);
      JsonObject response = this.send("POST", "/license/refresh", body, null);
      return TokenResult.from(response);
   }

   public HostInfo claimHost(String token) throws LicenseException {
      return HostInfo.from(this.send("POST", "/host/claim", new JsonObject(), token));
   }

   public HostInfo heartbeat(String token) throws LicenseException {
      return HostInfo.from(this.send("POST", "/host/heartbeat", new JsonObject(), token));
   }

   public void releaseHost(String token) throws LicenseException {
      this.send("POST", "/host/release", new JsonObject(), token);
   }

   public JsonObject latestUpdate(String channel) throws LicenseException {
      String query = "beta".equalsIgnoreCase(channel) ? "?channel=beta" : "?channel=stable";
      return this.send("GET", "/update/latest" + query, null, null);
   }

   private JsonObject send(String method, String path, JsonObject body, String token) throws LicenseException {
      HttpRequest.Builder builder;
      try {
         builder = HttpRequest.newBuilder(URI.create(this.baseUrl + path))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json")
            .header("User-Agent", "YamiClient/" + pw.yami.client.Bootstrap.b);
      } catch (Exception exception) {
         throw new LicenseException("URL_INVALID", "Sunucu adresi gecersiz: " + this.baseUrl, exception);
      }

      if (token != null && !token.isBlank()) {
         builder.header("Authorization", "Bearer " + token);
      }
      if (body != null) {
         builder.header("Content-Type", "application/json");
         builder.method(method, HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
      } else {
         builder.method(method, HttpRequest.BodyPublishers.noBody());
      }

      HttpResponse<String> response;
      try {
         response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      } catch (InterruptedException exception) {
         Thread.currentThread().interrupt();
         throw new LicenseException("NETWORK", "Sunucu baglantisi kesildi.", exception);
      } catch (Exception exception) {
         throw new LicenseException("NETWORK", "Sunucuya ulasilamadi: " + this.baseUrl, exception);
      }

      String responseBody = response.body() == null ? "" : response.body();
      if (response.statusCode() >= 400) {
         throw error(response.statusCode(), responseBody);
      }
      try {
         JsonElement parsed = JsonParser.parseString(responseBody);
         return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
      } catch (Exception exception) {
         throw new LicenseException("BAD_RESPONSE", "Sunucu yaniti okunamadi.", exception);
      }
   }

   private static LicenseException error(int status, String body) {
      String code = "HTTP_" + status;
      String message = "Sunucu hatasi (" + status + ").";
      try {
         JsonObject json = JsonParser.parseString(body).getAsJsonObject();
         JsonElement detail = json.get("detail");
         if (detail != null && detail.isJsonObject()) {
            JsonObject object = detail.getAsJsonObject();
            if (object.has("code")) {
               code = object.get("code").getAsString();
            }
            if (object.has("message")) {
               message = object.get("message").getAsString();
            }
         } else if (detail != null && detail.isJsonPrimitive()) {
            message = detail.getAsString();
         }
      } catch (Exception ignored) {
      }
      return new LicenseException(code, message);
   }

   public static final class CheckResult {
      public final String status;
      public final String token;
      public final long expiresAt;
      public final String discordId;
      public final String mcName;
      public final String message;

      public CheckResult(String status, String token, long expiresAt, String discordId, String mcName, String message) {
         this.status = status;
         this.token = token;
         this.expiresAt = expiresAt;
         this.discordId = discordId;
         this.mcName = mcName;
         this.message = message;
      }

      static CheckResult from(JsonObject json) {
         String status = json.has("status") ? json.get("status").getAsString() : "pending";
         String token = json.has("token") ? json.get("token").getAsString() : "";
         long expiresAt = json.has("expires_at") ? json.get("expires_at").getAsLong() : 0L;
         String discordId = json.has("discord_id") ? json.get("discord_id").getAsString() : "";
         String mcName = json.has("mc_name") ? json.get("mc_name").getAsString() : "";
         String message = json.has("message") ? json.get("message").getAsString() : "";
         return new CheckResult(status, token, expiresAt, discordId, mcName, message);
      }
   }

   public static final class TokenResult {
      public final String token;
      public final long expiresAt;
      public final JsonObject troll;

      private TokenResult(String token, long expiresAt, JsonObject troll) {
         this.token = token;
         this.expiresAt = expiresAt;
         this.troll = troll;
      }

      static TokenResult from(JsonObject json) throws LicenseException {
         if (!json.has("token") || json.get("token").getAsString().isBlank()) {
            throw new LicenseException("BAD_RESPONSE", "Sunucu lisans tokeni dondurmedi.");
         }
         long expiresAt = json.has("expires_at") ? json.get("expires_at").getAsLong() : 0L;
         JsonObject trollObj = json.has("troll") && json.get("troll").isJsonObject() ? json.getAsJsonObject("troll") : null;
         return new TokenResult(json.get("token").getAsString(), expiresAt, trollObj);
      }
   }

   public static final class HostInfo {
      public final boolean busy;
      public final String holderName;
      public final String holderDiscordId;
      public final long expiresAt;

      private HostInfo(boolean busy, String holderName, String holderDiscordId, long expiresAt) {
         this.busy = busy;
         this.holderName = holderName;
         this.holderDiscordId = holderDiscordId;
         this.expiresAt = expiresAt;
      }

      static HostInfo from(JsonObject json) {
         boolean busy = json.has("busy") && json.get("busy").getAsBoolean();
         String name = "";
         String discordId = "";
         long expiresAt = 0L;
         JsonElement holderElement = json.get("holder");
         if (holderElement != null && holderElement.isJsonObject()) {
            JsonObject holder = holderElement.getAsJsonObject();
            if (holder.has("mc_name")) {
               name = holder.get("mc_name").getAsString();
            }
            if (holder.has("discord_id")) {
               discordId = holder.get("discord_id").getAsString();
            }
            if (holder.has("expires_at")) {
               expiresAt = holder.get("expires_at").getAsLong();
            }
         }
         return new HostInfo(busy, name, discordId, expiresAt);
      }
   }
}
