package com.android.purebilibili.feature.anime4k

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.min
import kotlin.math.roundToInt

/** 画质增强的用户可持久化配置。插件总开关由 PluginManager 单独管理。 */
@Serializable
data class Anime4KConfig(
    val algorithm: VideoEnhancementAlgorithm = VideoEnhancementAlgorithm.ANIME4K,
    val preset: Anime4KPreset = Anime4KPreset.FAST,
    /** 面向用户的 0～1 锐化强度，内部会转换为 RCAS 的反向 stop 参数。 */
    val fsrSharpness: Float = DEFAULT_FSR_SHARPNESS,
    val rememberAcrossVideos: Boolean = false,
    val rememberedEnabled: Boolean = false
)

@Serializable
enum class VideoEnhancementAlgorithm {
    ANIME4K,
    FSR_1_0
}

@Serializable
enum class Anime4KPreset {
    FAST,
    QUALITY
}

const val DEFAULT_FSR_SHARPNESS: Float = 0.9f

/** AMD RCAS 的 0 表示最强，数值每增加 1 表示锐度减半。 */
fun resolveFsrRcasSharpnessStops(strength: Float): Float {
    return 2f * (1f - strength.coerceIn(0f, 1f))
}

/** FSR 1.0 官方建议的最大 4 倍面积缩放，即每条边最多 2 倍。 */
fun resolveFsr1TargetSize(
    sourceWidth: Int,
    sourceHeight: Int,
    requestedWidth: Int,
    requestedHeight: Int,
    glMaxTextureSize: Int
): Pair<Int, Int> {
    if (
        sourceWidth <= 0 || sourceHeight <= 0 ||
        requestedWidth <= 0 || requestedHeight <= 0 ||
        glMaxTextureSize <= 0
    ) {
        return 1 to 1
    }
    val requestedScale = min(
        requestedWidth.toFloat() / sourceWidth,
        requestedHeight.toFloat() / sourceHeight
    )
    val textureScale = min(
        glMaxTextureSize.toFloat() / sourceWidth,
        glMaxTextureSize.toFloat() / sourceHeight
    )
    val scale = min(requestedScale.coerceAtLeast(1f), min(2f, textureScale))
        .coerceAtLeast(min(1f, textureScale))
    return (sourceWidth * scale).roundToInt().coerceAtLeast(1) to
        (sourceHeight * scale).roundToInt().coerceAtLeast(1)
}

private val videoEnhancementJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun encodeVideoEnhancementConfig(config: Anime4KConfig): String {
    return videoEnhancementJson.encodeToString(config)
}

internal fun decodeVideoEnhancementConfig(raw: String): Anime4KConfig {
    val storedPreset = runCatching {
        videoEnhancementJson.parseToJsonElement(raw)
            .jsonObject["preset"]
            ?.jsonPrimitive
            ?.content
    }.getOrNull()
    if (storedPreset == "BALANCED") {
        // 旧中间档与 Kazumi 效率链相同，升级后继续归并到效率档。
        return Anime4KConfig(preset = Anime4KPreset.FAST)
    }
    return videoEnhancementJson.decodeFromString(raw)
}

/** 与 Kazumi 一致，只提供效率和质量两档。 */
fun resolveAnime4KPresetLabel(preset: Anime4KPreset): String {
    return when (preset) {
        Anime4KPreset.FAST -> "效率档"
        Anime4KPreset.QUALITY -> "质量档"
    }
}

enum class Anime4KShaderChain {
    KAZUMI_EFFICIENCY,
    KAZUMI_QUALITY
}

/** 渲染器使用的非持久化 CNN 预设参数。 */
data class Anime4KRenderProfile(
    val shaderChain: Anime4KShaderChain
)

fun resolveAnime4KRenderProfile(preset: Anime4KPreset): Anime4KRenderProfile {
    return when (preset) {
        Anime4KPreset.FAST -> Anime4KRenderProfile(
            shaderChain = Anime4KShaderChain.KAZUMI_EFFICIENCY
        )

        Anime4KPreset.QUALITY -> Anime4KRenderProfile(
            shaderChain = Anime4KShaderChain.KAZUMI_QUALITY
        )
    }
}

/**
 * Kazumi 会让 shader 直接处理视频原始尺寸。这里只在输入超过 GPU 纹理上限时等比缩小，
 * 不能按性能档主动压低 720P/1080P，否则 CNN 会放大已经丢失的细节并产生涂抹感。
 */
fun resolveAnime4KInputSize(
    inputWidth: Int,
    inputHeight: Int,
    glMaxTextureSize: Int
): Pair<Int, Int> {
    if (inputWidth <= 0 || inputHeight <= 0 || glMaxTextureSize <= 0) return 1 to 1
    val longEdge = maxOf(inputWidth, inputHeight)
    if (longEdge <= glMaxTextureSize) return inputWidth to inputHeight

    val clampScale = glMaxTextureSize.toFloat() / longEdge.toFloat()
    return (inputWidth * clampScale).roundToInt().coerceAtLeast(1) to
        (inputHeight * clampScale).roundToInt().coerceAtLeast(1)
}
