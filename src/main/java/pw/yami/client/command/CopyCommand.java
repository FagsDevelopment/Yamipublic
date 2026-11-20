package pw.yami.client.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import pw.yami.client.Mc;
import pw.yami.client.manager.Managers;
import pw.yami.client.module.ModuleBase;
import pw.yami.client.utils.chat.ChatUtils;

public class CopyCommand extends ClientCommand {
    public CopyCommand() {
        super("Copy", "Copies a module's settings to clipboard", aep("copy"));
    }

    @Override
    public void aet(LiteralArgumentBuilder literalArgumentBuilder) {
        literalArgumentBuilder.then(
            aer("module", StringArgumentType.string()).executes(CopyCommand::execute)
        );
    }

    private static int execute(CommandContext commandContext) throws CommandSyntaxException {
        String modName = StringArgumentType.getString(commandContext, "module");
        ModuleBase target = null;
        for (Object obj : Managers.moduleManager.XM()) {
            ModuleBase mod = (ModuleBase)obj;
            if (mod.getText2().equalsIgnoreCase(modName) || mod.Kn().equalsIgnoreCase(modName) || mod.OO().equalsIgnoreCase(modName)) {
                target = mod;
                break;
            }
        }

        if (target == null) {
            ChatUtils.UL("Module not found: " + modName);
            return 1;
        }

        try {
            JsonObject json = target.LT(25681, 44222, '\u218e');
            String exported = "YAMI_CFG:" + json.toString();
            Mc.client.keyboard.setClipboard(exported);
            ChatUtils.UB("Copied settings for \u00a7s" + target.getText2() + "\u00a7f to clipboard! Share it with friends.");
        } catch (Exception ex) {
            ChatUtils.UL("Failed to copy settings: " + ex.getMessage());
        }

        return 1;
    }
}
