"""
O6b: the small cross-mod index of the wiki (wiki/data/index.js, window.WIKI_INDEX).

The page loads a mod's data (data/<mod>.js) only when it shows that mod (assets/module-loader.js); the root start page
loads none. What a page needs to know about the other mods comes from this index:
  * counts  - the home page tiles and the root sidebar numbers (countsPublic: the same in the public view, where
              module-loader.js pruneHidden leaves out what exists only with a "release": false mod; only if different);
  * items / blocks  [id, name en, name de, icon, texture, needsMods] - names and pictures of foreign ids (ingredients,
              guidebook, questionary, TZ editor icons) and the links to their pages;
  * ench [id, en, de], features [id, title en, title de], recipes / tags (ids) - links into other mods and the first
              quick-search hits before the full data is there;
  * head    - mod + generatedFrom of the mod (the root start page footer and version line).
Written and checked by generate.py on every run, from the generated data/<mod>.json of every catalogue module.
"""
from __future__ import annotations

import json
from pathlib import Path

WIKI = Path(__file__).resolve().parent
TARGET = "data/index.js"
MARKER = "window.WIKI_INDEX = "


def catalogue(wiki: Path = WIKI) -> list[dict]:
    text = (wiki / "data/modules.js").read_text(encoding="utf-8")
    return json.loads(text.split("window.WIKI_MODULES = ", 1)[1].strip().rstrip(";"))


def _is_obj(x) -> bool:
    return isinstance(x, dict)


def _name(e: dict, key: str) -> str | None:
    n = e.get("name")
    if isinstance(n, dict):
        return n.get(key) or None
    return n if isinstance(n, str) and n else None


def _title(f: dict, lang: str) -> str | None:
    v = f.get(lang)
    return (v.get("title") or None) if isinstance(v, dict) else None


def _row(*values):
    out = list(values)
    while out and out[-1] in (None, "", []):
        out.pop()
    return out


# ---- the O5b prune of assets/module-loader.js (pruneHidden), per mod, for countsPublic ----
def _gone(x, hid: set[str]) -> bool:
    if not isinstance(x, str):
        return False  # JS: String(object) has no ':'
    if x.startswith("#"):
        x = x[1:]
    return x.find(":") > 0 and x.split(":")[0] in hid


def _needs(e: dict, hid: set[str]) -> bool:
    return isinstance(e.get("needsMods"), list) and any(m in hid for m in e["needsMods"])


def _groups(r: dict) -> list:
    out = []
    if _is_obj(r.get("key")):
        out.extend(r["key"].values())
    if isinstance(r.get("ingredientGroups"), list):
        out.extend(r["ingredientGroups"])
    if _is_obj(r.get("slots")):
        out.extend(r["slots"].values())
    return out


def _dead_recipe(r, hid: set[str]) -> bool:
    if not _is_obj(r):
        return False
    res = r.get("result")
    if _needs(r, hid) or (_is_obj(res) and _gone(res.get("id"), hid)):
        return True
    for g in _groups(r):
        g = g if isinstance(g, list) else ([] if not g else [g])
        if g and all(_gone(x, hid) for x in g):
            return True
    return False


def pruned(d: dict, hid: set[str]) -> dict:
    """A shallow copy of one mod's data as pruneHidden leaves it (recipes, loot tables, items, blocks)."""
    dead_r = {r.get("id") for r in d.get("recipes") or [] if _dead_recipe(r, hid)}
    dead_e, dead_loot = set(), set()
    for k in ("items", "blocks"):
        for e in d.get(k) or []:
            if _is_obj(e) and _needs(e, hid):
                dead_e.add(e.get("id"))
                if e.get("lootTable"):
                    dead_loot.add(e["lootTable"])
    if not dead_r and not dead_e:
        return d
    out = dict(d)
    if isinstance(d.get("recipes"), list):
        out["recipes"] = [r for r in d["recipes"] if not (_is_obj(r) and r.get("id") in dead_r)]
    if isinstance(d.get("lootTables"), list):
        out["lootTables"] = [t for t in d["lootTables"]
                             if t.get("id") not in dead_loot and str(t.get("id")).replace(":blocks/", ":", 1) not in dead_e]
    for k in ("items", "blocks"):
        if isinstance(d.get(k), list):
            out[k] = [e for e in d[k] if not (_is_obj(e) and e.get("id") in dead_e)]
    return out


def counts(d: dict) -> dict:
    """The numbers the page computes from loaded data (index.html famCounts and the NAV_GROUPS counts)."""
    items = [e for e in d.get("items") or [] if _is_obj(e) and e.get("id")]
    item_ids = {e["id"] for e in items}
    blocks = [e for e in d.get("blocks") or [] if _is_obj(e) and e.get("id") and e["id"] not in item_ids]
    iw = d.get("inWorld") if _is_obj(d.get("inWorld")) else {}
    ln = lambda k: len(d.get(k) or []) if isinstance(d.get(k), list) else 0  # noqa: E731
    return {
        "entries": len(items) + len(blocks), "recipes": ln("recipes"), "features": ln("features"), "config": ln("config"),
        "inworld": sum(1 for e in iw.get("entries") or [] if _is_obj(e) and e.get("kind")),
        "enchantments": ln("enchantments"), "trades": ln("trades"), "lootTables": ln("lootTables"),
        "advancements": ln("advancements"), "tags": ln("tags"),
    }


def _recipe_ids(d: dict) -> list[str]:
    """Recipe ids for links into the mod: without the mod's own namespace ("x" = "<mod>:x"), and without those that are
    also an item/block id there (hrefFor finds the item first)."""
    ns = (d.get("mod") or {}).get("id", "") + ":"
    known = {e.get("id") for k in ("items", "blocks") for e in d.get(k) or [] if _is_obj(e)}
    return [r["id"][len(ns):] if r["id"].startswith(ns) else r["id"]
            for r in d.get("recipes") or [] if _is_obj(r) and r.get("id") and r["id"] not in known]


def mod_index(d: dict, hidden: set[str]) -> dict:
    def ent(e):
        return _row(e["id"], _name(e, "en_us"), _name(e, "de_de"), e.get("icon") or None, e.get("texture") or None,
                    e.get("needsMods") if isinstance(e.get("needsMods"), list) else None)
    out = {
        "head": {k: d[k] for k in ("mod", "generatedFrom") if k in d},
        "counts": counts(d),
        "items": [ent(e) for e in d.get("items") or [] if _is_obj(e) and e.get("id")],
        "blocks": [ent(e) for e in d.get("blocks") or [] if _is_obj(e) and e.get("id")],
        "ench": [_row(e["id"], _name(e, "en_us"), _name(e, "de_de")) for e in d.get("enchantments") or [] if _is_obj(e) and e.get("id")],
        "features": [_row(f["id"], _title(f, "en"), _title(f, "de")) for f in d.get("features") or [] if _is_obj(f) and f.get("id")],
        "recipes": _recipe_ids(d),
        "tags": [t["id"] for t in d.get("tags") or [] if _is_obj(t) and t.get("id")],
    }
    if hidden:
        pub = counts(pruned(d, hidden))
        if pub != out["counts"]:
            out["countsPublic"] = pub
    return out


def build(wiki: Path = WIKI) -> dict:
    mods = catalogue(wiki)
    hidden = {m["id"] for m in mods if m.get("release") is False}
    out = {}
    for m in mods:
        path = wiki / "data" / (m["id"] + ".json")
        if not path.is_file():
            continue
        out[m["id"]] = mod_index(json.loads(path.read_text(encoding="utf-8")), hidden - {m["id"]})
    return {"v": 1, "mods": out}


def render(index: dict) -> str:
    lines = [json.dumps(k) + ": " + json.dumps(v, ensure_ascii=False, separators=(",", ":")) for k, v in index["mods"].items()]
    return ("// Generated by wiki/generate.py (wiki/wiki_index.py) - do not edit.\n"
            "// Cross-mod index: counts, names and pictures of every mod; the page loads a mod's full data only when it shows it.\n"
            + MARKER + '{"v": ' + str(index["v"]) + ', "mods": {\n' + ",\n".join(lines) + "\n}};\n")


def read(wiki: Path = WIKI) -> dict | None:
    try:
        text = (wiki / TARGET).read_text(encoding="utf-8")
        return json.loads(text.split(MARKER, 1)[1].rstrip().rstrip(";"))
    except (OSError, ValueError, IndexError):
        return None


def sync(wiki: Path = WIKI, check: bool = False) -> list[str]:
    try:
        index = build(wiki)
    except (OSError, ValueError, IndexError) as e:
        return [f"wiki/{TARGET} not built: {e}"]
    text = render(index)
    target = wiki / TARGET
    if check:
        if not target.is_file() or target.read_text(encoding="utf-8") != text:
            return [f"wiki/{TARGET} is out of date with the wiki data - run python wiki/generate.py --all"]
        return []
    if not target.is_file() or target.read_text(encoding="utf-8") != text:
        target.write_text(text, encoding="utf-8", newline="\n")
    return []


if __name__ == "__main__":
    import sys
    problems = sync(check="--check" in sys.argv)
    for problem in problems:
        print("PROBLEM:", problem)
    if not problems and "--check" not in sys.argv:
        print(f"wiki/{TARGET}: {(WIKI / TARGET).stat().st_size} bytes")
    sys.exit(1 if problems else 0)
