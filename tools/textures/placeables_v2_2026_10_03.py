"""Usage: python tools/textures/placeables_v2_2026_10_03.py [--preview-only]

Placeables v2 (owner 2026-10-03). No new pixel art: candles and sea pickles on a small-parts spot reuse the vanilla
block models. This script writes the item model definitions the small-parts renderer draws them through
(ITEM_MODEL on a stand-in stack, see PlacedSmallPartsRenderer#blockModel):

  items/placed_<candle>.json, items/placed_<candle>_lit.json  -> minecraft:block/<candle>_one_candle[_lit]
  items/placed_sea_pickle.json, items/placed_dead_sea_pickle.json -> minecraft:block/[dead_]sea_pickle

and the preview C:/Users/o_o/code/minecraft-mods/previews/placeables-v2-vorschau.png (labelled A..F): the new small
parts (vanilla, mod) and top-down sketches of mixed spots (sprites of the parts at their slot positions).
Needs Pillow for the preview only.
"""
import io
import json
import os
import sys
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
JAR = os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar')
PREVIEW = r'C:\Users\o_o\code\minecraft-mods\previews\placeables-v2-vorschau.png'
COLOURS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan',
           'purple', 'blue', 'brown', 'green', 'red', 'black']
CANDLES = ['candle'] + [c + '_candle' for c in COLOURS]

NEW_VANILLA = ['bone', 'feather', 'arrow', 'spectral_arrow', 'blaze_rod', 'breeze_rod', 'glowstone_dust', 'glow_ink_sac',
               'prismarine_crystals', 'nether_star', 'rabbit_foot', 'turtle_scute', 'armadillo_scute', 'disc_fragment_5',
               'ghast_tear']
NEW_MOD = ['nihilith_shard', 'astralit_dust', 'ender_quartz', 'raw_enderite', 'enderite_scrap', 'cracked_diamond', 'sage_orb']


def write_json(path, data):
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def definitions():
    items = os.path.join(ASSETS, 'items')
    for candle in CANDLES:
        write_json(os.path.join(items, f'placed_{candle}.json'),
                   {'model': {'type': 'minecraft:model', 'model': f'minecraft:block/{candle}_one_candle'}})
        write_json(os.path.join(items, f'placed_{candle}_lit.json'),
                   {'model': {'type': 'minecraft:model', 'model': f'minecraft:block/{candle}_one_candle_lit'}})
    for name in ('sea_pickle', 'dead_sea_pickle'):
        write_json(os.path.join(items, f'placed_{name}.json'), {'model': {'type': 'minecraft:model', 'model': f'minecraft:block/{name}'}})


def preview():
    from PIL import Image, ImageDraw
    jar = zipfile.ZipFile(JAR)

    def vanilla(name):
        return Image.open(io.BytesIO(jar.read(f'assets/minecraft/textures/item/{name}.png'))).convert('RGBA').crop((0, 0, 16, 16))

    def mod(name):
        for root in (ASSETS, os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'simplebuilding'),
                     os.path.join(REPO, 'mc1_21_11', 'fabric', 'src', 'main', 'resources', 'assets', 'simplebuilding')):
            path = os.path.join(root, 'textures', 'item', name + '.png')
            if os.path.exists(path):
                return Image.open(path).convert('RGBA').crop((0, 0, 16, 16))
        raise FileNotFoundError(name)

    scale = 4
    cell = 16 * scale + 16
    width = 16 + cell * 8
    rows = [('A  neue Vanilla-Kleinteile', [vanilla(n) for n in NEW_VANILLA]),
            ('B  neue eigene Kleinteile', [mod(n) for n in NEW_MOD])]
    # Slots je Anzahl (Pixel x, z) wie PlacedSmallParts.SLOTS
    slots = {1: [(8, 8)], 2: [(5.5, 6), (10.5, 10.5)], 3: [(5, 5.5), (11, 6), (7.5, 11)],
             4: [(4.5, 4.5), (11.5, 4.75), (4.75, 11.5), (11.25, 11.25)]}
    mixes = [('C  Kerze + 2 Kiesel + Ei', ['candle', 'pebble', 'pebble', 'egg'], 'stone'),
             ('D  Kerzen an + Splitter', ['red_candle_lit', 'white_candle_lit', 'flint_chip'], 'stone'),
             ('E  Gurken + Kiesel, nass', ['sea_pickle', 'sea_pickle', 'pebble'], 'water'),
             ('F  Glanz: Staub + Stern', ['glowstone_dust', 'nether_star'], 'stone')]
    height = 16 + len(rows) * (cell * 2 + 28) + 28 + 16 * 8 + 40
    img = Image.new('RGBA', (width, height), (40, 40, 46, 255))
    draw = ImageDraw.Draw(img)
    y = 12
    for title, sprites in rows:
        draw.text((12, y), title, fill=(240, 240, 240, 255))
        y += 18
        for i, s in enumerate(sprites):
            x = 12 + (i % 8) * cell
            yy = y + (i // 8) * cell
            img.alpha_composite(s.resize((16 * scale, 16 * scale), Image.NEAREST), (x, yy))
        y += cell * ((len(sprites) + 7) // 8) + 10

    def part_sprite(name):
        if name == 'pebble':
            return mod('stone_pebble')
        if name == 'flint_chip':
            return mod('flint_chip')
        if name.endswith('_lit'):
            base = vanilla(name[:-4])
            glow = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
            ImageDraw.Draw(glow).ellipse((5, 0, 10, 5), fill=(255, 200, 80, 200))
            glow.alpha_composite(base)
            return glow
        return vanilla(name)

    big = 8
    tile = 16 * big
    for i, (title, parts, ground) in enumerate(mixes):
        x0 = 12 + i * (tile + 24)
        draw.text((x0, y), title.split('  ')[0], fill=(240, 240, 240, 255))
        draw.text((x0, y + 18 + tile + 4), title.split('  ')[1], fill=(200, 200, 200, 255))
        top = y + 18
        floor = (110, 110, 110, 255) if ground == 'stone' else (50, 80, 170, 255)
        draw.rectangle((x0, top, x0 + tile - 1, top + tile - 1), fill=floor)
        for j, name in enumerate(parts):
            sx, sz = slots[len(parts)][j]
            spr = part_sprite(name).resize((8 * big, 8 * big), Image.NEAREST)
            img.alpha_composite(spr, (int(x0 + sx * big - 4 * big), int(top + sz * big - 4 * big)))
    os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
    img = img.crop((0, 0, width, y + 18 + tile + 26))
    img.save(PREVIEW)
    print('preview', PREVIEW)


if __name__ == '__main__':
    if '--preview-only' not in sys.argv:
        definitions()
    if '--no-preview' not in sys.argv:
        preview()
