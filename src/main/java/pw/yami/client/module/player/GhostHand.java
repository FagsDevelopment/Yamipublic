package pw.yami.client.module.player;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import pw.yami.client.model.player.GhostHandIgnoreTargetMode;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BlockListSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.Setting;

public class GhostHand extends ClientModule {
   private static GhostHand INSTANCE;
   Setting mode = this.registerSetting(new EnumSetting("Mode", "mode", GhostHandIgnoreTargetMode.IGNORE, GhostHandIgnoreTargetMode.values()));
   Setting targetList = this.registerSetting(new BlockListSetting("TargetList", "Valid block whitelist", this::isTargetListVisible, Blocks.CHEST));
   Setting ignoreList = this.registerSetting(new BlockListSetting("IgnoreList", "Valid block blacklist", this::isIgnoreListVisible, Blocks.BEDROCK));
   public static boolean bl;

   public GhostHand() {
      super("GhostHand", "Wall hack", ModuleCategory.PLAYER);
      INSTANCE = this;
   }

   public static GhostHand acR() {
      return INSTANCE;
   }

   public boolean acS(Block block2) {
      switch ((GhostHandIgnoreTargetMode)this.mode.atx()) {
         case IGNORE:
            if (((List)this.ignoreList.atx()).contains(block2)) {
               return true;
            }
            break;
         case TARGET:
            if (!((List)this.targetList.atx()).contains(block2)) {
               return true;
            }
      }

      return false;
   }

   private Boolean isIgnoreListVisible() {
      boolean flag;
      if (this.mode.atx() == GhostHandIgnoreTargetMode.IGNORE) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isTargetListVisible() {
      boolean flag;
      if (this.mode.atx() == GhostHandIgnoreTargetMode.TARGET) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }
}
