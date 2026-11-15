#!/usr/bin/env python3
"""N62 (owner 15.11.): texture PROPOSALS for the Pulse Shapecutter (redstone_shapecutter) and the Auto Shapecutter (auto_shapecutter).

Own pixel art (no Mojang pixels, nothing read from the client jar): full blocks like the crafter, faces top / front / side / bottom.
Two variants per block:
  A  polished iron casing with oak-coloured corner posts - the Shapecutter's look (iron base, wooden corner posts, steel blade)
  B  grey stone casing with a darker frame - the crafter/dropper family
Motifs: top = blade slot with the steel blade's teeth; front = the output mouth (the cut comes out here); side = casing with the
machine's mark (Pulse: redstone strip with a lamp, Auto: copper cog); bottom = plate with rivets. Pulse uses redstone accents,
Auto copper accents (like Simple Machines' Auto Crafter).

    python3 tools/textures/proposals/n62/n62_saws_2026_11_15.py                 # proposal PNGs here (+ .layers.json beside them)
    python3 tools/textures/proposals/n62/n62_saws_2026_11_15.py --install A     # copy variant A into the asset folder (provisional)
    python3 tools/textures/proposals/n62/n62_saws_2026_11_15.py --preview DIR   # 8x face sheets, 3D renders, items.json

Round log (A8c): see docs/ai/PLAN-N62-FORMSAEGE-2026-11-15.md (texrate numbers per face are printed by --preview).
"""
from __future__ import annotations

import json
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
sys.path.insert(0, str(ROOT / "tools/textures"))
import layers  # noqa: E402

ASSETS = ROOT / "modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block"
BLOCKS = ("redstone_shapecutter", "auto_shapecutter")
FACES = ("top", "front", "side", "bottom")

def _ramp(lo, hi, n=9):
    return [tuple(round(a + (b - a) * i / (n - 1)) for a, b in zip(lo, hi)) for i in range(n)]


# Round 2 (A8c): finer ramps (9 tones, ~13 luminance apart) - round 1 had 7 tones ~24 apart: speckled like gravel, 21-33 % hard.
IRON = _ramp((56, 58, 65), (200, 203, 209))
STONE = _ramp((58, 58, 59), (166, 164, 160))


def tone(ramp, i):
    """Tone i on the old 0..6 scale of round 1 (keeps the motif code readable), on any ramp length."""
    return ramp[round(i * (len(ramp) - 1) / 6)]
WOOD = [(70, 50, 28), (94, 68, 38), (120, 88, 50), (146, 108, 63), (170, 128, 76)]
STEEL = [(96, 104, 112), (134, 142, 150), (176, 184, 192), (214, 220, 226), (238, 242, 246)]
RED = [(84, 4, 4), (132, 10, 6), (182, 24, 14), (226, 52, 32), (252, 104, 74)]
COPPER = [(70, 36, 24), (96, 50, 32), (120, 64, 40), (150, 82, 52), (186, 106, 66), (216, 132, 84)]
HOLE = [(18, 16, 18), (28, 26, 30), (40, 38, 42)]


def noise_base(ramp, seed, lo=3, hi=5):
    """A casing surface: 2x2 clusters of ramp tones lo..hi, never the same tone as the cluster left or above, with single-pixel
    flecks one tone up or down (no flat areas >= 6 px, A8d; one tone apart = soft, A8b)."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    grid = {}
    for by in range(8):
        for bx in range(8):
            options = [k for k in range(lo, hi + 1) if k != grid.get((bx - 1, by)) and k != grid.get((bx, by - 1))] or [lo]
            k = grid[(bx, by)] = rng.choice(options)
            for y in range(by * 2, by * 2 + 2):
                for x in range(bx * 2, bx * 2 + 2):
                    kk = k + (rng.random() < 0.35) * rng.choice((-1, 1))
                    img.putpixel((x, y), ramp[max(0, min(len(ramp) - 1, kk))] + (255,))
    return img


def hole(img, x0, y0, x1, y1, seed):
    """A dark opening: the three hole tones in a loose pattern (round 3: a one-colour opening was the largest flat area)."""
    rng = random.Random(seed)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            k = 1 if (x + y) % 3 else 0
            if rng.random() < 0.25:
                k = 2 if y > (y0 + y1) / 2 else 0
            img.putpixel((x, y), HOLE[k] + (255,))


def rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            img.putpixel((x, y), tuple(color) + (255,))


def bevel(img, x0, y0, x1, y1, ramp, light=5, dark=1):
    """Light edge top/left, dark edge bottom/right with a tone change in the corner (Vanilla edge rule)."""
    for x in range(x0, x1 + 1):
        img.putpixel((x, y0), tone(ramp, light) + (255,))
        img.putpixel((x, y1), tone(ramp, dark) + (255,))
    for y in range(y0, y1 + 1):
        img.putpixel((x0, y), tone(ramp, light - 1) + (255,))
        img.putpixel((x1, y), tone(ramp, dark + 1) + (255,))
    img.putpixel((x0, y1), tone(ramp, dark + 2) + (255,))
    img.putpixel((x1, y0), tone(ramp, dark + 2) + (255,))


def casing(variant, seed):
    """Base of every face: A iron panel, B stone panel, both with a 1 px frame."""
    ramp = IRON if variant == "A" else STONE
    img = noise_base(ramp, seed)
    layers.snap(img, "Gehaeuse")
    bevel(img, 0, 0, 15, 15, ramp, light=5 if variant == "A" else 4, dark=0)
    layers.snap(img, "Rahmen")
    return img, ramp


def posts(img, seed, columns=True):
    """Variant A: oak-coloured corner posts (side: full-height columns; top/bottom: corner caps)."""
    rng = random.Random(seed)
    if columns:
        for x0 in (0, 13):
            for y in range(16):
                for dx in range(3):
                    k = 3 if dx == 0 else 2 if dx == 1 else 1
                    k += (y % 5 == 2 and dx == 1) * -1 + (rng.random() < 0.2) * rng.choice((-1, 1))
                    img.putpixel((x0 + dx, y), WOOD[max(0, min(4, k))] + (255,))
            img.putpixel((x0 + 1, 0), WOOD[4] + (255,))
            img.putpixel((x0 + 1, 15), WOOD[1] + (255,))
        # round 3 (A8b): the iron next to a post sits in its shadow, one tone darker - no hard wood/iron seam
        for y in range(1, 15):
            for x in (3, 12):
                img.putpixel((x, y), IRON[2 + (y % 3 == 0)] + (255,))
    else:
        for cx, cy in ((0, 0), (13, 0), (0, 13), (13, 13)):
            for y in range(3):
                for x in range(3):
                    k = 3 - (x + y) // 2 + (rng.random() < 0.25) * rng.choice((-1, 1))
                    img.putpixel((cx + x, cy + y), WOOD[max(0, min(4, k))] + (255,))
    layers.snap(img, "Holzpfosten")


def top(block, variant):
    img, ramp = casing(variant, 11 if block == BLOCKS[0] else 12)
    if variant == "A":
        posts(img, 21, columns=False)
    # blade slot across the middle with the blade's teeth
    hole(img, 2, 6, 13, 9, 5)
    for x in range(2, 14):
        img.putpixel((x, 6), HOLE[0] + (255,))
        img.putpixel((x, 9), tone(ramp, 4) + (255,))
    layers.snap(img, "Schlitz")
    for x in range(3, 13):
        img.putpixel((x, 7), STEEL[3 if x % 2 else 2] + (255,))
        img.putpixel((x, 8), STEEL[1 if x % 2 else 0] + (255,))
    img.putpixel((3, 7), STEEL[4] + (255,))
    layers.snap(img, "Saegeblatt")
    accent = RED if block == BLOCKS[0] else COPPER
    if block == BLOCKS[0]:
        for x, y in ((4, 3), (11, 3), (4, 12), (11, 12)):
            img.putpixel((x, y), accent[3] + (255,))
            img.putpixel((x + 1, y), accent[2] + (255,))
            img.putpixel((x, y + 1), accent[1] + (255,))
            img.putpixel((x + 1, y + 1), accent[0] + (255,))
    else:
        for x in range(2, 14):
            img.putpixel((x, 5), accent[4 if x < 8 else 3] + (255,))
            img.putpixel((x, 10), accent[2 if x < 8 else 1] + (255,))
    layers.snap(img, "Akzent")
    return img


def front(block, variant):
    img, ramp = casing(variant, 31 if block == BLOCKS[0] else 32)
    if variant == "A":
        posts(img, 41, columns=True)
    accent = RED if block == BLOCKS[0] else COPPER
    # output mouth with a rim; Auto: copper rim, Pulse: iron/stone rim and a redstone lamp above
    rim = accent if block == BLOCKS[1] else ramp
    hole(img, 4, 6, 11, 12, 6)
    bevel(img, 4, 6, 11, 12, rim, light=4 if block == BLOCKS[1] else 5, dark=1)
    for x in range(5, 11):
        img.putpixel((x, 7), HOLE[0] + (255,))
    for x, y in ((6, 9), (9, 10), (7, 11)):
        img.putpixel((x, y), HOLE[2] + (255,))
    layers.snap(img, "Auswurf")
    if block == BLOCKS[0]:
        bevel(img, 6, 2, 9, 4, ramp, light=2, dark=0)
        img.putpixel((7, 3), RED[4] + (255,))
        img.putpixel((8, 3), RED[3] + (255,))
        img.putpixel((7, 2), RED[2] + (255,))
        img.putpixel((8, 4), RED[1] + (255,))
    else:
        for x in range(5, 11):
            img.putpixel((x, 3), COPPER[4 if x % 2 else 3] + (255,))
        img.putpixel((7, 2), COPPER[5] + (255,))
        img.putpixel((8, 4), COPPER[1] + (255,))
    layers.snap(img, "Akzent")
    return img


def side(block, variant):
    img, ramp = casing(variant, 51 if block == BLOCKS[0] else 52)
    if variant == "A":
        posts(img, 61, columns=True)
    # a horizontal groove (the blade's path) at mid height
    for x in range(3 if variant == "A" else 1, 13 if variant == "A" else 15):
        img.putpixel((x, 7), tone(ramp, 1 if x % 3 else 2) + (255,))
        img.putpixel((x, 8), tone(ramp, 5 if x % 4 else 4) + (255,))
    layers.snap(img, "Fuge")
    if block == BLOCKS[0]:
        # redstone strip with a lamp
        for y in range(3, 13):
            if y in (7, 8):
                continue
            img.putpixel((7, y), RED[2 if y < 7 else 1] + (255,))
            img.putpixel((8, y), RED[1 if y < 7 else 0] + (255,))
        bevel(img, 6, 6, 9, 9, ramp, light=2, dark=0)
        img.putpixel((7, 7), RED[4] + (255,))
        img.putpixel((8, 7), RED[3] + (255,))
        img.putpixel((7, 8), RED[3] + (255,))
        img.putpixel((8, 8), RED[2] + (255,))
    else:
        # copper cog
        cog = ["..XX..", ".XXXX.", "XXOOXX", "XXOOXX", ".XXXX.", "..XX.."]
        for r, row in enumerate(cog):
            for c, ch in enumerate(row):
                if ch == "X":
                    img.putpixel((5 + c, 5 + r), COPPER[4 - (r + c) // 3] + (255,))
                elif ch == "O":
                    img.putpixel((5 + c, 5 + r), HOLE[1] + (255,))
        for x, y in ((7, 4), (8, 11), (4, 8), (11, 7)):
            img.putpixel((x, y), COPPER[2] + (255,))
    layers.snap(img, "Akzent")
    return img


def bottom(block, variant):
    img, ramp = casing(variant, 71 if block == BLOCKS[0] else 72)
    if variant == "A":
        posts(img, 81, columns=False)
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        img.putpixel((x, y), tone(ramp, 6) + (255,))
        img.putpixel((x + 1, y + 1), tone(ramp, 1) + (255,))
        img.putpixel((x + 1, y), tone(ramp, 3) + (255,))
        img.putpixel((x, y + 1), tone(ramp, 3) + (255,))
    layers.snap(img, "Nieten")
    return img


PAINT = {"top": top, "front": front, "side": side, "bottom": bottom}


def textures():
    """{(block, variant, face): image}"""
    return {(b, v, f): PAINT[f](b, v) for b in BLOCKS for v in ("A", "B") for f in FACES}


def write_proposals(tex):
    for (b, v, f), img in tex.items():
        path = HERE / f"{b}_{v}_{f}.png"
        img.save(path)
        layers.write_beside(path, layers.from_steps(16, 16, layers.steps_of(img) or [("Grundform", img.tobytes())]))


def install(tex, variant):
    """Provisional: the chosen variant into the asset folder (with layer sources), so the game loads the blocks."""
    ASSETS.mkdir(parents=True, exist_ok=True)
    for (b, v, f), img in tex.items():
        if v == variant:
            layers.save(img, ASSETS / f"{b}_{f}.png")


# ------------------------------------------------------------------ preview

def iso(tex_top, tex_front, tex_side, s=8):
    """A 3D-ish block: top, front (left, south face) and side (right, east face) as an isometric cube, nearest-neighbour."""
    import math
    c = math.cos(math.radians(30))
    w = int(32 * s * c) + 4
    h = int(16 * s + 16 * s) + 4
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    ox, oy = w / 2, 16 * s * 0.5 + 2

    def p(x, y, z):
        return (ox + (x - z) * s * c, oy + (x + z) * s * 0.5 - y * s)

    def shade(col, f):
        return tuple(int(v * f) for v in col[:3]) + (255,)

    for u in range(16):
        for v in range(16):
            # top (y = 16): texel u along x, v along z
            col = tex_top.getpixel((u, v))
            d.polygon([p(u, 16, v), p(u + 1, 16, v), p(u + 1, 16, v + 1), p(u, 16, v + 1)], fill=shade(col, 1.0))
            # front (z = 16): u along x, v down from y 16
            col = tex_front.getpixel((u, v))
            d.polygon([p(u, 16 - v, 16), p(u + 1, 16 - v, 16), p(u + 1, 15 - v, 16), p(u, 15 - v, 16)], fill=shade(col, 0.82))
            # side (x = 16): u along -z (from front edge to back), v down
            col = tex_side.getpixel((u, v))
            z0, z1 = 16 - u, 15 - u
            d.polygon([p(16, 16 - v, z0), p(16, 16 - v, z1), p(16, 15 - v, z1), p(16, 15 - v, z0)], fill=shade(col, 0.64))
    return img


def preview(tex, out: Path):
    out.mkdir(parents=True, exist_ok=True)
    sys.path.insert(0, str(ROOT / "tools/textures"))
    import texrate  # noqa: E402
    s = 8
    files = {}
    rates = {}
    for b in BLOCKS:
        for v in ("A", "B"):
            sheet = Image.new("RGB", (16 + 4 * (16 * s + 16) + 16 * s * 2 + 40, 16 * s + 70), "#20242b")
            d = ImageDraw.Draw(sheet)
            d.text((12, 8), f"{b} Variante {v} | oben, vorne, Seite, unten | 8x ungefiltert | rechts 3D", fill="white")
            for i, f in enumerate(FACES):
                big = tex[(b, v, f)].resize((16 * s, 16 * s), Image.NEAREST)
                x = 12 + i * (16 * s + 16)
                sheet.paste(big, (x, 40), big)
                d.text((x, 44 + 16 * s), f, fill="#cbd5e1")
            cube = iso(tex[(b, v, "top")], tex[(b, v, "front")], tex[(b, v, "side")], s=6)
            sheet.paste(cube, (12 + 4 * (16 * s + 16), 30), cube)
            name = f"{b}_{v}.png"
            sheet.save(out / name)
            files[(b, v)] = name
            for f in FACES:
                tmp = HERE / f"{b}_{v}_{f}.png"
                m = texrate.metrics(str(tmp))
                light, why = texrate.rate(m)
                rates[(b, v, f)] = {"light": light, "why": why, "hard": m["hard"], "big": m["big"]}
    # side by side: both blocks in variant A and B as cubes
    pair = Image.new("RGB", (2 * 2 * 200 + 60, 260), "#20242b")
    d = ImageDraw.Draw(pair)
    d.text((12, 8), "3D: Takt-Formsaege A | Auto-Formsaege A | Takt-Formsaege B | Auto-Formsaege B", fill="white")
    for i, (v, b) in enumerate([("A", BLOCKS[0]), ("A", BLOCKS[1]), ("B", BLOCKS[0]), ("B", BLOCKS[1])]):
        cube = iso(tex[(b, v, "top")], tex[(b, v, "front")], tex[(b, v, "side")], s=6)
        pair.paste(cube, (12 + i * 205, 40), cube)
    pair.save(out / "n62_cubes.png")
    return files, rates


def items_json(files, out: Path, rates):
    def rate_text(b, v):
        parts = []
        for f in FACES:
            r = rates.get((b, v, f))
            if isinstance(r, dict):
                parts.append(f"{f}: {r.get('light', '?')}")
        return (" texrate: " + ", ".join(parts) + ".") if parts else ""

    items = [
        {"id": "N62-N", "title": "Namen der neuen Formsägen",
         "text": ("Redstone-Variante (ein Schnitt pro Signal, wie der Crafter), Vorschläge: (1) Takt-Formsäge / Pulse Shapecutter – "
                  "Empfehlung: sagt genau, was sie tut (ein Takt = ein Schnitt), passt zur Formsäge-Familie und klingt nicht nach Bauteil; "
                  "(2) Redstone-Formsäge / Redstone Shapecutter – am deutlichsten, aber lang und sagt nicht, dass sie nur pro Impuls schneidet; "
                  "(3) Schalt-Formsäge / Trigger Shapecutter. Automatische Variante: Auto-Formsäge / Auto Shapecutter (wie Auto-Crafter). "
                  "Die IDs bleiben namensneutral (simpleblocks:redstone_shapecutter, simpleblocks:auto_shapecutter); eingebaut ist vorläufig (1)."),
         "images": [{"file": "tex/n62_cubes.png", "cap": "Beide Blöcke, Variante A und B (3D)", "rec": True}],
         "choices": "1 Takt-Formsäge (Empfehlung) | 2 Redstone-Formsäge | 3 Schalt-Formsäge | eigener Name"},
        {"id": "N62-T1", "title": "Takt-Formsäge (Redstone): Textur",
         "text": ("Eigene Pixel, kein Vanilla-Pixel. A: polierte Eisenhülle mit Holz-Eckpfosten wie die Formsäge; B: Steinhülle wie Crafter/Werfer. "
                  "Oben der Sägeschlitz mit Stahlblatt und vier Redstone-Punkten, vorne der Auswurf mit Redstone-Lampe, Seite Redstone-Leiste mit "
                  "Lampe, unten Nieten. A ist vorläufig im Spiel (Vorschau, wartet auf Besitzer)." + rate_text(BLOCKS[0], "A")),
         "images": [{"file": f"tex/{files[(BLOCKS[0], 'A')]}", "cap": "Variante A – Eisen + Holzpfosten (Formsägen-Look)", "rec": True},
                    {"file": f"tex/{files[(BLOCKS[0], 'B')]}", "cap": "Variante B – Steinhülle (Crafter-Familie)", "rec": False}],
         "choices": "A | B | A mit Änderungen | B mit Änderungen"},
        {"id": "N62-T2", "title": "Auto-Formsäge: Textur",
         "text": ("Wie die Takt-Formsäge, aber mit Kupfer statt Redstone (wie der Auto-Crafter aus Simple Machines): Kupferrand um den Sägeschlitz "
                  "und den Auswurf, Kupfer-Zahnrad an der Seite. A ist vorläufig im Spiel (Vorschau, wartet auf Besitzer)." + rate_text(BLOCKS[1], "A")),
         "images": [{"file": f"tex/{files[(BLOCKS[1], 'A')]}", "cap": "Variante A – Eisen + Holzpfosten, Kupfer-Akzente", "rec": True},
                    {"file": f"tex/{files[(BLOCKS[1], 'B')]}", "cap": "Variante B – Steinhülle, Kupfer-Akzente", "rec": False}],
         "choices": "A | B | A mit Änderungen | B mit Änderungen"},
    ]
    (out / "items.json").write_text(json.dumps(items, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main():
    tex = textures()
    write_proposals(tex)
    if "--install" in sys.argv:
        install(tex, sys.argv[sys.argv.index("--install") + 1])
    if "--preview" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--preview") + 1])
        files, rates = preview(tex, out / "tex")
        items_json(files, out / "tex", rates)
    print(f"n62 saws: {len(tex)} proposal textures written")


if __name__ == "__main__":
    main()
