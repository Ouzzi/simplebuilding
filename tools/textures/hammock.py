"""Usage: python tools/textures/hammock.py [client.jar] [preview.png]

Hammock (owner queue 2026-10-02, docs/ai/PLAN-HAENGEMATTE-2026-10-02.md). Writes into mc26_3/overlay/resources/assets/simplebuilding/:
- textures/block/hammock_rope.png: a twisted string rope in the tones of vanilla item/string;
- textures/item/<colour>_hammock.png: 16 icons, the sagging cloth in the three main tones of the vanilla wool texture of
  that colour, spreaders in the tones of stripped oak, ropes like the block rope;
- the block models: one cloth template (anchor end to the north, the cloth sags 22.5 degrees towards the middle, a
  stripped-oak spreader bar at the end) with one child per colour that only swaps in the vanilla wool texture, the
  rope end (two ropes fanning up from the spreader ends to a knot at the anchor) and the rope span (gap of 3);
- blockstates (model north = towards the anchor: foot y = rotation of the opposite facing, head y = of the facing),
  item definitions and item models.
With a preview path it also draws a sheet: the 16 icons (A-P, 8x), the rope texture and a side view of the block models
between two fence posts for both gaps (2 and 3)."""
import io
import json
import math
import os
import sys
import zipfile

from PIL import Image, ImageDraw

JAR = sys.argv[1] if len(sys.argv) > 1 else os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar')
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..')
OUT = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
COLOURS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple',
           'blue', 'brown', 'green', 'red', 'black']
# Creative-tab order (vanilla's gameplay colour order), used for the preview letters.
TAB_ORDER = ['white', 'light_gray', 'gray', 'black', 'brown', 'red', 'orange', 'yellow', 'lime', 'green', 'cyan',
             'light_blue', 'blue', 'purple', 'magenta', 'pink']
ROTATION = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
OPPOSITE = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}

SAG = 22.5          # cloth angle
PIVOT = (8, 3, 16)  # cloth pivot: middle of the hammock, bottom of the cloth
ROPE_ANGLE = 22.5


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def vanilla(jar, path):
    return Image.open(io.BytesIO(jar.read('assets/minecraft/textures/' + path + '.png'))).convert('RGBA')


def tones(im, n, min_lum=0):
    """`n` tones of a texture from dark to light at even luminance quantiles (opaque pixels, not darker than min_lum)."""
    px = sorted((im.getpixel((x, y))[:3] for x in range(im.width) for y in range(min(16, im.height))
                 if im.getpixel((x, y))[3] > 200 and lum(im.getpixel((x, y))) >= min_lum), key=lum)
    return [px[round(0.08 * (len(px) - 1) + i * 0.84 * (len(px) - 1) / (n - 1))] for i in range(n)]


def darker(c, f=0.62):
    return tuple(max(0, round(v * f)) for v in c)


# --- textures ----------------------------------------------------------------------------------------------------

def rope_texture(string_tones):
    dark, mid, light = string_tones
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            k = (x + y) % 4
            im.putpixel((x, y), (dark if k == 0 else mid if k == 1 else light) + (255,))
    return im


def icon(wool, wood, rope):
    """Side view: two ropes from the upper corners, spreaders, the cloth sagging between them."""
    dark, mid, light = wool
    outline = darker(dark, 0.7)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    put = lambda x, y, c: im.putpixel((x, y), c + (255,))
    # ropes: knot at the anchor, down to the spreader
    for x, y in [(1, 1), (1, 2), (2, 3), (2, 4), (14, 1), (14, 2), (13, 3), (13, 4)]:
        put(x, y, rope[1] if y % 2 else rope[2])
    put(1, 1, rope[0])
    put(14, 1, rope[0])
    # spreaders (seen end-on)
    for x, y in [(2, 5), (3, 5), (12, 5), (13, 5)]:
        put(x, y, wood[2])
    for x, y in [(2, 6), (3, 6), (12, 6), (13, 6)]:
        put(x, y, wood[0])
    # cloth: top edge sags towards the middle, three rows thick, outlined below
    for x in range(3, 13):
        t = (x - 7.5) / 4.5
        top = 6 + round(4 * (1 - t * t))
        put(x, top, light)
        put(x, top + 1, mid if x % 3 else light)
        put(x, top + 2, dark)
        put(x, top + 3, outline)
    for x in (3, 12):
        put(x, 6, light)
    return im


# --- models ------------------------------------------------------------------------------------------------------

def faces(texture, uv=None, skip=()):
    out = {}
    for f in ['north', 'east', 'south', 'west', 'up', 'down']:
        if f in skip:
            continue
        face = {'texture': texture}
        if uv:
            face['uv'] = uv[f] if isinstance(uv, dict) else uv
        out[f] = face
    return out


def cloth_template():
    rot = {'origin': list(PIVOT), 'axis': 'x', 'angle': SAG}
    return {
        'ambientocclusion': False,
        'textures': {'particle': '#wool'},
        'elements': [
            {'name': 'cloth', 'from': [2, 3, 0], 'to': [14, 4, 16], 'rotation': rot,
             'faces': faces('#wool', {'up': [2, 0, 14, 16], 'down': [2, 0, 14, 16], 'north': [2, 7, 14, 8],
                                      'south': [2, 7, 14, 8], 'east': [0, 7, 16, 8], 'west': [0, 7, 16, 8]})},
            {'name': 'side_west', 'from': [1, 3, 0], 'to': [2, 6, 16], 'rotation': rot,
             'faces': faces('#wool', {'up': [1, 0, 2, 16], 'down': [1, 0, 2, 16], 'north': [1, 5, 2, 8],
                                      'south': [1, 5, 2, 8], 'east': [0, 5, 16, 8], 'west': [0, 5, 16, 8]})},
            {'name': 'side_east', 'from': [14, 3, 0], 'to': [15, 6, 16], 'rotation': rot,
             'faces': faces('#wool', {'up': [14, 0, 15, 16], 'down': [14, 0, 15, 16], 'north': [14, 5, 15, 8],
                                      'south': [14, 5, 15, 8], 'east': [0, 5, 16, 8], 'west': [0, 5, 16, 8]})},
            {'name': 'spreader', 'from': [0, 8.5, 0], 'to': [16, 10.5, 2],
             'faces': faces('#bar', {'up': [0, 0, 16, 2], 'down': [0, 2, 16, 4], 'north': [0, 4, 16, 6],
                                     'south': [0, 6, 16, 8], 'east': [0, 0, 2, 2], 'west': [2, 0, 4, 2]})},
        ],
    }


def rope_end():
    strand = {'north': [7, 0, 8, 15], 'south': [8, 0, 9, 15], 'east': [9, 0, 10, 15], 'west': [10, 0, 11, 15],
              'up': [7, 0, 8, 1], 'down': [8, 0, 9, 1]}
    return {
        'ambientocclusion': False,
        'textures': {'particle': '#rope', 'rope': 'simplebuilding:block/hammock_rope'},
        'elements': [
            {'name': 'rope_west', 'from': [1, -6, 0.5], 'to': [2, 9, 1.5],
             'rotation': {'origin': [1.5, -6, 1], 'axis': 'z', 'angle': -ROPE_ANGLE}, 'faces': faces('#rope', strand)},
            {'name': 'rope_east', 'from': [14, -6, 0.5], 'to': [15, 9, 1.5],
             'rotation': {'origin': [14.5, -6, 1], 'axis': 'z', 'angle': ROPE_ANGLE}, 'faces': faces('#rope', strand)},
            {'name': 'knot', 'from': [7, 6.5, 0], 'to': [9, 9, 2.5], 'faces': faces('#rope', [4, 4, 6, 6])},
            # into the anchor's block up to a fence post or rod (6 px); hidden inside full blocks
            {'name': 'tie', 'from': [7.5, 7.25, -6], 'to': [8.5, 8.25, 0],
             'faces': faces('#rope', {'north': [7, 7, 8, 8], 'south': [8, 7, 9, 8], 'east': [0, 7, 6, 8],
                                      'west': [0, 8, 6, 9], 'up': [7, 0, 8, 6], 'down': [8, 0, 9, 6]}, skip=('south',))},
        ],
    }


def rope_span():
    return {
        'ambientocclusion': False,
        'textures': {'particle': '#rope', 'rope': 'simplebuilding:block/hammock_rope'},
        'elements': [
            {'name': 'span', 'from': [7.5, 7.25, -6], 'to': [8.5, 8.25, 16],
             'faces': faces('#rope', {'north': [7, 7, 8, 8], 'south': [8, 7, 9, 8], 'east': [0, 7, 16, 8],
                                      'west': [0, 8, 16, 9], 'up': [7, 0, 8, 16], 'down': [8, 0, 9, 16]})},
        ],
    }


def blockstate_hammock(colour):
    variants = {}
    for facing in ROTATION:
        for part in ('foot', 'head'):
            outward = OPPOSITE[facing] if part == 'foot' else facing
            v = {'model': 'simplebuilding:block/%s_hammock' % colour}
            if ROTATION[outward]:
                v['y'] = ROTATION[outward]
            variants['facing=%s,part=%s' % (facing, part)] = v
    return {'variants': variants}


def blockstate_rope():
    variants = {}
    for facing in ROTATION:
        for kind in ('end', 'span'):
            v = {'model': 'simplebuilding:block/hammock_rope_%s' % kind}
            if ROTATION[facing]:
                v['y'] = ROTATION[facing]
            variants['facing=%s,kind=%s' % (facing, kind)] = v
    return {'variants': variants}


def write_json(rel, data):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def save(rel, im):
    path = os.path.join(OUT, 'textures', rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)


# --- preview -----------------------------------------------------------------------------------------------------

def element_polygon(el):
    """Side view (z right, y up) of an element after its rotation, as model-space (z, y) points."""
    (x0, y0, z0), (x1, y1, z1) = el['from'], el['to']
    pts = [(z0, y0), (z1, y0), (z1, y1), (z0, y1)]
    rot = el.get('rotation')
    if rot and rot['axis'] == 'x':
        a = math.radians(rot['angle'])
        oz, oy = rot['origin'][2], rot['origin'][1]
        out = []
        for z, y in pts:
            dz, dy = z - oz, y - oy
            # right-handed about +x: y' = y cos - z sin, z' = y sin + z cos
            out.append((oz + dy * math.sin(a) + dz * math.cos(a), oy + dy * math.cos(a) - dz * math.sin(a)))
        return out
    if rot and rot['axis'] == 'z':
        # side view of a strand leaning in x: its z/y extent with the y shortened by cos
        a = math.radians(rot['angle'])
        oy = rot['origin'][1]
        return [(z, oy + (y - oy) * math.cos(a)) for z, y in pts]
    return pts


def preview(path, icons, rope_tex, wool_white, wood, string):
    S = 8
    labels = 'ABCDEFGHIJKLMNOP'
    W, H = 20 + 8 * 140, 800
    sheet = Image.new('RGBA', (W, H), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    d.text((20, 8), 'Haengematte / Hammock - Icons (8x), Kreativ-Reihenfolge', fill=(0, 0, 0))
    for i, colour in enumerate(TAB_ORDER):
        x, y = 20 + (i % 8) * 140, 30 + (i // 8) * 160
        sheet.alpha_composite(icons[colour].resize((16 * S, 16 * S), Image.NEAREST), (x, y))
        d.text((x, y + 16 * S + 4), '%s  %s' % (labels[i], colour), fill=(0, 0, 0))
    d.text((20, 360), 'Seil (8x)', fill=(0, 0, 0))
    sheet.alpha_composite(rope_tex.resize((128, 128), Image.NEAREST), (20, 376))
    # side views
    models = {'cloth': cloth_template(), 'end': rope_end(), 'span': rope_span()}
    colour_of = {'cloth': wool_white[1], 'side_west': wool_white[2], 'side_east': wool_white[2], 'spreader': wood[1]}
    P = 6  # pixels per model pixel
    for row, gap in enumerate((2, 3)):
        ox, oy = 260, 560 + row * 215   # origin of block column 0, lower layer bottom
        d.text((ox, oy - 205), 'Seitenansicht, %d Bloecke Abstand (Zaunpfosten als Anker)' % gap, fill=(0, 0, 0))
        n = gap + 2
        for b in range(n):  # grid
            for layer in range(2):
                d.rectangle([ox + b * 16 * P, oy - (layer + 1) * 16 * P, ox + (b + 1) * 16 * P, oy - layer * 16 * P],
                            outline=(170, 170, 170))
        for b in (0, n - 1):  # fence posts on rope height, ground column below
            cx = ox + b * 16 * P + 8 * P
            d.rectangle([cx - 2 * P, oy - 32 * P, cx + 2 * P, oy], fill=(122, 92, 54))

        def draw(model, block, layer, flip):
            for el in model['elements']:
                poly = element_polygon(el)
                pts = []
                for z, y in poly:
                    zz = 16 - z if flip else z
                    pts.append((ox + block * 16 * P + zz * P, oy - layer * 16 * P - y * P))
                col = colour_of.get(el['name'], string[1])
                d.polygon(pts, fill=col + (255,), outline=darker(col) + (255,))

        # foot cloth in block 1 (anchor west = model north), head cloth in block 2 (anchor east)
        draw(models['cloth'], 1, 0, False)
        draw(models['cloth'], 2, 0, True)
        draw(models['end'], 1, 1, False)
        draw(models['end'], 2, 1, True)
        if gap == 3:
            draw(models['span'], 3, 1, True)
    sheet.save(path)


def main():
    jar = zipfile.ZipFile(JAR)
    string = tones(vanilla(jar, 'item/string'), 3, min_lum=110)  # without the dark outline
    wood = tones(vanilla(jar, 'block/stripped_oak_log'), 3)
    rope_tex = rope_texture((darker(string[0], 0.78), darker(string[1], 0.92), string[2]))
    save('block/hammock_rope.png', rope_tex)
    icons = {}
    wools = {}
    for colour in COLOURS:
        wools[colour] = tones(vanilla(jar, 'block/%s_wool' % colour), 3)
        icons[colour] = icon(wools[colour], wood, string)
        save('item/%s_hammock.png' % colour, icons[colour])
        write_json('models/block/%s_hammock.json' % colour, {
            'parent': 'simplebuilding:block/hammock_cloth_template',
            'textures': {'wool': 'minecraft:block/%s_wool' % colour}})
        write_json('blockstates/%s_hammock.json' % colour, blockstate_hammock(colour))
        write_json('models/item/%s_hammock.json' % colour, {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': 'simplebuilding:item/%s_hammock' % colour}})
        write_json('items/%s_hammock.json' % colour, {
            'model': {'type': 'minecraft:model', 'model': 'simplebuilding:item/%s_hammock' % colour}})
    template = cloth_template()
    template['textures']['bar'] = 'minecraft:block/stripped_oak_log'
    write_json('models/block/hammock_cloth_template.json', template)
    write_json('models/block/hammock_rope_end.json', rope_end())
    write_json('models/block/hammock_rope_span.json', rope_span())
    write_json('blockstates/hammock_rope.json', blockstate_rope())
    if PREVIEW:
        preview(PREVIEW, icons, rope_tex, wools['white'], wood, string)
    print('hammock: 16 colours, rope, models and blockstates written')


if __name__ == '__main__':
    main()
