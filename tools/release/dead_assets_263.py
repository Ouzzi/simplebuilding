#!/usr/bin/env python3
"""simplebuilding-1 (a-w1a, PLAN40): SimpleBuilding assets that the 26.3 jar ships but nothing on 26.3 uses.

The 26.3 line reuses the shared 26.2 resources (mc26_3/resources.gradle). Content that moved into sub-mods on 26.3 (pads,
furnace carts, backpack models ...) keeps its hand-written 26.2 files in src/main/resources, so the 26.3 jar carried them
although no 26.3 item, block or code reaches them. This script finds them by reachability and writes the list that
:mc26_3:fabric excludes from its processed resources (dev runs and jar alike, so every 26.3 test sees the same tree):

    python3 tools/release/dead_assets_263.py           write mc26_3/not-shipped-on-26.3.txt
    python3 tools/release/dead_assets_263.py --check   exit 1 when a listed file is reachable again or gone (it would be
                                                       missing on 26.3); newly dead files only warn (gradle checkNotShipped263)

Roots (always live): every file of another namespace or kind than SimpleBuilding's textures/models/blockstates/items,
every module resource tree (any namespace), every string literal of the 26.3 Java sources (SB + modules), the
item/blockstate files of ids the 26.3 datagen registers (mc26_3/generated/wiki/items.json). A live JSON file makes
every file live that one of its strings names (namespace optional, with or without textures/ or the extension, also as
a path suffix for equipment/GUI sprites). Over-keeping is safe; only files nothing names are listed.
The 26.2 line keeps them (it still registers the content until the port run).
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'mc26_3' / 'not-shipped-on-26.3.txt'
NS = 'simplebuilding'
KINDS = ('textures/', 'models/', 'blockstates/', 'items/')
STR = re.compile(r'"((?:[^"\\\n]|\\.)*)"')


def tree(base):
    """Relative paths (jar layout) of every file below base."""
    if not base.is_dir():
        return {}
    return {p.relative_to(base).as_posix(): p for p in base.rglob('*') if p.is_file()}


def sb_files():
    removed = set()
    lst = ROOT / 'mc26_3/generated/removed-on-26.3.txt'
    if lst.is_file():
        removed = {l.strip() for l in lst.read_text(encoding='utf-8').splitlines() if l.strip() and not l.startswith('#')}
    files = {}
    for rel, p in tree(ROOT / 'src/main/generated').items():
        if rel not in removed:
            files[rel] = p
    for base in ('src/main/resources', 'mc26_3/overlay/resources', 'mc26_3/generated'):
        files.update(tree(ROOT / base))
    return {r: p for r, p in files.items() if not r.startswith(('wiki/', '.cache/')) and not r.endswith(('.layers.json', '.txt'))}


def module_files():
    """jar-relative path -> file of every module resource tree (all namespaces)."""
    out = {}
    for mod in sorted((ROOT / 'modules').iterdir()):
        for sub in ('shared/resources', 'generated/resources', 'fabric/src/main/resources'):
            out.update(tree(mod / sub))
    return out


def moved_copies(modules):
    """'<kind><path with extension>' of every asset a module ships under its own namespace (the moved 26.3 copy)."""
    out = set()
    for rel in modules:
        parts = rel.split('/', 2)
        if len(parts) == 3 and parts[0] == 'assets' and parts[1] != NS:
            out.add(parts[2])
    return out


def java_strings(own):
    """String literals of SimpleBuilding's 26.3 sources (own=True) or of the module sources (own=False)."""
    if own:
        roots = [ROOT / d for d in ('common/src/shared/java', 'common/src/main/java', 'common/src/jei/java', 'common/src/jade/java',
                                    'src/main/java', 'mc26_3/overlay/java', 'mc26_3/framework/java', 'framework/src/main/java')]
    else:
        roots = [m / s for m in (ROOT / 'modules').iterdir() for s in ('shared/java', 'fabric/src/main/java')]
    out = set()
    for r in roots:
        if r.is_dir():
            for p in r.rglob('*.java'):
                out.update(m.group(1) for m in STR.finditer(p.read_text(encoding='utf-8', errors='replace')) if 2 <= len(m.group(1)) <= 200)
    return out


def json_strings(path):
    try:
        text = path.read_text(encoding='utf-8')
    except (UnicodeDecodeError, OSError):
        return set()
    return {m.group(1) for m in STR.finditer(text) if 2 <= len(m.group(1)) <= 200}


def keys(s, bare):
    """Normalised reference forms of a string: path without namespace, textures/ and extension. A string with another
    namespace never names a SimpleBuilding file; one without a namespace only counts where bare is true (SimpleBuilding's
    own files and code, where an id may be built with the mod id; in a module file it means minecraft or the module)."""
    s = s.strip()
    if ':' in s:
        ns, s = s.split(':', 1)
        if ns != NS:
            return set()
    elif not bare:
        return set()
    s = s.lstrip('/')
    if s.startswith('assets/'):
        s = s.split('/', 2)[-1] if s.count('/') >= 2 else s
    for pre in KINDS:
        if s.startswith(pre):
            s = s[len(pre):]
    for ext in ('.png.mcmeta', '.png', '.json'):
        if s.endswith(ext):
            s = s[:-len(ext)]
    return {s} if s and re.fullmatch(r'[a-z0-9_./-]+', s) else set()


def candidate_key(rel):
    """(kind, path below kind without extension) of a SimpleBuilding asset candidate, else None."""
    pre = f'assets/{NS}/'
    if not rel.startswith(pre):
        return None
    rest = rel[len(pre):]
    for kind in KINDS:
        if rest.startswith(kind):
            path = rest[len(kind):]
            for ext in ('.png.mcmeta', '.png', '.json'):
                if path.endswith(ext):
                    return kind, path[:-len(ext)]
    return None


def registered():
    data = json.loads((ROOT / 'mc26_3/generated/wiki/items.json').read_text(encoding='utf-8'))
    ids = {e['id'] if isinstance(e, dict) else e for e in data['items']} | {e['id'] if isinstance(e, dict) else e for e in data['blocks']}
    return {i.split(':', 1)[1] for i in ids if i.startswith(NS + ':')}


def dead():
    files = sb_files()
    regs = registered()
    modules = module_files()
    moved = moved_copies(modules)
    cand = {rel: candidate_key(rel) for rel in files}
    # Only moved content: a module ships the same asset path under its own namespace (SB's file is the 26.2 original).
    cand = {rel: k for rel, k in cand.items() if k and rel.split('/', 2)[2] in moved}
    # Index: reference key -> candidate files (exact path and every path suffix after a '/').
    index = {}
    for rel, (kind, path) in cand.items():
        parts = path.split('/')
        for i in range(len(parts)):
            index.setdefault('/'.join(parts[i:]), set()).add(rel)
    live = set()
    todo = []

    def mark(strings, bare):
        for s in strings:
            for k in keys(s, bare):
                for rel in index.get(k, ()):
                    # A plain name ("elytra_pad", "simplebuilding:elytra_pad": no '/' in the path) is a registry id, a GUI
                    # sprite, an equipment/palette/particle/effect texture - never a block or item texture or a model
                    # (those are always named "block/..." or "item/...").
                    if '/' not in k and (cand[rel][0] == 'models/' or cand[rel][1].startswith(('block/', 'item/'))):
                        continue
                    if rel not in live:
                        # items/ and blockstates/ are roots by id only: a texture named like a moved item does not revive them.
                        if cand[rel][0] in ('items/', 'blockstates/') and cand[rel][1] not in regs:
                            continue
                        live.add(rel)
                        todo.append(rel)

    for rel, (kind, path) in cand.items():
        if kind in ('items/', 'blockstates/') and path in regs:
            live.add(rel)
            todo.append(rel)
    mark(java_strings(True), True)
    mark(java_strings(False), False)
    for rel, p in files.items():
        if rel not in cand and p.suffix == '.json':
            mark(json_strings(p), True)
    for p in modules.values():
        if p.suffix == '.json':
            mark(json_strings(p), False)
    while todo:
        rel = todo.pop()
        p = files[rel]
        if p.suffix == '.json':
            mark(json_strings(p), True)
    # Ids SimpleBuilding builds at run time ("placed_" + item, "chess/" + material ...): a moved file whose name such a
    # fragment of SB's own code starts or ends stays, even when no literal names it in full (the 26.3 placed-template
    # renderer still asks for simplebuilding:placed_<template>).
    generic = {'block/', 'item/', 'entity/', 'gui/', 'sprites/', 'textures/', 'models/', 'container/', 'simplebuilding:'}
    frags = [f.split(':')[-1].removeprefix('textures/') for f in java_strings(True) if re.fullmatch(r'[a-z0-9_/:]{4,}', f)]
    frags = [f for f in frags if f not in generic and len(f) >= 4 and (f.endswith(('_', '/')) or f.startswith('_'))]
    for rel, (kind, path) in cand.items():
        if rel in live:
            continue
        stem = path.rsplit('/', 1)[-1]
        if any((f.endswith('_') and stem.startswith(f.split('/')[-1]) and len(f.split('/')[-1]) >= 4)
               or (f.startswith('_') and stem.endswith(f))
               or (f.endswith('/') and path.startswith(f)) for f in frags):
            live.add(rel)
    # A live texture keeps its animation file and vice versa.
    for rel in list(cand):
        if rel.endswith('.png.mcmeta') and rel[:-len('.mcmeta')] in live:
            live.add(rel)
    return sorted(rel for rel in cand if rel not in live)


def render(items):
    head = ['# SimpleBuilding files the 26.3 line does not ship (moved content, no 26.3 item, block, model or code names them).',
            '# Written by tools/release/dead_assets_263.py - do not edit; :mc26_3:fabric processResources excludes these paths.',
            '# The 26.2 line still uses them until the port run.']
    return '\n'.join(head + items) + '\n'


def main(argv):
    items = dead()
    text = render(items)
    if '--check' in argv:
        current = OUT.read_text(encoding='utf-8') if OUT.is_file() else ''
        old = {l.strip() for l in current.splitlines() if l.strip() and not l.startswith('#')}
        new = set(items)
        if old - new:
            # Excluded but now named by 26.3 content (or the file is gone): the 26.3 jar would miss it.
            print(f'{OUT.relative_to(ROOT)}: listed files are used on 26.3 again or gone: {sorted(old - new)[:10]}; '
                  'run python3 tools/release/dead_assets_263.py')
            return 1
        if new - old:
            print(f'warning: {len(new - old)} more SimpleBuilding files are unused on 26.3 (still shipped): {sorted(new - old)[:5]}; '
                  'run python3 tools/release/dead_assets_263.py to drop them from the jar')
        print(f'ok: {len(old)} SimpleBuilding files not shipped on 26.3')
        return 0
    OUT.write_text(text, encoding='utf-8')
    print(f'{OUT.relative_to(ROOT)}: {len(items)} files')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
