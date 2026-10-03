"""Usage: python tools/textures/raw_enderite_scrap_v3_2026_10_03.py <vanilla textures dir> [preview png]

Owner 2026-10-03: "the colours are not right" - ten further proposals A-J for the Raw Enderite Scrap on the owner's
own form (tools/textures/hand/owner/layered_raw_enderite_owner.png; his ten tones, ranked dark -> light, are what the
palettes below are laid onto). Proposals only. Derived, not guessed:

Vanilla analysis (printed when run, shown as colour strips in the preview):
- netherite_scrap has 7 tones, netherite_ingot 10. Six of the scrap's seven tones are exactly ancient_debris's -
  the scrap takes its colours from the ORE, not from the ingot; only its darkest tone is darker than the ore's.
- Against the ingot, per brightness quantile the scrap is brighter (value x ~1.3), far more saturated (ingot nearly
  grey, scrap 0.15-0.69) and its hue sits about +20 degrees from the ingot's slight red tint (orange-brown).
A-C use the proportion enderite scrap : enderite ingot = netherite scrap : netherite ingot, quantile by quantile
  (see derived_palette): A exactly (7 tones like the scrap, hue +9, darkest tone darkened like the scrap's); B the same without the hue shift (stays in the ingot's violet); C as A but spread over 10 tones.
D-F take the colours straight from the installed enderite family: D the Raw Enderite Fragment (the closest thing
  to an enderite ore), E ingot + nugget, F the enderite block with a stretched contrast.
G-J freer, still clearly enderite: G an end-stone crust on the top faces (ancient debris's crust idea), H a violet
  core under a dark mantle, I variant A with pearl-teal glints, J variant A with stronger layering (the undersides
  of each layer one tone darker)."""
import colorsys
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build', 'raw-enderite-scrap-v3-vorschau.png')
sys.argv = [sys.argv[0], V]
import proposal_sheet_2026_10_02 as ps  # noqa: E402

PINK, PINK_LIGHT = (199, 125, 255), (244, 210, 255)
END_STONE = [(196, 194, 140), (220, 222, 158), (236, 240, 182)]
PEARL = [(40, 120, 110), (100, 196, 176)]


def vtex(path):
    return Image.open(os.path.join(V, path + '.png')).convert('RGBA')


def weighted(im, exclude=()):
    """Every opaque pixel's colour, dark -> light (duplicates kept: they carry the proportions)."""
    return sorted((im.getpixel((x, y))[:3] for y in range(im.height) for x in range(im.width)
                   if im.getpixel((x, y))[3] == 255 and im.getpixel((x, y))[:3] not in exclude), key=ps.lum)


def at(cols, q):
    return cols[min(len(cols) - 1, int(q * len(cols)))]


def hsv(c):
    return colorsys.rgb_to_hsv(*[v / 255 for v in c])


def rgb(h, s, v):
    return tuple(round(max(0, min(1, x)) * 255) for x in colorsys.hsv_to_rgb(h % 1.0, s, v))


def vanilla_transform(steps=7):
    """Per-quantile (saturation gain, value ratio) of netherite_scrap against netherite_ingot, and the hue shift
    between the ingot's tinted tones and the scrap."""
    ingot, scrap = weighted(vtex('item/netherite_ingot')), weighted(vtex('item/netherite_scrap'))
    qs = [(i + 0.5) / steps for i in range(steps)]
    table = []
    for q in qs:
        hi, si, vi = hsv(at(ingot, q))
        hs, ss, vs = hsv(at(scrap, q))
        table.append((q, ss - si, vs / max(vi, 0.03)))
    tinted = [c for c in ingot if hsv(c)[1] > 0.1]
    hue_ingot = math.atan2(sum(math.sin(2 * math.pi * hsv(c)[0]) for c in tinted),
                           sum(math.cos(2 * math.pi * hsv(c)[0]) for c in tinted)) / (2 * math.pi)
    hue_scrap = math.atan2(sum(math.sin(2 * math.pi * hsv(c)[0]) for c in scrap),
                           sum(math.cos(2 * math.pi * hsv(c)[0]) for c in scrap)) / (2 * math.pi)
    shift = ((hue_scrap - hue_ingot + 0.5) % 1.0) - 0.5
    return table, shift, ingot, scrap


def derived_palette(steps=7, hue_shift=True, darken_darkest=True):
    """Proportional analogy, quantile by quantile: enderite scrap : enderite ingot = netherite scrap : netherite ingot.
    The netherite scrap's tone at quantile q is moved by exactly the step that takes the netherite ingot's tone at q to
    the enderite ingot's tone at q - value scaled by v_enderite_ingot / v_netherite_ingot, saturation raised by
    s_enderite_ingot - s_netherite_ingot - and its hue becomes the enderite ingot's hue plus the scrap's own measured
    offset from its ingot, halved (+9 degrees; the netherite ingot is nearly grey, so its hue cannot carry the step,
    and the full +18 turns violet into magenta).
    (Applying the netherite ingot -> scrap step to the enderite ingot directly, the first attempt, over-saturates:
    the netherite ingot starts grey, the enderite ingot already violet.)"""
    _, shift, n_ingot, n_scrap = vanilla_transform(steps)
    e_ingot = weighted(ps.load(os.path.join(ps.SB_ITEM, 'enderite_ingot.png')), exclude=(PINK, PINK_LIGHT))
    mean_ss = sum(hsv(c)[1] for c in n_scrap) / len(n_scrap)
    out = []
    for k in range(steps):
        q = (k + 0.5) / steps
        _, sn, vn = hsv(at(n_ingot, q))
        he, se, ve = hsv(at(e_ingot, q))
        _, ss, vs = hsv(at(n_scrap, q))
        v2 = vs * ve / max(vn, 0.03)
        # saturation: the scrap's profile across the quantiles (dark tones richer, light tones paler), scaled to
        # the enderite ingot's overall saturation - a straight transfer of the netherite numbers turns neon violet
        s2 = se * ss / mean_ss
        if darken_darkest and k == 0:
            v2 *= 0.75
        out.append(rgb(he + (shift / 2 if hue_shift else 0), min(1.0, s2), min(1.0, v2)))
    return sorted(out, key=ps.lum)


def resample(cols, n):
    return [cols[round(i * (len(cols) - 1) / (n - 1))] for i in range(n)]


def owner_form():
    return ps.load(os.path.join(HERE, 'hand', 'owner', 'layered_raw_enderite_owner.png'))


def lay(palette, src=None, contrast=1.0):
    """The owner's tones (ranked) onto the palette; contrast > 1 pushes the ranks apart."""
    src = src or owner_form()
    tones = sorted({src.getpixel(p)[:3] for p in ps.opaque(src)}, key=ps.lum)
    out = src.copy()
    for i, c in enumerate(tones):
        q = i / max(1, len(tones) - 1)
        q = min(1.0, max(0.0, 0.5 + (q - 0.5) * contrast))
        t = palette[round(q * (len(palette) - 1))]
        for p in ps.opaque(src):
            if src.getpixel(p)[:3] == c:
                out.putpixel(p, t + (255,))
    return out


def tops(im):
    """Pixels on a top face: nothing (or something much darker) directly above."""
    out = []
    for (x, y) in ps.opaque(im):
        if y == 0 or im.getpixel((x, y - 1))[3] == 0 or ps.lum(im.getpixel((x, y - 1))) < ps.lum(im.getpixel((x, y))) - 45:
            out.append((x, y))
    return out


def undersides(im):
    return [(x, y) for (x, y) in ps.opaque(im)
            if y > 0 and im.getpixel((x, y - 1))[3] and ps.lum(im.getpixel((x, y - 1))) > ps.lum(im.getpixel((x, y))) + 25]


def step_down(palette, c):
    i = min(range(len(palette)), key=lambda k: sum((palette[k][j] - c[j]) ** 2 for j in range(3)))
    return palette[max(0, i - 1)]


def variants():
    a_pal = derived_palette()
    a = lay(a_pal)
    family = lambda n: ps.load(os.path.join(ps.SB_ITEM, n + '.png'))
    out = [
        ('A Vanilla-Transformation exakt', a),
        ('B ohne Farbton-Verschiebung', lay(derived_palette(hue_shift=False))),
        ('C wie A, 10 Stufen', lay(resample(derived_palette(steps=10), 10))),
        ('D Farben vom Enderit-Fragment', lay(resample(sorted(set(weighted(family('raw_enderite'))), key=ps.lum), 10))),
        ('E Farben Barren + Nugget', lay(resample(sorted(set(weighted(family('enderite_ingot'), (PINK, PINK_LIGHT))
                                                             + weighted(family('enderite_nugget')), ), key=ps.lum), 10))),
        ('F Enderitblock, mehr Kontrast', lay(resample(sorted(set(weighted(ps.load(os.path.join(
            ps.ROOT, 'src', 'main', 'resources', 'assets', 'simplebuilding', 'textures', 'block', 'enderite_block.png')))),
            key=ps.lum), 10), contrast=1.3)),
    ]
    g = a.copy()
    for k, p in enumerate(tops(owner_form())):
        if (p[0] + p[1]) % 2 == 0:
            g.putpixel(p, END_STONE[k % len(END_STONE)] + (255,))
    out.append(('G Endstein-Kruste oben', g))
    src = owner_form()
    pts = ps.opaque(src)
    cx = sum(x for x, _ in pts) / len(pts)
    cy = sum(y for _, y in pts) / len(pts)
    rmax = max(math.hypot(x - cx, y - cy) for x, y in pts)
    core_pal = ps.ramp((60, 20, 110), (120, 70, 200), (190, 150, 250), n=6)
    mantle_pal = ps.ramp((16, 6, 30), (40, 18, 72), (70, 40, 112), n=6)
    core, mantle = lay(core_pal), lay(mantle_pal)
    h = core.copy()
    for (x, y) in pts:
        if math.hypot(x - cx, y - cy) > rmax * 0.62:
            h.putpixel((x, y), mantle.getpixel((x, y)))
    out.append(('H violetter Kern, dunkler Mantel', h))
    i = a.copy()
    for k, p in enumerate(sorted(pts, key=lambda p: -ps.lum(a.getpixel(p)))[:3]):
        i.putpixel(p, PEARL[k % 2] + (255,))
    out.append(('I A + Perlen-Glanzpunkte', i))
    j = a.copy()
    for p in undersides(owner_form()):
        j.putpixel(p, step_down(a_pal, a.getpixel(p)) + (255,))
    out.append(('J A + staerkere Schichtung', j))
    return out


def strip(d, im, x, y, cols, label, cell=18):
    d.text((x, y), label, fill=(0, 0, 0, 255))
    for k, c in enumerate(cols):
        d.rectangle([x + k * cell, y + 14, x + (k + 1) * cell - 2, y + 14 + cell], fill=tuple(c) + (255,))


def main():
    table, shift, ingot, scrap = vanilla_transform()
    print(f'netherite: ingot {len(set(ingot))} tones, scrap {len(set(scrap))} tones, hue shift {shift * 360:+.0f} deg')
    for q, ds, vr in table:
        print(f'  quantile {q:.2f}: saturation {ds:+.2f}, value x{vr:.2f}')
    debris = weighted(vtex('block/ancient_debris_side'))
    shared = set(scrap) & set(debris)
    print(f'  scrap tones shared with ancient_debris: {len(shared)} of {len(set(scrap))}')
    vs = variants()
    refs = [('Enderit-Barren', ps.load(os.path.join(ps.SB_ITEM, 'enderite_ingot.png'))),
            ('Enderit-Nugget', ps.load(os.path.join(ps.SB_ITEM, 'enderite_nugget.png'))),
            ('Netheritschrott', vtex('item/netherite_scrap').crop((0, 0, 16, 16))),
            ('Netheritbarren', vtex('item/netherite_ingot').crop((0, 0, 16, 16))),
            ('jetzt eingebaut', ps.load(os.path.join(ps.SB_ITEM, 'layered_raw_enderite.png')))]
    s, cell = 9, 16 * 9 + 12
    width = 20 + max(len(vs), len(refs)) * cell
    im = Image.new('RGBA', (width, 40 + 3 * (cell + 26) + 140), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((10, 6), 'Raw Enderite Scrap v3 - 10 Farbvarianten auf der Besitzer-Form (Herleitung aus Vanilla Barren -> Schrott)',
           fill=(0, 0, 0, 255))
    y = 28
    for k, (label, sprite) in enumerate(refs):
        d.text((10 + k * cell, y), label, fill=(0, 0, 0, 255))
        im.alpha_composite(sprite.resize((16 * s, 16 * s), Image.NEAREST), (10 + k * cell, y + 14))
    y += cell + 26
    for k, (label, sprite) in enumerate(vs):
        d.text((10 + k * cell, y), label[:24], fill=(0, 0, 0, 255))
        im.alpha_composite(sprite.resize((16 * s, 16 * s), Image.NEAREST), (10 + k * cell, y + 14))
    y += cell + 26
    d.text((10, y), 'Slotgroesse (1x und 2x) neben Enderit-Barren, Nugget, Netheritschrott, Netheritbarren:', fill=(0, 0, 0, 255))
    slot_refs = [r for _, r in refs[:4]]
    for k, (_, sprite) in enumerate(vs):
        x = 10 + k * cell
        for j, r in enumerate([sprite] + slot_refs):
            im.alpha_composite(r, (x + (j % 3) * 18, y + 16 + (j // 3) * 18))
        im.alpha_composite(sprite.resize((32, 32), Image.NEAREST), (x + 60, y + 16))
    y += 70
    strip(d, im, 10, y, sorted(set(ingot), key=ps.lum), 'Vanilla Netheritbarren (10 Toene)')
    strip(d, im, 230, y, sorted(set(scrap), key=ps.lum), 'Vanilla Netheritschrott (7 Toene, 6 = Antiker Schutt)')
    strip(d, im, 520, y, sorted(set(weighted(ps.load(os.path.join(ps.SB_ITEM, 'enderite_ingot.png')), (PINK, PINK_LIGHT))),
                                key=ps.lum), 'Enderit-Barren')
    strip(d, im, 790, y, derived_palette(), f'abgeleitete Enderit-Schrott-Palette (A, Farbton {shift * 180:+.0f} Grad)')
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    im.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
