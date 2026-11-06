"""Generate Simple Storage's barrel resources (WH26-6: moved from Simple Crucibles' generator); --check compares without writing.

Four barrel tiers: a plain barrel cube (turned by "top" like Vanilla's barrel, open variant with the *_top_open texture) and, attached to a
crucible (Simple Crucibles' block, via SimpleLib's BarrelDock contract), a smaller body with a flange on the crucible's belly and a chute.
"""

import argparse
import json
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
OUTPUT = MODULE / "generated/resources"
NS = "simplestorage"
CRLF, LF = b"\r\n", b"\n"
BARREL_TIERS = ("copper", "reinforced", "netherite", "enderite")
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
# Vanilla's barrel blockstate rotations for its "facing" (here "top", DW03C-4).
TOPS = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "east": {"x": 90, "y": 90}, "south": {"x": 90, "y": 180}, "west": {"x": 90, "y": 270}}

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




def files():
    out = {}
    a = f"assets/{NS}"
    d = f"data/{NS}"
    for tier in BARREL_TIERS:
        name = f"{tier}_barrel"
        tex = {"side": f"{NS}:block/{name}_side", "top": f"{NS}:block/{name}_top", "bottom": f"{NS}:block/{name}_bottom",
               "flange": f"{NS}:block/{name}_flange", "particle": f"{NS}:block/{name}_side"}
        body = cuboid((0, 0, 0), (16, 16, 16), {"down": "#bottom", "up": "#top", "north": "#side", "south": "#side", "west": "#side", "east": "#side"})
        out[f"{a}/models/block/{name}.json"] = {"parent": "minecraft:block/block", "textures": tex, "elements": [body]}
        out[f"{a}/models/block/{name}_attached.json"] = {"parent": "minecraft:block/block", "textures": tex,
                                                         "elements": [DOCKED_BODY, *DOCK]}
        # DW03C-4: open like Vanilla's barrel (the *_top_open texture), loose barrels turned by "top" like Vanilla's "facing".
        out[f"{a}/models/block/{name}_open.json"] = {"parent": "minecraft:block/block", "textures": dict(tex, top=f"{NS}:block/{name}_top_open"),
                                                     "elements": [body]}
        variants = {}
        for facing, rot in FACINGS.items():
            for attached in ("false", "true"):
                for open_ in ("false", "true"):
                    for top, top_rot in TOPS.items():
                        if attached == "true":
                            v = {"model": f"{NS}:block/{name}_attached"}
                            if rot:
                                v["y"] = rot
                        else:
                            v = {"model": f"{NS}:block/{name}" + ("_open" if open_ == "true" else ""), **top_rot}
                        variants[f"attached={attached},facing={facing},open={open_},top={top}"] = v
        out[f"{a}/blockstates/{name}.json"] = {"variants": variants}
        out[f"{a}/items/{name}.json"] = {"model": {"type": "minecraft:model", "model": f"{NS}:block/{name}"}}
        out[f"{d}/loot_table/blocks/{name}.json"] = {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                       "condition": {"type": "minecraft:survives_explosion"}}],
            "random_sequence": f"{NS}:blocks/{name}",
        }
    out[f"{d}/recipe/copper_barrel.json"] = {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"C": "minecraft:copper_ingot", "B": "minecraft:barrel"},
        "pattern": [" C ", "CBC", " C "],
        "result": {"id": f"{NS}:copper_barrel", "count": 1},
    }
    # (minecraft:mineable/pickaxe lists the barrels in shared/resources, with the module's other pickaxe blocks.)
    pickaxe = [f"{NS}:{t}_barrel" for t in BARREL_TIERS]
    out["data/minecraft/tags/block/needs_stone_tool.json"] = {"values": [p for p in pickaxe if "enderite" not in p]}
    # DW03C-4: the barrels are barrels for other mods too (the chest interface mounts on c:barrels).
    out["data/c/tags/block/barrels.json"] = {"replace": False, "values": pickaxe}
    out["data/minecraft/tags/block/needs_diamond_tool.json"] = {"values": [f"{NS}:enderite_barrel"]}
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
