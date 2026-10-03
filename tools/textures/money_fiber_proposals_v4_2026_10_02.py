"""Usage: python tools/textures/money_fiber_proposals_v4_2026_10_02.py <vanilla textures dir> [preview png]

Owner 2026-10-02, round 4 for Simple Money's Special Fiber and Resin Fiber: oriented on vanilla's string only in
STYLE - one-pixel strokes in two alternating light tones, a one-pixel dark shadow under them (no full outline), easy
to read in a slot - but every proposal has a silhouette of its own that is not a thread loop: spool, tied bundle,
twisted rope, braid, fluffy tuft, a fibre drawn from a lump, a woven strip, a knot, a bead loop, a coiled spring.
Same ten forms for both fibres, coloured with their materials (Special Fiber = amethyst, gold, diamond glint;
Resin Fiber = resin, honeycomb, iron). Proposals only."""
import math
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import money_fiber_proposals_v3_2026_10_02 as v3f  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'money-fasern-v4-vorschau.png')
v3f.V = V
render = v3f.render


def line(x0, y0, x1, y1):
    n = max(abs(x1 - x0), abs(y1 - y0))
    return [(round(x0 + (x1 - x0) * i / max(1, n)), round(y0 + (y1 - y0) * i / max(1, n))) for i in range(n + 1)]


def twist(cells, m, k0=0):
    """The string's two alternating light tones along a stroke."""
    return {p: m['light'][(i + k0) % 2] for i, p in enumerate(cells)}


def spool(m):
    """Fibre wound on a spool: five wound rows between two accent end plates."""
    st = {}
    for r, y in enumerate(range(5, 11)):
        st.update(twist(line(5, y, 10, y), m, r))
    for y in (3, 4, 11, 12):
        for x in range(4, 12):
            st[(x, y)] = m['acc'][0] if y in (3, 11) else m['acc_mid']
    st.update(twist(line(11, 8, 13, 10), m))  # loose end
    return render(st, m['shadow'])


def bundle(m):
    """Three strands side by side on the diagonal, tied in the middle, the top end splayed."""
    st = {}
    for s in range(3):
        st.update(twist(line(2 + s, 13, 11 + s, 4), m, s))
    for (x, y) in ((12, 2), (14, 3), (13, 1), (15, 2)):
        st[(x, y)] = m['light'][0]
    for k in range(-1, 3):
        st[(6 + k, 9 + k)] = m['acc'][0] if k % 2 else m['acc'][1]
    return render(st, m['shadow'])


def rope(m):
    """A short piece of twisted rope, slanted, the twist visible as alternating light/dark diagonal pairs."""
    st = {}
    for i in range(10):
        x, y = 3 + i, 12 - i
        st[(x, y)] = m['light'][0] if i % 2 == 0 else m['mid']
        st[(x + 1, y)] = m['mid'] if i % 2 == 0 else m['light'][1]
        st[(x + 1, y + 1)] = m['light'][1] if i % 2 == 0 else m['mid']
    for p in ((2, 13), (3, 14), (14, 2), (15, 3)):  # frayed ends
        st[p] = m['light'][0]
    return render(st, m['shadow'])


def braid(m):
    """A braid: two strands crossing in a zigzag, top to bottom."""
    st = {}
    for y in range(2, 14):
        a = 6 + (y % 4 if y % 4 < 2 else 4 - y % 4)
        b = 9 - (y % 4 if y % 4 < 2 else 4 - y % 4)
        st[(a, y)] = m['light'][0]
        st[(b, y)] = m['acc'][1]
        st[(a + 1, y)] = m['light'][1]
    st[(7, 1)] = m['acc_mid']
    st[(8, 1)] = m['acc_mid']
    return render(st, m['shadow'])


def tuft(m):
    """A fluffy tuft (like teased cotton): short curls round a soft middle."""
    st = {}
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 7.5, (y + 0.5 - 8.0) * 1.2)
            if d <= 4.6 and (x * 5 + y * 3) % 7 != 0:
                st[(x, y)] = m['light'][0] if (x + y) % 3 else m['light'][1]
    for p in ((3, 5), (12, 6), (2, 9), (13, 10), (6, 2), (9, 13)):  # curls standing off
        st[p] = m['light'][1]
    st[(7, 7)] = m['acc'][0]
    return render(st, m['shadow'])


def drawn(m, name):
    """The fibre drawn out of its material: a lump at the top, three strands pulled down to drops."""
    st = {}
    for y in range(1, 6):
        for x in range(4, 12):
            if math.hypot(x + 0.5 - 8, (y + 0.5 - 3.5) * 1.4) <= 3.6:
                st[(x, y)] = m['acc'][0] if (x + y) < 9 else m['acc'][1] if (x + y) < 13 else m['acc_mid']
    for x0, x1, k in ((6, 5, 0), (8, 8, 1), (10, 11, 0)):
        st.update(twist(line(x0, 6, x1, 12 - (x0 == 8)), m, k))
    for p in ((5, 13), (8, 12), (11, 13)):
        st[p] = m['drop']
    return render(st, m['shadow'])


def woven(m):
    """A woven strip (fabric tape), slanted, warp fibre / weft accent in a checker."""
    st = {}
    for i in range(11):
        for w in range(3):
            x, y = 2 + i + w, 11 - i + w
            st[(x, y)] = m['light'][i % 2] if (i + w) % 2 == 0 else m['acc'][1]
    return render(st, m['shadow'])


def knot(m):
    """A thick overhand knot in the middle, two short loose ends."""
    st = {}
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 1.6 <= d <= 3.4:
                st[(x, y)] = m['light'][0] if (x - y) % 3 else m['mid']
    st[(8, 8)] = m['mid']
    st.update(twist(line(4, 11, 2, 14), m))
    st.update(twist(line(11, 5, 13, 2), m, 1))
    return render(st, m['shadow'])


def bead_loop(m):
    """A closed loop with three beads."""
    st = {}
    for k in range(28):
        a = k / 28 * 2 * math.pi
        st[(round(7.5 + math.cos(a) * 4.6), round(7.5 + math.sin(a) * 4.6))] = m['light'][k % 2]
    for (x, y) in ((7, 2), (12, 9), (3, 10)):
        for dx, dy, c in ((0, 0, m['acc'][0]), (1, 0, m['acc'][1]), (0, 1, m['acc'][1]), (1, 1, m['acc_mid'])):
            st[(x + dx, y + dy)] = c
    return render(st, m['shadow'])


def spring(m):
    """A coiled, spring-like fibre on the diagonal: front windings light, back windings one tone darker."""
    st = {}
    for t in range(60):
        u = t / 59
        cx, cy = 2.5 + u * 11, 13.5 - u * 11
        a = u * 6 * 2 * math.pi
        x, y = round(cx + math.cos(a) * 1.8), round(cy + math.sin(a) * 1.8)
        front = math.sin(a) > 0
        if front or (x, y) not in st:
            st[(x, y)] = m['light'][t % 2] if front else m['mid']
    return render(st, m['shadow'])


FORMS = [('Spule', spool), ('gebuendelt + Bindung', bundle), ('gedrehtes Seilstueck', rope), ('Zopf', braid),
         ('Bueschel (aufgefasert)', tuft), ('aus Klumpen gezogen', None), ('gewebter Streifen', woven),
         ('Knoten', knot), ('Schlaufe mit Perlen', bead_loop), ('Spirale', spring)]


def main():
    mats = v3f.materials()
    rows = []
    for name, m in mats.items():
        rows.append((name, [drawn(m, name) if fn is None else fn(m) for _, fn in FORMS]))
    string = v3f.vitem('string')
    money = lambda n: ps.load(os.path.join(ps.MONEY_ITEM, n + '.png'))
    refs = [('Vanilla-Faden (Stil)', string), ('Spezialfaser jetzt', money('special_fiber')),
            ('Harzfaser jetzt', money('resin_fiber')), ('Vanilla-Leine', v3f.vitem('lead')),
            ('Weizen', v3f.vitem('wheat')), ('Harzklumpen', v3f.vitem('resin_clump'))]
    ps.sheet('Simple Money - Fasern Runde 4: eigene Silhouetten im Stil des Vanilla-Fadens (1-px-Striche, Wechseltoene, '
             'Schatten darunter)', refs, rows, PREVIEW, scale=10,
             notes=['  '.join(f'{ps.LETTERS[k]}: {n}' for k, (n, _) in enumerate(FORMS[:5])),
                    '  '.join(f'{ps.LETTERS[k + 5]}: {n}' for k, (n, _) in enumerate(FORMS[5:])),
                    'Spezialfaser: Amethyst + Gold, Kupfertropfen; Harzfaser: Harz + Honigwabe, Honigtropfen.'])
    _slot_check(rows, string)
    print('ok')


def _slot_check(rows, string):
    """Slot-sized comparison strip (1x and 2x on the inventory grey) next to the string, for the quick-look test."""
    imgs = [string] + [im for _, r in rows for im in r]
    strip = Image.new('RGBA', (len(imgs) * 52 + 8, 92), (139, 139, 139, 255))
    for k, im in enumerate(imgs):
        strip.alpha_composite(im, (8 + k * 52, 8))
        strip.alpha_composite(im.resize((32, 32), Image.NEAREST), (8 + k * 52, 40))
    strip.save(os.path.splitext(PREVIEW)[0] + '-slot.png')


if __name__ == '__main__':
    main()
