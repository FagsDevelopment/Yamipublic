package pw.yami.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import pw.yami.client.module.ClientModule;
import pw.yami.client.module.ModuleBase;
import pw.yami.client.utils.chat.ChatUtils;

public class HideCommand extends ClientCommand {
   public HideCommand() {
      super("Hide", "Hides a module from the HUD", aep("hide"));
   }

   @Override
   public void aet(LiteralArgumentBuilder literalArgumentBuilder) {
      LiteralArgumentBuilder temp = (LiteralArgumentBuilder)literalArgumentBuilder.then(aer("module", CommandSourceHelper.Sc()).executes(HideCommand::run));
      Command command2 = HideCommand::runNoArg;
      temp.executes(command2);
   }

   private static int runNoArg(CommandContext commandContext) throws CommandSyntaxException {
      ChatUtils.UL("Must provide module to hide!");
      return 1;
   }

   private static int run(CommandContext commandContext) throws CommandSyntaxException {
      ModuleBase moduleBase = CommandSourceHelper.Sd(commandContext);
      if (moduleBase instanceof ClientModule) {
         ((ClientModule) moduleBase).WT(false);
         ChatUtils.UB("§7" + moduleBase.OO() + "§f is now §chidden§f in the Hud");
      }
      return 1;
   }
}
