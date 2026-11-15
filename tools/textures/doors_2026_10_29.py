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

N57-1 (b-ig57g, owner 14.11.): every door variant gets the four big formats (2x2, 2x3, 1.5x2, 1.5x3; the half doors stay oak
only): the Vanilla woods, iron and the four copper stages (textures Vanilla-derived at build time like oak), and the doors of
Simple Ores, Simple End and Simple Enchanting (textures rearranged from their committed door textures, committed here with
layer sources). To stay small, every model's elements live once in ``block/door_geo/<hash>`` (shared by all variants with the
same see-through pattern) and each variant only has child models that set the texture; blockstates use partial variant keys.

  python3 tools/textures/doors_2026_10_29.py          # writes models, blockstates, items, loot, recipes, unlocks, mod textures
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

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import door_voxel  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OVERLAY = ROOT / "mc26_3/overlay/resources"
ASSETS = OVERLAY / "assets/simplebuilding"
DATA = OVERLAY / "data/simplebuilding"
NS = "simplebuilding"
JAR = os.environ.get("SB_CLIENT_JAR") or os.path.expanduser("~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar")

# format id -> (cols, rows, half); the 1.5 wide doors (ABN30-5) are two columns like the 2x2 doors, the second only 8 px of leaf (NARROW)
FORMATS = {"2x2": (2, 2, False), "2x3": (2, 3, False), "1_5x2": (2, 2, False), "1_5x3": (2, 3, False), "half_2": (1, 2, True), "half_3": (1, 3, True)}
NARROW = {"1_5x2", "1_5x3"}
WIDE = ["2x2", "2x3", "1_5x2", "1_5x3"]


class Variant:
    """One door variant: ``id`` (block id prefix), the base door and material items, textures from the Vanilla jar or a module,
    the mod that must be loaded for its recipes (None: always), whether it is wood (axe, stick in the 1.5 recipe), names."""

    def __init__(self, id, door, material, en, de, mod=None, tex=None, wood=True, formats=WIDE, alt_door=None, particle=None):
        self.id, self.door, self.material, self.en, self.de, self.mod = id, door, material, en, de, mod
        self.tex = tex  # None: Vanilla (minecraft:block/<id>_door_top/bottom from the jar); else the module texture folder
        self.wood, self.formats, self.alt_door = wood, formats, alt_door
        self.particle = particle or (f"minecraft:block/{id}_planks" if tex is None and wood else
                                     f"{door.split(':')[0]}:block/{id}_door_bottom")

    @property
    def vanilla(self):
        return self.tex is None


def _vw(id, en, de, planks=None):
    return Variant(id, f"minecraft:{id}_door", planks or f"minecraft:{id}_planks", en, de)


def _copper(stage, en, de):
    pre = f"{stage}_" if stage else ""
    return Variant(f"{pre}copper", f"minecraft:{pre}copper_door", "minecraft:copper_ingot", en, de, wood=False,
                   alt_door=f"minecraft:waxed_{pre}copper_door")


_ORES = "modules/simpleores/shared/resources/assets/simpleores/textures/block"
_END = "modules/simpleend/shared/resources/assets/simpleend/textures/block"
_ENCH = "modules/simpleenchanting/shared/resources/assets/simpleenchanting/textures/block"
VARIANTS = [
    Variant("oak", "minecraft:oak_door", "minecraft:oak_planks", "Oak Door", "Eichentür", formats=list(FORMATS)),
    _vw("spruce", "Spruce Door", "Fichtenholztür"), _vw("birch", "Birch Door", "Birkenholztür"),
    _vw("jungle", "Jungle Door", "Tropenholztür"), _vw("acacia", "Acacia Door", "Akazienholztür"),
    _vw("dark_oak", "Dark Oak Door", "Schwarzeichenholztür"), _vw("mangrove", "Mangrove Door", "Mangrovenholztür"),
    _vw("cherry", "Cherry Door", "Kirschholztür"), _vw("pale_oak", "Pale Oak Door", "Blasseichenholztür"),
    _vw("bamboo", "Bamboo Door", "Bambustür"), _vw("crimson", "Crimson Door", "Karmesintür"), _vw("warped", "Warped Door", "Wirrtür"),
    Variant("iron", "minecraft:iron_door", "minecraft:iron_ingot", "Iron Door", "Eisentür", wood=False),
    _copper("", "Copper Door", "Kupfertür"), _copper("exposed", "Exposed Copper Door", "Angelaufene Kupfertür"),
    _copper("weathered", "Weathered Copper Door", "Verwitterte Kupfertür"), _copper("oxidized", "Oxidized Copper Door", "Oxidierte Kupfertür"),
] + [Variant(m, f"simpleores:{m}_door", f"simpleores:{m}_ingot", en, de, mod="simpleores", tex=_ORES, wood=False)
     for m, en, de in (("brass", "Brass Door", "Messingtür"), ("bronze", "Bronze Door", "Bronzetür"), ("lead", "Lead Door", "Bleitür"),
                       ("nickel", "Nickel Door", "Nickeltür"), ("silver", "Silver Door", "Silbertür"),
                       ("stainless_steel", "Stainless Steel Door", "Edelstahltür"), ("tin", "Tin Door", "Zinntür"), ("zinc", "Zinc Door", "Zinktür"))] + [
    Variant("ashwood", "simpleend:ashwood_door", "simpleend:ashwood_planks", "Ashwood Door", "Aschholztür", mod="simpleend", tex=_END),
    Variant("duskwood", "simpleend:duskwood_door", "simpleend:duskwood_planks", "Duskwood Door", "Dämmerholztür", mod="simpleend", tex=_END),
    Variant("crimson_blazewood", "simpleenchanting:crimson_blazewood_door", "simpleenchanting:crimson_blazewood_planks",
            "Crimson Blazewood Door", "Karmesin-Lohenholztür", mod="simpleenchanting", tex=_ENCH),
    Variant("warped_blazewood", "simpleenchanting:warped_blazewood_door", "simpleenchanting:warped_blazewood_planks",
            "Warped Blazewood Door", "Wirr-Lohenholztür", mod="simpleenchanting", tex=_ENCH),
]
BY_ID = {v.id: v for v in VARIANTS}
WOODS = [v.id for v in VARIANTS]  # every registered variant (name kept for older callers)
VANILLA = [v.id for v in VARIANTS if v.vanilla]

# ------------------------------------------------------------------ derivation of the textures


def _vanilla(jar, name):
    from PIL import Image
    with zipfile.ZipFile(str(jar)) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")


def _door_atlas(jar, wood):
    """The door, 16 x 32: top half over bottom half (Vanilla from the jar, a module's from its committed textures)."""
    from PIL import Image
    v = BY_ID.get(wood)
    if v is not None and not v.vanilla:
        top = Image.open(ROOT / v.tex / f"{wood}_door_top.png").convert("RGBA")
        bottom = Image.open(ROOT / v.tex / f"{wood}_door_bottom.png").convert("RGBA")
    else:
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
    formats = BY_ID[wood].formats if wood in BY_ID else list(FORMATS)
    for fmt, (cols, rows, half) in FORMATS.items():
        if fmt not in formats:
            continue
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
    """Vanilla-derived output for tools/textures/vanilla_derived.py: texture path -> image (the Vanilla variants)."""
    out = {}
    for wood in VANILLA:
        out.update(derive_wood(jar, wood))
    return out


def derived_paths():
    """The texture paths derived() writes (for the explicit OUTPUTS lines of vanilla_derived.py and .gitignore)."""
    out = []
    for wood in VANILLA:
        for fmt in BY_ID[wood].formats:
            cols, rows, half = FORMATS[fmt]
            for r in range(rows):
                for c in range(cols):
                    out.append(f"block/{tex_name(wood, fmt, c, r)}.png")
    return out


def mod_textures():
    """Committed textures of the module variants: repository path -> image (own pixels, rearranged)."""
    out = {}
    for v in VARIANTS:
        if v.vanilla:
            continue
        for rel, img in derive_wood(None, v.id).items():
            out[ASSETS / "textures" / rel] = img
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


_IMAGES = {}


def _cell_image(wood, name):
    """The derived 16 x 16 texture of a door cell (needs the Vanilla jar; None without it, the models then keep the plain slab)."""
    if wood not in _IMAGES:
        v = BY_ID.get(wood)
        _IMAGES[wood] = derive_wood(JAR, wood) if (v is not None and not v.vanilla) or os.path.exists(JAR) else {}
    return _IMAGES[wood].get(f"block/{name}.png")


def _voxel(wood, name, run_axis, run, depth, front, back):
    """IG31-9 (owner 31.10., variant B): a leaf with see-through texels becomes boxes with an inner wall (tools/door_voxel.py)."""
    img = _cell_image(wood, name)
    if img is None:
        return None
    return door_voxel.rect_elements(img, run_axis, run, depth, front, back, "#t")


def _particle(wood):
    return BY_ID[wood].particle if wood in BY_ID else "minecraft:block/oak_planks"


def closed_model(wood, fmt, c, r, hinge):
    """One cell of a closed door: the slab on the south edge (north-facing frame). South face u=0 at the hinge (LEFT: west)."""
    cols, rows, half = FORMATS[fmt]
    left = hinge == "left"
    w = 8 if half or (fmt in NARROW and c == 1) else 16
    x0, x1 = (0, w) if left or w == 16 else (16 - w, 16)
    south = [0, 0, w, 16] if left else [w, 0, 0, 16]
    north = [w, 0, 0, 16] if left else [0, 0, w, 16]
    # IG31-34: each thin edge shows the texture column at its own end (the frame), not always the first three columns;
    # u = 0 lies at the low x for LEFT, at the high x for RIGHT
    lo_edge, hi_edge = [0, 0, 3, 16], [w - 3, 0, w, 16]
    west, east = (lo_edge, hi_edge) if left else (hi_edge, lo_edge)
    el = slab([x0, 0, 13], [x1, 16, 16], {"south": south, "north": north, "up": [0, 0, w, 3], "down": [0, 13, w, 16], "east": east, "west": west})
    tex = f"{NS}:block/{tex_name(wood, fmt, c, r)}"
    # seen from +z (south) the left edge is the low x; the back (north) is mirrored
    voxel = _voxel(wood, tex_name(wood, fmt, c, r), "x", (x0, x1), (13, 16), ("south", south, True), ("north", north, False))
    return {"textures": {"t": tex, "particle": _particle(wood)}, "elements": voxel or [el]}


def twin_model(wood, fmt, r):
    """IG31-34: the shared middle cell of two 1.5 wide doors (a double door 3 blocks wide): the LEFT door's stub in the west half,
    the mirrored RIGHT door's stub in the east half (north-facing frame as seen by the blockstate of either door)."""
    a = closed_model(wood, fmt, 1, r, "left")
    b = closed_model(wood, fmt, 1, r, "right")
    return {"textures": a["textures"], "elements": a["elements"] + b["elements"]}


def open_model(wood, fmt, seg, r, hinge):
    """One cell of an open door: the leaf strip along the hinge edge (LEFT: west), segment ``seg`` of the leaf (0 at the hinge cell)."""
    cols, rows, half = FORMATS[fmt]
    left = hinge == "left"
    length = 8 if half or (fmt in NARROW and seg == 1) else 16
    x0, x1 = (0, 3) if left else (13, 16)
    z0 = 16 - length
    east = [0, 0, length, 16]
    west = [length, 0, 0, 16]
    # IG31-34: u = 0 at the hinge (z 16), the leaf end (north) shows the texture's last columns
    el = slab([x0, 0, z0], [x1, 16, 16], {"east": east, "west": west, "up": [0, 0, 3, length], "down": [0, 0, 3, length],
                                          "north": [length - 3, 0, length, 16], "south": [0, 0, 3, 16]})
    tex = f"{NS}:block/{tex_name(wood, fmt, seg, r)}"
    # seen from +x (east) the left edge is the high z
    voxel = _voxel(wood, tex_name(wood, fmt, seg, r), "z", (z0, 16), (x0, x1), ("east", east, False), ("west", west, True))
    return {"textures": {"t": tex, "particle": _particle(wood)}, "elements": voxel or [el]}


def item_model(wood, fmt):
    """3D inventory model: the closed door, every cell shrunk to 16/rows px, hinge on the left."""
    cols, rows, half = FORMATS[fmt]
    u = 16.0 / rows
    width = (cols * u) if not half else u / 2
    if fmt in NARROW:
        width = 1.5 * u
    x_start = (16 - width) / 2
    textures = {"particle": _particle(wood)}
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
    """Every state of the block (col 0-1, row 0-2, swing, twin) gets exactly one model, with partial variant keys (N57-1: a
    third of the full table): the states a door never reaches (MIXCHECK31-5: open with col 1, rows above the door, closed swing
    cells) take the nearest real cell, so the client does not log 'Missing model for variant'. Closed: the cell's model by col
    and row (swing does not matter; the stub column of a 1.5 door splits by twin). Open: the hinge column's swing cells show the
    leaf's second segment, everything else the first."""
    cols, rows, half = FORMATS[fmt]
    rot = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
    variants = {}
    for facing, rt in rot.items():
        for hinge in ("left", "right"):
            pre = f"facing={facing},hinge={hinge}"
            for r in range(3):
                rr = min(r, rows - 1)
                for c in range(2):
                    cc = min(c, cols - 1)
                    closed = {"model": f"{NS}:block/{wood}_door_{fmt}_closed_c{cc}_r{rr}_{hinge}", **rt}
                    if fmt in NARROW and c == 1:
                        variants[f"{pre},open=false,col=1,row={r},twin=false"] = closed
                        variants[f"{pre},open=false,col=1,row={r},twin=true"] = {"model": f"{NS}:block/{wood}_door_{fmt}_closed_twin_r{rr}", **rt}
                    else:
                        variants[f"{pre},open=false,col={c},row={r}"] = closed
                s0 = {"model": f"{NS}:block/{wood}_door_{fmt}_open_s0_r{rr}_{hinge}", **rt}
                s1 = {"model": f"{NS}:block/{wood}_door_{fmt}_open_s1_r{rr}_{hinge}", **rt} if not half and r < rows else s0
                variants[f"{pre},open=true,col=1,row={r}"] = s0
                variants[f"{pre},open=true,col=0,row={r},swing=false"] = s0
                variants[f"{pre},open=true,col=0,row={r},swing=true"] = s1
    return {"variants": variants}


def covered_once(variants):
    """Self-check: every state of the block matches exactly one key (WideDoorBlock's properties)."""
    import itertools
    props = {"facing": ["north", "east", "south", "west"], "hinge": ["left", "right"], "open": ["false", "true"], "col": ["0", "1"],
             "row": ["0", "1", "2"], "swing": ["false", "true"], "twin": ["false", "true"]}
    keys = [dict(kv.split("=") for kv in k.split(",")) for k in variants]
    names = list(props)
    for combo in itertools.product(*props.values()):
        state = dict(zip(names, combo))
        hits = sum(1 for k in keys if all(state[p] == v for p, v in k.items()))
        if hits != 1:
            return f"{state}: {hits} keys"
    return None


def loot(wood, fmt):
    name = f"{NS}:{wood}_door_{fmt}"
    return {"type": "minecraft:block",
            "pools": [{"condition": {"type": "minecraft:survives_explosion"},
                       "entries": [{"type": "minecraft:item",
                                    "condition": {"type": "minecraft:match_block", "blocks": name, "state": {"col": "0", "row": "0", "swing": "false"}},
                                    "name": name}],
                       "rolls": 1}],
            "random_sequence": f"{NS}:blocks/{wood}_door_{fmt}"}


def conditions(mod):
    """Load conditions on the recipes of a module variant (all three loaders; Simple Enchanting is Fabric only)."""
    if mod is None:
        return {}
    return {"fabric:load_conditions": [{"condition": "fabric:all_mods_loaded", "values": [mod]}],
            "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": mod}],
            "forge:condition": {"type": "forge:mod_loaded", "modid": mod}}


def recipes(wood):
    """2x2 = 4 doors, 2x3 = 6 doors (copper: the waxed door as well), 1.5x2 = a door + 2 planks + a stick (metal: 2 ingots), 1.5x3 =
    the 1.5x2 door + a plank (ingot). The half doors are retired (a-ig31c: no recipe)."""
    v = BY_ID[wood]
    door = [v.door, v.alt_door] if v.alt_door else v.door
    mat = v.material
    cond = conditions(v.mod)
    narrow = [door, mat, mat] + (["minecraft:stick"] if v.wood else [])
    return {
        "2x2": ({**cond, "type": "minecraft:crafting_shaped", "category": "redstone", "key": {"#": door}, "pattern": ["##", "##"],
                 "result": {"count": 1, "id": f"{NS}:{wood}_door_2x2"}}, v.door),
        "2x3": ({**cond, "type": "minecraft:crafting_shaped", "category": "redstone", "key": {"#": door}, "pattern": ["##", "##", "##"],
                 "result": {"count": 1, "id": f"{NS}:{wood}_door_2x3"}}, v.door),
        "1_5x2": ({**cond, "type": "minecraft:crafting_shapeless", "category": "redstone", "ingredients": narrow,
                   "result": {"count": 1, "id": f"{NS}:{wood}_door_1_5x2"}}, v.door),
        "1_5x3": ({**cond, "type": "minecraft:crafting_shapeless", "category": "redstone", "ingredients": [f"{NS}:{wood}_door_1_5x2", mat],
                   "result": {"count": 1, "id": f"{NS}:{wood}_door_1_5x3"}}, f"{NS}:{wood}_door_1_5x2"),
        "half_2": None,
        "half_3": None,
    }


def unlock(recipe_id, ingredient, mod=None):
    short = ingredient.split(":")[1]
    return {**conditions(mod), "parent": "minecraft:recipes/root",
            "criteria": {f"has_{short}": {"conditions": {"items": [{"items": ingredient}]}, "trigger": "minecraft:inventory_changed"},
                         "has_the_recipe": {"conditions": {"recipes": f"{NS}:{recipe_id}"}, "trigger": "minecraft:recipe_unlocked"}},
            "requirements": [["has_the_recipe", f"has_{short}"]],
            "rewards": {"recipes": [f"{NS}:{recipe_id}"]}}


GEO = "door_geo"


def _split(model, out, item=False):
    """N57-1: the elements go to a shared geometry model (named by their hash), the variant keeps a child with its textures."""
    import hashlib
    geo = {"parent": "minecraft:block/block", "elements": model["elements"]} if item else {"elements": model["elements"]}
    h = hashlib.sha1(json.dumps(geo, sort_keys=True).encode()).hexdigest()[:12]
    name = f"{GEO}/{'item_' if item else ''}{h}"
    out[ASSETS / f"models/block/{name}.json"] = geo
    return {"parent": f"{NS}:block/{name}", "textures": model["textures"]}


def files():
    out = {}
    for wood in WOODS:
        v = BY_ID[wood]
        recs = recipes(wood)
        for fmt in v.formats:
            cols, rows, half = FORMATS[fmt]
            name = f"{wood}_door_{fmt}"
            out[ASSETS / f"blockstates/{name}.json"] = blockstate(wood, fmt)
            out[ASSETS / f"items/{name}.json"] = {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}}
            out[ASSETS / f"models/item/{name}.json"] = _split(item_model(wood, fmt), out, item=True)
            for hinge in ("left", "right"):
                for r in range(rows):
                    for c in range(cols):
                        out[ASSETS / f"models/block/{name}_closed_c{c}_r{r}_{hinge}.json"] = _split(closed_model(wood, fmt, c, r, hinge), out)
                    out[ASSETS / f"models/block/{name}_open_s0_r{r}_{hinge}.json"] = _split(open_model(wood, fmt, 0, r, hinge), out)
                    if not half:
                        out[ASSETS / f"models/block/{name}_open_s1_r{r}_{hinge}.json"] = _split(open_model(wood, fmt, 1, r, hinge), out)
                    if fmt in NARROW:
                        out[ASSETS / f"models/block/{name}_closed_twin_r{r}.json"] = _split(twin_model(wood, fmt, r), out)
            out[DATA / f"loot_table/blocks/{name}.json"] = loot(wood, fmt)
            if recs[fmt] is None:
                continue
            recipe, ingredient = recs[fmt]
            out[DATA / f"recipe/{name}.json"] = recipe
            out[DATA / f"advancement/recipes/redstone/{name}.json"] = unlock(name, ingredient, v.mod)
    return out


def write_atlas(path, jar):
    """Preview of the derived atlases of the registered woods plus the Vanilla door next to them (for previews, never the repo)."""
    from PIL import Image
    tiles = []
    for wood in WOODS[:3]:
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


def _dump(content):
    """Compact JSON (N57-1: ~4000 generated files; one line each keeps the repository small)."""
    return json.dumps(content, separators=(",", ":")) + "\n"


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
        if path.name.startswith(tuple(f"{w}_door_" for w in WOODS)) and path.parent.name == "blockstates":
            problem = covered_once(content["variants"])
            if problem:
                print(f"{path.name}: {problem}")
                return 1
        same = path.is_file() and json.loads(path.read_text()) == content
        if a.check:
            if not same:
                stale.append(path.name)
            continue
        if not same or path.read_text() != _dump(content):
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(_dump(content))
    # geometry models no variant uses any more
    geo_dir = ASSETS / "models/block" / GEO
    extra = [f for f in geo_dir.glob("*.json") if f not in data] if geo_dir.is_dir() else []
    # module variants: committed textures with their layer source (A8 b)
    textures = mod_textures()
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import layers
    for path, img in textures.items():
        if a.check:
            from PIL import Image
            if not path.is_file() or Image.open(path).convert("RGBA").tobytes() != img.tobytes():
                stale.append(path.name)
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        layers.snap(img, "Umlegung der Mod-Tuer")
        layers.save(img, path)
    if a.check:
        stale += [f.name for f in extra]
        print("stale: " + ", ".join(stale[:8]) + f" ({len(stale)})" if stale else f"OK doors ({len(data)} files, {len(textures)} textures)")
        return 1 if stale else 0
    for f in extra:
        f.unlink()
    print(f"{len(data)} files, {len(textures)} module textures written ({len(extra)} unused geometry models removed)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
