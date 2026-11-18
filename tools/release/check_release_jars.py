#!/usr/bin/env python3
"""N95: release jars carry no test code. Scans every *.jar in <dir> (and the jars nested in META-INF/jars/).

    python3 tools/release/check_release_jars.py <dir> [<dir> ...]     exit 1 on any finding

Findings (same rule as gradle/release-strip.gradle, which strips them with -PsbRelease):
- test class: a com/** class in a package segment test/tests/testing/gametest or whose name ends in Test, Tests or GameTest*
  (ModuleGameTest, ModuleNeoTests, ModuleForgeTests ...); SimpleLib's IdMovesTests is allowed (runtime, SimpleLib.init)
- *.layers.json (texture editor sources)
- a "fabric-gametest" entrypoint in fabric.mod.json
"""
from __future__ import annotations

import io
import json
import re
import sys
import zipfile
from pathlib import Path

TEST_DIR = re.compile(r'/(test|tests|testing|gametest)/')
TEST_NAME = re.compile(r'(Tests?|GameTest[A-Za-z]*)$')
ALLOWED = ('com/simplelib/test/IdMovesTests',)


def test_class(name: str) -> bool:
    if not (name.startswith('com/') and name.endswith('.class')) or name.startswith(ALLOWED):
        return False
    simple = name[:-6].rsplit('/', 1)[-1].split('$')[0]
    return bool(TEST_DIR.search(name) or TEST_NAME.search(simple))


def scan(z: zipfile.ZipFile, where: str = '') -> list[str]:
    """Findings of one jar (and its nested jars) as 'kind: path'."""
    out = []
    for name in z.namelist():
        if name.endswith('/'):
            continue
        if test_class(name):
            out.append(f'test class: {where}{name}')
        elif name.endswith('.layers.json'):
            out.append(f'layers.json: {where}{name}')
        elif name == 'fabric.mod.json':
            try:
                entry = json.loads(z.read(name)).get('entrypoints') or {}
            except ValueError:
                out.append(f'fabric.mod.json unreadable: {where}{name}')
                continue
            if 'fabric-gametest' in entry:
                out.append(f'fabric-gametest entrypoint: {where}{name} {entry["fabric-gametest"]}')
        elif name.startswith('META-INF/jars/') and name.endswith('.jar'):
            out += scan(zipfile.ZipFile(io.BytesIO(z.read(name))), f'{where}{name.rsplit("/", 1)[1]}!')
    return out


def main(argv: list[str]) -> int:
    if not argv:
        print(__doc__)
        return 2
    jars = sorted(j for d in argv for j in Path(d).glob('*.jar'))
    if not jars:
        print(f'no jars in {argv}')
        return 2
    total = 0
    for jar in jars:
        with zipfile.ZipFile(jar) as z:
            found = scan(z)
        total += len(found)
        print(f'{jar.name}: {len(found)}' + ''.join(f'\n  {f}' for f in found[:20]) + (f'\n  ... {len(found) - 20} more' if len(found) > 20 else ''))
    print(f'{len(jars)} jars, {total} findings')
    return 1 if total else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
