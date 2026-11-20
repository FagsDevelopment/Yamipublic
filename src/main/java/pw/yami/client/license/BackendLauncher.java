package pw.yami.client.license;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Auto-launches the Python license backend (server + Discord bot) when the
 * game starts, if a valid {@code hostBackendDir} is configured in license.json.
 *
 * <p>On shutdown the child processes are destroyed so the host doesn't leak.</p>
 */
public final class BackendLauncher {
   private static final Logger LOG = LogManager.getLogger("Yami-Backend");
   private static volatile Process serverProcess;
   private static volatile Process botProcess;
   private static volatile boolean started = false;

   private BackendLauncher() {
   }

   /**
    * Starts the backend if {@code hostBackendDir} is set and the directory
    * actually exists. Silently does nothing on client machines (no dir).
    */
   public static void tryStart(LicenseConfig config) {
      if (started) {
         return;
      }
      String dir = config.hostBackendDir;
      if (dir == null || dir.isBlank()) {
         return;
      }
      Path baseDir = Path.of(dir);
      if (!Files.isDirectory(baseDir)) {
         LOG.warn("[Yami] hostBackendDir not found: {}", dir);
         return;
      }
      Path venvPython = baseDir.resolve(".venv").resolve("Scripts").resolve("python.exe");
      if (!Files.isRegularFile(venvPython)) {
         // Try Linux/Mac path
         venvPython = baseDir.resolve(".venv").resolve("bin").resolve("python");
         if (!Files.isRegularFile(venvPython)) {
            LOG.warn("[Yami] Python venv not found in {}", dir);
            return;
         }
      }
      Path envFile = baseDir.resolve(".env");
      if (!Files.isRegularFile(envFile)) {
         LOG.warn("[Yami] .env not found in {}", dir);
         return;
      }

      started = true;
      final Path python = venvPython;
      CompletableFuture.runAsync(() -> launch(python, baseDir));
   }

   private static void launch(Path python, Path baseDir) {
      try {
         // Load .env variables
         java.util.Map<String, String> envVars = loadDotEnv(baseDir.resolve(".env"));

         // Check if server is running, if not start it
         boolean serverUp = isServerRunning(envVars.getOrDefault("YAMI_PORT", "8000"));
         if (!serverUp) {
            String port = envVars.getOrDefault("YAMI_PORT", "8000");
            ProcessBuilder serverBuilder = new ProcessBuilder(
               python.toString(), "-m", "uvicorn", "server.main:app",
               "--host", "0.0.0.0", "--port", port
            );
            serverBuilder.directory(baseDir.toFile());
            serverBuilder.environment().putAll(envVars);
            serverBuilder.redirectErrorStream(true);
            serverProcess = serverBuilder.start();
            consumeOutput(serverProcess, "Backend");
            LOG.info("[Yami] Backend server started (PID {})", serverProcess.pid());
            Thread.sleep(2000);
         } else {
            LOG.info("[Yami] Backend server is already running.");
         }

         // Register shutdown hook
         Runtime.getRuntime().addShutdownHook(new Thread(BackendLauncher::stopAll, "Yami-Backend-Shutdown"));

      } catch (Exception exception) {
         LOG.error("[Yami] Failed to start backend: {}", exception.getMessage());
      }
   }

   private static boolean isServerRunning(String port) {
      try {
         java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
            new java.net.URL("http://127.0.0.1:" + port + "/health").openConnection();
         conn.setConnectTimeout(2000);
         conn.setReadTimeout(2000);
         conn.setRequestMethod("GET");
         int code = conn.getResponseCode();
         conn.disconnect();
         return code >= 200 && code < 500;
      } catch (Exception exception) {
         return false;
      }
   }



   private static java.util.Map<String, String> loadDotEnv(Path envPath) {
      java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
      try {
         for (String line : Files.readAllLines(envPath)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
               continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
               continue;
            }
            String key = trimmed.substring(0, eq).trim();
            String value = trimmed.substring(eq + 1).trim();
            // Remove surrounding quotes if present
            if (value.length() >= 2 && (
               (value.startsWith("\"") && value.endsWith("\"")) ||
               (value.startsWith("'") && value.endsWith("'"))
            )) {
               value = value.substring(1, value.length() - 1);
            }
            result.put(key, value);
         }
      } catch (Exception exception) {
         LOG.warn("[Yami] Failed to load .env: {}", exception.getMessage());
      }
      return result;
   }

   private static void consumeOutput(Process process, String label) {
      Thread thread = new Thread(() -> {
         try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
               LOG.info("[Yami-{}] {}", label, line);
            }
         } catch (Exception ignored) {
         }
      }, "Yami-" + label + "-Output");
      thread.setDaemon(true);
      thread.start();
   }

   /** Kills both backend processes if they are alive. */
   public static void stopAll() {
      destroy(serverProcess, "Backend");
      destroy(botProcess, "Bot");
      started = false;
   }

   private static void destroy(Process process, String label) {
      if (process != null && process.isAlive()) {
         LOG.info("[Yami] Stopping {} (PID {})", label, process.pid());
         process.destroy();
         try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
               process.destroyForcibly();
            }
         } catch (InterruptedException ignored) {
            process.destroyForcibly();
         }
      }
   }
}
