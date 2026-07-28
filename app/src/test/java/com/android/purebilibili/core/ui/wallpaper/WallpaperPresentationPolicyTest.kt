package com.android.purebilibili.core.ui.wallpaper

import com.android.purebilibili.core.util.WindowWidthSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperPresentationPolicyTest {

    @Test
    fun splashDecodeSizeIsBoundedToWindowPixels() {
        assertEquals(
            SplashWallpaperDecodeSize(widthPx = 1440, heightPx = 3200),
            resolveSplashWallpaperDecodeSize(windowWidthPx = 1440, windowHeightPx = 3200),
        )
        assertEquals(
            SplashWallpaperDecodeSize(widthPx = 1, heightPx = 1),
            resolveSplashWallpaperDecodeSize(windowWidthPx = 0, windowHeightPx = -1),
        )
    }

    @Test
    fun splash_compactWidth_usesFullCrop() {
        assertEquals(
            SplashWallpaperLayout.FULL_CROP,
            resolveSplashWallpaperLayout(WindowWidthSizeClass.Compact)
        )
    }

    @Test
    fun splash_compactWidth_usesPosterCardForExtremeAspectMismatch() {
        assertEquals(
            SplashWallpaperLayout.POSTER_CARD_BLUR_BG,
            resolveSplashWallpaperLayout(
                widthSizeClass = WindowWidthSizeClass.Compact,
                imageAspectRatio = 0.42f
            )
        )
        assertEquals(
            SplashWallpaperLayout.POSTER_CARD_BLUR_BG,
            resolveSplashWallpaperLayout(
                widthSizeClass = WindowWidthSizeClass.Compact,
                imageAspectRatio = 1.0f
            )
        )
    }

    @Test
    fun splash_compactWidth_keepsFullCropForPhoneLikePosterRatio() {
        assertEquals(
            SplashWallpaperLayout.FULL_CROP,
            resolveSplashWallpaperLayout(
                widthSizeClass = WindowWidthSizeClass.Compact,
                imageAspectRatio = 0.56f
            )
        )
    }

    @Test
    fun splash_tabletWidth_usesFullCrop() {
        assertEquals(
            SplashWallpaperLayout.FULL_CROP,
            resolveSplashWallpaperLayout(WindowWidthSizeClass.Medium)
        )
        assertEquals(
            SplashWallpaperLayout.FULL_CROP,
            resolveSplashWallpaperLayout(WindowWidthSizeClass.Expanded)
        )
    }

    @Test
    fun profile_compactWidth_usesTopBannerWithBlurBackground() {
        assertEquals(
            ProfileWallpaperLayout.TOP_BANNER_BLUR_BG,
            resolveProfileWallpaperLayout(WindowWidthSizeClass.Compact)
        )
    }

    @Test
    fun profile_tabletWidth_usesPosterCardWithBlurBackground() {
        assertEquals(
            ProfileWallpaperLayout.POSTER_CARD_BLUR_BG,
            resolveProfileWallpaperLayout(WindowWidthSizeClass.Medium)
        )
        assertEquals(
            ProfileWallpaperLayout.POSTER_CARD_BLUR_BG,
            resolveProfileWallpaperLayout(WindowWidthSizeClass.Expanded)
        )
    }
}
