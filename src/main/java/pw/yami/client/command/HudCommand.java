package pw.yami.client.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.command.CommandSource;
import pw.yami.client.manager.Managers;
import pw.yami.client.model.render.LeftMiddleMode;
import pw.yami.client.model.render.TopMiddleMode;
import pw.yami.client.module.hud.RenderModule;
import pw.yami.client.utils.chat.ChatUtils;

public class HudCommand extends ClientCommand {
   public HudCommand() {
      super("HUD", "Changes HUD element positions from chat", aeq("hud", "hudpos", "hpos", "pos"));
   }

   @Override
   public void aet(LiteralArgumentBuilder literalArgumentBuilder) {
      SuggestionProvider hudSuggestions = (context, suggestionsBuilder) -> {
         List<String> names = new ArrayList<>();
         for (Object obj : Managers.drawMatrixManager.Hz()) {
            if (obj instanceof RenderModule) {
               names.add(((RenderModule)obj).OO().replace(" ", ""));
            }
         }
         return CommandSource.suggestMatching(names, suggestionsBuilder);
      };

      // .hud list
      literalArgumentBuilder.then(
         aep("list").executes(HudCommand::listHuds)
      );

      // .hud reset <hud>
      literalArgumentBuilder.then(
         aep("reset").then(
            aer("hud", StringArgumentType.string()).suggests(hudSuggestions)
               .executes(HudCommand::resetHud)
         )
      );

      // .hud pos <hud> <x> <y>
      // .hud pos <hud>
      literalArgumentBuilder.then(
         aep("pos").then(
            aer("hud", StringArgumentType.string()).suggests(hudSuggestions)
               .then(
                  aer("x", DoubleArgumentType.doubleArg()).then(
                     aer("y", DoubleArgumentType.doubleArg()).executes(HudCommand::setPos)
                  )
               )
               .executes(HudCommand::getPos)
         )
      );

      // .hud <hud> <x> <y>
      // .hud <hud>
      literalArgumentBuilder.then(
         aer("hud", StringArgumentType.string()).suggests(hudSuggestions)
            .then(
               aer("x", DoubleArgumentType.doubleArg()).then(
                  aer("y", DoubleArgumentType.doubleArg()).executes(HudCommand::setPos)
               )
            )
            .executes(HudCommand::getPosDirect)
      );

      literalArgumentBuilder.executes(HudCommand::showHelp);
   }

   private static RenderModule findHudModule(String name) {
      if (name == null || name.trim().isEmpty()) {
         return null;
      }
      String clean = name.trim().replace(" ", "").toLowerCase();
      for (Object obj : Managers.drawMatrixManager.Hz()) {
         if (obj instanceof RenderModule) {
            RenderModule mod = (RenderModule)obj;
            String oo = mod.OO().replace(" ", "").toLowerCase();
            String t2 = mod.getText2().replace(" ", "").toLowerCase();
            String kn = mod.Kn().replace(" ", "").toLowerCase();
            if (oo.equals(clean) || t2.equals(clean) || kn.equals(clean) || kn.equals(clean + "-module")) {
               return mod;
            }
         }
      }
      return null;
   }

   private static int setPos(CommandContext commandContext) {
      String hudName = StringArgumentType.getString(commandContext, "hud");
      double x = DoubleArgumentType.getDouble(commandContext, "x");
      double y = DoubleArgumentType.getDouble(commandContext, "y");

      RenderModule hud = findHudModule(hudName);
      if (hud == null) {
         ChatUtils.UL("HUD modulu bulunamadi: " + hudName + "! (.hud list yazarak listeyi gorebilirsiniz)");
         return 0;
      }

      hud.adf((float)x, (float)y, LeftMiddleMode.LEFT, TopMiddleMode.TOP);
      ChatUtils.UB("§2[+] §f" + hud.OO() + " pozisyonu ayarlandi: §eX: " + (int)x + "§f, §eY: " + (int)y);
      return 1;
   }

   private static int getPos(CommandContext commandContext) {
      String hudName = StringArgumentType.getString(commandContext, "hud");
      return showHudInfo(hudName);
   }

   private static int getPosDirect(CommandContext commandContext) {
      String hudName = StringArgumentType.getString(commandContext, "hud");
      return showHudInfo(hudName);
   }

   private static int showHudInfo(String hudName) {
      RenderModule hud = findHudModule(hudName);
      if (hud == null) {
         ChatUtils.UL("HUD modulu bulunamadi: " + hudName + "! (.hud list yazarak listeyi gorebilirsiniz)");
         return 0;
      }

      float curX = hud.add();
      float curY = hud.ade();
      float rawX = (Float)hud.numberSetting1.atx();
      float rawY = (Float)hud.numberSetting2.atx();
      boolean enabled = hud.awg();

      ChatUtils.UB("§e--- " + hud.OO() + " ---");
      ChatUtils.UB("§7Durum: " + (enabled ? "§aAcik" : "§cKapali"));
      ChatUtils.UB("§7Konum: §eX: " + (int)curX + "§f, §eY: " + (int)curY + " §7(Ayar: " + (int)rawX + ", " + (int)rawY + ")");
      ChatUtils.UB("§7Degistirmek icin: §s.hud pos " + hud.OO().replace(" ", "") + " <x> <y>");
      return 1;
   }

   private static int resetHud(CommandContext commandContext) {
      String hudName = StringArgumentType.getString(commandContext, "hud");
      RenderModule hud = findHudModule(hudName);
      if (hud == null) {
         ChatUtils.UL("HUD modulu bulunamadi: " + hudName);
         return 0;
      }

      hud.adf(0.0F, 0.0F, LeftMiddleMode.LEFT, TopMiddleMode.TOP);
      ChatUtils.UB("§2[+] §f" + hud.OO() + " pozisyonu sifirlandi (§eX: 0, Y: 0§f)");
      return 1;
   }

   private static int listHuds(CommandContext commandContext) {
      List list = Managers.drawMatrixManager.Hz();
      if (list.isEmpty()) {
         ChatUtils.UL("Hicbir HUD modulu bulunamadi.");
         return 0;
      }

      ChatUtils.UB("§e--- HUD Modulleri (" + list.size() + ") ---");
      for (Object obj : list) {
         if (obj instanceof RenderModule) {
            RenderModule mod = (RenderModule)obj;
            float x = mod.add();
            float y = mod.ade();
            String status = mod.awg() ? "§a[ON]" : "§c[OFF]";
            ChatUtils.UB("§7- §s" + mod.OO() + " " + status + " §7-> §eX: " + (int)x + "§7, §eY: " + (int)y);
         }
      }
      ChatUtils.UB("§7Pozisyon ayarlamak icin: §s.hud pos <isim> <x> <y>");
      return 1;
   }

   private static int showHelp(CommandContext commandContext) {
      ChatUtils.UB("§e--- HUD Pozisyon Komutlari ---");
      ChatUtils.UB("§s.hud pos <hud> <x> <y> §7- HUD koordinatlarini ayarlar");
      ChatUtils.UB("§s.hud <hud> <x> <y>     §7- Kisa kullanim");
      ChatUtils.UB("§s.hud <hud>             §7- Mevcut koordinatlari gosterir");
      ChatUtils.UB("§s.hud list              §7- Tum HUD modullerini ve konumlarini listeler");
      ChatUtils.UB("§s.hud reset <hud>       §7- Konumu (0, 0)'a sifirlar");
      ChatUtils.UB("§7Alternatifler: §s.hudpos§7, §s.pos§7, §s.hpos");
      return 1;
   }
}
