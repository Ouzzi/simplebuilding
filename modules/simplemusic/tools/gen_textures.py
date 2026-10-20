"""Own pixel art of Simple Music (no Mojang pixels): sheet music, blank/custom disc, Orchestrion side/top.

python3 modules/simplemusic/tools/gen_textures.py              write the chosen variants into the module
python3 modules/simplemusic/tools/gen_textures.py --variants D also write every variant and a 16x preview sheet to D
Needs Pillow. Variants: sheet A/B/C, disc A/B/C, orchestrion A/B (side + top). Chosen (provisional): see CHOSEN.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

MODULE = Path(__file__).resolve().parents[1]
TEX = MODULE / 'shared/resources/assets/simplemusic/textures'
CHOSEN = {'sheet': 'A', 'disc': 'A', 'orchestrion': 'A'}

PAPER = {'.': None, 'o': (74, 62, 44), 'p': (233, 226, 204), 's': (206, 195, 166), 'd': (176, 163, 131),
         'l': (120, 112, 96), 'n': (36, 30, 26), 'r': (154, 64, 52), 'w': (248, 244, 230)}

SHEET_A = [
    '................',
    '...oooooooooo...',
    '..owppppppppso..',
    '..oppppppppppo..',
    '..ollllllllllo..',
    '..oppppppnnnpo..',
    '..ollllllnlnlo..',
    '..opppnppnpnpo..',
    '..olllnllnlnlo..',
    '..oppnnpnnpnpo..',
    '..ollnnlnnllso..',
    '..oppppppppsso..',
    '..ospppppppsdo..',
    '..osssssssddo...',
    '...ooooooooo....',
    '................',
]
SHEET_B = [
    '................',
    '..oooooooooooo..',
    '.osswwwwwwwwsso.',
    '.oddooooooooddo.',
    '...oppppppppo...',
    '...ollllllllo...',
    '...opppppnppo...',
    '...olllllnllo...',
    '...opppnnnppo...',
    '...ollnnnlllo...',
    '...oppnnppppo...',
    '...ollllllllo...',
    '..oooooooooooo..',
    '.osswwwwwwwwsso.',
    '.oddooooooooddo.',
    '..oooooooooooo..',
]
SHEET_C = [
    '................',
    '..ooooooooooo...',
    '..owpppppppppo..',
    '..opppppnnpppo..',
    '..olllllnlnllo..',
    '..opppppnppnpo..',
    '..olllllnllllo..',
    '..opppppnppppo..',
    '..olllnnnllllo..',
    '..oppnnnnppppo..',
    '..ollnnnllllso..',
    '..opppppppppso..',
    '..ospppppprpdo..',
    '..osssssssddo...',
    '...ooooooooo....',
    '................',
]


def grid(rows, palette):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            c = palette[ch]
            if c:
                img.putpixel((x, y), c + (255,))
    return img


def disc(body, groove, rim, label, label_dark, shine, hole=(24, 20, 18), note=None):
    """A record: 14 px across, rim outline, alternating grooves, a label in the middle, a shine in the upper left."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if r > 7.0:
                continue
            if r > 6.2:
                c = rim
            elif r <= 1.0:
                c = hole
            elif r <= 2.6:
                c = label_dark if (x + y) % 5 == 0 or r > 2.2 else label
            else:
                c = groove if int(r * 1.6) % 2 == 0 else body
            img.putpixel((x, y), c + (255,))
    for x, y in ((4, 3), (3, 4), (5, 3), (3, 5), (4, 4)):
        img.putpixel((x, y), shine + (255,))
    if note:
        for x, y in note:
            img.putpixel((x, y), (250, 246, 236, 255))
    return img


DISC = {
    # A: charcoal black with an amethyst label (the recipe's amethyst shard)
    'A': dict(body=(46, 42, 48), groove=(34, 31, 36), rim=(20, 18, 22), label=(154, 108, 196), label_dark=(110, 72, 150), shine=(112, 108, 120)),
    # B: blackstone grey, copper label
    'B': dict(body=(58, 54, 58), groove=(44, 41, 45), rim=(25, 23, 26), label=(196, 112, 74), label_dark=(150, 80, 50), shine=(122, 118, 124)),
    # C: deep black, pale silver label like a test pressing
    'C': dict(body=(32, 30, 34), groove=(24, 22, 26), rim=(12, 11, 13), label=(198, 200, 206), label_dark=(150, 152, 160), shine=(96, 94, 104)),
}
# custom disc = the blank disc of the variant with a written label (a little note on it)
NOTE_MARK = ((8, 6), (8, 7), (8, 8), (7, 8))

WOOD = {'o': (58, 40, 26), 'w': (122, 86, 54), 'v': (104, 72, 44), 'h': (146, 106, 68), 'k': (40, 28, 20),
        'c': (198, 116, 76), 'C': (232, 160, 112), 'x': (140, 74, 46), 'g': (28, 22, 18), '.': None}

SIDE_A = [  # wooden case with four copper organ pipes
    'oooooooooooooooo',
    'ohhhhhhhhhhhhhvo',
    'ohkkkkkkkkkkkkvo',
    'ohk...C......kvo',
    'ohk..Cc..C...kvo',
    'ohk..Cc.Cc.C.kvo',
    'ohkC.Cc.Cc.Cckvo',
    'ohkCcCc.Cc.Cckvo',
    'ohkCcCcxCcxCckvo',
    'ohkCcCcxCcxCckvo',
    'ohkxxxxxxxxxxkvo',
    'ohkkkkkkkkkkkkvo',
    'ohwwvwwwwvwwwwvo',
    'ohgwgwgwgwgwgwvo',
    'ovvvvvvvvvvvvvvo',
    'oooooooooooooooo',
]
SIDE_B = [  # note block like planks, a copper bell row and a dark grille
    'oooooooooooooooo',
    'ohhhhhhhhhhhhhvo',
    'ohwwwwvwwwwwwwvo',
    'ohvvvvvvvvvvvvvo',
    'ohwwvwwwwwvwwwvo',
    'ohkkkkkkkkkkkkvo',
    'ohkCc.Cc.Cc.Ccko',
    'ohkcx.cx.cx.cxko',
    'ohkkkkkkkkkkkkvo',
    'ohwwwwwvwwwwwwvo',
    'ohvvvvvvvvvvvvvo',
    'ohgkgkgkgkgkgkvo',
    'ohkgkgkgkgkgkgvo',
    'ohwwwwwwvwwwwwvo',
    'ovvvvvvvvvvvvvvo',
    'oooooooooooooooo',
]
TOP_A = [  # dark wood top with a disc slot and copper corners
    'oooooooooooooooo',
    'oCcvvvvvvvvvvcCo',
    'ocwwwwwwwwwwwwco',
    'ovwhhhhhhhhhhwvo',
    'ovwhwwwwwwwwhwvo',
    'ovwhwkkkkkkwhwvo',
    'ovwhwkggggkwhwvo',
    'ovwhwkgxxgkwhwvo',
    'ovwhwkgxxgkwhwvo',
    'ovwhwkggggkwhwvo',
    'ovwhwkkkkkkwhwvo',
    'ovwhwwwwwwwwhwvo',
    'ovwhhhhhhhhhhwvo',
    'ocwwwwwwwwwwwwco',
    'oCcvvvvvvvvvvcCo',
    'oooooooooooooooo',
]
TOP_B = [  # planks with a long record slot
    'oooooooooooooooo',
    'ohhhhhhhhhhhhhvo',
    'ohwwwwvwwwwwwwvo',
    'ohvvvvvvvvvvvvvo',
    'ohwwwwwwwvwwwwvo',
    'ohwkkkkkkkkkkwvo',
    'ohwkggggggggkwvo',
    'ohwkkkkkkkkkkwvo',
    'ohwwvwwwwwwwwwvo',
    'ohvvvvvvvvvvvvvo',
    'ohwwwwwvwwwwwwvo',
    'ohcwwwwwwwwwwcvo',
    'ohCcwwwwvwwwcCvo',
    'ohvvvvvvvvvvvvvo',
    'ovvvvvvvvvvvvvvo',
    'oooooooooooooooo',
]


def variants():
    out = {}
    for k, rows in (('A', SHEET_A), ('B', SHEET_B), ('C', SHEET_C)):
        out[f'sheet_{k}'] = grid(rows, PAPER)
    for k, colors in DISC.items():
        out[f'blank_disc_{k}'] = disc(**colors)
        out[f'custom_disc_{k}'] = disc(**colors, note=NOTE_MARK)
    out['orchestrion_side_A'] = grid(SIDE_A, WOOD)
    out['orchestrion_top_A'] = grid(TOP_A, WOOD)
    out['orchestrion_side_B'] = grid(SIDE_B, WOOD)
    out['orchestrion_top_B'] = grid(TOP_B, WOOD)
    return out


def preview(images, path):
    names = list(images)
    scale = 8
    cols = 6
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * (16 * scale + 16), rows * (16 * scale + 30)), (198, 198, 198, 255))
    draw = ImageDraw.Draw(sheet)
    for i, name in enumerate(names):
        x = (i % cols) * (16 * scale + 16) + 8
        y = (i // cols) * (16 * scale + 30) + 8
        sheet.alpha_composite(images[name].resize((16 * scale, 16 * scale), Image.NEAREST), (x, y))
        draw.text((x, y + 16 * scale + 4), name, fill=(20, 20, 20, 255))
    sheet.save(path)


def main(argv):
    images = variants()
    for row in SIDE_A + SIDE_B + TOP_A + TOP_B + SHEET_A + SHEET_B + SHEET_C:
        assert len(row) == 16, row
    (TEX / 'item').mkdir(parents=True, exist_ok=True)
    (TEX / 'block').mkdir(parents=True, exist_ok=True)
    images[f"sheet_{CHOSEN['sheet']}"].save(TEX / 'item/sheet_music.png')
    images[f"blank_disc_{CHOSEN['disc']}"].save(TEX / 'item/blank_disc.png')
    images[f"custom_disc_{CHOSEN['disc']}"].save(TEX / 'item/custom_disc.png')
    images[f"orchestrion_side_{CHOSEN['orchestrion']}"].save(TEX / 'block/orchestrion_side.png')
    images[f"orchestrion_top_{CHOSEN['orchestrion']}"].save(TEX / 'block/orchestrion_top.png')
    if '--variants' in argv:
        out = Path(argv[argv.index('--variants') + 1])
        out.mkdir(parents=True, exist_ok=True)
        for name, img in images.items():
            img.save(out / f'{name}.png')
            img.resize((256, 256), Image.NEAREST).save(out / f'{name}_16x.png')
        preview(images, out / 'preview_8x.png')
    print(f'simplemusic textures written (chosen {CHOSEN})')


if __name__ == '__main__':
    main(sys.argv[1:])
