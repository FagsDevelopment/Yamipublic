package pw.yami.client.module.combat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javazoom.jl.player.Player;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.Setting;

public class KurdishTrap extends ClientModule {
   public Setting loop = this.registerSetting(new BooleanSetting("Loop", "Loop audio playback while enabled", true));

   private static final String MP3_PATH = "C:\\Users\\imo34\\Desktop\\openyami\\OpenHachimi-main\\src\\main\\resources\\assets\\yamiclient\\sounds\\kurdish.mp3";
   private Thread audioThread;
   private volatile Player activePlayer;
   private volatile boolean running = false;

   public KurdishTrap() {
      super("KurdishTrap", "Plays kurdish.mp3 while trapping", ModuleCategory.COMBAT);
   }

   @Override
   public String Kn() {
      return "KurdishTrap";
   }

   @Override
   public void awd() {
      super.awd();
      this.startAudio();
   }

   @Override
   public void awe() {
      super.awe();
      this.stopAudio();
   }

   private synchronized void startAudio() {
      this.stopAudio();
      this.running = true;
      this.audioThread = new Thread(() -> {
         while (this.running) {
            InputStream is = null;
            try {
               File file = new File(MP3_PATH);
               if (file.exists()) {
                  is = new BufferedInputStream(new FileInputStream(file));
               } else {
                  is = KurdishTrap.class.getResourceAsStream("/assets/yamiclient/sounds/kurdish.mp3");
               }
               if (is == null) {
                  break;
               }
               this.activePlayer = new Player(is);
               this.activePlayer.play();
            } catch (Exception ignored) {
            } finally {
               if (is != null) {
                  try {
                     is.close();
                  } catch (Exception ignored) {
                  }
               }
            }
            if (!(Boolean)this.loop.atx()) {
               break;
            }
         }
      }, "KurdishTrap-AudioThread");
      this.audioThread.setDaemon(true);
      this.audioThread.start();
   }

   private synchronized void stopAudio() {
      this.running = false;
      if (this.activePlayer != null) {
         try {
            this.activePlayer.close();
         } catch (Exception ignored) {
         }
         this.activePlayer = null;
      }
      if (this.audioThread != null) {
         this.audioThread.interrupt();
         this.audioThread = null;
      }
   }
}
