#!/usr/bin/env python3
"""
Turns ES-DE's Android system and emulator rules into the JSON file Huginn ships in its assets.

ES-DE (https://es-de.org, MIT licence) keeps an up-to-date list of which folder name, file
extensions and launch command every system uses on Android, and which package each emulator
lives in. Huginn reads the same launch commands, so updating is just running this again:

    python3 tools/import_esde.py <path to ES-DE>/resources/systems/android

Writes app/src/main/assets/esde/rules.json.
"""
import json
import pathlib
import sys
import xml.etree.ElementTree as ET

root = pathlib.Path(sys.argv[1])
out = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets/esde/rules.json"

emulators = {}
for emu in ET.parse(root / "es_find_rules.xml").getroot().findall("emulator"):
    entries = []
    for rule in emu.findall("rule"):
        if rule.get("type") == "androidpackage":
            entries += [e.text.strip() for e in rule.findall("entry") if e.text]
    if entries:
        emulators[emu.get("name")] = entries

systems = []
for s in ET.parse(root / "es_systems.xml").getroot().findall("system"):
    commands = []
    for c in s.findall("command"):
        text = (c.text or "").strip()
        if not text or text.startswith("PLACEHOLDER"):
            continue
        commands.append([c.get("label") or s.findtext("fullname"), text])
    if not commands:
        continue
    extensions = []
    for ext in (s.findtext("extension") or "").split():
        e = ext.lstrip(".").lower()
        if e and e not in extensions:
            extensions.append(e)
    systems.append({
        "n": s.findtext("name"),
        "f": s.findtext("fullname"),
        "e": extensions,
        "c": commands,
    })

out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps({"emulators": emulators, "systems": systems}, ensure_ascii=False, separators=(",", ":")))
print(f"{len(systems)} systems, {len(emulators)} emulators -> {out} ({out.stat().st_size // 1024} KB)")
