"""Usage: python tools/textures/money_fiber_proposals_v2_2026_10_02.py <vanilla textures dir> [preview png]

Owner 2026-10-02, round 2 for Simple Money's Special Fiber and Resin Fiber: no recolours this time - ten item designs
of their own per fibre (own silhouette, vanilla palette and shading: light from the top left, darkest one-pixel outline
at the bottom right like vanilla items). The ten forms are the same for both fibres, each in its fibre's materials:
Special Fiber = amethyst threads with gold, copper and a diamond glint; Resin Fiber = resin threads with honeycomb,
iron and bone-meal white. Proposals only."""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'money-fasern-v2-vorschau.png')


def vitem(name):
    return ps.load(os.path.join(V, 'item', name + '.png'))


def ramp5(name):
    cols = ps.ramp_of(vitem(name))
    return [cols[round(i * (len(cols) - 1) / 4)] for i in range(5)]


def materials():
    return {
        'Spezialfaser': {'main': ramp5('amethyst_shard'), 'acc': ramp5('gold_nugget'), 'acc2': ramp5('copper_nugget'),
                         'glint': (161, 251, 232), 'base': 'amethyst_shard'},
        'Harzfaser': {'main': ramp5('resin_clump'), 'acc': ramp5('honeycomb'), 'acc2': ramp5('iron_nugget'),
                      'glint': (238, 238, 230), 'base': 'resin_clump'},
    }


class Canvas:
    def __init__(self, m):
        self.m = m
        self.im = ps.blank()

    def put(self, x, y, ramp, k):
        """k = tone 0..4; light from the top left: one step lighter there, one darker bottom right."""
        x, y = int(x), int(y)
        if not (0 <= x < 16 and 0 <= y < 16):
            return
        k += 1 if x + y < 12 else -1 if x + y > 19 else 0
        self.im.putpixel((x, y), self.m[ramp][max(0, min(4, k))] + (255,))

    def raw(self, x, y, colour):
        if 0 <= x < 16 and 0 <= y < 16:
            self.im.putpixel((int(x), int(y)), tuple(colour) + (255,))

    def line(self, x0, y0, x1, y1, ramp, tones=(2, 3)):
        n = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(n + 1):
            t = i / max(1, n)
            self.put(round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t), ramp, tones[i % len(tones)])

    def done(self):
        dark = ps.mix(self.m['main'][0], (0, 0, 0), 0.5)
        return ps.outline(self.im, dark)


def strang(m):
    """Hank: two twisted strands on the diagonal, little loops at both ends, a binding band in the middle."""
    c = Canvas(m)
    for i in range(10):
        x, y = 3 + i, 12 - i
        c.put(x, y, 'main', 3 if i % 2 else 2)
        c.put(x + 1, y + 1, 'main', 2 if i % 2 else 1)
    for x, y in ((2, 12), (2, 13), (3, 14), (13, 2), (14, 2), (14, 3)):
        c.put(x, y, 'main', 2)
    for k in range(-1, 2):
        c.put(7 + k, 8 + k, 'acc', 3)
        c.put(8 + k, 9 + k, 'acc', 2)
    return c.done()


def spule(m):
    """Spool: thread wound between two flanges."""
    c = Canvas(m)
    for y in range(5, 11):
        for x in range(5, 11):
            c.put(x, y, 'main', 3 if (y % 2 == 0) else 2)
    for y in (3, 4, 11, 12):
        for x in range(4, 12):
            c.put(x, y, 'acc2', 3 if y in (3, 11) else 1)
    c.put(8, 6, 'acc', 4)
    c.put(11, 9, 'main', 3)
    c.put(12, 10, 'main', 2)
    return c.done()


def knaeuel(m):
    """Ball of thread: wrapped stripes and a loose end."""
    c = Canvas(m)
    for y in range(16):
        for x in range(16):
            if math.hypot(x + 0.5 - 7.5, y + 0.5 - 7.5) <= 4.6:
                c.put(x, y, 'main', 3 if (x - y) % 3 == 0 else 2)
    for x, y in ((5, 6), (6, 5), (9, 10), (10, 9)):
        c.put(x, y, 'acc', 3)
    for x, y in ((11, 12), (12, 13), (13, 13), (14, 14)):
        c.put(x, y, 'main', 2)
    return c.done()


def buendel(m):
    """Bundle: four tight fibres tied with a band, the top end fanned out."""
    c = Canvas(m)
    for s in range(4):
        for i in range(10):
            x, y = 2 + i + (s - 1), 12 - i + (s - 2) * 0 + s - 1
            c.put(x, y, 'main', (3, 2, 3, 1)[s] if i % 3 else 2)
    for x, y in ((12, 1), (14, 3), (13, 1), (14, 2)):
        c.put(x, y, 'main', 3)
    for k in range(-2, 3):
        c.put(7 + k, 7 + k, 'acc', 3 if k < 1 else 2)
    return c.done()


def kordel(m):
    """Coiled two-tone cord, like vanilla's lead."""
    c = Canvas(m)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 7.5, y + 0.5 - 7.0)
            if 3.0 <= d <= 5.0:
                a = math.atan2(y + 0.5 - 7.0, x + 0.5 - 7.5)
                two = int((a + math.pi) / (math.pi / 5)) % 2
                c.put(x, y, 'acc' if two else 'main', 3 if d < 4 else 2)
    for x, y in ((11, 11), (12, 12), (13, 13), (13, 14)):
        c.put(x, y, 'main', 2)
    return c.done()


def bueschel(m):
    """Tuft: fibres fanning up from a bound base (like a tassel)."""
    c = Canvas(m)
    for k, a in enumerate((-0.9, -0.5, -0.15, 0.15, 0.5, 0.9)):
        for r in range(2, 9):
            c.put(round(7.5 + math.sin(a) * r * 0.9), round(12 - math.cos(a) * r), 'main', 3 if (k + r) % 2 else 2)
    for x in range(6, 10):
        c.put(x, 12, 'acc', 3)
        c.put(x, 13, 'acc', 1)
    return c.done()


def spindel(m):
    """Spindle: a stick with the fibre wound round its middle."""
    c = Canvas(m)
    c.line(2, 13, 13, 2, 'acc2', (2, 3))
    for y in range(16):
        for x in range(16):
            u, v = (x - y) / 1.414, (x + y - 15) / 1.414
            if (u / 2.4) ** 2 + (v / 4.2) ** 2 <= 1:
                c.put(x, y, 'main', 3 if (x + y) % 2 else 2)
    c.put(6, 6, 'acc', 4)
    return c.done()


def knoten(m):
    """A single cord with a loop and an overhand knot, both tails hanging down."""
    c = Canvas(m)
    for k in range(24):
        a = k / 24 * 2 * math.pi
        c.put(round(7.5 + math.cos(a) * 3.2), round(5.5 + math.sin(a) * 3.2), 'main', 3 if k % 2 else 2)
    for i in range(6):
        c.put(6 - i // 2, 9 + i, 'main', 2 + i % 2)
        c.put(9 + i // 2, 9 + i, 'main', 1 + i % 2)
    c.put(7, 9, 'acc', 3)
    c.put(8, 9, 'acc', 4)
    c.put(8, 10, 'acc', 2)
    return c.done()


def material(m, name):
    """The fibre's own material with threads drawn out of it: an amethyst shard wound with gold wire, or a resin drop
    pulled into strings."""
    c = Canvas(m)
    if name == 'Spezialfaser':
        for y in range(16):
            for x in range(16):
                u, v = (x - y) / 1.414, (x + y - 15) / 1.414
                if abs(u) <= 1.8 and abs(v) <= 5.0 - abs(u) * 0.6:
                    c.put(x, y, 'main', 3 if u < 0 else 2)
        for k in (-3, 0, 3):
            for t in range(-2, 3):
                c.put(7.5 + k / 1.414 + t / 1.414, 7.5 + k / 1.414 - t / 1.414 + 0.5, 'acc', 3)
        c.raw(6, 5, m['glint'])
    else:
        for y in range(16):
            for x in range(16):
                if math.hypot(x + 0.5 - 7.5, y + 0.5 - 6.0) <= 3.6 or (y < 6 and abs(x + 0.5 - 7.5) <= (y - 1) * 0.6):
                    c.put(x, y, 'main', 3 if x + y < 13 else 2)
        for x0 in (5, 7, 10):
            for y in range(10, 15):
                c.put(x0 + (y % 2 if x0 != 7 else 0), y, 'acc', 3 if y < 12 else 2)
        c.raw(6, 4, m['glint'])
    return c.done()


def gewebe(m):
    """A small woven patch, warp in the fibre, weft in the accent, frayed corners."""
    c = Canvas(m)
    for y in range(4, 12):
        for x in range(4, 12):
            warp = (x + y) % 2 == 0
            c.put(x, y, 'main' if warp else 'acc', 3 if warp else 2)
    for x, y in ((3, 5), (3, 9), (12, 6), (12, 10), (6, 3), (10, 12)):
        c.put(x, y, 'main', 2)
    return c.done()


FORMS = [('Strang', strang), ('Spule', spule), ('Knaeuel', knaeuel), ('Buendel', buendel), ('Kordel (Rolle)', kordel),
         ('Bueschel', bueschel), ('Spindel', spindel), ('Knoten', knoten), ('Material + Faeden', None), ('Gewebe', gewebe)]


def main():
    mats = materials()
    rows = []
    for name, m in mats.items():
        rows.append((name, [material(m, name) if fn is None else fn(m) for _, fn in FORMS]))
    money = lambda n: ps.load(os.path.join(ps.MONEY_ITEM, n + '.png'))
    refs = [('Spezialfaser jetzt', money('special_fiber')), ('Harzfaser jetzt', money('resin_fiber')),
            ('Faden', vitem('string')), ('Weizen', vitem('wheat')), ('Seetang', vitem('kelp')),
            ('Harzklumpen', vitem('resin_clump')), ('Leine', vitem('lead')), ('Amethystscherbe', vitem('amethyst_shard'))]
    labels = [f'{ps.LETTERS[k]}: {n}' for k, (n, _) in enumerate(FORMS)]
    ps.sheet('Simple Money - Fasern Runde 2: je 10 eigene Item-Formen (Vanilla-Palette und -Schattierung)', refs, rows,
             PREVIEW, notes=['  '.join(labels[:5]), '  '.join(labels[5:]),
                             'Spezialfaser: Amethyst-Faeden mit Gold/Kupfer, Diamantglanz; Harzfaser: Harz-Faeden mit '
                             'Honigwabe/Eisen, Knochenmehl-Weiss.'])
    print('ok')


if __name__ == '__main__':
    main()
