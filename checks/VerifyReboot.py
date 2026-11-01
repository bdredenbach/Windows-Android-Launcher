"""Check real, device-local playback state after an actual emulator reboot."""
import sys
import xml.etree.ElementTree as ET

boot = int(sys.argv[1])
first, again = (ET.parse(path).getroot() for path in sys.argv[2:4])


def number(root, name):
    return int(root.find(f"int[@name='{name}']").attrib["value"])


print(f"CHECK: boot={boot}; first boot={number(first, 'boot')}, runs={number(first, 'runs')}; cold restart boot={number(again, 'boot')}, runs={number(again, 'runs')}")
assert boot >= 0 and number(first, "before_reboot") != boot, "Android must actually reboot"
assert number(first, "boot") == boot, "Nth must claim the new Android boot"
assert number(first, "runs") == number(first, "before_runs") + 1, "Exactly one presentation per reboot"
assert first.find("boolean[@name='seen']").attrib["value"] == "true"
assert number(again, "boot") == boot and number(again, "runs") == number(first, "runs"), "Home and cold process restart cannot replay startup in the same boot"
print("PASS: actual Android reboot plays startup once; repeated Home and a cold app restart do not replay it.")
