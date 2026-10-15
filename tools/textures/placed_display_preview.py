"""Usage: python3 tools/textures/placed_display_preview.py [--out DIR] [--jar client.jar] [--models-only]

Step 2 of docs/ai/VORSCHLAG-ITEM-DISPLAY-3D-2026-10-15.md (owner answers D1-D10): before/after pictures for
placed smithing templates, pottery sherds and music discs. PREVIEW ONLY - nothing here is registered in the game,
no game model, block or Java file is touched, no texture is written into the mod resources.

How it works (offline, no game client):
  - reads the vanilla sprites from the 26.3 client jar (SB_CLIENT_JAR or --jar) and the four mod discs from
    tools/textures/music_disc_textures.py (they are vanilla-derived too);
  - "before" = what PlacedPlate / PlacedSmallParts draw today: item/generated plate (14/16 edge, thickness 1.6 px
    * 14/16 = 1.4 px, piles 8/16 and 0.8 px, SLOTS, LIFT 0.2 px), edge quads sampled from the border pixel;
  - "after" = the proposed block models as Minecraft model JSON elements (written next to the pictures):
      silhouette extrusion (templates, sherds): one cuboid per run of equal sprite rows, front face = the item sprite
        itself, sides = the border pixel, thickness 2 px single (16/7 model px at scale 14/16), ~1.1 px in a pile;
      round disc: 12 px diameter, 12 one-pixel strips, 1 px thick, new top-view texture (built here from the
        item sprite: rim, body, groove ring, label, 2x2 hole); stack of up to 4 (+1 px each, turned);
        a disc inside a mixed pile sits alone at its slot, 0.67 x footprint;
  - the same place() chain as Java (translate/rotateY/rotateX/scale), pile lift computed from the real overlap of
    the footprints instead of the constant LIFT;
  - a small z-buffer rasterizer (orthographic, nearest-neighbor texels, supersampled) with Minecraft's face shading
    (up 1.0, down 0.5, north/south 0.8, east/west 0.6). It approximates the in-game light; before and after use the
    same shading, so the comparison is fair.

Output PNGs contain vanilla pixels and are NOT committed (.gitignore, like docs/ai/previews/n41); the model JSON
files (no pixels) are. Needs Pillow + numpy.
"""
import io
import json
import math
import os
import sys
import zipfile

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
DEFAULT_JAR = '/root/.gradle/caches/minecraftforge/forgegradle/mavenizer/caches/minecraft_tasks/26.3/client.jar'
DEFAULT_OUT = os.path.join(ROOT, 'docs', 'ai', 'previews', 'oc-display-previews')
SOLID_ALPHA = 26

# ---------------------------------------------------------------------------------------------------------------
# constants of the current game code (PlacedPlate / PlacedSmallParts) and of the proposal
# ---------------------------------------------------------------------------------------------------------------
SCALE = 14.0 / 16.0           # PlacedPlate.SCALE
THICKNESS_SCALE = 1.6         # PlacedPlate.THICKNESS_SCALE (before)
GAP = 0.002
PILE_SCALE = 8.0 / 16.0
LIFT = 0.2 / 16.0
SLOTS = [  # px x, px z, rotation deg; facing north
    [(8.0, 8.0, 0.0)],
    [(5.5, 6.0, -12.0), (10.5, 10.5, 16.0)],
    [(5.0, 5.5, -10.0), (11.0, 6.0, 14.0), (7.5, 11.0, -4.0)],
    [(4.5, 4.5, -10.0), (11.5, 4.75, 12.0), (4.75, 11.5, 6.0), (11.25, 11.25, -14.0)],
]
# proposal: single = 2.0 px thick (model thickness at scale 14/16), pile ~1.1 px (same model at 8/16)
MODEL_THICKNESS = 2.0 / SCALE   # px in model space (16/7)
DISC_D = 12                     # round disc diameter px
DISC_H = 1.0                    # px
DISC_MIX_SCALE = 0.67           # disc footprint when it shares the spot with other parts
DISC_STACK_TURN = [0.0, 11.0, -8.0, 14.0]   # degrees per stack level
DISC_STACK_SHIFT = [(0.0, 0.0), (0.6, -0.4), (-0.5, 0.5), (0.4, 0.6)]   # px, so the pile reads as a pile


# ---------------------------------------------------------------------------------------------------------------
# small matrix helpers (joml order: m.translate(..).rotateY(..) = M @ T @ R)
# ---------------------------------------------------------------------------------------------------------------
def T(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def Sc(x, y, z):
    return np.diag([x, y, z, 1.0])


def RY(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    m = np.eye(4)
    m[0, 0], m[0, 2], m[2, 0], m[2, 2] = c, s, -s, c
    return m


def RX(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    m = np.eye(4)
    m[1, 1], m[1, 2], m[2, 1], m[2, 2] = c, -s, s, c
    return m


YROT = {'north': 180.0, 'south': 0.0, 'west': 90.0, 'east': 270.0}   # Direction.toYRot


def chain(*ms):
    out = np.eye(4)
    for m in ms:
        out = out @ m
    return out


# ---------------------------------------------------------------------------------------------------------------
# textures
# ---------------------------------------------------------------------------------------------------------------
class Sprites:
    def __init__(self, jar):
        self.jar = jar
        self.zip = zipfile.ZipFile(jar)
        self.cache = {}

    def item(self, name):
        if name not in self.cache:
            data = self.zip.read('assets/minecraft/textures/item/%s.png' % name)
            self.cache[name] = Image.open(io.BytesIO(data)).convert('RGBA')
        return self.cache[name]

    def mod_disc(self, song):
        key = 'mod_' + song
        if key not in self.cache:
            sys.path.insert(0, HERE)
            os.environ['SB_CLIENT_JAR'] = self.jar
            import music_disc_textures as mdt
            mdt.JAR = self.jar
            base = self.item('music_disc_13')
            self.cache[key] = mdt.disc(base, song, 1)
        return self.cache[key]


def arr(img):
    return np.asarray(img.convert('RGBA'), dtype=np.uint8)


def mask_of(img):
    return arr(img)[:, :, 3] >= SOLID_ALPHA


# ---------------------------------------------------------------------------------------------------------------
# top-view texture of a disc (12 x 12), built from the 16 x 16 item sprite (vanilla disc layout, hole at (7,7))
# ---------------------------------------------------------------------------------------------------------------
def disc_top(sprite):
    a = arr(sprite)
    px = lambda x, y: tuple(int(v) for v in a[y, x, :3])
    outline, body, groove, rim, bottom = px(5, 3), px(5, 4), px(6, 5), px(2, 10), px(5, 12)
    hole = px(7, 7)
    n = DISC_D
    out = np.zeros((n, n, 4), dtype=np.uint8)
    c = (n - 1) / 2.0
    # label: the 3 x 3 sprite pixels around the hole (x 6..8, y 6..8) -> 4 x 4 (hole column and row doubled -> 2 x 2 hole);
    # a third of the diameter, like a real record label
    cols = [6, 7, 7, 8]
    rows = [6, 7, 7, 8]
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - c, y - c)
            if d > 6.0:
                continue
            if d > 5.0:
                col = outline
            elif d > 4.0:
                col = groove if (x + y) % 4 == 0 and d > 4.5 else body
            elif d > 3.2:
                col = groove
            elif 4 <= x <= 7 and 4 <= y <= 7:
                lx, ly = x - 4, y - 4
                col = hole if (lx in (1, 2) and ly in (1, 2)) else px(cols[lx], rows[ly])
            else:
                col = body
            out[y, x] = col + (255,)
    # lower rim shade on the south edge of the sprite = the dark side of the strips
    for x in range(n):
        for y in range(n - 1, -1, -1):
            if out[y, x, 3]:
                if y >= n - 2 and math.hypot(x - c, y - c) > 5.0:
                    out[y, x, :3] = bottom
                break
    return Image.fromarray(out, 'RGBA')


# ---------------------------------------------------------------------------------------------------------------
# model JSON of the proposal ("after")
# ---------------------------------------------------------------------------------------------------------------
def fmt(v):
    v = round(float(v), 4)
    return int(v) if v == int(v) else v


def stepped_model(mask, tex_var='item'):
    """Silhouette extrusion: cuboids from runs of equal sprite rows; front = the sprite, sides = border pixel."""
    rows = [sum((1 << (15 - x)) for x in range(16) if mask[y][x]) for y in range(16)]
    t0, t1 = 8 - MODEL_THICKNESS / 2, 8 + MODEL_THICKNESS / 2
    elements = []
    v = 0
    eps = 0.1
    while v < 16:
        end = v + 1
        while end < 16 and rows[end] == rows[v]:
            end += 1
        u = 0
        while u < 16:
            if not (rows[v] & (1 << (15 - u))):
                u += 1
                continue
            u1 = u
            while u1 < 16 and (rows[v] & (1 << (15 - u1))):
                u1 += 1
            tex = '#' + tex_var
            faces = {
                'south': {'uv': [u, v, u1, end], 'texture': tex},
                'west': {'uv': [u + 0.45, v, u + 0.45 + eps, end], 'texture': tex},
                'east': {'uv': [u1 - 0.45 - eps, v, u1 - 0.45, end], 'texture': tex},
                'up': {'uv': [u, v + 0.45, u1, v + 0.45 + eps], 'texture': tex},
                'down': {'uv': [u, end - 0.45 - eps, u1, end - 0.45], 'texture': tex},
            }
            elements.append({'from': [u, 16 - end, fmt(t0)], 'to': [u1, 16 - v, fmt(t1)], 'faces': faces})
            u = u1
        v = end
    return {'textures': {tex_var: '<item sprite>', 'particle': '<item sprite>'}, 'elements': elements,
            '_comment': 'preview only; model space like item/generated (0..16, z centered 8), no back face (it sits on the surface); thickness %.3f px' % MODEL_THICKNESS}


def disc_model(top_img):
    """12 strips of one pixel, 1 px thick; block-model space (y 0 = floor), centered on x = z = 8."""
    m = mask_of(top_img)
    n = DISC_D
    elements = []
    tex = '#disc'
    eps = 0.1
    for r in range(n):
        xs = [x for x in range(n) if m[r][x]]
        if not xs:
            continue
        a, b = min(xs), max(xs) + 1
        x0, x1 = 8 - n / 2 + a, 8 - n / 2 + b
        z0 = 8 - n / 2 + r
        faces = {
            'up': {'uv': [a, r, b, r + 1], 'texture': tex},
            'down': {'uv': [a, r, b, r + 1], 'texture': tex},
            'north': {'uv': [a, r + 0.45, b, r + 0.45 + eps], 'texture': tex},
            'south': {'uv': [a, r + 0.45, b, r + 0.45 + eps], 'texture': tex},
            'west': {'uv': [a + 0.45, r, a + 0.45 + eps, r + 1], 'texture': tex},
            'east': {'uv': [b - 0.45 - eps, r, b - 0.45, r + 1], 'texture': tex},
        }
        elements.append({'from': [x0, 0, z0], 'to': [x1, DISC_H, z0 + 1], 'faces': faces})
    return {'textures': {'disc': '<top view>', 'particle': '<top view>'}, 'elements': elements,
            '_comment': 'preview only; round disc %d px, %d strips, %.1f px thick' % (n, len(elements), DISC_H)}


# ---------------------------------------------------------------------------------------------------------------
# quads
# ---------------------------------------------------------------------------------------------------------------
class Quad:
    def __init__(self, c00, c10, c01, tex, uv, shade, solid=None):
        self.c00, self.c10, self.c01 = np.array(c00, float), np.array(c10, float), np.array(c01, float)
        self.tex, self.uv, self.shade, self.solid = tex, uv, shade, solid


def face_corners(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {
        'up': ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1)),
        'down': ((x0, y0, z1), (x1, y0, z1), (x0, y0, z0)),
        'north': ((x1, y1, z0), (x0, y1, z0), (x1, y0, z0)),
        'south': ((x0, y1, z1), (x1, y1, z1), (x0, y0, z1)),
        'east': ((x1, y1, z1), (x1, y1, z0), (x1, y0, z1)),
        'west': ((x0, y1, z0), (x0, y1, z1), (x0, y0, z0)),
    }[face]


SHADE_W = {'x': 0.6, 'y': 1.0, 'z': 0.8}


def shade_of(normal):
    nx, ny, nz = normal
    s = nx * nx * 0.6 + nz * nz * 0.8
    s += ny * ny * (1.0 if ny > 0 else 0.5)
    return s


def model_quads(model, textures, world):
    """Quads of a model JSON (px model space) under a 4x4 world matrix (model px -> render px)."""
    out = []
    for el in model['elements']:
        f, t = el['from'], el['to']
        for face, spec in el['faces'].items():
            tex = textures[spec['texture'][1:]]
            c00, c10, c01 = [np.array(p) for p in face_corners(face, f, t)]
            nrm = {'up': (0, 1, 0), 'down': (0, -1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0),
                   'west': (-1, 0, 0)}[face]
            w = lambda p: (world @ np.append(p, 1.0))[:3]
            n = world[:3, :3] @ np.array(nrm, float)
            n = n / (np.linalg.norm(n) or 1.0)
            out.append(Quad(w(c00), w(c10), w(c01), tex, spec['uv'], shade_of(n)))
    return out


def plate_quads(sprite_img, world):
    """item/generated: front and back face (whole sprite, cutout) plus one edge quad per exposed pixel side.
    Model space 0..16, thickness 1 px in z (7.5..8.5), pixel (u, v) at x = u, y = 15 - v."""
    a = arr(sprite_img)
    m = a[:, :, 3] >= SOLID_ALPHA
    out = []
    w = lambda p: (world @ np.append(p, 1.0))[:3]
    nrm = lambda v: (lambda n: n / (np.linalg.norm(n) or 1.0))(world[:3, :3] @ np.array(v, float))
    tex = a.copy()
    tex[:, :, 3] = np.where(m, 255, 0)
    z0, z1 = 7.5, 8.5
    out.append(Quad(w((0, 16, z1)), w((16, 16, z1)), w((0, 0, z1)), tex, [0, 0, 16, 16], shade_of(nrm((0, 0, 1)))))
    out.append(Quad(w((0, 16, z0)), w((16, 16, z0)), w((0, 0, z0)), tex, [0, 0, 16, 16], shade_of(nrm((0, 0, -1)))))
    for v in range(16):
        for u in range(16):
            if not m[v][u]:
                continue
            y0, y1 = 15 - v, 16 - v
            px = lambda yy, uu: [uu, yy, uu + 1, yy + 1]
            if u == 0 or not m[v][u - 1]:       # west edge
                out.append(Quad(w((u, y1, z0)), w((u, y1, z1)), w((u, y0, z0)), tex, [u, v, u + 0.01, v + 1], shade_of(nrm((-1, 0, 0)))))
            if u == 15 or not m[v][u + 1]:      # east edge
                out.append(Quad(w((u + 1, y1, z1)), w((u + 1, y1, z0)), w((u + 1, y0, z1)), tex, [u, v, u + 0.01, v + 1], shade_of(nrm((1, 0, 0)))))
            if v == 0 or not m[v - 1][u]:       # top edge
                out.append(Quad(w((u, y1, z0)), w((u + 1, y1, z0)), w((u, y1, z1)), tex, [u, v, u + 1, v + 0.01], shade_of(nrm((0, 1, 0)))))
            if v == 15 or not m[v + 1][u]:      # bottom edge
                out.append(Quad(w((u, y0, z1)), w((u + 1, y0, z1)), w((u, y0, z0)), tex, [u, v, u + 1, v + 0.01], shade_of(nrm((0, -1, 0)))))
    return out


# ---------------------------------------------------------------------------------------------------------------
# placement (mirror of PlacedPlate.transform and PlacedSmallParts.place, block units; model centered at origin)
# ---------------------------------------------------------------------------------------------------------------
PX = 1.0 / 16.0
TO_RENDER = Sc(16, 16, 16)            # block units -> render px


def base():
    """block model (y 0 = floor, x/z centered on 8) px -> block units, x/z centered at the origin."""
    return chain(T(-0.5, 0, -0.5), Sc(PX, PX, PX))


def plate_world_single(face, facing, tm, zs):
    """PlacedPlate.transform(face, facing) for a model in 0..1 space (plate). tm = plate thickness in model px (shift
    to the surface), zs = z stretch (before: 1.6, after: 1.0 - the model carries its own thickness)."""
    yrot = YROT[facing]
    m = T(0.5, 0.5, 0.5)
    if face == 'wall':
        m = m @ RY(-yrot)
    elif face == 'floor':
        m = m @ RY(180.0 - yrot) @ RX(-90.0)
    thickness = SCALE * tm / 16.0
    m = m @ T(0, 0, -(0.5 - thickness / 2.0 - GAP)) @ Sc(SCALE, SCALE, SCALE * zs) @ T(-0.5, -0.5, -0.5)
    return m


def plate_world_pile(facing, count, index, lift, tm, zs):
    """PlacedSmallParts.place for a flat part (SLOTS, lift in px) -> model(0..1, not yet centered) matrix."""
    slot = SLOTS[count - 1][index]
    scale = SCALE if count <= 1 else PILE_SCALE
    thickness = scale * tm / 16.0
    m = chain(T(0.5, 0, 0.5), RY(180.0 - YROT[facing]), T((slot[0] - 8) / 16, 0, (slot[1] - 8) / 16), RY(slot[2]),
              T(0, lift / 16.0 + thickness / 2.0 + GAP, 0), RX(-90.0), Sc(scale, scale, scale * zs),
              T(-0.5, -0.5, -0.5))
    return m


# ---------------------------------------------------------------------------------------------------------------
# footprints for the lift computation (convex polygons in px, SAT)
# ---------------------------------------------------------------------------------------------------------------
def poly_square(cx, cz, half, rot):
    a = math.radians(rot)
    c, s = math.cos(a), math.sin(a)
    pts = [(-half, -half), (half, -half), (half, half), (-half, half)]
    return [(cx + x * c + z * s, cz - x * s + z * c) for x, z in pts]


def poly_circle(cx, cz, r, n=12):
    return [(cx + r * math.cos(2 * math.pi * i / n), cz + r * math.sin(2 * math.pi * i / n)) for i in range(n)]


def overlap(p, q, margin=-0.3):
    for poly in (p, q):
        for i in range(len(poly)):
            x1, y1 = poly[i]
            x2, y2 = poly[(i + 1) % len(poly)]
            ax, ay = y1 - y2, x2 - x1
            pa = [ax * x + ay * y for x, y in p]
            pb = [ax * x + ay * y for x, y in q]
            ln = math.hypot(ax, ay)
            if max(pa) - min(pb) < margin * ln or max(pb) - min(pa) < margin * ln:
                return False
    return True


def pile_lifts(parts):
    """parts: list of (polygon, thickness px). Lift = top of the highest earlier overlapping part."""
    lifts = []
    for i, (poly, th) in enumerate(parts):
        lift = 0.0
        for j in range(i):
            if overlap(poly, parts[j][0]):
                lift = max(lift, lifts[j] + parts[j][1])
        lifts.append(lift)
    return lifts


# ---------------------------------------------------------------------------------------------------------------
# renderer
# ---------------------------------------------------------------------------------------------------------------
class Camera:
    def __init__(self, yaw=32.0, pitch=34.0, scale=13.0, target=(8.0, 2.0, 8.0), size=(340, 270), ss=2):
        y, p = math.radians(yaw), math.radians(pitch)
        c = np.array([math.sin(y) * math.cos(p), math.sin(p), math.cos(y) * math.cos(p)])
        right = np.cross([0, 1, 0], c)
        right /= np.linalg.norm(right)
        up = np.cross(c, right)
        self.R = np.stack([right, up, c])
        self.scale, self.target, self.size, self.ss = scale, np.array(target, float), size, ss


def render(quads, cam, bg=(176, 196, 214)):
    ss = cam.ss
    W, H = cam.size[0] * ss, cam.size[1] * ss
    S = cam.scale * ss
    col = np.zeros((H, W, 3), np.float32)
    col[:] = bg
    zbuf = np.full((H, W), -1e9, np.float32)
    tgt = cam.R @ cam.target

    def proj(p):
        q = cam.R @ p - tgt
        return np.array([W / 2 + S * q[0], H / 2 - S * q[1], q[2]])

    for q in quads:
        P00, P10, P01 = proj(q.c00), proj(q.c10), proj(q.c01)
        P11 = P10 + P01 - P00
        xs = [P00[0], P10[0], P01[0], P11[0]]
        ys = [P00[1], P10[1], P01[1], P11[1]]
        x0, x1 = max(0, int(math.floor(min(xs)))), min(W - 1, int(math.ceil(max(xs))))
        y0, y1 = max(0, int(math.floor(min(ys)))), min(H - 1, int(math.ceil(max(ys))))
        if x1 < x0 or y1 < y0:
            continue
        A = np.array([[P10[0] - P00[0], P01[0] - P00[0]], [P10[1] - P00[1], P01[1] - P00[1]]])
        det = np.linalg.det(A)
        if abs(det) < 1e-6:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        dx, dy = gx - P00[0], gy - P00[1]
        s = (A[1, 1] * dx - A[0, 1] * dy) / det
        t = (-A[1, 0] * dx + A[0, 0] * dy) / det
        eps = 1e-4
        ok = (s >= -eps) & (s <= 1 + eps) & (t >= -eps) & (t <= 1 + eps)
        if not ok.any():
            continue
        depth = P00[2] + s * (P10[2] - P00[2]) + t * (P01[2] - P00[2])
        u0, v0, u1, v1 = q.uv
        th, tw = q.tex.shape[:2]
        if q.solid is not None:
            rgba = np.broadcast_to(np.array(q.solid + (255,), np.uint8), s.shape + (4,))
        else:
            uu = np.clip(np.floor(u0 + s * (u1 - u0)).astype(int), 0, tw - 1)
            vv = np.clip(np.floor(v0 + t * (v1 - v0)).astype(int), 0, th - 1)
            rgba = q.tex[vv, uu]
        sub = zbuf[y0:y1 + 1, x0:x1 + 1]
        take = ok & (rgba[:, :, 3] >= SOLID_ALPHA) & (depth > sub + 1e-4)
        if not take.any():
            continue
        sub[take] = depth[take]
        c = col[y0:y1 + 1, x0:x1 + 1]
        c[take] = rgba[:, :, :3][take].astype(np.float32) * q.shade
    img = Image.fromarray(np.clip(col, 0, 255).astype(np.uint8), 'RGB')
    return img.resize(cam.size, Image.BOX) if ss > 1 else img


# ---------------------------------------------------------------------------------------------------------------
# scenes
# ---------------------------------------------------------------------------------------------------------------
def checker(c1, c2, n=6):
    a = np.zeros((n, n, 4), np.uint8)
    for y in range(n):
        for x in range(n):
            a[y, x] = (c1 if (x + y) % 2 == 0 else c2) + (255,)
    return a


def ground_quads():
    tex = checker((112, 112, 112), (126, 126, 126))
    # 24 x 24 px ground, 4 px squares, the block cell is x/z 0..16 (rendered at 4 px offset)
    f, t = (-4.0, -2.0, -4.0), (20.0, 0.0, 20.0)
    qs = []
    for face in ('up', 'south', 'east'):
        c00, c10, c01 = face_corners(face, f, t)
        qs.append(Quad(c00, c10, c01, tex, [0, 0, 6, 6] if face == 'up' else [0, 0, 1, 1],
                       {'up': 1.0, 'south': 0.8, 'east': 0.6}[face], solid=None if face == 'up' else (96, 96, 96)))
    return qs


def wall_quads():
    """a wall behind the block cell: its face is the plane z = 0 (north side), 24 px high"""
    tex = checker((120, 120, 124), (132, 132, 136))
    f, t = (-12.0, 0.0, -4.0), (28.0, 24.0, 0.0)
    qs = []
    for face, shade in (('south', 0.8), ('up', 1.0), ('east', 0.6)):
        c00, c10, c01 = face_corners(face, f, t)
        qs.append(Quad(c00, c10, c01, tex, [0, 0, 6, 5] if face == 'south' else [0, 0, 1, 1], shade,
                       solid=None if face == 'south' else (100, 100, 104)))
    return qs


def to_world(m_block):
    return TO_RENDER @ m_block


class Scene:
    def __init__(self, sprites):
        self.sp = sprites
        self.model_cache = {}
        self.models = {}          # name -> json dict, written next to the pictures

    # ---- parts --------------------------------------------------------------------------------------------
    def sil_model(self, kind, name):
        key = (kind, name)
        if key not in self.model_cache:
            mask = mask_of(self.sp.item(name))
            rows = tuple(tuple(r) for r in mask)
            mkey = (kind, rows)
            if mkey not in self.model_cache:
                self.model_cache[mkey] = stepped_model(mask)
                stem = name.replace('_armor_trim_smithing_template', '').replace('_smithing_template', '').replace('_pottery_sherd', '')
                self.models['placed_%s_%s' % (kind, stem)] = self.model_cache[mkey]
            self.model_cache[key] = self.model_cache[mkey]
        return self.model_cache[key]

    def disc_sprite(self, name):
        if name.startswith('mod:'):
            return self.sp.mod_disc(name[4:])
        return self.sp.item('music_disc_' + name)

    def disc_assets(self, name):
        key = ('disc', name)
        if key not in self.model_cache:
            top = disc_top(self.disc_sprite(name))
            self.model_cache[key] = (top, disc_model(top))
        return self.model_cache[key]

    def sprite(self, kind, name):
        if kind == 'disc':
            return self.disc_sprite(name)
        return self.sp.item(name)

    # ---- placing -------------------------------------------------------------------------------------------
    def before_single(self, kind, name, face, facing):
        w = plate_world_single(face, facing, THICKNESS_SCALE, THICKNESS_SCALE)
        return plate_quads(self.sprite(kind, name), to_world(w) @ Sc(PX, PX, PX))

    def before_pile(self, parts, facing):
        qs = []
        n = len(parts)
        for i, (kind, name) in enumerate(parts):
            w = plate_world_pile(facing, n, i, i * LIFT * 16.0, THICKNESS_SCALE, THICKNESS_SCALE)
            qs += plate_quads(self.sprite(kind, name), to_world(w) @ Sc(PX, PX, PX))
        return qs

    def after_single(self, kind, name, face, facing):
        if kind == 'disc':
            top, model = self.disc_assets(name)
            textures = {'disc': arr(top), 'particle': arr(top)}
            if face == 'floor':
                w = chain(T(0.5, 0, 0.5), RY(180.0 - YROT[facing]), base())
            else:   # wall: model +y (disc top) turns out of the wall, the back sits GAP in front of the wall plane
                w = chain(T(0.5, 0.5, 0.5), RY(-YROT[facing]), T(0, 0, -0.5 + GAP), RX(90.0), base())
            return model_quads(model, textures, to_world(w))
        model = self.sil_model(kind, name)
        tex = arr(self.sprite(kind, name))
        textures = {'item': tex, 'particle': tex}
        w = plate_world_single(face, facing, MODEL_THICKNESS, 1.0)
        return model_quads(model, textures, to_world(w @ Sc(PX, PX, PX)))

    def after_pile(self, parts, facing):
        """parts: list of (kind, name); disc-only pile = stack; otherwise slots with computed lift."""
        n = len(parts)
        if all(k == 'disc' for k, _ in parts):
            qs = []
            for i, (kind, name) in enumerate(parts):
                top, model = self.disc_assets(name)
                textures = {'disc': arr(top), 'particle': arr(top)}
                w = chain(T(0.5, 0, 0.5), RY(180.0 - YROT[facing]), T(DISC_STACK_SHIFT[i][0] / 16.0, i * DISC_H / 16.0, DISC_STACK_SHIFT[i][1] / 16.0), RY(DISC_STACK_TURN[i]), base())
                qs += model_quads(model, textures, to_world(w))
            return qs
        scale = SCALE if n <= 1 else PILE_SCALE
        thick = scale * MODEL_THICKNESS
        foot = []
        for i, (kind, name) in enumerate(parts):
            sx, sz, rot = SLOTS[n - 1][i]
            if kind == 'disc':
                foot.append((poly_circle(sx, sz, DISC_D / 2 * DISC_MIX_SCALE), DISC_H))
            else:
                foot.append((poly_square(sx, sz, 16 * scale / 2.0, rot), thick))
        lifts = pile_lifts(foot)
        self.last_lifts = lifts
        qs = []
        for i, (kind, name) in enumerate(parts):
            sx, sz, rot = SLOTS[n - 1][i]
            if kind == 'disc':
                top, model = self.disc_assets(name)
                textures = {'disc': arr(top), 'particle': arr(top)}
                w = chain(T(0.5, 0, 0.5), RY(180.0 - YROT[facing]), T((sx - 8) / 16, lifts[i] / 16.0, (sz - 8) / 16), RY(rot),
                          Sc(DISC_MIX_SCALE, 1.0, DISC_MIX_SCALE), base())
                qs += model_quads(model, textures, to_world(w))
            else:
                model = self.sil_model(kind, name)
                tex = arr(self.sprite(kind, name))
                textures = {'item': tex, 'particle': tex}
                w = plate_world_pile(facing, n, i, lifts[i], MODEL_THICKNESS, 1.0)
                qs += model_quads(model, textures, to_world(w @ Sc(PX, PX, PX)))
        return qs


# ---------------------------------------------------------------------------------------------------------------
# sheets
# ---------------------------------------------------------------------------------------------------------------
def font(size):
    for path in ('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed.ttf'):
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def cell(quads_fn, cam, wall=False):
    qs = ground_quads() + (wall_quads() if wall else []) + quads_fn()
    return render(qs, cam)


def sheet(title, rows, path, note):
    """rows: list of (label, before_fn, after_fn, camera kwargs, wall)"""
    cw, ch = 340, 270
    lab = 150
    head = 62
    img = Image.new('RGB', (lab + 2 * cw + 12, head + len(rows) * ch + 8), (236, 236, 236))
    d = ImageDraw.Draw(img)
    d.text((8, 6), title, fill=(0, 0, 0), font=font(18))
    d.text((8, 30), note, fill=(60, 60, 60), font=font(12))
    d.text((lab + 8, 44), 'Vorher (heute im Spiel)', fill=(150, 30, 30), font=font(14))
    d.text((lab + cw + 12, 44), 'Nachher (Vorschlag)', fill=(20, 110, 40), font=font(14))
    for i, (label, bf, af, kw, wall) in enumerate(rows):
        y = head + i * ch
        d.text((8, y + ch // 2 - 24), label, fill=(0, 0, 0), font=font(13))
        cam = Camera(**kw)
        for j, fn in enumerate((bf, af)):
            if fn is None:
                continue
            img.paste(cell(fn, cam, wall), (lab + j * (cw + 12), y))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print('wrote', path)


def main():
    argv = sys.argv[1:]
    out = DEFAULT_OUT
    jar = os.environ.get('SB_CLIENT_JAR') or DEFAULT_JAR
    if '--out' in argv:
        out = argv[argv.index('--out') + 1]
    if '--jar' in argv:
        jar = argv[argv.index('--jar') + 1]
    sp = Sprites(jar)
    sc = Scene(sp)
    names = {'coast': 'coast_armor_trim_smithing_template', 'silence': 'silence_armor_trim_smithing_template',
             'wild': 'wild_armor_trim_smithing_template', 'netherite': 'netherite_upgrade_smithing_template'}
    cam = dict(yaw=32.0, pitch=34.0)
    low = dict(yaw=32.0, pitch=15.0, target=(8.0, 1.0, 8.0))
    wallcam = dict(yaw=28.0, pitch=26.0, target=(8.0, 7.0, 6.0))

    def S(kind, name, face, facing):
        return (lambda: sc.before_single(kind, name, face, facing)), (lambda: sc.after_single(kind, name, face, facing))

    def P(parts, facing='north'):
        return (lambda: sc.before_pile(parts, facing)), (lambda: sc.after_pile(parts, facing))

    def rows_for(kind, ex, label_of, pile_parts, extra=()):
        rows = []
        for n in ex:
            b, a = S(kind, n, 'floor', 'north')
            rows.append((label_of(n) + '\nBoden, einzeln', b, a, cam, False))
        b, a = S(kind, ex[0], 'floor', 'north')
        rows.append((label_of(ex[0]) + '\nBoden, flache\nKamera (Kante)', b, a, low, False))
        b, a = P(pile_parts)
        rows.append((('Schallplatten-Stapel\n(%d), Boden' if kind == 'disc' else 'Haeufchen\n%d Teile, Boden') % len(pile_parts), b, a, cam, False))
        for n in ex[:2]:
            b, a = S(kind, n, 'wall', 'south')
            rows.append((label_of(n) + '\nWand, einzeln', b, a, wallcam, True))
        rows.extend(extra)
        return rows

    tmpl_ex = [names['coast'], names['silence'], names['netherite']]
    tl = lambda n: {names['coast']: 'Coast', names['silence']: 'Silence', names['netherite']: 'Netherite-\nAufwertung', names['wild']: 'Wild'}[n]
    sheet('Schmiedevorlagen: platziert, Vorher/Nachher (Vorschau, nicht im Spiel)',
          rows_for('tmpl', tmpl_ex, tl, [('tmpl', names['coast']), ('tmpl', names['silence']), ('tmpl', names['wild']), ('tmpl', names['netherite'])]),
          os.path.join(out, 'templates.png'),
          'Nachher: 2 px dick, Kanten mit Blockschattierung; Haeufchen ca. 1,1 px, Lift aus der Ueberlappung berechnet. Licht: Naeherung.')

    sh = ['archer_pottery_sherd', 'heart_pottery_sherd', 'brewer_pottery_sherd']
    sl = lambda n: n.replace('_pottery_sherd', '').capitalize()
    sheet('Toepferscherben: platziert, Vorher/Nachher (Vorschau, nicht im Spiel)',
          rows_for('sherd', sh, sl, [('sherd', sh[0]), ('sherd', sh[1]), ('sherd', sh[2]), ('sherd', 'angler_pottery_sherd')]),
          os.path.join(out, 'sherds.png'),
          'Vorher = Platte, wie sie heute aussaehe (Scherben sind heute NICHT platzierbar). Nachher: 2 px, eine Silhouette fuer alle 23.')

    dsc = ['cat', 'pigstep', 'mod:voidline']
    dl = lambda n: {'cat': 'Cat (Vanilla)', 'pigstep': 'Pigstep (Vanilla)', 'mod:voidline': 'Voidline (Mod)', '13': '13'}[n]
    stack = [('disc', 'cat'), ('disc', 'pigstep'), ('disc', 'mod:voidline'), ('disc', '13')]
    mixed = [('tmpl', names['coast']), ('disc', 'cat'), ('sherd', sh[0])]
    extra = []
    b, a = P(stack)
    extra.append(('Stapel (4)\nflache Kamera', b, a, low, False))
    b, a = P(mixed)
    extra.append(('Gemischtes\nHaeufchen\n(Vorlage, Platte,\nScherbe)', b, a, cam, False))
    sheet('Schallplatten: platziert, Vorher/Nachher (Vorschau, nicht im Spiel)',
          rows_for('disc', dsc, dl, stack, extra),
          os.path.join(out, 'discs.png'),
          'Vorher = 15x10-Ellipse des Item-Bildes (Vanilla-Platten heute nicht platzierbar). Nachher: runde Scheibe O12, 1 px, Draufsicht-Textur.')

    # top-view textures, enlarged next to the item sprites (for the review of the new pixels)
    cells = [('cat', None), ('pigstep', None), ('13', None), ('mod:voidline', None)]
    big = Image.new('RGB', (len(cells) * 300 + 10, 340), (200, 200, 200))
    d = ImageDraw.Draw(big)
    for i, (n, _) in enumerate(cells):
        spr = sc.disc_sprite(n)
        top = disc_top(spr)
        big.paste(spr.resize((128, 128), Image.NEAREST), (10 + i * 300, 28), spr.resize((128, 128), Image.NEAREST))
        big.paste(top.resize((144, 144), Image.NEAREST), (150 + i * 300, 28), top.resize((144, 144), Image.NEAREST))
        d.text((10 + i * 300, 6), dl(n) + ': Item-Bild | Draufsicht 12x12', fill=(0, 0, 0), font=font(12))
        # lying disc as the strips would show: top view at 1:1 scale on a plain ground
        d.text((10 + i * 300, 180), 'Draufsicht (1:1, 4x)', fill=(0, 0, 0), font=font(11))
        big.paste(top.resize((48, 48), Image.NEAREST), (10 + i * 300, 198), top.resize((48, 48), Image.NEAREST))
    big.save(os.path.join(out, 'disc_topview_textures.png'))
    print('wrote', os.path.join(out, 'disc_topview_textures.png'))

    # model JSON (no pixels): one per silhouette, the round disc
    mdir = os.path.join(out, 'models')
    os.makedirs(mdir, exist_ok=True)
    for name, model in sc.models.items():
        with open(os.path.join(mdir, name + '.json'), 'w', encoding='utf-8', newline='\n') as f:
            json.dump(model, f, indent=1)
            f.write('\n')
    top, model = sc.disc_assets('cat')
    with open(os.path.join(mdir, 'placed_disc_round.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump(model, f, indent=1)
        f.write('\n')
    print('models:', sorted(os.listdir(mdir)))
    print('pile lifts (last mixed pile, px):', getattr(sc, 'last_lifts', None))


if __name__ == '__main__':
    main()
