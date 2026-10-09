"""Usage: python tools/textures/octets_2026_10_09.py [--check] [--preview DIR] [--vanilla DIR]

Material-Achtel (Queue Nachtrag 24, docs/ai/PLAN-Q-HAMMER-OCTETS-2026-10-09.md). Writes into the 26.3 overlay:

  blockstates/<wood>_octet.json, blockstates/melon_octet.json   multipart: bit -> corner model (x/y rotation, uvlock)
  models/block/octet/<wood>.json         the chess corner template with the vanilla planks texture (no copy)
  models/block/octet/melon_corner.json   one melon octet: outer faces rind (melon_side / melon_top), cut faces flesh
  models/item/octet/<wood>_octet.json + items/<wood>_octet.json   the octet item (the chess octet item shape)
  textures/block/melon_flesh.png         own 16x16 pixel art: the cut melon (flesh with seeds)

--check compares instead of writing. --preview DIR renders octet examples (needs the vanilla textures in --vanilla DIR,
an extracted assets/ tree; nothing of it is checked in). Needs Pillow.
"""
import io
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
# Order = ModBlocks.OCTET_WOODS
WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'pale_oak', 'poplar',
         'bamboo', 'crimson', 'warped']

# melon flesh: the reds of the vanilla melon slice, seeds in its darkest red-brown
FLESH = {'r': (191, 49, 35), 'l': (193, 60, 45), 'h': (202, 113, 93), 'd': (175, 22, 11), 's': (89, 24, 15),
         'k': (122, 24, 14)}
FLESH_MAP = [
    'rrlrrdrrlrrrdrlr',
    'rlrrrrrsrrlrrrrr',
    'rrrdrlrkrrrrlrdr',
    'lrsrrrrrrdrrrrrr',
    'rrkrrlrrrrrsrlrr',
    'rrrrrrrdlrrkrrrr',
    'rdrlrrrrrrrrrrdl',
    'rrrrrsrrrlrrdrrr',
    'rlrrrkrrrrrrrrrr',
    'rrrdrrrlrrsrrrlr',
    'hrrrrrrrrrkrdrrr',
    'rrlrsrdrrrrrrrrr',
    'rrrrkrrrlrrrrsrl',
    'rdrrrrrrrrdrrkrr',
    'rrrlrrhrrrrlrrrr',
    'rlrrrdrrrdrrrrdr',
]


def png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return buf.getvalue()


def js(data):
    return (json.dumps(data, indent=2) + '\n').encode('utf-8')


def flesh():
    img = Image.new('RGB', (16, 16))
    for y, row in enumerate(FLESH_MAP):
        for x, c in enumerate(row):
            img.putpixel((x, y), FLESH[c])
    return img


def rotate(pos, xr, yr):
    x, y, z = pos
    if xr == 180:
        y, z = 1 - y, 1 - z
    for _ in range(yr // 90):
        x, z = 1 - z, x
    return x, y, z


def corner_rotation(x, y, z):
    for xr in (0, 180):
        for yr in (0, 90, 180, 270):
            if rotate((0, 0, 0), xr, yr) == (x, y, z):
                return xr, yr
    raise ValueError((x, y, z))


def multipart(model):
    parts = []
    for i in range(8):
        x, y, z = i & 1, (i >> 1) & 1, (i >> 2) & 1
        xr, yr = corner_rotation(x, y, z)
        apply = {'model': model, 'uvlock': True}
        if xr:
            apply['x'] = xr
        if yr:
            apply['y'] = yr
        parts.append({'when': {f'o{x}{y}{z}': 'true'}, 'apply': apply})
    return {'multipart': parts}


def outputs():
    files = {}
    a = lambda rel, data: files.__setitem__(os.path.join(ASSETS, *rel.split('/')), data)
    for wood in WOODS:
        a(f'models/block/octet/{wood}.json', js({
            'parent': 'simplebuilding:block/chess/octet_corner', 'textures': {'all': f'minecraft:block/{wood}_planks'}}))
        a(f'blockstates/{wood}_octet.json', js(multipart(f'simplebuilding:block/octet/{wood}')))
        a(f'models/item/octet/{wood}_octet.json', js({
            'parent': 'simplebuilding:item/chess/octet', 'textures': {'all': f'minecraft:block/{wood}_planks'}}))
        a(f'items/{wood}_octet.json', js({'model': {'type': 'minecraft:model', 'model': f'simplebuilding:item/octet/{wood}_octet'}}))
    # Melon: the corner's outer faces (north, west, down) carry the rind, the three cut faces the flesh.
    outer = {'texture': '#side'}
    a('models/block/octet/melon_corner.json', js({
        'parent': 'minecraft:block/block',
        'textures': {'particle': 'minecraft:block/melon_side', 'side': 'minecraft:block/melon_side',
                     'end': 'minecraft:block/melon_top', 'cut': 'simplebuilding:block/melon_flesh'},
        'elements': [{'from': [0, 0, 0], 'to': [8, 8, 8], 'faces': {
            'north': dict(outer, cullface='north'), 'west': dict(outer, cullface='west'),
            'down': {'texture': '#end', 'cullface': 'down'},
            'south': {'texture': '#cut'}, 'east': {'texture': '#cut'}, 'up': {'texture': '#cut'}}}]}))
    a('blockstates/melon_octet.json', js(multipart('simplebuilding:block/octet/melon_corner')))
    a('textures/block/melon_flesh.png', png_bytes(flesh()))
    return files


# --- preview: isometric voxels, one texture per voxel face kind ---
def render(cells, tex, size=6):
    """cells: {(x, y, z): key} on a 0.5 grid (octets); tex[key] = (top, side) 16x16 images. View from south-east."""
    w = size * 8
    pts = []
    for (x, y, z) in cells:
        pts.append((x, y, z))
    img = Image.new('RGBA', (w * 6, w * 6), (0, 0, 0, 0))
    ox, oy = w * 3, w * 3

    def proj(x, y, z):
        return ox + (x - z) * w * 0.87, oy + (x + z) * w * 0.5 - y * w

    order = sorted(cells, key=lambda p: (p[0] + p[2], p[1]))
    d = ImageDraw.Draw(img)
    for (x, y, z) in order:
        top, side = tex[cells[(x, y, z)]]
        ct = top.resize((1, 1), Image.BOX).getpixel((0, 0))[:3]
        cs = side.resize((1, 1), Image.BOX).getpixel((0, 0))[:3]
        s = 0.5
        faces = [
            ([(x, y + s, z), (x + s, y + s, z), (x + s, y + s, z + s), (x, y + s, z + s)], ct, 1.0),
            ([(x, y, z + s), (x + s, y, z + s), (x + s, y + s, z + s), (x, y + s, z + s)], cs, 0.8),
            ([(x + s, y, z), (x + s, y, z + s), (x + s, y + s, z + s), (x + s, y + s, z)], cs, 0.6),
        ]
        for quad, col, shade in faces:
            d.polygon([proj(*p) for p in quad], fill=tuple(int(c * shade) for c in col) + (255,), outline=(40, 40, 40, 255))
    bbox = img.getbbox()
    return img.crop(bbox) if bbox else img


def textured_face(tex, size=64):
    return tex.resize((size, size), Image.NEAREST)


def preview(files, vanilla, out_dir):
    vt = lambda n: Image.open(os.path.join(vanilla, 'assets', 'minecraft', 'textures', 'block', n + '.png')).convert('RGBA')
    fl = Image.open(io.BytesIO(files[os.path.join(ASSETS, 'textures', 'block', 'melon_flesh.png')])).convert('RGBA')
    tex = {w: (vt(w + '_planks'), vt(w + '_planks')) for w in WOODS}
    tex['melon'] = (vt('melon_top'), vt('melon_side'))
    tex['flesh'] = (fl, fl)
    full = {(x * 0.5, y * 0.5, z * 0.5) for x in (0, 1) for y in (0, 1) for z in (0, 1)}
    examples = [
        ('oak: 1 Achtel oben weg = Treppe', 'oak', full - {(0.5, 0.5, 0.5)}),
        ('oak: oben+unten je 1 weg = Achtel', 'oak', full - {(0.5, 0.5, 0.5), (0.0, 0.0, 0.5)}),
        ('spruce: Diagonale = Achtel', 'spruce', full - {(0.5, 0.5, 0.5), (0.0, 0.5, 0.0)}),
        ('cherry: Stufe - 1 = Achtel', 'cherry', {p for p in full if p[1] == 0} - {(0.5, 0.0, 0.5)}),
        ('crimson: Saeule 2/8', 'crimson', {(0.0, 0.0, 0.0), (0.0, 0.5, 0.0)}),
        ('melon: 7/8 (Schnitt = Fruchtfleisch)', 'melon', full - {(0.5, 0.5, 0.5)}),
    ]
    font = ImageFont.load_default()
    cw, ch = 230, 210
    img = Image.new('RGBA', (cw * 3, ch * 2 + 40 + 120), (198, 198, 198, 255))
    d = ImageDraw.Draw(img)
    d.text((8, 8), 'Material-Achtel (Queue N24): Hammer-Zerlegung und Melone - Farben je Flaeche gemittelt', fill=(0, 0, 0, 255), font=font)
    for i, (label, key, cells) in enumerate(examples):
        cx, cy = (i % 3) * cw, 40 + (i // 3) * ch
        cellmap = {}
        for p in cells:
            k = key
            if key == 'melon':
                # cut faces show flesh: approximate by tinting the voxel whose +x/+z/+y neighbour is missing
                k = 'flesh' if any((p[0] + dx, p[1] + dy, p[2] + dz) not in cells and 0 <= p[0] + dx <= 0.5
                                   and 0 <= p[1] + dy <= 0.5 and 0 <= p[2] + dz <= 0.5
                                   for dx, dy, dz in ((0.5, 0, 0), (0, 0.5, 0), (0, 0, 0.5))) else 'melon'
            cellmap[p] = k
        sprite = render(cellmap, tex, 12)
        img.alpha_composite(sprite, (cx + (cw - sprite.width) // 2, cy + 10))
        d.text((cx + 6, cy + ch - 24), label, fill=(0, 0, 0, 255), font=font)
    # textures row: the new flesh texture and the wood planks used by the octets
    y = 40 + 2 * ch
    d.text((8, y), 'neu: melon_flesh.png (x4) | Holz-Achtel nutzen die Vanilla-Bretter:', fill=(0, 0, 0, 255), font=font)
    img.alpha_composite(textured_face(fl), (8, y + 16))
    for i, w in enumerate(WOODS):
        img.alpha_composite(textured_face(tex[w][0], 32), (90 + i * 40, y + 16))
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, 'octets-n24.png')
    img.save(path)
    print('preview ' + path)


def main():
    check = '--check' in sys.argv
    files = outputs()
    bad = []
    for path, data in files.items():
        if check:
            try:
                with open(path, 'rb') as f:
                    if f.read() != data:
                        bad.append(path)
            except OSError:
                bad.append(path)
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, 'wb') as f:
                f.write(data)
    if check:
        if bad:
            print('OUTDATED: ' + str(len(bad)) + ' files, e.g. ' + bad[0])
            sys.exit(1)
        print(f'OK: {len(files)} octet files up to date')
        return
    print(f'wrote {len(files)} files')
    if '--preview' in sys.argv:
        out_dir = sys.argv[sys.argv.index('--preview') + 1]
        vanilla = sys.argv[sys.argv.index('--vanilla') + 1]
        preview(files, vanilla, out_dir)


if __name__ == '__main__':
    main()
