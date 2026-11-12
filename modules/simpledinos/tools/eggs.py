"""Simple Dinosaurs: the dinosaur eggs (a-dino36b, owner DINO36 "the egg skins do not look good").

Own pixel art drawn from code, built like the Vanilla turtle and sniffer eggs: a shell ramp of five tones, light from the top, the
widest tier darkest at its lower edge, patterns as 2-5 px groups with a light pixel on top/left and a darker one below/right
(no single-pixel noise, no hash noise), separate textures for the sides and the lid so the shading can follow the egg shape.

The model is six stacked boxes like the Vanilla dragon egg (1 px steps; widths 6/8/10/8/6/4, 12 px high) with automatic UVs:
    side texture row 15 = foot (cols 5-10), 14 = base (4-11), 9-13 = belly (3-12), 7-8 = shoulder (4-11), 5-6 = upper (5-10), 4 = tip (6-9)
    lid texture: tip 4 x 4 (6-9) and the 1 px rings 5-10, 4-11, 3-12 (the rest of each lid lies under the next box)

Variants (preview DINO36b): A "Flecken" (big soft blotches like the sniffer egg), B "Sprenkel" (pale shell with small groups like
the turtle egg), C "Artmuster" (one motif per kind). CHOSEN is built into the mod.
"""
from __future__ import annotations

from PIL import Image

KINDS = ('trex', 'triceratops', 'stegosaurus', 'utahraptor', 'parasaurolophus')
VARIANTS = ('A', 'B', 'C')
CHOSEN = 'C'

# model boxes (from, to) in pixels, bottom to top
TIERS = [((5, 0, 5), (11, 1, 11)), ((4, 1, 4), (12, 2, 12)), ((3, 2, 3), (13, 7, 13)), ((4, 7, 4), (12, 9, 12)),
         ((5, 9, 5), (11, 11, 11)), ((6, 11, 6), (10, 12, 10))]


def H(s):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def with_lum(c, target):
    """The colour scaled to a luminance (hue kept), clamped to 0..255."""
    f = target / max(1.0, lum(c))
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


# shell base and pattern hue per kind: the ramp steps about 16 luminance (two steps stay below the A8b "hard" jump of 40), darker
# tones lean a little warmer/more saturated like Vanilla shading; the pattern sits one step beyond the shell's dark1 (light2 for
# light dots), so shell -> halo -> pattern goes in soft steps like the sniffer egg
SHELL_BASE = {'trex': '8e8a62', 'triceratops': 'c6af88', 'stegosaurus': '7e8795', 'utahraptor': 'ad7250', 'parasaurolophus': '62978a'}
PATTERN_HUE = {'trex': '6e5230', 'triceratops': 'a06e3c', 'stegosaurus': 'b0582e', 'utahraptor': '6a3418', 'parasaurolophus': 'c8ecd6'}
LIGHT_PATTERN = {'parasaurolophus'}
STEP = 18.0
# variant B: pale shell for every kind (turtle egg), the kind shows in the speckles
PALE_BASE = 'ddd5bb'


def _ramp_from(base_hex):
    b = H(base_hex)
    lb = lum(b)
    out = []
    for k in (-2, -1, 0, 1, 2):
        c = with_lum(b, lb + k * STEP)
        if k < 0:  # warmer, a touch more saturated in the shade
            c = (min(255, c[0] + 3 * -k), c[1], max(0, c[2] - 2 * -k))
        out.append(c)
    return out


def ramp(kind, variant):
    return _ramp_from(PALE_BASE if variant == 'B' else SHELL_BASE[kind])


def pat(kind, variant='C'):
    """(shadow, tone, light) of the pattern."""
    r = ramp(kind, variant)
    hue = H(PATTERN_HUE[kind])
    if kind in LIGHT_PATTERN and variant != 'B':
        top = lum(r[4]) + 0.3 * STEP
        return with_lum(hue, top - 0.5 * STEP), with_lum(hue, top), with_lum(hue, top + 0.5 * STEP)
    if variant == 'B':  # speckles on the pale shell: darker, the kind's hue (as the turtle egg's teal dots)
        base = lum(r[0]) - 2.2 * STEP
        return with_lum(hue, base - 0.7 * STEP), with_lum(hue, base), with_lum(hue, base + 0.7 * STEP)
    tone = lum(r[1]) - 1.5 * STEP
    return with_lum(hue, tone - 0.5 * STEP), with_lum(hue, tone), with_lum(hue, tone + 0.5 * STEP)


# ---------------------------------------------------------------- shapes
def silhouette_rows():
    """{row: (first col, last col)} of the side texture that the side faces show."""
    rows = {}
    for (x0, y0, _), (x1, y1, _) in TIERS:
        for y in range(y0, y1):
            rows[15 - y] = (x0, x1 - 1)
    return rows


SIDE_ROWS = silhouette_rows()


# shell tone index (0 dark2 .. 4 light2) per side row: light from above, the belly darkens toward the ground
ROW_TONE = {4: 3, 5: 3, 6: 3, 7: 3, 8: 2, 9: 2, 10: 2, 11: 2, 12: 2, 13: 1, 14: 1, 15: 0}
# rim catches: the first row of each step (and of the belly) is lit in 2-3 px groups, the tone changes along it (A8d 2)
RIM = {(7, 4), (8, 4), (6, 5), (7, 5), (5, 7), (6, 7), (9, 7), (4, 9), (5, 9), (8, 9), (9, 9), (5, 14), (6, 14), (10, 14)}


def _hash(x, y, s):
    n = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


SHAPES = [((0, 0), (1, 0)), ((0, 0), (0, 1)), ((0, 0), (1, 0), (0, 1)), ((0, 0), (1, 0), (1, 1)), ((0, 0), (1, 0), (2, 0))]


def clusters(visible, seed, share=0.3):
    """Tone groups of 2-3 px (A8d 3): {(x, y): +1 or -1}, a free pixel between groups, about `share` of the visible pixels.
    Light groups lean to the upper left, dark ones to the lower right (A8d 4); no single pixels."""
    out = {}
    cells = sorted(visible, key=lambda c: _hash(c[0], c[1], seed))
    target = int(len(visible) * share)
    for (x, y) in cells:
        if len(out) >= target:
            break
        shape = SHAPES[int(_hash(x, y, seed + 1) * len(SHAPES)) % len(SHAPES)]
        group = [(x + dx, y + dy) for dx, dy in shape]
        if any(g not in visible or g in out for g in group):
            continue
        if any((gx + ax, gy + ay) in out for gx, gy in group for ax, ay in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            continue
        cx = sum(g[0] for g in group) / len(group) - 7.5
        cy = sum(g[1] for g in group) / len(group) - 9.5
        sign = 1 if (_hash(x, y, seed + 2) - 0.5) - (cx + cy) * 0.05 > 0 else -1
        for g in group:
            out[g] = sign
    return out


SIDE_VISIBLE = {(x, y) for y, (a, b) in SIDE_ROWS.items() for x in range(a, b + 1)}
TOP_VISIBLE = {(x, y) for y in range(3, 13) for x in range(3, 13)}


def shell_side(kind, variant):
    """Shell without pattern: tone per row (ROW_TONE), the turning box edges a step darker, lit rims, 2-3 px tone groups.
    Outside the silhouette the texture is clear (only the visible part is drawn, like the Vanilla turtle egg)."""
    r = ramp(kind, variant)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    groups = clusters(SIDE_VISIBLE - RIM, 31, 0.32)
    for (x, y) in SIDE_VISIBLE:
        first, last = SIDE_ROWS[y]
        t = ROW_TONE[y]
        if x in (first, last):
            t -= 1
        t += groups.get((x, y), 0)
        if (x, y) in RIM:
            t = ROW_TONE[y] + 1
        px[x, y] = r[max(0, min(4, t))] + (255,)
    return img


def top_ring(x, y):
    return int(max(abs(x - 7.5), abs(y - 7.5)) - 0.5)  # 0..1 tip, 2 upper ring, 3 shoulder ring, 4 belly ring


def shell_top(kind, variant):
    """Lid seen from above: tip and rings, each outer ring a step darker; along each ring the tone changes every 2-3 px (A8d 2),
    lighter toward the top left. Outside the used 10 x 10 the texture is clear."""
    r = ramp(kind, variant)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    groups = clusters(TOP_VISIBLE, 47, 0.22)
    for (x, y) in TOP_VISIBLE:
        ring = top_ring(x, y)
        t = 3 if ring <= 3 else 2
        lean = (x + y) - 15  # negative toward the light
        if ring >= 2 and ((x + 2 * y) // 3) % 2 == 0:
            t += -1 if lean > 0 else 1
        t += groups.get((x, y), 0) if ring >= 2 else 0
        px[x, y] = r[max(0, min(4, t))] + (255,)
    for (x, y) in ((6, 6), (7, 6), (6, 7)):  # highlight group on the tip toward the light
        px[x, y] = r[4] + (255,)
    for (x, y) in ((9, 9), (8, 9)):  # the far side of the tip a step darker
        px[x, y] = r[2] + (255,)
    return img


# blotch stamps: '#' = tone, the renderer adds light top/left and shadow bottom/right inside the shape
STAMPS = {
    'blob': ['.##.', '####', '.##.'],
    'blob2': ['.##', '###', '##.'],
    'bean': ['###.', '.###'],
    'drop': ['.#.', '###', '.#.'],
    'pair': ['##', '#.'],
    'dash': ['##'],
    'dot': ['#'],
    'band': ['########'],
    'chev': ['#...#', '.#.#.', '..#..'],
    'plate': ['.#.', '###'],
}


def stamp(px, name, ox, oy, colors, rows=None, halo=None):
    """Draws a stamp; rows limits it to the visible rows (None = anywhere). halo = (ramp, step): the shell pixels next to the shape
    shift one ramp step toward the pattern first, so the edge goes shell -> halo -> pattern in soft steps (A8b, like the sniffer egg)."""
    sh, tone, hi = colors
    shape = STAMPS[name]
    cells = {(ox + x, oy + y) for y, row in enumerate(shape) for x, ch in enumerate(row) if ch == '#'}

    def visible(x, y):
        if not (0 <= x < 16 and 0 <= y < 16):
            return False
        return rows is None or (y in rows and rows[y][0] <= x <= rows[y][1])

    if halo is not None:
        ramp_, step = halo
        for (x, y) in cells:
            for (nx, ny) in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if (nx, ny) in cells or not visible(nx, ny) or px[nx, ny][3] == 0:
                    continue
                c = px[nx, ny][:3]
                if c in ramp_:
                    i = ramp_.index(c)
                    px[nx, ny] = ramp_[max(0, min(4, i + step))] + (255,)
    for (x, y) in cells:
        if not visible(x, y):
            continue
        c = tone
        if (x, y - 1) not in cells and (x - 1, y) not in cells and len(cells) > 2:
            c = hi
        elif (x + 1, y) not in cells and (x, y + 1) not in cells and len(cells) > 2:
            c = sh
        px[x, y] = c + (255,)


# per kind and variant: side stamps and lid stamps as (name, x, y); stamps are clipped to the visible side rows
LAYOUT = {
    'A': {
        'side': [('blob', 3, 9), ('blob2', 9, 10), ('bean', 6, 6), ('pair', 8, 4), ('dash', 10, 8), ('pair', 6, 12), ('dot', 4, 14), ('dash', 8, 15)],
        'top': [('blob2', 5, 5), ('pair', 9, 4), ('dash', 3, 10), ('dot', 11, 9)],
    },
    'B': {
        'side': [('dot', 7, 4), ('pair', 5, 5), ('dash', 9, 6), ('pair', 4, 8), ('dot', 10, 8), ('pair', 7, 9), ('dash', 3, 11), ('pair', 10, 11),
                 ('dot', 6, 12), ('dash', 8, 13), ('dash', 5, 14), ('dot', 9, 15)],
        'top': [('pair', 6, 6), ('dot', 9, 5), ('dash', 4, 10), ('dot', 11, 8), ('dot', 8, 11)],
    },
}
# variant C: one motif per kind
MOTIF = {
    'trex': {'side': [('blob', 3, 9), ('blob2', 9, 10), ('bean', 6, 7), ('pair', 8, 5), ('pair', 6, 12), ('dash', 10, 8), ('dot', 4, 14), ('dot', 5, 5)],
             'top': [('blob2', 5, 5), ('pair', 9, 4), ('dash', 3, 10)]},
    'triceratops': {'side': 'bands', 'top': [('dash', 6, 5), ('dash', 9, 9), ('dash', 4, 10), ('dash', 10, 4), ('dot', 7, 11), ('dot', 3, 6)]},
    'stegosaurus': {'side': [('drop', 4, 9), ('drop', 9, 9), ('drop', 6, 11), ('drop', 11, 11), ('drop', 7, 5), ('dot', 5, 7), ('dot', 10, 7),
                             ('dash', 5, 14), ('dot', 9, 14)],
                    'top': [('drop', 5, 5), ('drop', 9, 8), ('dot', 4, 10), ('dot', 11, 5)]},
    'utahraptor': {'side': [('dot', 7, 4), ('pair', 5, 5), ('dash', 8, 7), ('pair', 4, 8), ('dot', 10, 7), ('pair', 7, 10), ('dash', 10, 10),
                            ('dash', 3, 12), ('pair', 9, 12), ('dot', 6, 13), ('dash', 5, 14), ('dot', 9, 15)],
                   'top': [('pair', 6, 6), ('dot', 9, 5), ('dash', 4, 10), ('pair', 10, 9)]},
    'parasaurolophus': {'side': [('drop', 3, 9), ('drop', 9, 9), ('dot', 7, 12), ('drop', 6, 5), ('dot', 11, 12), ('dot', 5, 14), ('dash', 8, 14),
                                 ('dot', 9, 7), ('dot', 4, 7)],
                        'top': [('drop', 6, 5), ('drop', 9, 9), ('dot', 4, 10)]},
}


def side_pattern(kind, variant):
    if variant == 'C':
        return MOTIF[kind]['side']
    return LAYOUT[variant]['side']


def top_pattern(kind, variant):
    if variant == 'C':
        return MOTIF[kind]['top']
    return LAYOUT[variant]['top']


def pattern_colors(kind, variant):
    return pat(kind, variant)


def _snap(snap, img, name):
    if snap is not None:
        snap(img, name)


def egg_side(kind, variant=CHOSEN, snap=None):
    """Side texture; snap(img, step name) records the drawing steps (layer sources, A8 b)."""
    img = shell_side(kind, variant)
    _snap(snap, img, 'Schale')
    px = img.load()
    colors = pattern_colors(kind, variant)
    layout = side_pattern(kind, variant)
    if layout == 'bands':
        sh, tone, hi = (with_lum(c, lum(c) + 0.6 * STEP) for c in colors)  # bands a little lighter than spots: long edges
        r = ramp(kind, variant)
        for y in (5, 8, 11, 14):  # broken bands, the tone steps every 2-3 px (A8d 4), a darker shell row below as soft edge
            first, last = SIDE_ROWS[y]
            for x in range(first, last + 1):
                k = (x + y) % 5
                if k == 4:
                    continue
                px[x, y] = (hi if k == 0 else sh if k == 3 else tone) + (255,)
                if y + 1 in SIDE_ROWS and SIDE_ROWS[y + 1][0] <= x <= SIDE_ROWS[y + 1][1] and px[x, y + 1][:3] in r:
                    i = r.index(px[x, y + 1][:3])
                    px[x, y + 1] = r[max(0, i - 1)] + (255,)
        for (x, y) in ((6, 9), (7, 9), (9, 12), (10, 12)):
            px[x, y] = tone + (255,)
    else:
        halo = (ramp(kind, variant), 1 if kind in LIGHT_PATTERN and variant != 'B' else -1)
        for name, x, y in layout:
            stamp(px, name, x, y, colors, rows=SIDE_ROWS, halo=halo)
    _snap(snap, img, 'Muster')
    return img


def egg_top(kind, variant=CHOSEN, snap=None):
    img = shell_top(kind, variant)
    _snap(snap, img, 'Schale')
    px = img.load()
    colors = pattern_colors(kind, variant)
    lid_rows = {y: (3, 12) for y in range(3, 13)}
    halo = (ramp(kind, variant), 1 if kind in LIGHT_PATTERN and variant != 'B' else -1)
    for name, x, y in top_pattern(kind, variant):
        stamp(px, name, x, y, colors, rows=lid_rows, halo=halo)
    _snap(snap, img, 'Muster')
    return img


# ---------------------------------------------------------------- brooding and cracks (shared by all kinds)
GLOW = (255, 188, 96)


def overlay_side(phase):
    """Phases 0..11 brooding: a soft warm sheen rises from the ground in four steps (0-2 none, 3-5, 6-8, 9-11); it is translucent so
    it warms every shell colour. 12..14: hairline cracks like the Vanilla eggs (dark line, a pale lip on the lit side)."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    level = 3 if phase >= 9 else 2 if phase >= 6 else 1 if phase >= 3 else 0
    top_row = {0: 16, 1: 13, 2: 10, 3: 7}[level]
    sheen = {c for c in SIDE_VISIBLE if c[1] >= top_row}
    groups = clusters(sheen, 61 + level, 0.4)  # the sheen breathes in 2-3 px groups (A8d 3), no flat film
    for (x, y) in sheen:
        if y == top_row and (x + level) % 3 == 0:
            continue  # ragged upper edge, no straight line
        a = 36 if y == top_row else 50 + 8 * ((x // 2 + y) % 2)
        px[x, y] = GLOW + (a + 14 * groups.get((x, y), 0),)
    if level == 3:
        for (x, y) in ((6, 10), (7, 10), (9, 12), (10, 12)):  # warm glints where the sheen is strongest
            px[x, y] = GLOW + (120,)
    if phase >= 12:
        crack(px, phase - 11)
    return img


# crack paths (diagonal steps like the turtle egg), each step adds a branch
CRACK_SIDE = [[(7, 6), (8, 7), (8, 8), (7, 9)],
              [(7, 10), (6, 11), (9, 8), (10, 9)],
              [(5, 12), (11, 10), (6, 5), (8, 11), (9, 12)]]
CRACK_TOP = [[(7, 7), (8, 8), (8, 9)], [(6, 9), (9, 6)], [(5, 10), (10, 5), (6, 6)]]


def crack(px, step, top=False):
    lines = CRACK_TOP if top else CRACK_SIDE
    cells = [c for group in lines[:step] for c in group]
    for i, (x, y) in enumerate(cells):  # the line changes tone every 2 px (A8d 4)
        px[x, y] = (46, 32, 22, 255) if (i // 2) % 2 == 0 else (64, 46, 32, 255)
    for i, (x, y) in enumerate(cells):  # pale lip on the upper left of the line, translucent so it lightens the shell's own tone
        for (lx, ly) in ((x - 1, y), (x, y - 1)):
            if (lx, ly) not in cells and 0 <= lx < 16 and 0 <= ly < 16 and px[lx, ly][3] < 200:
                px[lx, ly] = (255, 250, 232, 110 if i % 3 else 80)
                break


def overlay_top(step):
    """Lid overlay: 0 = nothing, 1..3 = cracks."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    if step:
        crack(img.load(), step, top=True)
    return img


# ---------------------------------------------------------------- model
FACES = ('down', 'up', 'north', 'south', 'west', 'east')


def egg_parent_model():
    """Four tiers; sides use #side/#overlay, lids use #top/#overlay_top. The bottom face lies on the ground (cullface down)."""
    elements = []
    for side, lid, grow in (('#side', '#top', 0.0), ('#overlay', '#overlay_top', 0.02)):
        for (x0, y0, z0), (x1, y1, z1) in TIERS:
            lo = [max(0.0, round(v - grow, 3)) for v in (x0, y0, z0)]
            hi = [round(v + grow, 3) for v in (x1, y1, z1)]
            faces = {}
            for f in FACES:
                faces[f] = {'texture': lid if f in ('up', 'down') else side}
                if f == 'down' and y0 == 0:
                    faces[f]['cullface'] = 'down'
            elements.append({'from': lo, 'to': hi, 'faces': faces})
    return {'parent': 'minecraft:block/block', 'textures': {'particle': '#side'}, 'elements': elements}
