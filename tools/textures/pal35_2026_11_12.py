"""PAL35 (a-pal35a, owner 11./12.11., N30/N35): lamp, lantern, chain and pole of the eight Simple Ores building metals, poles of the
two rose alloys.

Own pixel art (PRINZIPIEN A8 b/c/d, committed with its layer source), built from the metal's own parts so every metal reads as itself:
  tones      the METALS14 ramp of the metal (metals14.SPANS[6], 0 = brightest ... 9 = darkest); rose gold / rose copper: their ramps
  lamp       copper bulb layout (bright top/left edge, plate, light cut-outs): the cut-outs are the metal's own grate pattern (METALS25),
             unlit = the dark inside, lit = warm light brightest in the middle, powered = the red dot of the Vanilla bulb
  lantern    the Vanilla lantern sheet layout (body 6x7, top 6x6, cap 4x2, handle): each metal has its own pane bars (PANES)
  chain      two crossed strips of links (Vanilla chain layout); each metal has its own link length and shape (LINKS)
  pole       lightning rod layout (head 4x4, shaft 2x12); each metal has its own head motif and shaft rings (POLES)
Files per metal: block/<m>_lamp(_lit)(_powered), block/<m>_lantern, item/<m>_lantern, block/<m>_chain, item/<m>_chain, block/<m>_pole
(+ .layers.json). Rounds (A8c): ROUNDS holds the parameter sets, ROUND is built; plan docs/ai/PLAN-A-PAL35-2026-11-12.md section 6.

    python3 tools/textures/pal35_2026_11_12.py --own             write the textures + .layers.json
    python3 tools/textures/pal35_2026_11_12.py --check           committed PNGs = generator
    python3 tools/textures/pal35_2026_11_12.py --rounds DIR      every round (1x) for the preview
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402
import metals14_2026_11_09 as m14  # noqa: E402
import ores_2026_10_30 as ores  # noqa: E402

ROOT = HERE.parents[1]
TEX = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/textures'
METALS = ('silver', 'lead', 'zinc', 'nickel', 'tin', 'brass', 'bronze', 'stainless_steel')
POLE_METALS = METALS + ('rose_gold', 'rose_bronze')
# the rose alloys are not in METALS14: their ramp (ores RAMPS soft/hard) over a span like the warm metals
ROSE = {'rose_gold': ('soft', (0.2, 6.2)), 'rose_bronze': ('hard', (0.0, 6.0))}
CLEAR = (0, 0, 0, 0)

# R1: first drawing, every part in the metal's block span (METALS14). Evaluation (plan section 6): the unlit lamp cut-outs and the lantern
# frame of the pale metals stayed light grey (the span ends at mid grey) - no "inside", the glow did not read; the grate pattern alone
# read as "grate block with light". R2: dark parts (lamp inside, lantern frame, chain walls, pole shadow side) from the metal's FULL ramp
# (deep), a raised 1-px cross like the copper bulb's, link walls and cap edge with A8d tone steps. Evaluation: lamps with small cut-outs
# (nickel dots, tin waves) lit too weakly, the cross took half their light. R3 (built): R2 + the light falls on the bars next to each
# cut-out (halo, like the copper bulb's lit rims), softer light ramp (A8b), the lantern top with a centre rivet of the metal's motif.
ROUNDS = {'R1': {'cross': False, 'deep': False, 'wall_steps': False, 'cap_edge': False, 'soft_light': False, 'rivet': False, 'halo': False},
          'R2': {'cross': True, 'deep': True, 'wall_steps': True, 'cap_edge': True, 'soft_light': False, 'rivet': False, 'halo': False},
          'R3': {'cross': True, 'deep': True, 'wall_steps': True, 'cap_edge': True, 'soft_light': True, 'rivet': True, 'halo': True}}
# R4 (built): texrate of R3 - lamps 2 x rot / 18 x orange on "hart" (16-36 %, Vanilla copper bulb 19-21 %, orange too): the jump bar -> dark
# cut-out is one step. R4 = R3 + a rim one tone towards the cut-out on every bar pixel next to it (unlit: shadow, lit: light), cut-outs one
# step less dark, stronger light on the bars (A8b, hard share down to the Vanilla bulb's level); lantern top rim and cap with A8d tone
# steps (R3: 45-47 % in areas >= 6 px for brass, tin, stainless steel).
ROUNDS['R4'] = {**ROUNDS['R3'], 'rim': True}
ROUND = 'R4'


def colour_of(metal: str):
    if metal in ROSE:
        ramp_name, (lo, hi) = ROSE[metal]
        ramp = ores.RAMPS[ramp_name]
    else:
        ramp = ores.RAMPS[m14.METALS[metal]]
        lo, hi = m14.SPANS[m14.ROUND][metal]
    return lambda t: ores.ramp_at(ramp, (lo + (hi - lo) * max(0.0, min(9.0, t)) / 9.0) / 9.0) + (255,)


def full_of(metal: str, rnd: str = ROUND):
    """R2+: the metal's whole ramp (0 = brightest, 9 = darkest) for the dark parts; R1: the block span like colour_of."""
    if not ROUNDS[rnd]['deep']:
        return colour_of(metal)
    ramp = ores.RAMPS[ROSE[metal][0]] if metal in ROSE else ores.RAMPS[m14.METALS[metal]]
    return lambda t: ores.ramp_at(ramp, max(0.0, min(9.0, t)) / 9.0) + (255,)


def _mix(a, b, f: float):
    return tuple(round(a[i] + (b[i] - a[i]) * f) for i in range(3)) + (255,)


def _lum(col) -> float:
    return 0.299 * col[0] + 0.587 * col[1] + 0.114 * col[2]


def _by_lum(tone, target: float):
    """The tone of a ramp function (0..9 in quarter steps) whose luminance is closest to target."""
    return min((tone(i / 4.0) for i in range(37)), key=lambda col: abs(_lum(col) - target))


def _src(name: str) -> Image.Image:
    return Image.open(TEX / f'block/{name}.png').convert('RGBA')


def _section(i: int, a, b, n: int = 3):
    """A8d: an edge changes its tone in sections of n px."""
    return a if (i // n) % 2 == 0 else b


# ---------------------------------------------------------------- lamp
LIGHT = [(255, 251, 228, 255), (255, 236, 172, 255), (250, 208, 122, 255), (232, 170, 86, 255), (200, 132, 62, 255)]
LIGHT_SOFT = [(255, 250, 226, 255), (255, 238, 180, 255), (252, 218, 140, 255), (240, 188, 104, 255), (218, 154, 78, 255)]
RED = [(255, 84, 60, 255), (214, 24, 14, 255), (150, 4, 4, 255)]


def lamp(metal: str, lit: bool, powered: bool, rnd: str = ROUND) -> Image.Image:
    p, c = ROUNDS[rnd], colour_of(metal)
    grate = _src(f'{metal}_grate')
    im = Image.new('RGBA', (16, 16), c(4))
    layers.snap(im, 'Platte')
    for i in range(16):
        im.putpixel((i, 0), _section(i, c(1), c(2)))
        im.putpixel((0, i), _section(i + 1, c(1), c(2)))
        im.putpixel((i, 15), _section(i, c(7), c(6)))
        im.putpixel((15, i), _section(i + 1, c(6), c(7)))
    for i in range(1, 15):
        im.putpixel((i, 1), _section(i, c(3), c(2), 2))
        im.putpixel((1, i), _section(i, c(3), c(2), 2))
        im.putpixel((i, 14), _section(i, c(5), c(6), 2))
        im.putpixel((14, i), _section(i, c(5), c(6), 2))
    layers.snap(im, 'Rahmen')
    holes = set()
    for y in range(2, 14):
        for x in range(2, 14):
            g = grate.getpixel((x, y))
            if g[3] == 0:
                holes.add((x, y))
            else:
                im.putpixel((x, y), g)
    if p['cross']:
        for i in range(2, 14):
            for x, y in ((i, 7), (7, i)):
                holes.discard((x, y)); im.putpixel((x, y), c(3))
            for x, y in ((i, 8), (8, i)):
                holes.discard((x, y)); im.putpixel((x, y), c(5))
        im.putpixel((7, 7), c(2)); im.putpixel((8, 8), c(5)); im.putpixel((8, 7), c(4)); im.putpixel((7, 8), c(4))
    layers.snap(im, 'Käfig (Rostmuster)')
    ramp = LIGHT_SOFT if p['soft_light'] else LIGHT
    deep = full_of(metal, rnd)
    rim = p.get('rim', False)
    if lit and p['halo']:   # the light falls on the bars next to each cut-out; small cut-outs (nickel dots, tin waves) shine stronger
        glow = (0.35 if len(holes) >= 0.25 * 144 else 0.6) + (0.15 if rim else 0.0) + (0.15 if rim and _lum(c(4)) < 170 else 0.0)
        for y in range(2, 14):
            for x in range(2, 14):
                if (x, y) not in holes and any(n in holes for n in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1))):
                    im.putpixel((x, y), _mix(im.getpixel((x, y)), ramp[2], glow))
        layers.snap(im, 'Lichtschein auf dem Käfig')
    if rim:   # R4: the dark inside sits about 75 luminance below the plate (the Vanilla bulb's step), the shade 90
        bar = _lum(c(4))
        inside, shaded = _by_lum(deep, bar - 70), _by_lum(deep, bar - 85)
    if not lit and rim:   # the shadow rim of the dark inside on the bars next to each cut-out, half way (two soft steps, A8b)
        for y in range(2, 14):
            for x in range(2, 14):
                if (x, y) not in holes and any(n in holes for n in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1))):
                    im.putpixel((x, y), _mix(im.getpixel((x, y)), inside, 0.5))
        layers.snap(im, 'Schattenrand am Käfig')
    for (x, y) in holes:
        shade = (x - 1, y) not in holes and (x, y - 1) not in holes   # inner corner below/right of a bar: one step darker
        if lit:
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            k = 0 if d < 2.6 else 1 if d < 4.0 else 2 if d < 5.4 else 3
            im.putpixel((x, y), ramp[min(4, k + (1 if shade else 0))])
        else:
            if rim:
                im.putpixel((x, y), shaded if shade else inside)
            else:
                im.putpixel((x, y), deep(8 if shade else 7) if p['deep'] else c(9 if shade else 8))
    layers.snap(im, 'Licht' if lit else 'Inneres')
    if powered:
        for (x, y), k in (((7, 7), 0), ((8, 7), 1), ((7, 8), 1), ((8, 8), 2)):
            im.putpixel((x, y), RED[k])
        layers.snap(im, 'Strom (Redstone-Punkt)')
    return im


# ---------------------------------------------------------------- lantern
# pane bars of the 4 x 5 window (x 1..4, y 3..7): m = metal bar, g = glow
PANES = {
    'silver': ('mggm', 'gggg', 'gggg', 'gggg', 'gggg'),             # arched window
    'lead': ('gggg', 'gggg', 'mmmm', 'gggg', 'gggg'),               # heavy cross bar
    'zinc': ('mggm', 'gmmg', 'gggg', 'gmmg', 'mggm'),               # diagonal X
    'nickel': ('mggm', 'gggg', 'gmmg', 'gggg', 'mggm'),             # rivets
    'tin': ('gmmg', 'gggg', 'mggm', 'gggg', 'gmmg'),                # waves
    'brass': ('gmmg', 'mggm', 'gmmg', 'mggm', 'gmmg'),              # lattice
    'bronze': ('mggm', 'gggg', 'gggg', 'gggg', 'mggm'),             # round porthole
    'stainless_steel': ('mggm', 'mggm', 'mggm', 'mggm', 'mggm'),    # one slot
}
# Vanilla lantern glow layout (brightness of the 4 x 5 window) -> tone index of GLOW
GLOW_MAP = ((3, 2, 2, 3), (2, 1, 1, 2), (1, 0, 0, 1), (1, 0, 0, 1), (2, 1, 1, 2))
GLOW = [(255, 250, 222, 255), (255, 224, 146, 255), (240, 186, 96, 255), (204, 138, 66, 255)]
# the top's centre motif (x 2..3, y 11..12)
TOP_MOTIF = {'silver': 0, 'lead': 8, 'zinc': 'diag', 'nickel': 1, 'tin': 'wave', 'brass': 2, 'bronze': 7, 'stainless_steel': 'slot'}


def _lantern_body(im, metal, c, p, ox, oy):
    """Body 6 x 7 at (ox, oy): frame and the metal's panes over the glow (c = the lantern's tones, R2+ deep)."""
    for y in range(7):
        for x in range(6):
            frame = x in (0, 5) or y in (0, 6)
            if frame:
                t = 3 if x == 0 or y == 0 else 6
                if x == 5 or y == 6:
                    t = 6
                if (x, y) in ((0, 0), (5, 0), (0, 6), (5, 6)):
                    t = 5
                im.putpixel((ox + x, oy + y), c(t if y != 0 else _section(x, t, t + 1, 2)))
    layers.snap(im, 'Gehäuse')
    for j, row in enumerate(PANES[metal]):
        for i, ch in enumerate(row):
            x, y = ox + 1 + i, oy + 1 + j
            if ch == 'g':
                im.putpixel((x, y), GLOW[GLOW_MAP[j][i]])
            else:
                im.putpixel((x, y), c(4 if i < 2 else 6))
    layers.snap(im, 'Scheiben und Licht')


def lantern_sheet(metal: str, rnd: str = ROUND) -> Image.Image:
    p, c = ROUNDS[rnd], (full_of(metal, rnd) if ROUNDS[rnd]['deep'] else colour_of(metal))
    fd = 0
    im = Image.new('RGBA', (16, 16), CLEAR)
    # cap sides (x 1..4, y 0..1)
    for x in range(1, 5):
        im.putpixel((x, 0), c(2 + fd // 2 if x < 4 else 4 + fd // 2))
        im.putpixel((x, 1), c(4 + fd // 2 if x < 4 else 6 + fd // 2))
    layers.snap(im, 'Kappe')
    _lantern_body(im, metal, c, p, 0, 2)
    # top / bottom 6 x 6 (x 0..5, y 9..14) with the cap top 4 x 4 (x 1..4, y 10..13)
    for y in range(9, 15):
        for x in range(6):
            edge = x in (0, 5) or y in (9, 14)
            t = (5 if x == 5 or y == 14 else 3) if edge else 4
            if edge and p.get('rim'):   # R4 (A8d): the rim changes its tone every 2 px
                t += _section(x + y, 0, 1, 2)
            im.putpixel((x, y), c(t + fd // 2))
    for y in range(10, 14):
        for x in range(1, 5):
            t = 2 if x == 1 or y == 10 else 5 if x == 4 or y == 13 else 3
            if p.get('rim') and t == 3 and (x + y) % 2:
                t = 4
            im.putpixel((x, y), c(t + fd // 2))
    layers.snap(im, 'Deckel')
    motif = TOP_MOTIF[metal]
    if p['rivet']:
        if isinstance(motif, int):
            for x, y in ((2, 11), (3, 11), (2, 12), (3, 12)):
                im.putpixel((x, y), c(motif))
        elif motif == 'diag':
            im.putpixel((2, 11), c(1)); im.putpixel((3, 12), c(7)); im.putpixel((3, 11), c(4)); im.putpixel((2, 12), c(4))
        elif motif == 'wave':
            im.putpixel((2, 11), c(2)); im.putpixel((3, 11), c(5)); im.putpixel((2, 12), c(5)); im.putpixel((3, 12), c(2))
        else:   # slot
            im.putpixel((2, 11), c(7)); im.putpixel((2, 12), c(7)); im.putpixel((3, 11), c(2)); im.putpixel((3, 12), c(2))
        layers.snap(im, 'Deckel-Niete')
    # handle (x 11..13): standing y 1..2 and 10..11, hanging y 1..4 and 6..11 (Vanilla layout)
    rows = {1: 'aab', 2: 'a.b', 3: 'a.b', 4: 'bab', 6: 'a.b', 7: 'bab', 10: 'aab', 11: 'a.b'}
    for y, row in rows.items():
        for i, ch in enumerate(row):
            if ch != '.':
                im.putpixel((11 + i, y), c((3 if ch == 'a' else 6) + fd // 2))
    layers.snap(im, 'Bügel')
    return im


def lantern_item(metal: str, rnd: str = ROUND) -> Image.Image:
    p, c = ROUNDS[rnd], (full_of(metal, rnd) if ROUNDS[rnd]['deep'] else colour_of(metal))
    fd = 0
    im = Image.new('RGBA', (16, 16), CLEAR)
    for (x, y), t in (((8, 1), 3), ((7, 2), 3), ((8, 2), 6), ((7, 3), 6), ((8, 3), 3), ((7, 4), 3)):
        im.putpixel((x, y), c(t + fd // 2))
    layers.snap(im, 'Haken')
    for x in range(6, 10):
        im.putpixel((x, 5), c((2 if x < 9 else 4) + fd // 2))
        im.putpixel((x, 6), c((4 if x < 9 else 6) + fd // 2))
    layers.snap(im, 'Kappe')
    _lantern_body(im, metal, c, p, 5, 7)
    return im


# ---------------------------------------------------------------- chain
# link height (period = 2 (h - 1), so 3, 5 or 9 tile 16 px) and shape
LINKS = {'silver': (5, 'oval'), 'lead': (3, 'heavy'), 'zinc': (5, 'twist'), 'nickel': (5, 'square'), 'tin': (5, 'wave'),
         'brass': (9, 'stud2'), 'bronze': (5, 'thick'), 'stainless_steel': (9, 'stud')}


def _link(metal, c, p, h, shape, k):
    """The pixels {(x, y): colour} of one face-on link, 3 wide and h high, the k-th along the chain (for wave/twist)."""
    out = {}
    steps = p['wall_steps']
    for y in range(h):
        end = y in (0, h - 1)
        for x in range(3):
            if not end and x == 1:
                if shape in ('stud', 'stud2') and (y == h // 2 or (shape == 'stud2' and y in (h // 3, h - 1 - h // 3))):
                    out[(x, y)] = c(4)
                elif shape == 'heavy' and not p.get('rim'):
                    out[(x, y)] = c(8)   # R1-R3: the hole of a heavy link dark; R4: open like the Vanilla chain (read as a blob)
                continue
            if shape in ('oval', 'thick') and end and x != 1 and shape == 'oval':
                continue
            if x == 0:
                t = 2 if not steps else _section(y, 2, 3, 2)
            elif x == 2:
                t = 6 if not steps else _section(y, 6, 5, 2)
            else:
                t = 3 if y == 0 else 6
            if shape == 'twist':
                t = (2 if y < h // 2 else 5) if x == 0 else (5 if y < h // 2 else 2) if x == 2 else t
            if shape == 'wave':
                t += (k % 2) * 1
            if shape == 'thick' and end and x != 1:
                t += 1
            if shape == 'heavy' and not p.get('rim'):
                t += 1
            out[(x, y)] = c(t)
    return out


def _chain_tones(metal: str, rnd: str):
    """R2+: links in the upper part of the full ramp shifted one step darker (Vanilla chains are dark, pale metals stay readable)."""
    if not ROUNDS[rnd]['deep']:
        return colour_of(metal)
    f = full_of(metal, rnd)
    return lambda t: f(t + 1)


def chain_block(metal: str, rnd: str = ROUND) -> Image.Image:
    p, c = ROUNDS[rnd], _chain_tones(metal, rnd)
    h, shape = LINKS[metal]
    period = 2 * (h - 1)
    im = Image.new('RGBA', (16, 16), CLEAR)
    k = 0
    for start in range(0, 16, period):
        for dx, off in ((0, 0), (3, h - 1)):
            for (x, y), col in _link(metal, c, p, h, shape, k).items():
                yy = (start + off + y) % 16
                im.putpixel((dx + x, yy), col)
            k += 1
    layers.snap(im, 'Glieder')
    return im


def chain_item(metal: str, rnd: str = ROUND) -> Image.Image:
    p, c = ROUNDS[rnd], _chain_tones(metal, rnd)
    h, shape = LINKS[metal]
    im = Image.new('RGBA', (16, 16), CLEAR)
    y0, k = 1, 0
    while y0 < 15:
        if k % 2 == 0:
            for (x, y), col in _link(metal, c, p, h, shape, k).items():
                if y0 + y < 15:
                    im.putpixel((6 + x, y0 + y), col)
        else:
            for y in range(h):
                if y0 + y < 15 and (y0 + y, 7) and (y not in (0, h - 1) or im.getpixel((7, y0 + y))[3] == 0):
                    im.putpixel((7, y0 + y), c(_section(y, 3, 5, 2) if p['wall_steps'] else 4))
        y0 += h - 1
        k += 1
    layers.snap(im, 'Glieder')
    return im


# ---------------------------------------------------------------- pole
# head motif in the 4 x 4 head (x 0..3, y 0..3) and the shaft rings (rows of the 2 x 12 shaft, y 4..15)
POLES = {'silver': ('knob', (7, 11)), 'lead': ('socket', (5, 9, 13)), 'zinc': ('diag', 'diag'), 'nickel': ('rivets', 'dots'),
         'tin': ('wave', 'wave'), 'brass': ('cross', (9, 10)), 'bronze': ('round', (4, 8, 9)), 'stainless_steel': ('slot', 'groove'),
         'rose_gold': ('rose', 'spiral'), 'rose_bronze': ('leaf', (6, 10, 14))}


def pole(metal: str, rnd: str = ROUND) -> Image.Image:
    p, c = ROUNDS[rnd], colour_of(metal)
    head, rings = POLES[metal]
    im = Image.new('RGBA', (16, 16), CLEAR)
    for y in range(4):
        for x in range(4):
            t = 2 if x == 0 or y == 0 else 5 if x == 3 or y == 3 else 3
            im.putpixel((x, y), c(t))
    if p['cap_edge']:
        im.putpixel((0, 0), c(1)); im.putpixel((1, 0), c(1)); im.putpixel((3, 3), c(6))
    layers.snap(im, 'Kopf')
    centre = ((1, 1), (2, 1), (1, 2), (2, 2))
    motif = {
        'knob': {(1, 1): 0, (2, 1): 1, (1, 2): 1, (2, 2): 3},
        'socket': {(1, 1): 7, (2, 1): 6, (1, 2): 6, (2, 2): 5},
        'diag': {(1, 1): 1, (2, 2): 6, (2, 1): 4, (1, 2): 4},
        'rivets': {(0, 0): 0, (3, 0): 1, (0, 3): 1, (3, 3): 4, (1, 1): 4, (2, 1): 4, (1, 2): 4, (2, 2): 4},
        'wave': {(1, 1): 5, (2, 1): 2, (1, 2): 2, (2, 2): 5},
        'cross': {(1, 0): 1, (2, 0): 1, (0, 1): 1, (0, 2): 1, (3, 1): 5, (3, 2): 5, (1, 3): 5, (2, 3): 5, (1, 1): 2, (2, 1): 3, (1, 2): 3, (2, 2): 4},
        'round': {(0, 0): 4, (3, 0): 5, (0, 3): 5, (3, 3): 7, (1, 1): 1, (2, 1): 2, (1, 2): 2, (2, 2): 3},
        'slot': {(1, 1): 6, (2, 1): 6, (1, 2): 2, (2, 2): 3},
        'rose': {(1, 1): 0, (2, 1): 1, (1, 2): 1, (2, 2): 2, (1, 0): 4, (2, 0): 4, (0, 1): 4, (0, 2): 4, (3, 1): 6, (3, 2): 6, (1, 3): 6, (2, 3): 6},
        'leaf': {(1, 1): 5, (2, 1): 4, (1, 2): 4, (2, 2): 6, (0, 0): 1, (3, 0): 2, (0, 3): 2, (3, 3): 6},
    }[head]
    for (x, y), t in motif.items():
        im.putpixel((x, y), c(t))
    layers.snap(im, 'Kopf-Motiv')
    deep = full_of(metal, rnd)
    for y in range(4, 16):
        left, right = _section(y, 2, 3, 3), _section(y + 1, 5, 6, 3)
        im.putpixel((0, y), c(left)); im.putpixel((1, y), deep(right + 1) if p['deep'] else c(right))
    layers.snap(im, 'Schaft')
    if isinstance(rings, tuple):
        for y in rings:
            im.putpixel((0, y), c(1)); im.putpixel((1, y), c(3))
            if y + 1 < 16 and y + 1 not in rings:
                im.putpixel((0, y + 1), c(4)); im.putpixel((1, y + 1), c(7))
    elif rings == 'diag':
        for y in range(5, 16, 2):
            im.putpixel(((y // 2) % 2, y), c(1))
    elif rings == 'dots':
        for y in range(6, 16, 3):
            im.putpixel((0, y), c(0))
    elif rings == 'wave':
        for y in range(4, 16):
            w = (0, 1, 2, 1)[y % 4]
            im.putpixel((0, y), c(2 + w)); im.putpixel((1, y), c(6 - w))
    elif rings == 'groove':
        for y in range(4, 16):
            im.putpixel((1, y), c(_section(y, 6, 7, 3)))
        im.putpixel((0, 6), c(0)); im.putpixel((0, 12), c(0))
    elif rings == 'spiral':
        for y in range(5, 16, 3):
            im.putpixel((0, y), c(1)); im.putpixel((1, y + 1 if y + 1 < 16 else y), c(3))
    layers.snap(im, 'Schaft-Ringe')
    return im


# ---------------------------------------------------------------- output
def own(rnd: str = ROUND) -> dict:
    out = {}
    for m in METALS:
        for lit in (False, True):
            for powered in (False, True):
                name = f'{m}_lamp' + ('_lit' if lit else '') + ('_powered' if powered else '')
                out[TEX / f'block/{name}.png'] = lamp(m, lit, powered, rnd)
        out[TEX / f'block/{m}_lantern.png'] = lantern_sheet(m, rnd)
        out[TEX / f'item/{m}_lantern.png'] = lantern_item(m, rnd)
        out[TEX / f'block/{m}_chain.png'] = chain_block(m, rnd)
        out[TEX / f'item/{m}_chain.png'] = chain_item(m, rnd)
    for m in POLE_METALS:
        out[TEX / f'block/{m}_pole.png'] = pole(m, rnd)
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('--own', action='store_true')
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--rounds', type=Path)
    a = ap.parse_args()
    if a.rounds:
        a.rounds.mkdir(parents=True, exist_ok=True)
        for rnd in ROUNDS:
            for path, im in own(rnd).items():
                im.save(a.rounds / f'{rnd}_{path.parent.name}_{path.name}')
        return 0
    if a.check:
        bad = [str(p.relative_to(ROOT)) for p, im in own().items()
               if not p.exists() or Image.open(p).convert('RGBA').tobytes() != im.convert('RGBA').tobytes()]
        print('\n'.join(bad) or 'pal35: all lamp/lantern/chain/pole textures match')
        return 1 if bad else 0
    if a.own:
        for path, im in own().items():
            path.parent.mkdir(parents=True, exist_ok=True)
            layers.save(im, path)
        print(f'pal35: {len(own())} textures + layers written')
    return 0


if __name__ == '__main__':
    sys.exit(main())
