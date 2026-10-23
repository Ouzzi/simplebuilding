"""Wither Wings / Nether Wings textures (owner 23.10., queue EL24-1, docs/ai/PLAN-A-ELYTRA24-2026-10-23.md).

Own pixels, close to Vanilla (light from the top left, dark tone of the main colour as item outline), each with its layer
source (layer 1 "Grundform", layer 2 "Licht"). Three looks: A feather wings (built in), B bone wings, C ash feathers.
Per look: the item icon (+ broken icon, like Vanilla's elytra_broken) and the worn texture, a 64x32 Elytra sheet where only
the wing box of ElytraModel (texOffs 22,0, box 10x20x2) is painted. The front face carries the wing plate, the back face its
mirror and the two side faces its edge columns, so the cut-out feather tips line up on the 2 px thick plate; the bottom face
follows the last row. Stage 2 (Nether Wings) is the same shape with ember-red Nether Gem veins.

    python tools/textures/elytra24_textures.py            # write look A into simplemobs
    python tools/textures/elytra24_textures.py --check    # fail when a committed PNG/layer source differs
    python tools/textures/elytra24_textures.py --preview DIR   # PNGs of all looks, 1x and 16x/8x (preview page)
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
MOBS = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/textures'
BUILT_IN = 'A'

# ---------------------------------------------------------------- palettes (stage 1 / stage 2 changes only the veins)
PAL = {
    'A': {'O': '#0d0c10', 'B': '#7a756f', 'b': '#56514e', 'L': '#4a4752', 'M': '#2e2c34', 'D': '#1c1b21', 'V': '#5c5868',
          'H': '#a29c94', 'h': '#6a6676'},
    'B': {'O': '#0b0a0c', 'B': '#8f8a80', 'b': '#615c56', 'L': '#3b2f36', 'M': '#2a2027', 'D': '#1a1419', 'V': '#4a3a44',
          'H': '#b8b2a6', 'h': '#56444f'},
    'C': {'O': '#1a1a1e', 'B': '#9c9a96', 'b': '#75736f', 'L': '#8a8a92', 'M': '#66666f', 'D': '#46464e', 'V': '#7fa6c4',
          'H': '#c8c6c2', 'h': '#a8b8c8'},
}
NETHER = {'V': '#a31f19', 'h': '#ff6a3a'}

# ---------------------------------------------------------------- item icons: left half (8 wide), mirrored to 16
ICON_A = [
    '........',
    'O.......',
    'BO......',
    'bBO.....',
    'OLBOO...',
    'OMLBBOO.',
    'OMMLMBBO',
    'OVMMLMbO',
    'OMVMMMO.',
    '.OMVMMO.',
    '.OMOVMO.',
    '.OMOMO..',
    '..O.MO..',
    '....O...',
    '........',
    '........',
]
ICON_A_LIGHT = [
    '........',
    '........',
    'H.......',
    '.H......',
    '..H.....',
    '...HH...',
    '.....HH.',
    '....h...',
    '...h....',
    '........',
    '........',
    '........',
    '........',
    '........',
    '........',
    '........',
]
ICON_B = [
    '........',
    '........',
    'O.......',
    'BO......',
    'OBOO....',
    'OMBBOOO.',
    'OMMMBBBO',
    'OMMVMMMO',
    '.OMMVMMO',
    '.OMMMVMO',
    '..OMOMVO',
    '..O.OMMO',
    '.....OMO',
    '......OO',
    '........',
    '........',
]
ICON_B_LIGHT = [
    '........',
    '........',
    '........',
    'H.......',
    '.H......',
    '..HH....',
    '....HHH.',
    '........',
    '........',
    '........',
    '........',
    '........',
    '........',
    '........',
    '........',
    '........',
]
ICONS = {'A': (ICON_A, ICON_A_LIGHT), 'B': (ICON_B, ICON_B_LIGHT), 'C': (ICON_A, ICON_A_LIGHT)}


def mirror_half(rows):
    """8-wide left half -> 16 wide with a free 1 px margin: the outermost column folds into the outline."""
    out = []
    for r in rows:
        h = list(r[1:])
        if r[0] != '.' and h[0] != '.':
            h[0] = 'O'
        h = ''.join(h)
        out.append('.' + h + h[::-1] + '.')
    return out


def mirror_light(rows, shape):
    return [''.join(c if shape[y][x] not in '.O' else '.' for x, c in enumerate(row)) for y, row in enumerate(rows)]


def broken(rows, light):
    """Vanilla's broken Elytra hangs limp and torn: drop the lowest feather tips and punch two holes."""
    out = [list(r) for r in rows]
    for y in range(len(out) - 1, -1, -1):
        if any(c != '.' for c in out[y]):
            out[y] = ['.'] * 16
            break
    for x, y in ((2, 8), (13, 9)):
        if out[y][x] != '.':
            out[y][x] = 'D' if out[y][x] != 'O' else 'O'
    lit = [''.join(c if out[y][x] != '.' else '.' for x, c in enumerate(row)) for y, row in enumerate(light)]
    return [''.join(r) for r in out], lit


# ---------------------------------------------------------------- worn wing plate (10 x 20, front-face orientation)
def feather_plate() -> tuple[list[str], list[str]]:
    """Five 2 px primaries hanging from a bone leading edge; the outer ones (left) are the longest."""
    tips = [19, 18, 16, 15, 13]
    shape, light = [], []
    for y in range(20):
        row, lit = '', ''
        for x in range(10):
            k, side = x // 2, x % 2
            tip = tips[k]
            if y == 0:
                c, l = 'B', ('H' if x < 6 else '.')
            elif y == 1:
                c, l = 'b', '.'
            elif y <= 3:
                c, l = ('L' if (x + y) % 3 else 'M'), '.'
            elif y > tip or (y == tip and side == 1):
                c, l = '.', '.'
            elif y == tip:
                c, l = 'D', '.'
            elif side == 0:
                c = 'V' if 5 <= y <= tip - 2 else 'M'
                l = 'h' if y in (6, 10) and k < 3 else '.'
            else:
                c, l = ('D' if y > (4 + tip) // 2 else 'M'), '.'
            row += c
            lit += l
        shape.append(row)
        light.append(lit)
    return shape, light


BONE_PLATE = [
    'BBBBBBBBBB',
    'BbMMMMMMbB',
    'BMMMMMMbMB',
    'BMMMMMbMMB',
    'BMMVMbMMMB',
    'BDMMbMMMMB',
    'bDMbMMVMMB',
    'bDMbMMMMMB',
    '.DbMMMMMDB',
    '.DbMMVMMDB',
    '..bMMMMDDB',
    '..BDMMMDDB',
    '..BDDMMD.B',
    '...BDDD..B',
    '...B.....B',
    '.........b',
    '.........b',
    '.........b',
    '.........B',
    '..........',
]
BONE_LIGHT = ['HHHHHH....'] + ['.' * 10] * 19


def plate(look: str) -> tuple[list[str], list[str]]:
    if look == 'B':
        return BONE_PLATE, BONE_LIGHT
    return feather_plate()


def sheet(shape10, light10) -> tuple[list[str], list[str]]:
    """64x32 Elytra sheet: up 24..34x0..2, down 34..44x0..2, west 22..24, north 24..34, east 34..36, south 36..46 (rows 2..22)."""
    grid = [['.'] * 64 for _ in range(32)]
    lit = [['.'] * 64 for _ in range(32)]

    def put(x, y, c, l='.'):
        grid[y][x] = c
        lit[y][x] = l if c != '.' else '.'

    for y in range(20):
        for x in range(10):
            put(24 + x, 2 + y, shape10[y][x], light10[y][x])                    # north: the plate
            put(36 + x, 2 + y, shape10[y][9 - x], light10[y][9 - x])            # south: its mirror
        for d in range(2):
            put(22 + d, 2 + y, shape10[y][0] if shape10[y][0] == '.' else 'D')  # west: outer edge column
            put(34 + d, 2 + y, shape10[y][9] if shape10[y][9] == '.' else 'D')  # east: inner edge column
    for d in range(2):
        for x in range(10):
            put(24 + x, d, 'B')                                                  # up: the bone edge
            c = shape10[19][x]
            put(34 + x, d, 'D' if c != '.' else '.')                             # down: only under the last row
    return [''.join(r) for r in grid], [''.join(r) for r in lit]


# ---------------------------------------------------------------- layer sources
def doc(shape, light, pal, width) -> dict:
    chars = sorted({c for row in shape + light for c in row if c != '.'})
    palette = [pal[c] + 'ff' for c in chars]
    index = {c: i for i, c in enumerate(chars)}
    d = layers.digits_for(len(palette))
    h = len(shape)

    def rows(grid):
        assert len(grid) == h and all(len(r) == width for r in grid), (len(grid), [len(r) for r in grid][:3])
        return [''.join('.' * d if c == '.' else format(index[c], f'0{d}x') for c in r) for r in grid]

    for y, row in enumerate(light):
        for x, c in enumerate(row):
            assert c == '.' or shape[y][x] != '.', f'light pixel outside the shape at {x},{y}'
    return {'format': layers.FORMAT, 'version': 1, 'width': width, 'height': h, 'digits': d, 'palette': palette,
            'layers': [{'name': 'Grundform', 'visible': True, 'opacity': 100, 'blend': 'normal', 'rows': rows(shape)},
                       {'name': 'Licht', 'visible': True, 'opacity': 100, 'blend': 'normal', 'rows': rows(light)}]}


def textures(look: str) -> dict[str, dict]:
    """name (path under textures/) -> layer source for one look."""
    out = {}
    half, half_light = ICONS[look]
    icon = mirror_half(half)
    icon_light = mirror_light(mirror_half(half_light), icon)
    p10, l10 = plate(look)
    s64, l64 = sheet(p10, l10)
    for stage, extra in (('wither_wings', {}), ('nether_wings', NETHER)):
        pal = {**PAL[look], **extra}
        out[f'item/{stage}'] = doc(icon, icon_light, pal, 16)
        out[f'item/{stage}_broken'] = doc(*broken(icon, icon_light), pal, 16)
        out[f'entity/equipment/wings/{stage}'] = doc(s64, l64, pal, 64)
    return out


def scaled(doc_: dict, k: int) -> bytes:
    rgba = layers.compose(doc_)
    w, h = doc_['width'], doc_['height']
    out = bytearray()
    for y in range(h * k):
        for x in range(w * k):
            i = ((y // k) * w + x // k) * 4
            out += rgba[i:i + 4]
    return layers.png_write(w * k, h * k, bytes(out))


def main(argv: list[str]) -> int:
    if '--preview' in argv:
        out = Path(argv[argv.index('--preview') + 1])
        out.mkdir(parents=True, exist_ok=True)
        for look in PAL:
            for name, d in textures(look).items():
                base = name.replace('/', '_')
                (out / f'{look}_{base}.png').write_bytes(scaled(d, 16 if d['width'] == 16 else 8))
                (out / f'{look}_{base}_1x.png').write_bytes(scaled(d, 1))
        print(f'previews in {out}')
        return 0
    bad = []
    for name, d in textures(BUILT_IN).items():
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
        print('elytra24 textures current')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
