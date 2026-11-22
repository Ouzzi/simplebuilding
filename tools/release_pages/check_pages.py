#!/usr/bin/env python3
"""Checks store-page drafts: summary <=150 chars, no internal codes, one page per mod in modules.json, in English
(docs/release/pages/<id>.md) and German (docs/release/pages/de/<id>.md, O7). Both languages also get the player-text rules of
tools/check_player_texts.py (internal codes, development history, repo paths; British spelling in English, ASCII umlauts in German).
Modules marked "example": true (developer examples like wiringexample) are never published: they must NOT have a page."""
import json, re, sys, pathlib
FORBIDDEN = re.compile(r"\b[A-Z]{1,4}\d{2,3}[A-Za-z0-9-]*\b|\b[A-Z]-[A-Z]\d+\b|\bN\d{1,3}[a-z]?\b|\b[Bb]-[a-z]+\d+\b|\bStufe\b|\bstage \d\b|\bGate\b|\bQueue\b|wave1|B[uü]ndel|claude-|\bsb-|\b\d\d\.\d\d\.(\d{4})?|\b\d[a-c]-\d\b")
# German pages: "Stufe" (slab, tier) and "Bündel" (bundle) are real words there; only the release-process uses stay forbidden.
FORBIDDEN_DE = re.compile(FORBIDDEN.pattern.replace(r"\bStufe\b", r"\bStufe \d+[a-z]-\d+\b").replace("B[uü]ndel", r"B[uü]ndel \d+"))
R = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(R / "tools"))
import check_player_texts  # noqa: E402
PAGE_ALLOW = {"CC0"}  # license name in the License section, not a task code

# lang -> (sub-folder, summary heading, release-channel heading, forbidden pattern)
LANGS = {"en": ("", "## Summary", "## Release channel: Beta", FORBIDDEN),
         "de": ("de/", "## Kurzbeschreibung", "## Release-Kanal: Beta", FORBIDDEN_DE)}

def check_page(mid, lang, p, bad):
    sub, summary, channel, forbidden = LANGS[lang]
    name = sub + p.name
    if not p.exists(): bad.append(f"missing {name}"); return
    t = p.read_text(encoding="utf-8")
    s = re.search(summary + r"[^\n]*\n\n(.+)\n", t)
    if not s or len(s.group(1)) > 150: bad.append(f"{name}: summary {len(s.group(1)) if s else 'missing'} chars")
    for h in forbidden.finditer(t): bad.append(f"{name}: forbidden '{h.group(0)}'")
    if channel not in t or "> **Beta.**" not in t: bad.append(f"{name}: no Beta release-channel block / Beta note (owner: all mods released as beta)")
    for kind, tok in check_player_texts.findings(mid, lang, t):
        if tok not in PAGE_ALLOW: bad.append(f"{name}: {kind} '{tok}'")

def main():
    bad = []
    for m in json.load(open(R / "modules/modules.json"))["modules"]:
        pages = {lang: R / f"docs/release/pages/{sub}{m['id']}.md" for lang, (sub, *_) in LANGS.items()}
        if m.get("example"):
            for lang, p in pages.items():
                if p.exists(): bad.append(f"{LANGS[lang][0]}{p.name}: example module, never published - delete the draft")
            continue
        if m.get("release") is False:
            continue  # REL75: kept out of the release; an existing draft stays for later but is not generated/listed
        for lang, p in pages.items(): check_page(m["id"], lang, p, bad)
    print("\n".join(bad) or "OK: all pages present in English and German, summaries <=150, no internal codes or player-text findings (codes, dev history, repo paths, British spelling, ASCII umlauts), Beta channel on every page")
    return 1 if bad else 0
if __name__ == "__main__": sys.exit(main())
