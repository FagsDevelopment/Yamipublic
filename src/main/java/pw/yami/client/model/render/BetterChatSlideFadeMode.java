package pw.yami.client.model.render;

public enum BetterChatSlideFadeMode {
   SLIDE,
   FADE,
   OFF;

   private static final BetterChatSlideFadeMode[] d = mr();

   private static BetterChatSlideFadeMode[] mr() {
      return new BetterChatSlideFadeMode[]{SLIDE, FADE, OFF};
   }
}
