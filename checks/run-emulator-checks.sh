#!/usr/bin/env bash
set -euo pipefail
collect() {
  mkdir -p screenshots
  free -m > screenshots/host-memory.txt
  if ! timeout -k 2s 4s adb shell true; then return; fi
  timeout -k 2s 5s adb exec-out run-as com.nthexperience.desktop cat shared_prefs/desktop.xml > screenshots/test-desktop-prefs.xml 2>/dev/null || true
  timeout -k 2s 5s adb shell dumpsys window > screenshots/test-window-state.txt 2>/dev/null || true
  timeout -k 2s 8s adb logcat -d -t 500 > screenshots/device-log.txt 2>/dev/null || true
  for name in file-open-install-blocked file-open-install file-open-installed file-open-unsupported file-open-failure storage-setup storage-root storage-documents storage-device-menu storage-picker storage-revoked storage-failure desktop start-menu notepad gadget-gallery gadgets-desktop calendar floating-gadgets landscape landscape-notepad landscape-start landscape-calendar restored-portrait control-panel control-panel-classic personalization system computer computer-landscape file-managers explorer-folder desktop-clean default-home-start desktop-context-menu landscape-cutout-left landscape-cutout-right app-folder folder-portrait folder-landscape quick-launch desktop-organization backup-preview backup-restored backup-recovered failure aero-opaque aero-colors aero-windows startup-pearl welcome-center startup-settings aero-failure glass-opacity glass-start-0 glass-start-50 glass-start-100 start-search start-portrait tray-battery tray-network tray-volume tray-calendar gadget-gallery-expanded gadget-pack-desktop gadget-calculator gadget-converter gadget-checklist gadget-stopwatch gadget-countdown gadget-world-clock gadget-headlines explorer-navigation explorer-file-menu explorer-folder-menu explorer-copied-files explorer-read-only explorer-picker explorer-picker-multiple taskbar-no-recents explorer-failure weather-follow-settings gadget-network gadget-compass gadget-puzzle gadget-designs aligned-threebutton-0-status-hidden aligned-threebutton-0-status-shown aligned-threebutton-1-status-hidden aligned-threebutton-1-status-shown aligned-gestural-0-status-hidden aligned-gestural-0-status-shown aligned-gestural-1-status-hidden aligned-gestural-1-status-shown gadgets-failure; do
    timeout -k 2s 4s adb exec-out run-as com.nthexperience.desktop cat "files/$name.png" > "screenshots/$name.png" 2>/dev/null || true
  done
  timeout -k 2s 4s adb exec-out run-as com.nthexperience.desktop cat files/file-open-tree.txt > screenshots/file-open-tree.txt 2>/dev/null || true
  timeout -k 2s 4s adb exec-out run-as com.nthexperience.desktop cat files/storage-tree.txt > screenshots/storage-tree.txt 2>/dev/null || true
  timeout -k 2s 4s adb exec-out run-as com.nthexperience.desktop cat files/explorer-tree.txt > screenshots/explorer-tree.txt 2>/dev/null || true
  timeout -k 2s 4s adb exec-out run-as com.nthexperience.desktop cat files/picker-tree.txt > screenshots/picker-tree.txt 2>/dev/null || true
  timeout -k 2s 8s adb exec-out cat /sdcard/Nth-Desktop-Startup.mp4 > screenshots/startup-preview.mp4 2>/dev/null || true
}
trap collect EXIT
adb install -r fixture/build/outputs/apk/debug/fixture-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Permission changes can restart an Android app; apply them outside instrumentation.
adb shell appops set --uid com.nthexperience.desktop MANAGE_EXTERNAL_STORAGE deny
timeout -k 3s 90s adb shell am instrument -w -e suite storage -e phase setup com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke | tee storage-setup-test.txt
grep -q 'PASS:' storage-setup-test.txt
adb shell am force-stop com.nthexperience.desktop
adb shell appops set --uid com.nthexperience.desktop MANAGE_EXTERNAL_STORAGE allow
timeout -k 3s 150s adb shell am instrument -w -e suite storage -e phase access com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke | tee storage-access-test.txt
grep -q 'PASS:' storage-access-test.txt
adb shell mkdir -p /sdcard/Download
adb push installprobe/build/outputs/apk/debug/installprobe-debug.apk /sdcard/Download/Nth-Install-Probe.apk
adb shell am force-stop com.nthexperience.desktop
adb shell appops set com.nthexperience.desktop REQUEST_INSTALL_PACKAGES deny
timeout -k 3s 90s adb shell am instrument -w -e phase blocked com.nthexperience.desktop.test/com.nthexperience.desktop.FileOpenSmoke | tee file-open-blocked-test.txt
grep -q 'PASS:' file-open-blocked-test.txt
adb shell am force-stop com.nthexperience.desktop
adb shell appops set com.nthexperience.desktop REQUEST_INSTALL_PACKAGES allow
timeout -k 3s 230s adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.FileOpenSmoke | tee file-open-test.txt
grep -q 'PASS:' file-open-test.txt
adb shell rm -f /sdcard/Download/Nth-Install-Probe.apk
adb shell appops set com.nthexperience.desktop REQUEST_INSTALL_PACKAGES deny
adb shell am force-stop com.nthexperience.desktop
adb shell appops set --uid com.nthexperience.desktop MANAGE_EXTERNAL_STORAGE deny
timeout -k 3s 60s adb shell am instrument -w -e suite storage -e phase revoked com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke | tee storage-test.txt
grep -q 'PASS:' storage-test.txt
adb shell am force-stop com.nthexperience.desktop
timeout -k 3s 260s adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke | tee explorer-test.txt
grep -q 'PASS:' explorer-test.txt
if [[ "$1" == 30 ]]; then
  timeout -k 3s 300s adb shell am instrument -w -e suite legacy com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke | tee smoke-test.txt
  grep -q 'PASS:' smoke-test.txt
fi
adb shell pm list instrumentation
adb shell am force-stop com.nthexperience.desktop
timeout -k 3s 230s adb shell am instrument -w -e suite aero com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke | tee aero-test.txt
grep -q 'PASS:' aero-test.txt
adb shell am force-stop com.nthexperience.desktop
timeout -k 3s 230s adb shell am instrument -w -e suite gadgets com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke | tee gadgets-test.txt
grep -q 'PASS:' gadgets-test.txt
adb devices -l
timeout -k 3s 10s adb reboot
sleep 4
timeout -k 3s 8s adb kill-server || true
timeout -k 3s 8s adb start-server
if ! timeout -k 3s 35s adb wait-for-device; then
  adb connect localhost:5555
  export ANDROID_SERIAL=localhost:5555
  timeout -k 3s 70s adb wait-for-device
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
adb shell am start -W -n com.nthexperience.desktop/.MainActivity
# Boot completion, Activity resume and keyguard dismissal are distinct asynchronous events.
# Observe the launcher's own committed boot identity before taking the first snapshot.
boot=$(adb shell settings get global boot_count | tr -d '\r')
for attempt in $(seq 1 25); do
  adb exec-out run-as com.nthexperience.desktop cat shared_prefs/startup_session.xml > reboot-first.xml
  if python3 -c 'import sys,xml.etree.ElementTree as E; n=E.parse("reboot-first.xml").getroot().find("int[@name=\"boot\"]"); sys.exit(0 if n is not None and n.attrib["value"]==sys.argv[1] else 1)' "$boot"; then break; fi
  adb shell wm dismiss-keyguard
  sleep 1
done
printf 'CHECK: verifying the first visible launcher after a real reboot\n'
boot=$(adb shell settings get global boot_count | tr -d '\r')
adb exec-out run-as com.nthexperience.desktop cat shared_prefs/startup_session.xml > reboot-first.xml
mkdir -p screenshots
adb shell dumpsys window > screenshots/reboot-first-window.txt
adb shell dumpsys activity activities > screenshots/reboot-first-activity.txt
adb shell cat /proc/uptime > screenshots/reboot-first-uptime.txt
adb logcat -d -t 1000 > screenshots/reboot-first-log.txt
adb shell input keyevent KEYCODE_HOME
adb shell input keyevent KEYCODE_HOME
adb shell am force-stop com.nthexperience.desktop
adb shell am start -W -n com.nthexperience.desktop/.MainActivity
sleep 2
adb exec-out run-as com.nthexperience.desktop cat shared_prefs/startup_session.xml > reboot-again.xml
python3 checks/VerifyReboot.py "$boot" reboot-first.xml reboot-again.xml | tee reboot-test.txt
grep -q 'PASS:' reboot-test.txt
mkdir -p screenshots
adb exec-out screencap -p > screenshots/startup-after-reboot.png
