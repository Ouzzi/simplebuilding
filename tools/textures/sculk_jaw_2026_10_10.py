"""Sculk-Kiefer (QUEUE N24/N34/N37): Modelle, Zahn-Textur, Blockstates.

Besitzer N37: die Falle tarnt sich als Sculk. Alle Sculk-Flaechen zeigen die Vanilla-Textur ``minecraft:block/sculk``
(per Referenz, nichts Vanilla eingecheckt) mit deren eigener Leuchtanimation - das ist zugleich das ruhige "Atmen" der
Falle. Die Zaehne sind nur eine feine, dunkle Andeutung (eigene Textur ``sculk_jaw.png`` in der Sculk-Palette).

Gegen die hellen Kantenlinien (N37, Bild im Spiel): keine Teilbereiche einer gemeinsamen Textur mehr (die Kanten zogen
Nachbartexel aus dem Knochen-Feld), sondern Auto-UV aus der Lage wie bei Vanilla-Bloecken; verdeckte Flaechen
(Unterseiten auf dem Sockel, die Naht zwischen beiden Lippen) fallen weg, Flaechen am Blockrand bekommen cullface.

Bewegung (N37): vier Bilder offen -> 1/3 -> 2/3 -> zu, im Blockzustand ``motion`` (siehe SculkJawBlock).
Form zu = N34: zwei Sculk-Lippen schliessen sich von den Raendern zur flachen Kuppel, Fangzaehne greifen ineinander.

  python3 tools/textures/sculk_jaw_2026_10_10.py          # schreibt Textur + Modelle + Vorschau
  python3 tools/textures/sculk_jaw_2026_10_10.py --check
"""
import argparse
import json
import math
import os
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "mc26_3/overlay/resources/assets/simplebuilding"
PREVIEW = Path(os.environ.get("SB_PREVIEW_DIR", os.path.expanduser("~/previews") + "/n37"))
WIKI = ROOT / "wiki/assets/textures"


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


# Sculk-Palette (dunkel) fuer die Zaehne: nur die Spitzen eine Spur heller, kein Knochenweiss.
TOOTH = [rgb(h) for h in ("06141a", "0b1f27", "11303a", "1a424b", "2b5a5e")]


def rng(seed):
    st = [seed]

    def nxt():
        st[0] = (st[0] * 1664525 + 1013904223) & 0xFFFFFFFF
        return st[0] / 0x100000000
    return nxt


def texture():
    """16x16 Zahn-Textur fuer Auto-UV: Zeile v entspricht Modellhoehe 16 - v; die Zaehne reichen bis Hoehe 10."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    r = rng(17)
    for y in range(16):
        for x in range(16):
            t = 1 + (1 if r() < 0.3 else 0)
            if y <= 7:  # Hoehe 9..16: Spitzen, eine Stufe heller
                t += 1
            if y <= 6 and (x + y) % 5 == 0:
                t += 1  # feine Kante, nur bei genauem Hinsehen
            if y >= 12 and r() < 0.4:
                t -= 1
            px[x, y] = TOOTH[max(0, min(4, t))]
    return img


SCULK, TEETH = "#sculk", "#teeth"
LIPS = [(0, 2, 4, 0), (1, 4, 6, 1), (3, 6, 7, 2.5)]  # (Abstand vom Rand, y0, y1, z-Einzug) je Stufe


def jaw_elements(f):
    """Bild f in 0..1: 0 offen, 1 zu. Liste von (from, to, Textur)."""
    els = [([0, 0, 0], [16, 2, 16], SCULK)]
    if f == 0:
        els += [([0, 2, 0], [2, 4, 16], SCULK), ([14, 2, 0], [16, 4, 16], SCULK)]
        for z in (0, 4, 8, 12):
            els += [([0, 4, z], [2, 6, z + 2], TEETH), ([0.5, 6, z + 0.5], [1.5, 8, z + 1.5], TEETH)]
        for z in (2, 6, 10, 14):
            els += [([14, 4, z], [16, 6, z + 2], TEETH), ([14.5, 6, z + 0.5], [15.5, 8, z + 1.5], TEETH)]
        return els
    inner = 2 + 6 * f  # Innenkante der Lippen wandert vom Rand zur Mitte
    for out, y0, y1, zi in LIPS:
        if out < inner:
            els.append(([out, y0, zi], [inner, y1, 16 - zi], SCULK))
            els.append(([16 - inner, y0, zi], [16 - out, y1, 16 - zi], SCULK))
    if f == 1:
        for z in (1.5, 5.5, 9.5):
            els += [([5.5, 7, z], [8.5, 8.5, z + 2], TEETH), ([7, 8.5, z + 0.5], [9, 10, z + 1.5], TEETH)]
        for z in (3.5, 7.5, 11.5):
            els += [([7.5, 7, z], [10.5, 8.5, z + 2], TEETH), ([7, 8.5, z + 0.5], [9, 10, z + 1.5], TEETH)]
        return els
    top = max(y1 for out, y0, y1, zi in LIPS if out < inner)
    for z in (1.5, 5.5, 9.5):  # Zaehne reiten auf den Innenkanten der Lippen mit
        els += [([inner - 2.5, top, z], [inner, top + 1.5, z + 2], TEETH),
                ([inner - 1.5, top + 1.5, z + 0.5], [inner - 0.5, top + 3, z + 1.5], TEETH)]
    for z in (3.5, 7.5, 11.5):
        els += [([16 - inner, top, z], [18.5 - inner, top + 1.5, z + 2], TEETH),
                ([16.5 - inner, top + 1.5, z + 0.5], [17.5 - inner, top + 3, z + 1.5], TEETH)]
    return els


AXIS = {"west": (0, -1), "east": (0, 1), "down": (1, -1), "up": (1, 1), "north": (2, -1), "south": (2, 1)}


def covered(i, face, els):
    """Liegt die Flaeche ganz an einem anderen Quader an (verdeckt, z. B. Unterseite auf dem Sockel, Lippennaht)?"""
    frm, to, _ = els[i]
    ax, sign = AXIS[face]
    plane = to[ax] if sign > 0 else frm[ax]
    others = [a for a in range(3) if a != ax]
    for j, (f2, t2, _) in enumerate(els):
        if j == i:
            continue
        touches = (f2[ax] == plane) if sign > 0 else (t2[ax] == plane)
        if touches and all(f2[a] <= frm[a] and t2[a] >= to[a] for a in others):
            return True
    return False


def r4(v):
    v = round(v, 4)
    return int(v) if v == int(v) else v


def model(f):
    raw = [([r4(v) for v in a], [r4(v) for v in b], t) for a, b, t in jaw_elements(f)]
    elements = []
    for i, (frm, to, texture_ref) in enumerate(raw):
        faces = {}
        for face, (ax, sign) in AXIS.items():
            if covered(i, face, raw):
                continue
            entry = {"texture": texture_ref}
            if (sign < 0 and frm[ax] == 0) or (sign > 0 and to[ax] == 16):
                entry["cullface"] = face
            faces[face] = entry
        if faces:
            elements.append({"from": frm, "to": to, "faces": faces})
    return {"parent": "minecraft:block/block",
            "textures": {"sculk": "minecraft:block/sculk", "teeth": "simplebuilding:block/sculk_jaw",
                         "particle": "minecraft:block/sculk"},
            "elements": elements}


FRAMES = {"sculk_jaw": 0.0, "sculk_jaw_motion_1": 1 / 3, "sculk_jaw_motion_2": 2 / 3, "sculk_jaw_snapped": 1.0}


def blockstates():
    """triggered=zu, motion=Zwischenbild: Zuschnappen zaehlt 2 -> 1 -> 0 von offen her, Oeffnen von zu her."""
    pick = {(False, 0): "sculk_jaw", (True, 2): "sculk_jaw_motion_1", (True, 1): "sculk_jaw_motion_2",
            (True, 0): "sculk_jaw_snapped", (False, 2): "sculk_jaw_motion_2", (False, 1): "sculk_jaw_motion_1"}
    return {"variants": {f"motion={m},triggered={'true' if t else 'false'}": {"model": "simplebuilding:block/" + name}
                         for (t, m), name in sorted(pick.items(), key=lambda kv: (kv[0][1], kv[0][0]))}}


def files():
    out = {"textures/block/sculk_jaw.png": texture(), "blockstates/sculk_jaw.json": blockstates(),
           "items/sculk_jaw.json": {"model": {"type": "minecraft:model", "model": "simplebuilding:block/sculk_jaw"}}}
    for name, f in FRAMES.items():
        out[f"models/block/{name}.json"] = model(f)
    return out


def render(mdl, teeth, scale=10):
    """Grobe Isometrie (Sculk als Flaechenfarbe, Zaehne aus der Textur gemittelt) - nur fuer die Form."""
    W = 16 * scale * 2
    canvas = Image.new("RGB", (W, W), (46, 46, 52))
    d = ImageDraw.Draw(canvas)
    c = math.cos(math.radians(45))

    def proj(x, y, z):
        x, z = x - 8, z - 8
        return (W / 2 + (x - z) * c * scale, W * 0.62 + (x + z) * c * scale * 0.5 - y * scale * 0.87)
    tcol = teeth.convert("RGB").resize((1, 1), Image.BOX).getpixel((0, 0))
    polys = []
    for e in mdl["elements"]:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        tex_ref = next(iter(e["faces"].values()))["texture"]
        col = (13, 40, 48) if tex_ref == SCULK else tuple(min(255, v + 25) for v in tcol)
        for name, pts, sh in (("up", [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], 1.0),
                              ("south", [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], 0.78),
                              ("east", [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)], 0.62)):
            if name in e["faces"]:
                key = sum(p[0] + p[2] + p[1] * 1.2 for p in pts) / 4
                polys.append((key, pts, tuple(int(v * sh) for v in col)))
    polys.sort(key=lambda p: p[0])
    for _, pts, col in polys:
        d.polygon([proj(*p) for p in pts], fill=col, outline=(0, 0, 0))
    return canvas


def preview(f):
    tex = f["textures/block/sculk_jaw.png"]
    names = list(FRAMES)
    out = Image.new("RGB", (320 * len(names) + 140, 320), (46, 46, 52))
    for i, name in enumerate(names):
        out.paste(render(f[f"models/block/{name}.json"], tex), (320 * i, 0))
    out.paste(tex.convert("RGB").resize((128, 128), Image.NEAREST), (320 * len(names) + 6, 20))
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
    preview(f).save(PREVIEW / "jaw-frames.png")
    print("written, preview", PREVIEW / "jaw-frames.png")
    return 0


if __name__ == "__main__":
    sys.exit(main())
