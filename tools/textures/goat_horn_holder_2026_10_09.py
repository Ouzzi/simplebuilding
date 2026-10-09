"""Usage: python tools/textures/goat_horn_holder_2026_10_09.py [client.jar] [preview.png]

Goat horn holder (owner 2026-10-08, queue N24 "Ziegenhorn platzierbar; Fackeln oder stabartige Items hineinstecken",
docs/ai/PLAN-PLATZIEREN-N24-2026-10-09.md): a goat horn put down as a 3D horn - thin tip at the back, curving up into
a wide mouth that holds a torch or a rod (GoatHornHolderBlock). Standing on a floor, or one block higher on a wall with
a little peg at the tip.

The horn's own pixel art, painted from the colours of the vanilla goat horn sprite (grey horn, lighter ridges, dark
inside): five cuboids getting wider towards the mouth, ridge rings at the same heights on every cuboid, the mouth's top
a rim around the dark opening.

Writes into mc26_3/overlay/resources/assets/simplebuilding/:
  textures/block/goat_horn_holder.png
  models/block/goat_horn_holder_floor.json, goat_horn_holder_wall.json, blockstates/goat_horn_holder.json
  items/held_<torch>.json (torch, soul, copper, redstone torch -> the vanilla standing torch model) and
  items/standing_<rod>.json (-> the standing rod columns) - the holder renderer draws the held item through them.
The mouth heights must match GoatHornHolderBlock.FLOOR_MOUTH / WALL_MOUTH (9 / 13 px).
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
TORCHES = ('torch', 'soul_torch', 'copper_torch', 'redstone_torch')
RODS = ('stick', 'bone', 'blaze_rod', 'breeze_rod', 'diamond_rod')
WALL_LIFT = 4
# (from, to) of the horn standing on the floor, facing north: tip to the south, mouth up (top at y 9)
HORN = [
    ([7.5, 0, 12], [8.5, 1, 14.5]),
    ([7, 0, 10], [9, 2, 12.5]),
    ([6.5, 1, 8], [9.5, 4, 10.5]),
    ([6, 3, 6.5], [10, 7, 10]),
    ([5.5, 6, 5.5], [10.5, 9, 10.5]),
]
PEG = ([7, 3, 14], [9, 5, 16])


def palette(jar):
    im = Image.open(io.BytesIO(jar.read('assets/minecraft/textures/item/goat_horn.png'))).convert('RGBA')
    cs = sorted({im.getpixel((x, y))[:3] for y in range(16) for x in range(16) if im.getpixel((x, y))[3] > 128}, key=sum)
    # dark inside .. light ridge
    return {'inside': cs[0], 'inside2': cs[1], 'shadow': cs[3], 'outline': cs[4], 'body': cs[5], 'body2': cs[6],
            'ridge': cs[7], 'light': cs[8]}


def texture(p):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))

    def put(x, y, c):
        im.putpixel((x, y), c + (255,))
    # rows 0..9: the horn's sides by height (row r = model y 9-r-1 .. 9-r): body with ridge rings
    for r in range(10):
        for x in range(16):
            ring = r % 3
            c = p['ridge'] if ring == 0 else p['body'] if ring == 1 else p['body2']
            if ring == 2 and x % 4 == 1:
                c = p['outline']
            if ring == 0 and x % 5 == 2:
                c = p['light']
            put(x, r, c)
    # mouth top 5x5 at (0,10): rim and the dark opening
    for y in range(5):
        for x in range(5):
            rim = x in (0, 4) or y in (0, 4)
            c = (p['light'] if (x + y) % 3 == 0 else p['ridge']) if rim else (p['inside'] if (x + y) % 2 else p['inside2'])
            put(x, 10 + y, c)
    # tip / bottom faces and the peg at (8,10)
    for y in range(10, 16):
        for x in range(8, 16):
            put(x, y, p['shadow'] if (x + y) % 3 else p['outline'])
    return im


def side_uv(y0, y1, w):
    v0, v1 = 9 - y1, 9 - y0
    return [8 - w / 2, v0, 8 + w / 2, v1]


def element(frm, to, mouth=False, lift=0):
    (x0, y0, z0), (x1, y1, z1) = frm, to
    wx, wz = x1 - x0, z1 - z0
    faces = {
        'north': {'uv': side_uv(y0, y1, wx), 'texture': '#horn'},
        'south': {'uv': side_uv(y0, y1, wx), 'texture': '#horn'},
        'west': {'uv': side_uv(y0, y1, wz), 'texture': '#horn'},
        'east': {'uv': side_uv(y0, y1, wz), 'texture': '#horn'},
        'up': {'uv': [0, 10, 5, 15] if mouth else [8 - wx / 2, 0, 8 + wx / 2, min(9, wz)], 'texture': '#horn'},
        'down': {'uv': [8, 10, 8 + min(8, wx), 10 + min(6, wz)], 'texture': '#horn'},
    }
    return {'from': [x0, y0 + lift, z0], 'to': [x1, y1 + lift, z1], 'faces': faces}


def model(wall):
    lift = WALL_LIFT if wall else 0
    els = [element(f, t, i == len(HORN) - 1, lift) for i, (f, t) in enumerate(HORN)]
    if wall:
        (x0, y0, z0), (x1, y1, z1) = PEG
        els.append({'from': [x0, y0, z0], 'to': [x1, y1, z1], 'faces': {d: {'uv': [8, 10, 10, 12], 'texture': '#horn'}
                                                                         for d in ('north', 'south', 'west', 'east', 'up', 'down')}})
    return {'parent': 'minecraft:block/block', 'ambientocclusion': False,
            'textures': {'horn': 'simplebuilding:block/goat_horn_holder', 'particle': 'simplebuilding:block/goat_horn_holder'},
            'elements': els}


def write_json(rel, data):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def blockstate():
    variants = {}
    for face, name in (('floor', 'goat_horn_holder_floor'), ('wall', 'goat_horn_holder_wall')):
        for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            v = {'model': 'simplebuilding:block/' + name}
            if y:
                v['y'] = y
            variants['face=%s,facing=%s' % (face, facing)] = v
    # never placed under a ceiling, but every state needs a model
    for facing in ('north', 'east', 'south', 'west'):
        variants['face=ceiling,facing=%s' % facing] = {'model': 'simplebuilding:block/goat_horn_holder_floor', 'x': 180}
    return {'variants': variants}


def preview(path, jar, tex):
    spec = importlib.util.spec_from_file_location('rods', os.path.join(HERE, 'standing_rods_2026_10_04.py'))
    r = importlib.util.module_from_spec(spec)
    saved = sys.argv
    sys.argv = [saved[0]]
    try:
        spec.loader.exec_module(r)
    finally:
        sys.argv = saved
    torch_tex = Image.open(io.BytesIO(jar.read('assets/minecraft/textures/block/torch.png'))).convert('RGBA')
    torch = json.loads(jar.read('assets/minecraft/models/block/template_torch.json'))
    stick_tex = Image.open(os.path.join(OUT, 'textures', 'block', 'standing_stick.png')).convert('RGBA')
    with open(os.path.join(OUT, 'models', 'block', 'standing_stick.json')) as f:
        stick = json.load(f)
    W, H = 1100, 560
    sheet = Image.new('RGBA', (W, H), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    d.text((20, 8), 'Ziegenhorn-Halter (N24) - Textur (8x), Boden/Wand leer, mit Fackel, mit Stock (Modell-Mathematik wie im Spiel)',
           fill=(0, 0, 0))
    sheet.alpha_composite(tex.resize((128, 128), Image.NEAREST), (20, 40))
    scenes = []
    for wall in (False, True):
        for held in (None, 'torch', 'stick'):
            scenes.append((wall, held))
    for i, (wall, held) in enumerate(scenes):
        qs = []
        for el in model(wall)['elements']:
            qs += r.quads(el, {'horn': tex}, lambda p: p)
        if wall:   # the wall block behind (south)
            pass
        mouth = 13 if wall else 9
        bottom = mouth - 3
        if held == 'torch':
            for el in torch['elements']:
                qs += r.quads(el, {'torch': torch_tex}, lambda p, b=bottom: (p[0], p[1] + b, p[2]))
        elif held == 'stick':
            for el in stick['elements']:
                qs += r.quads(el, {'rod': stick_tex}, lambda p, b=bottom: (p[0], p[1] + b, p[2]))
        x = 180 + (i % 3) * 300
        y = 260 + (i // 3) * 260
        r.render(d, qs, r.iso, (x + 120, y), 11)
        r.render(d, qs, lambda p: r.iso(p, 135), (x + 230, y), 6)
        d.text((x, y + 20), '%s, %s' % ('Wand' if wall else 'Boden', held or 'leer'), fill=(40, 40, 40))
    sheet.save(path)


def main():
    jar = zipfile.ZipFile(JAR)
    p = palette(jar)
    tex = texture(p)
    path = os.path.join(OUT, 'textures', 'block', 'goat_horn_holder.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    tex.save(path)
    write_json('models/block/goat_horn_holder_floor.json', model(False))
    write_json('models/block/goat_horn_holder_wall.json', model(True))
    write_json('blockstates/goat_horn_holder.json', blockstate())
    for t in TORCHES:
        write_json('items/held_%s.json' % t, {'model': {'type': 'minecraft:model', 'model': 'minecraft:block/%s' % t}})
    for rod in RODS:
        write_json('items/standing_%s.json' % rod, {'model': {'type': 'minecraft:model', 'model': 'simplebuilding:block/standing_%s' % rod}})
    if PREVIEW:
        preview(PREVIEW, jar, tex)


if __name__ == '__main__':
    main()
