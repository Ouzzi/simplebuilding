#!/usr/bin/env python3
"""Prueft alle 1,5-Tuer-Modelle (oak_door_1_5x*): jede freiliegende Voxelflaeche hat eine Face
mit Textur und gueltiger UV (0..16, Flaeche > 0); keine Face auf verdeckter Innenseite noetig.
Aufruf: python3 tools/check_door_models.py [models-dir]"""
import json, sys, glob, os
D = sys.argv[1] if len(sys.argv) > 1 else "mc26_3/overlay/resources/assets/simplebuilding/models/block"
DIRS = {"east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0), "south": (0, 0, 1), "north": (0, 0, -1)}
errs = []
files = sorted(glob.glob(os.path.join(D, "*_door_1_5x*.json")))
for f in files:
    n = os.path.basename(f)
    m = json.load(open(f))
    els = m.get("elements", [])
    if not els: errs.append(f"{n}: keine Elemente"); continue
    occ = set()
    for e in els:
        a, b = e["from"], e["to"]
        if any(float(v) != int(v) for v in a + b): errs.append(f"{n}: nicht ganzzahlig {a}->{b}"); continue
        if any(b[i] <= a[i] for i in range(3)): errs.append(f"{n}: leeres Element {a}->{b}"); continue
        for x in range(int(a[0]), int(b[0])):
            for y in range(int(a[1]), int(b[1])):
                for z in range(int(a[2]), int(b[2])): occ.add((x, y, z))
    texs = m.get("textures", {})
    for i, e in enumerate(els):
        a, b = [int(v) for v in e["from"]], [int(v) for v in e["to"]]
        faces = e.get("faces", {})
        for d, (dx, dy, dz) in DIRS.items():
            ax = [abs(dx), abs(dy), abs(dz)].index(1)
            fixed = (b if (dx + dy + dz) > 0 else a)[ax]
            rng = [range(a[k], b[k]) for k in range(3)]
            rng[ax] = [fixed - (1 if (dx + dy + dz) > 0 else 0)]
            exposed = any((x + dx, y + dy, z + dz) not in occ for x in rng[0] for y in rng[1] for z in rng[2])
            f_ = faces.get(d)
            if exposed and not f_: errs.append(f"{n}: Element {i} {a}->{b}: Face {d} fehlt (freiliegend)"); continue
            if not f_: continue
            t = f_.get("texture", "")
            if not t.startswith("#") or t[1:] not in texs: errs.append(f"{n}: Element {i} {d}: Textur {t!r} undefiniert")
            uv = f_.get("uv")
            if not uv or len(uv) != 4 or any(not 0 <= v <= 16 for v in uv) or uv[0] == uv[2] or uv[1] == uv[3]:
                errs.append(f"{n}: Element {i} {d}: UV ungueltig {uv}")
            if "cullface" in f_ and f_["cullface"] != d: errs.append(f"{n}: Element {i} {d}: cullface {f_['cullface']} != Richtung")
print(f"{len(files)} Modelle geprueft, {len(errs)} Fehler")
for e in errs[:60]: print(" ", e)
sys.exit(1 if errs else 0)
