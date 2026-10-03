"""Usage: python tools/textures/astralit_nihilit_proposals_2026_10_03.py <vanilla textures dir> [preview png]

Owner 2026-10-03: Astralit Block and Nihilith Block (the base blocks; polished/bricks follow later) - per block three
new textures (A-C) and three contrast adjustments of the current one (D stronger, E softer, F different: the accent
specks lifted, the base noise calmed). Proposals only. Every texture tiles: the new ones take the stone pattern of a
tiling vanilla block (calcite, amethyst block, dripstone / packed ice) recoloured onto the block's own colours by
brightness rank; the contrast variants change colours only. The preview shows each one as a 3x3 tiling."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'astralit-nihilit-vorschau.png')
BLOCK = os.path.join(ps.ROOT, 'src', 'main', 'resources', 'assets', 'simplebuilding', 'textures', 'block')


def vblock(name):
    return ps.load(os.path.join(V, 'block', name + '.png'))


def current(name):
    return ps.load(os.path.join(BLOCK, name + '.png'))


def is_accent(c, base_hue):
    """A speck that is not of the block's own hue (nihilith's teal specks, astralit's white sparkles)."""
    r, g, b = c
    if base_hue == 'pink':
        return min(r, g, b) > 225
    return g > r + 40 and g > b - 30


def body_colours(im, hue):
    return sorted([im.getpixel(p)[:3] for p in ps.opaque(im) if not is_accent(im.getpixel(p)[:3], hue)], key=ps.lum)


def matched(src, cols):
    pts = sorted(ps.opaque(src), key=lambda p: (ps.lum(src.getpixel(p)), p[1], p[0]))
    out = Image.new('RGBA', (16, 16))
    for i, p in enumerate(pts):
        out.putpixel(p, cols[min(len(cols) - 1, int((i + 0.5) / len(pts) * len(cols)))] + (255,))
    return out


def keep_accents(new, old, hue):
    out = new.copy()
    for p in ps.opaque(old):
        if is_accent(old.getpixel(p)[:3], hue):
            out.putpixel(p, old.getpixel(p))
    return out


def contrast(im, k, hue, accents_too=True):
    pts = ps.opaque(im)
    mean = [sum(im.getpixel(p)[i] for p in pts) / len(pts) for i in range(3)]
    out = im.copy()
    for p in pts:
        c = im.getpixel(p)[:3]
        if not accents_too and is_accent(c, hue):
            continue
        out.putpixel(p, tuple(max(0, min(255, round(mean[i] + (c[i] - mean[i]) * k))) for i in range(3)) + (255,))
    return out


def different(im, hue):
    """Base noise calmed (contrast 0.6), accents lifted towards white so they read as crystal specks."""
    out = contrast(im, 0.6, hue, accents_too=False)
    for p in ps.opaque(im):
        c = im.getpixel(p)[:3]
        if is_accent(c, hue):
            out.putpixel(p, ps.mix(c, (255, 255, 255), 0.35) + (255,))
    return out


def tile(im, n=3):
    out = Image.new('RGBA', (16 * n, 16 * n))
    for y in range(n):
        for x in range(n):
            out.alpha_composite(im, (16 * x, 16 * y))
    return out


def variants(name, hue, sources):
    cur = current(name)
    cols = body_colours(cur, hue)
    new = [(f'neu: Muster {src}', keep_accents(matched(vblock(src), cols), cur, hue)) for src in sources]
    return cur, new + [('Kontrast staerker', contrast(cur, 1.45, hue)), ('Kontrast schwaecher', contrast(cur, 0.65, hue)),
                       ('anders: Grund ruhiger, Kristallpunkte heller', different(cur, hue))]


def main():
    blocks = [('Astralit-Block', 'astralit_block', 'pink', ('calcite', 'amethyst_block', 'dripstone_block')),
              ('Nihilith-Block', 'nihilith_block', 'blue', ('calcite', 'amethyst_block', 'packed_ice'))]
    s = 4
    cell = 48 * s + 16
    im = Image.new('RGBA', (150 + 7 * cell, 40 + len(blocks) * (cell + 40) + 20), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((10, 8), 'Astralit / Nihilith - je 3 neue Texturen (A-C) und 3 Kontrast-Anpassungen (D-F), jeweils 3x3 gekachelt',
           fill=(0, 0, 0, 255))
    y = 34
    for label, name, hue, sources in blocks:
        cur, vs = variants(name, hue, sources)
        d.text((10, y + cell // 2), label, fill=(0, 0, 0, 255))
        cells = [('jetzt', cur)] + [(f'{ps.LETTERS[k]}: {n}', v) for k, (n, v) in enumerate(vs)]
        for k, (n, v) in enumerate(cells):
            x = 150 + k * cell
            d.text((x, y), n[:34], fill=(0, 0, 0, 255))
            im.alpha_composite(tile(v).resize((48 * s, 48 * s), Image.NEAREST), (x, y + 16))
        y += cell + 40
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    im.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
