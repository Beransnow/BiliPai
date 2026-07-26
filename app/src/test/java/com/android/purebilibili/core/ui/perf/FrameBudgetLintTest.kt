package com.android.purebilibili.core.ui.perf

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 帧预算棘轮。
 *
 * 这些不是「风格问题」，每一条都对应一类**已知会吃掉帧预算**的写法。它们在本仓库里
 * 都已有存量，所以这里不做「必须为零」的断言——那只会立刻变红然后被人删掉——
 * 而是**冻结当前数量，只允许减少**。
 *
 * 这条测试要回答的是一个具体的历史问题：近 5 个版本里卡片转场相关 commit 超过 40 条、
 * 多次反复 revert，却没有任何机制阻止「修好一处、又在别处新增一处」。棘轮让新增
 * 变成一个必须在 PR diff 里显式改数字的动作。
 *
 * 不复用 `StyleLintSupport`：那套目前只扫 `feature/`，而这里的问题大量集中在
 * `core/`（`UnifiedBlur`、`ModifierExt`、`Animations` 都在 core）。自带扫描也避免了
 * 与正在进行中的 lint 迁移工作互相踩。
 */
class FrameBudgetLintTest {

    @Test
    fun composedModifiersDoNotGrow() {
        val hits = mainSources().sumOf { it.readText().countOf(COMPOSED) }
        assertRatchet(
            actual = hits,
            limit = MAX_COMPOSED,
            what = "Modifier.composed { }",
            why = "composed{} 没有 equals 实现，Compose 无法比较也无法复用，" +
                "每次重组都要重新执行整个 lambda——官方明确的性能反模式。" +
                "尤其 core/util/Animations.kt 的 animateEnter 是每张卡片一个。" +
                "请迁移到 Modifier.Node，或退一步改成 @Composable 工厂函数 + Modifier.then。",
        )
    }

    @Test
    fun offscreenCompositingDoesNotGrow() {
        val hits = mainSources().sumOf { it.readText().countOf(OFFSCREEN) }
        assertRatchet(
            actual = hits,
            limit = MAX_OFFSCREEN,
            what = "CompositingStrategy.Offscreen",
            why = "每一处都会为整棵子树额外申请一块离屏缓冲并做一次全量合成。" +
                "确实需要 BlendMode.DstIn 遮罩时它是必需的（去掉会出黑边），" +
                "但除此之外应优先考虑不需要离屏的画法。",
        )
    }

    @Test
    fun hazeSourceRegistrationsDoNotGrow() {
        val hits = mainSources().sumOf { it.readText().countOf(HAZE_SOURCE) }
        assertRatchet(
            actual = hits,
            limit = MAX_HAZE_SOURCE,
            what = "hazeSourceCompat(",
            why = "每注册一个 haze source，对应子树在每帧都要被 record 一次。" +
                "注册点应当条件挂载——消费方不存在时（例如液态玻璃关闭）根本不该注册。",
        )
    }

    @Test
    fun blockingReadsInStoreDoNotGrow() {
        val hits = storeSources().sumOf { it.readText().countOf(RUN_BLOCKING) }
        assertRatchet(
            actual = hits,
            limit = MAX_RUN_BLOCKING_IN_STORE,
            what = "core/store 下的 runBlocking",
            why = "设置读取几乎总是发生在首帧路径上，runBlocking 会把 DataStore 的" +
                "首次读盘（冷启下可达 50–150ms）直接压在主线程。" +
                "正确做法是内存缓存优先 + 未命中时返回默认值，由启动协程异步回填。",
        )
    }

    /**
     * 每帧重建 RenderEffect 的守卫。
     *
     * `graphicsLayer` 里直接 `createBlurEffect(...)` 而不比较半径，等于每一帧都新建一个
     * RenderEffect 对象并让底层重新编译着色器。正确写法在
     * `VideoCardTransitionBackgroundPolicy.kt:740` —— 先比较 `lastBlurRadiusPx`，
     * 只有真正变化时才重建。这里以「文件内是否存在半径守卫标识」做近似判定。
     */
    @Test
    fun unguardedBlurEffectFilesDoNotGrow() {
        val offenders = mainSources()
            .map { it to it.readText() }
            .filter { (_, text) -> BLUR_EFFECT.containsMatchIn(text) }
            .filterNot { (_, text) -> RADIUS_GUARD.containsMatchIn(text) }
            .map { (file, _) -> file.name }

        assertTrue(
            offenders.size <= MAX_UNGUARDED_BLUR_EFFECT_FILES,
            "含 createBlurEffect 但没有半径守卫的文件有 ${offenders.size} 个" +
                "（上限 $MAX_UNGUARDED_BLUR_EFFECT_FILES）：${offenders.sorted()}。" +
                "请参照 VideoCardTransitionBackgroundPolicy 的 lastBlurRadiusPx 写法，" +
                "半径未变化时复用已有 RenderEffect。",
        )
    }

    @Test
    fun infiniteTransitionsDoNotGrow() {
        val hits = mainSources().sumOf { it.readText().countOf(INFINITE_TRANSITION) }
        assertRatchet(
            actual = hits,
            limit = MAX_INFINITE_TRANSITION,
            what = "rememberInfiniteTransition(",
            why = "无限循环动画只要处于组合中就会持续申请帧，即使用户根本看不见。" +
                "新增装饰性循环动画前，先确认它能被动效档位关掉。",
        )
    }

    /**
     * 扫描器自检：确保上面几条不是在空集合上跑绿。
     *
     * 源码文本扫描类守卫最隐蔽的失效方式是「路径变了，一个文件都没扫到，于是全绿」。
     */
    @Test
    fun scannerActuallyReadsSources() {
        assertTrue(mainSources().size > 500, "main 源码只扫到 ${mainSources().size} 个文件，扫描路径可能已失效")
        assertTrue(storeSources().isNotEmpty(), "core/store 下一个文件都没扫到，扫描路径可能已失效")
    }

    private fun assertRatchet(actual: Int, limit: Int, what: String, why: String) {
        assertTrue(
            actual <= limit,
            "「$what」当前 $actual 处，超过冻结上限 $limit 处。\n$why\n" +
                "如果这次改动确实必须新增，请连同上限一起调大，并在 PR 里写明理由——" +
                "这个动作是刻意做得显眼的。",
        )
    }

    private fun String.countOf(pattern: Regex): Int = pattern.findAll(this).count()

    private fun mainSources(): List<File> = cachedMain

    private fun storeSources(): List<File> =
        cachedMain.filter { it.invariantPath.contains("/core/store/") }

    private companion object {
        val COMPOSED = Regex("""=\s*composed\s*[({]""")
        val OFFSCREEN = Regex("""CompositingStrategy\.Offscreen""")
        val HAZE_SOURCE = Regex("""\.hazeSourceCompat\(""")
        val RUN_BLOCKING = Regex("""\brunBlocking\s*[({]""")
        val BLUR_EFFECT = Regex("""createBlurEffect\s*\(""")
        val RADIUS_GUARD = Regex("""last\w*(Blur)?Radius""")
        val INFINITE_TRANSITION = Regex("""rememberInfiniteTransition\s*\(""")

        // ── 冻结于接入棘轮时的实测值，只能调小 ──────────────────────────
        const val MAX_COMPOSED = 23
        const val MAX_OFFSCREEN = 2
        const val MAX_HAZE_SOURCE = 28
        const val MAX_RUN_BLOCKING_IN_STORE = 1
        const val MAX_INFINITE_TRANSITION = 17

        // 当前 3 个：PredictiveBackBackgroundPolicy.kt（每帧重建，转场期最热的一条路径）、
        // ImagePreviewDialog.kt、MainActivity.kt（splash 淡出期，峰值半径 70dp）。
        const val MAX_UNGUARDED_BLUR_EFFECT_FILES = 3

        val cachedMain: List<File> by lazy {
            val roots = listOf(
                "src/main/java/com/android/purebilibili",
                "app/src/main/java/com/android/purebilibili",
            )
            val root = roots.map { File(it) }.firstOrNull { it.isDirectory }
                ?: error("找不到 main 源码根目录，cwd=" + File(".").absoluteFile.canonicalPath)
            root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        }

        val File.invariantPath: String get() = path.replace('\\', '/')
    }
}
