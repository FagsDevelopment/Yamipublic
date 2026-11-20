package pw.yami.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import pw.yami.client.Bootstrap;
import pw.yami.client.utils.chat.ChatUtils;

public class LoadCommand extends ClientCommand {
   public static boolean d;

   public LoadCommand() {
      super("Load", "Loads the current config", aep("load"));
   }

   @Override
   public void aet(LiteralArgumentBuilder literalArgumentBuilder) {
      literalArgumentBuilder.executes(LoadCommand::HB);
   }

   private static int HB(CommandContext commandContext) throws CommandSyntaxException {
      Bootstrap.clientPaths.anZ();
      ChatUtils.UB("Loaded client config!");
      return 1;
   }
}
