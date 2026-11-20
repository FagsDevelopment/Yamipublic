package pw.yami.client.utils.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DiscordIPC {
   private static RandomAccessFile pipe;
   private static boolean connected = false;
   private static long startTime = System.currentTimeMillis();

   public static synchronized boolean connect(String clientId) {
      if (connected && pipe != null) {
         return true;
      }

      for (int i = 0; i < 10; i++) {
         try {
            File file = new File("\\\\.\\pipe\\discord-ipc-" + i);
            if (!file.exists()) {
               continue;
            }
            pipe = new RandomAccessFile(file, "rw");

            // Send HANDSHAKE (opcode 0)
            JsonObject handshake = new JsonObject();
            handshake.addProperty("v", 1);
            handshake.addProperty("client_id", clientId);

            writePacket(0, handshake.toString());

            // Read response
            readPacket();
            connected = true;
            return true;
         } catch (Exception ignored) {
            close();
         }
      }
      return false;
   }

   public static synchronized void updatePresence(String clientId, String details, String state, String largeText) {
      try {
         if (!connected || pipe == null) {
            if (!connect(clientId)) {
               return;
            }
         }

         long pid = ProcessHandle.current().pid();

         JsonObject activity = new JsonObject();
         if (details != null && !details.isEmpty()) {
            activity.addProperty("details", details);
         }
         if (state != null && !state.isEmpty()) {
            activity.addProperty("state", state);
         }

         JsonObject timestamps = new JsonObject();
         timestamps.addProperty("start", startTime / 1000L);
         activity.add("timestamps", timestamps);

         JsonObject assets = new JsonObject();
         assets.addProperty("large_image", "yami");
         assets.addProperty("large_text", largeText != null ? largeText : "Yami Client 2.0");
         activity.add("assets", assets);

         JsonObject args = new JsonObject();
         args.addProperty("pid", pid);
         args.add("activity", activity);

         JsonObject frame = new JsonObject();
         frame.addProperty("cmd", "SET_ACTIVITY");
         frame.add("args", args);
         frame.addProperty("nonce", UUID.randomUUID().toString());

         writePacket(1, frame.toString());
      } catch (Exception e) {
         close();
      }
   }

   public static synchronized void clearPresence() {
      try {
         if (connected && pipe != null) {
            long pid = ProcessHandle.current().pid();
            JsonObject args = new JsonObject();
            args.addProperty("pid", pid);
            args.add("activity", null);

            JsonObject frame = new JsonObject();
            frame.addProperty("cmd", "SET_ACTIVITY");
            frame.add("args", args);
            frame.addProperty("nonce", UUID.randomUUID().toString());

            writePacket(1, frame.toString());
         }
      } catch (Exception ignored) {
      } finally {
         close();
      }
   }

   public static synchronized void close() {
      connected = false;
      if (pipe != null) {
         try {
            pipe.close();
         } catch (Exception ignored) {
         }
         pipe = null;
      }
   }

   private static void writePacket(int opcode, String json) throws Exception {
      byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
      byte[] header = new byte[8];
      // Opcode Little Endian
      header[0] = (byte)(opcode & 0xFF);
      header[1] = (byte)((opcode >> 8) & 0xFF);
      header[2] = (byte)((opcode >> 16) & 0xFF);
      header[3] = (byte)((opcode >> 24) & 0xFF);
      // Length Little Endian
      int len = bytes.length;
      header[4] = (byte)(len & 0xFF);
      header[5] = (byte)((len >> 8) & 0xFF);
      header[6] = (byte)((len >> 16) & 0xFF);
      header[7] = (byte)((len >> 24) & 0xFF);

      pipe.write(header);
      pipe.write(bytes);
   }

   private static String readPacket() throws Exception {
      byte[] header = new byte[8];
      pipe.readFully(header);
      int len = (header[4] & 0xFF) | ((header[5] & 0xFF) << 8) | ((header[6] & 0xFF) << 16) | ((header[7] & 0xFF) << 24);
      byte[] data = new byte[len];
      pipe.readFully(data);
      return new String(data, StandardCharsets.UTF_8);
   }

   public static void resetStartTime() {
      startTime = System.currentTimeMillis();
   }
}
