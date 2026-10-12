#!/usr/bin/env python3
"""Mod selection for the game start: catalog tree, resolution and launch output.

One source for the Zentrale backend (devqueue.py, per-user selection) and the laptop starter
(``apply``: writes integration/enabled-mods.json and the "Enable Simple X" switches of the super-mods).

A selection is stored as the set of switched-off ids, so new mods start switched on.
Rules (docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md, KONZEPT-SUPERMOD-SUBMOD):
- the parent switch switches its children with it (done by the UI, children stay individually switchable);
- a ``bundle: required`` child is locked on while its parent is on;
- every ``requires`` entry that is a module is switched on automatically (locked, with the reason);
- libraries a super-mod nests (SimpleLib in SimpleBuilding) count as requirements;
- planned sub-mods (not yet in modules.json) are shown locked inside their parent and never launched.
"""
import argparse
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
ID = re.compile(r"^[a-z][a-z0-9_]{1,63}$")
# Libraries nested by a super-mod's jar but not listed in its "requires".
NESTED_LIBS = {"simplebuilding": ["simplelib"]}
# Sub-mods decided in PLAN-MODULAUFTEILUNG §2.1 that are still part of SimpleBuilding; dropped once in modules.json.
PLANNED = [
    {"id": "simpleoctets", "name": "Simple Octets", "description": "1/8-Blöcke, Octet-Zellen und Steinsäge-Wege."},
    {"id": "simplehammers", "name": "Simple Hammers", "description": "Vorschlaghämmer, Hammer-Verzauberungen, In-World-Crafting."},
    {"id": "simplestorage", "name": "Simple Storage", "description": "Stufen-Truhen, Shulker, Rucksäcke, Kisten."},
    {"id": "simpleovens", "name": "Simple Ovens", "description": "Öfen, Räucheröfen und Hochöfen in Stufen."},
    {"id": "simplecrucibles", "name": "Simple Crucibles", "description": "Tiegel, Fässer, verstärkter Kessel, Seelenlava."},
]
DEV_ALIASES = {"cloth-config": "cloth_config"}


def load_registry(repo=REPO):
    return json.loads((Path(repo) / "modules/modules.json").read_text(encoding="utf-8"))["modules"]


def catalog(repo=REPO):
    """Flat list of nodes (parents before children) for the UI and the resolver."""
    modules = [m for m in load_registry(repo) if not m.get("example")]
    ids = {m["id"] for m in modules}
    parents = {m.get("parent") for m in modules if m.get("parent")}
    nodes = []
    for m in modules:
        parent = m.get("parent") if m.get("parent") in ids else None
        kind = "super" if m["id"] in parents else "sub" if parent else "lib" if m["id"].endswith("lib") else "mod"
        requires = [r for r in m.get("requires", []) if r in ids] + [r for r in NESTED_LIBS.get(m["id"], []) if r in ids]
        nodes.append({
            "id": m["id"], "name": m.get("displayName") or m.get("name") or m["id"],
            "description": m.get("description", ""), "kind": kind, "parent": parent,
            "bundle": m.get("bundle") if parent else None, "requires": sorted(set(requires)),
            "external": sorted({DEV_ALIASES.get(r, r) for r in m.get("requires", []) if r not in ids}),
            "loaders": m.get("loaders", []), "minecraft": m.get("minecraft", ""), "planned": False,
        })
    if "simplebuilding" in ids:
        for p in PLANNED:
            if p["id"] not in ids:
                nodes.append({**p, "kind": "sub", "parent": "simplebuilding", "bundle": "required", "requires": [],
                              "external": [], "loaders": [], "minecraft": "", "planned": True})
    order = {n["id"]: i for i, n in enumerate(nodes)}
    top = [n for n in nodes if not n["parent"]]
    top.sort(key=lambda n: ({"super": 0, "mod": 1, "lib": 2}[n["kind"]], order[n["id"]]))
    result = []
    for n in top:
        result.append(n)
        kids = [c for c in nodes if c["parent"] == n["id"]]
        kids.sort(key=lambda c: (c["bundle"] != "required", c["planned"], c["name"].lower()))
        result.extend(kids)
    return result


def normalize_off(off, nodes):
    """Validated, sorted list of switched-off ids (unknown ids are an error, planned ids are ignored)."""
    if not isinstance(off, list) or len(off) > 200 or any(not isinstance(i, str) or not ID.fullmatch(i) for i in off):
        raise ValueError("invalid selection")
    known = {n["id"]: n for n in nodes}
    unknown = [i for i in off if i not in known]
    if unknown:
        raise ValueError("unknown mods: " + ", ".join(sorted(unknown)))
    return sorted({i for i in off if not known[i]["planned"]})


def resolve(nodes, off):
    """Effective state per node: {id: {"on": bool, "locked": reason or None}} plus launch data."""
    off = set(normalize_off(off, nodes))
    by_id = {n["id"]: n for n in nodes}
    state = {}
    for n in nodes:
        parent = by_id.get(n["parent"]) if n["parent"] else None
        if n["planned"]:
            on = parent is not None and state[parent["id"]]["on"]
            state[n["id"]] = {"on": on, "locked": "geplant – heute Teil von " + parent["name"] if parent else "geplant"}
        elif parent and n["bundle"] == "required" and state[parent["id"]]["on"]:
            state[n["id"]] = {"on": True, "locked": "Pflicht-Teil von " + parent["name"]}
        else:
            state[n["id"]] = {"on": n["id"] not in off, "locked": None}
    # requirements: switch on whatever an enabled mod needs (transitively)
    changed = True
    while changed:
        changed = False
        for n in nodes:
            if not state[n["id"]]["on"]:
                continue
            for r in n["requires"]:
                s = state.get(r)
                if s is None:
                    continue
                if not s["on"]:
                    s["on"], changed = True, True
                if not s["locked"]:
                    s["locked"] = "benötigt von " + n["name"]
    return state


def launch_plan(nodes, off, registry):
    """What the starter applies: module ids for the integration run and config switches per super-mod file."""
    state = resolve(nodes, off)
    by_id = {n["id"]: n for n in nodes}
    modules, switches, external = [], {}, set()
    for m in registry:
        if m.get("example"):
            modules.append(m["id"])  # wiring checks, harmless for play
            continue
        n = by_id.get(m["id"])
        if not n or not state[n["id"]]["on"]:
            continue
        external.update(n["external"])
        # enabled children keep their own jar too (the dev jar of a super-mod may not carry its nested jars);
        # a switched-off child of an enabled parent is silenced by its "Enable Simple X" switch below
        modules.append(m["id"])
    for n in nodes:
        parent = by_id.get(n["parent"]) if n["parent"] else None
        if parent and not n["planned"] and state[parent["id"]]["on"]:
            key = "enable" + re.sub(r"[^A-Za-z0-9]", "", n["name"])
            switches.setdefault(parent["id"] + ".json", {})[key] = state[n["id"]]["on"]
    return {"modules": modules, "configSwitches": switches, "devModsNeeded": sorted(external)}


def apply(repo, off, run_dir="integration/run-fabric-263", target=None):
    """Writes the launch selection into a checkout (laptop starter, before :integration:runIntegrationClient)."""
    repo = Path(repo)
    target = Path(target) if target else repo
    sys.path.insert(0, str(repo / "tools"))
    import multimod
    registry = load_registry(repo)
    plan = launch_plan(catalog(repo), off, registry)
    current = multimod.selection(repo)
    _, dev = multimod.registries(repo)
    dev_ids = {d["id"] for d in dev}
    dev_mods = list(current["devMods"]) + [d for d in plan["devModsNeeded"] if d in dev_ids and d not in current["devMods"]]
    value = multimod.validate_selection({"schemaVersion": 1, "modules": plan["modules"], "devMods": dev_mods}, repo)
    multimod.write(target / "integration/enabled-mods.json", value)
    config_dir = target / run_dir / "config"
    for name, keys in plan["configSwitches"].items():
        path = config_dir / name
        data = {}
        if path.exists():
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except ValueError:
                data = {}
        if not isinstance(data, dict):
            data = {}
        data.update(keys)
        multimod.write(path, data)
    return plan


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    sub = ap.add_subparsers(dest="cmd", required=True)
    a = sub.add_parser("apply", help="write enabled-mods.json + Enable-Simple-X switches")
    a.add_argument("--off", default="", help="comma-separated switched-off mod ids (as in simplemods://start?off=)")
    a.add_argument("--repo", default=str(REPO))
    sub.add_parser("catalog", help="print the catalog as JSON").add_argument("--repo", default=str(REPO))
    args = ap.parse_args(argv)
    if args.cmd == "catalog":
        print(json.dumps(catalog(args.repo), indent=2, ensure_ascii=False))
        return 0
    off = [x for x in args.off.replace(";", ",").split(",") if x.strip()]
    try:
        plan = apply(args.repo, [x.strip() for x in off])
    except ValueError as e:
        print("modselect: " + str(e), file=sys.stderr)
        return 2
    print("modselect: %d modules, switches %s" % (len(plan["modules"]),
          ", ".join(f"{f}:{k}={v}" for f, ks in plan["configSwitches"].items() for k, v in ks.items() if not v) or "all on"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
