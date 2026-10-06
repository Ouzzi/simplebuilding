"""Usage: python tools/textures/chess_2026_10_06.py [--check] [--no-preview]

Schach (owner 2026-10-06, docs/ai/PLAN-SCHACH-2026-10-06.md). Derives everything for the octets and chess pieces
from the hand-painted quartz checkers, so the pieces carry exactly the checker colours:

  textures/block/chess/<colour>.png         the checker's material field (8x8, the quartz field for quartz) tiled 2x2
  textures/block/chess/<colour>_accent.png  the same field lighter (dark colours) or darker (light colours)
  models/block/chess/octet_corner.json      one octet (0.5^3) in the lower north-west corner, template
  models/block/chess/octet_<colour>.json    the corner per colour; the blockstate turns it into all eight corners
  blockstates/checker_octet.json            multipart: colour + bit -> corner model with x/y rotation and uvlock
  models/block/chess_pieces.json            particle only (the pieces are drawn by ChessPiecesRenderer)
  models/item/chess/<piece>[_flat].json     piece templates made of cuboids, front to the north, centred in the block
  models/item/chess/<item id>.json          per colour, textures #m (material) and #a (accent)
  models/item/chess/octet.json + <colour>_octet.json   the octet item (a centred half cube)
  items/<item id>.json                      item definitions

and the preview C:/Users/o_o/code/minecraft-mods/previews/schach-vorschau.png (every colour: octet, 3D pieces, flat
pieces, stairs and slab; below octet examples), labelled. --check compares instead of writing. Needs Pillow.
"""
import io
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
CHECKERS = os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'simplebuilding', 'textures', 'block')
PREVIEW = r'C:\Users\o_o\code\minecraft-mods\previews\schach-vorschau.png'

# Order = ChessColor; None = quartz (field from the lapis checker's quartz half).
COLOURS = [
    ('quartz', None), ('purpur', 'purpur_quartz_checker'), ('lapis', 'lapis_quartz_checker'),
    ('blackstone', 'blackstone_quartz_checker'), ('resin', 'resin_quartz_checker'),
    ('nether_brick', 'nether_brick_quartz_checker'), ('red_nether_brick', 'red_nether_brick_quartz_checker'),
    ('nihilith', 'nihilith_quartz_checker'), ('astralit', 'astralit_quartz_checker'),
    ('ender_quartz', 'ender_quartz_checker'), ('polished_astralit', 'polished_astralit_checker'),
    ('polished_nihilith', 'polished_nihilith_checker'), ('polished_ender_quartz', 'polished_ender_quartz_checker'),
]
PIECES = ['pawn', 'rook', 'knight', 'bishop', 'queen', 'king']
HEIGHTS = {'pawn': 7, 'rook': 9, 'knight': 10, 'bishop': 11, 'queen': 12, 'king': 14}

# --- 3D pieces: cuboids (x1, y1, z1, x2, y2, z2, texture) in pixels, foot 5..11, front to the north (-z) ---
FOOT = [(5, 0, 5, 11, 1, 11, 'm'), (6, 1, 6, 10, 2, 10, 'a')]
SHAPES = {
    'pawn': FOOT[:1] + [(6, 1, 6, 10, 2, 10, 'm'), (7, 2, 7, 9, 4, 9, 'm'), (6, 4, 6, 10, 5, 10, 'a'),
                        (7, 5, 6, 9, 7, 10, 'm'), (6, 5, 7, 10, 7, 9, 'm')],
    'rook': FOOT[:1] + [(6, 1, 6, 10, 7, 10, 'm'), (5, 7, 5, 11, 8, 11, 'a'),
                        (5, 8, 5, 7, 9, 7, 'm'), (9, 8, 5, 11, 9, 7, 'm'), (5, 8, 9, 7, 9, 11, 'm'), (9, 8, 9, 11, 9, 11, 'm')],
    'knight': FOOT + [(6, 2, 7, 10, 6, 10, 'm'), (6, 6, 5, 10, 9, 9, 'm'), (7, 5, 4, 9, 7, 6, 'm'),
                      (6, 9, 8, 7, 10, 9, 'a'), (9, 9, 8, 10, 10, 9, 'a'), (7, 3, 10, 9, 8, 11, 'a'),
                      (6, 7, 6, 7, 8, 7, 'a'), (9, 7, 6, 10, 8, 7, 'a')],
    'bishop': FOOT + [(7, 2, 7, 9, 6, 9, 'm'), (6, 6, 6, 10, 7, 10, 'a'), (6, 7, 7, 10, 9, 9, 'm'),
                      (7, 7, 6, 9, 9, 10, 'm'), (7, 9, 7, 9, 10, 9, 'm'), (7, 10, 7, 9, 11, 9, 'a')],
    'queen': FOOT + [(6, 2, 6, 10, 4, 10, 'm'), (7, 4, 7, 9, 8, 9, 'm'), (6, 8, 6, 10, 9, 10, 'a'),
                     (6, 9, 6, 10, 10, 10, 'm'), (6, 10, 6, 7, 11, 7, 'm'), (9, 10, 6, 10, 11, 7, 'm'),
                     (6, 10, 9, 7, 11, 10, 'm'), (9, 10, 9, 10, 11, 10, 'm'), (7, 10, 7, 9, 12, 9, 'a')],
    'king': FOOT + [(6, 2, 6, 10, 4, 10, 'm'), (7, 4, 7, 9, 9, 9, 'm'), (6, 9, 6, 10, 10, 10, 'a'),
                    (6, 10, 6, 10, 11, 10, 'm'), (7, 11, 7, 9, 14, 9, 'a'), (6, 12, 7, 10, 13, 9, 'a')],
}

# --- flat pieces: a 6x6 token (1 px, corners cut) with the symbol raised 1 px in the accent; row 0 = north ---
ICONS = {
    'pawn': ['..##..', '.####.', '..##..', '..##..', '.####.', '######'],
    'rook': ['#.##.#', '######', '.####.', '.####.', '.####.', '######'],
    'knight': ['..##..', '.###..', '##.#..', '..###.', '.####.', '######'],
    'bishop': ['..#...', '.##.#.', '.#.##.', '..##..', '.####.', '######'],
    'queen': ['.#..#.', '#.##.#', '.####.', '..##..', '.####.', '######'],
    'king': ['..##..', '######', '..##..', '.####.', '.####.', '######'],
}


def flat_shape(piece):
    """An 8x8 token (one checker field, corners cut, 1 px) with the 6x6 symbol raised 1 px in the accent."""
    icon = ['.' * 8] + ['.' + row + '.' for row in ICONS[piece]] + ['.' * 8]
    boxes = []
    for row in range(8):
        col = 0
        while col < 8:
            corner = row in (0, 7) and col in (0, 7)
            raised = icon[row][col] == '#'
            if corner:
                col += 1
                continue
            end = col + 1
            while end < 8 and (icon[row][end] == '#') == raised and not (row in (0, 7) and end == 7):
                end += 1
            boxes.append((4 + col, 0, 4 + row, 4 + end, 2 if raised else 1, 5 + row, 'a' if raised else 'm'))
            col = end
    return boxes


def shape(piece, flat):
    return flat_shape(piece) if flat else SHAPES[piece]


# --- textures ---
def field(checker):
    if checker is None:
        img = Image.open(os.path.join(CHECKERS, 'lapis_quartz_checker.png')).convert('RGBA')
        tile = img.crop((0, 0, 8, 8))
    else:
        img = Image.open(os.path.join(CHECKERS, checker + '.png')).convert('RGBA')
        tile = img.crop((8, 0, 16, 8))
    out = Image.new('RGBA', (16, 16))
    for x in (0, 8):
        for y in (0, 8):
            out.paste(tile, (x, y))
    return out


def accent(img):
    px = [img.getpixel((x, y)) for y in range(img.height) for x in range(img.width)]
    lum = sum(0.299 * r + 0.587 * g + 0.114 * b for r, g, b, a in px) / len(px) / 255.0
    mean = [sum(p[i] for p in px) / len(px) for i in range(3)]
    out = []
    for r, g, b, a in px:
        # Halb geglaettet (ruhige Flaeche fuer Symbole), dann deutlich heller bzw. dunkler als das Material.
        smooth = [0.5 * c + 0.5 * m for c, m in zip((r, g, b), mean)]
        if lum < 0.42:
            out.append(tuple(int(round(c + (255 - c) * 0.55)) for c in smooth) + (a,))
        else:
            out.append(tuple(int(round(c * 0.45)) for c in smooth) + (a,))
    res = Image.new('RGBA', img.size)
    res.putdata(out)
    return res


def png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return buf.getvalue()


def js(data):
    return (json.dumps(data, indent=2) + '\n').encode('utf-8')


def faces(texture, cull=None):
    out = {}
    for f in ('north', 'south', 'east', 'west', 'up', 'down'):
        face = {'texture': texture}
        if cull and f in cull:
            face['cullface'] = f
        out[f] = face
    return out


def piece_display(piece, flat):
    h = 2 if flat else HEIGHTS[piece]
    if flat:
        gui = {'rotation': [80, 0, 0], 'translation': [0, 0, 0], 'scale': [2.2, 2.2, 2.2]}
        fixed = {'rotation': [-90, 0, 0], 'translation': [0, 0, -6], 'scale': [2.0, 2.0, 2.0]}
    else:
        s = round(max(0.9, min(1.4, 12.0 / h)), 2)
        gui = {'rotation': [30, 225, 0], 'translation': [0, round(s * (8 - h / 2) * 0.87, 2), 0], 'scale': [s, s, s]}
        fixed = {'rotation': [0, 180, 0], 'translation': [0, round(8 - h / 2, 2), 0], 'scale': [1.2, 1.2, 1.2]}
    return {
        'gui': gui,
        'fixed': fixed,
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.6, 0.6, 0.6]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.6, 0.6, 0.6]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 2, 0], 'scale': [0.8, 0.8, 0.8]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 2, 0], 'scale': [0.8, 0.8, 0.8]},
    }


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


def outputs():
    files = {}
    a = lambda rel, data: files.__setitem__(os.path.join(ASSETS, *rel.split('/')), data)
    for colour, checker in COLOURS:
        tex = field(checker)
        a(f'textures/block/chess/{colour}.png', png_bytes(tex))
        a(f'textures/block/chess/{colour}_accent.png', png_bytes(accent(tex)))
    # octet block
    a('models/block/chess/octet_corner.json', js({
        'parent': 'minecraft:block/block',
        'textures': {'particle': '#all'},
        'elements': [{'from': [0, 0, 0], 'to': [8, 8, 8], 'faces': faces('#all', cull=('north', 'west', 'down'))}]}))
    multipart = []
    for colour, _ in COLOURS:
        a(f'models/block/chess/octet_{colour}.json', js({
            'parent': 'simplebuilding:block/chess/octet_corner', 'textures': {'all': f'simplebuilding:block/chess/{colour}'}}))
        for i in range(8):
            x, y, z = i & 1, (i >> 1) & 1, (i >> 2) & 1
            xr, yr = corner_rotation(x, y, z)
            apply = {'model': f'simplebuilding:block/chess/octet_{colour}', 'uvlock': True}
            if xr:
                apply['x'] = xr
            if yr:
                apply['y'] = yr
            multipart.append({'when': {'color': colour, f'o{x}{y}{z}': 'true'}, 'apply': apply})
    a('blockstates/checker_octet.json', js({'multipart': multipart}))
    a('models/block/chess_pieces.json', js({'textures': {'particle': 'simplebuilding:block/chess/quartz'}}))
    a('blockstates/chess_pieces.json', js({'variants': {'': {'model': 'simplebuilding:block/chess_pieces'}}}))
    # octet item
    a('models/item/chess/octet.json', js({
        'parent': 'minecraft:block/block',
        'textures': {'particle': '#all'},
        'elements': [{'from': [4, 4, 4], 'to': [12, 12, 12], 'faces': faces('#all')}],
        'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [1.0, 1.0, 1.0]}}}))
    for colour, _ in COLOURS:
        a(f'models/item/chess/{colour}_octet.json', js({
            'parent': 'simplebuilding:item/chess/octet', 'textures': {'all': f'simplebuilding:block/chess/{colour}'}}))
        a(f'items/{colour}_octet.json', js({'model': {'type': 'minecraft:model', 'model': f'simplebuilding:item/chess/{colour}_octet'}}))
    # pieces
    for piece in PIECES:
        for flat in (False, True):
            name = piece + ('_flat' if flat else '')
            elements = [{'from': list(b[:3]), 'to': list(b[3:6]), 'faces': faces('#' + b[6])} for b in shape(piece, flat)]
            a(f'models/item/chess/{name}.json', js({
                'textures': {'particle': '#m'}, 'elements': elements, 'display': piece_display(piece, flat)}))
    for colour, _ in COLOURS:
        for flat in (False, True):
            for piece in PIECES:
                item = f'{colour}_chess_{piece}' + ('_flat' if flat else '')
                a(f'models/item/chess/{item}.json', js({
                    'parent': f'simplebuilding:item/chess/{piece}' + ('_flat' if flat else ''),
                    'textures': {'m': f'simplebuilding:block/chess/{colour}', 'a': f'simplebuilding:block/chess/{colour}_accent'}}))
                a(f'items/{item}.json', js({'model': {'type': 'minecraft:model', 'model': f'simplebuilding:item/chess/{item}'}}))
    return files


# --- preview: tiny isometric voxel renderer ---
def voxels(boxes):
    out = {}
    for x1, y1, z1, x2, y2, z2, t in boxes:
        for x in range(int(x1), int(x2)):
            for y in range(int(y1), int(y2)):
                for z in range(int(z1), int(z2)):
                    out[(x, y, z)] = t
    return out


def render(vox, textures, size=5, turn=True):
    """Isometric view from the south-east; texture per voxel key. turn: front (north) turned to face the viewer."""
    if turn:
        vox = {(15 - x, y, 15 - z): t for (x, y, z), t in vox.items()}
    if not vox:
        return Image.new('RGBA', (8, 8))
    w = size
    pts = []
    for (x, y, z) in vox:
        pts.append(((x - z) * w, (x + z) * w / 2 - y * w))
    minx = min(p[0] for p in pts) - w * 2
    maxx = max(p[0] for p in pts) + w * 2
    miny = min(p[1] for p in pts) - w * 2
    maxy = max(p[1] for p in pts) + w * 2
    img = Image.new('RGBA', (int(maxx - minx), int(maxy - miny)), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def col(t, u, v, shade):
        r, g, b, _ = textures[t].getpixel((u % 16, v % 16))
        return (int(r * shade), int(g * shade), int(b * shade), 255)

    for (x, y, z) in sorted(vox, key=lambda p: (p[0] + p[2], p[1])):
        t = vox[(x, y, z)]
        ox = (x - z) * w - minx
        oy = (x + z) * w / 2 - y * w - miny
        top = [(ox, oy - w / 2), (ox + w, oy), (ox, oy + w / 2), (ox - w, oy)]
        if (x, y + 1, z) not in vox:
            d.polygon(top, fill=col(t, x, z, 1.0))
        if (x, y, z + 1) not in vox:
            d.polygon([(ox - w, oy), (ox, oy + w / 2), (ox, oy + w * 1.5), (ox - w, oy + w)], fill=col(t, x, 15 - y, 0.78))
        if (x + 1, y, z) not in vox:
            d.polygon([(ox, oy + w / 2), (ox + w, oy), (ox + w, oy + w), (ox, oy + w * 1.5)], fill=col(t, 15 - z, 15 - y, 0.6))
    return img


def top_down(vox, textures, size=7):
    """View from straight above (north up): the highest voxel of each column, darker the lower it is."""
    tops = {}
    for (x, y, z), t in vox.items():
        if (x, z) not in tops or y > tops[(x, z)][0]:
            tops[(x, z)] = (y, t)
    xs = [x for x, _ in tops]
    zs = [z for _, z in tops]
    img = Image.new('RGBA', ((max(xs) - min(xs) + 1) * size, (max(zs) - min(zs) + 1) * size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for (x, z), (y, t) in tops.items():
        r, g, b, _ = textures[t].getpixel((x % 16, z % 16))
        shade = 1.0 if y >= 1 else 0.85
        px, pz = (x - min(xs)) * size, (z - min(zs)) * size
        d.rectangle([px, pz, px + size - 1, pz + size - 1], fill=(int(r * shade), int(g * shade), int(b * shade), 255))
    return img


def preview(files):
    tex = {}
    for colour, checker in COLOURS:
        base = Image.open(io.BytesIO(files[os.path.join(ASSETS, 'textures', 'block', 'chess', colour + '.png')])).convert('RGBA')
        acc = Image.open(io.BytesIO(files[os.path.join(ASSETS, 'textures', 'block', 'chess', colour + '_accent.png')])).convert('RGBA')
        chk = Image.open(os.path.join(CHECKERS, checker + '.png')).convert('RGBA') if checker else None
        tex[colour] = {'m': base, 'a': acc, 'c': chk}
    cell_w, cell_h = 96, 112
    label_w = 150
    cols = ['A Achtel'] + [f'B {p}' for p in PIECES] + [f'C {p} 2D' for p in PIECES] + ['D Treppe', 'E Stufe']
    rows = len(COLOURS)
    extra_h = 230
    img = Image.new('RGBA', (label_w + cell_w * len(cols), 40 + cell_h * rows + extra_h), (198, 198, 198, 255))
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype('arial.ttf', 13)
    except OSError:
        font = ImageFont.load_default()
    for i, c in enumerate(cols):
        d.text((label_w + i * cell_w + 4, 12), c, fill=(0, 0, 0, 255), font=font)

    def put(sprite, x, y, w=cell_w, h=cell_h):
        img.alpha_composite(sprite, (int(x + (w - sprite.width) / 2), int(y + h - 8 - sprite.height)))

    for r, (colour, checker) in enumerate(COLOURS):
        y = 40 + r * cell_h
        d.text((6, y + cell_h / 2 - 8), colour, fill=(0, 0, 0, 255), font=font)
        t = tex[colour]
        put(render(voxels([(0, 0, 0, 8, 8, 8, 'm')]), t, 6, False), label_w, y)
        for i, piece in enumerate(PIECES):
            put(render(voxels(shape(piece, False)), t, 5), label_w + (1 + i) * cell_w, y)
            put(top_down(voxels(shape(piece, True)), t, 11), label_w + (7 + i) * cell_w, y)
        if checker:
            ct = {'c': t['c']}
            stairs = [(0, 0, 0, 16, 8, 16, 'c'), (8, 8, 0, 16, 16, 16, 'c')]
            put(render(voxels(stairs), ct, 3, False), label_w + 13 * cell_w, y)
            put(render(voxels([(0, 0, 0, 16, 8, 16, 'c')]), ct, 3, False), label_w + 14 * cell_w, y)
    # octet examples
    y0 = 40 + rows * cell_h + 10
    d.text((6, y0), 'F Achtel-Beispiele (eine Farbe je Zelle; 8/8 = voller Wuerfel)', fill=(0, 0, 0, 255), font=font)
    examples = [
        ('1/8', 'lapis', [(0, 0, 0, 8, 8, 8)]),
        ('Treppe 3/8', 'purpur', [(0, 0, 0, 16, 8, 8), (8, 8, 0, 16, 16, 8)]),
        ('Saeule 2/8', 'blackstone', [(0, 0, 0, 8, 16, 8)]),
        ('7/8', 'resin', [(0, 0, 0, 16, 8, 16), (0, 8, 0, 16, 16, 8), (0, 8, 8, 8, 16, 16)]),
        ('8/8', 'ender_quartz', [(0, 0, 0, 16, 16, 16)]),
        ('Bogen 2 Zellen', 'quartz', [(0, 0, 0, 8, 16, 8), (24, 0, 0, 32, 16, 8), (0, 16, 0, 32, 24, 8)]),
    ]
    for i, (label, colour, boxes) in enumerate(examples):
        x = label_w + i * 200
        sprite = render(voxels([b + ('m',) for b in boxes]), tex[colour], 4, False)
        put(sprite, x, y0 + 20, 200, 190)
        d.text((x + 10, y0 + 200), f'{label} ({colour})', fill=(0, 0, 0, 255), font=font)
    return img


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
        print(f'OK: {len(files)} chess files up to date')
        return
    print(f'wrote {len(files)} files')
    if '--no-preview' not in sys.argv:
        img = preview(files)
        big = img.resize((img.width, img.height), Image.NEAREST)
        os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
        big.save(PREVIEW)
        print('preview ' + PREVIEW)


if __name__ == '__main__':
    main()
