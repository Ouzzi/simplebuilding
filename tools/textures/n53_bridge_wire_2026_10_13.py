"""Bridge Redstone wire, N53 (owner 2026-10-13, correction of N52): the old look again, as one finished PNG per part.

The look before N52: a thin, wavy 1-2 px dust line and a ragged dot with holes, its pixels braided along the diagonals
((x + z) % 3) between the Astral, Nihil and redstone-red ramps. That old version recoloured Vanilla's redstone_dust
pixels; this one is an own redrawing in the same look (AGENTS.md point 7), checked in as plain PNGs - nothing is
generated at build time and no part is a stack of layers.

No z-fighting: the dot and the line halves of the multipart model lie in one plane and overlap at the centre. Every
pixel colour is taken from ONE picture of the cell in world space (WORLD), so wherever two parts overlap they show the
very same pixel. line1 (east/west parts, rotated y=270 in the blockstate, and the 'up' wall part) is that world picture
turned back by the model rotation: texture pixel (u, v) lands on world pixel (v, 15 - u).
Run: python3.12 tools/textures/n53_bridge_wire_2026_10_13.py [preview.png]
"""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'block')
RAMPS = (   # six brightness steps each (same ramps as the Astral/Nihil powders and redstone red)
    [(38, 18, 34), (74, 36, 64), (122, 60, 104), (176, 98, 152), (226, 156, 204), (255, 214, 238)],       # astral
    [(12, 28, 34), (24, 52, 62), (48, 104, 116), (88, 158, 170), (156, 220, 226), (214, 250, 250)],       # nihil
    [(44, 6, 6), (94, 12, 10), (150, 22, 16), (204, 40, 26), (240, 90, 64), (255, 170, 150)],             # redstone
)
# Wavy north-south line: x of the dust in every row z (a second x where the line steps sideways).
NS = [(7,), (7,), (7, 8), (8,), (8,), (8, 7), (7,), (6, 7), (7,), (7, 8), (8,), (8,), (7, 8), (7,), (7,), (7,)]
# Wavy east-west line (own path, not the transposed north-south one): z of the dust in every column x.
EW = [(8,), (8,), (8, 7), (7,), (7,), (7,), (7, 8), (8,), (8, 9), (8,), (7, 8), (7,), (7,), (7, 8), (8,), (8,)]
DOT = [   # ragged 6x6 dot with two holes, rows z = 5..10, columns x = 5..10
    '.#.##.',
    '######',
    '##.###',
    '######',
    '###.##',
    '.####.',
]


def mask(kind):
    m = set()
    if kind == 'ns':
        for z, xs in enumerate(NS):
            m.update((x, z) for x in xs)
    elif kind == 'ew':
        for x, zs in enumerate(EW):
            m.update((x, z) for z in zs)
    else:
        for dz, row in enumerate(DOT):
            m.update((5 + dx, 5 + dz) for dx, c in enumerate(row) if c == '#')
    return m


def world(active):
    """The whole cell seen from above: one colour per world pixel, shared by every part."""
    lo, mid, hi = (2, 3, 3) if not active else (4, 5, 5)   # like the old textures: mostly step 3 (off) or 5 (on)
    im = Image.new('RGBA', (16, 16))
    for z in range(16):
        for x in range(16):
            h = (x * 7 + z * 13) % 5
            step = lo if h == 0 else hi if h == 1 else mid
            im.putpixel((x, z), RAMPS[(x + z) % 3][step] + (255,))
    return im


def part(w, m, rotated=False):
    out = Image.new('RGBA', (16, 16))
    for u in range(16):
        for v in range(16):
            x, z = (v, 15 - u) if rotated else (u, v)
            if (x, z) in m:
                out.putpixel((u, v), w.getpixel((x, z)))
    return out


def textures():
    out = {}
    for suffix, active in (('', False), ('_on', True)):
        w = world(active)
        out[f'bridge_redstone_dot{suffix}'] = part(w, mask('dot'))
        out[f'bridge_redstone_line0{suffix}'] = part(w, mask('ns'))
        out[f'bridge_redstone_line1{suffix}'] = part(w, mask('ew'), rotated=True)
    return out


def main():
    tex = textures()
    for name, im in tex.items():
        im.save(os.path.join(OUT, name + '.png'))
    if len(sys.argv) > 1:
        preview(tex, sys.argv[1])
    print('ok')


def top_view(tex, suffix, sides):
    """A cell as the multipart puts it together (north/south halves of line0, west/east halves of line1, dot)."""
    im = Image.new('RGBA', (16, 16), (60, 56, 66, 255))
    l0, l1 = tex[f'bridge_redstone_line0{suffix}'], tex[f'bridge_redstone_line1{suffix}']
    for side, src, box in (('n', l0, (0, 0, 16, 8)), ('s', l0, (0, 8, 16, 16)), ('w', l1, (0, 0, 16, 8)), ('e', l1, (0, 8, 16, 16))):
        if side not in sides:
            continue
        half = Image.new('RGBA', (16, 16))
        half.paste(src.crop(box), box[:2])
        if side in 'we':   # blockstate y=270: texture (u, v) -> world (v, 15 - u)
            turned = Image.new('RGBA', (16, 16))
            for u in range(16):
                for v in range(16):
                    turned.putpixel((v, 15 - u), half.getpixel((u, v)))
            half = turned
        im.alpha_composite(half)
    if sides not in ('ns', 'we'):
        im.alpha_composite(tex[f'bridge_redstone_dot{suffix}'])
    return im


def preview(tex, path):
    combos = ['', 'ns', 'we', 'ne', 'nes', 'nesw']
    s, pad = 8, 8
    sheet = Image.new('RGBA', (pad + len(combos) * (16 * s + pad), pad + 2 * (16 * s + pad)), (139, 139, 139, 255))
    for r, suffix in enumerate(('', '_on')):
        for i, combo in enumerate(combos):
            sheet.alpha_composite(top_view(tex, suffix, combo).resize((16 * s, 16 * s), Image.NEAREST),
                                  (pad + i * (16 * s + pad), pad + r * (16 * s + pad)))
    sheet.save(path)


if __name__ == '__main__':
    main()
