package pw.yami.client.license;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.HexFormat;

/**
 * Builds a stable hardware fingerprint from identifiers that survive a Windows
 * reinstall but change when the machine is replaced.
 *
 * <p>Only the SHA-256 hash ever leaves the machine. The raw identifiers are
 * never sent to the backend.</p>
 */
public final class HwidUtil {
   private static final String SALT = "OpenYami-HWID-v1";

   private static volatile String cachedHwid;
   private static volatile String cachedShort;

   private HwidUtil() {
   }

   public static String getHwid() {
      String value = cachedHwid;
      if (value == null) {
         synchronized (HwidUtil.class) {
            value = cachedHwid;
            if (value == null) {
               value = compute();
               cachedHwid = value;
            }
         }
      }
      return value;
   }

   public static String getShortHwid() {
      String value = cachedShort;
      if (value == null) {
         String full = getHwid();
         StringBuilder builder = new StringBuilder();
         for (int index = 0; index < 16 && index < full.length(); index++) {
            if (index > 0 && index % 4 == 0) {
               builder.append('-');
            }
            builder.append(Character.toUpperCase(full.charAt(index)));
         }
         value = builder.toString();
         cachedShort = value;
      }
      return value;
   }

   public static String compute() {
      List<String> parts = new ArrayList<>();
      String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

      if (os.contains("win")) {
         addWindowsHardware(parts);
      } else if (os.contains("mac")) {
         String ioreg = run("ioreg", "-rd1", "-c", "IOPlatformExpertDevice");
         add(parts, "PLATFORM_UUID", extractQuoted(ioreg, "IOPlatformUUID"));
         add(parts, "PLATFORM_SERIAL", extractQuoted(ioreg, "IOPlatformSerialNumber"));
      } else {
         add(parts, "PRODUCT_UUID", readFirstLine(Path.of("/sys/class/dmi/id/product_uuid")));
         add(parts, "BOARD_SERIAL", readFirstLine(Path.of("/sys/class/dmi/id/board_serial")));
         add(parts, "MACHINE_ID", readFirstLine(Path.of("/etc/machine-id")));
      }

      if (parts.isEmpty()) {
         add(parts, "COMPUTER", System.getenv("COMPUTERNAME"));
         add(parts, "HOST", System.getenv("HOSTNAME"));
         add(parts, "USER", System.getProperty("user.name"));
         add(parts, "OS", System.getProperty("os.name"));
         add(parts, "ARCH", System.getProperty("os.arch"));
         List<String> addresses = macAddresses();
         for (int index = 0; index < addresses.size(); index++) {
            add(parts, "MAC" + index, addresses.get(index));
         }
      }

      if (parts.isEmpty()) {
         parts.add("OS:" + System.getProperty("os.name", "?"));
         parts.add("ARCH:" + System.getProperty("os.arch", "?"));
         parts.add("USER:" + System.getProperty("user.name", "?"));
      }

      String joined = SALT + "|" + String.join("|", parts);
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         return HexFormat.of().formatHex(digest.digest(joined.getBytes(StandardCharsets.UTF_8)));
      } catch (Exception exception) {
         throw new IllegalStateException("SHA-256 is unavailable", exception);
      }
   }

   private static void addWindowsHardware(List<String> parts) {
      String command = "$ErrorActionPreference='SilentlyContinue';"
         + "$p=Get-CimInstance -ClassName Win32_ComputerSystemProduct;"
         + "$b=Get-CimInstance -ClassName Win32_BaseBoard;"
         + "$o=Get-CimInstance -ClassName Win32_BIOS;"
         + "$c=Get-CimInstance -ClassName Win32_Processor | Select-Object -First 1;"
         + "Write-Output ('UUID=' + $p.UUID);"
         + "Write-Output ('BOARD=' + $b.SerialNumber);"
         + "Write-Output ('BIOS=' + $o.SerialNumber);"
         + "Write-Output ('CPU=' + $c.ProcessorId)";
      String output = run(
         "powershell.exe",
         "-NoProfile",
         "-NonInteractive",
         "-ExecutionPolicy",
         "Bypass",
         "-Command",
         command
      );
      for (String line : output.split("\\R")) {
         int separator = line.indexOf('=');
         if (separator <= 0) {
            continue;
         }
         String key = line.substring(0, separator).trim().toUpperCase(Locale.ROOT);
         String value = line.substring(separator + 1);
         if (key.equals("UUID")) {
            add(parts, "SYSTEM_UUID", value);
         } else if (key.equals("BOARD")) {
            add(parts, "BASEBOARD", value);
         } else if (key.equals("BIOS")) {
            add(parts, "BIOS", value);
         } else if (key.equals("CPU")) {
            add(parts, "CPU", value);
         }
      }
   }

   private static void add(List<String> values, String label, String value) {
      String cleaned = clean(value);
      if (cleaned != null) {
         values.add(label + ":" + cleaned);
      }
   }

   private static String clean(String value) {
      if (value == null) {
         return null;
      }
      String cleaned = value.replace('\r', ' ').replace('\n', ' ').trim();
      if (cleaned.isEmpty()) {
         return null;
      }
      String upper = cleaned.toUpperCase(Locale.ROOT);
      if (upper.contains("TO BE FILLED")
         || upper.contains("DEFAULT STRING")
         || upper.equals("NONE")
         || upper.equals("UNKNOWN")
         || upper.equals("NOT APPLICABLE")
         || upper.equals("OEM")
         || upper.equals("SERIAL")
         || upper.equals("NULL")) {
         return null;
      }
      if (upper.matches("0{8,}")
         || upper.matches("F{8,}")
         || upper.matches("0{8}-0{4}-0{4}-0{4}-0{12}")
         || upper.matches("F{8}-F{4}-F{4}-F{4}-F{12}")) {
         return null;
      }
      return cleaned;
   }

   private static String run(String... command) {
      Process process = null;
      try {
         ProcessBuilder builder = new ProcessBuilder(command);
         builder.redirectErrorStream(true);
         process = builder.start();
         Process started = process;
         CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> {
            StringBuilder builder2 = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(started.getInputStream(), StandardCharsets.UTF_8))) {
               String line;
               while ((line = reader.readLine()) != null) {
                  if (!line.isBlank()) {
                     builder2.append(line.trim()).append('\n');
                  }
               }
            } catch (Exception ignored) {
            }
            return builder2.toString();
         });
         if (!process.waitFor(20L, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            return "";
         }
         return output.get(5L, TimeUnit.SECONDS).trim();
      } catch (Exception exception) {
         if (process != null) {
            process.destroyForcibly();
         }
         return "";
      }
   }

   private static String readFirstLine(Path path) {
      try {
         return Files.readString(path, StandardCharsets.UTF_8).trim();
      } catch (Exception exception) {
         return "";
      }
   }

   private static String extractQuoted(String text, String key) {
      if (text == null) {
         return "";
      }
      int keyIndex = text.indexOf(key);
      if (keyIndex < 0) {
         return "";
      }
      int quoteStart = text.indexOf('"', keyIndex);
      if (quoteStart < 0) {
         return "";
      }
      int quoteEnd = text.indexOf('"', quoteStart + 1);
      if (quoteEnd < 0) {
         return "";
      }
      return text.substring(quoteStart + 1, quoteEnd);
   }

   private static List<String> macAddresses() {
      List<String> result = new ArrayList<>();
      try {
         Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
         while (interfaces.hasMoreElements()) {
            NetworkInterface network = interfaces.nextElement();
            if (network.isLoopback() || network.isVirtual() || !network.isUp()) {
               continue;
            }
            byte[] mac = network.getHardwareAddress();
            if (mac == null || mac.length == 0) {
               continue;
            }
            StringBuilder builder = new StringBuilder();
            for (byte value : mac) {
               builder.append(String.format(Locale.ROOT, "%02X", value));
            }
            result.add(builder.toString());
         }
      } catch (Exception ignored) {
      }
      Collections.sort(result);
      return result;
   }
}
