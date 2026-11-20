package pw.yami.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pw.yami.client.manager.Managers;
import pw.yami.client.utils.chat.ChatUtils;

public class HelpCommand extends ClientCommand {
   public static boolean ac;

   public HelpCommand() {
      super("Help", "Displays command functionality", aeq("help", "h", "?"));
   }

   private static String ael(ClientCommand clientCommand) {
      if (clientCommand.aex().isEmpty()) {
         Object[] objs = new Object[]{clientCommand.getText(), clientCommand.getText2()};
         return String.format("§s%s §7%s", objs);
      } else {
         Object[] objs2;
         Object[] objs3 = objs2 = new Object[]{clientCommand.getText(), null, null};
         Object[] temp = objs3;
         objs2[1] = clientCommand.aex();
         temp[2] = clientCommand.getText2();
         return String.format("§s%s §f%s §7%s", objs3);
      }
   }

   private static String groupOf(String name) {
      String n = name.toLowerCase();
      if (n.equals("hud") || n.equals("hudpos") || n.equals("pos") || n.equals("hpos")) {
         return "HUD";
      }
      if (n.equals("toggle") || n.equals("enable") || n.equals("disable") || n.equals("disableall") || n.equals("drawn") || n.equals("hideall") || n.equals("modules")) {
         return "Modules";
      }
      if (n.equals("config") || n.equals("save") || n.equals("load") || n.equals("prefix") || n.equals("reloadsound") || n.equals("reset") || n.equals("resetgui")) {
         return "Config";
      }
      if (n.equals("bind") || n.equals("binds")) {
         return "Binds";
      }
      if (n.equals("coords") || n.equals("waypoint") || n.equals("yaw") || n.equals("hclip") || n.equals("vclip")) {
         return "Movement";
      }
      if (n.equals("friend") || n.equals("kit") || n.equals("macro") || n.equals("nbt") || n.equals("peek") || n.equals("queue") || n.equals("stats") || n.equals("vanish") || n.equals("skin grab") || n.equals("skingrab")) {
         return "Misc";
      }
      if (n.equals("copy") || n.equals("paste") || n.equals("folder") || n.equals("openfolder") || n.equals("leave") || n.equals("connect") || n.equals("debug") || n.equals("customfont") || n.equals("notify")) {
         return "Utility";
      }
      return "Other";
   }

   @Override
   public void aet(LiteralArgumentBuilder literalArgumentBuilder) {
      LiteralArgumentBuilder temp = (LiteralArgumentBuilder)literalArgumentBuilder.then(aer("command", HelpCommandHelper.avw()).executes(HelpCommand::aen));
      Command command2 = HelpCommand::aem;
      temp.executes(command2);
   }

   private static int aem(CommandContext commandContext) throws CommandSyntaxException {
      ChatUtils.UE("§s[Commands Help]");
      ChatUtils.UE("§7Quick: §f.t §7/ §f.e §7/ §f.d §7/ §f.panic §7/ §f.vis §7/ §f.hide §7/ §f.h");
      Map<String, List<ClientCommand>> groups = new LinkedHashMap<>();
      Iterator iterator2 = Managers.commandSourceChatScreenHelper.getList().iterator();

      while (iterator2.hasNext()) {
         ClientCommand clientCommand = (ClientCommand)iterator2.next();
         if (clientCommand instanceof SettingCommand) {
            continue;
         }
         String g = groupOf(clientCommand.getText());
         groups.computeIfAbsent(g, k -> new ArrayList<>()).add(clientCommand);
      }

      for (Map.Entry<String, List<ClientCommand>> entry : groups.entrySet()) {
         ChatUtils.UE("§e" + entry.getKey() + ":");
         StringBuilder sb = new StringBuilder("  ");
         int n = 0;
         for (ClientCommand c : entry.getValue()) {
            if (n > 0) sb.append("§7, ");
            sb.append("§f").append(c.getText());
            n++;
         }
         ChatUtils.UE(sb.toString());
      }

      return 1;
   }

   private static int aen(CommandContext commandContext) throws CommandSyntaxException {
      ChatUtils.UB(ael(HelpCommandHelper.avx(commandContext)));
      return 1;
   }
}
