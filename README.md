# Nth Desktop for Android

A Vista-style Android home launcher by Nth Experience. **0.2.0 — Aero & gadgets** brings a dark reflective taskbar, a two-column Start menu with bottom search, Selawik typography, vertical desktop shortcuts, glass window borders, and a docked gadget Sidebar.

## Desktop gadgets

Open **Start → Gadgets**, or hold blank desktop space and choose **Gadgets**. Add or remove gadgets in the gallery. Hold and drag a gadget onto another gadget to swap their order. Tap a gadget to use or configure it. The Sidebar can be hidden and reopened from the gallery. Its selection and order survive restarts.

| Gadget | Behavior |
| --- | --- |
| Clock | Live local time, ivory/black faces, optional second hand |
| Calendar | Today's date on a tear-off page; tap for the native month grid |
| Weather | User-selected city or optional approximate location, °C/°F, cached forecasts from MET Norway |
| Notes | Yellow sticky note, saved automatically and independently of Notepad |
| Photo Frame | Choose up to 12 images with Android's document picker; slideshow while the launcher is visible |
| System Meter | Actual battery level and physical memory usage on two analog dials |

The initial Sidebar includes Clock, Calendar and Weather. Weather shows a setup prompt until you choose a location; it never displays a sample forecast as real weather. Gadgets stop ticking while the launcher is in the background. Weather requests honor provider cache expiry and remain off when the Sidebar or Weather gadget is hidden.

## Launcher behavior

- Native HOME activity and standard optional default-home chooser.
- Start search and All Programs list real launchable apps in the current Android profile. Hold an app for desktop pin/unpin or Android app info.
- Vertical desktop icon columns. Hold and drag shortcuts onto each other to swap slots; their order is saved.
- Taskbar quick launch, recent-launch shortcuts, and buttons for built-in tool windows. The recent strip does not claim to be a running-app/task manager.
- Drag, minimize, maximize/restore and close built-in tool windows. Android apps open normally under Android's window management.
- Notepad saves automatically on the device. Files and Music use Android's document picker and installed handlers.
- The right edge of the taskbar shows the desktop. Back dismisses Start or minimizes the top built-in tool.
- Three wallpaper/color treatments. The aurora artwork uses lossless WebP; see [artwork and prompt details](ASSETS.md).
- Android's status bar is hidden by default for the desktop look; swipe from the top to reveal system controls, or enable it in Personalization. Navigation and cutout insets remain respected.

These are native built-in desktop gadgets. This release does not yet host third-party Android home-screen widgets, work-profile apps, or move other Android apps into its built-in windows.

## Install and update

Install the supplied test APK, then open **Nth Desktop**. Try it before choosing **Make home**. To switch back: **Android Settings → Apps → Default apps → Home app**. The 0.2.0 handoff APK uses the same preserved test signing key as 0.1.0, so it can be installed as an update.

Notes, selected photo references, preferences and cached forecasts are stored locally. Photo access uses the document picker's retained grants; no broad photo/storage permission is requested. Approximate location is requested only when you tap **Use my approximate location**. A city can be chosen without granting location access. Forecasts require Internet access; city search depends on the device's Android Geocoder service. Selected weather coordinates are sent to MET Norway. Uninstalling clears local app data.

GitHub Actions uses its runner's debug key. Its APK artifact may not have the same signing identity as the handoff APK. Reuse the private test signing key for in-place hand-built test updates, and never commit signing keys or use the debug key for a production release.

## Build and checks

JDK 17, Android SDK platform 36, Build Tools 35.0.0, Gradle 8.13 and AGP 8.13.2. Minimum Android 8.0 (API 26), target API 36. No third-party runtime-library dependencies.

```sh
./gradlew assembleDebug assembleDebugAndroidTest
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

```sh
mkdir -p build/state-check
javac -d build/state-check app/src/main/java/com/nthexperience/desktop/LauncherState.java checks/LauncherStateCheck.java
java -cp build/state-check LauncherStateCheck
```

On a disposable emulator:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.LauncherSmoke
```

The instrumentation runner checks app discovery, Start search/All Programs, note persistence, window controls, wallpaper settings, gadget selection, sticky notes, clock settings, calendar, weather parsing/unit conversion and configuration. It captures real desktop, menu, window and gadget-gallery screens. It does not require live weather or location permission to pass. GitHub Actions builds and runs these checks on Android 11. Physical-device acceptance testing is separate.

## Origin and credits

The original idea was prompted by [jovanovski/windowslauncher](https://github.com/jovanovski/windowslauncher). At inspection it had no declared source license. No source code or proprietary Windows graphics from that project were copied. The application, icons and wallpaper were independently created. This is not a Microsoft product.

The openly licensed Selawik font is copyright Microsoft Corporation and is included under SIL OFL 1.1. Weather data are provided by MET Norway under CC BY 4.0. Gradle's wrapper is build tooling under its own license. Full notices are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Licensing of Nth Experience's application code remains for the owner to decide; no open-source license is selected by this setup.
