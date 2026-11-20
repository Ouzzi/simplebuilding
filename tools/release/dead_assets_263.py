#!/usr/bin/env python3
"""simplebuilding-1 (a-w1a, PLAN40): SimpleBuilding assets that the 26.3 jar ships but nothing on 26.3 uses.

The 26.3 line reuses the shared 26.2 resources (mc26_3/resources.gradle). Content that moved into sub-mods on 26.3 (pads,
furnace carts, backpack models ...) keeps its hand-written 26.2 files in src/main/resources, so the 26.3 jar carried them
although no 26.3 item, block or code reaches them. This script finds them by reachability and writes the list that
:mc26_3:fabric excludes from its processed resources (dev runs and jar alike, so every 26.3 test sees the same tree):

    python3 tools/release/dead_assets_263.py           write mc26_3/not-shipped-on-26.3.txt
    python3 tools/release/dead_assets_263.py --check   exit 1 when a listed file is reachable again or gone (it would be
                                                       missing on 26.3) or when a file died without being listed (it would
                                                       ship unused; gradle checkNotShipped263)

Roots (always live): every file of another namespace or kind than SimpleBuilding's textures/models/blockstates/items,
every module resource tree (any namespace), every string literal of the 26.3 Java sources (SB + modules), the
item/blockstate files of ids the 26.3 datagen registers (mc26_3/generated/wiki/items.json). A live JSON file makes
every file live that one of its strings names (namespace optional, with or without textures/ or the extension, also as
a path suffix for equipment/GUI sprites). Over-keeping is safe; only files nothing names are listed.
Candidates (N114, release audit item 9): moved content as above, every texture of the 26.3-only overlay
(mc26_3/overlay/resources), and FORCED (files only name heuristics keep alive, each with its reason).
The 26.2 line keeps them (it still registers the content until the port run).
N117: tools/release/check_release_assets.py proves on the built release jar set that nothing they reference is missing.
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


# N114 (release audit N110, item 9): files only name heuristics keep alive (a bare id or a run-time path prefix of
# 26.2 code that 26.3 never reaches). Always listed; --check fails when one is gone (then drop it here).
FORCED = {
    'assets/simplebuilding/textures/item/netherite_chest.png':
        'no model names it on any line (the 26.2 item is a chest special model); 26.3 has no SB netherite chest',
    **{f'assets/simplebuilding/textures/gui/container/backpack/{tier}.png':
        '26.2 BackpackScreen background; on 26.3 the SB backpack menu opens only for SB backpack items (not registered, '
        'Simple Storage ships its own screen) and draws the panel style instead'
       for tier in ('basic', 'reinforced', 'netherite', 'enderite')},
}


# N115/N116: name fragments of SimpleBuilding's code ("netherite_", "_chisel", "octant_", "_dyed" ...) are registry ids,
# recipe ids, tag/cooldown checks and 26.2 datagen names (ModModelProvider, whose 26.3 output is the generated JSON this
# script already reads) - never a texture path the 26.3 client asks for. So they never keep these textures; only a real
# reference does (a live model/item definition/atlas naming the path, a full path literal in 26.3 code):
#  * every item texture (textures/item/<name>): items are drawn through item definitions and models, which the
#    reference walk follows. N115 did this for the storage family; N116 checked the other 75 moved item textures
#    (wands, chisels, sledgehammers, cores, octants, Enderite tools/materials, trim templates, gadget dials ...) one
#    by one and dropped the fragment rule for all of them. Item textures that 26.3 code builds from a prefix at run
#    time are listed in RUNTIME_ITEM_TEXTURES (kept while the prefix literal is still in the 26.3 code).
#  * the block textures of the moved storage blocks (placed bundles, placed backpacks of every tier, Simple Storage on
#    26.3, STORAGE_IN_SB = false): their models/blockstates are removed on 26.3, nothing else names them (N116).
# Other block textures and models still fall back to fragments (the placed-template renderer builds
# simplebuilding:placed_<item> at run time; open item: check them one by one like the items).
RUNTIME_ITEM_TEXTURES = {
    'guide_book_': 'GuideTooltip draws simplebuilding:textures/item/guide_book_<book>.png for every inserted chapter '
                   '(MEGA_GUIDES on 26.3)',
}
STORAGE_BLOCK = re.compile(r'(?:(?:[a-z]+_)?backpack|placed_(?:[a-z]+_)?bundle)(?:_(?:front|back|side|top))?(?:_dyed(?:_overlay)?)?')
STORAGE_BLOCK_BASE = re.compile(r'(?:_(?:front|back|side|top))?(?:_dyed(?:_overlay)?)?$')


def fragment_proof(kind, path, regs, code):
    """True when name fragments of SB's code must not keep this candidate (see above); code = SB 26.3 string literals."""
    if kind != 'textures/' or path.count('/') != 1:
        return False
    sub, name = path.split('/')
    if sub == 'item':
        return not any(name.startswith(pre) and pre in code for pre in RUNTIME_ITEM_TEXTURES)
    return sub == 'block' and bool(STORAGE_BLOCK.fullmatch(name)) and STORAGE_BLOCK_BASE.sub('', name) not in regs


def overlay_textures():
    """jar paths of the textures in the 26.3-only overlay (a dead one there is deleted, not excluded)."""
    return {rel for rel in tree(ROOT / 'mc26_3/overlay/resources') if rel.startswith(f'assets/{NS}/textures/')}


def registered():
    data = json.loads((ROOT / 'mc26_3/generated/wiki/items.json').read_text(encoding='utf-8'))
    ids = {e['id'] if isinstance(e, dict) else e for e in data['items']} | {e['id'] if isinstance(e, dict) else e for e in data['blocks']}
    return {i.split(':', 1)[1] for i in ids if i.startswith(NS + ':')}


def dead():
    files = sb_files()
    regs = registered()
    modules = module_files()
    moved = moved_copies(modules)
    overlay = overlay_textures()
    cand = {rel: candidate_key(rel) for rel in files}
    # Moved content (a module ships the same asset path under its own namespace; SB's file is the 26.2 original) and
    # every texture of the 26.3-only overlay (nothing else could need it). Forced entries are listed in any case.
    cand = {rel: k for rel, k in cand.items() if k and (rel.split('/', 2)[2] in moved or rel in overlay) and rel not in FORCED}
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
    own_code = java_strings(True)
    mark(own_code, True)
    mark(java_strings(False), False)
    for rel, p in files.items():
        if rel not in cand and p.suffix == '.json':
            mark(json_strings(p), True)
    for p in modules.values():
        if p.suffix == '.json':
            mark(json_strings(p), False)
    followed = set()

    def follow():
        while todo:
            rel = todo.pop()
            if rel not in followed and files[rel].suffix == '.json':
                followed.add(rel)
                mark(json_strings(files[rel]), True)

    follow()
    # Ids SimpleBuilding builds at run time ("placed_" + item, "chess/" + material ...): a moved file whose name such a
    # fragment of SB's own code starts or ends stays, even when no literal names it in full (the 26.3 placed-template
    # renderer still asks for simplebuilding:placed_<template>).
    generic = {'block/', 'item/', 'entity/', 'gui/', 'sprites/', 'textures/', 'models/', 'container/', 'simplebuilding:'}
    frags = [f.split(':')[-1].removeprefix('textures/') for f in own_code if re.fullmatch(r'[a-z0-9_/:]{4,}', f)]
    frags = [f for f in frags if f not in generic and len(f) >= 4 and (f.endswith(('_', '/')) or f.startswith('_'))]
    for rel, (kind, path) in cand.items():
        if rel in live or fragment_proof(kind, path, regs, own_code):
            continue
        stem = path.rsplit('/', 1)[-1]
        if any((f.endswith('_') and stem.startswith(f.split('/')[-1]) and len(f.split('/')[-1]) >= 4)
               or (f.startswith('_') and stem.endswith(f) and rel not in overlay)
               or (f.endswith('/') and path.startswith(f)) for f in frags):
            live.add(rel)
    # 26.3 overlay textures: a suffix fragment ("_active") is appended to a name the code already has, so it keeps a file
    # only while the file without that suffix is live too (astral_redstone_active died with astral_redstone, N114).
    by_path = {(kind, path): rel for rel, (kind, path) in cand.items()}
    grew = True
    while grew:  # until nothing changes: a base may itself live only through a suffix (trapped_copper_oxidized_left)
        grew = False
        for rel, (kind, path) in cand.items():
            if rel in live or rel not in overlay or fragment_proof(kind, path, regs, own_code):
                continue
            for f in frags:
                if f.startswith('_') and path.endswith(f):
                    base = by_path.get((kind, path[:-len(f)]))
                    if base is None or base in live:
                        live.add(rel)
                        grew = True
                        break
    # N117: a file the fragment rules keep is live like any other, so the files its JSON names are live too (a model
    # kept by a name fragment named an item texture that was listed: simplebuilding:item/enderite_spear).
    todo.extend(sorted(live - followed))
    follow()
    # A live texture keeps its animation file and vice versa.
    for rel in list(cand):
        if rel.endswith('.png.mcmeta') and rel[:-len('.mcmeta')] in live:
            live.add(rel)
    return sorted({rel for rel in cand if rel not in live} | set(FORCED))


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
        gone = sorted(set(FORCED) - set(sb_files()))
        if gone:
            print(f'tools/release/dead_assets_263.py: FORCED names files that no longer exist: {gone}; drop them from FORCED')
            return 1
        if old - new:
            # Excluded but now named by 26.3 content (or the file is gone): the 26.3 jar would miss it.
            print(f'{OUT.relative_to(ROOT)}: listed files are used on 26.3 again or gone: {sorted(old - new)[:10]}; '
                  'run python3 tools/release/dead_assets_263.py')
            return 1
        if new - old:
            # N114: a newly dead file would ship unused in the release jar - fail, so the list is refreshed with the change.
            print(f'{OUT.relative_to(ROOT)}: {len(new - old)} more SimpleBuilding files are unused on 26.3 (still shipped): '
                  f'{sorted(new - old)[:5]}; run python3 tools/release/dead_assets_263.py to drop them from the jar '
                  '(or delete them when they are 26.3-only overlay files)')
            return 1
        print(f'ok: {len(old)} SimpleBuilding files not shipped on 26.3')
        return 0
    OUT.write_text(text, encoding='utf-8')
    print(f'{OUT.relative_to(ROOT)}: {len(items)} files')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
