"""Block textures of the Questionary Table (own pixel art in the Vanilla style, no Vanilla sheet copied): an oak table
top with a lapis inlay along the front edge, its edge and underside, dark legs, an open book (lapis-blue cover, parchment
spread with ink lines) and a lapis inkwell.
python3 modules/simplequestionary/tools/gen_textures.py [--check] [--preview DIR]"""
import argparse
import sys
from pathlib import Path
from PIL import Image

MODULE = Path(__file__).resolve().parents[1]
OUT = MODULE / 'shared/resources/assets/simplequestionary/textures/block'

C = {
    # oak-like table wood
    'W': (0xCF, 0xAE, 0x74), 'w': (0xB8, 0x94, 0x5F), 'm': (0x9C, 0x7A, 0x4A), 'd': (0x7E, 0x60, 0x38), 'k': (0x5F, 0x46, 0x27),
    # dark legs and stand
    'L': (0x7D, 0x5D, 0x37), 'l': (0x6B, 0x4E, 0x2E), 'n': (0x57, 0x3E, 0x22), 'N': (0x45, 0x30, 0x19),
    # lapis and gold
    'a': (0x4F, 0x7F, 0xE0), 'b': (0x2E, 0x5C, 0xC8), 'c': (0x1E, 0x3F, 0x9A), 'e': (0x14, 0x2A, 0x6B), 'g': (0xE8, 0xB9, 0x23),
    # book cover and pages
    'B': (0x34, 0x52, 0x95), 'v': (0x26, 0x3E, 0x78), 'V': (0x1A, 0x2C, 0x58),
    'p': (0xF4, 0xE9, 0xCF), 'q': (0xE3, 0xD3, 0xAE), 'r': (0xC9, 0xB5, 0x8A), 't': (0x8A, 0x76, 0x58),
}


def grid(rows):
    img = Image.new('RGBA', (16, 16))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            img.putpixel((x, y), C[ch] + (255,))
    return img


# Three boards running front to back (5, 6, 5 px) with dark seams, knots and grain; the lapis inlay sits in rows 1-2.
TOP = [
    'dddddddddddddddd',
    'dkcbbabbbbabbckd',
    'dkecccccccccceed',
    'wwmwwdwwwwwwdwwW',
    'wwmwWdwwmwwwdwwW',
    'wwwwWdwwmwwWdmww',
    'Wwwwwdwwwwwwdmww',
    'Wwwmwdwwwwmwdmww',
    'wwwmwdwWwwmwdwww',
    'wwwmwdwWwwwwdwwm',
    'wWwwwdwwwwwwdwwm',
    'wWwwwdmwwWwwdwwm',
    'wwwwmdmwwWwwdWww',
    'wwwwmdwwwwwmdWww',
    'mwwwwdwwwwwmdwww',
    'dddddddddddddddd',
]

# Rows 0-2: the 3 px edge of the top (light upper line, darker body); rows 3-15: the underside with two cross braces.
SIDE = [
    'WWWWWWWWWWWWWWWW',
    'wwmwwwwwwmwwwwmw',
    'ddkddddkdddddkdd',
    'mmmmdmmmmmmdmmmm',
    'mdmmmmmmmdmmmmmm',
    'kkkkkkkkkkkkkkkk',
    'mmmmmdmmmmmmmdmm',
    'mmdmmmmmmdmmmmmm',
    'mmmmmmmdmmmmmmmd',
    'dmmmmmmmmmmdmmmm',
    'kkkkkkkkkkkkkkkk',
    'mmmdmmmmmmmmdmmm',
    'mmmmmmdmmmmmmmmm',
    'mdmmmmmmmmdmmmmm',
    'mmmmmdmmmmmmmdmm',
    'dddddddddddddddd',
]

# A turned post: light left column, dark right column, rings at rows 2 and 12.
LEG = [
    'nnnnnnnnnnnnnnnn',
    'lllllllLlnllllll',
    'NNNNNNNNNNNNNNNN',
    'llllllLLnnllllll',
    'lLlllllLlnlllLll',
    'llllllLlnnllllll',
    'lllLllLLlnllllll',
    'llllllLlnnlllLll',
    'lllllLLLlnllllll',
    'lLllllLlnNllllll',
    'llllllLLlnllLlll',
    'llllllLlnnllllll',
    'NNNNNNNNNNNNNNNN',
    'llllllLlnnllllll',
    'lllLllLLlnllllll',
    'nnnnnnnnnnnnnnnn',
]

# Cover everywhere except the spread (cols 2-13, rows 4-11): two pages with ink lines and a shaded spine,
# row 12 the page edges seen from the side. Gold corner studs on the cover.
BOOK = [
    'VgvvvvvvvvvvvvgV',
    'vBBBBBBBBBBBBBBv',
    'vBvvvvvvvvvvvvBv',
    'vBBBBBBBBBBBBBBv',
    'vvpppppqrpppppvv',
    'vvpttpttrqtttpvv',
    'vvppppqqrqppppvv',
    'vvpttttpqrptttvv',
    'vvpppppqrqppppvv',
    'vvptttpqrqttppvv',
    'vvppppqqrqppppvv',
    'vvqqqqqqrrqqqqvv',
    'vVpqpqpqpqpqpqVv',
    'vBBBBBBBBBBBBBBv',
    'VgvvvvvvvvvvvvgV',
    'VVVVVVVVVVVVVVVV',
]

# Inkwell: lapis glass sides with a highlight, dark ink in the neck (top uv 4-6).
LAPIS = [
    'aabcbbbbbbbbbbbb',
    'abccbbbbbbbbbbbb',
    'bcccbbbbbbbbbbbb',
    'cccebbbbbbbbbbbb',
    'bbbbaaccbbbbbbbb',
    'bbbbaeecbbbbbbbb',
    'bbbbceecbbbbbbbb',
    'bbbbccccbbbbbbbb',
    'bbbbbbbbbbbbbbbb',
    'bbbbbbbbbbbbbbbb',
    'bbbbbbbbbbbbbbbb',
    'bbbbbbbbbbbbbbbb',
    'bbbbbbbbbbbbbbbb',
    'bbbbbbbbbbbbbbbb',
    'bbbbbbbbbbbbbbbb',
    'bbbbbbbbbbbbbbbb',
]

TEXTURES = {
    'questionary_table_top': TOP,
    'questionary_table_side': SIDE,
    'questionary_table_leg': LEG,
    'questionary_table_book': BOOK,
    'questionary_table_lapis': LAPIS,
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--preview')
    args = parser.parse_args()
    bad = []
    for name, rows in TEXTURES.items():
        img = grid(rows)
        path = OUT / f'{name}.png'
        if args.check:
            if not path.exists() or list(Image.open(path).convert('RGBA').getdata()) != list(img.getdata()):
                bad.append(name)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            img.save(path)
    if args.preview:
        sheet = Image.new('RGBA', (272 * len(TEXTURES) - 16, 256), (60, 60, 60, 255))
        for i, rows in enumerate(TEXTURES.values()):
            sheet.paste(grid(rows).resize((256, 256), Image.NEAREST), (i * 272, 0))
        Path(args.preview).mkdir(parents=True, exist_ok=True)
        sheet.save(Path(args.preview) / 'textures.png')
    if bad:
        print('outdated textures:', ', '.join(bad))
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
