"""Usage: python tools/textures/proposals_v8_2026_10_02.py <preview dir>

Round 8 (owner 2026-10-02): ten Resonance Rod proposals (heads x shafts, vanilla palettes) and ten copper core
proposals - round-4 C kept, only the four diagonal 2x2 blocks between the points replaced by other shapes."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import resonance_rod_2026_10_02 as rod  # noqa: E402
import cores_2026_10_02 as cores  # noqa: E402

OUT = sys.argv[1] if len(sys.argv) > 1 else 'build/proposals-v8/'

# ================================================================== Resonance Rod
HEADS = {
    'drei': [  # current: long shard, one up, one right
        '.......o.....oo.',
        '......obo...obeo',
        '......ofgo.obfdo',
        '......odgoofdgo.',
        '.......oaodfgo..',
        '.........gdgooo.',
        '..........gbedo.',
        '...........ogo..',
    ],
    'gross': [  # one big shard like vanilla's amethyst_shard
        '............ooo.',
        '...........obbeo',
        '..........obfddo',
        '.........ofddgo.',
        '........ofddgo..',
        '........adggo...',
        '.........goo....',
        '................',
    ],
    'zwei': [  # long shard plus the upright one
        '.......o.....oo.',
        '......obo...obeo',
        '......ofgo.obfdo',
        '......odgoofdgo.',
        '.......oaodfgo..',
        '.........gdgo...',
        '..........go....',
        '................',
    ],
    'faecher': [  # three short crystals fanned out like an amethyst cluster
        '.........o...o..',
        '........obo.obo.',
        '....o...ofgoofgo',
        '...obo..odgodgo.',
        '...ofgo.odgdgo..',
        '....odgoodggo...',
        '.....oadgdgo....',
        '.......aggo.....',
    ],
    'fassung': [  # one shard held by two iron prongs
        '...........ooo..',
        '..........obeo..',
        '.......k.obfdo..',
        '.......wkofdgo..',
        '........wodgo.k.',
        '........lsoo.kw.',
        '.........slkwl..',
        '..........mm....',
    ],
}
SHAFTS = {
    'stock': ['.......nWl', '......nwlsk', '.....nlsmk', '....nwsmk', '...nrRqk', '..nlsmk', '.nwsmk', 'nlsmk', 'nsmk',
              '.kk'],
    'dick': ['......nWl', '.....nwlsm', '....nwlsmk', '...nwlsmk', '..nrRqmk', '.nwlsmk', 'nwlsmk', 'nlsmk', '.nmk',
             '..k'],
    'schlank': ['........nW', '.......nlk', '......nsk', '.....nlk', '....nrk', '...nsk', '..nlk', '.nsk', 'nlk', 'kk'],
}
ROD_COMBOS = [('drei', 'stock'), ('gross', 'stock'), ('zwei', 'stock'), ('faecher', 'stock'), ('fassung', 'stock'),
              ('drei', 'dick'), ('gross', 'dick'), ('zwei', 'schlank'), ('faecher', 'schlank'), ('fassung', 'dick')]


def rod_image(head, shaft):
    rows = ['................'] * 16
    rows = [list(r) for r in rows]
    for i, r in enumerate(SHAFTS[shaft]):
        for x, ch in enumerate(r):
            if ch != '.':
                rows[6 + i][x] = ch
    for y, r in enumerate(HEADS[head]):
        for x, ch in enumerate(r):
            if ch != '.':
                rows[y][x] = ch
    return rod.image([''.join(r) for r in rows], rod.PAL)


# ================================================================== Copper core (round-4 C, new diagonals)
COPPER = [(77, 36, 22), (138, 65, 41), (156, 69, 41), (193, 90, 54), (231, 124, 86), (252, 153, 130)]
# top-left quadrant patterns, cells x/y in 4..6, value = tone 0..5 (mirrored to the other three quadrants)
DIAGONALS = {
    'Linie': {(4, 4): 2, (5, 5): 4, (6, 6): 3},
    'Doppellinie': {(4, 4): 2, (5, 5): 4, (6, 5): 3, (5, 6): 3, (6, 6): 3},
    'Winkel': {(5, 5): 4, (6, 5): 3, (5, 6): 3},
    'Raute': {(5, 4): 3, (4, 5): 3, (5, 5): 4, (6, 6): 2},
    'Pfeil': {(4, 4): 5, (5, 5): 4, (6, 5): 2, (5, 6): 2},
    'Dreieck': {(4, 4): 3, (5, 4): 2, (4, 5): 2, (6, 6): 3},
    'nur Punkt': {(4, 4): 2},
    'Edelstein': {(4, 4): 5, (5, 5): 1, (6, 6): 3},
    'rund': {(5, 5): 4, (6, 5): 3, (5, 6): 3, (6, 6): 2},
    'Treppe': {(4, 5): 2, (5, 5): 4, (5, 4): 2, (6, 6): 3},
}
SHADE = {(-1, -1): 1, (1, -1): 0, (-1, 1): 0, (1, 1): -1}  # light from the top left


def copper_core(pattern):
    base = cores.slim(Image.open(os.path.join(cores.SRC, 'copper_core.png')).convert('RGBA').crop((0, 0, 16, 16)))
    im = base.copy()
    for sx in (-1, 1):
        for sy in (-1, 1):
            for x in range(4, 7):
                for y in range(4, 7):
                    im.putpixel((x if sx < 0 else 16 - x, y if sy < 0 else 16 - y), (0, 0, 0, 0))
            for (x, y), tone in DIAGONALS[pattern].items():
                t = max(0, min(5, tone + SHADE[(sx, sy)]))
                im.putpixel((x if sx < 0 else 16 - x, y if sy < 0 else 16 - y), COPPER[t] + (255,))
    return im


def sheet(images, labels, path):
    cell = 110
    s = Image.new('RGBA', (5 * cell + 10, 2 * (cell + 60) + 10), (139, 139, 139, 255))
    d = ImageDraw.Draw(s)
    for i, (im, lab) in enumerate(zip(images, labels)):
        x, y = 8 + (i % 5) * cell, 8 + (i // 5) * (cell + 60)
        d.text((x, y), lab, fill=(255, 255, 255))
        s.alpha_composite(im.resize((96, 96), Image.NEAREST), (x, y + 14))
        s.alpha_composite(im, (x, y + 116))
        s.alpha_composite(im.resize((32, 32), Image.NEAREST), (x + 24, y + 116))
    s.save(path)


def main():
    os.makedirs(OUT, exist_ok=True)
    rods = [rod_image(h, s) for h, s in ROD_COMBOS]
    sheet(rods, [f'{i + 1}: {h} / {s}' for i, (h, s) in enumerate(ROD_COMBOS)], os.path.join(OUT, 'resonanzstab-1-10.png'))
    names = list(DIAGONALS)
    sheet([copper_core(n) for n in names], [f'{i + 1}: {n}' for i, n in enumerate(names)], os.path.join(OUT, 'kupferkern-1-10.png'))
    print('ok ->', OUT)


if __name__ == '__main__':
    main()
