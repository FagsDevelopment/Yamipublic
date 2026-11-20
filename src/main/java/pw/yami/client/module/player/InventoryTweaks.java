package pw.yami.client.module.player;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import pw.yami.client.event.EventPhase;
import pw.yami.client.event.MouseDraggedEvent;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.manager.Managers;
import pw.yami.client.module.Module5;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.UnidentifiedHelper22;
import pw.yami.eventbus.annotation.EventListener;

public class InventoryTweaks extends Module5 {
   private static InventoryTweaks INSTANCE;
   private final UnidentifiedHelper22 unidentifiedHelper22;
   Setting booleanSetting1_1;
   Setting numberSetting1;
   Setting booleanSetting2_1;
   public static boolean bn;

   public InventoryTweaks() {
      super("InventoryTweaks", "Makes your inventory better", ModuleCategory.PLAYER);
      this.unidentifiedHelper22 = new UnidentifiedHelper22();
      this.booleanSetting1_1 = this.registerSetting(new BooleanSetting("FastDrop", "Drops items from the hotbar faster", false));
      this.numberSetting1 = this.registerSetting(new NumberSetting("FastDropDelay", "The delay for dropping items", 0, 0, 20, this::isNumberSetting1Visible));
      this.booleanSetting2_1 = this.registerSetting(new BooleanSetting("DragItemMove", "Allows you to hold down shift and drag move items.", true));
      INSTANCE = this;
   }

   public static InventoryTweaks pr() {
      return INSTANCE;
   }

   @EventListener
   public void onTickPre(TickPreEvent tickPreEvent) {
      if (tickPreEvent.getEventPhase() == EventPhase.PRE) {
         if ((Boolean)this.booleanSetting1_1.atx() && client.options.dropKey.isPressed() && this.unidentifiedHelper22.afq((Number)this.numberSetting1.atx())) {
            Managers.serverManager.iO('\uf560', new PlayerActionC2SPacket(Action.DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            this.unidentifiedHelper22.afr(29432, 5265492, (byte)40);
         }
      }
   }

   @EventListener
   public void onMouseDragged(MouseDraggedEvent mouseDraggedEvent) {
      if ((Boolean)this.booleanSetting2_1.atx()) {
         mouseDraggedEvent.aim();
      }
   }

   private Boolean isNumberSetting1Visible() {
      return (Boolean)this.booleanSetting1_1.atx();
   }
}
