package pw.yami.client.module.render;

import net.minecraft.client.option.SimpleOption;
import net.minecraft.util.math.MathHelper;
import pw.yami.client.event.EventPhase;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class Zoom extends ClientModule {
    private final Setting smoothSetting = this.registerSetting(new BooleanSetting("Smooth", "Smooth zoom transition", true));
    private final Setting smoothCameraSetting = this.registerSetting(new BooleanSetting("SmoothCamera", "Reduce camera motion while zoomed", true));
    private final Setting scrollSetting = this.registerSetting(new BooleanSetting("Scroll", "Adjust zoom with scroll wheel (not wired in this port)", false));
    private final Setting amountSetting = this.registerSetting(new NumberSetting("Amount", "Zoom divisor (higher = more zoom)", 2, 2, 5));

    private int baselineFov = 75;
    private boolean savedSmoothCamera = false;
    private boolean fovUpdateActive = false;

    private final FloatAnimation zoomAnimation = new FloatAnimation();
    private final FloatAnimation scrollAnimation = new FloatAnimation();

    public Zoom() {
        super("Zoom", "Zooms your camera in.", ModuleCategory.RENDER);
    }

    @Override
    public void awd() {
        if (client == null || client.player == null || client.world == null) {
            this.awc();
            return;
        }
        if (!fovUpdateActive) {
            SimpleOption<Integer> fov = client.options.getFov();
            baselineFov = fov.getValue();
        }
        zoomAnimation.setTarget(1.0F, smoothSetting.atx() != null && (Boolean) smoothSetting.atx() ? 200L : 0L);
        savedSmoothCamera = client.options.smoothCameraEnabled;
        fovUpdateActive = true;
    }

    @Override
    public void awe() {
        if (client != null && client.options != null) {
            SimpleOption<Integer> fovOption = client.options.getFov();
            fovOption.setValue(baselineFov);
            client.options.smoothCameraEnabled = savedSmoothCamera;
            client.options.hudHidden = false;
        }
        fovUpdateActive = false;
        zoomAnimation.setTarget(0.0F, smoothSetting.atx() != null && (Boolean) smoothSetting.atx() ? 100L : 0L);
        scrollAnimation.setTarget(0.0F, smoothSetting.atx() != null && (Boolean) smoothSetting.atx() ? 100L : 0L);
    }

    private int calculateFov() {
        float baseline = baselineFov;
        int divisor = (Integer) amountSetting.atx();
        int fovDifference = 75 / divisor - baselineFov;
        float distance = Math.abs(fovDifference);
        int fov = (int) (baseline - distance * getZoomBlend());
        if (scrollSetting.atx() != null && (Boolean) scrollSetting.atx()) {
            float adjusted = fov - scrollAnimation.getValue();
            fov = (int) MathHelper.clamp(adjusted, 10.0F, 150.0F);
        }
        return fov;
    }

    public float getZoomBlend() {
        double progress = 1.0 - zoomAnimation.getValue();
        return (float) (1.0 - Math.pow(progress, 4.0));
    }

    @EventListener
    public void onTickPre(TickPreEvent tickPreEvent) {
        if (tickPreEvent.getEventPhase() != EventPhase.PRE) return;
        if (client == null || client.player == null || client.world == null) return;
        if (!awg()) return;

        int fov = calculateFov();
        SimpleOption<Integer> fovOption = client.options.getFov();
        fovOption.setValue(fov);

        boolean smoothCamera = false;
        if (awg() && smoothCameraSetting.atx() != null && (Boolean) smoothCameraSetting.atx()) {
            smoothCamera = true;
        }
        client.options.smoothCameraEnabled = smoothCamera;

        if (fov >= baselineFov && !awg()) {
            fovUpdateActive = false;
            return;
        }
        fovUpdateActive = true;
    }

    private static class FloatAnimation {
        private float value = 0.0F;
        private float target = 0.0F;
        private long startTime = 0L;
        private long duration = 0L;

        public void setTarget(float t, long dur) {
            this.target = t;
            this.startTime = System.currentTimeMillis();
            this.duration = Math.max(1L, dur);
            // capture current value as start by keeping value; we approximate linearly
        }

        public float getValue() {
            if (duration <= 0) return target;
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= duration) return target;
            float start = (target == 1.0F) ? 0.0F : 1.0F; // rough: animate 0<->1
            float progress = (float) elapsed / (float) duration;
            return start + (target - start) * progress;
        }
    }
}
