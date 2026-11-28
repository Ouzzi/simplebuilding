#!/usr/bin/env python3
"""O8 audit 4: every mod names the same author(s) in its loader metadata (simplemoney's fabric.mod.json named a person
instead of the project's author name, the only one of 60 jars; owner data belongs on the legal pages, not in mod metadata).

Reads fabric.mod.json "authors" and (neo)forge mods.toml authors= of every module, the core and the release jars given as
arguments (nested META-INF/jars too). The majority value is the reference; every other value is a finding.
    python3 tools/release/check_mod_authors.py [<jar dir> ...]      exit 1 on any finding
"""
from __future__ import annotations

import collections
import io
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOML = re.compile(r'^\s*authors\s*=\s*"([^"]*)"', re.M)


def names(value) -> tuple[str, ...]:
    if isinstance(value, str):
        return tuple(a.strip() for a in value.split(',') if a.strip())
    out = []
    for a in value or []:
        out.append(a.get('name', '') if isinstance(a, dict) else str(a))
    return tuple(out)


def from_text(name: str, text: str):
    if name.endswith('fabric.mod.json'):
        try:
            return names(json.loads(text).get('authors'))
        except ValueError:
            return None
    m = TOML.search(text)
    return names(m.group(1)) if m else None


def sources(root: Path = ROOT):
    pats = ['modules/*/*/src/main/resources/fabric.mod.json', 'modules/*/*/src/main/resources/META-INF/*mods.toml',
            'mc26_3/*/src/main/resources/fabric.mod.json', 'mc26_3/*/src/main/resources/META-INF/*mods.toml',
            'src/main/resources/fabric.mod.json']
    for pat in pats:
        for p in sorted(root.glob(pat)):
            a = from_text(p.name, p.read_text(encoding='utf-8'))
            if a is not None:
                yield str(p.relative_to(root)), a


def jar_entries(z: zipfile.ZipFile, where: str):
    for n in z.namelist():
        if n == 'fabric.mod.json' or n.endswith('mods.toml'):
            a = from_text(n, z.read(n).decode('utf-8', 'replace'))
            if a is not None:
                yield f'{where}{n}', a
        elif n.startswith('META-INF/jars/') and n.endswith('.jar'):
            yield from jar_entries(zipfile.ZipFile(io.BytesIO(z.read(n))), f'{where}{n.rsplit("/", 1)[1]}!')


def findings(entries: list[tuple[str, tuple]]) -> tuple[tuple, list[str]]:
    # jar-in-jar libraries without own metadata (Loom writes an id only) carry no authors: not a mod, skipped
    entries = [(w, a) for w, a in entries if a]
    if not entries:
        return (), []
    ref = collections.Counter(a for _, a in entries).most_common(1)[0][0]
    return ref, [f'{w}: authors differ from the other mods' for w, a in entries if a != ref]


def main(argv: list[str]) -> int:
    entries = list(sources())
    for d in argv:
        for j in sorted(Path(d).glob('*.jar')):
            with zipfile.ZipFile(j) as z:
                entries += list(jar_entries(z, f'{j.name}!'))
    ref, bad = findings(entries)
    for b in bad:
        print(b)  # the differing value itself is not printed (it may be a person's name)
    print(f'{len(entries)} metadata files, reference authors {list(ref)}, {len(bad)} findings')
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
