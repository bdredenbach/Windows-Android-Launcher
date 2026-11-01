"""Inspect the author's unmodified release in a disposable emulator, with no accounts."""
import json, pathlib, re, subprocess, time, xml.etree.ElementTree as ET

OUT = pathlib.Path(__file__).parent / "evidence"
OUT.mkdir(exist_ok=True)
PACKAGE = "rocks.gorjan.gokixp"
COMPONENT = PACKAGE + "/.MainActivity"
events = []

def adb(*args, binary=False, timeout=30):
    return subprocess.check_output(["adb", *args], text=not binary, timeout=timeout)

def shell(*args):
    return adb("shell", *map(str,args))

def tree():
    shell("uiautomator", "dump", "/sdcard/review.xml")
    return ET.fromstring(shell("cat", "/sdcard/review.xml"))

def capture(name, include_tree=True):
    (OUT / (name+".png")).write_bytes(adb("exec-out", "screencap", "-p", binary=True))
    try:
        if not include_tree: return
        t = tree()
        ET.ElementTree(t).write(OUT / (name+".xml"), encoding="utf-8")
        labels = [dict(text=n.get("text"), id=n.get("resource-id"), description=n.get("content-desc")) for n in t.iter("node") if n.get("text") or n.get("content-desc")]
        events.append({"capture":name,"labels":labels})
    except Exception as error:
        events.append({"capture":name,"tree_error":str(error)})

def tap_node(predicate, required=False):
    for n in tree().iter("node"):
        if predicate(n):
            coords = list(map(int,re.findall(r"\d+",n.get("bounds",""))))
            if len(coords)==4 and coords[2]>coords[0] and coords[3]>coords[1]:
                shell("input","tap",(coords[0]+coords[2])//2,(coords[1]+coords[3])//2)
                time.sleep(.7)
                return True
    if required: raise RuntimeError("Requested UI item was not visible")
    return False

def tap_id(suffix, required=False):
    return tap_node(lambda n:n.get("resource-id","").endswith("/"+suffix),required)

def dismiss_setup():
    for _ in range(8):
        if tap_node(lambda n:n.get("resource-id","").endswith("/permission_deny_button")): continue
        if tap_node(lambda n:n.get("text","").lower() in ["not now","skip","no thanks","later"]): continue
        if tap_id("dialog_close_button"): continue
        break

def relaunch():
    shell("am","force-stop",PACKAGE)
    shell("am","start","-n",COMPONENT)
    time.sleep(3)

def close_tool():
    if not tap_id("dialog_close_button"): shell("input","keyevent","4")
    time.sleep(.5)

try:
    (OUT/"package.txt").write_text(shell("dumpsys","package",PACKAGE))
    (OUT/"device.txt").write_text(shell("getprop","ro.build.fingerprint"))
    record = subprocess.Popen(["adb","shell","screenrecord","--time-limit","12","/sdcard/first-launch.mp4"])
    shell("am","start","-n",COMPONENT)
    time.sleep(4)
    capture("01-first-launch", include_tree=False)
    record.wait(timeout=20)
    adb("pull","/sdcard/first-launch.mp4",str(OUT/"first-launch.mp4"))

    # Choose Vista in the existing preference file on this rootable throwaway emulator.
    # The published APK remains unmodified. Preserve every other preference.
    shell("am","force-stop",PACKAGE)
    pref = "/data/data/"+PACKAGE+"/shared_prefs/taskbar_widget_prefs.xml"
    owner = shell("stat","-c","%u:%g",pref).strip()
    xml = ET.fromstring(shell("cat",pref))
    for node in list(xml):
        if node.get("name")=="selected_theme": xml.remove(node)
    ET.SubElement(xml,"string",{"name":"selected_theme"}).text="Windows Vista"
    for node in list(xml):
        if node.get("name")=="rover_visible": xml.remove(node)
    ET.SubElement(xml,"boolean",{"name":"rover_visible","value":"false"})
    local = OUT/"configured-preferences.xml"
    ET.ElementTree(xml).write(local,encoding="utf-8",xml_declaration=True)
    adb("push",str(local),"/data/local/tmp/vista-preferences.xml")
    shell("cp","/data/local/tmp/vista-preferences.xml",pref)
    shell("chown",owner,pref)
    shell("chmod","660",pref)
    output = adb("shell","am","instrument","-w","com.nthexperience.desktop.test/com.nthexperience.desktop.VistaReview",timeout=180)
    (OUT/"instrumentation.txt").write_text(output)
    print(output)
    for name in adb("shell","run-as","com.nthexperience.desktop","ls","files/vista-review").splitlines():
        (OUT/name).write_bytes(adb("exec-out","run-as","com.nthexperience.desktop","cat","files/vista-review/"+name,binary=True))
    if "PASS:" not in output: raise RuntimeError("Research instrumentation failed")
    events.append({"completed":True,"release":"v2.1.6","configured_theme":"Windows Vista"})
finally:
    (OUT/"setup-events.json").write_text(json.dumps(events,indent=2))
    (OUT/"logcat.txt").write_text(adb("logcat","-d","-t","1200"))
