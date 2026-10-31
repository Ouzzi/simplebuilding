"""b-rohr33 (31.10.): orthographic ray caster for Minecraft block-model elements (preview only, nothing here is part of the game).

Reads element lists as in model JSON (from/to in pixels, faces with uv, optional face rotation 0/90/180/270), places them with
a multipart rotation (x/y in 90 degree steps around the block centre) at a block position and renders with Minecraft's face
shading (up 1.0, down 0.5, north/south 0.8, east/west 0.6). Idea and camera from b-abn32's abn32_render.py.

Face texture mapping (default uv like Minecraft): s = left -> right as seen from outside, t = top -> bottom;
north: s from x=to down, south: s = x, east: s from z=to down, west: s = z, up: s = x, t = z, down: s = x, t from z=to down.
A face rotation R turns the texture clockwise by R on the face.
"""
from __future__ import annotations

import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6}
# (axis, sign) of the face normal
NORMAL = {'east': (0, 1), 'west': (0, -1), 'up': (1, 1), 'down': (1, -1), 'south': (2, 1), 'north': (2, -1)}


def rot_matrix(x_deg=0, y_deg=0):
    """Multipart rotation: first x, then y (Minecraft's order); both clockwise as seen from +axis like the block-state JSON."""
    ax, ay = math.radians(-x_deg), math.radians(-y_deg)
    rx = np.array([[1, 0, 0], [0, math.cos(ax), -math.sin(ax)], [0, math.sin(ax), math.cos(ax)]])
    ry = np.array([[math.cos(ay), 0, math.sin(ay)], [0, 1, 0], [-math.sin(ay), 0, math.cos(ay)]])
    return np.round(ry @ rx, 6)


def load_textures(tex_map: dict, resolve) -> dict:
    out = {}
    for key, img in tex_map.items():
        if isinstance(img, str):
            img = resolve(img)
        out['#' + key] = np.array(img.convert('RGBA'), dtype=float)
    return out


def part(elements, textures: dict, pos=(0, 0, 0), x=0, y=0):
    """A placed model: elements in model space, textures {'#name': RGBA array}, block position, multipart rotation."""
    return dict(elements=elements, textures=textures, pos=np.array(pos, float) * 16, rot=rot_matrix(x, y))


def _face_st(face, p, lo, hi):
    x, y, z = p[:, 0], p[:, 1], p[:, 2]
    sx = (x - lo[0]) / max(hi[0] - lo[0], 1e-9)
    sy = (y - lo[1]) / max(hi[1] - lo[1], 1e-9)
    sz = (z - lo[2]) / max(hi[2] - lo[2], 1e-9)
    if face == 'north':
        return 1 - sx, 1 - sy
    if face == 'south':
        return sx, 1 - sy
    if face == 'east':
        return 1 - sz, 1 - sy
    if face == 'west':
        return sz, 1 - sy
    if face == 'up':
        return sx, sz
    return sx, 1 - sz  # down


def _default_uv(face, lo, hi):
    x1, y1, z1 = lo
    x2, y2, z2 = hi
    return {'north': (16 - x2, 16 - y2, 16 - x1, 16 - y1), 'south': (x1, 16 - y2, x2, 16 - y1),
            'east': (16 - z2, 16 - y2, 16 - z1, 16 - y1), 'west': (z1, 16 - y2, z2, 16 - y1),
            'up': (x1, z1, x2, z2), 'down': (x1, 16 - z2, x2, 16 - z1)}[face]


def render(parts, w, h, scale, yaw, pitch, target, bg=(0, 0, 0, 0)):
    """Orthographic view turned by yaw (around y) and pitch (down) onto target (block units); scale = screen px per model px."""
    cy, sy, cp, sp = math.cos(yaw), math.sin(yaw), math.cos(pitch), math.sin(pitch)
    fwd = np.array([-sy * cp, -sp, -cy * cp])
    right = np.array([cy, 0.0, -sy])
    up = np.cross(right, fwd)
    jj, ii = np.mgrid[0:h, 0:w]
    sx = (ii.ravel() + 0.5 - w / 2) / scale
    syy = (h / 2 - jj.ravel() - 0.5) / scale
    org = np.array(target, float) * 16 + np.outer(sx, right) + np.outer(syy, up) - fwd * 3000
    n = len(sx)
    best = np.full(n, np.inf)
    col = np.tile(np.array(bg[:3], float), (n, 1))
    alpha = np.full(n, float(bg[3]) if len(bg) > 3 else 255.0)
    centre = np.array([8.0, 8.0, 8.0])
    for pt in parts:
        rt = pt['rot'].T                          # world -> model: inverse rotation around the block centre
        o_all = (org - pt['pos'] - centre) @ rt.T + centre
        d = rt @ fwd
        for el in pt['elements']:
            lo, hi = np.array(el['from'], float), np.array(el['to'], float)
            with np.errstate(divide='ignore', invalid='ignore'):
                inv = 1.0 / d
                t1 = (lo - o_all) * inv
                t2 = (hi - o_all) * inv
            tmin, tmax = np.minimum(t1, t2), np.maximum(t1, t2)
            tn = np.nanmax(tmin, axis=1)
            tf = np.nanmin(tmax, axis=1)
            hit = (tn <= tf) & (tf > 0) & (tn < best)
            if not hit.any():
                continue
            axis = np.nanargmax(tmin, axis=1)
            for name, (ax, sign) in NORMAL.items():
                if name not in el['faces']:
                    continue
                if d[ax] * sign >= 0:            # face looks away from the camera
                    continue
                m = hit & (axis == ax)
                if not m.any():
                    continue
                fc = el['faces'][name]
                arr = pt['textures'][fc['texture']]
                p = o_all[m] + np.outer(tn[m], d)
                s, t = _face_st(name, p, lo, hi)
                r = fc.get('rotation', 0) % 360
                if r == 90:
                    s, t = t, 1 - s
                elif r == 180:
                    s, t = 1 - s, 1 - t
                elif r == 270:
                    s, t = 1 - t, s
                u1, v1, u2, v2 = fc.get('uv') or _default_uv(name, lo, hi)
                k = arr.shape[1] / 16.0
                u = (u1 + np.clip(s, 0, 0.9999) * (u2 - u1)) * k
                v = (v1 + np.clip(t, 0, 0.9999) * (v2 - v1)) * k
                # flipped uv ranges sample from the far edge inwards
                ui = np.floor(u - (1e-6 if u2 < u1 else 0)).astype(int) % arr.shape[1]
                vi = np.floor(v - (1e-6 if v2 < v1 else 0)).astype(int) % arr.shape[0]
                texel = arr[vi, ui]
                ok = texel[:, 3] > 8
                idx = np.where(m)[0][ok]
                col[idx] = texel[ok, :3] * SHADE[name]
                alpha[idx] = 255
                best[idx] = tn[m][ok]
    out = np.dstack([col.reshape(h, w, 3), alpha.reshape(h, w, 1)]).clip(0, 255).astype(np.uint8)
    return Image.fromarray(out, 'RGBA')


# ------------------------------------------------------------------------------------------------- Vanilla neighbours (runtime only)

VANILLA = Path('/root/mc263src/assets/minecraft')


def vanilla_model(name: str):
    """Elements + textures of a Vanilla block model, read at runtime from the local game sources (never copied into the repo)."""
    data = json.loads((VANILLA / f'models/block/{name}.json').read_text())
    tex = {k: Image.open(VANILLA / f"textures/{v.split(':')[-1]}.png") for k, v in data['textures'].items() if not v.startswith('#')}
    return data['elements'], load_textures(tex, None)


def chest_parts(pos, facing_y=0):
    """A Vanilla chest drawn from its entity texture (64x64): base 14x10x14, lid 14x5x14, lock 2x4x1 (runtime only)."""
    img = Image.open(VANILLA / 'textures/entity/chest/normal.png').convert('RGBA')
    sheet = np.array(img, dtype=float)

    def faces(ox, oy, wx, hy, dz):
        # Minecraft entity cube layout (texture units of 4 per 16): top (ox+dz, oy), sides below
        q = 16 / 64
        U = lambda a, b, c, e: [a * q, b * q, c * q, e * q]  # noqa: E731
        # the entity model is drawn upside down: the texture's 'bottom' cell is the visible top
        return {'up': {'uv': U(ox + dz + wx, oy, ox + dz + 2 * wx, oy + dz), 'texture': '#c'},
                'down': {'uv': U(ox + dz + wx, oy + dz, ox + dz, oy), 'texture': '#c'},
                'north': {'uv': U(ox + dz, oy + dz, ox + dz + wx, oy + dz + hy), 'texture': '#c'},
                'south': {'uv': U(ox + 2 * dz + wx, oy + dz, ox + 2 * dz + 2 * wx, oy + dz + hy), 'texture': '#c'},
                'west': {'uv': U(ox, oy + dz, ox + dz, oy + dz + hy), 'texture': '#c'},
                'east': {'uv': U(ox + dz + wx, oy + dz, ox + 2 * dz + wx, oy + dz + hy), 'texture': '#c'}}
    els = [{'from': [1, 0, 1], 'to': [15, 10, 15], 'faces': faces(0, 19, 14, 10, 14)},
           {'from': [1, 9, 1], 'to': [15, 14, 15], 'faces': faces(0, 0, 14, 5, 14)},
           {'from': [7, 7, 0], 'to': [9, 11, 1], 'faces': faces(0, 0, 2, 4, 1)}]
    return part(els, {'#c': sheet}, pos, 0, facing_y)
