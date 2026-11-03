# Vista desktop polish — 0.20

The implementation follows the Vista taskbar/window interaction model while using actual Nth windows and supported Android actions.

## References reviewed

- [Microsoft Shell: The Taskbar](https://learn.microsoft.com/en-us/windows/win32/shell/taskbar). Explicitly identifies Vista's Quick Launch, task buttons, window arrangement, Show Desktop, Start Task Manager, Properties and pre-Windows 7 context-menu behavior.
- [Microsoft's Vista launch demonstration transcript, January 2007](https://news.microsoft.com/speeches/steve-ballmer-new-york-business-launch-of-windows-vista-2007-microsoft-office-system-and-microsoft-exchange-server-2007/). Demonstrates multitasking, document identity in task buttons and thumbnails, Alt+Tab and Flip 3D. Transcript reviewed; full video playback is not claimed.
- [Microsoft CES 2007 demonstration transcript](https://news.microsoft.com/speeches/bill-gates-robbie-bach-2007-international-consumer-electronics-show-ces/). Reviews Instant Search and desktop discovery. Transcript reviewed, rather than claiming to have watched archived footage.
- [Microsoft Vista accessibility walkthrough](https://blogs.microsoft.com/accessibility/getting-started-with-windows-vista-using-a-keyboard/). Explains the desktop/Start/Quick Launch/notification-area order, Win+D, keyboard navigation and search.
- [Microsoft window management guidance](https://learn.microsoft.com/en-us/windows/win32/uxguide/win-window-mgt). Standard title controls, top-level taskbar presence and usable work-area bounds. This is a later Windows 7 guide; shared rules are used without treating its Windows 7-specific features as Vista behavior.
- [Microsoft menu guidance](https://learn.microsoft.com/en-us/windows/win32/uxguide/cmd-menus). Contextual actions, checked states and disabled unavailable commands. Also explicitly a later Windows 7 guide.
- [Microsoft Aero icon guidance](https://learn.microsoft.com/en-us/windows/win32/uxguide/vis-icons). Original dimensional artwork and recognizable small-size silhouettes. No Microsoft binaries are bundled.
- [Microsoft notification-area guidance](https://learn.microsoft.com/en-us/windows/win32/uxguide/winenv-notification). Distinguishes Quick Launch from application windows and notification icons.
- [O'Reilly Vista Video LiveLessons](https://www.oreilly.com/videos/microsoft-windows-vista/9780768682434/). Reviewed the published lesson overview for initial tour, taskbar, Explorer, Sidebar and shutdown. Subscriber videos were not played; behavioral implementation is grounded in the accessible Microsoft sources above.
- [Android ActivityManager](https://developer.android.com/reference/android/app/ActivityManager) and [Process](https://developer.android.com/reference/android/os/Process). Supported memory readings and own-process elapsed CPU time; Android's restrictions on other apps' tasks/processes govern the adaptation.

## Implemented adaptation

Hold/right-click the taskbar background or its grip for the shared Vista-style menu: Quick Launch, Cascade, Stacked, Side by Side, Undo Arrangement, Show Desktop/Show Open Windows, Start Task Manager and Properties. The existing edge desktop button also toggles restoration. Restoring the desktop restores only windows hidden by that action, preserves already minimized windows and retains its restore set through Activity recreation.

Task Manager has Applications and Performance tabs, File/View menus, selection, Switch To, End Task for Nth windows, New Task through Start search and App Info for actual Android session entries. External entries are labelled Recently opened, never falsely marked Running or given a fake process kill. Performance shows device physical memory, Nth Java heap and Nth own-process CPU time normalized to one core. Charts are bounded histories sampled every two seconds while visible and active, with pause/refresh controls. No new permissions or dependencies are added. The manager is available through Accessories, canonical System-task search, shortcut folders and validated backups. Its tab/selection/pause state survives recreation.

Window caption icons and task buttons share Restore/Minimize/Maximize/Close commands. Caption double-tap toggles maximize. Captions and taskbar buttons use the same current title (including image/folder names), and hover labels show the whole title. New keyboard actions are Ctrl+Shift+Esc, Alt+F4, Alt+Space, Win+D and Win+E when delivered by Android. Android can reserve hardware combinations.

Arrangement uses the actual available workspace excluding cutouts, taskbar, IME and an always-on-top Sidebar. Narrow or crowded screens cascade rather than squeezing caption controls into unusable strips. Undo restores exact prior geometry/maximized state for windows that still exist and is cleared on orientation changes. Caption menus are dismissed during pause, rotation and detach. Task/window cleanup uses the same close path as the caption button.

## Verification

A dedicated native PolishSmoke suite physically holds the taskbar, taps actual menu entries and window controls, checks the actual view geometry and state, recreates the real Activity and opens an actual separate-UID app and Android App Info. It covers task selection/switch/end, note continuity, timer suspension/resumption, caption double-tap, taskbar window commands, arrangement/undo, desktop restoration, searchable routing and valid backup inclusion. WindowArrangementCheck tests tiny through wide workspaces, crowded layouts and rounding boundaries. Existing seven portable and full Android 11/16 launcher suites remain required.


## Connected desktop — 0.21

The next pass extends the existing Vista shell with on-demand snapshots and a touch switcher, bounded still-image Explorer thumbnails (EXIF-aware), local verified Recycle Bin recovery, shared recent documents/folders and searchable Favorites, named workspaces, opt-in synthesized event sounds and a right-corner brightness flyout. Original vector artwork and synthesized PCM are used. No screen capture or accessibility service permission is requested.

Additional platform references:
- https://learn.microsoft.com/en-us/windows/win32/shell/taskbar-extensions — distinguishes Vista hover thumbnails from the later Windows7 taskbar.
- https://learn.microsoft.com/en-us/windows/win32/shell/thumbnail-providers — Vista file-specific thumbnail behavior.
- https://developer.android.com/reference/android/view/MotionEvent#ACTION_HOVER_ENTER — pointer/stylus hover; phone touch uses an explicit switcher.
- https://developer.android.com/reference/android/view/WindowManager.LayoutParams#screenBrightness — foreground Activity brightness override.
- https://developer.android.com/reference/android/provider/Settings.System#canWrite(android.content.Context) — explicit user-controlled settings-write grant; requested only from the brightness flyout.
- https://developer.android.com/guide/topics/ui/accessibility/views/apps-views — touch target and accessibility guidance.

Adaptation decisions: previews are bounded snapshots rendered only on demand and kept in memory; privacy hides the whole browser preview when any private tab exists. External Android windows are represented by labelled app launch entries. Phone taskbar overflow retains original tap/hold behavior. Recycle Bin supports regular files with readable/deletable originals and creatable parents; it keeps a durable SHA256-verified recovery copy and receipt before original deletion and verifies restored bytes before removing its copy. Folders or unsupported recovery locations are clearly permanent. Limits and restore conflicts are shown in the UI. Named workspaces are bounded JSON references and never capture note contents, private browser session state or Android app state. Brightness scope, device permissions and recent document history are deliberately excluded from desktop backups.


## Vista finishing touches — 0.22

Task-button surfaces now share AeroGlass compositing for tint, transparency and the entire 0–100 opacity range. Active Nth windows and grouped folder families have distinct glass shading. Both Quick Launch separator grips use the same narrow two-line drawing and identical dimensions. The notification area uses original metallic battery, blue-monitor network, speaker and sun glyphs. Its chevron expands inline; settings select always-visible icons, and the clock remains accessible.

Task Manager retains real-window task management and adds owned processes, networking, sortable task/status columns, update speeds, paused state and opt-out animated graphs. Animation interpolates measured history buffers; unavailable initial CPU/network intervals are labelled. CPU is Nth elapsed CPU time divided by wall time (one core; labels may exceed 100% on multicore work), RAM is device memory, heap is Nth Java memory, and traffic uses device interface totals. VPN counters may count traffic more than once; no network payloads are inspected. No added permissions or dependencies. See Android's [TrafficStats](https://developer.android.com/reference/android/net/TrafficStats), [ActivityManager](https://developer.android.com/reference/android/app/ActivityManager), and [Process](https://developer.android.com/reference/android/os/Process) APIs.

Recycle Bin is a virtual Explorer view over the existing private verified-copy backend. Name/original location/date/size columns, sorting, search, selection, restore-all, empty confirmation and Properties operate on actual entries. Properties shares the capacity preference with new deletions and the desktop visibility preference with Desktop Settings. Lowering capacity retains existing entries. Original empty/full bin artwork follows real entries. This does not access Android's system trash or collect other apps' deletions.

Independent folder windows use bounded UUID tool IDs and persist their separate navigation and geometry through the normal session path. Icon long-press starts an Activity-local drag with no global URI grants. Drop confirms Copy/Move and delegates to the same cancellable verified document operations; directory desktop drops create permission-scoped links. Folder links, grouping, tray options and background selection are validated in settings backups. Permissions and actual file bytes are not embedded.

Desktop Background uses original thumbnail artwork, a separate draft preview, Android-selected pictures decoded within bounded dimensions/pixel counts, five position modes and six background colours. Save applies and Cancel discards. Recycle Bin artwork and tray icons are original Canvas drawings. Gadget Gallery adds framed selection/details and responsive columns; existing Add/Remove/settings and Sidebar docking/resizing remain. Sidebar has direct close/options/move tools.

FidelitySmoke exercises task opacity, separators, inline tray expansion, preview/save/custom image decoding, measured graph counters/animation/pause/reduced motion, real PIDs, native multi-window grouping and physical cross-window icon drag/drop with exact-byte checks. TaskbarGroupingCheck covers empty/large lists, exact fit/overflow, stable order and no dropped/duplicated window IDs. All retained native suites remain required.

The user's linked YouTube videos could not be played in this environment. The supplied screenshot, described interactions and accessible primary sources informed the implementation; full video review is not claimed.
