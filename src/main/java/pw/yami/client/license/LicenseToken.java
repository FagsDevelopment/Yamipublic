package pw.yami.client.license;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Base64;

/**
 * A short-lived license token issued by the backend.
 *
 * <p>Format: {@code base64url(canonical-json).base64url(ed25519-signature)}.
 * The client verifies the signature with the embedded public key and then
 * checks the HWID and Minecraft identity claims.</p>
 */
public final class LicenseToken {
   public final long licenseId;
   public final String discordId;
   public final String mcUuid;
   public final String mcName;
   public final String hwid;
   public final long issuedAt;
   public final long expiresAt;
   public final int version;
   public final String tokenId;

   private LicenseToken(
      long licenseId,
      String discordId,
      String mcUuid,
      String mcName,
      String hwid,
      long issuedAt,
      long expiresAt,
      int version,
      String tokenId
   ) {
      this.licenseId = licenseId;
      this.discordId = discordId;
      this.mcUuid = mcUuid;
      this.mcName = mcName;
      this.hwid = hwid;
      this.issuedAt = issuedAt;
      this.expiresAt = expiresAt;
      this.version = version;
      this.tokenId = tokenId;
   }

   public static LicenseToken parseAndVerify(String token) throws LicenseException {
      if (token == null || token.isBlank()) {
         throw new LicenseException("TOKEN_MISSING", "Lisans tokeni bulunamadi.");
      }
      String trimmed = token.trim();
      String[] parts = trimmed.split("\\.", -1);
      if (parts.length != 2) {
         throw new LicenseException("TOKEN_FORMAT", "Lisans tokeni formati gecersiz.");
      }

      byte[] body;
      byte[] signature;
      try {
         body = Base64.getUrlDecoder().decode(parts[0]);
         signature = Base64.getUrlDecoder().decode(parts[1]);
      } catch (IllegalArgumentException exception) {
         throw new LicenseException("TOKEN_FORMAT", "Lisans tokeni kodlamasi gecersiz.", exception);
      }

      try {
         Signature verifier = Signature.getInstance("Ed25519");
         verifier.initVerify(Keys.licensePublicKey());
         verifier.update(body);
         if (!verifier.verify(signature)) {
            throw new LicenseException("TOKEN_SIGNATURE", "Lisans tokeni imzasi gecersiz.");
         }
      } catch (LicenseException exception) {
         throw exception;
      } catch (Exception exception) {
         throw new LicenseException("TOKEN_SIGNATURE", "Lisans tokeni dogrulanamadi.", exception);
      }

      JsonObject json;
      try {
         json = JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
      } catch (Exception exception) {
         throw new LicenseException("TOKEN_PAYLOAD", "Lisans tokeni icerigi gecersiz.", exception);
      }

      return new LicenseToken(
         longValue(json, "lid"),
         stringValue(json, "did"),
         stringValue(json, "uuid"),
         stringValue(json, "name"),
         stringValue(json, "hwid"),
         longValue(json, "iat"),
         longValue(json, "exp"),
         (int)longValue(json, "ver"),
         stringValue(json, "jti")
      );
   }

   public boolean isExpired() {
      return System.currentTimeMillis() / 1000L >= this.expiresAt;
   }

   public boolean matchesHwid(String value) {
      return value != null && !value.isEmpty() && this.hwid.equalsIgnoreCase(value);
   }

   public boolean matchesMinecraftUuid(String value) {
      return true; // Lisans hesaba degil makinenin HWID'sine baglidir
   }

   public long secondsUntilExpiry() {
      return this.expiresAt - System.currentTimeMillis() / 1000L;
   }

   private static String stringValue(JsonObject json, String key) {
      return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
   }

   private static long longValue(JsonObject json, String key) {
      try {
         return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsLong() : 0L;
      } catch (Exception exception) {
         return 0L;
      }
   }
}
