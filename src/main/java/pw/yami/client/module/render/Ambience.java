package pw.yami.client.module.render;

import java.awt.Color;
import java.util.Calendar;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.world.biome.Biome;
import pw.yami.client.event.EventPhase;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.ColorSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class Ambience extends ClientModule {
    public final Setting brightnessSetting;
    public final Setting colorSetting;
    public final Setting occlusionSetting;
    public final Setting occlusionColorSetting;
    public final Setting worldTimeSetting;
    private final Setting syncSetting;
    private final Setting timeSetting;
    public final Setting worldWeatherSetting;
    public final Setting weatherSetting;
    public final Setting weatherAlwaysSetting;
    public final Setting opacitySetting;
    // fieldDepth / strength dropped (no DoF shader in open-yami)

    private double savedGamma = 1.0;
    private boolean weatherOverrideSuppressed = false;
    private long savedGameTime = 0;
    private long savedDayTime = 0;
    private boolean hasServerTimeSnapshot = false;

    public Ambience() {
        super("Ambience", "Ambient world changes (zio port)", ModuleCategory.RENDER);

        brightnessSetting = this.registerSetting(new EnumSetting("Brightness", "Brightness mode", BrightnessChoice.GAMMA, BrightnessChoice.values()));
        colorSetting = this.registerSetting(new ColorSetting("Color", "Screen color", new Color(-1, true), false, true, this::isScreenBrightnessVisible));
        occlusionSetting = this.registerSetting(new BooleanSetting("Occlusion", "Occlusion", false, this::isOcclusionVisible));
        occlusionColorSetting = this.registerSetting(new ColorSetting("OcclusionColor", "Occlusion color", new Color(-1, true), false, true, this::isOcclusionVisible));
        worldTimeSetting = this.registerSetting(new BooleanSetting("WorldTime", "Override world time", false));
        syncSetting = this.registerSetting(new BooleanSetting("Sync", "Sync to real time", false, this::isSyncVisible));
        timeSetting = this.registerSetting(new NumberSetting("Time", "Time in hours", 0.0F, 0.0F, 24.0F, this::isTimeVisible));
        worldWeatherSetting = this.registerSetting(new BooleanSetting("WorldWeather", "Override weather", false));
        weatherSetting = this.registerSetting(new EnumSetting("Weather", "Weather type", WeatherChoice.CLEAR, WeatherChoice.values(), this::isWeatherVisible));
        weatherAlwaysSetting = this.registerSetting(new BooleanSetting("Always", "Always precipitate", false, this::isAlwaysVisible));
        opacitySetting = this.registerSetting(new NumberSetting("Opacity", "Weather opacity", 100, 0, 100, this::isWeatherVisible));

        previousBrightness = (BrightnessChoice) brightnessSetting.atx();
    }

    private BrightnessChoice previousBrightness;

    @Override
    public void awd() {
        if (client == null || client.world == null) return;
        GameOptions options = client.options;
        SimpleOption<Double> gammaOption = options.getGamma();
        savedGamma = Math.max(0.0, Math.min(1.0, gammaOption.getValue()));
        if (client.world != null) {
            savedGameTime = client.world.getTime();
            savedDayTime = client.world.getTimeOfDay();
            hasServerTimeSnapshot = true;
        } else {
            hasServerTimeSnapshot = false;
        }
        previousBrightness = (BrightnessChoice) brightnessSetting.atx();
    }

    @Override
    public void awe() {
        if (client == null || client.world == null) return;
        ClientPlayerEntity p = client.player;
        if (p != null) {
            RegistryEntry nightVision = StatusEffects.NIGHT_VISION;
            p.removeStatusEffect(nightVision);
        }
        if (hasServerTimeSnapshot && client.world != null) {
            client.world.setTime(savedGameTime);
            client.world.setTimeOfDay(savedDayTime);
        }
        GameOptions options = client.options;
        SimpleOption<Double> gammaOption = options.getGamma();
        gammaOption.setValue(savedGamma);
    }

    @EventListener
    public void onTickPre(TickPreEvent tickPreEvent) {
        if (tickPreEvent.getEventPhase() != EventPhase.POST) return;
        if (client == null || client.player == null || client.world == null) return;
        if (!awg()) return;

        // world time
        if ((Boolean) worldTimeSetting.atx()) {
            long worldTime;
            if ((Boolean) syncSetting.atx()) {
                Calendar calendar = Calendar.getInstance();
                float timeTicks = calendar.get(Calendar.HOUR_OF_DAY);
                float minutes = calendar.get(Calendar.MINUTE);
                timeTicks += minutes / 60.0F;
                timeTicks *= 1000.0F;
                worldTime = (long) (timeTicks + 18000.0F);
            } else {
                float configured = (Float) timeSetting.atx();
                configured *= 1000.0F;
                worldTime = (long) (configured + 18000.0F);
            }
            client.world.setTime(worldTime);
            client.world.setTimeOfDay(worldTime);
        }

        // brightness
        BrightnessChoice bc = (BrightnessChoice) brightnessSetting.atx();
        if (bc == BrightnessChoice.GAMMA) {
            SimpleOption<Double> gammaOption = client.options.getGamma();
            // Vanilla only accepts 0.0-1.0; values above 1.0 spam the log with
            // "Illegal option value" every tick and make the options screen flicker.
            if (gammaOption.getValue() < 0.999) {
                gammaOption.setValue(1.0);
            }
        } else if (bc == BrightnessChoice.POTION) {
            ClientPlayerEntity pl = client.player;
            RegistryEntry effectToCheck = StatusEffects.NIGHT_VISION;
            if (!pl.hasStatusEffect(effectToCheck)) {
                StatusEffectInstance nightVisionEffect = new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 68);
                pl.addStatusEffect(nightVisionEffect);
            }
        } else if (bc == BrightnessChoice.NONE) {
            // nothing
        }
    }

    private Boolean isScreenBrightnessVisible() {
        return brightnessSetting.atx() == BrightnessChoice.SCREEN;
    }

    private Boolean isOcclusionVisible() {
        BrightnessChoice bc = (BrightnessChoice) brightnessSetting.atx();
        return bc == BrightnessChoice.NONE || bc == BrightnessChoice.POTION;
    }

    private Boolean isSyncVisible() {
        return (Boolean) worldTimeSetting.atx();
    }

    private Boolean isTimeVisible() {
        if (!(Boolean) worldTimeSetting.atx()) return false;
        return !(Boolean) syncSetting.atx();
    }

    private Boolean isWeatherVisible() {
        return (Boolean) worldWeatherSetting.atx();
    }

    private Boolean isAlwaysVisible() {
        if (!(Boolean) worldWeatherSetting.atx()) return false;
        WeatherChoice wc = (WeatherChoice) weatherSetting.atx();
        return wc != null && wc.getPrecipitation() != null && wc.getPrecipitation() != Biome.Precipitation.NONE;
    }

    public enum BrightnessChoice {
        SCREEN("Screen"),
        GAMMA("Gamma"),
        POTION("Potion"),
        NONE("None");

        private final String displayName;
        BrightnessChoice(String displayName) { this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }

    public enum WeatherChoice {
        VANILLA("Vanilla", null),
        CLEAR("Clear", Biome.Precipitation.NONE),
        SNOW("Snow", Biome.Precipitation.SNOW),
        RAIN("Rain", Biome.Precipitation.RAIN);

        private final String displayName;
        private final Biome.Precipitation precipitation;
        WeatherChoice(String displayName, Biome.Precipitation precipitation) {
            this.displayName = displayName;
            this.precipitation = precipitation;
        }
        @Override public String toString() { return displayName; }
        public Biome.Precipitation getPrecipitation() { return precipitation; }
    }
}
