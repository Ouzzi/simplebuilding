"""Usage: python tools/textures/proposals_v2_2026_10_02.py <vanilla textures dir> <preview dir>

Second round of texture proposals (owner 2026-10-02: the recoloured first round was not convincing).
Every sprite is drawn for its function in vanilla's item language: 1 px outline in the darkest shade of
the material (never pure black), light from the top left, 3-5 tone ramps, readable at 1x. Round devices
are rasterised from geometry (rim, face, ticks, needle) so the circles stay clean; the rest are
hand-set pixel grids. Writes uebersicht.png and the 16x16 candidates into <preview dir>/png/."""
from PIL import Image, ImageDraw
import math
import os
import sys

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
OUT = sys.argv[2] if len(sys.argv) > 2 else 'build/proposals-v2/'
ROOT = os.path.join(os.path.dirname(__file__), '..', '..')

IRON = [(58, 58, 62), (98, 98, 104), (146, 146, 152), (196, 196, 200), (236, 236, 238)]
COPPER = [(92, 42, 26), (150, 74, 46), (193, 108, 74), (230, 146, 106), (252, 196, 160)]
GOLD = [(110, 58, 8), (176, 106, 18), (226, 166, 36), (250, 214, 76), (255, 246, 172)]
AMETHYST = [(66, 42, 112), (102, 70, 162), (141, 106, 204), (184, 146, 240), (238, 204, 255)]
WOOD = [(46, 32, 12), (82, 58, 24), (122, 88, 38), (158, 120, 58)]
SCULK = [(8, 16, 20), (6, 40, 48), (10, 78, 92), (0, 140, 146), (40, 214, 226), (180, 250, 250)]
DEEP = [(36, 36, 40), (54, 54, 60), (74, 74, 82), (98, 98, 106)]
RED = [(96, 16, 16), (168, 34, 30), (220, 62, 50), (250, 140, 120)]
CREAM = [(196, 182, 150), (232, 222, 192), (250, 246, 228)]
GREEN, YELLOW, ORANGE = (86, 186, 52), (236, 206, 54), (236, 132, 40)


def blank():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def px(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), tuple(c) + (255,))


def grid(rows, pal):
    im = blank()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                px(im, x, y, pal[ch])
    return im


def disc(im, cx, cy, r_out, rim, ramp, face, outline=None, light=(-0.7, -0.7)):
    """Round case: outline ring, shaded rim of width `rim`, flat face inside."""
    outline = outline or ramp[0]
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > r_out:
                continue
            if r > r_out - 1.0:
                px(im, x, y, outline)
            elif r > r_out - 1.0 - rim:
                d = (dx * light[0] + dy * light[1]) / max(r, 0.01)
                i = 1 + round((d + 1) / 2 * (len(ramp) - 2))
                px(im, x, y, ramp[min(len(ramp) - 1, i)])
            elif face is not None:
                px(im, x, y, face)


def ray(im, cx, cy, angle_deg, r0, r1, c):
    a = math.radians(angle_deg)
    steps = int((r1 - r0) * 3) + 1
    for i in range(steps + 1):
        r = r0 + (r1 - r0) * i / steps
        px(im, int(cx + math.cos(a) * r), int(cy - math.sin(a) * r), c)


# ================================================================== Messuhr (Velocity Gauge)
def gauge_a():
    """Iron speedometer: light dial, tick arc green -> red over the top, red needle, dark hub."""
    im = blank()
    disc(im, 8, 8, 7.6, 1.6, IRON, CREAM[1])
    for i, ang in enumerate(range(210, -31, -30)):
        col = GREEN if i < 3 else YELLOW if i < 6 else RED[2]
        ray(im, 8, 8, ang, 4.6, 5.2, col)
    ray(im, 8, 8, 40, 0, 4.2, RED[2])
    ray(im, 8, 8, 220, 0, 1.2, RED[1])
    px(im, 7, 7, IRON[0]); px(im, 8, 7, IRON[1])
    for x in range(6, 10):
        px(im, x, 11, IRON[2])
    return im


def gauge_b():
    """Copper stopwatch: crown and loop on top, white face, black hand, four hour marks."""
    im = blank()
    disc(im, 8, 9, 6.6, 1.3, COPPER, CREAM[2])
    for x, y, c in ((7, 0, COPPER[0]), (8, 0, COPPER[0]), (6, 1, COPPER[0]), (9, 1, COPPER[0]), (7, 1, COPPER[4]), (8, 1, COPPER[3]),
                    (7, 2, COPPER[2]), (8, 2, COPPER[1]), (11, 2, COPPER[0]), (12, 3, COPPER[1])):
        px(im, x, y, c)
    for x, y in ((8, 4), (12, 9), (8, 13), (4, 9)):
        px(im, x, y, IRON[1])
    ray(im, 8, 9, 60, 0, 3.6, (40, 34, 30))
    px(im, 7, 8, RED[2])
    return im


def gauge_c():
    """Golden half-moon dial: flat bottom, dark face, colour band, white needle."""
    rows = [
        '................',
        '................',
        '.....oooooo.....',
        '...ooHhhhhhoo...',
        '..oHhffffffhmo..',
        '.oHffGGYYRRffmo.',
        '.ohfGffffffRfmo.',
        'ohffGfffffffRfmo',
        'ohfGfffffwffRfmo',
        'ohfffffffwfffmmo',
        'ohffffffwffffmmo',
        'ommmmmmmcmmmmmmo',
        'oddddddddddddddo',
        '.oooooooooooooo.',
        '................',
        '................',
    ]
    return grid(rows, {'o': GOLD[0], 'H': GOLD[4], 'h': GOLD[3], 'm': GOLD[1], 'd': GOLD[2], 'f': (36, 30, 40),
                       'G': GREEN, 'Y': YELLOW, 'R': RED[2], 'w': (246, 246, 246), 'c': GOLD[0]})


# ================================================================== Erzdetektor (Ore Detector)
def detector_a():
    """Dowsing rod: a forked stick, copper caps on both arms, a diamond glint at the fork."""
    rows = [
        '................',
        '..oo........oo..',
        '.oCco......oCco.',
        '.ocCo......ocCo.',
        '..owo......owo..',
        '...owo....owo...',
        '....owo..owo....',
        '.....owoowo.....',
        '......oDdo......',
        '......owwo......',
        '......owwo......',
        '......owwo......',
        '......owwo......',
        '......owwo......',
        '.......oo.......',
        '................',
    ]
    return grid(rows, {'o': WOOD[0], 'w': WOOD[2], 'C': COPPER[3], 'c': COPPER[1],
                       'D': (172, 252, 236), 'd': (34, 196, 180)})


def detector_b():
    """Handheld detector: iron box, green screen with an ore blip, antenna to the top right."""
    rows = [
        '...........oo...',
        '..........oAo...',
        '.........oAo....',
        '...oooooooAooo..',
        '..oHhhhhhhhhhmo.',
        '..ohsssssssshmo.',
        '..ohsgggggsshmo.',
        '..ohsggbbgsshmo.',
        '..ohsgbBbggshmo.',
        '..ohsggbggsshmo.',
        '..ohsssssssshmo.',
        '..ohhrhhhkhhhmo.',
        '..ohhhhhhhhhhmo.',
        '..ommmmmmmmmmmo.',
        '...oooooooooooo.',
        '................',
    ]
    return grid(rows, {'o': IRON[0], 'H': IRON[4], 'h': IRON[3], 'm': IRON[1], 'A': IRON[2], 's': (22, 40, 30),
                       'g': (34, 74, 44), 'b': (110, 220, 120), 'B': (220, 255, 210), 'r': RED[2], 'k': GREEN})


def detector_c():
    """Ore lens: a copper magnifier, the glass shows ore specks, wooden handle to the bottom left."""
    im = blank()
    disc(im, 9.5, 6.5, 5.6, 1.2, COPPER, (126, 178, 196))
    for x, y, c in ((8, 5, (40, 40, 44)), (9, 5, (40, 40, 44)), (10, 7, (86, 236, 246)), (11, 7, (86, 236, 246)),
                    (8, 8, (230, 180, 40)), (7, 6, (200, 236, 244)), (8, 4, (200, 236, 244))):
        px(im, x, y, c)
    for i in range(5):
        px(im, 2 + i, 13 - i, WOOD[2]); px(im, 1 + i, 13 - i, WOOD[0]); px(im, 2 + i, 14 - i, WOOD[0])
    px(im, 6, 10, COPPER[1]); px(im, 5, 10, COPPER[0])
    return im


# ================================================================== Attraktor (Magnet)
def magnet(body, tips, coil=None):
    rows = [
        '................',
        '..oooo....oooo..',
        '.oTTTto..oTTTto.',
        '.otttso..otttso.',
        '.oooooo..oooooo.',
        '.oHbbdo..oHbbdo.',
        '.oHbbdo..oHbbdo.',
        '.oHbbdo..oHbbdo.',
        '.oHbbdoooohbbdo.',
        '.oHbbbHhhhbbbdo.',
        '..oHbbbbbbbbdo..',
        '..ohbbbbbbbbdo..',
        '...oddbbbbddo...',
        '....oooooooo....',
        '................',
        '................',
    ]
    pal = {'o': body[0], 'H': body[3], 'h': body[2], 'b': body[2], 'd': body[1], 'T': tips[3], 't': tips[2], 's': tips[1]}
    im = grid(rows, pal)
    if coil:
        for y in (9, 11):
            for x in range(3, 13):
                if im.getpixel((x, y))[3] and im.getpixel((x, y))[:3] != body[0]:
                    px(im, x, y, coil[2] if x < 8 else coil[1])
    return im


def attractor_a():
    return magnet(RED, IRON)


def attractor_b():
    """North/south magnet: left arm red, right arm blue, iron tips."""
    blue = [(18, 30, 96), (34, 62, 170), (52, 96, 220), (130, 160, 250)]
    im = magnet(RED, IRON)
    for y in range(16):
        for x in range(8, 16):
            p = im.getpixel((x, y))
            if p[3] and p[:3] in RED:
                px(im, x, y, blue[RED.index(p[:3])])
    return im


def attractor_c():
    return magnet(IRON, IRON, coil=COPPER)


# ================================================================== Amethyst-Resonanzstab
def rod_a():
    """Tuning fork of copper with amethyst tips, diagonal like vanilla tools."""
    rows = [
        '...........oo...',
        '..........oAAo..',
        '.......oo.oaAo..',
        '......oAAoocoo..',
        '......oaAocCo...',
        '.......ocCCo....',
        '......ocCCo.....',
        '.....ocCo.......',
        '....ocCo........',
        '...ocCo.........',
        '..ocCo..........',
        '.ocCo...........',
        '.oco............',
        '..o.............',
        '................',
        '................',
    ]
    return grid(rows, {'o': COPPER[0], 'c': COPPER[1], 'C': COPPER[3], 'A': AMETHYST[4], 'a': AMETHYST[2]})


def rod_b():
    """Wooden staff, copper ferrule, a three-crystal amethyst cluster on top."""
    rows = [
        '..........o.....',
        '.........oAo.o..',
        '......o..oAaoAo.',
        '.....oAo.oAaoao.',
        '.....oAaooaaoo..',
        '......oaaaaao...',
        '.......occco....',
        '......ocCco.....',
        '.....owwo.......',
        '....owwo........',
        '...owwo.........',
        '..owwo..........',
        '.owwo...........',
        '.owo............',
        '..o.............',
        '................',
    ]
    return grid(rows, {'o': (40, 26, 60), 'A': AMETHYST[4], 'a': AMETHYST[2], 'c': COPPER[1], 'C': COPPER[3], 'w': WOOD[2]})


def rod_c():
    """Resonator: a copper tube ending in a glowing amethyst point, rings along the tube."""
    rows = [
        '............oo..',
        '...........oAAo.',
        '..........oAEao.',
        '.........oAaao..',
        '........ocaao...',
        '.......ocCco....',
        '......ocCco.....',
        '.....orrro......',
        '....ocCco.......',
        '...ocCco........',
        '..orrro.........',
        '.ocCco..........',
        '.occo...........',
        '..oo............',
        '................',
        '................',
    ]
    return grid(rows, {'o': COPPER[0], 'c': COPPER[1], 'C': COPPER[3], 'r': COPPER[2], 'A': AMETHYST[3], 'a': AMETHYST[1], 'E': AMETHYST[4]})


# ================================================================== Echolot (bigger, echo detail)
def echo_a():
    """Deepslate ring, black face, three broken teal echo rings and a bright centre."""
    im = blank()
    disc(im, 8, 8, 8.0, 1.6, DEEP, SCULK[0], outline=(20, 20, 24))
    for r, c, gap in ((5.0, SCULK[2], (120, 160)), (3.4, SCULK[3], (300, 340)), (1.8, SCULK[4], (500, 500))):
        for a in range(0, 360, 8):
            if gap[0] <= a <= gap[1]:
                continue
            px(im, int(8 + math.cos(math.radians(a)) * r), int(8 - math.sin(math.radians(a)) * r), c)
    px(im, 7, 7, SCULK[5]); px(im, 8, 8, SCULK[4]); px(im, 7, 8, SCULK[4]); px(im, 8, 7, SCULK[5])
    return im


def echo_b():
    """Sculk handheld: dark body with sculk tendrils on top, screen with a returning wave."""
    rows = [
        '...o.o....o.o...',
        '...oto....oto...',
        '...oTo....oTo...',
        '.oooooooooooooo.',
        'oHhhhhhhhhhhhhmo',
        'ohsssssssssssshmo'[:16],
        'ohsaassssssllshm',
        'ohsasaassslslshm',
        'ohssssaaallssshm',
        'ohsssssssssssshm',
        'ohhhhhgghhhhhhmo',
        'ohhhhhgghhhhhhmo',
        'ommmmmmmmmmmmmmo',
        '.oooooooooooooo.',
        '................',
        '................',
    ]
    im = grid(rows, {'o': (20, 20, 24), 'H': DEEP[3], 'h': DEEP[2], 'm': DEEP[1], 's': SCULK[0], 'a': SCULK[3], 'l': SCULK[4],
                     't': SCULK[4], 'T': SCULK[3], 'g': SCULK[2]})
    return im


def echo_c():
    """Full-size recovery-compass style: echo shard in the middle, ripple arcs left and right."""
    im = blank()
    disc(im, 8, 8, 8.0, 1.4, [(20, 20, 24)] + SCULK[1:4], SCULK[0], outline=(13, 13, 13))
    for r, c in ((5.4, SCULK[2]), (4.0, SCULK[3])):
        for a in list(range(130, 231, 10)) + list(range(-50, 51, 10)):
            px(im, int(8 + math.cos(math.radians(a)) * r), int(8 - math.sin(math.radians(a)) * r), c)
    for x, y, c in ((8, 5, SCULK[5]), (7, 6, SCULK[4]), (8, 6, SCULK[4]), (7, 7, SCULK[3]), (8, 7, SCULK[4]), (9, 7, SCULK[3]),
                    (7, 8, SCULK[3]), (8, 8, SCULK[4]), (7, 9, SCULK[2]), (8, 9, SCULK[3]), (8, 10, SCULK[2])):
        px(im, x, y, c)
    return im


# ================================================================== Guide books
def book_front(cover, emblem, clasp):
    """Front view like the current guides, but vanilla-shaded: spine left, page edge right, emblem."""
    rows = [
        '................',
        '..oooooooooooo..',
        '.oSsCCCCCCCCCcp.',
        '.oSsCHHHHHHHCcp.',
        '.oSsCHeeeeeeCcp.',
        '.oSsCHeEEEEeCcp.',
        '.oSsCHeEEEEeCcp.',
        '.oSsCHeeeeeeCcp.',
        '.oSsCHHHHHHHCcp.',
        '.oSsCCCCCCCCCcp.',
        '.oSsCCCCCCCCKcp.',
        '.oSsCCCCCCCCKcp.',
        '.oSsccccccccccp.',
        '..oooooooooooPP.',
        '................',
        '................',
    ]
    return grid(rows, {'o': cover[0], 'S': cover[1], 's': cover[3], 'C': cover[2], 'c': cover[1], 'H': clasp[2],
                       'e': emblem[0], 'E': emblem[1], 'K': clasp[3], 'p': (228, 222, 204), 'P': (186, 178, 160)})


def book_vanilla_shape(cover, accent):
    """Vanilla item/book outline (diagonal), cover recoloured and a bookmark ribbon."""
    rows = [
        '................',
        '........ooo.....',
        '......ooCCCo....',
        '....ooCCCCCCo...',
        '..ooCCCHCCCCCo..',
        'ooCCCCCCHCCCCco.',
        'oCCCCCCCCHCCCcco',
        'ooCCCHCCCCCCccp.',
        'oscCCCCCCCccpPp.',
        'oPscCCCCCccpPPpc',
        '.oPscCCccpPPpcc.',
        '..oPsccpPPpcc...',
        '...oPspPpcc.....',
        '....ospcc..R....',
        '.....ooo...R....',
        '................',
    ]
    return grid(rows, {'o': cover[0], 'C': cover[2], 'c': cover[1], 'H': accent[1], 's': cover[3], 'p': (214, 214, 214),
                       'P': (168, 168, 168), 'R': accent[0]})


def guide_mod():
    blue = [(18, 26, 66), (36, 58, 132), (56, 92, 186), (96, 138, 222)]
    return [book_front(blue, [(196, 140, 30), (250, 214, 76)], GOLD),
            book_vanilla_shape(blue, [(210, 50, 40), (250, 214, 76)]),
            book_front(blue, [(250, 246, 228), (96, 138, 222)], IRON)]


def guide_vanilla():
    brown = [(46, 28, 10), (84, 52, 18), (120, 78, 30), (158, 112, 52)]
    return [book_front(brown, [(60, 120, 30), (110, 186, 60)], GOLD),
            book_vanilla_shape(brown, [(60, 140, 40), (110, 186, 60)]),
            book_front(brown, [(250, 246, 228), (110, 186, 60)], IRON)]


# ================================================================== Tiefenschiefer-Weisheitserz
def sage_ore(vanilla):
    base = Image.open(os.path.join(vanilla, 'block/deepslate.png')).convert('RGBA')
    xp = [(52, 92, 10), (118, 176, 24), (190, 236, 62), (246, 255, 168)]
    out = []
    # A: round experience nodules (2-3 px orbs with a light top-left)
    a = base.copy()
    for cx, cy in ((3, 3), (11, 2), (6, 8), (13, 10), (2, 12), (9, 13)):
        for x, y, c in ((0, 0, xp[2]), (1, 0, xp[1]), (0, 1, xp[1]), (1, 1, xp[0]), (-1, 0, xp[0]), (0, -1, xp[3])):
            px(a, cx + x, cy + y, c)
    out.append(a)
    # B: small crystal shards (diagonal 3 px) catching the light
    b = base.copy()
    for cx, cy in ((3, 4), (10, 3), (7, 9), (13, 11), (3, 13)):
        px(b, cx, cy, xp[3]); px(b, cx + 1, cy - 1, xp[2]); px(b, cx - 1, cy + 1, xp[1]); px(b, cx, cy + 1, xp[0]); px(b, cx + 1, cy, xp[1])
    out.append(b)
    # C: one thin glowing vein through the stone with brighter knots
    c = base.copy()
    path = [(1, 3), (2, 3), (3, 4), (4, 5), (5, 5), (6, 6), (7, 7), (8, 7), (9, 8), (10, 9), (11, 9), (12, 10), (13, 11), (14, 12),
            (9, 2), (10, 3), (10, 4), (4, 11), (5, 12), (6, 12), (7, 13)]
    for i, (x, y) in enumerate(path):
        px(c, x, y, xp[1] if i % 3 else xp[2])
    for x, y in ((7, 7), (12, 10), (10, 3), (5, 12)):
        px(c, x, y, xp[3])
    out.append(c)
    return out


# ================================================================== Weisheitskugel (experience orb)
def sage_orb(vanilla):
    sheet = Image.open(os.path.join(vanilla, 'entity/experience/experience_orb.png')).convert('RGBA')

    def frame(i, tint):
        f = sheet.crop(((i % 4) * 16, (i // 4) * 16, (i % 4) * 16 + 16, (i // 4) * 16 + 16))
        g = blank()
        for y in range(16):
            for x in range(16):
                p = f.getpixel((x, y))
                if p[3]:
                    g.putpixel((x, y), (round(p[0] * tint[0]), round(p[1] * tint[1]), round(p[2] * tint[2]), p[3]))
        return g
    # vanilla ExperienceOrbRenderer colour at the middle of its pulse: r 0.5, g 1.0, b 0.1
    exact = frame(4, (0.5, 1.0, 0.1))
    smaller = frame(2, (0.5, 1.0, 0.1))
    sparkle = frame(3, (0.62, 1.0, 0.16))
    px(sparkle, 10, 4, (255, 255, 230)); px(sparkle, 11, 3, (255, 255, 230))
    return [exact, smaller, sparkle]


# ================================================================== Eisenkern
def iron_core():
    a = blank()  # iron sphere with four rivets and a glowing light-blue eye
    disc(a, 8, 8, 6.8, 2.2, IRON, (40, 60, 80))
    for x, y in ((8, 3), (3, 8), (12, 8), (8, 12)):
        px(a, x, y, IRON[4])
    for x, y, c in ((7, 7, (200, 250, 255)), (8, 7, (120, 220, 250)), (7, 8, (120, 220, 250)), (8, 8, (60, 160, 220))):
        px(a, x, y, c)
    b = grid([  # octagonal iron casing like a conduit frame, bright core visible through the cross
        '................',
        '.....oooooo.....',
        '....oHHhhhmo....',
        '...oHhoCCohmo...',
        '..oHhoLCCcohmo..',
        '.oHhoLLCCccohmo.',
        '.oHoLLLCCcccomo.',
        '.ohoCCCWWCCComo.',
        '.ohoCCCWWCCComo.',
        '.ohoLLLCCcccomo.',
        '.ohhoLLCCccommo.',
        '..ohhoLCCcommo..',
        '...ohhoCCommo...',
        '....ommmmmmo....',
        '.....oooooo.....',
        '................',
    ], {'o': IRON[0], 'H': IRON[4], 'h': IRON[3], 'm': IRON[1], 'C': (110, 200, 236), 'c': (60, 140, 196), 'L': (180, 236, 250), 'W': (250, 255, 255)})
    c = grid([  # iron cage around a pale crystal heart
        '................',
        '.......oo.......',
        '......oHmo......',
        '....oohmmhoo....',
        '...oHo.CC.omo...',
        '..oHo.CLLc.omo..',
        '..oho.LWLc.omo..',
        '.oHhooCLLcoohmo.',
        '.ohmooccccoommo.',
        '..oho.cccc.omo..',
        '..oho..cc..omo..',
        '...omo....omo...',
        '....oommmmoo....',
        '......omo.......',
        '.......o........',
        '................',
    ], {'o': IRON[0], 'H': IRON[4], 'h': IRON[3], 'm': IRON[1], 'C': (150, 220, 240), 'c': (80, 160, 210), 'L': (200, 246, 255), 'W': (255, 255, 255)})
    return [a, b, c]


def current(path):
    for base in ('mc26_3/overlay/resources/assets/simplebuilding/textures/', 'src/main/resources/assets/simplebuilding/textures/'):
        p = os.path.join(ROOT, base, path + '.png')
        if os.path.exists(p):
            return Image.open(p).convert('RGBA').crop((0, 0, 16, 16))
    return blank()


def main():
    os.makedirs(os.path.join(OUT, 'png'), exist_ok=True)
    rows = [
        ('Messuhr', 'item/velocity_gauge', [gauge_a(), gauge_b(), gauge_c()]),
        ('Erzdetektor', 'item/detector', [detector_a(), detector_b(), detector_c()]),
        ('Attraktor', 'item/magnet', [attractor_a(), attractor_b(), attractor_c()]),
        ('Amethyst-Resonanzstab', 'item/amethyst_lens', [rod_a(), rod_b(), rod_c()]),
        ('Echolot', 'item/echo_sounder_00', [echo_a(), echo_b(), echo_c()]),
        ('Guide SimpleBuilding', 'item/guide_book', guide_mod()),
        ('Guide Vanilla', 'item/guide_book_vanilla_start', guide_vanilla()),
        ('Tiefenschiefer-Weisheitserz', 'block/deepslate_sage_ore', sage_ore(V)),
        ('Weisheitskugel', 'item/sage_orb', sage_orb(V)),
        ('Eisenkern', 'item/iron_core', iron_core()),
    ]
    s, cell, left = 6, 16 * 6 + 10, 200
    sheet = Image.new('RGBA', (left + 4 * cell + 4 * 40, len(rows) * cell + 30), (139, 139, 139, 255))
    d = ImageDraw.Draw(sheet)
    for i, h in enumerate(['jetzt', 'A', 'B', 'C']):
        d.text((left + i * cell + 40, 8), h, fill=(255, 255, 255))
    for r, (label, path, variants) in enumerate(rows):
        y = 26 + r * cell
        d.text((6, y + 40), label, fill=(255, 255, 255))
        for i, tex in enumerate([current(path)] + variants):
            sheet.alpha_composite(tex.resize((16 * s, 16 * s), Image.NEAREST), (left + i * cell, y))
        # 1x and 2x in an inventory-like slot strip, right of the big tiles
        for i, tex in enumerate([current(path)] + variants):
            x = left + 4 * cell + i * 40
            d.rectangle((x, y + 30, x + 17, y + 47), fill=(139, 139, 139), outline=(55, 55, 55))
            sheet.alpha_composite(tex, (x + 1, y + 31))
            sheet.alpha_composite(tex.resize((32, 32), Image.NEAREST), (x, y + 54))
        for label_, tex in zip('abc', variants):
            tex.save(os.path.join(OUT, 'png', f'{label.lower().replace(" ", "_")}_{label_}.png'))
    sheet.save(os.path.join(OUT, 'uebersicht.png'))
    print('ok ->', OUT)


if __name__ == '__main__':
    main()
