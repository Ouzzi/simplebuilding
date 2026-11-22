#!/usr/bin/env python3
"""
N50 TZEDIT50 Phase 2: the data of the wiki dev page "Testzentrale-Editor" (index.html ?tab=tzeditor, assets/tzeditor.js).

Reads the checked-in layout configs (src/main/resources/data/simplebuilding/testcentre/layout_<variant>_<area>.json)
and writes
  wiki/data/testcentre/layout_<variant>_<area>.json  copies the page fetches (the wiki serves only wiki/),
  wiki/data/testcentre/index.json                    the areas (variant, area, name, size) and id -> texture path
                                                     (module textures the wiki already holds under assets/textures/;
                                                     Vanilla ids get none - the page looks them up in the dev host's
                                                     data/vanilla-icons.json at runtime and otherwise shows the name;
                                                     "vanilla" = base names of module ids without a picture, looked up
                                                     the same way, never guessed as file paths).

  wiki/data/testcentre/plan.json                     N72b: the game plan of the editor checks and the view "as built", from
                                                     the integration dump (--plan DIR, see plan_from_dump); kept as it is
                                                     by the normal run (only ids, keys and reasons - no pixels).

usage: build_editor.py [--repo REPO] [--check] [--plan DUMP_DIR]   (--check: exit 1 when the written files are not up to date)
  DUMP_DIR = testcentre-dump/ of `SB_TC_DUMP=1 run.py --targets integration-263
             --filter sbintegration:cross_mod_game_test_test_centre_plan_with_all_modules` (integration/run-fabric-263/)
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from tzedit_save import LAYOUT_DIR, check_save, layout_text  # noqa: E402

NAMES = {"metals": "Metalle", "wood": "Holz", "aging": "Verwitternde Blöcke", "rest": "Rest", "stone": "Stein",
         "compact": "Kompakt je Mod", "items": "Items je Mod"}
VARIANTS = ["lines", "blocks"]
ORDER = ["metals", "wood", "aging", "rest", "stone", "compact", "items"]
SUFFIXES = ("", "_side", "_front", "_top", "_0", "_bottom", "_inventory", "_item", "_on")


def wiki_icons(wiki: Path) -> dict[str, str]:
    """id -> icon path of the generated wiki module data (wiki/data/<module>.json)."""
    out = {}
    for path in sorted((wiki / "data").glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except ValueError:
            continue
        if not isinstance(data, dict):
            continue
        for section in ("items", "blocks"):
            for item in data.get(section) or []:
                if isinstance(item, dict) and isinstance(item.get("id"), str):
                    icon = item.get("icon") or item.get("texture")
                    if isinstance(icon, str) and (wiki / icon).is_file():
                        out.setdefault(item["id"], icon)
    return out


ROLE_ORDER = ("", "item", "front", "side", "top", "end", "bottom")


def texture_groups(wiki: Path) -> dict[str, str]:
    """group (ns:name, usually the block/item id) -> best texture file of the wiki texture index (data/textures.js)."""
    path = wiki / "data" / "textures.js"
    if not path.is_file():
        return {}
    text = path.read_text(encoding="utf-8")
    data = json.loads(text[text.index("{"):text.rstrip().rstrip(";").rindex("}") + 1])
    best: dict[str, tuple[int, str]] = {}
    for t in data.get("textures", []):
        group, file = t.get("group"), t.get("file")
        if not group or not file or t.get("kind") not in ("block", "item") or not (wiki / file).is_file():
            continue
        role = t.get("role") or ""
        rank = (0 if t.get("kind") == "item" else 1) * 10 + (ROLE_ORDER.index(role) if role in ROLE_ORDER else 9)
        if group not in best or rank < best[group][0]:
            best[group] = (rank, file)
    return {g: f for g, (_r, f) in best.items()}


def texture(wiki: Path, icons: dict[str, str], groups: dict[str, str], ident: str, namespaces: list[str]) -> str | None:
    ns, _, path = ident.partition(":")
    if ns == "minecraft":
        return None
    for base in bases(path):
        for full in [f"{ns}:{base}"] + [f"{other}:{base}" for other in namespaces if other != ns]:
            if full in icons:
                return icons[full]
            if full in groups:
                return groups[full]
        for kind in ("block", "item"):
            for suffix in SUFFIXES:
                rel = f"assets/textures/{ns}/{kind}/{base}{suffix}.png"
                if (wiki / rel).is_file():
                    return rel
    return None


# N86: "_side_half_slab"/"_half_slab" before "_slab" (they end in "_slab" too; else the picture base would be "x_half"/"x_side_half").
SHAPES = ("_block_octet", "_octet", "_side_half_slab", "_half_slab", "_slab", "_stairs", "_wall", "_button", "_pressure_plate", "_fence_gate", "_fence",
          "_trapdoor", "_pane", "_sheet")


def bases(path: str) -> list[str]:
    """The id itself, then what it is made of (shown with the base block's picture): waxed twin, shapes of a block
    (octet, slab, stairs ... of X -> X, Xs, X_block, X_planks), chess pieces (<material>_chess_<piece> -> material)."""
    out = [path, path.removeprefix("waxed_")]
    stem = path.removeprefix("waxed_")
    if "_chess_" in stem:
        stem = stem.split("_chess_")[0]
        out.append(stem)
    for shape in SHAPES:
        if stem.endswith(shape):
            stem = stem[:-len(shape)]
            break
    for b in (stem, stem.removeprefix("cut_")):
        out += [b, b + "s", b + "_block", b + "_planks", b + "_bricks" if b.endswith("_brick") else b]
    return list(dict.fromkeys(x for x in out if x))


# --- N72b: game plan (integration dump) -------------------------------------------------------------------------------
GAME = "common/src/shared/java/com/simplebuilding/dev/testcentre"
PER_PAIR = 114          # OrderAreas: MAX_WIDTH (TestCentreLayout.MAX_ROW_WIDTH 120 - KIOSK_WIDTH 2 - 2) - START_X 2
BIG_FAMILY = 24         # a missing pattern with at least this many variants counts as "very large variant family"


def _tsv(path: Path) -> list[list[str]]:
    return [line.rstrip("\n").split("\t") for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def game_exclusions(repo: Path) -> dict[str, str]:
    """TestCentreLayout.EXCLUDED + MODULE_EXCLUDED: id -> reason, read from the source (the game's own reasons)."""
    import re
    text = (repo / GAME / "TestCentreLayout.java").read_text(encoding="utf-8")
    text = text.replace("com.simplebuilding.version.McVersion.PADS_NAMESPACE + \":", "\"simplepads:")
    return dict(re.findall(r'"([a-z0-9_]+:[a-z0-9_/]+)",\s*"([^"]+)"', text))


def _pattern(ident: str) -> str:
    """family pattern of an id: the material word replaced (simplebuilding:oak_door_2x3 -> simplebuilding:*_door_2x3)"""
    ns, _, path = ident.partition(":")
    parts = path.split("_")
    for i in range(1, len(parts)):
        rest = "_".join(parts[i:])
        if rest[:1].isalpha() and any(ch.isdigit() for ch in rest) or rest in ("door", "wall_head", "wall_skull", "wall_sign", "wall_hanging_sign"):
            return f"{ns}:*_{rest}"
    return ident


def technical_reason(ident: str, blocks: set[str], excluded: dict[str, str]) -> str:
    """Why a registered block has no field (TestCentreLayout.technical / EXCLUDED, same rules as the game)."""
    if ident in excluded:
        return excluded[ident]
    ns, _, path = ident.partition(":")
    if "wall_" in path and f"{ns}:{path.replace('wall_', '')}" in blocks:
        return "Wandvariante (gleiches Item wie die Grundform, TestCentreLayout.technical)"
    if path.startswith("potted_"):
        return "Blumentopf mit Pflanze (Topf + Pflanze, kein eigenes Item)"
    return "Block ohne eigenes Item im Spielplan (OrderAreas.blocks: nur BlockItems und Setzbares) - entsteht im Spiel"


def plan_from_dump(repo: Path, dump: Path) -> dict:
    """The game's own plan with all modules: line rows (family, side, algorithmic rows), the "new" band, the compact
    blocks per mod, and the exceptions with reasons (big variant families of the config, technical blocks)."""
    wiki = repo / "wiki"
    configured = set()
    for path in (repo / LAYOUT_DIR).glob("layout_lines_*.json"):
        configured |= {r["key"] for r in json.loads(path.read_text(encoding="utf-8"))["rows"]}
    rows: dict[str, dict] = {}
    labels: dict[str, str] = {}
    fresh: list[str] = []
    for p in _tsv(dump / "lines-rows.tsv"):
        if p[0] == "#label":
            labels[p[1]] = p[4]
            continue
        if p[0].startswith("#"):
            continue
        group, kind, family, side, x, what = p[0], p[1], p[2], p[3], int(p[4]), p[8]
        if group.startswith("new:"):
            if what not in ("MISSING", "NONE"):
                fresh.append(what)
            continue
        row = rows.setdefault(group, {"key": group, "kind": kind, "family": family, "side": side, "x": x, "cells": []})
        row["cells"].append("#red" if what == "MISSING" else "#light" if what == "NONE" else what)
    rest = [{"key": r["key"], "label": labels.get(r["key"], r["key"]), "family": r["family"], "cells": r["cells"]}
            for r in sorted(rows.values(), key=lambda r: (r["side"], r["x"])) if r["key"] not in configured]
    expected: dict[str, list[str]] = {}
    for p in _tsv(dump / "compact-cells.tsv"):
        expected.setdefault(p[0], []).append(p[2])
    plan_ids = {i for ids in expected.values() for i in ids}
    # exceptions 1: very large variant families the config does not list one by one (they stand in the game)
    compact = repo / LAYOUT_DIR / "layout_blocks_compact.json"
    placed = set()
    if compact.is_file():
        placed = {c for r in json.loads(compact.read_text(encoding="utf-8"))["rows"] for c in r["cells"] if c and not c.startswith("#")}
    groups: dict[str, list[str]] = {}
    for ident in sorted(plan_ids - placed):
        groups.setdefault(_pattern(ident), []).append(ident)
    exceptions = []
    for pattern, ids in sorted(groups.items()):
        if len(ids) >= BIG_FAMILY and pattern != ids[0]:
            materials = sorted({i.partition(":")[2].split("_" + pattern.split("*_", 1)[1])[0] for i in ids})
            exceptions.append({"family": pattern, "ids": ids, "kind": "family",
                               "reason": f"sehr große Variantenfamilie: {len(materials)} Materialien, {len(ids)} Blöcke; im Spiel stehen alle im "
                                         f"Kompakt-Bereich (OrderAreas.compact baut nach dem Algorithmus), die Config listet sie nicht einzeln"})
            for ident in ids:
                expected[ident.partition(":")[0]].remove(ident)
    # exceptions 2: registered mod blocks without a field in the game plan (no item / excluded), grouped by reason
    blocks = set()
    for path in sorted((wiki / "data").glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except ValueError:
            continue
        if isinstance(data, dict):
            blocks |= {b["id"] for b in data.get("blocks") or [] if isinstance(b, dict) and isinstance(b.get("id"), str)}
    excluded = game_exclusions(repo)
    by_reason: dict[str, list[str]] = {}
    for ident in sorted(b for b in blocks - plan_ids if not b.startswith("minecraft:")):
        by_reason.setdefault(technical_reason(ident, blocks, excluded), []).append(ident)
    for reason, ids in sorted(by_reason.items(), key=lambda kv: -len(kv[1])):
        exceptions.append({"family": ids[0] if len(ids) == 1 else _pattern(ids[0]) if len({_pattern(i) for i in ids}) == 1 else f"{len(ids)} Blöcke",
                           "ids": ids, "kind": "technical", "reason": reason})
    stamp = (dump / "lines-rows.tsv").stat().st_mtime
    import datetime
    return {"schema": 1, "source": "Integrationstest cross_mod_game_test_test_centre_plan_with_all_modules (SB_TC_DUMP), "
                                   + datetime.datetime.fromtimestamp(stamp, datetime.timezone.utc).strftime("%Y-%m-%d"),
            "lines": {"perPair": PER_PAIR, "family": {k: r["family"] for k, r in sorted(rows.items())},
                      "side": {k: [r["side"], r["x"]] for k, r in sorted(rows.items())}, "rest": rest, "fresh": sorted(set(fresh))},
            "compact": {"expected": expected}, "exceptions": exceptions}


def outputs(repo: Path) -> dict[Path, str]:
    wiki = repo / "wiki"
    icons = wiki_icons(wiki)
    groups = texture_groups(wiki)
    namespaces = sorted({k.split(":")[0] for k in list(icons) + list(groups)} - {"minecraft"})
    saves = []
    for path in sorted((repo / LAYOUT_DIR).glob("layout_*.json")):
        saves.append(check_save(json.loads(path.read_text(encoding="utf-8"))))
    saves.sort(key=lambda s: (VARIANTS.index(s["variant"]) if s["variant"] in VARIANTS else 9,
                              ORDER.index(s["area"]) if s["area"] in ORDER else 99, s["area"]))
    files, areas, textures, vanilla = {}, [], {}, {}
    for s in saves:
        name = f"layout_{s['variant']}_{s['area']}.json"
        files[wiki / "data" / "testcentre" / name] = layout_text(s)
        ids = {c for r in s["rows"] for c in r["cells"] if c and not c.startswith("#")}
        ids |= {t for r in s["rows"] for t in r.get("tops", {}).values()}
        for ident in sorted(ids):
            full = ident if ":" in ident else "minecraft:" + ident
            tex = texture(wiki, icons, groups, full, namespaces)
            if tex:
                textures[ident] = tex
            elif not full.startswith("minecraft:"):
                vanilla[ident] = bases(full.partition(":")[2])[1:6]
        areas.append({"variant": s["variant"], "area": s["area"], "name": NAMES.get(s["area"], s["area"]), "file": name,
                      "rows": len(s["rows"]), "columns": len(s["columns"]), "ids": len(ids)})
    # module ids without an own picture: base names the page tries among the dev-only Vanilla pictures
    index = {"schema": 1, "areas": areas, "textures": dict(sorted(textures.items())), "vanilla": dict(sorted(vanilla.items()))}
    files[wiki / "data" / "testcentre" / "index.json"] = json.dumps(index, ensure_ascii=False, separators=(",", ":")) + "\n"
    plan = wiki / "data" / "testcentre" / "plan.json"
    if plan.is_file():  # written only with --plan (needs the integration dump); the normal run keeps it
        files[plan] = plan.read_text(encoding="utf-8")
    return files


def main(argv: list[str]) -> int:
    p = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    p.add_argument("--repo", default=str(HERE.parents[1]))
    p.add_argument("--check", action="store_true")
    p.add_argument("--plan", type=Path, help="testcentre-dump/ of the integration run: write data/testcentre/plan.json")
    a = p.parse_args(argv)
    repo = Path(a.repo)
    if a.plan:
        plan = plan_from_dump(repo, a.plan)
        target = repo / "wiki" / "data" / "testcentre" / "plan.json"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(plan, ensure_ascii=False, separators=(",", ":"), sort_keys=True) + "\n", encoding="utf-8")
        miss = sum(len(v) for v in plan["compact"]["expected"].values())
        print(f"plan.json: {len(plan['lines']['family'])} line rows, {len(plan['lines']['rest'])} without config, "
              f"{miss} compact blocks, {len(plan['exceptions'])} exception groups")
    files = outputs(repo)
    stale = [f for f, text in files.items() if not f.is_file() or f.read_text(encoding="utf-8") != text]
    out_dir = repo / "wiki" / "data" / "testcentre"
    extra = [f for f in out_dir.glob("*.json") if f not in files] if out_dir.is_dir() else []
    if a.check:
        for f in stale + extra:
            print(f"stale: {f.relative_to(repo)} (run python3 tools/tzedit/build_editor.py)")
        return 1 if stale or extra else 0
    for f in extra:
        f.unlink()
    for f in stale:
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(files[f], encoding="utf-8")
    index = json.loads(files[out_dir / "index.json"])
    print(f"{len(index['areas'])} areas, {len(index['textures'])} textures, {len(stale)} file(s) written")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
