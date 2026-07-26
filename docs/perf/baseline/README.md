# baseline/

这里放**真机采集**的基准数据，CI 与 PR 用它做回归比对。

目录当前为空——首份基线必须按 `../README.md` 的「首次快照顺序」采集：

1. 在接入 Baseline Profile **之前**的提交上采一次，存到 `../snapshots/<date>-before/`
2. 接上 Baseline Profile 后采一次，只比对启动指标（`timeToInitialDisplayMs`）
3. 拆掉首页无效全屏层后再采一次，把结果定为本目录的 `baseline/`

这个顺序不能省：跳过第 1 步就没有对照组，无法证明后续两步各自的收益。

预期文件：

- `mobile-home-feed.json`
- `tablet-home-feed.json`
- `card-transition.json`
- `macrobench-pixel6api31.json`（模拟器趋势线，仅用于数量级回归告警）
