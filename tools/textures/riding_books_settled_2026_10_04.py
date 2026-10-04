"""Generate the owner's settled Riding books; --check verifies installed pixels.

Proposal B is the old double-jump/range book with +12% contrast. Apply a
further 25% to the proposal's RGB distances from its opaque-pixel mean,
round and clamp per channel, preserving alpha and transparent pixels.
"""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw

import horseshoe_template_and_riding_books_2026_10_04 as proposal

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'modules/simpleriding/shared/resources/assets/simpleriding'
BOOKS = {'leaping': 'double_jump', 'tailwind': 'range'}


def more_contrast(image):
    points = proposal.ps.opaque(image)
    mean = tuple(sum(image.getpixel(p)[i] for p in points) / len(points) for i in range(3))
    result = image.copy()
    for point in points:
        pixel = image.getpixel(point)
        result.putpixel(point, proposal.contrast(pixel[:3], 1.25, mean) + (pixel[3],))
    return result


def main():
    # Pin the formula, clipping and alpha independently of the installed sprites.
    sample = Image.new('RGBA', (3, 1))
    sample.putdata([(10, 20, 30, 255), (210, 220, 230, 128), (80, 90, 100, 0)])
    checked = more_contrast(sample)
    assert [checked.getpixel((x, 0)) for x in range(3)] == [(0, 0, 5, 255), (235, 245, 255, 128), (80, 90, 100, 0)]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--preview', type=Path, default=ROOT / 'build/riding-books-settled-vorschau.png')
    args = parser.parse_args()
    sheet = Image.new('RGB', (960, 650), '#292c32')
    draw = ImageDraw.Draw(sheet)
    draw.text((20, 12), 'Simple Riding - Besitzer-Buecher (16x, ohne Glaettung)', fill='white')
    for x, title in zip((120, 400, 680), ('A: alt', 'B: Vorschlag (+12 %)', 'C: Vorschlag +25 %')):
        draw.text((x, 40), title, fill='white')
    for row, (name, source) in enumerate(BOOKS.items()):
        old = proposal.book(source, 1.0)
        proposed = proposal.book(source, 1.12)
        settled = more_contrast(proposed)
        assert settled.size == (16, 16)
        assert settled.getchannel('A').tobytes() == old.getchannel('A').tobytes()
        path = ASSETS / f'textures/item/enchanted_book_{name}.png'
        if args.check:
            with Image.open(path) as installed:
                assert installed.size == (16, 16) and installed.convert('RGBA').tobytes() == settled.tobytes(), path
        else:
            settled.save(path)
        y = 75 + row * 280
        draw.text((12, y + 115), name.title(), fill='white')
        for x, sprite in zip((120, 400, 680), (old, proposed, settled)):
            sheet.paste(sprite.resize((256, 256), Image.Resampling.NEAREST), (x, y),
                        sprite.getchannel('A').resize((256, 256), Image.Resampling.NEAREST))
    if not args.check:
        args.preview.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(args.preview)
        print(f'Preview: {args.preview}')
    print('Riding books: 2/2 pixels, 16x16 and unchanged alpha OK')


if __name__ == '__main__':
    main()
