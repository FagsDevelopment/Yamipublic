package pw.yami.client.module.misc;

import pw.yami.client.event.EventPhase;
import pw.yami.client.event.OnGameJoinEvent;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.setting.StringSetting;
import pw.yami.client.utils.StopwatchImpl;
import pw.yami.client.utils.chat.ChatUtils;
import pw.yami.eventbus.annotation.EventListener;

public class AutoLogin extends ClientModule {
   private final StopwatchImpl stopwatchImpl = new StopwatchImpl();
   private boolean b = false;
   Setting password = this.registerSetting(new StringSetting("Password", "login password", ""));
   Setting command = this.registerSetting(new StringSetting("Command", "login command", "/l"));
   Setting numberSetting1 = this.registerSetting(new NumberSetting("Delay", "login delay", 0.0F, 0.5F, 5.0F));
   Setting booleanSetting1_1 = this.registerSetting(new BooleanSetting("Notify", "login success send message", true));
   public static boolean bh;

   public AutoLogin() {
      super("AutoLogin", "", ModuleCategory.MISC);
   }

   @EventListener
   public void onOnGameJoin(OnGameJoinEvent onGameJoinEvent) {
      if (this.awg()) {
         String str = (String)this.password.atx();
         if (str != null && !str.isEmpty()) {
            this.b = true;
            this.stopwatchImpl.afr(29432, 5265492, (byte)40);
         } else {
            if ((Boolean)this.booleanSetting1_1.atx()) {
               ChatUtils.UB("No password provided");
            }
         }
      }
   }

   @EventListener
   public void onTickPre(TickPreEvent tickPreEvent) {
      if (tickPreEvent.getEventPhase() == EventPhase.PRE) {
         if (this.b) {
            float f = (Float)this.numberSetting1.atx();
            if (this.stopwatchImpl.afq((long)(f * 1000.0F))) {
               String str = (String)this.command.atx();
               String str2 = (String)this.password.atx();
               if (str == null || str.isEmpty()) {
                  str = "/l";
               }

               ChatUtils.UH(str + " " + str2);
               if ((Boolean)this.booleanSetting1_1.atx()) {
                  ChatUtils.UB("Login command sent");
               }

               this.b = false;
            }
         }
      }
   }
}
