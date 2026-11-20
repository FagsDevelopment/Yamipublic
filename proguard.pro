# ===========================================================================
# ProGuard Configuration for Yami Fabric Client (Minecraft 1.21.1)
# ===========================================================================

# Diagnostics / Warnings
-dontwarn **
-dontnote **
-ignorewarnings

# Only perform Obfuscation (keep shrinking & optimization off to avoid breaking Fabric reflection)
-dontshrink
-dontoptimize

# Obfuscation settings with Arabic dictionary
-obfuscationdictionary 'arabic-dictionary.txt'
-classobfuscationdictionary 'arabic-dictionary.txt'
-packageobfuscationdictionary 'arabic-dictionary.txt'
-repackageclasses 'pw.yami.client.ن'
-allowaccessmodification
-overloadaggressively
-useuniqueclassmembernames
-renamesourcefileattribute 'الله.java'
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

# ---------------------------------------------------------------------------
# Fabric Loader & Entrypoints (Must remain intact for Fabric Loader to find them)
# ---------------------------------------------------------------------------
-keep class * implements net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint {
    public *;
}
-keep class * implements net.fabricmc.api.ModInitializer {
    public *;
}
-keep class * implements net.fabricmc.api.ClientModInitializer {
    public *;
}

# Main client entrypoint & bootstrap
-keep class pw.yami.client.Bootstrap {
    public *;
}
-keep class pw.yami.client.Yami {
    public *;
}

# ---------------------------------------------------------------------------
# Mixins (Mixin classes must keep exact names & methods for Mixin engine)
# ---------------------------------------------------------------------------
-keep class pw.yami.client.mixin.** {
    *;
}
-keepclassmembers class pw.yami.client.mixin.** {
    *;
}

# ---------------------------------------------------------------------------
# License Launchers & Gson Serialization
# ---------------------------------------------------------------------------
-keep class pw.yami.client.license.BackendLauncher {
    public *;
}
-keep class pw.yami.client.license.UpdateApplier {
    public *;
}
-keepclassmembers class pw.yami.client.license.LicenseConfig {
    public *;
}
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName *;
}
