"""Usage: python3.12 tools/textures/wd25_ui_2026_10_25.py [--install | --check | --preview DIR]

WD25 (Besitzer 25.10., Plan docs/ai/PLAN-B-WD25UI-2026-10-25.md), eigene Pixel, keine Vanilla-Pixel:
- WD25-6 Verkaufsblock-Deckel (simpleshops sales_block_top): das alte Gitter wirkte wie eine offene Kiste.
  A Bretter wie die Seiten (1-px-Rahmen, Nieten), B wie A mit Eisenband und Muenzschlitz,
  C (Standard) der bisherige 2-px-Eisenrahmen mit geschlossenen Brettern (Familie Handelsterminal-Deckel).
  Ebenen: Rahmen (Eisen), Bretter, Nieten/Beschlag.
- WD25-12 Leer-Slot-Icon der Pfeilspitze am Befiederungstisch (arrow_tip): zeigte einen ganzen Pfeil.
  A (Standard) aufrechte Spitze mit Widerhaken und Schaftansatz, B aufrechte Feuersteinspitze mit Schaeftungskerben,
  C die alte schraege Spitze ohne langen Schaft. Ein Grau 85,85,85 wie Vanillas Slot-Silhouetten.
--install schreibt die Standards (PNG + .layers.json), --check prueft, ob die eingebauten Dateien dem Generator
entsprechen, --preview DIR schreibt alle Varianten (x1) fuer die Vorschauseite. Braucht Pillow.
"""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, HERE)
import layers  # noqa: E402

SHOPS = os.path.join(REPO, 'modules/simpleshops/shared/resources/assets/simpleshops/textures/block')
SLOTS = os.path.join(REPO, 'mc26_3/overlay/resources/assets/simplebuilding/textures/gui/sprites/container/slot')

# Farben des Verkaufsblocks (wie sales_block_side/front): Eisen hell..dunkel, Bretter, Gold, Schlitz.
LID_COLORS = {
    'W': (0xE0, 0xE4, 0xEA), 'I': (0xB8, 0xBC, 0xC4), 'M': (0x88, 0x8C, 0x94), 'S': (0x60, 0x64, 0x6C), 'K': (0x3A, 0x3C, 0x42),
    'p': (0x7C, 0x5A, 0x34), 'm': (0x64, 0x48, 0x28), 'd': (0x4E, 0x36, 0x1E), 'l': (0x38, 0x26, 0x15),
    'g': (0xF8, 0xD0, 0x54), 'o': (0xCE, 0x9A, 0x26), 'x': (0x24, 0x18, 0x0E),
}
LID_GROUPS = [('Bretter', 'pmdl'), ('Nieten und Beschlag', 'goxN')]

# A: Bretter wie die Seitenflaeche (1-px-Eisenrahmen, Nieten in den Ecken), quer zur Front.
LID_A = [
    'WIIIIIIIWIIIIIII', 'IpppplpppppppppS', 'ImNSmlmmmmdmSNmS', 'IddddldddddddddS',
    'IllllllllllllllS', 'IppppppppppplppS', 'ImdmmmmmmdmlmmmS', 'IddddddddddldddS',
    'WllllllllllllllS', 'IlpppppppppppppS', 'IlmmmmmmdmmmmmmS', 'IldddddddddddddS',
    'IllllllllllllllS', 'IpNSppplppppSNpS', 'ImmmmmmlmmmmmmdS', 'ISSSSSSSSSSSSSSK',
]
# B: wie A, quer ueber die Mitte ein Eisenband mit goldgefasstem Muenzschlitz.
LID_B = LID_A[:6] + ['IIIIIIIIIIIIIIIS', 'IMMMMoxxxxoMMMMS', 'WSSSSSooooSSSSSS'] + LID_A[9:]


def _lid_c():
    """C: der bisherige 2-px-Eisenrahmen (Schachbrett innen), innen 12x12 geschlossene Bretter mit vier Nieten."""
    inner = [
        'ppppl ppppppp'.replace(' ', ''),
        'mNmmlmmmdmNm',
        'ddddlddddddd',
        'llllllllllll',
        'ppppppppl ppp'.replace(' ', ''),
        'mmdmmmmmlmmm',
        'mmmmmdmmlmmm',
        'ddddddddlddd',
        'llllllllllll',
        'ppplpppppppp',
        'mNmlmmdmmmNm',
        'dddldddddddd',
    ]
    rows = ['WIIIIIIIWIIIIIII', 'IIMIMIMIMIMIMIMS']
    for i, row in enumerate(inner):
        y = i + 2
        left = ('W' if y == 8 else 'I') + ('M' if y % 2 == 0 else 'I')
        right = ('I' if y % 2 == 0 else 'M') + 'S'
        rows.append(left + row + right)
    rows += ['IMIMIMIMIMIMIMIS', 'ISSSSSSSSSSSSSSK']
    return rows


LID_C = _lid_c()
LIDS = {'A': LID_A, 'B': LID_B, 'C': LID_C}
LID_DEFAULT = 'D'
# WD25-19 (Besitzer: "zu strahlend und viel Kontrast"): D = Form C mit gedaempfter Palette - Eisen dunkler und enger
# beieinander (wie Vanillas Eisenbeschlaege im Schatten), Bretter mit weicheren Fugen; die Oberseite wird im Spiel
# heller beleuchtet als die Seiten, darum etwas dunkler als deren Palette.
LID_SOFT = dict(LID_COLORS, W=(0xA4, 0xA8, 0xAE), I=(0x8C, 0x90, 0x96), M=(0x76, 0x7A, 0x80), S=(0x5E, 0x62, 0x68),
                K=(0x44, 0x46, 0x4C), p=(0x6C, 0x4F, 0x2E), m=(0x5C, 0x42, 0x25), d=(0x4E, 0x37, 0x1F), l=(0x40, 0x2C, 0x18))
LIDS['D'] = LID_C

GREY = (85, 85, 85)
TIP_A = [
    '................', '................', '.......##.......', '......####......', '......####......', '.....######.....',
    '.....######.....', '....########....', '....########....', '...##########...', '..####.##.####..', '..###..##..###..',
    '..##...##...##..', '.......##.......', '................', '................']
TIP_B = [
    '................', '................', '.......##.......', '......####......', '......####......', '.....######.....',
    '.....######.....', '....########....', '....########....', '...##########...', '...##########...', '....###..###....',
    '...####..####...', '.......##.......', '................', '................']
TIP_C = [
    '................', '................', '.......########.', '........#######.', '.........######.', '..........#####.',
    '.........######.', '........#######.', '.......####.###.', '......####...##.', '......##.......#', '................',
    '................', '................', '................', '................']
TIPS = {'A': TIP_A, 'B': TIP_B, 'C': TIP_C}
# WD25-19 (Besitzer 25.10.): Spitze B, aber ausgeduennt - Mitte frei wie Vanillas Slot-Silhouetten (Umriss 1 px);
# die Feder (bisher volle Silhouette) ebenso als Umriss.
FEATHER = [
    '................', '................', '............#...', '............##..', '...........###..', '..........#####.',
    '..........####..', '........#####...', '......#.######..', '.....########...', '.....######.....', '...#########....',
    '....######......', '..#######.......', '.....##.........', '................']


def outline(rows):
    """Nur die Randpixel einer Silhouette (ein Nachbar oben/unten/links/rechts ist leer): Mitte frei."""
    def on(x, y):
        return 0 <= y < len(rows) and 0 <= x < len(rows[y]) and rows[y][x] == '#'
    return [''.join('#' if on(x, y) and not (on(x - 1, y) and on(x + 1, y) and on(x, y - 1) and on(x, y + 1)) else '.'
                    for x in range(16)) for y in range(16)]


TIPS['B2'] = outline(TIP_B)[:10] + ['...#........#...', '....###..###....', '...##..##..##...', '.......##.......'] + ['.' * 16] * 2  # Fuss von Hand
TIPS['F'] = FEATHER
TIPS['F2'] = outline(FEATHER)
TIP_DEFAULT = 'B2'
FEATHER_DEFAULT = 'F2'


def lid(name):
    rows = LIDS[name]
    base = LID_SOFT if name == 'D' else LID_COLORS
    colors = dict(base, N=base['W'])
    img = Image.new('RGBA', (16, 16))
    img.putdata([tuple(colors[c]) + (255,) for row in rows for c in row])
    layers.snap_grid(img, rows, colors, LID_GROUPS, base='Rahmen')
    return img


def tip(name):
    rows = TIPS[name]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    img.putdata([GREY + (255,) if c == '#' else (0, 0, 0, 0) for row in rows for c in row])
    layers.snap(img, 'Grundform')
    return img


def targets():
    return [(lid(LID_DEFAULT), os.path.join(SHOPS, 'sales_block_top.png')), (tip(TIP_DEFAULT), os.path.join(SLOTS, 'arrow_tip.png')),
            (tip(FEATHER_DEFAULT), os.path.join(SLOTS, 'arrow_fletching.png'))]


def main(argv):
    for rows in list(LIDS.values()) + list(TIPS.values()):
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    if '--preview' in argv:
        out = argv[argv.index('--preview') + 1]
        os.makedirs(out, exist_ok=True)
        for n in LIDS:
            lid(n).save(os.path.join(out, 'lid_' + n + '.png'))
        for n in TIPS:
            tip(n).save(os.path.join(out, 'tip_' + n + '.png'))
        return 0
    if '--check' in argv:
        bad = 0
        for img, path in targets():
            have = Image.open(path).convert('RGBA').tobytes()
            if have != img.convert('RGBA').tobytes():
                print('veraltet:', os.path.relpath(path, REPO))
                bad += 1
        return 1 if bad else 0
    if '--install' in argv:
        for img, path in targets():
            layers.save(img, path)
            print('geschrieben:', os.path.relpath(path, REPO))
        return 0
    print(__doc__)
    return 2


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
