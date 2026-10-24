"""Wings stage looks, round 2 (owner 24.10., queue WA24-2): Look A pattern on an Elytra-like base shape.

The base shape is Vanilla's Elytra silhouette (item icon and the wing plate of the worn sheet, read from the Vanilla client
jar) with a few cut-outs, so the wings read as "Elytra, slightly changed". Stage 1, stage 2 and stage 3 each get an own
pattern, two variants per stage (1, 2). Owner 24.10. (queue WB24-2): stages 1, 2 and 3 are built in as variant 1, and variant 2
of stage 3 became stage 4, the Void Wings.

    python tools/textures/wa24wings_textures.py                 # write the built-in looks of all four stages into simplemobs
    python tools/textures/wa24wings_textures.py --check         # fail when a committed wings PNG/source differs
    python tools/textures/wa24wings_textures.py --variants DIR  # all stage/variant PNGs + sheets + data (preview page, tools/)
    python tools/textures/wa24wings_textures.py --vanilla JAR   # (with --variants) read the Vanilla masks from the client jar
"""
from __future__ import annotations

import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import elytra24_textures as el  # noqa: E402
import layers  # noqa: E402

ROOT = el.ROOT
MOBS = el.MOBS
# built-in looks: file name -> (stage pattern, variant)
BUILT_IN = {'wither_wings': (1, 1), 'nether_wings': (2, 1), 'end_wings': (3, 1), 'void_wings': (3, 2)}

# Vanilla's wing plate (south face of the 64x32 sheet, 10x20) and item icon (16x16) as silhouette masks. Stored so the generator
# runs without the Vanilla jar; --vanilla JAR re-reads and verifies them.
PLATE_MASK = [
    '######....', '#######...', '########..', '########..', '########..', '#########.', '#########.', '#########.',
    '#########.', '##########', '##########', '##########', '##########', '##########', '.#########', '.#########',
    '.#########', '..########', '..########', '...#######',
]
ICON_MASK = [
    '................', '................', '....########....', '..############..', '.######..######.', '.######..######.',
    '.######..######.', '.######..######.', '.######..######.', '.######..######.', '..#####..#####..', '..#####..#####..',
    '...###....###...', '...##......##...', '................', '................',
]
# own cut-outs (x, y) on top of Vanilla: ragged feather tips at the free end, a notch in the trailing edge
PLATE_CUTS = [(9, 14), (9, 15), (8, 18), (9, 18), (9, 19), (5, 19), (2, 18), (2, 19)]
ICON_CUTS_HALF = [(2, 10), (2, 11), (4, 12), (3, 13)]  # left half, mirrored

PALS = {
    1: {'O': '#0d0c10', 'B': '#7a756f', 'b': '#56514e', 'L': '#4a4752', 'M': '#2e2c34', 'D': '#1c1b21', 'V': '#5c5868',
        'H': '#a29c94', 'h': '#6a6676'},
    2: {'O': '#0d0c10', 'B': '#7a756f', 'b': '#56514e', 'L': '#43343c', 'M': '#2a2027', 'D': '#1a1419', 'V': '#a31f19',
        'H': '#a29c94', 'h': '#ff6a3a'},
    3: {'O': '#0b0714', 'B': '#a79ccf', 'b': '#7d6fa6', 'L': '#43346b', 'M': '#2a2047', 'D': '#150f27', 'V': '#7ae0d0',
        'H': '#d9d0f0', 'h': '#8ff0e6'},
}
PAL3_V2 = {'V': '#c056e0', 'h': '#f0a8ff'}


def verify_vanilla(jar: str) -> None:
    z = zipfile.ZipFile(jar)
    w, h, d = layers.png_read(z.read('assets/minecraft/textures/entity/equipment/wings/elytra.png'))
    got = [''.join('#' if d[(y * w + x) * 4 + 3] else '.' for x in range(36, 46)) for y in range(2, 22)]
    assert got == PLATE_MASK, 'plate mask differs from Vanilla'
    w, h, d = layers.png_read(z.read('assets/minecraft/textures/item/elytra.png'))
    got = [''.join('#' if d[(y * w + x) * 4 + 3] else '.' for x in range(w)) for y in range(h)]
    assert got == ICON_MASK, 'icon mask differs from Vanilla'
    print('masks match Vanilla')


def cut(mask, cuts, mirror=False):
    g = [list(r) for r in mask]
    for x, y in cuts:
        g[y][x] = '.'
        if mirror:
            g[y][len(g[0]) - 1 - x] = '.'
    return [''.join(r) for r in g]


def solid(mask, y, x):
    return 0 <= y < len(mask) and 0 <= x < len(mask[0]) and mask[y][x] == '#'


def edge(mask, y, x):
    return any(not solid(mask, y + dy, x + dx) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


# ---------------------------------------------------------------- patterns: (stage, variant, x, y) -> body char / vein char
def body(stage, var, x, y, w=10):
    """Pattern char of an interior pixel of the plate (S orientation: x 0 = body side, y 0 = shoulder edge)."""
    if stage == 1:
        if var == 1:        # vertical feathers, 2 px wide, light vein on the left px (Look A)
            return 'V' if x % 2 == 0 and y >= 5 else ('L' if (x + y) % 3 else 'M')
        return 'V' if x % 3 == 1 and y >= 4 else ('L' if x % 3 == 0 else 'M')   # 3 px feathers, centre vein
    if stage == 2:
        if var == 1:        # diagonal ember cracks over dark scales
            return 'V' if (x + y) % 6 == 0 and y >= 4 else ('h' if (x + y) % 6 == 0 and y % 4 == 0 else ('M' if (x // 2 + y // 2) % 2 else 'L'))
        return 'V' if y % 4 == 3 and y >= 5 else ('M' if (x + y // 4) % 2 else 'L')   # ember rows between feather rows
    if var == 1:            # End: long pale veins running to the tip, sparkles
        return 'h' if (x * 7 + y * 3) % 23 == 0 else ('V' if (x - y // 2) % 4 == 0 and y >= 4 else ('L' if (x + y) % 3 == 0 else 'M'))
    return 'V' if (x * x + (y - 2) * (y - 2)) // 12 % 2 == 0 and y >= 4 and x % 2 == 0 else ('h' if (x + y) % 11 == 0 else 'M')   # rings


def plate(stage, var):
    """S-orientation 10x20 plate (shape, light)."""
    mask = cut(PLATE_MASK, PLATE_CUTS)
    shape, light = [], []
    for y in range(20):
        row, lit = '', ''
        for x in range(10):
            if not solid(mask, y, x):
                row += '.'; lit += '.'; continue
            if y == 0:
                c, l = 'B', 'H'
            elif y == 1:
                c, l = 'b', '.'
            elif y <= 3:
                c, l = ('L' if (x + y) % 3 else 'M'), '.'
            elif edge(mask, y, x) and y > 4:
                c, l = 'D', '.'
            else:
                c = body(stage, var, x, y)
                l = 'h' if c == 'V' and y % 5 == 1 else '.'
            row += c; lit += l
        shape.append(row); light.append(lit)
    return shape, light


def icon(stage, var):
    mask = cut(ICON_MASK, [(x, y) for x, y in ICON_CUTS_HALF], mirror=True)
    shape, light = [], []
    for y in range(16):
        row, lit = '', ''
        for x in range(16):
            if not solid(mask, y, x):
                row += '.'; lit += '.'; continue
            lx = min(x, 15 - x)                      # distance from the outer side, mirrored pattern
            if edge(mask, y, x):
                c, l = 'O', '.'
            elif y == 3 or (y == 4 and lx >= 2):
                c, l = 'B', ('H' if lx < 5 else '.')
            else:
                c = body(stage, var, lx, y)
                l = 'h' if c == 'V' and y % 5 == 1 else ('H' if not solid(mask, y - 1, x - 1) and x < 8 else '.')
                if c == 'h':
                    c = 'V'
            row += c; lit += l
        shape.append(row); light.append(lit)
    return shape, light


def palette(stage, var):
    return {**PALS[stage], **(PAL3_V2 if stage == 3 and var == 2 else {})}


STAGE_NAMES = {1: 'wither_wings', 2: 'nether_wings', 3: 'end_wings'}


def textures(stage: int, var: int, name: str | None = None) -> dict[str, dict]:
    pal = palette(stage, var)
    s, l = plate(stage, var)
    mirrored = [r[::-1] for r in s], [r[::-1] for r in l]       # sheet() paints north = given, south = its mirror = S
    s64, l64 = el.sheet(*mirrored)
    i, il = icon(stage, var)
    name = name or STAGE_NAMES[stage]
    return {f'item/{name}': el.doc(i, il, pal, 16),
            f'item/{name}_broken': el.doc(*el.broken(i, il), pal, 16),
            f'entity/equipment/wings/{name}': el.doc(s64, l64, pal, 64)}


def main(argv: list[str]) -> int:
    if '--variants' in argv:
        out = Path(argv[argv.index('--variants') + 1])
        out.mkdir(parents=True, exist_ok=True)
        if '--vanilla' in argv:
            verify_vanilla(argv[argv.index('--vanilla') + 1])
        for stage in STAGE_NAMES:
            for var in (1, 2):
                for name, d in textures(stage, var).items():
                    base = f's{stage}v{var}_' + name.replace('/', '_')
                    (out / f'{base}.png').write_bytes(el.scaled(d, 1))
        print(f'variants in {out}')
        return 0
    bad = []
    built = {k: v for n, (st, var) in BUILT_IN.items() for k, v in textures(st, var, n).items()}
    for name, d in built.items():
        png = MOBS / f'{name}.png'
        src = layers.layers_path(png)
        text = layers.dumps(d)
        if '--check' in argv:
            if not src.is_file() or src.read_text(encoding='utf-8') != text or not png.is_file() \
                    or layers.png_read(png.read_bytes())[2] != layers.compose(d):
                bad.append(str(png))
            continue
        png.parent.mkdir(parents=True, exist_ok=True)
        src.write_text(text, encoding='utf-8', newline='\n')
        png.write_bytes(layers.png_write(d['width'], d['height'], bytes(layers.compose(d))))
        print(f'wrote {png.relative_to(ROOT)} (+ layers)')
    if bad:
        print('stale:', *bad, sep='\n  ')
        return 1
    if '--check' in argv:
        print('wa24 wings textures current')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
