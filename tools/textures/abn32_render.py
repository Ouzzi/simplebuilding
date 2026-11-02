"""b-abn32 (31.10.): tiny orthographic ray caster for block-model previews (boxes in pixel units, 16 = one block).

Every box has six faces ('px','nx','py','ny','pz','nz'); a face is a sampler f(p) -> RGB array for the local hit points p (N x 3).
Optional rotation (3 x 3 matrix, pivot) per box. Shading like Minecraft's block faces: top 1.0, bottom 0.5, z 0.8, x 0.6.
Preview only; nothing here is part of the game.
"""
from __future__ import annotations

import math

import numpy as np
from PIL import Image

SHADE = {'py': 1.0, 'ny': 0.5, 'pz': 0.8, 'nz': 0.8, 'px': 0.6, 'nx': 0.6}
AXIS = {'px': (0, 1), 'nx': (0, -1), 'py': (1, 1), 'ny': (1, -1), 'pz': (2, 1), 'nz': (2, -1)}


def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def box(lo, hi, faces, rot=None, pivot=(0, 0, 0)):
    return dict(lo=np.array(lo, float), hi=np.array(hi, float), faces=faces, rot=None if rot is None else np.array(rot, float), pivot=np.array(pivot, float))


def tex(img: Image.Image, u_fn, v_fn):
    """Sampler: texel (u_fn(p), v_fn(p)) of a 16 x 16 image, wrapped."""
    arr = np.array(img.convert('RGB'), dtype=float)

    def f(p):
        u = np.floor(u_fn(p)).astype(int) % arr.shape[1]
        v = np.floor(v_fn(p)).astype(int) % arr.shape[0]
        return arr[v, u]
    return f


def flat(rgb):
    c = np.array(rgb[:3], float)
    return lambda p: np.tile(c, (len(p), 1))


def render(boxes, w, h, scale, yaw, pitch, target, bg=(0, 0, 0, 0)):
    """Orthographic view: camera turned by yaw (around y) and pitch (down), looking at target; scale = screen px per model px."""
    cy, sy, cp, sp = math.cos(yaw), math.sin(yaw), math.cos(pitch), math.sin(pitch)
    fwd = np.array([-sy * cp, -sp, -cy * cp])          # into the scene
    right = np.array([cy, 0.0, -sy])
    up = np.cross(right, fwd)
    jj, ii = np.mgrid[0:h, 0:w]
    sx = (ii.ravel() + 0.5 - w / 2) / scale
    syy = (h / 2 - jj.ravel() - 0.5) / scale
    org = np.array(target, float) + np.outer(sx, right) + np.outer(syy, up) - fwd * 2000
    n = len(sx)
    best = np.full(n, np.inf)
    col = np.tile(np.array(bg[:3], float), (n, 1))
    alpha = np.full(n, bg[3] if len(bg) > 3 else 255, float)
    for b in boxes:
        if b['rot'] is not None:
            rt = b['rot'].T
            o = (org - b['pivot']) @ rt.T + b['pivot']
            d = np.tile(rt @ fwd, (n, 1))
        else:
            o, d = org, np.tile(fwd, (n, 1))
        with np.errstate(divide='ignore', invalid='ignore'):
            inv = 1.0 / d
            t1 = (b['lo'] - o) * inv
            t2 = (b['hi'] - o) * inv
        tmin = np.minimum(t1, t2)
        tmax = np.maximum(t1, t2)
        tn = np.nanmax(tmin, axis=1)
        tf = np.nanmin(tmax, axis=1)
        hit = (tn <= tf) & (tf > 0) & (tn < best)
        if not hit.any():
            continue
        axis = np.nanargmax(tmin, axis=1)
        idx = np.nonzero(hit)[0]
        p = o[idx] + d[idx] * tn[idx, None]
        for name, (ax, sgn) in AXIS.items():
            sel = (axis[idx] == ax) & ((d[idx, ax] < 0) if sgn > 0 else (d[idx, ax] > 0))
            if not sel.any():
                continue
            k = idx[sel]
            face = b['faces'].get(name)
            if face is None:
                continue
            col[k] = face(p[sel]) * SHADE[name]
            best[k] = tn[k]
            alpha[k] = 255
    out = np.dstack([np.clip(col, 0, 255).reshape(h, w, 3), alpha.reshape(h, w, 1)]).astype(np.uint8)
    return Image.fromarray(out, 'RGBA')
