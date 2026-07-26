#!/usr/bin/env bash
#
# 生成并入库 Baseline Profile。
#
# 与旧版的区别：androidx.baselineprofile 插件会把产物**写回源码树**并自动注册
# source set，因此不再需要手动 find + cp（旧版的 app/src/main/baseline-prof.txt
# 是 AGP 8 之前的约定路径，现在已失效）。这里只负责触发生成 + 校验产物。
#
# 用法:
#   scripts/update_baseline_profile.sh          # 用已连接的真机采集（推荐，数据更真实）
#   scripts/update_baseline_profile.sh gmd      # 用 Gradle managed device（pixel6Api31）采集
#
# 注意:
#   - 采集前请先在设备上登录一次，Generator 需要真实登录态才能刷出首页 feed。
#   - BP 插件与配置缓存历史上有兼容问题，这里显式关闭；正常 assembleRelease 不受影响。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-device}"
PROFILE_DIR="app/src/main/generated/baselineProfiles"
BASELINE_FILE="$PROFILE_DIR/baseline-prof.txt"
STARTUP_FILE="$PROFILE_DIR/startup-prof.txt"

GRADLE_ARGS=(
  ":app:generateBaselineProfile"
  "--no-configuration-cache"
  "--console=plain"
)

case "$MODE" in
  gmd)
    echo "[1/3] 使用 Gradle managed device (pixel6Api31) 生成 Baseline Profile..."
    ;;
  device)
    echo "[1/3] 使用已连接真机生成 Baseline Profile..."
    adb devices
    ;;
  *)
    echo "未知模式: $MODE (支持 device | gmd)" >&2
    exit 2
    ;;
esac

./gradlew "${GRADLE_ARGS[@]}"

echo "[2/3] 校验产物..."
if [[ ! -s "$BASELINE_FILE" ]]; then
  echo "缺失或为空: $BASELINE_FILE" >&2
  echo "采集可能中断了。检查设备是否锁屏、是否已登录、Generator 是否找到底栏标签。" >&2
  exit 1
fi

BASELINE_LINES="$(wc -l < "$BASELINE_FILE" | tr -d ' ')"
echo "  $BASELINE_FILE: ${BASELINE_LINES} 行"
if [[ -s "$STARTUP_FILE" ]]; then
  echo "  $STARTUP_FILE: $(wc -l < "$STARTUP_FILE" | tr -d ' ') 行"
else
  echo "  警告: 没有 startup-prof.txt（检查 Generator 的 includeInStartupProfile）" >&2
fi

if [[ "$BASELINE_LINES" -lt 1500 ]]; then
  echo "警告: profile 只有 ${BASELINE_LINES} 行，明显偏少，疑似采集提前结束。" >&2
  echo "      BaselineProfileArtifactTest 会拦下这种产物，请重跑。" >&2
fi

echo "[3/3] 待提交的改动:"
git --no-pager diff --stat -- "$PROFILE_DIR" || true

echo
echo "完成。请提交 $PROFILE_DIR 下的改动，并用以下命令确认 APK 已打包 profile:"
echo "  ./gradlew :app:assembleRelease"
echo "  unzip -l app/build/outputs/apk/release/*.apk | grep dexopt"
