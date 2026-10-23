"""Fear mobs (Dread, Hollow Watcher) textures, own pixel art: entity skins + glow layers, items, spawn eggs, effect icons.

    python tools/textures/fear_textures.py          # write PNGs + previews (~/previews/fear-mobs)
    python tools/textures/fear_textures.py --check  # fail if the committed PNGs differ

References (looked at, never copied or checked in): Vanilla iron golem / wither skeleton (dark iron, bone-black),
iron chain (chain link rhythm), anvil (heavy iron), Enderman + eyes layer (black body, violet eyes), End stone
(pale yellow palette, taken as colors only). Shapes are drawn here; the spawn eggs use Vanilla's common egg outline
so they sit in line with the other eggs.

UV layouts (128x128) match the Java models:
Dread (DreadModel): legs (0,0)/(0,21) 6x15x6, body (24,0) 16x15x9, faulds (24,24) 17x4x10, head (0,42) 10x11x10,
crest (40,42) 1x3x8, arms (78,0)/(102,0) 6x16x6, pauldrons (78,22)/(78,35) 9x5x8, handle (110,22) 2x6x2,
chain (118,22) 1x6x1, mace head (40,54) 6x6x6, spike (64,54) 2x2x2.
Watcher (WatcherModel): legs (0,0)/(12,0) 3x30x3, arms (24,0)/(36,0) 3x34x3, body (48,0) 9x16x5, head (76,0) 8x12x8,
neck (108,0) 2x3x2, thin shard (48,21) 2x5x2, thick shard (56,21) 3x3x3.
"""
import os
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/textures'
PREVIEW = Path(os.environ.get('SB_PREVIEW_DIR', os.path.expanduser('~/previews')) + '/fear-mobs')

T = (0, 0, 0, 0)
# Dread: dark blued iron with rust, soul light
IRON = [(0x2a, 0x2c, 0x31), (0x36, 0x39, 0x3f), (0x44, 0x48, 0x4f), (0x55, 0x5a, 0x62)]
IRON_HI = (0x74, 0x7a, 0x83)
IRON_DK = (0x1a, 0x1b, 0x1f)
RUST = [(0x55, 0x2c, 0x1a), (0x74, 0x3a, 0x1e), (0x92, 0x50, 0x25), (0xae, 0x66, 0x2d)]
HOLLOW = (0x08, 0x0a, 0x0e)
SOUL = [(0x0e, 0x6a, 0x72), (0x2f, 0xc6, 0xd0), (0x7f, 0xf4, 0xfa), (0xd4, 0xff, 0xff)]
LEATHER = [(0x2e, 0x21, 0x18), (0x45, 0x31, 0x22)]
# Watcher (N50 camouflage): a dim End stone coloured shadow that blends into the islands, End stone splinters, many eyes
# (the eye glow layer only shows once it is triggered, see WatcherRenderer)
SHADE = [(0x2c, 0x2a, 0x20), (0x37, 0x34, 0x27), (0x44, 0x40, 0x30), (0x53, 0x4e, 0x3a)]
SHADE_HI = (0x69, 0x63, 0x47)
ENDSTONE = [(0xc5, 0xbe, 0x8b), (0xdb, 0xdd, 0x9e), (0xee, 0xf6, 0xb4), (0xa8, 0x9f, 0x6e)]
EYE_DIM = (0x22, 0x1f, 0x2a)
EYE = [(0x9b, 0x2c, 0xd6), (0xd8, 0x6a, 0xff), (0xf6, 0xe6, 0xff)]


def noise(x, y, k=0):
    return ((x * 73856093) ^ (y * 19349663) ^ (k * 83492791)) & 0xFF


def faces(u, v, w, h, d):
    """Cube unwrap: name -> (x, y, width, height)."""
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


def paint(img, u, v, w, h, d, fn, k=0):
    """Fills every face of a box: fn(face, lx, ly, fw, fh, gx, gy, k) -> color or None (keep)."""
    for name, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
        for ly in range(fh):
            for lx in range(fw):
                c = fn(name, lx, ly, fw, fh, x0 + lx, y0 + ly, k)
                if c is not None:
                    img.putpixel((x0 + lx, y0 + ly), c if len(c) == 4 else c + (255,))


# ---------------------------------------------------------------------------------------------------- Dread

def iron(gx, gy, k, rusty=True):
    n = noise(gx, gy, k)
    c = IRON[1] if n < 120 else IRON[2] if n < 225 else IRON[0] if n < 245 else IRON[3]
    if rusty:
        r = noise(gx // 2, gy // 2, k + 31)
        if r < 14:
            c = RUST[(r + gx) % 2]
        elif r < 17:
            c = RUST[2]
    return c


def plate(face, lx, ly, fw, fh, gx, gy, k):
    """A riveted armor plate: light top edge, dark lower edge, rivets in the corners, rust patches."""
    c = iron(gx, gy, k)
    if ly == 0 and face not in ('top', 'bottom'):
        c = IRON_HI if lx % 3 else IRON[3]
    elif ly == fh - 1 and face not in ('top', 'bottom'):
        c = IRON_DK
    if face in ('front', 'back', 'left', 'right') and fw >= 5 and fh >= 5 and ly in (1, fh - 2) and lx in (1, fw - 2):
        c = IRON_HI
    return c


def leg(face, lx, ly, fw, fh, gx, gy, k):
    c = plate(face, lx, ly, fw, fh, gx, gy, k)
    if face in ('front', 'back', 'left', 'right'):
        if ly in (6, 7):  # knee cop
            c = IRON[3] if ly == 6 else IRON[0]
            if face == 'front' and lx in (2, 3) and ly == 6:
                c = IRON_HI
        if ly >= fh - 3:  # sabaton
            c = IRON[0] if ly == fh - 1 else IRON[1]
            if ly == fh - 3:
                c = IRON[3]
    return c


def torso(face, lx, ly, fw, fh, gx, gy, k):
    c = plate(face, lx, ly, fw, fh, gx, gy, k)
    if face == 'front':
        mid = fw // 2
        if lx in (mid - 1, mid) and 1 <= ly <= fh - 3:  # center ridge
            c = IRON[3] if lx == mid - 1 else IRON_HI
        if ly in (5, 10) and 1 <= lx <= fw - 2:  # plate seams
            c = IRON_DK
        # diagonal chain bandolier, left shoulder to right hip
        t = lx + ly - 2
        if 0 <= lx < fw and abs(lx - (ly * fw) // fh) <= 0 and 1 <= ly <= fh - 2:
            c = (0x59, 0x60, 0x6b) if ly % 2 else (0x25, 0x2c, 0x3d)
        # cracks that show the soul light (glow layer)
        if (lx, ly) in CRACKS:
            c = HOLLOW
    if face == 'back' and ly in (5, 10) and 1 <= lx <= fw - 2:
        c = IRON_DK
    return c


CRACKS = {(11, 3), (12, 4), (12, 5), (4, 11), (5, 12)}


def faulds(face, lx, ly, fw, fh, gx, gy, k):
    c = iron(gx, gy, k)
    if face in ('front', 'back', 'left', 'right'):
        if lx % 3 == 2:
            c = IRON_DK
        elif ly == 0:
            c = IRON[3]
        elif ly == fh - 1:
            c = RUST[1] if noise(gx, gy, 5) < 120 else IRON[0]
    return c


def helmet(face, lx, ly, fw, fh, gx, gy, k):
    c = iron(gx, gy, k + 3)
    if face in ('top',):
        c = IRON[2] if noise(gx, gy, 9) < 160 else IRON[3]
        if lx in (fw // 2 - 1, fw // 2):
            c = IRON[0]
        return c
    if face == 'bottom':
        return HOLLOW if 1 <= lx <= fw - 2 and 1 <= ly <= fw - 2 else IRON_DK
    if ly == 0:
        c = IRON_HI if lx % 2 else IRON[3]
    if ly == fh - 1:
        c = IRON_DK
    if face == 'front':
        mid = fw // 2
        if ly == 4 and 1 <= lx <= fw - 2 and lx not in (mid - 1, mid):  # visor slit
            c = HOLLOW
        if ly == 3 and 1 <= lx <= fw - 2:
            c = IRON[3]
        if ly == 5 and 1 <= lx <= fw - 2 and lx not in (mid - 1, mid):
            c = IRON_DK
        if lx in (mid - 1, mid) and 2 <= ly <= fh - 2:  # nose guard
            c = IRON_HI if lx == mid - 1 else IRON[3]
        if ly in (7, 8) and lx in (2, 3, 6, 7) and (lx + ly) % 2 == 0:  # breathing holes
            c = HOLLOW
    if face in ('left', 'right') and ly == 4 and 2 <= lx <= fw - 3:
        c = IRON[0]
    return c


def crest(face, lx, ly, fw, fh, gx, gy, k):
    return RUST[2] if (lx + ly) % 3 else RUST[1]


def arm(face, lx, ly, fw, fh, gx, gy, k):
    c = plate(face, lx, ly, fw, fh, gx, gy, k + 11)
    if face in ('front', 'back', 'left', 'right'):
        if ly in (7, 8):  # elbow
            c = IRON[3] if ly == 7 else IRON[0]
        if ly >= fh - 4:  # gauntlet
            c = IRON[1] if (lx + ly) % 4 else IRON[0]
            if ly == fh - 4:
                c = IRON[3]
            if face == 'front' and ly == fh - 2 and lx % 2 == 1:
                c = IRON_HI
    if face == 'bottom':
        c = IRON[0]
    return c


def pauldron(face, lx, ly, fw, fh, gx, gy, k):
    c = iron(gx, gy, k + 17)
    if face in ('front', 'back', 'left', 'right'):
        c = IRON[3] if ly % 2 == 0 else IRON[1]
        if ly == fh - 1:
            c = IRON_DK
        if noise(gx, gy, 19) < 40:
            c = RUST[noise(gx, gy, 20) % 3]
    elif face == 'top':
        if lx == 0 or lx == fw - 1 or ly == 0 or ly == fh - 1:
            c = IRON[3]
        if (lx, ly) in ((2, 2), (fw - 3, 2), (2, fh - 3), (fw - 3, fh - 3)):
            c = IRON_HI
    return c


def handle(face, lx, ly, fw, fh, gx, gy, k):
    if ly in (0, fh - 1):
        return IRON[3]
    return LEATHER[(ly + lx) % 2]


def chain(face, lx, ly, fw, fh, gx, gy, k):
    return (0x59, 0x60, 0x6b) if ly % 3 == 0 else (0x3e, 0x44, 0x53) if ly % 3 == 1 else (0x25, 0x2c, 0x3d)


def mace(face, lx, ly, fw, fh, gx, gy, k):
    c = iron(gx, gy, k + 23)
    if (lx in (0, fw - 1)) != (ly in (0, fh - 1)):
        c = IRON[0]
    if lx in (2, 3) and ly in (2, 3) and face != 'top':
        c = IRON[3] if (lx + ly) % 2 else IRON[2]
    return c


def spike(face, lx, ly, fw, fh, gx, gy, k):
    return IRON_HI if (lx + ly) % 2 == 0 else IRON[3]


def dread_textures():
    img = Image.new('RGBA', (128, 128), T)
    paint(img, 0, 0, 6, 15, 6, leg, 1)
    paint(img, 0, 21, 6, 15, 6, leg, 2)
    paint(img, 24, 0, 16, 15, 9, torso, 3)
    paint(img, 24, 24, 17, 4, 10, faulds, 4)
    paint(img, 0, 42, 10, 11, 10, helmet, 5)
    paint(img, 40, 42, 1, 3, 8, crest, 6)
    paint(img, 78, 0, 6, 16, 6, arm, 7)
    paint(img, 102, 0, 6, 16, 6, arm, 8)
    paint(img, 78, 22, 9, 5, 8, pauldron, 9)
    paint(img, 78, 35, 9, 5, 8, pauldron, 10)
    paint(img, 110, 22, 2, 6, 2, handle, 11)
    paint(img, 118, 22, 1, 6, 1, chain, 12)
    paint(img, 40, 54, 6, 6, 6, mace, 13)
    paint(img, 64, 54, 2, 2, 2, spike, 14)
    # glow layer: soul flames behind the visor slit and breathing holes, light through the breastplate cracks
    glow = Image.new('RGBA', (128, 128), T)
    fx, fy, fw, fh = faces(0, 42, 10, 11, 10)['front']
    for lx in range(1, fw - 1):
        if lx in (4, 5):
            continue
        glow.putpixel((fx + lx, fy + 4), SOUL[2] + (255,) if lx in (2, 7) else SOUL[1] + (255,))
    for lx, ly in ((2, 7), (6, 7), (3, 8), (7, 8)):
        glow.putpixel((fx + lx, fy + ly), SOUL[0] + (255,))
    glow.putpixel((fx + 2, fy + 3), SOUL[0] + (200,))
    glow.putpixel((fx + 7, fy + 3), SOUL[0] + (200,))
    bx, by, _, _ = faces(24, 0, 16, 15, 9)['front']
    for lx, ly in CRACKS:
        glow.putpixel((bx + lx, by + ly), SOUL[1] + (255,))
    bx, by, bw, bh = faces(0, 42, 10, 11, 10)['bottom']  # flames seen from below the open helmet
    for ly in range(2, bh - 2):
        for lx in range(2, bw - 2):
            if noise(lx, ly, 41) < 110:
                glow.putpixel((bx + lx, by + ly), SOUL[noise(lx, ly, 42) % 2] + (255,))
    return img, glow


# ---------------------------------------------------------------------------------------------------- Watcher

def shade(gx, gy, k):
    n = noise(gx, gy, k)
    c = SHADE[1] if n < 110 else SHADE[0] if n < 190 else SHADE[2] if n < 245 else SHADE[3]
    if noise(gx // 3, gy // 2, k + 7) < 28:
        c = SHADE_HI if n & 1 else SHADE[3]
    return c


def limb(face, lx, ly, fw, fh, gx, gy, k):
    c = shade(gx, gy, k)
    if face in ('front', 'back', 'left', 'right') and ly >= fh - 4:  # long dark fingers / feet
        c = SHADE[0] if (lx + ly) % 2 else SHADE[1]
    if (gx, gy) in EYES_LIMB:
        c = EYE_DIM
    return c


def wbody(face, lx, ly, fw, fh, gx, gy, k):
    c = shade(gx, gy, k)
    if face == 'front' and ly % 3 == 1 and 1 <= lx <= fw - 2 and ly < 10:  # rib shadows
        c = SHADE[3] if lx not in (fw // 2,) else SHADE[0]
    if (gx, gy) in EYES_BODY:
        c = EYE_DIM
    return c


def whead(face, lx, ly, fw, fh, gx, gy, k):
    c = shade(gx, gy, k)
    if face == 'bottom':
        c = SHADE[0]
    if (gx, gy) in EYES_HEAD:
        c = EYE_DIM
    return c


def shard(face, lx, ly, fw, fh, gx, gy, k):
    n = noise(gx, gy, k)
    c = ENDSTONE[0] if n < 100 else ENDSTONE[1] if n < 210 else ENDSTONE[2]
    if face in ('front', 'right') and ly == fh - 1:
        c = ENDSTONE[3]
    if face == 'top':
        c = ENDSTONE[2]
    return c


def eye_pixels():
    """Where the eyes sit (texture coordinates): (x, y, size) with size 2 = two pixels wide."""
    hx, hy, _, _ = faces(76, 0, 8, 12, 8)['front']
    head = [(1, 3, 2), (5, 2, 2), (3, 6, 2), (0, 8, 1), (6, 7, 1), (2, 10, 1), (5, 10, 2), (7, 4, 1)]
    out_head = []
    for lx, ly, s in head:
        out_head.append((hx + lx, hy + ly, s))
    for side in ('left', 'right', 'back'):
        sx, sy, sw, sh = faces(76, 0, 8, 12, 8)[side]
        for lx, ly, s in ((2, 3, 1), (5, 7, 2), (1, 9, 1)):
            out_head.append((sx + lx, sy + ly, s))
    bx, by, _, _ = faces(48, 0, 9, 16, 5)['front']
    body = [(bx + 2, by + 2, 2), (bx + 6, by + 5, 1), (bx + 3, by + 11, 1), (bx + 6, by + 13, 2)]
    lx_, ly_, _, _ = faces(24, 0, 3, 34, 3)['front']
    rx_, ry_, _, _ = faces(36, 0, 3, 34, 3)['front']
    limbs = [(lx_ + 1, ly_ + 6, 1), (lx_ + 0, ly_ + 19, 1), (rx_ + 1, ry_ + 12, 1), (rx_ + 2, ry_ + 25, 1)]
    return out_head, body, limbs


EYES_HEAD_L, EYES_BODY_L, EYES_LIMB_L = eye_pixels()


def _cells(lst):
    s = set()
    for x, y, n in lst:
        for i in range(n):
            s.add((x + i, y))
    return s


EYES_HEAD, EYES_BODY, EYES_LIMB = _cells(EYES_HEAD_L), _cells(EYES_BODY_L), _cells(EYES_LIMB_L)


def watcher_textures():
    img = Image.new('RGBA', (128, 128), T)
    paint(img, 0, 0, 3, 30, 3, limb, 1)
    paint(img, 12, 0, 3, 30, 3, limb, 2)
    paint(img, 24, 0, 3, 34, 3, limb, 3)
    paint(img, 36, 0, 3, 34, 3, limb, 4)
    paint(img, 48, 0, 9, 16, 5, wbody, 5)
    paint(img, 76, 0, 8, 12, 8, whead, 6)
    paint(img, 108, 0, 2, 3, 2, lambda f, lx, ly, fw, fh, gx, gy, k: SHADE[1], 7)
    paint(img, 48, 21, 2, 5, 2, shard, 8)
    paint(img, 56, 21, 3, 3, 3, shard, 9)
    eyes = Image.new('RGBA', (128, 128), T)
    for x, y, n in EYES_HEAD_L + EYES_BODY_L + EYES_LIMB_L:
        if n == 2:  # a wide eye: violet rim, pale pupil
            eyes.putpixel((x, y), EYE[1] + (255,))
            eyes.putpixel((x + 1, y), EYE[2] + (255,))
        else:
            eyes.putpixel((x, y), EYE[0] + (255,))
    return img, eyes


# ---------------------------------------------------------------------------------------------------- items

EGG = ["......aaaa......", ".....abcdba.....", "....abcebcba....", "...abcbbefcda...", "...acececgcbh...",
       "..abfggcegcbdh..", "..abggfcefcbdh..", "..aefgfccfcbbh..", "..afecccccebeh..", "..adcfffccffdh..",
       "..aeddbeebddeh..", "..abcfcbbcfcbh..", "...adbeeeebdh...", "....hddbbddh....", ".....hhhhhh.....", "................"]


def grid(rows, pal):
    im = Image.new('RGBA', (16, 16), T)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                c = pal[ch]
                im.putpixel((x, y), c if len(c) == 4 else c + (255,))
    return im


def egg(base, shadow, rim, spots, spot_cells):
    """Vanilla's egg outline, own colors and spots: base/shadow shading by distance from the light (top left)."""
    im = Image.new('RGBA', (16, 16), T)
    for y, row in enumerate(EGG):
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            edge = ch in 'ah'
            if edge:
                c = rim
            else:
                d = (x - 6) + (y - 5)
                c = base[2] if d < -2 else base[1] if d < 5 else base[0] if d < 9 else shadow
            im.putpixel((x, y), c + (255,))
    for (x, y), c in zip(spot_cells, spots):
        if im.getpixel((x, y))[3]:
            im.putpixel((x, y), c + (255,))
    return im


def dread_egg():
    spots = [SOUL[1], SOUL[2], RUST[2], RUST[1], SOUL[1], RUST[3], IRON_HI, SOUL[0], RUST[2]]
    cells = [(6, 3), (7, 3), (9, 6), (10, 6), (5, 8), (11, 9), (7, 4), (8, 11), (4, 11)]
    return egg([IRON[1], IRON[2], IRON[3]], IRON[0], IRON_DK, spots, cells)


def watcher_egg():
    spots = [EYE[1], EYE[2], EYE[1], EYE[2], EYE[0], ENDSTONE[1], ENDSTONE[2], EYE[0]]
    cells = [(6, 4), (7, 4), (9, 7), (10, 7), (5, 9), (11, 10), (11, 11), (7, 12)]
    return egg([SHADE[1], SHADE[2], SHADE[3]], SHADE[0], (0x05, 0x04, 0x08), spots, cells)


def dread_link():
    """One heavy oval chain link lying diagonally, rusted, with a broken half link hanging from it."""
    rows = ["................",
            "................",
            ".........aaaa...",
            "........abccda..",
            ".......abd..eda.",
            "......abd....ea.",
            ".....abd....eda.",
            "....abd....eda..",
            "...abd....eda...",
            "..abd....eda....",
            "..ab....eda.....",
            "..afd..eda......",
            "...affeda.......",
            "....aaaa.g......",
            "........gh......",
            "........h......."]
    pal = {'a': (0x14, 0x15, 0x19), 'b': IRON_HI, 'c': IRON[3], 'd': IRON[2], 'e': RUST[1], 'f': RUST[2],
           'g': IRON[1], 'h': RUST[0]}
    return grid(rows, pal)


def calm_amulet():
    """A thin chain loop holding an iron diamond setting with an amethyst stone."""
    rows = ["....cdcdcdcd....",
            "...c........c...",
            "..d..........d..",
            "..c..........c..",
            "...d........d...",
            "....c......c....",
            ".....dc..cd.....",
            ".......ab.......",
            "......abbe......",
            ".....abffbe.....",
            "....abfgfhbe....",
            "....bfgihhfe....",
            ".....bfhhfe.....",
            "......bffe......",
            ".......be.......",
            "........e......."]
    pal = {'a': IRON_HI, 'b': IRON[3], 'c': (0x59, 0x60, 0x6b), 'd': (0x3e, 0x44, 0x53), 'e': IRON[0],
           'f': (0x8d, 0x6c, 0xe0), 'g': (0xcf, 0xa0, 0xff), 'h': (0x6a, 0x4a, 0xb4), 'i': (0xf1, 0xe4, 0xff)}
    return grid(rows, pal)


def watcher_eye():
    """A dark eyeball in a wisp of shadow: violet iris, pale pupil glint, one End stone splinter."""
    rows = ["................",
            "................",
            ".....aaaaaa.....",
            "....abbccbba....",
            "...abccddccba...",
            "..abcdeffedcba..",
            "..abdeghhgedba..",
            "..acdfhiihfdca..",
            "..acdfhjihfdca..",
            "..abdeghhgedba..",
            "..abcdeffedcb.k.",
            "...abccddccb.kl.",
            "....abbccbba.l..",
            ".....aaaaaa.....",
            "................",
            "................"]
    pal = {'a': (0x06, 0x05, 0x09), 'b': SHADE[1], 'c': SHADE[2], 'd': SHADE[3], 'e': SHADE_HI, 'f': EYE[0],
           'g': (0x6b, 0x1f, 0x96), 'h': EYE[1], 'i': (0x1a, 0x05, 0x24), 'j': EYE[2], 'k': ENDSTONE[2], 'l': ENDSTONE[0]}
    return grid(rows, pal)


def icon(rows, pal):
    im = Image.new('RGBA', (18, 18), T)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                im.putpixel((x + 1, y + 1), pal[ch] + (255,))
    return im


def focus_icon():
    """A calm open eye with a steady cross in the pupil."""
    rows = ["................",
            "................",
            "................",
            ".....aaaaaa.....",
            "...aabbbbbbaa...",
            "..abbccddccbba..",
            ".abbcdeffedcbba.",
            "abbccdfgffdccbba",
            ".abbcdeffedcbba.",
            "..abbccddccbba..",
            "...aabbbbbbaa...",
            ".....aaaaaa.....",
            "................",
            "................",
            "................",
            "................"]
    pal = {'a': (0x3b, 0x22, 0x5c), 'b': (0xe9, 0xe1, 0xf4), 'c': (0xb8, 0x8c, 0xe8), 'd': (0x84, 0x52, 0xc9),
           'e': (0x4b, 0x25, 0x7d), 'f': (0x1c, 0x0d, 0x2e), 'g': (0xff, 0xff, 0xff)}
    return icon(rows, pal)


def shivering_icon():
    """Three trembling ice-blue strokes, each a small zigzag (only used without SimpleBuilding)."""
    im = Image.new('RGBA', (18, 18), T)
    wave = [0, 1, 1, 0, -1, -1]
    for col, x0 in enumerate((4, 9, 14)):
        for y in range(2, 16):
            x = x0 + wave[(y + col * 2) % 6]
            im.putpixel((x, y), (0x9f, 0xd3, 0xea, 255))
            im.putpixel((x + 1, y), (0x4f, 0x87, 0xb0, 255))
        im.putpixel((x0 + wave[(2 + col * 2) % 6], 2), (0xe6, 0xfb, 0xff, 255))
    return im


def outputs():
    dread, dread_glow = dread_textures()
    watcher, eyes = watcher_textures()
    return {
        RES / 'entity/dread/dread.png': dread,
        RES / 'entity/dread/dread_glow.png': dread_glow,
        RES / 'entity/hollow_watcher/hollow_watcher.png': watcher,
        RES / 'entity/hollow_watcher/hollow_watcher_eyes.png': eyes,
        RES / 'item/dread_link.png': dread_link(),
        RES / 'item/calm_amulet.png': calm_amulet(),
        RES / 'item/watcher_eye.png': watcher_eye(),
        # item/dread_spawn_egg.png: W24-1: spawn egg drawn by tools/textures/spawn_eggs_2026_10_23.py (style B, owner 23.10.)
        # item/hollow_watcher_spawn_egg.png: likewise
        RES / 'mob_effect/focus.png': focus_icon(),
        RES / 'mob_effect/shivering.png': shivering_icon(),
    }


def preview(items):
    PREVIEW.mkdir(parents=True, exist_ok=True)
    small = [p for p in items if 'entity' not in str(p)]
    sheet = Image.new('RGBA', (len(small) * 300 + 20, 340), (48, 48, 54, 255))
    d = ImageDraw.Draw(sheet)
    for i, p in enumerate(small):
        im = items[p]
        big = im.resize((im.width * 16, im.height * 16), Image.NEAREST)
        sheet.paste(big, (20 + i * 300, 30), big)
        d.text((20 + i * 300, 8), p.stem, fill=(255, 255, 255, 255))
    sheet.save(PREVIEW / 'items.png')
    for p in items:
        if 'entity' in str(p):
            im = items[p]
            bg = Image.new('RGBA', im.size, (90, 90, 96, 255))
            bg.alpha_composite(im)
            bg.resize((im.width * 6, im.height * 6), Image.NEAREST).save(PREVIEW / f'skin-{p.stem}.png')


def main():
    items = outputs()
    if '--check' in sys.argv:
        bad = [str(p) for p, im in items.items() if not p.is_file() or Image.open(p).convert('RGBA').tobytes() != im.tobytes()]
        if bad:
            print('stale:', *bad, sep='\n  '); sys.exit(1)
        print('fear textures current'); return
    for p, im in items.items():
        p.parent.mkdir(parents=True, exist_ok=True); im.save(p)
    preview(items)


if __name__ == '__main__':
    main()
