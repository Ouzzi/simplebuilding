"""Usage: python tools/textures/hammock.py [client.jar] [preview.png]

Hammock (owner queue 2026-10-02, v2 2026-10-04, docs/ai/PLAN-HAENGEMATTE-2026-10-02.md). Writes into
mc26_3/overlay/resources/assets/simplebuilding/:
- textures/block/hammock_rope.png: a twisted string rope in the tones of vanilla item/string;
- textures/item/<colour>_hammock.png: 16 icons, the sagging cloth in the three main tones of the vanilla wool texture of
  that colour, spreaders in the tones of stripped oak, ropes like the block rope;
- block models in the canonical frame (facing south: the line runs to +z, diagonally to -x/+z): per layout (straight or
  diagonal, 2 to 4 free cells) one cloth template drawn by the cloth head (two blocks of cloth in the middle between the
  anchors, sagging 22.5 degrees, stripped-oak spreaders, diagonal turned -45 degrees about y - 26.3 rotates elements
  about x and y at once) with one child per colour that swaps in the vanilla wool texture, and one rope end drawn by the
  first rope cell (two ropes from the spreader ends up to a knot at the anchor and a tie into it); the far rope end is
  the same model turned 180 degrees; the other cells use an empty model;
- blockstates (y = facing's rotation - 180) and the item definitions and item models.
With a preview path it draws a sheet: icons, rope texture and the hammocks (2, 3, 4 straight, side and top views;
diagonal 2 and 4, side view along the line and top view), computed with the same element rotations as the game.
"""
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

SAG = 22.5                                    # cloth angle
HALF = 16 * math.cos(math.radians(SAG))       # horizontal half length of the cloth (14.78 px)
SPREADER_Y = 9.5                              # middle of the spreader bar, lower layer
KNOT_Y = 8                                    # rope height in the anchor's (upper) layer
GAPS = (2, 3, 4)


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


def cloth_cells(gap):
    return {2: [0, 1], 3: [0, 1, 2], 4: [1, 2]}[gap]


def head_cell(gap):
    return 2 if gap == 4 else 1


def frame(diagonal, cell):
    """Line frame in the coordinates of `cell` (canonical, facing south): origin on the first anchor's face (corner),
    unit vector u along the line, p across it, cell length along the line."""
    if diagonal:
        r = 1 / math.sqrt(2)
        return (16 + 16 * cell, -16 * cell), (-r, r), (r, r), 16 * math.sqrt(2)
    return (8, -16 * cell), (0, 1), (1, 0), 16


def at(origin, u, p, t, w=0.0):
    return origin[0] + t * u[0] + w * p[0], origin[1] + t * u[1] + w * p[1]


def r1(v):
    return round(v, 4)


def cloth_template(diagonal, gap):
    o, u, p, cell = frame(diagonal, head_cell(gap))
    cx, cz = at(o, u, p, gap * cell / 2)
    yaw = -45 if diagonal else 0

    def tilted(name, x0, y0, x1, y1, toward_b, uv):
        z0, z1 = (cz, cz + 16) if toward_b else (cz - 16, cz)
        rot = {'origin': [r1(cx), 3, r1(cz)], 'x': -SAG if toward_b else SAG, 'y': yaw, 'z': 0}
        return {'name': name, 'from': [r1(cx + x0), y0, r1(z0)], 'to': [r1(cx + x1), y1, r1(z1)], 'rotation': rot,
                'faces': faces('#wool', uv)}

    cloth_uv = {'up': [2, 0, 14, 16], 'down': [2, 0, 14, 16], 'north': [2, 7, 14, 8], 'south': [2, 7, 14, 8],
                'east': [0, 7, 16, 8], 'west': [0, 7, 16, 8]}
    side_uv = {'up': [1, 0, 2, 16], 'down': [1, 0, 2, 16], 'north': [1, 5, 2, 8], 'south': [1, 5, 2, 8],
               'east': [0, 5, 16, 8], 'west': [0, 5, 16, 8]}
    elements = []
    for toward_b in (False, True):
        tag = 'b' if toward_b else 'a'
        elements.append(tilted('cloth_' + tag, -6, 3, 6, 4, toward_b, cloth_uv))
        elements.append(tilted('side_left_' + tag, -7, 3, -6, 6, toward_b, side_uv))
        elements.append(tilted('side_right_' + tag, 6, 3, 7, 6, toward_b, side_uv))
        z = cz + HALF if toward_b else cz - HALF
        elements.append({'name': 'spreader_' + tag, 'from': [r1(cx - 8), SPREADER_Y - 1, r1(z - 1)],
                         'to': [r1(cx + 8), SPREADER_Y + 1, r1(z + 1)],
                         'rotation': {'origin': [r1(cx), 3, r1(cz)], 'x': 0, 'y': yaw, 'z': 0},
                         'faces': faces('#bar', {'up': [0, 0, 16, 2], 'down': [0, 2, 16, 4], 'north': [0, 4, 16, 6],
                                                 'south': [0, 6, 16, 8], 'east': [0, 0, 2, 2], 'west': [2, 0, 4, 2]})})
    return {'ambientocclusion': False, 'textures': {'particle': '#wool', 'bar': 'minecraft:block/stripped_oak_log'},
            'elements': elements}


def strand(name, a, b):
    """A rope strand from point a (bottom) to point b (top): a column along +y turned about x, then z."""
    d = [b[i] - a[i] for i in range(3)]
    length = math.sqrt(sum(v * v for v in d))
    dx, dy, dz = (v / length for v in d)
    alpha = math.degrees(math.asin(dz))
    gamma = math.degrees(math.atan2(-dx, dy))
    uv = {'north': [7, 0, 8, 15], 'south': [8, 0, 9, 15], 'east': [9, 0, 10, 15], 'west': [10, 0, 11, 15],
          'up': [7, 0, 8, 1], 'down': [8, 0, 9, 1]}
    return {'name': name, 'from': [r1(a[0] - 0.5), r1(a[1]), r1(a[2] - 0.5)],
            'to': [r1(a[0] + 0.5), r1(a[1] + length), r1(a[2] + 0.5)],
            'rotation': {'origin': [r1(a[0]), r1(a[1]), r1(a[2])], 'x': r1(alpha), 'y': 0, 'z': r1(gamma)},
            'faces': faces('#rope', uv)}


def rope_end(diagonal, gap):
    o, u, p, cell = frame(diagonal, 0)
    knot = (o[0], KNOT_Y, o[1])
    start = gap * cell / 2 - HALF
    y = SPREADER_Y - 16
    elements = []
    for name, w in (('rope_left', -7), ('rope_right', 7)):
        ex, ez = at(o, u, p, start, w)
        elements.append(strand(name, (ex, y, ez), knot))
    yaw = -45 if diagonal else 0
    tie = 6 * math.sqrt(2) if diagonal else 6
    elements.append({'name': 'knot', 'from': [r1(knot[0] - 1), 6.75, r1(knot[2] - 1)],
                     'to': [r1(knot[0] + 1), 9.25, r1(knot[2] + 1.5)],
                     'rotation': {'origin': [r1(knot[0]), KNOT_Y, r1(knot[2])], 'x': 0, 'y': yaw, 'z': 0},
                     'faces': faces('#rope', [4, 4, 6, 6])})
    # into the anchor's block up to a fence post or rod; hidden inside full blocks
    elements.append({'name': 'tie', 'from': [r1(knot[0] - 0.5), 7.5, r1(knot[2] - tie)], 'to': [r1(knot[0] + 0.5), 8.5, r1(knot[2])],
                     'rotation': {'origin': [r1(knot[0]), KNOT_Y, r1(knot[2])], 'x': 0, 'y': yaw, 'z': 0},
                     'faces': faces('#rope', {'north': [7, 7, 8, 8], 'south': [8, 7, 9, 8], 'east': [0, 7, 6, 8],
                                              'west': [0, 8, 6, 9], 'up': [7, 0, 8, 6], 'down': [8, 0, 9, 6]}, skip=('south',))})
    return {'ambientocclusion': False, 'textures': {'particle': '#rope', 'rope': 'simplebuilding:block/hammock_rope'},
            'elements': elements}


def check_bounds(model, name):
    for el in model['elements']:
        for v in el['from'] + el['to']:
            assert -16 <= v <= 32, (name, el['name'], v)


def layout_name(diagonal, gap):
    return ('diagonal_' if diagonal else '') + str(gap)


def y_of(facing):
    return (ROTATION[facing] - 180) % 360


def variant(model, y):
    v = {'model': model}
    if y:
        v['y'] = y
    return v


def blockstate_hammock(colour):
    variants = {'part=foot': {'model': 'simplebuilding:block/hammock_empty'}}
    for facing in ROTATION:
        for diagonal in (False, True):
            for gap in GAPS:
                key = 'diagonal=%s,facing=%s,gap=%d,part=head' % (str(diagonal).lower(), facing, gap)
                variants[key] = variant('simplebuilding:block/%s_hammock_%s' % (colour, layout_name(diagonal, gap)), y_of(facing))
    return {'variants': variants}


def blockstate_rope():
    variants = {}
    for facing in ROTATION:
        for diagonal in (False, True):
            for gap in GAPS:
                for index in range(4):
                    key = 'diagonal=%s,facing=%s,gap=%d,index=%d' % (str(diagonal).lower(), facing, gap, index)
                    model = 'simplebuilding:block/hammock_rope_%s' % layout_name(diagonal, gap)
                    if index == 0:
                        variants[key] = variant(model, y_of(facing))
                    elif index == gap - 1:
                        variants[key] = variant(model, (y_of(facing) + 180) % 360)
                    else:
                        variants[key] = {'model': 'simplebuilding:block/hammock_rope_link'}
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


# --- preview: the same element rotations as the game (joml rotationZYX, right-handed) -------------------------------

def rot_x(v, a):
    c, s = math.cos(a), math.sin(a)
    return v[0], v[1] * c - v[2] * s, v[1] * s + v[2] * c


def rot_y(v, a):
    c, s = math.cos(a), math.sin(a)
    return v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c


def rot_z(v, a):
    c, s = math.cos(a), math.sin(a)
    return v[0] * c - v[1] * s, v[0] * s + v[1] * c, v[2]


def element_corners(el):
    (x0, y0, z0), (x1, y1, z1) = el['from'], el['to']
    pts = [(x, y, z) for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]
    rot = el.get('rotation')
    if not rot:
        return pts
    o = rot['origin']
    out = []
    for q in pts:
        v = (q[0] - o[0], q[1] - o[1], q[2] - o[2])
        if 'axis' in rot:
            v = {'x': rot_x, 'y': rot_y, 'z': rot_z}[rot['axis']](v, math.radians(rot['angle']))
        else:
            v = rot_x(v, math.radians(rot.get('x', 0)))
            v = rot_y(v, math.radians(rot.get('y', 0)))
            v = rot_z(v, math.radians(rot.get('z', 0)))
        out.append((v[0] + o[0], v[1] + o[1], v[2] + o[2]))
    return out


def blockstate_turn(pt, y):
    """Variant y rotation: clockwise seen from above (north -> east) about the block centre."""
    a, b = pt[0] - 8, pt[2] - 8
    for _ in range((y // 90) % 4):
        a, b = -b, a
    return a + 8, pt[1], b + 8


def hull(points):
    pts = sorted(set(points))
    if len(pts) < 3:
        return pts

    def cross(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])

    lower, upper = [], []
    for q in pts:
        while len(lower) >= 2 and cross(lower[-2], lower[-1], q) <= 0:
            lower.pop()
        lower.append(q)
    for q in reversed(pts):
        while len(upper) >= 2 and cross(upper[-2], upper[-1], q) <= 0:
            upper.pop()
        upper.append(q)
    return lower[:-1] + upper[:-1]


def scene(models, facing, diagonal, gap):
    """World pieces (cell, model, y) of one hammock with its first anchor at (0, 1, 0), the anchors and the cells."""
    step = {'south': (0, 1), 'north': (0, -1), 'east': (1, 0), 'west': (-1, 0)}[facing]
    if diagonal:
        cw = {'south': (-1, 0), 'west': (0, -1), 'north': (1, 0), 'east': (0, 1)}[facing]
        step = (step[0] + cw[0], step[1] + cw[1])
    rope = lambda i: ((i + 1) * step[0], 1, (i + 1) * step[1])
    name = layout_name(diagonal, gap)
    pieces = [(rope(0), models['rope_' + name], y_of(facing)),
              (rope(gap - 1), models['rope_' + name], (y_of(facing) + 180) % 360)]
    c = rope(head_cell(gap))
    pieces.append(((c[0], 0, c[2]), models['cloth_' + name], y_of(facing)))
    anchors = [(0, 1, 0), rope(gap)]
    cells = [rope(i) for i in range(gap)] + [(rope(i)[0], 0, rope(i)[2]) for i in cloth_cells(gap)]
    return pieces, anchors, cells


def colour_of(el, wool, wood, string):
    n = el['name']
    if n.startswith('cloth'):
        return wool[1]
    if n.startswith('side'):
        return wool[2]
    if n.startswith('spreader'):
        return wood[1]
    return darker(string[1], 0.85)


def draw_view(d, box, pieces, anchors, cells, project, scale, wool, wood, string, fence):
    ox, oy = box

    def P(pt):
        a, b = project(pt)
        return ox + a * scale, oy - b * scale

    for c in cells:
        q = [P((c[0] * 16 + dx, c[1] * 16 + dy, c[2] * 16 + dz)) for dx in (0, 16) for dy in (0, 16) for dz in (0, 16)]
        d.polygon(hull(q), outline=(165, 165, 165))
    for a in anchors:  # fence post (6..10) on rope height and below
        q = [P((a[0] * 16 + dx, y, a[2] * 16 + dz)) for dx in (6, 10) for y in (0, 32) for dz in (6, 10)]
        d.polygon(hull(q), fill=fence, outline=darker(fence))
    for cell, model, y in pieces:
        for el in model['elements']:
            q = [P((cell[0] * 16 + w[0], cell[1] * 16 + w[1], cell[2] * 16 + w[2]))
                 for w in (blockstate_turn(pt, y) for pt in element_corners(el))]
            col = colour_of(el, wool, wood, string)
            d.polygon(hull(q), fill=col + (255,), outline=darker(col) + (255,))


def preview(path, icons, rope_tex, models, wool, wood, string):
    S = 8
    labels = 'ABCDEFGHIJKLMNOP'
    W, H = 1180, 1620
    sheet = Image.new('RGBA', (W, H), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    d.text((20, 8), 'Haengematte v2 - Icons (8x), Kreativ-Reihenfolge', fill=(0, 0, 0))
    for i, colour in enumerate(TAB_ORDER):
        x, y = 20 + (i % 8) * 140, 30 + (i // 8) * 160
        sheet.alpha_composite(icons[colour].resize((16 * S, 16 * S), Image.NEAREST), (x, y))
        d.text((x, y + 16 * S + 4), '%s  %s' % (labels[i], colour), fill=(0, 0, 0))
    d.text((20, 360), 'Seil (8x)', fill=(0, 0, 0))
    sheet.alpha_composite(rope_tex.resize((128, 128), Image.NEAREST), (20, 376))
    fence = (122, 92, 54)
    sc = 4
    side = lambda pt: (pt[2], pt[1])        # straight hammock facing south: z to the right, y up
    top = lambda pt: (pt[2], -pt[0])        # top view: z to the right, x down
    for row, gap in enumerate(GAPS):
        pieces, anchors, cells = scene(models, 'south', False, gap)
        base = 560 + row * 175
        d.text((180, base - 150), 'gerade, %d frei - Seite' % gap, fill=(0, 0, 0))
        draw_view(d, (180, base), pieces, anchors, cells, side, sc, wool, wood, string, fence)
        d.text((640, base - 150), 'Draufsicht', fill=(0, 0, 0))
        draw_view(d, (640, base - 70), pieces, anchors, cells, top, sc, wool, wood, string, fence)
    r = 1 / math.sqrt(2)
    along = lambda pt: ((-pt[0] + pt[2]) * r, pt[1])
    for row, gap in enumerate((2, 4)):
        pieces, anchors, cells = scene(models, 'south', True, gap)
        base = 1180 + row * 330
        d.text((180, base - 200), 'diagonal, %d frei - Seite entlang der Linie' % gap, fill=(0, 0, 0))
        draw_view(d, (180, base - 40), pieces, anchors, cells, along, sc, wool, wood, string, fence)
        d.text((690, base - 200), 'Draufsicht', fill=(0, 0, 0))
        draw_view(d, (960, base - 180), pieces, anchors, cells, lambda pt: (pt[0], -pt[2]), 3, wool, wood, string, fence)
    sheet.save(path)


def main():
    jar = zipfile.ZipFile(JAR)
    string = tones(vanilla(jar, 'item/string'), 3, min_lum=110)  # without the dark outline
    wood = tones(vanilla(jar, 'block/stripped_oak_log'), 3)
    rope_tex = rope_texture((darker(string[0], 0.78), darker(string[1], 0.92), string[2]))
    save('block/hammock_rope.png', rope_tex)
    models = {}
    for diagonal in (False, True):
        for gap in GAPS:
            name = layout_name(diagonal, gap)
            cloth = cloth_template(diagonal, gap)
            rope = rope_end(diagonal, gap)
            check_bounds(cloth, 'cloth ' + name)
            check_bounds(rope, 'rope ' + name)
            models['cloth_' + name] = cloth
            models['rope_' + name] = rope
            write_json('models/block/hammock_cloth_%s.json' % name, cloth)
            write_json('models/block/hammock_rope_%s.json' % name, rope)
    write_json('models/block/hammock_empty.json', {'textures': {'particle': 'minecraft:block/white_wool'}})
    write_json('models/block/hammock_rope_link.json', {'textures': {'particle': 'simplebuilding:block/hammock_rope'}})
    write_json('blockstates/hammock_rope.json', blockstate_rope())
    icons = {}
    wools = {}
    for colour in COLOURS:
        wools[colour] = tones(vanilla(jar, 'block/%s_wool' % colour), 3)
        icons[colour] = icon(wools[colour], wood, string)
        save('item/%s_hammock.png' % colour, icons[colour])
        for diagonal in (False, True):
            for gap in GAPS:
                name = layout_name(diagonal, gap)
                write_json('models/block/%s_hammock_%s.json' % (colour, name), {
                    'parent': 'simplebuilding:block/hammock_cloth_%s' % name,
                    'textures': {'wool': 'minecraft:block/%s_wool' % colour}})
        write_json('blockstates/%s_hammock.json' % colour, blockstate_hammock(colour))
        write_json('models/item/%s_hammock.json' % colour, {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': 'simplebuilding:item/%s_hammock' % colour}})
        write_json('items/%s_hammock.json' % colour, {
            'model': {'type': 'minecraft:model', 'model': 'simplebuilding:item/%s_hammock' % colour}})
    if PREVIEW:
        preview(PREVIEW, icons, rope_tex, models, wools['white'], wood, string)
    print('hammock v2: 16 colours, 6 layouts, models and blockstates written')


if __name__ == '__main__':
    main()
