#!/usr/bin/env bash
set -euo pipefail

APK="${1:-heart-sounds-v1.1/Asfendiyarov_Heart_Sounds_v1.1.apk}"
EVIDENCE="heart-sounds-v1.1/test-evidence"
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
grep -q "Нормальные тоны сердца" "$EVIDENCE/home.xml"

find_and_tap_description() {
  local target="$1"
  local attempts="${2:-8}"
  local direction="${3:-down}"
  local temp_xml="$EVIDENCE/current.xml"

  for ((attempt=1; attempt<=attempts; attempt++)); do
    adb shell uiautomator dump /sdcard/window.xml >/dev/null
    adb pull /sdcard/window.xml "$temp_xml" >/dev/null

    local coordinates
    coordinates=$(python3 - "$temp_xml" "$target" <<'PY'
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

    if [[ "$direction" == "down" ]]; then
      adb shell input swipe 540 1550 540 550 450
    else
      adb shell input swipe 540 550 540 1550 450
    fi
    sleep 1
  done

  echo "Could not find UI element: $target" >&2
  return 1
}

find_and_tap_description "Проверка звука Bluetooth" 2 down
sleep 6
adb logcat -d -s HeartSounds:I HeartSounds:E '*:S' > "$EVIDENCE/test-signal.log"
grep -q "PLAY_START|title=Проверка звука" "$EVIDENCE/test-signal.log"
grep -q "TEST_COMPLETE" "$EVIDENCE/test-signal.log"
adb exec-out screencap -p > "$EVIDENCE/02-test-complete.png"

find_and_tap_description "Воспроизвести: Аортальный стеноз" 8 down
adb logcat -d -s HeartSounds:I HeartSounds:E '*:S' > "$EVIDENCE/aortic-stenosis.log"
grep -q "PLAY_START|title=Аортальный стеноз" "$EVIDENCE/aortic-stenosis.log"
adb exec-out screencap -p > "$EVIDENCE/03-aortic-stenosis.png"

find_and_tap_description "Воспроизвести: Митральный стеноз" 6 down
adb logcat -d -s HeartSounds:I HeartSounds:E '*:S' > "$EVIDENCE/mitral-stenosis.log"
grep -q "PLAY_START|title=Митральный стеноз" "$EVIDENCE/mitral-stenosis.log"
adb exec-out screencap -p > "$EVIDENCE/04-mitral-stenosis.png"

if grep -q "MEDIA_ERROR\|PLAYBACK_EXCEPTION\|FATAL EXCEPTION" "$EVIDENCE"/*.log; then
  echo "Playback error found in logcat" >&2
  exit 1
fi

adb shell dumpsys package "$PACKAGE" > "$EVIDENCE/package.txt"
grep -q "versionName=1.1.0" "$EVIDENCE/package.txt"

echo "Android emulator smoke test passed."
