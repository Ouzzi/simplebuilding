"""Usage: python tools/textures/proposals_v9_2026_10_02.py <preview dir>

Round 9 (owner 2026-10-02):
- Resonance Rod: round-8 no. 1, the main shard's tip sharper, the left shard one pixel down and one right (A/B/C).
- Copper core: round-8 no. 9 ("rund") with the copper behind the star removed, so in each 45-degree direction exactly
  three copper pixels touch the star (A/B/C).
- Small parts: Stone Pebble, Flint Chip, placed egg - three proposals each."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import resonance_rod_2026_10_02 as rod  # noqa: E402
import proposals_v8_2026_10_02 as v8  # noqa: E402
import cores_2026_10_02 as cores  # noqa: E402

OUT = sys.argv[1] if len(sys.argv) > 1 else 'build/proposals-v9/'

LEFT = [  # the upright shard, already one pixel down and one right of round 8
    '................',
    '........o.......',
    '.......obo......',
    '.......ofgo.....',
    '.......odgo.....',
    '........oa......',
]
TIPS = {
    'A': [  # single tip pixel, shard narrows over two rows
        '..............o.',
        '.............obo',
        '............obedo',
        '...........obfdo.',
        '..........ofdgo..',
        '.........odfgo...',
        '.........gdgooo..',
        '..........gbedo..',
        '...........ogo...',
    ],
    'B': [  # tip in the very corner, a longer point
        '...............o',
        '..............oe',
        '.............obo',
        '............obdo',
        '...........ofdgo',
        '..........odfgo.',
        '.........gdgooo.',
        '..........gbedo.',
        '...........ogo..',
    ],
    'C': [  # like round 8 but the flat top cut to one lit pixel with a dark facet beside it
        '..............o.',
        '.............oeo',
        '............obfo',
        '...........obfdo',
        '..........ofdgo.',
        '.........odfgo..',
        '.........gdgooo.',
        '..........gbedo.',
        '...........ogo..',
    ],
}


def rod_variant(tip):
    rows = [list('................') for _ in range(16)]
    for i, r in enumerate(v8.SHAFTS['stock']):
        for x, ch in enumerate(r):
            if ch != '.':
                rows[6 + i][x] = ch
    for layer in (LEFT, TIPS[tip]):
        for y, r in enumerate(layer):
            for x, ch in enumerate(r[:16]):
                if ch != '.':
                    rows[y][x] = ch
    return rod.image([''.join(r) for r in rows], rod.PAL)


# ------------------------------------------------------------------ copper core
STAR_TOUCH = {
    # top-left quadrant cells that may touch the star diagonally; per variant the three that stay
    'A': [(6, 6), (5, 6), (6, 5)],          # the rounded block's inner corner
    'B': [(6, 6), (5, 5), (6, 5)],          # a short diagonal with one side pixel
    'C': [(6, 6), (5, 5), (4, 4)],          # a straight diagonal spoke
}


def core_variant(v):
    im = v8.copper_core('rund')
    star = cores.star_colours(Image.open(os.path.join(cores.SRC, 'copper_core.png')).convert('RGBA').crop((0, 0, 16, 16)))
    for sx in (-1, 1):
        for sy in (-1, 1):
            def m(p):
                return (p[0] if sx < 0 else 16 - p[0], p[1] if sy < 0 else 16 - p[1])
            # remove the copper hidden behind the star: every non-star pixel of this quadrant that touches a star
            # pixel and is not one of the three kept cells
            keep = {m(p) for p in STAR_TOUCH[v]}
            for x in range(3, 8):
                for y in range(3, 8):
                    q = m((x, y))
                    p = im.getpixel(q)
                    if not p[3] or p[:3] in star or q in keep:
                        continue
                    touches = any(0 <= q[0] + dx < 16 and 0 <= q[1] + dy < 16 and im.getpixel((q[0] + dx, q[1] + dy))[:3] in star
                                  and im.getpixel((q[0] + dx, q[1] + dy))[3]
                                  for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                    if touches:
                        im.putpixel(q, (0, 0, 0, 0))
            for i, p in enumerate(STAR_TOUCH[v]):
                tone = [4, 3, 3][i] + v8.SHADE[(sx, sy)]
                im.putpixel(m(p), v8.COPPER[max(0, min(5, tone))] + (255,))
    return im


# ------------------------------------------------------------------ small parts
STONE = [(68, 68, 68), (100, 100, 100), (126, 126, 126), (150, 150, 150), (178, 178, 178)]
FLINT = [(30, 30, 32), (52, 52, 56), (74, 74, 80), (100, 100, 106), (140, 140, 146)]


def draw(rows, pal):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in pal:
                im.putpixel((x, y), pal[ch] + (255,))
    return im


def pebbles():
    p = {'o': STONE[0], 'd': STONE[1], 'm': STONE[2], 'l': STONE[3], 'h': STONE[4]}
    a = draw(['................'] * 5 + [
        '......oooo......',
        '....oollmmoo....',
        '...olhllmmmdo...',
        '...ollmmmmddo...',
        '...odmmmmddoo...',
        '....oddddoo.....',
        '.....oooo.......'] + ['................'] * 4, p)  # one round pebble
    b = draw(['................'] * 4 + [
        '..........ooo...',
        '.........ollmo..',
        '...ooo...omddo..',
        '..ollmo...ooo...',
        '..omddo.........',
        '...ooo...oooo...',
        '........ollmmo..',
        '........odddoo..',
        '.........ooo....'] + ['................'] * 3, p)  # three small pebbles
    c = draw(['................'] * 5 + [
        '.....ooooo......',
        '...oolhlmmoo....',
        '..ollmmmmmddo...',
        '..odmmmmdddoo...',
        '...ooddddoo.....',
        '.....oooo.......'] + ['................'] * 5, p)  # flat skipping stone
    return [a, b, c]


def flint_chips():
    p = {'o': FLINT[0], 'd': FLINT[1], 'm': FLINT[2], 'l': FLINT[3], 'h': FLINT[4]}
    a = draw(['................'] * 4 + [
        '.........oo.....',
        '........olho....',
        '.......olmmo....',
        '......olmmdo....',
        '.....olmmdo.....',
        '.....ommdo......',
        '......odo.......',
        '.......o........'] + ['................'] * 4, p)  # a sharp flake
    b = draw(['................'] * 5 + [
        '.......ooo......',
        '......ohlmo.....',
        '.....olmmddo....',
        '....olmmddo.....',
        '....oddddo......',
        '.....oooo.......'] + ['................'] * 5, p)  # a triangular chip
    c = draw(['................'] * 4 + [
        '...........o....',
        '..oo......olo...',
        '..olo....olmo...',
        '...omo..olmdo...',
        '...odo..omdo....',
        '....o...odo.....',
        '.........o......'] + ['................'] * 5, p)  # two splinters
    return [a, b, c]


def eggs():
    shell = {'o': (150, 128, 96), 'd': (196, 176, 140), 'm': (224, 208, 172), 'l': (240, 230, 204), 'h': (252, 248, 236),
             's': (184, 160, 120)}
    a = draw(['................'] * 3 + [
        '.......oo.......',
        '......olho......',
        '.....olhhmo.....',
        '.....olhmmo.....',
        '....olmmmmdo....',
        '....olmmmddo....',
        '....odmmdddo....',
        '.....oddddo.....',
        '......oooo......'] + ['................'] * 4, shell)  # vanilla-like egg, standing
    b = draw(['................'] * 3 + [
        '.......oo.......',
        '......olho......',
        '.....olhmmo.....',
        '.....olsmmo.....',
        '....olmmmsdo....',
        '....osmmmddo....',
        '....odmsdddo....',
        '.....oddddo.....',
        '......oooo......'] + ['................'] * 4, shell)  # speckled
    c = draw(['................'] * 4 + [
        '......oooo......',
        '.....olhhmo.....',
        '....olhmmmdo....',
        '....olmmmmdo....',
        '....odmmmddo....',
        '.....oddddo.....',
        '......oooo......'] + ['................'] * 5, shell)  # rounder, lower
    return [a, b, c]


def sheet(rows, path):
    cell = 110
    s = Image.new('RGBA', (140 + 3 * cell, len(rows) * (cell + 50) + 20), (139, 139, 139, 255))
    d = ImageDraw.Draw(s)
    for r, (label, ims) in enumerate(rows):
        y = 10 + r * (cell + 50)
        d.text((6, y + 40), label, fill=(255, 255, 255))
        for i, im in enumerate(ims):
            x = 140 + i * cell
            d.text((x, y), 'ABC'[i], fill=(255, 255, 255))
            s.alpha_composite(im.resize((96, 96), Image.NEAREST), (x, y + 14))
            s.alpha_composite(im, (x, y + 116))
            s.alpha_composite(im.resize((32, 32), Image.NEAREST), (x + 24, y + 116))
    s.save(path)


def main():
    os.makedirs(OUT, exist_ok=True)
    sheet([('Resonanzstab', [rod_variant(t) for t in 'ABC']),
           ('Kupferkern', [core_variant(v) for v in 'ABC'])], os.path.join(OUT, 'stab-und-kern.png'))
    sheet([('Steinkiesel', pebbles()), ('Feuersteinsplitter', flint_chips()), ('Ei (Blocktextur)', eggs())],
          os.path.join(OUT, 'kleinteile.png'))
    print('ok ->', OUT)


if __name__ == '__main__':
    main()
