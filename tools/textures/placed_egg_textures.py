"""Usage: python tools/textures/placed_egg_textures.py [assets dir] [vanilla client jar or textures dir]

Placed eggs (owner 2026-10-02, reworked the same day): a real 3D egg in the vanilla look. The vanilla item sprite
(minecraft:item/egg, blue_egg, brown_egg) is the colour source: the egg's silhouette is turned into a stack of
cuboids (one sprite row = half a pixel high, half the sprite width wide, rows of equal width merged), and each
cuboid's sides show exactly its rows of the sprite - speckles, highlight and shadow as on the item, without the dark
outline (a 3D egg gets its edges from the face shading). The texture is the sprite with the outline replaced by the
neighbouring shell colour and the transparent rest filled with the nearest shell colour (for the tops of the steps).

Writes into the assets dir (default mc26_3/overlay/resources/assets/simplebuilding):
  textures/block/placed_egg_<white|blue|brown>.png   16x16 shell
  models/block/placed_egg_<colour>.json              the 3D egg (also the legacy placed_egg block state)
  items/placed_egg_<colour>.json                     item model definition; the small-parts renderer draws the egg
                                                     through it (ITEM_MODEL on a stand-in stack)
The cuboids must match PlacedSmallParts#EGG_BOXES (Java); the game test
placed_template_game_test_the_egg_model_matches_the_egg_hitbox checks that."""
from PIL import Image
import io
import json
import os
import sys
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(REPO, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
VANILLA = sys.argv[2] if len(sys.argv) > 2 else os.path.expanduser('~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar')
EGGS = (('egg', 'white'), ('blue_egg', 'blue'), ('brown_egg', 'brown'))


def sprite(name):
    if os.path.isdir(VANILLA):
        return Image.open(os.path.join(VANILLA, 'item', f'{name}.png')).convert('RGBA')
    with zipfile.ZipFile(VANILLA) as jar:
        return Image.open(io.BytesIO(jar.read(f'assets/minecraft/textures/item/{name}.png'))).convert('RGBA')


def spans(im):
    """Per sprite row (top first): (row, x0, x1) of the opaque run, x1 exclusive."""
    out = []
    for y in range(16):
        xs = [x for x in range(16) if im.getpixel((x, y))[3] > 128]
        if xs:
            out.append((y, xs[0], xs[-1] + 1))
    return out


def shell(im):
    rows = spans(im)
    px = {(x, y): im.getpixel((x, y)) for y in range(16) for x in range(16) if im.getpixel((x, y))[3] > 128}
    out = dict(px)
    top, bottom = rows[0][0], rows[-1][0]
    for y, x0, x1 in rows:
        # top and bottom rows are pure outline: take the shell of the row next to them
        if y == top or y == bottom:
            src = y + 1 if y == top else y - 1
            for x in range(x0, x1):
                out[(x, y)] = px.get((x, src), out[(x, y)])
            continue
        if x1 - x0 > 2:
            out[(x0, y)] = px[(x0 + 1, y)]
            out[(x1 - 1, y)] = px[(x1 - 2, y)]
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            if (x, y) in out:
                c = out[(x, y)]
            else:
                c = min(out.items(), key=lambda kv: ((kv[0][0] - x) ** 2 + (kv[0][1] - y) ** 2, kv[0]))[1]
            img.putpixel((x, y), c[:3] + (255,))
    return img


def boxes(im):
    """Cuboids, bottom first: (y0, y1, half width in pixels, top sprite row, bottom sprite row)."""
    rows = spans(im)
    runs = []
    for y, x0, x1 in rows:
        w = x1 - x0
        if runs and runs[-1][2] == w:
            runs[-1][1] = y
        else:
            runs.append([y, y, w])
    bottom = rows[-1][0]
    out = []
    for first, last, w in reversed(runs):
        y0 = (bottom - last) * 0.5
        y1 = (bottom - first + 1) * 0.5
        out.append((y0, y1, w / 4.0, first, last))
    return out


def model(colour, cuboids):
    tex = f'simplebuilding:block/placed_egg_{colour}'
    elements = []
    for i, (y0, y1, half, first, last) in enumerate(cuboids):
        w = int(half * 4)
        u0, u1 = 8 - w // 2, 8 + w // 2
        side = {'uv': [u0, first, u1, last + 1], 'texture': '#egg'}
        faces = {d: dict(side) for d in ('north', 'south', 'east', 'west')}
        faces['up'] = {'uv': [u0, 8 - w // 2, u1, 8 + w // 2], 'texture': '#egg'}
        if i == 0:
            faces['down'] = {'uv': [u0, 8 - w // 2, u1, 8 + w // 2], 'texture': '#egg', 'cullface': 'down'}
        elements.append({'from': [8 - half, y0, 8 - half], 'to': [8 + half, y1, 8 + half], 'faces': faces})
    return {'parent': 'minecraft:block/block', 'textures': {'egg': tex, 'particle': tex}, 'elements': elements}


if __name__ == '__main__':
    for sub in ('textures/block', 'models/block', 'items'):
        os.makedirs(os.path.join(OUT, sub), exist_ok=True)
    for item, colour in EGGS:
        im = sprite(item)
        shell(im).save(os.path.join(OUT, 'textures', 'block', f'placed_egg_{colour}.png'))
        cuboids = boxes(im)
        with open(os.path.join(OUT, 'models', 'block', f'placed_egg_{colour}.json'), 'w', encoding='utf-8', newline='\n') as f:
            json.dump(model(colour, cuboids), f, indent=2)
            f.write('\n')
        with open(os.path.join(OUT, 'items', f'placed_egg_{colour}.json'), 'w', encoding='utf-8', newline='\n') as f:
            json.dump({'model': {'type': 'minecraft:model', 'model': f'simplebuilding:block/placed_egg_{colour}'}}, f, indent=2)
            f.write('\n')
        print(colour, [(c[0], c[1], c[2]) for c in cuboids])
