"""Simple Dinosaurs: textures, models, block states and item models of the fossil block, the five eggs, the large saddle and the
three T. rex armors, from ONE spec (concept docs/ai/KONZEPT-A-WP18-2026-10-29.md section 3.2/3.3). Own pixel art drawn from code.

    python3 modules/simpledinos/tools/gen_blocks.py            write
    python3 modules/simpledinos/tools/gen_blocks.py --check    fail when a file is stale

Fossil block: ten stages (the bones come out of the sediment as the progress rises). Egg: one base texture per dinosaur; the
twelve brooding steps add warm glow dots (overlay), the three crack steps add cracks (overlay). The egg model is a stack of three
boxes with automatic UVs, so the front of the 16 x 16 texture is the front of the egg.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
sys.path.insert(0, str(ROOT / 'tools/textures'))
import layers as L  # noqa: E402

NS = 'simpledinos'
ASSETS = MODULE / 'shared/resources/assets' / NS
KINDS = ('trex', 'triceratops', 'stegosaurus', 'utahraptor', 'parasaurolophus')
STAGES = 10
PHASES = 15


def H(s):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def hsh(x, y, s):
    n = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


# ---------------------------------------------------------------- fossil block
BONES = ["................",
         ".....xxxxxx.....",
         "....xxxxxxxx....",
         "....xx.xx.xx....",
         "....xxxxxxxx....",
         ".....xx..xx.....",
         "..x...xxxx...x..",
         ".xxx.xx..xx.xxx.",
         "..x.xx.xx.xx.x..",
         "....xx.xx.xx....",
         "...xx..xx..xx...",
         "...x...xx...x...",
         "......xxxx......",
         "................",
         "................",
         "................"]


def fossil_stage(stage: int):
    """Sediment (sandstone-like) with the fossil: the bones are covered at stage 0 and fully free at stage 9."""
    sed, sed_d, sed_l = H('b39c6e'), H('7a6642'), H('c9b588')
    bone, bone_d, bone_l = H('efe6cc'), H('b8aa84'), H('fbf6e4')
    pixels = [(x, y) for y, row in enumerate(BONES) for x, ch in enumerate(row) if ch == 'x']
    # reveal in a stable random order
    pixels.sort(key=lambda p: hsh(p[0], p[1], 77))
    shown = set(pixels[: round(len(pixels) * stage / (STAGES - 1))])
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = hsh(x, y, 5)
            c = sed
            if n > 0.78:
                c = sed_d
            elif n < 0.14:
                c = sed_l
            # the dust thins out around the revealed bones, so the layer lifts step by step
            if (x, y) in shown:
                c = bone if hsh(x, y, 9) > 0.18 else bone_d
                if y > 0 and (x, y - 1) not in shown and hsh(x, y, 11) > 0.4:
                    c = bone_l
            elif (x, y) in set(pixels) and stage > 0:
                c = mix(c, bone_d, 0.18 * stage / (STAGES - 1))
            px[x, y] = c + (255,)
    return img


# ---------------------------------------------------------------- eggs
EGG_STYLES = {
    'trex': (H('7d8c58'), H('3e4a28'), H('b4c088'), 'spots'),
    'triceratops': (H('b69a68'), H('6e5632'), H('dcc898'), 'stripes'),
    'stegosaurus': (H('6c7686'), H('353c4a'), H('a0aabb'), 'plates'),
    'utahraptor': (H('a8643c'), H('5a2e18'), H('dc9a68'), 'speckle'),
    'parasaurolophus': (H('4e8a84'), H('26504c'), H('8ac8c0'), 'dots'),
}


def egg_base(kind: str):
    base, dark, light, style = EGG_STYLES[kind]
    seed = 40 + KINDS.index(kind)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = hsh(x, y, seed)
            c = base
            if style == 'spots':
                if hsh(x // 2, y // 2, seed) > 0.72:
                    c = dark
            elif style == 'stripes':
                if (y // 2) % 3 == 0:
                    c = mix(base, dark, 0.55)
            elif style == 'plates':
                if (x + y) % 6 == 0 or (x - y) % 6 == 0:
                    c = mix(base, dark, 0.45)
            elif style == 'speckle':
                if n > 0.8:
                    c = dark
            elif style == 'dots':
                if hsh(x // 3, y // 3, seed) > 0.6 and (x + y) % 2 == 0:
                    c = light
            if n < 0.1:
                c = mix(c, light, 0.35)
            elif n > 0.93:
                c = mix(c, dark, 0.4)
            if x >= 12:
                c = mix(c, dark, 0.22)  # shaded side
            px[x, y] = c + (255,)
    return img


GLOW_SPOTS = sorted(((x, y) for y in range(5, 15) for x in range(3, 14)), key=lambda p: hsh(p[0], p[1], 123))
CRACK_PATH = [(8, 5), (8, 6), (7, 7), (7, 8), (8, 9), (8, 10), (9, 11), (9, 12), (10, 13), (7, 9), (6, 10), (5, 10), (4, 11),
              (9, 8), (10, 8), (11, 9), (12, 9), (8, 12), (8, 13), (7, 13)]
CRACK_COUNT = (7, 13, 20)


def glow_pixels(step: int):
    """Warm dots of brooding step 0..11 (11 = the most)."""
    n = round(len(GLOW_SPOTS) * 0.55 * step / 11)
    return GLOW_SPOTS[:n]


def egg_overlay(phase: int):
    """Overlay of a state phase: glow for 0..11, glow 11 plus cracks for 12..14."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    step = min(phase, 11)
    for i, (x, y) in enumerate(glow_pixels(step)):
        px[x, y] = (mix(H('ffb23a'), H('ffe48a'), hsh(x, y, 3)) + (255,))
    if phase >= 12:
        for i, (x, y) in enumerate(CRACK_PATH[: CRACK_COUNT[phase - 12]]):
            px[x, y] = H('241810') + (255,)
    return img


# ---------------------------------------------------------------- items
SADDLE = ["................",
          "................",
          "....oooooooo....",
          "...oSSSSSSSSo...",
          "..oSSsSSSSsSSo..",
          "..oSSSSSSSSSSo..",
          ".oBBBBBBBBBBBBo.",
          ".oBbBBBBBBBBbBo.",
          ".oBBBBBBBBBBBBo.",
          ".oBBBBBBBBBBBBo.",
          ".oiBBBBBBBBBBio.",
          ".oiiBBBBBBBBiio.",
          "..oiiooooooiio..",
          "...oo......oo...",
          "................",
          "................"]
SADDLE_COLORS = {'o': '#2a1a0e', 'S': '#8a5a34', 's': '#a8744a', 'B': '#6a4024', 'b': '#8a5a34', 'i': '#a8adb4'}

GUARD = ["................",
         "................",
         "....oooooooo....",
         "...oPPPPPPPPo...",
         "..oPPhPPPPhPPo..",
         "..oPPPPPPPPPPo..",
         "..oPPPPrPPPPPo..",
         ".oPPPPPPPPPPPPo.",
         ".oPPPPPPPPPPPPo.",
         ".oddPPPPPPPPddo.",
         "..oddddddddddo..",
         "...oooooooooo...",
         "................",
         "................",
         "................",
         "................"]
GUARD_COLORS = {
    'leather': {'o': '#2a1a0e', 'P': '#7a5030', 'h': '#9a6a40', 'r': '#c8a070', 'd': '#4e321c'},
    'iron': {'o': '#22262c', 'P': '#9aa0a8', 'h': '#c4c9d0', 'r': '#e4e8ec', 'd': '#5c6168'},
    'diamond': {'o': '#0e3c3a', 'P': '#3fd2c6', 'h': '#8ff0e6', 'r': '#e0fffb', 'd': '#1f8a82'},
}


def grid_image(rows, colors, groups, base='Grundfarbe'):
    cols = {k: H(v) for k, v in colors.items()}
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in cols:
                px[x, y] = cols[ch] + (255,)
    return L.snap_grid(img, rows, {k: v + (255,) for k, v in cols.items()}, groups, base)


# ---------------------------------------------------------------- JSON
def block_model_cube(texture: str):
    return {'parent': 'minecraft:block/cube_all', 'textures': {'all': texture}}


FACES = ('down', 'up', 'north', 'south', 'west', 'east')


def egg_parent_model():
    boxes = [([4, 0, 4], [12, 3, 12]), ([3, 3, 3], [13, 9, 13]), ([5, 9, 5], [11, 12, 11])]
    elements = []
    for tex, grow in (('#egg', 0.0), ('#overlay', 0.02)):
        for lo, hi in boxes:
            elements.append({'from': [round(v - grow, 3) for v in lo], 'to': [round(v + grow, 3) for v in hi],
                             'faces': {f: {'texture': tex} for f in FACES}})
    return {'parent': 'minecraft:block/block', 'textures': {'particle': '#egg'}, 'elements': elements}


def dumps(obj):
    return json.dumps(obj, indent=2, ensure_ascii=False) + '\n'


def outputs():
    """{path: PIL image | str} of everything this script owns."""
    files = {}
    tb, ti, mb, mi, bs, it = (ASSETS / 'textures/block', ASSETS / 'textures/item', ASSETS / 'models/block', ASSETS / 'models/item',
                              ASSETS / 'blockstates', ASSETS / 'items')
    for s in range(STAGES):
        files[tb / f'fossil_{s}.png'] = fossil_stage(s)
        files[mb / f'fossil_{s}.json'] = dumps(block_model_cube(f'{NS}:block/fossil_{s}'))
    files[bs / 'fossil_block.json'] = dumps({'variants': {f'stage={s}': {'model': f'{NS}:block/fossil_{s}'} for s in range(STAGES)}})
    files[it / 'fossil_block.json'] = dumps({'model': {'type': 'minecraft:model', 'model': f'{NS}:block/fossil_9'}})
    for kind in KINDS:
        files[tb / f'egg_{kind}.png'] = egg_base(kind)
    for p in range(PHASES):
        files[tb / f'egg_overlay_{p}.png'] = egg_overlay(p)
    files[mb / 'dino_egg.json'] = dumps(egg_parent_model())
    for kind in KINDS:
        for p in range(PHASES):
            files[mb / f'{kind}_egg_{p}.json'] = dumps({'parent': f'{NS}:block/dino_egg', 'textures': {
                'egg': f'{NS}:block/egg_{kind}', 'overlay': f'{NS}:block/egg_overlay_{p}'}})
        files[bs / f'{kind}_egg.json'] = dumps({'variants': {f'phase={p}': {'model': f'{NS}:block/{kind}_egg_{p}'} for p in range(PHASES)}})
        files[it / f'{kind}_egg.json'] = dumps({'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{kind}_egg_11'}})
    files[ti / 'large_saddle.png'] = grid_image(SADDLE, SADDLE_COLORS, [('Umriss', 'o'), ('Schnallen', 'i'), ('Licht', 'sb')], 'Leder')
    for tier, colors in GUARD_COLORS.items():
        files[ti / f'trex_armor_{tier}.png'] = grid_image(GUARD, colors, [('Umriss', 'o'), ('Kanten', 'd'), ('Licht', 'hr')], 'Platte')
    for name in ('large_saddle', 'trex_armor_leather', 'trex_armor_iron', 'trex_armor_diamond'):
        files[mi / f'{name}.json'] = dumps({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{name}'}})
        files[it / f'{name}.json'] = dumps({'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{name}'}})
    return files


def preview_images(image_uri):
    """Preview cards for the HTML page: fossil stages, egg kinds, egg brooding and crack steps, items (data URIs via image_uri(path))."""
    import tempfile
    cards = []
    with tempfile.TemporaryDirectory() as tmp:
        def uri(img, scale=6):
            p = Path(tmp) / 'x.png'
            img.save(p)
            return image_uri(p, scale)

        cards.append(dict(title='Fossilienblock: 10 Stufen beim Pinseln', text='Die Knochen kommen Stufe für Stufe aus dem Sediment (etwa 60 s Pinseln).',
                          items=[dict(src=uri(fossil_stage(s)), label=f'Stufe {s}') for s in range(STAGES)]))
        cards.append(dict(title='Die fünf Eier (Muster)', text='Je Dinosaurier ein eigenes Muster. In Stufe 1 schlüpft nur der T-Rex; die anderen vier folgen nach Abnahme.',
                          items=[dict(src=uri(egg_base(k)), label=k) for k in KINDS]))
        base = egg_base('trex')
        glow = []
        for p in (0, 3, 6, 9, 11, 12, 13, 14):
            im = base.copy()
            im.alpha_composite(egg_overlay(p))
            glow.append(dict(src=uri(im), label=('Brüten ' + str(p + 1) + '/12') if p < 12 else f'Risse {p - 11}/3'))
        cards.append(dict(title='Ei: Brütstufen und Risse (T-Rex-Ei)', text='12 sichtbare Brütstufen (warmer Glanz nimmt zu), danach drei Rissstufen bis zum Schlüpfen.', items=glow))
        gear = [dict(src=uri(grid_image(SADDLE, SADDLE_COLORS, [])), label='Großer Sattel')]
        for tier, colors in GUARD_COLORS.items():
            gear.append(dict(src=uri(grid_image(GUARD, colors, [])), label='T-Rex-Rüstung ' + tier))
        cards.append(dict(title='Sattel und Rüstungen', text='Items; am T-Rex erscheinen sie als Sattel bzw. Platten auf Kopf und Rücken.', items=gear))
    return cards


def main(argv):
    check = '--check' in argv
    stale = []
    for path, content in outputs().items():
        if isinstance(content, str):
            if not path.exists() or path.read_text(encoding='utf-8') != content:
                stale.append(path)
                if not check:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_text(content, encoding='utf-8')
        else:
            same = path.exists() and Image.open(path).convert('RGBA').tobytes() == content.convert('RGBA').tobytes()
            if not same:
                stale.append(path)
                if not check:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    L.snap(content, 'Grundform') if not L.steps_of(content) else None
                    L.save(content, path)
    if check and stale:
        print('stale: ' + ', '.join(str(p.relative_to(ROOT)) for p in stale))
        return 1
    print('simpledinos blocks/items: %d files %s' % (len(outputs()), 'current' if not stale else 'written'))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
