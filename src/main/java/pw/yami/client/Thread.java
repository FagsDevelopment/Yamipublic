package pw.yami.client;

public class Thread extends java.lang.Thread {
   public Thread() {
      this.setName("Yami-ShutdownHook");
   }

   @Override
   public void run() {
      Bootstrap.NZ("Saving configurations and shutting down!");
      Bootstrap.clientPaths.aoe();
      Bootstrap.clientPaths.aog();
      if (Bootstrap.g) {
         Bootstrap.clientPaths.anX();
      }
   }
}
