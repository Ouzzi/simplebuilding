"""Generate SimpleLib's Minecraft 26.3 resources; --check compares content without writing.

SimpleLib ships the shared materials (items, recipes on request, common tags), the warmable-food tag and the hammer
tags. The crucibles, barrels and the reinforced cauldron moved to Simple Crucibles (split stage 5).
"""

import argparse
import json
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
OUTPUT = MODULE / "generated/resources"
NS = "simplelib"
CRLF, LF = b"\r\n", b"\n"

# Owner 37: everything that is eaten or served warm.
WARMABLE = (
    "cooked_beef", "cooked_porkchop", "cooked_chicken", "cooked_mutton", "cooked_rabbit",
    "cooked_cod", "cooked_salmon", "baked_potato", "bread", "pumpkin_pie",
    "mushroom_stew", "rabbit_stew", "beetroot_soup", "suspicious_stew",
)


def files():
    out = {}
    d = f"data/{NS}"
    out[f"{d}/tags/item/warmable_food.json"] = {"values": [f"minecraft:{i}" for i in WARMABLE]}
    # In-world upgrades (InWorldUpgrades): hammers of any mod (a hammer mod fills it) run every upgrade.
    out[f"{d}/tags/item/hammers.json"] = {"values": []}
    out[f"{d}/tags/item/upgrade_tools.json"] = {"values": [f"#{NS}:hammers"]}
    materials(out)
    warped_wart(out)
    out["data/minecraft/tags/block/mineable/pickaxe.json"] = {"values": [f"{NS}:{b}" for b in MATERIAL_BLOCKS]}
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
                       "condition": {"type": "minecraft:survives_explosion"}}],
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


def warped_wart(out):
    """Warped wart (owner N71): the plant (4 ages, like nether wart: ages 1 and 2 share a model), its item, loot table and
    the 9 <-> block recipes with Vanilla's warped wart block."""
    a = f"assets/{NS}"
    d = f"data/{NS}"
    for stage in range(3):
        out[f"{a}/models/block/warped_wart_stage{stage}.json"] = {
            "parent": "minecraft:block/crop", "textures": {"crop": f"{NS}:block/warped_wart_stage{stage}"}}
    out[f"{a}/blockstates/warped_wart.json"] = {"variants": {
        f"age={age}": {"model": f"{NS}:block/warped_wart_stage{(0, 1, 1, 2)[age]}"} for age in range(4)}}
    out[f"{a}/models/item/warped_wart.json"] = {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/warped_wart"}}
    out[f"{a}/items/warped_wart.json"] = {"model": {"type": "minecraft:model", "model": f"{NS}:item/warped_wart"}}
    # Same drops as nether wart (26.3 loot format, copied from Vanilla): 1 when young, 2-4 (+ fortune) when fully grown.
    mature = {"type": "minecraft:match_block", "blocks": f"{NS}:warped_wart", "state": {"age": "3"}}
    out[f"{d}/loot_table/blocks/warped_wart.json"] = {
        "type": "minecraft:block",
        "pools": [{"entries": [{"type": "minecraft:item", "name": f"{NS}:warped_wart", "modifier": [
            {"type": "minecraft:set_count", "condition": mature, "count": {"type": "minecraft:uniform", "min": 2, "max": 4}},
            {"type": "minecraft:apply_bonus", "condition": mature, "enchantment": "minecraft:fortune",
             "formula": "minecraft:uniform_bonus_count", "parameters": {"bonusMultiplier": 1}}]}],
            "modifier": {"type": "minecraft:explosion_decay"}, "rolls": 1}],
        "random_sequence": f"{NS}:blocks/warped_wart",
    }
    # Brewing (26.3, data driven): water bottle + warped wart = warped potion, in all three bottles like Vanilla's awkward.
    for container in ("potion", "splash_potion", "lingering_potion"):
        out[f"{d}/recipe/brewing/{container}_water_warped_wart.json"] = {
            "type": "minecraft:brewing",
            "input": {"item": f"minecraft:{container}", "potion_contents": {"potions": "minecraft:water"}},
            "output": {"components": {"minecraft:potion_contents": {"potion": f"{NS}:warped"}}, "id": f"minecraft:{container}"},
            "reagent": {"item": f"{NS}:warped_wart"}}
    block = "minecraft:warped_wart_block"
    recipes = {
        "warped_wart_block": ("building_blocks", {"type": "minecraft:crafting_shaped", "category": "building",
                              "key": {"#": f"{NS}:warped_wart"}, "pattern": ["###", "###", "###"], "result": {"id": block}},
                              [f"{NS}:warped_wart"]),
        "warped_wart_from_warped_wart_block": ("misc", {"type": "minecraft:crafting_shapeless", "category": "misc",
                              "ingredients": [block], "result": {"count": 9, "id": f"{NS}:warped_wart"}}, [block]),
    }
    for rid, (category, recipe, unlocking) in recipes.items():
        out[f"{d}/recipe/{rid}.json"] = recipe
        criteria = {f"has_{i.split(':')[1]}": {"conditions": {"items": [{"items": i}]}, "trigger": "minecraft:inventory_changed"} for i in unlocking}
        criteria["has_the_recipe"] = {"conditions": {"recipes": f"{NS}:{rid}"}, "trigger": "minecraft:recipe_unlocked"}
        out[f"{d}/advancement/recipes/{category}/{rid}.json"] = {
            "parent": "minecraft:recipes/root", "criteria": criteria,
            "requirements": [["has_the_recipe"] + [f"has_{i.split(':')[1]}" for i in unlocking]], "rewards": {"recipes": [f"{NS}:{rid}"]}}


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
