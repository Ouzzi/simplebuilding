"""Generate the village field kitchen structures (plan section 11, owner answers 44-46).

One 3x3x5 piece per village type: the jigsaw on the street side (copied from Vanilla's
plains_accessory_1: building_entrance, aligned, structure_void), an unlit campfire with an iron
crucible on top holding cooked food, a barrel with raw meat (loot table) and a seat. --check compares
without writing. Uses tools/nbt_min.py from the repository root.
"""
import argparse
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(MODULE.parents[1] / "tools"))
import nbt_min as N  # noqa: E402

OUT = MODULE / "shared/resources/data/simplelib/structure/village"
DATA_VERSION = 5023
# type: ground block, ground properties, seat stairs, crucible contents
TYPES = {
    "plains": ("minecraft:grass_block", {"snowy": "false"}, "minecraft:oak_stairs", [("minecraft:bread", 2), ("minecraft:cooked_beef", 1)]),
    "desert": ("minecraft:sand", {}, "minecraft:smooth_sandstone_stairs", [("minecraft:baked_potato", 3), ("minecraft:cooked_rabbit", 1)]),
    "savanna": ("minecraft:grass_block", {"snowy": "false"}, "minecraft:acacia_stairs", [("minecraft:cooked_mutton", 2), ("minecraft:bread", 1)]),
    "snowy": ("minecraft:snow_block", {}, "minecraft:spruce_stairs", [("minecraft:cooked_porkchop", 2), ("minecraft:baked_potato", 2)]),
    "taiga": ("minecraft:podzol", {"snowy": "false"}, "minecraft:spruce_stairs", [("minecraft:cooked_salmon", 2), ("minecraft:cooked_cod", 1)]),
}


def s(v):
    return (N.STRING, v)


def i(v):
    return (N.INT, v)


def compound(d):
    return (N.COMPOUND, d)


def state(name, props=None):
    out = {"Name": s(name)}
    if props:
        out["Properties"] = compound({k: s(v) for k, v in props.items()})
    return out


def build(kind):
    ground, ground_props, seat, food = TYPES[kind]
    palette = []

    def pid(name, props=None):
        entry = state(name, props)
        if entry not in palette:
            palette.append(entry)
        return palette.index(entry)

    blocks = []

    def put(pos, name, props=None, nbt=None):
        b = {"pos": (N.LIST, (N.INT, list(pos))), "state": i(pid(name, props))}
        if nbt:
            b["nbt"] = compound(nbt)
        blocks.append(compound(b)[1])

    # ground layer (jigsaw at 0,0,0 faces the street to the west, like Vanilla accessories)
    for x in range(3):
        for z in range(5):
            if (x, z) == (0, 0):
                continue
            put((x, 0, z), ground, ground_props)
    put((0, 0, 0), "minecraft:jigsaw", {"orientation": "west_up"}, {
        "id": s("minecraft:jigsaw"), "name": s("minecraft:building_entrance"), "target": s("minecraft:building_entrance"),
        "pool": s("minecraft:village/" + kind + "/streets"), "final_state": s("minecraft:structure_void"),
        "joint": s("aligned")})
    contents = [compound({"Slot": i(n), "Item": compound({"id": s(item), "count": i(count)}), "Count": i(count)})[1]
                for n, (item, count) in enumerate(food)]
    objects = {
        (1, 1, 2): ("minecraft:campfire", {"lit": "false", "signal_fire": "false", "waterlogged": "false", "facing": "west"}, None),
        (1, 2, 2): ("simplelib:iron_crucible", {"facing": "west", "lit": "false"},
                    {"id": s("simplelib:crucible"), "Contents": (N.LIST, (N.COMPOUND, contents))}),
        (1, 1, 1): ("minecraft:barrel", {"facing": "up", "open": "false"},
                    {"id": s("minecraft:barrel"), "LootTable": s("simplelib:chests/village_field_kitchen")}),
        (1, 1, 3): (seat, {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"}, None),
    }
    for x in range(3):
        for y in (1, 2):
            for z in range(5):
                name, props, nbt = objects.get((x, y, z), ("minecraft:air", None, None))
                put((x, y, z), name, props, nbt)
    return {
        "size": (N.LIST, (N.INT, [3, 3, 5])),
        "entities": (N.LIST, (N.COMPOUND, [])),
        "blocks": (N.LIST, (N.COMPOUND, blocks)),
        "palette": (N.LIST, (N.COMPOUND, palette)),
        "DataVersion": i(DATA_VERSION),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    bad = 0
    for kind in TYPES:
        path = OUT / kind / "field_kitchen.nbt"
        root = build(kind)
        if args.check:
            if not path.exists() or N.load(path) != root:
                print(f"out of date: {path.relative_to(MODULE)}")
                bad += 1
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        N.dump(root, path)
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
