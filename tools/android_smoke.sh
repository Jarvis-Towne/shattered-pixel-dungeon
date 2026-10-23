#!/usr/bin/env bash
set -euo pipefail
mkdir -p smoke-results
adb wait-for-device
# 测试模拟器预先确认系统全屏提示，并关闭系统过场动画。
# 只配置 Android 系统界面，不替代游戏内的真实点击或功能断言。
adb shell settings put secure immersive_mode_confirmations confirmed
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb logcat -c
adb install -r dist/Jiangniu-v1.0.0.apk
adb install -r test-apk/android-debug-androidTest.apk
# 每轮只保留本次截图，避免启动失败时误收集上次测试的画面。
adb shell rm -f '/sdcard/Android/data/com.jarvistowne.jiangniu/files/cow-smoke/*.png'
python3 - <<'PY'
from pathlib import Path
import shutil
for name in ('screenshots', 'cow-smoke'):
    directory = Path('smoke-results') / name
    if directory.is_dir():
        shutil.rmtree(directory)
PY
set +e
adb shell am instrument -w -r \
  -e class com.shatteredpixel.shatteredpixeldungeon.android.CowSmokeTest \
  com.jarvistowne.jiangniu.test/androidx.test.runner.AndroidJUnitRunner \
  | tee smoke-results/instrumentation.txt
TEST_STATUS=${PIPESTATUS[0]}
set -e
adb logcat -d > smoke-results/logcat.txt
adb pull /sdcard/Android/data/com.jarvistowne.jiangniu/files/cow-smoke smoke-results/ || true
adb exec-out screencap -p > smoke-results/final-screen.png
if [ "$TEST_STATUS" -ne 0 ]; then exit "$TEST_STATUS"; fi
python3 - <<'PY'
from pathlib import Path
text = Path('smoke-results/instrumentation.txt').read_text()
if 'OK (' not in text or 'FAILURES!!!' in text or 'INSTRUMENTATION_FAILED' in text:
    raise SystemExit('Android 冒烟测试未通过，请检查 instrumentation.txt。')
print('Android 安装、开局、转职、技能、存档恢复测试通过。')
PY
