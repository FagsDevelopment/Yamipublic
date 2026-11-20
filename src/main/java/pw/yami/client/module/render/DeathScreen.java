package pw.yami.client.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import pw.yami.client.event.TickPreEvent;
import pw.yami.client.event.network.ChannelRead0Event;
import pw.yami.client.event.network.SendImmediatelyEvent;
import pw.yami.client.event.player.UpdateDeathEvent;
import pw.yami.client.event.render.RenderEvent;
import pw.yami.client.handler.EntityHandler;
import pw.yami.client.manager.RenderUtils;
import pw.yami.client.model.world.EntityInteractType;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.setting.StringSetting;
import pw.yami.eventbus.annotation.EventListener;

public class DeathScreen extends ClientModule {
    public Setting titleText = this.registerSetting(new StringSetting("Title", "Banner title text", "ELIMINATED"));
    public Setting duration = this.registerSetting(new NumberSetting("Duration", "Display duration in ms", 1000, 2000, 5000));
    public Setting scale = this.registerSetting(new NumberSetting("Scale", "Overall banner scale", 0.6F, 1.0F, 1.8F));
    public Setting yOffset = this.registerSetting(new NumberSetting("Y-Offset", "Vertical screen position", 0.05F, 0.22F, 0.70F));
    public Setting showVictim = this.registerSetting(new BooleanSetting("Victim", "Show eliminated player name", true));
    public Setting sound = this.registerSetting(new BooleanSetting("Sound", "Play kill sound", true));
    public Setting test = this.registerSetting(new BooleanSetting("Test", "Trigger test animation", false));

    private final Map<Integer, Long> recentAttacks = new ConcurrentHashMap<>();
    private long killTime = 0L;
    private String victimName = "";

    public DeathScreen() {
        super("DeathScreen", "Clean ELIMINATED banner on kills", ModuleCategory.RENDER);
    }

    @Override
    public void awe() {
        this.recentAttacks.clear();
        this.killTime = 0L;
        this.victimName = "";
    }

    public void trigger(String victim) {
        this.killTime = System.currentTimeMillis();
        this.victimName = (victim != null && !victim.isEmpty()) ? victim : "Enemy";
        if ((Boolean)this.sound.atx() && client.player != null) {
            client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.65F);
        }
    }

    @EventListener
    public void onPacketSend(SendImmediatelyEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerInteractEntityC2SPacket) {
            try {
                EntityHandler handler = (EntityHandler)packet;
                if (handler.Qd() == EntityInteractType.ATTACK) {
                    Entity target = handler.Qc();
                    if (target instanceof PlayerEntity) {
                        this.recentAttacks.put(target.getId(), System.currentTimeMillis());
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    @EventListener
    public void onPacketReceive(ChannelRead0Event event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof EntityStatusS2CPacket) {
            EntityStatusS2CPacket p = (EntityStatusS2CPacket)packet;
            if (p.getStatus() == 3 && client.world != null) {
                Entity ent = p.getEntity(client.world);
                if (ent instanceof PlayerEntity && ent != client.player) {
                    this.checkPlayerDeath((PlayerEntity)ent);
                }
            }
        } else if (packet instanceof GameMessageS2CPacket) {
            GameMessageS2CPacket p = (GameMessageS2CPacket)packet;
            if (p.content() != null && client.player != null) {
                String msg = p.content().getString().toLowerCase();
                String myName = client.player.getName().getString().toLowerCase();
                if (msg.contains(myName)) {
                    if (msg.contains("slain") || msg.contains("killed") || msg.contains("shot")
                        || msg.contains("blown up") || msg.contains("eliminated") || msg.contains("died")
                        || msg.contains("fucked") || msg.contains("sniped")) {
                        String victim = this.extractVictim(p.content().getString(), client.player.getName().getString());
                        this.trigger(victim);
                    }
                }
            }
        }
    }

    @EventListener
    public void onDeath(UpdateDeathEvent updateDeathEvent) {
        LivingEntity living = updateDeathEvent.getLiving();
        if (living instanceof PlayerEntity && living != client.player) {
            this.checkPlayerDeath((PlayerEntity)living);
        }
    }

    @EventListener
    public void onTick(TickPreEvent event) {
        if ((Boolean)this.test.atx()) {
            this.test.aty(false);
            this.trigger("TestPlayer");
        }
        long now = System.currentTimeMillis();
        this.recentAttacks.entrySet().removeIf(e -> now - e.getValue() > 6500L);
    }

    private void checkPlayerDeath(PlayerEntity player) {
        long now = System.currentTimeMillis();
        Long attackTime = this.recentAttacks.get(player.getId());
        boolean recentlyAttacked = attackTime != null && (now - attackTime < 6500L);
        boolean inProximity = client.player != null && client.player.squaredDistanceTo(player) < 64.0;
        if (recentlyAttacked || (inProximity && !this.recentAttacks.isEmpty())) {
            this.trigger(player.getName().getString());
        }
    }

    private String extractVictim(String rawMsg, String myName) {
        String[] words = rawMsg.split("\\s+");
        for (String word : words) {
            String clean = word.replaceAll("[^a-zA-Z0-9_]", "");
            if (!clean.isEmpty() && !clean.equalsIgnoreCase(myName) && clean.length() >= 3) {
                return clean;
            }
        }
        return "Enemy";
    }

    @EventListener(priority = 100)
    public void onRender(RenderEvent renderEvent) {
        if (this.killTime == 0L || client.getWindow() == null) return;

        long now = System.currentTimeMillis();
        long elapsed = now - this.killTime;
        long totalDuration = ((Number)this.duration.atx()).longValue();
        if (elapsed >= totalDuration) return;

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        DrawContext ctx = renderEvent.getContext();

        // Simple fade in/out alpha
        long fadeIn = 150L;
        long fadeOut = 400L;
        float alpha;
        if (elapsed < fadeIn) {
            alpha = (float)elapsed / (float)fadeIn;
        } else if (elapsed > totalDuration - fadeOut) {
            alpha = MathHelper.clamp((float)(totalDuration - elapsed) / (float)fadeOut, 0.0F, 1.0F);
        } else {
            alpha = 1.0F;
        }

        // Subtle scale pop
        float baseScale = ((Number)this.scale.atx()).floatValue();
        float animScale;
        if (elapsed < fadeIn) {
            float t = (float)elapsed / (float)fadeIn;
            animScale = baseScale * (1.0F + 0.15F * (1.0F - t) * (1.0F - t));
        } else {
            animScale = baseScale;
        }

        float cx = sw / 2.0F;
        float cy = sh * ((Number)this.yOffset.atx()).floatValue();

        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(cx, cy, 0.0F);
        matrices.scale(animScale, animScale, 1.0F);

        float cardW = 170.0F;
        float cardH = 36.0F;
        float hw = cardW / 2.0F;
        float hh = cardH / 2.0F;

        // Card background
        int bgA = (int)(200.0F * alpha);
        int cardBg = (bgA << 24) | 0x0D0D12;
        ctx.fill((int)-hw, (int)-hh, (int)hw, (int)hh, cardBg);

        // Top accent line
        int lineA = (int)(255.0F * alpha);
        int lineCol = (lineA << 24) | 0xFF2236;
        ctx.fill((int)-hw, (int)-hh, (int)hw, (int)(-hh + 2), lineCol);

        // Title text
        String title = ((String)this.titleText.atx());
        if (title == null || title.trim().isEmpty()) title = "ELIMINATED";
        title = title.toUpperCase();
        float titleW = RenderUtils.acF(title);
        int textCol = (lineA << 24) | 0xFFFFFF;
        RenderUtils.acE(ctx, title, -titleW / 2.0F, -hh + 6.0F, textCol);

        // Thin divider
        int divA = (int)(120.0F * alpha);
        int divCol = (divA << 24) | 0x991824;
        ctx.fill(-40, 2, 40, 3, divCol);

        // Victim name
        if ((Boolean)this.showVictim.atx() && this.victimName != null && !this.victimName.isEmpty()) {
            String sub = this.victimName;
            float subW = RenderUtils.acF(sub);
            int subCol = (lineA << 24) | 0xFFB4BC;
            RenderUtils.acE(ctx, sub, -subW / 2.0F, 6.0F, subCol);
        }

        matrices.pop();
    }
}