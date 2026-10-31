"""b-ig3114 (31.10.): candidates for queue IG31-14 - PREVIEW ONLY, nothing is written into the modules.

Motifs (plan docs/ai/PLAN-B-IG3114-2026-10-31.md), each Ist + variants A/B/C:

* platten      - the Simple Music discs (deep_pulse, ember_forge, end_departure, fortress_march, obsidian_throne, sky_forge,
                 custom_disc, custom_disc_composed) on the Vanilla disc layout (15x10, caps of 5, 5x3 label around the hole,
                 two shadow rows), own groove arcs, and a label SHAPE of its own per disc (colour-blind safe);
* music        - blank disc, sheet music, pocket jukebox (+ playing state);
* daemmerholz  - Duskwood log side/top, stripped log side/top, sign and hanging sign items (planks A and the door items C
                 are accepted and stay untouched - their palette is only read);
* besatz       - the runtime inversion of armor trims (TrimInversion) as pixel maths on Vanilla iron armor with Vanilla
                 patterns, rendered ONLY into the preview page (Vanilla-derived, never written to the repo).

Own pixels only go to tools/textures/ig3114/<motif>_<variant>_<name>.png plus .layers.json (one layer per drawing step).
Vanilla images are read from the client jar in memory for the comparison columns.

    python3 tools/textures/ig3114_2026_10_31.py [--jar <26.3 client.jar>] [--preview /root/sb-tools-b/previews/b-ig3114-preview.html]
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import math
import os
import random
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import layers  # noqa: E402

OUT = HERE / 'ig3114'
JAR_DEFAULT = os.path.expanduser('~/.gradle/caches/minecraftforge/forgegradle/mavenizer/caches/minecraft_tasks/26.3/client.jar')
MUSIC = ROOT / 'modules/simplemusic/shared/resources/assets/simplemusic/textures/item'
END = ROOT / 'modules/simpleend/shared/resources/assets/simpleend/textures'


# ---------------------------------------------------------------- basics

def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def scale(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c[:3])


def mix(a, b, t):
    return tuple(round(a[i] * (1 - t) + b[i] * t) for i in range(3))


def new(w=16, h=16):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


def put(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), tuple(c[:3]) + (255,))


def hard_share(im):
    """Share of neighbouring opaque pixel pairs whose luminance differs by more than 40 (same figure as tex29)."""
    hard = total = 0
    for y in range(im.height):
        for x in range(im.width):
            a = im.getpixel((x, y))
            if not a[3]:
                continue
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < im.width and y + dy < im.height:
                    b = im.getpixel((x + dx, y + dy))
                    if b[3]:
                        total += 1
                        hard += abs(lum(a) - lum(b)) > 40
    return hard / max(1, total)


def grid(im, rows, colors, x0=0, y0=0):
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.' and ch in colors:
                put(im, x0 + x, y0 + y, colors[ch])
    return im


def save_own(im, motif, variant, name):
    OUT.mkdir(exist_ok=True)
    path = OUT / f'{motif}_{variant}_{name}.png'
    im.save(path)
    steps = layers.steps_of(im)
    final = im.convert('RGBA').tobytes()
    if not steps:
        steps = [('Grundform', final)]
    elif steps[-1][1] != final:
        steps.append(('Nachbearbeitung', final))
    layers.write_beside(path, layers.from_steps(im.width, im.height, steps))
    return im


class Jar:
    def __init__(self, path):
        self.z = zipfile.ZipFile(path) if path and Path(path).exists() else None

    def img(self, rel):
        if not self.z:
            return new()
        return Image.open(io.BytesIO(self.z.read('assets/minecraft/textures/' + rel + '.png'))).convert('RGBA')


# ---------------------------------------------------------------- platten (music discs)

# Zones of the Vanilla disc (15 x 10, rows 3..12, left-aligned): O outline, B body, G groove light, R lower rim,
# K bottom outline, L label field (5 x 3 around the hole H at (7, 7)). The groove arcs are our own (two rings).
DISC = [
    '................',
    '................',
    '................',
    '.....OOOOO......',
    '..OOOBBBBBOOO...',
    '.OBBGGBBBGGBBO..',
    'OBGBBBLLLBBBGBO.',
    'OBGBBLLHLLBBGBO.',
    'OBBGBBLLLBBGBBO.',
    'ORBBGGBBBGGBBRO.',
    '.KRRRBBBBBRRRK..',
    '..KKKRRRRRKKK...',
    '.....KKKKK......',
    '................',
    '................',
    '................',
]
BLACK = (62, 61, 66)
HOLE = (16, 14, 18)

# label shapes: (x, y) -> m(ain) / d(ark) / l(ight); the hole stays dark
LABELS = {
    'ring': {(6, 6): 'l', (7, 6): 'm', (8, 6): 'm', (5, 7): 'd', (6, 7): 'm', (8, 7): 'm', (9, 7): 'd', (6, 8): 'm', (7, 8): 'm', (8, 8): 'd'},
    'flame': {(7, 6): 'l', (6, 7): 'm', (8, 7): 'm', (6, 8): 'd', (7, 8): 'm', (8, 8): 'd'},
    'crescent': {(6, 6): 'm', (7, 6): 'l', (5, 7): 'm', (6, 8): 'm', (7, 8): 'd'},
    'bars': {(6, 6): 'l', (6, 7): 'm', (6, 8): 'm', (8, 6): 'm', (8, 7): 'm', (8, 8): 'd'},
    'crown': {(6, 6): 'l', (8, 6): 'l', (5, 7): 'm', (6, 7): 'm', (8, 7): 'm', (9, 7): 'm', (6, 8): 'd', (7, 8): 'd', (8, 8): 'd'},
    'cross': {(7, 6): 'l', (5, 7): 'm', (6, 7): 'm', (8, 7): 'm', (9, 7): 'm', (7, 8): 'd'},
    'full': {(6, 6): 'l', (7, 6): 'm', (8, 6): 'm', (5, 7): 'm', (6, 7): 'm', (8, 7): 'm', (9, 7): 'm', (6, 8): 'm', (7, 8): 'm', (8, 8): 'd'},
    'note': {(6, 6): 'm', (7, 6): 'm', (8, 6): 'd', (5, 7): 'm', (6, 7): 'm', (8, 7): 'd', (9, 7): 'm', (6, 8): 'd', (7, 8): 'd', (8, 8): 'm'},
}

# name -> label shape, (main, dark, light), tinted body (B), rim colour (C), short description
DISCS = {
    'deep_pulse': ('ring', ((44, 176, 186), (22, 100, 112), (160, 238, 236)), (34, 62, 68), (30, 112, 120), 'Ring (Puls)'),
    'ember_forge': ('flame', ((232, 112, 32), (168, 52, 22), (255, 216, 92)), (94, 46, 36), (156, 66, 28), 'Flamme'),
    'end_departure': ('crescent', ((164, 104, 204), (104, 62, 144), (234, 230, 172)), (56, 44, 72), (104, 68, 144), 'Sichel'),
    'fortress_march': ('bars', ((196, 54, 52), (124, 30, 32), (242, 144, 112)), (66, 36, 40), (124, 40, 42), 'zwei Balken'),
    'obsidian_throne': ('crown', ((224, 174, 52), (98, 56, 148), (255, 238, 142)), (40, 30, 56), (128, 96, 34), 'Krone'),
    'sky_forge': ('cross', ((112, 172, 232), (62, 104, 172), (236, 246, 255)), (58, 72, 94), (72, 124, 184), 'Kreuz'),
    'custom_disc': ('full', ((152, 98, 204), (102, 62, 152), (222, 194, 242)), (62, 54, 72), (112, 72, 152), 'volles Etikett'),
    'custom_disc_composed': ('note', ((234, 224, 198), (62, 52, 46), (252, 248, 234)), (68, 64, 60), (150, 132, 102), 'Etikett mit Note'),
}


def disc_ramp(body):
    """Vanilla's disc steps (outline -29, groove +17, rim -24, bottom -45 luminance on the black disc); on light bodies
    the steps stay about as large in luminance instead of growing with the factor (soft, A8b)."""
    L = max(1.0, lum(body))
    return {'O': scale(body, max(0.56, 1 - 36 / L)), 'B': body, 'G': scale(body, min(1.30, 1 + 20 / L)),
            'R': scale(body, max(0.64, 1 - 30 / L)), 'K': scale(body, max(0.30, 1 - 62 / L)), 'L': body, 'H': HOLE}


def disc(body=BLACK, label=None, colors=None, rim=None, grooves=True, label_fill=None):
    im = new()
    ramp = disc_ramp(body)
    rows = DISC if grooves else [r.replace('G', 'B') for r in DISC]
    grid(im, rows, {k: v for k, v in ramp.items() if k not in 'GRK'} | {'G': ramp['B'], 'R': ramp['B'], 'K': ramp['O']})
    layers.snap(im, 'Scheibe')
    if grooves:
        grid(im, [r if 'G' in r else '' for r in rows], {'G': ramp['G']})
        layers.snap(im, 'Rillen')
    r = {'R': ramp['R'], 'K': ramp['K']}
    if rim is not None:
        r = {'R': scale(rim, 0.8), 'K': scale(rim, 0.45)}
    grid(im, rows, r)
    layers.snap(im, 'Schattenrand')
    if label_fill is not None:  # blank paper label (Rohling)
        grid(im, rows, {'L': label_fill[0]})
        put(im, 6, 6, label_fill[1])
        put(im, 8, 8, label_fill[2])
    if label:
        tone = {'m': colors[0], 'd': colors[1], 'l': colors[2]}
        for (x, y), role in LABELS[label].items():
            put(im, x, y, tone[role])
    put(im, 7, 7, HOLE)
    layers.snap(im, 'Etikett')
    return im


def platten():
    out = {}
    for v in 'ABC':
        for name, (shape, cols, tint, rim, _) in DISCS.items():
            if v == 'A':
                im = disc(BLACK, shape, cols)
            elif v == 'B':
                im = disc(tint, shape, cols)
            else:
                im = disc(BLACK, shape, cols, rim=rim)
            out[(v, name)] = save_own(im, 'platten', v, name)
    return out


# ---------------------------------------------------------------- music (blank disc, sheet music, pocket jukebox)

PAPER = {'o': (126, 122, 116), 'e': (176, 172, 164), 'p': (248, 246, 236), 'q': (226, 222, 210), 'h': (255, 255, 250),
         's': (170, 164, 156), 'n': (52, 44, 50), 'm': (98, 90, 96)}


def sheet_a():
    """Vanilla paper's diagonal sheet (own outline), faint staff lines along the long axis, a bold beamed note pair."""
    im = new()
    rows = [
        '................',
        '................',
        '.........oo.....',
        '........ohpo....',
        '......oohpppo...',
        '.....ohpppppqo..',
        '...oohpppppppqo.',
        '..ohpppppppppqo.',
        '.ohppppppppppqo.',
        '..oqpppppppqqo..',
        '...oqqpppppqo...',
        '....ooqpppqo....',
        '.....ooqqqo.....',
        '.......ooo......',
        '................',
        '................',
    ]
    grid(im, rows, PAPER)
    layers.snap(im, 'Blatt')
    for y in range(16):
        for x in range(16):
            p = im.getpixel((x, y))
            if p[3] and p[:3] in (PAPER['p'], PAPER['q']) and (x + 2 * y) in (16, 21, 26):
                put(im, x, y, PAPER['e'])
    layers.snap(im, 'Notenlinien')
    note = [(7, 4), (8, 4), (9, 4), (10, 4), (11, 4), (7, 5), (11, 5), (7, 6), (11, 6), (7, 7), (11, 7), (7, 8), (11, 8),
            (5, 8), (6, 8), (5, 9), (6, 9), (9, 8), (10, 8), (9, 9), (10, 9)]
    for (x, y) in note:
        put(im, x, y, PAPER['n'])
    for (x, y) in [(8, 5), (9, 5), (10, 5)]:
        put(im, x, y, PAPER['m'])
    layers.snap(im, 'Noten')
    return im


def sheet_b():
    """Upright page (map-like proportions, 10 x 14) with a dog-ear, three staff lines, two beamed eighth notes."""
    im = new()
    for y in range(1, 15):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (1, 14)
            if x - y > 9:  # dog-ear cut
                continue
            put(im, x, y, PAPER['o'] if edge or x - y == 9 else PAPER['q'] if x == 11 or y == 13 else PAPER['p'])
    for (x, y) in [(10, 1), (11, 2), (10, 2)]:
        put(im, x, y, PAPER['e'])
    layers.snap(im, 'Blatt')
    for y in (5, 8, 11):
        for x in range(4, 12):
            if im.getpixel((x, y))[3]:
                put(im, x, y, PAPER['s'])
    layers.snap(im, 'Notenlinien')
    for (x, y, c) in [(5, 10, 'n'), (6, 10, 'n'), (6, 9, 'n'), (6, 8, 'm'), (6, 7, 'm'), (6, 6, 'm'), (6, 5, 'n'),
                      (9, 9, 'n'), (10, 9, 'n'), (10, 8, 'n'), (10, 7, 'm'), (10, 6, 'm'), (10, 5, 'n'), (10, 4, 'm'),
                      (7, 5, 'm'), (8, 5, 'm'), (9, 4, 'm')]:
        put(im, x, y, PAPER[c])
    layers.snap(im, 'Noten')
    return im


def sheet_c():
    """A short scroll: sheet between two rolls, three staff lines, two notes."""
    im = new()
    for y in range(4, 12):
        for x in range(3, 13):
            put(im, x, y, PAPER['o'] if y in (4, 11) else PAPER['q'] if y == 10 else PAPER['p'])
    layers.snap(im, 'Blatt')
    for y in (6, 8):
        for x in range(4, 12):
            put(im, x, y, PAPER['s'])
    layers.snap(im, 'Notenlinien')
    for x0 in (1, 12):
        for y in range(3, 13):
            for x in (x0, x0 + 1, x0 + 2):
                if x == 14 and x0 == 12 and False:
                    continue
                edge = y in (3, 12) or x in (x0, x0 + 2)
                put(im, x, y, PAPER['o'] if edge else PAPER['e'] if y > 6 else PAPER['h'])
    layers.snap(im, 'Rollen')
    for (x, y, c) in [(5, 9, 'n'), (6, 9, 'n'), (6, 8, 'n'), (6, 7, 'm'), (6, 6, 'n'), (6, 5, 'm'),
                      (9, 7, 'n'), (10, 7, 'n'), (10, 6, 'n'), (10, 5, 'm'), (11, 5, 'm')]:
        put(im, x, y, PAPER[c])
    layers.snap(im, 'Noten')
    return im


WOOD = {'o': (46, 31, 22), 'a': (78, 52, 36), 'b': (100, 67, 47), 'c': (120, 81, 57), 'd': (140, 96, 68), 'e': (160, 112, 80),
        'k': (22, 18, 18), 'm': (152, 152, 158), 'M': (196, 196, 202), 'g': (58, 40, 30)}


def note_glyph(im):
    for (x, y, c) in [(13, 0, (250, 246, 230)), (14, 0, (250, 246, 230)), (13, 1, (250, 246, 230)), (13, 2, (250, 246, 230)),
                      (12, 3, (250, 246, 230)), (13, 3, (220, 214, 196))]:
        put(im, x, y, c)
    return im


def jukebox_a(playing=False):
    """A small wooden jukebox in front view: lid with the dark disc slot, front frame, round speaker inlay."""
    im = new()
    rows = [
        '................',
        '................',
        '................',
        '..oooooooooooo..',
        '..odeeeeeeeedo..',
        '..odckkkkkkcdo..',
        '..oaaaaaaaaaao..',
        '..odddddddddco..',
        '..odcbbbbbbcco..',
        '..odbcgggcbbco..',
        '..odbgcccgbbco..',
        '..odbcgggcbbco..',
        '..odcbbbbbbcco..',
        '..obbbbbbbbbbo..',
        '..oooooooooooo..',
        '................',
    ]
    grid(im, rows, WOOD)
    layers.snap(im, 'Kasten')
    if playing:
        put(im, 6, 5, WOOD['m'])
        note_glyph(im)
        layers.snap(im, 'Spielt')
    return im


def jukebox_b(playing=False):
    """Pocket radio: carrying handle, speaker grille left, disc slot and two knobs right."""
    im = new()
    rows = [
        '................',
        '.....oooooo.....',
        '....oa....ao....',
        '.oooooooooooooo.',
        '.odeeeeeeeeeedo.',
        '.odkgkgkdkkkkco.',
        '.odgkgkgdcccbco.',
        '.odkgkgkdcmcMco.',
        '.odgkgkgdcccbco.',
        '.odkgkgkdcbbbco.',
        '.odcccccccccbco.',
        '.obbbbbbbbbbbbo.',
        '.oooooooooooooo.',
        '................',
        '................',
        '................',
    ]
    grid(im, rows, WOOD)
    layers.snap(im, 'Kasten')
    if playing:
        put(im, 12, 7, (250, 220, 120))
        note_glyph(im)
        layers.snap(im, 'Spielt')
    return im


def jukebox_c(playing=False):
    """Little record player, three-quarter view: lid surface with the black disc and its label, wooden front."""
    im = new()
    rows = [
        '................',
        '................',
        '................',
        '...oooooooooo...',
        '..odeeeeeeeedo..',
        '..odekkkkkkedo..',
        '..odkkkmkkkkdo..',
        '..odkkkkkkkkdo..',
        '..odekkkkkkedo..',
        '..odeeeeeeedmo..',
        '..oaaaaaaaaaao..',
        '..obccccccccbo..',
        '..obcbbbbbbcbo..',
        '..oooooooooooo..',
        '................',
        '................',
    ]
    grid(im, rows, WOOD)
    put(im, 7, 7, (170, 60, 60))
    put(im, 8, 7, (210, 90, 70))
    layers.snap(im, 'Kasten')
    if playing:
        put(im, 6, 6, (120, 120, 126))
        note_glyph(im)
        layers.snap(im, 'Spielt')
    return im


GREY = (142, 142, 148)
PAPER_LABEL = ((236, 232, 220), (252, 250, 242), (206, 200, 186))


def music():
    out = {}
    blank = {'A': lambda: disc(GREY, grooves=False), 'B': lambda: disc(BLACK, grooves=False, label_fill=PAPER_LABEL),
             'C': lambda: disc(GREY, label_fill=PAPER_LABEL)}
    sheets = {'A': sheet_a, 'B': sheet_b, 'C': sheet_c}
    boxes = {'A': jukebox_a, 'B': jukebox_b, 'C': jukebox_c}
    for v in 'ABC':
        out[(v, 'blank_disc')] = save_own(blank[v](), 'music', v, 'blank_disc')
        out[(v, 'sheet_music')] = save_own(sheets[v](), 'music', v, 'sheet_music')
        out[(v, 'pocket_jukebox')] = save_own(boxes[v](), 'music', v, 'pocket_jukebox')
        out[(v, 'pocket_jukebox_playing')] = save_own(boxes[v](True), 'music', v, 'pocket_jukebox_playing')
    return out


# ---------------------------------------------------------------- daemmerholz (Duskwood)

def planks_palette():
    """The seven tones of the accepted Duskwood planks A (read, never changed), dark -> light."""
    im = Image.open(END / 'block/duskwood_planks.png').convert('RGBA')
    return sorted({p[:3] for p in im.getdata() if p[3]}, key=lum)


def bark_ramp(planks):
    """Seven bark tones: the planks' hue, a little browner, about 52..118 luminance in ~11 steps."""
    mid = planks[len(planks) // 2]
    out = []
    for i in range(7):
        f = 0.38 + 0.075 * i
        c = scale(mid, f)
        out.append((min(255, c[0] + 4), c[1], max(0, c[2] - 6)))
    return out


def log_side(variant, seed=31):
    rnd = random.Random(seed)
    bark = bark_ramp(planks_palette())
    im = new()
    # ridge/furrow pattern across the 16 columns (Vanilla logs: 1-3 px wide vertical bands)
    base = [4, 4, 2, 5, 4, 1, 3, 5, 5, 2, 4, 3, 1, 4, 5, 3]
    for x in range(16):
        y = 0
        start = rnd.randrange(4)
        while y < 16 + start:
            seg = rnd.randint(2, 6)
            t = max(0, min(6, base[x] + rnd.choice((-1, 0, 0, 1))))
            for k in range(seg):
                yy = (y - start + k) % 16
                put(im, x, yy, bark[t])
            y += seg
    layers.snap(im, 'Rinde')
    if variant == 'C':  # plate bark: cross breaks per plate, shifted per column group
        groups = [(0, 3), (3, 7), (7, 10), (10, 13), (13, 16)]
        for gi, (a, b) in enumerate(groups):
            for y in ((gi * 5 + 2) % 16, (gi * 5 + 10) % 16):
                for x in range(a, b):
                    put(im, x, y, bark[max(0, lum_index(im.getpixel((x, y)), bark) - 2)])
                    put(im, x, (y + 1) % 16, bark[min(6, lum_index(im.getpixel((x, (y + 1) % 16)), bark) + 1)])
        layers.snap(im, 'Platten')
    if variant == 'B':  # a few muted rose "dusk" glints (End identity of the old texture, much softer)
        for (x, y, c) in [(3, 4, (150, 118, 138)), (3, 5, (124, 98, 114)), (11, 11, (150, 118, 138)), (12, 11, (124, 98, 114))]:
            put(im, x, y, c)
        layers.snap(im, 'Glanzpunkte')
    return im


def lum_index(p, ramp):
    return min(range(len(ramp)), key=lambda i: abs(lum(ramp[i]) - lum(p)))


def log_top(variant, stripped=False):
    planks = planks_palette()
    bark = bark_ramp(planks)
    side = log_side(variant)
    im = new()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5)) * 0.7 + math.hypot(x - 7.5, y - 7.5) * 0.3
            d += (((x * 73856093) ^ (y * 19349663)) % 7 - 3) * 0.12  # slightly wavy rings like Vanilla log tops
            ring = int(d + 0.5)
            t = [3, 5, 4, 2, 4, 5, 3, 4][ring % 8]
            if ring <= 1:
                t = 1
            put(im, x, y, planks[t])
    layers.snap(im, 'Jahresringe')
    for i in range(16):  # the outer frame: bark (log) or the darkest wood tones (stripped)
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(im, x, y, planks[0] if stripped else side.getpixel((x, y))[:3])
    if not stripped:
        for i in range(1, 15):
            for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
                put(im, x, y, bark[5] if (x + y) % 3 else bark[6])
    layers.snap(im, 'Rand')
    if variant == 'B' and not stripped:
        put(im, 7, 7, (150, 118, 138))
        put(im, 8, 8, (124, 98, 114))
        layers.snap(im, 'Kern')
    return im


def stripped_side(variant, seed=7):
    rnd = random.Random(seed)
    planks = planks_palette()
    im = new()
    base = [3, 4, 4, 2, 3, 5, 4, 3, 2, 4, 5, 3, 4, 2, 3, 4]
    for x in range(16):
        y = 0
        while y < 16:
            seg = rnd.randint(3, 8)
            t = max(0, min(6, base[x] + rnd.choice((-1, 0, 0, 0, 1))))
            for k in range(seg):
                put(im, x, (y + k) % 16, planks[t])
            y += seg
    layers.snap(im, 'Holz')
    if variant == 'C':
        for x in range(16):
            y = (x * 5 + 3) % 16
            put(im, x, y, planks[max(0, lum_index(im.getpixel((x, y)), planks) - 2)])
        layers.snap(im, 'Querfasern')
    return im


def sign_item(variant, hanging=False):
    planks = planks_palette()
    bark = bark_ramp(planks)
    im = new()
    if hanging:
        x0, x1, y0, y1 = 2, 13, 6, 12
        for x in range(1, 15):  # stripped-log top bar
            put(im, x, 1, planks[5] if x % 4 else planks[4])
            put(im, x, 2, planks[2] if x % 3 else planks[1])
        for cx in (4, 11):  # chains
            for (dx, y, c) in [(0, 3, (76, 76, 80)), (-1, 4, (52, 52, 56)), (1, 4, (52, 52, 56)), (0, 4, (96, 96, 100)), (-1, 5, (52, 52, 56)), (1, 5, (52, 52, 56))]:
                put(im, cx + dx, y, c)
        layers.snap(im, 'Balken und Ketten')
    else:
        x0, x1, y0, y1 = 2, 14, 2, 8
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t = 4 if y == y0 else 3
            if variant == 'A' and (y - y0) in (2, 5) and (x * 7 + y) % 5 in (0, 1, 3):
                t = 1
            if variant == 'B':
                if y == (y0 + y1) // 2:
                    t = 1
                elif (x * 5 + y) % 7 in (0, 3) and y not in (y0,):
                    t = 2
            if variant == 'C':
                if y == y0 or x == x0:
                    t = 5
                elif y == y1 or x == x1:
                    t = 2
                elif (y - y0) in (2, 4) and (x * 3 + y) % 4 in (0, 1):
                    t = 2
            put(im, x, y, planks[t])
    layers.snap(im, 'Brett')
    for x in range(x0, x1 + 1):  # two shadow rows under the board (Vanilla)
        put(im, x, y1 + 1, planks[0])
        put(im, x, y1 + 2, scale(planks[0], 0.72))
    layers.snap(im, 'Schatten')
    if not hanging:
        for y in range(11, 16):
            put(im, 7, y, bark[5])
            put(im, 8, y, bark[3] if y % 3 else bark[4])
            put(im, 9, y, bark[1])
        layers.snap(im, 'Pfosten')
    return im


def daemmerholz():
    out = {}
    for v in 'ABC':
        for name, im in [('duskwood_log', log_side(v)), ('duskwood_log_top', log_top(v)),
                         ('stripped_duskwood_log', stripped_side(v)), ('stripped_duskwood_log_top', log_top(v, True)),
                         ('duskwood_sign', sign_item(v)), ('duskwood_hanging_sign', sign_item(v, True))]:
            out[(v, name)] = save_own(im, 'daemmerholz', v, name)
    return out


# ---------------------------------------------------------------- besatz (inverted armor trims, runtime maths)

RIM_SUBTRACT = 80
BORDER = {1: 1, 2: 2, 3: 3}


def subtract(c, n=RIM_SUBTRACT):
    return tuple(max(0, v - n) for v in c[:3])


def trim_colored(pattern, base_keys, palette):
    """Vanilla paletted permutation: grey keys of trim_base -> the material palette."""
    key = {base_keys[i]: palette[i] for i in range(8)}
    out = new(pattern.width, pattern.height)
    for y in range(pattern.height):
        for x in range(pattern.width):
            p = pattern.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), key.get(p[:3], p[:3]) + (255,))
    return out


def dist(pattern, x, y, maxd):
    w, h = pattern.width, pattern.height
    for d in range(1, maxd + 1):
        for dx in range(-d, d + 1):
            dy = d - abs(dx)
            for yy in (y + dy, y - dy):
                xx = x + dx
                if 0 <= xx < w and 0 <= yy < h and pattern.getpixel((xx, yy))[3]:
                    return d
    return 0


def ist_fill(palette):
    order = sorted(palette, key=lum)
    return order[max(0, len(order) - 3)]


def shaded(armor_px, palette):
    """A palette tone that follows the armor's brightness (light armor -> light trim tone)."""
    if not armor_px[3]:
        return palette[3]  # not reached for A-C (masked)
    idx = 6 - round(min(1.0, max(0.0, (lum(armor_px) - 60) / 170)) * 5)  # 1 .. 6
    return palette[max(1, min(6, idx))]


def inverted(pattern, armor, palette, variant, stage):
    out = new(pattern.width, pattern.height)
    fill = ist_fill(palette)
    for y in range(pattern.height):
        for x in range(pattern.width):
            if pattern.getpixel((x, y))[3]:
                continue
            a = armor.getpixel((x, y))
            if variant != 'ist' and not a[3]:
                continue  # A-C: only where the armor itself has pixels (Ist also fills the gaps, e.g. the helmet's face opening)
            if variant == 'ist':
                d = dist(pattern, x, y, BORDER[stage])
                out.putpixel((x, y), (subtract(fill) if d else fill) + (255,))
            elif variant in ('A', 'C'):
                d = dist(pattern, x, y, BORDER[stage])
                f = shaded(a, palette)
                out.putpixel((x, y), (subtract(f) if d else f) + (255,))
            elif variant == 'B':
                d = dist(pattern, x, y, stage + 1)
                if d == 0:
                    continue
                f = shaded(a, palette)
                out.putpixel((x, y), (subtract(f) if d == 1 else f) + (255,))
    if variant == 'C':  # engraving: shadow on the top/left inner edge of the cut-out, light on the bottom/right one
        for y in range(pattern.height):
            for x in range(pattern.width):
                if not pattern.getpixel((x, y))[3]:
                    continue
                up = y > 0 and not pattern.getpixel((x, y - 1))[3]
                left = x > 0 and not pattern.getpixel((x - 1, y))[3]
                down = y < pattern.height - 1 and not pattern.getpixel((x, y + 1))[3]
                right = x < pattern.width - 1 and not pattern.getpixel((x + 1, y))[3]
                if up or left:
                    out.putpixel((x, y), palette[6] + (255,))
                elif down or right:
                    out.putpixel((x, y), palette[1] + (255,))
    return out


# front view of the humanoid armor texture (64x32): head front, body front, both arms, both legs/boots
FRONT = [((8, 8, 8, 8), (4, 0), False), ((20, 20, 8, 12), (4, 8), False), ((44, 20, 4, 12), (0, 8), False),
         ((44, 20, 4, 12), (12, 8), True), ((4, 20, 4, 12), (4, 20), False), ((4, 20, 4, 12), (8, 20), True)]


def front(tex):
    o = new(16, 32)
    for (u, v, w, h), (x, y), mirror in FRONT:
        part = tex.crop((u, v, u + w, v + h))
        if mirror:
            part = part.transpose(Image.FLIP_LEFT_RIGHT)
        o.alpha_composite(part, (x, y))
    return o


def besatz(jar):
    armor = jar.img('entity/equipment/humanoid/iron')
    keys = [jar.img('palettes/trim_base').getpixel((i, 0))[:3] for i in range(8)]
    combos = [('coast', 'amethyst'), ('sentry', 'gold'), ('wild', 'redstone')]
    res = {}
    for pat, mat in combos:
        pattern = jar.img('trims/entity/humanoid/' + pat)
        pal = [jar.img('palettes/trim/' + mat).getpixel((i, 0))[:3] for i in range(8)]
        normal = armor.copy()
        normal.alpha_composite(trim_colored(pattern, keys, pal))
        res[(pat, mat, 'vanilla', 0)] = front(normal)
        for v in ('ist', 'A', 'B', 'C'):
            for stage in (1, 2, 3):
                if stage != 2 and pat != 'coast':
                    continue
                im = armor.copy()
                im.alpha_composite(inverted(pattern, armor, pal, v, stage))
                res[(pat, mat, v, stage)] = front(im)
    return res


# ---------------------------------------------------------------- preview page

def uri(im, s):
    buf = io.BytesIO()
    im.resize((im.width * s, im.height * s), Image.NEAREST).save(buf, 'PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


CSS = (':root{--bg:#f4f2f6;--fg:#1d1b20;--card:#fff;--muted:#4a4452;--acc:#8a5a00;--line:#8886;--slot:#8b8b8b}'
       '@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--muted:#c9c2d4;--acc:#ffd27a}}'
       ':root[data-theme="dark"]{--bg:#1d1b20;--fg:#e8e4ee;--card:#2a2730;--muted:#c9c2d4;--acc:#ffd27a}'
       'body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif}main{max-width:1200px;margin:auto;padding:16px}'
       'h1{font-size:22px}h2{font-size:19px;margin-top:30px;border-top:2px solid var(--line);padding-top:12px}h3{font-size:15px;margin:14px 0 6px}'
       'img{image-rendering:pixelated;display:block;max-width:100%;height:auto}'
       '.row{display:flex;flex-wrap:wrap;gap:10px;align-items:flex-start;margin:6px 0}'
       '.cell{background:var(--card);padding:6px;border-radius:6px;max-width:100%}.cap{font-size:12px;color:var(--muted);margin-top:3px;max-width:200px}'
       '.slot{background:var(--slot);padding:2px;display:inline-block;margin-top:4px}.slot img{width:32px;height:32px}.one img{width:16px;height:16px}'
       '.hard{font-weight:600}.warn{color:#c0392b}.note{max-width:920px}'
       'table{border-collapse:collapse;font-size:13px}td,th{border:1px solid var(--line);padding:3px 6px;vertical-align:top;text-align:left}'
       '.choose{display:flex;flex-wrap:wrap;gap:8px;margin:8px 0}.choose label{background:var(--card);padding:6px 10px;border-radius:6px;cursor:pointer}'
       'pre{background:var(--card);padding:10px;border-radius:6px;white-space:pre-wrap;word-break:break-all;user-select:all}'
       'button{font:inherit;padding:6px 12px;border-radius:6px;border:1px solid var(--line);background:var(--card);color:var(--fg);cursor:pointer}'
       '.wrap{overflow-x:auto;max-width:100%}')


def cell(im, cap, s=10, hard=True):
    h = hard_share(im) if hard else None
    tag = '' if h is None else f' · <span class="hard{" warn" if h > 0.12 else ""}">{h * 100:.0f} % hart</span>'
    small = (f'<div class="slot"><img src="{uri(im, 2)}" alt=""></div> <div class="slot one"><img src="{uri(im, 1)}" alt=""></div>'
             if im.width <= 16 and im.height <= 16 else '')
    return f'<div class="cell"><img src="{uri(im, s)}" alt="{cap}" width="{im.width * s}" height="{im.height * s}">{small}<div class="cap">{cap}{tag}</div></div>'


def choose(key, opts):
    radios = ''.join(f'<label><input type="radio" name="{key}" value="{o}"{" checked" if o == "?" else ""}> {o}</label>' for o in opts)
    return f'<div class="choose"><b>{key} =</b>{radios}</div>'


def page(jar, pl, mu, dh, bz):
    h = ['<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">',
         '<title>IG31-14 Vorschau</title><style>', CSS, '</style></head><body><main>',
         '<h1>IG31-14 – Simple Music, Dämmerholz, Musikplatten, inverser Besatz</h1>',
         '<p class="note">Nur Vorschau, nichts im Spiel ersetzt. Je Motiv: <b>Vanilla</b> zum Vergleich, <b>Ist</b>, dann Varianten A/B/C '
         '(eigene Pixel, Vanilla-Raster). Unter jedem Bild: 10-fach, 2-fach im grauen Slot, Originalgröße, Anteil harter Übergänge '
         '(Nachbarpaare mit Helligkeitssprung &gt; 40; Ziel ~7 %, über 12 % rot – bei Items zählen Etikett-/Notenkontraste mit, '
         'Vanilla-Platten liegen selbst bei ~20–30 %). Abgenommene Texturen (Dämmerholz-Bretter A, Tür-Items C) bleiben unberührt. '
         'Antwort unten auswählen und den Code kopieren.</p>']

    # platten
    h.append('<h2>1 · Musikplatten (platten)</h2><p class="note">Vanilla-Aufbau exakt: 15×10, links bündig, Kappen 5 px, Etikett 5×3 '
             'um das Loch, zwei Schattenreihen unten. Jede Platte hat eine <b>eigene Etikett-Form</b> (auch farbenblind unterscheidbar). '
             'A schwarz (wie die meisten Vanilla-Platten), B Körper getönt (wie Pigstep/Creator), C schwarz mit farbigem Randbogen (wie Platte 5).</p>')
    h.append('<h3>Vanilla</h3><div class="row">')
    for n in ('13', 'cat', 'pigstep', 'creator', '5', 'otherside'):
        h.append(cell(jar.img('item/music_disc_' + n), 'music_disc_' + n))
    h.append('</div>')
    for name, (shape, _, _, _, desc) in DISCS.items():
        h.append(f'<h3>{name} – Etikett: {desc}</h3><div class="row">')
        h.append(cell(Image.open(MUSIC / f'{name}.png' if name.startswith('custom') else MUSIC / f'music_disc_{name}.png').convert('RGBA'), 'Ist'))
        for v in 'ABC':
            h.append(cell(pl[(v, name)], v))
        h.append('</div>')
    h.append(choose('platten', ['A', 'B', 'C', 'ist', '?']))

    # music
    h.append('<h2>2 · Simple-Music-Items (music)</h2><p class="note">Rohling (Platten-Proportionen wie oben), Notenblatt (Vanilla: Papier, Karte), '
             'Taschen-Jukebox (Vanilla: Jukebox-Deckel, Bündel). A Vanilla-nah: grauer Rohling ohne Rillen, Papier-Diagonale, kleine Holz-Jukebox · '
             'B schwarzer Rohling mit leerem Papier-Etikett, Blatt hochkant mit Eselsohr, Kofferradio mit Griff · C grauer Rohling mit Rillen + Papier-Etikett, '
             'Notenrolle, Mini-Plattenspieler. Die 16 Farben und die offenen Zustände (Bündel-Ansicht) entstehen nach der Wahl aus der Grundform.</p>')
    h.append('<h3>Vanilla</h3><div class="row">')
    for rel in ('item/paper', 'item/map', 'item/bundle', 'block/jukebox_top', 'block/jukebox_side'):
        h.append(cell(jar.img(rel), rel))
    h.append('</div>')
    for name in ('blank_disc', 'sheet_music', 'pocket_jukebox', 'pocket_jukebox_playing'):
        h.append(f'<h3>{name}</h3><div class="row">')
        h.append(cell(Image.open(MUSIC / f'{name}.png').convert('RGBA'), 'Ist'))
        for v in 'ABC':
            h.append(cell(mu[(v, name)], v))
        h.append('</div>')
    h.append(choose('music', ['A', 'B', 'C', 'ist', '?']))

    # daemmerholz
    h.append('<h2>3 · Dämmerholz (daemmerholz)</h2><p class="note">Farben aus den abgenommenen Brettern A; Rinde dunkler und etwas brauner. '
             'A Längsfurchen wie Vanilla-Eiche/Schwarzeiche, Schild mit zwei Maserungsreihen · B wie A + wenige gedämpfte Rosa-Glanzpunkte '
             '(End-Kennung des alten Stamms, viel weicher), Schild aus zwei Brettern mit Fuge · C Plattenrinde (versetzte Querbrüche wie Fichte), '
             'Schild mit hellem Rahmen. Schilder in Vanilla-Proportionen: Brett 13×7, zwei Schattenreihen, Pfosten 3 px.</p>')
    h.append('<h3>Vanilla</h3><div class="row">')
    for rel in ('block/dark_oak_log', 'block/dark_oak_log_top', 'block/stripped_dark_oak_log', 'block/oak_log', 'item/oak_sign', 'item/oak_hanging_sign'):
        h.append(cell(jar.img(rel), rel))
    h.append('</div><h3>Abgenommen (nur Bezug, unverändert)</h3><div class="row">')
    h.append(cell(Image.open(END / 'block/duskwood_planks.png').convert('RGBA'), 'Bretter A (abgenommen)'))
    h.append(cell(Image.open(END / 'item/duskwood_door.png').convert('RGBA'), 'Tür-Item C (abgenommen)'))
    h.append('</div>')
    ist = {'duskwood_log': 'block/duskwood_log', 'duskwood_log_top': 'block/duskwood_log_top',
           'stripped_duskwood_log': 'block/stripped_duskwood_log', 'stripped_duskwood_log_top': 'block/stripped_duskwood_log_top',
           'duskwood_sign': 'item/duskwood_sign', 'duskwood_hanging_sign': 'item/duskwood_hanging_sign'}
    for name, rel in ist.items():
        h.append(f'<h3>{name}</h3><div class="row">')
        h.append(cell(Image.open(END / f'{rel}.png').convert('RGBA'), 'Ist'))
        for v in 'ABC':
            h.append(cell(dh[(v, name)], v))
        h.append('</div>')
    h.append('<h3>Stamm 3×3 (Kacheln, Seite)</h3><div class="row">')
    for label, im in [('Ist', Image.open(END / 'block/duskwood_log.png').convert('RGBA'))] + [(v, dh[(v, 'duskwood_log')]) for v in 'ABC']:
        t = new(48, 48)
        for i in range(3):
            for k in range(3):
                t.alpha_composite(im, (i * 16, k * 16))
        h.append(cell(t, label, s=4, hard=False))
    h.append('</div>')
    h.append(choose('daemmerholz', ['A', 'B', 'C', 'ist', '?']))

    # besatz
    h.append('<h2>4 · Inverser Rüstungsbesatz (besatz)</h2><p class="note">Vorderansicht der Eisenrüstung (Helm, Brust, Arme, Stiefel) aus der '
             'Rüstungstextur; Vanilla-Muster coast/sentry/wild mit Amethyst/Gold/Redstone. <b>Ist</b>: eine flache Farbe über die ganze Rüstung '
             '(darum wirkt es „angemalt“), Rand = Füllung −80. <b>A Schattiert</b>: Füllung folgt der Helligkeit der Rüstung über die 8er-Besatzpalette, '
             'Rand bleibt Subtraktion. <b>B Saum</b>: nur ein Band um das Muster (Stufe = Bandbreite), Rest blanke Rüstung. '
             '<b>C Gravur</b>: wie A, das ausgeschnittene Muster bekommt oben/links eine Schattenkante (eingraviert). '
             '<b>Befund Ist:</b> die Inversion füllt auch die Lücken der Rüstung (z. B. die Gesichtsöffnung des Helms, Ränder an Armen/Beinen), '
             'weil die Besatz-Ebene auf demselben Modell liegt – A–C füllen nur, wo die Rüstung selbst Pixel hat (im Client noch zu bestätigen). '
             'Hinweis Umsetzung: A–C brauchen beim Bau der Besatz-Ebene die Rüstungstextur (Maske + Helligkeit).</p>')
    for pat, mat in [('coast', 'amethyst'), ('sentry', 'gold'), ('wild', 'redstone')]:
        h.append(f'<h3>{pat} · {mat} (Stufe 2)</h3><div class="row wrap">')
        h.append(cell(bz[(pat, mat, 'vanilla', 0)], 'Vanilla-Besatz (normal)', s=6, hard=False))
        for v in ('ist', 'A', 'B', 'C'):
            h.append(cell(bz[(pat, mat, v, 2)], 'Ist' if v == 'ist' else v, s=6))
        h.append('</div>')
    h.append('<h3>Stufen 1 · 2 · 3 (coast · amethyst)</h3>')
    for v in ('ist', 'A', 'B', 'C'):
        h.append(f'<div class="row wrap"><div class="cell"><b>{"Ist" if v == "ist" else v}</b></div>')
        for st in (1, 2, 3):
            h.append(cell(bz[('coast', 'amethyst', v, st)], f'Stufe {st}', s=5))
        h.append('</div>')
    h.append(choose('besatz', ['A', 'B', 'C', 'ist', '?']))

    # simple models
    h.append('<h2>5 · Simple Models – Beispiele (Vorschlag)</h2><p class="note">Ist: genau ein Beispiel (<code>examples/catalogue/tomato.json</code>), '
             'das nur das Vanilla-Apfelmodell nutzt – man sieht also keinen Unterschied. Vorschlag (Umsetzung nach Freigabe, eigener Branch):</p>'
             '<table><tr><th>ID</th><th>Beispiel</th><th>zeigt</th></tr>'
             '<tr><td>S1</td><td>Tomate mit <b>eigener</b> 16×16-Textur (statt Apfelmodell)</td><td>vollständiges Paket: items/ + models/ + textures/</td></tr>'
             '<tr><td>S2</td><td>Schwert-Skin „Kristallklinge“ auf Diamantschwert</td><td>Werkzeug, handheld-Modell, Haltbarkeit/Werte bleiben</td></tr>'
             '<tr><td>S3</td><td>Fass → „Vorratskiste“ (Block-Item mit Blockmodell)</td><td>3D-Item-Modell (parent block), Inventar-Ansicht</td></tr>'
             '<tr><td>S4</td><td>Zwei Modelle für dasselbe Item (Apfel → Tomate, Apfel → Birne)</td><td>Auswahl im Modellbrowser, Suche/Tags/Autor, Filter „Item“</td></tr>'
             '<tr><td>S5</td><td>Beispiel mit Umlaut-Namen (match_name „Brötchen“)</td><td>Namensabgleich am Amboss, Umlaute</td></tr>'
             '<tr><td>S6</td><td>Fertiges Beispiel-Ressourcenpaket als Zip + Handbuchseite „Beispiele“ (EN/DE) + Gametest „Beispielkatalog lädt“</td>'
             '<td>Server-Admin kann alles in einem Schritt testen; Tests sichern das Format</td></tr></table>'
             '<p class="note">Antwort z. B. <code>models=S1,S3,S6</code> oder <code>models=alle</code> im Freitext unten.</p>')
    h.append('<label>Freitext / models=: <input id="free" style="width:100%;max-width:520px" placeholder="z. B. models=alle; platten: B, aber Etikett heller"></label>')

    h.append('<h2>Antwortcode</h2><pre id="code">IG3114|music=?|daemmerholz=?|platten=?|besatz=?</pre>'
             '<button id="copy" type="button">Code kopieren</button>')
    h.append('<script>\n' + JS + '\n</script></main></body></html>')
    return ''.join(h)


JS = """(function () {
  var keys = ['music', 'daemmerholz', 'platten', 'besatz'];
  function code() {
    var parts = ['IG3114'];
    keys.forEach(function (k) {
      var el = document.querySelector('input[name="' + k + '"]:checked');
      parts.push(k + '=' + (el ? el.value : '?'));
    });
    var free = document.getElementById('free').value.trim();
    if (free) parts.push('note=' + free.replace(/\\|/g, '/'));
    return parts.join('|');
  }
  function update() { document.getElementById('code').textContent = code(); }
  document.addEventListener('change', update);
  document.getElementById('free').addEventListener('input', update);
  document.getElementById('copy').addEventListener('click', function () {
    var t = code();
    if (navigator.clipboard) { navigator.clipboard.writeText(t).catch(function () {}); }
  });
  update();
})();"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--jar', default=os.environ.get('SB_CLIENT_JAR') or JAR_DEFAULT)
    ap.add_argument('--preview')
    a = ap.parse_args()
    jar = Jar(a.jar)
    pl, mu, dh = platten(), music(), daemmerholz()
    report = {f'{m}:{v}:{n}': round(hard_share(im) * 100, 1) for m, d in (('platten', pl), ('music', mu), ('daemmerholz', dh)) for (v, n), im in d.items()}
    print(json.dumps(report, indent=0)[:4000])
    if a.preview:
        if not jar.z:
            sys.exit('client jar not found: ' + str(a.jar))
        bz = besatz(jar)
        html = page(jar, pl, mu, dh, bz)
        Path(a.preview).parent.mkdir(parents=True, exist_ok=True)
        Path(a.preview).write_text(html, encoding='utf-8')
        print('preview', a.preview, len(html.encode()) // 1024, 'KiB')


if __name__ == '__main__':
    main()
