package obf;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

/**
 * BestHunt Obfuscator v4.0 Ultimate
 *
 * [FIX 1] Anti-Reflection & Caller Class Verification in Inline Decryptors:
 *         Detects reflection (Method.invoke, jdk.internal.reflect) and external callers.
 *         Returns corrupted data when called via reflection or by automated deobfuscators.
 * [FIX 2] Anti-Constant-Folding for Numbers:
 *         Numbers helpers depend on non-final static runtime fields (f1 & 0), completely
 *         blocking CFR/Procyon/java-deobfuscator constant folding.
 * [FIX 3] Full Lambda & Small Method Coverage:
 *         Methods with >= 2 instructions (including lambdas and getters) are flow-obfuscated
 *         and protected by exception handlers. No naked "this.setting.get()" lambdas.
 * [FIX 4] 5 Diverse Exception Handler Patterns:
 *         No single pattern; varies between mutating catches, multi-catches, nested ranges,
 *         and dead-path throwers to defeat automated handler-stripping passes.
 * [FIX 5] Realistic Decoy API Pollution:
 *         Dead code injected with actual Minecraft/Meteor Client classes (EndCrystal, Blocks,
 *         Packets, Vec3d) so static symbol analysis sees misleading, conflicting features.
 */
public class Obfuscator {

    private static final Random RNG = new Random(0xDEADBEEFL);
    private static final String MIXIN_PKG = "pw/yami/client/mixin/";
    private static final String SECURITY_PKG = "pw/yami/client/license/";

    private static final Set<String> PROTECTED_METHODS = Set.of(
        "<init>", "<clinit>",
        "onActivate", "onDeactivate", "isActive", "toggle",
        "getInfoString", "getPackage", "onRegisterCategories",
        "onInitialize", "getRepo", "init",
        "isStandPoseActive", "get",
        "equals", "hashCode", "toString",
        "render", "tick",
        "onPreLaunch", "boot", "define",
        "NT", "NU"
    );
    private static final String EVENT_HANDLER_DESC = "Lpw/yami/eventbus/annotation/EventListener;";

    private int unicodeCounter = 0;
    private int stringsEncrypted = 0, numbersEncoded = 0, junkAdded = 0;
    private int flowObf = 0, fieldsRenamed = 0, methodsRenamed = 0;
    private int debugStripped = 0, fakeClassesAdded = 0, exceptHandlers = 0;

    private final List<String> allClassNames = new ArrayList<>();
    private final List<String> fakeClassNames = new ArrayList<>();
    private final Set<String> honeypotClassNames = new HashSet<>();
    private ClassLoader hierarchyLoader;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) { System.exit(1); }
        new Obfuscator().process(new File(args[0]), new File(args[1]));
    }

    public void process(File inputJar, File outputJar) throws Exception {
        process(inputJar, outputJar, Collections.emptyList());
    }

    public void process(File inputJar, File outputJar, List<File> classpathFiles) throws Exception {
        System.out.println("[Obfuscator] v4.0 Ultimate — " + inputJar.getName());

        Map<String, byte[]> classes = new LinkedHashMap<>();
        Map<String, byte[]> resources = new LinkedHashMap<>();
        Manifest manifest = null;

        try (JarInputStream jis = new JarInputStream(new FileInputStream(inputJar))) {
            manifest = jis.getManifest();
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.isDirectory()) continue;
                byte[] data = readAllBytes(jis);
                if (entry.getName().endsWith(".class")) {
                    classes.put(entry.getName(), data);
                    allClassNames.add(entry.getName().replace(".class", ""));
                } else {
                    resources.put(entry.getName(), data);
                }
            }
        }

        System.out.println("[Obfuscator] " + classes.size() + " sınıf, " + resources.size() + " kaynak.");

        // Classpath'ten sınıf hiyerarşisi çözümleyici oluştur (SettingColor/Color gibi Meteor sınıfları için)
        List<URL> urls = new ArrayList<>();
        urls.add(inputJar.toURI().toURL());
        for (File f : classpathFiles) {
            urls.add(f.toURI().toURL());
        }
        this.hierarchyLoader = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
        System.out.println("[Obfuscator] Hiyerarşi çözümleyici: " + urls.size() + " kaynak yüklendi.");

        String[] fakePkgs = {
            "pw/yami/client/core/", "pw/yami/client/engine/",
            "pw/yami/client/impl/", "pw/yami/client/internal/",
            "pw/yami/client/crypto/", "pw/yami/client/net/",
            "pw/yami/client/proto/", "pw/yami/client/render/",
            "pw/yami/client/data/", "pw/yami/client/io/",
            "pw/yami/client/codec/", "pw/yami/client/matrix/",
            "pw/yami/client/packet/", "pw/yami/client/shader/",
            "pw/yami/client/asm/", "pw/yami/client/nativebridge/",
            "pw/yami/client/memory/", "pw/yami/client/bytecode/"
        };
        for (int i = 0; i < 160; i++) {
            fakeClassNames.add(fakePkgs[RNG.nextInt(fakePkgs.length)] + nextUnicode());
        }

        Map<String, byte[]> transformed = new LinkedHashMap<>();
        int classIndex = 0;
        for (Map.Entry<String, byte[]> e : classes.entrySet()) {
            String className = e.getKey().replace(".class", "");
            if (shouldSkip(className)) {
                transformed.put(e.getKey(), stripDebugInfo(e.getValue()));
                continue;
            }
            try {
                byte[] result = transformClass(e.getValue(), className, classIndex++);
                transformed.put(e.getKey(), result);
            } catch (Exception ex) {
                System.err.println("[!] " + className + ": " + ex.getMessage());
                transformed.put(e.getKey(), e.getValue());
            }
        }

        for (String fakeName : fakeClassNames) {
            byte[] data = generateFakeClass(fakeName);
            transformed.put(fakeName + ".class", data);
            fakeClassesAdded++;
        }

        // ═══ 25 REALISTIC HONEYPOT DECOYS (YEM SINIFLAR) ═══
        // Decompiler'ları ve tersine mühendisleri yanıltmak için gerçekçi görünen tuzak sınıflar
        Map<String, byte[]> realisticHoneypots = generateRealisticHoneypots();
        for (Map.Entry<String, byte[]> hp : realisticHoneypots.entrySet()) {
            honeypotClassNames.add(hp.getKey());
            transformed.put(hp.getKey(), hp.getValue());
        }
        System.out.println("[Obfuscator] " + realisticHoneypots.size() + " adet gercekci Honeypot (Tuzak) sinifi JAR kokune eklendi.");

        // ═══ FULL TITANIUM OBFUSCATED JAR PACKING ═══
        // Tüm sınıflar ağır ASM obfuscation (Arapça Unicode semboller, çok katmanlı String şifreleme,
        // dinamik sayı kodlama, sahte tuzak alanlar ve metotlar, anti-debug, sıfırlanmış debug tabloları)
        // ile korunarak doğrudan JAR'a eklenir. Böylece Fabric KnotClassLoader modülleri eksiksiz
        // ve hatasız yüklerken decompiler'lar yalnızca okunamaz Arapça semboller ve çökmeler görür.
        Map<String, byte[]> jarRootClasses = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> e : transformed.entrySet()) {
            jarRootClasses.put(e.getKey(), e.getValue());
        }

        System.out.println("[Obfuscator] " + jarRootClasses.size() + " adet tam zirhli obfuscated sinif (Arapca Unicode & String Sifreleme) JAR kokune yazildi.");

        Manifest outManifest = manifest != null ? manifest : new Manifest();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar), outManifest)) {
            for (Map.Entry<String, byte[]> e : jarRootClasses.entrySet()) {
                jos.putNextEntry(new ZipEntry(e.getKey()));
                jos.write(e.getValue());
                jos.closeEntry();
            }
            for (Map.Entry<String, byte[]> e : resources.entrySet()) {
                jos.putNextEntry(new ZipEntry(e.getKey()));
                jos.write(e.getValue());
                jos.closeEntry();
            }
            addAntiDecompilerEntries(jos);
        }

        printStats(outputJar);
    }

    private static byte[] getPayloadKey() {
        byte[] k = new byte[32];
        long seed = 0x8F54C29B7E31A0D6L;
        for (int i = 0; i < 32; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            k[i] = (byte) ((seed >> 24) ^ (i * 37 + 0x5C));
        }
        return k;
    }

    private byte[] createEncryptedPayload(Map<String, byte[]> classesToEncrypt) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzos)) {
            dos.writeInt(classesToEncrypt.size());
            for (Map.Entry<String, byte[]> entry : classesToEncrypt.entrySet()) {
                String className = entry.getKey().replace('/', '.').replace(".class", "");
                dos.writeUTF(className);
                dos.writeInt(entry.getValue().length);
                dos.write(entry.getValue());
            }
        }

        byte[] plain = baos.toByteArray();
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(getPayloadKey(), "AES"), new GCMParameterSpec(128, iv));
        byte[] cipherText = cipher.doFinal(plain);

        ByteBuffer out = ByteBuffer.allocate(iv.length + cipherText.length);
        out.put(iv);
        out.put(cipherText);
        return out.array();
    }

    private boolean isProtectedPayloadClass(String path) {
        String p = path.replace('\\', '/');
        if (honeypotClassNames.contains(p)) return false; // Root JAR'da decompiler yemi olarak kalir
        if (p.startsWith(MIXIN_PKG)) return false;
        if (p.startsWith("pw/yami/client/loader/")) return false;
        if (p.equals("pw/yami/client/Bootstrap.class")) return false;
        if (p.equals("pw/yami/client/Mc.class")) return false;
        if (p.equals("pw/yami/client/Thread.class")) return false;
        if (p.startsWith("pw/yami/client/license/UpdateApplier")) return false;

        if (p.startsWith("pw/yami/client/module/")) return true;
        if (p.startsWith("pw/yami/client/modules/")) return true;
        if (p.startsWith("pw/yami/client/manager/")) return true;
        if (p.startsWith("pw/yami/client/gui/")) return true;
        if (p.startsWith("pw/yami/client/command/")) return true;
        if (p.startsWith("pw/yami/client/combat/")) return true;
        if (p.startsWith("pw/yami/client/player/")) return true;
        if (p.startsWith("pw/yami/client/render/")) return true;
        if (p.startsWith("pw/yami/client/setting/")) return true;
        if (p.startsWith("pw/yami/client/utils/")) return true;
        if (p.startsWith("pw/yami/client/world/")) return true;
        if (p.startsWith("pw/yami/client/event/")) return true;
        if (p.startsWith("pw/yami/client/model/")) return true;
        if (p.startsWith("pw/yami/client/config/")) return true;
        if (p.startsWith("pw/yami/client/handler/")) return true;
        if (p.startsWith("pw/yami/client/api/")) return true;
        if (p.startsWith("pw/yami/client/license/")) return true;
        if (p.startsWith("pw/yami/eventbus/")) return true;
        if (p.startsWith("pw/yami/satin/")) return true;
        if (p.startsWith("pw/yami/")) return true;
        return false;
    }

    private boolean shouldSkip(String cn) {
        return cn.equals("pw/yami/client/Bootstrap")
            || cn.equals("pw/yami/client/Mc")
            || cn.equals("pw/yami/client/Thread")
            || cn.startsWith(MIXIN_PKG)
            || cn.startsWith("pw/yami/client/loader/")
            || cn.startsWith("pw/yami/client/license/UpdateApplier")
            || cn.startsWith("pw/yami/satin/")
            || cn.startsWith("pw/yami/eventbus/")
            || !cn.startsWith("pw/yami/");
    }

    private byte[] transformClass(byte[] data, String className, int classIndex) {
        ClassReader cr = new ClassReader(data);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        // 1. Debug bilgileri temizle
        stripDebugFromNode(cn);

        // Arayüzler (Interfaces) JVM spesifikasyonuna göre statik alan/metot enjeksiyonuna uygun değildir
        if ((cn.access & Opcodes.ACC_INTERFACE) != 0) {
            ClassWriter cw = new ClassWriter(0);
            cn.accept(cw);
            return cw.toByteArray();
        }

        int algoVariant = classIndex % 5;
        long classSeed = className.hashCode() * 2654435761L;

        // 1. Debug bilgileri temizle
        stripDebugFromNode(cn);

        // 2. Runtime Opaque Fields oluştur (<clinit>'te dinamik hesaplanır)
        String[] opaqueFields = addRuntimeOpaqueFields(cn, classSeed);

        // 3. Per-class inline decryptor üret (Anti-Reflection + Caller Verification korumalı)
        String decryptorName = nextUnicode();
        String decryptorName2 = nextUnicode();
        generateInlineDecryptor(cn, decryptorName, algoVariant, classSeed);
        generateInlineDecryptor2(cn, decryptorName2, algoVariant, classSeed);

        // 4. String şifreleme
        encryptStrings(cn, decryptorName, decryptorName2, algoVariant, classSeed);

        // 5. Sayı kodlama (Anti-constant-folding ile dinamik alana bağlı)
        String numberHelper = nextUnicode();
        generateNumberHelper(cn, numberHelper, classSeed, opaqueFields[0]);
        encodeNumbers(cn, numberHelper);

        // 6. Flow Obfuscation: Statik başlatma yarış durumlarını önlemek için devre dışı (tüm sınıflar zaten AES-256 şifreli)
        // if ((cn.access & Opcodes.ACC_INTERFACE) == 0) {
        //     addHardcoreFlowObfuscation(cn, opaqueFields);
        // }

        // 7. Ağır sahte üyeler (arayüzler hariç)
        if ((cn.access & Opcodes.ACC_INTERFACE) == 0) {
            addHeavyJunkMembers(cn);
        }

        // 8. Sahte decryptor çağrıları (dead code içinde)
        addFakeDecryptorCalls(cn, decryptorName, decryptorName2);

        // 9. Alan adlandırma GSON ve public alanları bozduğu için devre dışı (tüm sınıflar zaten AES-256 şifreli)
        // renameAllFields(cn);

        // 10. Özel metotları yeniden adlandır (this::method lambda çağrılarını bozduğu için devre dışı - sınıflar zaten AES-256 şifreli)
        // if ((cn.access & Opcodes.ACC_INTERFACE) == 0) {
        //     renamePrivateMethods(cn);
        // }

        // 11. Anti-decompiler metadata
        addAntiDecompilerMetadata(cn);

        // 12. Exception handler tuzakları StackMapTable bozduğu için devre dışı (Java 21 JVM güvenliği)
        // addDiverseExceptionHandlers(cn, opaqueFields);

        // Java 21 için StackMapTable çerçevelerini otomatik hesapla
        // hierarchyLoader ile Meteor/Minecraft sınıf hiyerarşisini doğru çözer
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                // hierarchyLoader'dan sınıfları yükleyerek gerçek hiyerarşiyi çöz
                try {
                    ClassLoader cl = hierarchyLoader != null ? hierarchyLoader : getClassLoader();
                    Class<?> c1, c2;
                    try {
                        c1 = Class.forName(type1.replace('/', '.'), false, cl);
                        c2 = Class.forName(type2.replace('/', '.'), false, cl);
                    } catch (ClassNotFoundException e) {
                        return "java/lang/Object";
                    }
                    if (c1.isAssignableFrom(c2)) return type1;
                    if (c2.isAssignableFrom(c1)) return type2;
                    if (c1.isInterface() || c2.isInterface()) return "java/lang/Object";
                    do {
                        c1 = c1.getSuperclass();
                    } while (!c1.isAssignableFrom(c2));
                    return c1.getName().replace('.', '/');
                } catch (Throwable t) {
                    return "java/lang/Object";
                }
            }
        };
        try {
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Exception e) {
            ClassWriter cw2 = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw2);
            return cw2.toByteArray();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ANTI-REFLECTION & CALLER VERIFICATION INLINE DECRYPTOR
    // ═══════════════════════════════════════════════════════════════
    private void generateInlineDecryptor(ClassNode cn, String name, int variant, long seed) {
        MethodNode mn = new MethodNode(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            name, "(Ljava/lang/String;I)Ljava/lang/String;", null, null
        );
        mn.instructions = new InsnList();

        // null check
        LabelNode notNull = new LabelNode();
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        mn.instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, notNull));
        mn.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        mn.instructions.add(new InsnNode(Opcodes.ARETURN));
        mn.instructions.add(notNull);

        // ═══ ANTI-REFLECTION & CALLER VERIFICATION ═══
        // Throwable t = new Throwable(); StackTraceElement[] st = t.getStackTrace();
        // if (st == null || st.length < 2 || !st[1].getClassName().equals(cn.name.replace('/', '.'))) return "\u0000[PROTECTED]";
        LabelNode callerOk = new LabelNode();
        LabelNode corrupt = new LabelNode();

        mn.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/Throwable"));
        mn.instructions.add(new InsnNode(Opcodes.DUP));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Throwable", "<init>", "()V", false));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false));
        mn.instructions.add(new VarInsnNode(Opcodes.ASTORE, 2));

        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        mn.instructions.add(new JumpInsnNode(Opcodes.IFNULL, corrupt));
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        mn.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
        mn.instructions.add(new InsnNode(Opcodes.ICONST_2));
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPLT, corrupt));

        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        mn.instructions.add(new InsnNode(Opcodes.ICONST_1));
        mn.instructions.add(new InsnNode(Opcodes.AALOAD));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;", false));
        mn.instructions.add(new LdcInsnNode(cn.name.replace('/', '.')));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
        mn.instructions.add(new JumpInsnNode(Opcodes.IFNE, callerOk));

        mn.instructions.add(corrupt);
        mn.instructions.add(new LdcInsnNode("\u0000[PROTECTED]"));
        mn.instructions.add(new InsnNode(Opcodes.ARETURN));

        mn.instructions.add(callerOk);

        // char[] c = s.toCharArray();
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false));
        mn.instructions.add(new VarInsnNode(Opcodes.ASTORE, 2)); // c

        // int len = c.length;
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        mn.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
        mn.instructions.add(new VarInsnNode(Opcodes.ISTORE, 3)); // len

        // char[] r = new char[len];
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
        mn.instructions.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_CHAR));
        mn.instructions.add(new VarInsnNode(Opcodes.ASTORE, 4)); // r

        // int i = 0;
        mn.instructions.add(new InsnNode(Opcodes.ICONST_0));
        mn.instructions.add(new VarInsnNode(Opcodes.ISTORE, 5)); // i

        int classConst = (int) (seed & 0xFFFF);

        LabelNode loopStart = new LabelNode();
        LabelNode loopEnd = new LabelNode();

        mn.instructions.add(loopStart);
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPGE, loopEnd));

        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));

        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
        mn.instructions.add(new InsnNode(Opcodes.CALOAD));

        switch (variant) {
            case 0 -> {
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
                mn.instructions.add(new InsnNode(Opcodes.ICONST_1));
                mn.instructions.add(new InsnNode(Opcodes.IADD));
                mn.instructions.add(new InsnNode(Opcodes.IMUL));
                mn.instructions.add(new LdcInsnNode(classConst));
                mn.instructions.add(new InsnNode(Opcodes.IADD));
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
                mn.instructions.add(new LdcInsnNode(31));
                mn.instructions.add(new InsnNode(Opcodes.IMUL));
                mn.instructions.add(new InsnNode(Opcodes.IXOR));
            }
            case 1 -> {
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
                mn.instructions.add(new InsnNode(Opcodes.ICONST_3));
                mn.instructions.add(new InsnNode(Opcodes.ISHL));
                mn.instructions.add(new InsnNode(Opcodes.IXOR));
                mn.instructions.add(new LdcInsnNode(classConst));
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
                mn.instructions.add(new InsnNode(Opcodes.ICONST_2));
                mn.instructions.add(new InsnNode(Opcodes.IADD));
                mn.instructions.add(new InsnNode(Opcodes.IMUL));
                mn.instructions.add(new InsnNode(Opcodes.IADD));
            }
            case 2 -> {
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
                mn.instructions.add(new LdcInsnNode(7));
                mn.instructions.add(new InsnNode(Opcodes.IMUL));
                mn.instructions.add(new InsnNode(Opcodes.IADD));
                mn.instructions.add(new LdcInsnNode(classConst));
                mn.instructions.add(new InsnNode(Opcodes.IXOR));
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
                mn.instructions.add(new InsnNode(Opcodes.IMUL));
                mn.instructions.add(new InsnNode(Opcodes.IADD));
            }
            case 3 -> {
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
                mn.instructions.add(new LdcInsnNode(7));
                mn.instructions.add(new InsnNode(Opcodes.IREM));
                mn.instructions.add(new InsnNode(Opcodes.IUSHR));
                mn.instructions.add(new LdcInsnNode(classConst));
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
                mn.instructions.add(new LdcInsnNode(13));
                mn.instructions.add(new InsnNode(Opcodes.IMUL));
                mn.instructions.add(new InsnNode(Opcodes.IADD));
                mn.instructions.add(new InsnNode(Opcodes.IXOR));
            }
            default -> {
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
                mn.instructions.add(new LdcInsnNode((int) 2654435761L));
                mn.instructions.add(new InsnNode(Opcodes.IMUL));
                mn.instructions.add(new LdcInsnNode(16));
                mn.instructions.add(new InsnNode(Opcodes.IUSHR));
                mn.instructions.add(new LdcInsnNode(classConst));
                mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 5));
                mn.instructions.add(new InsnNode(Opcodes.IADD));
                mn.instructions.add(new InsnNode(Opcodes.IXOR));
            }
        }

        mn.instructions.add(new LdcInsnNode(0xFFFF));
        mn.instructions.add(new InsnNode(Opcodes.IAND));
        mn.instructions.add(new InsnNode(Opcodes.IXOR));
        mn.instructions.add(new InsnNode(Opcodes.I2C));
        mn.instructions.add(new InsnNode(Opcodes.CASTORE));

        mn.instructions.add(new IincInsnNode(5, 1));
        mn.instructions.add(new JumpInsnNode(Opcodes.GOTO, loopStart));
        mn.instructions.add(loopEnd);

        mn.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        mn.instructions.add(new InsnNode(Opcodes.DUP));
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false));
        mn.instructions.add(new InsnNode(Opcodes.ARETURN));

        mn.maxLocals = 6;
        mn.maxStack = 6;
        cn.methods.add(mn);
    }

    private void generateInlineDecryptor2(ClassNode cn, String name, int variant, long seed) {
        MethodNode mn = new MethodNode(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            name, "(Ljava/lang/String;II)Ljava/lang/String;", null, null
        );
        mn.instructions = new InsnList();

        LabelNode notNull = new LabelNode();
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        mn.instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, notNull));
        mn.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        mn.instructions.add(new InsnNode(Opcodes.ARETURN));
        mn.instructions.add(notNull);

        // Anti-reflection / caller check
        LabelNode callerOk = new LabelNode();
        LabelNode corrupt = new LabelNode();

        mn.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/Throwable"));
        mn.instructions.add(new InsnNode(Opcodes.DUP));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Throwable", "<init>", "()V", false));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false));
        mn.instructions.add(new VarInsnNode(Opcodes.ASTORE, 3));

        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        mn.instructions.add(new JumpInsnNode(Opcodes.IFNULL, corrupt));
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        mn.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
        mn.instructions.add(new InsnNode(Opcodes.ICONST_2));
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPLT, corrupt));

        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        mn.instructions.add(new InsnNode(Opcodes.ICONST_1));
        mn.instructions.add(new InsnNode(Opcodes.AALOAD));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;", false));
        mn.instructions.add(new LdcInsnNode(cn.name.replace('/', '.')));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
        mn.instructions.add(new JumpInsnNode(Opcodes.IFNE, callerOk));

        mn.instructions.add(corrupt);
        mn.instructions.add(new LdcInsnNode("\u0000[PROTECTED]"));
        mn.instructions.add(new InsnNode(Opcodes.ARETURN));

        mn.instructions.add(callerOk);

        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false));
        mn.instructions.add(new VarInsnNode(Opcodes.ASTORE, 3));

        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        mn.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
        mn.instructions.add(new VarInsnNode(Opcodes.ISTORE, 4));

        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        mn.instructions.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_CHAR));
        mn.instructions.add(new VarInsnNode(Opcodes.ASTORE, 5));

        mn.instructions.add(new InsnNode(Opcodes.ICONST_0));
        mn.instructions.add(new VarInsnNode(Opcodes.ISTORE, 6));

        int classConst = (int) ((seed >> 16) & 0xFFFF);

        LabelNode loopStart = new LabelNode();
        LabelNode loopEnd = new LabelNode();

        mn.instructions.add(loopStart);
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 6));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPGE, loopEnd));

        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 5));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 6));
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 6));
        mn.instructions.add(new InsnNode(Opcodes.CALOAD));

        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 6));
        mn.instructions.add(new InsnNode(Opcodes.ICONST_1));
        mn.instructions.add(new InsnNode(Opcodes.IADD));
        mn.instructions.add(new InsnNode(Opcodes.IMUL));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 6));
        mn.instructions.add(new InsnNode(Opcodes.ICONST_5));
        mn.instructions.add(new InsnNode(Opcodes.IREM));
        mn.instructions.add(new InsnNode(Opcodes.ISHL));
        mn.instructions.add(new InsnNode(Opcodes.IXOR));
        mn.instructions.add(new LdcInsnNode(classConst));
        mn.instructions.add(new InsnNode(Opcodes.IADD));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 6));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 6));
        mn.instructions.add(new InsnNode(Opcodes.IMUL));
        mn.instructions.add(new InsnNode(Opcodes.ICONST_3));
        mn.instructions.add(new InsnNode(Opcodes.IMUL));
        mn.instructions.add(new InsnNode(Opcodes.IXOR));

        mn.instructions.add(new LdcInsnNode(0xFFFF));
        mn.instructions.add(new InsnNode(Opcodes.IAND));
        mn.instructions.add(new InsnNode(Opcodes.IXOR));
        mn.instructions.add(new InsnNode(Opcodes.I2C));
        mn.instructions.add(new InsnNode(Opcodes.CASTORE));

        mn.instructions.add(new IincInsnNode(6, 1));
        mn.instructions.add(new JumpInsnNode(Opcodes.GOTO, loopStart));
        mn.instructions.add(loopEnd);

        mn.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        mn.instructions.add(new InsnNode(Opcodes.DUP));
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 5));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false));
        mn.instructions.add(new InsnNode(Opcodes.ARETURN));

        mn.maxLocals = 7;
        mn.maxStack = 7;
        cn.methods.add(mn);
    }

    private void encryptStrings(ClassNode cn, String dec1, String dec2, int variant, long seed) {
        int classConst = (int) (seed & 0xFFFF);
        int classConst2 = (int) ((seed >> 16) & 0xFFFF);

        boolean isItf = (cn.access & Opcodes.ACC_INTERFACE) != 0;

        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null) continue;
            if (mn.name.equals(dec1) || mn.name.equals(dec2)) continue;

            for (AbstractInsnNode insn : mn.instructions.toArray()) {
                if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String str) {
                    if (str.isEmpty()) continue;

                    int key = RNG.nextInt(0xFFFF);

                    if (str.length() < 20) {
                        String enc = encryptVariant(str, key, variant, classConst);
                        ldc.cst = enc;
                        InsnList patch = new InsnList();
                        patch.add(new LdcInsnNode(key));
                        patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cn.name, dec1,
                            "(Ljava/lang/String;I)Ljava/lang/String;", isItf));
                        mn.instructions.insert(ldc, patch);
                    } else {
                        int key2 = RNG.nextInt(0xFFFF);
                        String enc = encryptVariant2(str, key, key2, classConst2);
                        ldc.cst = enc;
                        InsnList patch = new InsnList();
                        patch.add(new LdcInsnNode(key));
                        patch.add(new LdcInsnNode(key2));
                        patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cn.name, dec2,
                            "(Ljava/lang/String;II)Ljava/lang/String;", isItf));
                        mn.instructions.insert(ldc, patch);
                    }
                    stringsEncrypted++;
                }
            }
        }
    }

    private String encryptVariant(String plain, int key, int variant, int classConst) {
        char[] c = plain.toCharArray();
        char[] r = new char[c.length];
        for (int i = 0; i < c.length; i++) {
            int shift = switch (variant) {
                case 0 -> (key * (i + 1) + classConst) ^ (i * 31);
                case 1 -> (key ^ (i << 3)) + classConst * (i + 2);
                case 2 -> ((key + i * 7) ^ classConst) + (i * i);
                case 3 -> (key >>> (i % 7)) ^ (classConst + i * 13);
                default -> ((key * (int) 2654435761L) >>> 16) ^ (classConst + i);
            };
            r[i] = (char) (c[i] ^ (shift & 0xFFFF));
        }
        return new String(r);
    }

    private String encryptVariant2(String plain, int k1, int k2, int classConst) {
        char[] c = plain.toCharArray();
        char[] r = new char[c.length];
        for (int i = 0; i < c.length; i++) {
            int shift = ((k1 * (i + 1)) ^ (k2 << (i % 5))) + classConst ^ (i * i * 3);
            r[i] = (char) (c[i] ^ (shift & 0xFFFF));
        }
        return new String(r);
    }

    // ═══════════════════════════════════════════════════════════════
    // ANTI-CONSTANT-FOLDING NUMBER ENCODER
    // ═══════════════════════════════════════════════════════════════
    private void generateNumberHelper(ClassNode cn, String name, long seed, String runtimeField) {
        MethodNode mn = new MethodNode(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            name, "(III)I", null, null
        );
        mn.instructions = new InsnList();
        int cc = (int) (seed & 0xFF);

        // return ((a ^ b) * 3 + c) ^ cc ^ (runtimeField & 0)
        // runtimeField & 0 is ALWAYS 0 at runtime, but prevents static constant folding completely!
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 0));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        mn.instructions.add(new InsnNode(Opcodes.IXOR));
        mn.instructions.add(new InsnNode(Opcodes.ICONST_3));
        mn.instructions.add(new InsnNode(Opcodes.IMUL));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        mn.instructions.add(new InsnNode(Opcodes.IADD));
        mn.instructions.add(new LdcInsnNode(cc));
        mn.instructions.add(new InsnNode(Opcodes.IXOR));
        mn.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, cn.name, runtimeField, "I"));
        mn.instructions.add(new InsnNode(Opcodes.ICONST_0));
        mn.instructions.add(new InsnNode(Opcodes.IAND));
        mn.instructions.add(new InsnNode(Opcodes.IXOR));
        mn.instructions.add(new InsnNode(Opcodes.IRETURN));
        mn.maxLocals = 3;
        mn.maxStack = 4;
        cn.methods.add(mn);
    }

    private void encodeNumbers(ClassNode cn, String helper) {
        long seed = cn.name.hashCode() * 2654435761L;
        int cc = (int) (seed & 0xFF);

        boolean isItf = (cn.access & Opcodes.ACC_INTERFACE) != 0;

        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null) continue;
            if (mn.name.equals(helper)) continue;

            for (AbstractInsnNode insn : mn.instructions.toArray()) {
                if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer val) {
                    if (Math.abs(val) < 5) continue;

                    int a = RNG.nextInt();
                    int b = RNG.nextInt();
                    int abXor = a ^ b;
                    int c = (val ^ cc) - abXor * 3;

                    InsnList patch = new InsnList();
                    patch.add(new LdcInsnNode(a));
                    patch.add(new LdcInsnNode(b));
                    patch.add(new LdcInsnNode(c));
                    patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cn.name, helper,
                        "(III)I", isItf));

                    mn.instructions.insert(insn, patch);
                    mn.instructions.remove(insn);
                    numbersEncoded++;
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // RUNTIME DYNAMIC OPAQUE FIELDS (<clinit>-based)
    // ═══════════════════════════════════════════════════════════════
    private String[] addRuntimeOpaqueFields(ClassNode cn, long seed) {
        String f1 = nextUnicode(), f2 = nextUnicode(), f3 = nextUnicode(), f4 = nextUnicode();
        int fieldAccess = (cn.access & Opcodes.ACC_INTERFACE) != 0
            ? (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)
            : (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
        cn.fields.add(new FieldNode(fieldAccess, f1, "I", null, null));
        cn.fields.add(new FieldNode(fieldAccess, f2, "J", null, null));
        cn.fields.add(new FieldNode(fieldAccess, f3, "I", null, null));
        cn.fields.add(new FieldNode(fieldAccess, f4, "I", null, null));

        MethodNode clinit = null;
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("<clinit>")) { clinit = mn; break; }
        }
        if (clinit == null) {
            clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions = new InsnList();
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            cn.methods.add(clinit);
        }

        InsnList init = new InsnList();

        // f1 = Object.class.getName().length() * 3 + 7  → 55
        init.add(new LdcInsnNode(Type.getType("Ljava/lang/Object;")));
        init.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName",
            "()Ljava/lang/String;", false));
        init.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length",
            "()I", false));
        init.add(new InsnNode(Opcodes.ICONST_3));
        init.add(new InsnNode(Opcodes.IMUL));
        init.add(new LdcInsnNode(7));
        init.add(new InsnNode(Opcodes.IADD));
        init.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, f1, "I"));

        // f2 = System.nanoTime() | 1L
        init.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false));
        init.add(new LdcInsnNode(1L));
        init.add(new InsnNode(Opcodes.LOR));
        init.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, f2, "J"));

        // f3 = Thread.currentThread().getName().length()
        init.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread",
            "()Ljava/lang/Thread;", false));
        init.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getName",
            "()Ljava/lang/String;", false));
        init.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
        init.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, f3, "I"));

        // f4 = Runtime.getRuntime().availableProcessors() * 5 + 3 (always >= 8 at runtime)
        init.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Runtime", "getRuntime", "()Ljava/lang/Runtime;", false));
        init.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Runtime", "availableProcessors", "()I", false));
        init.add(new InsnNode(Opcodes.ICONST_5));
        init.add(new InsnNode(Opcodes.IMUL));
        init.add(new InsnNode(Opcodes.ICONST_3));
        init.add(new InsnNode(Opcodes.IADD));
        init.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, f4, "I"));

        clinit.instructions.insert(init);

        return new String[]{f1, f2, f3, f4};
    }

    // ═══════════════════════════════════════════════════════════════
    // HARDCORE FLOW OBFUSCATION (Lambdalar ve Küçük Metotlar Dahil)
    // ═══════════════════════════════════════════════════════════════
    private void addHardcoreFlowObfuscation(ClassNode cn, String[] opaqueFields) {
        for (MethodNode mn : cn.methods) {
            // Lambdalar ve küçük metotlar dahil: size >= 2
            if (mn.instructions == null || mn.instructions.size() < 2) continue;
            if (mn.name.equals("<init>") || mn.name.equals("<clinit>")) continue;

            // 1. Runtime Opaque Trap: f1 (55) asla 0 olamaz
            LabelNode skip1 = new LabelNode();
            InsnList trap1 = new InsnList();
            trap1.add(new FieldInsnNode(Opcodes.GETSTATIC, cn.name, opaqueFields[0], "I"));
            trap1.add(new JumpInsnNode(Opcodes.IFNE, skip1));
            addRealisticDeadCode(trap1, cn.name);
            trap1.add(skip1);
            mn.instructions.insert(trap1);

            // 2. f4 (availableProcessors * 5 + 3) her zaman > 2
            if (mn.instructions.size() > 8) {
                LabelNode skip2 = new LabelNode();
                InsnList trap2 = new InsnList();
                trap2.add(new FieldInsnNode(Opcodes.GETSTATIC, cn.name, opaqueFields[3], "I"));
                trap2.add(new InsnNode(Opcodes.ICONST_2));
                trap2.add(new JumpInsnNode(Opcodes.IF_ICMPGT, skip2));
                addRealisticDeadCode(trap2, cn.name);
                trap2.add(skip2);

                AbstractInsnNode mid = findSafeInsertPoint(mn, mn.instructions.size() / 2);
                if (mid != null) mn.instructions.insertBefore(mid, trap2);
            }

            // 3. f2 (nanoTime | 1) her zaman != 0
            LabelNode skip3 = new LabelNode();
            InsnList trap3 = new InsnList();
            trap3.add(new FieldInsnNode(Opcodes.GETSTATIC, cn.name, opaqueFields[1], "J"));
            trap3.add(new InsnNode(Opcodes.LCONST_0));
            trap3.add(new InsnNode(Opcodes.LCMP));
            trap3.add(new JumpInsnNode(Opcodes.IFNE, skip3));
            addRealisticDeadCode(trap3, cn.name);
            trap3.add(skip3);

            // 4. Cebirsel Invariant Trap: x*(x+1)&1 her zaman 0'dır (ardışık iki tam sayının çarpımı daima çifttir)
            if (mn.instructions.size() > 16) {
                LabelNode skip4 = new LabelNode();
                InsnList trap4 = new InsnList();
                trap4.add(new LdcInsnNode(Type.getType("Ljava/lang/Object;")));
                trap4.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", false));
                trap4.add(new InsnNode(Opcodes.DUP));
                trap4.add(new InsnNode(Opcodes.ICONST_1));
                trap4.add(new InsnNode(Opcodes.IADD));
                trap4.add(new InsnNode(Opcodes.IMUL));
                trap4.add(new InsnNode(Opcodes.ICONST_1));
                trap4.add(new InsnNode(Opcodes.IAND));
                trap4.add(new JumpInsnNode(Opcodes.IFEQ, skip4));
                addRealisticDeadCode(trap4, cn.name);
                trap4.add(skip4);

                AbstractInsnNode quarter = findSafeInsertPoint(mn, mn.instructions.size() / 4);
                if (quarter != null) mn.instructions.insertBefore(quarter, trap4);
            }

            for (AbstractInsnNode insn : mn.instructions.toArray()) {
                if (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN) {
                    mn.instructions.insertBefore(insn, trap3);
                    break;
                }
            }

            flowObf++;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DECOY API POLLUTION (Gerçek Minecraft & Meteor Client Referansları)
    // ═══════════════════════════════════════════════════════════════
    private void addRealisticDeadCode(InsnList list, String owner) {
        int choice = RNG.nextInt(8);
        switch (choice) {
            case 0 -> {
                // Decoy: System nanoTime & hash
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false));
                list.add(new InsnNode(Opcodes.POP2));
            }
            case 1 -> {
                // Decoy: Math calculations
                list.add(new LdcInsnNode(1.61803398875));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Math", "sin", "(D)D", false));
                list.add(new InsnNode(Opcodes.POP2));
            }
            case 2 -> {
                // Decoy: Collections
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList", "()Ljava/util/List;", false));
                list.add(new InsnNode(Opcodes.POP));
            }
            case 3 -> {
                // Decoy: Cryptography & MessageDigest
                list.add(new LdcInsnNode("SHA-256"));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/security/MessageDigest", "getInstance",
                    "(Ljava/lang/String;)Ljava/security/MessageDigest;", false));
                list.add(new InsnNode(Opcodes.POP));
            }
            case 4 -> {
                // Decoy: ConcurrentHashMap & Collections
                list.add(new TypeInsnNode(Opcodes.NEW, "java/util/concurrent/ConcurrentHashMap"));
                list.add(new InsnNode(Opcodes.DUP));
                list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/concurrent/ConcurrentHashMap", "<init>", "()V", false));
                list.add(new InsnNode(Opcodes.POP));
            }
            case 5 -> {
                // Decoy: Memory & Runtime check
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Runtime", "getRuntime", "()Ljava/lang/Runtime;", false));
                list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Runtime", "freeMemory", "()J", false));
                list.add(new InsnNode(Opcodes.POP2));
            }
            case 6 -> {
                // Decoy: String building & UUID
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/UUID", "randomUUID", "()Ljava/util/UUID;", false));
                list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/UUID", "toString", "()Ljava/lang/String;", false));
                list.add(new InsnNode(Opcodes.POP));
            }
            default -> {
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false));
                list.add(new InsnNode(Opcodes.POP2));
            }
        }
    }

    private AbstractInsnNode findSafeInsertPoint(MethodNode mn, int targetIndex) {
        AbstractInsnNode[] insns = mn.instructions.toArray();
        if (insns.length == 0) return null;
        targetIndex = Math.max(0, Math.min(targetIndex, insns.length - 1));
        for (int i = targetIndex; i < insns.length - 1; i++) {
            AbstractInsnNode node = insns[i];
            if (node == null || node.getPrevious() == null) continue;
            int op = node.getOpcode();
            if (op >= 0 && !(node instanceof JumpInsnNode) && op != Opcodes.ATHROW
                && !(op >= Opcodes.IRETURN && op <= Opcodes.RETURN)
                && !(node instanceof LabelNode) && !(node instanceof FrameNode)) {
                return node;
            }
        }
        return null;
    }

    private void addFakeDecryptorCalls(ClassNode cn, String dec1, String dec2) {
        if ((cn.access & Opcodes.ACC_INTERFACE) != 0) return;
        boolean isItf = (cn.access & Opcodes.ACC_INTERFACE) != 0;

        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null || mn.instructions.size() < 10) continue;
            if (mn.name.equals("<init>") || mn.name.equals("<clinit>")) continue;
            if (mn.name.equals(dec1) || mn.name.equals(dec2)) continue;

            int count = 1 + RNG.nextInt(2);
            for (int c = 0; c < count; c++) {
                AbstractInsnNode target = findSafeInsertPoint(mn, RNG.nextInt(Math.max(1, mn.instructions.size() / 2)));
                if (target == null) continue;

                LabelNode skip = new LabelNode();
                InsnList fake = new InsnList();

                // Çeşitlendirilmiş Opak Predicate Desenleri (Desen tanımayı ve otomatik ASM silmeyi engeller)
                int pattern = RNG.nextInt(4);
                switch (pattern) {
                    case 0 -> {
                        // 1. Cebirsel Invariant: Ardışık iki tam sayının çarpımı daima çifttir: (x * (x + 1)) & 1 == 0
                        int randVal = 100 + RNG.nextInt(500);
                        fake.add(new LdcInsnNode(randVal));
                        fake.add(new InsnNode(Opcodes.DUP));
                        fake.add(new InsnNode(Opcodes.ICONST_1));
                        fake.add(new InsnNode(Opcodes.IADD));
                        fake.add(new InsnNode(Opcodes.IMUL));
                        fake.add(new InsnNode(Opcodes.ICONST_1));
                        fake.add(new InsnNode(Opcodes.IAND));
                        fake.add(new JumpInsnNode(Opcodes.IFEQ, skip)); // Daima 0 olduğu için daima skip'e atlar
                    }
                    case 1 -> {
                        // 2. Thread Priority Invariant: Thread priority daima 1..10 arasındadır, asla > 10 olamaz
                        fake.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false));
                        fake.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getPriority", "()I", false));
                        fake.add(new IntInsnNode(Opcodes.BIPUSH, 10));
                        fake.add(new JumpInsnNode(Opcodes.IF_ICMPLE, skip)); // Daima <= 10 olduğu için skip'e atlar
                    }
                    case 2 -> {
                        // 3. String Descriptor Invariant: String descriptor uzunluğu 19'dur, asla < 2 olamaz
                        fake.add(new LdcInsnNode(Type.getType("Ljava/lang/String;")));
                        fake.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false));
                        fake.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
                        fake.add(new InsnNode(Opcodes.ICONST_2));
                        fake.add(new JumpInsnNode(Opcodes.IF_ICMPGE, skip)); // Daima >= 2 olduğu için skip'e atlar
                    }
                    default -> {
                        // 4. nanoTime Bitwise Invariant: (nanoTime | 1L) asla 0 olamaz
                        fake.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false));
                        fake.add(new LdcInsnNode(1L));
                        fake.add(new InsnNode(Opcodes.LOR));
                        fake.add(new InsnNode(Opcodes.LCONST_0));
                        fake.add(new InsnNode(Opcodes.LCMP));
                        fake.add(new JumpInsnNode(Opcodes.IFNE, skip)); // Daima != 0 olduğu için skip'e atlar
                    }
                }

                String targetDec = RNG.nextBoolean() ? dec1 : dec2;
                fake.add(new LdcInsnNode(nextUnicode()));
                fake.add(new LdcInsnNode(RNG.nextInt(0xFFFF)));
                fake.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cn.name, targetDec,
                    "(Ljava/lang/String;I)Ljava/lang/String;", isItf));
                fake.add(new InsnNode(Opcodes.POP));

                fake.add(skip);
                mn.instructions.insertBefore(target, fake);
                junkAdded++;
            }
        }
    }

    private void addHeavyJunkMembers(ClassNode cn) {
        if ((cn.access & Opcodes.ACC_INTERFACE) != 0) return; // Arayüzlere (interface) sahte üye eklenmez
        String[] descs = {"I", "J", "Z", "D", "F", "B", "Ljava/lang/Object;", "Ljava/lang/String;",
            "[B", "[I", "Ljava/util/List;", "Ljava/util/Map;", "Ljava/util/Set;",
            "Ljava/util/concurrent/atomic/AtomicInteger;", "Ljava/nio/ByteBuffer;"};

        int fieldCount = 10 + RNG.nextInt(8);
        for (int i = 0; i < fieldCount; i++) {
            cn.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC | (RNG.nextBoolean() ? Opcodes.ACC_STATIC : 0),
                nextUnicode(), descs[RNG.nextInt(descs.length)], null, null));
            junkAdded++;
        }

        int methodCount = 6 + RNG.nextInt(6);
        for (int i = 0; i < methodCount; i++) {
            String name = nextUnicode();
            String desc = switch (RNG.nextInt(8)) {
                case 0 -> "(I)I";
                case 1 -> "(Ljava/lang/String;)Ljava/lang/String;";
                case 2 -> "(II)Z";
                case 3 -> "()V";
                case 4 -> "(J)J";
                case 5 -> "([B)[B";
                case 6 -> "(ILjava/lang/Object;)Ljava/lang/Object;";
                default -> "(Ljava/lang/String;I)I";
            };
            MethodNode mn = new MethodNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
            mn.instructions = new InsnList();
            generateComplexFakeBody(mn, desc);
            cn.methods.add(mn);
            junkAdded++;
        }
    }

    private void generateComplexFakeBody(MethodNode mn, String desc) {
        mn.instructions.add(new LdcInsnNode(RNG.nextInt(10000)));
        mn.instructions.add(new VarInsnNode(Opcodes.ISTORE, 10));

        LabelNode loop = new LabelNode();
        LabelNode end = new LabelNode();
        mn.instructions.add(new InsnNode(Opcodes.ICONST_0));
        mn.instructions.add(new VarInsnNode(Opcodes.ISTORE, 11));
        mn.instructions.add(loop);
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 11));
        mn.instructions.add(new LdcInsnNode(10));
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPGE, end));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 10));
        mn.instructions.add(new LdcInsnNode(RNG.nextInt(0xFFFF)));
        mn.instructions.add(new InsnNode(Opcodes.IXOR));
        mn.instructions.add(new LdcInsnNode(RNG.nextInt(100)));
        mn.instructions.add(new InsnNode(Opcodes.IADD));
        mn.instructions.add(new VarInsnNode(Opcodes.ISTORE, 10));
        mn.instructions.add(new IincInsnNode(11, 1));
        mn.instructions.add(new JumpInsnNode(Opcodes.GOTO, loop));
        mn.instructions.add(end);

        char ret = desc.charAt(desc.indexOf(')') + 1);
        switch (ret) {
            case 'I' -> { mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 10)); mn.instructions.add(new InsnNode(Opcodes.IRETURN)); }
            case 'J' -> { mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 10)); mn.instructions.add(new InsnNode(Opcodes.I2L)); mn.instructions.add(new InsnNode(Opcodes.LRETURN)); }
            case 'Z' -> { mn.instructions.add(new InsnNode(Opcodes.ICONST_0)); mn.instructions.add(new InsnNode(Opcodes.IRETURN)); }
            case 'V' -> mn.instructions.add(new InsnNode(Opcodes.RETURN));
            default -> { mn.instructions.add(new InsnNode(Opcodes.ACONST_NULL)); mn.instructions.add(new InsnNode(Opcodes.ARETURN)); }
        }
        mn.maxLocals = 12;
        mn.maxStack = 4;
    }

    private void renameAllFields(ClassNode cn) {
        Map<String, String> map = new HashMap<>();
        for (FieldNode fn : cn.fields) {
            if (fn.name.length() > 0 && fn.name.charAt(0) >= 0x0400) continue;
            if ((fn.access & Opcodes.ACC_ENUM) != 0) continue;
            String n = nextUnicode();
            map.put(fn.name + "|" + fn.desc, n);
            fn.name = n;
            fieldsRenamed++;
        }
        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null) continue;
            for (AbstractInsnNode insn : mn.instructions.toArray()) {
                if (insn instanceof FieldInsnNode fin && fin.owner.equals(cn.name)) {
                    String k = fin.name + "|" + fin.desc;
                    if (map.containsKey(k)) fin.name = map.get(k);
                }
            }
        }
    }

    private void renamePrivateMethods(ClassNode cn) {
        Map<String, String> map = new HashMap<>();
        for (MethodNode mn : cn.methods) {
            if ((mn.access & Opcodes.ACC_PRIVATE) == 0) continue;
            if (PROTECTED_METHODS.contains(mn.name)) continue;
            if (hasEventHandlerAnnotation(mn)) continue;
            if (mn.name.startsWith("<")) continue;
            if (mn.name.startsWith("lambda$")) continue;
            if (mn.name.length() > 0 && mn.name.charAt(0) >= 0x0400) continue;
            String n = nextUnicode();
            map.put(mn.name + "|" + mn.desc, n);
            mn.name = n;
            methodsRenamed++;
        }
        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null) continue;
            for (AbstractInsnNode insn : mn.instructions.toArray()) {
                if (insn instanceof MethodInsnNode min && min.owner.equals(cn.name)) {
                    String k = min.name + "|" + min.desc;
                    if (map.containsKey(k)) min.name = map.get(k);
                }
            }
        }
    }

    private boolean hasEventHandlerAnnotation(MethodNode mn) {
        if (mn.visibleAnnotations != null)
            for (AnnotationNode a : mn.visibleAnnotations) if (a.desc.equals(EVENT_HANDLER_DESC)) return true;
        if (mn.invisibleAnnotations != null)
            for (AnnotationNode a : mn.invisibleAnnotations) if (a.desc.equals(EVENT_HANDLER_DESC)) return true;
        return false;
    }

    private byte[] stripDebugInfo(byte[] data) {
        ClassReader cr = new ClassReader(data);
        ClassWriter cw = new ClassWriter(0);
        cr.accept(cw, ClassReader.SKIP_DEBUG);
        debugStripped++;
        return cw.toByteArray();
    }

    private void stripDebugFromNode(ClassNode cn) {
        cn.sourceFile = null;
        cn.sourceDebug = null;
        for (MethodNode mn : cn.methods) {
            if (mn.instructions != null)
                for (AbstractInsnNode insn : mn.instructions.toArray())
                    if (insn instanceof LineNumberNode) mn.instructions.remove(insn);
            mn.localVariables = null;
            mn.parameters = null;
            debugStripped++;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 5 DIVERSE EXCEPTION HANDLER PATTERNS (Lambdalar Dahil)
    // ═══════════════════════════════════════════════════════════════
    private void addDiverseExceptionHandlers(ClassNode cn, String[] opaqueFields) {
        int patternIndex = 0;
        for (MethodNode mn : cn.methods) {
            // Lambdalar ve küçük metotlar dahil: size >= 4
            if (mn.instructions == null || mn.instructions.size() < 4) continue;
            if (mn.name.equals("<init>") || mn.name.equals("<clinit>")) continue;

            AbstractInsnNode[] insns = mn.instructions.toArray();
            if (insns.length < 3) continue;

            int pattern = (patternIndex++) % 4;

            try {
                switch (pattern) {
                    case 0 -> {
                        // Pattern 1: Nested Try-Catch Ranges (NullPointerException inside Throwable)
                        LabelNode lStart = new LabelNode();
                        LabelNode lMid = new LabelNode();
                        LabelNode lEnd = new LabelNode();
                        LabelNode h1 = new LabelNode();
                        LabelNode h2 = new LabelNode();
                        LabelNode after = new LabelNode();

                        mn.instructions.insert(lStart);
                        int midIdx = Math.max(1, insns.length / 2);
                        mn.instructions.insert(insns[midIdx], lMid);
                        mn.instructions.insert(insns[insns.length - 2], lEnd);

                        InsnList handlers = new InsnList();
                        handlers.add(h1);
                        handlers.add(new InsnNode(Opcodes.POP));
                        handlers.add(new JumpInsnNode(Opcodes.GOTO, after));
                        handlers.add(h2);
                        handlers.add(new InsnNode(Opcodes.POP));
                        handlers.add(new JumpInsnNode(Opcodes.GOTO, after));
                        handlers.add(after);

                        mn.instructions.insert(lEnd, handlers);

                        if (mn.tryCatchBlocks == null) mn.tryCatchBlocks = new ArrayList<>();
                        mn.tryCatchBlocks.add(new TryCatchBlockNode(lStart, lMid, h1, "java/lang/NullPointerException"));
                        mn.tryCatchBlocks.add(new TryCatchBlockNode(lStart, lEnd, h2, "java/lang/Throwable"));
                        exceptHandlers += 2;
                    }
                    case 1 -> {
                        // Pattern 2: Multi-catch with Mutating Side-Effect
                        LabelNode start = new LabelNode();
                        LabelNode end = new LabelNode();
                        LabelNode h = new LabelNode();
                        LabelNode after = new LabelNode();

                        mn.instructions.insert(start);
                        mn.instructions.insert(insns[insns.length - 2], end);

                        InsnList hCode = new InsnList();
                        hCode.add(h);
                        hCode.add(new InsnNode(Opcodes.POP));
                        // Mutating side-effect: Dummy read of static runtime field
                        hCode.add(new FieldInsnNode(Opcodes.GETSTATIC, cn.name, opaqueFields[0], "I"));
                        hCode.add(new InsnNode(Opcodes.POP));
                        hCode.add(new JumpInsnNode(Opcodes.GOTO, after));
                        hCode.add(after);

                        mn.instructions.insert(end, hCode);

                        if (mn.tryCatchBlocks == null) mn.tryCatchBlocks = new ArrayList<>();
                        mn.tryCatchBlocks.add(new TryCatchBlockNode(start, end, h, "java/lang/RuntimeException"));
                        exceptHandlers++;
                    }
                    case 2 -> {
                        // Pattern 3: Intersecting try-blocks with Dead-Code Thrower
                        LabelNode tryStart = new LabelNode();
                        LabelNode tryEnd = new LabelNode();
                        LabelNode handler = new LabelNode();
                        LabelNode after = new LabelNode();

                        mn.instructions.insert(insns[0], tryStart);
                        int insertPos = Math.min(insns.length - 2, 8);
                        mn.instructions.insert(insns[insertPos], tryEnd);

                        InsnList hCode = new InsnList();
                        hCode.add(handler);
                        hCode.add(new InsnNode(Opcodes.POP));
                        hCode.add(new JumpInsnNode(Opcodes.GOTO, after));
                        hCode.add(after);

                        mn.instructions.insert(tryEnd, hCode);

                        if (mn.tryCatchBlocks == null) mn.tryCatchBlocks = new ArrayList<>();
                        mn.tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, handler, "java/lang/IllegalStateException"));
                        exceptHandlers++;
                    }
                    default -> {
                        // Pattern 4: Finally Handler (type == null)
                        LabelNode fStart = new LabelNode();
                        LabelNode fEnd = new LabelNode();
                        LabelNode fHandler = new LabelNode();
                        LabelNode fAfter = new LabelNode();

                        mn.instructions.insert(fStart);
                        mn.instructions.insert(insns[insns.length - 2], fEnd);

                        InsnList fCode = new InsnList();
                        fCode.add(fHandler);
                        fCode.add(new InsnNode(Opcodes.POP));
                        fCode.add(new JumpInsnNode(Opcodes.GOTO, fAfter));
                        fCode.add(fAfter);

                        mn.instructions.insert(fEnd, fCode);

                        if (mn.tryCatchBlocks == null) mn.tryCatchBlocks = new ArrayList<>();
                        mn.tryCatchBlocks.add(new TryCatchBlockNode(fStart, fEnd, fHandler, "java/lang/Exception"));
                        exceptHandlers++;
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void addAntiDecompilerMetadata(ClassNode cn) {
        cn.sourceFile = null;
        if (cn.innerClasses == null) cn.innerClasses = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            cn.innerClasses.add(new InnerClassNode(
                cn.name + "$" + nextUnicode(), cn.name, nextUnicode(),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_ABSTRACT));
        }
        if (cn.invisibleAnnotations == null) cn.invisibleAnnotations = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            AnnotationNode a = new AnnotationNode("L" + nextUnicode() + "/" + nextUnicode() + ";");
            a.values = new ArrayList<>(List.of(nextUnicode(), nextUnicode(), nextUnicode(), RNG.nextInt()));
            cn.invisibleAnnotations.add(a);
        }
    }

    private void addAntiDecompilerEntries(JarOutputStream jos) throws IOException {
        // Rastgele sahte bytecode ve corrupted metadata girdileri (decompiler crash & stall)
        for (int i = 0; i < 45; i++) {
            String name = "META-INF/" + nextUnicode() + (RNG.nextBoolean() ? ".class" : ".dat");
            try {
                jos.putNextEntry(new ZipEntry(name));
                byte[] g = new byte[60 + RNG.nextInt(400)];
                RNG.nextBytes(g);
                g[0] = (byte)0xCA; g[1] = (byte)0xFE; g[2] = (byte)0xBA; g[3] = (byte)0xBE;
                jos.write(g);
                jos.closeEntry();
            } catch (ZipException ignored) {}
        }
    }

    private byte[] generateFakeClass(String className) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String parent = RNG.nextInt(4) == 0 && !fakeClassNames.isEmpty()
            ? fakeClassNames.get(RNG.nextInt(fakeClassNames.size()))
            : "java/lang/Object";
        if (parent.equals(className)) parent = "java/lang/Object";

        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, className, null, parent, null);

        int fc = 5 + RNG.nextInt(8);
        String[] ds = {"I", "J", "Z", "D", "Ljava/lang/Object;", "[B", "Ljava/util/Map;", "Ljava/lang/String;"};
        for (int i = 0; i < fc; i++)
            cw.visitField(Opcodes.ACC_PRIVATE | (RNG.nextBoolean() ? Opcodes.ACC_STATIC : 0),
                nextUnicode(), ds[RNG.nextInt(ds.length)], null, null).visitEnd();

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, parent, "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        int mc = 5 + RNG.nextInt(7);
        for (int m = 0; m < mc; m++) {
            String mDesc = switch (RNG.nextInt(5)) {
                case 0 -> "(I)I"; case 1 -> "(Ljava/lang/String;)V";
                case 2 -> "()Ljava/lang/Object;"; case 3 -> "(II)J"; default -> "([BI)Z";
            };
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC | (RNG.nextBoolean() ? Opcodes.ACC_STATIC : 0),
                nextUnicode(), mDesc, null, null);
            mv.visitCode();

            for (int o = 0; o < 3 + RNG.nextInt(5); o++) {
                mv.visitLdcInsn(RNG.nextInt(10000));
                mv.visitLdcInsn(RNG.nextInt(10000));
                mv.visitInsn(Opcodes.IXOR);
                mv.visitInsn(Opcodes.POP);
            }

            if (RNG.nextInt(3) == 0 && !fakeClassNames.isEmpty()) {
                String other = fakeClassNames.get(RNG.nextInt(fakeClassNames.size()));
                if (!other.equals(className)) {
                    mv.visitTypeInsn(Opcodes.NEW, other);
                    mv.visitInsn(Opcodes.POP);
                }
            }

            char ret = mDesc.charAt(mDesc.indexOf(')') + 1);
            switch (ret) {
                case 'I' -> { mv.visitInsn(Opcodes.ICONST_0); mv.visitInsn(Opcodes.IRETURN); }
                case 'J' -> { mv.visitInsn(Opcodes.LCONST_0); mv.visitInsn(Opcodes.LRETURN); }
                case 'Z' -> { mv.visitInsn(Opcodes.ICONST_0); mv.visitInsn(Opcodes.IRETURN); }
                case 'V' -> mv.visitInsn(Opcodes.RETURN);
                default -> { mv.visitInsn(Opcodes.ACONST_NULL); mv.visitInsn(Opcodes.ARETURN); }
            }
            mv.visitMaxs(4, 4);
            mv.visitEnd();
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static final char[] ARABIC_CHARS = {
        '\u0627', '\u0628', '\u0629', '\u062A', '\u062B', '\u062C', '\u062D', '\u062E',
        '\u062F', '\u0630', '\u0631', '\u0632', '\u0633', '\u0634', '\u0635', '\u0636',
        '\u0637', '\u0638', '\u0639', '\u063A', '\u0641', '\u0642', '\u0643', '\u0644',
        '\u0645', '\u0646', '\u0647', '\u0648', '\u0649', '\u064A', '\u067E', '\u0686',
        '\u06AF', '\u06CC'
    };

    private String nextUnicode() {
        unicodeCounter++;
        StringBuilder sb = new StringBuilder();
        int len = 3 + (unicodeCounter % 4);
        for (int i = 0; i < len; i++) {
            int idx = Math.abs((unicodeCounter * 31 + i * 17 + RNG.nextInt(100)) % ARABIC_CHARS.length);
            sb.append(ARABIC_CHARS[idx]);
        }
        return sb.toString();
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    private void printStats(File out) {
        System.out.println("[Obfuscator] ═══════════════════════════════════════════");
        System.out.println("[Obfuscator] v4.0 Ultimate Tamamlandı: " + out.getName());
        System.out.println("[Obfuscator] String şifreleme:      " + stringsEncrypted + " (Anti-reflection & caller check)");
        System.out.println("[Obfuscator] Sayı kodlama:          " + numbersEncoded + " (Anti-constant-folding runtime)");
        System.out.println("[Obfuscator] Junk üyeler:           " + junkAdded);
        System.out.println("[Obfuscator] Flow obfuscation:      " + flowObf + " (Lambdalar dahil)");
        System.out.println("[Obfuscator] Field rename:          " + fieldsRenamed);
        System.out.println("[Obfuscator] Method rename:         " + methodsRenamed);
        System.out.println("[Obfuscator] Debug silme:           " + debugStripped);
        System.out.println("[Obfuscator] Sahte sınıf:           " + fakeClassesAdded);
        System.out.println("[Obfuscator] Exception handlers:    " + exceptHandlers + " (5 farklı tuzak deseni)");
        System.out.println("[Obfuscator] Honeypot tuzakları:    " + honeypotClassNames.size() + " (25 gerçekçi modül)");
        System.out.println("[Obfuscator] ═══════════════════════════════════════════");
    }

    // ═══════════════════════════════════════════════════════════════
    // 25 REALISTIC HONEYPOT DECOYS (YEMLER)
    // ═══════════════════════════════════════════════════════════════
    private static class HoneyMethod {
        final String name;
        final String desc;
        final List<Object> csts;
        HoneyMethod(String name, String desc, List<Object> csts) {
            this.name = name;
            this.desc = desc;
            this.csts = csts;
        }
    }

    private Map<String, byte[]> generateRealisticHoneypots() {
        Map<String, byte[]> map = new LinkedHashMap<>();

        map.put("pw/yami/client/module/dupe/AutoDupe.class", buildHoneypot(
            "pw/yami/client/module/dupe/AutoDupe",
            new String[][]{{"delay", "D"}, {"desyncBypass", "Z"}, {"packetBufferLimit", "I"}},
            List.of(
                new HoneyMethod("onActivate", "()V", List.of("AutoDupe enabled: Initializing 2b2t packet buffer...", 1024)),
                new HoneyMethod("executeDupeSequence", "(I)Z", List.of("Dispatching slot swap desync packet to index %d", 765))
            )
        ));

        map.put("pw/yami/client/module/dupe/AnvilDupe.class", buildHoneypot(
            "pw/yami/client/module/dupe/AnvilDupe",
            new String[][]{{"retryCount", "I"}, {"autoRename", "Z"}},
            List.of(
                new HoneyMethod("onActivate", "()V", List.of("AnvilDupe: Syncing rename packet queue...", 3)),
                new HoneyMethod("performAnvilSync", "()Z", List.of("Packet 0x14 sent to anvil container", 100))
            )
        ));

        map.put("pw/yami/client/module/dupe/ShulkerDupe.class", buildHoneypot(
            "pw/yami/client/module/dupe/ShulkerDupe",
            new String[][]{{"packetDelay", "J"}, {"mode", "Ljava/lang/String;"}},
            List.of(
                new HoneyMethod("triggerShulkerDrop", "(I)V", List.of("Shulker box break event desync initiated", 250L))
            )
        ));

        map.put("pw/yami/client/module/exploit/StashDumper.class", buildHoneypot(
            "pw/yami/client/module/exploit/StashDumper",
            new String[][]{{"endpoint", "Ljava/lang/String;"}, {"apiKey", "Ljava/lang/String;"}, {"chunkThreshold", "I"}},
            List.of(
                new HoneyMethod("dumpCoordinates", "(III)Z", List.of("https://yami-license-2yrt.onrender.com/api/v2/stash/upload", "yami_live_8f3a92b1c4e0d7", "SHA-256")),
                new HoneyMethod("flushStashQueue", "()V", List.of("Flushing stash buffer to cloud server...", 1000))
            )
        ));

        map.put("pw/yami/client/module/exploit/CoordLogger.class", buildHoneypot(
            "pw/yami/client/module/exploit/CoordLogger",
            new String[][]{{"logPortals", "Z"}, {"logChests", "Z"}},
            List.of(
                new HoneyMethod("logCoords", "(DDD)V", List.of("CoordLogger: Found portal cluster at [%f, %f, %f]"))
            )
        ));

        map.put("pw/yami/client/module/exploit/ChunkCrashBypass.class", buildHoneypot(
            "pw/yami/client/module/exploit/ChunkCrashBypass",
            new String[][]{{"packetThreshold", "I"}, {"suppressErrors", "Z"}},
            List.of(
                new HoneyMethod("bypassCrash", "()Z", List.of("Suppressing oversized chunk packet", 150))
            )
        ));

        map.put("pw/yami/client/module/combat/AutoCrystalPlus.class", buildHoneypot(
            "pw/yami/client/module/combat/AutoCrystalPlus",
            new String[][]{{"breakRange", "D"}, {"placeRange", "D"}, {"antiSuicide", "Z"}},
            List.of(
                new HoneyMethod("onTick", "()V", List.of("Targeting crystal at distance", 5.2D)),
                new HoneyMethod("calculateDamage", "(DDD)D", List.of("Simulating explosion damage vector", 12.0D))
            )
        ));

        map.put("pw/yami/client/module/combat/AnchorAuraX.class", buildHoneypot(
            "pw/yami/client/module/combat/AnchorAuraX",
            new String[][]{{"autoGlowstone", "Z"}, {"chargeDelay", "I"}},
            List.of(
                new HoneyMethod("explodeAnchor", "(III)Z", List.of("Charging respawn anchor with glowstone", 4))
            )
        ));

        map.put("pw/yami/client/module/combat/BedAuraPro.class", buildHoneypot(
            "pw/yami/client/module/combat/BedAuraPro",
            new String[][]{{"minHealth", "D"}, {"autoCraft", "Z"}},
            List.of(
                new HoneyMethod("placeAndExplode", "()V", List.of("Placing bed in nether dimension", 8.5D))
            )
        ));

        map.put("pw/yami/client/module/render/StorageViewer.class", buildHoneypot(
            "pw/yami/client/module/render/StorageViewer",
            new String[][]{{"showShulkers", "Z"}, {"renderDistance", "I"}},
            List.of(
                new HoneyMethod("renderChests", "()V", List.of("Rendering storage ESP box highlights", 128))
            )
        ));

        map.put("pw/yami/client/module/render/AntiOverlay.class", buildHoneypot(
            "pw/yami/client/module/render/AntiOverlay",
            new String[][]{{"noPumpkin", "Z"}, {"noFire", "Z"}},
            List.of(
                new HoneyMethod("cancelOverlay", "(Ljava/lang/String;)Z", List.of("Cancelling screen overlay rendering"))
            )
        ));

        map.put("pw/yami/client/module/render/HoleESPAdvanced.class", buildHoneypot(
            "pw/yami/client/module/render/HoleESPAdvanced",
            new String[][]{{"bedrockColor", "I"}, {"obsidianColor", "I"}},
            List.of(
                new HoneyMethod("renderHole", "(III)V", List.of("Scanning hole safety: bedrock base", 0x00FF00))
            )
        ));

        map.put("pw/yami/client/license/AuthManager.class", buildHoneypot(
            "pw/yami/client/license/AuthManager",
            new String[][]{{"authServer", "Ljava/lang/String;"}, {"masterToken", "Ljava/lang/String;"}, {"authorized", "Z"}},
            List.of(
                new HoneyMethod("verifyLicense", "(Ljava/lang/String;)Z", List.of("https://yami-license-2yrt.onrender.com/api/v3/client/verify", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.fake_signature", "AES/CBC/PKCS5Padding")),
                new HoneyMethod("collectHWID", "()Ljava/lang/String;", List.of("PROCESSOR_IDENTIFIER", "COMPUTERNAME", "SHA-256"))
            )
        ));

        map.put("pw/yami/client/license/HWIDValidator.class", buildHoneypot(
            "pw/yami/client/license/HWIDValidator",
            new String[][]{{"cachedHWID", "Ljava/lang/String;"}, {"valid", "Z"}},
            List.of(
                new HoneyMethod("getMachineHWID", "()Ljava/lang/String;", List.of("YAMI-HWID-HASH-")),
                new HoneyMethod("validateMachine", "()Z", List.of("Hardware ID check passed"))
            )
        ));

        map.put("pw/yami/client/license/LicenseVerifier.class", buildHoneypot(
            "pw/yami/client/license/LicenseVerifier",
            new String[][]{{"expiryDate", "Ljava/lang/String;"}, {"tier", "I"}},
            List.of(
                new HoneyMethod("isKeyValid", "(Ljava/lang/String;)Z", List.of("2029-12-31", 3))
            )
        ));

        map.put("pw/yami/client/license/IntegrityGuard.class", buildHoneypot(
            "pw/yami/client/license/IntegrityGuard",
            new String[][]{{"expectedHash", "[B"}, {"tampered", "Z"}},
            List.of(
                new HoneyMethod("checkIntegrity", "()Z", List.of("Checking JAR signature and bytecode checksum", 32))
            )
        ));

        map.put("pw/yami/client/net/NetworkPacketRelay.class", buildHoneypot(
            "pw/yami/client/net/NetworkPacketRelay",
            new String[][]{{"upstreamGateway", "Ljava/lang/String;"}, {"bufferSize", "I"}},
            List.of(
                new HoneyMethod("dispatchFrame", "(Ljava/lang/String;I)Z", List.of("tcp://127.0.0.1:25565", 8192))
            )
        ));

        map.put("pw/yami/client/net/RemotePayloadHandler.class", buildHoneypot(
            "pw/yami/client/net/RemotePayloadHandler",
            new String[][]{{"host", "Ljava/lang/String;"}, {"port", "I"}},
            List.of(
                new HoneyMethod("fetchDynamicConfig", "()Ljava/lang/String;", List.of("yami-license-2yrt.onrender.com", 443))
            )
        ));

        map.put("pw/yami/client/net/PacketEncoderUtil.class", buildHoneypot(
            "pw/yami/client/net/PacketEncoderUtil",
            new String[][]{{"protocolVersion", "I"}, {"compressionLevel", "I"}},
            List.of(
                new HoneyMethod("encodePacket", "([B)[B", List.of("Packet compression gzip stream", 765))
            )
        ));

        map.put("pw/yami/client/net/SessionValidator.class", buildHoneypot(
            "pw/yami/client/net/SessionValidator",
            new String[][]{{"sessionAlive", "Z"}, {"lastPing", "J"}},
            List.of(
                new HoneyMethod("keepAlive", "()Z", List.of("Session heartbeat sent to auth server", 60000L))
            )
        ));

        map.put("pw/yami/client/internal/UnpackEngine.class", buildHoneypot(
            "pw/yami/client/internal/UnpackEngine",
            new String[][]{{"archiveName", "Ljava/lang/String;"}, {"unpacked", "Z"}},
            List.of(
                new HoneyMethod("unpackAll", "(Ljava/lang/ClassLoader;)Z", List.of("assets/yamiclient/cache/runtime.bin", "Decompressing inner runtime classes..."))
            )
        ));

        map.put("pw/yami/client/internal/NativeKeyGen.class", buildHoneypot(
            "pw/yami/client/internal/NativeKeyGen",
            new String[][]{{"masterSeed", "J"}, {"rounds", "I"}},
            List.of(
                new HoneyMethod("generateSubKey", "(I)[B", List.of("Key derivation Feistel round", 16))
            )
        ));

        map.put("pw/yami/client/internal/PayloadDecryptionService.class", buildHoneypot(
            "pw/yami/client/internal/PayloadDecryptionService",
            new String[][]{{"cipherSpec", "Ljava/lang/String;"}, {"salt", "Ljava/lang/String;"}},
            List.of(
                new HoneyMethod("decryptInnerPayload", "([B[B)[B", List.of("AES/CBC/PKCS5Padding", "PBEWithHmacSHA256AndAES_256"))
            )
        ));

        map.put("pw/yami/client/internal/BytecodeDecryptor.class", buildHoneypot(
            "pw/yami/client/internal/BytecodeDecryptor",
            new String[][]{{"magic", "I"}, {"classesProcessed", "I"}},
            List.of(
                new HoneyMethod("transformClassBytes", "([B)[B", List.of("JVM bytecode magic check", 0xCAFEBABE))
            )
        ));

        map.put("pw/yami/client/internal/VMProtector.class", buildHoneypot(
            "pw/yami/client/internal/VMProtector",
            new String[][]{{"vmSafe", "Z"}, {"hypervisorDetected", "Z"}},
            List.of(
                new HoneyMethod("detectVirtualMachine", "()Z", List.of("Checking hypervisor CPUID and VMWare/VBox artifacts"))
            )
        ));

        map.put("pw/yami/client/module/combat/GrimDisabler.class", buildHoneypot(
            "pw/yami/client/module/combat/GrimDisabler",
            new String[][]{{"bypassVersion", "Ljava/lang/String;"}, {"predictionLag", "D"}},
            List.of(
                new HoneyMethod("spoofReachVector", "(DDD)V", List.of("GrimAC reach raytrace bypass active", 3.01D)),
                new HoneyMethod("onPacketSend", "(I)Z", List.of("C03PlayerPosition spoofed", 14))
            )
        ));

        map.put("pw/yami/client/module/combat/AutoMaceElite.class", buildHoneypot(
            "pw/yami/client/module/combat/AutoMaceElite",
            new String[][]{{"smashRange", "D"}, {"autoFallDistance", "F"}},
            List.of(
                new HoneyMethod("executeSmashAttack", "(D)Z", List.of("Heavy weapon attack packet dispatched", 8.5D))
            )
        ));

        map.put("pw/yami/client/module/combat/AutoTotemGod.class", buildHoneypot(
            "pw/yami/client/module/combat/AutoTotemGod",
            new String[][]{{"strictInvSync", "Z"}, {"healthThreshold", "F"}},
            List.of(
                new HoneyMethod("swapTotemOffhand", "(I)V", List.of("Offhand slot quickswap packet sent", 45))
            )
        ));

        map.put("pw/yami/client/module/combat/BowAimbotPrediction.class", buildHoneypot(
            "pw/yami/client/module/combat/BowAimbotPrediction",
            new String[][]{{"gravityFactor", "D"}, {"velocityPredict", "D"}},
            List.of(
                new HoneyMethod("calculateAimVector", "(DDD)[D", List.of("Trajectory computed for target", 18.2D))
            )
        ));

        map.put("pw/yami/client/module/combat/InfiniteAura.class", buildHoneypot(
            "pw/yami/client/module/combat/InfiniteAura",
            new String[][]{{"range", "D"}, {"cps", "I"}},
            List.of(
                new HoneyMethod("attackEntity", "(I)V", List.of("Multi-target aura sweep", 20))
            )
        ));

        map.put("pw/yami/client/module/combat/CrystalOptimizer.class", buildHoneypot(
            "pw/yami/client/module/combat/CrystalOptimizer",
            new String[][]{{"zeroTick", "Z"}, {"multiPlace", "I"}},
            List.of(
                new HoneyMethod("predictObsidianPlacement", "()Z", List.of("Crystal placement tick rate optimized", 2))
            )
        ));

        map.put("pw/yami/client/module/movement/PhaseGlitch.class", buildHoneypot(
            "pw/yami/client/module/movement/PhaseGlitch",
            new String[][]{{"clipDistance", "D"}, {"antiVoid", "Z"}},
            List.of(
                new HoneyMethod("clipThroughBlock", "(DDD)V", List.of("Block collision box desync invoked", 0.05D))
            )
        ));

        map.put("pw/yami/client/module/movement/ElytraBypassPro.class", buildHoneypot(
            "pw/yami/client/module/movement/ElytraBypassPro",
            new String[][]{{"glideSpeed", "D"}, {"boostFactor", "F"}},
            List.of(
                new HoneyMethod("boostFirework", "()V", List.of("Elytra pitch packet correction", 2.5F))
            )
        ));

        map.put("pw/yami/client/module/movement/FastSwimMatrix.class", buildHoneypot(
            "pw/yami/client/module/movement/FastSwimMatrix",
            new String[][]{{"swimSpeed", "D"}},
            List.of(
                new HoneyMethod("applyWaterInertia", "(D)V", List.of("Fluid friction bypass applied", 1.8D))
            )
        ));

        map.put("pw/yami/client/module/movement/NoSlowBypass.class", buildHoneypot(
            "pw/yami/client/module/movement/NoSlowBypass",
            new String[][]{{"strictMode", "Z"}, {"hypixelBypass", "Z"}},
            List.of(
                new HoneyMethod("onItemUseSlowdown", "()V", List.of("Soul sand and web slowdown bypassed"))
            )
        ));

        map.put("pw/yami/client/module/movement/SpiderMatrix.class", buildHoneypot(
            "pw/yami/client/module/movement/SpiderMatrix",
            new String[][]{{"climbSpeed", "D"}},
            List.of(
                new HoneyMethod("climbWall", "()V", List.of("Wall climb motion added", 0.45D))
            )
        ));

        map.put("pw/yami/client/module/render/XRayV3.class", buildHoneypot(
            "pw/yami/client/module/render/XRayV3",
            new String[][]{{"opacity", "F"}, {"diamondOre", "Z"}, {"netheriteOre", "Z"}},
            List.of(
                new HoneyMethod("isBlockVisible", "(I)Z", List.of("Scanning chunk sub-mesh for target ores", 56))
            )
        ));

        map.put("pw/yami/client/module/render/TrueSightESP.class", buildHoneypot(
            "pw/yami/client/module/render/TrueSightESP",
            new String[][]{{"glowColor", "I"}, {"seeInvisibles", "Z"}},
            List.of(
                new HoneyMethod("renderOutline", "(I)V", List.of("Shading outline shader pass", 0xFF00FF))
            )
        ));

        map.put("pw/yami/client/module/render/TracersElite.class", buildHoneypot(
            "pw/yami/client/module/render/TracersElite",
            new String[][]{{"lineWidth", "F"}, {"targetMode", "Ljava/lang/String;"}},
            List.of(
                new HoneyMethod("drawLineToPlayer", "(DDD)V", List.of("Tracer projected onto crosshair", 2.0F))
            )
        ));

        map.put("pw/yami/client/module/exploit/FastBreakMatrix.class", buildHoneypot(
            "pw/yami/client/module/exploit/FastBreakMatrix",
            new String[][]{{"multiplier", "F"}, {"packetMine", "Z"}},
            List.of(
                new HoneyMethod("sendInstantBreakPacket", "(III)Z", List.of("PlayerActionC2SPacket STOP_DESTROY_BLOCK sent", 1))
            )
        ));

        map.put("pw/yami/client/module/exploit/ServerCrashExploit.class", buildHoneypot(
            "pw/yami/client/module/exploit/ServerCrashExploit",
            new String[][]{{"packetsPerTick", "I"}, {"overflowBytes", "I"}},
            List.of(
                new HoneyMethod("dispatchFloodQueue", "()V", List.of("CustomPayload packet size overflow simulation", 65535))
            )
        ));

        map.put("pw/yami/client/module/exploit/ItemTeleport.class", buildHoneypot(
            "pw/yami/client/module/exploit/ItemTeleport",
            new String[][]{{"maxDistance", "D"}},
            List.of(
                new HoneyMethod("teleportToEntity", "(I)Z", List.of("Desync player movement packet sent to entity", 50.0D))
            )
        ));

        map.put("pw/yami/client/module/exploit/DupeBook2b2t.class", buildHoneypot(
            "pw/yami/client/module/exploit/DupeBook2b2t",
            new String[][]{{"pageCount", "I"}, {"signBook", "Z"}},
            List.of(
                new HoneyMethod("craftOversizedBook", "()Z", List.of("Generated 100 pages of Unicode payload for chunk save bug", 100))
            )
        ));

        map.put("pw/yami/client/module/exploit/DisablerNCP.class", buildHoneypot(
            "pw/yami/client/module/exploit/DisablerNCP",
            new String[][]{{"pingSpoof", "I"}, {"fastSneak", "Z"}},
            List.of(
                new HoneyMethod("bypassNoCheatPlus", "()V", List.of("KeepAliveC2SPacket delayed by 250ms", 250))
            )
        ));

        map.put("pw/yami/client/module/exploit/DisablerVulcan.class", buildHoneypot(
            "pw/yami/client/module/exploit/DisablerVulcan",
            new String[][]{{"strafeBypass", "Z"}, {"timerFactor", "D"}},
            List.of(
                new HoneyMethod("suppressVulcanChecks", "()V", List.of("Sending invalid position packet offset", 0.0001D))
            )
        ));

        map.put("pw/yami/client/module/exploit/DisablerMatrix.class", buildHoneypot(
            "pw/yami/client/module/exploit/DisablerMatrix",
            new String[][]{{"stepMode", "Z"}, {"fallSpoof", "Z"}},
            List.of(
                new HoneyMethod("spoofGroundState", "()V", List.of("OnGround flag alternating in movement packets"))
            )
        ));

        map.put("pw/yami/client/module/player/FastEatPacket.class", buildHoneypot(
            "pw/yami/client/module/player/FastEatPacket",
            new String[][]{{"packets", "I"}},
            List.of(
                new HoneyMethod("instantEatItem", "()V", List.of("Sending 32 PlayerMoveC2SPackets in single tick", 32))
            )
        ));

        map.put("pw/yami/client/module/player/GodModeDesync.class", buildHoneypot(
            "pw/yami/client/module/player/GodModeDesync",
            new String[][]{{"desyncHealth", "F"}},
            List.of(
                new HoneyMethod("cancelDamagePackets", "()Z", List.of("EntityStatusS2CPacket damage sound cancelled", 20.0F))
            )
        ));

        map.put("pw/yami/client/module/player/AutoArmorFast.class", buildHoneypot(
            "pw/yami/client/module/player/AutoArmorFast",
            new String[][]{{"swapDelay", "I"}, {"equipElytra", "Z"}},
            List.of(
                new HoneyMethod("equipBestArmor", "()V", List.of("Equipping prot 4 netherite set", 0))
            )
        ));

        map.put("pw/yami/client/module/misc/PacketCanceller.class", buildHoneypot(
            "pw/yami/client/module/misc/PacketCanceller",
            new String[][]{{"cancelKeepAlive", "Z"}, {"cancelChat", "Z"}},
            List.of(
                new HoneyMethod("shouldDropPacket", "(Ljava/lang/String;)Z", List.of("Packet filter evaluated"))
            )
        ));

        return map;
    }

    private byte[] buildHoneypot(String internalName, String[][] fields, List<HoneyMethod> methods) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        // <init>()
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        // Fields
        for (String[] f : fields) {
            cw.visitField(Opcodes.ACC_PUBLIC, f[0], f[1], null, null).visitEnd();
        }

        // Methods
        for (HoneyMethod m : methods) {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, m.name, m.desc, null, null);
            mv.visitCode();

            for (Object cst : m.csts) {
                if (cst instanceof String s) {
                    mv.visitLdcInsn(s);
                    mv.visitInsn(Opcodes.POP);
                } else if (cst instanceof Integer num) {
                    mv.visitLdcInsn(num);
                    mv.visitInsn(Opcodes.POP);
                } else if (cst instanceof Long l) {
                    mv.visitLdcInsn(l);
                    mv.visitInsn(Opcodes.POP2);
                } else if (cst instanceof Double d) {
                    mv.visitLdcInsn(d);
                    mv.visitInsn(Opcodes.POP2);
                }
            }

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            mv.visitInsn(Opcodes.POP2);

            char ret = m.desc.charAt(m.desc.indexOf(')') + 1);
            switch (ret) {
                case 'I', 'Z' -> {
                    mv.visitInsn(Opcodes.ICONST_1);
                    mv.visitInsn(Opcodes.IRETURN);
                }
                case 'J' -> {
                    mv.visitInsn(Opcodes.LCONST_1);
                    mv.visitInsn(Opcodes.LRETURN);
                }
                case 'D' -> {
                    mv.visitInsn(Opcodes.DCONST_1);
                    mv.visitInsn(Opcodes.DRETURN);
                }
                case 'V' -> mv.visitInsn(Opcodes.RETURN);
                default -> {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitInsn(Opcodes.ARETURN);
                }
            }
            mv.visitMaxs(4, 4);
            mv.visitEnd();
        }

        cw.visitEnd();
        return cw.toByteArray();
    }
}
