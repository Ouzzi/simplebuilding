"""Usage: python tools/textures/standing_rods_2026_10_04.py [client.jar] [preview.png]

Standing rods (owner 2026-10-04, docs/ai/PLAN-SENKRECHTE-RODS-2026-10-04.md): stick, bone, blaze rod, breeze rod and
diamond rod stand upright as the block simplebuilding:standing_rod (state rod=...). Each column is made from the pixels
of its item texture, without the head piece of the lightning-rod-like rods:

- the diagonal item texture is un-sheared pixel for pixel: image row y stays a row (bottom row = model y 0), the cross
  coordinate is x + y, so the column is as tall as the texture has rows (stick 13 px, rods and bone 14 px);
- cross-section 2x2 px like the lightning rod's stem; rows with wide ends (the bone's joints, 5+ pixels across) become
  4x4 boxes, and the short tip rows next to them join the joint;
- the light edge of the texture (upper left = small x + y) goes to the north and west faces, the shadow edge to the
  south and east faces, the caps come from the top and bottom rows.

Writes into mc26_3/overlay/resources/assets/simplebuilding/: textures/block/standing_<rod>.png,
models/block/standing_<rod>.json, blockstates/standing_rod.json. With a preview path it draws every column in 3D next
to its item texture, and a hammock between standing-stick posts (two sticks stacked) at 2, 3 and 4 free blocks, side
and top views with the middle line between the anchors - rendered with the same element rotations as the game
(joml rotationZYX), using tools/textures/hammock.py's models.
"""
import importlib.util
import io
import json
import math
import os
import sys
import zipfile

from PIL import Image, ImageDraw

JAR = sys.argv[1] if len(sys.argv) > 1 else os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar')
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
OUT = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
# rod state name -> item texture (vanilla or the mod's own)
RODS = {
    'stick': 'minecraft:item/stick',
    'bone': 'minecraft:item/bone',
    'blaze_rod': 'minecraft:item/blaze_rod',
    'breeze_rod': 'minecraft:item/breeze_rod',
    'diamond_rod': 'simplebuilding:item/diamond_rod',
}
WIDE = 5          # a row with this many pixels across or more is a joint (4x4 box)


def load_texture(jar, ref):
    ns, path = ref.split(':')
    if ns == 'minecraft':
        return Image.open(io.BytesIO(jar.read('assets/minecraft/textures/%s.png' % path))).convert('RGBA')
    return Image.open(os.path.join(OUT, 'textures', path + '.png')).convert('RGBA')


def strip(item):
    """Rows of the un-sheared item texture from the bottom up: each a list of RGB colours ordered by x + y."""
    rows = []
    for y in range(item.height - 1, -1, -1):
        cs = [item.getpixel((x, y))[:3] for x in range(item.width) if item.getpixel((x, y))[3] > 0]
        if cs:
            rows.append(cs)
    return rows


def boxes(rows):
    """(first row, last row, width) runs: 2 px, 4 px for joints; tip rows next to a joint join it."""
    widths = [4 if len(r) >= WIDE else 2 for r in rows]
    for i in range(len(rows)):           # a 2 px tip at either end touching a joint belongs to the joint
        if widths[i] == 2 and len(rows[i]) < 4:
            if (i == 0 and len(rows) > 1 and widths[1] == 4) or (i == len(rows) - 1 and i > 0 and widths[i - 1] == 4):
                widths[i] = 4
    runs, start = [], 0
    for i in range(1, len(rows) + 1):
        if i == len(rows) or widths[i] != widths[start]:
            runs.append((start, i - 1, widths[start]))
            start = i
    return runs


def window(row, w, light):
    """`w` colours of one row for the light faces (inside the outline: the item's highlight and body) or the shadow
    faces (body and the dark outline edge). Item textures outline every pixel row; a block column has no outline on
    its lit side (like the end rod), the shadow edge keeps it. Short rows (tips) use what they have, repeating edges."""
    inner = row[1:-1] if len(row) >= 3 else list(row)
    cs = list(inner) if light else list(row[1:]) if len(row) >= 3 else list(row)
    while len(cs) < w:
        cs = ([cs[0]] + cs) if light else (cs + [cs[-1]])
    return cs[:w] if light else cs[-w:]


def texture(rows, runs):
    """Block texture: columns 0-3 light faces, 4-7 shadow faces (row r of the model = texture row 15 - r, centred),
    8-11 top cap, 12-15 bottom cap (rows 0-3)."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for r0, r1, w in runs:
        for r in range(r0, r1 + 1):
            off = (4 - w) // 2
            for i, c in enumerate(window(rows[r], w, True)):
                im.putpixel((off + i, 15 - r), c + (255,))
            for i, c in enumerate(window(rows[r], w, False)):
                im.putpixel((4 + off + i, 15 - r), c + (255,))
    for base, r in ((8, len(rows) - 1), (12, 0)):
        w = next(run[2] for run in runs if run[0] <= r <= run[1])
        off = (4 - w) // 2
        light, shadow = window(rows[r], w, True), window(rows[r], w, False)
        for j in range(w):           # light half towards north-west, shadow half towards south-east
            for i in range(w):
                c = light[i] if i + j < w else shadow[i]
                im.putpixel((base + off + i, off + j), c + (255,))
    return im


def model(name, runs, particle):
    elements = []
    for r0, r1, w in runs:
        lo, hi = 8 - w / 2, 8 + w / 2
        off = (4 - w) // 2
        y0, y1 = r0, r1 + 1
        v0, v1 = 16 - y1, 16 - y0
        a = [off, v0, off + w, v1]                   # light window, left = light edge
        a_flip = [off + w, v0, off, v1]
        b = [4 + off, v0, 4 + off + w, v1]           # shadow window, right = shadow edge
        b_flip = [4 + off + w, v0, 4 + off, v1]
        faces = {
            'north': {'uv': a_flip, 'texture': '#rod'},   # seen from the north the west edge is on the right
            'west': {'uv': a, 'texture': '#rod'},         # seen from the west the north edge is on the left
            'south': {'uv': b, 'texture': '#rod'},        # seen from the south the east edge is on the right
            'east': {'uv': b_flip, 'texture': '#rod'},    # seen from the east the south edge is on the left
        }
        if r1 == height(runs) - 1:
            faces['up'] = {'uv': [8 + off, off, 8 + off + w, off + w], 'texture': '#rod'}
        if r0 == 0:
            faces['down'] = {'uv': [12 + off, off, 12 + off + w, off + w], 'texture': '#rod'}
        elements.append({'from': [lo, y0, lo], 'to': [hi, y1, hi], 'faces': faces})
    # caps between runs of different width (the joint's top/bottom ring around the thinner shaft)
    for k in range(len(runs) - 1):
        (_, r1, w1), (r0, _, w2) = runs[k], runs[k + 1]
        wide = max(w1, w2)
        el = elements[k] if w1 == wide else elements[k + 1]
        off = (4 - wide) // 2
        face = 'up' if w1 == wide else 'down'
        base = 8 if face == 'up' else 12
        el['faces'][face] = {'uv': [base + off, off, base + off + wide, off + wide], 'texture': '#rod'}
    return {'ambientocclusion': False,
            'textures': {'particle': '#rod', 'rod': 'simplebuilding:block/standing_' + name},
            'elements': elements}


def height(runs):
    return runs[-1][1] + 1


def connected(mdl, runs):
    """N16 (2026-10-09): the model with a standing rod above (state up=true) - the shaft goes on to the top of the
    block, so stacked rods join without a gap. The extension is a 2x2 shaft piece textured with shaft rows."""
    h = height(runs)
    ext = 16 - h
    out = json.loads(json.dumps(mdl))
    if ext <= 0:
        return out
    shaft = max((r for r in runs if r[2] == 2), key=lambda r: r[1] - r[0])
    r_a = max(shaft[0], (shaft[0] + shaft[1] + 1 - ext) // 2)
    v0, v1 = 16 - (r_a + ext), 16 - r_a
    off = 1
    for el in out['elements']:
        el['faces'].pop('up', None) if el['to'][1] == h and el['to'][0] - el['from'][0] == 2 else None
    out['elements'].append({'from': [7, h, 7], 'to': [9, 16, 9], 'faces': {
        'north': {'uv': [off + 2, v0, off, v1], 'texture': '#rod'},
        'west': {'uv': [off, v0, off + 2, v1], 'texture': '#rod'},
        'south': {'uv': [4 + off, v0, 4 + off + 2, v1], 'texture': '#rod'},
        'east': {'uv': [4 + off + 2, v0, 4 + off, v1], 'texture': '#rod'},
    }})
    return out


def write_json(rel, data):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


# --- preview: textured quads with the game's element rotations, painter's algorithm ------------------------------

def load_hammock():
    spec = importlib.util.spec_from_file_location('hammock', os.path.join(HERE, 'hammock.py'))
    mod = importlib.util.module_from_spec(spec)
    saved = sys.argv
    sys.argv = [saved[0]]
    try:
        spec.loader.exec_module(mod)
    finally:
        sys.argv = saved
    return mod


def face_uv(el, face):
    if 'uv' in el['faces'][face]:
        return el['faces'][face]['uv']
    (x0, y0, z0), (x1, y1, z1) = el['from'], el['to']
    return {'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0], 'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0],
            'south': [x0, 16 - y1, x1, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
            'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0]}[face]


def face_corners(el, face):
    """Corners of a face as (top-left, top-right, bottom-left) seen from outside, in element coordinates."""
    (x0, y0, z0), (x1, y1, z1) = el['from'], el['to']
    return {
        'north': ((x1, y1, z0), (x0, y1, z0), (x1, y0, z0)),
        'south': ((x0, y1, z1), (x1, y1, z1), (x0, y0, z1)),
        'west': ((x0, y1, z0), (x0, y1, z1), (x0, y0, z0)),
        'east': ((x1, y1, z1), (x1, y1, z0), (x1, y0, z1)),
        'up': ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1)),
        'down': ((x0, y0, z1), (x1, y0, z1), (x0, y0, z0)),
    }[face]


def quads(el, textures, transform, step=1.0):
    """Texel quads (four world points, RGBA) of all faces of an element."""
    out = []
    for face, spec in el['faces'].items():
        tex = textures[spec['texture'].lstrip('#')]
        u0, v0, u1, v1 = face_uv(el, face)
        tl, tr, bl = face_corners(el, face)
        nu = max(1, int(round(abs(u1 - u0))))
        nv = max(1, int(round(abs(v1 - v0))))
        for i in range(nu):
            for j in range(nv):
                s0, s1, t0, t1 = i / nu, (i + 1) / nu, j / nv, (j + 1) / nv
                pt = lambda s, t: tuple(tl[k] + s * (tr[k] - tl[k]) + t * (bl[k] - tl[k]) for k in range(3))
                corners = [pt(s0, t0), pt(s1, t0), pt(s1, t1), pt(s0, t1)]
                tu = u0 + (s0 + s1) / 2 * (u1 - u0)
                tv = v0 + (t0 + t1) / 2 * (v1 - v0)
                c = tex.getpixel((min(tex.width - 1, int(tu * tex.width / 16)), min(tex.height - 1, int(tv * tex.width / 16))))
                if c[3] == 0:
                    continue
                world = [transform(rotate(el, q)) for q in corners]
                out.append((world, c))
    return out


def rotate(el, q):
    rot = el.get('rotation')
    if not rot:
        return q
    o = rot['origin']
    v = (q[0] - o[0], q[1] - o[1], q[2] - o[2])
    if 'axis' in rot:
        v = {'x': HAMMOCK.rot_x, 'y': HAMMOCK.rot_y, 'z': HAMMOCK.rot_z}[rot['axis']](v, math.radians(rot['angle']))
    else:
        v = HAMMOCK.rot_x(v, math.radians(rot.get('x', 0)))
        v = HAMMOCK.rot_y(v, math.radians(rot.get('y', 0)))
        v = HAMMOCK.rot_z(v, math.radians(rot.get('z', 0)))
    return v[0] + o[0], v[1] + o[1], v[2] + o[2]


def shade(world):
    a, b, c = world[0], world[1], world[3]
    u = [b[i] - a[i] for i in range(3)]
    v = [c[i] - a[i] for i in range(3)]
    n = (u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0])
    length = math.sqrt(sum(x * x for x in n)) or 1
    nx, ny, nz = (x / length for x in n)
    return min(1.0, 0.6 * nx * nx + 0.8 * nz * nz + (1.0 if ny > 0 else 0.5) * ny * ny)


def render(d, qs, project, origin, scale):
    """project(pt) -> (screen x, screen y, depth); larger depth = farther."""
    items = []
    for world, c in qs:
        pts = [project(p) for p in world]
        depth = sum(p[2] for p in pts) / 4
        f = shade(world)
        col = tuple(min(255, int(c[k] * f)) for k in range(3)) + (255,)
        items.append((depth, [(origin[0] + p[0] * scale, origin[1] - p[1] * scale) for p in pts], col))
    items.sort(key=lambda it: -it[0])
    for _, poly, col in items:
        d.polygon(poly, fill=col)


def iso(pt, yaw=45):
    """Camera from the south-east above (yaw 45: sees the south and east faces) or the north-west (yaw 225)."""
    x, y, z = pt[0] - 8, pt[1], pt[2] - 8
    a = math.radians(yaw)
    sx = x * math.cos(a) - z * math.sin(a)
    sz = x * math.sin(a) + z * math.cos(a)
    e = math.radians(30)
    return sx, y * math.cos(e) - sz * math.sin(e), -(sz * math.cos(e) + y * math.sin(e))


def preview(path, jar, items, models_rods, textures_rods):
    W, H = 1280, 1340
    sheet = Image.new('RGBA', (W, H), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    d.text((20, 8), 'Aufgestellte Staebe - Item-Textur (8x), Blocktextur (4x) und Saeule (3D, 14x), ohne Kopfstueck', fill=(0, 0, 0))
    for i, name in enumerate(RODS):
        x = 20 + i * 250
        d.text((x, 30), '%s  %s  (%d px hoch)' % ('ABCDE'[i], name, height_of(models_rods[name])), fill=(0, 0, 0))
        sheet.alpha_composite(items[name].resize((128, 128), Image.NEAREST), (x, 50))
        sheet.alpha_composite(textures_rods[name].resize((64, 64), Image.NEAREST), (x + 140, 50))
        d.text((x + 140, 118), 'Blocktextur', fill=(60, 60, 60))
        qs = []
        for el in models_rods[name]['elements']:
            qs += quads(el, {'rod': textures_rods[name]}, lambda p: p)
        render(d, qs, lambda p: iso(p, 225), (x + 60, 440), 14)
        render(d, qs, iso, (x + 170, 440), 14)
        d.text((x + 30, 450), 'von Nordwest', fill=(60, 60, 60))
        d.text((x + 140, 450), 'von Suedost', fill=(60, 60, 60))
    # hammock between standing-stick posts
    hm = HAMMOCK
    wool = load_texture(jar, 'minecraft:block/white_wool')
    bar = load_texture(jar, 'minecraft:block/stripped_oak_log')
    rope = Image.open(os.path.join(OUT, 'textures', 'block', 'hammock_rope.png')).convert('RGBA')
    tex = {'wool': wool, 'bar': bar, 'rope': rope, 'rod': textures_rods['stick']}
    models = {}
    for gap in hm.GAPS:
        models['cloth_%d' % gap] = hm.cloth_template(False, gap)
        models['rope_%d' % gap] = hm.rope_end(False, gap)
    side = lambda p: (p[2], p[1], p[0])          # line along +z to the right, y up, depth = x (smaller is nearer)
    top = lambda p: (p[2], -p[0], -p[1])         # seen from above: z right, x down
    for row, gap in enumerate(hm.GAPS):
        pieces, anchors, cells = hm.scene(models, 'south', False, gap)
        qs = []
        for cell, mdl, y in pieces:
            for el in mdl['elements']:
                qs += quads(el, tex, lambda q, cell=cell, y=y: tuple(
                    c * 16 + w for c, w in zip(cell, hm.blockstate_turn(q, y))))
        for a in anchors:                        # two standing sticks: one on the ground, one on rope height
            for level in (0, 1):
                for el in models_rods['stick']['elements']:
                    qs += quads(el, tex, lambda q, a=a, level=level: (a[0] * 16 + q[0], level * 16 + q[1], a[2] * 16 + q[2]))
        base_y = 680 + row * 280
        sc = 4
        ox = 60
        worst, cloth, ends = hm.symmetry(models, False, gap)
        d.text((ox, base_y - 150), 'Haengematte an Stock-Pfosten, gerade, %d frei - Seite (Mittellinie rot)  '
                                   'Tuch %+.2f..%+.2f px, Abweichung %.4f px' % (gap, cloth[0], cloth[1], worst), fill=(0, 0, 0))
        mid = (anchors[0][2] + anchors[1][2]) * 8 + 8
        for z0 in range(anchors[0][2], anchors[1][2] + 1):   # cell grid
            for yy in (0, 1):
                d.rectangle([ox + z0 * 16 * sc, base_y - (yy + 1) * 16 * sc, ox + (z0 + 1) * 16 * sc, base_y - yy * 16 * sc],
                            outline=(170, 170, 170))
        render(d, qs, side, (ox, base_y), sc)
        d.line([ox + mid * sc, base_y - 34 * sc, ox + mid * sc, base_y + 4], fill=(220, 0, 0), width=2)
        tx = 720
        d.text((tx, base_y - 150), 'Draufsicht', fill=(0, 0, 0))
        for z0 in range(anchors[0][2], anchors[1][2] + 1):
            d.rectangle([tx + z0 * 16 * sc, base_y - 110, tx + (z0 + 1) * 16 * sc, base_y - 110 + 16 * sc], outline=(170, 170, 170))
        render(d, qs, top, (tx, base_y - 110), sc)
        d.line([tx + mid * sc, base_y - 120, tx + mid * sc, base_y - 110 + 16 * sc + 10], fill=(220, 0, 0), width=2)
    sheet.save(path)


def height_of(mdl):
    return int(max(el['to'][1] for el in mdl['elements']))


HAMMOCK = None


def main():
    global HAMMOCK
    jar = zipfile.ZipFile(JAR)
    items, mdls, texs = {}, {}, {}
    variants = {}
    for name, ref in RODS.items():
        item = load_texture(jar, ref)
        rows = strip(item)
        runs = boxes(rows)
        tex = texture(rows, runs)
        mdl = model(name, runs, ref)
        items[name], mdls[name], texs[name] = item, mdl, tex
        path = os.path.join(OUT, 'textures', 'block', 'standing_%s.png' % name)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        tex.save(path)
        write_json('models/block/standing_%s.json' % name, mdl)
        write_json('models/block/standing_%s_up.json' % name, connected(mdl, runs))
        variants['rod=%s,up=false' % name] = {'model': 'simplebuilding:block/standing_%s' % name}
        variants['rod=%s,up=true' % name] = {'model': 'simplebuilding:block/standing_%s_up' % name}
        print('%-12s %2d px high, boxes %s' % (name, height(runs), [(r0, r1 + 1, w) for r0, r1, w in runs]))
    write_json('blockstates/standing_rod.json', {'variants': variants})
    if PREVIEW:
        HAMMOCK = load_hammock()
        preview(PREVIEW, jar, items, mdls, texs)


if __name__ == '__main__':
    main()
