"""Usage: python tools/textures/proposals_v3_2026_10_02.py <vanilla textures dir> <preview dir>

Third round of texture proposals (owner feedback on round 2, 2026-10-02):
- Messuhr: like round-2 C (half dial) but closer to the vanilla clock (its gold ramp and outline), amethyst visible.
- Erzdetektor: keep the current detector, squashed (tilted back, flatter rim), leaning towards the echo sounder.
- Attraktor: the owner's orientation (U turned 45 degrees clockwise, bend lower left), red and blue arms, iron tips.
- Amethyst-Resonanzstab: round-2 B (crystal cluster) on the current iron shaft.
- Echolot: A = current compass enlarged to the full 16 px width; B/C = loosely compass-like with their own animation.
- Tiefenschiefer-Weisheitserz: long strokes top left -> bottom right, small strokes top right -> bottom left.
Settled (no choice left, shown for checking): guide books (round-2 B with a bookmark ribbon at the side),
sage orb (round-2 A = the vanilla orb), the cores (current, outer star one pixel slimmer), the new iron rod.
Writes uebersicht.png and the 16x16 candidates into <preview dir>/png/."""
from PIL import Image, ImageDraw
import math
import os
import sys

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
OUT = sys.argv[2] if len(sys.argv) > 2 else 'build/proposals-v3/'
ROOT = os.path.join(os.path.dirname(__file__), '..', '..')

# vanilla clock gold (item/clock_00): outline brown, dark -> light gold, highlight
CLK = {'o': (117, 40, 2), 'a': (178, 100, 17), 'l': (220, 150, 19), 'd': (233, 177, 21), 'c': (250, 214, 74),
       'b': (253, 245, 95), 'j': (251, 247, 183)}
SKY = [(58, 83, 172), (73, 104, 216)]  # clock dial blues
IRON = [(53, 53, 53), (74, 74, 74), (94, 94, 94), (114, 114, 114), (130, 130, 130), (168, 168, 168), (216, 216, 216), (255, 255, 255)]
AME = [(53, 33, 92), (84, 57, 138), (141, 106, 204), (179, 142, 243), (207, 160, 243), (254, 203, 230)]
RED = [(74, 12, 14), (122, 22, 24), (170, 32, 32), (214, 52, 44), (246, 104, 88)]
BLUE = [(14, 18, 66), (26, 40, 120), (38, 62, 178), (52, 96, 226), (120, 156, 250)]
XP = [(36, 72, 6), (74, 130, 14), (128, 196, 28), (186, 238, 66), (238, 255, 168)]


def blank():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def px(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), tuple(c[:3]) + (255,))


def grid(rows, pal):
    im = blank()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                px(im, x, y, pal[ch])
    return im


def over(base, top):
    out = base.copy()
    out.alpha_composite(top)
    return out


def tex(path):
    for base in ('mc26_3/overlay/resources/assets/simplebuilding/textures/', 'src/main/resources/assets/simplebuilding/textures/'):
        p = os.path.join(ROOT, base, path + '.png')
        if os.path.exists(p):
            return Image.open(p).convert('RGBA').crop((0, 0, 16, 16))
    return blank()


def vtex(path):
    return Image.open(os.path.join(V, path + '.png')).convert('RGBA').crop((0, 0, 16, 16))


def line(im, x0, y0, x1, y1, cols):
    """Pixel line; cols[0] at the start, cols[-1] at the end."""
    n = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(n + 1):
        t = i / n if n else 0
        px(im, round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t), cols[min(len(cols) - 1, int(t * len(cols)))])


# ================================================================== Messuhr
def needle(cx, cy, ang, length, tip, body, im=None):
    im = im or blank()
    a = math.radians(ang)
    for i in range(1, int(length * 2) + 1):
        r = i / 2
        x, y = int(math.floor(cx + math.cos(a) * r)), int(math.floor(cy - math.sin(a) * r))
        px(im, x, y, tip if r > length - 1.2 else body)
    return im


def gauge_a():
    """Half clock: clock gold rim, clock-blue dial, gold ticks, amethyst crystal as the needle cap."""
    rows = [
        '................',
        '................',
        '.....aaaaaa.....',
        '...aabbccccaa...',
        '..abbSSSSSSdca..',
        '.abSSSSSSSSSSdo.',
        '.abSsSSSSSSsSdo.',
        'abSSSSSSSSSSSSdo',
        'acSsSSSSSSSSsSlo',
        'acSSSSSSSSSSSSlo',
        'acSsSSSSSSSSsSlo',
        'adcccccPQcccccdo',
        'oalllllRTllllaao',
        '.oooooooooooooo.',
        '................',
        '................',
    ]
    pal = dict(CLK)
    pal.update({'S': SKY[0], 's': CLK['c'], 'P': AME[5], 'Q': AME[3], 'R': AME[2], 'T': AME[1]})
    dial = grid(rows, pal)
    for x, y in ((7, 4), (8, 4)):
        px(dial, x, y, CLK['b'])
    return dial, needle(7.5, 11.0, 55, 6.2, (255, 255, 255), (216, 216, 216))


def gauge_b():
    """Half clock with an amethyst dial: gold rim and base like the clock, green-yellow-red arc, white needle."""
    rows = [
        '................',
        '................',
        '.....aaaaaa.....',
        '...aabbccccaa...',
        '..abbGGYYYRRdca.',
        '.abGG3333333RRdo',
        '.abG33333333332o',
        'abG3333333333332',
        'acG3333333333332',
        'ac33333333333332',
        'ac22222222222222',
        'adcccccPQcccccdo',
        'oalllllRTllllaao',
        '.oooooooooooooo.',
        '................',
        '................',
    ]
    pal = dict(CLK)
    pal.update({'G': (86, 186, 52), 'Y': (236, 206, 54), 'R': (220, 62, 50), '3': AME[1], '2': AME[0],
                'P': AME[5], 'Q': AME[3]})
    pal['R'] = (220, 62, 50)
    dial = grid(rows, pal)
    # the base gem, after the arc red took the letter
    for x, y, c in ((7, 12, AME[2]), (8, 12, AME[1])):
        px(dial, x, y, c)
    # fix the right rim the arc letters overran
    for y in range(6, 11):
        px(dial, 15, y, CLK['o'])
        px(dial, 14, y, CLK['l'] if y > 7 else CLK['d'])
    return dial, needle(7.5, 11.0, 55, 6.2, (255, 255, 255), (216, 216, 216))


def gauge_c():
    """Round like the vanilla clock: the upper half is the dial, the lower half a gold body with an amethyst set in."""
    rows = [
        '......aaaa......',
        '....aabbccaa....',
        '...abjFFFFdca...',
        '..abFFFFFFFFca..',
        '..abFtFFFFtFco..',
        '.abFFFFFFFFFFdo.',
        '.abtFFFFFFFFtdo.',
        '.acFFFFFFFFFFdo.',
        '.addddddddddddo.',
        '.oblcccPQccclao.',
        '.olbccPQRTcclao.',
        '..olbccRTccldo..',
        '..oalllllllaao..',
        '...oaallaaaao...',
        '....ooaaaaoo....',
        '......oooo......',
    ]
    pal = dict(CLK)
    pal.update({'F': (250, 246, 228), 't': CLK['a'], 'P': AME[5], 'Q': AME[3], 'R': AME[2], 'T': AME[1]})
    dial = grid(rows, pal)
    return dial, needle(7.5, 8.0, 55, 5.2, (200, 30, 30), (40, 34, 30))


# ================================================================== Erzdetektor
DET = {'A': (28, 44, 48), 'B': (79, 113, 120), 'C': (250, 214, 74), 'D': (8, 22, 26), 'E': (42, 69, 75),
       'F': (7, 11, 14), 'G': (20, 49, 57), 'H': (21, 38, 44), 'I': (233, 177, 21), 'J': (15, 74, 82),
       'k': (8, 46, 54), 'K': (13, 69, 82), 'g': (19, 142, 153), 'n': (228, 216, 242), 'N': (255, 255, 255),
       'p': (138, 130, 148)}


def detector_a():
    """The current detector, squashed by three rows (lid seen tilted back), same palette and marks."""
    rows = [
        '................',
        '................',
        '................',
        '.....AAAAAA.....',
        '...AABBCCBBAA...',
        '..ABDGGDDDDEEF..',
        '.ABDGDDDDDDDDHF.',
        '.ABDDDDDDDDDDHF.',
        '.ABDDDDDDDDDJHF.',
        '..AEDDDDDDDJHF..',
        '..AEEDJDDJDHHF..',
        '...FFHHHHHHFF...',
        '.....FFFFFF.....',
        '................',
        '................',
        '................',
    ]
    return grid(rows, DET), detector_needle(7.5, 8.0, 1.0)


def detector_b():
    """Squashed lid plus the compass-style side band underneath (like the echo sounder), teal veins in it."""
    rows = [
        '................',
        '................',
        '.....AAAAAA.....',
        '...AABBCCBBAA...',
        '..ABDGGDDDDEEF..',
        '.ABDGDDDDDDDDHF.',
        '.ABDDDDDDDDDDHF.',
        '.ABDDDDDDDDDJHF.',
        '.AEEDDDDDDDJHHF.',
        '.AEEEHDJJDHHHkF.',
        '.FgkEEHHHHHHkgF.',
        '..FgkkKKKKkkgF..',
        '...FFkKggKkFF...',
        '.....FFFFFF.....',
        '................',
        '................',
    ]
    return grid(rows, DET), detector_needle(7.5, 7.0, 1.0)


def detector_c():
    """Like B, a little flatter still, gold north mark and a gold pin; widest at the full 14 px."""
    rows = [
        '................',
        '................',
        '................',
        '....AAAAAAAA....',
        '..AABBBCCBBBAA..',
        '.ABDGGDDDDDDEEF.',
        '.ABDGDDDDDDDDHF.',
        'ABDDDDDDDDDDDDHF',
        'ABDDDDDDDDDDDJHF',
        'AEEDDDDDDDDDJHHF',
        '.AEEEHHDDHHHHkF.',
        '.FgkkEHHHHHHkgF.',
        '..FFgkKKKKkgFF..',
        '....FFFFFFFF....',
        '................',
        '................',
    ]
    return grid(rows, DET), detector_needle(7.5, 8.0, 1.0)


def detector_needle(cx, cy, squash):
    """Needle of the detector seen tilted: north up, flattened vertically."""
    im = blank()
    px(im, int(cx), int(cy) - 2, DET['n'])
    px(im, int(cx), int(cy) - 1, DET['n'])
    px(im, int(cx), int(cy) - 3, DET['N'])
    px(im, int(cx), int(cy), DET['p'])
    px(im, int(cx), int(cy) + 1, (92, 86, 100))
    return im


# ================================================================== Attraktor
def attractor(body_l, body_r, tips, cx=6.2, cy=9.8, r_in=1.6, r_out=4.9, length=6.2, tip_len=2.8, light=True):
    """U magnet turned 45 degrees clockwise: bend at the lower left, both arms reaching to the upper right.
    Rasterised with 4x4 supersampling, then outlined in each material's darkest tone and lit from the top left."""
    s2 = math.sqrt(2)
    cover = {}
    for y in range(16):
        for x in range(16):
            hit, part = 0, None
            for sy in range(4):
                for sx in range(4):
                    X, Y = x + (sx + 0.5) / 4 - cx, y + (sy + 0.5) / 4 - cy
                    u, v = (X - Y) / s2, (X + Y) / s2
                    r = math.hypot(u, v)
                    inside = (u <= 0 and r_in <= r <= r_out) or (0 < u <= length and r_in <= abs(v) <= r_out)
                    if inside:
                        hit += 1
                        tip = u > length - tip_len
                        part = ('T' if tip else '') + ('L' if v < 0 else 'R')
            if hit >= 7:
                cover[(x, y)] = part
    im = blank()
    for (x, y), part in cover.items():
        ramp = tips if part.startswith('T') else body_l if part.endswith('L') else body_r
        edge = any((x + dx, y + dy) not in cover for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            # outline: darkest tone, a lighter rim pixel where the light hits (top/left edge)
            lit = (x - 1, y) not in cover or (x, y - 1) not in cover
            c = ramp[1] if (lit and light) else ramp[0]
        else:
            X, Y = x + 0.5 - cx, y + 0.5 - cy
            v = (X + Y) / s2
            # across the arm: top-left side bright, bottom-right side dark
            band = (abs(v) - r_in) / (r_out - r_in)
            if v < 0:
                band = 1 - band
            c = ramp[min(len(ramp) - 1, 2 + int((1 - band) * (len(ramp) - 2)))]
        px(im, x, y, c)
    return im


def attractor_a():
    """Red/blue, iron tips (the round-2 colours on the owner's diagonal)."""
    return attractor(RED, BLUE, [(70, 70, 76), (110, 110, 116), (160, 160, 166), (210, 210, 214), (240, 240, 242)])


def attractor_b():
    """Slimmer arms and a wider gap, longer iron tips - reads more like the vanilla tool silhouettes."""
    return attractor(RED, BLUE, [(70, 70, 76), (110, 110, 116), (160, 160, 166), (210, 210, 214), (240, 240, 242)],
                     r_in=2.0, r_out=4.6, length=6.6, tip_len=3.0)


def attractor_c():
    """Chunky arms, short tips; the bend keeps the colour split so both poles stay visible."""
    return attractor(RED, BLUE, [(70, 70, 76), (110, 110, 116), (160, 160, 166), (210, 210, 214), (240, 240, 242)],
                     cx=6.6, cy=9.4, r_in=1.3, r_out=5.4, length=5.4, tip_len=2.4)


# ================================================================== Amethyst-Resonanzstab
SHAFT = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '........FGG.....',
    '.......FHIJJ....',
    '......FHIIKJ....',
    '.....FHLKKM.....',
    '....FHNOKKM.....',
    '...FPIIKKM......',
    '..FGIIKKM.......',
    '..FGJKKM........',
    '...FJJM.........',
    '....MM..........',
    '................',
]
SHAFT_PAL = {'F': IRON[2], 'G': IRON[4], 'H': IRON[6], 'I': IRON[5], 'J': IRON[1], 'K': IRON[3], 'M': IRON[0],
             'P': IRON[7], 'L': (255, 90, 74), 'N': (200, 20, 20), 'O': (112, 8, 8)}


def rod_a():
    """Current iron shaft, round-2 B cluster: a big shard on the axis and two small ones beside it."""
    shaft = grid(SHAFT, SHAFT_PAL)
    head = grid([
        '.............o..',
        '.....o......o5o.',
        '....o5o....o54o.',
        '....o43o..o543o.',
        '.....o32oo543o..',
        '......o2C432o...',
        '...........o....',
    ] + ['................'] * 9, {'o': AME[1], '5': AME[5], '4': AME[4], '3': AME[3], '2': AME[2], 'C': IRON[4]})
    return over(shaft, head)


def rod_b():
    """Cluster of three upright shards (like an amethyst cluster block) sitting in an iron cup on the shaft."""
    shaft = grid(SHAFT, SHAFT_PAL)
    head = grid([
        '...........o....',
        '..........o5o...',
        '.......o..o4o.o.',
        '......o5o.o43o5o',
        '......o43oo32o4o',
        '.......o32G32o3o',
        '........oGHGGoo.',
        '.........o......',
    ] + ['................'] * 8, {'o': AME[1], '5': AME[5], '4': AME[4], '3': AME[3], '2': AME[2],
                                  'G': IRON[4], 'H': IRON[6]})
    return over(shaft, head)


def rod_c():
    """One large faceted shard continuing the shaft, two tiny side crystals, a nugget ring under it."""
    shaft = grid(SHAFT, SHAFT_PAL)
    head = grid([
        '..............o.',
        '.............o5o',
        '............o54o',
        '.......o...o543o',
        '......o4o.o5432o',
        '.......o2o5432o.',
        '........HGo32o..',
        '.............o..',
    ] + ['................'] * 8, {'o': AME[1], '5': AME[5], '4': AME[4], '3': AME[3], '2': AME[2],
                                  'G': IRON[4], 'H': IRON[6]})
    return over(shaft, head)


# ================================================================== Iron rod (new block)
def iron_rod_textures():
    """Vanilla retexture: block/lightning_rod pixel for pixel, each copper tone swapped by brightness rank for a tone
    of the vanilla iron block ramp (lightest copper -> lightest iron)."""
    src = vtex('block/lightning_rod')
    copper = sorted({src.getpixel((x, y))[:3] for y in range(16) for x in range(16) if src.getpixel((x, y))[3]},
                    key=lambda c: -(0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]))
    iron = [(220, 220, 220), (205, 205, 205), (190, 190, 190), (172, 172, 172), (154, 154, 154), (136, 136, 136)]
    swap = {c: iron[min(len(iron) - 1, i)] for i, c in enumerate(copper)}
    out = blank()
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), swap[p[:3]] + (255,))
    return out


# ================================================================== Echolot
ECHO = {'o': (43, 58, 61), 'O': (13, 20, 23), 'W': (255, 255, 255), 'L': (216, 216, 216), 'M': (168, 168, 168),
        'm': (130, 130, 130), 'D': (94, 94, 94), 'd': (58, 68, 71), 'F': (11, 26, 29), 'f': (21, 52, 58),
        's': (16, 82, 87), 'k': (8, 46, 54), 'K': (13, 69, 82), 'g': (19, 142, 153), 'G': (41, 223, 235),
        't': (159, 250, 255), 'n': (47, 107, 114)}


def echo_a():
    """The current echo sounder, widened to the full 16 px (one column each side of the glass, one more row)."""
    rows = [
        '................',
        '.....oooooo.....',
        '...ooLWLMLLmoo..',
        '..oLMdsssssssdD.',
        '.oLdsfFFFFFFFsdO',
        'oMdfFFFFFFFFFFdO',
        'oMdFFFFFFFFFFFdO',
        'oMdFFFFFFFFFFFdO',
        'omdFFFFFFFFFFFdO',
        'OmDdFFFFFFFFFdDO',
        'OLWmddFFFFddmDDO',
        'OLLMmDdddddDmmkO',
        '.OgkMmmmmmmmDkgO',
        '..OOkgGGGGgkOO..',
        '....OOOOOOOO....',
        '................',
    ]
    im = grid(rows, ECHO)
    # needle north: cyan head, pale tip, grey pin, dark tail
    for x, y, c in ((8, 4, ECHO['t']), (8, 5, ECHO['G']), (8, 6, ECHO['G']), (8, 7, ECHO['M']), (8, 8, ECHO['n'])):
        px(im, x, y, c)
    return im


def echo_b():
    """Sonar: compass-like metal case, inside echo rings; the bright arc on the outer ring shows the direction
    (animation: the arc travels round the ring, the inner ring pulses)."""
    rows = [
        '................',
        '.....oooooo.....',
        '...ooLWLMLmoo...',
        '..oLMdsssssdDO..',
        '.oLdsFnnnnFFsdO.',
        '.oMdFnFFFFnFFdO.',
        'oMdFnFFggFFnFFdO',
        'oMdFnFgFFgFnFFdO',
        'omdFnFgFFgFnFFdO',
        'OmDdFnFggFnFFdDO',
        'OLWmdFnnnnFFddDO',
        '.OLMmDdddddDmkO.',
        '..OgkMmmmmmDkgO.',
        '...OOkgGGgkOO...',
        '.....OOOOOO.....',
        '................',
    ]
    im = grid(rows, ECHO)
    for x, y in ((6, 4), (7, 4), (8, 4)):
        px(im, x, y, ECHO['G'])
    px(im, 7, 4, ECHO['t'])
    return im


def echo_c():
    """Recovery-compass body with two sculk-sensor feelers on top (they glow in turn as the animation)."""
    rows = [
        '...O........O...',
        '..OgO......OgO..',
        '..OGO......OGO..',
        '...Ok.oooo.kO...',
        '....ooLWLMoo....',
        '...oLdssssdDO...',
        '..oLsFFFFFFsdO..',
        '.oMdFFFFFFFFdDO.',
        '.oMdFFFFFFFFdDO.',
        '.omdFFFFFFFFdDO.',
        '.OmDdFFFFFFdDDO.',
        '.OLWmddFFddmDkO.',
        '..OgkMmmmmmDkgO.',
        '...OOkgGGgkOO...',
        '.....OOOOOO.....',
        '................',
    ]
    im = grid(rows, ECHO)
    for x, y, c in ((7, 6, ECHO['t']), (7, 7, ECHO['G']), (7, 8, ECHO['G']), (7, 9, ECHO['M']), (7, 10, ECHO['n'])):
        px(im, x, y, c)
    return im


# ================================================================== Guide books (settled: round-2 B + bookmark)
def book(cover, accent, ribbon):
    rows = [
        '................',
        '........ooo.....',
        '......ooCCCo....',
        '....ooCCCCCCo...',
        '..ooCCCHCCCCCo..',
        'ooCCCCCCHCCCCco.',
        'oCCCCCCCCHCCCcco',
        'ooCCCHCCCCCCccp.',
        'oscCCCCCCCccpPpR',
        'oPscCCCCCccpPPpr',
        '.oPscCCccpPPpcr.',
        '..oPsccpPPpcc.Rr',
        '...oPspPpcc...r.',
        '....ospcc.......',
        '.....ooo........',
        '................',
    ]
    return grid(rows, {'o': cover[0], 'C': cover[2], 'c': cover[1], 'H': accent, 's': cover[3], 'p': (214, 214, 214),
                       'P': (168, 168, 168), 'R': ribbon[1], 'r': ribbon[0]})


def guide_books():
    blue = [(18, 26, 66), (36, 58, 132), (56, 92, 186), (96, 138, 222)]
    brown = [(46, 28, 10), (84, 52, 18), (120, 78, 30), (158, 112, 52)]
    return (book(blue, (250, 214, 76), [(140, 24, 20), (214, 52, 44)]),
            book(brown, (110, 186, 60), [(40, 104, 26), (86, 170, 52)]))


# ================================================================== Tiefenschiefer-Weisheitserz
def crystal(im, a, b, n):
    """Main stroke top left -> bottom right: a two pixel band (like the shards of vanilla diamond ore), the upper
    right pixel of each step the lit one, bright at the top-left end, a shadow pixel under the lower end."""
    tones = [4, 3, 3, 2, 2, 1]
    for i in range(n):
        k = tones[min(len(tones) - 1, round(i * (len(tones) - 1) / max(1, n - 1)))]
        px(im, a + i, b + i, XP[max(0, k - 1)])
        px(im, a + i + 1, b + i, XP[k])
    px(im, a + n - 1, b + n, XP[0])
    px(im, a, b, XP[4])


def counter(im, c, d, n=2):
    """Small stroke top right -> bottom left, single pixel wide, lit at the top."""
    for i in range(n):
        px(im, c - i, d + i, XP[3 - min(2, i)])


def sage_ore_a():
    """Four shards of different length, four small counter strokes between them."""
    im = vtex('block/deepslate')
    for a, b, n in ((1, 1, 3), (9, 3, 4), (3, 8, 4), (11, 11, 3)):
        crystal(im, a, b, n)
    for c, d, n in ((7, 1, 2), (14, 7, 2), (8, 9, 3), (3, 13, 2)):
        counter(im, c, d, n)
    return im


def sage_ore_b():
    """Shards crossed near their upper end by a counter stroke - small crystal stars, two lone strokes."""
    im = vtex('block/deepslate')
    for (a, b, n), (c, d) in (((1, 2, 4), (4, 1)), ((10, 1, 3), (13, 0)), ((5, 8, 4), (8, 7)), ((11, 10, 4), (14, 9))):
        crystal(im, a, b, n)
        counter(im, c, d, 3)
    for c, d in ((8, 3), (3, 13)):
        counter(im, c, d, 2)
    return im


def sage_ore_c():
    """Fewer, longer shards (vein weight of vanilla emerald ore) with short counter strokes branching off them."""
    im = vtex('block/deepslate')
    for a, b, n in ((1, 2, 5), (8, 1, 3), (7, 8, 5)):
        crystal(im, a, b, n)
    for c, d, n in ((4, 5, 2), (13, 2, 2), (11, 10, 2), (3, 11, 3), (14, 6, 2), (6, 14, 2)):
        counter(im, c, d, n)
    return im


# ================================================================== Weisheitskugel (settled: round-2 A)
def sage_orb():
    sheet = Image.open(os.path.join(V, 'entity/experience/experience_orb.png')).convert('RGBA')
    f = sheet.crop((0, 16, 16, 32))
    g = blank()
    for y in range(16):
        for x in range(16):
            p = f.getpixel((x, y))
            if p[3]:
                g.putpixel((x, y), (round(p[0] * 0.5), round(p[1] * 1.0), round(p[2] * 0.1), p[3]))
    return g


# ================================================================== Kerne (settled: current, one pixel slimmer)
def slimmer(core, cx=8, cy=8, keep=3):
    """Outer star one pixel closer to the centre on every side; the nether-star middle stays where it is."""
    out = blank()
    for y in range(16):
        for x in range(16):
            sx = x - 1 if x < cx else x + 1 if x > cx else x
            sy = y - 1 if y < cy else y + 1 if y > cy else y
            if 0 <= sx < 16 and 0 <= sy < 16:
                p = core.getpixel((sx, sy))
                if p[3]:
                    out.putpixel((x, y), p)
    for y in range(16):
        for x in range(16):
            if abs(x - cx) + abs(y - cy) <= keep:
                p = core.getpixel((x, y))
                if p[3]:
                    out.putpixel((x, y), p)
    return out


CORES = ('copper', 'iron', 'gold', 'diamond', 'netherite', 'enderite')


def main():
    os.makedirs(os.path.join(OUT, 'png'), exist_ok=True)
    g = [gauge_a(), gauge_b(), gauge_c()]
    dets = [detector_a(), detector_b(), detector_c()]
    cur_det = over(tex('item/detector_dial'), tex('item/detector_needle_16'))
    cur_gauge = over(tex('item/velocity_gauge_dial'), tex('item/velocity_gauge_needle_08'))
    books = guide_books()
    rows = [
        ('Messuhr', [cur_gauge] + [over(d, n) for d, n in g]),
        ('Erzdetektor', [cur_det] + [over(d, n) for d, n in dets]),
        ('Attraktor', [tex('item/magnet'), attractor_a(), attractor_b(), attractor_c()]),
        ('Amethyst-Resonanzstab', [tex('item/amethyst_lens'), rod_a(), rod_b(), rod_c()]),
        ('Echolot', [tex('item/echo_sounder_16'), echo_a(), echo_b(), echo_c()]),
        ('Tiefenschiefer-Weisheitserz', [tex('block/deepslate_sage_ore'), sage_ore_a(), sage_ore_b(), sage_ore_c()]),
        ('fest: Guides, Kugel, Eisenstab', [tex('item/guide_book'), books[0], books[1], sage_orb(), iron_rod_textures()]),
        ('fest: Kerne (vorher/nachher)', [x for c in CORES[:3] for x in (tex(f'item/{c}_core'), slimmer(tex(f'item/{c}_core')))]),
        ('', [x for c in CORES[3:] for x in (tex(f'item/{c}_core'), slimmer(tex(f'item/{c}_core')))]),
    ]
    s, cell, left = 6, 16 * 6 + 10, 200
    ncol = max(len(r[1]) for r in rows)
    sheet = Image.new('RGBA', (left + ncol * cell + 10, len(rows) * (cell + 40) + 30), (139, 139, 139, 255))
    d = ImageDraw.Draw(sheet)
    for i, h in enumerate(['jetzt', 'A', 'B', 'C']):
        d.text((left + i * cell + 40, 8), h, fill=(255, 255, 255))
    for r, (label, variants) in enumerate(rows):
        y = 26 + r * (cell + 40)
        d.text((6, y + 40), label, fill=(255, 255, 255))
        for i, t in enumerate(variants):
            x = left + i * cell
            sheet.alpha_composite(t.resize((16 * s, 16 * s), Image.NEAREST), (x, y))
            d.rectangle((x, y + 16 * s + 4, x + 17, y + 16 * s + 21), fill=(139, 139, 139), outline=(55, 55, 55))
            sheet.alpha_composite(t, (x + 1, y + 16 * s + 5))
            sheet.alpha_composite(t.resize((32, 32), Image.NEAREST), (x + 24, y + 16 * s + 4))
        if label and not label.startswith('fest'):
            for tag, t in zip('abc', variants[1:]):
                t.save(os.path.join(OUT, 'png', f'{label.lower().replace(" ", "_")}_{tag}.png'))
    sheet.save(os.path.join(OUT, 'uebersicht.png'))
    print('ok ->', OUT)


if __name__ == '__main__':
    main()
