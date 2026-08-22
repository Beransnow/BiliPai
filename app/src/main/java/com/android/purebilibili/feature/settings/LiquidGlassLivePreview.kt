package com.android.purebilibili.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.store.LiquidGlassMode
import com.android.purebilibili.core.ui.components.AppSlider
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.feature.home.components.biliPaiFloatingDockShell
import com.android.purebilibili.feature.home.components.resolveLiquidGlassTuning
import coil.compose.AsyncImage
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@Composable
internal fun LiquidGlassAdjustmentPanel(
    persistedProgress: Float,
    previewImageUri: String?,
    onProgressCommitted: (Float) -> Unit,
    onPreviewImageChanged: (String?) -> Unit,
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
    val tuning = remember(previewProgress) {
        resolveLiquidGlassTuning(previewProgress)
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
            text = "所选图片仅用于本页效果预览。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

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

@Composable
private fun LiquidGlassHomeSample(
    progress: Float,
    previewImageUri: String?,
    modifier: Modifier = Modifier,
) {
    val backdrop = rememberLayerBackdrop()
    val tuning = remember(progress) { resolveLiquidGlassTuning(progress) }
    val sampleShape = RoundedCornerShape(24.dp)
    val glassColor = MaterialTheme.colorScheme.surfaceContainer
    val contentColor = MaterialTheme.colorScheme.onSurface
    var customImageFailed by remember(previewImageUri) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(224.dp)
            .clip(sampleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
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
                DefaultLiquidGlassPreviewContent()
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
private fun DefaultLiquidGlassPreviewContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF345B7E),
                        Color(0xFF9E7B58),
                        Color(0xFF567D58),
                    )
                )
            )
            .padding(top = 58.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppText(
            text = "推荐",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) { index ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(104.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            if (index == 0) Color(0xFFD6A667) else Color(0xFF7296A8)
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Spacer(
                        modifier = Modifier
                            .width(if (index == 0) 88.dp else 70.dp)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.88f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Spacer(
                        modifier = Modifier
                            .width(54.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.58f))
                    )
                }
            }
        }
    }
}
