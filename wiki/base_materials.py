"""
Raw materials of a recipe in total (owner 2026-10-02): every ingredient of a recipe is resolved
through the recipes that make it until only basic materials are left - ingots, logs and stems,
and everything that has no recipe at all (sugar cane, honeycomb, cobblestone, diamonds, string ...).

The rules, each one deliberate:

* Base materials: every ``*_ingot`` (owner: "bis Barren"), every member of ``#minecraft:logs``,
  the gathered drops in ``GATHERED`` (leather, diamond, redstone ...) and every item without a usable
  recipe. Smelting an ore is never the way to an item (ores drop their resource when mined). Vanilla items (``minecraft:``) are only resolved through
  vanilla recipes - a mod recipe that also makes a vanilla item (cracked diamond -> diamond, copper
  axe -> iron axe) is a second source, not the way the item is normally made.
* Cycles and way-back recipes: a recipe whose ingredient is already being resolved higher up in the
  branch is dropped (diamond <- diamond block <- diamond, nugget <- ingot <- nugget). A recipe that
  only works through such a dropped step is dropped as well, so a storage block never becomes the
  "raw material" of its own ingot. An item left with no recipe this way counts as a base material.
* Several recipes for one item: the first one by station (crafting table, furnace, stonecutter,
  smithing table), then by recipe id - deterministic, so the generated data does not flip.
* Yield: every amount is a share of one craft (``Fraction``), e.g. a stick is a quarter plank pair:
  1 stick = 1/2 plank = 1/8 log. Fuel is not counted.
* Tags (any planks) are computed with one example member (oak first); the recipe notes which tags.
* Dye/transmute recipes, special recipes without a fixed result and the mod's book upgrades are
  skipped: they change components of an item instead of making it.
"""

from __future__ import annotations

import json
from fractions import Fraction

SHAPED = ("minecraft:crafting_shaped", "simplebuilding:backpack_upgrade", "simplebuilding:reinforced_bundle")
SHAPELESS = ("minecraft:crafting_shapeless", "simplebuilding:enchanted_shapeless")
COOKING = ("minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking")
SMITHING = ("minecraft:smithing_transform", "simplebuilding:count_based_smithing")
STONECUTTING = ("minecraft:stonecutting",)
# Station order for choosing between several recipes of one item.
PRIORITY = {**{t: 0 for t in SHAPED + SHAPELESS}, "minecraft:smelting": 1, "minecraft:blasting": 2,
            "minecraft:smoking": 2, "minecraft:campfire_cooking": 2, "minecraft:stonecutting": 3,
            **{t: 4 for t in SMITHING}}
MAX_DEPTH = 24
# Gathered or mined drops that also have a compacting or smelting recipe: they stay what they are
# (leather is not "four rabbit hides", a diamond not "a smelted ore").
GATHERED = frozenset("minecraft:" + name for name in (
    "leather", "diamond", "emerald", "coal", "lapis_lazuli", "redstone", "quartz", "amethyst_shard",
    "netherite_scrap", "glowstone_dust", "slime_ball", "honeycomb", "string", "wheat", "bone", "clay_ball"))


def with_namespace(ref: str) -> str:
    """stick -> minecraft:stick, #planks -> #minecraft:planks (the vanilla payload strips the namespace)."""
    if ref.startswith("#"):
        return ref if ":" in ref else "#minecraft:" + ref[1:]
    return ref if ":" in ref else "minecraft:" + ref


def parse_vanilla_payload(text: str) -> tuple[list[dict], dict[str, list[str]]]:
    """Recipes and item tags out of a wiki/data/vanilla-<line>.js file, with namespaces restored."""
    start = text.index("= {", text.index("window.VANILLA_RECIPES[")) + 2
    data = json.loads(text[start:text.rindex("}") + 1])
    tags = {with_namespace(k): [with_namespace(v) for v in values] for k, values in data.get("tags", {}).items()}
    recipes = []
    for raw in data.get("recipes", []):
        recipe = dict(raw)
        recipe["type"] = with_namespace(recipe.get("type", ""))
        result = dict(recipe.get("result") or {})
        if result.get("id"):
            result["id"] = with_namespace(result["id"])
        recipe["result"] = result
        if "key" in recipe:
            recipe["key"] = {k: [with_namespace(v) for v in vs] for k, vs in recipe["key"].items()}
        if "ingredientGroups" in recipe:
            recipe["ingredientGroups"] = [[with_namespace(v) for v in vs] for vs in recipe["ingredientGroups"]]
        if "slots" in recipe:
            recipe["slots"] = {k: [with_namespace(v) for v in vs] for k, vs in recipe["slots"].items()}
        recipe["id"] = with_namespace(recipe.get("id", ""))
        recipes.append(recipe)
    return recipes, tags


def mod_item_tags(tag_entries: list[dict]) -> dict[str, list[str]]:
    """collect_tags() output -> {"#ns:path": [ids or #tags]} for item tags only."""
    out = {}
    for tag in tag_entries:
        namespace, path = tag["id"].split(":", 1)
        if not path.startswith("item/"):
            continue
        out["#" + namespace + ":" + path[len("item/"):]] = [v["id"] for v in tag.get("values", []) if v.get("id")]
    return out


def recipe_inputs(recipe: dict) -> list[tuple[list[str], int]] | None:
    """[(alternatives, count)] consumed by one craft, or None for recipes that make nothing new."""
    kind = recipe.get("type")
    if kind in SHAPED and recipe.get("pattern"):
        counts: dict[str, int] = {}
        for row in recipe["pattern"]:
            for char in row:
                if char != " ":
                    counts[char] = counts.get(char, 0) + 1
        return [(recipe.get("key", {}).get(char, []), n) for char, n in sorted(counts.items()) if recipe.get("key", {}).get(char)]
    if kind in SHAPELESS:
        return [(group, 1) for group in recipe.get("ingredientGroups", []) if group]
    slots = recipe.get("slots") or {}
    if kind in COOKING or kind in STONECUTTING:
        return [(slots["ingredient"], 1)] if slots.get("ingredient") else None
    if kind in SMITHING:
        out = []
        for slot, count_key in (("template", None), ("base", "base_count"), ("addition", "addition_count")):
            if slots.get(slot):
                out.append((slots[slot], int(recipe.get(count_key, 1) or 1) if count_key else 1))
        return out or None
    return None


class Resolver:
    """Resolves items to base materials over a pool of mod and vanilla recipes."""

    def __init__(self, mod_recipes: list[dict], vanilla_recipes: list[dict], tags: dict[str, list[str]]):
        self.tags = tags
        self.by_result: dict[str, list[dict]] = {}
        for recipe in list(vanilla_recipes) + list(mod_recipes):
            result = (recipe.get("result") or {}).get("id")
            if not result or result.startswith("#") or (recipe.get("result") or {}).get("fromInput"):
                continue
            if recipe.get("type") not in PRIORITY or recipe_inputs(recipe) is None:
                continue
            if recipe["type"] in COOKING and any(ref.endswith("_ore") for ref in (recipe.get("slots") or {}).get("ingredient", [])):
                continue  # ore smelting: a by-route, ores drop their resource
            # Vanilla items only through vanilla recipes (see the module docstring).
            if result.startswith("minecraft:") and not recipe.get("id", "").startswith("minecraft:"):
                continue
            self.by_result.setdefault(result, []).append(recipe)
        for recipes in self.by_result.values():
            recipes.sort(key=lambda r: (PRIORITY[r["type"]], r.get("id", "")))
        self.logs = set(self.members("#minecraft:logs"))
        self.clean: dict[str, dict[str, Fraction]] = {}

    def members(self, tag: str, seen=None) -> list[str]:
        seen = seen or set()
        out = []
        for value in self.tags.get(tag, []):
            if value.startswith("#"):
                if value not in seen:
                    seen.add(value)
                    out += [m for m in self.members(value, seen) if m not in out]
            elif value not in out:
                out.append(value)
        return out

    def example(self, ref: str) -> str | None:
        """An id for an ingredient reference: the item itself, or a tag's example member (oak first)."""
        if not ref.startswith("#"):
            return ref
        members = self.members(ref)
        for member in members:
            if member.startswith("minecraft:oak_"):
                return member
        return members[0] if members else None

    def is_base(self, item: str) -> bool:
        return item.endswith("_ingot") or item in self.logs or item in GATHERED

    def resolve(self, item: str, path: tuple = (), depth: int = 0):
        """
        (materials {id: Fraction} for one item, taint). The taint names the items higher up in the branch
        whose cycle this result relies on: an item whose every recipe runs into one of them falls back to
        being a base material, which is only right as long as that ancestor is not the item being solved.
        """
        if self.is_base(item) or depth > MAX_DEPTH:
            return {item: Fraction(1)}, frozenset()
        if item in self.clean:
            return self.clean[item], frozenset()
        blockers: set[str] = set()
        chosen = None
        for recipe in self.by_result.get(item, []):
            got = self.craft(recipe, path + (item,), depth)
            if got[0] is None:
                blockers |= got[1]
                continue
            materials, taint = got
            if item in taint:
                blockers |= taint - {item}
                continue  # only works through the item itself: a way-back recipe (block -> ingot)
            yield_ = Fraction(int(recipe["result"].get("count", 1) or 1))
            candidate = ({k: v / yield_ for k, v in materials.items()}, taint)
            if not taint:
                chosen = candidate
                break
            if chosen is None:
                chosen = candidate
        if chosen is None:
            return {item: Fraction(1)}, frozenset(blockers - {item})
        if not chosen[1]:
            self.clean[item] = chosen[0]
        return chosen

    def craft(self, recipe: dict, path: tuple, depth: int):
        """
        (materials, taint) of one craft of a recipe, or (None, blockers) when an ingredient is already
        being resolved higher up in the branch.
        """
        totals: dict[str, Fraction] = {}
        taint: set[str] = set()
        for alternatives, count in recipe_inputs(recipe) or []:
            ingredient = self.example(alternatives[0]) if alternatives else None
            if ingredient is None:
                return None, frozenset()
            if ingredient in path:
                return None, frozenset({ingredient})
            materials, sub_taint = self.resolve(ingredient, path, depth + 1)
            taint |= sub_taint
            for key, value in materials.items():
                totals[key] = totals.get(key, Fraction(0)) + value * count
        return totals, frozenset(taint)

    def for_recipe(self, recipe: dict) -> dict | None:
        """The wiki's baseMaterials block for one recipe, or None when it makes nothing new."""
        inputs = recipe_inputs(recipe)
        if not inputs or recipe.get("type") not in PRIORITY or (recipe.get("result") or {}).get("fromInput"):
            return None
        result = recipe["result"].get("id")
        got = self.craft(recipe, (result,) if result else (), 0)
        if got[0] is None:
            return None
        totals = got[0]
        tags = sorted({alts[0] for alts, _ in inputs if alts and alts[0].startswith("#")})
        materials = [{"id": key, "count": number(value)} for key, value in
                     sorted(totals.items(), key=lambda kv: (-kv[1], kv[0])) if value > 0]
        out = {"yield": int(recipe["result"].get("count", 1) or 1), "materials": materials}
        if tags:
            out["tagExamples"] = tags
        return out


def number(value: Fraction):
    """An int when whole, else a float rounded to three places (the page shows two)."""
    return int(value) if value.denominator == 1 else round(float(value), 3)


def annotate(recipes: list[dict], resolver: Resolver) -> None:
    """Adds baseMaterials to every recipe of the list that makes something (in place)."""
    for recipe in recipes:
        block = resolver.for_recipe(recipe)
        if block:
            recipe["baseMaterials"] = block
        else:
            recipe.pop("baseMaterials", None)
