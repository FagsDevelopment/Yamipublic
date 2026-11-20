package pw.yami.client.model.network;

public enum GodModePacketPortalMode {
   PACKET,
   PORTAL;

   private static final GodModePacketPortalMode[] c = Ou();

   private static GodModePacketPortalMode[] Ou() {
      return new GodModePacketPortalMode[]{PACKET, PORTAL};
   }
}
