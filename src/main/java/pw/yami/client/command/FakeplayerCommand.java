package pw.yami.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import pw.yami.client.manager.Managers;
import pw.yami.client.module.misc.FakePlayer;
import pw.yami.client.utils.chat.ChatUtils;

public class FakeplayerCommand extends ClientCommand {
   public FakeplayerCommand() {
      super("Fakeplayer", "Spawns, configures, or removes a client-side fake player", aeq("fakeplayer", "fp", "dummy"));
   }

   @Override
   public void aet(LiteralArgumentBuilder literalArgumentBuilder) {
      literalArgumentBuilder
         .executes(FakeplayerCommand::toggleDefault)
         .then(aep("record").executes(FakeplayerCommand::toggleRecord))
         .then(aep("play").executes(FakeplayerCommand::togglePlay))
         .then(aep("kill").executes(FakeplayerCommand::killFakePlayer))
         .then(aep("remove").executes(FakeplayerCommand::removeFakePlayer))
         .then(aep("off").executes(FakeplayerCommand::removeFakePlayer))
         .then(aer("name", StringArgumentType.string()).executes(FakeplayerCommand::spawnWithName));
   }

   private static FakePlayer getModule() {
      if (FakePlayer.INSTANCE != null) {
         return FakePlayer.INSTANCE;
      }
      return (FakePlayer)Managers.moduleManager.XL("FakePlayer");
   }

   private static int toggleDefault(CommandContext commandContext) throws CommandSyntaxException {
      FakePlayer module = getModule();
      if (module == null) {
         ChatUtils.UL("FakePlayer module not found!");
         return 0;
      }
      if (module.getFakePlayer() != null && module.getFakePlayer().isAlive()) {
         module.removeFakePlayer();
         ChatUtils.UB("§cRemoved fake player.");
      } else {
         String name = (String)module.name.atx();
         module.spawnFakePlayer(name);
         String displayName = "FakePlayer";
         if (name != null) {
            displayName = name;
         }
         ChatUtils.UB("§aSpawned fake player §s" + displayName + "§f.");
      }
      return 1;
   }

   private static int spawnWithName(CommandContext commandContext) throws CommandSyntaxException {
      FakePlayer module = getModule();
      if (module == null) {
         ChatUtils.UL("FakePlayer module not found!");
         return 0;
      }
      String name = StringArgumentType.getString(commandContext, "name");
      if (name.equalsIgnoreCase("record")) {
         return toggleRecord(commandContext);
      }
      if (name.equalsIgnoreCase("play")) {
         return togglePlay(commandContext);
      }
      if (name.equalsIgnoreCase("kill")) {
         return killFakePlayer(commandContext);
      }
      if (name.equalsIgnoreCase("remove") || name.equalsIgnoreCase("off") || name.equalsIgnoreCase("clear")) {
         return removeFakePlayer(commandContext);
      }
      module.name.aty(name);
      module.spawnFakePlayer(name);
      ChatUtils.UB("§aSpawned fake player §s" + name + "§f.");
      return 1;
   }

   private static int toggleRecord(CommandContext commandContext) throws CommandSyntaxException {
      FakePlayer module = getModule();
      if (module == null) return 0;
      boolean recording = module.toggleRecord();
      if (recording) {
         ChatUtils.UB("§aStarted recording fake player movement.");
      } else {
         ChatUtils.UB("§cStopped recording fake player movement.");
      }
      return 1;
   }

   private static int togglePlay(CommandContext commandContext) throws CommandSyntaxException {
      FakePlayer module = getModule();
      if (module == null) return 0;
      boolean playing = module.togglePlay();
      if (playing) {
         ChatUtils.UB("§aStarted playback on fake player.");
      } else {
         ChatUtils.UB("§cStopped playback on fake player.");
      }
      return 1;
   }

   private static int killFakePlayer(CommandContext commandContext) throws CommandSyntaxException {
      FakePlayer module = getModule();
      if (module == null || module.getFakePlayer() == null) {
         ChatUtils.UL("No active fake player to kill!");
         return 0;
      }
      module.killFakePlayer();
      ChatUtils.UB("§cKilled fake player.");
      return 1;
   }

   private static int removeFakePlayer(CommandContext commandContext) throws CommandSyntaxException {
      FakePlayer module = getModule();
      if (module == null) return 0;
      module.removeFakePlayer();
      ChatUtils.UB("§cRemoved fake player.");
      return 1;
   }
}
