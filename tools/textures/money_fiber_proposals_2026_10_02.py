"""Usage: python tools/textures/money_fiber_proposals_2026_10_02.py <vanilla textures dir> [preview png] [png dir]

Owner 2026-10-02: Simple Money - ten proposals each for the Special Fiber (copper and gold nuggets, diamond,
amethyst shard) and the Resin Fiber (resin clump, honeycomb, iron nugget, bone meal). Proposals only. Colours are the
ramps of those vanilla ingredients; shapes are vanilla string and lead (fibre/rope look) plus a few drawn forms
(skein, spool, bundle, loop, braid) built the same way for both fibres so a column reads as one style."""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'money-fasern-vorschau.png')
PNG_DIR = sys.argv[3] if len(sys.argv) > 3 else None


def vitem(name):
    return ps.load(os.path.join(V, 'item', name + '.png'))


def palettes():
    def body(name, n=5):
        return [c for c in ps.ramp_of(vitem(name), n)]
    special = {'main': body('amethyst_shard'), 'acc': body('gold_nugget'), 'acc2': body('copper_nugget'),
               'glint': [(161, 251, 232), (255, 255, 255)], 'base': 'amethyst_shard'}
    resin = {'main': body('resin_clump'), 'acc': body('honeycomb'), 'acc2': body('iron_nugget'),
             'glint': [(206, 206, 230), (255, 255, 255)], 'base': 'resin_clump'}
    return {'Spezialfaser': special, 'Harzfaser': resin}


def pal_map(p):
    m, a, b = p['main'], p['acc'], p['acc2']
    return {'o': ps.mix(m[0], (0, 0, 0), 0.45), 'd': m[1], 'm': m[2], 'M': m[3], 'w': m[4],
            'a': a[2], 'A': a[4], 'x': b[1], 'X': b[3]}


def string_recolor(p, braid=False, flecks=False):
    src = vitem('string')
    out = ps.blank()
    light = [xy for xy in ps.opaque(src) if ps.lum(src.getpixel(xy)) > 90]
    for xy in ps.opaque(src):
        if xy not in light:
            out.putpixel(xy, ps.mix(p['main'][0], (0, 0, 0), 0.35) + (255,))
    strand = ps.recolor(src, p['main'][2:], mask=lambda c: ps.lum(c) > 90)
    for k, xy in enumerate(sorted(light, key=lambda q: (q[1], q[0]))):
        c = strand.getpixel(xy)
        if braid:
            c = (p['acc'][3], p['main'][3], p['acc2'][3])[(xy[0] + xy[1]) % 3] + (255,)
        out.putpixel(xy, c)
    if flecks:
        for xy in light:
            if (xy[0] * 5 + xy[1] * 3) % 7 == 0:
                out.putpixel(xy, p['glint'][(xy[0] + xy[1]) % 2] + (255,))
    return out


def lead_recolor(p):
    src = vitem('lead')
    im = ps.recolor(src, p['main'])
    for xy in ps.opaque(src):  # the knot (lightest part of the lead) becomes the accent
        if ps.lum(src.getpixel(xy)) > 150:
            im.putpixel(xy, p['acc'][3] + (255,))
    return im


SKEIN = [
    '................',
    '................',
    '................',
    '.....oooooo.....',
    '....oMmMMmdo....',
    '...oMmaAmmMdo...',
    '...omMmmaAmdo...',
    '..oMmmMmmmaAdo..',
    '..oamMmmMmmmdo..',
    '..oAammMmmMmdo..',
    '...oAammmMmddo..',
    '...odAammmddo...',
    '....oddaAddo....',
    '.....oooooomo...',
    '............Mo..',
    '................',
]
SPOOL = [
    '................',
    '................',
    '....oooooooo....',
    '....oXXXXXXo....',
    '....oxxxxxxo....',
    '.....oMmMmo.....',
    '.....omaMmo.....',
    '.....oMmAmo.....',
    '.....omMmao.....',
    '.....oaMmMo.....',
    '.....oMmMmo.....',
    '....oXXXXXXo....',
    '....oxxxxxxo....',
    '....oooooooo....',
    '................',
    '................',
]


def bundle(p):
    """Three diagonal strands (main, accent, second accent) tied in the middle with an accent band."""
    im = ps.blank()
    cols = [p['main'], p['acc'], p['acc2']]
    for s, ramp in enumerate(cols):
        for i in range(11):
            x, y = 3 + i + s - 1, 13 - i + s - 1
            ps.px(im, x, y, ramp[3] if i % 3 else ramp[2])
    for k in range(-2, 3):
        ps.px(im, 8 + k, 8 + k, p['acc2'][1] if abs(k) == 2 else p['acc'][4])
    return ps.outline(im, ps.mix(p['main'][0], (0, 0, 0), 0.45))


def loop(p):
    im = ps.blank()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 4.0 <= d <= 5.3:
                ps.px(im, x, y, p['main'][3] if (x + y) % 2 else p['main'][2])
    for x, y in ((7, 2), (13, 8), (8, 13), (2, 7)):
        ps.px(im, x, y, p['acc'][4])
        ps.px(im, x + 1, y, p['acc'][2])
    return ps.outline(im, ps.mix(p['main'][0], (0, 0, 0), 0.45))


def braid(p):
    """A twisted two-tone rope, lower left to upper right."""
    im = ps.blank()
    for i in range(12):
        x, y = 2 + i, 13 - i
        first = p['main'] if i % 2 else p['acc']
        second = p['acc'] if i % 2 else p['main']
        ps.px(im, x, y, first[3])
        ps.px(im, x + 1, y, second[2])
    return ps.outline(im, ps.mix(p['main'][0], (0, 0, 0), 0.45))


def wrapped_base(p):
    """The main ingredient item with fibres wound round it (accent diagonals over the body)."""
    base = vitem(p['base'])
    im = base.copy()
    for (x, y) in ps.opaque(base):
        if (x + y) % 4 == 0:
            im.putpixel((x, y), (p['acc'][3] if (x // 2) % 2 else p['acc'][2]) + (255,))
    return im


def proposals(p):
    return [
        ('Faden (Vanilla-Faden)', string_recolor(p)),
        ('Faden, drei Farben verdrillt', string_recolor(p, braid=True)),
        ('Rolle (Vanilla-Leine)', lead_recolor(p)),
        ('Knaeuel', ps.grid(SKEIN, pal_map(p))),
        ('Spule', ps.grid(SPOOL, pal_map(p))),
        ('Buendel, gebunden', bundle(p)),
        ('Schlaufe mit Perlen', loop(p)),
        ('Kordel', braid(p)),
        ('Zutat umwickelt', wrapped_base(p)),
        ('Faden mit Glanzpunkten', string_recolor(p, flecks=True)),
    ]


def main():
    pals = palettes()
    rows, named = [], {}
    for label, p in pals.items():
        props = proposals(p)
        rows.append((label, [im for _, im in props]))
        key = 'special_fiber' if label == 'Spezialfaser' else 'resin_fiber'
        for k, (_, im) in enumerate(props):
            named[f'{key}_{ps.LETTERS[k].lower()}'] = im
    money = lambda n: ps.load(os.path.join(ps.MONEY_ITEM, n + '.png'))
    refs = [('Spezialfaser jetzt', money('special_fiber')), ('Harzfaser jetzt', money('resin_fiber')),
            ('Amethystscherbe', vitem('amethyst_shard')), ('Goldnugget', vitem('gold_nugget')),
            ('Harzklumpen', vitem('resin_clump')), ('Honigwabe', vitem('honeycomb')), ('Faden', vitem('string'))]
    labels = [f'{ps.LETTERS[k]}: {name}' for k, (name, _) in enumerate(proposals(pals['Spezialfaser']))]
    ps.sheet('Simple Money - Spezialfaser und Harzfaser, je 10 Vorschlaege', refs, rows, PREVIEW,
             notes=['  '.join(labels[:5]), '  '.join(labels[5:]),
                    'Spezialfaser: Amethyst + Gold/Kupfer (+ Diamantglanz); Harzfaser: Harz + Honigwabe/Eisen (+ Knochenmehlflecken).'])
    if PNG_DIR:
        ps.save_pngs(PNG_DIR, named)
    print('ok')


if __name__ == '__main__':
    main()
