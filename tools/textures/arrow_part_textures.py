"""Usage: python tools/textures/arrow_part_textures.py <out-dir> <vanilla textures dir> [preview.png]

Layer textures for the Fletching Table arrows (B14, owner 2026-10-01). Derived from vanilla
item/arrow.png: its pixels are split into tip (grey head), shaft (brown diagonal) and fletching
(white feathers bottom left). Each layer keeps vanilla's shape and shading ranks and takes its tones
from the vanilla item that makes the part (flint, copper nugget, end rod, phantom membrane ...).
Flint + stick + feather stays pixel-identical to the vanilla arrow. Output goes to
mc26_3/overlay/resources/assets/simplebuilding/textures/item/arrow/."""
from PIL import Image
import os
import sys

V = sys.argv[2] if len(sys.argv) > 2 else 'build/vanilla-textures/'  # vanilla assets/minecraft/textures/
OUT = sys.argv[1] if len(sys.argv) > 1 else 'build/arrow-textures/'
PREVIEW = sys.argv[3] if len(sys.argv) > 3 else None

SHAFT_COLORS = {(40, 30, 11), (137, 103, 39)}
FLETCHING_TOP = 11  # rows from here down (outside the shaft) are the fletching

# part id -> (vanilla texture, own mod texture dir or None); None texture = keep vanilla tones
TIPS = {
    'flint': None, 'copper': 'item/copper_nugget', 'iron': 'item/iron_nugget', 'gold': 'item/gold_nugget',
    'diamond': 'mod:item/diamond_pebble', 'netherite': 'mod:item/netherite_nugget',
    'enderite': 'mod:item/enderite_nugget', 'amethyst': 'item/amethyst_shard', 'prismarine': 'item/prismarine_shard',
}
SHAFTS = {'stick': None, 'end_rod': 'block/end_rod', 'blaze_rod': 'item/blaze_rod', 'breeze_rod': 'item/breeze_rod'}
FLETCHINGS = {'feather': None, 'phantom_membrane': 'item/phantom_membrane'}
MOD = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'simplebuilding', 'textures')


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def load(name):
    if name.startswith('mod:'):
        path = os.path.join(MOD, name[4:] + '.png')
    else:
        path = os.path.join(V, name + '.png')
    return Image.open(path).convert('RGBA')


def ramp(name, tones):
    """`tones` colours from the texture, dark to light, at even luminance quantiles of its opaque pixels."""
    im = load(name)
    px = sorted((im.getpixel((x, y))[:3] for x in range(im.width) for y in range(im.height) if im.getpixel((x, y))[3] > 200), key=lum)
    if tones == 1:
        return [px[len(px) // 2]]
    return [px[round(0.12 * (len(px) - 1) + i * 0.8 * (len(px) - 1) / (tones - 1))] for i in range(tones)]


def split(arrow):
    layers = {'tip': {}, 'shaft': {}, 'fletching': {}}
    for y in range(16):
        for x in range(16):
            p = arrow.getpixel((x, y))
            if p[3] == 0:
                continue
            if p[:3] in SHAFT_COLORS:
                layers['shaft'][(x, y)] = p
            elif y >= FLETCHING_TOP:
                layers['fletching'][(x, y)] = p
            else:
                layers['tip'][(x, y)] = p
    return layers


def recolor(pixels, source):
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    shades = sorted({p[:3] for p in pixels.values()}, key=lum)
    tones = ramp(source, len(shades)) if source else shades
    mapping = dict(zip(shades, tones))
    for (x, y), p in pixels.items():
        out.putpixel((x, y), mapping[p[:3]] + (255,))
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    layers = split(load('item/arrow'))
    made = {}
    for kind, table in (('tip', TIPS), ('shaft', SHAFTS), ('fletching', FLETCHINGS)):
        for part, source in table.items():
            im = recolor(layers[kind], source)
            im.save(os.path.join(OUT, f'{kind}_{part}.png'))
            made[(kind, part)] = im
    if PREVIEW:
        scale, cols = 4, len(SHAFTS) * len(FLETCHINGS)
        sheet = Image.new('RGBA', (cols * 18 * scale, len(TIPS) * 18 * scale), (198, 198, 198, 255))
        for row, tip in enumerate(TIPS):
            col = 0
            for shaft in SHAFTS:
                for fletching in FLETCHINGS:
                    cell = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
                    for layer in (made[('shaft', shaft)], made[('fletching', fletching)], made[('tip', tip)]):
                        cell.alpha_composite(layer)
                    sheet.alpha_composite(cell.resize((16 * scale, 16 * scale), Image.NEAREST), (col * 18 * scale + scale, row * 18 * scale + scale))
                    col += 1
        sheet.save(PREVIEW)


if __name__ == '__main__':
    main()
