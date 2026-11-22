#!/usr/bin/env python3
"""Generate docs/release/pages/<modid>.md + INDEX.md (store page drafts). Run from repo root: python3 tools/release_pages/gen_pages.py"""
import json, re, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from pages_data import P, V
import pages_data
LINKS = getattr(pages_data, "LINKS", {})
G = getattr(pages_data, "G", {})
FEATURE_TEXT = getattr(pages_data, "FEATURE_TEXT", {})
# External mods a feature of the jar uses only when present, not named in modules.json (its "optional" must equal the jar's
# suggests, tools/multimod.py): Simple Visuals and Simple Sounds open their Mod Menu config screen only with Cloth Config.
OPT_EXTRA = {"simplevisuals": ["cloth_config"], "simplesounds": ["cloth_config"]}
from check_pages import FORBIDDEN
R = pathlib.Path(__file__).resolve().parents[2]
mods = json.load(open(R / "modules/modules.json"))["modules"]
byid = {m["id"]: m for m in mods}
EXT = {"cloth-config": "Cloth Config", "cloth_config": "Cloth Config", "jei": "JEI", "roughlyenoughitems": "Roughly Enough Items",
       "modmenu": "Mod Menu", "jade": "Jade", "appleskin": "AppleSkin", "mousetweaks": "Mouse Tweaks",
       "trinkets_updated": "Trinkets Updated", "curios": "Curios", "flan": "Flan", "ftbchunks": "FTB Chunks",
       "openpartiesandclaims": "Open Parties and Claims", "griefdefender": "GriefDefender", "claimchunk": "ClaimChunk"}
nm = lambda i: byid[i]["name"] if i in byid else EXT.get(i, i)
SUPERS = {"simplebuilding", "simplequalityoflife", "simpleaesthetics", "simpleoptimization"}
# Owner W3B (15.11.2026): every mod is first released as a beta - version <x.y.z>-beta.<n> from modules.json (SimpleBuilding:
# gradle.properties mod_version), release channel Beta on Modrinth and CurseForge, no performance promises.
MOD_VERSION = re.search(r"^mod_version=(.+)$", (R / "gradle.properties").read_text(), re.M).group(1).strip()
version = lambda m: MOD_VERSION if m["version"] == "${mod_version}" else m["version"]
# Super-mod sub-mods without their own switch in the super mod's config (checked in SimplebuildingConfig / OvensConfig).
NO_SWITCH = {"simplebuilding": ", except Simple Crucibles (switched in the Simple Ovens config) and Simple Tools,"}
OPT = {"simpleoptimization", "simpleoptrender", "simpleoptmemory", "simpleoptserver"}
BETA_NOTE = ("> **Beta.** This mod is released as a beta: features, balance and config options can still change between versions, "
             "and bugs are possible. Back up your world before you install or update it, and report problems on the issue tracker.")
OPT_NOTE = ("> No performance promises: the effect has not been measured across computers and mod packs yet, so this page names "
            "no FPS, memory or tick-time figures. Results depend on your hardware and the other mods you run.")

PLATFORM = {"fabricloader", "minecraft", "java", "fabric-api"}
canon = lambda d: "cloth-config" if d == "cloth_config" else d

def nested(i):
    """Simple mods packed into this mod's Fabric jar (jar-in-jar), so a player never downloads them separately."""
    if i == "simplebuilding":  # mc26_3/fabric/build.gradle: the release jar nests the required sub-mods (+ bundleOptional)
        e = byid[i]
        return {x for x in e.get("requires", []) + e.get("bundleOptional", []) if x in byid}
    g = R / f"modules/{i}/fabric/build.gradle"
    return set(re.findall(r"include project\(':modules:([a-z0-9]+):fabric'\)", g.read_text())) if g.exists() else set()

def jar_requires(i):
    """Hard dependencies of the Fabric jar: fabric.mod.json "depends" (SimpleBuilding: modules.json, the 26.3 build injects them)."""
    if i == "simplebuilding": return {canon(x) for x in byid[i].get("requires", [])}
    f = R / f"modules/{i}/fabric/src/main/resources/fabric.mod.json"
    return {canon(x) for x in json.load(open(f)).get("depends", {})} - PLATFORM

def clean(t):
    t = t.strip()
    return t if t and not FORBIDDEN.search(t) else ""

def features(i):
    p = R / f"modules/{i}/wiki/manual.json"
    out = []
    if p.exists():
        for x in json.load(open(p)).get("features", []):
            if x.get("id", "").startswith("config_"): continue
            en = x.get("en") or {}
            t = clean(en.get("title", ""))
            if not t or t.startswith("Trade: "): continue  # raw trade ids read badly; the Vanilla-use text describes the offers
            s = clean((en.get("summary", "").split(". ")[0]).rstrip("."))
            if (i, x.get("id")) in FEATURE_TEXT:  # store wording for a manual feature (None = not on the store page)
                if FEATURE_TEXT[(i, x.get("id"))] is None: continue
                t, s = FEATURE_TEXT[(i, x.get("id"))]
            out.append(f"- **{t}**" + (f" - {s}." if s and len(s) <= 170 else ""))
    return out[:12]

rows = []
for m in mods:
    i = m["id"]
    if i not in P or m.get("example") or m.get("release") is False: continue  # examples and "release": false (REL75) get no store page
    summ, over, cats, tags = P[i]
    inc = nested(i)
    need = jar_requires(i)
    req = [nm(x) for x in sorted(need - inc, key=lambda x: (x in byid, nm(x)))]  # separate downloads: external mods first
    incl = [nm(x) for x in dict.fromkeys(["simplelib"] + m.get("requires", []) + sorted(inc)) if x in inc]  # SimpleLib first
    opt = [nm(x) for x in dict.fromkeys(m.get("optional", []) + [x for x in m.get("requires", []) if canon(x) not in need] + OPT_EXTRA.get(i, [])) if x not in inc]
    released = lambda o: o.get("release") is not False and not o.get("example") and o["id"] != i
    supers = [nm(o["id"]) for o in mods if o["id"] in SUPERS and released(o) and i in nested(o["id"])]
    needed_by = [nm(o["id"]) for o in mods if released(o) and o["id"] not in SUPERS and i in jar_requires(o["id"]) and i not in nested(o["id"])]
    by = f" Simple mods that need it as a separate download: {', '.join(needed_by)}." if needed_by else ""
    if i in SUPERS: rel = f"**Super mod.** Its sub-mods ({', '.join(x for x in incl if x != 'SimpleLib')}) are included in this download; each can be switched off in the config (\"Enable Simple XY\"){NO_SWITCH.get(i, '')} and each is also available on its own."
    elif i == "simplelib": rel = "**Library.** Every Simple mod includes SimpleLib in its own download, so you never have to install it by hand."
    elif supers: rel = f"**Sub-mod** of {', '.join(supers)}: included in that download, and it also runs on its own with the required mods above.{by}"
    else: rel = f"**Standalone mod.** It is not part of a super mod; install it on its own with the required mods above.{by}"
    lines = [f"# {m['name']} - store page draft", "", "Status: **draft**", "",
        "## Summary (Modrinth, max 150 characters)", "", summ, "",
        "## Release channel: Beta", "", f"- Version number: `{version(m)}`",
        "- Modrinth \"Version type\": **Beta**; CurseForge \"Release type\": **Beta** (never Release for this first version)", "",
        "## Description (Markdown)", "", f"# {m['name']}", "", BETA_NOTE, ""] + ([OPT_NOTE, ""] if i in OPT else []) + [over, ""]
    f = features(i)
    if f: lines += ["## Features", ""] + f + [""]
    if i in V: lines += ["## Vanilla use", "", V[i], ""]
    if i in G: lines += ["## Good to know", "", f"- {G[i]}", ""]
    lines += ["## Dependencies", "",
        "- **Required:** Fabric API" + "".join(f", {x}" for x in req) + ("" if req else " (no other mods)"),
        *([f"- **Included in this download:** {', '.join(incl)}"] if incl else []),
        "- **Optional:** " + (", ".join(opt) if opt else "none"), "",
        "## Compatibility", "", "- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25",
        "- Fabric only for now.", "", "## Part of the Simple family", "", rel, "",
        "## License", "", "All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.", "",
        "## Links", "", *[f"- {k}: {u}" for k, u in zip(("Wiki", "Issues", "Source"), LINKS.get(i, (f"https://wiki.simplemods.de/?mod={i}", "https://github.com/Ouzzi/simplebuilding/issues", "https://github.com/Ouzzi/simplebuilding")))], "",
        "## Categories and tags (suggestion)", "", f"- Modrinth categories: {cats}", f"- Tags: {tags}", "- Environment: client and server", ""]
    (R / "docs/release/pages").mkdir(parents=True, exist_ok=True)
    (R / f"docs/release/pages/{i}.md").write_text("\n".join(l for l in lines if l is not None) + "\n")
    rows.append(f"| [{m['name']}]({i}.md) | `{i}` | `{version(m)}` | Beta | {summ} | draft |")
idx = ["# Store pages - index", "", f"{len(rows)} drafts for Modrinth / CurseForge. Generated by `tools/release_pages/gen_pages.py` from `tools/release_pages/pages_data.py` and `modules/modules.json`; check with `python3 tools/release_pages/check_pages.py`. Texts are drafts and need owner review before publishing. Every mod is released as a **beta** (version `<x.y.z>-beta.<n>`, release channel Beta, no performance promises); check with `python3 tools/release/check_beta.py`.", "",
       "| Mod | Id | Version | Channel | Summary | Status |", "|---|---|---|---|---|---|"] + rows
(R / "docs/release/pages/INDEX.md").write_text("\n".join(idx) + "\n")
print(len(rows), "pages")
