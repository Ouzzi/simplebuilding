"""Simple Shops look, final (ABN30-6, owner choice 30.10.): Sales Block A + more ender pearl / eye of ender, Trade Terminal A, Sales Storage A,
refined after the Endwood process (a-tex29 boards A, PRINZIPIEN A8b): ten-tone ramps instead of three, boards with a lit and a shaded
edge, soft mottling along the grain, hard steps around 7 %.

Builds on the candidates of shops29_2026_10_29.py (same drawing, same layers): its ``planks`` is replaced by a soft version, every
flat layer is mottled by neighbouring ramp tones, and the Sales Block gets the ender decoration (two ender pearls and an eye of ender
on the counter shelf, a pearl plaque on the sides, an eye emblem on the roof). The three blocks' textures are written to
modules/simpleshops/.../textures/block/ with their layer sources; the older generators (simple_shops_2026_10_15 sales block,
simple_shops_terminal_2026_10_20, misctex23b sales storage) no longer write them.

    python3 tools/textures/shops31_2026_10_30.py --install      # writes the 9 textures + layer sources
    python3 tools/textures/shops31_2026_10_30.py --check        # exit 1 when one is stale
    python3 tools/textures/shops31_2026_10_30.py --report       # hard-step share of every texture
"""
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import layers as L  # noqa: E402
import shops29_2026_10_29 as S  # noqa: E402
from n41_proposals_2026_10_12 import hsh, vnoise  # noqa: E402
from simple_shops_storage_2026_10_22 import Layer  # noqa: E402

H = S.H
HARD = 40   # luminance step that counts as hard (tex29 figure)


def fine(pal, n=None):
    """The 6/5/4/3-tone ramps of the candidates stretched to twice the tones (every step halved): index i -> 2i, plus the halves between."""
    out = []
    for i in range(len(pal) - 1):
        a, b = pal[i], pal[i + 1]
        out.append(a)
        out.append(tuple(int(round((a[k] + b[k]) / 2)) for k in range(3)))
    out.append(pal[-1])
    return out


PALS = {'oak': S.OAK, 'spr': S.SPR, 'irn': S.IRN, 'gld': S.GLD, 'red': S.RED, 'crm': S.CRM, 'slt': S.SLT, 'pap': S.PAP}
FINE = {k: fine(v) for k, v in PALS.items()}
ENDP = [H('0b2924'), H('11403a'), H('1a5e4d'), H('2a8470'), H('48ad92'), H('8fdcc0')]
EYE = [H('120b1e'), H('241638'), H('3a2658')]


def lum(c):
    return 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]


def planks_soft(l, pal, seed, vertical=False, board=4, x0=0, y0=0, x1=16, y1=16):
    """Boards of ten tones: each board its own base level, a lit edge and a shaded seam over two pixels, grain running along the board
    (elongated value noise), staggered joints, a gentle fall of light from the top."""
    ramp = fine(pal)
    n = len(ramp)
    for y in range(y0, y1):
        for x in range(x0, x1):
            u, v = (x, y) if vertical else (y, x)
            b, k = u // board, u % board
            t = 0.55 + (hsh(b, 3, seed) - 0.5) * 0.16
            t += (vnoise(v, (u * 5 + b * 7) % 16, 6, seed) - 0.5) * 0.20 + (hsh(x, y, seed + 7) - 0.5) * 0.07
            t -= 0.06 * (y / 15.0)
            if k == 0:
                t += 0.17
            elif k == 1:
                t += 0.05
            elif k == board - 1:
                t -= 0.30
            elif k == board - 2 and board > 3:
                t -= 0.07
            if (v + b * 5 + seed) % 16 == 11 and 0 < k < board - 1:
                t -= 0.16
            l.put(x, y, ramp[max(0, min(n - 1, int(t * n)))])


def mottle(lay, seed, amount=1.0, keys=('oak', 'spr', 'irn', 'gld', 'red', 'crm', 'slt', 'pap')):
    """Moves every pixel of a layer by up to one half-step along its own ramp (low-frequency noise + a little grain): flat areas get tone
    without new colours. Pixels that are in no ramp (voids, ender art) stay."""
    lookup = {}
    for key in keys:
        for i, c in enumerate(FINE[key]):
            lookup.setdefault(c, (key, i))
    for y in range(16):
        for x in range(16):
            o = (y * 16 + x) * 4
            if lay.px[o + 3] == 0:
                continue
            c = tuple(lay.px[o:o + 3])
            if c not in lookup:
                continue
            key, i = lookup[c]
            ramp = FINE[key]
            nz = (vnoise(x, y, 4, seed) - 0.5) + (hsh(x, y, seed + 3) - 0.5) * 0.5
            d = 1 if nz * amount > 0.2 else -1 if nz * amount < -0.2 else 0
            lay.px[o:o + 3] = bytes(ramp[max(0, min(len(ramp) - 1, i + d))])


def to_fine(lay):
    """Rewrites a candidate layer's ramp colours as indices of the ten-tone ramps (so mottle can move them by half steps)."""
    lookup = {}
    for key, pal in PALS.items():
        for i, c in enumerate(pal):
            lookup.setdefault(c, (key, 2 * i))
    for y in range(16):
        for x in range(16):
            o = (y * 16 + x) * 4
            c = tuple(lay.px[o:o + 3])
            if lay.px[o + 3] and c in lookup:
                key, i = lookup[c]
                lay.px[o:o + 3] = bytes(FINE[key][i])


def refine(lays, seed, skip=()):
    for n, lay in enumerate(lays):
        if lay.name in skip:
            continue
        to_fine(lay)
        mottle(lay, seed + n * 11)
    return lays


# ------------------------------------------------------------------ soft awning
WAVE = (0.0, 0.0, 1 / 3, 2 / 3, 1.0, 1.0, 2 / 3, 1 / 3)   # 0 = red, 1 = cream, per column of an 8 px period: two soft folds across the block
CREAM_OF = {3: 2, 2: 1, 1: 0, 0: 0}                        # the cream tone that goes with a red tone (same place in the light)
RED_OF = {2: 3, 1: 2, 0: 1}


def soften_awning(lay):
    """The red/cream stripes of the awning as a soft wave (one third of the step per column) instead of 2 px stripes with a hard edge."""
    red_idx = {c: i for i, c in enumerate(S.RED)}
    crm_idx = {c: i for i, c in enumerate(S.CRM)}
    for y in range(16):
        for x in range(16):
            o = (y * 16 + x) * 4
            if lay.px[o + 3] == 0:
                continue
            c = tuple(lay.px[o:o + 3])
            if c in red_idx:
                r = red_idx[c]
                cr = CREAM_OF[r]
            elif c in crm_idx:
                cr = crm_idx[c]
                r = RED_OF[cr]
            else:
                continue
            a, b = S.RED[r], S.CRM[cr]
            w = WAVE[x % 8]
            lay.px[o:o + 3] = bytes(int(round(a[k] * (1 - w) + b[k] * w)) for k in range(3))


# ------------------------------------------------------------------ ender art
def art(l, x, y, rows, key):
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch in key:
                l.put(x + dx, y + dy, key[ch])


def pearl3(l, x, y):
    art(l, x, y, ['435', '332', '121'], {'1': ENDP[1], '2': ENDP[2], '3': ENDP[3], '4': ENDP[4], '5': ENDP[5]})


def eye3(l, x, y):
    art(l, x, y, ['343', '4P2', '221'], {'1': ENDP[1], '2': ENDP[2], '3': ENDP[3], '4': ENDP[4], 'P': EYE[0]})


def pearl4(l, x, y):
    art(l, x, y, ['.34.', '2532', '1322', '.11.'], {'1': ENDP[1], '2': ENDP[2], '3': ENDP[3], '4': ENDP[4], '5': ENDP[5]})


def eye4(l, x, y):
    art(l, x, y, ['.34.', '3PP2', '2PH1', '.21.'], {'1': ENDP[1], '2': ENDP[2], '3': ENDP[3], '4': ENDP[4], 'P': EYE[0], 'H': ENDP[5]})


def ring(l, x0, y0, size):
    """A gold frame (lit top-left, shaded bottom-right) of size x size pixels."""
    G = FINE['gld']
    S.bevel(l, x0, y0, x0 + size, y0 + size, G[7], G[2])
    for i in range(1, size - 1):
        l.put(x0 + i, y0 + 1, G[5])
        l.put(x0 + 1, y0 + i, G[5])


# ------------------------------------------------------------------ Sales Block A (+ ender)
def sales_block(face):
    S.planks = planks_soft
    base = S.sb_A_side(False) if face != 'top' else S.sb_A_top()
    soften_awning(base[1] if face != 'top' else base[0])
    lays = refine(base, 5 if face == 'front' else 8 if face == 'side' else 13)
    ender = Layer('Enderperlen')
    G = FINE['gld']
    if face == 'front':
        det = lays[2]
        S.fill(det, 2, 6, 14, 11, S.SPR[2])
        S.bevel(det, 2, 6, 14, 11, FINE['oak'][2], FINE['oak'][9])
        S.fill(det, 3, 7, 13, 10, S.SPR[1])
        for x in range(3, 13):
            det.put(x, 7, S.SPR[0])
        # The counter shelf: an ender pearl, two coin stacks, an eye of ender.
        for x, h in ((7, 2), (8, 1)):
            for k in range(h):
                det.put(x, 9 - k, G[7] if k == h - 1 else G[3])
        pearl3(ender, 4, 7)
        eye3(ender, 9, 7)
        # Counter board with the coin plate, a pearl bead on each end.
        S.fill(det, 2, 11, 14, 13, S.OAK[5])
        for x in range(2, 14):
            det.put(x, 12, S.OAK[2])
        S.fill(det, 6, 13, 10, 15, S.GLD[2])
        S.bevel(det, 6, 13, 10, 15, G[8], G[0])
        det.put(7, 14, S.VOID[0])
        det.put(8, 14, S.VOID[0])
        ender.put(3, 12, ENDP[4])
        ender.put(12, 12, ENDP[3])
        mottle(det, 29, 0.8)
    elif face == 'side':
        ring(ender, 5, 6, 6)
        pearl4(ender, 6, 7)
    else:
        det = lays[1]
        for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
            det.put(x, y, (0, 0, 0), 0)
        ring(ender, 5, 5, 6)
        eye4(ender, 6, 6)
    return lays + [ender]


# ------------------------------------------------------------------ Sales Storage A
def sales_storage(face):
    S.planks = planks_soft
    base = {'side': S.ss_A_side, 'intake': S.ss_A_intake, 'back': S.ss_A_back}[face]()
    lays = refine(base, {'side': 21, 'intake': 22, 'back': 23}[face])
    for lay in lays:   # the iron is worn (a quarter of the wood tone in it), so hoops and rings sit in the staves instead of on them
        if lay.name in ('Reifen', 'Eisenring', 'Baender'):
            for i in range(256):
                o = i * 4
                if lay.px[o + 3]:
                    c = tuple(lay.px[o:o + 3])
                    lay.px[o:o + 3] = bytes(int(round(c[j] * 0.72 + S.SPR[3][j] * 0.28)) for j in range(3))
    return lays


# ------------------------------------------------------------------ Trade Terminal A
def trade_terminal(face):
    S.planks = planks_soft
    base = {'front': S.tt_A_front, 'side': S.tt_A_side, 'top': S.tt_A_top}[face]()
    lays = refine(base, {'front': 31, 'side': 32, 'top': 33}[face])
    if face == 'top':   # the ledger page is aged paper (warmer, darker) and its lines are pencil, not ink: soft against the oak frame
        wood = S.OAK[3]
        for k in (1, 2):
            for i in range(256):
                o = i * 4
                if lays[k].px[o + 3]:
                    c = tuple(lays[k].px[o:o + 3])
                    t = 0.42 if k == 1 else 0.38
                    mate = wood if k == 1 else S.PAP[1]
                    lays[k].px[o:o + 3] = bytes(int(round(c[j] * (1 - t) + mate[j] * t)) for j in range(3))
    return lays


def feather(lays, name='Weiche Kanten'):
    """A last layer that softens the hardest steps of the composition: where two neighbouring pixels differ by more than HARD in
    luminance, the lighter one (the highlight of a pair with a smaller step keeps more of its punch) is moved 40 % of the way to the darker.
    Ender art, voids and one-pixel highlights keep their punch."""
    im = S.pil_of(lays)
    px = im.load()
    out = Layer(name)
    chosen = {}
    for y in range(16):
        for x in range(16):
            for dx, dy in ((1, 0), (0, 1)):
                x2, y2 = x + dx, y + dy
                if x2 > 15 or y2 > 15:
                    continue
                a, b = px[x, y], px[x2, y2]
                if a[3] != 255 or b[3] != 255 or abs(lum(a) - lum(b)) <= HARD:
                    continue
                (lx, ly), (dxx, dyy) = ((x, y), (x2, y2)) if lum(a) > lum(b) else ((x2, y2), (x, y))
                light, dark = px[lx, ly], px[dxx, dyy]
                chosen[(lx, ly)] = tuple(int(round(light[k] * 0.6 + dark[k] * 0.4)) for k in range(3))
    for (x, y), c in chosen.items():
        out.put(x, y, c)
    return lays + [out]


TEXTURES = {
    'sales_block_front': lambda: sales_block('front'), 'sales_block_side': lambda: sales_block('side'), 'sales_block_top': lambda: sales_block('top'),
    'sales_storage_side': lambda: sales_storage('side'), 'sales_storage_intake': lambda: sales_storage('intake'),
    'sales_storage_back': lambda: sales_storage('back'),
    'trade_terminal_front': lambda: trade_terminal('front'), 'trade_terminal_side': lambda: trade_terminal('side'),
    'trade_terminal_top': lambda: trade_terminal('top'),
}


def hard_share(im):
    px = im.load()
    pairs = hard = 0
    for y in range(16):
        for x in range(16):
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx > 15 or y + dy > 15:
                    continue
                a, b = px[x, y], px[x + dx, y + dy]
                if a[3] == 255 and b[3] == 255:
                    pairs += 1
                    hard += abs(lum(a) - lum(b)) > HARD
    return hard / pairs if pairs else 0.0


def build_all():
    return {n: feather(f()) for n, f in TEXTURES.items()}


def install():
    for name, lays in build_all().items():
        doc = S.doc_of(lays)
        path = os.path.join(S.TEX, name + '.png')
        with open(path, 'wb') as f:
            f.write(L.png_write(16, 16, bytes(L.compose(doc))))
        L.write_beside(path, doc)
        print('geschrieben:', name)


def check():
    bad = 0
    for name, lays in build_all().items():
        path = os.path.join(S.TEX, name + '.png')
        have = bytes(L.png_read(open(path, 'rb').read())[2]) if os.path.exists(path) else b''
        if have != bytes(L.compose(S.doc_of(lays))):
            print('veraltet:', name)
            bad += 1
    if not bad:
        print('OK shops31 (9 textures)')
    return 1 if bad else 0


def report():
    for name, lays in build_all().items():
        print(f'{name:24s} hard steps {hard_share(S.pil_of(lays)) * 100:4.1f} %')


if __name__ == '__main__':
    if '--install' in sys.argv:
        install()
    elif '--report' in sys.argv:
        report()
    else:
        sys.exit(check())
