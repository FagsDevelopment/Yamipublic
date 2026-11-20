package pw.yami.client.license;

public final class LicenseException extends Exception {
   private final String code;

   public LicenseException(String code, String message) {
      super(message);
      this.code = code;
   }

   public LicenseException(String code, String message, Throwable cause) {
      super(message, cause);
      this.code = code;
   }

   public String getCode() {
      return this.code;
   }
}
