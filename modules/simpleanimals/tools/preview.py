"""Simple Animals: owner preview page (docs/ai/PLAN-A-ANIMALS-VARIANTEN-2026-10-30.md). NOT part of the game or the build.

Renders every model of tools/gen_models.py (and the Vanilla horse model for zebra and zorse, own texture) from the same box
specs the game uses, with the same polygon/UV rules as Vanilla's ModelPart.Cube, as turntable frames (drag to turn), each next
to a player figure and the hitbox of the nearest Vanilla animal, in two variants where it makes sense. The page is one
self-contained HTML file (images as data URIs); Vanilla pixels are never used, the figures are plain coloured boxes.

    python3 modules/simpleanimals/tools/preview.py <out.html>
"""
from __future__ import annotations

import base64
import io
import json
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_models as G  # noqa: E402

W, H = 480, 330
S = 3.4          # screen pixels per model pixel
FRAMES = 16
PITCH = math.radians(17)


# ---------------------------------------------------------------- transforms
def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    rx_ = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry_ = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz_ = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz_ @ ry_ @ rx_


def part_matrix(pose, rotv, scale=(1, 1, 1)):
    m = np.eye(4)
    m[:3, :3] = rot(*rotv) @ np.diag(scale)
    m[:3, 3] = pose
    return m


def cube_quads(pos, size, grow, mirror, uv, tex_w, tex_h):
    """The six polygons of a Cube as (4 vertices in part space, 4 (u, v) in 0..1), exactly like ModelPart.Cube."""
    x, y, z = pos
    w, h, d = size
    minx, miny, minz, maxx, maxy, maxz = x - grow, y - grow, z - grow, x + w + grow, y + h + grow, z + d + grow
    if mirror:
        minx, maxx = maxx, minx
    t0, t1, t2, t3 = (minx, miny, minz), (maxx, miny, minz), (maxx, maxy, minz), (minx, maxy, minz)
    l0, l1, l2, l3 = (minx, miny, maxz), (maxx, miny, maxz), (maxx, maxy, maxz), (minx, maxy, maxz)
    u, v = uv
    u0, u1, u2, u22, u3, u4 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
    v0, v1, v2 = v, v + d, v + d + h
    polys = [([l1, l0, t0, t1], (u1, v0, u2, v1)), ([t2, t3, l3, l2], (u2, v1, u22, v0)), ([t0, l0, l3, t3], (u0, v1, u1, v2)),
             ([t1, t0, t3, t2], (u1, v1, u2, v2)), ([l1, t1, t2, l2], (u2, v1, u3, v2)), ([l0, l1, l2, l3], (u3, v1, u4, v2))]
    out = []
    for verts, (a0, b0, a1, b1) in polys:
        uvs = [(a1, b0), (a0, b0), (a0, b1), (a1, b1)]
        if mirror:
            verts, uvs = verts[::-1], uvs[::-1]
        out.append((verts, [(p / tex_w, q / tex_h) for p, q in uvs]))
    return out


class Mesh:
    """Flat list of textured quads in model space (y down, front = -z) plus flat-colour quads."""

    def __init__(self):
        self.quads = []   # (4x3 array, uvs or None, texture index or colour, double-sided)

    def add_model(self, parts, tex, tex_w, tex_h, root_rot=(0, 0, 0), tweaks=None, offset=(0, 0, 0), scale=1.0):
        tweaks = tweaks or {}
        mats = {None: np.eye(4)}
        mats[None][:3, :3] = rot(*root_rot)
        mats[None][:3, 3] = offset
        for p in parts:
            tw = tweaks.get(p['name'], {})
            pose = tuple(np.add(p['pose'], tw.get('d', (0, 0, 0))))
            m = mats[p['parent']] @ part_matrix(pose, p['rot'], tw.get('scale', (1, 1, 1)))
            mats[p['name']] = m
            if tw.get('hide'):
                continue
            for b in p['boxes']:
                for verts, uvs in cube_quads(b['pos'], b['size'], b.get('grow', 0), b.get('mirror', False), b['uv'], tex_w, tex_h):
                    pts = np.array([(m @ np.array([*v, 1.0]))[:3] for v in verts]) * scale
                    self.quads.append((pts, uvs, tex, False))

    def add_box(self, center_bottom, size, colour):
        """Plain box standing on the ground: size (w, h, d) in model px, colour tuple; in model space y down."""
        cx, cy, cz = center_bottom
        w, h, d = size
        x0, x1, y0, y1, z0, z1 = cx - w / 2, cx + w / 2, cy - h, cy, cz - d / 2, cz + d / 2
        c = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
        for idx in ((0, 1, 2, 3), (5, 4, 7, 6), (4, 0, 3, 7), (1, 5, 6, 2), (4, 5, 1, 0), (3, 2, 6, 7)):
            self.quads.append((np.array([c[i] for i in idx], dtype=float), None, colour, False))

    def add_floor(self, half, colour, y=24.0):
        c = [(-half, y, -half), (half, y, -half), (half, y, half), (-half, y, half)]
        self.quads.append((np.array(c, dtype=float), None, colour, True))


def steve(mesh, x):
    """Player figure (plain boxes, 1.8 m = 28.8 px; Vanilla player model proportions)."""
    k = 0.9375
    skin, shirt, trousers, shoes = (200, 144, 106), (58, 154, 176), (58, 58, 138), (60, 60, 60)
    mesh.add_box((x - 1.9 * k, 24, 0), (3.75, 11.25, 3.75), trousers)
    mesh.add_box((x + 1.9 * k, 24, 0), (3.75, 11.25, 3.75), trousers)
    mesh.add_box((x, 24 - 11.25, 0), (7.5, 11.25, 3.75), shirt)
    mesh.add_box((x - 5.6 * k, 24 - 11.25, 0), (3.75, 11.25, 3.75), shirt)
    mesh.add_box((x + 5.6 * k, 24 - 11.25, 0), (3.75, 11.25, 3.75), shirt)
    mesh.add_box((x, 24 - 22.5, 0), (7.5, 7.5, 7.5), skin)
    mesh.add_box((x - 1.9 * k, 24, 0), (3.8, 1.2, 3.8), shoes)
    mesh.add_box((x + 1.9 * k, 24, 0), (3.8, 1.2, 3.8), shoes)


def hitbox(mesh, x, width, height, length):
    mesh.add_box((x, 24, 0), (width * 16, height * 16, length * 16), (170, 176, 184))


# ---------------------------------------------------------------- rasteriser
def render(mesh, textures, azimuth, S=S):
    ca, sa = math.cos(azimuth), math.sin(azimuth)
    cp, sp = math.cos(PITCH), math.sin(PITCH)
    cx, cy = W / 2, H - 48
    zbuf = np.full((H, W), -1e9)
    img = np.zeros((H, W, 4), dtype=np.uint8)
    img[..., :3] = (36, 40, 48)
    img[..., 3] = 255

    def project(p):
        wx, wy, wz = p[:, 0], -(p[:, 1] - 24.0), -p[:, 2]
        x1 = wx * ca + wz * sa
        z1 = -wx * sa + wz * ca
        y2 = wy * cp - z1 * sp
        z2 = wy * sp + z1 * cp
        return np.stack([cx + S * x1, cy - S * y2, z2], axis=1)

    light = np.array([-0.35, 0.8, 0.5])
    light /= np.linalg.norm(light)
    # painter order does not matter (z-buffer); floor first so it never wins
    for pts, uvs, tex, floor in sorted(mesh.quads, key=lambda q: not q[3]):
        scr = project(pts)
        n = np.cross(pts[1] - pts[0], pts[2] - pts[0])
        ln = np.linalg.norm(n)
        if ln < 1e-9:
            continue
        n = n / ln
        wx, wy, wz = n[0], -n[1], -n[2]                      # world: y up, z toward the front
        x1, z1 = wx * ca + wz * sa, -wx * sa + wz * ca       # yaw
        y2, z2 = wy * cp - z1 * sp, wy * sp + z1 * cp        # pitch
        nv = np.array([x1, y2, z2])
        if nv[2] < 0:
            nv = -nv
        bright = 0.55 + 0.45 * max(0.0, float(np.dot(nv, light)))
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = (scr[i] for i in tri)
            minx, maxx = int(max(0, math.floor(min(a[0], b[0], c[0])))), int(min(W - 1, math.ceil(max(a[0], b[0], c[0]))))
            miny, maxy = int(max(0, math.floor(min(a[1], b[1], c[1])))), int(min(H - 1, math.ceil(max(a[1], b[1], c[1]))))
            if minx > maxx or miny > maxy:
                continue
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            xs, ys = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
            l1 = ((b[1] - c[1]) * (xs - c[0]) + (c[0] - b[0]) * (ys - c[1])) / den
            l2 = ((c[1] - a[1]) * (xs - c[0]) + (a[0] - c[0]) * (ys - c[1])) / den
            l3 = 1 - l1 - l2
            inside = (l1 >= -1e-6) & (l2 >= -1e-6) & (l3 >= -1e-6)
            if not inside.any():
                continue
            z = l1 * a[2] + l2 * b[2] + l3 * c[2]
            ok = inside & (z > zbuf[miny:maxy + 1, minx:maxx + 1])
            if not ok.any():
                continue
            if uvs is None:
                col = np.array(tex, dtype=float) * (bright if not floor else 1.0)
                px = np.broadcast_to(col, ok.shape + (3,))
                alpha = np.ones(ok.shape, dtype=bool)
            else:
                t = textures[tex]
                th, tw = t.shape[:2]
                u = l1 * uvs[tri[0]][0] + l2 * uvs[tri[1]][0] + l3 * uvs[tri[2]][0]
                v = l1 * uvs[tri[0]][1] + l2 * uvs[tri[1]][1] + l3 * uvs[tri[2]][1]
                ti = np.clip((u * tw).astype(int), 0, tw - 1)
                tj = np.clip((v * th).astype(int), 0, th - 1)
                texel = t[tj, ti]
                alpha = texel[..., 3] > 128
                px = texel[..., :3].astype(float) * bright
            sel = ok & alpha
            sub = img[miny:maxy + 1, minx:maxx + 1]
            zsub = zbuf[miny:maxy + 1, minx:maxx + 1]
            sub[sel, :3] = np.clip(px[sel], 0, 255).astype(np.uint8)
            zsub[sel] = z[sel]
    return Image.fromarray(img, 'RGBA').convert('RGB')


def gif_free_frames(mesh, textures, scale):
    frames = []
    for i in range(FRAMES):
        im = render(mesh, textures, 2 * math.pi * i / FRAMES, scale)
        buf = io.BytesIO()
        im.quantize(colors=96, method=Image.Quantize.MEDIANCUT).save(buf, 'PNG', optimize=True)
        frames.append('data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode())
    return frames


# ---------------------------------------------------------------- scenes
HORSE_PARTS = [
    dict(name='body', parent=None, pose=(0, 11, 5), rot=(0, 0, 0), boxes=[dict(pos=(-5, -8, -17), size=(10, 10, 22), uv=(0, 32), grow=0.05)]),
    dict(name='head_parts', parent=None, pose=(0, 4, -12), rot=(math.pi / 6, 0, 0), boxes=[dict(pos=(-2.05, -6, -2), size=(4, 12, 7), uv=(0, 35))]),
    dict(name='head', parent='head_parts', pose=(0, 0, 0), rot=(0, 0, 0), boxes=[dict(pos=(-3, -11, -2), size=(6, 5, 7), uv=(0, 13))]),
    dict(name='mane', parent='head_parts', pose=(0, 0, 0), rot=(0, 0, 0), boxes=[dict(pos=(-1, -11, 5.01), size=(2, 16, 2), uv=(56, 36))]),
    dict(name='mouth', parent='head_parts', pose=(0, 0, 0), rot=(0, 0, 0), boxes=[dict(pos=(-2, -11, -7), size=(4, 5, 5), uv=(0, 25))]),
    dict(name='ear_l', parent='head', pose=(0, 0, 0), rot=(0, 0, 0), boxes=[dict(pos=(0.55, -13, 4), size=(2, 3, 1), uv=(19, 16))]),
    dict(name='ear_r', parent='head', pose=(0, 0, 0), rot=(0, 0, 0), boxes=[dict(pos=(-2.55, -13, 4), size=(2, 3, 1), uv=(19, 16))]),
    dict(name='tail', parent='body', pose=(0, -5, 2), rot=(math.pi / 6, 0, 0), boxes=[dict(pos=(-1.5, 0, 0), size=(3, 14, 4), uv=(42, 36))]),
] + [dict(name=n, parent=None, pose=(sx * 4, 14, z), rot=(0, 0, 0),
          boxes=[dict(pos=(-3 if sx > 0 else -1, -1.01, zo), size=(4, 11, 4), uv=(48, 21), mirror=sx > 0)])
     for n, sx, z, zo in (('lh', 1, 7, -1), ('rh', -1, 7, -1), ('lf', 1, -10, -1.9), ('rf', -1, -10, -1.9))]


def texture_array(path):
    return np.array(Image.open(path).convert('RGBA'))


def scene(model, tex_path, tweaks=None, scale=1.0, root_rot=(0, 0, 0), rel=None, spacing=22, zoom=S):
    """One turntable: the animal in the middle, the player at the left, the Vanilla relative's hitbox at the right."""
    if model == 'horse':
        parts, tw, th = HORSE_PARTS, 64, 64
    else:
        spec = G.PALETTES[model]
        place, height = G.pack(spec)
        parts = [dict(name=p['name'], parent=p['parent'], pose=p['pose'], rot=p['rot'],
                      boxes=[dict(pos=b['pos'], size=b['size'], uv=place[b['share']], grow=b['grow'], mirror=b['mirror']) for b in p['boxes']])
                 for p in spec['parts']]
        tw, th = spec['w'], height
    mesh = Mesh()
    mesh.add_floor(400, (62, 84, 56))
    mesh.add_model(parts, 0, tw, th, root_rot=root_rot, tweaks=tweaks, scale=scale)
    steve(mesh, -spacing - 30)
    if rel:
        hitbox(mesh, spacing + 24 + rel[2] * 8, rel[0], rel[1], rel[2])
    return gif_free_frames(mesh, [texture_array(tex_path)], zoom)


def build(out_html):
    tex = G.TEX
    cards = []

    def card(key, title, text, variants):
        cards.append(dict(key=key, title=title, text=text, variants=variants))

    def variant(label, note, frames):
        return dict(label=label, note=note, frames=frames)

    stats = {k: G.hard_share(G.TEXTURES[k]()) for k in G.TEXTURES}

    def pct(k, home=''):
        return f'{stats[k] * 100:.0f} % harte Übergänge' + (f'. Heimat: {home}' if home else '')

    hide_antlers = {n: {'hide': True} for n in ('antler_right', 'antler_right_front', 'antler_right_top', 'antler_left', 'antler_left_front', 'antler_left_top')}
    deer_rel = (0.9, 1.4, 1.2)
    card('deer', 'Hirsch', 'Beide Fellarten als Varianten, nach Biom verteilt (Heimat 8 : 1, also etwas Mischung). Grau = Hitbox der Vanilla-Kuh (0,9 x 1,4 m).', [
        variant('Rotwild (Bock)', pct('deer_red', 'Taiga, Berge, Hügel, kalte Biome'), scene('deer_red', tex / 'deer/red.png', rel=deer_rel)),
        variant('Damwild (Ricke, helle Flecken)', pct('deer_fallow', 'alle übrigen Biome (Wald, Ebene)'), scene('deer_fallow', tex / 'deer/fallow.png', tweaks=hide_antlers, rel=deer_rel)),
        variant('Kitz (halbe Größe)', pct('deer_fawn'), scene('deer_fawn', tex / 'deer/fawn.png', scale=0.5, tweaks=hide_antlers, rel=deer_rel)),
    ])
    pig_rel = (0.9, 0.9, 1.0)
    card('boar', 'Wildschwein', 'Dunkel ist der Standard, dazu Biom-Varianten. Keine Geschlechter (Vanilla kennt keine). Grau = Hitbox des Vanilla-Schweins (0,9 x 0,9 m).', [
        variant('Dunkel (Standard)', pct('boar_dark', 'überall, wo keine andere Fellfarbe zuhause ist'), scene('boar_dark', tex / 'boar/dark.png', rel=pig_rel, zoom=4.2)),
        variant('Sandbraun', pct('boar_sandy', 'Wüste, Badlands, Savanne'), scene('boar_sandy', tex / 'boar/sandy.png', rel=pig_rel, zoom=4.2)),
        variant('Frostgrau', pct('boar_frost', 'Taiga, Berge, kalte Biome'), scene('boar_frost', tex / 'boar/frost.png', rel=pig_rel, zoom=4.2)),
        variant('Rostbraun', pct('boar_rust', 'Sumpf, Dschungel, Mangroven'), scene('boar_rust', tex / 'boar/rust.png', rel=pig_rel, zoom=4.2)),
        variant('Frischling (halbe Größe)', pct('boar_young'), scene('boar_young', tex / 'boar/young.png', scale=0.5, rel=pig_rel, zoom=4.2)),
    ])
    arm_rel = (0.7, 0.65, 0.8)
    curl = {'body': {'scale': (1.1, 1.45, 0.8), 'd': (0, 2, 0)}, 'head': {'hide': True}, 'right_hind_leg': {'hide': True}, 'left_hind_leg': {'hide': True},
            'right_front_leg': {'hide': True}, 'left_front_leg': {'hide': True}}

    def hog(key, label, home, curled=False):
        return variant(label, pct(key, home), scene(key, tex / (G.OUT_NAMES[key] + '.png'), tweaks=curl if curled else None, rel=arm_rel, spacing=12, zoom=7.5))

    card('hedgehog', 'Igel', 'Mehrere Arten als Biom-Varianten, realistisch braun als Standard. Jagt Silberfische, rollt sich in 0,3 s ein. Grau = Hitbox des Vanilla-Gürteltiers (0,7 x 0,65 m).', [
        hog('hedgehog_brown', 'Braunbrustigel (Standard)', 'Wald, Ebene, übrige Biome'),
        hog('hedgehog_brown', 'Braunbrustigel, eingerollt', '', True),
        hog('hedgehog_desert', 'Wüstenigel', 'Wüste, Badlands, Savanne'),
        hog('hedgehog_whitebreast', 'Weißbrustigel', 'Taiga, Berge, kalte Biome'),
        hog('hedgehog_fourtoed', 'Weißbauchigel (helle Stacheln)', 'Sumpf, Dschungel, Mangroven'),
    ])
    turtle_rel = (1.2, 0.4, 1.2)
    card('crab', 'Krebs', 'Rot. Selten, harmlos, läuft seitwärts. Grau = Hitbox der Vanilla-Schildkröte (1,2 x 0,4 m).', [
        variant('Rot (Standard)', pct('crab_red', 'überall'), scene('crab_red', tex / 'crab/red.png', root_rot=(0, math.pi / 2, 0), rel=turtle_rel, spacing=14, zoom=6.5)),
        variant('Korallenrot', pct('crab_coral', 'Strand, Ozean, Fluss'), scene('crab_coral', tex / 'crab/coral.png', root_rot=(0, math.pi / 2, 0), rel=turtle_rel, spacing=14, zoom=6.5)),
    ])
    horse_rel = (1.4, 1.6, 1.8)
    card('zebra', 'Zebra und Zorse', 'Zebra klassisch schwarz-weiß, Streifen bewusst hart, der Rest weich. Vanilla-Pferdemodell. Grau = Hitbox des Vanilla-Pferds (1,4 x 1,6 m).', [
        variant('Zebra (schwarz-weiß)', pct('zebra'), scene('horse', tex / 'zebra.png', rel=horse_rel, spacing=30, zoom=3.0)),
        variant('Zorse (braun, Streifen an Beinen/Hinterteil)', pct('zorse'), scene('horse', tex / 'zorse.png', rel=horse_rel, spacing=30, zoom=3.0)),
    ])
    html = (PAGE.replace('__DATA__', json.dumps(cards)).replace('__CODE__', 'ANIMALS31').replace('__TITLE__', 'Simple Animals: Fellvarianten nach Biom')
            .replace('__INTRO__', 'Hirsch, Wildschwein, Igel, Krebs und Zebra mit Varianten nach Biom (Heimat 8 : 1, daher etwas Mischung); Texturen nach A8b (weiche Töne, Zebra-Streifen bewusst hart). Die Standardfassung steckt bereits im Code.'))
    Path(out_html).write_text(html, encoding='utf-8')
    print('preview written:', out_html, f'{len(html) / 1e6:.1f} MB')


PAGE = """<!doctype html>
<html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Simple Animals Varianten</title>
<style>
:root{--bg:#1d2026;--card:#272b33;--tx:#e8eaee;--mut:#a4abb8;--acc:#7fc27f;--bad:#e0916b;--bd:#3b414d}
@media (prefers-color-scheme: light){:root{--bg:#f2f3f5;--card:#fff;--tx:#1c2026;--mut:#5a6270;--acc:#2f7d32;--bad:#b3541f;--bd:#cfd4dc}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--tx);font:16px/1.45 system-ui,sans-serif}
main{max-width:980px;margin:0 auto;padding:16px}h1{font-size:1.5rem;margin:.2em 0}h2{font-size:1.2rem;margin:.1em 0}
.card{background:var(--card);border:1px solid var(--bd);border-radius:10px;padding:14px;margin:16px 0}
.view{position:relative;max-width:480px;margin:8px auto;touch-action:pan-y;cursor:ew-resize;user-select:none}
.view img{width:100%;height:auto;display:block;image-rendering:pixelated;border-radius:8px}
.hint{color:var(--mut);font-size:.85rem;text-align:center}.vs{display:flex;flex-wrap:wrap;gap:8px;margin:8px 0}
button.t{border:1px solid var(--bd);border-radius:8px;padding:6px 10px;cursor:pointer;background:var(--card);color:var(--tx);font:inherit}
button.t[aria-pressed=true]{border-color:var(--acc);outline:2px solid var(--acc)}
.note{color:var(--mut);font-size:.9rem}.btn{background:var(--acc);color:#fff;border:0;border-radius:8px;padding:8px 14px;font:inherit;cursor:pointer}
.rate{display:flex;gap:14px;flex-wrap:wrap;margin:6px 0}.rate label{cursor:pointer}.ok{color:var(--acc)}.chg{color:var(--bad)}
textarea{width:100%;min-height:90px;background:var(--bg);color:var(--tx);border:1px solid var(--bd);border-radius:8px;padding:8px}
</style></head><body><main>
<h1>__TITLE__</h1>
<p class="note">__INTRO__ Jede Ansicht ist drehbar (ziehen, auch mit dem Finger); links steht ein Spieler (1,8 m), rechts die Hitbox des verwandten Vanilla-Tiers als grauer Kasten (Maßstab, kein Vanilla-Modell). Zu jeder Variante: Reiter wählen, ansehen, unten „passt“ oder „ändern“ anklicken. Der Antwortcode ganz unten fasst alles zusammen.</p>
<div id="cards"></div>
<div class="card"><h2>Deine Wahl</h2><p class="note">Antwortcode (kopieren und als Antwort senden):</p><textarea id="out" readonly></textarea>
<p><button class="btn" id="all">Alle Varianten passen</button> <button class="btn" id="copy">Kopieren</button></p></div>
</main><script>
const DATA=__DATA__;const CODE='__CODE__';const state={},rating={};
function build(){const root=document.getElementById('cards');DATA.forEach(c=>{
 const d=document.createElement('div');d.className='card';
 d.innerHTML='<h2>'+c.title+'</h2><p class="note">'+c.text+'</p><div class="vs"></div><div class="view"><img alt="'+c.title+'"></div><p class="hint">ziehen zum Drehen</p><p class="note" id="n-'+c.key+'"></p><div class="rate"></div>';
 const img=d.querySelector('img'),vs=d.querySelector('.vs'),rate=d.querySelector('.rate');state[c.key]={v:0,f:0};
 c.variants.forEach((v,i)=>{rating[c.key+'/'+i]='offen';
  const b=document.createElement('button');b.className='t';b.textContent=v.label;b.setAttribute('aria-pressed',i==0);
  b.onclick=()=>{state[c.key].v=i;[...vs.children].forEach((x,j)=>x.setAttribute('aria-pressed',j==i));show(c,img,rate)};vs.appendChild(b)});
 const view=d.querySelector('.view');let down=false,x0=0,f0=0;
 view.addEventListener('pointerdown',e=>{down=true;x0=e.clientX;f0=state[c.key].f;view.setPointerCapture(e.pointerId)});
 view.addEventListener('pointermove',e=>{if(!down)return;const n=c.variants[state[c.key].v].frames.length;state[c.key].f=((f0-Math.round((e.clientX-x0)/18))%n+n)%n;show(c,img,rate)});
 view.addEventListener('pointerup',()=>down=false);view.addEventListener('pointercancel',()=>down=false);
 root.appendChild(d);show(c,img,rate)})}
function show(c,img,rate){const s=state[c.key],v=c.variants[s.v],k=c.key+'/'+s.v;img.src=v.frames[s.f%v.frames.length];document.getElementById('n-'+c.key).textContent=v.note;
 rate.innerHTML='<b>'+v.label+':</b> ';['passt','ändern'].forEach(o=>{const l=document.createElement('label');l.className=o=='passt'?'ok':'chg';const r=document.createElement('input');r.type='radio';r.name='r-'+k;r.checked=rating[k]==o;r.onchange=()=>{rating[k]=o;summ()};l.appendChild(r);l.append(' '+o);rate.appendChild(l)})}
function summ(){document.getElementById('out').value=CODE+': '+DATA.map(c=>c.title+' ['+c.variants.map((v,i)=>v.label+'='+rating[c.key+'/'+i]).join('; ')+']').join(' | ')}
build();summ();
document.getElementById('all').onclick=()=>{for(const k in rating)rating[k]='passt';DATA.forEach(c=>{const d=document.querySelectorAll('.card')});summ();document.querySelectorAll('.rate input').forEach(r=>{r.checked=r.parentElement.className=='ok'})};
document.getElementById('copy').onclick=()=>{const t=document.getElementById('out');t.select();navigator.clipboard&&navigator.clipboard.writeText(t.value)};
</script></body></html>
"""

if __name__ == '__main__':
    build(sys.argv[1] if len(sys.argv) > 1 else '/root/sb-tools-a/previews/a-animals31-preview.html')
