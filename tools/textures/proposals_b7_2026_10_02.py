"""Usage: python tools/textures/proposals_b7_2026_10_02.py <vanilla textures dir> <preview dir>

Backlog B7b (owner 2026-10-01): Astral/Nihil Redstone powder, lamps and switches redrawn in vanilla's language.
Each built from the vanilla counterpart (redstone dust, redstone lamp, polished blackstone plate) remapped by
brightness rank onto an Astralit (pink-violet) or Nihilith (teal) ramp; off and on states. Three proposals each."""
import os
import sys

from PIL import Image, ImageDraw

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
OUT = sys.argv[2] if len(sys.argv) > 2 else 'build/proposals-b7/'
RAMPS = {
    'astral': [(38, 18, 34), (74, 36, 64), (122, 60, 104), (176, 98, 152), (226, 156, 204), (255, 214, 238)],
    'nihil': [(12, 28, 34), (24, 52, 62), (48, 104, 116), (88, 158, 170), (156, 220, 226), (214, 250, 250)],
}


def load(path):
    return Image.open(os.path.join(V, path + '.png')).convert('RGBA').crop((0, 0, 16, 16))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def remap(src, ramp, lo=0, hi=None):
    """Every distinct colour of src -> a ramp tone by brightness rank, spread over ramp[lo..hi]."""
    hi = len(ramp) - 1 if hi is None else hi
    cols = sorted({src.getpixel((x, y))[:3] for y in range(16) for x in range(16) if src.getpixel((x, y))[3]}, key=lum)
    idx = {c: lo + round(i * (hi - lo) / max(1, len(cols) - 1)) for i, c in enumerate(cols)}
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), ramp[idx[p[:3]]] + (p[3],))
    return out


def tint(gray, ramp, lo, hi):
    """Grey dust (vanilla tints it in code) -> ramp by its grey level."""
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            p = gray.getpixel((x, y))
            if p[3]:
                k = lo + round(p[0] / 255 * (hi - lo))
                out.putpixel((x, y), ramp[k] + (255,))
    return out


def cross():
    dot, line = load('block/redstone_dust_dot'), load('block/redstone_dust_line0')
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    im.alpha_composite(line)
    im.alpha_composite(line.rotate(90))
    im.alpha_composite(dot)
    return im


def powder(kind, active, variant):
    ramp = RAMPS[kind]
    lo, hi = (2, 5) if active else (1, 3)
    if variant == 'A':      # the vanilla dot alone
        return tint(load('block/redstone_dust_dot'), ramp, lo, hi)
    if variant == 'B':      # dot with the four arms, like vanilla dust connecting everywhere
        return tint(cross(), ramp, lo, hi)
    im = tint(load('block/redstone_dust_dot'), ramp, lo, hi)  # C: dot plus scattered grains
    for x, y in ((2, 3), (12, 2), (3, 12), (13, 11), (8, 14), (1, 7), (14, 6)):
        im.putpixel((x, y), ramp[hi - 1] + (255,))
    return im


def lamp(kind, active, variant):
    ramp = RAMPS[kind]
    src = load('block/redstone_lamp_on' if active else 'block/redstone_lamp')
    if variant == 'A':      # vanilla lamp, recoloured
        return remap(src, ramp, 0, 5 if active else 3)
    if variant == 'B':      # recoloured, the frame kept in end-stone greys so the glass glows more
        im = remap(src, ramp, 0, 5 if active else 3)
        for i in range(16):
            for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
                im.putpixel((x, y), (60, 56, 66, 255) if (x + y) % 2 else (78, 74, 86, 255))
        return im
    im = remap(src, ramp, 1 if active else 0, 5 if active else 2)  # C: brighter on, darker off (more contrast)
    return im


def switch(kind, active, variant):
    ramp = RAMPS[kind]
    plate = remap(load('block/polished_blackstone'), [(30, 26, 34), (44, 38, 50), (58, 52, 66), (72, 66, 82)])
    im = plate.copy()
    gem = ramp[4] if active else ramp[2]
    glow = ramp[5] if active else ramp[3]
    dark = ramp[1]
    if variant == 'A':      # a round gem set in the middle
        cells = {(7, 6): glow, (8, 6): gem, (6, 7): glow, (7, 7): gem, (8, 7): gem, (9, 7): dark, (6, 8): gem,
                 (7, 8): gem, (8, 8): dark, (9, 8): dark, (7, 9): dark, (8, 9): dark}
    elif variant == 'B':    # a lever-like slot with a crystal knob that slides up when on
        cells = {(7, y): (20, 18, 24) for y in range(4, 12)}
        cells.update({(8, y): (20, 18, 24) for y in range(4, 12)})
        top = 4 if active else 8
        for x in (6, 7, 8, 9):
            for y in (top, top + 1, top + 2):
                cells[(x, y)] = glow if (x < 8 and y == top) else gem if x < 9 else dark
    else:                   # C: four studs in the corners plus the gem, lit studs when on
        cells = {}
        for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
            cells[(x, y)] = glow if active else dark
        cells.update({(7, 7): glow, (8, 7): gem, (7, 8): gem, (8, 8): dark})
    for (x, y), c in cells.items():
        im.putpixel((x, y), c + (255,))
    # bevel: light top/left edge, dark bottom/right edge like vanilla plates
    for i in range(16):
        im.putpixel((i, 0), (86, 80, 96, 255))
        im.putpixel((0, i), (86, 80, 96, 255))
        im.putpixel((i, 15), (18, 16, 22, 255))
        im.putpixel((15, i), (18, 16, 22, 255))
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    rows = []
    for kind in ('astral', 'nihil'):
        for name, fn in (('Pulver', powder), ('Lampe', lamp), ('Schalter', switch)):
            rows.append((f'{kind} {name}', [(v, fn(kind, False, v), fn(kind, True, v)) for v in 'ABC']))
    cell = 100
    s = Image.new('RGBA', (150 + 3 * (2 * cell + 20), len(rows) * (cell + 20) + 30), (139, 139, 139, 255))
    d = ImageDraw.Draw(s)
    for i in range(3):
        d.text((150 + i * (2 * cell + 20) + 60, 8), f'{"ABC"[i]} (aus / an)', fill=(255, 255, 255))
    for r, (label, items) in enumerate(rows):
        y = 26 + r * (cell + 20)
        d.text((6, y + 40), label, fill=(255, 255, 255))
        for i, (v, off, on) in enumerate(items):
            x = 150 + i * (2 * cell + 20)
            s.alpha_composite(off.resize((96, 96), Image.NEAREST), (x, y))
            s.alpha_composite(on.resize((96, 96), Image.NEAREST), (x + cell, y))
    s.save(os.path.join(OUT, 'astral-nihil-redstone.png'))
    print('ok ->', OUT)


if __name__ == '__main__':
    main()
