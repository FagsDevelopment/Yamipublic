package pw.yami.client.manager;



import java.util.ArrayList;

import java.util.Arrays;

import java.util.Collections;

import java.util.Comparator;

import java.util.Iterator;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

import java.util.function.Function;

import pw.yami.client.Bootstrap;

import pw.yami.client.config.ConfigContainer;

import pw.yami.client.module.Armor;

import pw.yami.client.module.Chests;

import pw.yami.client.module.ClientModule;

import pw.yami.client.module.ColorHelper;

import pw.yami.client.module.ColorHelper3;

import pw.yami.client.module.Coords;

import pw.yami.client.module.Crosshair;

import pw.yami.client.module.Direction;

import pw.yami.client.module.DrawStackRenderModule;

import pw.yami.client.module.Durability;

import pw.yami.client.module.FPS;

import pw.yami.client.module.HUDLegacy;

import pw.yami.client.module.HitResultWorldHelper;

import pw.yami.client.module.Items;

import pw.yami.client.module.ModuleBase;

import pw.yami.client.module.ModuleManagerColorHelper;

import pw.yami.client.module.ModuleManagerExplosionS2CPacketPlayerPositionLookS2CPacketHelper;

import pw.yami.client.module.ModuleManagerScreenHelper;

import pw.yami.client.module.Modules;

import pw.yami.client.module.Notification;

import pw.yami.client.module.Ping;

import pw.yami.client.module.ServerBrand;

import pw.yami.client.module.Speeds;

import pw.yami.client.module.StackPotionContentsComponentManager;

import pw.yami.client.module.TPS;

import pw.yami.client.module.TextRadar;

import pw.yami.client.module.Watermark;

import pw.yami.client.module.Danger;

import pw.yami.client.module.client.Font;

import pw.yami.client.module.client.HUD;

import pw.yami.client.module.client.PlayerModule;

import pw.yami.client.module.client.UI;

import pw.yami.client.module.combat.AntiCrawl;

import pw.yami.client.module.combat.AntiPhase;

import pw.yami.client.module.combat.AntiRegear;

import pw.yami.client.module.combat.AntiWeakness;

import pw.yami.client.module.combat.ArmorAlert;

import pw.yami.client.module.combat.Aura;

import pw.yami.client.module.combat.AutoAnchor;

import pw.yami.client.module.combat.AutoCrystal;

import pw.yami.client.module.combat.AutoLog;

import pw.yami.client.module.combat.AutoMine;

import pw.yami.client.module.combat.AutoPotion;

import pw.yami.client.module.combat.AutoRelease;

import pw.yami.client.module.combat.AutoSpear;

import pw.yami.client.module.combat.AutoTrap;

import pw.yami.client.module.combat.KurdishTrap;

import pw.yami.client.module.combat.AutoWeb;

import pw.yami.client.module.combat.AutoXP;

import pw.yami.client.module.combat.Blocker;

import pw.yami.client.module.combat.CrawlTrap;

import pw.yami.client.module.combat.Criticals;

import pw.yami.client.module.combat.Flatten;

import pw.yami.client.module.combat.MainHand;

import pw.yami.client.module.combat.NoHitDelay;

import pw.yami.client.module.combat.Offhand;

import pw.yami.client.module.combat.PearlBlocker;

import pw.yami.client.module.combat.Penetrate;

import pw.yami.client.module.combat.PistonCrystal;

import pw.yami.client.module.combat.PistonKick;

import pw.yami.client.module.combat.SelfBow;

import pw.yami.client.module.combat.SelfTrap;

import pw.yami.client.module.combat.ShieldBreaker;

import pw.yami.client.module.combat.SpearAura;

import pw.yami.client.module.combat.Surround;

import pw.yami.client.module.exploit.AntiHunger;

import pw.yami.client.module.exploit.AntiKick;

import pw.yami.client.module.exploit.AutoDupe;

import pw.yami.client.module.exploit.Blink;

import pw.yami.client.module.exploit.ChorusControl;

import pw.yami.client.module.exploit.Crasher;

import pw.yami.client.module.exploit.Disabler;

import pw.yami.client.module.exploit.DurableElytra;

import pw.yami.client.module.exploit.FakeLatency;

import pw.yami.client.module.exploit.FakeVanilla;

import pw.yami.client.module.exploit.FastLatency;

import pw.yami.client.module.exploit.FastProjectile;

import pw.yami.client.module.exploit.GodMode;

import pw.yami.client.module.exploit.HandlerRevision;

import pw.yami.client.module.exploit.MaceSpoof;

import pw.yami.client.module.exploit.NoGlitch;

import pw.yami.client.module.exploit.NoMineAnimation;

import pw.yami.client.module.exploit.PacketCanceler;

import pw.yami.client.module.exploit.PacketFly;

import pw.yami.client.module.exploit.Phase;

import pw.yami.client.module.exploit.RocketExtend;

import pw.yami.client.module.exploit.ScreenKeeper;

import pw.yami.client.module.exploit.SpearFix;

import pw.yami.client.module.exploit.TridentTweaks;

import pw.yami.client.module.exploit.XCarry;

import pw.yami.client.module.hud.RenderModule;

import pw.yami.client.module.misc.AdaptiveFPS;

import pw.yami.client.module.misc.AntiAFK;

import pw.yami.client.module.misc.AntiSpam;

import pw.yami.client.module.misc.AnvilRename;

import pw.yami.client.module.misc.AutoEZ;

import pw.yami.client.module.misc.AutoLogin;

import pw.yami.client.module.misc.AutoReconnect;

import pw.yami.client.module.misc.BetterChat;

import pw.yami.client.module.misc.ChatNotifier;

import pw.yami.client.module.misc.ExtraTab;

import pw.yami.client.module.misc.FakePlayer;

import pw.yami.client.module.misc.DiscordRPC;

import pw.yami.client.module.misc.NameProtect;

import pw.yami.client.module.misc.NoPacketKick;

import pw.yami.client.module.misc.NoSoundLag;

import pw.yami.client.module.misc.PMSound;

import pw.yami.client.module.misc.PacketLogger;

import pw.yami.client.module.misc.Ping2;

import pw.yami.client.module.misc.PotionTracker;

import pw.yami.client.module.misc.SkinBlink;

import pw.yami.client.module.misc.Spammer;

import pw.yami.client.module.misc.SpinBot;

import pw.yami.client.module.misc.Timer;

import pw.yami.client.module.misc.Tooltips;

import pw.yami.client.module.misc.TrueDurability;

import pw.yami.client.module.movement.AntiLevitation;

import pw.yami.client.module.movement.AutoWalk;

import pw.yami.client.module.movement.Avoid;

import pw.yami.client.module.movement.ElytraAcclerate;

import pw.yami.client.module.movement.ElytraFly;

import pw.yami.client.module.movement.ElytraRoam;

import pw.yami.client.module.movement.EntityControl;

import pw.yami.client.module.movement.EntitySpeed;

import pw.yami.client.module.movement.FakeFly;

import pw.yami.client.module.movement.FastFall;

import pw.yami.client.module.movement.FastSwim;

import pw.yami.client.module.movement.Flight;

import pw.yami.client.module.movement.Follower;

import pw.yami.client.module.movement.LongJump;

import pw.yami.client.module.movement.NoFall;

import pw.yami.client.module.movement.NoJumpDelay;

import pw.yami.client.module.movement.NoSlow;

import pw.yami.client.module.movement.Parkour;

import pw.yami.client.module.movement.RocketBoost;

import pw.yami.client.module.movement.SafeWalk;

import pw.yami.client.module.movement.Speed;

import pw.yami.client.module.movement.Sprint;

import pw.yami.client.module.movement.Step;

import pw.yami.client.module.movement.Strafe;

import pw.yami.client.module.movement.Teleport;

import pw.yami.client.module.movement.TickShift;

import pw.yami.client.module.movement.Velocity;

import pw.yami.client.module.movement.Yaw;

import pw.yami.client.module.player.AccuratePotion;

import pw.yami.client.module.player.AirPlace;

import pw.yami.client.module.player.AutoArmor;

import pw.yami.client.module.player.AutoEat;

import pw.yami.client.module.player.AutoFish;

import pw.yami.client.module.player.AutoMount;

import pw.yami.client.module.player.AutoRespawn;

import pw.yami.client.module.player.AutoTool;

import pw.yami.client.module.player.BlockPosBoxMultitask;

import pw.yami.client.module.player.ChestStealer;

import pw.yami.client.module.player.FastPlace;

import pw.yami.client.module.player.FreeLook;

import pw.yami.client.module.player.Freecam;

import pw.yami.client.module.player.GhostHand;


import pw.yami.client.module.player.InventoryManager;

import pw.yami.client.module.player.MouseTweaks;

import pw.yami.client.module.player.MultiTask2;

import pw.yami.client.module.player.NoInteract;

import pw.yami.client.module.player.NoRotate;

import pw.yami.client.module.player.NoSlotSet;

import pw.yami.client.module.player.PacketEat;

import pw.yami.client.module.player.Reach;

import pw.yami.client.module.player.Replenish;

import pw.yami.client.module.player.Scaffold;

import pw.yami.client.module.player.Swing;

import pw.yami.client.module.player.TweakMarco;

import pw.yami.client.module.render.Ambience;

import pw.yami.client.module.render.Animations;

import pw.yami.client.module.render.AntiAliasing;

import pw.yami.client.module.render.AspectRatio;

import pw.yami.client.module.render.BetterHotbar;

import pw.yami.client.module.render.BlockHighlight;

import pw.yami.client.module.render.Breadcrumbs;

import pw.yami.client.module.render.BreakESP;

import pw.yami.client.module.render.Chams;

import pw.yami.client.module.render.CrystalHand;
import pw.yami.client.module.render.CrystalModel;
import pw.yami.client.module.render.DynamicCamera;

import pw.yami.client.module.render.ESP;

import pw.yami.client.module.render.FPSBoost;

import pw.yami.client.module.render.Fullbright;

import pw.yami.client.module.render.Glint;

import pw.yami.client.module.render.HoleESP;

import pw.yami.client.module.render.KillEffects;

import pw.yami.client.module.render.Nametags;

import pw.yami.client.module.render.NoRender;

import pw.yami.client.module.render.Particles;

import pw.yami.client.module.render.PhantomView;

import pw.yami.client.module.render.PhaseESP;

import pw.yami.client.module.render.PlaceEcho;

import pw.yami.client.module.render.PopChams;

import pw.yami.client.module.render.Search;

import pw.yami.client.module.render.Shaders;

import pw.yami.client.module.render.Skeleton;
import pw.yami.client.module.render.BigHead;

import pw.yami.client.module.render.Skybox;

import pw.yami.client.module.render.StorageESP;

import pw.yami.client.module.render.TargetESP;

import pw.yami.client.module.render.Tracers;

import pw.yami.client.module.render.Trajectories;

import pw.yami.client.module.render.TrueSight;

import pw.yami.client.module.render.ViewModel;

import pw.yami.client.module.render.Waypoints;

import pw.yami.client.module.render.IsraelPop;

import pw.yami.client.module.render.IsraelTrigger;


import pw.yami.client.module.render.FireFlies;

import pw.yami.client.module.render.LRain;

import pw.yami.client.module.render.DeathScreen;

import pw.yami.client.module.render.TwoDESP;

import pw.yami.client.module.render.MotionBlur;

import pw.yami.client.module.render.Zoom;

import pw.yami.client.module.hud.CSKillfeed;



public final class ModuleManager {

   private final Map map;

   private static final String c = "Registered {} modules!";



   public ModuleManager() {

      LinkedHashMap linkedHashMap = new LinkedHashMap();

      this.map = Collections.synchronizedMap(linkedHashMap);

      this.XG();

   }



   private void XG() {

      this.XH(

         new PearlBlocker(),

         new PotionTracker(),

         new BetterHotbar(),

         new HitResultWorldHelper(),

         new UI(),

         new ColorHelper3(),

         new Font(),

         new HUD(),

         new ColorHelper(),

         new AntiAliasing(),

         new AntiCrawl(),

         new AntiRegear(),

         new CrawlTrap(),

         new AntiWeakness(),

         new Aura(),

         new ArmorAlert(),

         new AutoMine(),

         new AutoAnchor(),

         new AutoArmor(),

         new AutoRelease(),

         new AutoCrystal(),

         new AutoLog(),

         new Glint(),

         new AutoPotion(),

         new Offhand(),

         new MainHand(),

         new AutoTrap(),

         new KurdishTrap(),

         new AutoXP(),

         new Penetrate(),

         new AntiPhase(),

         new Criticals(),

         new SelfTrap(),

         new Surround(),

         new NoHitDelay(),

         new PistonKick(),

         new ShieldBreaker(),

         new PistonCrystal(),

         new Replenish(),

         new SelfBow(),

         new ChorusControl(),

         new FakeVanilla(),

         new Crasher(),

         new AutoDupe(),

         new Disabler(),

         new ScreenKeeper(),

         new FastLatency(),

         new FastProjectile(),

         new MaceSpoof(),

         new NoMineAnimation(),

         new PacketCanceler(),

         new Phase(),

         new Reach(),

         new AntiAFK(),

         new SpinBot(),

         new AutoEat(),

         new AutoReconnect(),

         new AutoRespawn(),

         new BetterChat(),

         new GhostHand(),

         new InventoryManager(),

         new MouseTweaks(),

         new ChatNotifier(),

         new ChestStealer(),

         new TweakMarco(),

         new DurableElytra(),

         new FakePlayer(),

         new DiscordRPC(),

         new StackPotionContentsComponentManager(),

         new NoPacketKick(),

         new NoSoundLag(),

         new PMSound(),

         new ModuleManagerExplosionS2CPacketPlayerPositionLookS2CPacketHelper(),

         new Spammer(),

         new Swing(),

         new Timer(),

         new TrueDurability(),

         new AdaptiveFPS(),

         new XCarry(),

         new AutoWalk(),

         new RocketBoost(),

         new Flight(),

         new Follower(),

         new Strafe(),

         new NoFall(),

         new NoJumpDelay(),

         new NoSlow(),

         new Parkour(),

         new Speed(),

         new Sprint(),

         new Step(),

         new TridentTweaks(),

         new Ping2(),

         new Velocity(),

         new Ambience(),

         new Animations(),

         new AspectRatio(),

         new BlockHighlight(),

         new Breadcrumbs(),

         new BreakESP(),

         new CrystalModel(),
         new CrystalHand(),
         new DynamicCamera(),

         new Chams(),

         new ESP(),

         new ExtraTab(),

         new Freecam(),

         new FreeLook(),

         new FPSBoost(),

         new Fullbright(),

         new HoleESP(),

         new KillEffects(),

         new NameProtect(),

         new Nametags(),

         new NoRender(),

         new NoRotate(),

         new Particles(),

         new PhaseESP(),

         new PlaceEcho(),

         new TargetESP(),

         new PopChams(),

         new Search(),

         new Shaders(),

         new Skybox(),

         new StorageESP(),

         new Tooltips(),

         new Tracers(),

         new Skeleton(),


         new BigHead(),


         new Trajectories(),

         new TrueSight(),

         new PhantomView(),

         new ViewModel(),

         new Waypoints(),

         new Zoom(),

         new AirPlace(),

         new NoInteract(),

         new BlockPosBoxMultitask(),

         new AutoTool(),

         new AntiKick(),

         new Avoid(),

         new FastPlace(),

         new MultiTask2(),


         new NoGlitch(),

         new Scaffold(),

         new AutoLogin(),

         new FakeFly(),

         new Blocker(),

         new IsraelPop(),

         new IsraelTrigger(),


         new FireFlies(),

          new LRain(),

          new DeathScreen(),

          new TwoDESP(),

          new MotionBlur()

      );

      this.XI(

         new HUDLegacy(),

         new Speeds(),

         new Chests(),

         new FPS(),

         new Modules(),

         new TPS(),

         new DrawStackRenderModule(),

         new Armor(),

         new Notification(),

         new Ping(),

         new Crosshair(),

         new Durability(),

         new Coords(),

         new ServerBrand(),

         new Direction(),

         new Watermark(),

         new Danger(),

         new TextRadar(),

         new ModuleManagerScreenHelper(),

         new Items(),

          new CSKillfeed()

      );

      if (Bootstrap.NV()) {

         this.XJ(new ModuleManagerColorHelper());

      }



      Iterator iterator2 = this.XM().iterator();



      while (iterator2.hasNext()) {

         Object obj = iterator2.next();

         if (obj instanceof ClientModule) {

            Managers.playerScreenManager.akr(((ClientModule)obj).awi());

         }

      }



      Bootstrap.Oa("Registered {} modules!", this.map.size());

   }



   private void XH(ModuleBase... value) {

      Comparator comparator = Comparator.comparing(new Function() {

         @Override

         public Object apply(Object value) {

            return ((ConfigContainer)value).getText2();

         }

      });

      Arrays.sort(value, comparator);



      for (ModuleBase moduleBase : value) {

         this.XJ(moduleBase);

      }

   }



   private void XI(RenderModule... value) {

      Comparator comparator = Comparator.comparing(new Function() {

         @Override

         public Object apply(Object value) {

            return ((ConfigContainer)value).getText2();

         }

      });

      Arrays.sort(value, comparator);



      for (RenderModule renderModule : value) {

         this.XJ(renderModule);

         Managers.drawMatrixManager.Hx(renderModule);

      }

   }



   public void XJ(ModuleBase moduleBase) {

      Map temp = this.map;

      String str = moduleBase.Kn();

      temp.put(str, moduleBase);

   }



   public void XK(ModuleBase moduleBase) {

      Map temp = this.map;

      String str = moduleBase.Kn();

      temp.remove(str);

   }



   public ModuleBase XL(String str) {

      if ("follower-module".equals(str)) {

         ModuleBase m = (ModuleBase)this.map.get("follower-module");

         if (m != null) return m;

         return (ModuleBase)this.map.get("automace-module");

      }

      return (ModuleBase)this.map.get(str);

   }



   public List XM() {

      return new ArrayList(new java.util.LinkedHashSet(this.map.values()));

   }

}

