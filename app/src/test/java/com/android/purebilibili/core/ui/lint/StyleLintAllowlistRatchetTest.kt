package com.android.purebilibili.core.ui.lint

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 白名单体量棘轮。
 *
 * StyleLint 的设计意图是「只拦新增违规」，但它漏了一个环节：往
 * [StyleLintAllowlist] 里加一条豁免只需要改一行，**没有任何机制让人注意到总数在涨**。
 * 于是白名单可以无声膨胀，lint 看起来还是绿的。
 *
 * 加上这三个上限后，新增豁免必须同时把数字改大，PR diff 里就会出现
 * 「把上限从 96 提到 97」这个刺眼的动作——这正是文件头注释里
 * 「Adding a new path here is a documented exception, not a default」想要的效果。
 *
 * 迁移完一个文件就把对应数字调小，让棘轮只能往一个方向走。
 */
class StyleLintAllowlistRatchetTest {

    @Test
    fun shapeAllowlistDoesNotGrow() {
        assertTrue(
            StyleLintAllowlist.SHAPE_HITS.size <= MAX_SHAPE_HITS,
            "SHAPE_HITS 有 ${StyleLintAllowlist.SHAPE_HITS.size} 条，超过上限 $MAX_SHAPE_HITS。" +
                "迁移到 AppShapes 后请调小上限；确需新增豁免，请在 PR 里写明像素级理由。",
        )
    }

    @Test
    fun motionAllowlistDoesNotGrow() {
        assertTrue(
            StyleLintAllowlist.MOTION_HITS.size <= MAX_MOTION_HITS,
            "MOTION_HITS 有 ${StyleLintAllowlist.MOTION_HITS.size} 条，超过上限 $MAX_MOTION_HITS。" +
                "迁移到 AppMotionTokens 后请调小上限。",
        )
    }

    @Test
    fun surfaceAllowlistDoesNotGrow() {
        assertTrue(
            StyleLintAllowlist.SURFACE_HITS.size <= MAX_SURFACE_HITS,
            "SURFACE_HITS 有 ${StyleLintAllowlist.SURFACE_HITS.size} 条，超过上限 $MAX_SURFACE_HITS。" +
                "MaterialTheme.colorScheme.surface → AppSurfaceTokens 基本是 1:1 机械替换、" +
                "视觉零差异，是最容易削减的一类。",
        )
    }

    /**
     * 反方向的棘轮：已纳管的 feature 前缀只能增不能减。
     *
     * 少了这一条，「迁移」可以靠把前缀从名单里删掉来伪造通过。
     */
    @Test
    fun migratedFeaturePrefixesDoNotShrink() {
        assertTrue(
            StyleLintAllowlist.MIGRATED_TOKEN_PREFIXES.size >= MIN_MIGRATED_PREFIXES,
            "MIGRATED_TOKEN_PREFIXES 只剩 ${StyleLintAllowlist.MIGRATED_TOKEN_PREFIXES.size} 条，" +
                "低于下限 $MIN_MIGRATED_PREFIXES。已纳管的 feature 不应退出 spacing/color/" +
                "typography lint 覆盖。",
        )
    }

    private companion object {
        // 冻结于接入棘轮时的实测值，只能调小。
        //
        // 注意：接入时 HardcodedShape/Motion/SurfaceLintTest 三条本身是**红的**——
        // audio/ListenVideoScreen.kt、video/ui/gesture/GestureLevelOverlay.kt、
        // home/components/cards/HomeStyleSingleColumnVideoCard.kt 等文件在各自的
        // feature commit 里引入了新的硬编码，却没有同步更新白名单，而这些测试从来
        // 没有进过 CI，所以一直没人发现。
        //
        // 因此下面这三个数字是「当前白名单的长度」，不是「当前违规为零」。
        // 修复方向有两条，选哪条要在 PR 里说清楚：
        //   1. 把那些文件迁到 AppShapes / AppMotionTokens / AppSurfaceTokens（推荐）；
        //   2. 确有像素级理由无法迁移，则加进白名单并把这里的上限一并调大。
        // 第 2 条会让上限变大，这正是设计意图——它必须是一个显眼、需要解释的动作。
        const val MAX_SHAPE_HITS = 96
        const val MAX_MOTION_HITS = 20
        const val MAX_SURFACE_HITS = 57

        // 只能调大。直播与第一轮信息流模块已完成 token 迁移。
        const val MIN_MIGRATED_PREFIXES = 6
    }
}
