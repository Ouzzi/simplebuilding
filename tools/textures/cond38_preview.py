"""Simple Beacons IG31-38: preview of 3D inventory models for the Mighty/Evil/Mighty Evil conduits (and Strong).

No Minecraft needed: a tiny numpy ray-caster draws block-model-style cuboids (Vanilla conduit textures, 16 px = one block
side, GUI display rotation [30,225,0], scale 0.625, nearest texel sampling) exactly the way an `item/<kind>.json` block model
with `tints` would. Variants:
  A = Vanilla conduit shell (6x6x6, base.png) tinted with the kind colour (same tint as the placed block, ConduitKind.tint)
  B = Vanilla conduit cage (8x8x8, cage.png) tinted, plus a glowing eye core (eye.png) inside, like the active block
Run: python3.12 tools/textures/cond38_preview.py [VANILLA_ASSETS_DIR] [OUT_HTML]
"""
import base64, colorsys, io, math, sys
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
VAN = Path(sys.argv[1] if len(sys.argv) > 1 else '/root/mc263src/assets/minecraft')
OUT = Path(sys.argv[2] if len(sys.argv) > 2 else '/root/sb-tools-b/previews/b-cond38-preview.html')
ITEM = ROOT / 'modules/simplebeacons/shared/resources/assets/simplebeacons/textures/item'
CT = VAN / 'textures/entity/conduit'
KINDS = [('strong_conduit', 'Strong', 0x9FE6FF, 0x7FD8FF), ('mighty_conduit', 'Mighty (lila)', 0xC08CFF, 0xB36BFF),
         ('evil_conduit', 'Evil (rot)', 0xFF6A5E, 0xE03030), ('mighty_evil_conduit', 'Mighty Evil (dunkelrot)', 0xC02844, 0x8A0A1E)]  # tint, dust
LIGHT = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6}
NORM = {'up': (0, 1, 0), 'down': (0, -1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0)}
# (right, down) axes of each face seen from outside
AX = {'up': ((1, 0, 0), (0, 0, 1)), 'down': ((1, 0, 0), (0, 0, -1)), 'north': ((-1, 0, 0), (0, -1, 0)),
      'south': ((1, 0, 0), (0, -1, 0)), 'east': ((0, 0, -1), (0, -1, 0)), 'west': ((0, 0, 1), (0, -1, 0))}


def eye_shift(t, tint):
    """Vanilla eye (blue, hue ~195) turned to the hue of the kind's dust colour, value kept."""
    th = colorsys.rgb_to_hsv(*(((tint >> s) & 255) / 255 for s in (16, 8, 0)))
    out = t.copy()
    for y in range(t.shape[0]):
        for x in range(t.shape[1]):
            r, g, b, a = t[y, x]
            if a < 128:
                continue
            h, sat, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            h = (h + th[0] - 195 / 360) % 1.0
            out[y, x, :3] = [c * 255 for c in colorsys.hsv_to_rgb(h, sat * (0.6 + 0.4 * th[1]), v * (0.55 + 0.45 * th[2]))]
    return out


def tex(p):
    return np.asarray(Image.open(p).convert('RGBA'), dtype=np.float32)


def cuboid_faces(size, tx, ty, w, d, h, tint, texture, emissive=False, uvscale=None):
    """Faces of a centred cuboid (w,h,d model units) using the Vanilla ModelPart unwrap at texture offset (tx,ty)."""
    rect = {'up': (tx + d, ty, w, d), 'down': (tx + d + w, ty, w, d), 'west': (tx, ty + d, d, h), 'north': (tx + d, ty + d, w, h),
            'east': (tx + d + w, ty + d, d, h), 'south': (tx + 2 * d + w, ty + d, w, h)}
    return [dict(face=f, half=(w / 2, h / 2, d / 2), rect=r, tint=tint, tex=texture, emissive=emissive) for f, r in rect.items()]


def eye_faces(s, texture):
    return [dict(face=f, half=(s / 2,) * 3, rect=(0, 0, 8, 8), tint=0xFFFFFF, tex=texture, emissive=True) for f in NORM]


def render(faces, scale, size=48):
    rx, ry = math.radians(30), math.radians(225)
    cx, sx_ = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    Rx = np.array([[1, 0, 0], [0, cx, -sx_], [0, sx_, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    R = Rx @ Ry                      # model -> view; view z points at the camera
    ppu = 0.625 * scale * (size / 16)  # screen pixels per model unit
    ys, xs = np.mgrid[0:size, 0:size]
    sxp = (xs + 0.5 - size / 2) / ppu
    syp = -(ys + 0.5 - size / 2) / ppu
    img = np.zeros((size, size, 4), np.float32)
    zbuf = np.full((size, size), -1e9)
    for f in faces:
        n = np.array(NORM[f['face']], float)
        if (R @ n)[2] <= 1e-6:
            continue
        half = np.array(f['half'])
        centre = n * half
        r_ax, d_ax = (np.array(a, float) for a in AX[f['face']])
        # plane in view space: points p = R(centre + a*r + b*d); solve for the view (x,y) -> z
        nv = R @ n
        cv = R @ centre
        z = cv[2] + (nv[0] * (cv[0] - sxp) + nv[1] * (cv[1] - syp)) / nv[2]
        pv = np.stack([sxp, syp, z], -1)
        pm = (pv @ R)                  # R^T applied: model space
        loc = pm - centre
        a = loc @ r_ax; b = loc @ d_ax
        ext_r = abs(r_ax @ half); ext_d = abs(d_ax @ half)
        ok = (abs(a) <= ext_r) & (abs(b) <= ext_d)
        u = (a + ext_r) / (2 * ext_r); v = (b + ext_d) / (2 * ext_d)
        x0, y0, rw, rh = f['rect']
        px = np.clip((x0 + u * rw).astype(int), x0, x0 + rw - 1)
        py = np.clip((y0 + v * rh).astype(int), y0, y0 + rh - 1)
        t = f['tex'][py, px]
        tint = np.array([(f['tint'] >> 16) & 255, (f['tint'] >> 8) & 255, f['tint'] & 255]) / 255
        lum = 1.0 if f['emissive'] else LIGHT[f['face']]
        col = t[..., :3] * tint * lum
        m = ok & (t[..., 3] > 127) & (z > zbuf)
        img[m, :3] = col[m]; img[m, 3] = 255; zbuf[m] = z[m]
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')


def slot(im, size=48):
    bg = Image.new('RGBA', (size, size), (139, 139, 139, 255))
    bg.alpha_composite(im)
    return bg


def b64(im, zoom):
    im = im.resize((im.width * zoom, im.height * zoom), Image.NEAREST)
    buf = io.BytesIO(); im.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def main():
    base, cage, eye = tex(CT / 'base.png'), tex(CT / 'cage.png'), tex(CT / 'open_eye.png')
    rows = []
    for kid, label, tint, dust in KINDS:
        def A(): return cuboid_faces(6, 0, 0, 6, 6, 6, tint, base)
        def B(): return cuboid_faces(8, 0, 0, 8, 8, 8, tint, cage) + eye_faces(4.4, eye_shift(eye, dust))
        cells = {
            'heute': slot(Image.open(ITEM / f'{kid}.png').convert('RGBA').resize((48, 48), Image.NEAREST)) if (ITEM / f'{kid}.png').exists() else None,
            'vanilla': slot(render(cuboid_faces(6, 0, 0, 6, 6, 6, 0xFFFFFF, base), 1.0)),
            'Agross': slot(render(A(), 2.0)), 'Bgross': slot(render(B(), 1.6)),
            'Aklein': slot(render(A(), 1.0)), 'Bklein': slot(render(B(), 1.0)),
        }
        det = {'A': render(A(), 2.0 * 3, 144), 'B': render(B(), 1.6 * 3, 144)}
        rows.append((kid, label, cells, det))
    h = ['<!doctype html><meta charset=utf-8><title>COND38 Conduit-Items 3D</title><style>body{font:14px sans-serif;background:#222;color:#eee;margin:20px}'
         'img{image-rendering:pixelated}table{border-collapse:collapse}td,th{padding:8px;text-align:center;border:1px solid #444}'
         '.big{background:#8b8b8b}label{cursor:pointer}input:checked+span{outline:3px solid #6f6}code{background:#000;padding:4px 8px;display:block;margin:10px 0}</style>'
         '<h1>IG31-38: Mod-Conduits als 3D-Inventar-Item</h1>'
         '<p>Technik: normales Item-Modell (<code style="display:inline">minecraft:model</code> + Block-Modell aus Quadern mit Vanilla-Conduit-Texturen + <code style="display:inline">tints</code>, '
         'Farbe = dieselbe wie am platzierten Block). Kein Java noetig. Bilder = GUI-Ansicht (Skalierung 3, isometrisch 30/225), vierfach vergroessert; Lichtstufen wie Vanilla-Seitenlicht.</p>'
         '<p>Farben schon entschieden (Strong blau wie gebaut, Mighty lila, Evil rot, Mighty Evil dunkelrot). Wahl: <b>Form</b> und <b>Groesse</b>.</p>'
         '<h2>Inventar-Slot (48 px, 4x)</h2><table><tr><th>Conduit<th>Heute (flach; Strong = Vanilla-Special)<th>Vanilla-Conduit<th>A gross (Schale, Gui x2.0)<th>B gross (Kaefig+Kern, Gui x1.6)<th>A Vanilla-Groesse<th>B Vanilla-Groesse</tr>']
    for kid, label, c, d in rows:
        h.append(f'<tr><td>{label}')
        for k in ['heute', 'vanilla', 'Agross', 'Bgross', 'Aklein', 'Bklein']:
            h.append('<td>' + (f'<img src="{b64(c[k], 4)}">' if c[k] is not None else '<i>Vanilla-Special</i>'))
        h.append('</tr>')
    h.append('</table><h2>Detail (gross gerendert, 2x)</h2><table><tr><th>Conduit<th>A<th>B</tr>')
    for kid, label, c, d in rows:
        h.append(f'<tr><td>{label}<td class=big><img src="{b64(d["A"], 2)}"><td class=big><img src="{b64(d["B"], 2)}"></tr>')
    h.append('</table><h2>Wahl</h2><p>Form: <label><input type=radio name=f value=A checked><span>A Schale (Vanilla-Conduit umgefaerbt)</span></label> '
             '<label><input type=radio name=f value=B><span>B Kaefig + Kern (aktiver Conduit)</span></label></p>'
             '<p>Groesse: <label><input type=radio name=s value=gross checked><span>gross</span></label> <label><input type=radio name=s value=vanilla><span>Vanilla-Groesse</span></label></p>'
             '<p>Notiz: <input id=n size=50></p><code id=o></code><script>function u(){var f=document.querySelector("[name=f]:checked").value,s=document.querySelector("[name=s]:checked").value;'
             'o.textContent="COND38|form="+f+"|size="+s+"|note="+n.value}document.addEventListener("input",u);u()</script>')
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(''.join(h))
    print(OUT, OUT.stat().st_size)


main()
