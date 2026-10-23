"""Usage: python3 tools/textures/placed_display_3d_2026_10_16.py [--check]

Step 3 of docs/ai/VORSCHLAG-ITEM-DISPLAY-3D-2026-10-15.md (owner answers D1-D10): placed smithing templates, pottery
sherds and music discs as small block models instead of the punched-out item plate.

Writes (26.3 overlay, no pixels, only geometry that points at the existing item sprites):
  simplebuilding  mc26_3/overlay/resources/assets/simplebuilding/{models/block,items}/
      placed_slab_<shape>.json         parent geometry of one silhouette (cuboids per run of equal sprite rows)
      placed_disc_round.json           parent geometry of the round disc (12 px, strips of one pixel row)
      placed_<item path>.json          child (3 lines: parent + sprite), one per template / sherd / disc
      items/placed_<item path>.json    item definition the renderer puts on a stand-in stack (ITEM_MODEL)
  simpletrims     modules/simpletrims/shared/resources/assets/simpletrims/{models/block,items}/ (trim templates only,
      no dependency on SimpleBuilding: own parents, own children)
and, built from the client jar at BUILD time only (tools/textures/vanilla_derived.py, never committed):
  block/placed_disc_<disc>.png  16x16 top view of the disc (12 px circle at 2,2): colors from the item sprite

Geometry rules (owner: no surface may fight another one, "kein Z-Fighting, keine koplanaren Flaechen"):
  - a model face exists only where it is visible: no back face (the part sits on its support), and a side face of a
    cuboid is dropped (or the cuboid is split) wherever a neighboring cuboid of the same part touches it;
  - every cuboid of a part lies in one thickness layer, so no two faces of the same orientation share a plane over
    an overlapping area. The Java test PlacedDisplay3dTests checks that on the shipped JSON.
Slab thickness: 16/7 model px, which is exactly 2 px at the 14/16 scale of a single part and about 1.14 px at the 8/16
scale of a part in a pile. The disc is 1 px thick (approved in step 2); a stack adds 1 px per disc.
"""
from __future__ import annotations

import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
SB_ASSETS = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
ST_ASSETS = os.path.join(ROOT, 'modules', 'simpletrims', 'shared', 'resources', 'assets', 'simpletrims')
LIB_ASSETS = os.path.join(ROOT, 'modules', 'simplelib', 'shared', 'resources', 'assets', 'simplelib')

THICK = 16.0 / 7.0          # slab thickness in model px (Java: PlacedSolids.MODEL_THICKNESS)
DISC_H = 1.0                # disc thickness in px (Java: PlacedSolids.DISC_HEIGHT)
DISC_ROWS = [4, 8, 10, 10, 12, 12, 12, 12, 10, 10, 8, 4]   # width of each pixel row of the 12 px disc (Java: DISC_ROWS)

# Opaque pixels per sprite row (Bit 15 = left pixel), the same masks PlacedPlate carries for Vanilla.
MASK_TRIM = '038007e007f80ffe0fff1fff1fff3ffe3ffe7ffc7ffc7ff87ff83ff00ff003e0'
MASK_COAST = '038c07fe07ff0fff0fff1fff1fff3fff3fff7ffe7ffcfff8fff8fff06ff003e0'
MASK_WILD = '038007e007f80ffe0fff1fff1fff3fff3fff7ffe7ffc7ff87ff87ff02ff003e0'
MASK_NETHERITE = '00000ff81ffc1ffc1ffc1ffc1ffc1ffc1ffc1ffc1ffc1ffc1ffc0ff807c00000'
MASK_SHERD = '000001fc03fc3ffc3ffc3ffc3ffc3ffc3ffc3ffe0ffe1ffe3ffe7ff87fc00000'

TRIMS = ['bolt', 'coast', 'dune', 'eye', 'flow', 'host', 'raiser', 'rib', 'sentry', 'shaper', 'silence', 'snout', 'spire',
         'tide', 'vex', 'ward', 'wayfinder', 'wild']
SHERDS = ['angler', 'archer', 'arms_up', 'blade', 'brewer', 'burn', 'danger', 'explorer', 'flow', 'friend', 'guster', 'heart',
          'heartbreak', 'howl', 'miner', 'mourner', 'plenty', 'prize', 'scrape', 'sheaf', 'shelter', 'skull', 'snort']
VANILLA_DISCS = ['11', '13', '5', 'blocks', 'bounce', 'cat', 'chirp', 'creator', 'creator_music_box', 'far', 'lava_chicken', 'mall',
                 'mellohi', 'otherside', 'pigstep', 'precipice', 'relic', 'stal', 'strad', 'tears', 'wait', 'ward']
SONGS = ['voidline', 'driftwood', 'daybreak', 'brimstone']
TRACKS = ['', '_b_side', '_track_3', '_track_4']
MOD_DISCS = [song + track for song in SONGS for track in TRACKS]
# Simple Music (MU21): top views are committed (own pixels, modules/simplemusic/tools/gen_textures.py), not derived
MUSIC_DISCS = ['blank_disc', 'custom_disc'] + ['music_disc_' + d for d in (
    'ember_forge', 'obsidian_throne', 'end_departure', 'fortress_march', 'deep_pulse', 'sky_forge')]   # Simple Music (MU21, W24-3)


def mask_rows(hexmask: str) -> list[list[bool]]:
    return [[bool(int(hexmask[v * 4:v * 4 + 4], 16) & (1 << (15 - u))) for u in range(16)] for v in range(16)]


def png_mask(path: str) -> list[list[bool]]:
    from PIL import Image
    im = Image.open(path).convert('RGBA')
    w = im.width
    h = min(im.height, w)    # animated sheets: first frame
    return [[any(im.getpixel((x, y))[3] >= 26 for y in range(v * h // 16, max((v + 1) * h // 16, v * h // 16 + 1))
                 for x in range(u * w // 16, max((u + 1) * w // 16, u * w // 16 + 1))) for u in range(16)] for v in range(16)]


def mask_hex(mask: list[list[bool]]) -> str:
    return ''.join('%04x' % sum(1 << (15 - u) for u in range(16) if row[u]) for row in mask)


def disc_mask() -> list[list[bool]]:
    """16 x 16 sprite space, the 12 px circle at (2, 2); must match DISC_ROWS (checked)."""
    m = [[False] * 16 for _ in range(16)]
    for y in range(12):
        for x in range(12):
            dx, dy = x + 0.5 - 6, y + 0.5 - 6
            if dx * dx + dy * dy <= 36:
                m[y + 2][x + 2] = True
    widths = [sum(m[y + 2]) for y in range(12)]
    assert widths == DISC_ROWS, widths
    return m


def pieces(mask: list[list[bool]], merge_rows: bool) -> list[dict]:
    """Cuboids that tile the mask: runs of equal rows (merge_rows) or single rows, each run split at the x where a
    neighbor above or below starts or ends, so every side face of a piece is either fully exposed or fully hidden."""
    H, W = len(mask), len(mask[0])
    rows = [tuple(r) for r in mask]
    out = []
    v = 0
    while v < H:
        end = v + 1
        while merge_rows and end < H and rows[end] == rows[v]:
            end += 1
        row = rows[v]
        above = rows[v - 1] if v > 0 else (False,) * W
        below = rows[end] if end < H else (False,) * W
        u = 0
        while u < W:
            if not row[u]:
                u += 1
                continue
            u1 = u
            while u1 < W and row[u1]:
                u1 += 1
            cuts = [u] + [x for x in range(u + 1, u1) if above[x] != above[x - 1] or below[x] != below[x - 1]] + [u1]
            for a, b in zip(cuts, cuts[1:]):
                out.append({'u0': a, 'u1': b, 'v0': v, 'v1': end, 'left': a == u, 'right': b == u1,
                            'top': not above[a], 'bottom': not below[a]})
            u = u1
        v = end
    return out


def fmt(x: float):
    x = round(float(x), 4)
    return int(x) if x == int(x) else x


def slab_parent(mask: list[list[bool]], what: str) -> dict:
    """Silhouette extrusion in item/generated space: x = sprite column, y = 16 - row, z centered at 8, front = +Z."""
    z0, z1 = 8 - THICK / 2, 8 + THICK / 2
    e = 0.05
    elements = []
    for p in pieces(mask, merge_rows=True):
        u0, u1, v0, v1 = p['u0'], p['u1'], p['v0'], p['v1']
        faces = {'south': {'uv': [u0, v0, u1, v1], 'texture': '#item'}}
        if p['left']:
            faces['west'] = {'uv': [u0 + 0.5 - e, v0, u0 + 0.5 + e, v1], 'texture': '#item'}
        if p['right']:
            faces['east'] = {'uv': [u1 - 0.5 - e, v0, u1 - 0.5 + e, v1], 'texture': '#item'}
        if p['top']:
            faces['up'] = {'uv': [u0, v0 + 0.5 - e, u1, v0 + 0.5 + e], 'texture': '#item'}
        if p['bottom']:
            faces['down'] = {'uv': [u0, v1 - 0.5 - e, u1, v1 - 0.5 + e], 'texture': '#item'}
        elements.append({'from': [u0, fmt(16 - v1), fmt(z0)], 'to': [u1, fmt(16 - v0), fmt(z1)], 'faces': faces})
    return {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'elements': elements}


def disc_parent() -> dict:
    """Round disc in block space: x/z = texture pixel (disc at 2..14), y = 0..1, front = up. One piece row per strip so the
    side faces sample one border pixel each; no bottom face (the disc rests on its support or on the disc below)."""
    e = 0.05
    elements = []
    for p in pieces(disc_mask(), merge_rows=False):
        u0, u1, v0, v1 = p['u0'], p['u1'], p['v0'], p['v1']
        faces = {'up': {'uv': [u0, v0, u1, v1], 'texture': '#disc'}}
        if p['left']:
            faces['west'] = {'uv': [u0 + 0.5 - e, v0 + 0.5 - e, u0 + 0.5 + e, v0 + 0.5 + e], 'texture': '#disc'}
        if p['right']:
            faces['east'] = {'uv': [u1 - 0.5 - e, v0 + 0.5 - e, u1 - 0.5 + e, v0 + 0.5 + e], 'texture': '#disc'}
        if p['top']:   # north face: u runs from +x to -x
            faces['north'] = {'uv': [u1, v0 + 0.5 - e, u0, v0 + 0.5 + e], 'texture': '#disc'}
        if p['bottom']:
            faces['south'] = {'uv': [u0, v0 + 0.5 - e, u1, v0 + 0.5 + e], 'texture': '#disc'}
        elements.append({'from': [u0, 0, v0], 'to': [u1, fmt(DISC_H), v1], 'faces': faces})
    return {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'elements': elements}


def child(parent: str, textures: dict) -> dict:
    return {'parent': parent, 'textures': textures}


def item_def(model: str) -> dict:
    return {'model': {'type': 'minecraft:model', 'model': model}}


def sb_files() -> dict[str, dict]:
    """relative path (under SB_ASSETS) -> JSON for SimpleBuilding."""
    masks = {'trim': MASK_TRIM, 'coast': MASK_COAST, 'wild': MASK_WILD, 'netherite': MASK_NETHERITE, 'sherd': MASK_SHERD}
    files: dict[str, dict] = {}
    parents = {name: mask_rows(h) for name, h in masks.items()}
    # The upgrade templates of the framework mod (simplelib:*_upgrade_template): silhouette from their own textures.
    own = {}
    for item in ('enderite_upgrade_template', 'basic_upgrade_template'):
        m = png_mask(os.path.join(LIB_ASSETS, 'textures', 'item', item + '.png'))
        for name, pm in parents.items():
            if pm == m:
                own[item] = name
                break
        else:
            parents[item] = m
            own[item] = item
    for name, mask in parents.items():
        files[f'models/block/placed_slab_{name}.json'] = slab_parent(mask, name)
    files['models/block/placed_disc_round.json'] = disc_parent()

    def slab(path: str, shape: str, sprite: str):
        files[f'models/block/placed_{path}.json'] = child(f'simplebuilding:block/placed_slab_{shape}',
                                                          {'item': sprite, 'particle': sprite})
        files[f'items/placed_{path}.json'] = item_def(f'simplebuilding:block/placed_{path}')

    for t in TRIMS:
        slab(f'{t}_armor_trim_smithing_template', t if t in ('coast', 'wild') else 'trim',
             f'minecraft:item/{t}_armor_trim_smithing_template')
    slab('netherite_upgrade_smithing_template', 'netherite', 'minecraft:item/netherite_upgrade_smithing_template')
    for item, shape in own.items():
        slab(item, shape, f'simplelib:item/{item}')
    for s in SHERDS:
        slab(f'{s}_pottery_sherd', 'sherd', f'minecraft:item/{s}_pottery_sherd')
    for d in VANILLA_DISCS + MOD_DISCS:
        item = f'music_disc_{d}'
        tex = f'simplebuilding:block/placed_disc_{item}'
        files[f'models/block/placed_{item}.json'] = child('simplebuilding:block/placed_disc_round', {'disc': tex, 'particle': tex})
        files[f'items/placed_{item}.json'] = item_def(f'simplebuilding:block/placed_{item}')
    for item in MUSIC_DISCS:
        tex = f'simplebuilding:block/placed_disc_{item}'
        files[f'models/block/placed_{item}.json'] = child('simplebuilding:block/placed_disc_round', {'disc': tex, 'particle': tex})
        files[f'items/placed_{item}.json'] = item_def(f'simplebuilding:block/placed_{item}')
    return files


def st_files() -> dict[str, dict]:
    """relative path (under ST_ASSETS) -> JSON for the Simple Trims module (own copy, no SimpleBuilding reference)."""
    masks = {'trim': MASK_TRIM, 'coast': MASK_COAST, 'wild': MASK_WILD}
    parents = {name: mask_rows(h) for name, h in masks.items()}
    own = {}
    for item in ('glowing_trim_template', 'emitting_trim_template', 'pulsating_trim_template'):
        m = png_mask(os.path.join(ST_ASSETS, 'textures', 'item', item + '.png'))
        for name, pm in parents.items():
            if pm == m:
                own[item] = name
                break
        else:
            parents[item] = m
            own[item] = item
    files: dict[str, dict] = {}
    for name, mask in parents.items():
        files[f'models/block/placed_slab_{name}.json'] = slab_parent(mask, name)

    def slab(path: str, shape: str, sprite: str):
        files[f'models/block/placed_{path}.json'] = child(f'simpletrims:block/placed_slab_{shape}', {'item': sprite, 'particle': sprite})
        files[f'items/placed_{path}.json'] = item_def(f'simpletrims:block/placed_{path}')

    for t in TRIMS:
        slab(f'{t}_armor_trim_smithing_template', t if t in ('coast', 'wild') else 'trim',
             f'minecraft:item/{t}_armor_trim_smithing_template')
    for item, shape in own.items():
        slab(item, shape, f'simpletrims:item/{item}')
    return files


def dump(data: dict) -> str:
    return json.dumps(data, indent=2) + '\n'


# ---------------------------------------------------------------------------------------------------------------
# Vanilla-derived top views (build time only)
# ---------------------------------------------------------------------------------------------------------------
def disc_top(sprite):
    """16x16 top view of a record from its 16x16 item sprite (a 15 x 10 oval seen from an angle): the 12 px circle
    samples the sprite through the inverse squash, so rim, grooves and label keep their colors and place."""
    from PIL import Image
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = sprite.load()
    opaque = [(x, y) for y in range(16) for x in range(16) if px[x, y][3] >= 26]
    for y in range(12):
        for x in range(12):
            dx, dy = x + 0.5 - 6, y + 0.5 - 6
            if dx * dx + dy * dy > 36:
                continue
            sx, sy = 7.5 + dx * 1.25, 8.0 + dy * (5.0 / 6.0)
            ix, iy = int(sx), int(sy)
            if not (0 <= ix < 16 and 0 <= iy < 16) or px[ix, iy][3] < 26:
                ix, iy = min(opaque, key=lambda p: (p[0] + 0.5 - sx) ** 2 + (p[1] + 0.5 - sy) ** 2)
            out.putpixel((x + 2, y + 2), px[ix, iy][:3] + (255,))
    return out


def derived(jar):
    """Vanilla-derived output for tools/textures/vanilla_derived.py: texture path -> image."""
    import io
    import zipfile
    from PIL import Image
    sys.path.insert(0, HERE)
    import music_disc_textures
    out = {}
    with zipfile.ZipFile(str(jar)) as z:
        for d in VANILLA_DISCS:
            sprite = Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/item/music_disc_{d}.png'))).convert('RGBA')
            out[f'block/placed_disc_music_disc_{d}.png'] = disc_top(sprite)
    music_disc_textures.JAR = str(jar)
    discs, _ = music_disc_textures.outputs()
    for d in MOD_DISCS:
        out[f'block/placed_disc_music_disc_{d}.png'] = disc_top(discs[f'music_disc_{d}'])
    return out


def main() -> int:
    check = '--check' in sys.argv
    bad = 0
    for base, files in ((SB_ASSETS, sb_files()), (ST_ASSETS, st_files())):
        for rel, data in files.items():
            path = os.path.join(base, rel)
            text = dump(data)
            if check:
                if not os.path.exists(path) or open(path, encoding='utf-8').read() != text:
                    print('out of date:', os.path.relpath(path, ROOT))
                    bad += 1
                continue
            os.makedirs(os.path.dirname(path), exist_ok=True)
            if not os.path.exists(path) or open(path, encoding='utf-8').read() != text:
                with open(path, 'w', encoding='utf-8', newline='\n') as f:
                    f.write(text)
        print(('checked' if check else 'wrote'), len(files), 'files under', os.path.relpath(base, ROOT))
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
