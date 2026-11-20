package pw.yami.client.license;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import pw.yami.client.Bootstrap;

/**
 * Installs a staged update into the mods folder.
 *
 * <p>Windows keeps the running jar locked, so if the direct copy fails a small
 * detached PowerShell helper waits for the game process to exit and then
 * replaces the jar. The pending state survives crashes and is retried from the
 * {@code preLaunch} entrypoint on the next start.</p>
 */
public final class UpdateApplier implements PreLaunchEntrypoint {
   private static final String SCRIPT = String.join(
      "\r\n",
      "param(",
      "  [Parameter(Mandatory=$true)][int]$ParentPid,",
      "  [Parameter(Mandatory=$true)][string]$Source,",
      "  [Parameter(Mandatory=$true)][string]$Target,",
      "  [Parameter(Mandatory=$true)][string]$Pending",
      ")",
      "try { Wait-Process -Id $ParentPid -ErrorAction SilentlyContinue } catch { }",
      "Start-Sleep -Milliseconds 700",
      "for ($attempt = 0; $attempt -lt 60; $attempt++) {",
      "  try {",
      "    Copy-Item -LiteralPath $Source -Destination $Target -Force -ErrorAction Stop",
      "    Remove-Item -LiteralPath $Pending -Force -ErrorAction SilentlyContinue",
      "    Remove-Item -LiteralPath $Source -Force -ErrorAction SilentlyContinue",
      "    exit 0",
      "  } catch {",
      "    Start-Sleep -Milliseconds 500",
      "  }",
      "}",
      "exit 1",
      ""
   );

   @Override
   public void onPreLaunch() {
      AntiDebug.init();
      applyPending();
   }

   public static void stage(Path staged, String targetName, String version) {
      try {
         Path updateDir = updateDir();
         Files.createDirectories(updateDir);
         JsonObject json = new JsonObject();
         json.addProperty("staged", staged.toAbsolutePath().toString());
         json.addProperty("target", targetName == null ? "" : targetName);
         json.addProperty("version", version == null ? "" : version);
         Path pending = updateDir.resolve("pending.json");
         Path temporary = updateDir.resolve("pending.json.tmp");
         Files.writeString(temporary, json.toString(), StandardCharsets.UTF_8);
         try {
            Files.move(temporary, pending, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (Exception exception) {
            Files.move(temporary, pending, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (Exception exception) {
         Bootstrap.Ob("Guncelleme durumu yazilamadi: " + exception.getMessage());
      }
   }

   public static void applyPending() {
      Path directory;
      try {
         directory = updateDir();
      } catch (Exception exception) {
         return;
      }
      Path pending = directory.resolve("pending.json");
      if (!Files.isRegularFile(pending)) {
         return;
      }
      try {
         JsonObject json = JsonParser.parseString(Files.readString(pending, StandardCharsets.UTF_8)).getAsJsonObject();
         String stagedValue = json.has("staged") ? json.get("staged").getAsString() : "";
         String targetName = json.has("target") ? json.get("target").getAsString() : "";
         if (stagedValue.isBlank()) {
            Files.deleteIfExists(pending);
            return;
         }
         Path staged = Path.of(stagedValue);
         if (!Files.isRegularFile(staged)) {
            Files.deleteIfExists(pending);
            return;
         }
         if (targetName.isBlank()) {
            targetName = currentJarName();
         }
         Path target = modsDir().resolve(targetName);
         Files.createDirectories(target.getParent());
         try {
            Files.copy(staged, target, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(staged);
            Files.deleteIfExists(pending);
            Bootstrap.NZ("Bekleyen guncelleme kuruldu: " + targetName);
         } catch (Exception exception) {
            Bootstrap.Ob("Bekleyen guncelleme kilitli, cikista kurulacak: " + exception.getMessage());
            scheduleInstallAfterExit(staged, targetName);
         }
      } catch (Exception exception) {
         Bootstrap.Ob("Bekleyen guncelleme okunamadi: " + exception.getMessage());
         try {
            Files.deleteIfExists(pending);
         } catch (Exception ignored) {
         }
      }
   }

   public static void scheduleInstallAfterExit(Path staged, String targetName) {
      if (!isWindows()) {
         return;
      }
      try {
         Path updateDir = updateDir();
         Files.createDirectories(updateDir);
         Path script = updateDir.resolve("apply-update.ps1");
         Path log = updateDir.resolve("apply-update.log");
         Files.writeString(script, SCRIPT, StandardCharsets.UTF_8);
         Path target = modsDir().resolve(targetName);
         Files.createDirectories(target.getParent());
         ProcessBuilder builder = new ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-NonInteractive",
            "-WindowStyle",
            "Hidden",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            script.toAbsolutePath().toString(),
            "-ParentPid",
            String.valueOf(ProcessHandle.current().pid()),
            "-Source",
            staged.toAbsolutePath().toString(),
            "-Target",
            target.toAbsolutePath().toString(),
            "-Pending",
            updateDir.resolve("pending.json").toAbsolutePath().toString()
         );
         builder.redirectErrorStream(true);
         builder.redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()));
         builder.start();
         Bootstrap.NZ("Guncelleme cikista kurulmak uzere planlandi.");
      } catch (Exception exception) {
         Bootstrap.Ob("Guncelleme planlayicisi baslatilamadi: " + exception.getMessage());
      }
   }

   public static Path currentJarPath() {
      try {
         Path location = Path.of(UpdateApplier.class.getProtectionDomain().getCodeSource().getLocation().toURI());
         if (Files.isRegularFile(location) && location.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return location;
         }
      } catch (Exception ignored) {
      }
      return null;
   }

   public static String currentJarName() {
      Path location = currentJarPath();
      return location != null ? location.getFileName().toString() : "Yami-" + Bootstrap.b + ".jar";
   }

   private static Path updateDir() {
      return FabricLoader.getInstance().getGameDir().resolve("Yami").resolve("update");
   }

   private static Path modsDir() {
      return FabricLoader.getInstance().getGameDir().resolve("mods");
   }

   private static boolean isWindows() {
      return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
   }
}
