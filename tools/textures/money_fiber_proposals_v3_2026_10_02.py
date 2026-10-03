"""Usage: python tools/textures/money_fiber_proposals_v3_2026_10_02.py <vanilla textures dir> [preview png]

Owner 2026-10-02, round 3 for Simple Money's Special Fiber and Resin Fiber: start from vanilla's string
(item/string.png) and make a different fibre from it that is clearly no thread but belongs to the string family.
Proposals only.

The string's logic, kept in every variant: a one-pixel strand in two alternating light tones (the twist) and a
one-pixel dark shadow directly under it (right of it where the strand itself continues below). Each variant changes
the strand's STRUCTURE on the string's own path - two-ply, thicker, braided, fuzzy, beaded, glossy, corded, wire-wound,
knotted, dripping/dusted - and the fibre's materials colour it: Special Fiber = amethyst with gold (and a diamond
glint), Resin Fiber = resin with honeycomb (and iron / bone white)."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'money-fasern-v3-vorschau.png')


def vitem(name):
    return ps.load(os.path.join(V, 'item', name + '.png'))


def ramp(name, n=5):
    cols = ps.ramp_of(vitem(name))
    return [cols[round(i * (len(cols) - 1) / (n - 1))] for i in range(n)]


def materials():
    ame, gold, copper = ramp('amethyst_shard'), ramp('gold_nugget'), ramp('copper_nugget')
    resin, honey, iron = ramp('resin_clump'), ramp('honeycomb'), ramp('iron_nugget')
    return {
        # the shard's two palest tones would read as plain white string, so the strand takes the coloured middle
        'Spezialfaser': {'light': (ame[3], ame[2]), 'mid': ame[1], 'shadow': ps.mix(ame[0], (0, 0, 0), 0.45),
                         'acc': (gold[4], gold[3]), 'acc_mid': gold[2], 'metal': (gold[4], gold[3], gold[2]),
                         'glint': (161, 251, 232), 'drop': copper[3]},
        'Harzfaser': {'light': (resin[3], resin[2]), 'mid': resin[1], 'shadow': ps.mix(resin[0], (0, 0, 0), 0.45),
                      'acc': (honey[4], honey[3]), 'acc_mid': honey[2], 'metal': (iron[4], iron[3], iron[2]),
                      'glint': (238, 238, 230), 'drop': honey[3]},
    }


def string_strand():
    """{(x, y): 0/1} - the vanilla string's light pixels with their tone (0 = lighter of the two)."""
    src = vitem('string')
    pts = {p for p in ps.opaque(src) if ps.lum(src.getpixel(p)) > 150}
    lightest = max(ps.lum(src.getpixel(p)) for p in pts)
    return {p: 0 if ps.lum(src.getpixel(p)) >= lightest - 1 else 1 for p in pts}


def render(strand, shadow_colour, extra=None):
    """strand: {(x, y): rgb}. Shadow like the string: under each strand pixel, or right of it if the strand continues
    below. `extra` pixels (hairs, sparkles) are drawn last and cast no shadow."""
    im = ps.blank()
    for (x, y) in strand:
        below, right = (x, y + 1), (x + 1, y)
        target = below if below not in strand else right if right not in strand else None
        if target and 0 <= target[0] < 16 and 0 <= target[1] < 16:
            im.putpixel(target, shadow_colour + (255,))
    for p, c in strand.items():
        if 0 <= p[0] < 16 and 0 <= p[1] < 16:
            im.putpixel(p, tuple(c) + (255,))
    for p, c in (extra or {}).items():
        if 0 <= p[0] < 16 and 0 <= p[1] < 16 and im.getpixel(p)[3] == 0:
            im.putpixel(p, tuple(c) + (255,))
    return im


def path_order(strand):
    return sorted(strand, key=lambda p: (p[1], p[0]))


def variants(m):
    s = string_strand()
    order = path_order(s)
    tone = lambda p: m['light'][s[p]]
    out = []
    # A two-ply: the twist alternates fibre and accent instead of two light tones
    out.append(('zweifach gedreht', render({p: (m['light'][0] if s[p] == 0 else m['acc'][0]) for p in s}, m['shadow'])))
    # B thicker: the strand doubled to the right, left pixel light, right pixel one tone darker
    thick = {p: tone(p) for p in s}
    for (x, y) in s:
        thick.setdefault((x + 1, y), m['mid'])
    out.append(('dicker (2 px)', render(thick, m['shadow'])))
    # C braided: the thick strand in a checker of fibre and accent
    out.append(('geflochten', render({p: (m['light'][0] if (p[0] + p[1]) % 2 == 0 else m['acc'][1]) for p in thick}, m['shadow'])))
    # D fuzzy: short fibre hairs standing off up-left
    hairs = {(x - 1, y - 1): m['acc'][1] for (x, y) in s if (x * 3 + y) % 3 == 0}
    out.append(('gefasert', render({p: tone(p) for p in s}, m['shadow'], hairs)))
    # E beaded: three accent beads along the path
    beaded = {p: tone(p) for p in s}
    for p in (order[len(order) // 5], order[len(order) // 2], order[4 * len(order) // 5]):
        for dx, dy, c in ((0, 0, m['acc'][0]), (1, 0, m['acc'][1]), (0, 1, m['acc'][1]), (1, 1, m['acc_mid'])):
            beaded[(p[0] + dx, p[1] + dy)] = c
    out.append(('mit Perlen', render(beaded, m['shadow'])))
    # F glossy: every third strand pixel a glint, the rest one even tone
    out.append(('glaenzend', render({p: (m['glint'] if k % 3 == 0 else m['light'][1])
                                     for k, p in enumerate(order)}, m['shadow'])))
    # G cord: the shadow side becomes a second, darker fibre ply - a two-tone cord with its own dark edge
    cord = {p: tone(p) for p in s}
    for (x, y) in s:
        q = (x, y + 1) if (x, y + 1) not in s else (x + 1, y)
        cord.setdefault(q, m['mid'])
    out.append(('Kordel (zweilagig)', render(cord, m['shadow'])))
    # H wire-wound: metal strand with the fibre showing every other pixel
    out.append(('drahtumwickelt', render({p: (m['metal'][k % 2] if k % 2 == 0 else m['light'][0])
                                          for k, p in enumerate(order)}, m['shadow'])))
    # I knotted: two small knots (a lit pixel ring) on the path
    knotted = {p: tone(p) for p in s}
    for p in (order[len(order) // 4], order[3 * len(order) // 5]):
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                knotted[(p[0] + dx, p[1] + dy)] = m['acc_mid'] if dx or dy else m['acc'][0]
        knotted[(p[0] - 1, p[1] - 1)] = m['acc'][0]
    out.append(('knotig', render(knotted, m['shadow'])))
    # J dripping / dusted: resin drips hang from the low points, the special fibre carries sparkle dust
    extra = {}
    for (x, y) in s:
        if (x, y + 1) not in s and (x * 7 + y) % 6 == 0:
            extra[(x, y + 2)] = m['drop']
    for (x, y) in order[::4]:
        extra.setdefault((x + 1, y - 1), m['glint'])
    out.append(('Tropfen / Funkelstaub', render({p: tone(p) for p in s}, m['shadow'], extra)))
    return out


def main():
    mats = materials()
    rows, names = [], None
    for name, m in mats.items():
        vs = variants(m)
        names = [n for n, _ in vs]
        rows.append((name, [im for _, im in vs]))
    money = lambda n: ps.load(os.path.join(ps.MONEY_ITEM, n + '.png'))
    refs = [('Vanilla-Faden (Basis)', vitem('string')), ('Spezialfaser jetzt', money('special_fiber')),
            ('Harzfaser jetzt', money('resin_fiber')), ('Amethystscherbe', vitem('amethyst_shard')),
            ('Goldnugget', vitem('gold_nugget')), ('Harzklumpen', vitem('resin_clump')), ('Honigwabe', vitem('honeycomb'))]
    ps.sheet('Simple Money - Fasern Runde 3: aus dem Vanilla-Faden abgeleitet (gleiche Strichstaerke und Schatten, andere Struktur)',
             refs, rows, PREVIEW, scale=10,
             notes=['  '.join(f'{ps.LETTERS[k]}: {n}' for k, n in enumerate(names[:5])),
                    '  '.join(f'{ps.LETTERS[k + 5]}: {n}' for k, n in enumerate(names[5:])),
                    'Faden-Logik ueberall: 1-px-Strang, zwei helle Toene im Wechsel, 1-px-Schatten darunter/rechts.'])
    print('ok')


if __name__ == '__main__':
    main()
