package pw.yami.satin;

import java.lang.management.ManagementFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class Checks {
   public static void warn() {
      JFrame jFrame = new JFrame();
      jFrame.setAlwaysOnTop(true);
      JOptionPane.showMessageDialog(
         jFrame, "请购买BETA版\nThis is earlyacess build.\nYou do not have permissions to access.", "Warning", 2, UIManager.getIcon("OptionPane.warningIcon")
      );
      jFrame.dispose();
   }

   public static void check() {
      // no-op: disabled noverify warning
   }
}
