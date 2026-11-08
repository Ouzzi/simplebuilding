"""Textures and model of the strip station cage (IG31-59, own pixel art, nothing taken from Vanilla sheets): a wooden frame
(floor, four corner posts, top rim) with four 12x12 side panels. Three panel variants for the owner's pick: A iron bars,
B wooden slats, C iron mesh; A is built in; the preview a-strip10 (answer STRIP10) lets the owner switch.
python3 modules/simpletools/tools/gen_strip_station.py [--check] [--variant A|B|C]"""
import argparse
import json
import sys
from pathlib import Path
from PIL import Image
sys.path.insert(0, str(Path(__file__).resolve().parents[3] / 'tools/textures'))
from layers import save, snap_grid  # noqa: E402  layer sources beside the PNGs (A8 b)

MODULE = Path(__file__).resolve().parents[1]
ASSETS = MODULE / 'shared/resources/assets/simpletools'
CHOSEN = 'A'

C = {
    'a': (0x4B, 0x33, 0x1E), 'b': (0x6A, 0x49, 0x29), 'c': (0x86, 0x61, 0x38), 'd': (0x9E, 0x77, 0x49),
    'l': (0x22, 0x22, 0x2A), 'i': (0x3E, 0x3F, 0x48), 'j': (0x6A, 0x6C, 0x77), 'k': (0xA2, 0xA6, 0xB2),
}


def planks(iron_studs=False):
    """Four boards of 4 px, seams staggered, a soft lighter top edge and darker lower edge on each board."""
    rows = []
    for y in range(16):
        board, ly = divmod(y, 4)
        seam = (5, 11, 3, 9)[board]
        row = ''
        for x in range(16):
            if ly == 3:
                ch = 'a'
            elif x == seam:
                ch = 'a'
            elif ly == 0:
                ch = 'c'
            else:
                ch = 'b'
            # sparse grain, deterministic
            if ch == 'b' and (x * 7 + y * 13 + board * 5) % 11 == 0:
                ch = 'c' if (x + y) % 2 else 'a'
            if ch == 'c' and (x * 5 + y * 3) % 9 == 0:
                ch = 'd'
            row += ch
        rows.append(row)
    if iron_studs:
        for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14), (1, 7), (14, 7)):
            rows[y] = rows[y][:x] + 'j' + rows[y][x + 1:]
            rows[y - 1 if y > 0 else y + 1] = rows[y - 1 if y > 0 else y + 1][:x] + 'k' + rows[y - 1 if y > 0 else y + 1][x + 1:]
    return rows


def blank():
    return [['.'] * 16 for _ in range(16)]


def bars_a():
    """Iron bars: three vertical bars of 2 px (lit left pixel), a ring at top and bottom of the 12x12 opening."""
    g = blank()
    for y in range(2, 14):
        for x0 in (3, 7, 11):
            g[y][x0] = 'k'
            g[y][x0 + 1] = 'j'
            if y % 6 == 1:
                g[y][x0 + 1] = 'i'
    for x in range(2, 14):
        g[2][x], g[13][x] = 'j', 'i'
        g[3][x] = g[3][x] if g[3][x] != '.' else 'i'
    return [''.join(r) for r in g]


def bars_b():
    """Wooden slats: three boards of 3 px with 1 px gaps, an iron nail in each end."""
    g = blank()
    for y in range(2, 14):
        ly = (y - 2) % 4
        if ly == 3:
            continue
        for x in range(2, 14):
            g[y][x] = 'c' if ly == 0 else 'b'
            if ly == 2:
                g[y][x] = 'a' if x % 5 == 0 else 'b'
    for y in (3, 7, 11):
        g[y][3], g[y][12] = 'j', 'j'
    return [''.join(r) for r in g]


def bars_c():
    """Iron mesh: 1 px bars in a grid of 4 px, a heavier outer frame."""
    g = blank()
    for y in range(2, 14):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (2, 13)
            vert, hor = x in (5, 9), y in (5, 9)
            if edge:
                g[y][x] = 'j' if (x + y) % 2 else 'i'
            elif vert or hor:
                g[y][x] = 'k' if (vert and hor) else 'j'
    return [''.join(r) for r in g]


VARIANTS = {
    'A': dict(frame=lambda: planks(False), panel=bars_a, name='Iron bars'),
    'B': dict(frame=lambda: planks(False), panel=bars_b, name='Wooden slats'),
    'C': dict(frame=lambda: planks(True), panel=bars_c, name='Iron mesh'),
}

# The model elements (1/16 block), shared by the model file and the preview renderer: (from, to, texture, faces).
POST = [(0, 2, 0, 2, 14, 2), (14, 2, 0, 16, 14, 2), (0, 2, 14, 2, 14, 16), (14, 2, 14, 16, 14, 16)]
RIM = [(0, 14, 0, 16, 16, 2), (0, 14, 14, 16, 16, 16), (0, 14, 2, 2, 16, 14), (14, 14, 2, 16, 16, 14)]
FLOOR = [(0, 0, 0, 16, 2, 16)]
PANELS = [(2, 2, 0.5, 14, 14, 1.5, 'z'), (2, 2, 14.5, 14, 14, 15.5, 'z'), (0.5, 2, 2, 1.5, 14, 14, 'x'), (14.5, 2, 2, 15.5, 14, 14, 'x')]


def model_json():
    elements = []
    for x0, y0, z0, x1, y1, z1 in FLOOR + POST + RIM:
        elements.append({'from': [x0, y0, z0], 'to': [x1, y1, z1], 'faces': {
            d: {'uv': uv(d, x0, y0, z0, x1, y1, z1), 'texture': '#frame', **({'cullface': d} if cull(d, x0, y0, z0, x1, y1, z1) else {})}
            for d in ('north', 'south', 'east', 'west', 'up', 'down')}})
    for x0, y0, z0, x1, y1, z1, axis in PANELS:
        faces = ('north', 'south') if axis == 'z' else ('east', 'west')
        elements.append({'from': [x0, y0, z0], 'to': [x1, y1, z1], 'faces': {d: {'uv': [2, 2, 14, 14], 'texture': '#bars'} for d in faces}})
    return {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
            'textures': {'frame': 'simpletools:block/strip_station_frame', 'bars': 'simpletools:block/strip_station_bars',
                         'particle': 'simpletools:block/strip_station_frame'},
            'elements': elements}


def cull(d, x0, y0, z0, x1, y1, z1):
    return {'north': z0 == 0, 'south': z1 == 16, 'west': x0 == 0, 'east': x1 == 16, 'down': y0 == 0, 'up': y1 == 16}[d]


def uv(d, x0, y0, z0, x1, y1, z1):
    if d in ('up', 'down'):
        return [x0, z0, x1, z1]
    if d in ('north', 'south'):
        return [x0, 16 - y1, x1, 16 - y0]
    return [z0, 16 - y1, z1, 16 - y0]


def paint(rows):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), C[ch] + (255,))
    return img


def images(variant):
    v = VARIANTS[variant]
    frame_rows, panel_rows = v['frame'](), v['panel']()
    frame = snap_grid(paint(frame_rows), frame_rows, C, [('Eisen', 'ijkl')], 'Holz')
    panel = snap_grid(paint(panel_rows), panel_rows, C, [('Eisen', 'ijkl')], 'Holz')
    return frame, panel


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--variant', default=CHOSEN)
    args = ap.parse_args()
    frame, panel = images(args.variant)
    out = ASSETS / 'textures/block'
    model = ASSETS / 'models/block/strip_station.json'
    text = json.dumps(model_json(), indent=2) + '\n'
    stale = []
    for name, img in (('strip_station_frame', frame), ('strip_station_bars', panel)):
        path = out / f'{name}.png'
        if args.check:
            if not path.is_file() or list(Image.open(path).convert('RGBA').getdata()) != list(img.getdata()):
                stale.append(name)
        else:
            out.mkdir(parents=True, exist_ok=True)
            save(img, path)
    if args.check:
        if not model.is_file() or model.read_text(encoding='utf-8') != text:
            stale.append('model')
    else:
        model.write_text(text, encoding='utf-8')
    if stale:
        print('stale:', ', '.join(stale))
        sys.exit(1)
    print('strip station cage ok' if args.check else f'strip station cage written (variant {args.variant})')


if __name__ == '__main__':
    main()
