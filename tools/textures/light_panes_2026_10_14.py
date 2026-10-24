"""Light Panes (QUEUE N57, docs/ai/PLAN-LIGHT-PANE-2026-10-14.md): textures, models, blockstates, item definitions, preview.

Pane = flat 16x16x1 px lamp on any block side. All art is drawn here (own pixel art, nothing from Vanilla):

* ``light_pane``             warm lamp panel (glowstone yellow), lit and dark
* ``transparent_light_pane`` clear glass frame with a faint white veil; the veil (translucent, light 15 on the pane's own cell)
                             brightens the surface of the block behind it. Dark: only a thin frame.
* ``<dye>_light_pane``       sixteen colors, frosted panel in a toned-down dye color (never flat or garish), lit and dark
* ``redstone_light_pane``    dark panel with a spectrum bar when unpowered; powered it shows the colored panels above

Models share one base model (``block/light_pane_base``, ``#panel``). The block state rotates it like a Vanilla button.

  python3 tools/textures/light_panes_2026_10_14.py          # writes everything + wiki copies + previews
  python3 tools/textures/light_panes_2026_10_14.py --check  # exit 1 when a file is stale
"""
import argparse
import json
import os
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "modules/simplelights/shared/resources/assets/simplelights"  # WB24-6: the panes are Simple Lights'
WIKI = ROOT / "wiki/assets/textures"
PREVIEW = Path(os.environ.get("SB_PREVIEW_DIR", ROOT / "docs/ai/previews/oc-lightpane"))

# Dye order = DyeColor ids (white = 0 ... black = 15); base colors are the dye diffuse colors.
DYES = [("white", (249, 255, 254)), ("orange", (249, 128, 29)), ("magenta", (199, 78, 189)), ("light_blue", (58, 179, 218)),
        ("yellow", (254, 216, 61)), ("lime", (128, 199, 31)), ("pink", (243, 139, 170)), ("gray", (71, 79, 82)),
        ("light_gray", (157, 157, 151)), ("cyan", (22, 156, 156)), ("purple", (137, 50, 184)), ("blue", (60, 68, 170)),
        ("brown", (131, 84, 50)), ("green", (94, 124, 22)), ("red", (176, 46, 38)), ("black", (29, 29, 33))]
LAMP = (232, 196, 112)  # warm glowstone-like


def clamp(v):
    return max(0, min(255, int(round(v))))


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def scale(c, f):
    return tuple(clamp(v * f) for v in c)


def rng(seed):
    st = [seed]

    def nxt():
        st[0] = (st[0] * 1664525 + 1013904223) & 0xFFFFFFFF
        return st[0] / 0x100000000
    return nxt


def soften(base, lit):
    """Tone a dye color down: 22 % toward its gray (never garish), kept inside a readable brightness band."""
    gray = sum(base) / 3
    c = mix(base, (gray, gray, gray), 0.22)
    lo, hi = (58, 214) if lit else (22, 70)
    lum = sum(c) / 3 or 1
    f = max(lo / lum, min(hi / lum, (0.86 if lit else 0.32)))
    return tuple(clamp(v * f) for v in c)


def panel(base, lit, seed):
    """Frosted lamp panel: dark outer frame, lighter inner ring, dithered glass, soft center glow, four corner studs."""
    body = soften(base, lit)
    r = rng(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 0:
                c = scale(body, 0.58 if lit else 0.7)
            elif ring == 1:
                c = scale(body, 1.12 if lit else 1.15)
            else:
                dither = 1.0 + (0.045 if (x // 2 + y // 2) % 2 else -0.03) + (r() - 0.5) * 0.06
                glow = max(0.0, 1.0 - (abs(x - 7.5) + abs(y - 7.5)) / 9.0) * (0.14 if lit else 0.05)
                c = tuple(clamp(v * dither + 255 * glow * (0.55 if lit else 0.25)) for v in body)
            px[x, y] = c + (255,)
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):  # corner studs
        px[x, y] = scale(body, 0.7 if lit else 0.8) + (255,)
    for x, y in ((6, 7), (7, 7), (8, 8), (9, 8)):  # a small cross-light streak (breaks the flat look)
        if lit:
            px[x, y] = tuple(clamp(v * 1.1 + 18) for v in body) + (255,)
    return img


def transparent(lit):
    """Clear pane: a faint white veil with a diagonal glare. Dark: only a thin frame (veil gone)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 0:
                px[x, y] = (236, 244, 248, 215 if lit else 130)
            elif lit and ring == 1:
                px[x, y] = (255, 255, 255, 96)
            elif lit:
                a = 46 + (10 if (x // 2 + y // 2) % 2 else 0)
                if x + y in (9, 10, 11) and 3 <= x <= 12:
                    a += 38  # glare
                px[x, y] = (255, 255, 255, a)
    if lit:
        for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
            px[x, y] = (255, 255, 255, 150)
    return img


def redstone_off():
    """Unpowered multicolor pane: dark slate panel, red-brown frame, a spectrum bar of all sixteen colors (dimmed)."""
    img = panel((84, 70, 74), False, 211)
    px = img.load()
    for i, (_, c) in enumerate(DYES):
        col = soften(c, False)
        col = tuple(clamp(v * 2.0) for v in col)
        px[i, 7] = col + (255,)
        px[i, 8] = col + (255,)
    for i in (0, 15):  # the bar runs frame to frame; keep the frame ring readable at the ends
        px[i, 7] = scale(px[i, 7][:3], 0.8) + (255,)
        px[i, 8] = scale(px[i, 8][:3], 0.8) + (255,)
    return img


def redstone_icon():
    """Inventory icon of the multicolor pane: the spectrum bar over a lit warm-gray panel."""
    img = panel((190, 168, 160), True, 223)
    px = img.load()
    for i, (_, c) in enumerate(DYES):
        col = soften(c, True)
        px[i, 6] = col + (255,)
        px[i, 7] = col + (255,)
        px[i, 8] = col + (255,)
    return img


def icon(img):
    """Item sprite: the panel with its four corner pixels cleared (a soft cut instead of a hard square)."""
    out = img.copy()
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        out.putpixel((x, y), (0, 0, 0, 0))
    return out


def transparent_icon():
    img = transparent(True)
    px = img.load()
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 0:
                px[x, y] = (236, 244, 248, 255)
            elif ring == 1:
                px[x, y] = (214, 230, 238, 190)
            else:
                a = px[x, y][3]
                px[x, y] = (205, 226, 236, min(255, a + 70))
    return icon(img)


def base_model():
    side = {"texture": "#panel", "uv": [0, 0, 16, 1]}
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": "#panel"},
            "elements": [{"from": [0, 0, 0], "to": [16, 1, 16], "shade": False,
                          "faces": {"up": {"texture": "#panel", "uv": [0, 0, 16, 16]},
                                    "down": {"texture": "#panel", "uv": [0, 0, 16, 16], "cullface": "down"},
                                    "north": dict(side), "south": dict(side), "east": dict(side), "west": dict(side)}}]}


def child_model(texture):
    return {"parent": "simplelights:block/light_pane_base", "textures": {"panel": "simplelights:block/" + texture}}


ROT = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "east": {"x": 90, "y": 90},
       "south": {"x": 90, "y": 180}, "west": {"x": 90, "y": 270}}


def variant(model, facing):
    return dict({"model": "simplelights:block/" + model}, **ROT[facing])


def single_state(name):
    v = {}
    for facing in ROT:
        v[f"facing={facing},lit=true"] = variant(name, facing)
        v[f"facing={facing},lit=false"] = variant(name + "_off", facing)
    return {"variants": v}


def redstone_state():
    v = {}
    for facing in ROT:
        for c in range(16):
            v[f"color={c},facing={facing}"] = variant(DYES[c][0] + "_light_pane", facing)
        v[f"color=16,facing={facing}"] = variant("black_light_pane_off", facing)  # N65: dark by default, not the spectrum bar
    return {"variants": v}


def item_base_model():
    """3D item base (B-panes21): the 1 px plate of the block model, centered in the cube and tilted so the face shows in the GUI."""
    m = base_model()
    m["elements"][0]["from"], m["elements"][0]["to"] = [0, 7.5, 0], [16, 8.5, 16]
    m["display"] = {"gui": {"rotation": [60, 225, 0], "scale": [0.8, 0.8, 0.8]},
                    "fixed": {"rotation": [90, 0, 0], "scale": [0.5, 0.5, 0.5]},
                    "ground": {"translation": [0, 3, 0], "scale": [0.4, 0.4, 0.4]},
                    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
                    "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]}}
    return m


def item_files(name, panel=None):
    """Item definition + 3D model (plate of the pane); `panel` = texture of the plate (default: the lit block texture)."""
    return {f"items/{name}.json": {"model": {"type": "minecraft:model", "model": f"simplelights:item/{name}"}},
            f"models/item/{name}.json": {"parent": "simplelights:item/light_pane_item_base",
                                         "textures": {"panel": panel or f"simplelights:block/{name}"}}}


def rainbow(src, diag=True):
    """Whole panel as a rainbow: keeps the light/dark structure (frame, bevel, noise) of `src`, hue runs across the panel."""
    import colorsys
    out = src.copy()
    sp, op = src.load(), out.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = sp[x, y]
            h = ((x + y) / 30.0) if diag else (x // 2) / 8.0
            c = colorsys.hsv_to_rgb(h * 0.92, 0.72, min(1.0, 0.25 + 0.95 * max(r, g, b) / 255.0))
            op[x, y] = tuple(clamp(v * 255) for v in c) + (a,)
    return out


def files():
    out = {"models/block/light_pane_base.json": base_model(), "models/item/light_pane_item_base.json": item_base_model()}
    names = ["light_pane", "transparent_light_pane"] + [c + "_light_pane" for c, _ in DYES]
    tex = {"light_pane": (panel(LAMP, True, 101), panel(LAMP, False, 102)),
           "transparent_light_pane": (transparent(True), transparent(False))}
    for i, (c, base) in enumerate(DYES):
        tex[c + "_light_pane"] = (panel(base, True, 300 + i), panel(base, False, 400 + i))
    for name in names:
        on, off = tex[name]
        out[f"textures/block/{name}.png"] = on
        out[f"textures/block/{name}_off.png"] = off
        out[f"models/block/{name}.json"] = child_model(name)
        out[f"models/block/{name}_off.json"] = child_model(name + "_off")
        out[f"blockstates/{name}.json"] = single_state(name)
        out[f"textures/item/{name}.png"] = transparent_icon() if name == "transparent_light_pane" else icon(on)
        out.update(item_files(name))
    out["textures/block/redstone_light_pane_off.png"] = redstone_off()
    out["models/block/redstone_light_pane_off.json"] = child_model("redstone_light_pane_off")
    out["blockstates/redstone_light_pane.json"] = redstone_state()
    out["textures/item/redstone_light_pane.png"] = rainbow(redstone_icon())  # B-panes21: all rainbow, full panel (3D plate)
    out.update(item_files("redstone_light_pane", "simplelights:item/redstone_light_pane"))
    return out


def sheet(f, scale_=8):
    """Preview: row 1 lit panels (normal, 16 colors), row 2 dark, row 3 transparent + redstone over a neutral noise 'wall'."""
    cell = 16 * scale_ + 8
    cols = 17
    W, H = cols * cell + 8, 4 * cell + 8
    img = Image.new("RGBA", (W, H), (44, 44, 50, 255))
    r = rng(5)

    def wall(w, h):
        wl = Image.new("RGBA", (w, h))
        wp = wl.load()
        for y in range(h):
            for x in range(w):
                g = 104 + int((r() - 0.5) * 22)
                wp[x, y] = (g, g, g + 4, 255)
        return wl

    def put(tex, col, row, bg=None):
        tile = tex.resize((16 * scale_, 16 * scale_), Image.NEAREST)
        x, y = 8 + col * cell, 8 + row * cell
        if bg is not None:
            img.paste(bg.resize((16 * scale_, 16 * scale_), Image.NEAREST), (x, y))
        img.alpha_composite(tile, (x, y))

    names = ["light_pane"] + [c + "_light_pane" for c, _ in DYES]
    for i, name in enumerate(names):
        put(f[f"textures/block/{name}.png"], i, 0)
        put(f[f"textures/block/{name}_off.png"], i, 1)
    bg = wall(16, 16)
    put(f["textures/block/transparent_light_pane.png"], 0, 2, bg)
    put(f["textures/block/transparent_light_pane_off.png"], 1, 2, bg)
    put(bg, 2, 2)
    put(f["textures/block/redstone_light_pane_off.png"], 3, 2)
    for i, name in enumerate(["light_pane", "transparent_light_pane", "redstone_light_pane"] + [c + "_light_pane" for c, _ in DYES[:14]]):
        put(f[f"textures/item/{name}.png"], i, 3)
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
        print("stale: " + ", ".join(stale[:8]) + (" ..." if len(stale) > 8 else "") if stale else "OK light_panes")
        return 1 if stale else 0
    for rel, data in f.items():
        if isinstance(data, Image.Image) and rel.startswith("textures/"):
            dest = WIKI / rel[len("textures/"):]
            dest.parent.mkdir(parents=True, exist_ok=True)
            data.save(dest)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    sheet(f).save(PREVIEW / "light_panes_8x.png")
    print("written, preview", PREVIEW / "light_panes_8x.png")
    return 0


if __name__ == "__main__":
    sys.exit(main())
