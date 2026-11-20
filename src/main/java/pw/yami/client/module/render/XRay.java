package pw.yami.client.module.render;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BlockListSetting;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;

public class XRay extends ClientModule {
   Setting numberSetting1 = this.registerSetting(new NumberSetting("Opacity", "The opacity of the blocks in wallhack", 0, 120, 255));
   Setting booleanSetting1_1 = this.registerSetting(new BooleanSetting("SoftReload", "Reloads world renders without causing game interruption", true));
   Setting blockListSetting1;
   public static boolean az;

   public XRay() {
      super("XRay", "Allows you to see through solid blocks", ModuleCategory.RENDER);
      Block[] blocks2 = new Block[]{
         Blocks.EMERALD_ORE,
         Blocks.DIAMOND_ORE,
         Blocks.IRON_ORE,
         Blocks.GOLD_ORE,
         Blocks.COAL_ORE,
         Blocks.LAPIS_ORE,
         Blocks.REDSTONE_ORE,
         Blocks.COPPER_ORE,
         Blocks.DEEPSLATE_EMERALD_ORE,
         Blocks.DEEPSLATE_DIAMOND_ORE,
         Blocks.DEEPSLATE_IRON_ORE,
         Blocks.DEEPSLATE_GOLD_ORE,
         Blocks.DEEPSLATE_COAL_ORE,
         Blocks.DEEPSLATE_LAPIS_ORE,
         Blocks.DEEPSLATE_REDSTONE_ORE,
         Blocks.DEEPSLATE_COPPER_ORE,
         Blocks.TNT,
         Blocks.FURNACE,
         Blocks.NETHERITE_BLOCK,
         Blocks.EMERALD_BLOCK,
         Blocks.DIAMOND_BLOCK,
         Blocks.IRON_BLOCK,
         Blocks.GOLD_BLOCK,
         Blocks.COPPER_BLOCK,
         Blocks.BEACON,
         Blocks.SPAWNER,
         Blocks.ANCIENT_DEBRIS,
         Blocks.NETHER_GOLD_ORE
      };
      this.blockListSetting1 = this.registerSetting(new BlockListSetting("Blocks", "Valid block whitelist for wallhack", blocks2));
   }
}
