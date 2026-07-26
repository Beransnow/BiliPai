package com.android.purebilibili.perf

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Baseline Profile 的接线与产物守卫。
 *
 * 背景：`baselineprofile/` 模块、Generator 与 5 个 macrobenchmark 一直都在，但
 * 根/app 都没应用 `androidx.baselineprofile` 插件，app 也没有 `baselineProfile(...)`
 * 依赖，因此 APK 里从来没有 `assets/dexopt/baseline.prof`——
 * `PureApplication` 里的 `ProfileInstaller.writeProfile()` 一直在空转，
 * 每个 release 的关键路径都靠运行时 JIT 逐步预热。
 *
 * 这两条测试分别守住「接线不能再被摘掉」和「入库的产物不能是残缺的」。
 */
class BaselineProfileArtifactTest {

    @Test
    fun baselineProfileWiringIsPresent() {
        val rootBuild = repoFile("build.gradle.kts").readText()
        assertTrue(
            rootBuild.contains("androidx.baselineprofile"),
            "根 build.gradle.kts 缺少 androidx.baselineprofile 插件声明",
        )

        val appBuild = repoFile("app/build.gradle.kts").readText()
        assertTrue(
            appBuild.contains("id(\"androidx.baselineprofile\")"),
            "app 模块未应用 androidx.baselineprofile 插件",
        )
        assertTrue(
            appBuild.contains("baselineProfile(project(\":baselineprofile\"))"),
            "app 模块缺少 baselineProfile(project(\":baselineprofile\")) 依赖——" +
                "没有它，profile 不会被打进 APK",
        )

        val benchmarkBuild = repoFile("baselineprofile/build.gradle.kts").readText()
        assertTrue(
            benchmarkBuild.contains("id(\"androidx.baselineprofile\")"),
            "baselineprofile 模块未应用 androidx.baselineprofile 插件",
        )
    }

    /**
     * 产物质量守卫。
     *
     * 生成 profile 需要真机或 GMD，无法在纯 JVM 单测里完成，因此这里不强制产物存在——
     * 否则每个没跑过采集的开发者都会红。但一旦产物入库，它就必须是完整的：
     * 采集中途中断（设备锁屏、未登录、Generator 找不到底栏标签）会留下一个几百行的
     * 残缺 profile，那比没有更危险——它看起来像是已经优化过了。
     */
    @Test
    fun committedBaselineProfileIsNonTrivial() {
        val profile = repoFile("app/src/main/generated/baselineProfiles/baseline-prof.txt")
        if (!profile.exists()) {
            println(
                "尚未入库 Baseline Profile。跑 scripts/update_baseline_profile.sh 生成，" +
                    "否则 release 包的冷启动仍然全靠 JIT 预热。",
            )
            return
        }

        val lines = profile.readLines().filter { it.isNotBlank() }
        assertTrue(
            lines.size >= MIN_PROFILE_LINES,
            "baseline-prof.txt 只有 ${lines.size} 行（期望 >= $MIN_PROFILE_LINES），" +
                "疑似采集中断，请重跑 scripts/update_baseline_profile.sh",
        )
        assertTrue(
            lines.any { it.contains("com/android/purebilibili/MainActivity") },
            "profile 里没有 MainActivity，启动路径未被覆盖",
        )
        assertTrue(
            lines.any { it.contains("feature/home") || it.contains("HomeScreen") },
            "profile 里没有首页相关类，feed 渲染路径未被覆盖",
        )
    }

    private fun repoFile(relativePath: String): File {
        // 单测工作目录是 app/，仓库根在上一级。
        val fromApp = File(relativePath)
        if (fromApp.exists()) return fromApp
        return File("..", relativePath)
    }

    private companion object {
        const val MIN_PROFILE_LINES = 1500
    }
}
