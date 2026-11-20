package pw.yami.client.module.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import pw.yami.client.Mc;
import pw.yami.client.event.network.ChannelRead0Event;
import pw.yami.client.event.network.SendImmediatelyEvent;
import pw.yami.client.event.player.UpdateDeathEvent;
import pw.yami.client.gui.DrawBufferScreen;
import pw.yami.client.handler.EntityHandler;
import pw.yami.client.manager.RenderUtils;
import pw.yami.client.model.render.Easing;
import pw.yami.client.model.render.LeftMiddleMode;
import pw.yami.client.model.render.TopMiddleMode;
import pw.yami.client.model.world.EntityInteractType;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.ColorSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.eventbus.EventBusImpl;
import pw.yami.eventbus.annotation.EventListener;

public class CSKillfeed extends RenderModule {
   public static CSKillfeed INSTANCE;

   public enum IconStyle {
      CS2_Silhouette,
      Minecraft_Item
   }

   public enum WeaponType {
      MACE("Mace", Identifier.of("yamiclient", "textures/killfeed/mace.png")),
      SWORD("Sword", Identifier.of("yamiclient", "textures/killfeed/sword.png")),
      CRYSTAL("End Crystal", Identifier.of("yamiclient", "textures/killfeed/crystal.png")),
      ANCHOR("Respawn Anchor", Identifier.of("yamiclient", "textures/killfeed/anchor.png")),
      BOW("Bow", Identifier.of("yamiclient", "textures/killfeed/bow.png")),
      AXE("Axe", Identifier.of("yamiclient", "textures/killfeed/axe.png")),
      TNT("TNT", Identifier.of("yamiclient", "textures/killfeed/tnt.png")),
      BED("Bed", Identifier.of("yamiclient", "textures/killfeed/bed.png")),
      FIST("Fist", Identifier.of("yamiclient", "textures/killfeed/fist.png")),
      GENERIC("Kill", Identifier.of("yamiclient", "textures/killfeed/sword.png"));

      public final String displayName;
      public final Identifier texture;

      WeaponType(String name, Identifier tex) {
         this.displayName = name;
         this.texture = tex;
      }
   }

   private static final Identifier HEADSHOT_ICON = Identifier.of("yamiclient", "textures/killfeed/headshot.png");

   public static class KillNotice {
      public final String killer;
      public final String victim;
      public final WeaponType weapon;
      public final boolean headshot;
      public final boolean isLocalKill;
      public final long timestamp;
      public float slideAnim = 0.0F;

      public KillNotice(String killer, String victim, WeaponType weapon, boolean headshot, boolean isLocalKill) {
         this.killer = killer;
         this.victim = victim;
         this.weapon = weapon;
         this.headshot = headshot;
         this.isLocalKill = isLocalKill;
         this.timestamp = System.currentTimeMillis();
      }
   }

   private static class AttackRecord {
      public final long time;
      public final WeaponType weapon;
      public final boolean isCrit;

      public AttackRecord(long time, WeaponType weapon, boolean isCrit) {
         this.time = time;
         this.weapon = weapon;
         this.isCrit = isCrit;
      }
   }

   // Settings
   public Setting iconStyle = this.registerSetting(new EnumSetting("IconStyle", "Weapon icon rendering style", IconStyle.CS2_Silhouette, IconStyle.values()));
   public Setting duration = this.registerSetting(new NumberSetting("Duration", "Display time per kill in seconds", 1.5F, 4.0F, 8.0F));
   public Setting maxEntries = this.registerSetting(new NumberSetting("MaxNotices", "Maximum kill notices stacked at once", 1, 5, 8));
   public Setting headAvatars = this.registerSetting(new BooleanSetting("Heads", "Display player skin heads next to names", true));
   public Setting headSize = this.registerSetting(new NumberSetting("HeadSize", "Size of player skin head avatars", 10, 14, 20, this::isHeadsVisible));
   public Setting borderHighlight = this.registerSetting(new BooleanSetting("RedBorder", "CS2 red border highlight around your kills", true));
   public Setting borderColor = this.registerSetting(new ColorSetting("BorderColor", "Custom border color", new Color(225, 25, 25, 255), true, false, this::isBorderVisible));
   public Setting killerColor = this.registerSetting(new ColorSetting("KillerColor", "Killer name color (CS2 Gold)", new Color(245, 200, 65, 255)));
   public Setting victimColor = this.registerSetting(new ColorSetting("VictimColor", "Victim name color (CS2 Team Blue)", new Color(90, 160, 225, 255)));
   public Setting boxColor = this.registerSetting(new ColorSetting("BoxColor", "Background container color (pure pitch black)", new Color(0, 0, 0, 255)));
   public Setting sound = this.registerSetting(new BooleanSetting("Sound", "Play kill confirmation sound", true));
   public Setting allKills = this.registerSetting(new BooleanSetting("AllKills", "Show all server kills from chat", false));
   public Setting test = this.registerSetting(new BooleanSetting("Test", "Spawn test CS2 kill notice", false) {
      @Override
      public void Kl(Boolean flag) {
         if (Boolean.TRUE.equals(flag)) {
            triggerTestKill();
            super.Kl(false);
         } else {
            super.Kl(false);
         }
      }
   });

   private final List<KillNotice> notices = new CopyOnWriteArrayList<>();
   private final Map<Integer, AttackRecord> recentAttacks = new ConcurrentHashMap<>();
   private final Map<Integer, Long> lastDeathTimes = new ConcurrentHashMap<>();
   private long lastCrystalAttackTime = 0L;
   private float stackWidth = 160.0F;
   private float stackHeight = 22.0F;
   private int testCycle = 0;

   public CSKillfeed() {
      super("CSKillfeed", "CS2 style killfeed with player head avatars and weapon icons");
      INSTANCE = this;
      this.awb();
      EventBusImpl.INSTANCE.rD(this);
      this.leftMiddleMode.aty(LeftMiddleMode.RIGHT);
      this.topMiddleMode.aty(TopMiddleMode.TOP);
      this.numberSetting1.aty(-10.0F); // 10px from right edge
      this.numberSetting2.aty(25.0F);  // 25px from top
   }

   public static CSKillfeed getInstance() {
      return INSTANCE;
   }

   private Boolean isHeadsVisible() {
      return (Boolean)this.headAvatars.atx();
   }

   private Boolean isBorderVisible() {
      return (Boolean)this.borderHighlight.atx();
   }

   @Override
   protected void awd() {
      super.awd();
      EventBusImpl.INSTANCE.rD(this);
   }

   @Override
   public void awe() {
      super.awe();
      EventBusImpl.INSTANCE.rG(this);
      this.notices.clear();
      this.recentAttacks.clear();
      this.lastDeathTimes.clear();
   }

   public void addKill(String killer, String victim, WeaponType weapon, boolean headshot, boolean isLocalKill) {
      if (killer == null || killer.isEmpty()) killer = "Player";
      if (victim == null || victim.isEmpty()) victim = "Enemy";
      if (weapon == null) weapon = WeaponType.GENERIC;

      int max = ((Number)this.maxEntries.atx()).intValue();
      while (this.notices.size() >= max && !this.notices.isEmpty()) {
         this.notices.remove(0);
      }

      this.notices.add(new KillNotice(killer, victim, weapon, headshot, isLocalKill));

      if (isLocalKill && (Boolean)this.sound.atx() && client.player != null) {
         client.player.playSound(SoundEvents.ENTITY_ARROW_HIT_PLAYER, 1.0F, 1.3F);
      }
   }

   public void triggerTestKill() {
      WeaponType[] testWeapons = new WeaponType[]{
         WeaponType.MACE,
         WeaponType.CRYSTAL,
         WeaponType.SWORD,
         WeaponType.ANCHOR,
         WeaponType.BOW
      };
      WeaponType w = testWeapons[this.testCycle % testWeapons.length];
      boolean hs = (w == WeaponType.MACE || w == WeaponType.BOW);
      this.testCycle++;

      String killer = (client.player != null) ? client.player.getName().getString() : "Triponcio Pilatos";
      String victim = "BOT Larry";

      this.addKill(killer, victim, w, hs, true);
   }

   @EventListener
   public void onDeath(UpdateDeathEvent event) {
      LivingEntity living = event.getLiving();
      if (living instanceof PlayerEntity victim && victim != client.player) {
         this.handlePlayerDeath(victim);
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
                  ItemStack hand = Mc.client.player != null ? Mc.client.player.getMainHandStack() : ItemStack.EMPTY;
                  WeaponType type = getWeaponFromItemStack(hand);
                  boolean isCrit = Mc.client.player != null && Mc.client.player.fallDistance > 0.0F && !Mc.client.player.isOnGround();
                  this.recentAttacks.put(target.getId(), new AttackRecord(System.currentTimeMillis(), type, isCrit));
               } else if (target instanceof EndCrystalEntity || (target != null && target.getClass().getSimpleName().contains("EndCrystal"))) {
                  this.lastCrystalAttackTime = System.currentTimeMillis();
               }
            }
         } catch (Throwable ignored) {}
      }
   }

   @EventListener
   public void onPacketReceive(ChannelRead0Event event) {
      Packet<?> packet = event.getPacket();
      if (packet instanceof EntityStatusS2CPacket p) {
         if (p.getStatus() == 3 && client.world != null) {
            Entity ent = p.getEntity(client.world);
            if (ent instanceof PlayerEntity player && ent != client.player) {
               this.handlePlayerDeath(player);
            }
         }
      } else if (packet instanceof EntityDamageS2CPacket damagePacket) {
         if (client.world != null && client.player != null) {
            int damagedId = damagePacket.entityId();
            Entity target = client.world.getEntityById(damagedId);
            if (target instanceof PlayerEntity && target != client.player) {
               boolean isSource = damagePacket.sourceCauseId() == client.player.getId()
                               || damagePacket.sourceDirectId() == client.player.getId();
               double distSq = client.player.squaredDistanceTo(target);
               if (isSource || distSq <= 256.0) { // 16 blocks
                  ItemStack hand = client.player.getMainHandStack();
                  WeaponType type = getWeaponFromItemStack(hand);
                  boolean isCrit = client.player.fallDistance > 0.0F && !client.player.isOnGround();
                  this.recentAttacks.put(damagedId, new AttackRecord(System.currentTimeMillis(), type, isCrit));
               }
            }
         }
      } else if (packet instanceof GameMessageS2CPacket p) {
         if (p.content() != null && client.player != null) {
            this.parseChatMessage(p.content().getString());
         }
      }
   }

   private void handlePlayerDeath(PlayerEntity victim) {
      if (victim == null || client.player == null || victim == client.player) return;
      int id = victim.getId();
      long now = System.currentTimeMillis();

      // Avoid duplicate trigger for the same death within 2.5s
      Long lastDeath = this.lastDeathTimes.get(id);
      if (lastDeath != null && (now - lastDeath) < 2500L) {
         return;
      }

      AttackRecord record = this.recentAttacks.remove(id);
      boolean recentlyAttacked = (record != null && (now - record.time) < 7000L);
      double distSq = client.player.squaredDistanceTo(victim);
      boolean inCombatProximity = distSq < 400.0; // Within 20 blocks

      if (recentlyAttacked || inCombatProximity) {
         this.lastDeathTimes.put(id, now);
         WeaponType weapon;
         boolean isCrit = false;

         if (record != null) {
            weapon = record.weapon;
            isCrit = record.isCrit;
         } else if ((now - this.lastCrystalAttackTime) < 3000L) {
            weapon = WeaponType.CRYSTAL;
         } else {
            ItemStack hand = client.player.getMainHandStack();
            weapon = getWeaponFromItemStack(hand);
            isCrit = client.player.fallDistance > 0.0F && !client.player.isOnGround();
         }

         if (weapon == WeaponType.MACE) {
            isCrit = true;
         }

         String myName = client.player.getName().getString();
         String victimName = victim.getName().getString();
         this.addKill(myName, victimName, weapon, isCrit, true);
      }
   }

   private void parseChatMessage(String rawMsg) {
      if (rawMsg == null || rawMsg.isEmpty()) return;
      String clean = rawMsg.replaceAll("\u00A7[0-9a-fk-or]", "").trim();
      String lower = clean.toLowerCase();
      String myName = (client.player != null) ? client.player.getName().getString().toLowerCase() : "";

      boolean involvesMe = lower.contains(myName);
      if (!involvesMe && !(Boolean)this.allKills.atx()) {
         return;
      }

      String[] killWords = new String[]{
         "slain", "killed", "smashed", "blown up", "shot", "sniped", "eliminated",
         "died", "fucked", "destroyed", "murdered", "annihilated", "fragged", "rekt",
         "\u00F6ld\u00FCr\u00FCld\u00FC", "katletti", "gebertildi", "yok etti", "olduruldu",
         "katledildi", "ezildi", "vurdu", "deldi", "parcaladi"
      };

      boolean isKillMsg = false;
      for (String kw : killWords) {
         if (lower.contains(kw)) {
            isKillMsg = true;
            break;
         }
      }
      if (!isKillMsg) return;

      WeaponType weapon = WeaponType.SWORD;
      boolean isHeadshot = false;

      if (lower.contains("mace") || lower.contains("smashed")) {
         weapon = WeaponType.MACE;
         isHeadshot = true;
      } else if (lower.contains("crystal") || lower.contains("blown up") || lower.contains("kristal")) {
         weapon = WeaponType.CRYSTAL;
      } else if (lower.contains("anchor")) {
         weapon = WeaponType.ANCHOR;
      } else if (lower.contains("bow") || lower.contains("shot") || lower.contains("sniped") || lower.contains("arrow") || lower.contains("ok")) {
         weapon = WeaponType.BOW;
         isHeadshot = lower.contains("headshot") || lower.contains("sniped");
      } else if (lower.contains("axe") || lower.contains("balta")) {
         weapon = WeaponType.AXE;
      } else if (lower.contains("bed") || lower.contains("yatak")) {
         weapon = WeaponType.BED;
      } else if (lower.contains("tnt") || lower.contains("explosion") || lower.contains("bomba")) {
         weapon = WeaponType.TNT;
      }

      String victim = extractVictimFromChat(clean, (client.player != null) ? client.player.getName().getString() : "");
      String killer = (client.player != null) ? client.player.getName().getString() : "You";

      if (victim != null && !victim.equalsIgnoreCase(killer)) {
         this.addKill(killer, victim, weapon, isHeadshot, true);
      }
   }

   private String extractVictimFromChat(String msg, String myName) {
      String[] tokens = msg.split("[\\s\\[\\](),:;\"'!?]+");
      for (String token : tokens) {
         token = token.replaceAll("[^a-zA-Z0-9_]", "");
         if (token.length() >= 3 && token.length() <= 16 && !token.equalsIgnoreCase(myName)) {
            if (Mc.client.getNetworkHandler() != null && Mc.client.getNetworkHandler().getPlayerListEntry(token) != null) {
               return token;
            }
         }
      }
      for (String token : tokens) {
         token = token.replaceAll("[^a-zA-Z0-9_]", "");
         if (token.length() >= 3 && token.length() <= 16 && !token.equalsIgnoreCase(myName)) {
            return token;
         }
      }
      return "Enemy";
   }

   public static WeaponType getWeaponFromItemStack(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return WeaponType.FIST;
      Item item = stack.getItem();
      if (item == Items.MACE) return WeaponType.MACE;
      if (item instanceof SwordItem) return WeaponType.SWORD;
      if (item instanceof AxeItem) return WeaponType.AXE;
      if (item instanceof BowItem || item instanceof CrossbowItem) return WeaponType.BOW;
      if (item == Items.END_CRYSTAL) return WeaponType.CRYSTAL;
      if (item == Items.RESPAWN_ANCHOR) return WeaponType.ANCHOR;
      if (item == Items.TNT || item == Items.TNT_MINECART) return WeaponType.TNT;
      if (item == Items.RED_BED) return WeaponType.BED;
      return WeaponType.SWORD;
   }

   public static ItemStack getWeaponItemStack(WeaponType type) {
      return switch (type) {
         case MACE -> new ItemStack(Items.MACE);
         case SWORD -> new ItemStack(Items.NETHERITE_SWORD);
         case CRYSTAL -> new ItemStack(Items.END_CRYSTAL);
         case ANCHOR -> new ItemStack(Items.RESPAWN_ANCHOR);
         case BOW -> new ItemStack(Items.BOW);
         case AXE -> new ItemStack(Items.NETHERITE_AXE);
         case TNT -> new ItemStack(Items.TNT);
         case BED -> new ItemStack(Items.RED_BED);
         default -> ItemStack.EMPTY;
      };
   }

   public static Identifier getPlayerSkin(String playerName) {
      if (playerName != null && Mc.client.getNetworkHandler() != null) {
         PlayerListEntry entry = Mc.client.getNetworkHandler().getPlayerListEntry(playerName);
         if (entry != null && entry.getSkinTextures() != null) {
            return entry.getSkinTextures().texture();
         }
      }
      if (playerName != null && Mc.client.world != null) {
         for (PlayerEntity p : Mc.client.world.getPlayers()) {
            if (p instanceof AbstractClientPlayerEntity clientPlayer && playerName.equalsIgnoreCase(p.getName().getString())) {
               return clientPlayer.getSkinTextures().texture();
            }
         }
      }
      if (playerName != null && Mc.client.player != null && Mc.client.player.getName().getString().equalsIgnoreCase(playerName)) {
         return Mc.client.player.getSkinTextures().texture();
      }
      return DefaultSkinHelper.getTexture();
   }

   @Override
   public void adb() {
      if ((Boolean)this.test.atx()) {
         this.test.aty(false);
         this.triggerTestKill();
      }

      long now = System.currentTimeMillis();
      float durationMs = ((Number)this.duration.atx()).floatValue() * 1000.0F;

      // Remove expired notices
      this.notices.removeIf(n -> (now - n.timestamp) > durationMs);

      // Clean old attacks (> 7s)
      this.recentAttacks.entrySet().removeIf(e -> (now - e.getValue().time) > 7000L);

      // Clean old handled deaths (> 10s)
      this.lastDeathTimes.entrySet().removeIf(e -> (now - e.getValue()) > 10000L);

      // Check nearby players for deaths (health <= 0 or isDead)
      if (client.world != null && client.player != null) {
         for (PlayerEntity p : client.world.getPlayers()) {
            if (p != client.player && (p.isDead() || p.getHealth() <= 0.0F)) {
               this.handlePlayerDeath(p);
            }
         }
      }

      // Update slide-in animations
      for (KillNotice n : this.notices) {
         float elapsed = (float)(now - n.timestamp);
         if (elapsed < 240.0F) {
            n.slideAnim = (float) Easing.BACK_OUT.Yy(elapsed / 240.0);
         } else {
            n.slideAnim = 1.0F;
         }
      }

      // Compute stack dimensions
      float maxW = 0.0F;
      float totalH = 0.0F;
      int hSize = (Boolean)this.headAvatars.atx() ? ((Number)this.headSize.atx()).intValue() : 0;
      int iconW = 28;
      int spacing = 4;

      if (this.notices.isEmpty() && client.currentScreen instanceof DrawBufferScreen) {
         maxW = 160.0F;
         totalH = 22.0F;
      } else {
         for (KillNotice n : this.notices) {
            int wKiller = RenderUtils.acF(n.killer);
            int wVictim = RenderUtils.acF(n.victim);
            int hsW = n.headshot ? 14 : 0;
            int nW = 6 + (hSize > 0 ? hSize + 4 : 0) + wKiller + 6 + iconW + (hsW > 0 ? hsW + 4 : 0) + 6 + wVictim + (hSize > 0 ? 4 + hSize : 0) + 6;
            int nH = Math.max(hSize + 6, RenderUtils.acG() + 8);

            if (nW > maxW) maxW = nW;
            totalH += nH + spacing;
         }
         if (totalH > spacing) totalH -= spacing;
      }

      this.stackWidth = Math.max(maxW, 40.0F);
      this.stackHeight = Math.max(totalH, 20.0F);
   }

   @Override
   public float adl() {
      return this.stackWidth;
   }

   @Override
   public float adm() {
      return this.stackHeight;
   }

   @Override
   public void adc(DrawContext context) {
      long now = System.currentTimeMillis();
      float durationMs = ((Number)this.duration.atx()).floatValue() * 1000.0F;
      float baseX = this.add();
      float baseY = this.ade();
      float stackW = this.adl();

      int hSize = (Boolean)this.headAvatars.atx() ? ((Number)this.headSize.atx()).intValue() : 0;
      int iconW = 28;
      int iconH = 14;
      IconStyle style = (IconStyle)this.iconStyle.atx();

      // If in HUD editor and no notices, draw preview
      if (this.notices.isEmpty() && client.currentScreen instanceof DrawBufferScreen) {
         renderNotice(context, new KillNotice("You", "BOT Larry", WeaponType.MACE, true, true), baseX, baseY, 1.0F, hSize, iconW, iconH, style);
         return;
      }

      float curOffsetY = 0.0F;
      for (KillNotice n : this.notices) {
         float elapsed = (float)(now - n.timestamp);
         float remaining = durationMs - elapsed;

         // Fade out during last 400ms
         float alphaFactor = 1.0F;
         if (remaining < 400.0F) {
            alphaFactor = MathHelper.clamp(remaining / 400.0F, 0.0F, 1.0F);
         }

         int wKiller = RenderUtils.acF(n.killer);
         int wVictim = RenderUtils.acF(n.victim);
         int hsW = n.headshot ? 14 : 0;
         int nW = 6 + (hSize > 0 ? hSize + 4 : 0) + wKiller + 6 + iconW + (hsW > 0 ? hsW + 4 : 0) + 6 + wVictim + (hSize > 0 ? 4 + hSize : 0) + 6;
         int nH = Math.max(hSize + 6, RenderUtils.acG() + 8);

         // Align each notice to the right edge with slide-in animation
         float slideX = (1.0F - n.slideAnim) * 35.0F;
         float curX = baseX + (stackW - nW) + slideX;
         float curY = baseY + curOffsetY;

         renderNotice(context, n, curX, curY, alphaFactor, hSize, iconW, iconH, style);

         curOffsetY += nH + 4.0F;
      }
   }

   private void renderNotice(DrawContext context, KillNotice n, float x, float y, float alpha, int hSize, int iconW, int iconH, IconStyle style) {
      int wKiller = RenderUtils.acF(n.killer);
      int wVictim = RenderUtils.acF(n.victim);
      int hsW = n.headshot ? 14 : 0;
      int nW = 6 + (hSize > 0 ? hSize + 4 : 0) + wKiller + 6 + iconW + (hsW > 0 ? hsW + 4 : 0) + 6 + wVictim + (hSize > 0 ? 4 + hSize : 0) + 6;
      int nH = Math.max(hSize + 6, RenderUtils.acG() + 8);

      MatrixStack matrices = context.getMatrices();

      // 1. Box background & border (Pitch Black container + CS2 Red Border)
      int bgAlphaInt = Math.round(255 * alpha);
      Color bgPitchBlack = new Color(0, 0, 0, bgAlphaInt);

      boolean showRedBorder = (Boolean)this.borderHighlight.atx() && n.isLocalKill;
      if (showRedBorder) {
         Color bCol = (Color)this.borderColor.atx();
         Color bColAlpha = new Color(bCol.getRed(), bCol.getGreen(), bCol.getBlue(), bgAlphaInt);
         // Outer red border
         RenderUtils.acH(matrices, x, y, nW, nH, 3.0F, bColAlpha);
         // Inner pitch black background (pure solid #000000)
         RenderUtils.acH(matrices, x + 1.2F, y + 1.2F, nW - 2.4F, nH - 2.4F, 2.0F, bgPitchBlack);
      } else {
         RenderUtils.acH(matrices, x, y, nW, nH, 3.0F, bgPitchBlack);
      }

      float curX = x + 6.0F;
      int textY = (int)(y + (nH - 9) * 0.5F);

      // 2. Killer Head Avatar (on left)
      if (hSize > 0) {
         Identifier killerSkin = getPlayerSkin(n.killer);
         int headY = (int)(y + (nH - hSize) * 0.5F);
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         PlayerSkinDrawer.draw(context, killerSkin, (int)curX, headY, hSize);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         curX += hSize + 4.0F;
      }

      // 3. Killer Name (CS2 Gold)
      Color kCol = (Color)this.killerColor.atx();
      int kAlpha = Math.round(255 * alpha);
      int kColorInt = (kAlpha << 24) | (kCol.getRed() << 16) | (kCol.getGreen() << 8) | kCol.getBlue();
      context.drawText(client.textRenderer, n.killer, (int)curX, textY, kColorInt, true);
      curX += wKiller + 6.0F;

      // 4. Weapon Icon (in center)
      int iconY = (int)(y + (nH - iconH) * 0.5F);
      if (style == IconStyle.Minecraft_Item) {
         ItemStack stack = getWeaponItemStack(n.weapon);
         if (!stack.isEmpty()) {
            context.drawItem(stack, (int)curX + 6, (int)y + (nH - 16) / 2);
         } else {
            drawSilhouette(context, n.weapon.texture, (int)curX, iconY, iconW, iconH, alpha);
         }
      } else {
         drawSilhouette(context, n.weapon.texture, (int)curX, iconY, iconW, iconH, alpha);
      }
      curX += iconW + 6.0F;

      // 5. Headshot Icon (if crit / mace smash)
      if (n.headshot) {
         int hsY = (int)(y + (nH - 14) * 0.5F);
         drawSilhouette(context, HEADSHOT_ICON, (int)curX, hsY, 14, 14, alpha);
         curX += 14 + 4.0F;
      }

      // 6. Victim Name (CS2 Team Blue)
      Color vCol = (Color)this.victimColor.atx();
      int vColorInt = (kAlpha << 24) | (vCol.getRed() << 16) | (vCol.getGreen() << 8) | vCol.getBlue();
      context.drawText(client.textRenderer, n.victim, (int)curX, textY, vColorInt, true);
      curX += wVictim + 4.0F;

      // 7. Victim Head Avatar (on right)
      if (hSize > 0) {
         Identifier victimSkin = getPlayerSkin(n.victim);
         int vicHeadY = (int)(y + (nH - hSize) * 0.5F);
         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         PlayerSkinDrawer.draw(context, victimSkin, (int)curX, vicHeadY, hSize);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void drawSilhouette(DrawContext context, Identifier texture, int x, int y, int w, int h, float alpha) {
      if (texture == null) return;
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      context.drawTexture(texture, x, y, w, h, 0.0F, 0.0F, 64, 32, 64, 32);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }
}