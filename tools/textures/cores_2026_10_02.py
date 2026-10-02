"""Building cores, owner 2026-10-02: round-4 C (outer ring one pixel in, body as thick as before) with a softer
transition from the material to the nether-star middle: the star's rim pixels that touch the material take on part
of the material colour, and (variant C) the material pixels touching the star catch a little of its light.

Usage: python tools/textures/cores_2026_10_02.py <variant a|b|c> [out dir]  (default out: the 26.3 overlay)"""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
SRC = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'simplebuilding', 'textures', 'item')
OUT = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'item')
CORES = ('copper', 'iron', 'gold', 'diamond', 'netherite', 'enderite')
VARIANTS = {'a': (0.35, 0.0), 'b': (0.55, 0.0), 'c': (0.45, 0.22)}


def _in(v, c=8):
    return v - 1 if v < c else v + 1 if v > c else v


def slim(core):
    """Round-4 C: everything four or more pixels from the middle moves one pixel in; the middle stays."""
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            sx = _in(x) if abs(x - 8) >= 4 else x
            sy = _in(y) if abs(y - 8) >= 4 else y
            if not (0 <= sx < 16 and 0 <= sy < 16):
                continue
            p = core.getpixel((sx, sy))
            if p[3]:
                out.putpixel((x, y), p)
    for y in range(16):
        for x in range(16):
            if abs(x - 8) + abs(y - 8) <= 3 and core.getpixel((x, y))[3]:
                out.putpixel((x, y), core.getpixel((x, y)))
    return out


def star_colours(core):
    return {core.getpixel((x, y))[:3] for y in range(16) for x in range(16)
            if abs(x - 8) + abs(y - 8) <= 3 and core.getpixel((x, y))[3]}


def mix(a, b, f):
    return tuple(round(a[i] * (1 - f) + b[i] * f) for i in range(3))


def soften(core, blend, glow):
    star = star_colours(core)
    im = slim(core)
    src = im.copy()
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if not p[3]:
                continue
            nb = [src.getpixel((x + dx, y + dy)) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                  if 0 <= x + dx < 16 and 0 <= y + dy < 16]
            if p[:3] in star:
                mat = [q[:3] for q in nb if q[3] and q[:3] not in star]
                if mat and blend:
                    avg = tuple(sum(c[i] for c in mat) / len(mat) for i in range(3))
                    im.putpixel((x, y), mix(p[:3], avg, blend) + (255,))
            elif glow:
                lit = [q[:3] for q in nb if q[3] and q[:3] in star]
                if lit:
                    avg = tuple(sum(c[i] for c in lit) / len(lit) for i in range(3))
                    im.putpixel((x, y), mix(p[:3], avg, glow) + (255,))
    return im


def make(name, variant):
    core = Image.open(os.path.join(SRC, f'{name}_core.png')).convert('RGBA').crop((0, 0, 16, 16))
    return soften(core, *VARIANTS[variant])


if __name__ == '__main__':
    variant = sys.argv[1] if len(sys.argv) > 1 else 'b'
    out = sys.argv[2] if len(sys.argv) > 2 else OUT
    os.makedirs(out, exist_ok=True)
    for name in CORES:
        make(name, variant).save(os.path.join(out, f'{name}_core.png'))
    print('ok ->', out)


# ---------------------------------------------------------------------------- round 7: re-shaded (same shape)
# Owner 2026-10-02: "in Ordnung, aber ohne Seele" - same silhouette as round-4 C, shading redone with the vanilla
# ingot ramps (darkest -> highlight). Variants A-E, previewed for iron and copper.
RAMPS = {
    'iron': [(53, 53, 53), (94, 94, 94), (130, 130, 130), (168, 168, 168), (216, 216, 216), (255, 255, 255)],
    'copper': [(109, 52, 33), (138, 65, 41), (193, 90, 54), (231, 124, 86), (252, 153, 130), (251, 195, 182)],
}


def _parts(name):
    core = Image.open(os.path.join(SRC, f'{name}_core.png')).convert('RGBA').crop((0, 0, 16, 16))
    star = star_colours(core)
    shape = slim(core)
    mat = {(x, y) for y in range(16) for x in range(16) if shape.getpixel((x, y))[3] and shape.getpixel((x, y))[:3] not in star}
    allpx = {(x, y) for y in range(16) for x in range(16) if shape.getpixel((x, y))[3]}
    return shape, star, mat, allpx


def _edges(p, allpx):
    x, y = p
    lit = (x - 1, y) not in allpx or (x, y - 1) not in allpx
    dark = (x + 1, y) not in allpx or (x, y + 1) not in allpx
    return lit, dark


def _lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def _hue_shift(c, amount):
    """Painterly hue shift: shadows lean cool (towards blue/violet), highlights warm (towards yellow)."""
    l = _lum(c) / 255
    warm = (l - 0.5) * 2 * amount
    r, g, b = c
    if warm > 0:
        return (min(255, round(r + 40 * warm)), min(255, round(g + 28 * warm)), max(0, round(b - 20 * warm)))
    w = -warm
    return (max(0, round(r - 18 * w)), max(0, round(g - 10 * w)), min(255, round(b + 26 * w)))


def reshade(name, variant, ramp=None):
    """Same silhouette and the same light/dark structure as round-4 C; the material tones are remapped by brightness
    rank onto the vanilla ingot ramp (A), with a painterly hue shift (B), with the star glow spilling onto the
    material and a material-tinted star (C), with dark shadow edges and a stronger tinted star (D), or all of it plus
    bright point tips (E)."""
    shape, star, mat, allpx = _parts(name)
    ramp = ramp or RAMPS[name]
    tones = sorted({shape.getpixel(p)[:3] for p in mat}, key=_lum)
    index = {c: round(i * (len(ramp) - 1) / max(1, len(tones) - 1)) for i, c in enumerate(tones)}
    im = shape.copy()
    for (x, y) in mat:
        k = index[shape.getpixel((x, y))[:3]]
        lit, dark = _edges((x, y), allpx)
        near_star = any((x + dx, y + dy) in allpx and (x + dx, y + dy) not in mat for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if variant in ('c', 'e') and near_star:
            k = min(len(ramp) - 1, k + 1)
        if variant in ('d', 'e') and dark and not lit:
            k = 0
        if variant == 'e' and abs(x - 8) + abs(y - 8) >= 7 and lit:
            k = len(ramp) - 1
        c = ramp[k]
        if variant in ('b', 'e'):
            c = _hue_shift(c, 0.6)
        im.putpixel((x, y), c + (255,))
    if variant in ('c', 'd', 'e'):
        # the nether-star middle takes on some of the material, so star and frame read as one piece
        for (x, y) in allpx - mat:
            p = im.getpixel((x, y))
            im.putpixel((x, y), mix(p[:3], ramp[4], {'c': 0.2, 'd': 0.3, 'e': 0.25}[variant]) + (255,))
    return im
