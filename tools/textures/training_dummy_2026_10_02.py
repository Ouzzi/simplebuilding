"""Usage: python tools/textures/training_dummy_2026_10_02.py <vanilla textures dir> [preview.png]

Straw Armor Stand and Training Dummy (owner 2026-10-02, docs/ai/PLAN-TRAINING-DUMMY-2026-10-02.md), vanilla retextures:
- entity/training_dummy/straw_armor_stand: vanilla entity/armorstand/armorstand pixel for pixel, every wood tone
  swapped by brightness rank for a tone of the vanilla hay bale (block/hay_block_side, its yellow straw only); the
  stone base plate stays as it is.
- entity/training_dummy/training_dummy: the straw stand with twine bindings every few rows, in the red-brown of
  the hay bale's own binding.
- item/straw_armor_stand: vanilla item/armor_stand with the same straw swap (stone slab unchanged).
Writes into mc26_3/overlay/resources/assets/simplebuilding/textures/ and, if given, a labelled preview sheet
(A vanilla, B straw stand, C training dummy, D/E items)."""
from PIL import Image, ImageDraw
import os
import sys

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'  # vanilla assets/minecraft/textures/
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
OUT = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures')
TWINE = [(135, 53, 28), (146, 65, 35), (164, 81, 43)]  # hay_block_side binding, dark to light
BAND_EVERY = 5


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def load(name):
    return Image.open(os.path.join(V, name + '.png')).convert('RGBA')


CREDITS_FROM_Y = 56  # the entity sheet's bottom rows hold the authors' credits, left untouched


def is_wood(p, y=0):
    """Wood is warm (red clearly above blue); the stone plate, the outline and the credits are not."""
    return y < CREDITS_FROM_Y and p[3] > 0 and p[0] - p[2] > 25 and lum(p) > 40


def straw_ramp():
    """The hay bale's straw tones (its red binding left out), dark to light, below them two darker shades of the
    darkest straw so the wood's dark outline keeps its contrast."""
    im = load('block/hay_block_side')
    tones = {im.getpixel((x, y))[:3] for x in range(im.width) for y in range(im.height)}
    straw = sorted((t for t in tones if t[1] > 100), key=lum)
    darkest = straw[0]
    return [tuple(round(c * f) for c in darkest) for f in (0.5, 0.72)] + straw


def swap(src, ramp):
    """`src` with its wood tones replaced by rank (darkest wood -> darkest straw)."""
    wood = sorted({src.getpixel((x, y))[:3] for y in range(src.height) for x in range(src.width)
                   if is_wood(src.getpixel((x, y)), y)}, key=lum)
    mapping = {c: ramp[min(len(ramp) - 1, round(i * (len(ramp) - 1) / max(1, len(wood) - 1)))] for i, c in enumerate(wood)}
    out = src.copy()
    for y in range(src.height):
        for x in range(src.width):
            p = src.getpixel((x, y))
            if is_wood(p, y):
                out.putpixel((x, y), mapping[p[:3]] + (p[3],))
    return out, mapping


def bind(straw, src):
    """Twine bands on every BAND_EVERY-th row of the former wood, shaded by the straw below."""
    out = straw.copy()
    ramp = straw_ramp()
    for y in range(src.height):
        if y % BAND_EVERY != 2:
            continue
        for x in range(src.width):
            p = src.getpixel((x, y))
            if is_wood(p, y):
                rank = ramp.index(straw.getpixel((x, y))[:3]) if straw.getpixel((x, y))[:3] in ramp else 1
                out.putpixel((x, y), TWINE[min(2, rank * 3 // len(ramp))] + (255,))
    return out


def main():
    ramp = straw_ramp()
    stand = load('entity/armorstand/armorstand')
    straw, _ = swap(stand, ramp)
    dummy = bind(straw, stand)
    item, _ = swap(load('item/armor_stand'), ramp)
    os.makedirs(os.path.join(OUT, 'entity', 'training_dummy'), exist_ok=True)
    straw.save(os.path.join(OUT, 'entity', 'training_dummy', 'straw_armor_stand.png'))
    dummy.save(os.path.join(OUT, 'entity', 'training_dummy', 'training_dummy.png'))
    item.save(os.path.join(OUT, 'item', 'straw_armor_stand.png'))
    if PREVIEW:
        scale = 6
        sheets = [('A vanilla', stand), ('B straw stand', straw), ('C training dummy', dummy)]
        icons = [('D vanilla item', load('item/armor_stand')), ('E straw item', item)]
        width = len(sheets) * (64 * scale + 20) + 20
        sheet = Image.new('RGBA', (width, 64 * scale + 16 * 16 + 80), (198, 198, 198, 255))
        draw = ImageDraw.Draw(sheet)
        for i, (label, im) in enumerate(sheets):
            x = 20 + i * (64 * scale + 20)
            sheet.alpha_composite(im.resize((64 * scale, 64 * scale), Image.NEAREST), (x, 30))
            draw.text((x, 8), label, fill=(0, 0, 0, 255))
        for i, (label, im) in enumerate(icons):
            x = 20 + i * (16 * 16 + 40)
            y = 64 * scale + 60
            sheet.alpha_composite(im.resize((16 * 16, 16 * 16), Image.NEAREST), (x, y))
            draw.text((x, y - 16), label, fill=(0, 0, 0, 255))
        sheet.save(PREVIEW)


if __name__ == '__main__':
    main()
