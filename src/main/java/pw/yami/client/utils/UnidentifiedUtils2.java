package pw.yami.client.utils;

import net.minecraft.client.gui.screen.Screen;
import pw.yami.client.Mc;

public class UnidentifiedUtils2 {
   public static void mM(Screen screen) {
      try {
         Class accountScreen = Class.forName("ru.vidtu.ias.screen.AccountScreen");
         Mc.client.setScreen((Screen)accountScreen.getConstructor(Screen.class).newInstance(screen));
      } catch (ReflectiveOperationException exception) {
         exception.printStackTrace();
      }
   }
}
