"""Usage: python tools/textures/training_dummy_2026_10_02.py <vanilla textures dir> [preview.png]

Straw Armor Stand and Training Dummy (owner 2026-10-02, docs/ai/PLAN-TRAINING-DUMMY-2026-10-02.md), vanilla retextures:
- entity/training_dummy/straw_armor_stand: vanilla entity/armorstand/armorstand pixel for pixel, every wood tone
  swapped by brightness rank for a tone of the vanilla hay bale (block/hay_block_side, its yellow straw only); the
  stone base plate stays as it is.
- entity/training_dummy/training_dummy: the straw stand with twine bindings every few rows, in the red-brown of
  the hay bale's own binding.
- item/straw_armor_stand: vanilla item/armor_stand with the same straw swap (stone slab unchanged).
- (superseded 2026-10-04 by training_dummy_v3_2026_10_04.py, preview only) entity/training_dummy/stuffing (2026-10-03, 64x32, player head/body UV): the dummy's sack head (vanilla
  hay_block_top desaturated to burlap, stitched twine face) and straw torso (hay_block_side) with a red-white target.
- item/training_dummy: the straw item with a burlap head and the red target on the chest.
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


BURLAP_TINT = (0.78, 0.66, 0.50)  # hay -> sacking: keep the straw's light/dark, drop the yellow
TARGET = [(160, 32, 32), (226, 220, 206)]  # red, off-white rings


def burlap(p):
    l = lum(p)
    return tuple(min(255, round(l * t * 1.25)) for t in BURLAP_TINT) + (255,)


def stuffing():
    """64x32 texture in the player head/body layout: head at (0,0) 8x8x8, body at (16,16) 8x12x4."""
    top = load('block/hay_block_top')
    side = load('block/hay_block_side')
    out = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    for y in range(16):          # head: 32x16 block of faces
        for x in range(32):
            out.putpixel((x, y), burlap(top.getpixel((x % 16, y % 16))))
    for y in range(16, 32):      # body: 24x16 block of faces (16..40)
        for x in range(16, 40):
            p = side.getpixel(((x - 16) % 16, (y - 16) % 16))
            if p[1] < 100:       # the hay bale's red binding -> twine
                p = TWINE[1] + (255,)
            out.putpixel((x, y), p[:3] + (255,))
    stitch = (66, 42, 26, 255)  # dark twine, readable on the burlap
    # face on the head front (8..16, 8..16): two cross-stitched eyes and a stitched mouth
    for ex in (9, 13):
        for dx, dy in ((0, 0), (1, 1), (1, 0), (0, 1)):
            out.putpixel((ex + dx, 10 + dy), stitch if (dx + dy) % 2 == 0 else burlap((90, 80, 60)))
    for x in range(10, 14):
        out.putpixel((x, 13 + (x % 2)), stitch)
    # twine round the neck: bottom row of the head side faces
    for x in range(0, 32):
        out.putpixel((x, 15), TWINE[1] + (255,))
    # target on the torso front (20..28, 20..32), centred at (24, 25)
    for y in range(20, 32):
        for x in range(20, 28):
            d = max(abs(x + 0.5 - 24), abs(y + 0.5 - 25.5))
            if d <= 3.5:
                out.putpixel((x, y), (TARGET[0] if d <= 1.0 or 2.0 < d <= 3.0 else TARGET[1]) + (255,))
    return out


def dummy_item(straw_item):
    """The straw item: the top rows become the burlap head, a red dot marks the chest."""
    out = straw_item.copy()
    for y in range(0, 4):
        for x in range(16):
            p = out.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), burlap(p))
    for (x, y) in ((7, 6), (8, 6), (7, 7), (8, 7)):
        if out.getpixel((x, y))[3]:
            out.putpixel((x, y), TARGET[0] + (255,))
    return out


def front_view(stand_tex, stuff):
    """Rough front view for the preview: legs/plate from the stand sheet colours, torso and head from `stuff`."""
    v = Image.new('RGBA', (16, 34), (0, 0, 0, 0))
    v.alpha_composite(stuff.crop((8, 8, 16, 16)), (4, 0))      # head front
    v.alpha_composite(stuff.crop((20, 20, 28, 32)), (4, 8))    # torso front
    leg = stand_tex.getpixel((10, 10))
    for y in range(20, 32):
        for x in (5, 6, 9, 10):
            v.putpixel((x, y), leg)
    for x in range(1, 15):
        for y in (32, 33):
            v.putpixel((x, y), (150, 150, 150, 255))
    return v


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
    stuff = stuffing()
    ditem = dummy_item(item)
    # stuffing.png and item/training_dummy.png come from training_dummy_v3_2026_10_04.py since 2026-10-04 (pumpkin head);
    # this script only previews the round-2 sack head.
    if PREVIEW and 'v2' in os.path.basename(PREVIEW):
        scale = 8
        sheet = Image.new('RGBA', (64 * scale + 3 * 16 * 12 + 120, 34 * 12 + 60), (198, 198, 198, 255))
        draw = ImageDraw.Draw(sheet)
        sheet.alpha_composite(stuff.resize((64 * scale, 32 * scale), Image.NEAREST), (20, 30))
        draw.text((20, 8), 'A stuffing (head + torso)', fill=(0, 0, 0, 255))
        x0 = 40 + 64 * scale
        sheet.alpha_composite(front_view(dummy, stuff).resize((16 * 12, 34 * 12), Image.NEAREST), (x0, 30))
        draw.text((x0, 8), 'B dummy front', fill=(0, 0, 0, 255))
        for i, (label, im) in enumerate([('C straw item', item), ('D dummy item', ditem)]):
            x = 20 + i * (16 * 12 + 30)
            sheet.alpha_composite(im.resize((16 * 12, 16 * 12), Image.NEAREST), (x, 32 * scale + 60))
            draw.text((x, 32 * scale + 44), label, fill=(0, 0, 0, 255))
        sheet.save(PREVIEW)
    elif PREVIEW:
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
