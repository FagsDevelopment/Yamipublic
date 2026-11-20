package pw.yami.client.module.render;

import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import pw.yami.client.event.ClipToSpaceEvent;
import pw.yami.client.event.player.UpdateRotationEvent;
import pw.yami.client.event.render.BobViewEvent;
import pw.yami.client.event.render.GetFovEvent;
import pw.yami.client.mixin.CameraMixin;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class DynamicCamera extends ClientModule {
   private static DynamicCamera INSTANCE;

   public enum SmoothMode {
      Both,
      F5,
      FOV
   }

   // Dynamic FOV Settings
   public final Setting dynamicFov = this.registerSetting(new BooleanSetting("DynamicFOV", "Dynamically adjusts FOV based on player velocity and actions", true));
   public final Setting fovFactor = this.registerSetting(new NumberSetting("FovFactor", "Multiplier for sprint and movement FOV", 0.5F, 1.15F, 2.0F));
   public final Setting aimZoom = this.registerSetting(new BooleanSetting("AimZoom", "Subtly zooms in when aiming or charging weapons", true));
   public final Setting smoothFov = this.registerSetting(new BooleanSetting("SmoothFOV", "Smoothly transitions FOV changes", true));
   public final Setting fovSpeed = this.registerSetting(new NumberSetting("FovSpeed", "Speed of FOV transitions", 1.0F, 8.0F, 20.0F));

   // Camera Tilt Settings (First Person)
   public final Setting cameraTilt = this.registerSetting(new BooleanSetting("CameraTilt", "Tilts the camera dynamically when strafing left and right", true));
   public final Setting tiltAmount = this.registerSetting(new NumberSetting("TiltAmount", "Maximum angle of sideways camera tilt", 0.1F, 2.0F, 10.0F));
   public final Setting tiltSpeed = this.registerSetting(new NumberSetting("TiltSpeed", "Interpolation speed of camera tilt", 1.0F, 6.0F, 20.0F));

   // F5 Distance — always active in third person
   public final Setting f5Distance = this.registerSetting(new NumberSetting("F5Distance", "Third person camera distance", 2.0F, 9.0F, 25.0F));
   public final Setting distanceSpeed = this.registerSetting(new NumberSetting("DistanceSpeed", "Speed of F5 distance zoom animation", 1.0F, 10.0F, 30.0F));
   public final Setting speedPushback = this.registerSetting(new BooleanSetting("SpeedPushback", "Pushes camera further back when sprinting in F5", false));

   // F5 Sway Animation Settings
   public final Setting moveSway = this.registerSetting(new BooleanSetting("MoveSway", "Dynamic camera sway animation when moving sideways in F5", true));
   public final Setting swayAmount = this.registerSetting(new NumberSetting("SwayAmount", "Amount of lateral camera sway when strafing in F5", 0.1F, 0.75F, 3.0F));
   public final Setting swaySpeed = this.registerSetting(new NumberSetting("SwaySpeed", "Speed of lateral camera sway animation", 1.0F, 5.0F, 20.0F));

   // Smooth Camera / Follow Settings
   public final Setting smoothCamera = this.registerSetting(new BooleanSetting("SmoothCamera", "Cinematic smooth camera rotation follow", false));
   public final Setting smoothMode = this.registerSetting(new EnumSetting("SmoothMode", "Where smooth camera applies", SmoothMode.Both, SmoothMode.values()));
   public final Setting cameraSpeed = this.registerSetting(new NumberSetting("CameraSpeed", "Speed of cinematic camera follow", 1.0F, 12.0F, 30.0F));

   private double currentFov = -1.0;
   private float currentTilt = 0.0F;
   private float smoothYaw = 0.0F;
   private float smoothPitch = 0.0F;
   private Perspective lastPerspective = Perspective.FIRST_PERSON;

   private float animatedDistance = 9.0F;
   private float currentSway = 0.0F;
   private float currentForwardSway = 0.0F;
   private long lastFovTime = System.currentTimeMillis();
   private long lastTiltTime = System.currentTimeMillis();
   private long lastDistTime = System.currentTimeMillis();
   private long lastAnimTime = System.currentTimeMillis();

   public DynamicCamera() {
      super("DynamicCamera", "Dynamic camera motion, FOV smoothing, tilt, and F5 animations", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static DynamicCamera getInstance() {
      return INSTANCE;
   }

   @Override
   public void awe() {
      super.awe();
      this.currentTilt = 0.0F;
      this.currentFov = -1.0;
      this.currentSway = 0.0F;
      this.currentForwardSway = 0.0F;
      this.animatedDistance = 9.0F;
   }

   @EventListener
   public void onGetFov(GetFovEvent event) {
      if (client.player == null || client.world == null) {
         return;
      }
      if (!(Boolean) this.dynamicFov.atx()) {
         return;
      }

      double baseFov = client.options.getFov().getValue();
      double targetFov = baseFov;

      float fovMod = (Float) this.fovFactor.atx();

      // Check player movement state
      boolean isSprinting = client.player.isSprinting();
      if (isSprinting) {
         targetFov += (fovMod - 1.0F) * 20.0F;
      }

      // Speed effect
      if (client.player.hasStatusEffect(StatusEffects.SPEED)) {
         int amp = client.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1;
         targetFov += amp * 3.5F * (fovMod - 0.5F);
      }

      // Slowness effect
      if (client.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
         int amp = client.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1;
         targetFov -= amp * 4.0F;
      }

      // Aiming / charging bow/crossbow/trident
      if ((Boolean) this.aimZoom.atx() && client.player.isUsingItem()) {
         targetFov *= 0.88;
      }

      // Clamp
      targetFov = MathHelper.clamp(targetFov, 30.0, 160.0);

      long now = System.currentTimeMillis();
      float delta = (now - this.lastFovTime) / 1000.0F;
      this.lastFovTime = now;
      delta = MathHelper.clamp(delta, 0.001F, 0.1F);

      if ((Boolean) this.smoothFov.atx()) {
         if (this.currentFov < 0) {
            this.currentFov = targetFov;
         } else {
            float speed = (Float) this.fovSpeed.atx();
            this.currentFov = MathHelper.lerp(Math.min(1.0F, delta * speed), (float) this.currentFov, (float) targetFov);
         }
      } else {
         this.currentFov = targetFov;
      }

      event.Ac(this.currentFov);
      event.aim();
   }

   @EventListener
   public void onBobView(BobViewEvent event) {
      if (client.player == null) {
         return;
      }
      if (!(Boolean) this.cameraTilt.atx()) {
         return;
      }

      // Only apply camera tilt in first person mode to prevent F5 disorientations
      if (!client.options.getPerspective().isFirstPerson()) {
         this.currentTilt = 0.0F;
         return;
      }

      MatrixStack matrices = event.getMatrices();
      if (matrices == null) {
         return;
      }

      float targetTilt = 0.0F;
      float maxTilt = (Float) this.tiltAmount.atx();

      // Check sideways movement
      if (client.player.input != null) {
         float sideways = client.player.input.movementSideways;
         if (sideways > 0.0F) {
            targetTilt = -maxTilt; // Bank left
         } else if (sideways < 0.0F) {
            targetTilt = maxTilt;  // Bank right
         }
      }

      long now = System.currentTimeMillis();
      float delta = (now - this.lastTiltTime) / 1000.0F;
      this.lastTiltTime = now;
      delta = MathHelper.clamp(delta, 0.001F, 0.1F);

      float speed = (Float) this.tiltSpeed.atx();
      this.currentTilt = MathHelper.lerp(Math.min(1.0F, delta * speed), this.currentTilt, targetTilt);

      if (Math.abs(this.currentTilt) > 0.01F) {
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.currentTilt));
      }
   }

   @EventListener
   public void onClipToSpace(ClipToSpaceEvent event) {
      // Only apply custom distance in third person
      if (client.options.getPerspective().isFirstPerson()) {
         return;
      }

      float targetDist = (Float) this.f5Distance.atx();
      if ((Boolean) this.speedPushback.atx() && client.player != null && client.player.isSprinting()) {
         targetDist += 1.5F;
      }

      long now = System.currentTimeMillis();
      float delta = (now - this.lastDistTime) / 1000.0F;
      this.lastDistTime = now;
      delta = MathHelper.clamp(delta, 0.001F, 0.1F);

      float speed = (Float) this.distanceSpeed.atx();
      this.animatedDistance = MathHelper.lerp(Math.min(1.0F, delta * speed), this.animatedDistance, targetDist);

      event.TX(this.animatedDistance);
      event.aim();
   }

   @EventListener
   public void onUpdateRotation(UpdateRotationEvent event) {
      if (client.player == null) {
         return;
      }

      Perspective currentPerspective = client.options.getPerspective();
      if (currentPerspective != this.lastPerspective) {
         this.lastPerspective = currentPerspective;
         this.smoothYaw = event.ET();
         this.smoothPitch = event.EV();
         return;
      }

      if (!(Boolean) this.smoothCamera.atx()) {
         return;
      }

      // Do not smooth rotation when looking at yourself in front view to avoid conflicts
      if (currentPerspective.isFrontView()) {
         return;
      }

      // Check SmoothMode to decide if we should apply here
      SmoothMode mode = (SmoothMode) this.smoothMode.atx();
      boolean isFirstPerson = currentPerspective.isFirstPerson();
      if (mode == SmoothMode.F5 && isFirstPerson) {
         return;
      }
      if (mode == SmoothMode.FOV && !isFirstPerson) {
         return;
      }
      // SmoothMode.Both — always apply

      float targetYaw = event.ET();
      float targetPitch = event.EV();

      float delta = event.EY();
      float speed = (Float) this.cameraSpeed.atx();
      float factor = MathHelper.clamp(delta * speed * 0.1F, 0.05F, 1.0F);

      this.smoothYaw = MathHelper.lerpAngleDegrees(factor, this.smoothYaw, targetYaw);
      this.smoothPitch = MathHelper.lerp(factor, this.smoothPitch, targetPitch);

      event.EU(this.smoothYaw);
      event.EW(this.smoothPitch);
   }

   public void applyThirdPersonAnimation(Camera camera, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) {
      if (!thirdPerson) {
         this.currentSway = 0.0F;
         this.currentForwardSway = 0.0F;
         return;
      }

      if (!(Boolean) this.moveSway.atx()) {
         this.currentSway = 0.0F;
         this.currentForwardSway = 0.0F;
         return;
      }

      long now = System.currentTimeMillis();
      float delta = (now - this.lastAnimTime) / 1000.0F;
      this.lastAnimTime = now;
      delta = MathHelper.clamp(delta, 0.001F, 0.1F);

      float targetSway = 0.0F;
      float targetForward = 0.0F;

      if (client.player != null && client.player.input != null) {
         float sideways = client.player.input.movementSideways;
         float forward = client.player.input.movementForward;

         float maxSway = (Float) this.swayAmount.atx();
         if (sideways > 0.0F) {
            targetSway = -maxSway; // Moving left: camera smoothly sways to frame player
         } else if (sideways < 0.0F) {
            targetSway = maxSway;  // Moving right: camera smoothly sways to other side
         }

         if (forward > 0.0F) {
            targetForward = 0.25F; // Moving forward: camera subtly pulls back
         } else if (forward < 0.0F) {
            targetForward = -0.25F;
         }

         // When looking at self from front, player's left is screen's right
         if (inverseView) {
            targetSway = -targetSway;
         }
      }

      float speed = (Float) this.swaySpeed.atx();
      this.currentSway = MathHelper.lerp(Math.min(1.0F, delta * speed), this.currentSway, targetSway);
      this.currentForwardSway = MathHelper.lerp(Math.min(1.0F, delta * speed), this.currentForwardSway, targetForward);

      if (Math.abs(this.currentSway) > 0.001F || Math.abs(this.currentForwardSway) > 0.001F) {
         double yawRad = Math.toRadians(camera.getYaw());
         // Perpendicular horizontal right vector
         double rightX = -Math.sin(yawRad - Math.PI / 2.0);
         double rightZ = Math.cos(yawRad - Math.PI / 2.0);

         // Forward horizontal vector
         double fwdX = -Math.sin(yawRad);
         double fwdZ = Math.cos(yawRad);

         double offsetX = rightX * this.currentSway - fwdX * this.currentForwardSway;
         double offsetZ = rightZ * this.currentSway - fwdZ * this.currentForwardSway;

         Vec3d pos = camera.getPos();
         ((CameraMixin) camera).invokeSetPos(pos.x + offsetX, pos.y, pos.z + offsetZ);
      }
   }
}
