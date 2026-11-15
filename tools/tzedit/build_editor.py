#!/usr/bin/env python3
"""
N50 TZEDIT50 Phase 2: the data of the wiki dev page "Testzentrale-Editor" (index.html ?tab=tzeditor, assets/tzeditor.js).

Reads the checked-in layout configs (common/src/shared/resources/data/simplebuilding/testcentre/layout_<variant>_<area>.json)
and writes
  wiki/data/testcentre/layout_<variant>_<area>.json  copies the page fetches (the wiki serves only wiki/),
  wiki/data/testcentre/index.json                    the areas (variant, area, name, size) and id -> texture path
                                                     (module textures the wiki already holds under assets/textures/;
                                                     Vanilla ids get none - the page tries the dev-only assets/vanilla/
                                                     copy at runtime and otherwise shows the name).

usage: build_editor.py [--repo REPO] [--check]   (--check: exit 1 when the written files are not up to date)
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


SHAPES = ("_block_octet", "_octet", "_slab", "_stairs", "_wall", "_button", "_pressure_plate", "_fence_gate", "_fence",
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
    return files


def main(argv: list[str]) -> int:
    p = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    p.add_argument("--repo", default=str(HERE.parents[1]))
    p.add_argument("--check", action="store_true")
    a = p.parse_args(argv)
    repo = Path(a.repo)
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
