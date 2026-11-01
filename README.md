# Nth Desktop for Android

A Vista-style Android home launcher by Nth Experience. **0.11.0 — Device folders in Nth Explorer** opens Internal storage, Documents, Pictures, Music, Videos and Downloads in the built-in Vista browser. Optional file-access setup follows the startup animation, with Later and individual-folder alternatives. Device files use the existing navigation, selection, context menus and verified copy/cut engine. All 17 gadgets, Aero settings, linked folders and backups are retained.

Desktop cleanup runs once on upgrade, preserving other shortcut order, file-manager app selections, linked folders, notes and gadget layouts. Restoring original desktop icons uses Computer, Internet, Notepad and Personalize. The default-home prompts follow Android's actual home-role state and refresh when the launcher resumes or a menu opens. Long-press blank desktop space or right-click with a mouse for the context menu.

The taskbar and dock use the full width available beside Android's navigation controls. Landscape camera-cutout insets no longer shrink the entire launcher horizontally; desktop icons keep a consistent grid origin and avoid actual cutout rectangles, and portrait top/bottom cutout protection and keyboard/navigation insets are retained.

## Control Panel and Personalization

Open **Start → Control Panel**. Vista's category names, green section headings, task links, navigation pane, address bar and Category/Classic views organize real settings. Search finds matching settings across categories. Desktop Background, Window Color and Appearance, Desktop Icons, Taskbar and Start Menu, Clock and desktop-name pages change the launcher's actual preferences. System shows real Android device, memory and internal-storage information.

Wi-Fi, security, sound, storage, Bluetooth, accessibility, accounts, language and other operating-system settings open the matching Android page, with Android Settings as a fallback. The launcher themes its own settings; it does not replace Android's system Settings app.

## Computer, Explorer and File Managers

**Computer** opens the primary shared-storage root in Nth Explorer and shows actual internal-storage usage, favorite file shortcuts, linked folders and links to System properties/Control Panel. **Files** opens a native Explorer-style hub with Back/Forward/Up navigation, clickable address breadcrumbs, a scrollable side pane (or Folders menu on narrow screens), search, Tiles/Details views and a status bar.

- With optional device access, **Internal storage** opens the primary shared-storage root; Documents, Pictures, Music, Videos and Downloads open its actual standard folders in the same Nth window. Downloads maps to Android's `Download` directory; Videos maps to `Movies`. A missing standard directory offers an explicit Create action. Existing custom favorite mappings are preserved; hold a favorite and choose **Use device …** to reset it.
- On Android 11+, device browsing uses the **All files access** special setting for Nth. Android 8–10 uses runtime read/write storage grants, with legacy shared access on Android 10. Denial/revocation shows an in-launcher recovery page and leaves individual SAF folder links usable. Permission grants are never imported from a backup.
- The first setup dialog appears after the startup animation (or at the first visible desktop if startup is off/already seen). Later keeps the launcher usable; Choose folders opens Android's folder-access picker. The dialog survives activity recreation and stays dismissed after a choice. Reopen it through **Control Panel → File access** or Welcome Center. Existing installs get it once for this feature.
- The device root is shared user storage, not Linux `/`, operating-system directories or other apps' private data. `Android/data`, `Android/obb` and other protected Android subtrees are blocked; `Android/media` remains shared. Storage root and the Android directory cannot be renamed/deleted. Symbolic links and parent-path escapes are rejected. SD/USB/cloud locations can still be connected with Android's folder picker; direct device browsing currently covers primary shared storage.
- Device files and linked provider files use the same stream verification and capability-aware operations. Open files use installed Android viewers. The bridge is protected with Android's `MANAGE_DOCUMENTS` permission and is not advertised as a system-picker root. Only explicit selected-file read grants reach other apps; root/prefix/write/persistable grants are not returned.
- **Modify system settings**, usage access, accessibility-service access, contacts and phone permissions are not requested for this feature. Google Play separately reviews `MANAGE_EXTERNAL_STORAGE`; see [Android's guidance](https://developer.android.com/training/data-storage/manage-all-files) and [Play's permitted-use/declaration policy](https://support.google.com/googleplay/android-developer/answer/10467955). Store approval is not implied by the working installer.
- **Add a folder…** connects a user-selected folder. Android grants only the access you approve. Existing read-only links stay read-only; **Enable changes for this folder…** reopens Android's access picker to request editing. Android may restrict some roots and app-private locations.
- Click path segments, expand the folder tree, hold Back/Forward for history, or edit a friendly path inside a linked root. Search checks names in the current directory. Sort by name, modified date, type or size, with folders first. Listings show at most 2,000 items and explicitly label that limit.
- Hold or right-click an item for Open/Open with, Cut, Copy, Paste, Rename, Delete, selection and properties. Blank space offers View, Sort, Refresh, Paste, New folder and selection controls. Editing actions depend on the provider's capabilities and actual write grant.
- Copy verifies bytes and a SHA-256 digest by reopening the destination. Cut removes originals only after all selected copies verify and the source tree is checked again. Name collisions keep both files. Transfers support up to 5,000 source entries and 32 levels; destination name queries cap at 10,000 immediate entries. Providers must support creating and reading copied files.
- Delete requires confirmation and permanently deletes real provider files; there is no Recycle Bin or file-operation undo. Cancel, close or activity recreation cancels a transfer. Failed/cancelled copies attempt to remove newly created destination entries; a provider that refuses deletion can leave partial copies. Check the result message before retrying a move. Operations do not continue as a background service.
- The clipboard is local to the running launcher, supports either files or shortcuts, and is excluded from backups. Shortcut deletion removes a launcher link, never uninstalls the app. App folders do not contain other app folders.
- Link up to 16 folders. Removing a link releases its retained grants; it does not delete that folder. Revoked/unavailable links provide a recovery action. File Managers continues to group selected installed apps through Computer.
- Explorer navigation, sort/view mode, expanded folders and bounded selection state survive activity recreation. Active transfers are cancelled on recreation.
- Compatible apps can choose **Nth Explorer** through Android's `ACTION_GET_CONTENT`, or explicitly use our picker contract. It returns temporary read access only to chosen files. Android's `OPEN_DOCUMENT`, `CREATE_DOCUMENT` and `OPEN_DOCUMENT_TREE` screens remain the system permission/save pickers. This cannot replace an arbitrary app's private file browser. See [the integration contract](EXPLORER_INTEGRATION.md).

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
| Network Meter | Measured receive/send byte rates, 32-sample receive graph and interface totals since device boot |
| Compass | Live magnetic north from Android rotation-vector or accelerometer/magnetometer sensors, with screen-rotation correction and accuracy status |
| Tile Puzzle | Solvable fifteen-tile puzzle, legal adjacent moves, move count, New puzzle and saved board |

Each type has its own original native Canvas design: analog clock, tear-off calendar, amber weather card, folded sticky note, wooden photo frame, twin gauges, travel clocks, newspaper, clipboard, brass hourglass, metal chronograph, pocket calculator, ruler/conversion plate, exposed hard drive, network instrument, brass compass and colored wooden tiles. The faces remain sharp when resized. Original six gadget designs and saved contents are preserved.

Network rates are sampled only while Nth is visible and the gadget or tool is enabled/visible. They are aggregate interface byte counters, not carrier billing totals or an Internet speed test; counters may include local traffic and reset at reboot. An unavailable counter is shown as unavailable. Compass sensor subscriptions stop when Nth leaves the foreground; sensor availability/accuracy depend on the phone. It shows magnetic north, needs no location permission and is intended to be held flat. Its status provides calibration guidance. Tile Puzzle shuffles only through legal moves, saves automatically, and rejects impossible imported boards. The gallery has a Games category.

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
- Taskbar Quick Launch and buttons for built-in tool windows. Recently used apps remain in Start and are no longer duplicated after the taskbar divider.
- Drag, minimize, maximize/restore and close built-in tool windows. Android apps open normally under Android's window management.
- Notepad saves automatically on the device. Files and Music use Android's document picker and installed handlers.
- The right edge of the taskbar shows the desktop. Back dismisses Start or minimizes the top built-in tool.
- Three wallpaper/color treatments. The aurora artwork uses lossless WebP; see [artwork and prompt details](ASSETS.md).
- Android's status bar is hidden by default for the desktop look; swipe from the top to reveal system controls, or enable it in Personalization. Navigation and cutout insets remain respected.

These are native built-in desktop gadgets. This release does not yet host third-party Android home-screen widgets, work-profile apps, or move other Android apps into its built-in windows.

## Install and update

Install the supplied test APK, then open **Nth Desktop**. Try it before choosing **Make home**. To switch back: **Android Settings → Apps → Default apps → Home app**. The 0.11.0 handoff APK uses the same preserved test signing key as the earlier supplied builds, so it can be installed as an update.

Notes, selected photo references, linked folder references, file-manager shortcuts, preferences and cached forecasts are stored locally. Photo access uses the document picker's retained grants; device-wide shared-file browsing is an optional separate all-files grant. Approximate location is requested when you tap **Use my approximate location** or enable **Follow my location**. A city can be chosen without granting location access. Forecasts require Internet access; city search uses Photon/OSM. City search text is sent to that service. Selected weather coordinates are sent to MET Norway. Uninstalling clears local app data.

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
adb install -r fixture/build/outputs/apk/debug/fixture-debug.apk
adb shell am instrument -w -e suite storage com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke
adb shell am instrument -w -e suite legacy com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke
adb shell am instrument -w -e suite aero com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke
adb shell am instrument -w -e suite gadgets com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke
adb shell am instrument -w com.nthexperience.desktop.test/com.nthexperience.desktop.StorageSmoke
```

The device-storage suite verifies first-run setup after intro, Later/recreation, actual all-files grants and revocation, all standard directories opening inside Explorer, native filesystem operations, protected/root guards and a separate app receiving only its chosen native file. It also checks denial of write/rename/persistable/root-listing/unselected-file access. The Explorer suite uses a separate fixture APK/UID to test provider grants, verified copy/cut, keep-both naming, creation/rename/permanent-delete confirmation, read-only actions, failed and cancelled copies, self-descendant rejection, navigation, shortcut menus and taskbar cleanup. A separate caller UID verifies single/multiple picker results, MIME filtering, recreation, cancellation and denial of write/unselected-file access. Test and fixture APKs are not part of the delivered launcher. The dependency-free checks exercise boundary fitting, normalized positions, grid snapping and corrupt geometry. The instrumentation runner exercises real touch drag, docking, corner resizing, layout lock, independent orientation layouts, rotation and explicit activity recreation with note/cursor and Start query continuity. An isolated test folder in the disposable emulator’s Documents directory exercises Android’s real folder picker and storage provider, retained read grants, tree-boundary enforcement, file reads, nested navigation/search and rotation restoration. The test cleans up its own fixture folder; fixture data are absent from the production APK. The runner also checks upgrade cleanup, before/after default-home prompts using the real Android HOME role, the desktop context menu, and taskbar/dock bounds in both landscape rotations with a real emulated cutout and three-button navigation. It also checks Control Panel search/Classic view, actual settings changes and dispatch to Android Wi-Fi settings. It also checks app discovery, Start search/All Programs, note persistence, window controls, wallpaper settings, gadget selection, sticky notes, clock settings, calendar, weather parsing/unit conversion and configuration. It captures real desktop, menu, window and gadget-gallery screens. The established suite does not require live weather. The new gadget suite grants approximate/mock location only in disposable emulators to exercise real Android provider callbacks; those test grants and fixtures are not bundled into the production APK. The runner also groups shortcuts using real touch input, renames/reorders/dissolves a folder, checks folder continuity through rotation and edits Quick Launch. It saves and opens an actual backup through Android’s document picker, verifies the preview leaves live settings unchanged, restores both orientation layouts and Unicode notes, undoes the restore and rejects malformed imports. GitHub Actions runs the full established suite on Android 11. A focused Aero/startup suite also runs on Android 16, with real emulator reboots on both versions. It checks independent color controls, active/inactive windows, live minimize/restore, old backup compatibility, first-run skip/Welcome Center, ordinary Home suppression, preview cancellation during rotation, reduced motion and once-per-reboot identity. It also exercises opacity visibility, persistence, backup compatibility and exact 0/50/100% rendered pixels for every glass style; grouped Start search and destinations; profile validation; and native tray/calendar behavior with actual media volume changes. Android 11 chooses a real portrait and linked file through the document picker and checks retained read permission. It records an actual startup preview. The focused gadget suite adds real caption-icon rendering comparisons, seventeen-gadget enablement and backup validation, calculator/converter/checklist actions, measured stopwatch/countdown behavior, world zones, storage, RSS/Atom and city-response parsing, activity recreation and Android fused test-provider follow/pause/resume callbacks. The added checks exercise legal puzzle moves and solvability, network delta/reset handling, magnetic-bearing screen transforms, hardware lifecycle, and fixed icon columns in both landscape directions. The full Android 11 SystemUI image covers real status bars, notification-shade return and both three-button/gesture navigation. The Android 16 ATD image has no SystemUI; it covers rotations, layout bounds and gadget behavior. Physical-device acceptance testing is separate.

## Origin and credits

The original idea was prompted by [jovanovski/windowslauncher](https://github.com/jovanovski/windowslauncher). At inspection it had no declared source license. No source code or proprietary Windows graphics from that project were copied. The application, icons and wallpaper were independently created. This is not a Microsoft product.

The openly licensed Selawik font is copyright Microsoft Corporation and is included under SIL OFL 1.1. Weather data are provided by MET Norway under CC BY 4.0. Gradle's wrapper is build tooling under its own license. Full notices are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Licensing of Nth Experience's application code remains for the owner to decide; no open-source license is selected by this setup.

## Desktop organization and backup

Drag a shortcut onto the center of another to create a folder, or near its edge to swap positions. Use Desktop Organization to rename, reorder, add or remove shortcuts; dissolving a folder returns its contents to the desktop. Quick Launch can hold up to 12 chosen shortcuts, including folders. The desktop context menu includes New folder, icon sizes and sorting.

Control Panel → Backup and Restore exports desktop settings, app folders, names, Quick Launch, notes and both orientation layouts to a user-chosen JSON file. Opening a backup displays a preview; applying it keeps the previous setup for Undo last restore. Backups contain references to linked folders and photos, not their contents or portable access grants. Restore on another device may require relinking those locations. Unknown/newer backup versions, malformed data, nested folders, invalid settings and excessively large files are rejected before changing live settings.
