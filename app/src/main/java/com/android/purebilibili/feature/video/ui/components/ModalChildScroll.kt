package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

internal fun resolveModalChildScrollDelta(availableY: Float): Float = -availableY

@Composable
internal fun rememberModalChildScrollConnection(
    scrollableState: ScrollableState
): NestedScrollConnection {
    return remember(scrollableState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) {
                    return Offset.Zero
                }

                scrollableState.dispatchRawDelta(resolveModalChildScrollDelta(available.y))
                // 模态列表截断剩余手势，避免播放器折叠和详情页滚动。
                return Offset(x = 0f, y = available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return Velocity(x = 0f, y = available.y)
            }
        }
    }
}
