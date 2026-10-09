"""Shulker state (owner N17): item models of the 17 Vanilla shulker boxes with an open variant.

Writes mc26_3/overlay/resources/assets/minecraft/items/<color>_shulker_box.json: when the stack carries
simplebuilding:shulker_open the special shulker model is drawn with its lid open (openness 1), otherwise exactly
as Vanilla draws it. Run with --check to compare.
"""
import json
import sys
from pathlib import Path

TARGET = Path("mc26_3/overlay/resources/assets/minecraft/items")
COLORS = ["", "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
          "cyan", "purple", "blue", "brown", "green", "red", "black"]
TRANSFORM = {"left_rotation": [1.0, 0.0, 0.0, 0.0], "right_rotation": [-0.0, -0.0, -0.0, 1.0],
             "scale": [0.9995, 0.9995, 0.9995], "translation": [0.5, 1.4995, 0.5]}


def special(name, texture, openness=None):
    model = {"type": "minecraft:shulker_box", "texture": texture}
    if openness is not None:
        model["openness"] = openness
    return {"type": "minecraft:special", "base": f"minecraft:item/{name}", "model": model, "transformation": TRANSFORM}


def definition(color):
    name = f"{color}_shulker_box" if color else "shulker_box"
    texture = f"minecraft:shulker_{color}" if color else "minecraft:shulker"
    return {"model": {"type": "minecraft:condition", "property": "minecraft:has_component",
                      "component": "simplebuilding:shulker_open",
                      "on_true": special(name, texture, 1.0), "on_false": special(name, texture)}}


def main():
    check = "--check" in sys.argv
    for color in COLORS:
        path = TARGET / f"{color + '_' if color else ''}shulker_box.json"
        text = json.dumps(definition(color), indent=2) + "\n"
        if check:
            assert path.exists() and path.read_text() == text, f"stale item model: {path}"
        else:
            TARGET.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
    print("shulker item models: 17/17 current" if check else "shulker item models: 17 written")


if __name__ == "__main__":
    main()
