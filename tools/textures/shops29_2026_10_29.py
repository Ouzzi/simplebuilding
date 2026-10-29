"""Simple Shops look, candidates A/B/C (WP-14 / BS25-15, Besitzer 27.10.: Sales Block, Sales Storage, Trade Terminal gefallen nicht).

Own pixels in named layers (PRINZIPIEN A8), soft ramps (no pure white/black, small steps), light from the top left.
Nothing is shipped by default; the candidates only go to the preview page until the owner picks.

Sales Block   A "Marktstand"   red/cream awning roof, oak stall boards, counter window with coins
              B "Tresen"       light oak counter, framed panels, brass register plate with a green display, brass tray on top
              C "Stahlkasse"   riveted steel plates with gold trim, key pad + green screen, coin slot on top
Sales Storage A "Muenzfass"    spruce staves, iron hoops as cross braces, round iron-ringed mouth, banded lid
              B "Sparkiste"    oak crate, iron straps + corner brackets (cross braces), slot mouth, keyhole lid
              C "Schacht"      dark steel, gold chevrons pointing along the flow, gold-ringed funnel mouth, hatch with wheel
Trade Terminal A "Boersentafel" slate board with gold price bars and coins in an oak frame, ledger sheet on top
               B "Hauptbuch"    oak desk, drawer fronts with brass handles, green felt top with open ledger and coin stacks
               C "Smaragdkontor" dark wood with iron bands, big emerald in a gold frame, emerald seal on top

The storage side texture must work on all four long faces (turned by the model), so every frame/brace is drawn on all four edges.
Display case top frame and the Sales Storage cross braces of the current look are fixed directly (models + misctex23b).

    python3 tools/textures/shops29_2026_10_29.py --page /root/sb-tools-a/previews/a-shops29-preview.html
    python3 tools/textures/shops29_2026_10_29.py --install A     (after the owner's pick; letter for all three blocks, or A,B,C per block)
    python3 tools/textures/shops29_2026_10_29.py --check X       (checks the installed set against the letter)
"""
import base64
import html
import io
import json
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import layers as L  # noqa: E402
from simple_shops_storage_2026_10_22 import Layer  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
ASSETS = os.path.join(ROOT, 'modules', 'simpleshops', 'shared', 'resources', 'assets', 'simpleshops')
TEX = os.path.join(ASSETS, 'textures', 'block')


def H(s):
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


OAK = [H('52381f'), H('684a2b'), H('7f5c37'), H('966f42'), H('ab8350'), H('bf9860')]
SPR = [H('2f2013'), H('3f2b19'), H('503822'), H('634629'), H('76563a'), H('8a6846')]
IRN = [H('4c4e54'), H('5f626a'), H('73767e'), H('878a92'), H('9a9ea5'), H('abafb5')]
GLD = [H('8a6424'), H('a77d32'), H('c49a46'), H('dab85e'), H('e8cc7a')]
RED = [H('6e2824'), H('8e342e'), H('ab433a'), H('c45a4c')]
CRM = [H('b9ae92'), H('d2c8ac'), H('e4dcc4')]
SLT = [H('222e35'), H('2c3b43'), H('384a53'), H('46606b')]
FLT = [H('1f3a2c'), H('2a4d39'), H('376349'), H('467a58')]
EME = [H('0f4a2a'), H('1a6e3e'), H('2b9455'), H('4fc17c'), H('9fe8bd')]
PAP = [H('b8ab8a'), H('d6ccaf'), H('e8e0c8')]
VOID = [H('1c1410'), H('281d15'), H('35271b')]


def fill(l, x0, y0, x1, y1, c):
    """Box x0..x1-1, y0..y1-1."""
    for y in range(y0, y1):
        for x in range(x0, x1):
            l.put(x, y, c)


def bevel(l, x0, y0, x1, y1, lit, dark):
    """Frame of the box (x1/y1 exclusive): lit top/left, dark bottom/right."""
    for x in range(x0, x1):
        l.put(x, y0, lit)
        l.put(x, y1 - 1, dark)
    for y in range(y0, y1):
        l.put(x0, y, lit)
        l.put(x1 - 1, y, dark)
    l.put(x1 - 1, y0, lit)
    l.put(x0, y1 - 1, dark)


def planks(l, pal, seed, vertical=False, board=4, x0=0, y0=0, x1=16, y1=16):
    """Boards with a lit edge, a dark seam, sparse grain and staggered joints (one ramp step per transition)."""
    rnd = random.Random(seed)
    for y in range(y0, y1):
        for x in range(x0, x1):
            u, v = (x, y) if vertical else (y, x)
            k = u % board
            off = (u // board) * 5 + seed
            if k == board - 1:
                c = pal[1]
            elif k == 0:
                c = pal[4]
            else:
                c = pal[3]
                if (v + off) % 9 in (2, 3) and (u // board + k) % 2 == 0:
                    c = pal[2]
                if rnd.random() < 0.05:
                    c = pal[2]
            if (v + off) % 16 == 11 and 0 < k < board - 1:
                c = pal[1] if k == 1 else pal[2]
            l.put(x, y, c)


def rivet(l, x, y, lit=IRN[4], dark=IRN[1]):
    l.put(x, y, lit)
    l.put(x + 1, y, dark)


def coin(l, x, y):
    l.put(x, y, GLD[4])
    l.put(x + 1, y, GLD[3])
    l.put(x, y + 1, GLD[2])
    l.put(x + 1, y + 1, GLD[1])


def grid(l, rows, key, ox=0, oy=0):
    """Draw a char grid ('.' = untouched) with key {char: colour}."""
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch in key:
                l.put(ox + dx, oy + dy, key[ch])


def steel(l, x0=0, y0=0, x1=16, y1=16, seed=0):
    """Brushed steel plate: a soft base with faint horizontal streaks."""
    rnd = random.Random(seed)
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = IRN[2]
            if (x * 3 + y * 7 + seed) % 11 == 0 or rnd.random() < 0.02:
                c = IRN[1]
            elif (x + y * 2 + seed) % 13 == 0:
                c = IRN[3]
            l.put(x, y, c)


def make(*names):
    return [Layer(n) for n in names]


# ------------------------------------------------------------------ Sales Block

def sb_A_side(front=False):
    wood, awn, det = make('Holz', 'Markise', 'Details')
    planks(wood, OAK, 3, vertical=True)
    # Corner posts (darker oak, 2 px) at both edges.
    for y in range(16):
        for x in (0, 1):
            wood.put(x, y, OAK[4] if x == 0 else OAK[3])
        for x in (14, 15):
            wood.put(x, y, OAK[1] if x == 15 else OAK[2])
    # Awning valance: red/cream stripes (2 px wide), scalloped lower edge.
    for x in range(16):
        stripe = (x // 2) % 2 == 0
        top, mid, low = (RED[3], RED[2], RED[1]) if stripe else (CRM[2], CRM[1], CRM[0])
        awn.put(x, 0, top)
        awn.put(x, 1, mid)
        awn.put(x, 2, mid)
        awn.put(x, 3, low)
    # Scalloped lower edge: the fifth row only under every second pair of stripe columns.
    for x in range(16):
        if (x % 4) in (0, 1):
            awn.put(x, 4, RED[0] if (x // 2) % 2 == 0 else CRM[0])
    if front:
        fill(det, 2, 6, 14, 11, SPR[2])
        bevel(det, 2, 6, 14, 11, OAK[1], OAK[5])
        fill(det, 3, 7, 13, 10, SPR[1])
        for x in range(3, 13):
            det.put(x, 7, SPR[0])
        # Two coin stacks on the shelf.
        for x, h in ((4, 2), (5, 1), (9, 1), (10, 2), (11, 1)):
            for k in range(h):
                det.put(x, 9 - k, GLD[3] if k == h - 1 else GLD[1])
        fill(det, 2, 11, 14, 13, OAK[5])
        for x in range(2, 14):
            det.put(x, 12, OAK[2])
        # Brass coin plate with a slot under the counter.
        fill(det, 6, 13, 10, 15, GLD[2])
        bevel(det, 6, 13, 10, 15, GLD[4], GLD[0])
        det.put(7, 14, VOID[0])
        det.put(8, 14, VOID[0])
    return [wood, awn, det]


def sb_A_top():
    roof, det = make('Dach', 'Details')
    for y in range(16):
        for x in range(16):
            stripe = (x // 2) % 2 == 0
            c = RED[2] if stripe else CRM[1]
            if y in (0, 1):
                c = RED[3] if stripe else CRM[2]
            if y in (14, 15):
                c = RED[1] if stripe else CRM[0]
            if y % 8 == 4:
                c = RED[3] if stripe else CRM[2]   # soft fold lines
            roof.put(x, y, c)
    fill(det, 0, 0, 16, 1, OAK[4])
    fill(det, 0, 15, 16, 16, OAK[1])
    for y in range(16):
        det.put(0, y, OAK[4])
        det.put(15, y, OAK[1])
    det.put(7, 7, GLD[3])
    det.put(8, 7, GLD[2])
    det.put(7, 8, GLD[2])
    det.put(8, 8, GLD[1])
    return [roof, det]


def sb_B_side(front=False):
    wood, panel, det = make('Holz', 'Fuellungen', 'Details')
    planks(wood, OAK, 5, vertical=False, board=4)
    # Frame (rails and stiles), framed recessed panels.
    fill(panel, 0, 0, 16, 16, OAK[3])
    for x in range(16):
        panel.put(x, 0, OAK[5])
        panel.put(x, 1, OAK[4])
        panel.put(x, 15, OAK[1])
        panel.put(x, 14, OAK[2])
    for y in range(16):
        panel.put(0, y, OAK[5])
        panel.put(1, y, OAK[4])
        panel.put(15, y, OAK[1])
        panel.put(14, y, OAK[2])
    for (x0, x1) in ((3, 8), (8, 13)):
        for y in range(3, 13):
            for x in range(x0 + (1 if x0 == 8 else 0), x1 - (1 if x1 == 8 else 0)):
                panel.put(x, y, OAK[2])
        bevel(panel, x0 + (1 if x0 == 8 else 0), 3, x1 - (1 if x1 == 8 else 0), 13, OAK[1], OAK[4])
    if front:
        panel_clear = Layer('x')
        fill(det, 4, 4, 12, 12, OAK[2])
        # Brass register plate with a green price display and a coin slot.
        fill(det, 3, 3, 13, 13, GLD[2])
        bevel(det, 3, 3, 13, 13, GLD[4], GLD[0])
        fill(det, 5, 5, 11, 8, FLT[0])
        bevel(det, 5, 5, 11, 8, FLT[1], FLT[2])
        for x in (6, 8, 9):
            det.put(x, 6, FLT[3])
        fill(det, 5, 10, 11, 11, VOID[0])
        for x in range(5, 11):
            det.put(x, 11, GLD[3])
    else:
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            det.put(x, y, GLD[3])
    return [wood, panel, det]


def sb_B_top():
    wood, det = make('Holz', 'Messingschale')
    planks(wood, OAK, 7, vertical=False, board=4)
    bevel(wood, 0, 0, 16, 16, OAK[5], OAK[1])
    fill(det, 3, 3, 13, 13, GLD[1])
    bevel(det, 3, 3, 13, 13, GLD[4], GLD[0])
    fill(det, 4, 4, 12, 12, GLD[2])
    for y in range(4, 12):
        for x in range(4, 12):
            if (x + y) % 5 == 0:
                det.put(x, y, GLD[1])
    fill(det, 6, 7, 10, 9, VOID[0])
    det.put(6, 7, VOID[1])
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        det.put(x, y, GLD[4])
    return [wood, det]


def sb_C_side(front=False):
    plate, rim, det = make('Stahl', 'Rahmen', 'Details')
    steel(plate, seed=2)
    for i in range(16):
        rim.put(i, 0, IRN[5] if i % 8 else IRN[4])
        rim.put(0, i, IRN[5] if i % 8 else IRN[4])
        rim.put(i, 15, IRN[0])
        rim.put(15, i, IRN[0])
        rim.put(i, 1, IRN[4])
        rim.put(1, i, IRN[4])
        rim.put(i, 14, IRN[1])
        rim.put(14, i, IRN[1])
    # Gold bands top and bottom.
    for x in range(2, 14):
        det.put(x, 3, GLD[3])
        det.put(x, 4, GLD[1])
        det.put(x, 12, GLD[3])
    for x, y in ((3, 2), (12, 2), (3, 13), (12, 13)):
        det.put(x, y, IRN[5])
    if front:
        fill(det, 3, 5, 13, 8, FLT[0])
        bevel(det, 3, 5, 13, 8, IRN[1], IRN[4])
        for x in (4, 5, 7, 8, 11):
            det.put(x, 6, FLT[3])
        for y in (9, 10, 11):
            for x in (4, 7, 10):
                det.put(x, y, GLD[3] if y == 9 else GLD[2] if y == 10 else GLD[1])
                det.put(x + 1, y, GLD[2] if y == 9 else GLD[1] if y == 10 else GLD[0])
    return [plate, rim, det]


def sb_C_top():
    plate, rim, det = make('Stahl', 'Rahmen', 'Muenzschlitz')
    steel(plate, 2, 2, 14, 14, seed=4)
    for i in range(16):
        rim.put(i, 0, IRN[5])
        rim.put(0, i, IRN[5])
        rim.put(i, 15, IRN[0])
        rim.put(15, i, IRN[0])
    for i in range(1, 15):
        rim.put(i, 1, IRN[4])
        rim.put(1, i, IRN[4])
        rim.put(i, 14, IRN[1])
        rim.put(14, i, IRN[1])
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        rivet(det, x, y) if x < 8 else (det.put(x, y, IRN[4]), det.put(x - 1, y, IRN[1]))
    fill(det, 5, 6, 11, 10, GLD[1])
    bevel(det, 5, 6, 11, 10, GLD[4], GLD[0])
    fill(det, 6, 7, 10, 9, VOID[0])
    det.put(6, 7, VOID[1])
    return [plate, rim, det]


# ------------------------------------------------------------------ Sales Storage
# Side texture: row 0 is at the sales block, the flow runs towards row 15; frame on all four edges.

def ss_A_side():
    wood, hoop, det = make('Dauben', 'Reifen', 'Details')
    planks(wood, SPR, 11, vertical=True, board=3)
    for y0 in (2, 12):
        for x in range(16):
            hoop.put(x, y0, IRN[4] if x not in (0, 15) else IRN[3])
            hoop.put(x, y0 + 1, IRN[2] if x not in (0, 15) else IRN[1])
        for x in (3, 12):
            hoop.put(x, y0, IRN[5])
    for y in range(16):
        hoop.put(0, y, SPR[5] if y not in (2, 3, 12, 13) else IRN[4])
        hoop.put(15, y, SPR[1] if y not in (2, 3, 12, 13) else IRN[1])
    # A coin glint on the staves between the hoops (flow marker).
    for y in (6, 9):
        coin(det, 7, y)
    return [wood, hoop, det]


def ss_A_intake():
    wood, ring, mouth, det = make('Dauben', 'Eisenring', 'Oeffnung', 'Muenzen')
    planks(wood, SPR, 12, vertical=False, board=4)
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            lit = x + y < 15
            if r > 7.3:
                continue
            if r > 6.2:
                ring.put(x, y, IRN[4] if lit else IRN[1])
            elif r > 5.2:
                ring.put(x, y, IRN[3] if lit else IRN[2])
            elif r > 4.2:
                mouth.put(x, y, SPR[0] if lit else VOID[0])
            else:
                mouth.put(x, y, VOID[0] if r < 3 else VOID[1])
    coin(det, 6, 7)
    coin(det, 8, 8)
    return [wood, ring, mouth, det]


def ss_A_back():
    wood, band, lock = make('Deckel', 'Baender', 'Schloss')
    planks(wood, SPR, 13, vertical=False, board=4)
    for i in range(16):
        band.put(i, 0, IRN[4])
        band.put(0, i, IRN[4])
        band.put(i, 15, IRN[1])
        band.put(15, i, IRN[1])
    for x in range(16):
        band.put(x, 7, IRN[3])
        band.put(x, 8, IRN[1])
    for y in range(16):
        band.put(7, y, IRN[3])
        band.put(8, y, IRN[1])
    fill(lock, 6, 6, 10, 10, GLD[2])
    bevel(lock, 6, 6, 10, 10, GLD[4], GLD[0])
    lock.put(7, 8, VOID[0])
    lock.put(8, 8, VOID[0])
    return [wood, band, lock]


def ss_B_side():
    wood, strap, det = make('Bretter', 'Metallbaender', 'Details')
    planks(wood, OAK, 21, vertical=True, board=4)
    # Two straps across the flow plus an edge frame on every side (cross braces on top, bottom and both flanks).
    for y0 in (3, 11):
        for x in range(16):
            strap.put(x, y0, IRN[4])
            strap.put(x, y0 + 1, IRN[3])
            strap.put(x, y0 + 2, IRN[1])
    for i in range(16):
        strap.put(i, 0, IRN[4])
        strap.put(0, i, IRN[4])
        strap.put(i, 15, IRN[1])
        strap.put(15, i, IRN[1])
    for y0 in (3, 11):
        for x in (2, 13):
            det.put(x, y0 + 1, GLD[3])
    # Money runs through a shallow chute between the straps.
    for y in range(6, 11):
        for x in (7, 8):
            det.put(x, y, VOID[1] if y % 2 else VOID[2])
    coin(det, 7, 7)
    return [wood, strap, det]


def ss_B_intake():
    wood, frame, slot, det = make('Bretter', 'Rahmen', 'Schlitz', 'Details')
    planks(wood, OAK, 22, vertical=False, board=4)
    for y in range(16):
        for x in range(16):
            if min(x, y, 15 - x, 15 - y) < 2:
                frame.put(x, y, IRN[4] if x + y < 15 else IRN[2])
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        frame.put(x, y, IRN[5])
    fill(slot, 3, 5, 13, 11, VOID[0])
    bevel(slot, 3, 5, 13, 11, SPR[2], SPR[4])
    for x in range(4, 12):
        slot.put(x, 6, VOID[1])
    for x in (2, 13):
        for y in (2, 13):
            det.put(x, y, GLD[3])
    coin(det, 6, 8)
    coin(det, 8, 7)
    return [wood, frame, slot, det]


def ss_B_back():
    wood, strap, lock = make('Bretter', 'Metallbaender', 'Schloss')
    planks(wood, OAK, 23, vertical=False, board=4)
    for i in range(16):
        strap.put(i, 0, IRN[4])
        strap.put(0, i, IRN[4])
        strap.put(i, 15, IRN[1])
        strap.put(15, i, IRN[1])
    for y0 in (3, 11):
        for x in range(16):
            strap.put(x, y0, IRN[4])
            strap.put(x, y0 + 1, IRN[3])
            strap.put(x, y0 + 2, IRN[1])
    fill(lock, 6, 5, 10, 11, GLD[2])
    bevel(lock, 6, 5, 10, 11, GLD[4], GLD[0])
    lock.put(7, 7, VOID[0])
    lock.put(8, 7, VOID[0])
    lock.put(7, 8, VOID[0])
    lock.put(7, 9, VOID[1])
    return [wood, strap, lock]


def ss_C_side():
    plate, rim, chev = make('Stahl', 'Rahmen', 'Pfeile')
    steel(plate, seed=6)
    for i in range(16):
        rim.put(i, 0, IRN[5])
        rim.put(0, i, IRN[5])
        rim.put(i, 15, IRN[0])
        rim.put(15, i, IRN[0])
        rim.put(i, 1, IRN[4])
        rim.put(1, i, IRN[4])
        rim.put(i, 14, IRN[1])
        rim.put(14, i, IRN[1])
    # Gold chevrons pointing along the flow (down to the back), fading towards the back.
    v = ['##....##', '.##..##.', '..####..']
    for i, y0 in enumerate((2, 6, 10)):
        for dy, row in enumerate(v):
            for dx, ch in enumerate(row):
                if ch == '#':
                    chev.put(4 + dx, y0 + dy, (GLD[3], GLD[2], GLD[1])[i] if dx < 4 else (GLD[2], GLD[1], GLD[0])[i])
    return [plate, rim, chev]


def ss_C_intake():
    plate, rim, ring, mouth = make('Stahl', 'Rahmen', 'Goldring', 'Trichter')
    steel(plate, seed=7)
    for i in range(16):
        rim.put(i, 0, IRN[5])
        rim.put(0, i, IRN[5])
        rim.put(i, 15, IRN[0])
        rim.put(15, i, IRN[0])
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            lit = x + y < 15
            if r > 6.6:
                continue
            if r > 5.6:
                ring.put(x, y, GLD[4] if lit else GLD[1])
            elif r > 4.6:
                ring.put(x, y, GLD[2] if lit else GLD[0])
            elif r > 3.0:
                mouth.put(x, y, SLT[1] if lit else SLT[0])
            else:
                mouth.put(x, y, VOID[0])
    return [plate, rim, ring, mouth]


def ss_C_back():
    plate, rim, hatch, wheel = make('Stahl', 'Rahmen', 'Luke', 'Rad')
    steel(plate, seed=8)
    for i in range(16):
        rim.put(i, 0, IRN[5])
        rim.put(0, i, IRN[5])
        rim.put(i, 15, IRN[0])
        rim.put(15, i, IRN[0])
    fill(hatch, 2, 2, 14, 14, IRN[2])
    bevel(hatch, 2, 2, 14, 14, IRN[4], IRN[0])
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        hatch.put(x, y, IRN[5])
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 2.2 < r < 4.2:
                wheel.put(x, y, GLD[3] if x + y < 15 else GLD[1])
    fill(wheel, 7, 7, 9, 9, GLD[2])
    return [plate, rim, hatch, wheel]


# ------------------------------------------------------------------ Trade Terminal

def tt_A_side():
    wood, frame, det = make('Holz', 'Rahmen', 'Details')
    planks(wood, OAK, 31, vertical=True)
    for i in range(16):
        frame.put(i, 0, OAK[5])
        frame.put(0, i, OAK[5])
        frame.put(i, 15, OAK[0])
        frame.put(15, i, OAK[0])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        det.put(x, y, GLD[3])
    return [wood, frame, det]


def tt_A_front():
    wood, board, det = make('Holz', 'Tafel', 'Kurse')
    planks(wood, OAK, 32, vertical=True)
    fill(board, 2, 2, 14, 13, SLT[1])
    bevel(board, 2, 2, 14, 13, SPR[1], OAK[5])
    bevel(board, 3, 3, 13, 12, SLT[0], SLT[3])
    for y in range(4, 11):
        for x in range(4, 12):
            board.put(x, y, SLT[1] if (x + y) % 7 else SLT[2])
    # Gold price bars rising to the right, a coin top left, baseline underneath.
    for i, h in enumerate((2, 3, 5, 6)):
        x = 6 + i * 2
        for y in range(10 - h + 1, 11):
            det.put(x, y, GLD[3] if y == 10 - h + 1 else GLD[2])
            det.put(x + 1, y, GLD[2] if y == 10 - h + 1 else GLD[1])
    coin(det, 4, 4)
    for x in range(4, 12):
        det.put(x, 11, CRM[1] if x % 2 else CRM[0])
    for x in (6, 7, 8, 9):
        det.put(x, 14, GLD[2])
    return [wood, board, det]


def tt_A_top():
    wood, sheet, det = make('Holz', 'Blatt', 'Details')
    planks(wood, OAK, 33, vertical=False)
    bevel(wood, 0, 0, 16, 16, OAK[5], OAK[0])
    fill(sheet, 2, 2, 14, 14, PAP[1])
    bevel(sheet, 2, 2, 14, 14, PAP[2], PAP[0])
    for y in (5, 7, 9, 11):
        for x in range(4, 12 if y != 11 else 8):
            det.put(x, y, SPR[3] if (x + y) % 3 else SPR[4])
    coin(det, 10, 10)
    det.put(11, 3, RED[2])
    det.put(12, 3, RED[1])
    return [wood, sheet, det]


def tt_B_side():
    wood, drawer, det = make('Holz', 'Schublade', 'Griffe')
    planks(wood, OAK, 41, vertical=False)
    for i in range(16):
        drawer.put(i, 0, OAK[5])
        drawer.put(0, i, OAK[5])
        drawer.put(i, 15, OAK[0])
        drawer.put(15, i, OAK[0])
    for y0 in (2, 9):
        fill(drawer, 2, y0, 14, y0 + 5, OAK[3])
        bevel(drawer, 2, y0, 14, y0 + 5, OAK[1], OAK[5])
        for x in (7, 8):
            det.put(x, y0 + 2, GLD[3] if x == 7 else GLD[1])
    return [wood, drawer, det]


def tt_B_front():
    wood, drawer, det = make('Holz', 'Schubladen', 'Beschlag')
    planks(wood, OAK, 42, vertical=False)
    for i in range(16):
        drawer.put(i, 0, OAK[5])
        drawer.put(0, i, OAK[5])
        drawer.put(i, 15, OAK[0])
        drawer.put(15, i, OAK[0])
    # Ledger label plate on top, two drawers below with brass handles.
    fill(det, 3, 2, 13, 6, FLT[1])
    bevel(det, 3, 2, 13, 6, GLD[4], GLD[0])
    for x in (5, 6, 8, 9, 11):
        det.put(x, 4, GLD[3])
    for x0, x1 in ((2, 8), (8, 14)):
        fill(drawer, x0, 8, x1, 14, OAK[3])
        bevel(drawer, x0, 8, x1, 14, OAK[1], OAK[5])
        for x in range((x0 + x1) // 2 - 1, (x0 + x1) // 2 + 1):
            det.put(x, 10, GLD[3])
            det.put(x, 11, GLD[1])
    return [wood, drawer, det]


def tt_B_top():
    felt, book, det = make('Filz', 'Hauptbuch', 'Muenzen')
    for y in range(16):
        for x in range(16):
            felt.put(x, y, FLT[1] if (x * 3 + y * 5) % 7 else FLT[2])
    for i in range(16):
        felt.put(i, 0, OAK[5])
        felt.put(0, i, OAK[5])
        felt.put(i, 15, OAK[0])
        felt.put(15, i, OAK[0])
        felt.put(i, 1, OAK[3])
        felt.put(1, i, OAK[3])
        felt.put(i, 14, OAK[1])
        felt.put(14, i, OAK[1])
    fill(book, 3, 3, 13, 11, PAP[1])
    for y in range(3, 11):
        book.put(8, y, PAP[0])
        book.put(7, y, PAP[2] if y % 2 else PAP[1])
    bevel(book, 3, 3, 13, 11, PAP[2], PAP[0])
    book.put(8, 3, PAP[0])
    for y in (5, 7, 9):
        for x in range(4, 7):
            det.put(x, y, SPR[3])
        for x in range(9, 12):
            det.put(x, y, SPR[3]) if (x + y) % 5 else None
    coin(det, 3, 12)
    coin(det, 5, 12)
    coin(det, 11, 12)
    return [felt, book, det]


def tt_C_side():
    wood, band, det = make('Holz', 'Baender', 'Details')
    planks(wood, SPR, 51, vertical=True)
    for y0 in (2, 11):
        for x in range(16):
            band.put(x, y0, IRN[4])
            band.put(x, y0 + 1, IRN[3])
            band.put(x, y0 + 2, IRN[1])
    for i in range(16):
        band.put(0, i, SPR[5] if i not in (2, 3, 4, 11, 12, 13) else IRN[4])
        band.put(15, i, SPR[0] if i not in (2, 3, 4, 11, 12, 13) else IRN[1])
    for x in (3, 12):
        for y0 in (2, 11):
            det.put(x, y0 + 1, GLD[3])
    # Small emerald stud between the bands.
    det.put(7, 7, EME[3])
    det.put(8, 7, EME[2])
    det.put(7, 8, EME[2])
    det.put(8, 8, EME[1])
    return [wood, band, det]


def tt_C_front():
    wood, band, gem = make('Holz', 'Baender', 'Smaragd')
    planks(wood, SPR, 52, vertical=True)
    for y0 in (1, 13):
        for x in range(16):
            band.put(x, y0, IRN[4])
            band.put(x, y0 + 1, IRN[1])
    fill(gem, 3, 3, 13, 12, GLD[1])
    bevel(gem, 3, 3, 13, 12, GLD[4], GLD[0])
    # Emerald: a cut stone, light from the top left.
    rows = [
        '..####..',
        '.#++++#.',
        '#+++==+#',
        '#++==--#',
        '#+=---o#',
        '.#--oo#.',
        '..####..']
    key = {'#': EME[0], '+': EME[3], '=': EME[2], '-': EME[1], 'o': EME[0]}
    fill(gem, 4, 4, 12, 11, SLT[0])
    grid(gem, rows, key, 4, 4)
    gem.put(5, 5, EME[4])
    return [wood, band, gem]


def tt_C_top():
    wood, ring, seal = make('Holz', 'Goldring', 'Siegel')
    planks(wood, SPR, 53, vertical=False)
    bevel(wood, 0, 0, 16, 16, SPR[5], SPR[0])
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            lit = x + y < 15
            if 5.2 < r <= 6.4:
                ring.put(x, y, GLD[4] if lit else GLD[1])
            elif 4.2 < r <= 5.2:
                ring.put(x, y, GLD[2] if lit else GLD[0])
            elif r <= 4.2:
                seal.put(x, y, EME[3] if lit and r < 2.8 else EME[2] if r < 3.4 else EME[1])
    seal.put(6, 6, EME[4])
    return [wood, ring, seal]


VARIANTS = {
    'sales_block': {
        'A': {'sales_block_front': lambda: sb_A_side(True), 'sales_block_side': lambda: sb_A_side(False), 'sales_block_top': sb_A_top},
        'B': {'sales_block_front': lambda: sb_B_side(True), 'sales_block_side': lambda: sb_B_side(False), 'sales_block_top': sb_B_top},
        'C': {'sales_block_front': lambda: sb_C_side(True), 'sales_block_side': lambda: sb_C_side(False), 'sales_block_top': sb_C_top},
    },
    'sales_storage': {
        'A': {'sales_storage_side': ss_A_side, 'sales_storage_intake': ss_A_intake, 'sales_storage_back': ss_A_back},
        'B': {'sales_storage_side': ss_B_side, 'sales_storage_intake': ss_B_intake, 'sales_storage_back': ss_B_back},
        'C': {'sales_storage_side': ss_C_side, 'sales_storage_intake': ss_C_intake, 'sales_storage_back': ss_C_back},
    },
    'trade_terminal': {
        'A': {'trade_terminal_front': tt_A_front, 'trade_terminal_side': tt_A_side, 'trade_terminal_top': tt_A_top},
        'B': {'trade_terminal_front': tt_B_front, 'trade_terminal_side': tt_B_side, 'trade_terminal_top': tt_B_top},
        'C': {'trade_terminal_front': tt_C_front, 'trade_terminal_side': tt_C_side, 'trade_terminal_top': tt_C_top},
    },
}
NAMES = {'sales_block': 'Sales Block', 'sales_storage': 'Sales Storage', 'trade_terminal': 'Trade Terminal'}
RECOMMEND = {'sales_block': 'B', 'sales_storage': 'A', 'trade_terminal': 'A'}


def doc_of(lays):
    return L.from_layers(16, 16, [(lay.name, bytes(lay.px)) for lay in lays])


def pil_of(lays):
    from PIL import Image
    return Image.frombytes('RGBA', (16, 16), bytes(L.compose(doc_of(lays))))


def build(block, letter):
    return {n: f() for n, f in VARIANTS[block][letter].items()}


def install(spec):
    letters = spec.split(',')
    if len(letters) == 1:
        letters = letters * 3
    for block, letter in zip(VARIANTS, letters):
        for name, lays in build(block, letter).items():
            doc = doc_of(lays)
            path = os.path.join(TEX, name + '.png')
            with open(path, 'wb') as f:
                f.write(L.png_write(16, 16, bytes(L.compose(doc))))
            L.write_beside(path, doc)
            print('geschrieben:', name)


def check(spec):
    letters = spec.split(',')
    if len(letters) == 1:
        letters = letters * 3
    bad = 0
    for block, letter in zip(VARIANTS, letters):
        for name, lays in build(block, letter).items():
            path = os.path.join(TEX, name + '.png')
            have = bytes(L.png_read(open(path, 'rb').read())[2]) if os.path.exists(path) else b''
            if have != bytes(L.compose(doc_of(lays))):
                print('veraltet:', name)
                bad += 1
    return 1 if bad else 0


# ------------------------------------------------------------------ preview page

def load_old(name):
    from PIL import Image
    return Image.open(os.path.join(TEX, name + '.png')).convert('RGBA')


def b64(im, scale=1):
    from PIL import Image
    if scale != 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def model(n):
    return json.load(open(os.path.join(ASSETS, 'models', 'block', n + '.json')))


def texmap(m, over):
    out = {}
    for ref in m['textures'].values():
        if ref.startswith('#'):
            continue
        name = ref.split('/')[-1]
        out[ref] = over[name] if name in over else load_old(name)
    return out


def scene(over, k=7, only=None):
    import shops29_render as R
    from PIL import Image
    pos = {'sales_block': (0, 0, 0), 'sales_storage': (0, 0, 1), 'trade_terminal': (1, 0, 0), 'display_case': (1, 0, 1)}
    items = []
    for n, p in pos.items():
        if only and n not in only:
            continue
        m = model(n)
        items.append((m, texmap(m, over), p))
    img = R.blocks_to_image(items, k=k)
    bg = Image.new('RGBA', img.size, (126, 130, 140, 255))
    bg.alpha_composite(img)
    return bg


def single(block, over, k=9):
    import shops29_render as R
    from PIL import Image
    m = model(block)
    img = R.blocks_to_image([(m, texmap(m, over), (0, 0, 0))], k=k)
    bg = Image.new('RGBA', img.size, (126, 130, 140, 255))
    bg.alpha_composite(img)
    return bg


def facegrid(names, over):
    from PIL import Image
    W = Image.new('RGBA', (len(names) * (16 * 12 + 10) + 10, 16 * 12 + 20), (126, 130, 140, 255))
    for i, n in enumerate(names):
        W.alpha_composite((over.get(n) or load_old(n)).resize((192, 192), Image.NEAREST), (10 + i * 202, 10))
    return W


def ice_part(outdir):
    """OW24-4 cracked ice A/B/C (candidates of b-tex24, nothing installed): reuses its section, Vanilla ice is read from the jar
    for the page only."""
    from pathlib import Path
    import tex24_ice_heart_2026_10_24 as T
    from vanilla_derived import find_jar
    out = Path(outdir)
    out.mkdir(parents=True, exist_ok=True)
    return '<div class="ice"><style>' + T.CSS.replace('body{', '.ice{').replace('main{', '.ice main{') + '</style>' + '\n'.join(T.ice_section(find_jar(), out)) + '</div>'


def page(path):
    from PIL import Image
    cards = []
    sets = {}
    for letter in 'ABC':
        over = {}
        for block in VARIANTS:
            for n, lays in build(block, letter).items():
                over[n] = pil_of(lays)
        sets[letter] = over
    old = {}
    css = ('body{margin:0;padding:16px;background:#16181d;color:#e6e6ea;font:15px/1.5 system-ui,sans-serif}'
           'h1{font-size:22px}h2{font-size:18px;margin:28px 0 6px}h3{font-size:15px;margin:0 0 6px}'
           '.note{color:#b4b8c2;max-width:70ch}.grid{display:flex;flex-wrap:wrap;gap:14px}'
           '.card{background:#20232b;border:1px solid #333845;border-radius:8px;padding:10px}'
           '.card img{image-rendering:pixelated;max-width:100%;display:block;margin-bottom:6px}'
           'b.rec{color:#8fd694}.tag{font-size:12px;color:#c9a24a}')
    out = [f'<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
           f'<title>Shops Optik</title><style>{css}</style></head><body>',
           '<h1>Simple Shops: neue Optik (WP-14, BS25-15)</h1>',
           '<p class="note">Je Block drei Richtungen A/B/C neben dem alten Stand, Flaechen 12x, Iso-Ansicht (Vorderseite links, Seite rechts, Oberseite). '
           'Nichts davon ist eingebaut. Bitte pro Block eine Wahl (oder alles ein Satz). Sets A/B/C unten im Kontext mit Vitrine (neue Deckenstreben) '
           'und dem Handelsterminal.</p>',
           '<h2>Schon behoben (kein Geschmack, Fehler) <span class="tag">eingebaut</span></h2>',
           '<p class="note">Vitrine: oben umlaufender Rahmen aus vier Querstreben zwischen den Pfosten (vorher deckenlos). '
           'Sales Storage (altes Aussehen): zwei Querstreben aus Holz mit Eisennieten ueber dem Schacht, damit die Decke nicht wie eine fehlende Textur wirkt. '
           'Beide unten im Bild "Alter Stand".</p>']
    out.append('<h2>Alter Stand im Kontext</h2><div class="card"><img src="%s"></div>' % b64(scene({}, 8)))
    for block, vs in VARIANTS.items():
        out.append(f'<h2>{NAMES[block]}</h2><div class="grid">')
        names = list(vs['A'])
        order = [('Alter Stand', {})] + [(f'Variante {l}' + (' <b class="rec">(Empfehlung)</b>' if RECOMMEND[block] == l else ''), sets[l]) for l in 'ABC']
        for title, over in order:
            ov = dict(over)
            out.append(f'<div class="card"><h3>{title}</h3><img src="{b64(single(block, ov))}">'
                       f'<img src="{b64(facegrid(names, ov))}"></div>')
        out.append('</div>')
    out.append('<h2>Gesamtbild je Satz (alle drei Bloecke gleiche Variante)</h2><div class="grid">')
    for l in 'ABC':
        out.append(f'<div class="card"><h3>Satz {l}</h3><img src="{b64(scene(sets[l], 7))}"></div>')
    out.append('</div>')
    out.append(ice_part(os.path.splitext(path)[0] + '-ice'))
    out.append('</body></html>')
    with open(path, 'w') as f:
        f.write(''.join(out))
    print('Seite:', path, os.path.getsize(path) // 1024, 'KB')


def main(argv):
    if '--page' in argv:
        page(argv[argv.index('--page') + 1])
        return 0
    if '--install' in argv:
        install(argv[argv.index('--install') + 1])
        return 0
    if '--check' in argv:
        return check(argv[argv.index('--check') + 1])
    print(__doc__)
    return 2


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
