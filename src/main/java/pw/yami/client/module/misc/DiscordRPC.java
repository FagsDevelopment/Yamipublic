package pw.yami.client.module.misc;

import net.minecraft.client.network.ServerInfo;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.setting.StringSetting;
import pw.yami.client.utils.discord.DiscordIPC;
import pw.yami.eventbus.annotation.EventListener;

public class DiscordRPC extends ClientModule {
   public static final String APP_ID = "1567945966689190110";
   public Setting details = this.registerSetting(new StringSetting("Details", "Activity details text", "Yami Client 1.1"));
   public Setting state = this.registerSetting(new StringSetting("State", "Custom state text", "Winning"));
   public Setting showServer = this.registerSetting(new BooleanSetting("Show Server", "Show current server IP", true));
   public Setting showHealth = this.registerSetting(new BooleanSetting("Show Health", "Show player health in presence", true));

   private Thread thread;
   private volatile boolean running = false;
   private long lastUpdate = 0L;

   public DiscordRPC() {
      super("DiscordRPC", "Shows your playing status on Discord", ModuleCategory.MISC);
   }

   @Override
   public void awd() {
      DiscordIPC.resetStartTime();
      this.running = true;
      this.thread = new Thread(this::runWorker, "Yami-DiscordRPC");
      this.thread.setDaemon(true);
      this.thread.start();
   }

   @Override
   public void awe() {
      this.running = false;
      if (this.thread != null) {
         this.thread.interrupt();
         this.thread = null;
      }
      DiscordIPC.clearPresence();
   }

   private void runWorker() {
      while (this.running) {
         try {
            updatePresenceNow();
            Thread.sleep(4000L);
         } catch (InterruptedException ignored) {
            break;
         } catch (Exception ignored) {
         }
      }
      DiscordIPC.close();
   }

   private void updatePresenceNow() {
      String id = APP_ID;

      String det = (String)this.details.atx();
      String st = (String)this.state.atx();

      if ((Boolean)this.showServer.atx()) {
         if (client.isInSingleplayer()) {
            st = "Singleplayer";
         } else {
            ServerInfo info = client.getCurrentServerEntry();
            if (info != null) {
               st = "Server: " + info.address;
            } else if (client.world == null) {
               st = "Main Menu";
            }
         }
      }

      if ((Boolean)this.showHealth.atx() && client.player != null) {
         int hp = Math.round(client.player.getHealth());
         det = det + " | HP: " + hp + "/20";
      }

      DiscordIPC.updatePresence(id, det, st, "Yami Client 1.1");
   }
}
