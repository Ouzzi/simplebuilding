"""Wither loot textures (owner 21.10., queue b-wither21): Nether Heart, Nether Gem (items and placed blocks) and the
Strong Upgrade Shard. Own pixels, close to Vanilla (dark tone of the main colour as outline, light from the top left),
each as a layer source (docs/ai/PLAN-B-PIXELEDITOR-2026-10-21.md): layer 1 "Grundform", layer 2 "Licht".
Two variants per texture; variant A is built in. Pure Python (tools/textures/layers.py), no Pillow.
Owner 21.10. (queue b-rarity21): Nether Heart and Red Nether Gem (item + block) are animated: now and then their golden
veins/glints run over into blue from the top down and back (5-frame strip 16x80 + .png.mcmeta, long golden rest).

    python tools/textures/wither21_textures.py            # write variant A (PNG + .layers.json) into the modules
    python tools/textures/wither21_textures.py --check    # fail when a committed PNG/layer source differs
    python tools/textures/wither21_textures.py --preview DIR   # 16x PNGs of both variants (for the choice page)
    python tools/textures/wither21_textures.py --strips DIR    # 1x animation strips + mcmeta (preview page)
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
MOBS = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/textures'
BEACONS = ROOT / 'modules/simplebeacons/shared/resources/assets/simplebeacons/textures'

# ---------------------------------------------------------------- Nether Heart (item)
HEART_SHAPE = [
    '................',
    '......OOO.OO....',
    '.....OVvO.OvO...',
    '.....OVvOOOvO...',
    '...OOOVvMMMvOO..',
    '..ODDMMMMMMMMDO.',
    '..ODMMMMMMMMMDO.',
    '..ODMMMMMMMMMDO.',
    '..ODMMMMMMMMDDO.',
    '..OODMMMMMMMDO..',
    '...ODMMMMMMDDO..',
    '...OODMMMMDDO...',
    '....OODMMDDO....',
    '.....OODDDO.....',
    '.......OOO......',
    '................',
]
HEART_LIGHT_A = [
    '................',
    '................',
    '................',
    '................',
    '.........L......',
    '....HL..E...L...',
    '...HL...E.......',
    '...L....C.......',
    '...L...E.E......',
    '.......C..E.....',
    '......E....C....',
    '......E.........',
    '................',
    '................',
    '................',
    '................',
]
HEART_LIGHT_B = [
    '................',
    '................',
    '................',
    '................',
    '.........L......',
    '....HL.L....L...',
    '...HL....T..L...',
    '...L.L..T.......',
    '...L...T..L.....',
    '.....L.T..T.....',
    '......T..L.T....',
    '.....L.T........',
    '.......L........',
    '................',
    '................',
    '................',
]
HEART_A = {'O': '#3b0c10', 'D': '#6b1820', 'M': '#962a2a', 'L': '#c2473a', 'H': '#e57a5c',
           'V': '#7a2a40', 'v': '#4a1428', 'E': '#ff8a2a', 'C': '#ffd76a'}
HEART_B = {'O': '#2e0a0e', 'D': '#5a1214', 'M': '#7d1b1b', 'L': '#a8312a', 'H': '#c9554a',
           'V': '#1f8a8a', 'v': '#0e5257', 'T': '#3ec8b8'}

# ---------------------------------------------------------------- Nether Gem (item)
GEM_SHAPE = [
    '................',
    '................',
    '................',
    '.....OOOOOO.....',
    '....OLHHLLMO....',
    '...OLHLLMMMDO...',
    '..OLLLMMMMMDDO..',
    '..OMMMMMMMDDDO..',
    '...OMMMMMDDDO...',
    '....OMMMDDDO....',
    '.....OMDDDO.....',
    '......ODDO......',
    '.......OO.......',
    '................',
    '................',
    '................',
]
GEM_LIGHT = [
    '................',
    '................',
    '................',
    '................',
    '......S.........',
    '.....S..........',
    '........F.......',
    '.......FC.......',
    '........F.......',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
]
GEM_A = {'O': '#4a0b0b', 'D': '#8a1a12', 'M': '#c2361c', 'L': '#ec6a2c', 'H': '#ffb055',
         'S': '#fff0c0', 'F': '#ffd36a', 'C': '#fff6d0'}
GEM_B = {'O': '#1e0838', 'D': '#3f0f6e', 'M': '#6a1fb0', 'L': '#9a4ae0', 'H': '#c890ff',
         'S': '#f2e0ff', 'F': '#e070ff', 'C': '#fbe6ff'}

# ---------------------------------------------------------------- Strong Upgrade Shard (item)
# palette of the Strong Upgrade template (obsidian violet, gold cross, cyan sparkle)
SHARD_SHAPE_A = [
    '................',
    '................',
    '.........OO.....',
    '........OlmO....',
    '.......OlmbbO...',
    '......OlmbbbbO..',
    '.....OlbbnnbdO..',
    '....OlbbbgybdO..',
    '...OnnngyywbdO..',
    '...OmbbbgybbdO..',
    '..OlbbbbgybddO..',
    '..OmbbbbnnbdO...',
    '...OddbbbbdO....',
    '....OOddddO.....',
    '......OOOO......',
    '................',
]
SHARD_LIGHT_A = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '..........k.....',
    '................',
    '................',
    '................',
    '................',
    '....c...........',
    '................',
    '................',
    '................',
    '................',
    '................',
]
SHARD_PAL = {'O': '#0e0916', 'd': '#1e132e', 'b': '#2e1e46', 'm': '#422d62', 'l': '#5e4484',
             'n': '#845c1c', 'g': '#cea23a', 'y': '#f6dc7c', 'w': '#fffad6', 'c': '#46c8d2', 'k': '#befcfa'}


def mirror(rows):
    return [r[::-1] for r in rows]


# variant B: a slimmer splinter, the cross piece runs along its edge, cyan sparkles inside
SHARD_SHAPE_B = [
    '................',
    '................',
    '.......O........',
    '......OlO.......',
    '......OlmO......',
    '.....OlmbO......',
    '.....OlbbbO.....',
    '....OgybbbO.....',
    '....OgybbbdO....',
    '...OnnggbbdO....',
    '...OmbbgybddO...',
    '...ObbbgybddO...',
    '....OdbnnbdO....',
    '.....OOdddO.....',
    '.......OOO......',
    '................',
]
SHARD_LIGHT_B = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '........k.......',
    '................',
    '.........c......',
    '................',
    '................',
    '.....c..........',
    '................',
    '................',
    '................',
    '................',
]

# WI21 (owner 21.10.): "strong upgrade b aber kleiner oder a und kleiner also neu" -> two new, clearly smaller
# shards (8x10 px like a Vanilla amethyst/echo shard instead of 12x13): SA from variant A (wedge, gold cross),
# SB from variant B (slim splinter, gold along the edge). Built in: SB (named first); choice code SP21 A|B.
SHARD_SMALL_SHAPE_A = [
    '................',
    '................',
    '................',
    '................',
    '..........O.....',
    '.........OlO....',
    '........OlmbO...',
    '.......OlbgbO...',
    '......OnngynO...',
    '......OmbgbdO...',
    '.....OlbbbdO....',
    '.....OddbddO....',
    '......OOddO.....',
    '........OO......',
    '................',
    '................',
]
SHARD_SMALL_LIGHT_A = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '..........k.....',
    '................',
    '................',
    '................',
    '......c.........',
    '................',
    '................',
    '................',
    '................',
    '................',
]
SHARD_SMALL_SHAPE_B = [
    '................',
    '................',
    '................',
    '................',
    '.......O........',
    '......OlO.......',
    '......OlmO......',
    '.....OgybO......',
    '.....OgybdO.....',
    '....OnngbdO.....',
    '....OmbgybO.....',
    '.....ObnndO.....',
    '......OddO......',
    '.......OO.......',
    '................',
    '................',
]
SHARD_SMALL_LIGHT_B = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '.......k........',
    '................',
    '................',
    '................',
    '................',
    '........c.......',
    '................',
    '................',
    '................',
    '................',
]

# ---------------------------------------------------------------- placed Nether Heart (block, 8x8 cube: uv 4..12)
def heart_block(pal_light):
    """Flesh face: dark rim at the bottom/right, lighter at the top/left (bevel like Vanilla blocks); outside the face
    (only seen as break particles) the same flesh with scattered darker blotches."""
    base = []
    for y in range(16):
        row = ''
        for x in range(16):
            inner = 4 <= x <= 11 and 4 <= y <= 11
            if inner and (x == 11 or y == 11):
                row += 'D'
            elif inner and (x == 4 or y == 4):
                row += 'L'
            elif inner:
                row += 'M'
            else:
                row += 'D' if (x * 5 + y * 11 + x * y) % 7 == 0 else 'M'
        base.append(row)
    return base, pal_light


HEART_BLOCK_LIGHT_A = [
    '................',
    '................',
    '................',
    '................',
    '....H...........',
    '.....H.....E....',
    '..........C.....',
    '.........E......',
    '......E.E.......',
    '.......C........',
    '........E.......',
    '................',
    '................',
    '................',
    '................',
    '................',
]
HEART_BLOCK_LIGHT_B = [
    '................',
    '................',
    '................',
    '................',
    '....H...........',
    '.....H..L..T....',
    '..........T.....',
    '......L..T..L...',
    '........T.......',
    '......TT..L.....',
    '.....T...L......',
    '................',
    '................',
    '................',
    '................',
    '................',
]

# ---------------------------------------------------------------- placed Nether Gem (block, cross model like a cluster)
GEM_BLOCK_SHAPE = [
    '................',
    '................',
    '................',
    '.......OO.......',
    '......OHLO......',
    '......OLMO......',
    '......OLMDO.....',
    '......OLMDO.....',
    '..OO..OLMDO.....',
    '.OHLO.OLMDO.....',
    '.OLMO.OLMDO.OO..',
    '.OLMO.OLMDOOHLO.',
    '.OLMDOOLMDOOLMO.',
    '.OLMDOOLMDOOLDO.',
    '.OMMDOOMMDOOMDO.',
    '.ODDDOODDDOODDO.',
]
GEM_BLOCK_LIGHT = [
    '................',
    '................',
    '................',
    '................',
    '.......S........',
    '................',
    '........F.......',
    '........C.......',
    '........F.......',
    '..S.............',
    '...F............',
    '.............S..',
    '...F............',
    '........F.......',
    '................',
    '................',
]


def doc(shape, light, pal) -> dict:
    """A two-layer source: shape (Grundform) and light/glow (Licht); unknown letters fail loudly. Height = rows (16 or a
    strip of 16-high frames)."""
    chars = sorted({c for row in shape + light for c in row if c != '.'})
    palette = [pal[c] + 'ff' for c in chars]
    index = {c: i for i, c in enumerate(chars)}
    d = layers.digits_for(len(palette))

    h = len(shape)

    def rows(grid):
        assert len(grid) == h and h % 16 == 0 and all(len(r) == 16 for r in grid), grid
        return [''.join('.' * d if c == '.' else format(index[c], f'0{d}x') for c in r) for r in grid]

    for y, row in enumerate(light):
        for x, c in enumerate(row):
            assert c == '.' or shape[y][x] != '.', f'light pixel outside the shape at {x},{y}'
    return {'format': layers.FORMAT, 'version': 1, 'width': 16, 'height': h, 'digits': d, 'palette': palette,
            'layers': [{'name': 'Grundform', 'visible': True, 'opacity': 100, 'blend': 'normal', 'rows': rows(shape)},
                       {'name': 'Licht', 'visible': True, 'opacity': 100, 'blend': 'normal', 'rows': rows(light)}]}


# ---------------------------------------------------------------- animation: golden veins -> blue and back
FRAMES = 5
BLUE = {'e': '#3a9cff', 'c': '#b4ecff', 'f': '#5ab4ff', 'g': '#e4f8ff'}
HEART_TO_BLUE = {'E': 'e', 'C': 'c'}
GEM_TO_BLUE = {'F': 'f', 'C': 'g'}


def strip(shape, light, to_blue):
    """5 frames stacked: frame 0 golden, then the swapped letters turn blue from the top row down, frame 4 all blue."""
    ys = [y for y, row in enumerate(light) for c in row if c in to_blue]
    lo, hi = min(ys), max(ys) + 1
    out_shape, out_light = [], []
    for k in range(FRAMES):
        limit = lo - (-(hi - lo) * k // (FRAMES - 1))  # ceil: frame 1 already changes something
        out_shape += shape
        out_light += [''.join(to_blue.get(c, c) if y < limit else c for c in row) for y, row in enumerate(light)]
    return out_shape, out_light


def mcmeta(rest: int, blue: int = 40, step: int = 2) -> str:
    """Long golden rest, short run over to blue, a little blue, the same run back (Vanilla animation frames)."""
    order = [(0, rest)] + [(i, step) for i in range(1, FRAMES - 1)] + [(FRAMES - 1, blue)] \
        + [(i, step) for i in range(FRAMES - 2, 0, -1)]
    frames = ',\n      '.join(f'{{"index": {i}, "time": {t}}}' for i, t in order)
    return '{\n  "animation": {\n    "frames": [\n      ' + frames + '\n    ]\n  }\n}\n'


def animated() -> dict[str, tuple[dict, str]]:
    """name -> (strip layer source, mcmeta text); heart and gem rest differently long so they never blink together."""
    hb = heart_block(HEART_BLOCK_LIGHT_A)
    hp, gp = {**HEART_A, **BLUE}, {**GEM_A, **BLUE}
    return {
        'nether_heart': (doc(*strip(HEART_SHAPE, HEART_LIGHT_A, HEART_TO_BLUE), hp), mcmeta(200)),
        'nether_gem': (doc(*strip(GEM_SHAPE, GEM_LIGHT, GEM_TO_BLUE), gp), mcmeta(260)),
        'nether_heart_block': (doc(*strip(hb[0], hb[1], HEART_TO_BLUE), hp), mcmeta(200)),
        'nether_gem_block': (doc(*strip(GEM_BLOCK_SHAPE, GEM_BLOCK_LIGHT, GEM_TO_BLUE), gp), mcmeta(260)),
    }


def variants() -> dict[str, tuple[Path, dict, dict]]:
    """name -> (target PNG, variant A, variant B)."""
    hb_a = heart_block(HEART_BLOCK_LIGHT_A)
    hb_b = heart_block(HEART_BLOCK_LIGHT_B)
    return {
        'nether_heart': (MOBS / 'item/nether_heart.png', doc(HEART_SHAPE, HEART_LIGHT_A, HEART_A), doc(HEART_SHAPE, HEART_LIGHT_B, HEART_B)),
        'nether_gem': (MOBS / 'item/nether_gem.png', doc(GEM_SHAPE, GEM_LIGHT, GEM_A), doc(GEM_SHAPE, GEM_LIGHT, GEM_B)),
        # WI21: built in the small shard from B (SB), alternative the small one from A (SA)
        'strong_upgrade_shard': (BEACONS / 'item/strong_upgrade_shard.png',
                                 doc(SHARD_SMALL_SHAPE_B, SHARD_SMALL_LIGHT_B, SHARD_PAL),
                                 doc(SHARD_SMALL_SHAPE_A, SHARD_SMALL_LIGHT_A, SHARD_PAL)),
        'nether_heart_block': (MOBS / 'block/nether_heart.png', doc(hb_a[0], hb_a[1], HEART_A), doc(hb_b[0], hb_b[1], HEART_B)),
        'nether_gem_block': (MOBS / 'block/nether_gem.png', doc(GEM_BLOCK_SHAPE, GEM_BLOCK_LIGHT, GEM_A),
                             doc(GEM_BLOCK_SHAPE, GEM_BLOCK_LIGHT, GEM_B)),
    }


def scaled(doc_: dict, k: int = 16) -> bytes:
    """PNG of the composed texture, k-fold, nearest neighbour."""
    rgba = layers.compose(doc_)
    w = doc_['width']
    out = bytearray()
    for y in range(doc_['height'] * k):
        for x in range(w * k):
            i = ((y // k) * w + x // k) * 4
            out += rgba[i:i + 4]
    return layers.png_write(w * k, doc_['height'] * k, bytes(out))


def main(argv: list[str]) -> int:
    if '--preview' in argv:
        out = Path(argv[argv.index('--preview') + 1])
        out.mkdir(parents=True, exist_ok=True)
        for name, (_, a, b) in variants().items():
            (out / f'{name}_A.png').write_bytes(scaled(a))
            (out / f'{name}_B.png').write_bytes(scaled(b))
            (out / f'{name}_A_1x.png').write_bytes(scaled(a, 1))
            (out / f'{name}_B_1x.png').write_bytes(scaled(b, 1))
        print(f'previews in {out}')
        return 0
    if '--strips' in argv:
        out = Path(argv[argv.index('--strips') + 1])
        out.mkdir(parents=True, exist_ok=True)
        for name, (a, meta) in animated().items():
            (out / f'{name}_strip.png').write_bytes(scaled(a, 1))
            (out / f'{name}.mcmeta').write_text(meta, encoding='utf-8')
        print(f'strips in {out}')
        return 0
    bad = []
    anim = animated()
    for name, (png, a, _) in variants().items():
        meta = None
        if name in anim:
            a, meta = anim[name]
        src = layers.layers_path(png)
        mpath = png.with_name(png.name + '.mcmeta')
        text = layers.dumps(a)
        image = layers.png_write(16, a['height'], bytes(layers.compose(a)))
        if '--check' in argv:
            if not src.is_file() or src.read_text(encoding='utf-8') != text or not png.is_file() \
                    or layers.png_read(png.read_bytes())[2] != layers.compose(a) \
                    or (meta is not None and (not mpath.is_file() or mpath.read_text(encoding='utf-8') != meta)):
                bad.append(str(png))
            continue
        png.parent.mkdir(parents=True, exist_ok=True)
        src.write_text(text, encoding='utf-8', newline='\n')
        png.write_bytes(image)
        if meta is not None:
            mpath.write_text(meta, encoding='utf-8', newline='\n')
        print(f'wrote {png.relative_to(ROOT)} (+ layers{", mcmeta" if meta else ""})')
    if bad:
        print('stale:', *bad, sep='\n  ')
        return 1
    if '--check' in argv:
        print('wither21 textures current')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
