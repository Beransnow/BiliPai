# BiliPai release R8 configuration.
# Keep the configuration intentionally minimal so R8 can shrink, optimize and
# obfuscate all statically reachable app and library code.

# Runtime reflection metadata used by Retrofit/kotlinx.serialization and
# framework callbacks. These preserve metadata only; they do not keep classes.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# Optional classes referenced by third-party libraries on specific code paths.
# Consumer rules supplied by each dependency remain authoritative.
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn dev.chrisbanes.haze.**
-dontwarn io.github.alexzhirkevich.cupertino.**
-dontwarn androidx.room.paging.**
-dontwarn androidx.media3.**
-dontwarn coil3.**
-dontwarn com.google.zxing.**
-dontwarn org.fourthline.cling.**
-dontwarn javax.enterprise.context.**
-dontwarn javax.inject.**
-dontwarn org.seamless.**

# WebView JavaScript bridges are discovered by annotation at runtime.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# R8 9.3.1 can merge the large Compose-generated DynamicCardV2 method and
# unrelated static members into Play Services' Dynamite loader classes. The
# resulting register typing is rejected by Android 17 at startup (VerifyError:
# Boolean where an Integer is expected). Keep these two narrowly scoped class
# families out of optimization/class merging while still allowing obfuscation.
-keep,allowobfuscation class com.android.purebilibili.feature.dynamic.components.DynamicCardKt { *; }
-keep,allowobfuscation class com.google.android.gms.dynamite.** { *; }
