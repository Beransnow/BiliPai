# 性能基线与采集流程

这个目录是 BiliPai 性能工作的**唯一事实来源**。在它建立之前，`scripts/` 下的采集脚本
往一个不存在的目录写文件，所有数据只留在开发者本机——因此近 5 个版本里 40 多条动效相关
commit **没有一条附带前后帧率数据**，每次「优化」都只能靠主观观感判断，也就无法区分
「真的变快了」和「把卡顿挪到了别处」。

任何动效/渲染改动，PR 里必须能回答一个问题：**改动前后这三条黄金路径的数字是多少。**

## 目录结构

```
docs/perf/
├── README.md                  # 本文件
├── baseline/                  # ← 提交进 git，回归比对的基准
├── raw/                       # 脚本原始输出，不入 git（见 .gitignore）
└── snapshots/<date>-<sha7>/   # 每次更新基线时归档一份，带设备与构建元数据
```

## 采集环境固定项

数据可比的前提是环境一致。**每次采集都必须满足**：

- 同一台真机，记录型号 + Android 版本（写进 snapshot 的元数据）
- 使用 **`smooth`** variant（release 语义但跳过 R8/资源压缩，避免混淆干扰归因），
  卡片转场脚本例外——`card_transition_gfxinfo.sh` 只接受 `*.debug` 包
- 采集前预热：`adb shell cmd package compile -m speed-profile com.android.purebilibili`
- 屏幕亮度固定 50%，电量 > 50%，不插充电（避免调度策略变化）
- 关闭后台应用，保持联网（首页 feed 需要真实网络数据）
- 同一账号、同一登录态

## 三条黄金路径

| 路径 | 脚本 | 关注指标 |
|---|---|---|
| 首页竖向滚动（20 loop） | `scripts/mobile_perf_collect.sh` | p50/p90/p99 帧耗时、jank% |
| 首页分类横滑 | `scripts/mobile_perf_collect.sh` | 同上 |
| 卡片 → 详情转场开合 | `scripts/card_transition_gfxinfo.sh` | p50/p90/p95/p99、over-budget%、PSS delta |

平板另有 `scripts/tablet_perf_collect.sh`。

## 阈值

| 指标 | 目标 |
|---|---|
| p90 帧耗时 | ≤ 11.0 ms |
| p99 帧耗时 | ≤ 22.0 ms |
| jank 比例 | ≤ 3.0% |
| **回归门限** | 基线 × 1.15（超过即视为回归，需在 PR 中解释或修复） |

## 什么时候可以更新基线

基线只能因为**明确的功能新增**而上调，不能因为「这次就是慢了一点」而上调。

更新基线的 PR 必须：

1. 在描述里写明为什么允许变慢（新增了什么、为何无法避免）
2. 附上前后两份 json 的 diff
3. 把旧基线归档到 `snapshots/<date>-<sha7>/`

## 与 macrobenchmark 的分工（重要）

`:baselineprofile` 下的 macrobenchmark 可以在 Gradle managed device（`pixel6Api31`）上
无人值守运行，适合放进 CI 做**趋势告警**。但模拟器的 p99 抖动可达 ±40%，因此：

- **模拟器数据只用来抓数量级回归**（p90 从 8ms 变成 25ms 这种），容差放到 1.25×
- **不要用模拟器数据做 5% 级别的调优判定**，也不要拿它和真机数据放在一起比较
- 本目录 `baseline/` 里的真值**只接受真机采集**，由人工更新

## 常用命令

```bash
# 真机采集首页滚动
scripts/mobile_perf_collect.sh

# 卡片转场专项（需要 *.debug 包）
scripts/card_transition_gfxinfo.sh

# managed device 跑 macrobenchmark（CI/无真机时）
./gradlew :baselineprofile:pixel6Api31BenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR,LOW_BATTERY

# 生成并入库 Baseline Profile
scripts/update_baseline_profile.sh
```
