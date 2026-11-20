package pw.yami.client.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import pw.yami.client.Mc;
import pw.yami.client.manager.Managers;
import pw.yami.client.module.ModuleBase;
import pw.yami.client.utils.chat.ChatUtils;

public class PasteCommand extends ClientCommand {
    public PasteCommand() {
        super("Paste", "Pastes module settings from clipboard", aep("paste"));
    }

    @Override
    public void aet(LiteralArgumentBuilder literalArgumentBuilder) {
        literalArgumentBuilder.executes(PasteCommand::executeAuto)
            .then(aer("module", StringArgumentType.string()).executes(PasteCommand::executeTarget));
    }

    private static int executeAuto(CommandContext commandContext) throws CommandSyntaxException {
        return applyFromClipboard(null);
    }

    private static int executeTarget(CommandContext commandContext) throws CommandSyntaxException {
        String modName = StringArgumentType.getString(commandContext, "module");
        return applyFromClipboard(modName);
    }

    private static int applyFromClipboard(String overrideModName) {
        try {
            String clip = Mc.client.keyboard.getClipboard();
            if (clip == null || clip.trim().isEmpty()) {
                ChatUtils.UL("Clipboard is empty!");
                return 1;
            }

            String jsonStr = clip.trim();
            if (jsonStr.startsWith("YAMI_CFG:")) {
                jsonStr = jsonStr.substring("YAMI_CFG:".length()).trim();
            }

            JsonElement elem = JsonParser.parseString(jsonStr);
            if (!elem.isJsonObject()) {
                ChatUtils.UL("Clipboard does not contain valid module settings!");
                return 1;
            }

            JsonObject json = elem.getAsJsonObject();
            String targetName = overrideModName;
            if (targetName == null && json.has("name")) {
                targetName = json.get("name").getAsString();
            }

            if (targetName == null) {
                ChatUtils.UL("Could not determine module name. Usage: .paste <module>");
                return 1;
            }

            ModuleBase target = null;
            for (Object obj : Managers.moduleManager.XM()) {
                ModuleBase mod = (ModuleBase)obj;
                if (mod.getText2().equalsIgnoreCase(targetName) || mod.Kn().equalsIgnoreCase(targetName) || mod.OO().equalsIgnoreCase(targetName)) {
                    target = mod;
                    break;
                }
            }

            if (target == null) {
                ChatUtils.UL("Module not found: " + targetName);
                return 1;
            }

            target.OM(json);
            ChatUtils.UB("Applied settings to \u00a7s" + target.getText2() + "\u00a7f!");
        } catch (Exception ex) {
            ChatUtils.UL("Failed to paste settings: " + ex.getMessage());
        }
        return 1;
    }
}
