"""Resonance Rod (id amethyst_lens), redrawn from scratch 2026-10-02 (owner: mix of the old texture and round-5 B).

Vanilla item language: diagonal like a tool, iron shaft three pixels thick with a dark outline on the shadow side
(iron_sword palette), a redstone inlay, a bright iron collar, and a cluster of three amethyst shards in the vanilla
amethyst_shard palette - a long one on the axis, a short one up and a short one to the right.
item/amethyst_lens = charged, item/amethyst_lens_empty = no charge (shards and redstone dimmed).

Usage: python tools/textures/resonance_rod_2026_10_02.py [out dir]  (default: the 26.3 overlay)
Afterwards run gadget_animations_2026_10_03.py: it turns item/amethyst_lens into the idle animation strip."""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets',
                                                         'simplebuilding', 'textures', 'item')
PAL = {
    # amethyst (vanilla item/amethyst_shard)
    'o': (84, 57, 138), 'a': (111, 79, 171), 'g': (141, 106, 204), 'd': (179, 142, 243), 'f': (207, 160, 243),
    'b': (254, 203, 230), 'e': (255, 253, 213),
    # iron (vanilla item/iron_sword)
    'k': (24, 24, 24), 'n': (68, 68, 68), 'm': (107, 107, 107), 's': (150, 150, 150), 'l': (190, 190, 190),
    'w': (216, 216, 216), 'W': (255, 255, 255),
    # redstone (vanilla item/redstone)
    'r': (255, 0, 0), 'R': (170, 15, 1), 'q': (92, 7, 0),
}
# Owner 2026-10-02 (final): round-9 C - sharper tip, the upright shard one pixel lower and further right - with the
# whole amethyst part one pixel left so it sits on the shaft; shaft like vanilla's stick, rounded bottom end.
ROD = [
    '.............o..',
    '.......o....oeo.',
    '......obo..obfo.',
    '......ofgoobfdo.',
    '......odgofdgo..',
    '.......oodfgo...',
    '.......ngdgooo..',
    '......nwlgbedo..',
    '.....nlsmkogo...',
    '....nwsmk.......',
    '...nrRqk........',
    '..nlsmk.........',
    '.nwsmk..........',
    'nlsmk...........',
    'nsmk............',
    '.kk.............',
]


def image(rows, pal):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                im.putpixel((x, y), pal[ch] + (255,))
    return im


def dimmed():
    """No charge: the shards lose their glow (dark, greyish violet), the redstone goes dull; the iron stays."""
    pal = dict(PAL)
    for k in 'oagdfbe':
        r, g, b = PAL[k]
        grey = (r + g + b) / 3
        pal[k] = tuple(round((c * 0.45 + grey * 0.2)) for c in (r, g, b))
    for k in 'rRq':
        pal[k] = tuple(round(c * 0.45) for c in PAL[k])
    return pal


def textures():
    return {'amethyst_lens': image(ROD, PAL), 'amethyst_lens_empty': image(ROD, dimmed())}


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for name, im in textures().items():
        im.save(os.path.join(OUT, name + '.png'))
    print('ok ->', OUT)
