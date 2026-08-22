package com.android.purebilibili.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.purebilibili.R
import com.android.purebilibili.core.store.LiquidGlassAdvancedPreset
import com.android.purebilibili.core.store.LiquidGlassAdvancedSettings
import com.android.purebilibili.core.store.LiquidGlassMode
import com.android.purebilibili.core.store.resolveLiquidGlassAdvancedPreset
import com.android.purebilibili.core.ui.components.AppSlider
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.feature.home.components.biliPaiFloatingDockShell
import com.android.purebilibili.feature.home.components.resolveLiquidGlassTuning
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@Composable
internal fun LiquidGlassAdjustmentPanel(
    persistedProgress: Float,
    previewImageUri: String?,
    persistedAdvancedSettings: LiquidGlassAdvancedSettings,
    onProgressCommitted: (Float) -> Unit,
    onPreviewImageChanged: (String?) -> Unit,
    onAdvancedSettingsCommitted: (LiquidGlassAdvancedSettings) -> Unit,
    onShareSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val previewImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        onPreviewImageChanged(uri.toString())
    }
    var previewProgress by remember(persistedProgress) {
        mutableFloatStateOf(persistedProgress.coerceIn(0f, 1f))
    }
    var advancedSettings by remember(persistedAdvancedSettings) {
        mutableStateOf(persistedAdvancedSettings)
    }
    var presetSliderValue by remember(persistedAdvancedSettings) {
        mutableFloatStateOf(liquidGlassPresetSliderValue(persistedAdvancedSettings))
    }
    var advancedSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    val tuning = remember(previewProgress, advancedSettings) {
        resolveLiquidGlassTuning(previewProgress, advancedSettings)
    }
    val modeLabel = when (tuning.mode) {
        LiquidGlassMode.CLEAR -> "通透"
        LiquidGlassMode.BALANCED -> "平衡"
        LiquidGlassMode.FROSTED -> "磨砂"
    }
    val percentage = (previewProgress * 100f).roundToInt()

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                AppText(
                    text = "液态玻璃质感",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                AppText(
                    text = "顶部栏、搜索框、选择控件和底栏统一生效",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppText(
                text = "$modeLabel · $percentage%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        LiquidGlassHomeSample(
            progress = previewProgress,
            previewImageUri = previewImageUri,
            advancedSettings = advancedSettings,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTextButton(
                onClick = { previewImagePicker.launch(arrayOf("image/*")) },
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                AppText(if (previewImageUri == null) "选择相册图片" else "更换图片")
            }
            if (previewImageUri != null) {
                AppTextButton(onClick = { onPreviewImageChanged(null) }) {
                    Icon(Icons.Outlined.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    AppText("恢复默认")
                }
            }
        }
        AppText(
            text = if (previewImageUri == null) {
                "可在预览图中上下拖动背景；调节滑杆时图片与玻璃效果实时跟随。"
            } else {
                "所选图片仅用于本页预览；可上下拖动背景，滑杆效果实时跟随。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = "效果预设",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
            AppText(
                text = if (advancedSettings.preset == LiquidGlassAdvancedPreset.CUSTOM) {
                    "自定 · ${(presetSliderValue * 100f).roundToInt()}%"
                } else {
                    advancedSettings.preset.label
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        AppSlider(
            value = presetSliderValue,
            onValueChange = { value ->
                presetSliderValue = value.coerceIn(0f, 1f)
                advancedSettings = resolveLiquidGlassPresetSliderSettings(value)
            },
            onValueChangeFinished = {
                onAdvancedSettingsCommitted(advancedSettings)
            },
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "液态玻璃效果预设"
                    stateDescription = if (
                        advancedSettings.preset == LiquidGlassAdvancedPreset.CUSTOM
                    ) {
                        "自定 ${(presetSliderValue * 100f).roundToInt()}%"
                    } else {
                        advancedSettings.preset.label
                    }
                },
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            LIQUID_GLASS_PRESET_SLIDER_ANCHORS.forEach { preset ->
                AppText(
                    text = preset.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (preset == advancedSettings.preset) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    color = if (preset == advancedSettings.preset) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        AppText(
            text = when (advancedSettings.preset) {
                LiquidGlassAdvancedPreset.READABLE -> "清晰：关闭内容扭曲，优先保证文字和图标正常显示"
                LiquidGlassAdvancedPreset.BALANCED -> "均衡：保持 BiliPai 默认质感"
                LiquidGlassAdvancedPreset.PRISM -> "棱镜：强化色散与内容折射"
                LiquidGlassAdvancedPreset.CUSTOM -> "自定：使用下方高级参数"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppTextButton(
                onClick = { advancedSettingsExpanded = !advancedSettingsExpanded },
            ) {
                Icon(Icons.Outlined.Tune, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                AppText(if (advancedSettingsExpanded) "收起高级参数" else "高级参数")
            }
            AppTextButton(onClick = onShareSettings) {
                Icon(Icons.Outlined.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                AppText("一键分享设置")
            }
        }
        AnimatedVisibility(
            visible = advancedSettingsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LiquidGlassAdvancedSlider(
                    title = "内容可读性",
                    description = "通透度越高，越主动保护图标和文字对比度",
                    value = advancedSettings.contentReadability,
                    onValueChange = { value ->
                        val updatedSettings = advancedSettings.copy(
                            preset = LiquidGlassAdvancedPreset.CUSTOM,
                            contentReadability = value,
                        )
                        advancedSettings = updatedSettings
                        presetSliderValue = liquidGlassPresetSliderValue(updatedSettings)
                    },
                    onValueChangeFinished = {
                        onAdvancedSettingsCommitted(advancedSettings)
                    },
                )
                LiquidGlassAdvancedSlider(
                    title = "色散强度",
                    description = "控制玻璃边缘的彩色分离效果",
                    value = advancedSettings.chromaticAberration,
                    onValueChange = { value ->
                        val updatedSettings = advancedSettings.copy(
                            preset = LiquidGlassAdvancedPreset.CUSTOM,
                            chromaticAberration = value,
                        )
                        advancedSettings = updatedSettings
                        presetSliderValue = liquidGlassPresetSliderValue(updatedSettings)
                    },
                    onValueChangeFinished = {
                        onAdvancedSettingsCommitted(advancedSettings)
                    },
                )
                LiquidGlassAdvancedSlider(
                    title = "文字与图标扭曲",
                    description = "调至 0% 可完全关闭折射，让文字和图标正常显示",
                    value = advancedSettings.contentDistortion,
                    valueText = if (advancedSettings.contentDistortion <= 0.001f) {
                        "关闭"
                    } else {
                        "${(advancedSettings.contentDistortion * 100f).roundToInt()}%"
                    },
                    onValueChange = { value ->
                        val updatedSettings = advancedSettings.copy(
                            preset = LiquidGlassAdvancedPreset.CUSTOM,
                            contentDistortion = value,
                        )
                        advancedSettings = updatedSettings
                        presetSliderValue = liquidGlassPresetSliderValue(updatedSettings)
                    },
                    onValueChangeFinished = {
                        onAdvancedSettingsCommitted(advancedSettings)
                    },
                )
                AppTextButton(
                    onClick = {
                        val updatedSettings = advancedSettings.copy(
                            preset = LiquidGlassAdvancedPreset.CUSTOM,
                            contentDistortion = 0f,
                        )
                        advancedSettings = updatedSettings
                        presetSliderValue = liquidGlassPresetSliderValue(updatedSettings)
                        onAdvancedSettingsCommitted(updatedSettings)
                    },
                    enabled = advancedSettings.contentDistortion > 0.001f,
                ) {
                    Icon(Icons.Outlined.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    AppText("完全关闭文字扭曲")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = "通透",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppSlider(
                value = previewProgress,
                onValueChange = { previewProgress = it.coerceIn(0f, 1f) },
                onValueChangeFinished = { onProgressCommitted(previewProgress) },
                valueRange = 0f..1f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .semantics {
                        contentDescription = "液态玻璃质感"
                        stateDescription = "$modeLabel，$percentage%"
                    },
            )
            AppText(
                text = "磨砂",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AppText(
            text = "拖动时仅实时更新预览，松手后保存；50% 为原有 BiliPai 默认效果。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val LIQUID_GLASS_PRESET_BALANCED_POSITION = 0.5f
private const val LIQUID_GLASS_PRESET_ANCHOR_EPSILON = 0.001f
private val LIQUID_GLASS_PRESET_SLIDER_ANCHORS = listOf(
    LiquidGlassAdvancedPreset.READABLE,
    LiquidGlassAdvancedPreset.BALANCED,
    LiquidGlassAdvancedPreset.PRISM,
)

internal fun resolveLiquidGlassPresetSliderSettings(
    value: Float,
): LiquidGlassAdvancedSettings {
    val position = value.coerceIn(0f, 1f)
    val readable = resolveLiquidGlassAdvancedPreset(LiquidGlassAdvancedPreset.READABLE)
    val balanced = resolveLiquidGlassAdvancedPreset(LiquidGlassAdvancedPreset.BALANCED)
    val prism = resolveLiquidGlassAdvancedPreset(LiquidGlassAdvancedPreset.PRISM)
    val (start, end, fraction) = if (position <= LIQUID_GLASS_PRESET_BALANCED_POSITION) {
        Triple(readable, balanced, position / LIQUID_GLASS_PRESET_BALANCED_POSITION)
    } else {
        Triple(
            balanced,
            prism,
            (position - LIQUID_GLASS_PRESET_BALANCED_POSITION) /
                LIQUID_GLASS_PRESET_BALANCED_POSITION,
        )
    }
    val preset = when {
        position <= LIQUID_GLASS_PRESET_ANCHOR_EPSILON ->
            LiquidGlassAdvancedPreset.READABLE
        abs(position - LIQUID_GLASS_PRESET_BALANCED_POSITION) <=
            LIQUID_GLASS_PRESET_ANCHOR_EPSILON -> LiquidGlassAdvancedPreset.BALANCED
        position >= 1f - LIQUID_GLASS_PRESET_ANCHOR_EPSILON ->
            LiquidGlassAdvancedPreset.PRISM
        else -> LiquidGlassAdvancedPreset.CUSTOM
    }
    return LiquidGlassAdvancedSettings(
        preset = preset,
        contentReadability = lerpLiquidGlassPresetValue(
            start.contentReadability,
            end.contentReadability,
            fraction,
        ),
        chromaticAberration = lerpLiquidGlassPresetValue(
            start.chromaticAberration,
            end.chromaticAberration,
            fraction,
        ),
        contentDistortion = lerpLiquidGlassPresetValue(
            start.contentDistortion,
            end.contentDistortion,
            fraction,
        ),
    )
}

internal fun liquidGlassPresetSliderValue(settings: LiquidGlassAdvancedSettings): Float =
    when (settings.preset) {
        LiquidGlassAdvancedPreset.READABLE -> 0f
        LiquidGlassAdvancedPreset.BALANCED -> LIQUID_GLASS_PRESET_BALANCED_POSITION
        LiquidGlassAdvancedPreset.PRISM -> 1f
        LiquidGlassAdvancedPreset.CUSTOM -> {
            val readableChromatic = resolveLiquidGlassAdvancedPreset(
                LiquidGlassAdvancedPreset.READABLE
            ).chromaticAberration
            val balancedChromatic = resolveLiquidGlassAdvancedPreset(
                LiquidGlassAdvancedPreset.BALANCED
            ).chromaticAberration
            val prismChromatic = resolveLiquidGlassAdvancedPreset(
                LiquidGlassAdvancedPreset.PRISM
            ).chromaticAberration
            if (settings.chromaticAberration <= balancedChromatic) {
                val fraction = (settings.chromaticAberration - readableChromatic) /
                    (balancedChromatic - readableChromatic)
                fraction.coerceIn(0f, 1f) * LIQUID_GLASS_PRESET_BALANCED_POSITION
            } else {
                val fraction = (settings.chromaticAberration - balancedChromatic) /
                    (prismChromatic - balancedChromatic)
                LIQUID_GLASS_PRESET_BALANCED_POSITION +
                    fraction.coerceIn(0f, 1f) * LIQUID_GLASS_PRESET_BALANCED_POSITION
            }
        }
    }

private fun lerpLiquidGlassPresetValue(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction.coerceIn(0f, 1f)

@Composable
private fun LiquidGlassHomeSample(
    progress: Float,
    previewImageUri: String?,
    advancedSettings: LiquidGlassAdvancedSettings,
    modifier: Modifier = Modifier,
) {
    val backdrop = rememberLayerBackdrop()
    val tuning = remember(progress, advancedSettings) {
        resolveLiquidGlassTuning(progress, advancedSettings)
    }
    val sampleShape = RoundedCornerShape(24.dp)
    val glassColor = MaterialTheme.colorScheme.surfaceContainer
    val contentColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val previewPanLimitPx = remember(density) { with(density) { 280.dp.toPx() } }
    val sliderFollowRangePx = remember(density) { with(density) { 80.dp.toPx() } }
    var customImageFailed by remember(previewImageUri) { mutableStateOf(false) }
    var previewPanOffsetPx by remember(previewImageUri) { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .height(360.dp)
            .clip(sampleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(previewImageUri, previewPanLimitPx) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    previewPanOffsetPx = (previewPanOffsetPx + dragAmount)
                        .coerceIn(-previewPanLimitPx, previewPanLimitPx)
                }
            }
            .semantics {
                contentDescription = "首页效果预览，可上下拖动图片"
                stateDescription = "图片位置 ${(previewPanOffsetPx / previewPanLimitPx * 100f).roundToInt()}%"
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .requiredHeight(920.dp)
                    .graphicsLayer {
                        val sliderFollowOffset = (progress - 0.5f) * sliderFollowRangePx
                        translationY = (previewPanOffsetPx + sliderFollowOffset)
                            .coerceIn(-previewPanLimitPx, previewPanLimitPx)
                    }
            ) {
                if (previewImageUri != null && !customImageFailed) {
                    AsyncImage(
                        model = previewImageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { customImageFailed = true },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.08f))
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.liquid_glass_preview_sky),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth()
                .height(40.dp)
                .biliPaiFloatingDockShell(
                    backdrop = backdrop,
                    containerColor = glassColor,
                    pressProgress = 0f,
                    shape = CircleShape,
                    liquidGlassTuning = tuning,
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.width(8.dp))
            AppText(
                text = "搜索感兴趣的视频",
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .height(48.dp)
                .biliPaiFloatingDockShell(
                    backdrop = backdrop,
                    containerColor = glassColor,
                    pressProgress = 0f,
                    shape = CircleShape,
                    liquidGlassTuning = tuning,
                )
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Icon(Icons.Outlined.DynamicFeed, contentDescription = null, tint = contentColor)
            Icon(Icons.Outlined.Person, contentDescription = null, tint = contentColor)
        }
    }
}

@Composable
private fun LiquidGlassAdvancedSlider(
    title: String,
    description: String,
    value: Float,
    valueText: String = "${(value * 100f).roundToInt()}%",
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                AppText(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppText(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        AppSlider(
            value = value,
            onValueChange = { onValueChange(it.coerceIn(0f, 1f)) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = title
                    stateDescription = valueText
                },
        )
    }
}
