package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import kotlin.math.roundToInt

internal data class VideoDetailCoverDrawGeometry(val size: Size, val offset: Offset)

/** Preserve center-crop as the aspect ratio changes; scaling a pre-cropped layer would stretch it. */
internal fun resolveVideoDetailCoverDrawGeometry(
    intrinsicSize: Size,
    targetSize: Size,
    contentScale: ContentScale,
    layoutDirection: LayoutDirection,
): VideoDetailCoverDrawGeometry {
    val sourceSize = if (intrinsicSize == Size.Unspecified) targetSize else Size(
        intrinsicSize.width.takeIf { it.isFinite() && it > 0f } ?: targetSize.width,
        intrinsicSize.height.takeIf { it.isFinite() && it > 0f } ?: targetSize.height,
    )
    val scale = contentScale.computeScaleFactor(sourceSize, targetSize)
    val drawSize = Size(sourceSize.width * scale.scaleX, sourceSize.height * scale.scaleY)
    val offset = Alignment.Center.align(
        IntSize(drawSize.width.roundToInt(), drawSize.height.roundToInt()),
        IntSize(targetSize.width.roundToInt(), targetSize.height.roundToInt()),
        layoutDirection,
    )
    return VideoDetailCoverDrawGeometry(drawSize, Offset(offset.x.toFloat(), offset.y.toFloat()))
}

/**
 * 图片请求和布局节点保持固定视口，交接矩形只在绘制阶段读取。
 * 独立封面层按实际交接尺寸录制，避免 4:3、16:10 封面超出播放器时被小画布裁掉。
 * 玻璃与原生材质共用此几何，不改变播放器尺寸、封面裁切或图片请求身份。
 */
@Composable
internal fun VideoDetailReturnCoverImage(
    request: ImageRequest,
    landingLayout: VideoDetailReturnSourceCardLayout?,
    handoffProgressProvider: () -> Float,
    inverseScaleXProvider: () -> Float,
    inverseScaleYProvider: () -> Float,
    alphaProvider: () -> Float,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    contentTopInsetProvider: () -> Dp = { 0.dp },
    clipCornerDp: Dp = 0.dp,
    nativeSnapshotBoundsProvider: (() -> Rect?)? = null,
    slotOriginProvider: () -> Offset = { Offset.Zero },
    backingColor: Color = Color.Unspecified,
    onDecoded: () -> Unit = {},
) {
    val sizeResolver = rememberConstraintsSizeResolver()
    val resolvedRequest = remember(request, sizeResolver) {
        // Keep explicit source decode dimensions/cache identity. Otherwise resolve from
        // viewport constraints, independently of the animated cover rectangle.
        if (request.defined.sizeResolver != null) request else request.newBuilder().size(sizeResolver).build()
    }
    val painter = rememberAsyncImagePainter(
        model = resolvedRequest,
        contentScale = contentScale,
        onSuccess = { onDecoded() },
    )
    val path = remember { Path() }
    val coverLayer = rememberGraphicsLayer()
    // 录制期间的密度必须独立于可变 DrawScope；便捷 record(size) 重载会把
    // 当前绘制作用域作为 Density，录制块中的 toPx/图片绘制可能递归读取自身。
    val recordingDensity = LocalDensity.current
    val recordingLayoutDirection = LocalLayoutDirection.current
    val semantics = if (contentDescription != null) Modifier.semantics {
        this.contentDescription = contentDescription
        role = Role.Image
    } else Modifier
    Box(modifier.then(sizeResolver).then(semantics)) {
        Canvas(Modifier.matchParentSize()) {
            val alpha = alphaProvider().coerceIn(0f, 1f)
            if (alpha <= 0f || size.width <= 0f || size.height <= 0f) return@Canvas
            val progress = handoffProgressProvider()
            val entryFrame = resolveVideoDetailReturnMediaLayoutFrame(
                containerWidthPx = size.width.roundToInt(),
                containerHeightPx = size.height.roundToInt(),
                landingLayout = landingLayout,
                handoffProgress = progress,
                inverseScaleX = inverseScaleXProvider(),
                inverseScaleY = inverseScaleYProvider(),
                contentTopInsetPx = contentTopInsetProvider().roundToPx(),
                nativeSnapshotBounds = nativeSnapshotBoundsProvider?.invoke(),
            )
            val frame = resolveVideoDetailReturnMediaFrameInSlot(entryFrame, slotOriginProvider(), progress)
            coverLayer.record(
                density = recordingDensity,
                layoutDirection = recordingLayoutDirection,
                size = IntSize(frame.widthPx, frame.heightPx),
            ) {
                val bounds = Rect(Offset.Zero, size)
                val radius = if (landingLayout?.canRender == true) {
                    clipCornerDp.toPx() * progress.coerceIn(0f, 1f)
                } else 0f
                path.rewind()
                path.addRoundRect(RoundRect(bounds, CornerRadius(radius, radius)))
                val geometry = resolveVideoDetailCoverDrawGeometry(
                    painter.intrinsicSize, bounds.size, contentScale, layoutDirection,
                )
                clipPath(path) {
                    if (backingColor != Color.Unspecified) drawRect(backingColor)
                    translate(geometry.offset.x, geometry.offset.y) {
                        with(painter) { draw(geometry.size) }
                    }
                }
            }
            // 背板与图片作为一组叠化；仅透明阶段才需要离屏合成。
            coverLayer.alpha = alpha
            translate(frame.offsetXPx.toFloat(), frame.offsetYPx.toFloat()) {
                drawLayer(coverLayer)
            }
        }
    }
}

/** The cover canvas paints an opaque black backing even while its image is still loading. */
internal fun resolveVideoDetailOpaqueCoverInterior(
    frame: VideoDetailReturnMediaLayoutFrame,
    coverAlpha: Float,
    cornerRadiusPx: Float,
): Rect? {
    // Crossfades still need the complete backing beneath the live player.
    if (!coverAlpha.isFinite() || coverAlpha < 1f || !cornerRadiusPx.isFinite()) return null
    // Stay inside rounded corners and their antialiased boundary; never cull an exposed edge.
    val inset = cornerRadiusPx.coerceAtLeast(0f) + 1f
    val left = frame.offsetXPx + inset
    val top = frame.offsetYPx + inset
    val right = frame.offsetXPx + frame.widthPx - inset
    val bottom = frame.offsetYPx + frame.heightPx - inset
    return if (right > left && bottom > top) Rect(left, top, right, bottom) else null
}

/** Cull only the black pixels that the opaque resident cover will paint again above us. */
internal fun DrawScope.drawVideoDetailMediaBacking(
    frame: VideoDetailReturnMediaLayoutFrame,
    alpha: Float,
    cornerRadiusPx: Float,
    opaqueCoverInterior: Rect?,
) {
    if (alpha <= 0f) return
    fun DrawScope.paintBacking() {
        drawRoundRect(
            color = Color.Black.copy(alpha = alpha),
            topLeft = Offset(frame.offsetXPx.toFloat(), frame.offsetYPx.toFloat()),
            size = Size(frame.widthPx.toFloat(), frame.heightPx.toFloat()),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        )
    }
    if (opaqueCoverInterior == null) {
        paintBacking()
    } else {
        clipRect(
            left = opaqueCoverInterior.left, top = opaqueCoverInterior.top,
            right = opaqueCoverInterior.right, bottom = opaqueCoverInterior.bottom,
            clipOp = ClipOp.Difference,
        ) { paintBacking() }
    }
}
