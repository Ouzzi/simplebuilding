#!/usr/bin/env python3
"""
Stellt das Wiki als statische Seite zusammen - genau das, was wiki/index.html laedt.

    python tools/wiki_site.py                  pruefen, dann nach build/wiki-site kopieren
    python tools/wiki_site.py --out DIR        anderes Ziel
    python tools/wiki_site.py --verify-only    nur pruefen, nichts kopieren

Kopiert werden index.html, data/simplebuilding.js, die eigenen Texturen, auf die die Daten
verweisen, und eine leere .nojekyll. Bewusst NICHT kopiert:
  * wiki/assets/textures/minecraft/ - Mojangs Texturen werden nicht veroeffentlicht. Die Seite
    faengt fehlende Bilder ab und zeigt Vanilla-Zutaten dann als Textkachel;
  * generate.py, manual.json, README.md, HANDOFF.md und data/simplebuilding.json (laedt die
    Seite nicht);
  * Texturen, auf die nichts verweist (werden nur gemeldet).

Geprueft wird, was im Checkout funktioniert, auf einem statischen Host aber bricht:
  * index.html laedt nichts Absolutes, nichts von fremden Servern, nichts ueber "..";
  * data/simplebuilding.js traegt genau das Objekt aus simplebuilding.json - generate.py --check
    vergleicht nur die .json, die Seite laedt aber die .js;
  * jede Textur aus den Daten liegt unter assets/textures/ (nicht unter minecraft/) und existiert
    in genau dieser Schreibweise. Windows merkt einen Gross/Klein-Fehler nicht, GitHub Pages,
    Cloudflare Pages und Netlify schon.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
WIKI = REPO / "wiki"
DATA_JSON = WIKI / "data" / "simplebuilding.json"
DATA_JS = WIKI / "data" / "simplebuilding.js"
TEXTURES = WIKI / "assets" / "textures"
VANILLA_PREFIX = "assets/textures/minecraft/"
JS_MARKER = "window.WIKI_DATA = "
DATA_SCRIPT = 'src="data/simplebuilding.js"'
MARKER = ".nojekyll"  # liegt in jedem hier gebauten Ordner; nur solche Ordner werden geleert


def exists_exact(relative: str) -> bool:
    """Path.exists() ignoriert unter Windows die Schreibweise, ein statischer Host nicht."""
    current = WIKI
    for part in relative.split("/"):
        try:
            if part not in os.listdir(current):
                return False
        except OSError:
            return False
        current = current / part
    return current.is_file()


def referenced_textures(data: dict) -> set[str]:
    refs: set[str] = set()
    for section in ("items", "blocks"):
        for entry in data.get(section, []):
            if entry.get("texture"):
                refs.add(entry["texture"])
            refs.update(v for v in (entry.get("faces") or {}).values() if v)
    return refs


def check_index(html: str) -> list[str]:
    problems = []
    for attr, value in re.findall(r'\b(src|href)\s*=\s*"([^"]*)"', html):
        if not value or value.startswith("#") or "'" in value or "+" in value:
            continue  # Hash-Routen und Attribute, die das Skript zur Laufzeit zusammensetzt
        if re.match(r"^([a-z][a-z0-9+.-]*:|//|/)", value, re.I) or ".." in value.split("/"):
            problems.append(f'index.html: {attr}="{value}" bleibt nicht innerhalb der Seite')
        elif not exists_exact(value.split("?", 1)[0]):
            problems.append(f'index.html: {attr}="{value}" gibt es unter wiki/ nicht')
    if re.search(r"@import|url\(\s*['\"]?(https?:)?//", html, re.I):
        problems.append("index.html: laedt ein Stylesheet oder Bild von einem fremden Server")
    if html.count(DATA_SCRIPT) != 1:
        problems.append(f"index.html: {DATA_SCRIPT} muss genau einmal vorkommen")
    return problems


def check_data(data: dict) -> list[str]:
    problems = []
    js = DATA_JS.read_text(encoding="utf-8") if DATA_JS.exists() else ""
    if JS_MARKER not in js:
        problems.append("wiki/data/simplebuilding.js fehlt oder setzt window.WIKI_DATA nicht")
    else:
        body = js.split(JS_MARKER, 1)[1].rstrip().rstrip(";")
        try:
            if json.loads(body) != data:
                problems.append("wiki/data/simplebuilding.js weicht von simplebuilding.json ab - "
                                "python wiki/generate.py laufen lassen")
        except json.JSONDecodeError as error:
            problems.append(f"wiki/data/simplebuilding.js ist nach der Markierung kein JSON: {error}")
    for ref in sorted(referenced_textures(data)):
        if (not ref.startswith("assets/textures/") or ref.startswith(VANILLA_PREFIX)
                or ".." in ref.split("/")):
            problems.append(f"Daten: Texturpfad {ref!r} liegt nicht unter den eigenen Texturen")
        elif not exists_exact(ref):
            problems.append(f"Daten: wiki/{ref} fehlt (oder weicht in der Schreibweise ab)")
    return problems


def stage(out: Path, data: dict) -> None:
    out = out.resolve()
    if out == REPO or out in REPO.parents or out == WIKI or WIKI in out.parents:
        sys.exit(f"Ziel {out} abgelehnt: Repository-Wurzel oder innerhalb von wiki/")
    if out.exists():
        if any(out.iterdir()) and not (out / MARKER).exists():
            sys.exit(f"Ziel {out} abgelehnt: nicht leer und nicht von diesem Skript angelegt")
        shutil.rmtree(out)
    (out / "data").mkdir(parents=True)

    html = (WIKI / "index.html").read_text(encoding="utf-8")
    # Eine neue index.html soll nie eine alte, zwischengespeicherte Datendatei bekommen.
    version = hashlib.sha256(DATA_JS.read_bytes()).hexdigest()[:12]
    html = html.replace(DATA_SCRIPT, f'src="data/simplebuilding.js?v={version}"')
    (out / "index.html").write_text(html, encoding="utf-8", newline="\n")
    shutil.copyfile(DATA_JS, out / "data" / "simplebuilding.js")
    refs = sorted(referenced_textures(data))
    for ref in refs:
        target = out / ref
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(WIKI / ref, target)
    (out / MARKER).write_text("", encoding="utf-8")  # kein Jekyll-Lauf bei Branch-Deploys

    files = [p for p in out.rglob("*") if p.is_file()]
    total = sum(p.stat().st_size for p in files)
    print(f"{len(files)} Dateien, {total / 1048576:.1f} MB -> {out}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", type=Path, default=REPO / "build" / "wiki-site")
    parser.add_argument("--verify-only", action="store_true")
    args = parser.parse_args()

    if not DATA_JSON.is_file():
        print("FEHLER: wiki/data/simplebuilding.json fehlt - python wiki/generate.py laufen lassen")
        return 1
    data = json.loads(DATA_JSON.read_text(encoding="utf-8"))
    problems = check_index((WIKI / "index.html").read_text(encoding="utf-8")) + check_data(data)
    if problems:
        prefix = "::error::" if os.environ.get("GITHUB_ACTIONS") == "true" else "FEHLER: "
        for problem in problems:
            print(prefix + problem)
        print(f"{len(problems)} Problem(e): so ist das Wiki nicht statisch hostbar.")
        return 1

    refs = referenced_textures(data)
    orphans = sorted(p.relative_to(WIKI).as_posix() for p in TEXTURES.rglob("*.png")
                     if not p.relative_to(WIKI).as_posix().startswith(VANILLA_PREFIX)
                     and p.relative_to(WIKI).as_posix() not in refs)
    for orphan in orphans:
        print(f"Hinweis: wiki/{orphan} wird von nichts referenziert und nicht veroeffentlicht.")
    print("wiki: in sich geschlossen, jede referenzierte Datei vorhanden.")
    if not args.verify_only:
        stage(args.out, data)
    return 0


if __name__ == "__main__":
    sys.exit(main())
