package pw.yami.client.model.render;

public enum FastHideMode {
   FAST,
   HIDE,
   REMOVE,
   OFF;

   private static final FastHideMode[] e = awo();

   private static FastHideMode[] awo() {
      return new FastHideMode[]{FAST, HIDE, REMOVE, OFF};
   }
}
