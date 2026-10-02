"""Usage: python tools/textures/proposals_v5_2026_10_02.py <vanilla textures dir> <preview dir>

Round 5 (owner feedback on round 4, 2026-10-02):
- Erzdetektor: round-4 B with the top-left rim fixed; the 32 needle frames drawn in its tilted view (GIF).
- Attraktor: round-4 shape, iron magnet with redstone red and lapis blue laid on (vanilla item palettes).
- Amethyst-Resonanzstab: head between round-4 B and C, the shaft a bit thicker (3 px).
- Eisenkern: three more takes on round-4 C, each for iron and copper.
Writes uebersicht.png, erzdetektor.gif and the 16x16 candidates into <preview dir>/png/."""
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposals_v3_2026_10_02 as v3  # noqa: E402
import proposals_v4_2026_10_02 as v4  # noqa: E402
from proposals_v3_2026_10_02 import AME, IRON, blank, grid, over, px, tex  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
OUT = sys.argv[2] if len(sys.argv) > 2 else 'build/proposals-v5/'
v3.V = V
ROOT = os.path.join(HERE, '..', '..')

# ================================================================== Erzdetektor
DETECTOR_DIAL = [
    '................',
    '................',
    '....AAAAAAAA....',
    '..AABBBCCBBBAA..',
    '.ABBGGDDDDDDEEF.',
    '.ABGDDDDDDDDDHF.',
    'ABDDDDDDDDDDDDHF',
    'ABDDDDDIIDDDDDHF',
    'ABDDDDDDDDDDDJHF',
    'AEEDDDDDDDDDJHHF',
    '.AEEEHHDDHHHHkF.',
    '.FgkkEHHHHHHkgF.',
    '..FFgkKKKKkgFF..',
    '....FFFFFFFF....',
    '................',
    '................',
]
# Centred 2026-10-02 (owner: the needle did not look centred): the dial is symmetric about x = 7.5, so the hub is
# two pixels wide (7|8, 7) and the needle turns about the point between them; frames 17..31 mirror 15..1.
# Wider 2026-10-02 (owner: like the vanilla recovery compass): the needle is drawn as one 4-connected stroke - a
# diagonal step gets a filler pixel on the side nearer the true line, as vanilla's recovery compass does - and the
# grey tail is two pixels long.
PIVOT = (8.0, 7.5)                  # in pixel-edge coordinates: between columns 7 and 8, middle of row 7
HUB = [(7, 7), (8, 7)]
NEEDLE_RX, NEEDLE_RY = 4.6, 2.9     # the lid is seen tilted: tips land on x = 3 / 12 and y = 4 / 10
TAIL_RX, TAIL_RY = 2.4, 2.0         # two tail pixels each way
MARKS = [(7, 4), (8, 4), (2, 7), (13, 7), (7, 10), (8, 10)]
TAIL = (92, 86, 100)


def detector_dial():
    return grid(DETECTOR_DIAL, v3.DET)


def _stroke(start, ux, uy, rx, ry):
    """Cells from the hub cell `start` outwards along (ux*rx, uy*ry), 4-connected, hub excluded."""
    cx, cy = PIVOT
    cell = lambda u, v: (math.floor(u - 1e-6), math.floor(v))
    lx, ly = ux * rx, uy * ry
    norm = math.hypot(lx, ly) or 1.0

    def off_line(c):  # distance of a cell centre from the ideal needle line
        return abs((c[0] + 0.5 - cx) * ly - (c[1] + 0.5 - cy) * lx) / norm

    out, last = [], start
    for i in range(1, 33):
        c = cell(cx + lx * i / 32, cy + ly * i / 32)
        if c == last or c in HUB:
            continue
        if c[0] != last[0] and c[1] != last[1]:
            a, b = (c[0], last[1]), (last[0], c[1])
            filler = a if off_line(a) <= off_line(b) else b
            if filler not in HUB and filler not in out:
                out.append(filler)
        if c not in out:
            out.append(c)
        last = c
    return out


def needle_path(frame):
    """(head cells from the hub to the tip, tail cells from the hub outwards) of a needle frame, one column for the
    vertical frames (see needle_steps for the drawn, two-column version)."""
    frame %= 32
    if frame > 16:
        head, tail = needle_path(32 - frame)
        return [(15 - x, y) for x, y in head], [(15 - x, y) for x, y in tail]
    a = frame / 32 * 2 * math.pi
    dx, dy = -math.sin(a), math.cos(a)
    head = _stroke(HUB[0], dx, dy, NEEDLE_RX, NEEDLE_RY)
    tail = _stroke(HUB[1] if -dx > 1e-9 else HUB[0], -dx, -dy, TAIL_RX, TAIL_RY)
    return head, tail


# Straight up / down (frames 16 and 0) the needle is two pixels wide, columns 7 and 8, so it sits exactly on the dial
# centre x = 7.5 (owner 2026-10-02: at 12 and 6 o'clock it was half a pixel off). Vanilla's recovery compass solves the
# same problem the other way round - its dial is built round a single pivot pixel and the vertical needle runs in that
# pixel's column, with a darker side pixel at the base; our dial is symmetric about x = 7.5, so the needle takes both
# middle columns, the right one a shade darker (light from the top left, as on the recovery compass).
VERTICAL_FRAMES = (0, 16)
NEEDLE_SIDE = (170, 160, 190)


def needle_steps(frame):
    """(head steps from the hub to the tip - each step one cell, two side by side on the vertical frames -, tail
    cells). The glimmer of a calibrated detector runs step by step along the head."""
    head, tail = needle_path(frame)
    if frame % 32 in VERTICAL_FRAMES:
        return [[(7, y), (8, y)] for _, y in head], [(x, y) for _, y in tail for x in (7, 8)]
    return [[c] for c in head], tail


def detector_needle(frame):
    """Frame 0 points down (south), counting clockwise like the current detector_needle_XX (8 = left, 16 = up).
    Head lavender with a white tip (the layer is tinted by the find's distance), a two-pixel grey tail, four marks.
    The right half (17..31) is the mirror image of the left half (15..1) about x = 7.5, so both sides match; the
    vertical frames are two pixels wide (needle_steps)."""
    im = blank()
    steps, tail = needle_steps(frame)
    for p in tail:
        px(im, p[0], p[1], TAIL)
    for k, step in enumerate(steps):
        last = k == len(steps) - 1
        for j, p in enumerate(step):
            side = j == 1
            px(im, p[0], p[1], (v3.DET['n'] if side else v3.DET['N']) if last else (NEEDLE_SIDE if side else v3.DET['n']))
    cells = {c for step in steps for c in step}
    for m in MARKS:
        if m not in cells and m not in tail:
            px(im, m[0], m[1], v3.DET['p'])
    return im


def detector_gif(path):
    frames = []
    dial = detector_dial()
    for f in range(32):
        img = over(dial, detector_needle((16 + f) % 32)).resize((128, 128), Image.NEAREST)
        bg = Image.new('RGBA', img.size, (139, 139, 139, 255))
        bg.alpha_composite(img)
        frames.append(bg.convert('P', palette=Image.ADAPTIVE))
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=90, loop=0)


# ================================================================== Attraktor
REDSTONE = [(52, 6, 5), (92, 7, 0), (140, 8, 0), (196, 14, 1), (255, 40, 20)]
LAPIS = [(5, 36, 99), (18, 64, 139), (28, 83, 168), (52, 94, 195), (90, 130, 226)]
TIPS = [(70, 70, 76), (110, 110, 116), (160, 160, 166), (210, 210, 214), (240, 240, 242)]


def magnet(red, blue, tips=TIPS):
    return v3.attractor(red, blue, tips, cx=6.6, cy=9.4, r_in=1.3, r_out=5.4, length=5.4, tip_len=2.4)


def attractor_a():
    """Redstone red and lapis blue straight from the vanilla items, iron tips."""
    return magnet(REDSTONE, LAPIS)


def attractor_b():
    """Iron magnet with the colour laid on: the inner edge of each arm stays iron, the outer side is coated."""
    im = magnet(REDSTONE, LAPIS)
    plain = magnet(TIPS, TIPS)
    s2 = math.sqrt(2)
    for y in range(16):
        for x in range(16):
            if im.getpixel((x, y))[3] == 0:
                continue
            X, Y = x + 0.5 - 6.6, y + 0.5 - 9.4
            u, v = (X - Y) / s2, (X + Y) / s2
            r = math.hypot(u, v) if u <= 0 else abs(v)
            if r < 2.4:
                px(im, x, y, plain.getpixel((x, y)))
    return im


def attractor_c():
    """A, one step darker (closer to the round-4 darkness)."""
    return magnet(v4.shade(REDSTONE, 0.85), v4.shade(LAPIS, 0.85))


# ================================================================== Amethyst-Resonanzstab
def thick_shaft(im, wrap=False):
    """Three pixel diagonal shaft: lit top-left pixel, mid, shadow; a redstone band; dark end cap."""
    for i in range(6):
        x, y = 2 + i, 13 - i
        cols = [IRON[6], IRON[4], IRON[2]]
        if i == 3:
            cols = [(255, 90, 74), (200, 20, 20), (112, 8, 8)]
        if wrap and i == 0:
            cols = [IRON[3], IRON[2], IRON[1]]
        for k, c in enumerate(cols):
            px(im, x - 1 + k, y, c)
    px(im, 1, 14, IRON[2])
    px(im, 2, 14, IRON[0])
    px(im, 1, 13, IRON[4])
    return im


HEAD_PAL = {'o': AME[1], '5': AME[5], '4': AME[4], '3': AME[3], '2': AME[2], 'H': IRON[6], 'G': IRON[4], 'g': IRON[2]}


def rod_a():
    """Long shard on the axis (round-4 C) with two upright side shards (round-4 B) in an iron cup."""
    head = grid([
        '..............o.',
        '.............o5o',
        '.......o....o54o',
        '......o5o..o543o',
        '......o43oo5432o',
        '.......o32o432o.',
        '......HGGg32o3o.',
        '.......Ggo.ooo..',
    ] + ['................'] * 8, HEAD_PAL)
    return thick_shaft(head)


def rod_b():
    """Three shards like round-4 B, the middle one long like C, the cup a bit wider."""
    head = grid([
        '.............o..',
        '............o5o.',
        '........o..o54o.',
        '.......o5o.o43o.',
        '.......o43oo32oo',
        '.......o32o32o4o',
        '......HGHGGgoo3o',
        '.......Ggo...oo.',
    ] + ['................'] * 8, HEAD_PAL)
    return thick_shaft(head)


def rod_c():
    """Like A with the right side shard shorter and a wrapped grip end."""
    head = grid([
        '..............o.',
        '.............o5o',
        '.......o....o54o',
        '......o5o..o543o',
        '......o43oo5432o',
        '.......o32o432o.',
        '......HGGg32o.o.',
        '.......Ggo.oo...',
    ] + ['................'] * 8, HEAD_PAL)
    return thick_shaft(head, wrap=True)


# ================================================================== Kerne (around round-4 C)
def core_c(core):
    return v4.core_c(core)


def core_c1(core):
    """Round-4 C with the four straight points one pixel longer again."""
    out = core_c(core)
    for (x, y), (sx, sy) in (((8, 3), (8, 2)), ((8, 13), (8, 14)), ((3, 8), (2, 8)), ((13, 8), (14, 8))):
        p = core.getpixel((sx, sy))
        if p[3]:
            out.putpixel((x - (1 if x < 8 else -1 if x > 8 else 0), y - (1 if y < 8 else -1 if y > 8 else 0)), p)
    return out


def core_c2(core):
    """Round-4 C with the four diagonal corners one pixel further out again."""
    out = core_c(core)
    for (x, y) in ((3, 3), (4, 3), (3, 4), (12, 3), (13, 3), (13, 4), (3, 12), (3, 13), (4, 13), (13, 12), (12, 13), (13, 13)):
        p = core.getpixel((x, y))
        if p[3]:
            out.putpixel((x, y), p)
    return out


def core_c3(core):
    """Between round-4 B and C: the outer ring in where |d| >= 4 sideways, |d| >= 5 up/down (a touch wider than tall)."""
    def shift(x, y):
        return (v4._in(x) if abs(x - 8) >= 5 else x), (v4._in(y) if abs(y - 8) >= 4 else y)
    return v4.core_map(core, shift)


def main():
    os.makedirs(os.path.join(OUT, 'png'), exist_ok=True)
    iron = Image.open(os.path.join(ROOT, 'src/main/resources/assets/simplebuilding/textures/item/iron_core.png')).convert('RGBA').crop((0, 0, 16, 16))
    copper = Image.open(os.path.join(ROOT, 'src/main/resources/assets/simplebuilding/textures/item/copper_core.png')).convert('RGBA').crop((0, 0, 16, 16))
    dial = detector_dial()
    rows = [
        ('Erzdetektor', [over(tex('item/detector_dial'), tex('item/detector_needle_16'))] + [over(dial, detector_needle(f)) for f in (16, 20, 26)]),
        ('Attraktor', [tex('item/magnet'), attractor_a(), attractor_b(), attractor_c()]),
        ('Amethyst-Resonanzstab', [tex('item/amethyst_lens'), rod_a(), rod_b(), rod_c()]),
        ('Eisenkern', [core_c(iron), core_c1(iron), core_c2(iron), core_c3(iron)]),
        ('Kupferkern', [core_c(copper), core_c1(copper), core_c2(copper), core_c3(copper)]),
    ]
    s, cell, left = 6, 16 * 6 + 10, 200
    sheet = Image.new('RGBA', (left + 4 * cell + 10, len(rows) * (cell + 40) + 30), (139, 139, 139, 255))
    d = ImageDraw.Draw(sheet)
    for r, (label, variants) in enumerate(rows):
        y = 26 + r * (cell + 40)
        heads = ['v4-C', 'A', 'B', 'C'] if 'kern' in label else ['jetzt', 'A', 'B', 'C']
        if label == 'Erzdetektor':
            heads = ['jetzt', 'neu: Nord', 'Nordost', 'Ost-Südost']
        d.text((6, y + 40), label, fill=(255, 255, 255))
        for i, t in enumerate(variants):
            x = left + i * cell
            d.text((x, y - 13), heads[i], fill=(255, 255, 255))
            sheet.alpha_composite(t.resize((16 * s, 16 * s), Image.NEAREST), (x, y))
            d.rectangle((x, y + 16 * s + 4, x + 17, y + 16 * s + 21), fill=(139, 139, 139), outline=(55, 55, 55))
            sheet.alpha_composite(t, (x + 1, y + 16 * s + 5))
            sheet.alpha_composite(t.resize((32, 32), Image.NEAREST), (x + 24, y + 16 * s + 4))
        for tag, t in zip('abc', variants[1:]):
            t.save(os.path.join(OUT, 'png', f'{label.split(" ")[0].lower()}_{tag}.png'))
    sheet.save(os.path.join(OUT, 'uebersicht.png'))
    detector_gif(os.path.join(OUT, 'erzdetektor.gif'))
    print('ok ->', OUT)


if __name__ == '__main__':
    main()
