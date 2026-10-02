"""Usage: python tools/textures/enderite_nugget_proposals_v2_2026_10_02.py <vanilla textures dir> [preview png] [png dir]

Owner 2026-10-02, round 2 for the Enderite Nugget: the vanilla nugget shapes are right, the colouring should come
closer to the Enderite Ingot. Ten new proposals, proposals only. The main tool is histogram matching: a nugget pixel at
brightness quantile q takes the ingot colour at the same quantile, so dark outline, body tones, highlight and pink
sparkles appear in the ingot's own proportions; the variants differ in shape source, outline and sparkle handling."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'enderit-nugget-v2-vorschau.png')
PNG_DIR = sys.argv[3] if len(sys.argv) > 3 else None

OUTLINE = (28, 10, 51)
PINK, PINK_LIGHT = (199, 125, 255), (244, 210, 255)


def vitem(name):
    return ps.load(os.path.join(V, 'item', name + '.png'))


def ingot():
    return ps.load(os.path.join(ps.SB_ITEM, 'enderite_ingot.png'))


def ingot_colours(include_pink=True, include_outline=True):
    """Every opaque ingot pixel's colour, dark -> light (duplicates kept: they carry the proportions)."""
    im = ingot()
    cols = [im.getpixel(p)[:3] for p in ps.opaque(im)]
    if not include_pink:
        cols = [c for c in cols if c not in (PINK, PINK_LIGHT)]
    if not include_outline:
        cols = [c for c in cols if c != OUTLINE]
    return sorted(cols, key=ps.lum)


def matched(src, cols, contrast=1.0):
    """Histogram matching onto the ingot colours; contrast > 1 pushes the quantiles towards the ends."""
    pts = sorted(ps.opaque(src), key=lambda p: (ps.lum(src.getpixel(p)), p[1], p[0]))
    out = ps.blank()
    for i, p in enumerate(pts):
        q = (i + 0.5) / len(pts)
        q = min(1.0, max(0.0, 0.5 + (q - 0.5) * contrast))
        out.putpixel(p, cols[min(len(cols) - 1, int(q * len(cols)))] + (255,))
    return out


def with_outline(src, inner_cols):
    """Edge pixels of the shape (touching transparency) get the ingot outline, the rest is matched."""
    edge = [p for p in ps.opaque(src) if any(not (0 <= p[0] + dx < 16 and 0 <= p[1] + dy < 16)
                                             or src.getpixel((p[0] + dx, p[1] + dy))[3] == 0
                                             for dx, dy in ((1, 0), (0, 1)))]   # right/bottom edge = shadow side
    inner = ps.blank()
    for p in ps.opaque(src):
        if p not in edge:
            inner.putpixel(p, src.getpixel(p))
    out = matched(inner, inner_cols)
    for p in edge:
        out.putpixel(p, OUTLINE + (255,))
    return out


def sparkle(im, n=1):
    """Brightest pixel(s) become the ingot's pink highlight, like the sparkles on the bar."""
    out = im.copy()
    pts = sorted(ps.opaque(im), key=lambda p: (-ps.lum(im.getpixel(p)), p[1], p[0]))
    for k, p in enumerate(pts[:n]):
        out.putpixel(p, (PINK_LIGHT if k == 0 else PINK) + (255,))
    return out


def proposals():
    gold, iron, copper = vitem('gold_nugget'), vitem('iron_nugget'), vitem('copper_nugget')
    full = ingot_colours()
    plain = ingot_colours(include_pink=False)
    body = ingot_colours(include_pink=False, include_outline=False)
    return [
        ('Gold-Form, Barren-Verteilung', matched(gold, plain)),
        ('Eisen-Form, Barren-Verteilung', matched(iron, plain)),
        ('Kupfer-Form, Barren-Verteilung', matched(copper, plain)),
        ('Gold-Form + rosa Glanz', sparkle(matched(gold, plain))),
        ('Eisen-Form + rosa Glanz', sparkle(matched(iron, plain))),
        ('Kupfer-Form + 2 Glanzpunkte', sparkle(matched(copper, plain), 2)),
        ('Gold-Form, Barrenrand unten/rechts', sparkle(with_outline(gold, body))),
        ('Eisen-Form, Barrenrand unten/rechts', with_outline(iron, body)),
        ('Gold-Form, alle Barrenfarben', matched(gold, full)),
        ('Kupfer-Form, mehr Kontrast', sparkle(matched(copper, plain, contrast=1.35))),
    ]


def main():
    props = proposals()
    refs = [('Enderit-Barren', ingot()),
            ('Nugget jetzt', ps.load(os.path.join(ps.SB_ITEM, 'enderite_nugget.png'))),
            ('Goldnugget', vitem('gold_nugget')), ('Eisennugget', vitem('iron_nugget')),
            ('Kupfernugget', vitem('copper_nugget')), ('Netheritbarren', vitem('netherite_ingot'))]
    # the ingot next to every proposal row for a direct comparison
    rows = [('Vorschlag', [im for _, im in props]), ('neben Barren', [_pair(im) for _, im in props])]
    notes = [f'{ps.LETTERS[k]}: {name}' for k, (name, _) in enumerate(props)]
    ps.sheet('Enderit-Nugget Runde 2 - 10 neue Vorschlaege (Vanilla-Nugget-Formen, Farbverteilung des Enderit-Barrens)',
             refs, rows, PREVIEW, notes=['  '.join(notes[:5]), '  '.join(notes[5:]),
                                         'Zeile 2: links unten der Barren klein, rechts oben das Nugget - wie im Inventar nebeneinander.'])
    if PNG_DIR:
        ps.save_pngs(PNG_DIR, {f'enderite_nugget_v2_{ps.LETTERS[k].lower()}': im for k, (_, im) in enumerate(props)})
    print('ok')


def _pair(nugget):
    """Ingot at half size bottom left, the nugget top right, in one 16x16 cell."""
    from PIL import Image
    im = ps.blank()
    small = ingot().resize((8, 8), Image.NEAREST)
    im.alpha_composite(small, (0, 8))
    box = nugget.getbbox()
    piece = nugget.crop(box)
    im.alpha_composite(piece, (16 - piece.width, 0))
    return im


if __name__ == '__main__':
    main()
