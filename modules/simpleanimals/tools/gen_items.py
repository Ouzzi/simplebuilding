"""Simple Animals item textures: the five spawn eggs, raw and cooked crab, truffle and truffle soup (16 x 16, own pixel art, layers beside the PNG, A8).

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
import spawn_eggs_2026_10_23 as EG  # noqa: E402

OUT = MODULE / 'shared/resources/assets/simpleanimals/textures/item'


def H(s):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


# Spawn eggs (BOSS02, owner 03.11. "mehr vanilla like"): drawn like the Vanilla 26.3 eggs - the egg is the animal's head,
# ears/antlers/claws/tusks stick out, shaded by the shared egg engine (tools/textures/spawn_eggs_2026_10_23.py shade():
# outline from the material's own dark tones, light from the top left, calm 2x2 grain). Ramps from the entity textures.
DEER = [(0x3e, 0x22, 0x12), (0x63, 0x37, 0x1c), (0x8a, 0x50, 0x2a), (0xa8, 0x68, 0x38), (0xc4, 0x86, 0x50), (0xd8, 0xa4, 0x6c)]
CREAM = [(0x8a, 0x78, 0x5a), (0xb0, 0x9e, 0x7c), (0xd0, 0xc0, 0x9c), (0xe6, 0xda, 0xbc), (0xf4, 0xec, 0xd6)]
ANTLER = [(0x6a, 0x58, 0x40), (0x96, 0x84, 0x66), (0xc0, 0xb0, 0x8e), (0xde, 0xd2, 0xb4)]
BOAR = [(0x24, 0x18, 0x10), (0x3c, 0x2a, 0x1e), (0x56, 0x3e, 0x2c), (0x70, 0x54, 0x3c), (0x8a, 0x6c, 0x50), (0xa2, 0x84, 0x66)]
BRISTLE = [(0x14, 0x0e, 0x0a), (0x24, 0x1a, 0x12), (0x36, 0x28, 0x1c), (0x4a, 0x38, 0x28)]
SNOUT = [(0x6a, 0x44, 0x3a), (0x8e, 0x60, 0x52), (0xb0, 0x7e, 0x6c), (0xc8, 0x98, 0x84)]
SPINES = [(0x2a, 0x1e, 0x14), (0x44, 0x34, 0x24), (0x62, 0x4c, 0x34), (0x80, 0x68, 0x4a), (0x9e, 0x86, 0x64)]
SHELL = [(0x5a, 0x16, 0x0c), (0x86, 0x24, 0x12), (0xb0, 0x3a, 0x1e), (0xd0, 0x56, 0x30), (0xe8, 0x7a, 0x50), (0xf4, 0x9c, 0x74)]
BELLY = [(0x9a, 0x6e, 0x58), (0xc4, 0x9a, 0x80), (0xe2, 0xc0, 0xa4), (0xf2, 0xdc, 0xc6)]
ZWHITE = [(0x8c, 0x88, 0x80), (0xb4, 0xb0, 0xa6), (0xd6, 0xd2, 0xc6), (0xec, 0xe8, 0xde), (0xf8, 0xf6, 0xf0)]
ZBLACK = [(0x0e, 0x0c, 0x0a), (0x1c, 0x1a, 0x16), (0x2c, 0x28, 0x24), (0x3c, 0x38, 0x32)]
MUZZLE = [(0x1a, 0x18, 0x16), (0x30, 0x2c, 0x28), (0x46, 0x42, 0x3c), (0x5a, 0x54, 0x4c)]
EYE = (0x12, 0x0c, 0x08)
SHINE = (0xf4, 0xec, 0xd6)

# name -> (art, materials letter -> (ramp, base, grain), fixed letter -> RGB, main letter, small egg)
EGGS = {
    # red deer: antlers and ears stick out, cream muzzle with a dark nose
    'deer_spawn_egg': (["..a.a......a.a..",
                        "...aa.dddd.aa...",
                        "....adddddda....",
                        "....dddddddd....",
                        "...dddddddddd...",
                        "eedddddddddddee.",
                        "..ddEdddddEddd..",
                        "..dddddddddddd..",
                        "..dddccccccddd..",
                        "..ddccccccccdd..",
                        "..ddcccNNcccdd..",
                        "...dcccNNcccd...",
                        "....cccccccc....",
                        ".....cccccc.....",
                        "................",
                        "................"],
                       {'d': (DEER, 3, 0.7), 'e': (DEER, 2, 0.4), 'c': (CREAM, 2, 0.5), 'a': (ANTLER, 2, 0.3)},
                       {'E': EYE, 'N': BRISTLE[1]}, 'd', False),
    # wild boar: bristle crest, ears, small eyes, snout plate, tusks curling up at both sides
    'boar_spawn_egg': (["................",
                        "......mmmm......",
                        ".....bmmmmb.....",
                        "....bbbmmbbb....",
                        "..eebbbmmbbbee..",
                        "..ebbbbbbbbbbe..",
                        "..bbbbbbbbbbbb..",
                        "..bbEbbbbbbEbb..",
                        "..bbbbbbbbbbbb..",
                        ".TbbbssssssbbbT.",
                        ".TbbssssssssbbT.",
                        "..TbsNssssNsbT..",
                        "...bssssssssb...",
                        "....bbbbbbbb....",
                        ".....bbbbbb.....",
                        "................"],
                       {'b': (BOAR, 3, 0.8), 'm': (BRISTLE, 2, 0.5), 'e': (BOAR, 2, 0.4), 's': (SNOUT, 2, 0.4)},
                       {'E': EYE, 'N': BRISTLE[0], 'T': (0xe6, 0xdc, 0xc0)}, 'b', False),
    # hedgehog: small egg (like Vanilla's small mobs), spines stick out on top and frame the cream face
    'hedgehog_spawn_egg': (["................",
                            "................",
                            "....p..pp..p....",
                            "...p..pppp..p...",
                            "....pppppppp....",
                            "..pppppppppppp..",
                            "...pppppppppp...",
                            "..pppccccccppp..",
                            "..ppcEccccEcpp..",
                            "...pccccccccp...",
                            "...pcccNNcccp...",
                            "...ppccccccpp...",
                            "....pccccccp....",
                            "....ppccccpp....",
                            ".....pppppp.....",
                            "................"],
                           {'p': (SPINES, 3, 0.9), 'c': (CREAM, 2, 0.4)}, {'E': EYE, 'N': EYE}, 'p', True),
    # crab: eyes on stalks, claws stick out at the sides, legs below, pale belly
    'crab_spawn_egg': (["....E......E....",
                        "....r.rrrr.r....",
                        ".....rrrrrr.....",
                        "....rrrrrrrr....",
                        "CC.rrrrrrrrrr.CC",
                        "CCCrrrrrrrrrrCCC",
                        ".CCrrrrrrrrrrCC.",
                        "..rrrrrrrrrrrr..",
                        "..rrrrrrrrrrrr..",
                        "..rrrrrrrrrrrr..",
                        ".rrwwwwwwwwwwrr.",
                        "r.rwwwwwwwwwwr.r",
                        "...wwwwwwwwww...",
                        "....wwwwwwww....",
                        ".....wwwwww.....",
                        "................"],
                       {'r': (SHELL, 3, 0.7), 'C': (SHELL, 4, 0.4), 'w': (BELLY, 2, 0.4)}, {'E': EYE}, 'r', False),
    # zebra: horse head with ears and a dark mane, stripes across, dark muzzle
    'zebra_spawn_egg': (["...kk......kk...",
                         "...wk.kkkk.kw...",
                         "....wwwkkwww....",
                         "....kkwwwwkk....",
                         "...wwwkkkkwww...",
                         "...kkwwwwwwkk...",
                         "..wwwkwwwwkwww..",
                         "..wEwwkwwkwwEw..",
                         "..kkwwwwwwwwkk..",
                         "..wwkkwwwwkkww..",
                         "..wwwwmmmmwwww..",
                         "..kkwmmmmmmwkk..",
                         "...wmmNmmNmmw...",
                         "....mmmmmmmm....",
                         ".....mmmmmm.....",
                         "................"],
                        {'w': (ZWHITE, 3, 0.4), 'k': (ZBLACK, 2, 0.3), 'm': (MUZZLE, 2, 0.4)}, {'E': EYE, 'N': ZBLACK[0]},
                        'w', False),
}


def egg(name):
    """(image, layer stack) of one spawn egg through the shared engine."""
    art, mats, fixed, main, small = EGGS[name]
    return EG.grid_egg(art, mats, fixed, main, shape=EG.SMALL if small else EG.EGG, seed=sum(map(ord, name)) % 97)


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


# Truffle (IG31-20, variant A of the preview MOBS32-TRUFFLE-A): a dark, knobbly lump with pale marbling; o outline, b earth brown, d shade, h light, m marbling
TRUFFLE = ["................",
           "................",
           "......ooo.......",
           "....oobbbboo....",
           "...obbhhbbbbo...",
           "..obbhmbbbbdbo..",
           "..obbbbbmbbbdo..",
           "..obhbbmmbbbdo..",
           "..obbbbbbbhbdo..",
           "..obbmbbbbbddo..",
           "...obbbbmbbdo...",
           "...oddbbbbddo...",
           "....ooddddoo....",
           ".....oooooo.....",
           "................",
           "................"]
TRUFFLE_COLORS = {'o': '#1c120c', 'b': '#4a3524', 'd': '#2e2016', 'h': '#6b5038', 'm': '#a8987c'}

# Truffle soup: a wooden bowl (w, shade W) full of dark broth (s) with a slice of truffle (t) and steam (v)
SOUP = ["................",
        "......v..v......",
        ".....v..v.......",
        "................",
        "...oooooooooo...",
        "..osssssssssso..",
        "..ossttsssmsso..",
        "..osstttsssmso..",
        "..ossssssssso...",
        "..owwwwwwwwwwo..",
        "..owwwwwwwwwWo..",
        "...owwwwwwwWo...",
        "....owwwwWWo....",
        ".....oooooo.....",
        "................",
        "................"]
SOUP_COLORS = {'o': '#2a1a10', 's': '#3a2a1c', 't': '#6b5038', 'm': '#a8987c', 'w': '#9a6a3a', 'W': '#6e4824', 'v': '#e4dccb'}


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
    for name in EGGS:
        img, stack = egg(name)
        acc = bytearray(16 * 16 * 4)
        for layer, data in stack:  # disjoint opaque layers -> cumulative drawing steps
            for i in range(0, len(data), 4):
                if data[i + 3]:
                    acc[i:i + 4] = data[i:i + 4]
            L.snap(img, layer, bytes(acc))
        files[OUT / f'{name}.png'] = img
    for name, palette in LEGS:
        colors = {k: H(v) for k, v in palette.items()}
        files[OUT / f'{name}.png'] = image(LEG, colors, [('Umriss', 'o'), ('Gelenke', 'p'), ('Fleisch', 'w')], 'Schale')
    files[OUT / 'truffle.png'] = image(TRUFFLE, {k: H(v) for k, v in TRUFFLE_COLORS.items()},
                                       [('Umriss', 'o'), ('Schatten', 'd'), ('Licht', 'h'), ('Marmorierung', 'm')])
    files[OUT / 'truffle_soup.png'] = image(SOUP, {k: H(v) for k, v in SOUP_COLORS.items()},
                                            [('Umriss', 'o'), ('Brühe', 's'), ('Trüffel', 't'), ('Fett', 'm'), ('Dampf', 'v'), ('Schüsselschatten', 'W')], 'Schüssel')
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
