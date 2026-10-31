# Nth Desktop for Android

An independently implemented, Vista-inspired Android home launcher by Nth Experience. The first test build is **0.1.0**.

## Implemented in this build

- Native Android HOME activity with an optional standard default-home chooser.
- Original gradient aurora wallpapers, glass-style taskbar, and an original N start orb/icon set.
- Searchable Start menu listing launchable apps in the current Android profile.
- Launch real Android apps; hold a Start menu entry to pin/unpin it or open Android's app info.
- Hold and drag desktop shortcuts onto each other to swap positions. Shortcut order survives restarts.
- Recent-launch shortcuts in the taskbar (these do not claim to be running-app indicators).
- Built-in Notepad with automatic local saving, a clock/calendar, and personalization.
- Drag, minimize, maximize/restore, and close built-in tool windows; minimized tools can be restored from the taskbar.
- System document picker and the device's installed browser/file handlers.
- Portrait/landscape layouts, system-bar/cutout insets, keyboard insets, and package-change refresh.

This version does not host Android widgets or work-profile apps, resize other Android apps, read notifications, or replace Android's Recents/system controls. It requests no network, storage, accessibility, contacts, or notification permissions. The Internet shortcut hands a web URL to the installed browser.

## Try it

Install the debug APK, open **Nth Desktop**, and tap the N orb. You can try it before choosing it as your default home app. Select **Make home** in Start when ready. To return to your previous launcher, open **Android Settings → Apps → Default apps → Home app**.

- Tap a desktop shortcut once to open it.
- Hold a program in Start for desktop options.
- Hold a blank desktop area for personalization and shortcut management.
- Swipe up on blank desktop space to open program search.
- The strip at the far right of the taskbar minimizes built-in tools to show the desktop.
- Back dismisses Start or minimizes the top built-in tool.

Notes and preferences are stored only on the device; uninstalling the app clears them. This is a test build, not a production release.

## Build

Requires JDK 17, Android SDK platform 36, and SDK Build Tools 35.0.0. Open this directory in Android Studio, or set `ANDROID_HOME` to your SDK and run:

```sh
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. The Gradle wrapper uses Gradle 8.13 and Android Gradle Plugin 8.13.2. The app supports Android 8.0 (API 26) and above and targets API 36. No third-party runtime libraries are bundled.

Check the platform-independent shortcut/recent-launch rules:

```sh
mkdir -p build/state-check
javac -d build/state-check app/src/main/java/com/nthexperience/desktop/LauncherState.java checks/LauncherStateCheck.java
java -cp build/state-check LauncherStateCheck
```

## Origin and rights

The feature idea was prompted by [jovanovski/windowslauncher](https://github.com/jovanovski/windowslauncher). At inspection on October 31, 2026, it had no declared source license; its README also states that many assets belong to Microsoft. No source code or graphics from that project were copied here. Our application code, icons, wallpaper, and branding were created independently. This is not a Microsoft product and is not affiliated with Microsoft.

The Gradle wrapper is third-party build tooling supplied by Gradle under its own license. No open-source license is being chosen for Nth Experience's application code in this initial setup; licensing is reserved for the owner to decide.

## Emulator smoke test

A dependency-free instrumentation runner covers installed-app discovery, search, Notepad autosave, window controls, taskbar restore, and personalization. On a disposable emulator:

```sh
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.LauncherSmoke
```

The runner restores the previous note after its checks. It is intended for test devices; do not run it against your primary home configuration.

## First-build validation

The debug APK and instrumentation APK compile successfully locally and on GitHub Actions. The five platform-independent launcher behavior checks pass, and the APK signature is verified. The Android 11 emulator interaction checks pass: app discovery, search, note autosave, minimize/taskbar restore, maximize/restore, close/reopen, and personalization. Acceptance testing on a physical Android device is still pending. Normal pushes build both APK packages and run the emulator smoke test, with bounded timeouts and screenshot evidence.
