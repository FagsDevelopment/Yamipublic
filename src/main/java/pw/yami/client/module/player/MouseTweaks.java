package pw.yami.client.module.player;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;

/**
 * Mouse Tweaks style inventory helpers, implemented inside the client instead
 * of requiring the external Mouse Tweaks mod.
 */
public class MouseTweaks extends ClientModule {
   private static MouseTweaks INSTANCE;

   private final Setting wheel = this.registerSetting(
      new BooleanSetting("Wheel", "Moves items with the mouse wheel while a container is open", true)
   );
   private final Setting scrollDelay = this.registerSetting(
      new NumberSetting("ScrollDelay", "Delay between wheel transfers in milliseconds", 0, 0, 500, this::isWheelVisible)
   );
   private final Setting invert = this.registerSetting(
      new BooleanSetting("Invert", "Inverts the wheel transfer direction", false, this::isWheelVisible)
   );
   private final Setting reverseSearch = this.registerSetting(
      new BooleanSetting("ReverseSearch", "Searches the target inventory from last slot to first", true, this::isWheelVisible)
   );
   private final Setting shiftStack = this.registerSetting(
      new BooleanSetting("ShiftStack", "Shift + scroll quick-moves the whole hovered stack", true, this::isWheelVisible)
   );
   private final Setting dragQuickMove = this.registerSetting(
      new BooleanSetting("DragQuickMove", "Shift-drag to quick-move items between inventories", true)
   );

   private long lastScrollTime;

   public MouseTweaks() {
      super("MouseTweaks", "Adds Mouse Tweaks style inventory scrolling and dragging", ModuleCategory.PLAYER);
      INSTANCE = this;
   }

   public static MouseTweaks UY() {
      return INSTANCE;
   }

   public boolean isDragQuickMoveEnabled() {
      return this.awg() && this.bool(this.dragQuickMove);
   }

   /**
    * Called from HandledScreen#mouseScrolled. Returning true consumes the scroll.
    */
   public boolean onScroll(ScreenHandler handler, Slot hovered, double verticalAmount) {
      if (!this.awg() || !this.bool(this.wheel) || client.player == null || client.interactionManager == null) {
         return false;
      }
      if (handler == null || handler != client.player.currentScreenHandler || hovered == null || verticalAmount == 0.0) {
         return false;
      }

      // Never interfere with a click that is already in progress.
      if (!handler.getCursorStack().isEmpty()) {
         return true;
      }

      ItemStack hoveredStack = hovered.getStack();
      if (hoveredStack.isEmpty()) {
         // Consume the scroll anyway so the inventory does not start doing
         // something else as soon as the hovered stack runs out.
         return true;
      }

      long now = System.currentTimeMillis();
      long delay = ((Number)this.scrollDelay.atx()).longValue();
      if (delay > 0L && now - this.lastScrollTime < delay) {
         return true;
      }
      this.lastScrollTime = now;

      boolean push = verticalAmount < 0.0;
      if (this.bool(this.invert)) {
         push = !push;
      }

      int steps = Math.min(8, Math.max(1, (int)Math.abs(verticalAmount)));
      for (int i = 0; i < steps; i++) {
         if (hovered.getStack().isEmpty()) {
            break;
         }

         if (this.bool(this.shiftStack) && Screen.hasShiftDown()) {
            this.click(hovered, 0, SlotActionType.QUICK_MOVE);
            break;
         }

         boolean moved = push ? this.pushOne(handler, hovered) : this.pullOne(handler, hovered);
         if (!moved) {
            break;
         }
      }

      return true;
   }

   /**
    * Takes one item out of the hovered slot and puts it into the other
    * inventory (container -> player or player -> container).
    */
   private boolean pushOne(ScreenHandler handler, Slot hovered) {
      if (hovered instanceof CraftingResultSlot) {
         return false;
      }

      ItemStack source = hovered.getStack();
      if (source.isEmpty() || !hovered.canInsert(source)) {
         return false;
      }

      Slot target = this.findPushTarget(handler, hovered, source);
      if (target == null) {
         return false;
      }

      this.click(hovered, 0, SlotActionType.PICKUP);
      this.click(target, 1, SlotActionType.PICKUP);
      if (!handler.getCursorStack().isEmpty()) {
         this.click(hovered, 0, SlotActionType.PICKUP);
      }
      return true;
   }

   /**
    * Pulls one matching item from the other inventory into the hovered slot.
    */
   private boolean pullOne(ScreenHandler handler, Slot hovered) {
      ItemStack hoveredStack = hovered.getStack();
      if (hoveredStack.isEmpty() || !hovered.canInsert(hoveredStack)) {
         return false;
      }
      if (hoveredStack.getCount() >= hovered.getMaxItemCount(hoveredStack)) {
         return false;
      }

      Slot source = this.findPullSource(handler, hovered, hoveredStack);
      if (source == null) {
         return false;
      }

      this.click(source, 0, SlotActionType.PICKUP);
      this.click(hovered, 1, SlotActionType.PICKUP);
      if (!handler.getCursorStack().isEmpty()) {
         this.click(source, 0, SlotActionType.PICKUP);
      }
      return true;
   }

   private Slot findPushTarget(ScreenHandler handler, Slot hovered, ItemStack stack) {
      boolean targetIsPlayer = !(hovered.inventory instanceof PlayerInventory);
      Slot emptySlot = null;

      for (int i = 0; i < handler.slots.size(); i++) {
         Slot slot = handler.slots.get(this.index(handler, i));
         if (slot == hovered || this.isPlayerSlot(slot) != targetIsPlayer) {
            continue;
         }
         if (slot instanceof CraftingResultSlot || !slot.canInsert(stack)) {
            continue;
         }

         ItemStack inSlot = slot.getStack();
         if (inSlot.isEmpty()) {
            if (emptySlot == null) {
               emptySlot = slot;
            }
         } else if (ItemStack.areItemsAndComponentsEqual(inSlot, stack)
            && inSlot.getCount() < slot.getMaxItemCount(inSlot)) {
            return slot;
         }
      }

      return emptySlot;
   }

   private Slot findPullSource(ScreenHandler handler, Slot hovered, ItemStack reference) {
      boolean sourceIsPlayer = !(hovered.inventory instanceof PlayerInventory);

      for (int i = 0; i < handler.slots.size(); i++) {
         Slot slot = handler.slots.get(this.index(handler, i));
         if (slot == hovered || this.isPlayerSlot(slot) != sourceIsPlayer) {
            continue;
         }
         if (slot instanceof CraftingResultSlot) {
            continue;
         }

         ItemStack stack = slot.getStack();
         if (!stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, reference)) {
            return slot;
         }
      }

      return null;
   }

   private int index(ScreenHandler handler, int index) {
      return this.bool(this.reverseSearch) ? handler.slots.size() - 1 - index : index;
   }

   private boolean isPlayerSlot(Slot slot) {
      return slot.inventory instanceof PlayerInventory;
   }

   private void click(Slot slot, int button, SlotActionType actionType) {
      if (client.player == null || client.interactionManager == null) {
         return;
      }

      client.interactionManager.clickSlot(
         client.player.currentScreenHandler.syncId,
         slot.id,
         button,
         actionType,
         client.player
      );
   }

   private boolean bool(Setting setting) {
      return (Boolean)setting.atx();
   }

   private Boolean isWheelVisible() {
      return (Boolean)this.wheel.atx();
   }
}
