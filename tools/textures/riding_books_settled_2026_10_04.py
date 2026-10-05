"""Generate the Simple Riding books Leaping and Tailwind; --check verifies installed pixels.

Owner 2026-10-05: "Why did you use existing enchanted-book textures for Simple Riding? Please make new ones." The
earlier version (contrast-boosted copies of the double_jump / range books) is replaced by new motifs in the style of
the owner's books, drawn in texture_round6_2026_10_05.py (three variants each, INSTALL picks the built-in one):
Leaping = horseshoe arch with nails on jump-boost green, Tailwind = wind strokes with a curl on pale wind teal.
"""
import argparse
import os
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import texture_round6_2026_10_05 as round6  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'modules/simpleriding/shared/resources/assets/simpleriding'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--preview', type=Path, default=ROOT / 'build/riding-books-settled-vorschau.png')
    args = parser.parse_args()
    names = list(round6.INSTALL)
    sheet = Image.new('RGB', (40 + 3 * 300, 60 + len(names) * 300), '#292c32')
    draw = ImageDraw.Draw(sheet)
    draw.text((20, 12), 'Simple Riding - neue Buchmotive (16x), eingebaut: '
              + ', '.join(f'{n} {v}' for n, v in round6.INSTALL.items()), fill='white')
    for row, name in enumerate(names):
        y = 40 + row * 300
        for col, variant in enumerate('ABC'):
            sprite = round6.riding_book(name, variant)
            assert sprite.size == (16, 16)
            x = 20 + col * 300
            sheet.paste(sprite.resize((256, 256), Image.Resampling.NEAREST), (x, y),
                        sprite.getchannel('A').resize((256, 256), Image.Resampling.NEAREST))
            mark = ' (eingebaut)' if variant == round6.INSTALL[name] else ''
            draw.text((x, y + 262), f'{name} {variant}: {round6.RIDING_MOTIFS[name][variant][0]}{mark}', fill='white')
        installed = round6.riding_book(name, round6.INSTALL[name])
        path = ASSETS / f'textures/item/enchanted_book_{name}.png'
        if args.check:
            with Image.open(path) as current:
                assert current.size == (16, 16) and current.convert('RGBA').tobytes() == installed.tobytes(), path
        else:
            installed.save(path)
    if not args.check:
        args.preview.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(args.preview)
        print(f'Preview: {args.preview}')
    print(f'Riding books: {len(names)}/{len(names)} OK')


if __name__ == '__main__':
    main()
