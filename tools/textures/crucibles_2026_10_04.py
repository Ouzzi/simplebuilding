"""Usage: python tools/textures/crucibles_2026_10_04.py <vanilla textures dir> [preview.png]

Crucible textures for SimpleLib (plan PLAN-CRUCIBLE-2026-10-04, section 4). Derived from the Vanilla
cauldron (side/top/bottom/inner keep its shading ranks), with each tier getting its own shape detail
instead of a plain recolor (owner feedback "eigene Formen statt Recolors"):
  iron       - cauldron iron, a riveted belt band across the side, light rim
  reinforced - iron with a cracked-diamond band and diamond rivets (SimpleBuilding's reinforced tier)
  netherite  - netherite-block ramp, a gold-trimmed band and gold rivets
Handles (the two grips) are a separate 16x16 texture per tier. Output goes to
modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures/block/.
"""
import os
import sys
from PIL import Image, ImageDraw

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'modules', 'simplecrucibles', 'shared', 'resources',
                   'assets', 'simplecrucibles', 'textures', 'block')


def load(name):
    return Image.open(os.path.join(V, 'block', name + '.png')).convert('RGBA').crop((0, 0, 16, 16))


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def ramp_of(name):
    return sorted({p[:3] for p in load(name).get_flattened_data() if p[3] > 0}, key=lum)


def remap(img, ramp):
    """Replace every opaque pixel by the ramp tone of the same luminance rank."""
    tones = sorted({p[:3] for p in img.get_flattened_data() if p[3] > 0}, key=lum)
    lo, hi = lum(tones[0]), lum(tones[-1])
    out = img.copy()
    px = out.load()
    for y in range(16):
        for x in range(16):
            p = px[x, y]
            if p[3] == 0:
                continue
            t = 0 if hi == lo else (lum(p) - lo) / (hi - lo)
            r = ramp[min(len(ramp) - 1, int(round(t * (len(ramp) - 1))))]
            px[x, y] = (*r, p[3])
    return out


IRON = ramp_of('iron_block')
NETHERITE = ramp_of('netherite_block')
DIAMOND = [(26, 120, 132), (44, 182, 190), (94, 228, 214), (186, 250, 240)]
GOLD = [(122, 82, 20), (196, 146, 34), (246, 208, 76)]


def band(img, y, colors, rivets, rivet_color):
    """A two-pixel belt band at row y with rivets every few pixels (the tier's shape detail)."""
    px = img.load()
    for x in range(16):
        if px[x, y][3]:
            px[x, y] = (*colors[1], 255)
        if px[x, y + 1][3]:
            px[x, y + 1] = (*colors[0], 255)
    for x in rivets:
        px[x, y] = (*rivet_color, 255)


def rim(img, color):
    px = img.load()
    for x in range(16):
        if px[x, 0][3]:
            px[x, 0] = (*color, 255)


def handle(colors, accent):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], fill=(*colors[1], 255))
    d.rectangle([0, 0, 15, 1], fill=(*colors[-1], 255))
    d.rectangle([0, 14, 15, 15], fill=(*colors[0], 255))
    for x in (3, 12):
        d.point((x, 7), fill=(*accent, 255))
    return img



# VN12 (TX22-1, 2026-10-23): candidates for the iron handle, own pixels in named steps (layer sources); not used by
# tier() until the owner picks a letter (docs/ai/PLAN-A-VN12-2026-10-23.md). The model shows only rows 5-6 of
# columns 6-9 (outer face), column 0/15 rows 5-6 (ends) and rows 6-9 (top/bottom), so those carry the shape;
# the rest is a plain plate for the editor. Tones dark -> light.
HANDLE_RAMPS = {'A': ('1f1e23', '2d2c32', '3f3e45', '57565e', '76757c'),     # cast iron, cauldron family
                'B': ('3a3a3e', '5c5c62', '86868c', 'b2b2b6', 'dcdcdc'),     # bright iron like bars / chain
                'C': ('34343a', '55555c', '7c7c84', 'a6a6ac', 'd2d2d4')}     # forged: notches and rivet heads


def handle_vn12(variant='A'):
    from layers import snap  # tools/textures is the script folder
    k, d, m, l, h = [tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,) for c in HANDLE_RAMPS[variant]]
    img = Image.new('RGBA', (16, 16), m)
    px = img.load()
    for x in range(16):
        px[x, 0], px[x, 15] = l, d
    snap(img, 'Grundform')
    for x in range(16):           # the grip bar: lit top row, shaded bottom row
        px[x, 5], px[x, 6] = l, d
    for y in range(6, 10):        # top/bottom faces of both lugs (columns 0 and 15)
        px[0, y] = px[15, y] = l
    px[0, 6] = px[15, 6] = m
    px[0, 5], px[15, 5] = m, m    # ends sit a little darker
    snap(img, 'Griff')
    if variant == 'A':
        px[6, 5], px[7, 5], px[9, 6] = h, h, k
    elif variant == 'B':
        px[6, 5], px[7, 5], px[8, 5], px[9, 6], px[6, 6] = h, h, h, k, m
        px[0, 7] = px[15, 7] = h
    else:
        px[8, 5], px[8, 6] = m, k     # a forged notch between two lit bulges
        px[6, 5], px[7, 5], px[9, 5], px[9, 6] = h, l, h, k
        for y in (7, 9):
            px[0, y] = px[15, y] = m
    snap(img, 'Licht')
    return img

def solid(img):
    """The cauldron's bottom has a hole (legs); a crucible sits flat, so fill it with the inner floor."""
    floor = load('cauldron_inner')
    out = floor.copy()
    out.alpha_composite(img)
    return out


def tier(name):
    side, top, bottom, inner = (load('cauldron_' + n) for n in ('side', 'top', 'bottom', 'inner'))
    bottom = solid(bottom)
    if name == 'netherite':
        side, top, bottom, inner = (remap(i, NETHERITE) for i in (side, top, bottom, inner))
        band(side, 9, [NETHERITE[1], GOLD[1]], (2, 7, 8, 13), GOLD[2])
        rim(side, GOLD[1])
        h = handle(NETHERITE, GOLD[2])
    elif name == 'reinforced':
        band(side, 9, [DIAMOND[0], DIAMOND[2]], (2, 7, 8, 13), DIAMOND[3])
        rim(side, DIAMOND[1])
        h = handle(ramp_of('cauldron_side'), DIAMOND[2])
    else:
        cauldron = ramp_of('cauldron_side')
        band(side, 9, [cauldron[1], cauldron[-2]], (2, 7, 8, 13), cauldron[-1])
        rim(side, cauldron[-1])
        h = handle(cauldron, cauldron[-1])
    return {'side': side, 'top': top, 'bottom': bottom, 'inner': inner, 'handle': h}


def main():
    os.makedirs(OUT, exist_ok=True)
    sheets = []
    for name in ('iron', 'reinforced', 'netherite'):
        tex = tier(name)
        for part, img in tex.items():
            img.save(os.path.join(OUT, f'{name}_crucible_{part}.png'))
        sheets.append((name, tex))
    if PREVIEW:
        scale = 12
        w = 5 * 16 * scale + 6 * 8
        prev = Image.new('RGBA', (w, len(sheets) * (16 * scale + 40) + 10), (40, 40, 40, 255))
        d = ImageDraw.Draw(prev)
        for row, (name, tex) in enumerate(sheets):
            y = 10 + row * (16 * scale + 40)
            d.text((8, y), f"{'ABC'[row]}: {name}_crucible (side, top, bottom, inner, handle)", fill=(255, 255, 255, 255))
            for col, part in enumerate(('side', 'top', 'bottom', 'inner', 'handle')):
                big = tex[part].resize((16 * scale, 16 * scale), Image.NEAREST)
                prev.paste(big, (8 + col * (16 * scale + 8), y + 16), big)
        os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
        prev.save(PREVIEW)


if __name__ == '__main__':
    main()
