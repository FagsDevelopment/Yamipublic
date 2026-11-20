package pw.yami.client.license;

public enum LicenseState {
   CHECKING,
   VALID,
   PENDING_APPROVAL,
   HOST_BUSY,
   NOT_ACTIVATED,
   INVALID,
   EXPIRED,
   OFFLINE,
   ERROR;

   public boolean isLicensed() {
      return this == VALID || this == OFFLINE;
   }
}
