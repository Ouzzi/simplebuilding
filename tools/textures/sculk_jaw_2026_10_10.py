"""Sculk-Kiefer (QUEUE N24, Branch claude-q-traps): eigene 16x16-Textur + Modelle (offen / zugeschnappt).

Texturaufbau (eine Datei, drei Felder): oben links Zahnfleisch (Sculk), oben rechts Zahn (Knochen),
unten 16x8 das Maul-Innere (Boden). Die Modelle werden hier aus Quadern erzeugt:
- offen: flacher Kiefer, kurze Zaehne in zwei versetzten Reihen an den Raendern;
- zugeschnappt: die Zaehne wachsen als gebogene Klingen nach innen und greifen mittig ineinander.

  python3 tools/textures/sculk_jaw_2026_10_10.py          # schreibt Textur + Modelle + Vorschau
  python3 tools/textures/sculk_jaw_2026_10_10.py --check
"""
import argparse
import json
import os
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "mc26_3/overlay/resources/assets/simplebuilding"
PREVIEW = Path(os.environ.get("SB_PREVIEW_DIR", os.path.expanduser("~/previews") + "/traps"))
WIKI = ROOT / "wiki/assets/textures"


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


GUM = [rgb(h) for h in ("07161c", "0d252d", "133741", "1b4a55", "2a6a72")]
GLOW = [rgb(h) for h in ("2fa7b8", "5fe0dc", "a8fff0")]
BONE = [rgb(h) for h in ("6d6650", "9b9378", "c9c1a2", "e6dfc3", "f6f1da")]


def rng(seed):
    st = [seed]

    def nxt():
        st[0] = (st[0] * 1664525 + 1013904223) & 0xFFFFFFFF
        return st[0] / 0x100000000
    return nxt


def texture():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    r = rng(7)
    # Zahnfleisch 0..7 x 0..7: dunkel mit helleren Adern und wenigen leuchtenden Punkten.
    for y in range(8):
        for x in range(8):
            t = 1 + (1 if (x + 2 * y) % 5 == 0 else 0) + (1 if r() < 0.18 else 0)
            px[x, y] = GUM[min(t, 3)]
    for x, y in ((2, 1), (6, 4), (3, 6)):
        px[x, y] = GLOW[0]
    # Zahn 8..15 x 0..7: Knochen, links hell, rechts schattig, Spitze oben heller, Kerbe in der Mitte.
    for y in range(8):
        for x in range(8):
            tone = 3 if x < 3 else (2 if x < 6 else 1)
            if y < 2:
                tone = min(tone + 1, 4)
            if x == 4 and 3 <= y <= 5:
                tone = 0
            px[8 + x, y] = BONE[tone]
    # Boden 0..15 x 8..15: Zahnfleisch-Wulst quer, Zungenrille in der Mitte, Leuchtpunkte.
    for y in range(8):
        for x in range(16):
            t = 0 if y in (0, 7) else 1
            if abs(x - 7.5) <= 1.5:
                t = 2 if y % 2 == 0 else 3
            if (x * 3 + y * 5) % 11 == 0:
                t = min(t + 1, 4)
            px[x, 8 + y] = GUM[t]
    for x, y in ((3, 12), (12, 10), (5, 14), (10, 14)):
        px[x, y] = GLOW[1]
    px[12, 10] = GLOW[2]
    return img


GUM_UV, TOOTH_UV, FLOOR_UV = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 16, 16]


def box(frm, to, uv):
    face = {"uv": uv, "texture": "#0"}
    return {"from": frm, "to": to, "faces": {d: dict(face) for d in ("north", "south", "east", "west", "up", "down")}}


def jaw(snapped):
    els = [box([0, 0, 0], [16, 2, 16], GUM_UV)]
    els[0]["faces"]["up"] = {"uv": FLOOR_UV, "texture": "#0"}
    els.append(box([0, 2, 0], [2, 4, 16], GUM_UV))
    els.append(box([14, 2, 0], [16, 4, 16], GUM_UV))
    if not snapped:
        for z in (0, 4, 8, 12):  # linke Reihe: kurze Zaehne
            els.append(box([0, 4, z], [2, 6, z + 2], TOOTH_UV))
            els.append(box([0.5, 6, z + 0.5], [1.5, 8, z + 1.5], TOOTH_UV))
        for z in (2, 6, 10, 14):  # rechte Reihe, um eine Zahnbreite versetzt
            els.append(box([14, 4, z], [16, 6, z + 2], TOOTH_UV))
            els.append(box([14.5, 6, z + 0.5], [15.5, 8, z + 1.5], TOOTH_UV))
    else:
        for z in (0, 4, 8, 12):  # linke Klingen wachsen schraeg nach innen
            els.append(box([0, 4, z], [2, 7, z + 2], TOOTH_UV))
            els.append(box([2, 6, z], [4, 9, z + 2], TOOTH_UV))
            els.append(box([4, 8, z + 0.5], [6, 11, z + 1.5], TOOTH_UV))
            els.append(box([6, 10, z + 0.5], [7.5, 12, z + 1.5], TOOTH_UV))
        for z in (2, 6, 10, 14):  # rechte Klingen, versetzt, greifen dazwischen
            els.append(box([14, 4, z], [16, 7, z + 2], TOOTH_UV))
            els.append(box([12, 6, z], [14, 9, z + 2], TOOTH_UV))
            els.append(box([10, 8, z + 0.5], [12, 11, z + 1.5], TOOTH_UV))
            els.append(box([8.5, 10, z + 0.5], [10, 12, z + 1.5], TOOTH_UV))
    return {"parent": "minecraft:block/block", "textures": {"0": "simplebuilding:block/sculk_jaw",
            "particle": "simplebuilding:block/sculk_jaw"}, "elements": els}


def files():
    out = {
        "textures/block/sculk_jaw.png": texture(),
        "models/block/sculk_jaw.json": jaw(False),
        "models/block/sculk_jaw_snapped.json": jaw(True),
        "blockstates/sculk_jaw.json": {"variants": {
            "triggered=false": {"model": "simplebuilding:block/sculk_jaw"},
            "triggered=true": {"model": "simplebuilding:block/sculk_jaw_snapped"}}},
        "items/sculk_jaw.json": {"model": {"type": "minecraft:model", "model": "simplebuilding:block/sculk_jaw"}},
    }
    return out


def render(model, img, scale=14):
    """Grober Isometrie-Vorschauer: Quader mit der echten Textur (Naechster-Nachbar), Maler-Reihenfolge."""
    import math
    W = 16 * scale * 2
    canvas = Image.new("RGB", (W, W), (46, 46, 52))
    d = ImageDraw.Draw(canvas)
    c, s = math.cos(math.radians(45)), math.sin(math.radians(45))

    def proj(x, y, z):
        x, z = x - 8, z - 8
        rx, rz = x * c - z * s, x * s + z * c
        return (W / 2 + rx * scale, W * 0.62 + rz * scale * 0.5 - y * scale * 0.87)

    def avg(uv):
        reg = img.crop(tuple(uv)).convert("RGB").resize((1, 1), Image.BOX)
        return reg.getpixel((0, 0))
    polys = []
    for e in model["elements"]:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        col = avg(e["faces"]["north"]["uv"])
        faces = {"up": ([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], 1.0),
                 "south": ([(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], 0.78),
                 "east": ([(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)], 0.62)}
        for name, (pts, sh) in faces.items():
            if name == "up":
                col2 = avg(e["faces"]["up"]["uv"])
            else:
                col2 = col
            polys.append(((x0 + x1 + z0 + z1) / 2 * -0 + (y0 + y1) / 2 + (x0 + x1) / 2 + (z0 + z1) / 2 * 1.0, pts,
                          tuple(int(v * sh) for v in col2)))
    # hinten (kleines x+z) zuerst
    polys.sort(key=lambda p: (sum(q[0] + q[2] for q in p[1]) / 4, sum(q[1] for q in p[1]) / 4))
    for _, pts, col in polys:
        d.polygon([proj(*p) for p in pts], fill=col, outline=(0, 0, 0))
    return canvas


def preview(f):
    tex = f["textures/block/sculk_jaw.png"]
    out = Image.new("RGB", (16 * 14 * 2 * 2 + 16 * 8 + 30, 16 * 14 * 2), (46, 46, 52))
    out.paste(render(f["models/block/sculk_jaw.json"], tex), (0, 0))
    out.paste(render(f["models/block/sculk_jaw_snapped.json"], tex), (16 * 14 * 2, 0))
    out.paste(tex.convert("RGB").resize((16 * 8, 16 * 8), Image.NEAREST), (16 * 14 * 4 + 15, 20))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    a = ap.parse_args()
    f = files()
    stale = []
    for rel, data in f.items():
        p = RES / rel
        if isinstance(data, Image.Image):
            same = p.is_file() and Image.open(p).convert("RGBA").tobytes() == data.tobytes()
        else:
            same = p.is_file() and json.loads(p.read_text()) == data
        if a.check:
            if not same:
                stale.append(rel)
            continue
        p.parent.mkdir(parents=True, exist_ok=True)
        if isinstance(data, Image.Image):
            data.save(p)
        else:
            p.write_text(json.dumps(data, indent=2) + "\n")
    if a.check:
        print("stale: " + ", ".join(stale) if stale else "OK sculk_jaw")
        return 1 if stale else 0
    (WIKI / "block").mkdir(parents=True, exist_ok=True)
    f["textures/block/sculk_jaw.png"].save(WIKI / "block/sculk_jaw.png")
    PREVIEW.mkdir(parents=True, exist_ok=True)
    preview(f).save(PREVIEW / "sculk_jaw.png")
    print("written, preview", PREVIEW / "sculk_jaw.png")
    return 0


if __name__ == "__main__":
    sys.exit(main())
