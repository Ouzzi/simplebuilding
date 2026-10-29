"""Iso preview renderer for block models (preview only, Pillow). Viewer at the north-west corner above: left face = north,
right face = west, top = up (like the inventory icon). Used by shops29_2026_10_29.py for the shop preview page."""
import json
import math
import os

from PIL import Image

S = 8          # source texture upscale per pixel
A = 0.866      # horizontal step per block unit
B = 0.5        # vertical step per block unit along the ground
H = 1.0        # height per block unit
SHADE = {'up': 1.0, 'north': 0.82, 'west': 0.62}


def _proj(p, k):
    x, y, z = p
    return ((-x + z) * A * k, (-(x + z)) * B * k - y * H * k)


def _shade(img, f):
    r, g, b, a = img.split()
    f = f
    r, g, b = (c.point(lambda v: min(255, int(v * f))) for c in (r, g, b))
    return Image.merge('RGBA', (r, g, b, a))


def _face(canvas, tex, corners, k, ox, oy, shade):
    """tex: RGBA PIL image (already cropped/rotated); corners: world TL, TR, BL."""
    tl, tr, bl = (_proj(c, k) for c in corners)
    w, h = tex.size
    tex = _shade(tex, shade)
    # screen = tl + u*(tr-tl)/w + v*(bl-tl)/h
    ux, uy = (tr[0] - tl[0]) / w, (tr[1] - tl[1]) / w
    vx, vy = (bl[0] - tl[0]) / h, (bl[1] - tl[1]) / h
    det = ux * vy - uy * vx
    if abs(det) < 1e-9:
        return
    xs = [tl[0], tr[0], bl[0], tr[0] + bl[0] - tl[0]]
    ys = [tl[1], tr[1], bl[1], tr[1] + bl[1] - tl[1]]
    x0, x1, y0, y1 = math.floor(min(xs)), math.ceil(max(xs)), math.floor(min(ys)), math.ceil(max(ys))
    W, Hh = x1 - x0, y1 - y0
    # output (X,Y) -> input (u,v): inverse of the 2x2
    ia, ib = vy / det, -vx / det
    ic, id_ = -uy / det, ux / det
    # X' = X + x0 - tl.x ; u = ia*X' + ib*Y' ; v = ic*X' + id*Y'
    cx, cy = x0 - tl[0], y0 - tl[1]
    coeffs = (ia, ib, ia * cx + ib * cy, ic, id_, ic * cx + id_ * cy)
    out = tex.transform((W, Hh), Image.AFFINE, coeffs, Image.NEAREST)
    canvas.alpha_composite(out, (int(x0 + ox), int(y0 + oy)))


def resolve(textures, ref):
    while isinstance(ref, str) and ref.startswith('#'):
        ref = textures.get(ref[1:])
    return ref


def blocks_to_image(items, k=7, margin=16, bg=(0, 0, 0, 0)):
    """items: list of (model_dict, {texture_ref: PIL RGBA}, (bx, by, bz) in blocks). Texture refs are the model's texture
    values (e.g. 'simpleshops:block/sales_block_side') or its '#names'. Returns an RGBA image."""
    faces = []
    for model, texmap, (bx, by, bz) in items:
        t = dict(model.get('textures', {}))
        for e in model['elements']:
            (lx0, ly0, lz0), (lx1, ly1, lz1) = e['from'], e['to']
            x0, x1, y0, y1, z0, z1 = lx0 + bx * 16, lx1 + bx * 16, ly0 + by * 16, ly1 + by * 16, lz0 + bz * 16, lz1 + bz * 16
            key = (-((x0 + x1 + z0 + z1) / 2), (y0 + y1) / 2)
            for name, f in e['faces'].items():
                if name not in SHADE:
                    continue
                ref = resolve(t, f['texture'])
                img = texmap[ref]
                if name == 'north':
                    uv = f.get('uv', [16 - lx1, 16 - ly1, 16 - lx0, 16 - ly0])
                    c = ((x1, y1, z0), (x0, y1, z0), (x1, y0, z0))
                elif name == 'west':
                    uv = f.get('uv', [lz0, 16 - ly1, lz1, 16 - ly0])
                    c = ((x0, y1, z0), (x0, y1, z1), (x0, y0, z0))
                else:
                    uv = f.get('uv', [lx0, lz0, lx1, lz1])
                    c = ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1))
                crop = img.crop(tuple(round(v) for v in uv)).convert('RGBA')
                crop = crop.resize((crop.width * S, crop.height * S), Image.NEAREST)
                rot = f.get('rotation', 0)
                if rot:
                    crop = crop.rotate(-rot, expand=True)
                faces.append((key, 0 if name == 'up' else 1, crop, c, SHADE[name]))
    faces.sort(key=lambda q: (q[0], q[1]))
    allp = []
    for _, _, _, c, _ in faces:
        a, b, d = (_proj(p, k) for p in c)
        allp += [a, b, d, (b[0] + d[0] - a[0], b[1] + d[1] - a[1])]
    mnx, mxx = min(p[0] for p in allp), max(p[0] for p in allp)
    mny, mxy = min(p[1] for p in allp), max(p[1] for p in allp)
    ox, oy = -mnx + margin, -mny + margin
    canvas = Image.new('RGBA', (int(mxx - mnx + 2 * margin) + 2, int(mxy - mny + 2 * margin) + 2), bg)
    for _, _, crop, c, sh in faces:
        _face(canvas, crop, c, k, ox, oy, sh)
    return canvas
