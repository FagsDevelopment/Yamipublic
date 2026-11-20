package pw.yami.client.manager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;
import pw.yami.client.Bootstrap;
import pw.yami.client.Mc;
import pw.yami.client.command.BindCommand;
import pw.yami.client.command.BindsCommand;
import pw.yami.client.command.ClientCommand;
import pw.yami.client.command.ConfigCommand;
import pw.yami.client.command.ConnectCommand;
import pw.yami.client.command.CoordsCommand;
import pw.yami.client.command.CustomFontCommand;
import pw.yami.client.command.DebugCommand;
import pw.yami.client.command.DisableAllCommand;
import pw.yami.client.command.DisableCommand;
import pw.yami.client.command.DrawnCommand;
import pw.yami.client.command.EnableCommand;
import pw.yami.client.command.FakeplayerCommand;
import pw.yami.client.command.CopyCommand;
import pw.yami.client.command.PasteCommand;
import pw.yami.client.command.FolderCommand;
import pw.yami.client.command.FriendCommand;
import pw.yami.client.command.HClipCommand;
import pw.yami.client.command.HelpCommand;
import pw.yami.client.command.HideAllCommand;
import pw.yami.client.command.KitCommand;
import pw.yami.client.command.LeaveCommand;
import pw.yami.client.command.LoadCommand;
import pw.yami.client.command.MacroCommand;
import pw.yami.client.command.ModulesCommand;
import pw.yami.client.command.NbtCommand;
import pw.yami.client.command.NotifyCommand;
import pw.yami.client.command.OpenFolderCommand;
import pw.yami.client.command.PeekCommand;
import pw.yami.client.command.PrefixCommand;
import pw.yami.client.command.QueueCommand;
import pw.yami.client.command.ReloadSoundCommand;
import pw.yami.client.command.ResetCommand;
import pw.yami.client.command.ResetGuiCommand;
import pw.yami.client.command.SaveCommand;
import pw.yami.client.command.SettingCommand;
import pw.yami.client.command.SkinGrabCommand;
import pw.yami.client.command.StatsCommand;
import pw.yami.client.command.ToggleCommand;
import pw.yami.client.command.VClipCommand;
import pw.yami.client.command.VanishCommand;
import pw.yami.client.command.VisCommand;
import pw.yami.client.command.HideCommand;
import pw.yami.client.command.WaypointCommand;
import pw.yami.client.command.YawCommand;
import pw.yami.client.command.HudCommand;
import pw.yami.client.event.OnKeyEvent;
import pw.yami.client.event.RefreshEvent;
import pw.yami.client.event.SetScreenEvent;
import pw.yami.client.event.network.SendMessageEvent;
import pw.yami.client.module.ModuleBase;
import pw.yami.client.module.hud.RenderModule;
import pw.yami.eventbus.EventBusImpl;
import pw.yami.eventbus.annotation.EventListener;

public class CommandSourceChatScreenHelper {
   private final List list = new ArrayList();
   private final CommandDispatcher commandDispatcher = new CommandDispatcher();
   private final CommandSource commandSource = a(Mc.client);
   String text;
   private String text2 = ".";
   private int f = 46;
   private static final String c = "Registered {} commands!";

   public CommandSourceChatScreenHelper() {
      EventBusImpl.INSTANCE.rD(this);
      this.adE(
         new BindCommand(),
         new BindsCommand(),
         new ConfigCommand(),
         new ConnectCommand(),
         new CoordsCommand(),
         new DisableAllCommand(),
         new DisableCommand(),
         new EnableCommand(),
         new FakeplayerCommand(),
         new DrawnCommand(),
         new DebugCommand(),
         new CustomFontCommand(),
         new CopyCommand(),
         new PasteCommand(),
         new FolderCommand(),
         new FriendCommand(),
         new HClipCommand(),
         new HelpCommand(),
         new HideAllCommand(),
         new VisCommand(),
         new HideCommand(),
         new KitCommand(),
         new LeaveCommand(),
         new LoadCommand(),
         new MacroCommand(),
         new ModulesCommand(),
         new NbtCommand(),
         new NotifyCommand(),
         new OpenFolderCommand(),
         new PeekCommand(),
         new PrefixCommand(),
         new QueueCommand(),
         new ResetCommand(),
         new ResetGuiCommand(),
         new SaveCommand(),
         new ReloadSoundCommand(),
         new SkinGrabCommand(),
         new StatsCommand(),
         new ToggleCommand(),
         new VanishCommand(),
         new VClipCommand(),
         new WaypointCommand(),
         new YawCommand(),
         new HudCommand()
      );
      Iterator iterator2 = Managers.moduleManager.XM().iterator();

      while (iterator2.hasNext()) {
         ModuleBase moduleBase = (ModuleBase)iterator2.next();
         if (!(moduleBase instanceof RenderModule)) {
            this.adF(new SettingCommand(moduleBase));
         }
      }

      Bootstrap.Oa("Registered {} commands!", this.list.size());
      iterator2 = this.list.iterator();

      while (iterator2.hasNext()) {
         ClientCommand clientCommand = (ClientCommand)iterator2.next();
         Iterator iterator3 = clientCommand.getList().iterator();

         while (iterator3.hasNext()) {
            LiteralArgumentBuilder literalArgumentBuilder = (LiteralArgumentBuilder)iterator3.next();
            clientCommand.aet(literalArgumentBuilder);
            this.commandDispatcher.register(literalArgumentBuilder);
         }
      }
   }

   @EventListener(priority = 999)
   public void onSendMessage(SendMessageEvent sendMessageEvent) {
      String str = sendMessageEvent.auZ().trim();
      String temp = str;
      String str2 = this.text2;
      if (temp.startsWith(str2)) {
         String str3 = str.substring(1);
         sendMessageEvent.aim();
         Mc.client.inGameHud.getChatHud().addToMessageHistory(str);
         if (Mc.client.currentScreen instanceof ChatScreen) {
            this.text = str3;
            return;
         }

         CommandSourceChatScreenHelper commandSourceChatScreenHelper = this;

         try {
            commandSourceChatScreenHelper.commandDispatcher.execute(this.commandDispatcher.parse(str3, this.commandSource));
         } catch (Exception exception) {
         }
      }
   }

   @EventListener
   public void onSetScreen(SetScreenEvent setScreenEvent) {
      if (this.text != null) {
         String str = this.text;
         this.text = null;
         CommandSourceChatScreenHelper temp = this;

         try {
            temp.commandDispatcher.execute(this.commandDispatcher.parse(str, this.commandSource));
         } catch (Exception exception) {
         }
      }
   }

   @EventListener
   public void onOnKey(OnKeyEvent onKeyEvent) {
      if (onKeyEvent.KD() == 1 && onKeyEvent.KC() == this.f && Mc.client.currentScreen == null) {
         onKeyEvent.aim();
         MinecraftClient temp = Mc.client;
         ChatScreen chatScreen = a();
         temp.setScreen((Screen)chatScreen);
      }
   }

   @EventListener
   public void onRefresh(RefreshEvent refreshEvent) {
      refreshEvent.setText(this.text2);
      refreshEvent.setCommandDispatcher(this.commandDispatcher);
      refreshEvent.setCommandSource(this.commandSource);
   }

   private void adE(ClientCommand... value) {
      for (ClientCommand clientCommand : value) {
         this.adF(clientCommand);
      }
   }

   private void adF(ClientCommand clientCommand) {
      if (this.adH(clientCommand.getText()) == null) {
         this.list.add(clientCommand);
      }
   }

   public List getList() {
      return this.list;
   }

   public ClientCommand adH(String str) {
      Iterator iterator2 = this.list.iterator();

      while (iterator2.hasNext()) {
         ClientCommand clientCommand = (ClientCommand)iterator2.next();
         if (clientCommand.getText().equalsIgnoreCase(str)) {
            return clientCommand;
         }
      }

      return null;
   }

   public String getText2() {
      return this.text2;
   }

   public void adJ(String str, int n) {
      this.text2 = str;
      this.f = n;
   }

   public CommandDispatcher getCommandDispatcher() {
      return this.commandDispatcher;
   }

   public CommandSource getCommandSource() {
      return this.commandSource;
   }

   private static ChatScreen a() {
      return new ChatScreen("");
   }

   private static ClientCommandSource a(MinecraftClient client2) {
      return new ClientCommandSource(null, client2);
   }
}
