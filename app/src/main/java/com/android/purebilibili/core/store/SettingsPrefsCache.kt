package com.android.purebilibili.core.store

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SharedPreferences 影子缓存的读写工具。
 *
 * `SettingsManager` 的真值存在 DataStore 里，但 DataStore 只能挂起读取。为了让
 * `PureApplication` 启动、`NetworkUtils`、播放器初始化这类**必须同步拿到值**的地方能
 * 工作，项目里长期用 SharedPreferences 做影子缓存：每次 `set*` 都双写一份。
 *
 * 这些工具刻意放在 `SettingsManager.kt` 之外的独立文件：那个门面文件正在按领域拆成
 * 多个 store，且有行数棘轮（`SettingsManagerSizeRatchetTest`）盯着只减不增。
 * 通用基建本来也不属于门面。
 */

/**
 * 把影子缓存的**同步**写入挪到 IO 线程。
 *
 * 这些写入原先直接写在 `suspend fun` 体内，而 `suspend` 并不意味着离开主线程——
 * 调用方多是 `viewModelScope.launch { }`（Main 调度器），`settingsDataStore.edit{}`
 * 挂起返回后续体恢复在主线程，于是 `commit()` 就是一次**主线程同步写盘**。
 * lint 的 `ApplySharedPref` 报的正是这一批，此前被 lint-baseline 静默抑制着。
 *
 * 刻意保留 `commit()` 而不是换成 `apply()`：影子缓存存在的意义就是「下次进程启动时
 * 能同步读到」，其中切换应用图标那处还有硬前提——切 activity-alias 往往会立刻杀掉
 * 进程，`apply()` 的异步写入可能来不及落盘。换成 `apply()` 是拿正确性换性能。
 * 挪到 IO 线程则两者兼得：`withContext` 会等它落盘才返回，持久化保证不变，
 * 主线程不再被阻塞。
 *
 * `ApplySharedPref` 是**纯语法检查**——它只认 `commit()` 这个调用本身，与线程无关，
 * 所以上面的修复并不会让它变绿；而它建议的替代方案在这里是错的。因此就地抑制
 * 并写明理由，取代原先散在 lint-baseline 里的 4 条静默条目：
 * 抑制写在代码旁边，下一个读到的人能立刻看到取舍；写在 baseline 里则没人会看见。
 */
@SuppressLint("ApplySharedPref")
internal suspend fun commitPrefs(
    context: Context,
    name: String,
    edit: SharedPreferences.Editor.() -> Unit,
): Boolean = withContext(Dispatchers.IO) {
    context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().apply(edit).commit()
}
