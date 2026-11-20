package pw.yami.client.event;

public enum EventPhase {
   PRE,
   POST;

   private static final EventPhase[] c = eO();

   private static EventPhase[] eO() {
      return new EventPhase[]{PRE, POST};
   }
}
