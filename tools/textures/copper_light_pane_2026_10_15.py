"""Copper Light Pane (QUEUE N65): own design, one fixed copper color. Textures, models, blockstate, item files, preview.

Look: riveted copper plate with a round lens in the middle (an indicator lamp) instead of the frosted panels of the other panes.
Lit: warm glowing lens with a halo on the plate. Off: dark glass with a single glint. Reuses the shared pane base model and the
helpers of light_panes_2026_10_14.py (no Vanilla pixels, everything drawn here).

  python3 tools/textures/copper_light_pane_2026_10_15.py          # writes everything + wiki copies + preview
  python3 tools/textures/copper_light_pane_2026_10_15.py --check  # exit 1 when a file is stale
"""
import argparse
import json
import os
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import light_panes_2026_10_14 as lp  # noqa: E402

ROOT = lp.ROOT
RES = lp.RES
WIKI = lp.WIKI
PREVIEW = Path(os.environ.get("SB_PREVIEW_DIR", ROOT / "docs/ai/previews/oc-lightpane2"))
NAME = "copper_light_pane"

FRAME = (104, 54, 38)
BEVEL = (222, 138, 96)
BODY = (186, 100, 66)
RIVET = (240, 176, 126)
RIVET_SHADOW = (122, 64, 44)


def plate(lit):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    r = lp.rng(515 if lit else 516)
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 0:
                c = FRAME
            elif ring == 1:
                c = BEVEL if (x + y) < 15 else lp.scale(BEVEL, 0.82)  # lit from the top left
            else:
                d = 1.0 + (r() - 0.5) * 0.07 + (0.03 if (x + y) % 5 == 0 else 0.0)
                c = lp.scale(BODY, d)
                if lit:  # halo: the plate warms up around the lens
                    dist = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                    c = lp.mix(c, (255, 190, 110), max(0.0, 0.5 - dist / 14.0))
                    c = tuple(lp.clamp(v) for v in c)
            px[x, y] = tuple(lp.clamp(v) for v in c) + (255,)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):  # rivets with a shadow pixel to the lower right
        px[x, y] = RIVET + (255,)
        px[x + 1 if x < 8 else x, y + 1 if y < 8 else y] = RIVET_SHADOW + (255,)
    for x in range(5, 11):  # two short grooves above and below the lens
        for y in (3, 12):
            px[x, y] = lp.scale(BODY, 0.72) + (255,)
    for y in range(16):
        for x in range(16):  # the round lens, radius ~4.4
            dist = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if dist > 4.6:
                continue
            if dist > 3.7:
                c = (96, 50, 34)  # lens rim
            elif lit:
                c = (255, 240, 176) if dist < 1.8 else ((255, 214, 120) if dist < 2.9 else (240, 158, 74))
            else:
                c = (52, 34, 32) if dist < 2.9 else (72, 46, 40)
            px[x, y] = c + (255,)
    if not lit:
        px[6, 6] = (136, 108, 98, 255)  # glint on the dark glass
    return img


def files():
    on, off = plate(True), plate(False)
    out = {f"textures/block/{NAME}.png": on, f"textures/block/{NAME}_off.png": off,
           f"models/block/{NAME}.json": lp.child_model(NAME), f"models/block/{NAME}_off.json": lp.child_model(NAME + "_off"),
           f"blockstates/{NAME}.json": lp.single_state(NAME), f"textures/item/{NAME}.png": lp.icon(on)}
    out.update(lp.item_files(NAME))
    return out


def sheet(f, scale_=8):
    """Preview: row 1 the Copper Light Pane (lit, off, item), row 2 the old Light Pane for comparison, over a neutral wall."""
    cell = 16 * scale_ + 8
    img = Image.new("RGBA", (4 * cell + 8, 2 * cell + 8), (44, 44, 50, 255))

    def put(tex, col, row):
        img.alpha_composite(tex.resize((16 * scale_, 16 * scale_), Image.NEAREST), (8 + col * cell, 8 + row * cell))

    put(f[f"textures/block/{NAME}.png"], 0, 0)
    put(f[f"textures/block/{NAME}_off.png"], 1, 0)
    put(f[f"textures/item/{NAME}.png"], 2, 0)
    for i, rel in enumerate(("textures/block/light_pane.png", "textures/block/light_pane_off.png", "textures/item/light_pane.png")):
        put(Image.open(RES / rel).convert("RGBA"), i, 1)
    return img


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
        print("stale: " + ", ".join(stale) if stale else "OK copper_light_pane")
        return 1 if stale else 0
    for rel, data in f.items():
        if isinstance(data, Image.Image) and rel.startswith("textures/"):
            dest = WIKI / rel[len("textures/"):]
            dest.parent.mkdir(parents=True, exist_ok=True)
            data.save(dest)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    sheet(f).save(PREVIEW / "copper_light_pane_8x.png")
    print("written, preview", PREVIEW / "copper_light_pane_8x.png")
    return 0


if __name__ == "__main__":
    sys.exit(main())
