"""Generate SimpleLib's Minecraft 26.3 resources; --check compares content without writing.

Crucible models are built from elements: a floor, four walls (two pixels thick) and two handles. The
half-built blank shows the base plus the plates/handles/rim struck so far (stage 1-15, owner N51).
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


# Attached barrel (facing north = towards the crucible). Owner 2026-10-06: smaller than the crucible; owner N12: one
# more pixel smaller on every axis and closer to the crucible - body 13 x 11 x 13 (x 1.5..14.5, y 0..11, z 0..13, the
# crucible's rim is at 14), whole textures scaled onto the smaller faces. The flange closes the last pixel to the
# crucible's belly (z -1..0), a chute rests on the crucible's rim and drops onto the barrel's top.
def full_uv(element):
    for f in element["faces"].values():
        f["uv"] = [0, 0, 16, 16]
        f.pop("cullface", None)
    return element


DOCKED_BODY = full_uv(cuboid((1.5, 0, 0), (14.5, 11, 13), {"down": "#bottom", "up": "#top", "north": "#side", "south": "#side",
                                                           "west": "#side", "east": "#side"}))
DOCKED_BODY["faces"]["down"]["cullface"] = "down"
DOCK = [inside_uv(e) for e in (
    cuboid((3, 3, -1), (13, 9, 0), {"north": "#flange", "up": "#flange", "down": "#flange", "west": "#flange", "east": "#flange"}),
    cuboid((6, 14, -4), (10, 15, 1), {"north": "#flange", "up": "#flange", "down": "#flange", "west": "#flange", "east": "#flange",
                                      "south": "#flange"}),
    cuboid((6, 11, 0), (10, 14, 1), {"north": "#flange", "west": "#flange", "east": "#flange", "south": "#flange"}),
)]


def crucible_model(tier):
    tex = {k: f"{NS}:block/{tier}_crucible_{k}" for k in ("side", "top", "bottom", "inner", "handle")}
    tex["particle"] = tex["side"]
    return {"parent": "minecraft:block/block", "textures": tex, "elements": [*FOOT, FLOOR, *WALLS_FLAT, *HANDLES]}


# Iron crucible build (owner N51): 16 strikes on an iron block, one iron ingot each. Strike 1 turns the iron block into
# the base (foot + floor); strikes 1-8 raise the side plates north/east/south/west (half plate up to y 8, then the full
# one), 9-12 the handles east/west (half = 2 px, then 4 px), 13-16 the rim pieces; the 16th sets the iron crucible.
BLANK_STRIKES = 16
PLATE_STRIKES, HANDLE_STRIKES, RIM_STRIKES = 8, 4, 4


def half_plate(side):
    """The belly piece of a side cut at y 8 (first strike on that side)."""
    piece = json.loads(json.dumps(BELLY[side]))
    piece["to"][1] = 8
    return cuboid(tuple(piece["from"]), tuple(piece["to"]), {f: v["texture"] for f, v in piece["faces"].items()})


def half_handle(i):
    piece = HANDLES[i]
    frm, to = list(piece["from"]), list(piece["to"])
    to[2] = 8
    return cuboid(tuple(frm), tuple(to), {f: v["texture"] for f, v in piece["faces"].items()})


def blank_elements(stage):
    elements = [*FOOT, FLOOR]
    for side in range(4):
        done = stage - 2 * side
        if done >= 2:
            elements.append(BELLY[side])
        elif done == 1:
            elements.append(half_plate(side))
    for i in range(2):
        done = stage - PLATE_STRIKES - 2 * i
        if done >= 2:
            elements.append(HANDLES[i])
        elif done == 1:
            elements.append(half_handle(i))
    elements += NECK[:max(0, stage - PLATE_STRIKES - HANDLE_STRIKES)]
    return elements


def blank_model(stage):
    tex = {k: f"{NS}:block/iron_crucible_{k}" for k in ("side", "top", "bottom", "inner", "handle")}
    tex["particle"] = "minecraft:block/iron_block"
    return {"parent": "minecraft:block/block", "textures": tex, "elements": blank_elements(stage)}


# Crucible upgrades (owner N51): every unit struck in shows as a band of the next tier's material on one side
# (north, east, south, west), half a pixel proud of the belly at y 6-9.
UPGRADE_MATERIAL = {"iron": f"{NS}:block/cracked_diamond_block", "reinforced": "minecraft:block/netherite_block",
                    "netherite": f"{NS}:block/enderite_block"}
BANDS = [
    cuboid((1, 6, 0.5), (15, 9, 1), {"north": "#band", "up": "#band", "down": "#band", "west": "#band", "east": "#band"}),
    cuboid((15, 6, 1), (15.5, 9, 15), {"east": "#band", "up": "#band", "down": "#band", "north": "#band", "south": "#band"}),
    cuboid((1, 6, 15), (15, 9, 15.5), {"south": "#band", "up": "#band", "down": "#band", "west": "#band", "east": "#band"}),
    cuboid((0.5, 6, 1), (1, 9, 15), {"west": "#band", "up": "#band", "down": "#band", "north": "#band", "south": "#band"}),
]


def upgrade_model(tier, units):
    model = crucible_model(tier)
    model["textures"]["band"] = UPGRADE_MATERIAL[tier]
    model["elements"] = model["elements"] + [inside_uv(json.loads(json.dumps(b))) for b in BANDS[:units]]
    return model


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
        for units in range(1, 4):
            out[f"{a}/models/block/{name}_upgrade_{units}.json"] = upgrade_model(tier, units)
        variants = {}
        for lit in ("false", "true"):
            for units in range(4):
                model = f"{NS}:block/{name}" + (f"_upgrade_{units}" if units else "")
                variants.update(facing_variants(model, f",lit={lit},upgrade={units}"))
        out[f"{a}/blockstates/{name}.json"] = {"variants": variants}
        out[f"{a}/items/{name}.json"] = {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}}
        out[f"{d}/loot_table/blocks/{name}.json"] = {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{NS}:blocks/{name}",
        }
    blank_variants = {}
    for stage in range(1, BLANK_STRIKES):
        out[f"{a}/models/block/crucible_blank_{stage}.json"] = blank_model(stage)
        for name, rot in FACINGS.items():
            variant = {"model": f"{NS}:block/crucible_blank_{stage}"}
            if rot:
                variant["y"] = rot
            blank_variants[f"facing={name},stage={stage}"] = variant
    out[f"{a}/blockstates/crucible_blank.json"] = {"variants": blank_variants}
    # Blank: the iron block; CrucibleBlankBlock#getDrops adds one iron ingot per strike done (owner N51).
    pools = [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "minecraft:iron_block"}]}]
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
    # Like Vanilla's cauldron the item is a flat sprite (owner N34 2026-10-11: no 3D block - Vanilla's icon with
    # turquoise accents, tools/textures/crucible_art_v2_2026_10_05.py).
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
    out[f"{d}/tags/item/crucible_walls.json"] = {"values": ["minecraft:iron_ingot"]}
    out[f"{d}/tags/item/crucible_handles.json"] = {"values": ["minecraft:iron_ingot"]}
    out[f"{d}/tags/item/upgrade_reinforced.json"] = {"values": ["minecraft:diamond"]}
    out[f"{d}/tags/item/upgrade_netherite.json"] = {"values": ["minecraft:netherite_ingot"]}
    # Crucible upgrade units for the axe way (owner N51): SimpleLib's own materials, and Vanilla stand-ins because
    # SimpleLib alone offers no way to get them (they only get recipes on request).
    out[f"{d}/tags/item/crucible_upgrade_reinforced.json"] = {"values": [f"{NS}:cracked_diamond", "minecraft:diamond"]}
    out[f"{d}/tags/item/crucible_upgrade_netherite.json"] = {"values": [f"{NS}:netherite_nugget", "minecraft:netherite_scrap"]}
    # In-world upgrades (InWorldUpgrades): hammers of any mod (a hammer mod fills it) run every upgrade.
    out[f"{d}/tags/item/hammers.json"] = {"values": []}
    out[f"{d}/tags/item/upgrade_tools.json"] = {"values": [f"#{NS}:hammers"]}
    materials(out)
    pickaxe = [f"{NS}:{t}_crucible" for t in TIERS] + [f"{NS}:crucible_blank"] + [f"{NS}:{t}_barrel" for t in BARREL_TIERS]         + [f"{NS}:reinforced_cauldron"]
    out["data/minecraft/tags/block/mineable/pickaxe.json"] = {"values": pickaxe + [f"{NS}:{b}" for b in MATERIAL_BLOCKS]}
    out["data/minecraft/tags/block/needs_stone_tool.json"] = {"values": pickaxe}
    return out


# Shared materials (split stage 3d, PLAN-MODULAUFTEILUNG §2.5): group -> items; the blocks have block items.
MATERIAL_GROUPS = {
    "cracked_diamond": ("diamond_pebble", "cracked_diamond", "cracked_diamond_block"),
    "netherite_nugget": ("netherite_nugget",),
    "enderite": ("raw_enderite", "layered_raw_enderite", "enderite_scrap", "enderite_ingot", "enderite_nugget", "enderite_block"),
    "upgrade_templates": ("basic_upgrade_template", "enderite_upgrade_template"),
}
MATERIAL_BLOCKS = ("cracked_diamond_block", "enderite_block")


def material_recipes():
    """(advancement category, recipe id, group, recipe, unlocking items) - the recipes SimpleBuilding had before the
    move, same paths and values (26.3), now only while a mod requested the group (condition material_requested)."""
    m = lambda i: f"{NS}:{i}"
    v = lambda i: f"minecraft:{i}"
    def shaped(key, pattern, result, count=1, **extra):
        r = {"type": "minecraft:crafting_shaped", **extra, "key": key, "pattern": pattern, "result": {"id": result}}
        if count > 1:
            r["result"] = {"count": count, "id": result}
        return r
    def shapeless(ingredients, result, count=1, **extra):
        r = {"type": "minecraft:crafting_shapeless", **extra, "ingredients": ingredients, "result": {"id": result}}
        if count > 1:
            r["result"] = {"count": count, "id": result}
        return r
    def blasting(ingredient, result, ticks, xp, group):
        return {"type": "minecraft:blasting", "cookingtime": ticks, "experience": xp, "group": group,
                "ingredient": ingredient, "result": {"id": result}}
    nine = ["NNN", "NNN", "NNN"]
    return [
        ("misc", "cracked_diamond", "cracked_diamond", shaped({"P": m("diamond_pebble")}, ["PPP", "PPP", "PPP"], m("cracked_diamond")),
         [m("diamond_pebble")]),
        ("misc", "diamond_from_blasting_cracked_diamond", "cracked_diamond",
         blasting(m("cracked_diamond"), v("diamond"), 200, 1.0, "diamond_from_cracked"), [m("cracked_diamond")]),
        ("building_blocks", "cracked_diamond_block", "cracked_diamond",
         shaped({"C": m("cracked_diamond")}, ["CCC", "CCC", "CCC"], m("cracked_diamond_block"), category="building"), [m("cracked_diamond")]),
        ("misc", "cracked_diamond_from_cracked_diamond_block", "cracked_diamond",
         shapeless([m("cracked_diamond_block")], m("cracked_diamond"), 9, group="cracked_diamond_from_block"), [m("cracked_diamond_block")]),
        ("misc", "basic_upgrade_template", "upgrade_templates",
         shaped({"A": v("diamond"), "B": m("basic_upgrade_template"), "C": v("iron_block")}, ["ABA", "ACA", "AAA"],
                m("basic_upgrade_template"), 2), [m("basic_upgrade_template")]),
        ("misc", "netherite_nugget", "netherite_nugget", shapeless([v("netherite_ingot")], m("netherite_nugget"), 9),
         [v("netherite_ingot"), m("netherite_nugget")]),
        ("misc", "netherite_ingot", "netherite_nugget", shaped({"N": m("netherite_nugget")}, nine, v("netherite_ingot")),
         [m("netherite_nugget"), v("netherite_ingot")]),
        ("misc", "enderite_nugget_from_ingot", "enderite", shapeless([m("enderite_ingot")], m("enderite_nugget"), 9),
         [m("enderite_ingot"), m("enderite_nugget")]),
        ("misc", "enderite_ingot_from_nugget", "enderite", shaped({"N": m("enderite_nugget")}, nine, m("enderite_ingot")),
         [m("enderite_nugget"), m("enderite_ingot")]),
        ("misc", "layered_raw_enderite", "enderite", shapeless([m("raw_enderite")] * 4, m("layered_raw_enderite")), [m("raw_enderite")]),
        ("misc", "enderite_scrap_from_blasting_layered_raw_enderite", "enderite",
         blasting(m("layered_raw_enderite"), m("enderite_scrap"), 288000, 10.0, "enderite_scrap"), [m("layered_raw_enderite")]),
        ("misc", "enderite_ingot_from_scrap", "enderite",
         shaped({"D": v("diamond"), "N": v("netherite_ingot"), "S": m("enderite_scrap")}, ["SDS", "DND", "SDS"], m("enderite_ingot")),
         [m("enderite_scrap")]),
        ("decorations", "enderite_block", "enderite", shaped({"#": m("enderite_ingot")}, ["###", "###", "###"], m("enderite_block")),
         [m("enderite_ingot")]),
        ("building_blocks", "enderite_ingot", "enderite",
         shapeless([m("enderite_block")], m("enderite_ingot"), 9, category="building"), [m("enderite_block")]),
        ("misc", "enderite_upgrade_template", "upgrade_templates",
         shaped({"A": v("diamond"), "E": v("end_stone"), "T": m("enderite_upgrade_template")}, ["ATA", "AEA", "AAA"],
                m("enderite_upgrade_template"), 2), [m("enderite_upgrade_template")]),
    ]


def material_condition(group):
    return {"fabric:load_conditions": [{"condition": f"{NS}:material_requested", "material": group}],
            "neoforge:conditions": [{"type": f"{NS}:material_requested", "material": group}]}


def materials(out):
    a = f"assets/{NS}"
    d = f"data/{NS}"
    for group, ids in MATERIAL_GROUPS.items():
        for item in ids:
            if item in MATERIAL_BLOCKS:
                continue
            out[f"{a}/models/item/{item}.json"] = {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{item}"}}
            out[f"{a}/items/{item}.json"] = {"model": {"type": "minecraft:model", "model": f"{NS}:item/{item}"}}
    for block in MATERIAL_BLOCKS:
        out[f"{a}/models/block/{block}.json"] = {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{block}"}}
        out[f"{a}/blockstates/{block}.json"] = {"variants": {"": {"model": f"{NS}:block/{block}"}}}
        out[f"{a}/items/{block}.json"] = {"model": {"type": "minecraft:model", "model": f"{NS}:block/{block}"}}
        out[f"{d}/loot_table/blocks/{block}.json"] = {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{block}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{NS}:blocks/{block}",
        }
    for category, rid, group, recipe, unlocking in material_recipes():
        out[f"{d}/recipe/{rid}.json"] = {**material_condition(group), **recipe}
        criteria = {f"has_{i.split(':')[1]}": {"conditions": {"items": [{"items": i}]}, "trigger": "minecraft:inventory_changed"}
                    for i in unlocking}
        criteria["has_the_recipe"] = {"conditions": {"recipes": f"{NS}:{rid}"}, "trigger": "minecraft:recipe_unlocked"}
        out[f"{d}/advancement/recipes/{category}/{rid}.json"] = {
            **material_condition(group), "parent": "minecraft:recipes/root", "criteria": criteria,
            "requirements": [["has_the_recipe"] + [f"has_{i.split(':')[1]}" for i in unlocking]],
            "rewards": {"recipes": [f"{NS}:{rid}"]}}
    # Common tags (principle 6: other mods use the tag, never the id).
    c = "data/c/tags"
    out[f"{c}/item/ingots/enderite.json"] = {"values": [f"{NS}:enderite_ingot"]}
    out[f"{c}/item/ingots.json"] = {"values": ["#c:ingots/enderite"]}
    out[f"{c}/item/nuggets/enderite.json"] = {"values": [f"{NS}:enderite_nugget"]}
    out[f"{c}/item/nuggets/netherite.json"] = {"values": [f"{NS}:netherite_nugget"]}
    out[f"{c}/item/nuggets.json"] = {"values": ["#c:nuggets/enderite", "#c:nuggets/netherite"]}
    out[f"{c}/item/raw_materials/enderite.json"] = {"values": [f"{NS}:raw_enderite"]}
    out[f"{c}/item/raw_materials.json"] = {"values": ["#c:raw_materials/enderite"]}
    out[f"{c}/item/gems/cracked_diamond.json"] = {"values": [f"{NS}:cracked_diamond"]}
    out[f"{c}/item/gems.json"] = {"values": ["#c:gems/cracked_diamond"]}
    for kind in ("item", "block"):
        out[f"{c}/{kind}/storage_blocks/enderite.json"] = {"values": [f"{NS}:enderite_block"]}
        out[f"{c}/{kind}/storage_blocks/cracked_diamond.json"] = {"values": [f"{NS}:cracked_diamond_block"]}
        out[f"{c}/{kind}/storage_blocks.json"] = {"values": ["#c:storage_blocks/enderite", "#c:storage_blocks/cracked_diamond"]}
    out["data/minecraft/tags/block/needs_iron_tool.json"] = {"values": [f"{NS}:cracked_diamond_block"]}
    out["data/minecraft/tags/block/needs_diamond_tool.json"] = {"values": [f"{NS}:enderite_block"]}
    out["data/minecraft/tags/block/beacon_base_blocks.json"] = {"values": [f"{NS}:enderite_block"]}


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
