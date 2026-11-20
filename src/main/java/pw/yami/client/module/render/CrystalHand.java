package pw.yami.client.module.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import pw.yami.client.event.render.RenderFirstPersonItemEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class CrystalHand extends ClientModule {
   private static CrystalHand INSTANCE;
   private static ItemStack crystalStack;

   public enum HandChoice {
      Both,
      Mainhand,
      Offhand
   }

   public enum ReplaceChoice {
      TotemAndSword,
      TotemOnly,
      SwordOnly,
      All,
      None
   }

   public enum SpinAxis {
      Y,
      X,
      Z,
      All
   }

   // Which items to visually substitute with an End Crystal in first person
   public final Setting replaceMode = this.registerSetting(new EnumSetting("Replace", "Which held items to display as an End Crystal", ReplaceChoice.TotemAndSword, ReplaceChoice.values()));
   public final Setting hands = this.registerSetting(new EnumSetting("Hands", "Which hands to apply crystal modifications", HandChoice.Both, HandChoice.values()));

   // Spin settings
   public final Setting spin = this.registerSetting(new BooleanSetting("Spin", "Smooth continuous spinning rotation", false));
   public final Setting spinAxis = this.registerSetting(new EnumSetting("SpinAxis", "Axis of spin rotation", SpinAxis.Y, SpinAxis.values()));
   public final Setting spinSpeed = this.registerSetting(new NumberSetting("SpinSpeed", "Speed of spinning animation", 0.0F, 2.0F, 10.0F));

   // Bounce settings
   public final Setting bounce = this.registerSetting(new BooleanSetting("Bounce", "Smooth vertical bobbing float animation", true));
   public final Setting bounceSpeed = this.registerSetting(new NumberSetting("BounceSpeed", "Frequency of bounce animation", 0.1F, 2.0F, 10.0F));
   public final Setting bounceHeight = this.registerSetting(new NumberSetting("BounceHeight", "Amplitude of bounce animation", 0.01F, 0.06F, 0.5F));

   // Scale settings
   public final Setting scale = this.registerSetting(new NumberSetting("Scale", "Overall scale of held crystal", 0.1F, 1.0F, 2.5F));
   public final Setting scaleX = this.registerSetting(new NumberSetting("ScaleX", "X scale multiplier", 0.1F, 1.0F, 2.5F));
   public final Setting scaleY = this.registerSetting(new NumberSetting("ScaleY", "Y scale multiplier", 0.1F, 1.0F, 2.5F));
   public final Setting scaleZ = this.registerSetting(new NumberSetting("ScaleZ", "Z scale multiplier", 0.1F, 1.0F, 2.5F));

   // Translation offsets
   public final Setting x = this.registerSetting(new NumberSetting("X", "X-axis position offset", -3.0F, 0.0F, 3.0F));
   public final Setting y = this.registerSetting(new NumberSetting("Y", "Y-axis position offset", -3.0F, 0.0F, 3.0F));
   public final Setting z = this.registerSetting(new NumberSetting("Z", "Z-axis position offset", -3.0F, 0.0F, 3.0F));

   // Rotation angles
   public final Setting rotateX = this.registerSetting(new NumberSetting("RotateX", "Pitch rotation angle", -180.0F, 0.0F, 180.0F));
   public final Setting rotateY = this.registerSetting(new NumberSetting("RotateY", "Yaw rotation angle", -180.0F, 0.0F, 180.0F));
   public final Setting rotateZ = this.registerSetting(new NumberSetting("RotateZ", "Roll rotation angle", -180.0F, 0.0F, 180.0F));

   public CrystalHand() {
      super("CrystalHand", "Renders an animated End Crystal in hand when holding totems or swords", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static CrystalHand getInstance() {
      return INSTANCE;
   }

   public static ItemStack getCrystalStack() {
      if (crystalStack == null) {
         crystalStack = new ItemStack(Items.END_CRYSTAL);
      }
      return crystalStack;
   }

   public boolean shouldReplaceWithCrystal(ItemStack stack, Hand hand) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      if (stack.isOf(Items.END_CRYSTAL)) {
         return false;
      }

      HandChoice handChoice = (HandChoice) this.hands.atx();
      if (handChoice == HandChoice.Mainhand && hand != Hand.MAIN_HAND) {
         return false;
      }
      if (handChoice == HandChoice.Offhand && hand != Hand.OFF_HAND) {
         return false;
      }

      ReplaceChoice replaceChoice = (ReplaceChoice) this.replaceMode.atx();
      switch (replaceChoice) {
         case TotemAndSword:
            return stack.isOf(Items.TOTEM_OF_UNDYING) || stack.getItem() instanceof SwordItem;
         case TotemOnly:
            return stack.isOf(Items.TOTEM_OF_UNDYING);
         case SwordOnly:
            return stack.getItem() instanceof SwordItem;
         case All:
            return true;
         case None:
            return false;
      }
      return false;
   }

   public boolean isHandTarget(Hand hand) {
      HandChoice handChoice = (HandChoice) this.hands.atx();
      if (handChoice == HandChoice.Mainhand && hand != Hand.MAIN_HAND) {
         return false;
      }
      if (handChoice == HandChoice.Offhand && hand != Hand.OFF_HAND) {
         return false;
      }
      return true;
   }

   @EventListener
   public void onRenderFirstPersonItem(RenderFirstPersonItemEvent event) {
      if (event.stack == null) {
         return;
      }

      boolean isCrystal = event.stack.isOf(Items.END_CRYSTAL);
      if (!isCrystal && !this.shouldReplaceWithCrystal(event.stack, event.hand)) {
         return;
      }

      if (!this.isHandTarget(event.hand)) {
         return;
      }

      MatrixStack matrices = event.matrices;
      boolean isMain = event.hand == Hand.MAIN_HAND;

      // Translation
      float posX = (Float) this.x.atx();
      float posY = (Float) this.y.atx();
      float posZ = (Float) this.z.atx();
      if (!isMain) {
         posX = -posX;
      }

      // Floating bounce
      if ((Boolean) this.bounce.atx()) {
         float bSpeed = (Float) this.bounceSpeed.atx();
         float bHeight = (Float) this.bounceHeight.atx();
         double time = (System.currentTimeMillis() % 3600000L) / 1000.0;
         float bob = (float) Math.sin(time * bSpeed * Math.PI) * bHeight;
         posY += bob;
      }

      matrices.translate(posX, posY, posZ);

      // Scale
      float s = (Float) this.scale.atx();
      float sx = (Float) this.scaleX.atx() * s;
      float sy = (Float) this.scaleY.atx() * s;
      float sz = (Float) this.scaleZ.atx() * s;
      matrices.scale(sx, sy, sz);

      // Static rotation
      float rx = (Float) this.rotateX.atx();
      float ry = (Float) this.rotateY.atx();
      float rz = (Float) this.rotateZ.atx();
      if (!isMain) {
         ry = -ry;
         rz = -rz;
      }

      if (rx != 0.0F) {
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rx));
      }
      if (ry != 0.0F) {
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
      }
      if (rz != 0.0F) {
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rz));
      }

      // Dynamic continuous spin
      if ((Boolean) this.spin.atx()) {
         float sSpeed = (Float) this.spinSpeed.atx();
         double time = (System.currentTimeMillis() % 3600000L) / 1000.0;
         float spinAngle = (float) ((time * sSpeed * 90.0) % 360.0);
         SpinAxis axis = (SpinAxis) this.spinAxis.atx();
         switch (axis) {
            case X:
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spinAngle));
               break;
            case Y:
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isMain ? spinAngle : -spinAngle));
               break;
            case Z:
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(isMain ? spinAngle : -spinAngle));
               break;
            case All:
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spinAngle * 0.7F));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(isMain ? spinAngle : -spinAngle));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((isMain ? spinAngle : -spinAngle) * 0.5F));
               break;
         }
      }
   }
}
