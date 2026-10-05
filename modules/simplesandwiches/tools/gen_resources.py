"""Generate the Minecraft 26.3 resources; --check compares bytes without writing."""

import argparse
import json
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
OUTPUT = MODULE / "generated/resources"
NS = "simplesandwiches"
# A Windows checkout turns LF into CRLF; --check compares content, not line endings.
CRLF, LF = b"\r\n", b"\n"
# Keep in registry/ModBlocks.WOODS order (26.3 also registers poplar).
WOODS = (
    "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove",
    "cherry", "pale_oak", "poplar", "bamboo", "crimson", "warped",
)
KEYS = (
    "meat_raw", "meat_cooked", "fish_raw", "fish_cooked", "potato", "carrot",
    "golden", "apple", "melon", "berries", "beetroot", "kelp", "cookie", "pie",
    "chorus", "spider_eye", "rotten", "cheese", "cake", "netherite", "enderite", "generic",
)
# Audited against Vanilla 26.3 Items.java: FOOD, no USE_REMAINDER, except bread.
FOODS = (
    "apple", "golden_apple", "enchanted_golden_apple", "melon_slice", "sweet_berries",
    "glow_berries", "chorus_fruit", "carrot", "golden_carrot", "potato", "baked_potato",
    "poisonous_potato", "beetroot", "dried_kelp", "beef", "cooked_beef", "porkchop",
    "cooked_porkchop", "mutton", "cooked_mutton", "chicken", "cooked_chicken", "rabbit",
    "cooked_rabbit", "cod", "cooked_cod", "salmon", "cooked_salmon", "tropical_fish",
    "pufferfish", "cookie", "pumpkin_pie", "rotten_flesh", "spider_eye",
)
CUTS = ("up", "north", "south", "east", "west")


def model_ref(name):
    return {"type": "minecraft:model", "model": f"{NS}:{name}"}


def box(cut, slices):
    return {
        "up": [0, 0, 0, 16, slices, 16],
        "north": [0, 0, 16 - slices, 16, 16, 16],
        "south": [0, 0, 0, 16, 16, slices],
        "west": [16 - slices, 0, 0, 16, 16, 16],
        "east": [0, 0, 0, slices, 16, 16],
    }[cut]


def cuboid(bounds, inner=None):
    """Vanilla BlockElement UV projection; only boundary faces can be culled."""
    x0, y0, z0, x1, y1, z1 = bounds
    uv = {
        "down": [x0, 16 - z1, x1, 16 - z0],
        "up": [x0, z0, x1, z1],
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        "south": [x0, 16 - y1, x1, 16 - y0],
        "west": [z0, 16 - y1, z1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
    }
    boundary = dict(down=y0 == 0, up=y1 == 16, north=z0 == 0,
                    south=z1 == 16, west=x0 == 0, east=x1 == 16)
    faces = {}
    for face, coords in uv.items():
        texture = "inner" if face == inner else "top" if face in ("up", "down") else "side"
        faces[face] = {"uv": coords, "texture": f"#{texture}"}
        if boundary[face]:
            faces[face]["cullface"] = face
    return {"from": bounds[:3], "to": bounds[3:], "faces": faces}


def resources():
    files = {}

    def asset(path, value):
        files[f"assets/{NS}/{path}.json"] = value

    def data(path, value, namespace=NS):
        files[f"data/{namespace}/{path}.json"] = value

    def item(name, model):
        asset(f"items/{name}", {"model": model_ref(model)})

    def flat(name, parent="generated"):
        asset(f"models/item/{name}", {"parent": f"minecraft:item/{parent}",
                                     "textures": {"layer0": f"{NS}:item/{name}"}})

    def loot(name, entry):
        data(f"loot_table/blocks/{name}", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [entry]}],
            "random_sequence": f"{NS}:blocks/{name}",
        })

    def tag(kind, name, values, namespace=NS):
        data(f"tags/{kind}/{name}", {"replace": False, "values": values}, namespace)

    for wood in WOODS:
        name = f"{wood}_cutting_board"
        asset(f"blockstates/{name}", {"variants": {
            f"facing={direction}": {"model": f"{NS}:block/{name}", "y": rotation}
            for direction, rotation in (("north", 0), ("south", 180), ("east", 90), ("west", 270))
        }})
        asset(f"models/block/{name}", {
            "parent": "minecraft:block/block",
            "textures": {"top": f"{NS}:block/{name}", "side": f"{NS}:block/{name}_side", "particle": "#top"},
            # Owner picture 2026-10-05 (v4): 2 px plate with a raised 1 px rim frame around it.
            "elements": [cuboid([1, 0, 2, 15, 2, 14]),
                         cuboid([1, 2, 2, 15, 3, 3]), cuboid([1, 2, 13, 15, 3, 14]),
                         cuboid([1, 2, 3, 2, 3, 13]), cuboid([14, 2, 3, 15, 3, 13])],
        })
        item(name, f"block/{name}")
        data(f"recipe/{name}", {
            "type": "minecraft:crafting_shaped", "category": "misc", "group": f"{NS}:cutting_board",
            "pattern": ["SXX"], "key": {"S": "minecraft:stick", "X": f"minecraft:{wood}_slab"},
            "result": {"id": f"{NS}:{name}", "count": 1},
        })
        loot(name, {"type": "minecraft:item", "name": f"{NS}:{name}"})

    for food in ("cheese", "butter"):
        name = f"{food}_block"
        variants = {}
        for cut in CUTS:
            for slices in range(1, 17):
                suffix = "full" if slices == 16 else f"{cut}_{slices}"
                variants[f"cut={cut},slices={slices}"] = {"model": f"{NS}:block/{name}/{suffix}"}
                asset(f"models/block/{name}/{suffix}", {
                    "parent": "minecraft:block/block",
                    "textures": {part: f"{NS}:block/{name}_{part}" for part in ("top", "side", "inner")}
                                | {"particle": "#top"},
                    "elements": [cuboid(box(cut, slices), cut if slices < 16 else None)],
                })
        asset(f"blockstates/{name}", {"variants": variants})
        # The item shows how much is left (block_state component written by copy_state, see loot below).
        asset(f"items/{name}", {"model": {
            "type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "slices",
            "cases": [{"when": str(slices), "model": model_ref(f"block/{name}/up_{slices}")} for slices in range(1, 16)],
            "fallback": model_ref(f"block/{name}/full"),
        }})
        # Owner 2026-10-05: breaking keeps the block's state; a cut block drops itself with slices + cut
        # (Vanilla BlockItem restores them on placement), a whole block drops a plain block item.
        loot(name, {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{NS}:{name}",
             "condition": {"type": "minecraft:match_block", "blocks": f"{NS}:{name}", "state": {"slices": "16"}}},
            {"type": "minecraft:item", "name": f"{NS}:{name}",
             "modifier": {"type": "minecraft:copy_state", "block": f"{NS}:{name}", "properties": ["slices", "cut"]}},
        ]})

    variants = {}
    for content in ("milk", "butter", "curdling", "cheese", "spoiled"):
        for stage in range(4):
            suffix = f"milk_{stage}" if content == "milk" else f"curd_{stage}" if content == "curdling" else content
            variants[f"content={content},stage={stage}"] = {"model": f"{NS}:block/milk_cauldron/{suffix}"}
            # Vanilla 26.3 water_cauldron_full, with only content replaced (no water tint).
            asset(f"models/block/milk_cauldron/{suffix}", {
                "parent": "minecraft:block/template_cauldron_full",
                "textures": {"bottom": "minecraft:block/cauldron_bottom", "inside": "minecraft:block/cauldron_inner",
                             "particle": "minecraft:block/cauldron_side", "side": "minecraft:block/cauldron_side",
                             "top": "minecraft:block/cauldron_top", "content": f"{NS}:block/milk_cauldron/{suffix}"},
            })
    asset("blockstates/milk_cauldron", {"variants": variants})
    loot("milk_cauldron", {"type": "minecraft:item", "name": "minecraft:cauldron"})

    # Opened bread on the cutting board: two halves, drawn by the renderer through an ITEM_MODEL override
    # (subfolder: a render-only model, not a registered item).
    flat("board/bread_half")
    item("board/bread_half", "item/board/bread_half")
    sounds = {}
    for food in ("butter", "cheese"):
        sounds[f"block.{food}_block.cut"] = {
            "subtitle": f"subtitles.{NS}.block.{food}_block.cut",
            "sounds": [f"{NS}:block/{food}_cut{n}" for n in (1, 2, 3)],
        }
    files[f"assets/{NS}/sounds.json"] = sounds
    for name in ("knife", "cheese_slice", "butter_slice", "cake_slice"):
        flat(name, "handheld" if name == "knife" else "generated")
        item(name, f"item/{name}")
    data("recipe/knife", {"type": "minecraft:crafting_shaped", "category": "equipment",
                          "pattern": ["  N", " NN", "SN "],
                          "key": {"N": "minecraft:iron_nugget", "S": "minecraft:stick"},
                          "result": {"id": f"{NS}:knife", "count": 1}})

    parts = [{"type": "minecraft:condition", "property": "minecraft:custom_model_data", "index": 0,
              "on_true": model_ref("item/sandwich/bottom_buttered"),
              "on_false": model_ref("item/sandwich/bottom")}]
    for name in ("bottom", "bottom_buttered", *(f"top_{n}" for n in range(6))):
        flat(f"sandwich/{name}")
    for pos in range(5):
        cases = []
        for key in KEYS:
            name = f"sandwich/layer_{pos}_{key}"
            flat(name)
            cases.append({"when": key, "model": model_ref(f"item/{name}")})
        parts.append({"type": "minecraft:select", "property": "minecraft:custom_model_data", "index": pos,
                      "cases": cases, "fallback": {"type": "minecraft:empty"}})
    parts.append({"type": "minecraft:select", "property": "minecraft:custom_model_data", "index": 5,
                  "cases": [{"when": str(n), "model": model_ref(f"item/sandwich/top_{n}")} for n in range(6)],
                  "fallback": model_ref("item/sandwich/top_0")})
    asset("items/sandwich", {"model": {"type": "minecraft:composite", "models": parts}})

    tag("item", "sandwich_ingredients", [f"minecraft:{name}" for name in FOODS]
        + [f"{NS}:cheese_slice", f"{NS}:cake_slice"]
        + [{"id": f"simplebuilding:{name}", "required": False} for name in (
            "netherite_apple", "enchanted_netherite_apple", "netherite_carrot",
            "enderite_apple", "enchanted_enderite_apple", "enderite_carrot")])
    tag("item", "knife_repair_materials", ["minecraft:iron_nugget"])
    boards = [f"{NS}:{wood}_cutting_board" for wood in WOODS]
    for kind in ("item", "block"):
        tag(kind, "cutting_boards", boards)
    for name in ("mining", "durability"):
        tag("item", f"enchantable/{name}", [f"{NS}:knife"], "minecraft")
    tag("block", "mineable/axe", boards, "minecraft")
    for name in ("mineable/pickaxe", "cauldrons"):
        tag("block", name, [f"{NS}:milk_cauldron"], "minecraft")
    return {name: (json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")
            for name, value in sorted(files.items())}


def generate(check=False):
    expected = resources()
    existing = {p.relative_to(OUTPUT).as_posix(): p for p in OUTPUT.rglob("*")
                if p.is_file() and p.relative_to(OUTPUT).parts[0] != "wiki"}
    changes = []
    for name, content in expected.items():
        path = OUTPUT / name
        if name not in existing:
            changes.append(f"missing: {name}")
        elif path.read_bytes().replace(CRLF, LF) != content.replace(CRLF, LF):
            changes.append(f"different: {name}")
    extra = sorted(existing.keys() - expected.keys())
    changes.extend(f"extra: {name}" for name in extra)
    if check:
        if changes:
            print("\n".join(changes))
            return 1
    else:
        for name, content in expected.items():
            path = OUTPUT / name
            path.parent.mkdir(parents=True, exist_ok=True)
            if not path.exists() or path.read_bytes() != content:
                path.write_bytes(content)
        for name in extra:
            path = existing[name]
            if not path.resolve().is_relative_to(OUTPUT.resolve()):
                raise ValueError(f"Refusing to delete outside generated resources: {path}")
            path.unlink()
    print(f"{NS}: {len(expected)} generated resources {'valid' if check else 'written'}")
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Report missing, changed or extra resources without writing")
    raise SystemExit(generate(parser.parse_args().check))
