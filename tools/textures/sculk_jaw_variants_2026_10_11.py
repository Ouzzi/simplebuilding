"""Sculk-Kiefer, geschlossene Form: Vorschlaege A-D (Besitzer N34-Nachfrage, Branch claude-jaw2). Nur Vorschau, baut nichts ein.

Zeilen: aktuelle Form (N34, in claude-wave1), dann A-D. Spalten: Isometrie mit echter Textur, Draufsicht 8x, Textur 8x.
  A Shrieker-Kiefer: zwei Knochenplatten wie der Rand des Sculk-Shriekers klappen zu, in der Naht leuchtet Sculk.
  B Baerenfalle: zwei senkrechte Sculk-Backen treffen sich mittig, oben Saegezaehne im Zickzack.
  C Sculk-Knospe: runde, gestufte Kuppel mit leuchtenden Adern, Fangzaehne lugen nur seitlich aus der Naht.
  D Rippen: niedriger Sculk-Huegel, Knochenrippen von beiden Seiten greifen darueber ineinander.

  python3 tools/textures/sculk_jaw_variants_2026_10_11.py   # schreibt ~/previews/n34/jaw-vorschlaege.png
"""
import importlib.util
import math
import os
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
OUT = Path(os.environ.get("SB_PREVIEW_DIR", os.path.expanduser("~/previews") + "/n34")) / "jaw-vorschlaege.png"
S = 8  # Pixel je Texel

_spec = importlib.util.spec_from_file_location("jaw_base", HERE / "sculk_jaw_2026_10_10.py")
base = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(base)


def c(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


# Vanilla-naher Sculk: fast schwarzes Petrol, Shrieker-Knochen, Cyan-Leuchten.
SCULK = [c(h) for h in ("050f14", "0a1c24", "0f2a33", "153a44", "1e4f58")]
GLOW = [c(h) for h in ("1d8f99", "2fc6c9", "6ff0e4", "c8fff6")]
BONE = [c(h) for h in ("6f6852", "958d70", "b9b192", "d6cfb0", "ece6cc")]
GUM, TOOTH, FLOOR = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 16, 16]


def rng(seed):
    st = [seed]

    def nxt():
        st[0] = (st[0] * 1664525 + 1013904223) & 0xFFFFFFFF
        return st[0] / 0x100000000
    return nxt


def tex(seed, veins, bone_style, glow_dots):
    """16x16: Sculk-Feld (oben links), Knochen (oben rechts), Oberseite/Boden (unten)."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    r = rng(seed)
    for y in range(8):
        for x in range(8):
            t = 1 + (1 if r() < 0.35 else 0) + (1 if (x * 2 + y * 3) % 7 == 0 else 0)
            px[x, y] = SCULK[min(t, 3)]
    for x, y in glow_dots[:3]:
        px[x % 8, y % 8] = GLOW[1]
    for y in range(8):
        for x in range(8):
            if bone_style == "shrieker":  # Rillen quer wie am Shrieker-Rand
                tone = 3 if y % 3 else 1
                tone = tone + (1 if x < 2 else 0) - (1 if x > 5 else 0)
            elif bone_style == "fang":  # Spitze oben hell, Wurzel dunkel
                tone = 4 - y // 2 - (1 if x in (0, 7) else 0)
            else:  # Rippe: Mittelgrat
                tone = 3 if 2 <= x <= 5 else 1
                tone += 1 if x in (3, 4) and y % 2 == 0 else 0
            px[8 + x, y] = BONE[max(0, min(4, tone))]
    for y in range(8):
        for x in range(16):
            t = 1 + (1 if r() < 0.3 else 0)
            px[x, 8 + y] = SCULK[t]
    for path in veins:  # Adern: dunkler Rand, leuchtende Mitte
        for i, (x, y) in enumerate(path):
            px[x, 8 + y] = GLOW[0] if i in (0, len(path) - 1) else GLOW[1]
    for x, y in glow_dots[3:]:
        px[x, 8 + y] = GLOW[3]
    return img


def box(frm, to, uv, up=None):
    f = {"uv": uv, "texture": "#0"}
    faces = {d: dict(f) for d in ("north", "south", "east", "west", "down")}
    faces["up"] = {"uv": up or uv, "texture": "#0"}
    return {"from": frm, "to": to, "faces": faces}


def mirror(e):
    (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
    m = dict(e)
    m["from"], m["to"] = [16 - x1, y0, z0], [16 - x0, y1, z1]
    return m


def var_a():  # Shrieker-Kiefer
    els = [box([0, 0, 0], [16, 3, 16], GUM, FLOOR)]
    left = [box([0.5, 3, 0.5], [8, 5, 15.5], TOOTH), box([1.5, 5, 1.5], [7.5, 6.5, 14.5], TOOTH)]
    els += left + [mirror(e) for e in left]
    els.append(box([7.5, 3, 2], [8.5, 6, 14], GUM, FLOOR))  # leuchtende Naht
    for z in (2.5, 6.5, 10.5):
        els.append(box([6.5, 6.5, z], [8, 8, z + 1], TOOTH))
        els.append(box([8, 6.5, z + 2], [9.5, 8, z + 3], TOOTH))
    return els


def var_b():  # Baerenfalle
    els = [box([0, 0, 0], [16, 2, 16], GUM, FLOOR)]
    left = [box([2, 2, 1], [5, 4, 15], GUM), box([3.5, 4, 1], [5.5, 6, 15], GUM), box([5, 2, 1], [8, 8, 15], GUM)]
    els += left + [mirror(e) for e in left]
    for i, z in enumerate(range(1, 15, 2)):  # Zickzack: abwechselnd links/rechts hoch
        x0 = 6.5 if i % 2 == 0 else 8
        els.append(box([x0, 8, z], [x0 + 1.5, 9, z + 2], TOOTH))
        els.append(box([x0 + 0.25, 9, z + 0.5], [x0 + 1.25, 10.5, z + 1.5], TOOTH))
    return els


def var_c():  # Sculk-Knospe
    els = [box([0, 0, 0], [16, 2, 16], GUM, FLOOR),
           box([1, 2, 1], [15, 4, 15], GUM, FLOOR),
           box([2, 4, 2], [14, 6, 14], GUM, FLOOR),
           box([3.5, 6, 3.5], [12.5, 7.5, 12.5], GUM, FLOOR),
           box([5, 7.5, 5], [11, 8.5, 11], GUM, FLOOR),
           box([7.75, 8.5, 5.5], [8.25, 8.75, 10.5], TOOTH)]
    for y0, z in ((2.5, 0.5), (4.5, 1.5), (2.5, 15.0), (4.5, 14.0)):  # Fangspitzen vorn/hinten in der Naht
        els.append(box([7.25, y0, z], [8.75, y0 + 1.5, z + 0.5], TOOTH))
    return els


def var_d():  # Rippen
    els = [box([0, 0, 0], [16, 2, 16], GUM, FLOOR), box([1, 2, 1], [15, 4, 15], GUM, FLOOR)]
    for z in (1.5, 5.5, 9.5, 13.0):
        rib = [box([0.5, 2, z], [2.5, 6, z + 1.5], TOOTH), box([2.5, 5.5, z], [6, 7.5, z + 1.5], TOOTH),
               box([6, 7, z], [10, 8.5, z + 1.5], TOOTH)]
        els += rib if z != 13.0 else []
    for z in (3.5, 7.5, 11.5):
        rib = [box([0.5, 2, z], [2.5, 6, z + 1.5], TOOTH), box([2.5, 5.5, z], [6, 7.5, z + 1.5], TOOTH),
               box([6, 7, z], [10, 8.5, z + 1.5], TOOTH)]
        els += [mirror(e) for e in rib]
    return els


def face_cells(e, img):
    """Zerlegt die sichtbaren Flaechen (oben, Sueden, Osten) in Halbtexel-Zellen mit Texturfarbe."""
    (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
    px = img.load()
    out = []

    def tc(uv, fu, fv):  # fu, fv in 0..1
        u = uv[0] + (uv[2] - uv[0]) * fu
        v = uv[1] + (uv[3] - uv[1]) * fv
        return px[min(15, max(0, int(u))), min(15, max(0, int(v)))]

    def grid(a0, a1, b0, b1):
        na, nb = max(1, int(round((a1 - a0) * 2))), max(1, int(round((b1 - b0) * 2)))
        for i in range(na):
            for j in range(nb):
                yield a0 + (a1 - a0) * i / na, a0 + (a1 - a0) * (i + 1) / na, b0 + (b1 - b0) * j / nb, b0 + (b1 - b0) * (j + 1) / nb
    uv = e["faces"]["up"]["uv"]
    for a, b, p, q in grid(x0, x1, z0, z1):
        col = tc(uv, ((a + b) / 2 - x0) / (x1 - x0), ((p + q) / 2 - z0) / (z1 - z0))
        out.append(("up", [(a, y1, p), (b, y1, p), (b, y1, q), (a, y1, q)], col, 1.0))
    uv = e["faces"]["south"]["uv"]
    for a, b, p, q in grid(x0, x1, y0, y1):
        col = tc(uv, ((a + b) / 2 - x0) / (x1 - x0), 1 - ((p + q) / 2 - y0) / (y1 - y0))
        out.append(("south", [(a, p, z1), (b, p, z1), (b, q, z1), (a, q, z1)], col, 0.8))
    uv = e["faces"]["east"]["uv"]
    for a, b, p, q in grid(z0, z1, y0, y1):
        col = tc(uv, 1 - ((a + b) / 2 - z0) / (z1 - z0), 1 - ((p + q) / 2 - y0) / (y1 - y0))
        out.append(("east", [(x1, p, a), (x1, p, b), (x1, q, b), (x1, q, a)], col, 0.6))
    return out


def iso(els, img, scale=S):
    W, H = 30 * scale, 24 * scale
    can = Image.new("RGB", (W, H), (46, 46, 52))
    d = ImageDraw.Draw(can)
    cs = math.cos(math.radians(45))

    def proj(x, y, z):
        x, z = x - 8, z - 8
        rx, rz = (x - z) * cs, (x + z) * cs
        return (W / 2 + rx * scale, H * 0.62 + rz * scale * 0.5 - y * scale * 0.85)
    cells = []
    for e in els:
        for name, pts, col, sh in face_cells(e, img):
            cx = sum(p[0] for p in pts) / 4
            cy = sum(p[1] for p in pts) / 4
            cz = sum(p[2] for p in pts) / 4
            cells.append((cx + cz + cy * 1.2, pts, tuple(int(v * sh) for v in col[:3])))
    cells.sort(key=lambda t: t[0])
    for _, pts, col in cells:
        d.polygon([proj(*p) for p in pts], fill=col)
    return can


def top(els, img):
    can = Image.new("RGB", (16 * S, 16 * S), (46, 46, 52))
    d = ImageDraw.Draw(can)
    cells = []
    for e in els:
        for name, pts, col, sh in face_cells(e, img):
            if name == "up":
                h = pts[0][1]
                cells.append((h, pts, tuple(int(v * (0.55 + 0.45 * h / 10)) for v in col[:3])))
    cells.sort(key=lambda t: t[0])
    for _, pts, col in cells:
        d.rectangle([pts[0][0] * S, pts[0][2] * S, pts[2][0] * S - 1, pts[2][2] * S - 1], fill=col)
    return can


def main():
    veins_a = [[(7, 0), (7, 1), (8, 2), (8, 3), (8, 4), (7, 5), (7, 6), (8, 7)]]
    veins_c = [[(2, 1), (3, 2), (4, 2), (5, 3), (6, 3), (7, 4), (8, 4)], [(13, 0), (12, 1), (11, 2), (10, 3), (9, 4), (8, 4)],
               [(8, 4), (8, 5), (9, 6), (10, 7)], [(1, 6), (2, 6), (3, 5), (4, 5)]]
    veins_d = [[(0, 3), (2, 3), (3, 4), (5, 4), (6, 3), (9, 3), (10, 4), (12, 4), (13, 3), (15, 3)]]
    old_tex = base.texture()
    rows = [
        ("aktuell (N34)", base.jaw(True)["elements"], old_tex),
        ("A Shrieker-Kiefer", var_a(), tex(11, veins_a, "shrieker", [(2, 2), (5, 5), (6, 1), (7, 3), (8, 6)])),
        ("B Baerenfalle", var_b(), tex(23, [], "fang", [(1, 1), (6, 3), (3, 6), (3, 2), (12, 5), (7, 6)])),
        ("C Sculk-Knospe", var_c(), tex(37, veins_c, "fang", [(2, 5), (6, 2), (5, 6), (8, 4), (4, 1), (12, 6)])),
        ("D Rippen", var_d(), tex(41, veins_d, "rib", [(1, 3), (6, 6), (4, 1), (4, 6), (11, 1), (14, 6)])),
    ]
    iw, ih = 30 * S, 24 * S
    W = iw + 16 * S + 16 * S + 60
    out = Image.new("RGB", (W, len(rows) * (ih + 10)), (30, 30, 34))
    d = ImageDraw.Draw(out)
    for i, (name, els, img) in enumerate(rows):
        y = i * (ih + 10)
        out.paste(iso(els, img), (0, y))
        out.paste(top(els, img), (iw + 20, y + 30))
        out.paste(img.convert("RGB").resize((16 * S, 16 * S), Image.NEAREST), (iw + 16 * S + 40, y + 30))
        d.text((8, y + 6), name, fill=(235, 235, 235))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    out.save(OUT)
    print("preview", OUT)


if __name__ == "__main__":
    main()
