"""Usage: python tools/textures/blueprint_proposals_2026_10_03.py <vanilla textures dir> [preview png]

Owner 2026-10-03: ten proposals for the Blueprint (item blueprint; edited/signed would follow the chosen one) that fit
vanilla better. Proposals only. References: map / filled_map (the folded sheet with its brown edge), paper, book,
writable_book. Most proposals take the vanilla map sheet - silhouette, fold and edge shading - in cyanotype blue and
draw the plan in white on it: the mod's current house symbol, a grid, a floor plan; others roll it up or put it in a
folder like the book."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'blaupausen-vorschau.png')
BLUE = [(16, 40, 92), (28, 62, 128), (40, 86, 160), (58, 110, 186), (80, 134, 206)]
EDGE = [(20, 30, 70), (34, 52, 104)]
WHITE = [(176, 200, 232), (226, 238, 252)]
RED = [(140, 30, 30), (200, 60, 50)]


def vitem(name):
    return ps.load(os.path.join(V, 'item', name + '.png'))


def sheet_blue(src):
    """The map's sheet in blue: its brown edge pixels (low brightness, warm) -> dark blue edge, paper -> blue ramp."""
    out = src.copy()
    pts = ps.opaque(src)
    edge = [p for p in pts if src.getpixel(p)[0] - src.getpixel(p)[2] > 40 and ps.lum(src.getpixel(p)) < 150]
    paper = [p for p in pts if p not in edge]
    for group, ramp in ((edge, EDGE), (paper, BLUE[1:])):
        tmp = ps.blank()
        for p in group:
            tmp.putpixel(p, src.getpixel(p))
        rec = ps.recolor(tmp, ramp)
        for p in group:
            out.putpixel(p, rec.getpixel(p))
    return out


def paint(im, pts, colour):
    out = im.copy()
    for p in pts:
        if 0 <= p[0] < 16 and 0 <= p[1] < 16 and out.getpixel(p)[3]:
            out.putpixel(p, colour + (255,))
    return out


def house(x0=5, y0=4):
    """A small white house: roof, two walls, a door - drawn with 1-px lines like the map's markings."""
    roof = [(x0 + 2, y0), (x0 + 1, y0 + 1), (x0 + 3, y0 + 1), (x0, y0 + 2), (x0 + 4, y0 + 2)]
    walls = [(x0, y0 + 3 + i) for i in range(3)] + [(x0 + 4, y0 + 3 + i) for i in range(3)]
    floor = [(x0 + i, y0 + 6) for i in range(5)]
    door = [(x0 + 2, y0 + 5), (x0 + 2, y0 + 4)]
    return roof + walls + floor, door


def grid_lines(step=3, start=3):
    return [(x, y) for y in range(16) for x in range(16) if (x - start) % step == 0 or (y - start) % step == 0]


def floor_plan():
    outer = [(x, 4) for x in range(4, 12)] + [(x, 11) for x in range(4, 12)] + [(4, y) for y in range(4, 12)] + \
            [(11, y) for y in range(4, 12)]
    inner = [(8, y) for y in range(4, 9)] + [(x, 8) for x in range(8, 12)]
    gaps = [(6, 11), (11, 6), (8, 6)]
    return [p for p in outer + inner if p not in gaps]


def rolled():
    """A rolled-up blueprint lying on the diagonal: a cylinder shaded across its width (light top-left), the open end
    at the bottom left showing the white rolled edge."""
    import math
    im = ps.blank()
    for y in range(16):
        for x in range(16):
            # signed distance from the roll's axis (3,12)-(12,3), position along it
            d = ((x - 7.5) + (y - 7.5)) / math.sqrt(2)
            t = ((x - 7.5) - (y - 7.5)) / math.sqrt(2)
            if abs(d) <= 1.9 and abs(t) <= 6.0:
                k = 4 if d < -1.0 else 3 if d < 0 else 2 if d < 1.0 else 1
                im.putpixel((x, y), BLUE[k] + (255,))
    for p, c in (((3, 11), WHITE[1]), ((4, 12), WHITE[0]), ((3, 12), BLUE[0])):
        im.putpixel(p, c + (255,))
    return ps.outline(im, EDGE[0])


def proposals():
    m = sheet_blue(vitem('map'))
    lines, door = house()
    a = paint(paint(m, lines, WHITE[1]), door, WHITE[0])
    inner = [p for p in grid_lines() if 1 <= p[0] <= 14 and 1 <= p[1] <= 14 and m.getpixel(p)[3]
             and ps.lum(m.getpixel(p)) > ps.lum(EDGE[1]) + 10]
    b = m.copy()
    for p in inner:  # faint grid: half way between the blue sheet and the white line colour
        b.putpixel(p, ps.mix(m.getpixel(p)[:3], WHITE[0], 0.45) + (255,))
    c = paint(paint(b, lines, WHITE[1]), door, WHITE[0])
    d = paint(m, floor_plan(), WHITE[1])
    e = paint(paint(sheet_blue(vitem('paper')), lines, WHITE[1]), door, WHITE[0])
    folded = paint(paint(m, lines, WHITE[1]), door, WHITE[0])
    f = paint(paint(folded, [(13, 1), (14, 2), (13, 2)], WHITE[0]), [(14, 1)], (0, 0, 0)) if False else \
        paint(folded, [(12, 1), (13, 1), (13, 2)], WHITE[0])
    border = [(x, y) for y in range(2, 14) for x in range(2, 14) if x in (2, 13) or y in (2, 13)]
    g = paint(paint(paint(m, border, WHITE[0]), lines, WHITE[1]), door, WHITE[0])
    h = rolled()
    ribbon = paint(rolled(), [(7, 7), (8, 6), (8, 7)], RED[1])
    book = vitem('book')
    j = ps.recolor(book, BLUE)
    j = paint(j, house(5, 5)[0], WHITE[1])
    return [('Kartenblatt + Haus', a), ('Kartenblatt + Raster', b), ('Raster + Haus', c), ('Grundriss', d),
            ('Papier + Haus', e), ('Haus + Eselsohr', f), ('weisser Rahmen + Haus', g), ('gerollt', h),
            ('gerollt mit Band', ribbon), ('Mappe (Buch) + Haus', j)]


def main():
    props = proposals()
    item = lambda n: ps.load(os.path.join(ps.SB_ITEM, n + '.png'))
    refs = [('Blaupause jetzt', item('blueprint')), ('bearbeitet jetzt', item('blueprint_edited')),
            ('signiert jetzt', item('blueprint_signed')), ('Karte', vitem('map')), ('Karte (gefuellt)', vitem('filled_map')),
            ('Papier', vitem('paper')), ('Buch', vitem('book')), ('Buch und Feder', vitem('writable_book'))]
    notes = [f'{ps.LETTERS[k]}: {n}' for k, (n, _) in enumerate(props)]
    ps.sheet('Blaupause - 10 Vorschlaege naeher an Vanilla (Karte/Papier/Buch), Blau wie Lichtpause, Linien weiss', refs,
             [('Vorschlag', [im for _, im in props])], PREVIEW, scale=10, notes=['  '.join(notes[:5]), '  '.join(notes[5:])])
    print('ok')


if __name__ == '__main__':
    main()
