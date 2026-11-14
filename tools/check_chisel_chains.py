#!/usr/bin/env python3
"""Chisel chains of the 26.3 line without Minecraft (simplechisels-1, PLAN40).

Reads every ``data/<ns>/chisel_transformations/*.json`` that ships on 26.3 (modules, SimpleBuilding's 26.3 layers) in the
order Simple Chisels applies them (``ChiselTables``: the ``simplechisels`` namespace first, then the others by id) and checks:

1. every block a chain names exists on 26.3 (a blockstate file; Vanilla ids only with the client jar), so no link is
   skipped in game;
2. no file silently replaces a link another file already set in the same tier and table (an addition must hang its new
   blocks at the end of a chain or start a new one; the reasoned cases are in ``OVERRIDE_OK``);
3. every chiseled, cracked or mossy variant of a Simple mod is the target of some link (reachable with a chisel), except
   the reasoned ``EXEMPT`` blocks, bookshelves and octets. The GameTest ``own_variants_reachable`` checks the same in game;
4. no link joins two blocks of different worth (dupe guard): the stonecutter makes 4 Cut Iron from one Block of Iron, so a
   link Block of Iron <-> Cut Iron would turn one block into four (chisel back). Worth = 1/count of a stonecutting recipe from
   a common source block (the repo's 26.3 recipes, plus Vanilla's from the client jar when it is there: ``--jar <jar>``,
   default the Loom cache).

    python3 tools/check_chisel_chains.py [--list] [--jar client.jar]      --list prints every composed link
Exit 1 on an error.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REL = "chisel_transformations"
TIERS = ["stone", "iron", "diamond", "netherite", "enderite"]
TABLES = ["chisel", "touch"]
VARIANT = re.compile(r"^(chiseled|cracked|mossy)_")
EXEMPT = {
    # Storage block of the Cracked Diamond material (9 cracked diamonds), not a cracked look of the diamond block: a chain
    # diamond_block <-> cracked_diamond_block would turn cracked diamonds into diamonds.
    "simplelib:cracked_diamond_block": "storage block of a material, not a variant",
    "simplebuilding:cracked_diamond_block": "26.2 id of the same storage block",
    # PAL35c: emerald_block <-> cracked_emerald_block would turn cracked emeralds into emeralds.
    "simplelib:cracked_emerald_block": "storage block of a material, not a variant",
}
# (file id, block, block) links between blocks of different worth that are accepted on purpose (none).
VALUE_OK = set()
# (file id, tier/table, block) whose link is replaced on purpose. Empty: every addition so far hangs at a chain end.
OVERRIDE_OK = set()


def sb_removed():
    p = ROOT / "mc26_3/generated/removed-on-26.3.txt"
    if not p.is_file():
        return set()
    return {l.strip() for l in p.read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")}


def sb_layers():
    """SimpleBuilding's 26.3 resource layers, newest first: (root, honours the removed list)."""
    return [(ROOT / "mc26_3/overlay/resources", False), (ROOT / "mc26_3/generated", False),
            (ROOT / "src/main/generated", True), (ROOT / "src/main/resources", True)]


def resource_roots():
    """Every 26.3 resource root as (root, is_sb_layer)."""
    roots = []
    for mod in sorted((ROOT / "modules").iterdir()):
        for sub in ("shared/resources", "generated/resources", "fabric/src/main/resources"):
            r = mod / sub
            if r.is_dir():
                roots.append(r)
    return roots


def shipped(rel_glob):
    """{relative path: file} of 26.3 files matching ``rel_glob`` (SimpleBuilding's newest layer wins, removed files hidden)."""
    out = {}
    for r in resource_roots():
        for f in r.glob(rel_glob):
            out.setdefault(str(f.relative_to(r)), f)
    removed = sb_removed()
    for r, honours in sb_layers():
        if not r.is_dir():
            continue
        for f in r.glob(rel_glob):
            rel = str(f.relative_to(r))
            if honours and rel in removed:
                continue
            out.setdefault(rel, f)
    return out


def known_blocks():
    ids = set()
    for rel in shipped("assets/*/blockstates/*.json"):
        parts = rel.split("/")
        ids.add(parts[1] + ":" + parts[3][:-5])
    return ids


def stonecutting(jar):
    """{result: {source: count}} of every 26.3 stonecutting recipe (repo, plus the Vanilla jar when readable)."""
    texts = [f.read_text(encoding="utf-8") for f in shipped("data/*/recipe/*.json").values()]
    if jar and Path(jar).is_file():
        import zipfile
        with zipfile.ZipFile(jar) as z:
            for n in z.namelist():
                if n.startswith("data/minecraft/recipe/") and n.endswith(".json"):
                    texts.append(z.read(n).decode("utf-8"))
    out = {}
    for t in texts:
        try:
            d = json.loads(t)
        except Exception:  # noqa: BLE001
            continue
        if d.get("type") != "minecraft:stonecutting" or not isinstance(d.get("ingredient"), str):
            continue
        res = d.get("result", {})
        if isinstance(res, dict) and "id" in res:
            out.setdefault(res["id"], {})[d["ingredient"]] = res.get("count", 1)
    return out


def worth(cut, block):
    """{source: worth of block in source blocks} (the source itself is worth 1)."""
    w = {block: 1.0}
    for src, n in cut.get(block, {}).items():
        w.setdefault(src, 1.0 / n)
    return w


def vanilla_blocks(jar):
    """Vanilla block ids from the client jar's blockstates, or None without a readable jar."""
    if not jar or not Path(jar).is_file():
        return None
    import zipfile
    with zipfile.ZipFile(jar) as z:
        return {"minecraft:" + n.rsplit("/", 1)[1][:-5] for n in z.namelist()
                if n.startswith("assets/minecraft/blockstates/") and n.endswith(".json")}


def chain_files():
    files = []
    for rel, f in shipped(f"data/*/{REL}/*.json").items():
        parts = rel.split("/")
        files.append((parts[1], "/".join(parts[3:])[:-5], f))
    # ChiselTables: the simplechisels namespace first, all others after them in id order.
    files.sort(key=lambda t: (t[0] != "simplechisels", t[0] + ":" + t[1]))
    return files


def main(argv):
    jar = argv[argv.index("--jar") + 1] if "--jar" in argv else str(Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client.jar")
    cut = stonecutting(jar)
    blocks = known_blocks()
    vanilla = vanilla_blocks(jar)
    errors, links = [], {}
    setter = {}
    removals = {}
    for ns, path, f in chain_files():
        fid = f"{ns}:{path}"
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
        except Exception as e:  # noqa: BLE001
            errors.append(f"{fid}: unreadable ({e})")
            continue
        tier, table = data.get("tier", "stone"), data.get("table", "chisel")
        if tier not in TIERS or table not in TABLES:
            errors.append(f"{fid}: unknown tier/table {tier}/{table}")
            continue
        key = f"{tier}/{table}"
        fwd = links.setdefault(key, {})
        for chain in data.get("chains", []):
            ids = chain.get("blocks", [])
            for b in ids:
                if (b not in vanilla if b.startswith("minecraft:") and vanilla is not None else not b.startswith("minecraft:") and b not in blocks):
                    errors.append(f"{fid}: unknown block {b}")
            n = len(ids) if chain.get("cyclic") else len(ids) - 1
            for i in range(max(0, n)):
                a, b = ids[i], ids[(i + 1) % len(ids)]
                old = fwd.get(a)
                if old is not None and old != b and setter[(key, a)] != fid and (fid, key, a) not in OVERRIDE_OK:
                    errors.append(f"{fid}: replaces {key} {a} -> {old} (set by {setter[(key, a)]}) with -> {b}")
                fwd[a] = b
                setter[(key, a)] = fid
                # Shape steps (block -> stairs -> slab) are the documented Vanilla behaviour of the chisel (planks into stairs,
                # stairs into slabs, Vanilla wood chains); the guard is about full blocks multiplied by the stonecutter.
                shape = any(x.endswith(("_stairs", "_slab")) for x in (a, b))
                wa, wb = ({}, {}) if shape else (worth(cut, a), worth(cut, b))
                for src in set(wa) & set(wb):
                    if abs(wa[src] - wb[src]) > 1e-9 and (fid, a, b) not in VALUE_OK:
                        errors.append(f"{fid}: {a} <-> {b} differ in worth (from {src}: {wa[src]:g} vs {wb[src]:g}) - the chisel would make material")
        for b in data.get("remove", []):
            removals.setdefault(key, set()).add(b)
    for key, gone in removals.items():
        for b in gone:
            links.get(key, {}).pop(b, None)
    targets = {b for fwd in links.values() for b in fwd.values()}
    sources = {a for fwd in links.values() for a in fwd}
    for b in sorted(blocks):
        ns, path = b.split(":")
        if not ns.startswith("simple") or not VARIANT.search(path) or path.endswith(("_bookshelf", "_octet")) or b in EXEMPT:
            continue
        if b not in targets:
            errors.append(f"unreachable with a chisel: {b}" + ("" if b in sources else " (in no chain)"))
    if "--list" in argv:
        for key in sorted(links):
            for a, b in sorted(links[key].items()):
                print(f"{key:18} {a} -> {b}")
    for e in errors:
        print("ERROR", e)
    print(f"chisel chains: {len(chain_files())} files, {sum(len(v) for v in links.values())} links, "
          f"{len(errors)} problem(s)" + ("" if errors else "; every Simple variant reachable"))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
