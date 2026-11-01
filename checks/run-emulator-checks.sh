#!/usr/bin/env bash
set -euo pipefail
collect() {
  mkdir -p screenshots
  free -m > screenshots/host-memory.txt
  timeout 8s adb logcat -d -t 500 > screenshots/device-log.txt 2>/dev/null || true
  for name in desktop start-menu notepad gadget-gallery gadgets-desktop calendar floating-gadgets landscape landscape-notepad landscape-start landscape-calendar restored-portrait control-panel control-panel-classic personalization system computer computer-landscape file-managers explorer-folder desktop-clean default-home-start desktop-context-menu landscape-cutout-left landscape-cutout-right app-folder folder-portrait folder-landscape quick-launch desktop-organization backup-preview backup-restored backup-recovered failure aero-opaque aero-colors aero-windows startup-pearl welcome-center startup-settings startup-after-reboot aero-failure; do
    timeout 4s adb exec-out run-as com.nthexperience.desktop cat "files/$name.png" > "screenshots/$name.png" 2>/dev/null || true
  done
  timeout 4s adb exec-out run-as com.nthexperience.desktop cat files/picker-tree.txt > screenshots/picker-tree.txt 2>/dev/null || true
  timeout 8s adb exec-out cat /sdcard/Nth-Desktop-Startup.mp4 > screenshots/startup-preview.mp4 2>/dev/null || true
}
trap collect EXIT
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if [[ "$1" == 30 ]]; then
  timeout 300s adb shell am instrument -w -e suite legacy com.nthexperience.desktop.test/com.nthexperience.desktop.AeroSmoke | tee smoke-test.txt
  grep -q 'PASS:' smoke-test.txt
fi
adb shell pm list instrumentation
adb shell am force-stop com.nthexperience.desktop
timeout 150s adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.AeroSmoke | tee aero-test.txt
grep -q 'PASS:' aero-test.txt
adb devices -l
adb reboot
sleep 4
adb kill-server
adb start-server
if ! timeout 35s adb wait-for-device; then
  adb connect localhost:5555
  export ANDROID_SERIAL=localhost:5555
  timeout 70s adb wait-for-device
fi
adb devices -l
ready=0
for attempt in $(seq 1 75); do
  if [[ "$(adb shell getprop sys.boot_completed | tr -d '\r')" == 1 ]]; then ready=1; break; fi
  sleep 2
done
[[ "$ready" == 1 ]]
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell input keyevent KEYCODE_HOME
sleep 4
timeout 90s adb shell am instrument -w -e verify_boot true com.nthexperience.desktop.test/com.nthexperience.desktop.AeroSmoke | tee reboot-test.txt
grep -q 'PASS:' reboot-test.txt
