#!/usr/bin/env python3
"""Player texts without internal codes, development history or British spelling (release polish N112).

    python3 tools/check_player_texts.py            check, exit 1 on any finding
    python3 tools/check_player_texts.py --list     print every finding

Checked (fails): the player-facing strings of modules/<id>/wiki/manual.json and of SimpleBuilding's own wiki/manual.json
(every string under an "en"/"de" block: title, summary, details, controls, caveats ...; not "sources", "related", "id"),
which reach the in-game guide book and the wiki, and since N114 every value of every en_us.json/de_de.json lang file
(modules, SimpleBuilding's src/main/resources and its 26.3 overlay): item names, tooltips, config screens, chat
messages, guide pages. Real names that look like findings go to ALLOW / ALLOW_PHRASES.

Findings:
  * code     internal task/plan codes such as N86, WG26-2, IG31-13, PAL35b, (T4), A12, REV2-A-1, PLAN40, docs/ai/...,
             <mod>-<n> (simpleearth-2), "split stage 8c-1", branch names (b-polish110, claude-wave1), "PRINZIPIEN"
  * history  development dates (30.10., 23.10.2026, 2026-09-28), "owner"/"Besitzer" decisions ("(owner)",
             "owner decision"), "since bundle 198", "moved here from"/"hierher umgezogen"
  * british  British spelling in English text (colour, grey, centre, armour, fuelled, cancelled, -ise verbs ...);
             the house style is American English (owner rule "EN amerikanisch").

Real names stay allowed: keys (F1-F12), hex colors (#FF0000), game versions (V1-V27 of Simple Earth, shown on its
buttons), GUI labels (P1/P2 of the Octant), operator level OP4, config values (D3 = every 3rd day), see ALLOW.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

R = Path(__file__).resolve().parents[1]

CODE = re.compile(
    r"#[0-9A-Fa-f]{6}\b"                                  # hex color: consumed here, filtered below
    r"|\b[A-Z]{1,5}\d+[a-z]?(?:-[A-Z]?\d+[a-z]?)*(?:-[A-Z]-\d+)?\b"  # N86, WG26-2, IG31-13, PAL35b, REV2-A-1
    r"|\bPLAN[-\w]*|\bdocs/ai/[\w./-]*|\bPRINZIPIEN\b"
    r"|\bsimple[a-z]+-\d+\b"                              # simpleearth-2 (task codes named after the mod)
    r"|\b(?:split stage|Split-Stufe|Stufe) \d+[a-z]-\d+\b"
    r"|\b(?:a|b|claude)-[a-z]+\d+\w*\b")                  # branch names
HISTORY = re.compile(
    r"(?<![\d.])\b(?:0[1-9]|[12]\d|3[01])\.(?:0[1-9]|1[0-2])\.(?:20\d\d)?(?!\d)"  # 30.10. / 23.10.2026, not 1.21.11.
    r"|\b20\d\d-[01]\d(?:-[0-3]\d)?\b"                                    # 2026-09-28, 2026-09
    r"|\bport[- ]run\b"                                                     # "until the port run" (release process)
    r"|\((?:owner|Besitzer)\)|\bowner decision\b|\bBesitzer-?[Ee]ntscheid\w*"
    r"|\b(?:owner|Besitzer)[ ,]+N\d+"
    r"|\b(?:since|seit|before|vor|until|bis) (?:bundle|Bündel|Paket) \d+"
    r"|\bmoved here\b|\bhierher umgezogen\b", re.I)
BRITISH = re.compile(
    r"\b(?:colour|armour|grey|favourite|neighbour|behaviour|catalogue|centre|metre|litre|honour|harbour|odour|vapour"
    r"|flavour|labour|defence|licence|travell|cancell|fuell|labell|modell|levell|dialogue|programme|jeweller"
    r"|plough|mould|sceptre|lustre|fibre|aluminium|sulphur|whilst|amongst"
    r"|(?:real|organ|recogn|custom|optim|minim|maxim|initial|visual|priorit|normal|util|summar|synchron|stabil"
    r"|neutral|categor|memor|apolog|special|standard|author|capital|character|final|general|local|personal)is(?:e|es|ed|ing|ation|ations|able))\w*",
    re.I)

# Real names that look like codes (exact token match). Per mod where the meaning is local.
ALLOW = {
    "*": {*(f"F{i}" for i in range(1, 13)), "OP4", "MP3", "UTF8", "RGB"},
    "simpleearth": {f"V{i}" for i in range(1, 28)},       # version names on the Earth screen buttons ("V25")
    "simplebuildingtools": {"P1", "P2"},                  # Octant GUI labels (simplebuildingtools.gui.label.pos1)
    "simpletweaks": {"D3"},                               # schedule value "D3" = every 3rd day (command syntax)
    "simplebuilding": {"P1", "P2"},                       # Octant GUI labels (simplebuilding.gui.label.pos1, 26.2 line)
}


def player_strings(node, path="", lang=None):
    if isinstance(node, dict):
        for k, v in node.items():
            if k in ("sources", "related", "id"):
                continue
            yield from player_strings(v, f"{path}.{k}", k if k in ("en", "de") else lang)
    elif isinstance(node, list):
        for i, v in enumerate(node):
            yield from player_strings(v, f"{path}[{i}]", lang)
    elif isinstance(node, str) and lang:
        yield path, lang, node


# Real names that contain a flagged word (Vanilla's own en_us names keep British spelling).
ALLOW_PHRASES = ("A Complete Catalogue",)


def findings(mod: str, lang: str, text: str):
    allow = ALLOW["*"] | ALLOW.get(mod, set())
    for phrase in ALLOW_PHRASES:
        text = text.replace(phrase, "")
    for m in CODE.finditer(text):
        tok = m.group(0)
        if tok.startswith("#") or tok in allow or all(part in allow for part in tok.split("-")):  # V25-V27
            continue
        yield "code", tok
    for m in HISTORY.finditer(text):
        yield "history", m.group(0)
    if lang == "en":
        for m in BRITISH.finditer(text):
            yield "british", m.group(0)


def lang_files():
    for p in sorted(R.glob("modules/*/shared/resources/assets/*/lang/*.json")):
        if p.name in ("en_us.json", "de_de.json"):
            yield p.parts[len(R.parts) + 1], p
    for base in ("src/main/resources", "mc26_3/overlay/resources"):
        for name in ("en_us.json", "de_de.json"):
            p = R / base / "assets/simplebuilding/lang" / name
            if p.is_file():
                yield "simplebuilding", p


def manuals():
    for f in sorted(R.glob("modules/*/wiki/manual.json")):
        yield f.parts[len(R.parts) + 1], f
    if (R / "wiki/manual.json").is_file():
        yield "simplebuilding", R / "wiki/manual.json"


def main(argv):
    hits = []
    for mod, f in manuals():
        for path, lang, text in player_strings(json.loads(f.read_text(encoding="utf-8"))):
            for kind, tok in findings(mod, lang, text):
                hits.append(f"{f.relative_to(R)} {lang}{path}: {kind} '{tok}'")
    for mod, p in lang_files():
        lang = "en" if p.name == "en_us.json" else "de"
        for key, text in json.loads(p.read_text(encoding="utf-8")).items():
            if not isinstance(text, str):
                continue
            for kind, tok in findings(mod, lang, text):
                hits.append(f"{p.relative_to(R)} {key}: {kind} '{tok}'")
    if "--list" in argv or "--all-lang" in argv:
        print("\n".join(hits))
    if hits:
        print(f"PLAYER_TEXTS FAIL: {len(hits)} internal code / development history / British spelling finding(s) in "
              "manual.json or lang files" + ("" if "--list" in argv else "; run with --list") +
              ". Rewrite in plain player language (or add a real name to ALLOW).")
        return 1
    print("PLAYER_TEXTS_OK: manual.json player fields and all lang values free of internal codes, dev history and "
          "British spelling")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
