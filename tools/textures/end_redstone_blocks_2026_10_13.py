"""Astral, Nihil and Bridge Redstone Block (N51, 2026-10-13, docs/ai/PLAN-REDSTONE-LINIEN-2026-10-11.md).

Own pixel art, no vanilla pixels: drawn from pixel maps onto the line ramps of the dust (B7 ramps for Astral and Nihil,
the redstone red of end_signal_wire_2026_10_02.py for the third bridge strand). All three share the frame of a storage
block - a uniform light rim (no light top/left vs dark bottom/right) and a darker inner bevel - and each gets its own
motif: Astral a four-pointed star with small sparkles, Nihil a light ring around a dark void with pits, Bridge three
wide diagonal strands (Astral, Nihil, redstone) across the face.

W23-1 (owner 23.10. evening, T5 = A): the built faces are now the Vanilla redstone block build of
sbtex23_2026_10_23.rs_vanilla (light rim, noisy compacted dust, irregular dark core; Astral star glints, Nihil void and
pits, Bridge red with Astral and Nihil veins, 10 tones per line, layers). draw() below is the N51 art, kept for the record.

Writes textures/block/<line>_redstone_block.png (+ .layers.json) into the 26.3 overlay (the models come from datagen) and
a 16x preview (single faces and a 3x3 tiling) to SB_PREVIEW_DIR (default /root/previews/redstone-compat).
--check only compares the built PNGs.
Run: python3.12 tools/textures/end_redstone_blocks_2026_10_13.py
"""
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import layers  # noqa: E402
import sbtex23_2026_10_23 as sbtex23  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'block')
PREVIEW = os.environ.get('SB_PREVIEW_DIR', '/root/previews/redstone-compat')

# Same six steps as proposals_b7_2026_10_02.RAMPS and end_signal_wire_2026_10_02.RED (darker to lighter).
RAMPS = {
    'astral': [(38, 18, 34), (74, 36, 64), (122, 60, 104), (176, 98, 152), (226, 156, 204), (255, 214, 238)],
    'nihil': [(12, 28, 34), (24, 52, 62), (48, 104, 116), (88, 158, 170), (156, 220, 226), (214, 250, 250)],
    'red': [(44, 6, 6), (94, 12, 10), (150, 22, 16), (204, 40, 26), (240, 90, 64), (255, 170, 150)],
}

# Frame shared by all three: 4 = rim, 3 = inner light ring, 2 = field; '.' is filled by the motif.
FRAME = ['4' * 16, '4' + '3' * 14 + '4'] + ['43' + '.' * 12 + '34'] * 12 + ['4' + '3' * 14 + '4', '4' * 16]

# Astral: a four-pointed star (5 = core, 4 = arms), dark gaps (1) beside it, sparkles in the corners; field 2 with
# a few lighter 3 flecks.
ASTRAL = [
    '................',
    '................',
    '..4.....2.....3.',
    '.454....3.......',
    '..4....131......',
    '.......141......',
    '...3...141....4.',
    '..2.1114541112..',
    '....1115551113..',
    '.......141....  ',
    '.......141......',
    '..3....131...4..',
    '........3...454.',
    '.....2.......4..',
    '................',
    '................',
]
# Nihil: a ring of 4/3 around a dark void (0/1), three dark pits (1) with a light lip (3).
NIHIL = [
    '................',
    '................',
    '..13......3.....',
    '..3.............',
    '.......333......',
    '.....3344433....',
    '....34111143....',
    '....341000143...',
    '...3410000143...',
    '....34100143..13',
    '....3441143...3.',
    '.....334433.....',
    '.3.....33.......',
    '.13.........13..',
    '................',
    '................',
]

LINE_OF_STRAND = ('astral', 'nihil', 'red')


def pad(rows):
    return [(row + '.' * 16)[:16].replace(' ', '.') for row in rows]


def draw(kind):
    im = Image.new('RGBA', (16, 16))
    motif = pad(ASTRAL if kind == 'astral' else NIHIL if kind == 'nihil' else ['.' * 16] * 16)
    for y in range(16):
        for x in range(16):
            f = FRAME[y][x]
            if kind == 'bridge':
                ramp, step = bridge_pixel(x, y, f)
            else:
                ramp = RAMPS[kind]
                if f != '.':
                    step = int(f)
                else:
                    m = motif[y][x]
                    step = 2 if m == '.' else int(m)
            im.putpixel((x, y), ramp[step] + (255,))
    return im


def bridge_pixel(x, y, f):
    """Three wide diagonal strands (Astral, Nihil, redstone), five pixels each with a dark seam, running down to the
    left; each strand keeps its line's ramp, the rim and inner ring take the strand they cross."""
    t = x + y
    ramp = RAMPS[LINE_OF_STRAND[(t // 5) % 3]]
    if f != '.':
        return ramp, int(f)
    return ramp, (1, 3, 3, 2, 2)[t % 5]


def main():
    check = '--check' in sys.argv
    os.makedirs(OUT, exist_ok=True)
    faces, bad = {}, []
    for kind in ('astral', 'nihil', 'bridge'):
        im = sbtex23.rs_vanilla(kind)
        path = os.path.join(OUT, f'{kind}_redstone_block.png')
        if check:
            if not os.path.exists(path) or Image.open(path).convert('RGBA').tobytes() != im.tobytes():
                bad.append(path)
            continue
        layers.save(im, path)
        faces[kind] = im
    if check:
        for path in bad:
            print('out of date:', path)
        raise SystemExit(1 if bad else 0)
    preview(faces)
    print('ok')


def preview(faces):
    scale, pad_px = 16, 16
    cell = 16 * scale
    sheet = Image.new('RGBA', (pad_px + len(faces) * (cell * 2 + pad_px * 2), cell * 3 // 2 + pad_px * 3 + 20), (139, 139, 139, 255))
    d = ImageDraw.Draw(sheet)
    for i, (kind, im) in enumerate(faces.items()):
        x = pad_px + i * (cell * 2 + pad_px * 2)
        d.text((x, 4), f'{kind}_redstone_block', fill=(255, 255, 255))
        sheet.alpha_composite(im.resize((cell, cell), Image.NEAREST), (x, 20))
        tile = Image.new('RGBA', (48, 48))
        for ty in range(3):
            for tx in range(3):
                tile.alpha_composite(im, (tx * 16, ty * 16))
        sheet.alpha_composite(tile.resize((cell * 3 // 4 * 3 // 3, cell * 3 // 4 * 3 // 3), Image.NEAREST), (x + cell + pad_px, 20))
    os.makedirs(PREVIEW, exist_ok=True)
    sheet.save(os.path.join(PREVIEW, 'line-redstone-blocks.png'))


if __name__ == '__main__':
    main()
