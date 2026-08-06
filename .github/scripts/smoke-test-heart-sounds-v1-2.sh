#!/usr/bin/env bash
set -euo pipefail

APK="${1:-heart-sounds-v1.2/Asfendiyarov_Heart_Sounds_v1.2.apk}"
EVIDENCE="heart-sounds-v1.2/test-evidence"
PACKAGE="kz.edu.kaznmu.heartsounds"
ACTIVITY="$PACKAGE/.MainActivity"
mkdir -p "$EVIDENCE"

adb install -r "$APK"
adb logcat -c
adb shell am force-stop "$PACKAGE"
adb shell am start -W -n "$ACTIVITY" | tee "$EVIDENCE/launch.txt"
sleep 3

adb shell uiautomator dump /sdcard/window.xml >/dev/null
adb pull /sdcard/window.xml "$EVIDENCE/home.xml" >/dev/null
adb exec-out screencap -p > "$EVIDENCE/01-home.png"
grep -q "ПРОВЕРКА ЗВУКА" "$EVIDENCE/home.xml"
grep -q "24 реальные записи" "$EVIDENCE/home.xml"

find_and_tap() {
  local target="$1"
  local attempts="${2:-12}"
  for ((attempt=1; attempt<=attempts; attempt++)); do
    adb shell uiautomator dump /sdcard/window.xml >/dev/null
    adb pull /sdcard/window.xml "$EVIDENCE/current.xml" >/dev/null
    local coordinates
    coordinates=$(python3 - "$EVIDENCE/current.xml" "$target" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET

path, target = sys.argv[1], sys.argv[2]
root = ET.parse(path).getroot()
for node in root.iter("node"):
    if node.attrib.get("content-desc") == target or node.attrib.get("text") == target:
        match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print(f"{(x1+x2)//2} {(y1+y2)//2}")
            raise SystemExit(0)
raise SystemExit(1)
PY
    ) || true
    if [[ -n "$coordinates" ]]; then
      adb shell input tap $coordinates
      sleep 2
      return 0
    fi
    adb shell input swipe 540 1600 540 450 450
    sleep 1
  done
  echo "Could not find UI element: $target" >&2
  return 1
}

find_and_tap "Проверка звука Bluetooth" 2
sleep 6
adb logcat -d -s HeartSounds:I HeartSounds:E AndroidRuntime:E '*:S' > "$EVIDENCE/test-signal.log"
grep -q "PLAY_START|title=Проверка звука" "$EVIDENCE/test-signal.log"
adb exec-out screencap -p > "$EVIDENCE/02-test-complete.png"

find_and_tap "Воспроизвести: Adult Case 1" 6
sleep 2
adb logcat -d -s HeartSounds:I HeartSounds:E AndroidRuntime:E '*:S' > "$EVIDENCE/adult-case-1.log"
grep -q "PLAY_START|title=Нормальные тоны сердца — Adult Case 1" "$EVIDENCE/adult-case-1.log"
adb exec-out screencap -p > "$EVIDENCE/03-adult-case-1.png"

find_and_tap "Воспроизвести: Adult Case 3" 8
sleep 2
adb logcat -d -s HeartSounds:I HeartSounds:E AndroidRuntime:E '*:S' > "$EVIDENCE/adult-case-3.log"
grep -q "PLAY_START|title=Митральный стеноз — Adult Case 3" "$EVIDENCE/adult-case-3.log"
adb exec-out screencap -p > "$EVIDENCE/04-adult-case-3.png"

find_and_tap "Воспроизвести: Congenital Case 2" 22
sleep 2
adb logcat -d -s HeartSounds:I HeartSounds:E AndroidRuntime:E '*:S' > "$EVIDENCE/congenital-case-2.log"
grep -q "PLAY_START|title=Стеноз легочной артерии — Congenital Case 2" "$EVIDENCE/congenital-case-2.log"
adb exec-out screencap -p > "$EVIDENCE/05-congenital-case-2.png"

if grep -R -E "MEDIA_ERROR|PLAYBACK_EXCEPTION|FATAL EXCEPTION" "$EVIDENCE"/*.log; then
  echo "Playback or runtime error found" >&2
  exit 1
fi

adb shell dumpsys package "$PACKAGE" > "$EVIDENCE/package.txt"
grep -q "versionName=1.2.0" "$EVIDENCE/package.txt"
echo "Android 15 smoke test passed."
