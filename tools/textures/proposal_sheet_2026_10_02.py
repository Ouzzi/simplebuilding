"""Shared helpers for the 2026-10-02 proposal rounds (enderite nugget, trim templates, foods, money fibres).

Recolouring works like the settled sage/enderite textures: a source sprite's opaque pixels are ranked by brightness
and mapped onto a target ramp, so vanilla shapes and shading survive. Sheets are labelled A, B, C ... and scaled up."""
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
SB_ITEM = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'simplebuilding', 'textures', 'item')
MONEY_ITEM = os.path.join(ROOT, 'modules', 'simplemoney', 'shared', 'resources', 'assets', 'simplemoney', 'textures', 'item')
LETTERS = 'ABCDEFGHIJKLMNOP'


def load(path):
    return Image.open(path).convert('RGBA').crop((0, 0, 16, 16))


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def blank():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def opaque(im):
    return [(x, y) for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3]]


def ramp_of(im, n=None):
    """Distinct opaque colours of a sprite, dark -> light (optionally thinned to n steps)."""
    cols = sorted({im.getpixel(p)[:3] for p in opaque(im)}, key=lum)
    if n and len(cols) > n:
        cols = [cols[round(i * (len(cols) - 1) / (n - 1))] for i in range(n)]
    return cols


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def ramp(*stops, n=7):
    """Even ramp through the given colours."""
    out = []
    for i in range(n):
        t = i / (n - 1) * (len(stops) - 1)
        k = min(len(stops) - 2, int(t))
        out.append(mix(stops[k], stops[k + 1], t - k))
    return out


def recolor(im, target, mask=None, by_rank=True):
    """Map the (masked) opaque pixels onto target (dark -> light) by brightness rank (or by absolute brightness)."""
    out = im.copy()
    pts = [p for p in opaque(im) if mask is None or mask(im.getpixel(p))]
    if not pts:
        return out
    if by_rank:
        levels = sorted({round(lum(im.getpixel(p))) for p in pts})
        for p in pts:
            i = levels.index(round(lum(im.getpixel(p))))
            k = round(i * (len(target) - 1) / max(1, len(levels) - 1))
            out.putpixel(p, target[k] + (im.getpixel(p)[3],))
    else:
        lo = min(lum(im.getpixel(p)) for p in pts)
        hi = max(lum(im.getpixel(p)) for p in pts)
        for p in pts:
            t = (lum(im.getpixel(p)) - lo) / max(1, hi - lo)
            out.putpixel(p, target[round(t * (len(target) - 1))] + (im.getpixel(p)[3],))
    return out


def saturated(p, limit=40):
    return max(p[:3]) - min(p[:3]) > limit


def px(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), tuple(c[:3]) + (255,))


def grid(rows, pal):
    im = blank()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                px(im, x, y, pal[ch])
    return im


def outline(im, colour, diagonal=False):
    """Add a one-pixel outline around the opaque shape."""
    out = im.copy()
    nb = [(1, 0), (-1, 0), (0, 1), (0, -1)] + ([(1, 1), (-1, -1), (1, -1), (-1, 1)] if diagonal else [])
    for y in range(16):
        for x in range(16):
            if im.getpixel((x, y))[3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and im.getpixel((x + dx, y + dy))[3] for dx, dy in nb):
                out.putpixel((x, y), tuple(colour) + (255,))
    return out


def shift(im, dx, dy):
    out = blank()
    out.alpha_composite(im, (dx, dy))
    return out


def over(*layers):
    out = blank()
    for layer in layers:
        out.alpha_composite(layer)
    return out


def sheet(title, refs, rows, path, scale=8, bg=(139, 139, 139, 255), notes=None):
    """refs: [(label, image)]; rows: [(row label, [image, ...])] or a flat list of (label, image) for a single row
    of proposals. Proposal columns are labelled A, B, C ..."""
    cell = 16 * scale + 10
    cols = max([len(refs)] + [len(imgs) for _, imgs in rows])
    left = 150
    height = 34 + (cell + 22) * (len(rows) + (1 if refs else 0)) + (16 * len(notes or []))
    im = Image.new('RGBA', (left + cols * cell + 10, height), bg)
    d = ImageDraw.Draw(im)
    d.text((10, 8), title, fill=(0, 0, 0, 255))
    y = 30
    if refs:
        d.text((10, y + cell // 2), 'Bestand/Vanilla', fill=(0, 0, 0, 255))
        for k, (label, ref) in enumerate(refs):
            x = left + k * cell
            d.text((x, y), label[:20], fill=(0, 0, 0, 255))
            im.alpha_composite(ref.resize((16 * scale, 16 * scale), Image.NEAREST), (x, y + 14))
        y += cell + 22
        d.line([(10, y - 6), (im.width - 10, y - 6)], fill=(90, 90, 90, 255))
    for label, imgs in rows:
        d.text((10, y + cell // 2), label, fill=(0, 0, 0, 255))
        for k, sprite in enumerate(imgs):
            x = left + k * cell
            d.text((x, y), LETTERS[k], fill=(0, 0, 0, 255))
            im.alpha_composite(sprite.resize((16 * scale, 16 * scale), Image.NEAREST), (x, y + 14))
        y += cell + 22
    for n in notes or []:
        d.text((10, y), n, fill=(0, 0, 0, 255))
        y += 16
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    im.save(path)


def save_pngs(directory, named):
    os.makedirs(directory, exist_ok=True)
    for name, sprite in named.items():
        sprite.save(os.path.join(directory, name + '.png'))
