#!/usr/bin/env bash
set -euo pipefail
GAME_APK=${1:-android/build/outputs/apk/debug/android-debug.apk}
TEST_APK=${2:-android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk}
OUTPUT_DIR=${3:-smoke-results}
TEST_CLASS=${4:-com.shatteredpixel.shatteredpixeldungeon.android.CowSmokeTest}
for APK in "$GAME_APK" "$TEST_APK"; do
  if [ ! -f "$APK" ]; then
    echo "APK 不存在：$APK" >&2
    exit 1
  fi
done
mkdir -p "$OUTPUT_DIR"
adb wait-for-device
# 测试模拟器预先确认系统全屏提示，并关闭系统过场动画。
# 只配置 Android 系统界面，不替代游戏内的真实点击或功能断言。
adb shell settings put secure immersive_mode_confirmations confirmed
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb logcat -c
# 覆盖安装保留用户数据，兼容测试依赖由已发布旧版 APK 写出的真实存档。
adb install -r "$GAME_APK"
adb install -r "$TEST_APK"
# 每轮只保留本次截图，避免启动失败时误收集上次测试的画面。
adb shell rm -f '/sdcard/Android/data/com.jarvistowne.jiangniu/files/cow-smoke/*.png'
python3 - "$OUTPUT_DIR" <<'PY'
from pathlib import Path
import shutil
import sys
for name in ('screenshots', 'cow-smoke'):
    directory = Path(sys.argv[1]) / name
    if directory.is_dir():
        shutil.rmtree(directory)
PY
set +e
adb shell am instrument -w -r \
  -e class "$TEST_CLASS" \
  com.jarvistowne.jiangniu.test/androidx.test.runner.AndroidJUnitRunner \
  | tee "$OUTPUT_DIR/instrumentation.txt"
TEST_STATUS=${PIPESTATUS[0]}
set -e
adb logcat -d > "$OUTPUT_DIR/logcat.txt"
adb pull /sdcard/Android/data/com.jarvistowne.jiangniu/files/cow-smoke "$OUTPUT_DIR/" || true
adb exec-out screencap -p > "$OUTPUT_DIR/final-screen.png"
if [ "$TEST_STATUS" -ne 0 ]; then exit "$TEST_STATUS"; fi
python3 - "$OUTPUT_DIR" <<'PY'
from pathlib import Path
import re
import sys
text = (Path(sys.argv[1]) / 'instrumentation.txt').read_text()
if not re.search(r'OK \([1-9][0-9]* tests?\)', text) or 'FAILURES!!!' in text or 'INSTRUMENTATION_FAILED' in text:
    raise SystemExit('Android 冒烟测试未通过，请检查 instrumentation.txt。')
print('所选 Android 测试类执行通过；具体覆盖范围以 instrumentation.txt 与测试源码为准。')
PY
