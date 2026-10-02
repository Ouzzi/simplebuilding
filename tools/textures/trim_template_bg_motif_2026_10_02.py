"""Usage: python tools/textures/trim_template_bg_motif_2026_10_02.py <vanilla textures dir> [preview dir]

Owner 2026-10-02, round 2 for the Glowing / Pulsating / Emitting smithing templates: background and motif are now
proposed separately. Proposals only.
- Backgrounds: round-1 set A (the vanilla Sentry template) with its pattern removed - the cyan pixels are filled from
  their neighbours, then the body takes the template's colour - and ten variants of that plain plate.
- Motifs: ten symbol families, each drawn for all three templates on background 1 (A). The effect decides the
  drawing: Glowing = a solid, lit symbol with a soft halo and a glint; Pulsating = the symbol's outline repeated
  outwards like a pulse wave; Emitting = a small core with rays radiating out.
Writes besatz-hintergruende-vorschau.png and besatz-motive-vorschau.png into the preview dir."""
import math
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import trim_template_proposals_2026_10_02 as r1  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build')
r1.V = V
TEMPLATE = 'sentry'
THEMES = ('Glowing', 'Pulsating', 'Emitting')
CX, CY = 7.5, 7.5


def vtex(path):
    return ps.load(os.path.join(V, path + '.png'))


# ---------------------------------------------------------------- backgrounds
def plate_source():
    """The Sentry template with its cyan pattern filled in from the surrounding stone."""
    src = vtex(f'item/{TEMPLATE}_armor_trim_smithing_template')
    out = src.copy()
    todo = {p for p in ps.opaque(src) if r1.is_accent(src.getpixel(p))}
    while todo:
        done = {}
        for (x, y) in todo:
            nb = [out.getpixel((x + dx, y + dy)) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                  if 0 <= x + dx < 16 and 0 <= y + dy < 16 and (x + dx, y + dy) not in todo
                  and out.getpixel((x + dx, y + dy))[3]]
            if nb:
                done[(x, y)] = tuple(round(sum(c[i] for c in nb) / len(nb)) for i in range(3)) + (255,)
        if not done:
            break
        for p, c in done.items():
            out.putpixel(p, c)
        todo -= set(done)
    return out


def edge_pixels(im):
    return {p for p in ps.opaque(im) if any(not (0 <= p[0] + dx < 16 and 0 <= p[1] + dy < 16)
                                            or im.getpixel((p[0] + dx, p[1] + dy))[3] == 0
                                            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}


def step(ramp, colour, k):
    """The ramp colour k steps lighter (k > 0) or darker than `colour`."""
    i = min(range(len(ramp)), key=lambda j: sum((ramp[j][c] - colour[c]) ** 2 for c in range(3)))
    return ramp[max(0, min(len(ramp) - 1, i + k))]


def backgrounds(body):
    src = plate_source()
    base = ps.recolor(src, body)
    edge = edge_pixels(src)
    inner_edge = edge_pixels(_shrink(src))

    def per_pixel(fn):
        out = base.copy()
        for p in ps.opaque(base):
            out.putpixel(p, fn(p, base.getpixel(p)[:3]) + (255,))
        return out

    blurred = ps.blank()
    for (x, y) in ps.opaque(src):
        nb = [src.getpixel((x + dx, y + dy)) for dx in (-1, 0, 1) for dy in (-1, 0, 1)
              if 0 <= x + dx < 16 and 0 <= y + dy < 16 and src.getpixel((x + dx, y + dy))[3]]
        blurred.putpixel((x, y), tuple(round(sum(c[i] for c in nb) / len(nb)) for i in range(3)) + (255,))
    return [
        ('Satz A ohne Motiv', base),
        ('mehr Kontrast', ps.recolor(src, body, by_rank=False)),
        ('dunkler', ps.recolor(src, body[:-2])),
        ('heller', ps.recolor(src, body[2:])),
        ('dunkler Rand', per_pixel(lambda p, c: body[0] if p in edge else c)),
        ('weich (geglaettet)', ps.recolor(blurred, body)),
        ('Innenrahmen hell', per_pixel(lambda p, c: step(body, c, 2) if p in inner_edge else c)),
        ('Steinkorn', per_pixel(lambda p, c: step(body, c, 1) if (p[0] * 3 + p[1] * 5) % 7 == 0 else c)),
        ('Licht oben links', per_pixel(lambda p, c: step(body, c, 1 if p[0] + p[1] < 14 else -1))),
        ('Fase oben/links hell', per_pixel(lambda p, c: step(body, c, 2) if p in edge and (
            p[1] == 0 or base.getpixel((p[0], p[1] - 1))[3] == 0 or p[0] == 0 or base.getpixel((p[0] - 1, p[1]))[3] == 0)
            else (body[0] if p in edge else c))),
    ]


def _shrink(im):
    out = ps.blank()
    edge = edge_pixels(im)
    for p in ps.opaque(im):
        if p not in edge:
            out.putpixel(p, im.getpixel(p))
    return out


# ---------------------------------------------------------------- motifs
def shape_fns():
    """Ten base symbols as inside(x, y, scale) tests around the plate centre (pixel centres)."""
    def d(x, y):
        return x + 0.5 - CX, y + 0.5 - CY
    return [
        ('Kreis', lambda x, y, s: math.hypot(*d(x, y)) <= 1.9 * s),
        ('Raute', lambda x, y, s: abs(d(x, y)[0]) + abs(d(x, y)[1]) <= 2.2 * s),
        ('Quadrat', lambda x, y, s: max(abs(d(x, y)[0]), abs(d(x, y)[1])) <= 1.5 * s),
        ('Kreuz', lambda x, y, s: (abs(d(x, y)[0]) <= 0.6 * s and abs(d(x, y)[1]) <= 2.6 * s)
            or (abs(d(x, y)[1]) <= 0.6 * s and abs(d(x, y)[0]) <= 2.6 * s)),
        ('Vierstern', lambda x, y, s: abs(d(x, y)[0]) ** 0.5 + abs(d(x, y)[1]) ** 0.5 <= 1.75 * s ** 0.5),
        ('Dreieck', lambda x, y, s: d(x, y)[1] <= 1.6 * s and d(x, y)[1] >= -2.2 * s
            and abs(d(x, y)[0]) <= (d(x, y)[1] + 2.2 * s) * 0.62),
        ('Tropfen', lambda x, y, s: math.hypot(d(x, y)[0], d(x, y)[1] - 0.6 * s) <= 1.6 * s
            or (d(x, y)[1] < 0.6 * s and d(x, y)[1] >= -2.6 * s and abs(d(x, y)[0]) <= (d(x, y)[1] + 2.6 * s) * 0.5)),
        ('Auge', lambda x, y, s: (d(x, y)[0] / (2.8 * s)) ** 2 + (d(x, y)[1] / (1.4 * s)) ** 2 <= 1),
        ('Sechseck', lambda x, y, s: max(abs(d(x, y)[0]) * 0.866 + abs(d(x, y)[1]) * 0.5, abs(d(x, y)[1])) <= 1.8 * s),
        ('Ring mit Punkt', lambda x, y, s: math.hypot(*d(x, y)) <= 0.8 * s or 1.6 * s <= math.hypot(*d(x, y)) <= 2.4 * s),
    ]


def mask(fn, s):
    return {(x, y) for y in range(16) for x in range(16) if fn(x, y, s)}


def outline_of(cells):
    return {c for c in cells if any((c[0] + dx, c[1] + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}


def draw_motif(plate, fn, effect, acc):
    """acc: accent ramp dark -> light (at least 4)."""
    im = plate.copy()
    inside = lambda p: 0 <= p[0] < 16 and 0 <= p[1] < 16 and plate.getpixel(p)[3]
    dark, mid, light, top = acc[0], acc[len(acc) // 3], acc[2 * len(acc) // 3], acc[-1]

    def put(p, c):
        if inside(p):
            im.putpixel(p, tuple(c) + (255,))
    core = mask(fn, 1.25 if effect == 'Glowing' else 1.0)
    if effect == 'Glowing':
        halo = {(x + dx, y + dy) for x, y in core for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))} - core
        for p in halo:
            if inside(p):
                put(p, ps.mix(plate.getpixel(p)[:3], mid, 0.6))
        for x, y in core:
            put((x, y), top if (x + y) < CX + CY - 2 else light if (x + y) < CX + CY + 1 else mid)
        glint = min(core, key=lambda p: (p[0] + p[1], p[1]))
        put(glint, top)
        put((glint[0] + 1, glint[1]), top)
    elif effect == 'Pulsating':
        for s, c in ((2.3, dark), (1.65, mid), (1.0, light)):
            for p in outline_of(mask(fn, s)):
                put(p, c)
        put((7, 7), top)
        put((8, 8), top)
    else:  # Emitting: small core, eight rays
        small = mask(fn, 0.85) or {(7, 7), (8, 8)}
        for p in small:
            put(p, light)
        for k in range(8):
            a = k * math.pi / 4
            rays = ((3.0, top), (4.0, light), (5.0, mid)) if k % 2 == 0 else ((3.0, light), (4.0, mid))
            for r, c in rays:
                put((math.floor(CX + math.cos(a) * r), math.floor(CY + math.sin(a) * r)), c)
        put(min(small, key=lambda p: (p[0] + p[1], p[1])), top)
    return im


def main():
    th = r1.themes()
    refs = [(n, vtex(f'item/{n}_armor_trim_smithing_template')) for n in ('sentry', 'eye', 'ward', 'spire', 'silence', 'wayfinder', 'rib')]
    item = lambda n: ps.load(os.path.join(ps.SB_ITEM, n + '.png'))
    refs += [('Glowing jetzt', item('glowing_trim_template')), ('Pulsating jetzt', item('pulsating_trim_template')),
             ('Emitting jetzt', item('emitting_trim_template'))]
    rows, names = [], None
    plates = {}
    for name in THEMES:
        bgs = backgrounds(th[name]['body'])
        plates[name] = bgs[0][1]
        names = [n for n, _ in bgs]
        rows.append((name, [im for _, im in bgs]))
    ps.sheet('Besatzvorlagen - 10 Hintergruende ohne Motiv (Grundform: Satz A = Vanilla-Waechter/Sentry)', refs, rows,
             os.path.join(OUT, 'besatz-hintergruende-vorschau.png'),
             notes=['  '.join(f'{ps.LETTERS[k]}: {n}' for k, n in enumerate(names[:5])),
                    '  '.join(f'{ps.LETTERS[k + 5]}: {n}' for k, n in enumerate(names[5:]))])
    rows = []
    shapes = shape_fns()
    for name in THEMES:
        rows.append((name, [draw_motif(plates[name], fn, name, th[name]['accent']) for _, fn in shapes]))
    ps.sheet('Besatzvorlagen - 10 Motive, je auf Hintergrund A (Glowing = Leuchten/Glanz, Pulsating = Puls/Welle, '
             'Emitting = Strahlen)', refs, rows, os.path.join(OUT, 'besatz-motive-vorschau.png'),
             notes=['  '.join(f'{ps.LETTERS[k]}: {n}' for k, (n, _) in enumerate(shapes[:5])),
                    '  '.join(f'{ps.LETTERS[k + 5]}: {n}' for k, (n, _) in enumerate(shapes[5:])),
                    'Glowing: gefuelltes Symbol mit Lichthof und Glanzpunkt; Pulsating: Umriss dreifach nach aussen; '
                    'Emitting: kleiner Kern mit acht Strahlen.'])
    print('ok')


if __name__ == '__main__':
    main()
