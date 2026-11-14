#!/usr/bin/env python3
"""N50 Phase 1: static excerpt of ONE testcentre area (metal rows) as layout JSON, no game needed.
Reads the Slot patterns from OrderAreas.java and matches them against the PNG textures in the repo
(block + item dirs of all modules). Real export (Phase 2) must run in-game/gametest (item registry);
this excerpt only places ids whose texture file exists, so it is a lower bound of the real area.
usage: export_area.py <repo> <out.json>"""
import json, re, sys, base64
from pathlib import Path

repo = Path(sys.argv[1]); out = Path(sys.argv[2])
java = (repo / "common/src/shared/java/com/simplebuilding/dev/testcentre/OrderAreas.java").read_text()
body = java[java.index("public enum Slot {"):java.index("OTHER;")]
slots = {}
for m in re.finditer(r'\b([A-Z_]+)\(((?:"[^"]*"(?:,\s*)?)+)\)', body):
    slots[m.group(1)] = re.findall(r'"([^"]*)"', m.group(2))
core = re.search(r"CORE = List\.of\((.*?)\);", java, re.S).group(1)
core = [s.split(".")[1].strip() for s in core.split(",")]
names = list(re.findall(r"^\s+(?:/\*\*.*?\*/\s*)?", "")) or []
order = re.findall(r"\b([A-Z_]+)(?=\(|,|;)", body)
allslots = []
for t in order:
    if t in slots or t == "OTHER":
        if t not in allslots: allslots.append(t)
cols = list(core)
cols[cols.index("FENCE")] = "POLE"; cols[cols.index("FENCE_GATE")] = "CHAIN"
cols += [s for s in allslots[allslots.index("TILES"):] if s != "OTHER"]

png = {}
for d in repo.glob("**/assets/*/textures/block"):
    if "node_modules" in d.parts or "art" in d.parts: continue
    for f in d.glob("*.png"): png.setdefault(f.stem, ("block", f))
for d in repo.glob("**/assets/*/textures/item"):
    if "node_modules" in d.parts or "art" in d.parts: continue
    for f in d.glob("*.png"): png.setdefault(f.stem, ("item", f))

rows = [("copper", ""), ("copper", "exposed_"), ("copper", "weathered_"), ("copper", "oxidized_"),
        ("diamond", ""), ("emerald", ""), ("netherite", ""), ("enderite", "")]
tex = {}; grid = []
for mat, p in rows:
    cells = []
    for c in cols:
        hit = None
        for pat in slots.get(c, []):
            i = pat.lstrip("=").replace("{p}", p).replace("{m}", mat)
            if pat.startswith("=") and p: continue
            if i in png: hit = i; break
        if hit: tex[hit] = png[hit]
        cells.append(hit)
    grid.append({"key": p + mat, "label": (p + mat).replace("_", " "), "cells": cells})
placed = set(tex)
extra = {k: v for k, v in png.items() if k not in placed and v[0] == "block"
         and re.search(r"(^|_)(diamond|emerald|netherite|enderite|copper)(_|$)", k)}
tex.update(extra)  # known but unplaced -> area "Neu" in the editor
mods = {k: v[1].parts[-4] for k, v in png.items() if k in tex}
data = {"mods": mods, "schema": 1, "variant": "lines", "area": "metals-excerpt",
        "columns": [c.lower() for c in cols], "rows": grid,
        "textures": {k: "data:image/png;base64," + base64.b64encode(f.read_bytes()).decode() for k, (_, f) in sorted(tex.items())},
        "known": sorted(tex), "placed_ids": sorted(placed), "note": "static excerpt: ids with a texture file only"}
out.write_text(json.dumps(data, separators=(",", ":")))
print(len(cols), "cols", len(grid), "rows", sum(1 for r in grid for c in r["cells"] if c), "cells", len(tex), "textures")
