package pw.yami.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.File;
import java.nio.file.Path;
import pw.yami.client.Bootstrap;
import pw.yami.client.utils.chat.ChatUtils;

public class ConfigCommand extends ClientCommand {
    public static boolean ah;

    public ConfigCommand() {
        super("Config", "Manages client configurations", aep("config"));
    }

    @Override
    public void aet(LiteralArgumentBuilder literalArgumentBuilder) {
        // .config list
        literalArgumentBuilder.then(
            aep("list").executes(ConfigCommand::listConfigs)
        );

        // .config delete <name>
        literalArgumentBuilder.then(
            aep("delete").then(
                aer("config_name", StringArgumentType.string()).executes(ConfigCommand::deleteConfig)
            )
        );

        // .config save/load <name> [visual/module/all]
        RequiredArgumentBuilder temp = aer("save/load", StringArgumentType.string()).suggests(aes("save", "load"));
        RequiredArgumentBuilder requiredArgumentBuilder = aer("config_name", StringArgumentType.string());
        temp = temp;
        RequiredArgumentBuilder temp2 = (RequiredArgumentBuilder)requiredArgumentBuilder.then(
            aer("visual/module/all", StringArgumentType.string()).suggests(aes("visual", "module", "all")).executes(ConfigCommand::aoG)
        );
        Command command2 = ConfigCommand::aoF;
        requiredArgumentBuilder = temp2;
        RequiredArgumentBuilder requiredArgumentBuilder2 = temp;
        ArgumentBuilder argumentBuilder = requiredArgumentBuilder.executes(command2);
        temp = (RequiredArgumentBuilder)requiredArgumentBuilder2.then(argumentBuilder);
        command2 = ConfigCommand::aoE;
        ArgumentBuilder argumentBuilder2 = temp.executes(command2);
        LiteralArgumentBuilder literalArgumentBuilder2 = (LiteralArgumentBuilder)literalArgumentBuilder.then(argumentBuilder2);
        command2 = this::aoD;
        literalArgumentBuilder2.executes(command2);
    }

    private static int listConfigs(CommandContext commandContext) throws CommandSyntaxException {
        try {
            Path configDir = Bootstrap.clientPaths.getPath().resolve("Configs");
            File dir = configDir.toFile();
            if (!dir.exists() || !dir.isDirectory()) {
                ChatUtils.UL("Config directory not found!");
                return 1;
            }
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files == null || files.length == 0) {
                ChatUtils.UB("No saved configs found.");
                return 1;
            }
            ChatUtils.UB("\u00a7l--- Saved Configs ---");
            for (File f : files) {
                String name = f.getName().replace(".json", "");
                long size = f.length();
                String sizeStr = size > 1024 ? (size / 1024) + " KB" : size + " B";
                ChatUtils.UE("  \u00a7a" + name + " \u00a77(" + sizeStr + ")");
            }
            ChatUtils.UE("\u00a77Total: \u00a7f" + files.length + " config(s)");
            ChatUtils.UE("\u00a77Usage: \u00a7f.config load <name> \u00a77| \u00a7f.config save <name>");
        } catch (Exception e) {
            ChatUtils.UL("Error listing configs: " + e.getMessage());
        }
        return 1;
    }

    private static int deleteConfig(CommandContext commandContext) throws CommandSyntaxException {
        String name = StringArgumentType.getString(commandContext, "config_name");
        try {
            Path configFile = Bootstrap.clientPaths.getPath().resolve("Configs").resolve(name + ".json");
            File f = configFile.toFile();
            if (!f.exists()) {
                ChatUtils.UL("Config not found: " + name);
                return 1;
            }
            if (f.delete()) {
                ChatUtils.UB("Deleted config: \u00a7c" + name);
            } else {
                ChatUtils.UL("Failed to delete config: " + name);
            }
        } catch (Exception e) {
            ChatUtils.UL("Error deleting config: " + e.getMessage());
        }
        return 1;
    }

    private int aoD(CommandContext commandContext) throws CommandSyntaxException {
        ChatUtils.UL("Invalid usage! Usage: " + this.aex());
        ChatUtils.UE("\u00a77Commands: \u00a7f.config list \u00a77| \u00a7f.config save <name> \u00a77| \u00a7f.config load <name> \u00a77| \u00a7f.config delete <name>");
        return 1;
    }

    private static int aoE(CommandContext commandContext) throws CommandSyntaxException {
        ChatUtils.UL("Must provide a config name!");
        return 1;
    }

    private static int aoF(CommandContext commandContext) throws CommandSyntaxException {
        String str = StringArgumentType.getString(commandContext, "save/load");
        String str2 = StringArgumentType.getString(commandContext, "config_name");
        if (str.equalsIgnoreCase("save")) {
            Bootstrap.clientPaths.aoa(str2, "all");
            ChatUtils.UB("Saved config: \u00a7s" + str2);
        } else if (str.equalsIgnoreCase("load") && Bootstrap.clientPaths.aob(str2, "all")) {
            ChatUtils.UB("Loaded config: \u00a7s" + str2);
        }
        return 1;
    }

    private static int aoG(CommandContext commandContext) throws CommandSyntaxException {
        String str = StringArgumentType.getString(commandContext, "save/load");
        String str2 = StringArgumentType.getString(commandContext, "config_name");
        String str3 = StringArgumentType.getString(commandContext, "visual/module/all");
        if (str.equalsIgnoreCase("save")) {
            Bootstrap.clientPaths.aoa(str2, str3);
            ChatUtils.UB("Saved config: \u00a7s" + str2);
        } else if (str.equalsIgnoreCase("load") && Bootstrap.clientPaths.aob(str2, str3)) {
            ChatUtils.UB("Loaded config: \u00a7s" + str2);
        }
        return 1;
    }
}
