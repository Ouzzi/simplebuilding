#!/usr/bin/env python3
"""Woodwork assets (docs/ai/PLAN-HOLZWERK-2026-10-09.md, Queue Nachtrag 25).

Writes blockstates, block/item models and item definitions for the woodwork family into the 26.3 overlay, plus
the carved-wood motif palettes and the blocks-atlas source that derives the motifs at resource load. Every
model points at Vanilla textures; no Vanilla image is copied. The motif sprites come from the Vanilla atlas source
``paletted_permutations``: the decorated-pot pattern textures (``entity/decorated_pot/<x>_pottery_pattern``)
are recoloured with a key of their 15 colours - the pot background turns transparent, the three motif colours
turn into three dark tones of the stripped wood.

    python tools/textures/woodwork_2026_10_09.py            # write
    python tools/textures/woodwork_2026_10_09.py --check    # fail if a file would change
"""
from __future__ import annotations

import io
import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "mc26_3/overlay/resources/assets"
NS = "simplebuilding"

# id, log path, stripped log average colour (sRGB of the Vanilla 16x16 side, measured once 2026-10-09).
WOODS = [
    ("oak", "oak_log", 0xB19056),
    ("spruce", "spruce_log", 0x735934),
    ("birch", "birch_log", 0xC4B076),
    ("jungle", "jungle_log", 0xAB8454),
    ("acacia", "acacia_log", 0xAE5C3B),
    ("dark_oak", "dark_oak_log", 0x483824),
    ("mangrove", "mangrove_log", 0x77362F),
    ("cherry", "cherry_log", 0xD79194),
    ("pale_oak", "pale_oak_log", 0xF5EEEC),
    ("crimson", "crimson_stem", 0x89395A),
    ("warped", "warped_stem", 0x399693),
    ("bamboo", "bamboo_block", 0xC1AD50),
]
MOTIFS = ["angler", "archer", "arms_up", "blade", "brewer", "burn", "danger", "explorer", "flow", "friend",
          "guster", "heart", "heartbreak", "howl", "miner", "mourner", "plenty", "prize", "scrape", "sheaf",
          "shelter", "skull", "snort"]
# The 15 colours of every Vanilla pottery pattern, darkest first; the first three draw the motif.
POT_KEY = [(71, 37, 29), (82, 45, 37), (94, 55, 47), (105, 59, 49), (117, 66, 54), (125, 70, 57),
           (131, 73, 59), (139, 78, 64), (140, 78, 64), (148, 83, 68), (149, 90, 68), (159, 89, 73),
           (159, 101, 71), (171, 113, 75), (185, 128, 83)]
MOTIF_SHADES = (0.42, 0.52, 0.62)
# Own carving per wood (Queue Nachtrag 33, docs/ai/PLAN-HOLZWERK-2026-10-09.md "Schnitzmotive"): a transparent overlay on
# the stripped log - bevelled frame (light top/left, shadow bottom/right) and an incised 12x12 motif in the middle,
# the grain stays visible. Colours (cut edge, cut floor, light lip) are the darkest, third-darkest and lightest colour
# of the wood's Vanilla planks, measured once 2026-10-11; where the planks are darker than the stripped log (dark oak,
# crimson, warped) the light lip is the stripped log's lightest colour instead, else it would not show; on the light
# woods (birch, cherry, pale oak, bamboo) the two cut colours are those plank colours darkened (x0.72-0.8), else the
# cut would be lighter than its own lip and read as a raised white line.
CARVE_PAL = {
    "oak": (0x67502C, 0x967441, 0xC29D62),
    "spruce": (0x553A1F, 0x614B2E, 0x886539),
    "birch": (0x7B6C4C, 0x887C5C, 0xD7CB8D),
    "jungle": (0x68462F, 0x976A44, 0xBF8E6B),
    "acacia": (0x884728, 0x99502B, 0xC26D3F),
    "dark_oak": (0x291A0C, 0x3A2411, 0x523F27),
    "mangrove": (0x5D1C1E, 0x6F2A2D, 0x8B4D3A),
    "cherry": (0x94605C, 0xA27974, 0xE7CAC5),
    "pale_oak": (0x908686, 0x9E9495, 0xFFFBF8),
    "crimson": (0x3F1E2D, 0x4B2737, 0xA04468),
    "warped": (0x113835, 0x1F5752, 0x3DA9A6),
    "bamboo": (0x73652E, 0x96863D, 0xEFD97E),
}
# 12x12 motifs, '#' = cut. One per wood, after its tree.
CARVINGS = {
    "oak": [  # oak leaf with its midrib
        ".....##.....",
        "....#..#....",
        "..##.##.##..",
        "..#..##..#..",
        "...#.##.#...",
        ".##..##..##.",
        ".#...##...#.",
        "..#..##..#..",
        "...#.##.#...",
        "....####....",
        ".....##.....",
        ".....##.....",
    ],
    "spruce": [  # fir branch, needles hanging down
        ".....##.....",
        "....####....",
        "...#.##.#...",
        "..#..##..#..",
        "....####....",
        "...#.##.#...",
        "..#..##..#..",
        ".#...##...#.",
        "....####....",
        "..##.##.##..",
        ".#...##...#.",
        ".....##.....",
    ],
    "birch": [  # the birch rune (berkanan) between bark dashes
        "....#.......",
        "##..##....##",
        "....#.#.....",
        "....#..#....",
        "....#...#...",
        "....#..#....",
        "....##......",
        "....#.#.....",
        "....#..#....",
        "....#...#...",
        "##..#..#..##",
        "....##......",
    ],
    "jungle": [  # temple mask
        "...######...",
        "..#......#..",
        ".#........#.",
        ".#.##..##.#.",
        ".#........#.",
        ".#...##...#.",
        ".#........#.",
        ".#.######.#.",
        ".#.#.##.#.#.",
        ".#.######.#.",
        "..#......#..",
        "...######...",
    ],
    "acacia": [  # savanna sun
        ".....##.....",
        ".#...##...#.",
        "..#......#..",
        "....####....",
        "...#....#...",
        "##.#.##.#.##",
        "##.#.##.#.##",
        "...#....#...",
        "....####....",
        "..#......#..",
        ".#...##...#.",
        ".....##.....",
    ],
    "dark_oak": [  # four-lobed knot
        "..##....##..",
        ".#..#..#..#.",
        ".#...##...#.",
        "..#..##..#..",
        "...##..##...",
        "...#....#...",
        "...#....#...",
        "...##..##...",
        "..#..##..#..",
        ".#...##...#.",
        ".#..#..#..#.",
        "..##....##..",
    ],
    "mangrove": [  # stilt roots arching into the mud
        ".....##.....",
        ".....##.....",
        "....#..#....",
        "...#.##.#...",
        "..#.#..#.#..",
        ".#..#..#..#.",
        ".#.#....#.#.",
        "#..#....#..#",
        "#.#......#.#",
        "#.#......#.#",
        "############",
        "............",
    ],
    "cherry": [  # five-petal blossom
        ".....##.....",
        "....####....",
        "....####....",
        ".##..##..##.",
        "####....####",
        "####.##.####",
        ".##..##..##.",
        "............",
        "...##..##...",
        "..###..###..",
        "..##....##..",
        "............",
    ],
    "pale_oak": [  # the creaking's eye, cracked
        "..#......#..",
        "...#....#...",
        "...######...",
        "..#......#..",
        ".#...##...#.",
        "#...####...#",
        "#...####...#",
        ".#...##...#.",
        "..#......#..",
        "...######...",
        "...#....#...",
        "..#......#..",
    ],
    "crimson": [  # crimson fungus with roots
        "...######...",
        ".##......##.",
        "#...#..#...#",
        "#..........#",
        ".###.##.###.",
        "....#..#....",
        "....#..#....",
        "....#..#....",
        "...#....#...",
        "..#.#..#.#..",
        ".#..#..#..#.",
        "............",
    ],
    "warped": [  # twisting warped spiral
        "...######...",
        "..#......#..",
        ".#..####..#.",
        ".#.#....#.#.",
        ".#.#.##.#.#.",
        ".#.#..#.#.#.",
        ".#..##..#.#.",
        "..#.....#.#.",
        "...#####..#.",
        ".........#..",
        "..#.....#...",
        "...#####....",
    ],
    "bamboo": [  # bamboo fence lattice
        "...#....#...",
        "...#....#...",
        "...#....#...",
        "############",
        "...#....#...",
        "...#....#...",
        "...#....#...",
        "...#....#...",
        "############",
        "...#....#...",
        "...#....#...",
        "...#....#...",
    ],
}


def carving(wood: str, motif: bool = True) -> Image.Image:
    """The carving overlay of a wood: bevelled frame, and with {@code motif} its incised motif (cut edges facing the
    light are the darkest, the cut floor the mid tone, the lip below/right of a cut catches the light)."""
    dark, mid, light = (((c >> 16) & 255, (c >> 8) & 255, c & 255, 255) for c in CARVE_PAL[wood])
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(16):
        img.putpixel((i, 0), light)
        img.putpixel((0, i), light)
        img.putpixel((i, 15), mid)
        img.putpixel((15, i), mid)
    if not motif:
        return img
    cut = {(x + 2, y + 2) for y, row in enumerate(CARVINGS[wood]) for x, ch in enumerate(row) if ch == "#"}
    for x, y in cut:
        img.putpixel((x, y), dark if (x - 1, y) not in cut or (x, y - 1) not in cut else mid)
    for x, y in cut:
        for lip in ((x + 1, y), (x, y + 1)):
            if lip not in cut and 1 <= lip[0] <= 14 and 1 <= lip[1] <= 14 and img.getpixel(lip)[3] == 0:
                img.putpixel(lip, light)
    return img
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
# Crate opening (model: open top) per facing, the barrel's blockstate rotations.
CRATE_FACING = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "east": {"x": 90, "y": 90},
                "south": {"x": 90, "y": 180}, "west": {"x": 90, "y": 270}}


def crate_elements() -> list:
    """A real wooden crate, open at the top: four corner posts, three slats per side and three floor boards, with
    2 px gaps between the side slats and 1 px gaps between the floor boards (see-through, food shows between)."""
    def box(f, t):
        return {"from": f, "to": t, "faces": {d: {"texture": "#planks"} for d in
                                              ("north", "east", "south", "west", "up", "down")}}
    out = [box([x, 0, z], [x + 2, 16, z + 2]) for x in (0, 14) for z in (0, 14)]
    for y0, y1 in ((0, 4), (6, 10), (12, 16)):
        out += [box([2, y0, 0], [14, y1, 2]), box([2, y0, 14], [14, y1, 16]),
                box([0, y0, 2], [2, y1, 14]), box([14, y0, 2], [16, y1, 14])]
    out += [box([2, 0, z0], [14, 2, z1]) for z0, z1 in ((2, 5), (6, 10), (11, 14))]
    return out

files: dict[Path, bytes] = {}


def mc(path: str) -> str:
    return "minecraft:" + path


def sb(path: str) -> str:
    return f"{NS}:{path}"


def put_json(rel: str, data) -> None:
    files[OUT / rel] = (json.dumps(data, indent=2) + "\n").encode()


def put_png(rel: str, image: Image.Image) -> None:
    buf = io.BytesIO()
    image.save(buf, format="PNG", optimize=True)
    files[OUT / rel] = buf.getvalue()


def block_item(name: str, model: str | None = None) -> None:
    put_json(f"{NS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": model or sb(f"block/{name}")}})


def templates() -> None:
    put_json(f"{NS}/models/block/template_crate.json", {
        "parent": "minecraft:block/block", "textures": {"particle": "#planks"}, "elements": crate_elements()})
    cube = {"down": "#end", "up": "#end", "north": "#side", "south": "#side", "west": "#side", "east": "#side"}
    faces = {d: {"texture": t, "cullface": d} for d, t in cube.items()}
    put_json(f"{NS}/models/block/template_carved_log.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "#side"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces},
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {"north": {"texture": "#motif", "cullface": "north"}}},
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                "north": {"texture": "#frame", "cullface": "north"}, "east": {"texture": "#carving", "cullface": "east"},
                "south": {"texture": "#carving", "cullface": "south"}, "west": {"texture": "#carving", "cullface": "west"}}},
        ],
    })
    w = 2
    walls = [
        # north/south walls between the side walls, side walls full depth (tube along Y)
        ([w, 0, 0], [16 - w, 16, w], {"north": "#side", "south": "#inner"}, {"north": "north"}),
        ([w, 0, 16 - w], [16 - w, 16, 16], {"south": "#side", "north": "#inner"}, {"south": "south"}),
        ([0, 0, 0], [w, 16, 16], {"west": "#side", "east": "#inner", "north": "#side", "south": "#side"},
         {"west": "west", "north": "north", "south": "south"}),
        ([16 - w, 0, 0], [16, 16, 16], {"east": "#side", "west": "#inner", "north": "#side", "south": "#side"},
         {"east": "east", "north": "north", "south": "south"}),
    ]
    elements = []
    for frm, to, tex, cull in walls:
        fs = {d: ({"texture": t, "cullface": cull[d]} if d in cull else {"texture": t}) for d, t in tex.items()}
        fs["up"] = {"texture": "#end", "cullface": "up"}
        fs["down"] = {"texture": "#end", "cullface": "down"}
        elements.append({"from": frm, "to": to, "faces": fs})
    put_json(f"{NS}/models/block/template_hollow_log.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "#side"},
        "elements": elements,
    })


def palettes() -> None:
    key = Image.new("RGBA", (len(POT_KEY), 1))
    for i, c in enumerate(POT_KEY):
        key.putpixel((i, 0), c + (255,))
    put_png(f"{NS}/textures/palettes/woodwork/pot_pattern_key.png", key)
    for wood, _log, rgb in WOODS:
        pal = Image.new("RGBA", (len(POT_KEY), 1), (0, 0, 0, 0))
        base = ((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255)
        for i, f in enumerate(MOTIF_SHADES):
            pal.putpixel((i, 0), tuple(int(round(c * f)) for c in base) + (255,))
        put_png(f"{NS}/textures/palettes/woodwork/{wood}.png", pal)
    put_json("minecraft/atlases/blocks.json", {"sources": [{
        "type": "minecraft:paletted_permutations",
        "textures": [mc(f"entity/decorated_pot/{m}_pottery_pattern") for m in MOTIFS],
        "palette_key": sb("woodwork/pot_pattern_key"),
        "permutations": {wood: sb(f"woodwork/{wood}") for wood, _l, _c in WOODS},
    }]})


def wood_assets(wood: str, log: str) -> None:
    stripped = "stripped_" + log
    tex = {
        "log": mc(f"block/{log}"), "log_top": mc(f"block/{log}_top"),
        "stripped": mc(f"block/{stripped}"), "stripped_top": mc(f"block/{stripped}_top"),
        "planks": mc(f"block/{wood}_planks"),
    }
    # hollow logs
    for name, side, end in ((f"hollow_{log}", tex["log"], tex["log_top"]),
                            (f"hollow_{stripped}", tex["stripped"], tex["stripped_top"])):
        put_json(f"{NS}/models/block/{name}.json", {"parent": sb("block/template_hollow_log"),
                                                     "textures": {"side": side, "inner": tex["stripped"], "end": end}})
        put_json(f"{NS}/blockstates/{name}.json", {"variants": {
            "axis=x": {"model": sb(f"block/{name}"), "x": 90, "y": 90},
            "axis=y": {"model": sb(f"block/{name}")},
            "axis=z": {"model": sb(f"block/{name}"), "x": 90},
        }})
        block_item(name)
    # sheets (glass-pane shapes)
    for name, pane in ((f"{wood}_sheet", tex["log"]), (f"stripped_{wood}_sheet", tex["stripped"])):
        for part in ("post", "side", "side_alt", "noside", "noside_alt"):
            put_json(f"{NS}/models/block/{name}_{part}.json", {
                "parent": mc(f"block/template_glass_pane_{part}"),
                "textures": {"pane": pane, "edge": tex["stripped"]}})
        m = lambda part, y=None: ({"model": sb(f"block/{name}_{part}")} | ({"y": y} if y else {}))
        put_json(f"{NS}/blockstates/{name}.json", {"multipart": [
            {"apply": m("post")},
            {"when": {"north": "true"}, "apply": m("side")},
            {"when": {"east": "true"}, "apply": m("side", 90)},
            {"when": {"south": "true"}, "apply": m("side_alt")},
            {"when": {"west": "true"}, "apply": m("side_alt", 90)},
            {"when": {"north": "false"}, "apply": m("noside")},
            {"when": {"east": "false"}, "apply": m("noside_alt")},
            {"when": {"south": "false"}, "apply": m("noside_alt", 90)},
            {"when": {"west": "false"}, "apply": m("noside", 270)},
        ]})
        # Inventory: the sheet as a thin standing panel (a 3D icon; a flat Vanilla-texture item has none in the wiki).
        put_json(f"{NS}/models/item/{name}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": pane, "pane": pane, "edge": tex["stripped"]},
            "elements": [{"from": [0, 0, 7], "to": [16, 16, 9], "faces": {
                "north": {"texture": "#pane"}, "south": {"texture": "#pane"},
                "west": {"uv": [7, 0, 9, 16], "texture": "#edge"}, "east": {"uv": [7, 0, 9, 16], "texture": "#edge"},
                "up": {"uv": [0, 7, 16, 9], "texture": "#edge"}, "down": {"uv": [0, 7, 16, 9], "texture": "#edge"}}}],
        })
        block_item(name, sb(f"item/{name}"))
    # wooden cauldron: Vanilla cauldron shapes in wood (bark outside, stripped wood inside)
    name = f"{wood}_cauldron"
    ctex = {"particle": tex["log"], "side": tex["log"], "top": tex["log_top"], "bottom": tex["log_top"],
            "inside": tex["stripped"]}
    put_json(f"{NS}/models/block/{name}.json", {"parent": "minecraft:block/cauldron", "textures": ctex})
    contents = {"water": mc("block/water_still"), "powder_snow": mc("block/powder_snow")}
    for content, content_tex in contents.items():
        for level, template in ((1, "template_cauldron_level1"), (2, "template_cauldron_level2"), (3, "template_cauldron_full")):
            put_json(f"{NS}/models/block/{name}_{content}_{level}.json",
                     {"parent": mc(f"block/{template}"), "textures": ctex | {"content": content_tex}})
    put_json(f"{NS}/models/block/{name}_lava.json",
             {"parent": mc("block/template_cauldron_full"), "textures": ctex | {"content": mc("block/lava_still")}})
    variants = {}
    for content in ("empty", "water", "lava", "powder_snow"):
        for level in (1, 2, 3):
            model = {"empty": name, "lava": f"{name}_lava"}.get(content, f"{name}_{content}_{level}")
            variants[f"content={content},level={level}"] = {"model": sb(f"block/{model}")}
    put_json(f"{NS}/blockstates/{name}.json", {"variants": variants})
    block_item(name)
    # crate: an open box of plank slats with gaps (template_crate), the opening turned to facing like a barrel
    name = f"{wood}_crate"
    put_json(f"{NS}/models/block/{name}.json", {"parent": sb("block/template_crate"), "textures": {
        "particle": tex["planks"], "planks": tex["planks"]}})
    put_json(f"{NS}/blockstates/{name}.json", {"variants": {
        f"facing={facing}": ({"model": sb(f"block/{name}")} | rot) for facing, rot in CRATE_FACING.items()}})
    block_item(name)
    # carved wood: one model per motif, turned by facing
    name = f"carved_{log}"
    put_png(f"{NS}/textures/block/carved/{wood}_carving.png", carving(wood))
    put_png(f"{NS}/textures/block/carved/{wood}_frame.png", carving(wood, motif=False))
    for motif in MOTIFS:
        put_json(f"{NS}/models/block/carved/{name}_{motif}.json", {"parent": sb("block/template_carved_log"), "textures": {
            "side": tex["stripped"], "end": tex["stripped_top"],
            "carving": sb(f"block/carved/{wood}_carving"), "frame": sb(f"block/carved/{wood}_frame"),
            "motif": mc(f"entity/decorated_pot/{motif}_pottery_pattern_{wood}")}})
    put_json(f"{NS}/blockstates/{name}.json", {"variants": {
        f"facing={facing},motif={motif}": ({"model": sb(f"block/carved/{name}_{motif}")} | ({"y": y} if y else {}))
        for motif in MOTIFS for facing, y in FACING_Y.items()}})
    put_json(f"{NS}/items/{name}.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "motif",
        "cases": [{"when": motif, "model": {"type": "minecraft:model", "model": sb(f"block/carved/{name}_{motif}")}}
                  for motif in MOTIFS],
        "fallback": {"type": "minecraft:model", "model": sb(f"block/carved/{name}_angler")},
    }})


def stripped_cauldron(wood: str, log: str) -> None:
    """N35: the same cauldron in stripped wood (stripped log outside, planks inside)."""
    stripped = "stripped_" + log
    name = f"stripped_{wood}_cauldron"
    ctex = {"particle": mc(f"block/{stripped}"), "side": mc(f"block/{stripped}"), "top": mc(f"block/{stripped}_top"),
            "bottom": mc(f"block/{stripped}_top"), "inside": mc(f"block/{wood}_planks")}
    put_json(f"{NS}/models/block/{name}.json", {"parent": "minecraft:block/cauldron", "textures": ctex})
    for content, content_tex in {"water": mc("block/water_still"), "powder_snow": mc("block/powder_snow")}.items():
        for level, template in ((1, "template_cauldron_level1"), (2, "template_cauldron_level2"), (3, "template_cauldron_full")):
            put_json(f"{NS}/models/block/{name}_{content}_{level}.json",
                     {"parent": mc(f"block/{template}"), "textures": ctex | {"content": content_tex}})
    put_json(f"{NS}/models/block/{name}_lava.json",
             {"parent": mc("block/template_cauldron_full"), "textures": ctex | {"content": mc("block/lava_still")}})
    variants = {}
    for content in ("empty", "water", "lava", "powder_snow"):
        for level in (1, 2, 3):
            model = {"empty": name, "lava": f"{name}_lava"}.get(content, f"{name}_{content}_{level}")
            variants[f"content={content},level={level}"] = {"model": sb(f"block/{model}")}
    put_json(f"{NS}/blockstates/{name}.json", {"variants": variants})
    block_item(name)


def preview(jar: str, out: Path) -> None:
    """Previews from a local Minecraft jar (not checked in): carved_motifs.png (every sherd motif on every wood) and
    carved_old_new.png - per wood: old sherd face, new sherd face (framed), new carving side, carving side 3x3 tiled,
    all 8x."""
    import zipfile
    from PIL import ImageDraw
    z = zipfile.ZipFile(jar)
    def img(p): return Image.open(io.BytesIO(z.read(p))).convert("RGBA")
    pals = {}
    for wood, _l, rgb in WOODS:
        base = ((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255)
        pals[wood] = {POT_KEY[i]: tuple(int(round(c * f)) for c in base) + (255,) for i, f in enumerate(MOTIF_SHADES)}
    def sherd(wood, side, motif):
        pat = img(f"assets/minecraft/textures/entity/decorated_pot/{motif}_pottery_pattern.png")
        tile = side.copy()
        for y in range(16):
            for x in range(16):
                p = pat.getpixel((x, y))
                if p[3] and p[:3] in pals[wood]:
                    tile.putpixel((x, y), pals[wood][p[:3]])
        return tile
    sides = {w: img(f"assets/minecraft/textures/block/stripped_{log}.png").crop((0, 0, 16, 16)) for w, log, _c in WOODS}
    sheet = Image.new("RGBA", (len(MOTIFS) * 17 + 1, len(WOODS) * 17 + 1), (40, 40, 40, 255))
    for r, (wood, _log, _c) in enumerate(WOODS):
        for c, motif in enumerate(MOTIFS):
            sheet.paste(sherd(wood, sides[wood], motif), (1 + c * 17, 1 + r * 17))
    out.mkdir(parents=True, exist_ok=True)
    sheet.resize((sheet.width * 6, sheet.height * 6), Image.NEAREST).save(out / "carved_motifs.png")
    k, row_h = 8, 50
    old_new = Image.new("RGBA", (160 + (16 * 3 + 48 + 16) * k, len(WOODS) * (48 + 4) * k + 4 * k), (40, 40, 40, 255))
    d = ImageDraw.Draw(old_new)
    for r, (wood, _log, _c) in enumerate(WOODS):
        side = sides[wood]
        new_side = side.copy()
        new_side.alpha_composite(carving(wood))
        new_face = sherd(wood, side, "heart")
        new_face.alpha_composite(carving(wood, motif=False))
        y = 4 * k + r * (48 + 4) * k
        d.text((8, y + 8), wood, fill=(230, 230, 230, 255))
        x = 160
        for tile in (sherd(wood, side, "heart"), new_face, new_side):
            old_new.paste(tile.resize((16 * k, 16 * k), Image.NEAREST), (x, y))
            x += (16 + 4) * k
        wall = Image.new("RGBA", (48, 48))
        for i in range(3):
            for j in range(3):
                wall.paste(new_side, (16 * i, 16 * j))
        old_new.paste(wall.resize((48 * k, 48 * k), Image.NEAREST), (x, y))
    old_new.save(out / "carved_old_new.png")


def main() -> int:
    templates()
    palettes()
    for wood, log, _c in WOODS:
        wood_assets(wood, log)
        stripped_cauldron(wood, log)
    if "--preview" in sys.argv:
        i = sys.argv.index("--preview")
        preview(sys.argv[i + 1], Path(sys.argv[i + 2]))
        return 0
    check = "--check" in sys.argv
    changed = [p for p, data in files.items() if not p.exists() or p.read_bytes() != data]
    if check:
        for p in changed:
            print("would change:", p.relative_to(ROOT))
        return 1 if changed else 0
    for p in changed:
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(files[p])
    print(f"{len(files)} files, {len(changed)} written")
    return 0


if __name__ == "__main__":
    sys.exit(main())
