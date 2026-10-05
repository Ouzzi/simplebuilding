"""Usage: python tools/textures/texture_round6_2026_10_05.py [--check] [--preview DIR]

Texture round 6 (owner decisions 2026-10-05), one generator for the hand-drawn parts:
1. Training Dummy item icon: proposal C of round 5 (bust: wide carved pumpkin on the target torso, one plate) redrawn
   "more vanilla" - only vanilla colours (carved_pumpkin, hay_block_side, target_side, stone), light from the top
   left, a dark hue outline instead of black, no single-pixel noise. Writes item/training_dummy.png + wiki copy.
3. Simple Money fibres: "a mix of I (loop with beads) and J (spiral), colours roughly like the original": a closed
   loop whose strand is a coiled two-ply fibre (front windings light, back windings one tone darker, like J) with
   beads (like I) and a short coiled loose end; colours taken from the owner's original sprites (Special Fiber:
   purple + blue ply, tan/gold beads, dark brown shadow; Resin Fiber: white/grey ply, orange resin beads).
   Writes the module textures + wiki copies.
5. Simple Riding books Leaping and Tailwind: new motifs in the style of the owner's enchanted books (same book body
   as his books, cover recoloured, motif in light cover tones). Three variants each (A-C); INSTALL picks the one that
   is built in. Writes the module textures.
--check only compares the installed pixels. --preview DIR writes the per-topic sheets into DIR."""
import argparse
import colorsys
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
SB_TEX = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'item')
WIKI_TEX = os.path.join(ROOT, 'wiki', 'assets', 'textures')
MONEY_TEX = os.path.join(ROOT, 'modules', 'simplemoney', 'shared', 'resources', 'assets', 'simplemoney', 'textures', 'item')
RIDING_TEX = os.path.join(ROOT, 'modules', 'simpleriding', 'shared', 'resources', 'assets', 'simpleriding', 'textures', 'item')
OWNER_BOOKS = os.path.join(HERE, 'hand', 'q1', 'books')


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def draw(rows, pal):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    assert len(rows) == 16
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), pal[ch] + (255,))
    return img


def more_contrast(img, k=1.25, keep=None):
    """Owner 2026-10-05 ("etwas kontrastreicher"): every opaque pixel's RGB distance from the mean of the affected
    pixels times k, rounded and clamped; alpha, shape and motif unchanged. keep(pixel) -> True leaves a pixel alone
    (the grey page block of the enchanted books stays vanilla)."""
    pts = [(x, y) for y in range(img.height) for x in range(img.width)
           if img.getpixel((x, y))[3] and not (keep and keep(img.getpixel((x, y))))]
    if not pts:
        return img.copy()
    mean = [sum(img.getpixel(p)[i] for p in pts) / len(pts) for i in range(3)]
    out = img.copy()
    for p in pts:
        c = img.getpixel(p)
        out.putpixel(p, tuple(max(0, min(255, round(mean[i] + (c[i] - mean[i]) * k))) for i in range(3)) + (c[3],))
    return out


def spread(colors, k=1.25):
    """The same contrast step for a palette of '#rrggbb' colours (module guide covers)."""
    rgb = [hexrgb(c) for c in colors]
    mean = [sum(c[i] for c in rgb) / len(rgb) for i in range(3)]
    return tuple('#%02x%02x%02x' % tuple(max(0, min(255, round(mean[i] + (c[i] - mean[i]) * k))) for i in range(3))
                 for c in rgb)


# ---------------------------------------------------------------- 1. training dummy
DUMMY_PAL = {k: hexrgb(v) for k, v in {
    # carved_pumpkin / pumpkin_side
    'L': '#e3a64b', 'O': '#e3901d', 'M': '#c47614', 'D': '#a05a0b', 'E': '#7e3d0e',
    'f': '#2d0003', 'F': '#441300',
    # stem: pumpkin_top's stem greens are not on the side - armor stand wood browns (vanilla)
    's': '#5f4928', 'S': '#3d301e',
    # hay_block_side + twine
    'h': '#cbb630', 'H': '#bfab31', 'j': '#ab9225', 'k': '#94801e', 'K': '#8a7320',
    't': '#a4512b', 'T': '#87351c',
    # target_side
    'w': '#f3ebdf', 'W': '#ebd7ba', 'r': '#d53535', 'R': '#a43434',
    # stone plate (vanilla smooth stone tones)
    'g': '#a8a8a8', 'G': '#9d9d9d', 'q': '#6b6b6b', 'Q': '#535353',
}.items()}

DUMMY = [
    '................',
    '.......sS.......',
    '.....DDsSDE.....',
    '....DLOLOOME....',
    '....OffOMffE....',
    '....OOfOMfOE....',
    '....OfOOOOfE....',
    '....DMffffME....',
    '..jhtttttttTkK..',
    '....hrrrrrkK....',
    '....hrwwwrkK....',
    '....hrwrwrkK....',
    '....hrwwWrkK....',
    '....HRrrrRKK....',
    '...gggGGGGqQ....',
    '....qqqqqqQ.....',
]


def training_dummy():
    return draw(DUMMY, DUMMY_PAL)


# ---------------------------------------------------------------- 3. money fibres
FIBERS = {
    # colours of the owner's original sprites (git 0c8aa50d / hand/q1): ply A, ply B, back winding, beads, shadow
    'special_fiber': {'front': (hexrgb('#a25798'), hexrgb('#7e3d89')), 'back': hexrgb('#5677be'),
                      'back2': hexrgb('#697786'), 'bead': (hexrgb('#b67d5f'), hexrgb('#af7c49'), hexrgb('#7c5937')),
                      'shadow': hexrgb('#351f18')},
    'resin_fiber': {'front': (hexrgb('#d5d5d5'), hexrgb('#bdbdbd')), 'back': hexrgb('#767676'),
                    'back2': hexrgb('#585858'), 'bead': (hexrgb('#f7b128'), hexrgb('#dd5606'), hexrgb('#9a3403')),
                    'shadow': hexrgb('#3f3e3e')},
}


def fiber(m):
    """Loop (I) made of a coiled two-ply strand (J): a two pixel wide ring whose windings run across it as short
    diagonals - front windings in the two light ply tones, the parts running behind one tone darker; three beads
    sit on the ring. Shadow like vanilla string: one dark pixel under (or right of) the strand."""
    st = {}
    cx, cy = 7.5, 7.0
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx - 0.5, y + 0.5 - cy - 0.5)
            if 3.2 <= d <= 5.1:
                a = math.atan2(y + 0.5 - cy - 0.5, x + 0.5 - cx - 0.5)
                w = (a / (2 * math.pi) * 9 + (d - 3.4) * 0.3) % 1.0  # winding phase along the ring
                st[(x, y)] = m['front'][0] if w < 0.3 else m['front'][1] if w < 0.55 else m['back']
    # beads (I): 2x2, lit top left
    for bx, by in ((7, 1), (12, 7), (3, 9)):
        for (dx, dy), c in zip(((0, 0), (1, 0), (0, 1), (1, 1)), (m['bead'][0], m['bead'][1], m['bead'][1], m['bead'][2])):
            st[(bx + dx, by + dy)] = c
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y) in st:
        below, right = (x, y + 1), (x + 1, y)
        tgt = below if below not in st else right if right not in st else None
        if tgt and 0 <= tgt[0] < 16 and 0 <= tgt[1] < 16:
            im.putpixel(tgt, m['shadow'] + (255,))
    for p, c in st.items():
        im.putpixel(p, c + (255,))
    return im


# ---------------------------------------------------------------- 5. riding books
def owner_book(name):
    return Image.open(os.path.join(OWNER_BOOKS, f'enchanted_book_{name}.png')).convert('RGBA')


def is_page(p):
    return p[0] == p[1] == p[2]


def book_body(hue, sat, vk=1.0):
    """The owner's plain 'linear' book (same body as all his books), every coloured pixel recoloured to hue/sat with
    its own brightness kept (scaled by vk) - pages (grey) unchanged."""
    src = owner_book('linear')
    out = src.copy()
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if p[3] and not is_page(p):
                _, _, v = colorsys.rgb_to_hsv(*(c / 255 for c in p[:3]))
                r, g, b = colorsys.hsv_to_rgb(hue, sat, min(1.0, v * vk))
                out.putpixel((x, y), (round(r * 255), round(g * 255), round(b * 255), 255))
    return out


def cover_mask(img):
    """Cover pixels a motif may use: coloured, not on the silhouette edge, above the page block."""
    mask = set()
    for y in range(1, 10):
        for x in range(16):
            p = img.getpixel((x, y))
            if not p[3] or is_page(p):
                continue
            if all(0 <= x + dx < 16 and 0 <= y + dy < 16 and img.getpixel((x + dx, y + dy))[3]
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                mask.add((x, y))
    return mask


def tint(hue, sat, v):
    r, g, b = colorsys.hsv_to_rgb(hue, sat, v)
    return round(r * 255), round(g * 255), round(b * 255)


# Motifs in screen space (the owner's motifs are drawn straight onto the cover too). H highlight, L light, M mid,
# D a shade darker than the cover. Only cover pixels are painted.
RIDING_MOTIFS = {
    'leaping': {
        'A': ('Hufeisen als Bogen (Naegel)', [
            '................',
            '................',
            '................',
            '......HHHH......',
            '.....HLLLLH.....',
            '....HLD..DLH....',
            '....HL....LH....',
            '....LD....DL....',
            '....ML....LM....',
        ]),
        'B': ('Sprungbogen ueber Huerde', [
            '................',
            '................',
            '.........HH.....',
            '.......H....H...',
            '.....L.......H..',
            '....L.........H.',
            '...L...MM...HHH.',
            '.......MM....H..',
            '.......MM.......',
        ]),
        'C': ('kleines Hufeisen, Absprung-Striche', [
            '................',
            '................',
            '........HH......',
            '.......HLLH.....',
            '......HL..LH....',
            '......LD..DL....',
            '................',
            '.....M..M..M....',
            '.....M..M..M....',
        ]),
    },
    'tailwind': {
        'A': ('Windstriche mit Wirbel', [
            '................',
            '................',
            '........HH......',
            '.......H..H.....',
            '..........H.....',
            '..LLLLLLLH......',
            '................',
            '...MMMMMMMM.....',
            '...........M....',
        ]),
        'B': ('Feder (Kiel diagonal)', [
            '................',
            '................',
            '.........LH.....',
            '........LHL.....',
            '.......LHL......',
            '......LHL.......',
            '.....LHM........',
            '.....HM.........',
            '....H...........',
        ]),
        'C': ('Feder im Wind', [
            '................',
            '................',
            '.........LH.....',
            '........LHM.....',
            '.......LHM......',
            '......HM........',
            '.....H..........',
            '..MMMM..LLLLL...',
            '................',
        ]),
    },
}
RIDING_STYLE = {
    # hue, saturation, brightness factor of the body; motif tones are taken from the same hue
    'leaping': (0.30, 0.62, 1.0),   # jump-boost green
    'tailwind': (0.52, 0.5, 1.05),  # pale wind teal
}
INSTALL = {'leaping': 'A', 'tailwind': 'A'}


def riding_book(name, variant):
    hue, sat, vk = RIDING_STYLE[name]
    img = book_body(hue, sat, vk)
    mask = cover_mask(img)
    tones = {'H': tint(hue, sat * 0.12, 0.97), 'L': tint(hue, sat * 0.3, 0.86),
             'M': tint(hue, sat * 0.5, 0.7), 'D': tint(hue, sat, 0.2)}
    rows = RIDING_MOTIFS[name][variant][1]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.' and (x, y) in mask:
                img.putpixel((x, y), tones[ch] + (255,))
    return img


# ---------------------------------------------------------------- outputs
def outputs():
    out = {}
    dummy = training_dummy()
    out[os.path.join(SB_TEX, 'training_dummy.png')] = dummy
    out[os.path.join(WIKI_TEX, 'item', 'training_dummy.png')] = dummy
    for name, m in FIBERS.items():
        img = fiber(m)
        out[os.path.join(MONEY_TEX, f'{name}.png')] = img
        out[os.path.join(WIKI_TEX, 'simplemoney', 'item', f'{name}.png')] = img
    for name, v in INSTALL.items():
        out[os.path.join(RIDING_TEX, f'enchanted_book_{name}.png')] = riding_book(name, v)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    args = ap.parse_args()
    stale = []
    for path, img in outputs().items():
        if args.check:
            try:
                same = Image.open(path).convert('RGBA').tobytes() == img.tobytes()
            except OSError:
                same = False
            if not same:
                stale.append(os.path.relpath(path, ROOT))
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            img.save(path)
    if stale:
        raise SystemExit('stale: ' + ', '.join(stale))
    print('round 6 textures:', 'up to date' if args.check else 'written')


if __name__ == '__main__':
    main()
