"""Usage: python tools/textures/hammock.py [client.jar] [preview.png]

Hammock (owner queue 2026-10-02, v2 2026-10-04, v3 any angle 2026-10-04; docs/ai/PLAN-HAENGEMATTE-2026-10-02.md and
docs/ai/PLAN-HAENGEMATTE-WINKEL-2026-10-02.md). Writes into mc26_3/overlay/resources/assets/simplebuilding/:
- textures/block/hammock_rope.png: a twisted string rope in the tones of vanilla item/string;
- textures/item/<colour>_hammock.png: 16 icons, the sagging cloth in the three main tones of the vanilla wool texture of
  that colour, spreaders in the tones of stripped oak, ropes like the block rope;
- block models without elements (only the particle texture: wool of the colour, the rope) and one-variant blockstates -
  since v3 the cloth head's block-entity renderer (HammockRenderer) draws the whole hammock at any angle; the item
  definitions and item models.
With a preview path it draws a sheet: hammocks at several angles (top view with the occupied cells, side view along
the line), computed with a mirror of HammockLayout (cells) and HammockShape (boxes), plus the icons and the rope.
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
# Creative-tab order (vanilla's gameplay colour order), used for the preview.
TAB_ORDER = ['white', 'light_gray', 'gray', 'black', 'brown', 'red', 'orange', 'yellow', 'lime', 'green', 'cyan',
             'light_blue', 'blue', 'purple', 'magenta', 'pink']
# Old v2 layout models, removed by this generator (the renderer draws every angle).
OLD_LAYOUTS = ['2', '3', '4', 'diagonal_2', 'diagonal_3', 'diagonal_4']

SAG = 22.5                                    # cloth angle (HammockLayout.SAG_DEGREES)
PX = 1 / 16
REACH = math.cos(math.radians(SAG)) + PX      # HammockLayout.CLOTH_REACH
HALF = math.cos(math.radians(SAG))            # HammockShape.HALF


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


# --- geometry: mirror of HammockLayout (cells) and HammockShape (boxes) ---------------------------------------------

def line_cells(dx, dz):
    """HammockLayout.lineCells: cells the line between the anchor centres crosses inside, with entry/exit times."""
    ax, az = abs(dx), abs(dz)
    sx, sz = (dx > 0) - (dx < 0), (dz > 0) - (dz < 0)
    big = 1 << 62
    out, cx, cz, kx, kz, t_in = [], 0, 0, 1, 1, 0.0
    while kx <= ax or kz <= az:
        tx = (2 * kx - 1) * az if kx <= ax and ax else big
        tz = (2 * kz - 1) * ax if kz <= az and az else big
        nx, nz = cx, cz
        if tx == tz:
            t = (2 * kx - 1) / (2 * ax)
            nx, nz, kx, kz = nx + sx, nz + sz, kx + 1, kz + 1
        elif tx < tz:
            t = (2 * kx - 1) / (2 * ax)
            nx, kx = nx + sx, kx + 1
        else:
            t = (2 * kz - 1) / (2 * az)
            nz, kz = nz + sz, kz + 1
        out.append((cx, cz, t_in, t))
        cx, cz, t_in = nx, nz, t
    out.append((cx, cz, t_in, 1.0))
    return out


def cells(dx, dz):
    """Rope cells, cloth cells and the cloth head relative to the first anchor (x, z; cloth one layer down)."""
    length = math.hypot(dx, dz)
    line = line_cells(dx, dz)
    rope = [(c[0], c[1]) for c in line[1:-1]]
    r = REACH / length
    cloth = [(c[0], c[1]) for c in line[1:-1] if c[3] > 0.5 - r and c[2] < 0.5 + r]
    t = 0.5 + 0.5 / length - 1e-9
    head = next((c[0], c[1]) for c in line if c[3] >= t)
    return rope, cloth, head


def _norm(v):
    n = math.sqrt(sum(x * x for x in v))
    return [x / n for x in v]


def _cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]


def _frame(a, b):
    na = _norm(a)
    d = sum(na[i] * b[i] for i in range(3))
    nb = _norm([b[i] - d * na[i] for i in range(3)])
    c = _cross(na, nb)
    if c[1] < 0:
        nb = [-x for x in nb]
        c = _cross(na, nb)
    return na, nb, c


def _add(p, *terms):
    out = list(p)
    for v, k in terms:
        for i in range(3):
            out[i] += v[i] * k
    return out


def boxes(dx, dz):
    """HammockShape.boxes with the first anchor block at (0, 0, 0) (its centre at x = z = 0.5): tuples
    (part, centre, a, b, c, ha, hb, hc)."""
    length = math.hypot(dx, dz)
    ux, uz = dx / length, dz / length
    mx, mz = 0.5 + dx / 2, 0.5 + dz / 2
    cloth_y = -1.0
    across = [-uz, 0, ux]
    exit_ = 0.5 / max(abs(ux), abs(uz))
    knot_y = 8 * PX
    spreader_y = cloth_y + 9.5 * PX
    sag = math.radians(SAG)
    out = []
    for s in (-1, 1):
        tilt = [s * ux * math.cos(sag), math.sin(sag), s * uz * math.cos(sag)]
        f = _frame(tilt, across)
        origin = [mx, cloth_y + 3 * PX, mz]
        out.append(('cloth', _add(origin, (f[0], 0.5), (f[2], 0.5 * PX)), f[0], f[1], f[2], 0.5, 6 * PX, 0.5 * PX))
        for side in (-1, 1):
            out.append(('hem', _add(origin, (f[0], 0.5), (f[2], 1.5 * PX), (f[1], side * 6.5 * PX)), f[0], f[1], f[2],
                        0.5, 0.5 * PX, 1.5 * PX))
        u = [s * ux, 0, s * uz]
        flat = _frame(u, across)
        spreader = [mx + s * HALF * ux, spreader_y, mz + s * HALF * uz]
        out.append(('spreader', spreader, flat[0], flat[1], flat[2], PX, 8 * PX, PX))
        knot = [mx + s * (length / 2 - exit_) * ux, knot_y, mz + s * (length / 2 - exit_) * uz]
        for side in (-1, 1):
            start = _add(spreader, (across, side * 7 * PX))
            d = [knot[i] - start[i] for i in range(3)]
            ln = math.sqrt(sum(x * x for x in d))
            direction = [x / ln for x in d]
            fr = _frame(direction, [-direction[2], 0, direction[0]])
            out.append(('rope', [(start[i] + knot[i]) / 2 for i in range(3)], fr[0], fr[1], fr[2], ln / 2, 0.5 * PX, 0.5 * PX))
        out.append(('knot', _add(knot, (flat[0], -0.25 * PX)), flat[0], flat[1], flat[2], 1.25 * PX, PX, 1.25 * PX))
        tie = exit_ * 7 / 8
        out.append(('tie', _add(knot, (flat[0], tie / 2)), flat[0], flat[1], flat[2], tie / 2, 0.5 * PX, 0.5 * PX))
    return out


def corners(box):
    _, c, a, b, cc, ha, hb, hc = box
    return [[c[k] + i * ha * a[k] + j * hb * b[k] + m * hc * cc[k] for k in range(3)]
            for i in (-1, 1) for j in (-1, 1) for m in (-1, 1)]


def symmetric(dx, dz):
    """Largest mismatch (blocks) between the drawn corners and their mirror image through the middle."""
    length = math.hypot(dx, dz)
    ux, uz = dx / length, dz / length
    mx, mz = 0.5 + dx / 2, 0.5 + dz / 2
    pts = []
    for box in boxes(dx, dz):
        for p in corners(box):
            t = (p[0] - mx) * ux + (p[2] - mz) * uz
            w = -(p[0] - mx) * uz + (p[2] - mz) * ux
            pts.append((round(t, 7), round(w, 7), round(p[1], 7)))
    mirror = sorted((-t, -w, y) for t, w, y in pts)
    pts.sort()
    return max(max(abs(p[i] - q[i]) for i in range(3)) for p, q in zip(pts, mirror))


# --- files -------------------------------------------------------------------------------------------------------

def write_json(rel, data):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def remove(rel):
    path = os.path.join(OUT, rel)
    if os.path.exists(path):
        os.remove(path)


def save(rel, im):
    path = os.path.join(OUT, 'textures', rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)


# --- preview -----------------------------------------------------------------------------------------------------

def hull(points):
    pts = sorted(set((round(p[0], 3), round(p[1], 3)) for p in points))
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


def part_colour(part, wool, wood, string):
    return {'cloth': wool[1], 'hem': wool[2], 'spreader': wood[1]}.get(part, darker(string[1], 0.85))


ORDER = {'cloth': 0, 'hem': 1, 'spreader': 2, 'rope': 3, 'knot': 4, 'tie': 4}


def draw_hammock(d, ox, oy, dx, dz, wool, wood, string, fence, label):
    """Top view (grid, rope and cloth cells, anchors, boxes from above) and below it a side view along the line.
    Returns the width and height used."""
    s = 34  # pixels per block
    rope, cloth, head = cells(dx, dz)
    x0, z0 = min(0, dx) - 1, min(0, dz) - 1
    nx, nz = abs(dx) + 3, abs(dz) + 3
    top = lambda x, z: (ox + (x - x0) * s, oy + 18 + (z - z0) * s)
    d.text((ox, oy), label, fill=(0, 0, 0))
    for gx in range(nx + 1):
        d.line([top(x0 + gx, z0), top(x0 + gx, z0 + nz)], fill=(178, 178, 178))
    for gz in range(nz + 1):
        d.line([top(x0, z0 + gz), top(x0 + nx, z0 + gz)], fill=(178, 178, 178))
    for (cx, cz) in rope:
        d.rectangle([top(cx, cz), top(cx + 1, cz + 1)], fill=(172, 192, 226), outline=(90, 110, 160))
    for (cx, cz) in cloth:
        a, b = top(cx, cz), top(cx + 1, cz + 1)
        d.rectangle([a[0] + 3, a[1] + 3, b[0] - 3, b[1] - 3], outline=(200, 70, 70), width=2)
    for (ax, az) in ((0, 0), (dx, dz)):
        d.rectangle([top(ax + 6 * PX, az + 6 * PX), top(ax + 10 * PX, az + 10 * PX)], fill=fence, outline=darker(fence))
    bx = sorted(boxes(dx, dz), key=lambda b: ORDER[b[0]])
    for box in bx:
        col = part_colour(box[0], wool, wood, string)
        d.polygon(hull([top(p[0], p[2]) for p in corners(box)]), fill=col + (255,), outline=darker(col) + (255,))
    hx, hz = head
    d.text((top(hx, hz)[0] + 2, top(hx, hz)[1] + 1), 'K', fill=(150, 20, 20))
    # side view along the line: t to the right (from the first anchor centre), y up; the cloth layer is y -1..0
    length = math.hypot(dx, dz)
    ux, uz = dx / length, dz / length
    sy = oy + 18 + nz * s + 22
    side = lambda t, y: (ox + (t + 1.0) * s, sy + (1.2 - y) * s)
    d.text((ox, sy - 14), 'Seite entlang der Linie (%.2f Bloecke)' % length, fill=(0, 0, 0))
    d.line([side(-1, -1), side(length + 1, -1)], fill=(90, 90, 90))
    d.line([side(-1, 0), side(length + 1, 0)], fill=(175, 175, 175))
    for t in (0, length):
        d.rectangle([side(t - 2 * PX, 1), side(t + 2 * PX, -1)], fill=fence, outline=darker(fence))
    for box in bx:
        col = part_colour(box[0], wool, wood, string)
        q = [side((p[0] - 0.5) * ux + (p[2] - 0.5) * uz, p[1]) for p in corners(box)]
        d.polygon(hull(q), fill=col + (255,), outline=darker(col) + (255,))
    return max(nx * s, (length + 2) * s), (sy - oy) + 2.6 * s


ANGLES = [((0, 4), 'A gerade 0:4'), ((4, 1), 'B schraeg 4:1'), ((3, 1), 'C schraeg 3:1'), ((5, 2), 'D schraeg 5:2'),
          ((3, 2), 'E schraeg 3:2'), ((4, 4), 'F diagonal 4:4')]


def preview(path, icons, rope_tex, wool, wood, string):
    W, H = 1640, 1900
    sheet = Image.new('RGBA', (W, H), (205, 205, 205, 255))
    d = ImageDraw.Draw(sheet)
    d.text((20, 8), 'Haengematte v3 - jeder Winkel (2-4 frei entlang der Hauptachse, gleiche Hoehe). Draufsicht: blau = '
                    'Seil-Zellen, rot umrandet = Tuch-Zellen (eine Lage tiefer), K = Kopfteil', fill=(0, 0, 0))
    fence = (122, 92, 54)
    x, y, row_h = 20, 34, 0
    for (dx, dz), label in ANGLES:
        rope, cloth, _ = cells(dx, dz)
        angle = math.degrees(math.atan2(min(abs(dx), abs(dz)), max(abs(dx), abs(dz))))
        w, h = draw_hammock(d, x, y, dx, dz, wool, wood, string, fence,
                            '%s - %.1f Grad, %d Seil-/%d Tuchzellen' % (label, angle, len(rope), len(cloth)))
        row_h = max(row_h, h)
        x += max(w, 330) + 40
        if x > W - 480:
            x, y, row_h = 20, y + row_h + 26, 0
    if x > 20:
        y += row_h + 26
    y = int(y)
    d.text((20, y), 'Icons (4x) und Seil', fill=(0, 0, 0))
    for i, colour in enumerate(TAB_ORDER):
        sheet.alpha_composite(icons[colour].resize((64, 64), Image.NEAREST), (20 + i * 72, y + 16))
    sheet.alpha_composite(rope_tex.resize((64, 64), Image.NEAREST), (20, y + 90))
    sheet.crop((0, 0, W, y + 170)).save(path)


def main():
    jar = zipfile.ZipFile(JAR)
    string = tones(vanilla(jar, 'item/string'), 3, min_lum=110)  # without the dark outline
    wood = tones(vanilla(jar, 'block/stripped_oak_log'), 3)
    rope_tex = rope_texture((darker(string[0], 0.78), darker(string[1], 0.92), string[2]))
    save('block/hammock_rope.png', rope_tex)
    # every allowed offset is centred (the same check as HammockTests)
    worst = 0
    for dx in range(-5, 6):
        for dz in range(-5, 6):
            if 3 <= max(abs(dx), abs(dz)) <= 5:
                worst = max(worst, symmetric(dx, dz))
    assert worst < 1e-5, ('hammock not centred', worst)
    print('centred at all 96 offsets, mismatch %.2e blocks' % worst)
    # the v2 layout models are gone: the renderer draws every angle
    for layout in OLD_LAYOUTS:
        remove('models/block/hammock_cloth_%s.json' % layout)
        remove('models/block/hammock_rope_%s.json' % layout)
        for colour in COLOURS:
            remove('models/block/%s_hammock_%s.json' % (colour, layout))
    remove('models/block/hammock_empty.json')
    remove('models/block/hammock_rope_link.json')
    write_json('models/block/hammock_rope.json', {'textures': {'particle': 'simplebuilding:block/hammock_rope'}})
    write_json('blockstates/hammock_rope.json', {'variants': {'': {'model': 'simplebuilding:block/hammock_rope'}}})
    icons = {}
    wools = {}
    for colour in COLOURS:
        wools[colour] = tones(vanilla(jar, 'block/%s_wool' % colour), 3)
        icons[colour] = icon(wools[colour], wood, string)
        save('item/%s_hammock.png' % colour, icons[colour])
        write_json('models/block/%s_hammock.json' % colour, {'textures': {'particle': 'minecraft:block/%s_wool' % colour}})
        write_json('blockstates/%s_hammock.json' % colour, {'variants': {'': {'model': 'simplebuilding:block/%s_hammock' % colour}}})
        write_json('models/item/%s_hammock.json' % colour, {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': 'simplebuilding:item/%s_hammock' % colour}})
        write_json('items/%s_hammock.json' % colour, {
            'model': {'type': 'minecraft:model', 'model': 'simplebuilding:item/%s_hammock' % colour}})
    if PREVIEW:
        preview(PREVIEW, icons, rope_tex, wools['white'], wood, string)
    print('hammock v3: 16 colours, models and blockstates written (drawn by HammockRenderer)')


if __name__ == '__main__':
    main()
