"""Generate SimpleBuilding's 26.3 crucible resources (plan PLAN-CRUCIBLE-P5-2026-10-05); --check compares only.

Writes into mc26_3/overlay/resources: block states/models/item definitions for the Enderite crucible and barrel
(child models of SimpleLib's, textures swapped), soul lava, the buckets (copper buckets select their texture by the
simplebuilding:oxidation component), and the hand-written data that datagen does not cover: fluid tags and
SimpleLib tag additions. Recipes, loot tables and block tags come from datagen.
"""

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "mc26_3/overlay/resources"
NS = "simplebuilding"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
CRLF, LF = b"\r\n", b"\n"


LIB_MODELS = ROOT / "modules/simplelib/generated/resources/assets/simplelib/models/block"


def lib_model(name, textures):
    """A copy of SimpleLib's model with SB textures (no cross-namespace parent: the wiki renderer resolves only SB)."""
    model = json.loads((LIB_MODELS / f"{name}.json").read_text(encoding="utf-8"))
    model["textures"] = textures
    return model


def item_model(texture):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{texture}"}}


def model_ref(model):
    return {"type": "minecraft:model", "model": model}


def files():
    a, out = f"assets/{NS}", {}
    # Enderite crucible: SimpleLib's crucible model with Enderite textures.
    tex = {k: f"{NS}:block/enderite_crucible_{k}" for k in ("side", "top", "bottom", "inner", "handle")}
    tex["particle"] = tex["side"]
    out[f"{a}/models/block/enderite_crucible.json"] = lib_model("iron_crucible", tex)
    variants = {}
    for lit in ("false", "true"):
        for facing, rot in FACINGS.items():
            v = {"model": f"{NS}:block/enderite_crucible"}
            if rot:
                v["y"] = rot
            variants[f"facing={facing},lit={lit}"] = v
    out[f"{a}/blockstates/enderite_crucible.json"] = {"variants": variants}
    out[f"{a}/items/enderite_crucible.json"] = {"model": model_ref(f"{NS}:block/enderite_crucible")}
    # Enderite barrel: SimpleLib's barrel models (plain + flange when attached).
    btex = {"side": f"{NS}:block/enderite_barrel_side", "top": f"{NS}:block/enderite_barrel_top",
            "bottom": f"{NS}:block/enderite_barrel_bottom", "flange": f"{NS}:block/enderite_barrel_flange",
            "particle": f"{NS}:block/enderite_barrel_side"}
    out[f"{a}/models/block/enderite_barrel.json"] = lib_model("copper_barrel", btex)
    out[f"{a}/models/block/enderite_barrel_attached.json"] = lib_model("copper_barrel_attached", btex)
    variants = {}
    for facing, rot in FACINGS.items():
        for attached in ("false", "true"):
            for open_ in ("false", "true"):
                v = {"model": f"{NS}:block/enderite_barrel" + ("_attached" if attached == "true" else "")}
                if rot:
                    v["y"] = rot
                variants[f"attached={attached},facing={facing},open={open_}"] = v
    out[f"{a}/blockstates/enderite_barrel.json"] = {"variants": variants}
    out[f"{a}/items/enderite_barrel.json"] = {"model": model_ref(f"{NS}:block/enderite_barrel")}
    # Soul lava: like Vanilla lava, only a particle texture (the fluid renderer draws it).
    out[f"{a}/models/block/soul_lava.json"] = {"textures": {"particle": f"{NS}:block/soul_lava_still"}}
    out[f"{a}/blockstates/soul_lava.json"] = {"variants": {"": {"model": f"{NS}:block/soul_lava"}}}
    # Buckets.
    for name in ("soul_lava_bucket", "enderite_bucket", "enderite_water_bucket", "enderite_lava_bucket", "enderite_soul_lava_bucket"):
        # Owner N21/N28: the filled Enderite items hold one bucket of two and show the half texture
        # (tools/textures/enderite_bucket_fill_2026_10_09.py); the _full items (two buckets) the full one.
        half = name.startswith("enderite_") and name != "enderite_bucket"
        out[f"{a}/models/item/{name}.json"] = item_model(name + "_half" if half else name)
        out[f"{a}/items/{name}.json"] = {"model": model_ref(f"{NS}:item/{name}")}
        if half:
            out[f"{a}/models/item/{name}_full.json"] = item_model(name)
            out[f"{a}/items/{name}_full.json"] = {"model": model_ref(f"{NS}:item/{name}_full")}
    for name in ("copper_bucket", "copper_water_bucket", "copper_lava_bucket"):
        for stage in range(4):
            out[f"{a}/models/item/{name}_{stage}.json"] = item_model(f"{name}_{stage}")
        out[f"{a}/items/{name}.json"] = {"model": {
            "type": "minecraft:select", "property": "minecraft:component", "component": f"{NS}:oxidation",
            "cases": [{"when": stage, "model": model_ref(f"{NS}:item/{name}_{stage}")} for stage in (1, 2, 3)],
            "fallback": model_ref(f"{NS}:item/{name}_0")}}
    # Data datagen does not write: fluid tags (soul lava swims and burns like lava, crucible heat extreme) and
    # SimpleLib's tags (extreme-heat recipes, iron rod as crucible handle).
    out["data/minecraft/tags/fluid/lava.json"] = {"values": [f"{NS}:soul_lava", f"{NS}:flowing_soul_lava"]}
    out["data/simplelib/tags/fluid/extreme_heat.json"] = {"values": [f"{NS}:soul_lava", f"{NS}:flowing_soul_lava"]}
    out["data/simplelib/tags/item/needs_extreme_heat.json"] = {"values": [f"{NS}:layered_raw_enderite", f"{NS}:cracked_diamond"]}
    out["data/simplelib/tags/item/crucible_handles.json"] = {"values": [f"{NS}:iron_rod"]}
    return out


def render(data):
    return (json.dumps(data, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    wanted = {OUT / path: render(data) for path, data in files().items()}
    if args.check:
        bad = [p for p, b in wanted.items() if not p.exists() or p.read_bytes().replace(CRLF, LF) != b]
        for p in bad:
            print(f"out of date: {p.relative_to(ROOT)}")
        return 1 if bad else 0
    for p, b in wanted.items():
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(b)
    print(f"wrote {len(wanted)} files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
