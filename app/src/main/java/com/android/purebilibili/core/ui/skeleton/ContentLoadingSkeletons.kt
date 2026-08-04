package com.android.purebilibili.core.ui.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.util.VideoGridItemSkeleton
import com.android.purebilibili.core.util.shimmerEffect

/**
 * 内容列表首屏骨架（视频网格 / 媒体行 / 用户行）。
 * 用于搜索、分区、分类、直播区等「结果形态已知」的加载态，
 * 比主题 LoadingIndicator / 吉祥物更贴合列表占位。
 */

@Composable
fun ContentVideoGridSkeleton(
    modifier: Modifier = Modifier,
    minItemWidth: Dp = 160.dp,
    coverAspectRatio: Float = 16f / 10f,
    itemCount: Int = 8,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minItemWidth),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        userScrollEnabled = false,
        modifier = modifier.fillMaxSize(),
    ) {
        val skeletonKeys = List(itemCount.coerceAtLeast(0)) { it }
        lazyGridItems(
            items = skeletonKeys,
            key = { "content_video_grid_skeleton_$it" },
            contentType = { "content_video_grid_skeleton" },
        ) {
            VideoGridItemSkeleton(coverAspectRatio = coverAspectRatio)
        }
    }
}

@Composable
fun ContentVideoGridSkeletonFixedColumns(
    columns: Int,
    modifier: Modifier = Modifier,
    coverAspectRatio: Float = 16f / 10f,
    rows: Int = 4,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    spacing: Dp = 8.dp,
) {
    val safeColumns = columns.coerceAtLeast(1)
    val skeletonKeys = List(safeColumns * rows.coerceAtLeast(1)) { it }
    LazyVerticalGrid(
        columns = GridCells.Fixed(safeColumns),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        userScrollEnabled = false,
        modifier = modifier.fillMaxSize(),
    ) {
        lazyGridItems(
            items = skeletonKeys,
            key = { "content_video_fixed_skeleton_$it" },
            contentType = { "content_video_fixed_skeleton" },
        ) {
            VideoGridItemSkeleton(coverAspectRatio = coverAspectRatio)
        }
    }
}

/** 横向媒体行：左封面 + 右侧标题/副标题，适配直播/专栏/番剧/话题列表。 */
@Composable
fun MediaListRowSkeleton(
    modifier: Modifier = Modifier,
    coverWidth: Dp = 128.dp,
    coverAspectRatio: Float = 16f / 10f,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(coverWidth)
                .aspectRatio(coverAspectRatio)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect(),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(),
            )
        }
    }
}

/** 用户行：头像 + 两行文案，适配 UP / 主播搜索。 */
@Composable
fun UserListRowSkeleton(
    modifier: Modifier = Modifier,
    avatarSize: Dp = 48.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .shimmerEffect(),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(),
            )
        }
    }
}

@Composable
fun ContentMediaListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 8,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp),
    useUserRow: Boolean = false,
) {
    val skeletonKeys = List(itemCount.coerceAtLeast(0)) { it }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        userScrollEnabled = false,
    ) {
        lazyListItems(
            items = skeletonKeys,
            key = { "content_media_list_skeleton_$it" },
            contentType = { if (useUserRow) "user_row_skeleton" else "media_row_skeleton" },
        ) {
            if (useUserRow) {
                UserListRowSkeleton()
            } else {
                MediaListRowSkeleton()
            }
        }
    }
}
