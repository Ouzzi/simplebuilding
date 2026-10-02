"""Usage: python tools/textures/material_rods_2026_10_02.py <vanilla textures dir> [preview.png]

Material rods (owner 2026-10-02, docs/ai/PLAN-RODS-2026-10-02.md), all vanilla retextures:
- Gold Rod (block): vanilla block/lightning_rod pixel for pixel, each copper tone swapped by brightness rank for a tone
  of the vanilla gold block ramp - the same method as the Iron Rod (proposals_v3_2026_10_02.iron_rod_textures).
- Diamond / Netherite / Enderite Rod (items): vanilla item/blaze_rod pixel for pixel, each blaze tone swapped by
  brightness rank for a tone of the material's ramp (vanilla item/diamond, item/netherite_ingot, the mod's
  enderite_ingot), sampled like the fletching parts (arrow_part_textures.ramp).
Writes the textures into mc26_3/overlay/resources/assets/simplebuilding/textures/ and, if given, a preview sheet with
the vanilla originals, the rods, and the arrows with the new shafts (arrow_part_textures must have run first)."""
from PIL import Image, ImageDraw
import os
import sys

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'  # vanilla assets/minecraft/textures/
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
OUT = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures')
MOD = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'simplebuilding', 'textures')

# gold block (vanilla block/gold_block) from light to dark, its top highlight and one mid tone left out
GOLD = [(255, 253, 144), (255, 236, 79), (255, 216, 62), (245, 204, 39), (211, 150, 50), (204, 142, 39)]
RODS = {'diamond_rod': 'item/diamond', 'netherite_rod': 'item/netherite_ingot', 'enderite_rod': 'mod:item/enderite_ingot'}


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def load(name):
    path = os.path.join(MOD, name[4:] + '.png') if name.startswith('mod:') else os.path.join(V, name + '.png')
    return Image.open(path).convert('RGBA')


def ramp(name, tones):
    """`tones` colours of the texture, dark to light, at even luminance quantiles (as arrow_part_textures.ramp)."""
    im = load(name)
    px = sorted((im.getpixel((x, y))[:3] for x in range(im.width) for y in range(min(16, im.height))
                 if im.getpixel((x, y))[3] > 200), key=lum)
    return [px[round(0.12 * (len(px) - 1) + i * 0.8 * (len(px) - 1) / (tones - 1))] for i in range(tones)]


def swap(src, dark_to_light):
    """`src` pixel for pixel, its distinct tones replaced by rank (darkest -> first)."""
    shades = sorted({src.getpixel((x, y))[:3] for y in range(16) for x in range(16) if src.getpixel((x, y))[3]}, key=lum)
    mapping = {c: dark_to_light[min(len(dark_to_light) - 1, round(i * (len(dark_to_light) - 1) / max(1, len(shades) - 1)))]
               for i, c in enumerate(shades)}
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), mapping[p[:3]] + (p[3],))
    return out


def gold_rod():
    return swap(load('block/lightning_rod'), list(reversed(GOLD)))


def rod_item(material):
    blaze = load('item/blaze_rod')
    tones = len({blaze.getpixel((x, y))[:3] for y in range(16) for x in range(16) if blaze.getpixel((x, y))[3]})
    return swap(blaze, ramp(material, tones))


def main():
    made = {'block/gold_rod': gold_rod()}
    for rod, material in RODS.items():
        made['item/' + rod] = rod_item(material)
    for name, im in made.items():
        path = os.path.join(OUT, name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        im.save(path)
    if PREVIEW:
        preview(made)


def preview(made):
    scale, cell = 8, 18
    iron = Image.open(os.path.join(OUT, 'block', 'iron_rod.png')).convert('RGBA')
    rows = [
        ('Blitzableiter / Eisenstab / Goldstab', [load('block/lightning_rod'), iron, made['block/gold_rod']]),
        ('Lohenrute / Diamant- / Netherit- / Enderitstab',
         [load('item/blaze_rod')] + [made['item/' + rod] for rod in RODS]),
    ]
    arrows = os.path.join(OUT, 'item', 'arrow')
    if os.path.exists(os.path.join(arrows, 'shaft_diamond_rod.png')):
        layer = lambda n: Image.open(os.path.join(arrows, n + '.png')).convert('RGBA')
        for fletching in ('feather', 'phantom_membrane'):
            cells = []
            for shaft in ['stick', 'blaze_rod'] + list(RODS):
                im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
                for part in ('shaft_' + shaft, 'fletching_' + fletching, 'tip_flint'):
                    im.alpha_composite(layer(part))
                cells.append(im)
            rows.append(('Pfeile: Stock, Lohenrute, Diamant, Netherit, Enderit (' + fletching + ')', cells))
    width = max(len(r[1]) for r in rows) * cell * scale
    sheet = Image.new('RGBA', (width, len(rows) * (cell * scale + 14)), (198, 198, 198, 255))
    draw = ImageDraw.Draw(sheet)
    for r, (label, cells) in enumerate(rows):
        top = r * (cell * scale + 14)
        draw.text((4, top + 1), label, fill=(40, 40, 40, 255))
        for c, im in enumerate(cells):
            sheet.alpha_composite(im.resize((16 * scale, 16 * scale), Image.NEAREST), (c * cell * scale + scale, top + 14 + scale))
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    sheet.save(PREVIEW)


if __name__ == '__main__':
    main()
