# Nth Desktop for Android

A Vista-style Android home launcher by Nth Experience. **0.8.0 — A bigger gadget gallery** expands the native gallery from 6 to 14 working gadget types, corrects gadget window icons, adds optional foreground weather location following and replaces device-dependent city search with bounded Photon/OSM searches. It retains glass opacity, custom profile pictures, Start search and the system tray from 0.7.0. It keeps the Aero controls, window motion, optional original Nth startup and Welcome Center from 0.6.0. It retains the native Control Panel, Explorer, app folders, Quick Launch, backup/undo, gadgets and both landscape layouts from 0.5.0.

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

Open **Start → Gadgets**, or hold blank desktop space and choose **Gadgets**. Add or remove gadgets in the gallery. Hold a gadget and drag it onto the desktop to detach it, or drop it on the Sidebar to dock it again. Drop onto another docked gadget to place it before that gadget. Use **⋮** for size presets, docking and removal; drag the **◢** corner grip to resize a floating gadget. Tap its face to use or configure it. The gallery includes search, categories, per-gadget details/settings, optional grid snapping and a desktop layout lock. The six original gadget types remain compatible with saved layouts and notes. Each type can be enabled once; World Clock contains two independently chosen zones.

| Gadget | Behavior |
| --- | --- |
| Clock | Live local time, ivory/black faces, optional second hand |
| Calendar | Today's date on a tear-off page; tap for the native month grid |
| Weather | Fixed city, one-time approximate location or foreground Follow my location; °C/°F and cached MET Norway forecasts |
| Notes | Yellow sticky note, saved automatically and independently of Notepad |
| Photo Frame | Choose up to 12 images with Android's document picker; slideshow while the launcher is visible |
| System Meter | Actual battery level and physical memory usage on two analog dials |
| World Clock | Two independently selected Android time zones with local date/time |
| Feed Headlines | User-selected HTTPS RSS/Atom feed; cached titles and working article links |
| Checklist | Add, check, delete and clear completed saved tasks |
| Countdown | Start, pause, resume and reset a persistent countdown |
| Stopwatch | Real elapsed time, pause/reset and up to 20 saved laps |
| Calculator | Parentheses, arithmetic, unary signs, decimals/exponents and postfix percent |
| Unit Converter | Length, mass, temperature, speed, area, volume, data and time |
| Storage Meter | Actual available/used/total internal storage and Android storage settings |

The initial Sidebar includes Clock, Calendar and Weather. Weather shows a setup prompt until you choose a location; it never displays a sample forecast as real weather. Gadgets stop ticking while the launcher is in the background. Weather and feed requests honor cache expiry. They run while a corresponding face or its settings/reader window is visible, and stop initiating requests when Nth is in the background. Floating gadgets continue updating while the launcher is visible, even if the Sidebar is hidden.

### Weather and following your location

Open Weather settings. **Follow my location** is optional and off by default. It uses permitted approximate fixes from fused/network providers, accepts recent usable cached fixes, and requests new updates while Nth is visible. Returning Home refreshes the area. It requests no background location permission, runs no location foreground service and does not track while another app is in front. Provider updates are limited to five-minute/three-kilometre requests; a forecast is changed only for a material area change. Choosing a fixed city turns following off. Denial and disabled providers have clear recovery text and a Device location settings button. If no fresh fix arrives, existing location/forecast data remain in place.

Manual city search uses Photon/OSM with request timeouts, cancellation, stale-result rejection and a small in-memory query cache. It has a dedicated worker, so a slow geocoder cannot block app discovery or backups. Forecast requests still use MET Norway and round coordinates to three decimals. A new area needs a network connection; saved forecasts remain available offline for their area. Availability and forecast freshness depend on the providers.

### Gadget data and timing

New gadget contents and settings are included in desktop backups. The original sticky note still uses its original saved text. Checklist entries are limited to 50 and stopwatch laps to 20. Running timers use elapsed device time within a boot and wall time after a reboot; portable backups discard device-specific uptime anchors. Countdown completion appears when the gadget or tool is visible; it does not create an Android alarm or background notification. Stopwatch and countdown values are recovered when Nth resumes. Removed gadgets keep their saved content for when they are added again.

Feed Headlines accepts HTTPS RSS/Atom feeds and stores up to 12 article titles/links, with a minimum 15-minute cache. It supports relative Atom links, rejects unsafe URL schemes and XML document declarations, and keeps cached headlines on a fetch failure. It does not download full articles. The optional NASA button fills its public feed address; saving the address adds the configured gadget. Feed and forecast caches are excluded from portable backups. Existing Windows `.gadget` archives are not installed; this gallery contains original native Android implementations.

Reference galleries: [WinCustomize Sidebar Gadgets](https://www.wincustomize.com/explore/sidebar_gadgets) and [Notebooks’ gadget-gallery walkthrough](https://notebooks.com/2010/11/29/how-to-find-and-add-windows-gadgets/). We used their categories and gallery behavior as references; no third-party gadget code or artwork is bundled.

## Landscape and saved layouts

Enable Android auto-rotate and rotate your device. Landscape uses a matching wide lossless WebP wallpaper, reflows desktop icons and keeps Start navigation scrollable. Portrait and landscape each remember gadget positions, sizes, dock order, Sidebar visibility and built-in window geometry. The first arrangement for a new orientation starts from your current one; changes afterward are independent. Enabled gadgets, notes, photos and preferences are shared.

Open tool windows, their stacking/minimized state, editor cursors, calendar selection, scroll positions and an open Start search survive Android activity recreation, including rotation. Note contents also persist on the device. Saved arrangements survive app restarts; open tool sessions are restored through Android's saved activity state, rather than reopening after a force-stop or explicit fresh launch. Gadgets fit temporarily smaller viewports, including the keyboard, without overwriting their saved position or requested size.

## Start menu and system tray

Click the Start profile picture, or open **Control Panel → User Accounts → Change your desktop name**, to choose a picture. Nth copies a centered 256px WebP portrait into its private settings. That small copy is included in backups and does not depend on keeping the original image accessible. “Use the Nth picture” restores the original orb.

Start search finds installed apps (including renamed shortcuts), the same working settings catalog used by Control Panel, the saved Notepad and sticky-note text, and names of files you explicitly add through **Computer → Add a file to Start search**. It does not scan storage or document contents. Hold a linked file in Computer to remove it from search. File bookmarks are included in backups, but read grants must be authorized again on another device. A moved/deleted file may need to be chosen again.

The taskbar's battery, network, volume and clock controls open compact Vista-style flyouts. Battery shows Android's level, charging state and Battery Saver. Network reports the default transport and verified/local/offline status without requesting Wi-Fi names or location. Volume changes the real **media** stream and provides mute/restore; fixed-volume outputs direct the user to Sound settings. Calendar browses months and links to the full Calendar tool and Android date/time settings. Flyouts close on outside tap, Back, Home, leaving the launcher or rotation. Subscriptions run only while the launcher is in front.

## Aero appearance and motion

Open **Personalization → Window Color and Appearance**. Choose Frost, Graphite, Seafoam, Sage, Lavender, Gold, Ruby or Clear, then adjust color intensity and glass transparency. When **Enable glass transparency** is checked, **Glass opacity** appears: 0% removes the glass surface, 100% makes it opaque. It updates all Nth glass frames, Start, taskbar, sidebar and tray frames live; text, buttons and content panes remain readable. Turning transparency off hides the slider and uses opaque glass, while remembering your percentage. Restore default glass resets opacity to 85%. These controls affect the launcher’s glass independently of its wallpaper. Opaque mode avoids wallpaper sampling. The softened background is the wallpaper; it is not a capture of another Android app.

Native Nth windows have a soft caption glow and distinct active/inactive frames. Touch a window to activate it. Minimize shrinks toward the taskbar; its taskbar button restores it with the saved window geometry and note contents intact. Buttons, checks, radio choices, scrollbars, popup surfaces and launcher-owned dialogs use matching Vista-era treatments. Android file pickers, permission screens and other system settings keep their native interface.

## Startup and Welcome Center

Open **Personalization → Startup and Sounds**. The default is **First launch only**, with the original chime **off**. Other choices are **Once per phone reboot** and **Off**; **Preview startup** works without changing automatic-playback identity. The introduction lasts about three seconds: a restrained green loading sweep, the original glowing Nth pearl, then Welcome. **Skip** and Android Back dismiss it immediately.

Startup runs inside the home activity when it is visible and unlocked. It does not replace Android’s boot animation. Ordinary Home presses, returning from another app, Activity rotation and process recreation in the same boot do not replay it. Reboot identity uses Android’s boot counter and separate device-local state, which is excluded from desktop backups. If the boot counter is unavailable, automatic playback falls back to the initial introduction. The first introduction is also shown when updating an earlier Nth Desktop release that has never presented it.

The chime is generated from original overlapping tones, with no downloaded Windows audio. It respects silent/vibrate mode, system sound volume and Do Not Disturb, and stays quiet while music is playing. Skip/backgrounding stops it. **Reduce motion** and Android’s animation setting disable window transitions and use a short, silent still welcome. A backgrounded introduction is dismissed, rather than starting over when you return. Android 12+ keeps its standard system splash before app content; there is no separate splash activity.

**Welcome Center** opens after the first introduction and remains available from Start and Control Panel. Its working links lead to Personalization, app folders, gadgets, backup and default-home selection when needed. An optional checkbox opens it at later Nth Desktop starts; it is not a screen-by-screen tour.

## Launcher behavior

- Native HOME activity and standard optional default-home chooser.
- Start search and All Programs list real launchable apps in the current Android profile. Hold an app for desktop pin/unpin, naming, Quick Launch, folder placement or Android app info.
- Vertical desktop icon columns. Hold and drag a shortcut onto the center of another to group them, or near its edge to swap slots; their order is saved.
- Taskbar quick launch, recent-launch shortcuts, and buttons for built-in tool windows. The recent strip does not claim to be a running-app/task manager.
- Drag, minimize, maximize/restore and close built-in tool windows. Android apps open normally under Android's window management.
- Notepad saves automatically on the device. Files and Music use Android's document picker and installed handlers.
- The right edge of the taskbar shows the desktop. Back dismisses Start or minimizes the top built-in tool.
- Three wallpaper/color treatments. The aurora artwork uses lossless WebP; see [artwork and prompt details](ASSETS.md).
- Android's status bar is hidden by default for the desktop look; swipe from the top to reveal system controls, or enable it in Personalization. Navigation and cutout insets remain respected.

These are native built-in desktop gadgets. This release does not yet host third-party Android home-screen widgets, work-profile apps, or move other Android apps into its built-in windows.

## Install and update

Install the supplied test APK, then open **Nth Desktop**. Try it before choosing **Make home**. To switch back: **Android Settings → Apps → Default apps → Home app**. The 0.8.0 handoff APK uses the same preserved test signing key as the earlier supplied builds, so it can be installed as an update.

Notes, selected photo references, linked folder references, file-manager shortcuts, preferences and cached forecasts are stored locally. Photo access uses the document picker's retained grants; no broad photo/storage permission is requested. Approximate location is requested when you tap **Use my approximate location** or enable **Follow my location**. A city can be chosen without granting location access. Forecasts require Internet access; city search uses Photon/OSM. City search text is sent to that service. Selected weather coordinates are sent to MET Norway. Uninstalling clears local app data.

GitHub Actions uses its runner's debug key. Its APK artifact may not have the same signing identity as the handoff APK. Reuse the private test signing key for in-place hand-built test updates, and never commit signing keys or use the debug key for a production release.

## Build and checks

JDK 17, Android SDK platform 36, Build Tools 35.0.0, Gradle 8.13 and AGP 8.13.2. Minimum Android 8.0 (API 26), target API 36. No third-party runtime-library dependencies.

```sh
./gradlew assembleDebug assembleDebugAndroidTest
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

```sh
mkdir -p build/state-check
javac -d build/state-check app/src/main/java/com/nthexperience/desktop/LauncherState.java checks/LauncherStateCheck.java app/src/main/java/com/nthexperience/desktop/DesktopGeometry.java checks/DesktopGeometryCheck.java app/src/main/java/com/nthexperience/desktop/StartupPolicy.java checks/StartupPolicyCheck.java
java -cp build/state-check LauncherStateCheck
java -cp build/state-check DesktopGeometryCheck
java -cp build/state-check StartupPolicyCheck
```

On a disposable emulator:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e suite legacy com.nthexperience.desktop.test/com.nthexperience.desktop.GadgetSmoke
adb shell am instrument -w -e suite aero com.nthexperience.desktop.test/com.nthexperience.desktop.GadgetSmoke
adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.GadgetSmoke
```

The dependency-free checks exercise boundary fitting, normalized positions, grid snapping and corrupt geometry. The instrumentation runner exercises real touch drag, docking, corner resizing, layout lock, independent orientation layouts, rotation and explicit activity recreation with note/cursor and Start query continuity. An isolated test folder in the disposable emulator’s Documents directory exercises Android’s real folder picker and storage provider, retained read grants, tree-boundary enforcement, file reads, nested navigation/search and rotation restoration. The test cleans up its own fixture folder; fixture data are absent from the production APK. The runner also checks upgrade cleanup, before/after default-home prompts using the real Android HOME role, the desktop context menu, and taskbar/dock bounds in both landscape rotations with a real emulated cutout and three-button navigation. It also checks Control Panel search/Classic view, actual settings changes and dispatch to Android Wi-Fi settings. It also checks app discovery, Start search/All Programs, note persistence, window controls, wallpaper settings, gadget selection, sticky notes, clock settings, calendar, weather parsing/unit conversion and configuration. It captures real desktop, menu, window and gadget-gallery screens. The established suite does not require live weather. The new gadget suite grants approximate/mock location only in disposable emulators to exercise real Android provider callbacks; those test grants and fixtures are not bundled into the production APK. The runner also groups shortcuts using real touch input, renames/reorders/dissolves a folder, checks folder continuity through rotation and edits Quick Launch. It saves and opens an actual backup through Android’s document picker, verifies the preview leaves live settings unchanged, restores both orientation layouts and Unicode notes, undoes the restore and rejects malformed imports. GitHub Actions runs the full established suite on Android 11. A focused Aero/startup suite also runs on Android 16, with real emulator reboots on both versions. It checks independent color controls, active/inactive windows, live minimize/restore, old backup compatibility, first-run skip/Welcome Center, ordinary Home suppression, preview cancellation during rotation, reduced motion and once-per-reboot identity. It also exercises opacity visibility, persistence, backup compatibility and exact 0/50/100% rendered pixels for every glass style; grouped Start search and destinations; profile validation; and native tray/calendar behavior with actual media volume changes. Android 11 chooses a real portrait and linked file through the document picker and checks retained read permission. It records an actual startup preview. The focused gadget suite adds real caption-icon rendering comparisons, fourteen-gadget enablement and backup validation, calculator/converter/checklist actions, measured stopwatch/countdown behavior, world zones, storage, RSS/Atom and city-response parsing, activity recreation and Android fused test-provider follow/pause/resume callbacks. Physical-device acceptance testing is separate.

## Origin and credits

The original idea was prompted by [jovanovski/windowslauncher](https://github.com/jovanovski/windowslauncher). At inspection it had no declared source license. No source code or proprietary Windows graphics from that project were copied. The application, icons and wallpaper were independently created. This is not a Microsoft product.

The openly licensed Selawik font is copyright Microsoft Corporation and is included under SIL OFL 1.1. Weather data are provided by MET Norway under CC BY 4.0. Gradle's wrapper is build tooling under its own license. Full notices are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Licensing of Nth Experience's application code remains for the owner to decide; no open-source license is selected by this setup.

## Desktop organization and backup

Drag a shortcut onto the center of another to create a folder, or near its edge to swap positions. Use Desktop Organization to rename, reorder, add or remove shortcuts; dissolving a folder returns its contents to the desktop. Quick Launch can hold up to 12 chosen shortcuts, including folders. The desktop context menu includes New folder, icon sizes and sorting.

Control Panel → Backup and Restore exports desktop settings, app folders, names, Quick Launch, notes and both orientation layouts to a user-chosen JSON file. Opening a backup displays a preview; applying it keeps the previous setup for Undo last restore. Backups contain references to linked folders and photos, not their contents or portable access grants. Restore on another device may require relinking those locations. Unknown/newer backup versions, malformed data, nested folders, invalid settings and excessively large files are rejected before changing live settings.
