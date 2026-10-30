"""Simple Dinosaurs: owner preview page (docs/ai/PLAN-A-DINOS-2026-10-30.md). NOT part of the game or the build.

Renders every model of tools/gen_models.py (and the Vanilla horse model for zebra and zorse, own texture) from the same box
specs the game uses, with the same polygon/UV rules as Vanilla's ModelPart.Cube, as turntable frames (drag to turn), each next
to a player figure and the hitbox of the nearest Vanilla animal, in two variants where it makes sense. The page is one
self-contained HTML file (images as data URIs); Vanilla pixels are never used, the figures are plain coloured boxes.

    python3 modules/simpledinos/tools/preview.py <out.html>
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
TREX_HIDE_GEAR = {'saddle': {'hide': True}, 'armor_head': {'hide': True}, 'armor_back': {'hide': True}}
GEAR = {'saddle', 'armor_head', 'armor_back'}


def array_of(img):
    return np.array(img.convert('RGBA'))


def scene(key, hide=(), scale=1.0, tweaks=None, spacing=40, zoom=2.1, rel=None, pose=None):
    """One turntable: the dinosaur in the middle, the player at the left, a Vanilla relative's hitbox / the core hitbox at the right."""
    spec = G.PALETTES[key]
    place, height = G.pack(spec)
    parts = [dict(name=p['name'], parent=p['parent'], pose=p['pose'], rot=p['rot'],
                  boxes=[dict(pos=b['pos'], size=b['size'], uv=place[b['share']], grow=b['grow'], mirror=b['mirror']) for b in p['boxes']])
             for p in spec['parts']]
    tw = dict(tweaks or {})
    for name in hide:
        tw[name] = {'hide': True}
    mesh = Mesh()
    mesh.add_floor(900, (62, 84, 56))
    mesh.add_model(parts, 0, spec['w'], height, tweaks=tw, scale=scale)
    steve(mesh, -spacing - 30)
    if rel:
        hitbox(mesh, spacing + 30 + rel[2] * 8, rel[0], rel[1], rel[2])
    return gif_free_frames(mesh, [array_of(G.TEXTURES[key]())], zoom)


def image_uri(path, scale=8):
    im = Image.open(path).convert('RGBA')
    im = im.resize((im.width * scale, im.height * scale), Image.Resampling.NEAREST)
    buf = io.BytesIO()
    im.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def build(out_html):
    import gen_blocks as GB
    cards = []

    def stats(key):
        return f'{G.hard_share(G.TEXTURES[key]()) * 100:.0f} % harte \u00dcberg\u00e4nge'

    horse = (1.4, 1.6, 1.8)   # Vanilla horse hitbox for scale
    core = (3.0, 4.0, 3.0)    # the T. rex core hitbox (R4); the body is longer than that
    variants = [('A Olivgr\u00fcn mit dunklen Bändern (Empfehlung, im Spiel)', 'olive'), ('B Sandbraun (Savanne)', 'sand'), ('C Rostrot', 'rust')]
    cards.append(dict(key='trex', title='T-Rex, ausgewachsen', text='Lebensgröße (12 m Konzept, hier etwa 10,8 m von der Schnauze bis zur Schwanzspitze, 4,7 m Kopfhöhe), 3 x 4 Blöcke Kern-Hitbox (grau) neben dem Spieler (1,8 m). Ziehen zum Drehen.',
                      variants=[dict(label=label, note=stats(f'trex_{coat}_0'), frames=scene(f'trex_{coat}_0', hide=GEAR, rel=core)) for label, coat in variants]))
    cards.append(dict(key='trex_gear', title='T-Rex mit Sattel und Rüstung', text='Großer Sattel (nur aufgesetzt, wenn gesattelt) und Rüstung als Platten auf Kopf und Rücken: Leder, Eisen, Diamant (Texturvariante des T-Rex).',
                      variants=[dict(label='Gro\u00dfer Sattel', note='Sattel', frames=scene('trex_olive_0', hide=('armor_head', 'armor_back'), rel=core)),
                                dict(label='Leder-R\u00fcstung + Sattel', note='Leder', frames=scene('trex_olive_1', rel=core)),
                                dict(label='Eisen-R\u00fcstung + Sattel', note='Eisen', frames=scene('trex_olive_2', rel=core)),
                                dict(label='Diamant-R\u00fcstung + Sattel', note='Diamant', frames=scene('trex_olive_3', rel=core))]))
    cards.append(dict(key='trex_baby', title='Jungtier (Wachstum 25 % bis 100 %)', text='Das Jungtier schl\u00fcpft mit 25 % der Gr\u00f6\u00dfe und w\u00e4chst in 40 Minuten stufenlos (wie der Schn\u00fcffler). Gezeigt: 25 %, 60 %, 100 %.',
                      variants=[dict(label=f'{p} %', note='Wachstum', frames=scene('trex_olive_0', hide=GEAR, scale=0.25 + 0.75 * p / 100.0, rel=core)) for p in (0, 60)]
                      + [dict(label='100 %', note='ausgewachsen', frames=scene('trex_olive_0', hide=GEAR, rel=core))]))
    images = GB.preview_images(image_uri)
    html = PAGE.replace('__DATA__', json.dumps(cards)).replace('__IMAGES__', json.dumps(images))
    Path(out_html).write_text(html, encoding='utf-8')
    print('preview written:', out_html, f'{len(html) / 1e6:.1f} MB')


PAGE = """<!doctype html>
<html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Simple Dinosaurs Vorschau</title>
<style>
:root{--bg:#1d2026;--card:#272b33;--tx:#e8eaee;--mut:#a4abb8;--acc:#7fc27f;--bd:#3b414d}
@media (prefers-color-scheme: light){:root{--bg:#f2f3f5;--card:#fff;--tx:#1c2026;--mut:#5a6270;--acc:#2f7d32;--bd:#cfd4dc}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--tx);font:16px/1.45 system-ui,sans-serif}
main{max-width:980px;margin:0 auto;padding:16px}h1{font-size:1.5rem;margin:.2em 0}h2{font-size:1.2rem;margin:.1em 0}
.card{background:var(--card);border:1px solid var(--bd);border-radius:10px;padding:14px;margin:16px 0}
.view{position:relative;max-width:480px;margin:8px auto;touch-action:pan-y;cursor:ew-resize;user-select:none}
.view img{width:100%;height:auto;display:block;image-rendering:pixelated;border-radius:8px}
.hint{color:var(--mut);font-size:.85rem;text-align:center}.vs{display:flex;flex-wrap:wrap;gap:8px;margin:8px 0}
label.v{border:1px solid var(--bd);border-radius:8px;padding:6px 10px;cursor:pointer}
label.v:has(input:checked){border-color:var(--acc);outline:2px solid var(--acc)}
.note{color:var(--mut);font-size:.9rem}.btn{background:var(--acc);color:#fff;border:0;border-radius:8px;padding:8px 14px;font:inherit;cursor:pointer}
textarea{width:100%;min-height:90px;background:var(--bg);color:var(--tx);border:1px solid var(--bd);border-radius:8px;padding:8px}
.grid{display:flex;flex-wrap:wrap;gap:10px}.grid figure{margin:0;text-align:center;font-size:.8rem;color:var(--mut)}.grid img{image-rendering:pixelated;width:96px;height:96px;border:1px solid var(--bd);border-radius:6px;background:#3a3f48}
</style></head><body><main>
<h1>Simple Dinosaurs Stufe 1: Fossil, Eier, T-Rex</h1>
<p class="note">Eigene Würfel und Pixel, Vanilla-nah. Jede Ansicht ist drehbar (ziehen, auch mit dem Finger); links steht ein Spieler (1,8 m), rechts die Kern-Hitbox als grauer Kasten. Wähle die Fellfarbe, die im Spiel Standard sein soll (Empfehlung A ist bereits im Code); die anderen bleiben als mögliche Varianten. Die vier weiteren Dinosaurier folgen erst nach deiner Abnahme dieses Musters.</p>
<div id="cards"></div>
<div id="imgs"></div>
<div class="card"><h2>Deine Wahl</h2><p class="note">Der Text unten fasst die Auswahl zusammen; kopiere ihn in die Antwort.</p><textarea id="out" readonly></textarea><p><button class="btn" id="copy">Kopieren</button></p></div>
</main><script>
const DATA=__DATA__;const IMAGES=__IMAGES__;const state={};
function build(){const root=document.getElementById('cards');DATA.forEach(c=>{
 const d=document.createElement('div');d.className='card';d.innerHTML='<h2>'+c.title+'</h2><p class="note">'+c.text+'</p><div class="view"><img alt="'+c.title+'"></div><p class="hint">ziehen zum Drehen</p><div class="vs"></div><p class="note" id="n-'+c.key+'"></p>';
 const img=d.querySelector('img'),vs=d.querySelector('.vs');state[c.key]={v:0,f:0};
 c.variants.forEach((v,i)=>{const l=document.createElement('label');l.className='v';l.innerHTML='<input type="radio" name="'+c.key+'" '+(i==0?'checked':'')+'> '+v.label;l.querySelector('input').addEventListener('change',()=>{state[c.key].v=i;show(c,img);summ()});vs.appendChild(l)});
 const view=d.querySelector('.view');let down=false,x0=0,f0=0;
 view.addEventListener('pointerdown',e=>{down=true;x0=e.clientX;f0=state[c.key].f;view.setPointerCapture(e.pointerId)});
 view.addEventListener('pointermove',e=>{if(!down)return;const n=c.variants[state[c.key].v].frames.length;state[c.key].f=((f0-Math.round((e.clientX-x0)/18))%n+n)%n;show(c,img)});
 view.addEventListener('pointerup',()=>down=false);view.addEventListener('pointercancel',()=>down=false);
 root.appendChild(d);show(c,img)});
 const im=document.getElementById('imgs');IMAGES.forEach(g=>{const d=document.createElement('div');d.className='card';d.innerHTML='<h2>'+g.title+'</h2><p class="note">'+g.text+'</p><div class="grid">'+g.items.map(i=>'<figure><img src="'+i.src+'" alt=""><br>'+i.label+'</figure>').join('')+'</div>';im.appendChild(d)})}
function show(c,img){const s=state[c.key],v=c.variants[s.v];img.src=v.frames[s.f%v.frames.length];document.getElementById('n-'+c.key).textContent=v.note}
function summ(){document.getElementById('out').value='DINO30: '+DATA.filter(c=>c.key=='trex').map(c=>c.title+' = '+c.variants[state[c.key].v].label).join('; ')}
build();summ();document.getElementById('copy').onclick=()=>{const t=document.getElementById('out');t.select();navigator.clipboard&&navigator.clipboard.writeText(t.value)};
</script></body></html>
"""

if __name__ == '__main__':
    build(sys.argv[1] if len(sys.argv) > 1 else '/root/sb-tools-a/previews/a-dino30-preview.html')
