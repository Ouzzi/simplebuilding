#!/usr/bin/env python3
"""
N50 TZEDIT50 Phase 2: first export of the testcentre layout configs from the game.

Input: the plan tables the integration gametest writes with SB_TC_DUMP=1 (all modules loaded, TestCentreDump.allModules):
  lines-rows.tsv        every field of every row of the lines area (group, kind, side, x, z, column, key, item|MISSING|NONE, top)
                        plus #label (sign text of a row) and #hidden lines,
  compact-cells.tsv     the "blocks" variant (all blocks of a mod in a dense square),
  items-<variant>-cells.tsv  "items per mod" of each variant.
Output: one config per area in the schema of tzedit_save.check_save, written with tzedit_save.layout_text into
src/main/resources/data/simplebuilding/testcentre/layout_<variant>_<area>.json:

  lines: metals | wood | aging | rest | stone   (one area per row kind; stone = side B, then the stone rows on side A)
         columns = fixed slot columns (slot name lower case), shared extra columns (pattern, {m} -> m), special_<n>
         cells   = item id; "#red" = red placeholder (should exist), "#light" = light grey placeholder (none),
                   null = behind the end of that row (the world fills the side rectangle light grey); "tops": {column: octet id standing on that block}
  blocks: compact  rows = one line of a mod square ("<ns>-<n>"), columns c0.. (c0 of the first line = the mod sign -> null)
  lines|blocks: items  rows = one line of a mod ("<ns>-<n>"), columns c0..

usage: export_layout.py <dump dir> [--repo <repo>] [--check]   (--check: only report, write nothing)
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from tzedit_save import LAYOUT_DIR, check_save, layout_path, layout_text  # noqa: E402

KIND_AREA = {"METAL": "metals", "WOOD": "wood", "AGING": "aging", "REST": "rest", "STONE": "stone"}
AREA_ORDER = ["metals", "wood", "aging", "rest", "stone"]


def name(text: str) -> str:
    """A valid NAME of the schema (row key, column)."""
    s = re.sub(r"[^a-z0-9_-]+", "_", text.lower().replace("{m}", "m").replace("{p}", "p")).strip("_-") or "x"
    return s[:48]


def unique(base: str, taken: set[str]) -> str:
    out, n = base, 2
    while out in taken:
        tail = f"_{n}"
        out, n = base[:48 - len(tail)] + tail, n + 1
    taken.add(out)
    return out


def label(text: str) -> str:
    return "".join(c for c in text if ord(c) >= 32)[:80]


def read_lines(path: Path) -> list[dict]:
    rows, labels, order = {}, {}, []
    for line in path.read_text(encoding="utf-8").splitlines():
        f = line.split("\t")
        if line.startswith("#label"):
            labels[(f[1], int(f[2]), int(f[3]))] = f[4] if len(f) > 4 else f[1]
            continue
        if line.startswith("#") or len(f) < 9:
            continue
        group, kind, family, side, x, z, col, key, what = f[:9]
        top = f[9] if len(f) > 9 else ""
        ident = (group, side, int(x))
        if ident not in rows:
            rows[ident] = {"group": group, "kind": kind, "side": side, "x": int(x), "fields": [], "nearZ": int(z)}
            order.append(ident)
        rows[ident]["fields"].append((int(col), key, what, top))
    out = []
    for ident in order:
        r = rows[ident]
        r["fields"].sort()
        r["label"] = labels.get((r["group"], r["x"], r["nearZ"]), r["group"].replace("_", " "))
        out.append(r)
    return out


def lines_areas(rows: list[dict]) -> list[dict]:
    by_area: dict[str, list[dict]] = {a: [r for r in rows if KIND_AREA[r["kind"]] == a] for a in AREA_ORDER}
    # stone: the algorithm's stone order is side B first, then the stone rows that moved to side A (TZ38E-13).
    by_area["stone"] = [r for r in rows if r["kind"] == "STONE" and r["side"] == "B"] \
        + [r for r in rows if r["kind"] == "STONE" and r["side"] == "A"]
    saves = []
    for area in AREA_ORDER:
        members = by_area[area]
        if not members:
            continue
        # the side depth pads every row with filler fields (key "-"); the area ends with its longest real row
        width = 1 + max(col for r in members for col, key, _w, _t in r["fields"] if key != "-")
        keys: list[list[str]] = [[] for _ in range(width)]
        for r in members:
            for col, key, _what, _top in r["fields"]:
                if col < width:
                    keys[col].append(key)
        columns, taken, special = [], set(), 0
        for col in range(width):
            real = [k for k in keys[col] if k not in ("special", "-")]
            if real:
                columns.append(unique(name(real[0]), taken))
            else:
                special += 1
                columns.append(unique(f"special_{special}", taken))
        out_rows, row_keys = [], set()
        for r in members:
            cells: list = [None] * width
            tops = {}
            for col, key, what, top in r["fields"]:
                if col >= width or key == "-":
                    continue   # behind the end of the row: null (the world fills the side with light grey)
                cells[col] = "#red" if what == "MISSING" else "#light" if what == "NONE" else what
                if top:
                    tops[columns[col]] = top
            row = {"key": unique(name(r["group"]), row_keys), "label": label(r["label"]), "cells": cells}
            if tops:
                row["tops"] = tops
            out_rows.append(row)
        saves.append({"variant": "lines", "area": area, "columns": columns, "rows": out_rows, "excluded": []})
    return saves


def grid_area(path: Path, variant: str, area: str, sign_first: bool) -> dict:
    """Cells of a per-mod area (compact squares / item lines) as rows of a grid: one row per z line of a mod."""
    mods: dict[str, list[tuple[int, str, int, int]]] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        f = line.split("\t")
        if len(f) < 6:
            continue
        mods.setdefault(f[0], []).append((int(f[1]), f[2], int(f[3]), int(f[5])))
    rows, keys, width = [], set(), 1
    for ns, cells in mods.items():
        first = min(cells)
        x0 = first[2] - (1 if sign_first else 0)   # compact: cell 0 is the mod sign
        z0 = first[3]
        grid: dict[int, dict[int, str]] = {}
        for _i, ident, x, z in cells:
            grid.setdefault(z - z0, {})[x - x0] = ident
            width = max(width, x - x0 + 1)
        for line_no in sorted(grid):
            rows.append((ns, line_no, grid[line_no]))
    out_rows = []
    for ns, line_no, line in rows:
        cells = [line.get(c) for c in range(width)]
        out_rows.append({"key": unique(name(f"{ns}-{line_no + 1}"), keys), "label": label(f"{ns} {line_no + 1}"), "cells": cells})
    return {"variant": variant, "area": area, "columns": [f"c{c}" for c in range(width)], "rows": out_rows, "excluded": []}


def build(dump: Path) -> list[dict]:
    saves = lines_areas(read_lines(dump / "lines-rows.tsv"))
    if (dump / "compact-cells.tsv").exists():
        saves.append(grid_area(dump / "compact-cells.tsv", "blocks", "compact", True))
    for variant in ("lines", "blocks"):
        p = dump / f"items-{variant}-cells.tsv"
        if p.exists():
            saves.append(grid_area(p, variant, "items", False))
    return [check_save(s) for s in saves]


def main(argv: list[str]) -> int:
    p = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    p.add_argument("dump")
    p.add_argument("--repo", default=str(Path(__file__).resolve().parents[2]))
    p.add_argument("--check", action="store_true")
    a = p.parse_args(argv)
    repo = Path(a.repo)
    for save in build(Path(a.dump)):
        cells = sum(1 for r in save["rows"] for c in r["cells"] if c and not c.startswith("#"))
        tops = sum(len(r.get("tops", {})) for r in save["rows"])
        reds = sum(1 for r in save["rows"] for c in r["cells"] if c == "#red")
        text = layout_text(save)
        print(f"{save['variant']}/{save['area']}: {len(save['rows'])} rows x {len(save['columns'])} columns, {cells} ids"
              f" (+{tops} tops), {reds} red, {len(text) // 1024} KB")
        if not a.check:
            out = repo / layout_path(save)
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(text, encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
