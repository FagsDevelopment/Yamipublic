package pw.yami.client.module.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import pw.yami.client.event.combat.AttackEntityEvent;
import pw.yami.client.event.network.ChannelRead0Event;
import pw.yami.client.event.render.RenderPreEvent;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.annotation.EventListener;

public class BigHead extends ClientModule {
    public enum Mode {
        Off,
        Always,
        Hit,
        Totem,
        HitTotem
    }

    private final Setting modeSetting = this.registerSetting(new EnumSetting("Mode", "Off / Always / Hit / Totem / HitTotem", Mode.Always, Mode.values()));
    private final Setting scaleSetting = this.registerSetting(new NumberSetting("Scale", "Head scale (and animation peak)", 2.0F, 1.0F, 5.0F));
    private final Setting durationSetting = this.registerSetting(new NumberSetting("Duration", "Hit/Totem animation duration (ms)", 400.0F, 100.0F, 2000.0F));
    private final Setting selfSetting = this.registerSetting(new BooleanSetting("Self", "Apply to yourself", true));
    private final Setting playersSetting = this.registerSetting(new BooleanSetting("Players", "Apply to other players", true));

    private final Map<Integer, Long> animStart = new HashMap<>();

    public BigHead() {
        super("BigHead", "Big heads with Always/Hit/Totem modes", ModuleCategory.RENDER);
    }

    @Override
    public void awe() {
        animStart.clear();
    }

    private Mode mode() {
        return (Mode) modeSetting.atx();
    }

    private boolean applies(Entity entity) {
        if (entity == client.player) return (Boolean) selfSetting.atx();
        return (Boolean) playersSetting.atx();
    }

    private void startAnimation(Entity entity) {
        if (entity == null || client.player == null) return;
        if (!applies(entity)) return;
        animStart.put(entity.getId(), System.currentTimeMillis());
    }

    @EventListener
    public void onAttack(AttackEntityEvent event) {
        if (!awg()) return;
        Mode mode = mode();
        if (mode != Mode.Hit && mode != Mode.HitTotem) return;
        startAnimation(event.getTarget());
    }

    @EventListener
    public void onPacket(ChannelRead0Event event) {
        if (!awg()) return;
        Mode mode = mode();
        if (mode != Mode.Totem && mode != Mode.HitTotem) return;
        if (!(event.getPacket() instanceof EntityStatusS2CPacket)) return;
        EntityStatusS2CPacket packet = (EntityStatusS2CPacket) event.getPacket();
        if (packet.getStatus() != 35) return;
        if (client.world == null) return;
        startAnimation(packet.getEntity(client.world));
    }

    @EventListener
    public void onRenderPre(RenderPreEvent event) {
        if (!(event.entityModel instanceof PlayerEntityModel)) return;
        if (!(event.living instanceof PlayerEntity)) return;
        PlayerEntityModel model = (PlayerEntityModel) event.entityModel;

        float scale = 1.0F;
        if (awg() && applies(event.living)) {
            Mode mode = mode();
            if (mode == Mode.Always) {
                scale = (Float) scaleSetting.atx();
            } else if (mode == Mode.Hit || mode == Mode.Totem || mode == Mode.HitTotem) {
                Long start = animStart.get(event.living.getId());
                if (start != null) {
                    float t = (System.currentTimeMillis() - start) / (Float) durationSetting.atx();
                    if (t >= 1.0F) {
                        animStart.remove(event.living.getId());
                    } else {
                        float pulse = (float) Math.sin(t * Math.PI);
                        scale = 1.0F + ((Float) scaleSetting.atx() - 1.0F) * pulse;
                    }
                }
            }
        }

        model.head.xScale = scale;
        model.head.yScale = scale;
        model.head.zScale = scale;
    }
}
