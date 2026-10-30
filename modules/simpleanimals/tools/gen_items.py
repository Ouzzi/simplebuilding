"""Simple Animals item textures: the five spawn eggs, raw and cooked crab (16 x 16, own pixel art, layers beside the PNG, A8).

    python3 modules/simpleanimals/tools/gen_items.py           write
    python3 modules/simpleanimals/tools/gen_items.py --check   fail when a texture is stale
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
sys.path.insert(0, str(ROOT / 'tools/textures'))
import layers as L  # noqa: E402

OUT = MODULE / 'shared/resources/assets/simpleanimals/textures/item'


def H(s):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


EGG = ["................",
       "......oooo......",
       ".....ohbbbo.....",
       "....ohbbbbbo....",
       "...ohbbsbbbbo...",
       "...obbbbbsbbo...",
       "..obbbbbbbbbbo..",
       "..obbsbbbbbsbo..",
       "..obbbbbbbbbbo..",
       "..obbbbsbbbbbo..",
       "..obbbbbbbsbbo..",
       "...obbbbbbbbo...",
       "...obbbsbbbbo...",
       "....obbbbbbo....",
       ".....oooooo.....",
       "................"]

# (name, outline, base, highlight, spots): the egg takes the colours of the animal
EGGS = [('deer_spawn_egg', '#4a2a14', '#9a5a30', '#c88a58', '#e8d8b4'),
        ('boar_spawn_egg', '#1c1410', '#4a3a30', '#786858', '#d8c8a8'),
        ('hedgehog_spawn_egg', '#2e2418', '#6a5238', '#a8906a', '#e8d8c0'),
        ('crab_spawn_egg', '#5a1c10', '#b8502e', '#e07a52', '#f0d0b0'),
        ('zebra_spawn_egg', '#1c1814', '#e0d8c6', '#f4efe2', '#34302a')]

# a crab leg: shell on the outside (P), meat (w), a darker rim at the joints (p)
LEG = ["................",
       "................",
       ".........ooo....",
       "........oPPPo...",
       ".......oPPPPPo..",
       "......oPPpPPPo..",
       ".....oPPpwwPPo..",
       "....oPPpwwwpPo..",
       "...oPPpwwwwpo...",
       "..oPPpwwwwpo....",
       ".oPPpwwwwpo.....",
       ".oPpwwwppo......",
       "..oPpwppo.......",
       "...oPPpo........",
       "....ooo.........",
       "................"]
LEGS = [('raw_crab', {'o': '#3e1a14', 'P': '#b3523a', 'p': '#8a3a28', 'w': '#f2dcd0'}),
        ('cooked_crab', {'o': '#4a1a0c', 'P': '#e0652a', 'p': '#b04418', 'w': '#fff0d8'})]


def image(rows, colors, groups, base='Grundfarbe'):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colors:
                px[x, y] = colors[ch] + (255,)
    return L.snap_grid(img, rows, {k: v + (255,) for k, v in colors.items()}, groups, base)


def outputs():
    files = {}
    for name, outline, base, high, spot in EGGS:
        colors = {'o': H(outline), 'b': H(base), 'h': H(high), 's': H(spot)}
        files[OUT / f'{name}.png'] = image(EGG, colors, [('Umriss', 'o'), ('Licht', 'h'), ('Flecken', 's')])
    for name, palette in LEGS:
        colors = {k: H(v) for k, v in palette.items()}
        files[OUT / f'{name}.png'] = image(LEG, colors, [('Umriss', 'o'), ('Gelenke', 'p'), ('Fleisch', 'w')], 'Schale')
    return files


def main(argv):
    check = '--check' in argv
    stale = []
    for path, img in outputs().items():
        same = path.exists() and Image.open(path).convert('RGBA').tobytes() == img.tobytes()
        if not same:
            stale.append(path)
            if not check:
                path.parent.mkdir(parents=True, exist_ok=True)
                L.save(img, path)
    if check and stale:
        print('stale: ' + ', '.join(p.name for p in stale))
        return 1
    print('simpleanimals items: %d textures %s' % (len(outputs()), 'current' if not stale else 'written'))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
