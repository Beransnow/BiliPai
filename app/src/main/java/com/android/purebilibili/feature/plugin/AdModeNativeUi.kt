package com.android.purebilibili.feature.plugin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.data.model.response.PromotionBannerTarget
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.VideoPromotionPresentation
import kotlinx.coroutines.delay

@Composable
fun AdModePromotionBadge(
    promotion: VideoPromotionPresentation,
    modifier: Modifier = Modifier,
) {
    AppSurface(
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Pill),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        AppText(
            text = promotion.badgeLabel,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
fun AdModeInlineBanner(
    target: PromotionBannerTarget,
    onClick: (String, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppSurface(
        onClick = { onClick(target.bvid, target.cid) },
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Card),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = target.coverUrl,
                contentDescription = target.title,
                modifier = Modifier
                    .size(width = 96.dp, height = 64.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.fillMaxWidth(0.68f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                AppText(
                    text = "广告 · ${target.ownerName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                AppText(
                    text = target.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                AppText(
                    text = "立即观看",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun AdModeSplashHost(
    onOpenVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled by AdModeRuntime.enabled.collectAsStateWithLifecycle()
    val config by AdModeRuntime.config.collectAsStateWithLifecycle()
    val candidate by AdModeRuntime.splashCandidate.collectAsStateWithLifecycle()
    val latestOnOpenVideo by rememberUpdatedState(onOpenVideo)
    var secondsRemaining by remember { mutableIntStateOf(5) }
    val visibleCandidate = candidate.takeIf { enabled && config.showSplashAd }

    LaunchedEffect(visibleCandidate?.bvid) {
        if (visibleCandidate == null) return@LaunchedEffect
        secondsRemaining = 5
        while (secondsRemaining > 0) {
            delay(1_000)
            secondsRemaining -= 1
        }
        AdModeRuntime.dismissSplashCandidate()
    }

    if (visibleCandidate == null) return
    AppSurface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = visibleCandidate.pic,
                contentDescription = visibleCandidate.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            AppSurface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppText("广告", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    AppText(
                        text = visibleCandidate.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AppText(
                        text = visibleCandidate.owner.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTextButton(
                            onClick = {
                                AdModeRuntime.dismissSplashCandidate()
                                latestOnOpenVideo(visibleCandidate)
                            },
                        ) {
                            AppText("查看详情")
                        }
                        AppTextButton(onClick = AdModeRuntime::dismissSplashCandidate) {
                            AppText("跳过广告 ${secondsRemaining}s")
                        }
                    }
                }
            }
        }
    }
}
