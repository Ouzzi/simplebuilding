"""Usage: python tools/textures/food_proposals_2026_10_02.py <vanilla textures dir> [preview png] [png dir]

Owner 2026-10-02: ten proposals for the Netherite Apple, Enderite Apple, Netherite Carrot and Enderite Carrot.
Proposals only. Every set uses one recipe on all four items so they read as a family: the vanilla golden/plain apple
and carrot keep their shape and shading (brightness rank), the fruit body gets the ingot ramp (vanilla netherite
ingot, mod enderite ingot), and stem/leaves are treated per set."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'netherit-enderit-essen-vorschau.png')
PNG_DIR = sys.argv[3] if len(sys.argv) > 3 else None

NETHERITE = [(17, 17, 17), (39, 28, 29), (49, 41, 42), (60, 50, 50), (72, 69, 72), (90, 87, 90), (115, 113, 115),
             (146, 140, 146), (182, 176, 182)]
ENDERITE = [(28, 10, 51), (62, 33, 115), (71, 36, 128), (85, 48, 154), (109, 69, 184), (123, 81, 201), (142, 99, 220),
            (165, 125, 233), (207, 178, 251), (241, 232, 255)]
SPARK = {'netherite': [(214, 92, 32), (250, 160, 60)], 'enderite': [(199, 125, 255), (244, 210, 255)]}
DARK_STEM = {'netherite': [(24, 16, 16), (54, 36, 30), (80, 56, 44)], 'enderite': [(20, 8, 36), (44, 22, 80), (70, 40, 120)]}
GOLD_LEAVES = {0x715008, 0x573d06, 0x532906, 0x301600}
STEM = {0x542409, 0x7e370e}


def hexc(p):
    return (p[0] << 16) | (p[1] << 8) | p[2]


def vitem(name):
    return ps.load(os.path.join(V, 'item', name + '.png'))


def is_extra(name):
    """Stem (apples) / leaves (carrots) mask on the vanilla source."""
    if 'carrot' in name:
        return lambda p, xy: hexc(p) in GOLD_LEAVES or (p[1] > p[0] + 20 and p[1] > p[2])
    return lambda p, xy: xy[1] <= 3 or hexc(p) in STEM or (p[1] > p[0] + 20 and p[1] > p[2])


def build(src_name, metal, body_ramp, extra_mode, by_rank=True):
    src = vitem(src_name)
    extra = is_extra(src_name)
    # only the fruit body takes the ingot ramp, so stem and leaves do not steal ramp steps
    body_pts = [xy for xy in ps.opaque(src) if not extra(src.getpixel(xy), xy)]
    tmp = ps.blank()
    for xy in body_pts:
        tmp.putpixel(xy, src.getpixel(xy))
    body = ps.recolor(tmp, body_ramp, by_rank=by_rank)
    out = body.copy()
    leaves = ps.blank()
    for xy in ps.opaque(src):
        if xy not in body_pts:
            leaves.putpixel(xy, src.getpixel(xy))
    if extra_mode == 'vanilla':
        out.alpha_composite(leaves)
    elif extra_mode == 'dark':
        out.alpha_composite(ps.recolor(leaves, DARK_STEM[metal]))
    elif extra_mode == 'metal':
        out.alpha_composite(ps.recolor(leaves, body_ramp[:4]))
    return out


def sparkles(im, metal, n=2):
    out = im.copy()
    pts = sorted(ps.opaque(im), key=lambda p: (-ps.lum(im.getpixel(p)), p[1], p[0]))
    for k, p in enumerate(pts[:n]):
        out.putpixel(p, SPARK[metal][1 if k == 0 else 0] + (255,))
    return out


def veins(im, metal):
    """A few accent pixels in the mid tones (netherite: ancient-debris ember, enderite: pink)."""
    out = im.copy()
    pts = ps.opaque(im)
    lums = sorted(ps.lum(im.getpixel(p)) for p in pts)
    lo, hi = lums[len(lums) // 3], lums[2 * len(lums) // 3]
    for (x, y) in pts:
        if lo <= ps.lum(im.getpixel((x, y))) <= hi and (x * 7 + y * 3) % 11 == 0:
            out.putpixel((x, y), SPARK[metal][0] + (255,))
    return out


def tinted(src_name, metal, ramp):
    """Half fruit colour, half metal: keeps a hint of red/orange under the metal."""
    metal_im = build(src_name, metal, ramp, 'metal')
    src = vitem(src_name)
    out = metal_im.copy()
    for p in ps.opaque(src):
        a, b = src.getpixel(p), metal_im.getpixel(p)
        out.putpixel(p, ps.mix(b, a, 0.3) + (255,))
    return out


def sets():
    """(label, function(item kind 'apple'/'carrot', metal, ramp) -> image)"""
    g = lambda kind: 'golden_' + kind
    return [
        ('Goldform, ganz Metall', lambda k, m, r: build(g(k), m, r, 'metal')),
        ('Goldform, Stiel/Laub dunkel', lambda k, m, r: build(g(k), m, r, 'dark')),
        ('Goldform, Stiel/Laub Vanilla', lambda k, m, r: build(g(k), m, r, 'vanilla')),
        ('Apfel/Karotte, Laub gruen', lambda k, m, r: build(k, m, r, 'vanilla')),
        ('Goldform + Funkeln', lambda k, m, r: sparkles(build(g(k), m, r, 'dark'), m)),
        ('Goldform, mehr Kontrast', lambda k, m, r: ps.outline(build(g(k), m, r[1:], 'dark', by_rank=False), r[0])),
        ('poliert (hell)', lambda k, m, r: build(g(k), m, r[2:], 'dark')),
        ('matt (dunkel)', lambda k, m, r: build(g(k), m, r[:-2], 'dark')),
        ('Fruchtfarbe durchscheinend', lambda k, m, r: tinted(k, m, r)),
        ('Adern (Glut / Rosa)', lambda k, m, r: veins(build(g(k), m, r, 'dark'), m)),
    ]


def main():
    rows = [('Netherit-Apfel', 'apple', 'netherite', NETHERITE), ('Enderit-Apfel', 'apple', 'enderite', ENDERITE),
            ('Netherit-Karotte', 'carrot', 'netherite', NETHERITE), ('Enderit-Karotte', 'carrot', 'enderite', ENDERITE)]
    out_rows, named = [], {}
    for label, kind, metal, ramp in rows:
        imgs = []
        for k, (_, fn) in enumerate(sets()):
            im = fn(kind, metal, ramp)
            imgs.append(im)
            named[f'{metal}_{kind}_{ps.LETTERS[k].lower()}'] = im
        out_rows.append((label, imgs))
    item = lambda n: ps.load(os.path.join(ps.SB_ITEM, n + '.png'))
    refs = [('Netherit-Apfel jetzt', item('netherite_apple')), ('Enderit-Apfel jetzt', item('enderite_apple')),
            ('Netherit-Karotte jetzt', item('netherite_carrot')), ('Enderit-Karotte jetzt', item('enderite_carrot')),
            ('Goldener Apfel', vitem('golden_apple')), ('Goldene Karotte', vitem('golden_carrot')),
            ('Netheritbarren', vitem('netherite_ingot')), ('Enderitbarren', item('enderite_ingot'))]
    labels = [f'{ps.LETTERS[k]}: {name}' for k, (name, _) in enumerate(sets())]
    ps.sheet('Netherit-/Enderit-Apfel und -Karotte - 10 Vorschlaege (je Spalte ein Satz)', refs, out_rows, PREVIEW,
             notes=['  '.join(labels[:5]), '  '.join(labels[5:])])
    if PNG_DIR:
        ps.save_pngs(PNG_DIR, named)
    print('ok')


if __name__ == '__main__':
    main()
