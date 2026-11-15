"""Doors with see-through texels as 3D voxel slabs (IG31-9, variant B "inner wall", owner 31.10.).

A door leaf is one 3 px thick slab. Where its texture has fully transparent texels (windows, Vanilla's and ours) the player
sees through the hole *and* the leaf's thin edge is missing, so the door looks like it is not there. This turns the slab into
boxes: one box per run of opaque texels in a texture row, with the two big faces taken from the texture and a flat-coloured
wall (``Laibung``) on every box side that faces a hole or the slab's rim. Rows without holes give the same picture as the slab.

``slab_elements`` is imported by tools/textures/doors_2026_10_29.py and modules/simpleend/tools/gen_data.py.
"""
from __future__ import annotations

import math


def _texel(uv, left, right, top, bottom):
    """The texel under the middle of a face part; ``left``/``right``/``top``/``bottom`` are 0..1 fractions of the whole face."""
    u = uv[0] + (uv[2] - uv[0]) * (left + right) / 2
    v = uv[1] + (uv[3] - uv[1]) * (top + bottom) / 2
    return min(15, max(0, int(math.floor(u)))), min(15, max(0, int(math.floor(v))))


def _part_uv(uv, left, right, top, bottom):
    return [round(uv[0] + (uv[2] - uv[0]) * left, 4), round(uv[1] + (uv[3] - uv[1]) * top, 4),
            round(uv[0] + (uv[2] - uv[0]) * right, 4), round(uv[1] + (uv[3] - uv[1]) * bottom, 4)]


def slab_elements(img, run_axis, run, depth, front, back, texture="#top", cull=None):
    """Elements replacing one slab, or None when the texture has no transparent texel under the slab.

    img        PIL image, the leaf texture (RGBA)
    run_axis   'x' or 'z': the axis the texture's u runs along (the other horizontal axis is the slab's thickness)
    run        (r0, r1) extent of the slab along ``run_axis`` in px (1 px = 1 texel wide)
    depth      (d0, d1) extent along the thickness axis
    front/back (face, uv, left_is_low): a big face, its uv over the whole face, and whether its left edge (seen from outside)
               is the low coordinate of ``run_axis``
    cull       {face: cullface} for faces on the block rim; default none
    """
    cull = cull or {}
    r0, r1 = run
    n = r1 - r0
    px = img.convert("RGBA").load()

    def frac(face_info, i):
        _, _, left_is_low = face_info
        a, b = i / n, (i + 1) / n
        return (a, b) if left_is_low else (1 - b, 1 - a)

    def cell_texel(face_info, i, y):
        l, r = frac(face_info, i)
        return _texel(face_info[1], l, r, (15 - y) / 16, (16 - y) / 16)

    def opaque(i, y):
        for face_info in (front, back):
            tu, tv = cell_texel(face_info, i, y)
            if px[tu, tv][3] == 0:
                return False
        return True

    mask = [[opaque(i, y) for y in range(16)] for i in range(n)]
    if all(all(col) for col in mask):
        return None
    low_end, high_end = ("north", "south") if run_axis == "z" else ("west", "east")

    def need(i, y, di, dy):
        j, k = i + di, y + dy
        return not (0 <= j < n and 0 <= k < 16) or not mask[j][k]

    def flat(i, y):
        tu, tv = cell_texel(front, i, y)
        return {"texture": texture, "uv": [tu, tv, tu + 1, tv + 1]}

    def box(i0, i1, y):
        lo, hi = r0 + i0, r0 + i1
        if run_axis == "z":
            frm, to = [depth[0], y, lo], [depth[1], y + 1, hi]
        else:
            frm, to = [lo, y, depth[0]], [hi, y + 1, depth[1]]
        return frm, to

    out = []
    for y in range(16):
        i = 0
        while i < n:
            if not mask[i][y]:
                i += 1
                continue
            up, down = need(i, y, 0, 1), need(i, y, 0, -1)
            j = i + 1
            if not (up or down):  # runs only through cells whose top and bottom are covered
                while j < n and mask[j][y] and not (need(j, y, 0, 1) or need(j, y, 0, -1)):
                    j += 1
            faces = {}
            for face_info in (front, back):
                name, uv, _ = face_info
                l, r = (frac(face_info, i)[0], frac(face_info, j - 1)[1]) if face_info[2] else (frac(face_info, j - 1)[0], frac(face_info, i)[1])
                f = {"texture": texture, "uv": _part_uv(uv, l, r, (15 - y) / 16, (16 - y) / 16)}
                if name in cull:
                    f["cullface"] = cull[name]
                faces[name] = f
            if need(i, y, -1, 0):
                faces[low_end] = flat(i, y)
                if i == 0 and low_end in cull:
                    faces[low_end]["cullface"] = cull[low_end]
            if need(j - 1, y, 1, 0):
                faces[high_end] = flat(j - 1, y)
                if j == n and high_end in cull:
                    faces[high_end]["cullface"] = cull[high_end]
            if up:
                faces["up"] = flat(i, y)
                if y == 15 and "up" in cull:
                    faces["up"]["cullface"] = cull["up"]
            if down:
                faces["down"] = flat(i, y)
                if y == 0 and "down" in cull:
                    faces["down"]["cullface"] = cull["down"]
            frm, to = box(i, j, y)
            out.append({"from": frm, "to": to, "faces": faces})
            i = j
    return out



def rect_elements(img, run_axis, run, depth, front, back, texture="#top", cull=None):
    """N57-1 (b-ig57g): the same leaf as ``slab_elements`` from far fewer boxes - the opaque texels are covered greedily by
    rectangles (as wide, then as tall as they go), each one box with the two big faces mapped over its texels. Every box side
    that touches a hole or the rim anywhere gets a flat-coloured wall over the whole side (where an opaque neighbour covers part
    of it, that part lies inside the leaf and is never seen). Same arguments and result as ``slab_elements``; None when the
    texture has no transparent texel under the slab. Used by tools/textures/doors_2026_10_29.py for the big doors."""
    cull = cull or {}
    r0, r1 = run
    n = r1 - r0
    px = img.convert("RGBA").load()

    def frac(face_info, i):
        _, _, left_is_low = face_info
        a, b = i / n, (i + 1) / n
        return (a, b) if left_is_low else (1 - b, 1 - a)

    def cell_texel(face_info, i, y):
        l, r = frac(face_info, i)
        return _texel(face_info[1], l, r, (15 - y) / 16, (16 - y) / 16)

    def opaque(i, y):
        return all(px[cell_texel(fi, i, y)][3] != 0 for fi in (front, back))

    mask = [[opaque(i, y) for y in range(16)] for i in range(n)]
    if all(all(col) for col in mask):
        return None
    low_end, high_end = ("north", "south") if run_axis == "z" else ("west", "east")

    def solid(i, y):
        return 0 <= i < n and 0 <= y < 16 and mask[i][y]

    def flat(i, y):
        tu, tv = cell_texel(front, i, y)
        return {"texture": texture, "uv": [tu, tv, tu + 1, tv + 1]}

    used = [[False] * 16 for _ in range(n)]
    out = []
    for y0 in range(16):
        for i0 in range(n):
            if not mask[i0][y0] or used[i0][y0]:
                continue
            i1 = i0 + 1
            while i1 < n and mask[i1][y0] and not used[i1][y0]:
                i1 += 1
            y1 = y0 + 1
            while y1 < 16 and all(mask[i][y1] and not used[i][y1] for i in range(i0, i1)):
                y1 += 1
            for i in range(i0, i1):
                for y in range(y0, y1):
                    used[i][y] = True
            faces = {}
            for face_info in (front, back):
                name, uv, _ = face_info
                l, r = (frac(face_info, i0)[0], frac(face_info, i1 - 1)[1]) if face_info[2] else (frac(face_info, i1 - 1)[0], frac(face_info, i0)[1])
                f = {"texture": texture, "uv": _part_uv(uv, l, r, (16 - y1) / 16, (16 - y0) / 16)}
                if name in cull:
                    f["cullface"] = cull[name]
                faces[name] = f
            mid_y, mid_i = (y0 + y1 - 1) // 2, (i0 + i1 - 1) // 2
            if any(not solid(i0 - 1, y) for y in range(y0, y1)):
                faces[low_end] = flat(i0, mid_y)
                if i0 == 0 and low_end in cull:
                    faces[low_end]["cullface"] = cull[low_end]
            if any(not solid(i1, y) for y in range(y0, y1)):
                faces[high_end] = flat(i1 - 1, mid_y)
                if i1 == n and high_end in cull:
                    faces[high_end]["cullface"] = cull[high_end]
            if any(not solid(i, y1) for i in range(i0, i1)):
                faces["up"] = flat(mid_i, y1 - 1)
                if y1 == 16 and "up" in cull:
                    faces["up"]["cullface"] = cull["up"]
            if any(not solid(i, y0 - 1) for i in range(i0, i1)):
                faces["down"] = flat(mid_i, y0)
                if y0 == 0 and "down" in cull:
                    faces["down"]["cullface"] = cull["down"]
            lo, hi = r0 + i0, r0 + i1
            if run_axis == "z":
                frm, to = [depth[0], y0, lo], [depth[1], y1, hi]
            else:
                frm, to = [lo, y0, depth[0]], [hi, y1, depth[1]]
            out.append({"from": frm, "to": to, "faces": faces})
    return out
