#!/usr/bin/env python3
"""Generate the store page drafts: docs/release/pages/<modid>.md (English) + docs/release/pages/de/<modid>.md (German)
and both INDEX.md files. Run from repo root: python3 tools/release_pages/gen_pages.py"""
import json, re, sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from pages_data import P, V
import pages_data, pages_data_de
LINKS = getattr(pages_data, "LINKS", {})
G = getattr(pages_data, "G", {})
FEATURE_TEXT = getattr(pages_data, "FEATURE_TEXT", {})
# German copy (O7): same keys as P/V/G/FEATURE_TEXT; categories and tags stay the English Modrinth identifiers of P.
TEXT = {"en": (P, V, G, FEATURE_TEXT), "de": (pages_data_de.P_DE, pages_data_de.V_DE, pages_data_de.G_DE, pages_data_de.FEATURE_TEXT_DE)}
# External mods a feature of the jar uses only when present, not named in modules.json (its "optional" must equal the jar's
# suggests, tools/multimod.py): Simple Visuals and Simple Sounds open their Mod Menu config screen only with Cloth Config.
OPT_EXTRA = {"simplevisuals": ["cloth_config"], "simplesounds": ["cloth_config"]}
from check_pages import FORBIDDEN, FORBIDDEN_DE
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
NO_SWITCH = {"en": {"simplebuilding": ", except Simple Crucibles (switched in the Simple Ovens config) and Simple Tools,"},
             "de": {"simplebuilding": ", außer Simple Crucibles (geschaltet in der Konfiguration von Simple Ovens) und Simple Tools"}}
OPT = {"simpleoptimization", "simpleoptrender", "simpleoptmemory", "simpleoptserver"}
# Fixed page text per language. {x} fields are filled per mod.
T = {
"en": dict(
    title="{name} - store page draft", status="Status: **draft**", summary="## Summary (Modrinth, max 150 characters)",
    channel="## Release channel: Beta", vnum="- Version number: `{v}`",
    vtype="- Modrinth \"Version type\": **Beta**; CurseForge \"Release type\": **Beta** (never Release for this first version)",
    desc="## Description (Markdown)",
    beta=("> **Beta.** This mod is released as a beta: features, balance and config options can still change between versions, "
          "and bugs are possible. Back up your world before you install or update it, and report problems on the issue tracker."),
    opt=("> No performance promises: the effect has not been measured across computers and mod packs yet, so this page names "
         "no FPS, memory or tick-time figures. Results depend on your hardware and the other mods you run."),
    features="## Features", vanilla="## Vanilla use", good="## Good to know", deps="## Dependencies",
    req="- **Required:** Fabric API", req_none=" (no other mods)", incl="- **Included in this download:** ", optional="- **Optional:** ",
    none="none", compat="## Compatibility", compat1="- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Java 25",
    compat2="- Fabric only for now.", family="## Part of the Simple family",
    by=" Simple mods that need it as a separate download: {mods}.",
    super=("**Super mod.** Its sub-mods ({subs}) are included in this download; each can be switched off in the config "
           "(\"Enable Simple XY\"){noswitch} and each is also available on its own."),
    lib="**Library.** Every Simple mod includes SimpleLib in its own download, so you never have to install it by hand.",
    sub="**Sub-mod** of {supers}: included in that download, and it also runs on its own with the required mods above.{by}",
    alone="**Standalone mod.** It is not part of a super mod; install it on its own with the required mods above.{by}",
    license="## License",
    license1="All Rights Reserved with source-available code. Contributions need the contributor license agreement (CLA). Earlier SimpleBuilding versions stay CC0.",
    links="## Links", linknames=("Wiki", "Issues", "Source"), cats="## Categories and tags (suggestion)",
    cat="- Modrinth categories: {c}", tags="- Tags: {t}", env="- Environment: client and server"),
"de": dict(
    title="{name} - Entwurf der Store-Seite (Deutsch)", status="Status: **Entwurf**", summary="## Kurzbeschreibung (Modrinth, höchstens 150 Zeichen)",
    channel="## Release-Kanal: Beta", vnum="- Versionsnummer: `{v}`",
    vtype="- Modrinth „Version type“: **Beta**; CurseForge „Release type“: **Beta** (für diese erste Version nie Release)",
    desc="## Beschreibung (Markdown)",
    beta=("> **Beta.** Diese Mod erscheint als Beta: Funktionen, Balance und Konfigurationsoptionen können sich zwischen den Versionen "
          "noch ändern, und Fehler sind möglich. Sichere deine Welt, bevor du die Mod installierst oder aktualisierst, und melde "
          "Probleme im Issue-Tracker."),
    opt=("> Keine Leistungsversprechen: Die Wirkung wurde noch nicht auf verschiedenen Rechnern und in Modpacks gemessen, deshalb "
         "nennt diese Seite keine Werte für FPS, Speicher oder Tickzeit. Das Ergebnis hängt von deiner Hardware und deinen anderen Mods ab."),
    features="## Funktionen", vanilla="## Im Vanilla-Spiel", good="## Gut zu wissen", deps="## Abhängigkeiten",
    req="- **Erforderlich:** Fabric API", req_none=" (keine weiteren Mods)", incl="- **In diesem Download enthalten:** ",
    optional="- **Optional:** ", none="keine", compat="## Kompatibilität",
    compat1="- Minecraft 26.3, Fabric Loader 0.19.5 oder neuer, Java 25", compat2="- Vorerst nur für Fabric.",
    family="## Teil der Simple-Familie",
    by=" Simple-Mods, die sie als eigenen Download brauchen: {mods}.",
    super=("**Super-Mod.** Ihre Sub-Mods ({subs}) sind in diesem Download enthalten; jede lässt sich in der Konfiguration "
           "abschalten („Simple XY aktivieren“){noswitch}, und jede gibt es auch einzeln."),
    lib="**Bibliothek.** Jede Simple-Mod enthält SimpleLib in ihrem eigenen Download, du musst sie also nie von Hand installieren.",
    sub="**Sub-Mod** von {supers}: im Download der Super-Mod enthalten, und sie läuft auch allein mit den oben genannten erforderlichen Mods.{by}",
    alone="**Eigenständige Mod.** Sie gehört zu keiner Super-Mod; installiere sie allein mit den oben genannten erforderlichen Mods.{by}",
    license="## Lizenz",
    license1="All Rights Reserved, der Quellcode ist einsehbar (source-available). Beiträge setzen die Mitwirkenden-Lizenzvereinbarung (Contributor License Agreement, CLA) voraus. Frühere SimpleBuilding-Versionen bleiben CC0.",
    links="## Links", linknames=("Wiki", "Issues", "Quellcode"), cats="## Kategorien und Tags (Vorschlag)",
    cat="- Modrinth-Kategorien: {c}", tags="- Tags: {t}", env="- Umgebung: Client und Server"),
}

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

def clean(t, lang):
    t = t.strip()
    return t if t and not (FORBIDDEN_DE if lang == "de" else FORBIDDEN).search(t) else ""

# German abbreviations that end with ". " inside a sentence (z. B., u. a., d. h., bzw., ca., usw.): never a sentence end.
DE_ABBR = re.compile(r"(?:\b(?:z|B|u|a|d|h|s|o|bzw|ca|usw|etc|evtl|ggf|inkl|max|min|Nr|vgl|bspw|sog|zB)\.)$")

# English abbreviations that end with ". " inside a sentence (e.g., i.e., etc., vs.): never a sentence end (the Dragon Fight
# page was cut at "e.g.").
EN_ABBR = re.compile(r"(?:\b(?:e\.g|i\.e|etc|vs|approx|incl|min|max|No)\.)$")

def first_sentence(text, lang):
    abbr = DE_ABBR if lang == "de" else EN_ABBR
    parts, cur = text.split(". "), ""
    for p in parts:
        cur = f"{cur}. {p}" if cur else p
        if not abbr.search(cur + "."):
            return cur
    return cur

# Longest feature sentence on a page. The German page shows a sentence exactly where the English page does (German runs
# about 15-20 % longer, so it gets a wider cap), which keeps both pages parallel.
MAXLEN = {"en": 170, "de": 240}

def features(i, lang):
    p = R / f"modules/{i}/wiki/manual.json"
    out, ft = [], TEXT[lang][3]
    if p.exists():
        for x in json.load(open(p)).get("features", []):
            fid = x.get("id", "")
            # config entries are listed in the wiki; Forge adapter entries name a developer build flag, the page says Fabric only
            if fid.startswith("config_") or "forge" in fid: continue
            loc = x.get(lang) or {}
            top = x.get("title") if isinstance(x.get("title"), dict) else {}  # some manuals keep the title as {"en", "de"}
            t = clean(loc.get("title") or top.get(lang, ""), lang)
            # raw trade ids and per-mod price tables read badly; the Vanilla-use text describes the offers
            if not t or t.startswith(("Trade: ", "Handel: ", "Linked Price Table: ", "Verknüpfte Preisliste: ")): continue
            s = clean(first_sentence(loc.get("summary", ""), lang).rstrip("."), lang)
            en = clean(first_sentence((x.get("en") or {}).get("summary", ""), "en").rstrip("."), "en")
            show = bool(s) and len(s) <= MAXLEN[lang] and bool(en) and len(en) <= MAXLEN["en"]
            if (i, fid) in ft:  # store wording for a manual feature (None = not on the store page)
                if ft[(i, fid)] is None: continue
                t, s = ft[(i, fid)]
                show = bool(s)
            out.append(f"- **{t}**" + (f" - {s}." if show else ""))
    return out[:12]

def page(m, lang):
    i, L = m["id"], T[lang]
    Pl, Vl, Gl, _ = TEXT[lang]
    summ, over = Pl[i][:2]
    cats, tags = P[i][2:]
    inc = nested(i)
    need = jar_requires(i)
    req = [nm(x) for x in sorted(need - inc, key=lambda x: (x in byid, nm(x)))]  # separate downloads: external mods first
    incl = [nm(x) for x in dict.fromkeys(["simplelib"] + m.get("requires", []) + sorted(inc)) if x in inc]  # SimpleLib first
    opt = [nm(x) for x in dict.fromkeys(m.get("optional", []) + [x for x in m.get("requires", []) if canon(x) not in need] + OPT_EXTRA.get(i, [])) if x not in inc]
    released = lambda o: o.get("release") is not False and not o.get("example") and o["id"] != i
    supers = [nm(o["id"]) for o in mods if o["id"] in SUPERS and released(o) and i in nested(o["id"])]
    needed_by = [nm(o["id"]) for o in mods if released(o) and o["id"] not in SUPERS and i in jar_requires(o["id"]) and i not in nested(o["id"])]
    by = L["by"].format(mods=", ".join(needed_by)) if needed_by else ""
    if i in SUPERS: rel = L["super"].format(subs=", ".join(x for x in incl if x != "SimpleLib"), noswitch=NO_SWITCH[lang].get(i, ""))
    elif i == "simplelib": rel = L["lib"]
    elif supers: rel = L["sub"].format(supers=", ".join(supers), by=by)
    else: rel = L["alone"].format(by=by)
    lines = [f"# {L['title'].format(name=m['name'])}", "", L["status"], "", L["summary"], "", summ, "",
        L["channel"], "", L["vnum"].format(v=version(m)), L["vtype"], "",
        L["desc"], "", f"# {m['name']}", "", L["beta"], ""] + ([L["opt"], ""] if i in OPT else []) + [over, ""]
    f = features(i, lang)
    if f: lines += [L["features"], ""] + f + [""]
    if i in Vl: lines += [L["vanilla"], "", Vl[i], ""]
    if i in Gl: lines += [L["good"], "", f"- {Gl[i]}", ""]
    lines += [L["deps"], "",
        L["req"] + "".join(f", {x}" for x in req) + ("" if req else L["req_none"]),
        *([L["incl"] + ", ".join(incl)] if incl else []),
        L["optional"] + (", ".join(opt) if opt else L["none"]), "",
        L["compat"], "", L["compat1"], L["compat2"], "", L["family"], "", rel, "",
        L["license"], "", L["license1"], "",
        L["links"], "", *[f"- {k}: {u}" for k, u in zip(L["linknames"], LINKS.get(i, (f"https://wiki.simplemods.de/?mod={i}", "https://github.com/Ouzzi/simplebuilding/issues", "https://github.com/Ouzzi/simplebuilding")))], "",
        L["cats"], "", L["cat"].format(c=cats), L["tags"].format(t=tags), L["env"], ""]
    return "\n".join(lines) + "\n", summ

rows, rows_de = [], []
(R / "docs/release/pages/de").mkdir(parents=True, exist_ok=True)
for m in mods:
    i = m["id"]
    if i not in P or m.get("example") or m.get("release") is False: continue  # examples and "release": false (REL75) get no store page
    if i not in TEXT["de"][0]: sys.exit(f"pages_data_de.P_DE has no German copy for {i}")
    text, summ = page(m, "en")
    (R / f"docs/release/pages/{i}.md").write_text(text)
    text_de, summ_de = page(m, "de")
    (R / f"docs/release/pages/de/{i}.md").write_text(text_de)
    rows.append(f"| [{m['name']}]({i}.md) | `{i}` | `{version(m)}` | Beta | {summ} | draft | [DE](de/{i}.md) |")
    rows_de.append(f"| [{m['name']}]({i}.md) | `{i}` | `{version(m)}` | Beta | {summ_de} | Entwurf | [EN](../{i}.md) |")
idx = ["# Store pages - index", "", f"{len(rows)} drafts for Modrinth / CurseForge, each in English and German (`de/`). Generated by `tools/release_pages/gen_pages.py` from `tools/release_pages/pages_data.py`, `tools/release_pages/pages_data_de.py` and `modules/modules.json`; check with `python3 tools/release_pages/check_pages.py`. Texts are drafts and need owner review before publishing. Every mod is released as a **beta** (version `<x.y.z>-beta.<n>`, release channel Beta, no performance promises); check with `python3 tools/release/check_beta.py`.", "",
       "| Mod | Id | Version | Channel | Summary | Status | German |", "|---|---|---|---|---|---|---|"] + rows
(R / "docs/release/pages/INDEX.md").write_text("\n".join(idx) + "\n")
idx_de = ["# Store-Seiten (Deutsch) - Übersicht", "", f"{len(rows_de)} deutsche Entwürfe für Modrinth / CurseForge, erzeugt von `tools/release_pages/gen_pages.py` aus `tools/release_pages/pages_data_de.py`, den deutschen Texten der Handbücher und `modules/modules.json`; Prüfung mit `python3 tools/release_pages/check_pages.py`. Die Texte sind Entwürfe und brauchen vor der Veröffentlichung eine Durchsicht. Jede Mod erscheint als **Beta**.", "",
          "| Mod | Id | Version | Kanal | Kurzbeschreibung | Status | Englisch |", "|---|---|---|---|---|---|---|"] + rows_de
(R / "docs/release/pages/de/INDEX.md").write_text("\n".join(idx_de) + "\n")
print(len(rows), "pages,", len(rows_de), "German pages")
