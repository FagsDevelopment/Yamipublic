package pw.yami.client.license;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.HexFormat;

public final class CryptoUtil {
   private CryptoUtil() {
   }

   public static String sha256(Path file) throws Exception {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream input = Files.newInputStream(file)) {
         byte[] buffer = new byte[8192];
         int read;
         while ((read = input.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
         }
      }
      return HexFormat.of().formatHex(digest.digest());
   }

   public static String sha256(String value) throws Exception {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
   }

   public static boolean verifyEd25519(Path file, String signatureBase64, PublicKey key) throws Exception {
      byte[] signature = decodeSignature(signatureBase64);
      Signature verifier = Signature.getInstance("Ed25519");
      verifier.initVerify(key);
      try (InputStream input = Files.newInputStream(file)) {
         byte[] buffer = new byte[8192];
         int read;
         while ((read = input.read(buffer)) != -1) {
            verifier.update(buffer, 0, read);
         }
      }
      return verifier.verify(signature);
   }

   public static boolean verifyEd25519(byte[] data, String signatureBase64, PublicKey key) throws Exception {
      byte[] signature = decodeSignature(signatureBase64);
      Signature verifier = Signature.getInstance("Ed25519");
      verifier.initVerify(key);
      verifier.update(data);
      return verifier.verify(signature);
   }

   private static byte[] decodeSignature(String signatureBase64) {
      String cleaned = signatureBase64 == null ? "" : signatureBase64.replaceAll("\\s+", "");
      try {
         return Base64.getDecoder().decode(cleaned);
      } catch (IllegalArgumentException exception) {
         return Base64.getUrlDecoder().decode(cleaned);
      }
   }
}
