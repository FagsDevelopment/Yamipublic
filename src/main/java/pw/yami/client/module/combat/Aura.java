package pw.yami.client.module.combat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import pw.yami.client.api.IPlayerCooldown;
import pw.yami.client.event.TickEntitiesPreEvent;
import pw.yami.client.event.network.SendImmediatelyEvent;
import pw.yami.client.manager.Managers;
import pw.yami.client.model.combat.AuraBabyAdultMode;
import pw.yami.client.model.combat.AuraIgnoreBreakMode;
import pw.yami.client.model.combat.AuraNoneNormalMode;
import pw.yami.client.model.combat.AuraSwitchRandomMode;
import pw.yami.client.model.combat.AuraSwordAxeMode;
import pw.yami.client.model.combat.CurrentAverageMode;
import pw.yami.client.model.combat.TargetPriority;
import pw.yami.client.model.player.SwapMode;
import pw.yami.client.model.player.SwingMode;
import pw.yami.client.module.ModuleCategory;
import pw.yami.client.module.Multitask;
import pw.yami.client.module.exploit.Blink;
import pw.yami.client.module.exploit.SpearFix;
import pw.yami.client.module.movement.Follower;
import pw.yami.client.module.movement.Sprint;
import pw.yami.client.setting.BooleanSetting;
import pw.yami.client.setting.EnumSetting;
import pw.yami.client.setting.NumberSetting;
import pw.yami.client.setting.Setting;
import pw.yami.client.utils.StopwatchImpl;
import pw.yami.client.utils.combat.TargetUtils;
import pw.yami.client.utils.player.RotationUtils;
import pw.yami.client.utils.player.StackSlotHelper;
import pw.yami.client.utils.render.WorldUtils;
import pw.yami.client.utils.world.BoxUtils;
import pw.yami.client.utils.world.PlacementUtils;
import pw.yami.eventbus.annotation.EventListener;

public class Aura extends Multitask {
   private static Aura aura;
   private final List list = new ArrayList();
   public Setting eatStop = this.registerSetting(new BooleanSetting("Eat Stop", "Pause attacking while eating or drinking", true));
   Setting weapon = this.registerSetting(new EnumSetting("Weapon", "Only attacks an entity when a specified weapon is in your hand", AuraSwordAxeMode.All, AuraSwordAxeMode.values()));
   Setting rotate = this.registerSetting(new BooleanSetting("Rotate", "Automatically faces towards the target", true));
   Setting swing = this.registerSetting(new EnumSetting("Swing", "Swing side for attack", SwingMode.Normal, SwingMode.values()));
   Setting autoSwitch = this.registerSetting(new EnumSetting("AutoSwitch", "Switches to your selected weapon when attacking the target", AuraNoneNormalMode.None, AuraNoneNormalMode.values()));
   Setting swapAction = this.registerSetting(new EnumSetting("SwapAction", "Decide which swap mode to use", SwapMode.Hotbar, SwapMode.values()));
   Setting shieldMode = this.registerSetting(new EnumSetting("ShieldMode", "Will try and use an axe to break target shields", AuraIgnoreBreakMode.Break, AuraIgnoreBreakMode.values()));
   Setting sortPriority = this.registerSetting(new EnumSetting("SortPriority", "How to filter targets within range", TargetPriority.ClosestAngle, TargetPriority.values()));
   Setting maxTargets = this.registerSetting(new NumberSetting("MaxTargets", "How many entities to target at once", 1, 1, 10));
   Setting mode = this.registerSetting(new EnumSetting("Mode", "mode", AuraSwitchRandomMode.Switch, AuraSwitchRandomMode.values(), this::isModeVisible));
   Setting range = this.registerSetting(new NumberSetting("Range", "The maximum range the entity can be to attack it", 0.0, 3.0, 6.0));
   Setting wallsRange = this.registerSetting(new NumberSetting("WallsRange", "The maximum range the entity can be attacked through walls", 0.0, 3.0, 6.0));
   Setting mobAgeFilter = this.registerSetting(new EnumSetting("MobAgeFilter", "Determines the age of the mobs to target (baby, adult, or both)", AuraBabyAdultMode.Adult, AuraBabyAdultMode.values()));
   Setting ignoreNamed = this.registerSetting(new BooleanSetting("IgnoreNamed", "Whether or not to attack mobs with a name", false));
   Setting ignorePassive = this.registerSetting(new BooleanSetting("IgnorePassive", "Will only attack sometimes passive mobs if they are targeting you", true));
   Setting ignoreTamed = this.registerSetting(new BooleanSetting("IgnoreTamed", "Will avoid attacking mobs you tamed", false));
   Setting booleanSetting1_1 = this.registerSetting(new BooleanSetting("Reverse", "Reverse the entity knock back", false));
   Setting currentAverageMode = this.registerSetting(new EnumSetting("TpsSync", "", CurrentAverageMode.NONE, CurrentAverageMode.values()));
   Setting booleanSetting2_1 = this.registerSetting(new BooleanSetting("CustomDelay", "Use a custom delay instead of the vanilla cooldown", false));
   Setting hitDelay = this.registerSetting(new NumberSetting("HitDelay", "How fast you hit the entity", 0.0F, 11.0F, 60.0F, this.booleanSetting2_1::atx));
   Setting attackCD = this.registerSetting(new NumberSetting("AttackCD", "How fast you hit the entity", 0.0F, 1.0F, 2.0F, this::isAttackCDVisible));
   Setting switchDelay = this.registerSetting(new NumberSetting("SwitchDelay", "How many ticks to wait before hitting an entity after switching hotbar slots", 0, 0, 10));
   Setting players = this.registerSetting(new BooleanSetting("Players", "Entities to attack", true));
   Setting mobs = this.registerSetting(new BooleanSetting("Mobs", "Entities to attack", false));
   Setting animals = this.registerSetting(new BooleanSetting("Animals", "Entities to attack", false));
   Setting villagers = this.registerSetting(new BooleanSetting("Villagers", "Entities to attack", false));
   Setting slimes = this.registerSetting(new BooleanSetting("Slimes", "Entities to attack", false));
   StopwatchImpl stopwatchImpl = new StopwatchImpl();
   int D;
   private int E;
   public static boolean fj;
   private AuraSwordAxeMode previousWeapon = null;
   private boolean wasFollowerActive = false;
   private long lastMaceAttackTime = 0L;
   private boolean isSilentSwapping = false;

   public Aura() {
      super("Aura", "Attacks specified entities around you", ModuleCategory.COMBAT, 700);
      aura = this;
   }

   public static Aura aoS() {
      return aura;
   }

   @Override
   public void awe() {
      this.list.clear();
      this.lastMaceAttackTime = 0L;
      if (this.wasFollowerActive && this.previousWeapon != null) {
         this.weapon.aty(this.previousWeapon);
         this.previousWeapon = null;
         this.wasFollowerActive = false;
      }
   }

   @EventListener
   public void aoT(TickEntitiesPreEvent tickEntitiesPreEvent) {
      this.E--;

      // Follower auto-mace integration: automatically switch weapon to Mace when Follower is enabled in UI, and restore to Sword on disable
      boolean isFollowerActive = Follower.dM() != null && Follower.dM().awg();
      if (isFollowerActive) {
         if (!this.wasFollowerActive) {
            AuraSwordAxeMode current = (AuraSwordAxeMode)this.weapon.atx();
            this.previousWeapon = (current != null && current != AuraSwordAxeMode.Mace) ? current : AuraSwordAxeMode.Sword;
            this.wasFollowerActive = true;
         }
         if (this.weapon.atx() != AuraSwordAxeMode.Mace) {
            this.weapon.aty(AuraSwordAxeMode.Mace);
         }
      } else {
         if (this.wasFollowerActive) {
            AuraSwordAxeMode restoreMode = (this.previousWeapon != null && this.previousWeapon != AuraSwordAxeMode.Mace) ? this.previousWeapon : AuraSwordAxeMode.Sword;
            this.weapon.aty(restoreMode);
            this.previousWeapon = null;
            this.wasFollowerActive = false;
         }
      }

      if (TargetUtils.Xa(client.player) != GameMode.SPECTATOR) {
         if ((!(Boolean)this.eatStop.atx() || !this.isEating()) && ((Boolean)this.breakSetting.atx() || !this.RH(true))) {
            if (!Blink.bm()) {
               this.list.clear();
               final Aura aura2 = this;
               TargetUtils.WY(this.list, new Predicate() {
                  @Override
                  public boolean test(Object tickEntitiesPreEvent) {
                     return aura2.aoX((Entity)tickEntitiesPreEvent);
                  }
               }, (TargetPriority)this.sortPriority.atx(), (Integer)this.maxTargets.atx());

               // Ensure Follower's target is prioritized if Follower is active
               if (Follower.dM() != null && Follower.dM().awg()) {
                  Entity followerTarget = Follower.dM().getTarget();
                  if (followerTarget != null && this.aoX(followerTarget) && !this.list.contains(followerTarget)) {
                     this.list.add(0, followerTarget);
                  }
               }

               if (!this.list.isEmpty()) {
                  int originalSlot = client.player.getInventory().selectedSlot;
                  int weaponSlot = this.findWeaponSlot();
                  boolean isAlreadyHolding = this.apa();
                  AuraNoneNormalMode switchMode = (AuraNoneNormalMode) this.autoSwitch.atx();

                  boolean needSwap = !isAlreadyHolding && switchMode != AuraNoneNormalMode.None && weaponSlot != -1 && weaponSlot != originalSlot;
                  boolean silentSwap = needSwap && switchMode == AuraNoneNormalMode.Silent;

                  ItemStack attackStack = (needSwap || isAlreadyHolding)
                     ? (weaponSlot != -1 ? client.player.getInventory().getStack(weaponSlot) : client.player.getMainHandStack())
                     : client.player.getMainHandStack();

                  if (!this.canAttackWithWeapon(attackStack)) {
                     return;
                  }

                  if (!isAlreadyHolding && switchMode == AuraNoneNormalMode.None) {
                     if (this.getEffectiveWeapon() != AuraSwordAxeMode.Any) {
                        return;
                     }
                  }

                  if (needSwap) {
                     if (silentSwap) {
                        try {
                           this.isSilentSwapping = true;
                           client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(weaponSlot));
                           client.player.getInventory().selectedSlot = weaponSlot;
                           this.aoU();
                        } finally {
                           client.player.getInventory().selectedSlot = originalSlot;
                           client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
                           this.isSilentSwapping = false;
                        }
                     } else {
                        client.player.getInventory().selectedSlot = weaponSlot;
                        client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(weaponSlot));
                        this.aoU();
                     }
                  } else {
                     this.aoU();
                  }
               }
            }
         }
      }
   }

   private void aoU() {
      if (this.list.size() > 1) {
         switch ((AuraSwitchRandomMode)this.mode.atx()) {
            case Switch:
               if (this.D >= this.list.size()) {
                  this.D = 0;
               }

               int n2 = this.D;
               this.aoZ((Entity)this.list.get(n2));
               this.D++;
               break;
            case Random:
               List temp = this.list;
               Random temp2 = random2;
               int n3 = this.list.size();
               int n4 = temp2.nextInt(n3);
               this.aoZ((Entity)temp.get(n4));
               break;
            case Multi:
               List temp3 = this.list;
               final Aura aura2 = this;
               Consumer consumer = new Consumer() {
                  @Override
                  public void accept(Object obj) {
                     aura2.aoZ((Entity)obj);
                  }
               };
               temp3.forEach(consumer);
         }
      } else {
         List list2 = this.list;
         final Aura aura3 = this;
         Consumer consumer2 = new Consumer() {
            @Override
            public void accept(Object obj) {
               aura3.aoZ((Entity)obj);
            }
         };
         list2.forEach(consumer2);
      }
   }

   @EventListener
   private void onSendImmediately(SendImmediatelyEvent sendImmediatelyEvent) {
      if (!this.isSilentSwapping && !this.isUsingMace() && sendImmediatelyEvent.getPacket() instanceof UpdateSelectedSlotC2SPacket) {
         this.E = (Integer)this.switchDelay.atx();
      }
   }

   private boolean aoW() {
      Iterator iterator2 = this.list.iterator();

      while (iterator2.hasNext()) {
         Entity entity2 = (Entity)iterator2.next();
         if (entity2 instanceof PlayerEntity) {
            PlayerEntity temp = (PlayerEntity)entity2;
            DamageSources temp2 = client.world.getDamageSources();
            ClientPlayerEntity player2 = client.player;
            DamageSource damageSource = temp2.playerAttack((PlayerEntity)player2);
            if (temp.blockedByShield(damageSource) && this.shieldMode.atx() == AuraIgnoreBreakMode.Break) {
               return true;
            }
         }
      }

      return false;
   }

   boolean aoX(Entity entity2) {
      ClientPlayerEntity player2 = client.player;
      if (!entity2.equals(player2)) {
         Entity entity3 = client.cameraEntity;
         if (!entity2.equals(entity3)) {
            if ((!(entity2 instanceof LivingEntity) || !((LivingEntity)entity2).isDead()) && entity2.isAlive()) {
               Vec3d vec = BoxUtils.jy(client.player.getEyePos(), entity2.getBoundingBox());
                if (client.player.getEyePos().distanceTo(vec) > (Double)this.range.atx()) {
                  return false;
               }

               if (!this.apc(entity2)) {
                  return false;
               }

               if ((Boolean)this.ignoreNamed.atx() && entity2.hasCustomName()) {
                  return false;
               }

               if (!WorldUtils.Nk(entity2) && client.player.getEyePos().distanceTo(vec) > (Double)this.wallsRange.atx()) {
                  return false;
               }

               if ((Boolean)this.ignoreTamed.atx() && entity2 instanceof Tameable && ((Tameable)entity2).getOwnerUuid() != null) {
                  UUID temp = ((Tameable)entity2).getOwnerUuid();
                  UUID uuid = client.player.getUuid();
                  if (temp.equals(uuid)) {
                     return false;
                  }
               }

               if ((Boolean)this.ignorePassive.atx()) {
                  byte b = 0;

                  label111:
                  while (true) {
                     byte b2 = b;
                     switch (entity2 == null
                        ? -1
                        : (
                           b2 <= 0 && entity2 instanceof EndermanEntity
                              ? 0
                              : (b2 <= 1 && entity2 instanceof ZombifiedPiglinEntity ? 1 : (b2 <= 2 && entity2 instanceof WolfEntity ? 2 : 3))
                        )) {
                        case 0:
                           if (!((EndermanEntity)entity2).isAngry()) {
                              return false;
                           }

                           b = 1;
                           break;
                        case 1:
                           if (!((ZombifiedPiglinEntity)entity2).isAttacking()) {
                              return false;
                           }

                           b = 2;
                           break;
                        case 2:
                           if (!((WolfEntity)entity2).isAttacking()) {
                              return false;
                           }

                           b = 3;
                           break;
                        default:
                           break label111;
                     }
                  }
               }

               if (entity2 instanceof PlayerEntity) {
                  if (((PlayerEntity)entity2).isCreative()) {
                     return false;
                  }

                  if (Managers.friendManager.awr((PlayerEntity)entity2)) {
                     return false;
                  }

                  if (this.shieldMode.atx() == AuraIgnoreBreakMode.Ignore) {
                     PlayerEntity player3 = (PlayerEntity)entity2;
                     DamageSources temp2 = client.world.getDamageSources();
                     ClientPlayerEntity player4 = client.player;
                     DamageSource damageSource = temp2.playerAttack((PlayerEntity)player4);
                     if (player3.blockedByShield(damageSource)) {
                        return false;
                     }
                  }
               }

               if (entity2 instanceof AnimalEntity) {
                  return switch ((AuraBabyAdultMode)this.mobAgeFilter.atx()) {
                     case Baby -> ((AnimalEntity)entity2).isBaby();
                     case Adult -> {
                        if (!((AnimalEntity)entity2).isBaby()) {
                           yield true;
                        } else {
                           yield false;
                        }
                     }
                     case Both -> true;
                  };
               }

               return true;
            }

            return false;
         }
      }

      return false;
   }

   public static double getWeaponAttackSpeed(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return 4.0;
      Item item = stack.getItem();
      if (item instanceof SwordItem) return 1.6;
      if (item instanceof MaceItem) return 0.6;
      if (item instanceof TridentItem) return 1.1;
      if (item instanceof AxeItem) {
         if (item == Items.WOODEN_AXE || item == Items.GOLDEN_AXE || item == Items.STONE_AXE) return 0.8;
         if (item == Items.IRON_AXE) return 0.9;
         return 1.0;
      }
      double[] speed = new double[]{4.0};
      try {
         stack.applyAttributeModifiers(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(EntityAttributes.GENERIC_ATTACK_SPEED)) {
               if (modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                  speed[0] += modifier.value();
               }
            }
         });
      } catch (Exception ignored) {
         return 4.0;
      }
      return Math.max(0.1, speed[0]);
   }

   public static double getWeaponDamage(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return 0.0;
      Item item = stack.getItem();
      if (item instanceof SwordItem) {
         if (item == Items.NETHERITE_SWORD) return 8.0;
         if (item == Items.DIAMOND_SWORD) return 7.0;
         if (item == Items.IRON_SWORD) return 6.0;
         if (item == Items.STONE_SWORD) return 5.0;
         if (item == Items.GOLDEN_SWORD || item == Items.WOODEN_SWORD) return 4.0;
      }
      if (item instanceof AxeItem) {
         if (item == Items.NETHERITE_AXE) return 10.0;
         if (item == Items.DIAMOND_AXE) return 9.0;
         if (item == Items.IRON_AXE) return 9.0;
         if (item == Items.STONE_AXE) return 9.0;
         if (item == Items.GOLDEN_AXE || item == Items.WOODEN_AXE) return 7.0;
      }
      if (item instanceof MaceItem) return 7.0;
      if (item instanceof TridentItem) return 9.0;

      double[] dmg = new double[]{1.0};
      try {
         stack.applyAttributeModifiers(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
               if (modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                  dmg[0] += modifier.value();
               }
            }
         });
      } catch (Exception ignored) {
      }
      return dmg[0];
   }

   public int findWeaponSlot() {
      if (client.player == null) return -1;
      PlayerInventory inv = client.player.getInventory();
      boolean needAxe = this.aoW();
      AuraSwordAxeMode effective = needAxe ? AuraSwordAxeMode.Axe : this.getEffectiveWeapon();

      int bestSlot = -1;
      double highestDamage = -1.0;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = inv.getStack(i);
         if (stack.isEmpty()) continue;
         Item item = stack.getItem();
         boolean matches;

         if (needAxe) {
            matches = item instanceof AxeItem;
         } else {
            matches = switch (effective) {
               case Sword -> item instanceof SwordItem;
               case Axe -> item instanceof AxeItem;
               case Mace -> item instanceof MaceItem;
               case Trident -> item instanceof TridentItem;
               case All -> (item instanceof SwordItem || item instanceof AxeItem || item instanceof MaceItem || item instanceof TridentItem);
               case Any -> true;
            };
         }

         if (matches) {
            double dmg = getWeaponDamage(stack);
            if (dmg > highestDamage) {
               highestDamage = dmg;
               bestSlot = i;
            }
         }
      }
      return bestSlot;
   }

   private boolean canAttackWithWeapon(ItemStack stack) {
      if (client.player == null) return false;

      if (this.E > 0) {
         if (this.isUsingMace()) {
            this.E = 0;
         } else {
            return false;
         }
      }

      if (this.isUsingMace() || (stack != null && stack.getItem() instanceof MaceItem)) {
         if (System.currentTimeMillis() - this.lastMaceAttackTime < 1600L) {
            return false;
         }
      }

      if ((Boolean)this.booleanSetting2_1.atx()) {
         return this.stopwatchImpl.afq((Float)this.hitDelay.atx() * 50.0F * Managers.entityManager.sb((CurrentAverageMode)this.currentAverageMode.atx()));
      }

      double attackSpeed = getWeaponAttackSpeed(stack);
      float cooldownTicks = (float)(1.0 / attackSpeed * 20.0);
      int lastTicks = ((IPlayerCooldown)client.player).yami$getLastAttackedTicks();
      float progress = MathHelper.clamp((lastTicks + 0.5F) / cooldownTicks, 0.0F, 1.0F);

      return progress >= (Float)this.attackCD.atx();
   }

   private boolean aoY() {
      if (client.player == null) return false;
      return this.canAttackWithWeapon(client.player.getMainHandStack());
   }

   private void performAttack(Entity entity2) {
      ClientPlayNetworkHandler networkHandler = client.player.networkHandler;
      boolean sneaking = client.player.isSneaking();
      PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(entity2, sneaking);
      networkHandler.sendPacket(packet);
      client.player.attack(entity2);
      client.player.resetLastAttackedTicks();
   }

   void aoZ(Entity entity2) {
      if (this.isUsingMace()) {
         this.lastMaceAttackTime = System.currentTimeMillis();
      }
      Vec3d vec = BoxUtils.jz(entity2.getBoundingBox());
      if (!(client.player.getEyePos().distanceTo(vec) > (Double)this.range.atx())) {
         if ((Boolean)this.rotate.atx()) {
            float[] fs = RotationUtils.nM(vec);
            RotationUtils.nH(fs[0], fs[1]);
         }

         label49: {
            label55: {
               if ((Boolean)this.booleanSetting1_1.atx() && !Criticals.bZ().awg()) {
                  float[] fs2 = RotationUtils.nM(vec);
                  RotationUtils.nH(fs2[0] - 180.0F, fs2[1]);
                  if (!Managers.vec3dPlayerHelper.BW()) {
                     Managers.serverManager.iO('\uf560', new ClientCommandC2SPacket(client.player, Mode.START_SPRINTING));
                     this.performAttack(entity2);

                     Sprint.a = true;
                     ((SwingMode)this.swing.atx()).avX(Hand.MAIN_HAND);
                     if (!(Boolean)this.booleanSetting1_1.atx() || Criticals.bZ().awg()) {
                        break label49;
                     }

                     Managers.serverManager.iO('\uf560', new ClientCommandC2SPacket(client.player, Mode.STOP_SPRINTING));
                     break label55;
                  }
               }

               this.performAttack(entity2);

               Sprint.a = true;
               ((SwingMode)this.swing.atx()).avX(Hand.MAIN_HAND);
               if (!(Boolean)this.booleanSetting1_1.atx() || Criticals.bZ().awg()) {
                  break label49;
               }
            }

            if ((Boolean)this.rotate.atx()) {
               float[] fs3 = RotationUtils.nM(vec);
               RotationUtils.nH(fs3[0], fs3[1]);
            }
         }

         if ((Boolean)this.rotate.atx()) {
            Managers.movementTracker.Mk();
         }

         this.stopwatchImpl.afr(29432, 5265492, (byte)40);
      }
   }

   private boolean apa() {
      if (SpearFix.nt(client.player.getMainHandStack())) {
         return false;
      }

      if (this.aoW()) {
         return client.player.getMainHandStack().getItem() instanceof AxeItem;
      }

      return switch (this.getEffectiveWeapon()) {
         case Sword -> client.player.getMainHandStack().getItem() instanceof SwordItem;
         case Axe -> client.player.getMainHandStack().getItem() instanceof AxeItem;
         case Mace -> client.player.getMainHandStack().getItem() instanceof MaceItem;
         case Trident -> client.player.getMainHandStack().getItem() instanceof TridentItem;
         case All -> {
            Item held = client.player.getMainHandStack().getItem();
            yield (held instanceof AxeItem || held instanceof SwordItem || held instanceof MaceItem || held instanceof TridentItem);
         }
         default -> true;
      };
   }

   public Entity apb() {
      return !this.list.isEmpty() ? (Entity)this.list.getFirst() : null;
   }

   public boolean hasTarget() {
      return !this.list.isEmpty();
   }

   @Override
   public String hy() {
      return !this.list.isEmpty() ? this.apb().getName().getString() : super.hy();
   }

   private boolean apc(Entity entity2) {
      if (entity2 instanceof SlimeEntity) {
         return (Boolean)this.slimes.atx();
      } else if (entity2 instanceof PlayerEntity) {
         return (Boolean)this.players.atx();
      } else if (entity2 instanceof VillagerEntity || entity2 instanceof WanderingTraderEntity) {
         return (Boolean)this.villagers.atx();
      } else if (entity2 instanceof AnimalEntity) {
         return (Boolean)this.animals.atx();
      } else {
         return entity2 instanceof MobEntity ? (Boolean)this.mobs.atx() : false;
      }
   }

   static boolean apd(ItemStack stack) {
      return stack.getItem() instanceof AxeItem;
   }

   static boolean ape(ItemStack stack) {
      return true;
   }

   static boolean apf(ItemStack stack) {
      boolean flag;
      if (!(stack.getItem() instanceof AxeItem)
         && !(stack.getItem() instanceof SwordItem)
         && !(stack.getItem() instanceof MaceItem)
         && !(stack.getItem() instanceof TridentItem)) {
         flag = false;
      } else {
         flag = true;
      }

      return flag;
   }

   static boolean apg(ItemStack stack) {
      return stack.getItem() instanceof TridentItem;
   }

   static boolean aph(ItemStack stack) {
      return stack.getItem() instanceof MaceItem;
   }

   static boolean api(ItemStack stack) {
      return stack.getItem() instanceof SwordItem;
   }

   static boolean apj(ItemStack stack) {
      return stack.getItem() instanceof AxeItem;
   }

   private Boolean isAttackCDVisible() {
      boolean flag;
      if (!(Boolean)this.booleanSetting2_1.atx()) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private Boolean isModeVisible() {
      boolean flag;
      if ((Integer)this.maxTargets.atx() > 1) {
         flag = true;
      } else {
         flag = false;
      }

      return flag;
   }

   private boolean isEating() {
      if (client.player == null || !client.player.isUsingItem()) return false;
      ItemStack active = client.player.getActiveItem();
      return active.contains(net.minecraft.component.DataComponentTypes.FOOD) || active.getItem() instanceof net.minecraft.item.PotionItem;
   }

   public AuraSwordAxeMode getWeaponMode() {
      return (AuraSwordAxeMode)this.weapon.atx();
   }

   public void setWeaponMode(AuraSwordAxeMode m) {
      if (m != null) {
         this.weapon.aty(m);
      }
   }

   public AuraSwordAxeMode getEffectiveWeapon() {
      if (Follower.dM() != null && Follower.dM().awg()) {
         return AuraSwordAxeMode.Mace;
      }
      return (AuraSwordAxeMode)this.weapon.atx();
   }

   public long getLastMaceAttackTime() {
      return this.lastMaceAttackTime;
   }

   public boolean hasTargetWithin(double maxDistance) {
      if (client.player == null || client.world == null) return false;
      if (!this.list.isEmpty()) {
         for (Object obj : this.list) {
            if (obj instanceof Entity entity && entity.isAlive()) {
               if (client.player.distanceTo(entity) <= maxDistance) {
                  return true;
               }
            }
         }
      }
      for (Entity entity : client.world.getEntities()) {
         if (entity instanceof PlayerEntity && !entity.equals(client.player) && entity.isAlive()) {
            if (!Managers.friendManager.awr((PlayerEntity)entity) && client.player.distanceTo(entity) <= maxDistance) {
               return true;
            }
         }
      }
      return false;
   }

   public boolean isUsingMace() {
      if (client.player == null) return false;
      return client.player.getMainHandStack().getItem() instanceof MaceItem
         || this.getEffectiveWeapon() == AuraSwordAxeMode.Mace;
   }

   private static MatchException n() {
      return new MatchException(null, null);
   }
}
