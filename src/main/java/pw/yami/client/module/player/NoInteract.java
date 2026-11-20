package pw.yami.client.module.player;

import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ProjectileItem;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import pw.yami.client.event.SetScreenEvent;
import pw.yami.client.event.network.ChannelRead0Event;
import pw.yami.client.event.network.SendImmediatelyEvent;
import pw.yami.client.event.world.InteractBlockEvent;
import pw.yami.client.event.world.RedirectInteractBlockInternal_shouldCancelInteractionEvent;
import pw.yami.client.manager.Managers;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.module.combat.Aura;
import pw.yami.client.module.combat.AutoCrystal;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.world.PlacementUtils;
import pw.yami.eventbus.annotation.EventListener;

public class NoInteract extends ClientModule {
   private static NoInteract INSTANCE;

   public final Setting targetsSetting = this.registerSetting(new EnumSetting("Targets", "What interactive targets to block", TargetsChoice.Blocks, TargetsChoice.values()));
   public final Setting modeSetting = this.registerSetting(new EnumSetting("Mode", "Interaction prevention mode (Cancel or Shift to place through)", ModeChoice.Shift, ModeChoice.values()));
   public final Setting onlyFoodSetting = this.registerSetting(new BooleanSetting("OnlyFood", "Only block interactions when holding or eating food", false));
   public final Setting onlyInCombat = this.registerSetting(new BooleanSetting("OnlyInCombat", "Only block when in combat or when PvP modules are placing", true));

   public boolean shouldStopSneaking;

   public NoInteract() {
      super("NoInteract", "Prevents you from interacting with tile entities", ModuleCategory.PLAYER);
      INSTANCE = this;
   }

   public static NoInteract Xz() {
      return INSTANCE;
   }

   public static Hand selectUsableHand() {
      if (client.player == null) {
         return Hand.MAIN_HAND;
      }
      ItemStack mainHandStack = client.player.getMainHandStack();
      ItemStack offhandStack = client.player.getOffHandStack();

      if (!isFoodOrProjectile(mainHandStack) && isFoodOrProjectile(offhandStack)) {
         return Hand.OFF_HAND;
      }

      return Hand.MAIN_HAND;
   }

   public static boolean isFoodOrProjectile(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      if (stack.contains(DataComponentTypes.FOOD)) {
         return true;
      }
      Item item = stack.getItem();
      return item instanceof EnderPearlItem
         || item instanceof ProjectileItem
         || item instanceof RangedWeaponItem;
   }

   private boolean isSpawnPointTarget(BlockState state) {
      if (state == null || client.world == null) {
         return false;
      }
      if (client.world.getRegistryKey().equals(World.OVERWORLD)) {
         return state.getBlock() instanceof BedBlock;
      }
      if (client.world.getRegistryKey().equals(World.NETHER)) {
         return state.getBlock() instanceof RespawnAnchorBlock;
      }
      return false;
   }

   private boolean isBlockInteractionTarget(BlockState state, BlockPos position) {
      if (state == null) {
         return false;
      }
      if (state.getBlock() instanceof AnvilBlock) {
         return true;
      }
      if (state.getBlock() instanceof BedBlock) {
         return false;
      }
      if (state.hasBlockEntity()) {
         return true;
      }
      if (client.world != null && position != null && client.world.getBlockEntity(position) != null) {
         return true;
      }
      return false;
   }

   public boolean isCombatActive() {
      if (client.player == null || client.world == null) {
         return false;
      }

      if (PlacementUtils.isPlacing) {
         return true;
      }

      try {
         Aura aura = Aura.aoS();
         if (aura != null && aura.awg() && aura.hasTarget()) {
            return true;
         }

         AutoCrystal ac = AutoCrystal.sc();
         if (ac != null && ac.awg() && ac.sd() != null) {
            return true;
         }

         // Check if a hostile player is nearby within combat distance (8 blocks)
         for (PlayerEntity player : client.world.getPlayers()) {
            if (player != client.player && player.isAlive() && !Managers.friendManager.awr(player)) {
               if (client.player.squaredDistanceTo(player) <= 64.0) {
                  return true;
               }
            }
         }
      } catch (Exception ignored) {
      }

      return false;
   }

   public boolean shouldBlockInteraction(ItemStack stack, BlockPos position) {
      if (!this.awg() || client.world == null || client.player == null || position == null) {
         return false;
      }

      // If a placement routine (Surround, SelfTrap, AutoCrystal) is actively placing right now, always block container GUI
      if (PlacementUtils.isPlacing) {
         return true;
      }

      // If OnlyInCombat is enabled, don't block unless we are in combat or placing
      if ((Boolean)this.onlyInCombat.atx() && !this.isCombatActive()) {
         return false;
      }

      if ((Boolean)this.onlyFoodSetting.atx()) {
         if (stack == null || stack.isEmpty() || !stack.contains(DataComponentTypes.FOOD)) {
            return false;
         }
      }

      BlockState state = client.world.getBlockState(position);
      TargetsChoice targets = (TargetsChoice)this.targetsSetting.atx();
      if (targets == TargetsChoice.SpawnPoint) {
         return this.isSpawnPointTarget(state);
      }
      if (targets == TargetsChoice.Blocks) {
         return this.isBlockInteractionTarget(state, position);
      }
      return this.isBlockInteractionTarget(state, position) || this.isSpawnPointTarget(state);
   }

   @EventListener
   public void onRedirectInteractBlockInternal(RedirectInteractBlockInternal_shouldCancelInteractionEvent event) {
      if (!this.awg() || client.world == null || client.player == null) {
         return;
      }
      if (PlacementUtils.isPlacing) {
         event.aim();
         return;
      }
      if (client.crosshairTarget instanceof BlockHitResult) {
         BlockPos pos = ((BlockHitResult)client.crosshairTarget).getBlockPos();
         Hand hand = selectUsableHand();
         ItemStack stack = client.player.getStackInHand(hand);
         if (this.shouldBlockInteraction(stack, pos)) {
            ModeChoice mode = (ModeChoice)this.modeSetting.atx();
            if (mode == ModeChoice.Shift) {
               event.aim();
            }
         }
      }
   }

   @EventListener
   public void onInteractBlock(InteractBlockEvent interactBlockEvent) {
      if (!this.awg() || client.world == null || client.player == null) {
         return;
      }
      if (PlacementUtils.isPlacing) {
         return;
      }
      BlockPos pos = interactBlockEvent.getBlockHitResult().getBlockPos();
      Hand hand = interactBlockEvent.getHand();
      ItemStack stack = client.player.getStackInHand(hand);
      if (this.shouldBlockInteraction(stack, pos)) {
         ModeChoice mode = (ModeChoice)this.modeSetting.atx();
         if (mode == ModeChoice.Cancel) {
            interactBlockEvent.aim();
         }
      }
   }

   @EventListener
   public void onSendImmediately(SendImmediatelyEvent sendImmediatelyEvent) {
      if (!this.awg() || client.player == null || client.world == null) {
         return;
      }
      if (PlacementUtils.isPlacing) {
         return;
      }
      Packet packet = sendImmediatelyEvent.getPacket();
      if (packet instanceof PlayerInteractBlockC2SPacket) {
         PlayerInteractBlockC2SPacket p = (PlayerInteractBlockC2SPacket)packet;
         BlockPos pos = p.getBlockHitResult().getBlockPos();
         Hand hand = p.getHand();
         ItemStack stack = client.player.getStackInHand(hand);
         if (this.shouldBlockInteraction(stack, pos)) {
            ModeChoice mode = (ModeChoice)this.modeSetting.atx();
            if (mode == ModeChoice.Cancel) {
               sendImmediatelyEvent.aim();
            }
         }
      }
   }

   @EventListener
   public void onChannelRead0(ChannelRead0Event event) {
      if (client.player == null) return;
      Packet packet = event.getPacket();
      if (packet instanceof OpenScreenS2CPacket) {
         if (PlacementUtils.isPlacing) {
            event.aim();
            int syncId = ((OpenScreenS2CPacket)packet).getSyncId();
            client.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
         }
      }
   }

   @EventListener
   public void onSetScreen(SetScreenEvent event) {
      if (PlacementUtils.isPlacing || (this.awg() && this.isCombatActive())) {
         if (event.getScreen() instanceof HandledScreen && !(event.getScreen() instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) {
            client.setScreen(null);
         }
      }
   }

   public enum ModeChoice {
      Cancel("Cancel"),
      Shift("Shift");

      private final String displayName;

      ModeChoice(String displayName) {
         this.displayName = displayName;
      }

      @Override
      public String toString() {
         return this.displayName;
      }
   }

   public enum TargetsChoice {
      Blocks("Blocks"),
      SpawnPoint("SpawnPoint"),
      Both("Both");

      private final String displayName;

      TargetsChoice(String displayName) {
         this.displayName = displayName;
      }

      @Override
      public String toString() {
         return this.displayName;
      }
   }
}
