---
description: "Capture the real screen over adb and compare it with the approved reference by measured bounds and colour, not by eye."
agent: build
subtask: true
---
Run visual QA for $ARGUMENTS with the `visual-qa` agent (or inline following its brief).

1. Check the device first: `adb devices` · `adb shell wm size` · `adb shell wm density` · `adb shell settings get system font_scale`. Zero devices is a declared blocker, not a result.
2. Capture `adb exec-out screencap -p > /tmp/opencode/shot.png` and the hierarchy `adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml`.
3. Read text with `python3 tools/read_image_text.py /tmp/opencode/shot.png --json`; take real element bounds from the dump.
4. Produce the comparison table (item · reference · implemented · delta · verdict) with numbers, plus the states tested: Arabic RTL, largest font scale, landscape, empty/error/disabled.
5. Never write "100% match" or an estimated similarity; anything not measured is reported as not measured. Do not edit code.
