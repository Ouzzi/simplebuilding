"""Usage: python tools/textures/brew_textures.py <vanilla textures dir> [preview dir]

Own pixel art of branch claude-q-brew (docs/ai/PLAN-BRAUEN-WERKBANK-2026-10-09.md), drawn here pixel by pixel:
- mob effect icons (18x18, one pixel of air around them like the vanilla icons): shivering (a crosshair between two
  tremor zigzags, ice blue), mirage (an eye under heat waves, amber), reverse_mirage (the eye turned over, waves
  below, violet), faded (a disc half in colour, half in grey);
- item warden_tendril (16x16): one of the warden's curled head tendrils, deep teal with the glowing sculk spots;
- block storage_crafting_table: top = oak tray with a 3x3 grid of recessed cells (cell centres at 3, 8 and 13 px,
  where StorageCraftingTableRenderer lays the items) in a dark frame with iron corner brackets; front = the corner
  posts and rim of a work table over a chest-like drawer with an iron latch; side = posts, rim and an iron strap.
  The planks are vanilla oak planks (read from the vanilla directory at generation time, never checked in) - the
  shapes on them are new. The bottom uses minecraft:block/oak_planks directly.
Writes into mc26_3/overlay/resources/assets/simplebuilding/textures and, with a preview dir, two preview sheets
(16x, old/vanilla reference next to new): brew-effect-icons.png and storage-crafting-table-textures.png.
"""
from PIL import Image, ImageDraw
import os
import sys

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
TEX = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures')


def rgba(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, 255)


def from_map(rows, palette):
    h, w = len(rows), len(rows[0])
    out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == w, (y, row)
        for x, c in enumerate(row):
            if c != '.':
                out.putpixel((x, y), palette[c])
    return out


def assert_air_border(im):
    w, h = im.size
    for x in range(w):
        assert im.getpixel((x, 0))[3] == 0 and im.getpixel((x, h - 1))[3] == 0, 'paints on the border'
    for y in range(h):
        assert im.getpixel((0, y))[3] == 0 and im.getpixel((w - 1, y))[3] == 0, 'paints on the border'


# ---------------------------------------------------------------- effect icons (18x18)

SHIVERING = [
    '..................',
    '..................',
    '.......oooo.......',
    '.......owlo.......',
    '.......owlo.......',
    '..o....owmo....o..',
    '.olo...oooo...olo.',
    '..o............o..',
    '.ooooo......ooooo.',
    '.owwlm......mlwwo.',
    '.ooooo......ooooo.',
    '..o............o..',
    '.olo...oooo...olo.',
    '..o....owmo....o..',
    '.......owlo.......',
    '.......owlo.......',
    '.......oooo.......',
    '..................',
]
SHIVERING_PAL = {'o': rgba(0x2C4E63), 'w': rgba(0xF4FBFF), 'l': rgba(0xBFE6F5), 'm': rgba(0x7DB8D6)}

MIRAGE = [
    '..................',
    '..................',
    '...ll....ll.......',
    '..l..m..l..m..ll..',
    '......ll....ll..m.',
    '...ll....ll.......',
    '..l..m..l..m......',
    '..................',
    '.....oooooooo.....',
    '...oowwwhhwwwoo...',
    '..owwwhaaaahwwwo..',
    '.owwwhapkkpahwwwo.',
    '..owwwhaaaahwwwo..',
    '...oowwwhhwwwoo...',
    '.....oooooooo.....',
    '..................',
    '..................',
    '..................',
]
MIRAGE_PAL = {'o': rgba(0x5A3A12), 'w': rgba(0xFFF4DC), 'h': rgba(0xE8D3A8), 'a': rgba(0xE2B45A),
              'p': rgba(0xB27A22), 'k': rgba(0x2A1A08), 'l': rgba(0xF2C46A), 'm': rgba(0xC08A30)}


def reverse_mirage():
    # The eye turned over (its lid line runs the other way), the waves under it, in violet.
    rows = list(reversed(MIRAGE))
    rows = rows[1:] + rows[:1]  # one row up so the eye keeps the same height
    return rows


REVERSE_PAL = {'o': rgba(0x2E1F5C), 'w': rgba(0xEFE9FF), 'h': rgba(0xCFC4F2), 'a': rgba(0x7E64C8),
               'p': rgba(0x553FA0), 'k': rgba(0x120A26), 'l': rgba(0xA68EEA), 'm': rgba(0x6A52B8)}

FADED = [
    '..................',
    '..................',
    '......oooooo......',
    '....oorrrr4ioo....',
    '...orrrrrr44iio...',
    '...oyyyyyy33hho...',
    '..oyyyyyyy333hho..',
    '..oyyyyyyy333hho..',
    '..oggggggg222ggo..',
    '..oggggggg222ggo..',
    '..obbbbbbb111ffo..',
    '..obbbbbbb111ffo..',
    '...obbbbbb11ffo...',
    '...oppppppnnffo...',
    '....oopppp5noo....',
    '......oooooo......',
    '..................',
    '..................',
]
FADED_PAL = {'o': rgba(0x2B2B2B),
             'r': rgba(0xD9483B), 'y': rgba(0xF0C83C), 'g': rgba(0x58B848), 'b': rgba(0x3F7FD9), 'p': rgba(0x9B4FC4),
             # grey of the same brightness (Rec. 601) on the right half, slightly lighter at the edge
             '4': rgba(0x7A7A7A), 'i': rgba(0x8C8C8C), '3': rgba(0xBDBDBD), 'h': rgba(0xCFCFCF),
             '2': rgba(0x8E8E8E), '1': rgba(0x707070), 'f': rgba(0x828282), 'n': rgba(0x6A6A6A), '5': rgba(0x5E5E5E)}

# ---------------------------------------------------------------- warden tendril (16x16)

TENDRIL = [
    '................',
    '...........oo...',
    '..........ogwo..',
    '.........odgmo..',
    '.........odmo...',
    '........odgmo...',
    '.......oddmo....',
    '......odgmmo....',
    '.....oddmmo.....',
    '....odgddmo.....',
    '...oddmmmo......',
    '..odgmdgmo......',
    '..oddmmmdo......',
    '..oddddddo......',
    '...oooooo.......',
    '................',
]
TENDRIL_PAL = {'o': rgba(0x061A1E), 'd': rgba(0x0E3D45), 'm': rgba(0x15606B), 'g': rgba(0x29DFEB), 'w': rgba(0xB8FBFF)}

# ---------------------------------------------------------------- storage crafting table (16x16)

FRAME = rgba(0x4B2E18)
FRAME_LIGHT = rgba(0x6E4426)
POST = rgba(0x19140C)
IRON_LIGHT = rgba(0xD6D6D6)
IRON = rgba(0xA6A6A6)
IRON_DARK = rgba(0x5E5E5E)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def planks():
    return Image.open(os.path.join(V, 'block', 'oak_planks.png')).convert('RGBA')


def table_top():
    out = planks()
    cells = [(1, 4), (6, 9), (11, 14)]
    for y in range(16):
        for x in range(16):
            p = out.getpixel((x, y))
            in_cell = any(a <= x <= b for a, b in cells) and any(a <= y <= b for a, b in cells)
            if x in (0, 15) or y in (0, 15):
                out.putpixel((x, y), FRAME)
            elif in_cell:
                out.putpixel((x, y), shade(p, 0.78))  # recessed cell
            else:
                out.putpixel((x, y), shade(p, 1.08))  # raised divider
    # cell shadow on the upper and left inner edge (light from the top left)
    for a, b in cells:
        for c, d in cells:
            for k in range(a, b + 1):
                out.putpixel((k, c), shade(out.getpixel((k, c)), 0.85))
            for k in range(c, d + 1):
                out.putpixel((a, k), shade(out.getpixel((a, k)), 0.85))
    # iron corner brackets: an L of two pixels each way on the frame, a rivet inside
    for cx, cy, dx, dy in ((0, 0, 1, 1), (15, 0, -1, 1), (0, 15, 1, -1), (15, 15, -1, -1)):
        out.putpixel((cx, cy), IRON_DARK)
        out.putpixel((cx + dx, cy), IRON)
        out.putpixel((cx, cy + dy), IRON)
        out.putpixel((cx + 2 * dx, cy), IRON_LIGHT if dy > 0 else IRON)
        out.putpixel((cx, cy + 2 * dy), IRON_LIGHT if dx > 0 else IRON)
    return out


def rim_and_posts(out):
    for x in range(16):
        out.putpixel((x, 0), FRAME)
        out.putpixel((x, 1), FRAME_LIGHT)
        out.putpixel((x, 2), FRAME)
    for y in range(16):
        out.putpixel((0, y), POST)
        out.putpixel((15, y), POST)
    return out


def table_front():
    out = rim_and_posts(planks())
    # drawer: dark outline rows 6..13, cols 3..12, the board inside a little darker, bottom edge lit
    for y in range(6, 14):
        for x in range(3, 13):
            edge = y in (6, 13) or x in (3, 12)
            out.putpixel((x, y), FRAME if edge else shade(out.getpixel((x, y)), 0.9))
    for x in range(4, 12):
        out.putpixel((x, 7), shade(out.getpixel((x, 7)), 0.8))
    # iron latch like a chest's: 2x3, light top left
    out.putpixel((7, 8), IRON_LIGHT)
    out.putpixel((8, 8), IRON)
    out.putpixel((7, 9), IRON)
    out.putpixel((8, 9), IRON_DARK)
    out.putpixel((7, 10), IRON_DARK)
    out.putpixel((8, 10), IRON_DARK)
    return out


def table_side():
    out = rim_and_posts(planks())
    # iron strap across the side with two rivets
    for x in range(1, 15):
        out.putpixel((x, 8), IRON)
        out.putpixel((x, 9), IRON_DARK)
    for x in (3, 12):
        out.putpixel((x, 8), IRON_LIGHT)
    # a small key hole plate under the strap, like a lock on a chest side
    out.putpixel((7, 11), FRAME)
    out.putpixel((8, 11), FRAME)
    out.putpixel((7, 12), POST)
    out.putpixel((8, 12), FRAME)
    return out


def save(im, *path):
    p = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    im.save(p)
    print('wrote', os.path.normpath(p))


def sheet(tiles, path, scale=16):
    cell = 18 * scale + 24
    out = Image.new('RGBA', (cell * len(tiles) + 24, cell + 48), (198, 198, 198, 255))
    draw = ImageDraw.Draw(out)
    for i, (label, im) in enumerate(tiles):
        big = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
        x = 24 + i * cell + (18 * scale - big.width) // 2
        out.alpha_composite(big, (x, 40 + (18 * scale - big.height) // 2))
        draw.text((24 + i * cell, 12), label, fill=(0, 0, 0, 255))
    out.save(path)
    print('preview', path)


def on_dark(im):
    dark = Image.new('RGBA', im.size, (40, 40, 40, 255))
    dark.alpha_composite(im)
    return dark


def main():
    icons = {
        'shivering': from_map(SHIVERING, SHIVERING_PAL),
        'mirage': from_map(MIRAGE, MIRAGE_PAL),
        'reverse_mirage': from_map(reverse_mirage(), REVERSE_PAL),
        'faded': from_map(FADED, FADED_PAL),
    }
    for name, im in icons.items():
        assert im.size == (18, 18), name
        assert_air_border(im)
        save(im, 'mob_effect', name + '.png')
    tendril = from_map(TENDRIL, TENDRIL_PAL)
    assert_air_border(tendril)
    save(tendril, 'item', 'warden_tendril.png')
    faces = {'top': table_top(), 'front': table_front(), 'side': table_side()}
    for name, im in faces.items():
        save(im, 'block', 'storage_crafting_table_' + name + '.png')
    if PREVIEW:
        os.makedirs(PREVIEW, exist_ok=True)
        ref = lambda n: Image.open(os.path.join(V, n + '.png')).convert('RGBA')
        sheet([('A darkness (vanilla)', ref('mob_effect/darkness')), ('B shivering', icons['shivering']),
               ('C mirage', icons['mirage']), ('D reverse mirage', icons['reverse_mirage']), ('E faded', icons['faded']),
               ('F on HUD', on_dark(icons['shivering'])), ('G on HUD', on_dark(icons['faded'])),
               ('H echo shard (vanilla)', ref('item/echo_shard')), ('I warden tendril', tendril)],
              os.path.join(PREVIEW, 'brew-effect-icons.png'))
        sheet([('A vanilla top', ref('block/crafting_table_top')), ('B vanilla front', ref('block/crafting_table_front')),
               ('C vanilla side', ref('block/crafting_table_side')), ('D new top', faces['top']),
               ('E new front', faces['front']), ('F new side', faces['side'])],
              os.path.join(PREVIEW, 'storage-crafting-table-textures.png'))


if __name__ == '__main__':
    main()
