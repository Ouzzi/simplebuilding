"""Usage: python tools/textures/horseshoe_template_and_riding_books_2026_10_04.py <vanilla textures dir> [preview dir]

Owner 2026-10-04, proposals only:
1. Horseshoe smithing template (simpleriding): "improve the installed one a little" - ten proposals A-J on the
   installed texture, keeping its convention (basic-upgrade layout: a horseshoe instead of the arrow, iron inside,
   copper outside). Each changes one or two things only: outline, shading depth, horseshoe shape/size, nail holes,
   copper highlights, inner frame. -> hufeisen-vorlage-10-vorschau.png
2. Simple Riding books: the module's two enchantments (Leaping, Tailwind) have no book picture of their own. Proposals
   from the owner's OLD SimpleBuilding enchanted books (tools/textures/hand/q1/books/, the pre-texfix pictures) with
   slightly more contrast. -> simpleriding-buecher-vorschau.png"""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build')
RIDING = os.path.join(ps.ROOT, 'modules', 'simpleriding', 'shared', 'resources', 'assets', 'simpleriding', 'textures', 'item')
OLD_BOOKS = os.path.join(HERE, 'hand', 'q1', 'books')


def vitem(name):
    return ps.load(os.path.join(V, 'item', name + '.png'))


# ------------------------------------------------------------------ horseshoe template
def classify(tpl):
    """'iron' = the grey/white horseshoe pixels (low saturation, light), 'edge' = outline, 'copper' = the rest."""
    out = {}
    for p in ps.opaque(tpl):
        c = tpl.getpixel(p)[:3]
        sat = max(c) - min(c)
        if sat < 30 and ps.lum(c) > 90:
            out[p] = 'iron'
        elif ps.lum(c) < 60:
            out[p] = 'edge'
        else:
            out[p] = 'copper'
    return out


def remap(tpl, cls, which, fn):
    out = tpl.copy()
    for p, k in cls.items():
        if k == which:
            out.putpixel(p, fn(p, tpl.getpixel(p)[:3]) + (255,))
    return out


def contrast(c, k, mid=None):
    mid = mid or (128, 128, 128)
    return tuple(max(0, min(255, round(mid[i] + (c[i] - mid[i]) * k))) for i in range(3))


def template_variants():
    tpl = ps.load(os.path.join(RIDING, 'horseshoe_smithing_template.png'))
    cls = classify(tpl)
    copper = sorted({tpl.getpixel(p)[:3] for p, k in cls.items() if k == 'copper'}, key=ps.lum)
    iron = sorted({tpl.getpixel(p)[:3] for p, k in cls.items() if k == 'iron'}, key=ps.lum)
    cmid = copper[len(copper) // 2]
    imid = iron[len(iron) // 2] if iron else (200, 200, 200)
    edge = (58, 26, 14)

    def outline_dark(im):
        out = im.copy()
        for p, k in cls.items():
            x, y = p
            if k != 'iron' and any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or im.getpixel((x + dx, y + dy))[3] == 0
                                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out.putpixel(p, edge + (255,))
        return out

    def iron_outline(im):
        """A one-pixel dark iron rim round the horseshoe (on copper pixels touching it)."""
        out = im.copy()
        for (x, y), k in cls.items():
            if k == 'copper' and any(cls.get((x + dx, y + dy)) == 'iron' for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out.putpixel((x, y), (70, 70, 74, 255))
        return out

    def nails(im):
        """Three nail holes on each arm: the iron pixels at the arm's middle column, every second row, darker."""
        out = im.copy()
        irons = [p for p, k in cls.items() if k == 'iron']
        if not irons:
            return out
        xs = sorted({x for x, _ in irons})
        left, right = xs[0], xs[-1]
        for x in (left, right):
            for (px, py) in irons:
                if px == x and py % 2 == 0:
                    out.putpixel((px, py), (96, 96, 100, 255))
        return out

    def highlight_copper(im):
        out = im.copy()
        for (x, y), k in cls.items():
            if k == 'copper' and (x + y) < 9:
                out.putpixel((x, y), ps.mix(im.getpixel((x, y))[:3], (255, 210, 180), 0.35) + (255,))
        return out

    def warmer(im):
        return remap(im, cls, 'copper', lambda p, c: ps.mix(c, (190, 90, 50), 0.25))

    def frame(im):
        """An inner frame like the netherite template's: copper pixels next to the opaque border one step darker."""
        out = im.copy()
        opaque = set(cls)
        for (x, y), k in cls.items():
            ring = sum(1 for dx in (-2, -1, 0, 1, 2) for dy in (-2, -1, 0, 1, 2) if (x + dx, y + dy) not in opaque)
            if k == 'copper' and 1 <= ring <= 6:
                out.putpixel((x, y), contrast(im.getpixel((x, y))[:3], 0.8, (40, 20, 10)) + (255,))
        return out

    def brighter_iron(im):
        return remap(im, cls, 'iron', lambda p, c: ps.mix(c, (255, 255, 255), 0.25))

    def deeper(im):
        out = remap(im, cls, 'copper', lambda p, c: contrast(c, 1.25, cmid))
        return remap(out, cls, 'iron', lambda p, c: contrast(c, 1.2, imid))

    return [
        ('A dunkler Rand', outline_dark(tpl)),
        ('B mehr Tiefe (Kontrast)', deeper(tpl)),
        ('C Hufeisen mit Eisenkante', iron_outline(tpl)),
        ('D Nagelloecher', nails(tpl)),
        ('E Kupfer-Glanz oben links', highlight_copper(tpl)),
        ('F waermeres Kupfer', warmer(tpl)),
        ('G Innenrahmen wie Netherit', frame(tpl)),
        ('H helleres Eisen', brighter_iron(tpl)),
        ('I Rand + Nagelloecher', nails(outline_dark(tpl))),
        ('J Rand + Tiefe + Eisenkante', iron_outline(deeper(outline_dark(tpl)))),
    ], tpl


# ------------------------------------------------------------------ riding books
def book(name, k):
    im = ps.load(os.path.join(OLD_BOOKS, f'enchanted_book_{name}.png'))
    pts = [p for p in ps.opaque(im)]
    mean = tuple(sum(im.getpixel(p)[i] for p in pts) / len(pts) for i in range(3))
    out = im.copy()
    for p in pts:
        c = im.getpixel(p)
        out.putpixel(p, contrast(c[:3], k, mean) + (c[3],))
    return out


def main():
    variants, tpl = template_variants()
    refs = [('jetzt eingebaut', tpl),
            ('Basis-Vorlage (SB)', ps.load(os.path.join(ps.SB_ITEM, 'basic_upgrade_template.png'))),
            ('Vanilla Netherit-Vorlage', vitem('netherite_upgrade_smithing_template')),
            ('Eisen-Hufeisen', ps.load(os.path.join(RIDING, 'iron_horseshoe.png'))),
            ('Kupfer-Hufeisen', ps.load(os.path.join(RIDING, 'copper_horseshoe.png')))]
    ps.sheet('Hufeisen-Vorlage - 10 kleine Verbesserungen der eingebauten Fassung (Konvention: Hufeisen, innen Eisen, aussen Kupfer)',
             refs, [('Vorschlag', [im for _, im in variants])], os.path.join(OUT, 'hufeisen-vorlage-10-vorschau.png'),
             scale=10, notes=['  '.join(f'{n}' for n, _ in variants[:5]), '  '.join(f'{n}' for n, _ in variants[5:])])
    # Leaping (jump) on the owner's old double-jump book, Tailwind (wind) on his old range / kinetic books
    rows = [('Leaping (Sprung)', [book('double_jump', k) for k in (1.0, 1.12, 1.25)] + [book('versatility', 1.15)]),
            ('Tailwind (Rueckenwind)', [book('range', k) for k in (1.0, 1.12, 1.25)] + [book('kinetic_protection', 1.15)])]
    ps.sheet('Simple Riding - Verzauberungsbuecher aus den ALTEN SimpleBuilding-Buechern des Besitzers, leicht mehr Kontrast',
             [('Vanilla-Buch', vitem('enchanted_book'))], rows, os.path.join(OUT, 'simpleriding-buecher-vorschau.png'),
             scale=10, notes=['A: altes Buch unveraendert  B: Kontrast +12 %  C: Kontrast +25 %  D: anderes altes Buch, +15 %',
                              'Leaping: Doppelsprung-Buch (D: Vielseitigkeit), Tailwind: Reichweite-Buch (D: kinetischer Schutz)'])
    print('ok')


if __name__ == '__main__':
    main()
