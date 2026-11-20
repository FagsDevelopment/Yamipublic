package pw.yami.client;

import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pw.yami.client.command.ClientCommand;
import pw.yami.client.config.ClientPaths;
import pw.yami.client.config.ConfigContainer;
import pw.yami.client.config.FileBase;
import pw.yami.client.license.LicenseManager;
import pw.yami.client.manager.AntiCheatManager;
import pw.yami.client.manager.BlockPosWorldManager;
import pw.yami.client.manager.BreakManager;
import pw.yami.client.manager.CommandSourceChatScreenHelper;
import pw.yami.client.manager.DrawMatrixManager;
import pw.yami.client.manager.EntityEntityStatusS2CPacketManager;
import pw.yami.client.manager.EntityEntityTrackerUpdateS2CPacketHelper;
import pw.yami.client.manager.EntityManager;
import pw.yami.client.manager.FileManager;
import pw.yami.client.manager.FramebufferBuilderHelper;
import pw.yami.client.manager.FriendManager;
import pw.yami.client.manager.InventoryTracker;
import pw.yami.client.manager.Managers;
import pw.yami.client.manager.ModuleManager;
import pw.yami.client.manager.MovementTracker;
import pw.yami.client.manager.PlayerScreenManager;
import pw.yami.client.manager.PlayerStateManager;
import pw.yami.client.manager.RenderUtils;
import pw.yami.client.manager.ServerManager;
import pw.yami.client.manager.SoundManager;
import pw.yami.client.manager.TextManager;
import pw.yami.client.manager.UnidentifiedHelper19;
import pw.yami.client.manager.UnidentifiedHelper3;
import pw.yami.client.manager.Vec3dPlayerHelper;
import pw.yami.client.module.client.PlayerModule;
import pw.yami.client.render.BufferVertexBase;
import pw.yami.client.utils.render.DrawTextLayerTypeHelper;
import pw.yami.client.utils.render.MatrixUtils;
import pw.yami.client.utils.render.UnidentifiedUtils10;

public class Bootstrap {
   public static final String a = "Yami";
   public static final String b = "1.1";
   private static final Logger c = LogManager.getLogger("Yami");
   public static ClientPaths clientPaths;
   public static Thread thread;
   public static ExecutorService executorService;
   public static boolean g;

   public static void NT() {
      NZ("Initializing Renderer ...");
      MatrixUtils.Tn();
      UnidentifiedUtils10.atH();
      DrawTextLayerTypeHelper.wV();
   }

   public static void NU() {
      try {
         pw.yami.client.loader.SecureBoot.boot(Bootstrap.class.getClassLoader());
      } catch (Throwable ignored) {}
      MinecraftClient client2 = MinecraftClient.getInstance();
      Mc.client = client2;
      ClientCommand.client = client2;
      ConfigContainer.client = client2;
      BufferVertexBase.client = client2;
      NZ("Starting preInit ...");
      executorService = Executors.newFixedThreadPool(2);
      NZ("Initializing NetworkManager ...");
      Managers.serverManager = new ServerManager();
      NZ("Initializing MacroManager ...");
      Managers.playerScreenManager = new PlayerScreenManager();
      NZ("Initializing HUDManager ...");
      Managers.drawMatrixManager = new DrawMatrixManager();
      NZ("Initializing ModuleManager ...");
      Managers.moduleManager = new ModuleManager();
      NZ("Initializing ReloadManager ...");
      Managers.unidentifiedHelper3 = new UnidentifiedHelper3();
      NZ("Initializing SocialManager ...");
      Managers.friendManager = new FriendManager();
      NZ("Initializing WaypointManager ...");
      Managers.unidentifiedHelper19 = new UnidentifiedHelper19();
      NZ("Initializing TickManager ...");
      Managers.entityManager = new EntityManager();
      NZ("Initializing InventoryManager ...");
      Managers.inventoryTracker = new InventoryTracker();
      NZ("Initializing PositionManager ...");
      Managers.vec3dPlayerHelper = new Vec3dPlayerHelper();
      NZ("Initializing RotationManager ...");
      Managers.movementTracker = new MovementTracker();
      Managers.movementTracker.text = null;
      NZ("Initializing HitboxManager ...");
      Managers.entityEntityTrackerUpdateS2CPacketHelper = new EntityEntityTrackerUpdateS2CPacketHelper();
      NZ("Initializing BreakManager ...");
      Managers.breakManager = new BreakManager();
      NZ("Initializing AntiCheatManager ...");
      Managers.antiCheatManager = new AntiCheatManager();
      NZ("Initializing MovementManager ...");
      Managers.playerStateManager = new PlayerStateManager();
      NZ("Initializing HoleManager ...");
      Managers.blockPosWorldManager = new BlockPosWorldManager();
      NZ("Initializing TotemManager ...");
      Managers.entityEntityStatusS2CPacketManager = new EntityEntityStatusS2CPacketManager();
      NZ("Initializing I18NManager ...");
      Managers.textManager = new TextManager();
      NZ("Initializing CommandManager ...");
      Managers.commandSourceChatScreenHelper = new CommandSourceChatScreenHelper();
      NZ("Initializing SoundManager ...");
      Managers.soundManager = new SoundManager();
      NZ("Initializing ShaderManager ...");
      Managers.framebufferBuilderHelper = new FramebufferBuilderHelper();
      NZ("Initializing LookupManager ...");
      Managers.fileManager = new FileManager();
      NZ("Initializing RenderManager ...");
      Managers.renderUtils = new RenderUtils();
      NZ("Starting postInit ...");
      clientPaths = new ClientPaths();
      NZ("Loading Scripts ...");
      PlayerModule.dc();
      NZ("Loading Config ...");
      ExecutorService temp = executorService;
      final MinecraftClient client3 = client2;
      Runnable runnable = new Runnable() {
         @Override
         public void run() {
            Bootstrap.Oe(client3);
         }
      };
      temp.submit(runnable);
      NZ("Initializing shutdown hook ...");
      thread = new Thread();
      Runtime runtime = Runtime.getRuntime();
      Thread thread2 = thread;
      runtime.addShutdownHook(thread2);
      LicenseManager.get().init(client2);
   }

   public static boolean NV() {
      return FabricLoader.getInstance().getModContainer("baritone").isPresent() || FabricLoader.getInstance().getModContainer("baritone-meteor").isPresent();
   }

   public static boolean NW() {
      return FabricLoader.getInstance().getModContainer("mousetweaks").isPresent();
   }

   public static boolean NX() {
      return FabricLoader.getInstance().getModContainer("immediatelyfast").isPresent();
   }

   public static boolean NY() {
      return FabricLoader.getInstance().getModContainer("ias").isPresent();
   }

   public static void NZ(String str) {
      c.info("[{}] {}", "Yami", str);
   }

   public static void Oa(String str, Object... value) {
      Logger temp = c;
      Object[] objs = new Object[]{str};
      temp.info(String.format("[{}] %s", objs), "Yami", value);
   }

   public static void Ob(String str) {
      c.error("[{}] {}", "Yami", str);
   }

   public static void Oc(String str, Object... value) {
      Logger temp = c;
      Object[] objs = new Object[]{str};
      temp.error(String.format("[{}] %s", objs), "Yami", value);
   }

   public static InputStream Od(String str) {
      return Bootstrap.class.getClassLoader().getResourceAsStream(str);
   }

   static void Oe(MinecraftClient client2) {
      Iterator iterator2 = clientPaths.getSet().iterator();

      while (iterator2.hasNext()) {
         FileBase fileBase = (FileBase)iterator2.next();
         FileBase temp = fileBase;

         try {
            temp.aoQ();
         } catch (Exception exception) {
            exception.printStackTrace();
            Ob("An error occurred while loading the file " + fileBase.getText());
         }
      }

      Runnable runnable = Bootstrap::Of;
      client2.executeSync(runnable);
   }

   private static void Of() {
      g = true;
      NZ("Config loaded");
   }
}
