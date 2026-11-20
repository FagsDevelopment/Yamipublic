package pw.yami.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.File;
import pw.yami.client.Bootstrap;
import pw.yami.client.utils.chat.ChatUtils;

public class FolderCommand extends ClientCommand {
    public FolderCommand() {
        super("Folder", "Opens the client configurations folder", aep("folder"));
    }

    @Override
    public void aet(LiteralArgumentBuilder literalArgumentBuilder) {
        literalArgumentBuilder.executes(FolderCommand::execute);
    }

    private static int execute(CommandContext commandContext) throws CommandSyntaxException {
        try {
            File configsDir = Bootstrap.clientPaths.getPath().resolve("Configs").toFile();
            if (!configsDir.exists()) {
                configsDir.mkdirs();
            }
            OpenFolderCommandFileUtils.Do(configsDir);
            ChatUtils.UB("Opened config folder: §s" + configsDir.getAbsolutePath());
        } catch (Exception exception) {
            exception.printStackTrace();
            ChatUtils.UL("Failed to open config folder!");
        }
        return 1;
    }
}
