"""Usage: python tools/textures/placed_ingots_2026_10_09.py [client.jar] [preview.png]

Placed ingots (owner 2026-10-08, queue N24 "Barren als 3D-Modell platzierbar", docs/ai/PLAN-PLATZIEREN-N24-2026-10-09.md):
an ingot lying in a pile of small parts is a real 3D bar instead of the flat item plate - a trapezoid of two cuboids
(8 x 4 x 2 px base, 6 x 2 x 1 px top, long along x), like the bars on a vanilla gold block. Up to four make a little
stack (PlacedSmallParts.INGOT_STACK).

Colours come from the item sprite (vanilla from the client jar, the mod's own from the repo): its opaque pixels without
the outline, sorted by brightness, give five shades (dark .. light); the faces are painted with them by hand rules -
lit top with a highlight edge towards the north-west, darker sides, darkest bottom - so the bar reads as the same metal
without copying the sprite.

Writes into mc26_3/overlay/resources/assets/simplebuilding/:
  textures/block/placed_<ingot>.png    16x16 face sheet
  models/block/placed_<ingot>.json     the 3D bar
  items/placed_<ingot>.json            item model definition; the small-parts renderer draws the bar through it
                                       (ITEM_MODEL on a stand-in stack)
The cuboids must match PlacedSmallParts#INGOT_BOXES (Java); the game test placing_game_test_ingots_lie_as_bars_and_stack
checks that.
"""
import importlib.util
import io
import json
import os
import sys
import zipfile

from PIL import Image, ImageDraw

JAR = sys.argv[1] if len(sys.argv) > 1 else os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar')
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
OUT = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
MOD_TEXTURES = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'simplebuilding', 'textures')
INGOTS = {
    'copper_ingot': 'minecraft:item/copper_ingot',
    'iron_ingot': 'minecraft:item/iron_ingot',
    'gold_ingot': 'minecraft:item/gold_ingot',
    'netherite_ingot': 'minecraft:item/netherite_ingot',
    'enderite_ingot': 'simplebuilding:item/enderite_ingot',
}
# (from, to) in pixels; must match PlacedSmallParts.INGOT_BOXES = {y0, y1, half x, half z}
BASE = ([4, 0, 6], [12, 2, 10])
TOP = ([5, 2, 7], [11, 3, 9])


def sprite(jar, ref):
    ns, path = ref.split(':')
    if ns == 'minecraft':
        return Image.open(io.BytesIO(jar.read('assets/minecraft/textures/%s.png' % path))).convert('RGBA')
    return Image.open(os.path.join(MOD_TEXTURES, path + '.png')).convert('RGBA')


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def shades(im):
    """Five shades dark..light from the sprite's opaque pixels, the outline (darkest colour) left out."""
    px = [im.getpixel((x, y))[:3] for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3] > 128]
    outline = min(set(px), key=lum)
    body = sorted((c for c in px if c != outline), key=lum)   # weighted by how often a colour is used
    pick = lambda f: body[min(len(body) - 1, int(round(f * (len(body) - 1))))]
    return [pick(f) for f in (0.03, 0.25, 0.5, 0.78, 0.97)]


def texture(s):
    dark, low, mid, hi, light = s
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))

    def fill(x0, y0, x1, y1, c):
        for y in range(y0, y1):
            for x in range(x0, x1):
                im.putpixel((x, y), c + (255,))

    # base top 8x4 (0,0): only its rim shows around the top cuboid - lit edge north/west
    fill(0, 0, 8, 4, mid)
    fill(0, 0, 8, 1, hi)
    fill(0, 0, 1, 4, hi)
    # top cuboid's top 6x2 (0,4): the brightest face, a highlight streak along the north edge
    fill(0, 4, 6, 6, hi)
    fill(1, 4, 5, 5, light)
    # base long sides 8x2 (0,6) and short sides 4x2 (8,6): upper row mid, lower row low, darker ends
    fill(0, 6, 8, 7, mid)
    fill(0, 7, 8, 8, low)
    fill(0, 6, 1, 8, low)
    fill(7, 6, 8, 8, dark)
    fill(8, 6, 12, 7, low)
    fill(8, 7, 12, 8, dark)
    # top cuboid's long sides 6x1 (0,8) and short sides 2x1 (8,8)
    fill(0, 8, 6, 9, hi)
    fill(5, 8, 6, 9, mid)
    fill(8, 8, 10, 9, mid)
    # bottom 8x4 (0,10)
    fill(0, 10, 8, 14, dark)
    return im


def model(name):
    tex = 'simplebuilding:block/placed_%s' % name
    base = {'from': BASE[0], 'to': BASE[1], 'faces': {
        'up': {'uv': [0, 0, 8, 4], 'texture': '#ingot'},
        'down': {'uv': [0, 10, 8, 14], 'texture': '#ingot'},
        'north': {'uv': [0, 6, 8, 8], 'texture': '#ingot'},
        'south': {'uv': [0, 6, 8, 8], 'texture': '#ingot'},
        'west': {'uv': [8, 6, 12, 8], 'texture': '#ingot'},
        'east': {'uv': [8, 6, 12, 8], 'texture': '#ingot'},
    }}
    top = {'from': TOP[0], 'to': TOP[1], 'faces': {
        'up': {'uv': [0, 4, 6, 6], 'texture': '#ingot'},
        'north': {'uv': [0, 8, 6, 9], 'texture': '#ingot'},
        'south': {'uv': [0, 8, 6, 9], 'texture': '#ingot'},
        'west': {'uv': [8, 8, 10, 9], 'texture': '#ingot'},
        'east': {'uv': [8, 8, 10, 9], 'texture': '#ingot'},
    }}
    return {'parent': 'minecraft:block/block', 'textures': {'ingot': tex, 'particle': tex}, 'elements': [base, top]}


def write_json(rel, data):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


# the four places of an ingot stack (PlacedSmallParts.INGOT_STACK): pixel x, pixel z, lift, turn
STACK = {1: [(8, 8, 0, 0)], 2: [(8, 5, 0, 0), (8, 11, 0, 0)], 3: [(8, 5, 0, 0), (8, 11, 0, 0), (8, 8, 3, 90)],
         4: [(8, 5, 0, 0), (8, 11, 0, 0), (5, 8, 3, 90), (11, 8, 3, 90)]}


def preview(path, jar, sprites, models, textures):
    rods = importlib.util.spec_from_file_location('rods', os.path.join(HERE, 'standing_rods_2026_10_04.py'))
    r = importlib.util.module_from_spec(rods)
    saved = sys.argv
    sys.argv = [saved[0]]
    try:
        rods.loader.exec_module(r)
    finally:
        sys.argv = saved
    W, H = 1300, 760
    sheet = Image.new('RGBA', (W, H), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    d.text((20, 8), 'Platzierte Barren (N24) - Item (8x), Flaechentextur (8x), Barren 3D, Stapel 2/3/4 (gleiche Mathematik wie im Spiel)',
            fill=(0, 0, 0))
    for i, name in enumerate(INGOTS):
        x = 20 + i * 255
        d.text((x, 30), name, fill=(0, 0, 0))
        sheet.alpha_composite(sprites[name].resize((128, 128), Image.NEAREST), (x, 50))
        sheet.alpha_composite(textures[name].resize((128, 128), Image.NEAREST), (x + 120, 50))
        tex = {'ingot': textures[name]}
        qs = []
        for el in models[name]['elements']:
            qs += r.quads(el, tex, lambda p: p)
        r.render(d, qs, r.iso, (x + 110, 300), 12)
        for row, n in enumerate((2, 3, 4)):
            qs = []
            for px, pz, lift, turn in STACK[n]:
                for el in models[name]['elements']:
                    def tf(p, px=px, pz=pz, lift=lift, turn=turn):
                        dx, dz = p[0] - 8, p[2] - 8
                        if turn:
                            dx, dz = -dz, dx
                        return (px + dx, p[1] + lift, pz + dz)
                    qs += r.quads(el, tex, tf)
            r.render(d, qs, r.iso, (x + 40 + row * 0 + (row % 2) * 120, 420 + (row // 2) * 0 + row * 110), 7)
            d.text((x, 360 + row * 110), '%d Barren' % n, fill=(60, 60, 60))
    sheet.save(path)


def main():
    jar = zipfile.ZipFile(JAR)
    sprites, models, textures = {}, {}, {}
    for name, ref in INGOTS.items():
        im = sprite(jar, ref)
        tex = texture(shades(im))
        mdl = model(name)
        sprites[name], models[name], textures[name] = im, mdl, tex
        p = os.path.join(OUT, 'textures', 'block', 'placed_%s.png' % name)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        tex.save(p)
        write_json('models/block/placed_%s.json' % name, mdl)
        write_json('items/placed_%s.json' % name, {'model': {'type': 'minecraft:model', 'model': 'simplebuilding:block/placed_%s' % name}})
        print(name, shades(im))
    if PREVIEW:
        preview(PREVIEW, jar, sprites, models, textures)


if __name__ == '__main__':
    main()
