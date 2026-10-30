"""Simple Doors (WP-17 / BS28-2): door textures built from the Vanilla door at build time, plus models, blockstates, items, loot,
recipes and recipe unlocks of the oak doors (docs/ai/KONZEPT-A-WP17-2026-10-29.md, owner choice ABN29-3).

TEXTURES (Vanilla-derived, never committed, AGENTS.md rule 7): ``derived(jar)`` rearranges the pixels of the Vanilla oak door
(``oak_door_top`` over ``oak_door_bottom``, 16 x 32) into a wider and taller door - frame, panels, windows, hinges and knob
keep their Vanilla pixels, only whole panel columns/rows repeat: widened to 2 blocks by four panels (the knob only on the last),
made 3 high by two more window rows and one more solid panel (the knob stays mid-height). The half-width door (8 px) is the
frame plus one narrowed panel. Each cell of a door is its own 16 x 16 texture ``block/oak_door_<format>_c<col>_r<row>.png``
(``col`` 0 = hinge column, ``row`` 0 = bottom); the half doors are ``block/oak_door_half<h>_r<row>.png`` with the 8 px leaf in
the left half. ``derive_wood(jar, wood)`` does the same for any wood: the other woods are prepared, not registered (the owner
approves the oak doors first).

MODELS (committed, no Mojang pixels): north-facing frame, hinge LEFT = hinge at the player's left (west for north); the closed
leaf lies on the edge opposite ``facing``; an open leaf lies along the hinge edge (a 2-block leaf runs into the next cell, the
``swing`` cell). Written to mc26_3/overlay (26.3 only).

  python3 tools/textures/doors_2026_10_29.py          # writes models, blockstates, items, loot, recipes, unlocks
  python3 tools/textures/doors_2026_10_29.py --check  # exit 1 when a file is stale
  python3 tools/textures/doors_2026_10_29.py --atlas OUT.png [--jar client.jar]   # preview of the derived atlases (not for the repo)
"""
import argparse
import io
import json
import os
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OVERLAY = ROOT / "mc26_3/overlay/resources"
ASSETS = OVERLAY / "assets/simplebuilding"
DATA = OVERLAY / "data/simplebuilding"
NS = "simplebuilding"
JAR = os.environ.get("SB_CLIENT_JAR") or os.path.expanduser("~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar")

# format id -> (cols, rows, half); the 1.5 wide doors (ABN30-5) are two columns like the 2x2 doors, the second only 8 px of leaf (NARROW)
FORMATS = {"2x2": (2, 2, False), "2x3": (2, 3, False), "1_5x2": (2, 2, False), "1_5x3": (2, 3, False), "half_2": (1, 2, True), "half_3": (1, 3, True)}
NARROW = {"1_5x2", "1_5x3"}
WOODS = ["oak"]  # registered; the prepared woods (spruce ... warped) are derived by derive_wood() only
PREPARED = ["spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped"]

# ------------------------------------------------------------------ derivation of the textures


def _vanilla(jar, name):
    from PIL import Image
    with zipfile.ZipFile(str(jar)) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")


def _door_atlas(jar, wood):
    """The Vanilla door, 16 x 32: top half over bottom half."""
    from PIL import Image
    top, bottom = _vanilla(jar, f"{wood}_door_top"), _vanilla(jar, f"{wood}_door_bottom")
    img = Image.new("RGBA", (16, 32))
    img.paste(top, (0, 0))
    img.paste(bottom, (0, 16))
    return img


# Vanilla door columns: 0-1 frame (hinge side), 2-7 and 8-13 the two panels, 14-15 frame (knob side, the knob sits in the right panel).
PANEL_A7 = [2, 3, 4, 4, 5, 6, 7]    # left panel, one interior column repeated: 7 wide
PANEL_B7 = [8, 9, 10, 10, 11, 12, 13]
COLS_WIDE = [0, 1] + PANEL_A7 + PANEL_A7 + PANEL_A7 + PANEL_B7 + [14, 15]  # 2 + 28 + 2 = 32
PANEL_A5 = [2, 3, 4, 5, 7]          # four panels of five columns: 2 + 20 + 2 = 24 (the 1.5 wide door)
PANEL_B5 = [8, 9, 10, 11, 13]
COLS_15 = [0, 1] + PANEL_A5 * 3 + PANEL_B5 + [14, 15]
COLS_HALF = [0, 1, 8, 9, 12, 13, 14, 15]                                      # frame, a narrowed panel with the knob, frame = 8
# Vanilla door rows (0-31): 0-1 top frame, 2-6 and 7-11 the two window panes, 12 divider, 13-18 solid panel with the knob,
# 19-24 and 25-30 two more solid panels, 31 bottom frame. 3 high (48): two more window rows above, one more solid panel.
ROWS_2 = list(range(32))
ROWS_3 = list(range(0, 12)) + list(range(2, 7)) + list(range(2, 7)) + list(range(12, 25)) + list(range(19, 25)) + list(range(25, 32))
assert len(COLS_WIDE) == 32 and len(COLS_15) == 24 and len(COLS_HALF) == 8 and len(ROWS_3) == 48


def _build(atlas, cols, rows):
    from PIL import Image
    out = Image.new("RGBA", (len(cols), len(rows)))
    src, dst = atlas.load(), out.load()
    for y, sy in enumerate(rows):
        for x, sx in enumerate(cols):
            dst[x, y] = src[sx, sy]
    return out


def derive_wood(jar, wood):
    """Texture path (under assets/simplebuilding/textures/) -> 16 x 16 image of every door cell of one wood."""
    from PIL import Image
    atlas = _door_atlas(jar, wood)
    out = {}
    for fmt, (cols, rows, half) in FORMATS.items():
        big = _build(atlas, COLS_HALF if half else COLS_15 if fmt in NARROW else COLS_WIDE, ROWS_3 if rows == 3 else ROWS_2)
        for r in range(rows):
            y0 = (rows - 1 - r) * 16
            for c in range(cols):
                cell = Image.new("RGBA", (16, 16))
                w = 8 if half or (fmt in NARROW and c == 1) else 16
                x0 = c * (8 if half else 16)
                cell.paste(big.crop((x0, y0, x0 + w, y0 + 16)), (0, 0))
                name = f"{wood}_door_half{rows}_r{r}" if half else f"{wood}_door_{fmt}_c{c}_r{r}"
                out[f"block/{name}.png"] = cell
    return out


def derived(jar):
    """Vanilla-derived output for tools/textures/vanilla_derived.py: texture path -> image (the registered woods)."""
    out = {}
    for wood in WOODS:
        out.update(derive_wood(jar, wood))
    return out


# ------------------------------------------------------------------ models (north frame)


def tex_name(wood, fmt, c, r):
    cols, rows, half = FORMATS[fmt]
    return f"{wood}_door_half{rows}_r{r}" if half else f"{wood}_door_{fmt}_c{c}_r{r}"


def _faces(spec):
    return {face: {"texture": "#t", "uv": uv} for face, uv in spec.items()}


def slab(frm, to, uv):
    return {"from": frm, "to": to, "faces": _faces(uv)}


F = [0, 0, 16, 16]      # a face with u running with the default direction
FL = [16, 0, 0, 16]     # flipped
EDGE_TB = [0, 0, 16, 3]  # top / bottom of a thin leaf
EDGE_SIDE = [0, 0, 3, 16]


def closed_model(wood, fmt, c, r, hinge):
    """One cell of a closed door: the slab on the south edge (north-facing frame). South face u=0 at the hinge (LEFT: west)."""
    cols, rows, half = FORMATS[fmt]
    left = hinge == "left"
    w = 8 if half or (fmt in NARROW and c == 1) else 16
    x0, x1 = (0, w) if left or w == 16 else (16 - w, 16)
    south = [0, 0, w, 16] if left else [w, 0, 0, 16]
    north = [w, 0, 0, 16] if left else [0, 0, w, 16]
    side = [0, 0, 3, 16]
    el = slab([x0, 0, 13], [x1, 16, 16], {"south": south, "north": north, "up": [0, 0, w, 3], "down": [0, 0, w, 3], "east": side, "west": side})
    tex = f"{NS}:block/{tex_name(wood, fmt, c, r)}"
    return {"textures": {"t": tex, "particle": "minecraft:block/oak_planks"}, "elements": [el]}


def open_model(wood, fmt, seg, r, hinge):
    """One cell of an open door: the leaf strip along the hinge edge (LEFT: west), segment ``seg`` of the leaf (0 at the hinge cell)."""
    cols, rows, half = FORMATS[fmt]
    left = hinge == "left"
    length = 8 if half or (fmt in NARROW and seg == 1) else 16
    x0, x1 = (0, 3) if left else (13, 16)
    z0 = 16 - length
    east = [0, 0, length, 16]
    west = [length, 0, 0, 16]
    el = slab([x0, 0, z0], [x1, 16, 16], {"east": east, "west": west, "up": [0, 0, 3, length], "down": [0, 0, 3, length],
                                          "north": [0, 0, 3, 16], "south": [0, 0, 3, 16]})
    tex = f"{NS}:block/{tex_name(wood, fmt, seg, r)}"
    return {"textures": {"t": tex, "particle": "minecraft:block/oak_planks"}, "elements": [el]}


def item_model(wood, fmt):
    """3D inventory model: the closed door, every cell shrunk to 16/rows px, hinge on the left."""
    cols, rows, half = FORMATS[fmt]
    u = 16.0 / rows
    width = (cols * u) if not half else u / 2
    if fmt in NARROW:
        width = 1.5 * u
    x_start = (16 - width) / 2
    textures = {"particle": "minecraft:block/oak_planks"}
    elements = []
    for c in range(cols):
        for r in range(rows):
            key = f"c{c}r{r}"
            textures[key] = f"{NS}:block/{tex_name(wood, fmt, c, r)}"
            narrow = half or (fmt in NARROW and c == 1)
            ux = u / 2 if narrow else u
            x0 = x_start + c * u
            # the front of the item is the south face (the side seen when you stand behind a door facing north)
            south = [0, 0, 8, 16] if narrow else [0, 0, 16, 16]
            north = [8, 0, 0, 16] if narrow else [16, 0, 0, 16]
            face = {"south": {"texture": f"#{key}", "uv": south}, "north": {"texture": f"#{key}", "uv": north},
                    "up": {"texture": f"#{key}", "uv": [0, 0, 16, 2]}, "down": {"texture": f"#{key}", "uv": [0, 0, 16, 2]},
                    "east": {"texture": f"#{key}", "uv": [0, 0, 2, 16]}, "west": {"texture": f"#{key}", "uv": [0, 0, 2, 16]}}
            elements.append({"from": [round(x0, 3), round(r * u, 3), 7], "to": [round(x0 + ux, 3), round((r + 1) * u, 3), 9], "faces": face})
    return {"parent": "minecraft:block/block", "textures": textures, "elements": elements}


def blockstate(wood, fmt):
    """Every state of the block (col 0-1, row 0-2, swing) gets a model: the block's state definition has all combinations, and
    the ones a door never reaches (MIXCHECK31-5: open with col 1, rows above the door, closed swing cells) fall back to the
    nearest real cell, so the client does not log 'Missing model for variant'."""
    cols, rows, half = FORMATS[fmt]
    rot = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
    variants = {}
    for facing, rt in rot.items():
        for hinge in ("left", "right"):
            for r in range(3):
                rr = min(r, rows - 1)
                for c in range(2):
                    cc = min(c, cols - 1)
                    for swing in ("false", "true"):
                        closed = f"{NS}:block/{wood}_door_{fmt}_closed_c{cc}_r{rr}_{hinge}"
                        variants[f"facing={facing},hinge={hinge},open=false,col={c},row={r},swing={swing}"] = {"model": closed, **rt}
                        s1 = swing == "true" and not half and c == 0 and r < rows
                        variants[f"facing={facing},hinge={hinge},open=true,col={c},row={r},swing={swing}"] = {
                            "model": f"{NS}:block/{wood}_door_{fmt}_open_s{1 if s1 else 0}_r{rr}_{hinge}", **rt}
    return {"variants": variants}


def loot(wood, fmt):
    name = f"{NS}:{wood}_door_{fmt}"
    return {"type": "minecraft:block",
            "pools": [{"condition": {"type": "minecraft:survives_explosion"},
                       "entries": [{"type": "minecraft:item",
                                    "condition": {"type": "minecraft:match_block", "blocks": name, "state": {"col": "0", "row": "0", "swing": "false"}},
                                    "name": name}],
                       "rolls": 1}],
            "random_sequence": f"{NS}:blocks/{wood}_door_{fmt}"}


def recipes(wood):
    door, planks, stick = f"minecraft:{wood}_door", f"minecraft:{wood}_planks", "minecraft:stick"
    return {
        "2x2": ({"type": "minecraft:crafting_shaped", "category": "redstone", "key": {"#": door}, "pattern": ["##", "##"],
                 "result": {"count": 1, "id": f"{NS}:{wood}_door_2x2"}}, door),
        "2x3": ({"type": "minecraft:crafting_shaped", "category": "redstone", "key": {"#": door}, "pattern": ["##", "##", "##"],
                 "result": {"count": 1, "id": f"{NS}:{wood}_door_2x3"}}, door),
        "1_5x2": ({"type": "minecraft:crafting_shapeless", "category": "redstone", "ingredients": [door, f"{NS}:{wood}_door_half_2"],
                   "result": {"count": 1, "id": f"{NS}:{wood}_door_1_5x2"}}, f"{NS}:{wood}_door_half_2"),
        "1_5x3": ({"type": "minecraft:crafting_shapeless", "category": "redstone", "ingredients": [f"{NS}:{wood}_door_1_5x2", planks],
                   "result": {"count": 1, "id": f"{NS}:{wood}_door_1_5x3"}}, f"{NS}:{wood}_door_1_5x2"),
        "half_2": ({"type": "minecraft:crafting_shaped", "category": "redstone", "key": {"#": planks, "/": stick}, "pattern": ["#", "#", "/"],
                    "result": {"count": 1, "id": f"{NS}:{wood}_door_half_2"}}, planks),
        "half_3": ({"type": "minecraft:crafting_shapeless", "category": "redstone", "ingredients": [f"{NS}:{wood}_door_half_2", planks],
                    "result": {"count": 1, "id": f"{NS}:{wood}_door_half_3"}}, f"{NS}:{wood}_door_half_2"),
    }


def unlock(recipe_id, ingredient):
    short = ingredient.split(":")[1]
    return {"parent": "minecraft:recipes/root",
            "criteria": {f"has_{short}": {"conditions": {"items": [{"items": ingredient}]}, "trigger": "minecraft:inventory_changed"},
                         "has_the_recipe": {"conditions": {"recipes": f"{NS}:{recipe_id}"}, "trigger": "minecraft:recipe_unlocked"}},
            "requirements": [["has_the_recipe", f"has_{short}"]],
            "rewards": {"recipes": [f"{NS}:{recipe_id}"]}}


def files():
    out = {}
    for wood in WOODS:
        recs = recipes(wood)
        for fmt, (cols, rows, half) in FORMATS.items():
            name = f"{wood}_door_{fmt}"
            out[ASSETS / f"blockstates/{name}.json"] = blockstate(wood, fmt)
            out[ASSETS / f"items/{name}.json"] = {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}}
            out[ASSETS / f"models/item/{name}.json"] = item_model(wood, fmt)
            for hinge in ("left", "right"):
                for r in range(rows):
                    for c in range(cols):
                        out[ASSETS / f"models/block/{name}_closed_c{c}_r{r}_{hinge}.json"] = closed_model(wood, fmt, c, r, hinge)
                    out[ASSETS / f"models/block/{name}_open_s0_r{r}_{hinge}.json"] = open_model(wood, fmt, 0, r, hinge)
                    if not half:
                        out[ASSETS / f"models/block/{name}_open_s1_r{r}_{hinge}.json"] = open_model(wood, fmt, 1, r, hinge)
            out[DATA / f"loot_table/blocks/{name}.json"] = loot(wood, fmt)
            recipe, ingredient = recs[fmt]
            out[DATA / f"recipe/{name}.json"] = recipe
            out[DATA / f"advancement/recipes/redstone/{name}.json"] = unlock(name, ingredient)
    return out


def write_atlas(path, jar):
    """Preview of the derived atlases of the registered woods plus the Vanilla door next to them (for previews, never the repo)."""
    from PIL import Image
    tiles = []
    for wood in WOODS + PREPARED[:2]:
        atlas = _door_atlas(jar, wood)
        tiles.append((f"{wood} vanilla", atlas))
        tiles.append((f"{wood} 2x2", _build(atlas, COLS_WIDE, ROWS_2)))
        tiles.append((f"{wood} 2x3", _build(atlas, COLS_WIDE, ROWS_3)))
        tiles.append((f"{wood} 1.5x2", _build(atlas, COLS_15, ROWS_2)))
        tiles.append((f"{wood} 1.5x3", _build(atlas, COLS_15, ROWS_3)))
        tiles.append((f"{wood} half 2", _build(atlas, COLS_HALF, ROWS_2)))
        tiles.append((f"{wood} half 3", _build(atlas, COLS_HALF, ROWS_3)))
    s = 6
    width = sum(t.width for _, t in tiles) * s + 12 * (len(tiles) + 1)
    height = max(t.height for _, t in tiles) * s + 24
    sheet = Image.new("RGBA", (width, height), (70, 70, 76, 255))
    x = 12
    for _, t in tiles:
        sheet.alpha_composite(t.resize((t.width * s, t.height * s), Image.NEAREST), (x, 12))
        x += t.width * s + 12
    sheet.save(path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--atlas")
    ap.add_argument("--jar", default=JAR)
    a = ap.parse_args()
    if a.atlas:
        write_atlas(a.atlas, a.jar)
        return 0
    stale = []
    data = files()
    for path, content in data.items():
        same = path.is_file() and json.loads(path.read_text()) == content
        if a.check:
            if not same:
                stale.append(path.name)
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(content, indent=2) + "\n")
    if a.check:
        print("stale: " + ", ".join(stale[:8]) if stale else f"OK doors ({len(data)} files)")
        return 1 if stale else 0
    print(f"{len(data)} files written")
    return 0


if __name__ == "__main__":
    sys.exit(main())
