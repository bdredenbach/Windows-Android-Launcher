"""Configure the emulator display before the instrumentation process caches DisplayInfo."""
import re
import subprocess
import time

def adb(*args):
    return subprocess.check_output(['adb', 'shell', *args], text=True, timeout=8)

overlays = adb('cmd', 'overlay', 'list')
overlay = next(('com.android.internal.display.cutout.emulation.' + name for name in ('tall', 'corner', 'double') if 'com.android.internal.display.cutout.emulation.' + name in overlays), None)
assert overlay, 'Portrait fixture needs a cutout overlay'
adb('cmd', 'overlay', 'enable-exclusive', '--user', '0', '--category', overlay)
deadline = time.monotonic() + 15
while time.monotonic() < deadline:
    state = adb('dumpsys', 'window', 'displays')
    if re.search(r'mDisplayCutout=WmDisplayCutout\{DisplayCutout\{insets=Rect\(0, [1-9]\d* -', state):
        print('CHECK: portrait cutout is active before starting instrumentation')
        break
    time.sleep(.15)
else:
    raise AssertionError('WindowManager has not activated the portrait cutout')
adb('am', 'force-stop', 'com.nthexperience.desktop')
