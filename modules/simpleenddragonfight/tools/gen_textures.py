"""Textures of Simple End Dragon Fight, own pixel art in the Vanilla style (no Vanilla sheet copied):
- entity/dragonmite/dragonmite.png (64 x 32): obsidian-violet chitin segments with purple veins, pale end-stone
  spines along the back, translucent-looking wing stubs; layout = the cubes of client/DragonmiteModel.java;
- entity/dragonmite/dragonmite_eyes.png: only the magenta eyes (emissive layer);
- item/dragon_chitin.png: an overlapping, curved chitin plate with a violet sheen and a pale rim;
- item/dragonmite_spawn_egg.png: a dark violet egg with cream spots like the mite's spines.
python3 modules/simpleenddragonfight/tools/gen_textures.py [--check] [--preview DIR]"""
import argparse
import sys
from pathlib import Path
from PIL import Image
sys.path.insert(0, str(Path(__file__).resolve().parents[3] / 'tools/textures'))
from layers import save, snap, snap_grid  # noqa: E402  layer sources beside the PNGs (A8 b)

MODULE = Path(__file__).resolve().parents[1]
TEX = MODULE / 'shared/resources/assets/simpleenddragonfight/textures'

# palette
SHELL = [(0x1C, 0x10, 0x26), (0x26, 0x16, 0x33), (0x30, 0x1D, 0x40), (0x3B, 0x24, 0x4F)]
VEIN = (0x7B, 0x3F, 0xA6)
VEIN_HI = (0xA0, 0x5C, 0xC8)
BELLY = (0x4A, 0x33, 0x58)
SPINE = [(0xC9, 0xC4, 0x8A), (0xDF, 0xDB, 0xA6), (0xF0, 0xEE, 0xC6)]
WING = [(0x5A, 0x3A, 0x72), (0x74, 0x50, 0x8E), (0x8E, 0x6C, 0xA8)]
EYE = (0xFF, 0x4A, 0xE0)
EYE_CORE = (0xFF, 0xB8, 0xF4)
MANDIBLE = (0x12, 0x0A, 0x18)


def h(x, y, salt=0):
    """Deterministic pixel noise 0..255."""
    n = (x * 374761393 + y * 668265263 + salt * 2246822519) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return (n ^ (n >> 16)) & 0xFF


def faces(u, v, w, hgt, d):
    """UV rectangles (x, y, width, height) of a model cube: top, bottom, west, north, east, south."""
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'west': (u, v + d, d, hgt),
            'north': (u + d, v + d, w, hgt), 'east': (u + d + w, v + d, d, hgt), 'south': (u + 2 * d + w, v + d, w, hgt)}


def fill(img, rect, colour_at):
    x0, y0, w, hgt = rect
    for y in range(y0, y0 + hgt):
        for x in range(x0, x0 + w):
            img.putpixel((x, y), colour_at(x - x0, y - y0, w, hgt, x, y) + (255,))


def shell(lx, ly, w, hgt, x, y):
    c = SHELL[1 + h(x, y) % 3] if ly > 0 else SHELL[3]
    if ly == hgt - 1:
        c = SHELL[0]
    if (lx + ly) % 4 == 0 and h(x, y, 3) % 3 == 0:
        c = VEIN
    return c


def top(lx, ly, w, hgt, x, y):
    c = SHELL[2 + h(x, y, 1) % 2]
    if lx == w // 2 or (w % 2 == 0 and lx == w // 2 - 1):
        c = VEIN_HI if ly % 2 == 0 else VEIN
    elif ly == 0 or ly == hgt - 1:
        c = SHELL[1]
    return c


def entity():
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    segs = [((0, 0), (4, 3, 2)), ((0, 5), (6, 4, 5)), ((0, 14), (3, 3, 1)), ((0, 18), (1, 2, 1))]
    for (u, v), (w, hgt, d) in segs:
        f = faces(u, v, w, hgt, d)
        for name in ('west', 'north', 'east', 'south'):
            fill(img, f[name], shell)
        fill(img, f['top'], top)
        fill(img, f['bottom'], lambda lx, ly, w_, h_, x, y: BELLY if h(x, y, 5) % 4 else SHELL[2])
    snap(img, 'Körper')
    # head (segment 0) front: mandibles at the bottom row
    hx, hy, hw, hh = faces(0, 0, 4, 3, 2)['north']
    for x in range(hx, hx + hw):
        img.putpixel((x, hy + hh - 1), MANDIBLE + (255,))
    img.putpixel((hx, hy + 1), EYE + (255,))
    img.putpixel((hx + hw - 1, hy + 1), EYE + (255,))
    snap(img, 'Gesicht')
    # spines: pale end-stone horns, darker at the root
    for u, hgt in ((24, 2), (28, 3), (32, 2)):
        fill(img, (u, 0, 4, 1 + hgt), lambda lx, ly, w_, h_, x, y: SPINE[2] if ly == 0 else SPINE[1] if ly < h_ - 1 else SPINE[0])
    snap(img, 'Stacheln')
    # wing stubs: membrane with a darker frame and one rib
    wf = faces(24, 5, 4, 1, 3)
    for name, rect in wf.items():
        fill(img, rect, lambda lx, ly, w_, h_, x, y: WING[0] if lx in (0, w_ - 1) or (name in ('top', 'bottom') and ly == h_ - 1)
             else (WING[2] if lx == w_ // 2 else WING[1]))
    return snap(img, 'Flügel')


def eyes(base):
    img = Image.new('RGBA', base.size, (0, 0, 0, 0))
    hx, hy, hw, _ = faces(0, 0, 4, 3, 2)['north']
    img.putpixel((hx, hy + 1), EYE_CORE + (255,))
    img.putpixel((hx + hw - 1, hy + 1), EYE_CORE + (255,))
    return img


def grid(rows, palette):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), palette[ch] + (255,))
    return img


CHITIN_PAL = {'k': (0x14, 0x0B, 0x1C), 'a': SHELL[1], 'b': SHELL[2], 'c': SHELL[3], 'v': VEIN, 'V': VEIN_HI,
              'r': SPINE[0], 'R': SPINE[1], 'w': SPINE[2]}
# A curved chitin scute seen from above: three overlapping bands, each with a pale rim and a violet sheen.
CHITIN = [
    '................',
    '................',
    '.....kkkkkk.....',
    '...kkRwwwwRkk...',
    '..kRrcVVcccrRk..',
    '..kabbcccbbbak..',
    '.kkkRRwwwwRRkkk.',
    '.kRrbcVVcccbrRk.',
    '.kaabbcccbbbaak.',
    '.kkkRRwwwwRRkkk.',
    '.kRrbbVVccbbrRk.',
    '..kaabbccbbaak..',
    '..kaaabbbbaaak..',
    '...kkaaaaaakk...',
    '.....kkkkkk.....',
    '................',
]
EGG_PAL = {'k': (0x14, 0x0B, 0x1C), 'a': SHELL[1], 'b': SHELL[2], 'c': SHELL[3], 'v': VEIN, 'V': VEIN_HI,
           's': SPINE[1], 'S': SPINE[2], 'e': EYE}
EGG = [
    '................',
    '......kkkk......',
    '.....kccbbk.....',
    '....kcVbbsak....',
    '....kcbbbbak....',
    '...kcbsSbbbak...',
    '...kcbbsbVbak...',
    '..kcbbbbbbsbak..',
    '..kcVbbbbbbbak..',
    '..kbbbsSbbbbak..',
    '..kbbbbsbbsSak..',
    '..kabbbbbbbsak..',
    '...kabVbbbbak...',
    '...kaabbbbaak...',
    '....kkaaaakk....',
    '......kkkk......',
]


# layer sources of the item grids (A8 b): base = shell, then these on top
GRID_LAYERS = [('Adern', 'vV'), ('Stacheln', 'rRwsS'), ('Auge', 'e'), ('Kontur', 'k')]


def outputs():
    ent = entity()
    return {
        TEX / 'entity/dragonmite/dragonmite.png': ent,
        TEX / 'entity/dragonmite/dragonmite_eyes.png': eyes(ent),
        TEX / 'item/dragon_chitin.png': snap_grid(grid(CHITIN, CHITIN_PAL), CHITIN, CHITIN_PAL, GRID_LAYERS, 'Panzer'),
        # item/dragonmite_spawn_egg.png: W24-1: spawn egg drawn by tools/textures/spawn_eggs_2026_10_23.py (style B, owner 23.10.)
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--preview')
    args = ap.parse_args()
    bad = []
    for path, img in outputs().items():
        if args.check:
            if not path.is_file() or list(Image.open(path).convert('RGBA').getdata()) != list(img.getdata()):
                bad.append(path)
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        save(img, path)
    if args.preview:
        out = Path(args.preview)
        out.mkdir(parents=True, exist_ok=True)
        for path, img in outputs().items():
            img.resize((img.width * 16, img.height * 16), Image.NEAREST).save(out / f'tex-{path.stem}.png')
    if bad:
        print('outdated textures:', *bad, sep='\n  ')
        sys.exit(1)
    print('simpleenddragonfight textures', 'up to date' if args.check else 'written')


if __name__ == '__main__':
    main()
