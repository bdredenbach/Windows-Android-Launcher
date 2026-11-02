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
