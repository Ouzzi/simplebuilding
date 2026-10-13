"""Blighted Wither + Biome Brush textures (Simple Mobs), own pixel art plus one Vanilla-derived skin.

    python tools/textures/wither_textures.py          # write PNGs + previews (~/previews/wither)
    python tools/textures/wither_textures.py --check  # fail if the committed PNGs differ

Wither Heart: a charred anatomical heart (not a card heart) with two vessel stubs, its cracks glowing in the pale
soul-white of the Wither's eyes. Biome Brush: a netherite-dark handle with a heart-charcoal ferrule, the bristles fan
out in bands of biome colors (grass, leaf, sand, snow). Both drawn here.
The Blighted Wither skin is the Vanilla Wither skin remapped to an ash ramp with soul-cyan eyes: Vanilla-derived,
so it is only built (tools/textures/vanilla_derived.py, MODULE_OUTPUTS['simplemobs']) and never committed.
"""
import os
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/textures'
PREVIEW = Path(os.environ.get('SB_PREVIEW_DIR', os.path.expanduser('~/previews')) + '/wither')

PAL = {
    '.': None,
    'K': (0x14, 0x0f, 0x12),   # outline
    'd': (0x2b, 0x22, 0x27),   # charcoal dark
    'm': (0x3e, 0x33, 0x39),   # charcoal mid
    'h': (0x5c, 0x50, 0x56),   # charcoal light
    'w': (0x7a, 0x6d, 0x72),   # ash highlight
    'c': (0x6f, 0xc9, 0xd6),   # soul crack
    'C': (0xd8, 0xfb, 0xff),   # soul glow core
    'v': (0x63, 0x55, 0x5c),   # vessel
    'n': (0x31, 0x2b, 0x2e),   # netherite dark
    'N': (0x4a, 0x42, 0x45),   # netherite mid
    'H': (0x6c, 0x62, 0x64),   # netherite highlight
    'G': (0x3f, 0x8f, 0x2b),   # grass dark
    'g': (0x6c, 0xbf, 0x3c),   # grass
    'l': (0x9c, 0xd9, 0x5a),   # leaf light
    's': (0xe0, 0xd2, 0x92),   # sand
    'S': (0xbf, 0xa8, 0x66),   # sand dark
    'b': (0xc8, 0xe8, 0xf4),   # snow
    'B': (0x8c, 0xb8, 0xd8),   # snow shade
}

HEART = [
    '................',
    '.....KKK.KK.....',
    '.....KwvKKwK....',
    '....KKvvKKvKK...',
    '...KhwmvmmvmhK..',
    '..KhwmmmcmmmmdK.',
    '..KwmmmmCmmmmdK.',
    '..KmmmmcmcmmmdK.',
    '..KmmmcmmmCmddK.',
    '..KdmmCmmmmcddK.',
    '...KdmcmmmmmdK..',
    '...KddmcmmmddK..',
    '....KddmmmddK...',
    '.....KKdddKK....',
    '.......KKK......',
    '................',
]


def brush_rows():
    """Biome Brush, computed along the diagonal: a = x - y runs from the handle end (-11) to the bristle tips (+10),
    b = x + y - 15 is the side offset. Handle (2 wide), heart ferrule with a soul seam, bristles fanning out in bands of
    biome colors; then a dark outline around the shape."""
    grid = [['.'] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            a, b = x - y, x + y - 15
            if -11 <= a <= -2 and b in (0, 1):
                grid[y][x] = 'H' if b == 0 else 'n'
                if a in (-3, -2):
                    grid[y][x] = 'N'
            elif -1 <= a <= 1 and -1 <= b <= 2:
                grid[y][x] = 'C' if (a == 0 and b in (0, 1)) else ('c' if a == 0 else ('h' if b <= 0 else 'm'))
            elif 2 <= a <= 10 and abs(b - 0.5) <= (2.5 if a < 5 else 3.5) and not (a >= 9 and abs(b - 0.5) > 2.5):
                band = {-3: 'b', -2: 'b', -1: 'B', 0: 's', 1: 'g', 2: 'l', 3: 'G', 4: 'G'}[b]
                if band == 's' and a <= 4:
                    band = 'S'
                if band == 'l' and a >= 8:
                    band = 'l'
                if a >= 9 and band == 'G':
                    band = 'g'
                grid[y][x] = band
    out = [row[:] for row in grid]
    for y in range(16):
        for x in range(16):
            if grid[y][x] != '.':
                continue
            if any(0 <= x + dx < 16 and 0 <= y + dy < 16 and grid[y + dy][x + dx] != '.' for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out[y][x] = 'K'
    for y in range(16):
        if out[y][14] not in '.K':
            out[y][14] = 'K'
    for y in (0, 15):
        out[y] = ['.'] * 16
    for row in out:
        row[0] = '.'
        row[15] = '.'
    return [''.join(r) for r in out]


BRUSH = brush_rows()


def draw(rows):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            c = PAL[ch]
            if c:
                img.putpixel((x, y), c + (255,))
    return img


def outputs():
    return {RES / 'item/wither_heart.png': draw(HEART), RES / 'item/biome_brush.png': draw(BRUSH)}


def blighted(skin):
    """Vanilla Wither skin (near-black greys) -> pale ash: the greys follow an ash ramp from soot brown to bone white,
    the cyan-tinted eye pixels become soul light. Same shading order, so the Vanilla model reads unchanged."""
    ash = [(14, (0x2a, 0x22, 0x22)), (52, (0x8a, 0x7b, 0x72)), (83, (0xb0, 0xa3, 0x96)), (231, (0xf4, 0xf0, 0xe6))]
    soul = [(100, (0x4f, 0xc8, 0xd8)), (125, (0x8d, 0xf0, 0xfa)), (140, (0xd8, 0xff, 0xff))]

    def ramp(points, v):
        if v <= points[0][0]:
            return points[0][1]
        for (v0, c0), (v1, c1) in zip(points, points[1:]):
            if v <= v1:
                t = (v - v0) / (v1 - v0)
                return tuple(round(a + (b - a) * t) for a, b in zip(c0, c1))
        return points[-1][1]

    out = Image.new('RGBA', skin.size, (0, 0, 0, 0))
    for y in range(skin.height):
        for x in range(skin.width):
            r, g, b, a = skin.getpixel((x, y))
            if a == 0:
                continue
            c = ramp(soul, (g + b) // 2) if b > r + 15 else ramp(ash, (r + g + b) // 3)
            out.putpixel((x, y), c + (a,))
    return out


def abyssal(skin):
    """Abyssal Wither (owner N50, Enderite stage): the same shading order as the Blighted skin, but an End ramp from
    void black over chorus violet to end-stone pale, the eye pixels become ender magenta."""
    void = [(14, (0x14, 0x0c, 0x1e)), (52, (0x3e, 0x24, 0x58)), (83, (0x7a, 0x52, 0x9a)), (231, (0xe8, 0xe2, 0xbe))]
    eyes = [(100, (0xa0, 0x3c, 0xd8)), (125, (0xd8, 0x6c, 0xff)), (140, (0xf6, 0xc8, 0xff))]

    def ramp(points, v):
        if v <= points[0][0]:
            return points[0][1]
        for (v0, c0), (v1, c1) in zip(points, points[1:]):
            if v <= v1:
                t = (v - v0) / (v1 - v0)
                return tuple(round(a + (b - a) * t) for a, b in zip(c0, c1))
        return points[-1][1]

    out = Image.new('RGBA', skin.size, (0, 0, 0, 0))
    for y in range(skin.height):
        for x in range(skin.width):
            r, g, b, a = skin.getpixel((x, y))
            if a == 0:
                continue
            c = ramp(eyes, (g + b) // 2) if b > r + 15 else ramp(void, (r + g + b) // 3)
            out.putpixel((x, y), c + (a,))
    return out


def derived(jar):
    """Vanilla-derived output for tools/textures/vanilla_derived.py (module simplemobs): texture path -> image."""
    import io
    import zipfile
    with zipfile.ZipFile(jar) as z:
        skin = Image.open(io.BytesIO(z.read('assets/minecraft/textures/entity/wither/wither.png'))).convert('RGBA')
    return {'entity/blighted_wither/blighted_wither.png': blighted(skin),
            'entity/blighted_wither/abyssal_wither.png': abyssal(skin)}


def preview(items):
    PREVIEW.mkdir(parents=True, exist_ok=True)
    sheet = Image.new('RGBA', (16 * 16 * len(items) + 16 * (len(items) - 1), 256), (0x8b, 0x8b, 0x8b, 255))
    for i, im in enumerate(items.values()):
        big = im.resize((256, 256), Image.NEAREST)
        sheet.alpha_composite(big, (i * 272, 0))
    sheet.save(PREVIEW / 'items_16x.png')


def main():
    items = outputs()
    if '--check' in sys.argv:
        bad = [str(p) for p, im in items.items() if not p.is_file() or Image.open(p).convert('RGBA').tobytes() != im.tobytes()]
        if bad:
            print('stale:', *bad, sep='\n  '); sys.exit(1)
        print('wither textures current'); return
    for p, im in items.items():
        p.parent.mkdir(parents=True, exist_ok=True); im.save(p)
    preview(items)
    jar = os.environ.get('SB_CLIENT_JAR')
    if jar and Path(jar).is_file():
        for name, skin in derived(jar).items():
            stem = name.rsplit('/', 1)[1][:-4]
            skin.resize((skin.width * 8, skin.height * 8), Image.NEAREST).save(PREVIEW / f'{stem}_skin_8x.png')


if __name__ == '__main__':
    main()
