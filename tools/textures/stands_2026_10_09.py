"""Usage: python tools/textures/stands_2026_10_09.py [<vanilla textures dir> [preview.png]]

Small Armor Stand item icon (owner 2026-10-09, Nachtrag 29, docs/ai/PLAN-STAENDER-2026-10-09.md). Own shape in the
vanilla armor stand's wood and stone tones (no vanilla pixels copied): a wooden post with dark rings and a cross bar
on top (a T, like the entity model and the owner's reference pictures), standing on a smooth stone plate.
The medium armor stand was removed in the same round.
Writes into mc26_3/overlay/resources/assets/simplebuilding/textures/item/ and, if a vanilla dir and a preview path are
given, a 16x preview sheet: vanilla armor stand | small, each also on a light background."""
from PIL import Image
import os
import sys

V = sys.argv[1] if len(sys.argv) > 1 else None
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
OUT = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'item')

PAL = {
    'o': (61, 48, 30),     # wood outline, darkest
    'd': (95, 73, 40),     # wood shadow
    'm': (143, 119, 69),   # wood mid
    'l': (162, 130, 81),   # wood light
    'h': (180, 144, 90),   # wood highlight
    '1': (157, 157, 157),  # smooth stone dark
    '2': (168, 168, 168),  # smooth stone mid
    '3': (176, 176, 176),  # smooth stone light
    'k': (118, 118, 118),  # stone shadow edge
    'K': (88, 88, 88),     # stone outline
}

SMALL = [
    '................',
    '................',
    '....ddddddddd...',
    '...dhlhlhlhlho..',
    '....ooodloooo...',
    '.......dho......',
    '.......dlo......',
    '.......ooo......',
    '.......dho......',
    '.......dlo......',
    '.......dho......',
    '.......ooo......',
    '.......dlo......',
    '..k3232323232K..',
    '..k2121212121K..',
    '...KKKKKKKKKKK..',
]


def draw(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), 'rows must be 16x16'
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), PAL[ch] + (255,))
    return img


def main():
    out = {'small_armor_stand': draw(SMALL)}
    os.makedirs(OUT, exist_ok=True)
    for name, img in out.items():
        img.save(os.path.join(OUT, name + '.png'))
    if V and PREVIEW:
        vanilla = Image.open(os.path.join(V, 'item', 'armor_stand.png')).convert('RGBA')
        icons = [vanilla, out['small_armor_stand']]
        scale, pad = 16, 16
        sheet = Image.new('RGBA', (len(icons) * (16 * scale + pad) + pad, 2 * (16 * scale + pad) + pad), (40, 40, 40, 255))
        for i, icon in enumerate(icons):
            big = icon.resize((16 * scale, 16 * scale), Image.NEAREST)
            x = pad + i * (16 * scale + pad)
            sheet.alpha_composite(big, (x, pad))
            light = Image.new('RGBA', big.size, (198, 198, 198, 255))
            light.alpha_composite(big)
            sheet.alpha_composite(light, (x, 2 * pad + 16 * scale))
        sheet.save(PREVIEW)


if __name__ == '__main__':
    main()
