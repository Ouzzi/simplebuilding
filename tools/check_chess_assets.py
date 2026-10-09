"""Usage: python tools/check_chess_assets.py

Data check for the chess assets (queue N15, docs/ai/PLAN-SCHACH-2026-10-06.md), 26.3 overlay:
- every colour (ChessColor) x piece (ChessPiece) x {3D, flat} and every octet has an item definition, a model that
  resolves through its parents to elements, textures that exist, and an EN/DE name in both language trees;
- every piece model fits one quarter of the block (x/z 4..12 px, y 0..16, foot on y 0), so the ChessPiecesRenderer
  can put four of them on a block;
- the checker_octet blockstate has a part for every colour and every one of the eight corners, each pointing at an
  existing model, and chess_pieces has its blockstate and model.
Colours and pieces are read from the Java enums, so a new colour without assets fails here. Exit 1 on any problem.
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSET_ROOTS = [os.path.join(ROOT, p, 'assets') for p in ('mc26_3/overlay/resources', 'src/main/resources')]
LANG_TREES = ['mc26_3/overlay/resources', 'src/main/resources']
JAVA = os.path.join(ROOT, 'common/src/shared/java/com/simplebuilding/chess')


def enum_ids(name):
    with open(os.path.join(JAVA, name), encoding='utf-8') as f:
        return re.findall(r'^\s+[A-Z_]+\("([a-z_]+)"', f.read(), re.M)


def find(kind, ref, ext):
    ns, _, path = ref.partition(':') if ':' in ref else ('minecraft', None, ref)
    for root in ASSET_ROOTS:
        p = os.path.join(root, ns, kind, path + ext)
        if os.path.isfile(p):
            return p
    return None


def load(kind, ref):
    p = find(kind, ref, '.json')
    if p is None:
        return None
    with open(p, encoding='utf-8') as f:
        return json.load(f)


def resolve(ref, problems, where):
    """Elements and merged textures of a model, following parents inside the mod namespace."""
    textures, elements, seen = {}, None, set()
    while ref and ref not in seen:
        seen.add(ref)
        if not ref.startswith('simplebuilding:'):
            break
        model = load('models', ref)
        if model is None:
            problems.append(f'{where}: model {ref} missing')
            return None, {}
        for k, v in model.get('textures', {}).items():
            textures.setdefault(k, v)
        if elements is None and 'elements' in model:
            elements = model['elements']
        ref = model.get('parent')
    for k, v in textures.items():
        while isinstance(v, str) and v.startswith('#'):
            v = textures.get(v[1:])
        if v is None or (v.startswith('simplebuilding:') and find('textures', v, '.png') is None):
            problems.append(f'{where}: texture #{k} -> {textures[k]} unresolved')
    if not elements:
        problems.append(f'{where}: model {seen} has no elements')
    return elements, textures


def main():
    colors, pieces = enum_ids('ChessColor.java'), enum_ids('ChessPiece.java')
    problems = []
    if len(colors) < 13 or len(pieces) != 6:
        problems.append(f'enums not read: {colors} {pieces}')
    langs = []
    for tree in LANG_TREES:
        for code in ('en_us', 'de_de'):
            with open(os.path.join(ROOT, tree, 'assets/simplebuilding/lang', code + '.json'), encoding='utf-8') as f:
                langs.append((f'{tree} {code}', json.load(f)))
    items = [(f'{c}_octet', None) for c in colors]
    items += [(f'{c}_chess_{p}{"_flat" if flat else ""}', p) for c in colors for p in pieces for flat in (False, True)]
    for item, piece in items:
        definition = load('items', 'simplebuilding:' + item)
        if definition is None:
            problems.append(f'{item}: item definition missing')
            continue
        model = definition.get('model', {}).get('model')
        elements, _ = resolve(model, problems, item)
        if piece and elements:
            lo = [min(e['from'][i] for e in elements) for i in range(3)]
            hi = [max(e['to'][i] for e in elements) for i in range(3)]
            if lo[0] < 4 or lo[2] < 4 or hi[0] > 12 or hi[2] > 12 or lo[1] != 0 or hi[1] > 16:
                problems.append(f'{item}: model spans {lo}..{hi}, must fit 4..12 (x/z), foot on y 0')
        for name, lang in langs:
            if 'item.simplebuilding.' + item not in lang:
                problems.append(f'{item}: no name in {name}')
    states = load('blockstates', 'simplebuilding:checker_octet')
    parts = {}
    for part in (states or {}).get('multipart', []):
        when = part.get('when', {})
        corner = [k for k in when if re.fullmatch(r'o[01]{3}', k)]
        if len(corner) == 1:
            parts[(when.get('color'), corner[0])] = part['apply']['model']
    for c in colors:
        for corner in (f'o{x}{y}{z}' for x in '01' for y in '01' for z in '01'):
            model = parts.get((c, corner))
            if model is None:
                problems.append(f'checker_octet: no part for {c} {corner}')
            elif (c, corner) == (c, 'o000'):
                resolve(model, problems, f'checker_octet {c}')
    if load('blockstates', 'simplebuilding:chess_pieces') is None or load('models', 'simplebuilding:block/chess_pieces') is None:
        problems.append('chess_pieces: blockstate or block model missing')
    for p in problems:
        print('PROBLEM ' + p)
    if problems:
        sys.exit(1)
    print(f'OK: {len(items)} chess items, {len(parts)} octet parts, {len(colors)} colours x {len(pieces)} pieces')


if __name__ == '__main__':
    main()
