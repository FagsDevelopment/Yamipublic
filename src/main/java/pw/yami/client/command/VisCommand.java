package pw.yami.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleBase;
import pw.yami.client.utils.chat.ChatUtils;

public class VisCommand extends ClientCommand {
   public VisCommand() {
      super("Vis", "Makes a module visible in the HUD", aep("vis"));
   }

   @Override
   public void aet(LiteralArgumentBuilder literalArgumentBuilder) {
      LiteralArgumentBuilder temp = (LiteralArgumentBuilder)literalArgumentBuilder.then(aer("module", CommandSourceHelper.Sc()).executes(VisCommand::run));
      Command command2 = VisCommand::runNoArg;
      temp.executes(command2);
   }

   private static int runNoArg(CommandContext commandContext) throws CommandSyntaxException {
      ChatUtils.UL("Must provide module to show!");
      return 1;
   }

   private static int run(CommandContext commandContext) throws CommandSyntaxException {
      ModuleBase moduleBase = CommandSourceHelper.Sd(commandContext);
      if (moduleBase instanceof ClientModule) {
         ((ClientModule) moduleBase).WT(true);
         ChatUtils.UB("§7" + moduleBase.OO() + "§f is now §svisible§f in the Hud");
      }
      return 1;
   }
}
