package pw.yami.client.module.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import pw.yami.client.utils.render.Render3DContext;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.ColorSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class FireFlies extends ClientModule {
    private final Setting colorModeSetting;
    private final Setting pickColorSetting;
    private final Setting darkImprintSetting;
    private final Setting lightingSetting;
    private final Setting spawnDelaySetting;
    private final Setting trailLengthSetting;

    private final List<FirePart> parts = new ArrayList<>();

    public FireFlies() {
        super("FireFlies", "Floating firefly particles (vega port)", ModuleCategory.RENDER);
        colorModeSetting = this.registerSetting(new EnumSetting("ColorMode", "Color mode", ColorMode.Custom, ColorMode.values()));
        pickColorSetting = this.registerSetting(new ColorSetting("PickColor", "Custom color", new Color(255, 70, 0), false, true, null));
        darkImprintSetting = this.registerSetting(new BooleanSetting("DarkImprint", "Darken imprint", false));
        lightingSetting = this.registerSetting(new BooleanSetting("Lighting", "Bloom lighting", false));
        spawnDelaySetting = this.registerSetting(new NumberSetting("SpawnDelay", "Spawn delay (ticks)", 3.0F, 1.0F, 10.0F));
        trailLengthSetting = this.registerSetting(new NumberSetting("TrailLength", "Trail length", 4, 1, 8));
    }

    @Override
    public void awd() {
        parts.clear();
    }

    @Override
    public void awe() {
        parts.clear();
    }

    @EventListener
    public void onTickPre(pw.yami.client.event.TickPreEvent e) {
        if (e.getEventPhase() != pw.yami.client.event.EventPhase.POST) return;
        if (client == null || client.player == null || client.world == null) return;
        if (!awg()) return;

        parts.removeIf(FirePart::isToRemove);
        for (FirePart p : parts) p.update();

        int delayTicks = (int) ((Float) spawnDelaySetting.atx()).floatValue();
        if (delayTicks < 1) delayTicks = 1;
        if (client.player.age % (delayTicks + 1) == 0) {
            int color = getPartColor();
            Vec3d pos = generateVec(client.player.getPos(), 10.0, 4.0);
            parts.add(new FirePart(pos, 5500L, color));
            Vec3d pos2 = generateVec(client.player.getPos(), 6.0, 5.0);
            parts.add(new FirePart(pos2, 5500L, color));
        }
    }

    @EventListener
    public void onRender3D(Render3DContext ctx) {
        if (client == null || client.world == null || client.player == null) return;
        if (!awg()) return;
        if (parts.isEmpty()) return;

        float partialTick = ctx.f;
        MatrixStack matrices = ctx.matrices;

        net.minecraft.client.render.Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();

        // trail pass (kuyruk) - çizgiler
        if (ctx.boxRenderer != null) {
            for (FirePart part : parts) {
                if (part.isToRemove()) continue;
                float alpha = part.getAlpha();
                if (alpha < 0.01F) continue;
                int argb = part.color;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int a = (int) (alpha * 120);
                java.awt.Color col = new java.awt.Color(r, g, b, a);
                List<Vec3d> tr = part.trail;
                for (int i = 0; i + 1 < tr.size(); i++) {
                    Vec3d p1 = tr.get(i);
                    Vec3d p2 = tr.get(i + 1);
                    float fade = (float) (i + 1) / (float) tr.size();
                    int aa = (int) (a * fade);
                    java.awt.Color c2 = new java.awt.Color(r, g, b, Math.max(1, aa));
                    ctx.boxRenderer.YL(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, c2, 1.5F);
                }
                // son segment: son trail -> current pos
                if (!tr.isEmpty()) {
                    Vec3d last = tr.get(tr.size() - 1);
                    Vec3d cur = part.getRenderPos(partialTick);
                    java.awt.Color c3 = new java.awt.Color(r, g, b, a);
                    ctx.boxRenderer.YL(last.x, last.y, last.z, cur.x, cur.y, cur.z, c3, 1.5F);
                }
            }
        }

        // firepart pass (texture'lı quad)
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.render.GameRenderer::getPositionTexColorProgram);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, FIRE_PART_TEX);
        com.mojang.blaze3d.systems.RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);

        BufferBuilder buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR);

        boolean drew = false;
        for (FirePart part : parts) {
            if (part.isToRemove()) continue;
            Vec3d pos = part.getRenderPos(partialTick);
            float alpha = part.getAlpha();
            if (alpha < 0.01F) continue;

            int argb = part.color;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            int a = (int) (alpha * 200);

            float s = 0.15F * alpha * 5.0F;

            matrices.push();
            matrices.translate(pos.x, pos.y, pos.z);
            float camYaw = camera.getYaw();
            float camPitch = camera.getPitch();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - camYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-camPitch));
            buffer.vertex(matrices.peek().getPositionMatrix(), -s, -s, 0).color(r, g, b, a).texture(0, 0);
            buffer.vertex(matrices.peek().getPositionMatrix(), -s, s, 0).color(r, g, b, a).texture(0, 1);
            buffer.vertex(matrices.peek().getPositionMatrix(), s, s, 0).color(r, g, b, a).texture(1, 1);
            buffer.vertex(matrices.peek().getPositionMatrix(), s, -s, 0).color(r, g, b, a).texture(1, 0);
            matrices.pop();
            drew = true;
        }
        if (drew) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        // bloom pass (if lighting)
        if ((Boolean) lightingSetting.atx()) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, BLOOM_TEX);
            com.mojang.blaze3d.systems.RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
            BufferBuilder buffer2 = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR);
            boolean drew2 = false;
            for (FirePart part : parts) {
                if (part.isToRemove()) continue;
                Vec3d pos = part.getRenderPos(partialTick);
                float alpha = part.getAlpha();
                if (alpha < 0.01F) continue;
                int argb = part.color;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int a = (int) (alpha * 40);
                float s = 0.15F * alpha * 5.0F * 12.0F;
                matrices.push();
                matrices.translate(pos.x, pos.y, pos.z);
                float camYaw2 = camera.getYaw();
                float camPitch2 = camera.getPitch();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - camYaw2));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-camPitch2));
                buffer2.vertex(matrices.peek().getPositionMatrix(), -s, -s, 0).color(r, g, b, a).texture(0, 0);
                buffer2.vertex(matrices.peek().getPositionMatrix(), -s, s, 0).color(r, g, b, a).texture(0, 1);
                buffer2.vertex(matrices.peek().getPositionMatrix(), s, s, 0).color(r, g, b, a).texture(1, 1);
                buffer2.vertex(matrices.peek().getPositionMatrix(), s, -s, 0).color(r, g, b, a).texture(1, 0);
                matrices.pop();
                drew2 = true;
            }
            if (drew2) {
                BufferRenderer.drawWithGlobalProgram(buffer2.end());
            }
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        }

        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    }

    private int getPartColor() {
        String mode = colorModeSetting.atx().toString();
        if (mode.equalsIgnoreCase("RandomPalette")) {
            return java.awt.Color.HSBtoRGB((float) Math.random(), 1.0F, 1.0F);
        } else if (mode.equalsIgnoreCase("Client")) {
            return 0xFFFFFFFF;
        } else {
            return ((Color) pickColorSetting.atx()).getRGB();
        }
    }

    private Vec3d generateVec(Vec3d base, double rangeXZ, double rangeY) {
        return base.add(
            (Math.random() * 2 - 1) * rangeXZ,
            (Math.random() * 2 - 1) * rangeY / 3.0,
            (Math.random() * 2 - 1) * rangeXZ);
    }

    public enum ColorMode {
        RandomPalette("RandomPalette"), Custom("Custom"), Client("Client");
        private final String name;
        ColorMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    private static final net.minecraft.util.Identifier FIRE_PART_TEX = net.minecraft.util.Identifier.of("yamiclient", "textures/fireflies/firepart.png");
    private static final net.minecraft.util.Identifier BLOOM_TEX = net.minecraft.util.Identifier.of("yamiclient", "textures/fireflies/bloom.png");

    private class FirePart {
        Vec3d pos;
        Vec3d prevPos;
        List<Vec3d> trail = new ArrayList<>();
        float moveYaw;
        float moveYawSet;
        float speed;
        float yMotion;
        int msChangeSideRate;
        long rateTimer;
        long startTime;
        float maxAlive;
        int color;
        boolean toRemove = false;

        FirePart(Vec3d pos, long maxAlive, int color) {
            this.pos = pos;
            this.prevPos = pos;
            this.maxAlive = maxAlive;
            this.color = color;
            this.startTime = System.currentTimeMillis();
            this.moveYawSet = (float) (Math.random() * 360.0);
            this.moveYaw = this.moveYawSet;
            this.speed = 0.08F + (float) (Math.random() * 0.095);
            this.yMotion = -0.075F + (float) (Math.random() * 0.175);
            this.msChangeSideRate = 300 + (int) (Math.random() * 600);
            this.rateTimer = this.startTime;
        }

        public float getTimePC() {
            return Math.min((float) (System.currentTimeMillis() - startTime) / maxAlive, 1.0F);
        }

        public float getAlpha() {
            float t = getTimePC();
            if (t < 0.1F) return t / 0.1F;
            if (t > 0.8F) return (1.0F - t) / 0.2F;
            return 1.0F;
        }

        public Vec3d getRenderPos(float partialTick) {
            return prevPos.add(pos.subtract(prevPos).multiply(partialTick));
        }

        public void update() {
            long now = System.currentTimeMillis();
            if (now - rateTimer >= msChangeSideRate) {
                msChangeSideRate = 300 + (int) (Math.random() * 600);
                rateTimer = now;
                moveYawSet = (float) (Math.random() * 360.0);
            }
            moveYaw += (moveYawSet - moveYaw) * 0.065F;
            speed /= 1.005F;
            float motionX = -((float) Math.sin(Math.toRadians(moveYaw))) * speed;
            float motionZ = (float) Math.cos(Math.toRadians(moveYaw)) * speed;
            yMotion /= 1.02F;
            trail.add(pos);
            int trailLen = (int) ((Number) trailLengthSetting.atx()).intValue();
            if (trailLen < 1) trailLen = 1;
            while (trail.size() > trailLen) trail.remove(0);
            prevPos = pos;
            pos = pos.add(motionX, yMotion, motionZ);
            if (getTimePC() >= 1.0F) {
                toRemove = true;
            }
        }

        public boolean isToRemove() {
            return toRemove;
        }
    }
}
