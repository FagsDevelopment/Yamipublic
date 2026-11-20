package pw.yami.client.license;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Public keys embedded in the client.
 *
 * <p>The matching private keys live only in {@code license-system/keys} and must
 * never be shipped inside the jar. The license key signs short-lived tokens on
 * the backend, the update key signs release jars in CI.</p>
 */
public final class Keys {
   public static final String LICENSE_PUBLIC_KEY = "MCowBQYDK2VwAyEAMRUcr3YTW2bd5GFBw8fNNLK5cGA60GKTTyKSF58Buo4=";
   public static final String UPDATE_PUBLIC_KEY = "MCowBQYDK2VwAyEAtH+AjhmQpOUS5pMSq3aVCgkvRU10HLTAqyvtfMPr0vI=";

   private static volatile PublicKey licenseKey;
   private static volatile PublicKey updateKey;

   private Keys() {
   }

   public static PublicKey licensePublicKey() {
      PublicKey key = licenseKey;
      if (key == null) {
         synchronized (Keys.class) {
            key = licenseKey;
            if (key == null) {
               key = decode(LICENSE_PUBLIC_KEY);
               licenseKey = key;
            }
         }
      }
      return key;
   }

   public static PublicKey updatePublicKey() {
      PublicKey key = updateKey;
      if (key == null) {
         synchronized (Keys.class) {
            key = updateKey;
            if (key == null) {
               key = decode(UPDATE_PUBLIC_KEY);
               updateKey = key;
            }
         }
      }
      return key;
   }

   private static PublicKey decode(String base64) {
      try {
         byte[] der = Base64.getDecoder().decode(base64);
         return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
      } catch (Exception exception) {
         throw new IllegalStateException("Invalid embedded Ed25519 public key", exception);
      }
   }
}
