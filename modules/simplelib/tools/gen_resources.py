"""Generate SimpleLib's Minecraft 26.3 resources; --check compares content without writing.

Crucible models are built from elements: a floor, four walls (two pixels thick) and two handles. The
half-built blank shows the iron block's floor plus the walls/handles struck so far (stage 1-5).
"""

import argparse
import json
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
OUTPUT = MODULE / "generated/resources"
NS = "simplelib"
CRLF, LF = b"\r\n", b"\n"
TIERS = ("iron", "reinforced", "netherite")
BARREL_TIERS = ("copper", "reinforced", "netherite")
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}

# Owner 37: everything that is eaten or served warm.
WARMABLE = (
    "cooked_beef", "cooked_porkchop", "cooked_chicken", "cooked_mutton", "cooked_rabbit",
    "cooked_cod", "cooked_salmon", "baked_potato", "bread", "pumpkin_pie",
    "mushroom_stew", "rabbit_stew", "beetroot_soup", "suspicious_stew",
)


def face(tex, uv, cull=None):
    out = {"texture": tex, "uv": uv}
    if cull:
        out["cullface"] = cull
    return out


def cuboid(frm, to, textures):
    """textures: dict face -> texture variable; UVs follow Vanilla's element projection."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    uv = {
        "down": [x0, 16 - z1, x1, 16 - z0], "up": [x0, z0, x1, z1],
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
        "west": [z0, 16 - y1, z1, 16 - y0], "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
    }
    cull = dict(down=y0 == 0, up=y1 == 16, north=z0 == 0, south=z1 == 16, west=x0 == 0, east=x1 == 16)
    faces = {f: face(t, uv[f], f if cull[f] else None) for f, t in textures.items()}
    return {"from": list(frm), "to": list(to), "faces": faces}


def all_faces(side="#side", top="#top", bottom="#bottom", inner="#inner"):
    return {"down": bottom, "up": top, "north": side, "south": side, "west": side, "east": side}


# Kettle shape (owner 2026-10-05: narrower at the bottom and the top by 1-2 px): foot x/z 3-13 (y 0-2), floor 2-14
# (y 2-4, inner floor at y 4), belly 1-15 (y 4-11), neck 2-14 (y 11-14); the opening stays 3-13.
SIDES = {"north": "#side", "south": "#side", "west": "#side", "east": "#side"}
FOOT = [cuboid((3, 0, 3), (13, 2, 13), {"down": "#bottom", **SIDES})]
FLOOR = cuboid((2, 2, 2), (14, 4, 14), {"down": "#bottom", "up": "#inner", **SIDES})


def ring(o0, o1, y0, y1, underside):
    """Four wall pieces (north, east, south, west) between outer o0..o1 and the opening 3..13."""
    down = {"down": "#bottom"} if underside else {}
    return [
        cuboid((o0, y0, o0), (o1, y1, 3), {"north": "#side", "south": "#inner", "up": "#top", "west": "#side", "east": "#side", **down}),
        cuboid((13, y0, 3), (o1, y1, 13), {"east": "#side", "west": "#inner", "up": "#top", **down}),
        cuboid((o0, y0, 13), (o1, y1, o1), {"south": "#side", "north": "#inner", "up": "#top", "west": "#side", "east": "#side", **down}),
        cuboid((o0, y0, 3), (3, y1, 13), {"west": "#side", "east": "#inner", "up": "#top", **down}),
    ]


BELLY = ring(1, 15, 4, 11, True)
NECK = ring(2, 14, 11, 14, False)
# One wall = belly + neck piece of a side (the blank shows them strike by strike).
WALLS = [[BELLY[i], NECK[i]] for i in range(4)]
WALLS_FLAT = BELLY + NECK
HANDLES = [
    cuboid((15, 9, 6), (16, 11, 10), {"east": "#handle", "up": "#handle", "down": "#handle", "north": "#handle", "south": "#handle"}),
    cuboid((0, 9, 6), (1, 11, 10), {"west": "#handle", "up": "#handle", "down": "#handle", "north": "#handle", "south": "#handle"}),
]


def inside_uv(element):
    """Shifts face UVs that reach outside 0..16 back inside (outside the sprite the atlas neighbour would show)."""
    for f in element["faces"].values():
        u0, v0, u1, v1 = f["uv"]
        du = -min(u0, u1) if min(u0, u1) < 0 else 16 - max(u0, u1) if max(u0, u1) > 16 else 0
        dv = -min(v0, v1) if min(v0, v1) < 0 else 16 - max(v0, v1) if max(v0, v1) > 16 else 0
        f["uv"] = [u0 + du, v0 + dv, u1 + du, v1 + dv]
    return element


# Attached barrel (facing north = towards the crucible), owner feedback 2026-10-06: 1-2 px smaller than the crucible -
# body x/z 1..15, height 12 (the crucible's rim is at 14), whole textures scaled onto the smaller faces. A flange closes
# the 2 px to the crucible's belly (z -1..1), a chute rests on its rim and drops onto the barrel.
def full_uv(element):
    for f in element["faces"].values():
        f["uv"] = [0, 0, 16, 16]
    return element


DOCKED_BODY = full_uv(cuboid((1, 0, 1), (15, 12, 15), {"down": "#bottom", "up": "#top", "north": "#side", "south": "#side",
                                                       "west": "#side", "east": "#side"}))
DOCK = [inside_uv(e) for e in (
    cuboid((3, 3, -1), (13, 10, 1), {"north": "#flange", "up": "#flange", "down": "#flange", "west": "#flange", "east": "#flange"}),
    cuboid((6, 14, -4), (10, 15, 2), {"north": "#flange", "up": "#flange", "down": "#flange", "west": "#flange", "east": "#flange",
                                      "south": "#flange"}),
    cuboid((6, 12, 1), (10, 14, 2), {"north": "#flange", "west": "#flange", "east": "#flange", "south": "#flange"}),
)]


def crucible_model(tier):
    tex = {k: f"{NS}:block/{tier}_crucible_{k}" for k in ("side", "top", "bottom", "inner", "handle")}
    tex["particle"] = tex["side"]
    return {"parent": "minecraft:block/block", "textures": tex, "elements": [*FOOT, FLOOR, *WALLS_FLAT, *HANDLES]}


def blank_model(stage):
    tex = {k: f"{NS}:block/iron_crucible_{k}" for k in ("side", "top", "bottom", "inner", "handle")}
    tex["particle"] = "minecraft:block/iron_block"
    base = cuboid((0, 0, 0), (16, 4, 16), {"down": "#iron", "up": "#iron", "north": "#iron", "south": "#iron", "west": "#iron", "east": "#iron"})
    tex["iron"] = "minecraft:block/iron_block"
    walls = [piece for wall in WALLS[:min(stage, 4)] for piece in wall]
    elements = [base] + walls + HANDLES[:max(0, stage - 4)]
    return {"parent": "minecraft:block/block", "textures": tex, "elements": elements}


def facing_variants(model, extra=""):
    out = {}
    for name, rot in FACINGS.items():
        variant = {"model": model}
        if rot:
            variant["y"] = rot
        out[f"facing={name}{extra}"] = variant
    return out


def files():
    out = {}
    a = f"assets/{NS}"
    d = f"data/{NS}"
    for tier in TIERS:
        name = f"{tier}_crucible"
        out[f"{a}/models/block/{name}.json"] = crucible_model(tier)
        variants = {}
        for lit in ("false", "true"):
            variants.update(facing_variants(f"{NS}:block/{name}", f",lit={lit}"))
        out[f"{a}/blockstates/{name}.json"] = {"variants": variants}
        out[f"{a}/items/{name}.json"] = {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}}
        out[f"{d}/loot_table/blocks/{name}.json"] = {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{NS}:blocks/{name}",
        }
    blank_variants = {}
    for stage in range(1, 6):
        out[f"{a}/models/block/crucible_blank_{stage}.json"] = blank_model(stage)
        for name, rot in FACINGS.items():
            variant = {"model": f"{NS}:block/crucible_blank_{stage}"}
            if rot:
                variant["y"] = rot
            blank_variants[f"facing={name},stage={stage}"] = variant
    out[f"{a}/blockstates/crucible_blank.json"] = {"variants": blank_variants}
    # Blank: the iron block plus every part struck so far (walls = strikes 1-4, handles = 5).
    entries = [{"type": "minecraft:item", "name": "minecraft:iron_block"}]
    pools = [{"rolls": 1, "bonus_rolls": 0, "entries": entries}]
    for stage in range(1, 6):
        walls, handles = min(stage, 4), max(0, stage - 4)
        items = [("minecraft:heavy_weighted_pressure_plate", walls)] + ([("minecraft:iron_ingot", handles)] if handles else [])
        for item, count in items:
            pools.append({"rolls": 1, "bonus_rolls": 0,
                          "conditions": [{"condition": "minecraft:block_state_property", "block": f"{NS}:crucible_blank",
                                          "properties": {"stage": str(stage)}}],
                          "entries": [{"type": "minecraft:item", "name": item,
                                       "functions": [{"function": "minecraft:set_count", "count": count}]}]})
    out[f"{d}/loot_table/blocks/crucible_blank.json"] = {"type": "minecraft:block", "pools": pools,
                                                         "random_sequence": f"{NS}:blocks/crucible_blank"}
    # Barrels (plan section 8a, owner addition 11): plain barrel cube; attached, a flange on the crucible's belly and a
    # chute that rests on the crucible's rim (crucible belly at z=-1, neck at z=-2, opening from z=-3 seen from here).
    for tier in BARREL_TIERS:
        name = f"{tier}_barrel"
        tex = {"side": f"{NS}:block/{name}_side", "top": f"{NS}:block/{name}_top", "bottom": f"{NS}:block/{name}_bottom",
               "flange": f"{NS}:block/{name}_flange", "particle": f"{NS}:block/{name}_side"}
        body = cuboid((0, 0, 0), (16, 16, 16), {"down": "#bottom", "up": "#top", "north": "#side", "south": "#side", "west": "#side", "east": "#side"})
        out[f"{a}/models/block/{name}.json"] = {"parent": "minecraft:block/block", "textures": tex, "elements": [body]}
        out[f"{a}/models/block/{name}_attached.json"] = {"parent": "minecraft:block/block", "textures": tex,
                                                         "elements": [DOCKED_BODY, *DOCK]}
        variants = {}
        for facing, rot in FACINGS.items():
            for attached in ("false", "true"):
                for open_ in ("false", "true"):
                    v = {"model": f"{NS}:block/{name}" + ("_attached" if attached == "true" else "")}
                    if rot:
                        v["y"] = rot
                    variants[f"attached={attached},facing={facing},open={open_}"] = v
        out[f"{a}/blockstates/{name}.json"] = {"variants": variants}
        out[f"{a}/items/{name}.json"] = {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}}
        out[f"{d}/loot_table/blocks/{name}.json"] = {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{NS}:blocks/{name}",
        }
    out[f"{d}/recipe/copper_barrel.json"] = {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"C": "minecraft:copper_ingot", "B": "minecraft:barrel"},
        "pattern": [" C ", "CBC", " C "],
        "result": {"id": f"{NS}:copper_barrel", "count": 1},
    }
    # Reinforced cauldron (owner 30/56): one block, content as state; always full (bucket in, bucket out).
    rc = f"{NS}:block/reinforced_cauldron"
    rc_tex = {"particle": f"{rc}_side", "top": f"{rc}_top", "bottom": f"{rc}_bottom", "side": f"{rc}_side", "inside": f"{rc}_inner"}
    out[f"{a}/models/block/reinforced_cauldron.json"] = {"parent": "minecraft:block/cauldron", "textures": rc_tex}
    contents = {"water": "minecraft:block/water_still", "lava": "minecraft:block/lava_still",
                "powder_snow": "minecraft:block/powder_snow", "extreme": f"{rc}_extreme"}
    # Water and powder snow fill in three levels like Vanilla (owner addition 11: the reinforced cauldron inherits it).
    cauldron_variants = {}
    for content in ("empty", *contents):
        for level in (1, 2, 3):
            if content == "empty":
                model = rc
            elif content in ("water", "powder_snow") and level < 3:
                model = f"{rc}_{content}_level{level}"
            else:
                model = f"{rc}_{content}"
            cauldron_variants[f"content={content},level={level}"] = {"model": model}
    for content, texture in contents.items():
        out[f"{a}/models/block/reinforced_cauldron_{content}.json"] = {
            "parent": "minecraft:block/template_cauldron_full", "textures": dict(rc_tex, content=texture)}
        if content in ("water", "powder_snow"):
            for level in (1, 2):
                out[f"{a}/models/block/reinforced_cauldron_{content}_level{level}.json"] = {
                    "parent": f"minecraft:block/template_cauldron_level{level}", "textures": dict(rc_tex, content=texture)}
    out[f"{a}/blockstates/reinforced_cauldron.json"] = {"variants": cauldron_variants}
    # Like Vanilla's cauldron the item is a flat sprite (the block model has no GUI transform, owner image 17).
    out[f"{a}/models/item/reinforced_cauldron.json"] = {"parent": "minecraft:item/generated",
                                                        "textures": {"layer0": f"{NS}:item/reinforced_cauldron"}}
    out[f"{a}/items/reinforced_cauldron.json"] = {"model": {"type": "minecraft:model", "model": f"{NS}:item/reinforced_cauldron"}}
    out[f"{d}/loot_table/blocks/reinforced_cauldron.json"] = {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:reinforced_cauldron"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{NS}:blocks/reinforced_cauldron",
    }
    # Village field kitchen barrel (owner 46: raw meat beside the crucible).
    def meat(item, lo, hi, weight):
        return {"type": "minecraft:item", "name": item, "weight": weight,
                "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
    out[f"{d}/loot_table/chests/village_field_kitchen.json"] = {
        "type": "minecraft:chest",
        "pools": [{"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "bonus_rolls": 0, "entries": [
            meat("minecraft:beef", 1, 3, 3), meat("minecraft:porkchop", 1, 3, 3), meat("minecraft:mutton", 1, 3, 3),
            meat("minecraft:chicken", 1, 2, 2), meat("minecraft:rabbit", 1, 2, 1), meat("minecraft:potato", 2, 5, 2)]}],
        "random_sequence": f"{NS}:chests/village_field_kitchen",
    }
    # Tags.
    out[f"{d}/tags/block/heat_source/medium.json"] = {"values": ["minecraft:campfire", "minecraft:soul_campfire", "minecraft:magma_block"]}
    out[f"{d}/tags/block/heat_source/high.json"] = {"values": ["minecraft:lava_cauldron"]}
    out[f"{d}/tags/block/heat_source/extreme.json"] = {"values": []}
    out[f"{d}/tags/fluid/extreme_heat.json"] = {"values": []}
    out[f"{d}/tags/item/warmable_food.json"] = {"values": [f"minecraft:{i}" for i in WARMABLE]}
    out[f"{d}/tags/item/needs_extreme_heat.json"] = {"values": ["minecraft:ancient_debris"]}
    out[f"{d}/tags/item/crucible_walls.json"] = {"values": ["minecraft:heavy_weighted_pressure_plate"]}
    out[f"{d}/tags/item/crucible_handles.json"] = {"values": ["minecraft:iron_ingot"]}
    out[f"{d}/tags/item/upgrade_reinforced.json"] = {"values": ["minecraft:diamond"]}
    out[f"{d}/tags/item/upgrade_netherite.json"] = {"values": ["minecraft:netherite_ingot"]}
    pickaxe = [f"{NS}:{t}_crucible" for t in TIERS] + [f"{NS}:crucible_blank"] + [f"{NS}:{t}_barrel" for t in BARREL_TIERS]         + [f"{NS}:reinforced_cauldron"]
    out["data/minecraft/tags/block/mineable/pickaxe.json"] = {"values": pickaxe}
    out["data/minecraft/tags/block/needs_stone_tool.json"] = {"values": pickaxe}
    return out


def render(data):
    return (json.dumps(data, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    wanted = {OUTPUT / path: render(data) for path, data in files().items()}
    stale = [p for p in OUTPUT.rglob("*.json") if p not in wanted] if OUTPUT.exists() else []
    if args.check:
        bad = [p for p, b in wanted.items() if not p.exists() or p.read_bytes().replace(CRLF, LF) != b] + stale
        for p in bad:
            print(f"out of date: {p.relative_to(MODULE)}")
        return 1 if bad else 0
    for p in stale:
        p.unlink()
    for p, b in wanted.items():
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(b)
    print(f"wrote {len(wanted)} files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
