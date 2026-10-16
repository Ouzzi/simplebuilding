"""Copper Light Panes (QUEUE N68): the colors of the plain Light Panes with the copper switch. Textures, models, blockstates, items, preview.

Look: a frame in the pane's own color (darker outline, lighter bevel) around a weak checkerboard of the pane's main color at 100 % and 90 % saturation
(2 px cells) with a little noise; the full dye color is used, not toned down like the plain panes. Lit = full color, off = the same
pattern dark. Ids: copper_light_pane (the warm lamp color of light_pane), <dye>_copper_light_pane (16), copper_redstone_light_pane
(multicolor; its models are the sixteen colors, off = black_copper_light_pane_off). Reuses the base model and helpers of
light_panes_2026_10_14.py. Everything is drawn here (no Vanilla pixels). Replaces copper_light_pane_2026_10_15.py.

  python3 tools/textures/copper_light_panes_2026_10_16.py          # writes everything + wiki copies + preview
  python3 tools/textures/copper_light_panes_2026_10_16.py --check  # exit 1 when a file is stale
"""
import argparse
import colorsys
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
PREVIEW = Path(os.environ.get("SB_PREVIEW_DIR", ROOT / "docs/ai/previews/claude-copperrim"))

FRAME_K = 0.5   # outline = main color x 0.5 (darker step)
BEVEL_K = 1.3   # bevel = main color x 1.3 (lighter step), lit from the top left


def second(c):
    """The 90 % tone of a main color: saturation x 0.9 (greys have none to lose: value x 0.93 instead)."""
    h, s, v = colorsys.rgb_to_hsv(*(x / 255.0 for x in c))
    if s < 0.12:
        return lp.scale(c, 0.93)
    r, g, b = colorsys.hsv_to_rgb(h, s * 0.9, v)
    return (lp.clamp(r * 255), lp.clamp(g * 255), lp.clamp(b * 255))


def pane(base, lit, seed):
    k = 1.0 if lit else 0.3
    hi, lo = lp.scale(base, k), lp.scale(second(base), k)
    r = lp.rng(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 0:
                c = lp.scale(base, FRAME_K * (1.0 if lit else 0.7))
            elif ring == 1:
                c = lp.scale(base, BEVEL_K * (1.0 if (x + y) < 15 else 0.82) * (1.0 if lit else 0.55))
            else:
                c = lo if ((x - 2) // 2 + (y - 2) // 2) % 2 else hi
                c = lp.scale(c, 1.0 + (r() - 0.5) * 0.05)  # noise +-2.5 %
            px[x, y] = tuple(lp.clamp(v) for v in c) + (255,)
    return img


def spectrum_icon():
    img = pane((190, 168, 160), True, 650)
    px = img.load()
    for i, (_, c) in enumerate(lp.DYES):
        for y in (6, 7, 8):
            px[i, y] = tuple(lp.clamp(v) for v in c) + (255,) if 1 <= i <= 14 else px[i, y]
    return img


def redstone_state():
    v = {}
    for facing in lp.ROT:
        for c, (dye, _) in enumerate(lp.DYES):
            v[f"color={c},facing={facing},lit=true"] = lp.variant(dye + "_copper_light_pane", facing)
            v[f"color={c},facing={facing},lit=false"] = lp.variant("black_copper_light_pane_off", facing)
    return {"variants": v}


def names():
    return ["copper_light_pane"] + [c + "_copper_light_pane" for c, _ in lp.DYES]


def files():
    bases = [lp.LAMP] + [b for _, b in lp.DYES]
    out = {}
    for i, (name, base) in enumerate(zip(names(), bases)):
        on, off = pane(base, True, 700 + i), pane(base, False, 800 + i)
        out[f"textures/block/{name}.png"] = on
        out[f"textures/block/{name}_off.png"] = off
        out[f"models/block/{name}.json"] = lp.child_model(name)
        out[f"models/block/{name}_off.json"] = lp.child_model(name + "_off")
        out[f"blockstates/{name}.json"] = lp.single_state(name)
        out[f"textures/item/{name}.png"] = lp.icon(on)
        out.update(lp.item_files(name))
    out["blockstates/copper_redstone_light_pane.json"] = redstone_state()
    out["textures/item/copper_redstone_light_pane.png"] = lp.icon(spectrum_icon())
    out.update(lp.item_files("copper_redstone_light_pane"))
    return out


def sheet(f, scale_=8):
    """Preview: row 1 lit copper panes (warm + 16 colors), row 2 off, row 3 the plain panes lit for comparison, row 4 redstone icon."""
    cell = 16 * scale_ + 8
    img = Image.new("RGBA", (17 * cell + 8, 4 * cell + 8), (44, 44, 50, 255))

    def put(tex, col, row):
        img.alpha_composite(tex.resize((16 * scale_, 16 * scale_), Image.NEAREST), (8 + col * cell, 8 + row * cell))

    for col, name in enumerate(names()):
        put(f[f"textures/block/{name}.png"], col, 0)
        put(f[f"textures/block/{name}_off.png"], col, 1)
        plain = name.replace("_copper", "") if name != "copper_light_pane" else "light_pane"
        put(Image.open(RES / f"textures/block/{plain}.png").convert("RGBA"), col, 2)
    put(f["textures/item/copper_redstone_light_pane.png"], 0, 3)
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
        print("stale: " + ", ".join(stale) if stale else "OK copper_light_panes")
        return 1 if stale else 0
    for rel, data in f.items():
        if isinstance(data, Image.Image) and rel.startswith("textures/"):
            dest = WIKI / rel[len("textures/"):]
            dest.parent.mkdir(parents=True, exist_ok=True)
            data.save(dest)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    sheet(f).save(PREVIEW / "copper_light_panes_8x.png")
    print("written, preview", PREVIEW / "copper_light_panes_8x.png")
    return 0


if __name__ == "__main__":
    sys.exit(main())
