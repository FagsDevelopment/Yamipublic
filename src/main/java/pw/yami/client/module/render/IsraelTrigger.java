package pw.yami.client.module.render;

import java.awt.Color;
import java.util.Random;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import pw.yami.client.event.render.ApplyFogEvent;
import pw.yami.client.event.render.GetCloudsColorEvent;
import pw.yami.client.event.render.GetDefaultColorEvent;
import pw.yami.client.event.render.GetFogColorOverrideEvent;
import pw.yami.client.event.render.GetGrassColorAtEvent;
import pw.yami.client.event.render.GetSkyColorEvent;
import pw.yami.client.event.render.GetWaterColorEvent;
import pw.yami.client.event.render.RenderEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class IsraelTrigger extends ClientModule {
    private final Setting screenRainbowSetting = this.registerSetting(new BooleanSetting("ScreenRainbow", "Rainbow screen color overlay", true));
    private final Setting rainbowSetting = this.registerSetting(new BooleanSetting("Rainbow", "Rainbow block/ambient colors (sync)", true));
    private final Setting epilepsySetting = this.registerSetting(new BooleanSetting("Epilepsy", "Rapid flashing overlay", true));
    private final Setting popupSetting = this.registerSetting(new BooleanSetting("Popup", "Random israelpop.png popups", true));
    private final Setting fogDistanceSetting = this.registerSetting(new BooleanSetting("FogDistance", "Cycle fog distance", true));
    private final Setting popupFreqSetting = this.registerSetting(new NumberSetting("PopupFreq", "Popup frequency (lower = more)", 20, 5, 100));
    private final Setting intensitySetting = this.registerSetting(new NumberSetting("Intensity", "Effect intensity", 50, 20, 255));

    private static final Identifier POP_TEX = Identifier.of("yamiclient", "textures/israelpop.png");
    private final Random random = new Random();
    private long lastPopup = 0;
    private int popupX = 0, popupY = 0, popupSize = 128;
    private long popupStart = 0;
    private boolean popupActive = false;

    public IsraelTrigger() {
        super("IsraelTrigger", "Rainbow epilepsy troll visual", ModuleCategory.RENDER);
    }

    private Color rainbowColor(long t, float speed) {
        double seconds = t / 1000.0;
        float hue = (float)((seconds * speed) % 1.0);
        if (hue < 0) hue += 1.0F;
        return Color.getHSBColor(hue, 1.0F, 1.0F);
    }

    // Rainbow ambient colors (blocks' colors in sync)
    @EventListener
    public void onFog(GetFogColorOverrideEvent event) {
        if (!awg()) return;
        if (!(Boolean) rainbowSetting.atx()) return;
        event.aim();
        event.setColor(rainbowColor(System.currentTimeMillis(), 0.5F));
    }

    @EventListener
    public void onSky(GetSkyColorEvent event) {
        if (!awg()) return;
        if (!(Boolean) rainbowSetting.atx()) return;
        event.aim();
        event.setColor(rainbowColor(System.currentTimeMillis(), 0.5F));
    }

    @EventListener
    public void onGrass(GetGrassColorAtEvent event) {
        if (!awg()) return;
        if (!(Boolean) rainbowSetting.atx()) return;
        event.aim();
        event.MR(rainbowColor(System.currentTimeMillis(), 0.5F));
    }

    @EventListener
    public void onWater(GetWaterColorEvent event) {
        if (!awg()) return;
        if (!(Boolean) rainbowSetting.atx()) return;
        event.aim();
        event.MR(rainbowColor(System.currentTimeMillis(), 0.5F));
    }

    @EventListener
    public void onDefault(GetDefaultColorEvent event) {
        if (!awg()) return;
        if (!(Boolean) rainbowSetting.atx()) return;
        event.aim();
        event.MR(rainbowColor(System.currentTimeMillis(), 0.5F));
    }

    @EventListener
    public void onClouds(GetCloudsColorEvent event) {
        if (!awg()) return;
        if (!(Boolean) rainbowSetting.atx()) return;
        event.aim();
        event.setColor(rainbowColor(System.currentTimeMillis(), 0.5F));
    }

    // Fog distance cycling (new effect)
    @EventListener
    public void onApplyFog(ApplyFogEvent event) {
        if (!awg()) return;
        if (!(Boolean) fogDistanceSetting.atx()) return;
        event.aim();
        float t = (System.currentTimeMillis() % 4000) / 4000.0F;
        float start = 0.0F + (float) Math.sin(t * Math.PI * 2) * 0.3F;
        float end = 1.0F + (float) Math.sin(t * Math.PI * 2) * 0.5F;
        if (start < 0) start = 0;
        if (end < 0.5F) end = 0.5F;
        event.VT(start);
        event.VV(end);
    }

    @EventListener
    public void onRender(RenderEvent event) {
        if (!awg()) return;
        if (client == null || client.world == null) return;
        DrawContext ctx = event.getContext();
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        int intensity = (Integer) intensitySetting.atx();
        if (intensity < 0) intensity = 0;
        if (intensity > 255) intensity = 255;

        long t = System.currentTimeMillis();

        // Screen Rainbow: full-screen rainbow color overlay (continuous color change)
        if ((Boolean) screenRainbowSetting.atx()) {
            long tmod = t % 800; // fast cycle ~0.8s
            float hue = (float) tmod / 800.0F;
            Color c = Color.getHSBColor(hue, 1.0F, 1.0F);
            int alpha = Math.min(255, intensity);
            int argb = (alpha << 24) | (c.getRGB() & 0x00FFFFFF);
            ctx.fill(0, 0, w, h, argb);
        }

        // Epilepsy: rapid flashing full-screen overlay (increased intensity)
        if ((Boolean) epilepsySetting.atx()) {
            long tmod = t % 300;
            float hue = (float) tmod / 300.0F;
            Color c = Color.getHSBColor(hue, 1.0F, 1.0F);
            int alpha = 50 + (int)(Math.sin(t / 25.0) * 40);
            if (alpha < 15) alpha = 15;
            if (alpha > 100) alpha = 100;
            int argb = (alpha << 24) | (c.getRGB() & 0x00FFFFFF);
            ctx.fill(0, 0, w, h, argb);
        }

        // Random popup with fade in/out
        if ((Boolean) popupSetting.atx()) {
            long now = System.currentTimeMillis();
            int freq = (Integer) popupFreqSetting.atx();
            if (freq < 1) freq = 1;
            if (!popupActive && (now - lastPopup) > (freq * 40L)) {
                lastPopup = now;
                popupActive = true;
                popupStart = now;
                popupX = random.nextInt(Math.max(1, w - 128));
                popupY = random.nextInt(Math.max(1, h - 128));
                popupSize = 96 + random.nextInt(96);
            }
            if (popupActive) {
                long elapsed = now - popupStart;
                long total = 1500L;
                if (elapsed > total) {
                    popupActive = false;
                } else {
                    float progress = (float) elapsed / (float) total;
                    float alpha = 1.0F;
                    if (progress < 0.2F) alpha = progress / 0.2F;
                    else if (progress > 0.8F) alpha = (1.0F - progress) / 0.2F;
                    if (alpha > 0.01F) {
                        ctx.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                        ctx.drawTexture(POP_TEX, popupX, popupY, 0, 0, popupSize, popupSize, popupSize, popupSize);
                        ctx.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }
            }
        }
    }
}
