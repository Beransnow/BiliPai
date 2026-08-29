# === BiliPai ProGuard Rules ===
# Fixes: java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType

# --- General ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses,EnclosingMethod
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**

# === 优化选项 ===
# R8/AGP defaults are intentionally used here. The old global optimization
# exclusions disabled arithmetic/cast/field/class-merging optimizations for the
# entire app, even though the historical regressions were isolated to specific
# Compose/View entrypoints below.
-allowaccessmodification

# Retrofit, OkHttp and kotlinx.serialization ship consumer rules. Their
# packages and all application DTOs must not be blanket-kept here; generated
# adapters and directly referenced APIs remain reachable to R8 normally.
-dontwarn okhttp3.**
-dontwarn okio.**

# Navigation3 / Miuix 的预编译代码会直接调用本地 vendored 的
# NavigationEventHandlerKt.NavigationBackHandler。该顶层类及其重载不能被
# R8 改名或裁剪，否则发布包会抛出 NoSuchMethodError。
-keep class androidx.navigationevent.compose.** { *; }
-keep class androidx.navigationevent.** { *; }

# Android 16 ART can reject the heavily optimized dex for the large Compose
# VideoDetailScreen entrypoint. Keep this class unoptimized while preserving
# R8 for the rest of the release build.
-keep class com.android.purebilibili.feature.video.screen.VideoDetailScreenKt { *; }
-keep class com.android.purebilibili.feature.video.screen.VideoDetailScreenKt$* { *; }

# Release-only player overlay regressions are hard to diagnose because gestures
# can keep working while Compose control layers stop rendering. Keep the
# player section and overlay classes out of R8 optimization; this preserves
# minification for the rest of the app while protecting the control UI path.
-keep class com.android.purebilibili.feature.video.ui.section.** { *; }
-keep class com.android.purebilibili.feature.video.ui.overlay.** { *; }

# Release 下底栏搜索入口曾出现点击无响应，只在正式版复现。
# 保留底栏搜索、搜索页入口和导航交接相关的 Compose 函数及合成 lambda，
# 避免 R8 优化破坏点击、展开、焦点和搜索页入场链路。
-keep class com.android.purebilibili.feature.home.components.BottomBarKt { *; }
-keep class com.android.purebilibili.feature.home.components.BottomBarKt$* { *; }
-keep class com.android.purebilibili.feature.search.SearchScreenKt { *; }
-keep class com.android.purebilibili.feature.search.SearchScreenKt$* { *; }
-keep class com.android.purebilibili.feature.search.SearchEntryMotionSource { *; }
-keep class com.android.purebilibili.navigation.AppNavigationKt { *; }
-keep class com.android.purebilibili.navigation.AppNavigationKt$* { *; }

# === 首页视频卡片（圆角/封面裁剪/R8 下曾出现直角回归） ===
-keep class com.android.purebilibili.feature.home.components.cards.** { *; }
-keep class com.android.purebilibili.feature.home.HomeGlassVisualPolicyKt { *; }

# === 主题/圆角缩放（LocalCornerRadiusScale、UiPreset 枚举、resolveCornerRadiusScale） ===
-keep class com.android.purebilibili.core.theme.**Kt { *; }
-keep enum com.android.purebilibili.core.theme.UiPreset { *; }
-keep enum com.android.purebilibili.core.theme.AndroidNativeVariant { *; }

# === 共享元素过渡 data class（用作 remember key，R8 优化会破坏 equals）===
-keep,allowobfuscation,allowshrinking class com.android.purebilibili.core.ui.transition.** { *; }

# Third-party libraries below provide their own consumer rules. Keep only
# warnings for optional classes; blanket package keeps prevent R8 from pruning
# unused implementations and resources reachable through those libraries.
-dontwarn dev.chrisbanes.haze.**
-dontwarn io.github.alexzhirkevich.cupertino.**
-dontwarn androidx.room.paging.**
-dontwarn androidx.media3.**
-dontwarn coil.**
-dontwarn com.google.zxing.**
-dontwarn org.fourthline.cling.**
-dontwarn javax.enterprise.context.**
-dontwarn javax.inject.**
-dontwarn org.seamless.**

# === Login / Geetest WebView bridge ===
# Release-only SMS login failures can stem from R8 renaming @JavascriptInterface
# methods or shrinking login FieldMap helpers.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
