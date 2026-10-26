"""Wait for the real programme guide in a fresh Android emulator."""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

def adb(*args):
    return subprocess.check_output(["adb", *args], text=True, stderr=subprocess.STDOUT, timeout=30)

deadline = time.monotonic() + 150
attempt = 0
labels = []
while time.monotonic() < deadline:
    attempt += 1
    try:
        adb("shell", "uiautomator", "dump", "/sdcard/guide-check.xml")
        root = ET.fromstring(adb("shell", "cat", "/sdcard/guide-check.xml"))
        labels = [node.get("text", "") for node in root.iter("node")]
        programmes = [label for label in labels if label.startswith("Now ·")]
        if "48 channels" in labels and programmes:
            print("GUIDE_UI_OK", programmes[:4], flush=True)
            sys.exit(0)
        if any("isn't responding" in label for label in labels):
            for node in root.iter("node"):
                if node.get("text") == "Wait":
                    box = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
                    if len(box) == 4:
                        adb("shell", "input", "tap", str((box[0] + box[2]) // 2), str((box[1] + box[3]) // 2))
                        break
        if attempt % 5 == 0:
            print("Waiting for guide:", labels[:14], flush=True)
            adb("shell", "am", "start", "-n", "it.gianrolando.listentv/.MainActivity")
    except (subprocess.SubprocessError, ET.ParseError) as error:
        print("Emulator not ready:", type(error).__name__, flush=True)
    time.sleep(3)
print(adb("shell", "date"), flush=True)
print(adb("logcat", "-d", "-s", "ListenTvGuide", "AndroidRuntime"), flush=True)
raise AssertionError("No live guide title after waiting: " + repr(labels[:35]))
