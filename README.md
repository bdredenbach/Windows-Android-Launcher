# Nth Desktop for Android

A Vista-style Android home launcher by Nth Experience. **0.4.1 — Desktop cleanup** removes the redundant desktop Files/File Managers icons, hides home-setup prompts after default selection, adds a pointer-anchored Vista context menu and removes horizontal camera-cutout gutters in landscape. File management stays available in Computer and through the taskbar Files shortcut. It keeps the Control Panel/Explorer features and separate portrait/landscape gadget layouts.

Desktop cleanup runs once on upgrade, preserving other shortcut order, file-manager app selections, linked folders, notes and gadget layouts. Restoring original desktop icons uses Computer, Internet, Notepad and Personalize. The default-home prompts follow Android's actual home-role state and refresh when the launcher resumes or a menu opens. Long-press blank desktop space or right-click with a mouse for the context menu.

The taskbar and dock use the full width available beside Android's navigation controls. Landscape camera-cutout insets no longer shrink the entire launcher horizontally; desktop icons keep their own cutout-safe margins, and portrait top/bottom cutout protection and keyboard/navigation insets are retained.

## Control Panel and Personalization

Open **Start → Control Panel**. Vista's category names, green section headings, task links, navigation pane, address bar and Category/Classic views organize real settings. Search finds matching settings across categories. Desktop Background, Window Color and Appearance, Desktop Icons, Taskbar and Start Menu, Clock and desktop-name pages change the launcher's actual preferences. System shows real Android device, memory and internal-storage information.

Wi-Fi, security, sound, storage, Bluetooth, accessibility, accounts, language and other operating-system settings open the matching Android page, with Android Settings as a fallback. The launcher themes its own settings; it does not replace Android's system Settings app.

## Computer, Explorer and File Managers

**Computer** shows actual internal-storage usage, favorite file shortcuts, linked folders and links to System properties/Control Panel. **Files** opens a native Explorer-style hub with Back/Forward/Up navigation, clickable address breadcrumbs, a scrollable side pane (or Folders menu on narrow screens), search, Tiles/Details views and a status bar.

- Documents, Pictures, Music, Videos and Downloads use Android's file picker and installed viewers.
- **Add a folder…** lets you choose a folder using Android's folder picker. Explorer can then browse its real files and subfolders inside the launcher. Only read access is retained. Open files use Android's installed handlers.
- Folder search checks names in the current directory. Folders sort before files. Details adds size/type/date metadata; hold an item for its properties. Listings show at most 2,000 items and explicitly label that limit. Organize → Refresh reloads the provider.
- Link up to 16 folders. Hold a linked folder on the home page to remove its link and release the retained read grant. An unavailable/revoked location shows a recovery action.
- **File Managers** groups shortcuts to apps you select from your installed programs. It includes the system file picker. Add apps inside the folder; hold an app to remove its shortcut. Open this folder through Computer; the 0.4.1 desktop cleanup keeps it off the default desktop.
- The browser is read-only: it does not move, rename or delete user files. Android's folder picker restricts some locations, including storage roots and the Downloads root on recent Android versions; selecting individual files remains available.
- Explorer/Control Panel navigation, search and view mode survive activity recreation and rotation, together with the existing tool-window state.

## Desktop gadgets

Open **Start → Gadgets**, or hold blank desktop space and choose **Gadgets**. Add or remove gadgets in the gallery. Hold a gadget and drag it onto the desktop to detach it, or drop it on the Sidebar to dock it again. Drop onto another docked gadget to place it before that gadget. Use **⋮** for size presets, docking and removal; drag the **◢** corner grip to resize a floating gadget. Tap its face to use or configure it. The gallery includes optional grid snapping and a desktop layout lock.

| Gadget | Behavior |
| --- | --- |
| Clock | Live local time, ivory/black faces, optional second hand |
| Calendar | Today's date on a tear-off page; tap for the native month grid |
| Weather | User-selected city or optional approximate location, °C/°F, cached forecasts from MET Norway |
| Notes | Yellow sticky note, saved automatically and independently of Notepad |
| Photo Frame | Choose up to 12 images with Android's document picker; slideshow while the launcher is visible |
| System Meter | Actual battery level and physical memory usage on two analog dials |

The initial Sidebar includes Clock, Calendar and Weather. Weather shows a setup prompt until you choose a location; it never displays a sample forecast as real weather. Gadgets stop ticking while the launcher is in the background. Weather requests honor provider cache expiry and remain off when Weather is removed or its dock is hidden. Floating gadgets continue updating while the launcher is visible, even if the Sidebar is hidden.

## Landscape and saved layouts

Enable Android auto-rotate and rotate your device. Landscape uses a matching wide lossless WebP wallpaper, reflows desktop icons and keeps Start navigation scrollable. Portrait and landscape each remember gadget positions, sizes, dock order, Sidebar visibility and built-in window geometry. The first arrangement for a new orientation starts from your current one; changes afterward are independent. Enabled gadgets, notes, photos and preferences are shared.

Open tool windows, their stacking/minimized state, editor cursors, calendar selection, scroll positions and an open Start search survive Android activity recreation, including rotation. Note contents also persist on the device. Saved arrangements survive app restarts; open tool sessions are restored through Android's saved activity state, rather than reopening after a force-stop or explicit fresh launch. Gadgets fit temporarily smaller viewports, including the keyboard, without overwriting their saved position or requested size.

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

Install the supplied test APK, then open **Nth Desktop**. Try it before choosing **Make home**. To switch back: **Android Settings → Apps → Default apps → Home app**. The 0.4.1 handoff APK uses the same preserved test signing key as the earlier supplied builds, so it can be installed as an update.

Notes, selected photo references, linked folder references, file-manager shortcuts, preferences and cached forecasts are stored locally. Photo access uses the document picker's retained grants; no broad photo/storage permission is requested. Approximate location is requested only when you tap **Use my approximate location**. A city can be chosen without granting location access. Forecasts require Internet access; city search depends on the device's Android Geocoder service. Selected weather coordinates are sent to MET Norway. Uninstalling clears local app data.

GitHub Actions uses its runner's debug key. Its APK artifact may not have the same signing identity as the handoff APK. Reuse the private test signing key for in-place hand-built test updates, and never commit signing keys or use the debug key for a production release.

## Build and checks

JDK 17, Android SDK platform 36, Build Tools 35.0.0, Gradle 8.13 and AGP 8.13.2. Minimum Android 8.0 (API 26), target API 36. No third-party runtime-library dependencies.

```sh
./gradlew assembleDebug assembleDebugAndroidTest
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

```sh
mkdir -p build/state-check
javac -d build/state-check app/src/main/java/com/nthexperience/desktop/LauncherState.java checks/LauncherStateCheck.java app/src/main/java/com/nthexperience/desktop/DesktopGeometry.java checks/DesktopGeometryCheck.java
java -cp build/state-check LauncherStateCheck
java -cp build/state-check DesktopGeometryCheck
```

On a disposable emulator:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.LauncherSmoke
```

The dependency-free checks exercise boundary fitting, normalized positions, grid snapping and corrupt geometry. The instrumentation runner exercises real touch drag, docking, corner resizing, layout lock, independent orientation layouts, rotation and explicit activity recreation with note/cursor and Start query continuity. An isolated test folder in the disposable emulator’s Documents directory exercises Android’s real folder picker and storage provider, retained read grants, tree-boundary enforcement, file reads, nested navigation/search and rotation restoration. The test cleans up its own fixture folder; fixture data are absent from the production APK. The runner also checks upgrade cleanup, before/after default-home prompts using the real Android HOME role, the desktop context menu, and taskbar/dock bounds in both landscape rotations with a real emulated cutout and three-button navigation. It also checks Control Panel search/Classic view, actual settings changes and dispatch to Android Wi-Fi settings. It also checks app discovery, Start search/All Programs, note persistence, window controls, wallpaper settings, gadget selection, sticky notes, clock settings, calendar, weather parsing/unit conversion and configuration. It captures real desktop, menu, window and gadget-gallery screens. It does not require live weather or location permission to pass. GitHub Actions builds and runs these checks on Android 11. Physical-device acceptance testing is separate.

## Origin and credits

The original idea was prompted by [jovanovski/windowslauncher](https://github.com/jovanovski/windowslauncher). At inspection it had no declared source license. No source code or proprietary Windows graphics from that project were copied. The application, icons and wallpaper were independently created. This is not a Microsoft product.

The openly licensed Selawik font is copyright Microsoft Corporation and is included under SIL OFL 1.1. Weather data are provided by MET Norway under CC BY 4.0. Gradle's wrapper is build tooling under its own license. Full notices are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Licensing of Nth Experience's application code remains for the owner to decide; no open-source license is selected by this setup.
