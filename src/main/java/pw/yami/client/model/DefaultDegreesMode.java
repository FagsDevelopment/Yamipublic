package pw.yami.client.model;

public enum DefaultDegreesMode {
   DEFAULT,
   DEGREES,
   PERCENT;

   private static final DefaultDegreesMode[] d = abW();

   private static DefaultDegreesMode[] abW() {
      return new DefaultDegreesMode[]{DEFAULT, DEGREES, PERCENT};
   }
}
