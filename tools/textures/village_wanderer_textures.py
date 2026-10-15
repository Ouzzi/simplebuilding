"""Village Wanderer textures (2026-10-15, docs/ai/KONZEPT-DORF-WANDERER-2026-10-15.md section 7, owner answers section 11).

Own pixel art, no Vanilla pixels: the Vanilla Wandering Trader sheet was only a layout reference (the UV islands of
VillagerModel), nothing is copied or recoloured. Light from the top left, nothing painted on the outer pixel ring of a
16x16 sprite, no dark corner fills, no soft scaling.

* textures/entity/village_wanderer.png (64x64, VillagerModel UV layout): felt hat with a wide brim and a hood that hangs
  over the shoulders, warm ochre travel cloak with rust trim, green scarf with a small brass bell, bedroll and pouch on
  the back, leather boots, a short grey beard.
* textures/item/village_wanderer_spawn_egg.png (16x16): ochre egg with a green scarf band and a brass bell.

Writes modules/simplewanderers/shared/resources/assets/simplewanderers/textures/... and previews (front and back view
built from the same faces, unwrapped sheet, egg) to docs/ai/previews/oc-wanderers/.
Run: python3 tools/textures/village_wanderer_textures.py [--check]
"""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', '..', 'modules', 'simplewanderers', 'shared', 'resources', 'assets', 'simplewanderers', 'textures')
PREVIEW = os.environ.get('SB_PREVIEW_DIR', os.path.join(HERE, '..', '..', 'docs', 'ai', 'previews', 'oc-wanderers'))

# Ramps, dark to light.
OCHRE = [(88, 52, 22), (126, 76, 30), (164, 104, 42), (198, 136, 62), (226, 170, 92)]
RUST = [(70, 30, 18), (104, 44, 24), (142, 62, 32), (176, 88, 44)]
GREEN = [(24, 62, 36), (38, 92, 50), (60, 126, 66), (98, 164, 88)]
BRASS = [(122, 80, 18), (190, 140, 34), (240, 200, 80), (255, 240, 164)]
SKIN = [(122, 78, 56), (160, 106, 76), (196, 140, 100), (222, 170, 128)]
LEATHER = [(40, 26, 16), (66, 42, 24), (96, 64, 36), (128, 90, 52)]
CANVAS = [(120, 100, 66), (160, 138, 94), (196, 176, 130), (226, 210, 168)]
BEARD = [(120, 112, 104), (160, 152, 142), (198, 190, 180)]
WHITE = (236, 236, 228)
EYE = (46, 70, 40)

CLEAR = (0, 0, 0, 0)


def rgba(c):
    return c if len(c) == 4 else c + (255,)


class Sheet:
    def __init__(self, w=64, h=64):
        self.img = Image.new('RGBA', (w, h), CLEAR)
        self.px = self.img.load()

    def put(self, x, y, c):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            self.px[x, y] = rgba(c)

    def rect(self, x0, y0, x1, y1, c):
        """Fills [x0, x1) x [y0, y1)."""
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.put(x, y, c)

    def cloth(self, x0, y0, x1, y1, ramp, base=2, seed=0):
        """Flat colour with a sparse, deterministic weave: every 4th pixel steps one shade up or down."""
        for y in range(y0, y1):
            for x in range(x0, x1):
                n = (x * 7 + y * 13 + seed * 5 + (x ^ y) * 3) % 11
                shade = base + (1 if n == 0 else -1 if n == 5 else 0)
                self.put(x, y, ramp[max(0, min(len(ramp) - 1, shade))])

    def clear(self, x0, y0, x1, y1):
        self.rect(x0, y0, x1, y1, CLEAR)


def faces(u, v, w, h, d):
    """The six unwrapped faces of a Minecraft box (texOffs u, v; size w x h x d) as (x0, y0, x1, y1)."""
    return {
        'top': (u + d, v, u + d + w, v + d),
        'bottom': (u + d + w, v, u + d + 2 * w, v + d),
        'right': (u, v + d, u + d, v + d + h),
        'front': (u + d, v + d, u + d + w, v + d + h),
        'left': (u + d + w, v + d, u + 2 * d + w, v + d + h),
        'back': (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def shade_edges(s, box, ramp, light=True, dark=True):
    """Light top row and left column, dark bottom row and right column (inside the box, one pixel)."""
    x0, y0, x1, y1 = box
    for x in range(x0, x1):
        if light:
            s.put(x, y0, ramp[3])
        if dark:
            s.put(x, y1 - 1, ramp[1])
    for y in range(y0, y1):
        if light and y > y0:
            s.put(x0, y, ramp[3])
        if dark and y < y1 - 1:
            s.put(x1 - 1, y, ramp[1])


# --- entity ----------------------------------------------------------------------------------------------------------------

def head(s):
    f = faces(0, 0, 8, 10, 8)
    # Hair under the hat: top, back and the upper part of the sides; skin below, ears at the sides.
    s.cloth(*f['top'], LEATHER, 1)
    s.cloth(*f['back'], LEATHER, 1, 2)
    s.rect(*f['bottom'], SKIN[0])
    for side in ('right', 'left'):
        x0, y0, x1, y1 = f[side]
        s.cloth(x0, y0, x1, y1, SKIN, 2, 3)
        s.rect(x0, y0, x1, y0 + 5, LEATHER[1])
        s.rect(x0, y0 + 5, x0 + 2, y0 + 6, LEATHER[1])
    # Ears.
    s.rect(f['right'][0] + 3, f['right'][1] + 6, f['right'][0] + 5, f['right'][1] + 8, SKIN[3])
    s.rect(f['left'][0] + 3, f['left'][1] + 6, f['left'][0] + 5, f['left'][1] + 8, SKIN[3])
    # Face: shadow under the brim, brows, green eyes, cheeks, a short grey beard.
    x0, y0, x1, y1 = f['front']
    s.cloth(x0, y0, x1, y1, SKIN, 2, 1)
    s.rect(x0, y0, x1, y0 + 4, LEATHER[1])
    s.rect(x0, y0 + 4, x1, y0 + 5, RUST[0])
    for ex, white, pupil in ((x0 + 1, x0 + 1, x0 + 2), (x0 + 5, x0 + 6, x0 + 5)):
        s.rect(ex, y0 + 5, ex + 2, y0 + 6, RUST[1])
        s.put(white, y0 + 6, WHITE)
        s.put(pupil, y0 + 6, EYE)
    s.put(x0 + 1, y0 + 7, SKIN[1])
    s.put(x0 + 6, y0 + 7, SKIN[1])
    s.rect(x0 + 1, y0 + 8, x1 - 1, y0 + 10, BEARD[1])
    s.rect(x0 + 2, y0 + 9, x1 - 2, y0 + 10, BEARD[0])
    s.put(x0 + 1, y0 + 8, BEARD[2])
    s.put(x0 + 6, y0 + 8, BEARD[2])
    # Nose (24, 0) 2x4x2.
    n = faces(24, 0, 2, 4, 2)
    s.rect(*n['front'], SKIN[2])
    s.rect(n['front'][0], n['front'][1] + 3, n['front'][2], n['front'][3], SKIN[1])
    s.put(n['front'][0], n['front'][1], SKIN[3])
    for side in ('right', 'left'):
        s.rect(*n[side], SKIN[1])
    s.rect(*n['top'], SKIN[3])
    s.rect(*n['bottom'], SKIN[0])
    s.rect(*n['back'], SKIN[1])


def hat(s):
    f = faces(32, 0, 8, 10, 8)
    # Crown (rows 0-3 of the 10), band (row 4 = brim height), hood drape below.
    for name, box in f.items():
        x0, y0, x1, y1 = box
        if name == 'top':
            s.cloth(x0, y0, x1, y1, OCHRE, 3, 4)
            s.rect(x0 + 3, y0 + 1, x0 + 5, y1 - 1, OCHRE[2])
            s.rect(x0 + 3, y0 + 3, x0 + 5, y0 + 5, OCHRE[3])
            s.rect(x0, y0, x1, y0 + 1, OCHRE[4])
        elif name == 'bottom':
            s.rect(x0, y0, x1, y1, LEATHER[0])
        else:
            s.cloth(x0, y0, x1, y0 + 4, OCHRE, 3, 6)
            s.rect(x0, y0 + 4, x1, y0 + 5, RUST[2])
            s.rect(x0, y0 + 5, x1, y1, OCHRE[2])
            s.cloth(x0, y0 + 5, x1, y1, OCHRE, 2, 8)
            s.rect(x0, y0, x1, y0 + 1, OCHRE[4])
            s.rect(x0, y1 - 1, x1, y1, RUST[1])
    # Light left column and dark right column of each side face (hood drape).
    for name in ('right', 'front', 'left', 'back'):
        x0, y0, x1, y1 = f[name]
        for y in range(y0 + 5, y1 - 1):
            s.put(x0, y, OCHRE[3])
            s.put(x1 - 1, y, OCHRE[1])
    # Hood opening on the front: an arch through which the face shows (transparent), rust piping along its edge.
    x0, y0, x1, y1 = f['front']
    s.clear(x0 + 2, y0 + 5, x1 - 2, y1)
    s.clear(x0 + 2, y0 + 5, x0 + 3, y0 + 6)
    s.clear(x1 - 3, y0 + 5, x1 - 2, y0 + 6)
    s.put(x0 + 2, y0 + 5, RUST[2])
    s.put(x1 - 3, y0 + 5, RUST[2])
    for y in range(y0 + 6, y1 - 1):
        s.put(x0 + 1, y, RUST[2])
        s.put(x1 - 2, y, RUST[1])
    s.rect(x0 + 3, y0 + 5, x1 - 3, y0 + 6, RUST[2])
    # A hood point at the back.
    bx0, by0, bx1, by1 = f['back']
    s.rect(bx0 + 3, by0 + 6, bx0 + 5, by1 - 2, OCHRE[1])
    s.put(bx0 + 3, by0 + 6, OCHRE[3])

    # Brim (hat_rim, 30,47: 16x16x1): the front face is its top, the back face its underside. Rows run from the back of
    # the head (top of the face) to the front (bottom of the face). A rounded disc: the corners stay clear.
    r = faces(30, 47, 16, 16, 1)
    cx, cy = 7.5, 7.5
    for name, (x0, y0, x1, y1) in (('front', r['front']), ('back', r['back'])):
        for y in range(y0, y1):
            for x in range(x0, x1):
                dx, dy = x - x0 - cx, y - y0 - cy
                dist = (dx * dx + dy * dy) ** 0.5
                if dist > 8.05:
                    continue
                top = name == 'front'
                if dist > 7.0:
                    c = RUST[1] if top else LEATHER[1]
                elif dist > 6.0:
                    c = OCHRE[1] if top else LEATHER[2]
                else:
                    ramp = OCHRE if top else LEATHER
                    base = 3 if top else 2
                    c = ramp[base + (1 if (x * 5 + y * 3) % 9 == 0 else 0)]
                    if top and dx + dy < -4.5:
                        c = OCHRE[4]
                s.put(x, y, c)
    # The strips along the four edges of the 1-pixel box: only their middle shows, in the edge colour.
    for name, (x0, y0, x1, y1) in (('top', r['top']), ('bottom', r['bottom'])):
        s.rect(x0 + 4, y0, x1 - 4, y1, RUST[0])
    for name in ('right', 'left'):
        x0, y0, x1, y1 = r[name]
        s.rect(x0, y0 + 4, x1, y1 - 4, RUST[0])


def body(s):
    # Tunic under the cloak (mostly hidden): dark green with a belt.
    f = faces(16, 20, 8, 12, 6)
    for name, (x0, y0, x1, y1) in f.items():
        s.cloth(x0, y0, x1, y1, GREEN, 1, 2)
    for name in ('right', 'front', 'left', 'back'):
        x0, y0, x1, y1 = f[name]
        s.rect(x0, y0 + 8, x1, y0 + 9, LEATHER[1])
    x0, y0, x1, y1 = f['front']
    s.rect(x0 + 3, y0 + 8, x0 + 5, y0 + 9, BRASS[2])


def jacket(s):
    # Cloak (0, 38): 8 wide, 20 tall, 6 deep.
    f = faces(0, 38, 8, 20, 6)
    for name, (x0, y0, x1, y1) in f.items():
        if name == 'top':
            s.cloth(x0, y0, x1, y1, GREEN, 2, 5)
            s.rect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, GREEN[0])
            continue
        if name == 'bottom':
            s.rect(x0, y0, x1, y1, OCHRE[0])
            continue
        s.cloth(x0, y0, x1, y1, OCHRE, 2, 9)
        # Fabric folds: lighter vertical strips with a dark seam beside them.
        for fx in range(x0 + 1, x1 - 1, 3):
            for y in range(y0 + 6, y1 - 3):
                s.put(fx, y, OCHRE[3])
                s.put(fx + 1, y, OCHRE[1])
        # Scarf ring around the neck (rows 0-2), hem trim (last 2 rows) with a jagged edge.
        s.rect(x0, y0, x1, y0 + 3, GREEN[2])
        s.rect(x0, y0, x1, y0 + 1, GREEN[3])
        s.rect(x0, y0 + 2, x1, y0 + 3, GREEN[1])
        for x in range(x0, x1):
            s.put(x, y1 - 2, RUST[2])
            s.put(x, y1 - 1, RUST[1] if (x - x0) % 2 == 0 else RUST[0])
        # Light left edge, dark right edge.
        for y in range(y0 + 3, y1 - 2):
            s.put(x0, y, OCHRE[3])
            s.put(x1 - 1, y, OCHRE[1])
    # Front: centre seam, brass clasp, diagonal leather strap, belt and the scarf's hanging end with the bell.
    x0, y0, x1, y1 = f['front']
    for y in range(y0 + 3, y1 - 2):
        s.put(x0 + 4, y, OCHRE[0])
        s.put(x0 + 3, y, OCHRE[3])
    s.rect(x0 + 3, y0 + 3, x0 + 5, y0 + 4, BRASS[2])
    s.put(x0 + 3, y0 + 3, BRASS[3])
    for i in range(10):
        s.put(x0 + 1 + (i * 6) // 10 + (1 if i > 4 else 0), y0 + 4 + i, LEATHER[2])
    s.rect(x0, y0 + 12, x1, y0 + 14, LEATHER[1])
    s.rect(x0, y0 + 12, x1, y0 + 13, LEATHER[2])
    s.rect(x0 + 3, y0 + 12, x0 + 5, y0 + 14, BRASS[1])
    s.put(x0 + 3, y0 + 12, BRASS[3])
    s.rect(x0 + 5, y0 + 3, x0 + 7, y0 + 11, GREEN[2])
    s.rect(x0 + 5, y0 + 3, x0 + 6, y0 + 11, GREEN[3])
    s.rect(x0 + 6, y0 + 3, x0 + 7, y0 + 11, GREEN[1])
    for y in (y0 + 5, y0 + 8):
        s.rect(x0 + 5, y, x0 + 7, y + 1, GREEN[0])
    s.rect(x0 + 5, y0 + 11, x0 + 7, y0 + 13, BRASS[1])
    s.put(x0 + 5, y0 + 11, BRASS[3])
    s.put(x0 + 6, y0 + 12, BRASS[0])
    # Back: bedroll across the shoulders with two straps, a pouch low on the back.
    x0, y0, x1, y1 = f['back']
    s.rect(x0, y0 + 3, x1, y0 + 8, CANVAS[1])
    s.rect(x0, y0 + 3, x1, y0 + 4, CANVAS[3])
    s.rect(x0, y0 + 7, x1, y0 + 8, CANVAS[0])
    for x in range(x0 + 1, x1 - 1, 2):
        s.put(x, y0 + 5, CANVAS[2])
        s.put(x + 1, y0 + 6, CANVAS[0])
    s.rect(x0, y0 + 3, x0 + 1, y0 + 8, CANVAS[2])
    s.rect(x1 - 1, y0 + 3, x1, y0 + 8, CANVAS[0])
    for sx in (x0 + 2, x0 + 5):
        s.rect(sx, y0 + 3, sx + 1, y0 + 8, LEATHER[1])
        s.rect(sx, y0 + 8, sx + 1, y0 + 13, LEATHER[2])
        s.put(sx, y0 + 5, BRASS[1])
    s.rect(x0 + 2, y0 + 13, x0 + 6, y0 + 16, LEATHER[2])
    s.rect(x0 + 2, y0 + 13, x0 + 6, y0 + 14, LEATHER[3])
    s.rect(x0 + 5, y0 + 14, x0 + 6, y0 + 16, LEATHER[1])
    s.rect(x0 + 3, y0 + 14, x0 + 5, y0 + 15, LEATHER[1])
    s.put(x0 + 3, y0 + 14, BRASS[2])
    # Sides: a flap pocket.
    for name in ('right', 'left'):
        x0, y0, x1, y1 = f[name]
        s.rect(x0 + 2, y0 + 9, x0 + 5, y0 + 13, OCHRE[1])
        s.rect(x0 + 2, y0 + 9, x0 + 5, y0 + 10, RUST[2])
        s.rect(x0 + 2, y0 + 10, x0 + 3, y0 + 13, OCHRE[3])
        s.put(x0 + 3, y0 + 10, BRASS[2])


def arms(s):
    # Sleeves (44, 22): 4x8x4, ochre with a rust cuff.
    f = faces(44, 22, 4, 8, 4)
    for name, (x0, y0, x1, y1) in f.items():
        if name in ('top', 'bottom'):
            s.cloth(x0, y0, x1, y1, OCHRE, 2 if name == 'top' else 1, 3)
            continue
        s.cloth(x0, y0, x1, y1, OCHRE, 2, 4)
        s.rect(x0, y1 - 2, x1, y1, RUST[1])
        s.rect(x0, y1 - 2, x1, y1 - 1, RUST[2])
        for y in range(y0, y1 - 2):
            s.put(x0, y, OCHRE[3])
            s.put(x1 - 1, y, OCHRE[1])
    # Crossed hands (40, 38): 8x4x4, sleeve ends and two hands in the middle.
    h = faces(40, 38, 8, 4, 4)
    for name, (x0, y0, x1, y1) in h.items():
        if name in ('top', 'bottom'):
            s.cloth(x0, y0, x1, y1, OCHRE, 2, 7)
        else:
            s.cloth(x0, y0, x1, y1, OCHRE, 2, 7)
    x0, y0, x1, y1 = h['front']
    s.rect(x0, y0, x0 + 2, y1, OCHRE[2])
    s.rect(x1 - 2, y0, x1, y1, OCHRE[2])
    s.rect(x0 + 2, y0, x1 - 2, y1, SKIN[2])
    s.rect(x0 + 2, y0, x1 - 2, y0 + 1, SKIN[3])
    s.rect(x0 + 2, y1 - 1, x1 - 2, y1, SKIN[1])
    s.put(x0 + 4, y0 + 1, SKIN[1])
    s.put(x0 + 4, y0 + 2, SKIN[1])
    s.rect(x0 + 2, y0, x0 + 3, y1, RUST[2])
    s.rect(x1 - 3, y0, x1 - 2, y1, RUST[1])
    for name in ('right', 'left'):
        bx0, by0, bx1, by1 = h[name]
        s.cloth(bx0, by0, bx1, by1, OCHRE, 2, 2)


def legs(s):
    # Leg (0, 22): 4x12x4 - only the lower four rows peek out under the cloak: boots with laces and a cuff.
    f = faces(0, 22, 4, 12, 4)
    for name, (x0, y0, x1, y1) in f.items():
        if name in ('top', 'bottom'):
            s.rect(x0, y0, x1, y1, LEATHER[1])
            continue
        s.cloth(x0, y0, x1, y1, GREEN, 1, 3)
        s.rect(x0, y1 - 5, x1, y1, LEATHER[2])
        s.cloth(x0, y1 - 5, x1, y1, LEATHER, 2, 4)
        s.rect(x0, y1 - 6, x1, y1 - 5, RUST[2])
        s.rect(x0, y1 - 1, x1, y1, LEATHER[0])
        s.put(x0, y1 - 4, LEATHER[3])
    x0, y0, x1, y1 = f['front']
    for y in (y1 - 4, y1 - 3, y1 - 2):
        s.put(x0 + 1, y, LEATHER[3] if y % 2 == 0 else LEATHER[1])
        s.put(x0 + 2, y, LEATHER[1] if y % 2 == 0 else LEATHER[3])


def entity():
    s = Sheet()
    head(s)
    hat(s)
    body(s)
    jacket(s)
    arms(s)
    legs(s)
    return s.img


# --- spawn egg -------------------------------------------------------------------------------------------------------------

def egg():
    s = Sheet(16, 16)
    cx, cy = 7.5, 8.0
    rows = {}
    for y in range(1, 15):
        t = (y - cy) / 7.0
        half = 5.0 * (1 - t * t) ** 0.5 * (1.0 - 0.12 * t)  # a little narrower at the top
        rows[y] = half
    for y, half in rows.items():
        for x in range(16):
            dx = abs(x - cx)
            if dx > half:
                continue
            edge = dx > half - 1.0 or y in (1, 14)
            if edge:
                c = RUST[0] if (x + y) % 3 else RUST[1]
            else:
                lit = (x - cx) * -0.7 + (y - cy) * -0.7
                shade = 2 if lit < 1 else 3
                if lit > 3.2:
                    shade = 4
                if lit < -3.2:
                    shade = 1
                c = OCHRE[shade]
            s.put(x, y, c)
    # Green scarf band across the lower third, brass bell on it.
    for x in range(16):
        for y in (9, 10):
            if abs(x - cx) <= rows[y] - 1.0:
                s.put(x, y, GREEN[2] if y == 9 else GREEN[1])
    for x in range(16):
        y = 9
        if abs(x - cx) <= rows[y] - 1.0 and x < 6:
            s.put(x, y, GREEN[3])
    s.rect(9, 11, 11, 13, BRASS[1])
    s.put(9, 11, BRASS[3])
    s.put(10, 12, BRASS[0])
    s.put(10, 11, BRASS[2])
    # Brass speckles on the shoulders.
    for x, y in ((5, 4), (9, 5), (7, 3)):
        s.put(x, y, BRASS[2])
    return s.img


def outputs():
    return {'entity/village_wanderer.png': entity(), 'item/village_wanderer_spawn_egg.png': egg()}


# --- previews --------------------------------------------------------------------------------------------------------------

def backdrop(w, h, cell):
    img = Image.new('RGBA', (w, h), (198, 198, 198, 255))
    px = img.load()
    for y in range(h):
        for x in range(w):
            if ((x // cell) + (y // cell)) % 2:
                px[x, y] = (186, 186, 186, 255)
    return img


def doll(img, back):
    """A flat paper-doll of the wanderer (front or back), built from the same faces: not the in-game render, a check of the art."""
    out = Image.new('RGBA', (16, 28), CLEAR)

    def blit(box, at):
        out.alpha_composite(img.crop(box), at)

    hf = faces(0, 0, 8, 10, 8)
    af = faces(32, 0, 8, 10, 8)
    jf = faces(0, 38, 8, 20, 6)
    leg = faces(0, 22, 4, 12, 4)
    arm = faces(44, 22, 4, 8, 4)
    hand = faces(40, 38, 8, 4, 4)
    bl = faces(30, 47, 16, 16, 1)
    if not back:
        blit(hf['front'], (4, 4))
        blit(faces(24, 0, 2, 4, 2)['front'], (7, 8))
        blit(af['front'], (4, 4))
    else:
        blit(hf['back'], (4, 4))
        blit(af['back'], (4, 4))
    # The brim seen from the front: a one-pixel line at the forehead.
    for bx in range(16):
        out.putpixel((bx, 7), rgba(RUST[0] if bx in (0, 15) else OCHRE[1] if bx in (1, 14) else OCHRE[2]))
    jb = jf['back'] if back else jf['front']
    blit(jb, (4, 14))
    sleeve = arm['back'] if back else arm['front']
    blit(sleeve, (0, 14))
    blit(sleeve, (12, 14))
    if not back:
        blit(hand['front'], (4, 18))
    lf = leg['back'] if back else leg['front']
    out.alpha_composite(img.crop((lf[0], lf[3] - 4, lf[2], lf[3])), (4, 24))
    out.alpha_composite(img.crop((lf[0], lf[3] - 4, lf[2], lf[3])), (8, 24))
    return out


def preview_sheet(items):
    scale = 8
    img = outputs()['entity/village_wanderer.png']
    egg_img = outputs()['item/village_wanderer_spawn_egg.png']
    sheet = Image.new('RGBA', (64 * scale + 16 * 16 * 2 + 16 * 8 + 64, 64 * scale), (198, 198, 198, 255))
    bg = backdrop(64 * scale, 64 * scale, 8)
    bg.alpha_composite(img.resize((64 * scale, 64 * scale), Image.Resampling.NEAREST))
    sheet.paste(bg, (0, 0))
    x = 64 * scale + 16
    for back in (False, True):
        d = doll(img, back).resize((16 * 16, 28 * 16), Image.Resampling.NEAREST)
        cell = backdrop(16 * 16, 28 * 16, 16)
        cell.alpha_composite(d)
        sheet.paste(cell, (x, 8))
        x += 16 * 16 + 8
    e = backdrop(16 * 8, 16 * 8, 8)
    e.alpha_composite(egg_img.resize((16 * 8, 16 * 8), Image.Resampling.NEAREST))
    sheet.paste(e, (x + 8, 8))
    return sheet


def main():
    check = '--check' in sys.argv
    stale = []
    for name, img in outputs().items():
        path = os.path.join(ASSETS, name)
        if check:
            if not os.path.exists(path) or Image.open(path).convert('RGBA').tobytes() != img.tobytes():
                stale.append(path)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
    if check:
        if stale:
            raise SystemExit('stale: ' + ', '.join(stale))
        print('village wanderer textures: up to date')
        return
    os.makedirs(PREVIEW, exist_ok=True)
    preview_sheet(outputs()).save(os.path.join(PREVIEW, 'village_wanderer_8x16x.png'))
    print('village wanderer textures written, preview in', os.path.normpath(PREVIEW))


if __name__ == '__main__':
    main()
