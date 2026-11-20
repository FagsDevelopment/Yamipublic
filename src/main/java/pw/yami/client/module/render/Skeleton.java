package pw.yami.client.module.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import pw.yami.client.render.BoxRenderer;
import pw.yami.client.utils.render.Render3DContext;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.module.player.Freecam;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.ColorSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class Skeleton extends ClientModule {
    private final Setting selfSetting = this.registerSetting(new BooleanSetting("Self", "Render skeleton for yourself", true));
    private final Setting lineWidthSetting = this.registerSetting(new NumberSetting("LineWidth", "Line width", 1.0F, 0.1F, 3.0F));
    private final Setting colorSetting = this.registerSetting(new ColorSetting("Color", "Skeleton color", new Color(-4351489, true), false, true, null));
    private final Setting glowSetting = this.registerSetting(new BooleanSetting("Glow", "Draw a glow behind the skeleton", false));

    private final List<Vec3d> strip = new ArrayList<>();
    private BoxRenderer boxRenderer;
    private float lineWidth;
    private Color color;

    public Skeleton() {
        super("Skeleton", "Draws skeletons for players (zio port)", ModuleCategory.RENDER);
    }

    @EventListener
    public void onRender3D(Render3DContext ctx) {
        if (client == null || client.world == null || client.player == null) return;
        if (!awg()) return;

        this.boxRenderer = ctx.boxRenderer;
        this.lineWidth = (Float) lineWidthSetting.atx();
        this.color = (Color) colorSetting.atx();

        MatrixStack poseStack = ctx.matrices;
        float partialTick = ctx.f;

        ClientWorld level = client.world;
        Iterator<AbstractClientPlayerEntity> players = level.getPlayers().iterator();
        while (players.hasNext()) {
            AbstractClientPlayerEntity player = players.next();

            boolean selfVisible = true;
            if (!Freecam.gP().awg()) {
                selfVisible = client.options.getPerspective() != Perspective.FIRST_PERSON;
            }
            if (player == client.player) {
                if (!selfVisible) continue;
                if (!(Boolean) selfSetting.atx()) continue;
            }
            if (!player.isAlive()) continue;

            Vec3d relativePosition = new Vec3d(
                player.prevX + (player.getX() - player.prevX) * partialTick,
                player.prevY + (player.getY() - player.prevY) * partialTick,
                player.prevZ + (player.getZ() - player.prevZ) * partialTick);

            PlayerEntityRenderer renderer = (PlayerEntityRenderer) client.getEntityRenderDispatcher().getRenderer(player);
            PlayerEntityModel model = renderer.getModel();
            float limbSwing = player.limbAnimator.getPos(partialTick);
            float limbSwingAmount = player.limbAnimator.getSpeed(partialTick);
            float ageInTicks = player.age + partialTick;
            float netHeadYaw = 0.0F;
            float headPitch = MathHelper.lerp(partialTick, player.prevPitch, player.getPitch());
            model.setAngles(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

            ModelPart head = model.head;
            ModelPart leftArm = model.leftArm;
            ModelPart rightArm = model.rightArm;
            ModelPart leftLeg = model.leftLeg;
            ModelPart rightLeg = model.rightLeg;

            boolean crouching = player.isSneaking();

            strip.clear();
            poseStack.push();
            poseStack.translate(relativePosition.x, relativePosition.y, relativePosition.z);

            float bodyYaw = MathHelper.lerp(partialTick, player.prevBodyYaw, player.bodyYaw);
            Quaternionf bodyRotation = new Quaternionf();
            double yawRad = (bodyYaw + 180.0F) * Math.PI / 180.0;
            bodyRotation.setAngleAxis((float) yawRad, 0.0F, -1.0F, 0.0F);
            poseStack.multiply(bodyRotation);

            double torsoStartY = crouching ? 0.6 : 0.7;
            double torsoEndY = crouching ? 1.05 : 1.4;
            double zOff = crouching ? 0.23 : 0.0;
            vertex(poseStack, 0.0, torsoStartY, zOff, color);
            vertex(poseStack, 0.0, torsoEndY, 0.0, color);

            double shoulderY = crouching ? 1.05 : 1.35;
            vertex(poseStack, -0.37, shoulderY, 0.0, color);
            vertex(poseStack, 0.37, shoulderY, 0.0, color);

            double hipY = crouching ? 0.6 : 0.7;
            double hipZ = crouching ? 0.23 : 0.0;
            vertex(poseStack, -0.15, hipY, hipZ, color);
            vertex(poseStack, 0.15, hipY, hipZ, color);

            // head
            poseStack.push();
            float headOffsetY = crouching ? 1.05F : 1.4F;
            poseStack.translate(0.0F, headOffsetY, 0.0F);
            applyModelPartRotation(poseStack, head);
            vertex(poseStack, 0.0, 0.0, 0.0, color);
            vertex(poseStack, 0.0, 0.15, 0.0, color);
            poseStack.pop();
            strip.clear();

            // right leg
            poseStack.push();
            float rly = crouching ? 0.6F : 0.7F;
            float rlz = crouching ? 0.23F : 0.0F;
            poseStack.translate(0.15F, rly, rlz);
            applyModelPartRotation(poseStack, rightLeg);
            vertex(poseStack, 0.0, 0.0, 0.0, color);
            vertex(poseStack, 0.0, -0.6, 0.0, color);
            poseStack.pop();
            strip.clear();

            // left leg
            poseStack.push();
            float lly = crouching ? 0.6F : 0.7F;
            float llz = crouching ? 0.23F : 0.0F;
            poseStack.translate(-0.15F, lly, llz);
            applyModelPartRotation(poseStack, leftLeg);
            vertex(poseStack, 0.0, 0.0, 0.0, color);
            vertex(poseStack, 0.0, -0.6, 0.0, color);
            poseStack.pop();
            strip.clear();

            // right arm
            poseStack.push();
            float ray = crouching ? 1.05F : 1.35F;
            poseStack.translate(0.37F, ray, 0.0F);
            applyModelPartRotation(poseStack, rightArm);
            vertex(poseStack, 0.0, 0.0, 0.0, color);
            vertex(poseStack, 0.0, -0.55, 0.0, color);
            poseStack.pop();
            strip.clear();

            // left arm
            poseStack.push();
            float lay = crouching ? 1.05F : 1.35F;
            poseStack.translate(-0.37F, lay, 0.0F);
            applyModelPartRotation(poseStack, leftArm);
            vertex(poseStack, 0.0, 0.0, 0.0, color);
            vertex(poseStack, 0.0, -0.55, 0.0, color);
            poseStack.pop();
            strip.clear();

            poseStack.pop();
        }
    }

    private void applyModelPartRotation(MatrixStack poseStack, ModelPart part) {
        if (part.roll != 0.0F) poseStack.multiply(RotationAxis.POSITIVE_Z.rotation(part.roll));
        if (part.yaw != 0.0F) poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(-part.yaw));
        if (part.pitch != 0.0F) poseStack.multiply(RotationAxis.POSITIVE_X.rotation(-part.pitch));
    }

    private void vertex(MatrixStack m, double x, double y, double z, Color color) {
        org.joml.Matrix4f mat = m.peek().getPositionMatrix();
        org.joml.Vector4f v = new org.joml.Vector4f((float) x, (float) y, (float) z, 1.0F);
        v.mul(mat);
        Vec3d world = new Vec3d(v.x, v.y, v.z);
        if (!strip.isEmpty() && boxRenderer != null) {
            Vec3d prev = strip.get(strip.size() - 1);
            boxRenderer.YL(prev.x, prev.y, prev.z, world.x, world.y, world.z, color, lineWidth / 4.0F);
            if (glowSetting.atx() != null && (Boolean) glowSetting.atx()) {
                Color glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(1, (int) (color.getAlpha() * 0.35)));
                boxRenderer.YL(prev.x, prev.y, prev.z, world.x, world.y, world.z, glowColor, (lineWidth / 4.0F) * 3.0F);
            }
        }
        strip.add(world);
    }

    private Vec3d getCameraRelativePosition(Entity entity, float partialTick) {
        Vec3d entityPosition = entity.getPos();
        net.minecraft.client.render.Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPosition = camera.getPos();
        return entityPosition.subtract(cameraPosition);
    }
}
