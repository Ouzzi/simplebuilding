"""Resonance field beside the recipe book (inventory), round 2 (owner 2026-10-09): vanilla style.

Writes three GUI sprites:
- resonance_heart.png: 9x9 heart with cobblestone inside (round 4) and a dark outline;
  resonance_heart_max.png: the same heart in diamond tones, shown at maximum resonance.
- resonance_field.png / resonance_field_highlighted.png: the frame of the vanilla recipe-book button (20x18, rounded
  1 px black outline, white bevel top/left, dark bevel bottom/right, fill #C6C6C6; the highlighted one in the blue
  hover colours) without the book, as nine-slice sprites (border 3) so the field can grow with its text.

Run: python3.12 tools/textures/resonance_heart_2026_10_09.py
"""
import json
from pathlib import Path
from PIL import Image

# Round 4 (owner N29, 2026-10-09): back to 9x9 (the round 2 silhouette), still cobblestone inside (light stones,
# dark mortar) with a dark outline. At maximum resonance the same heart in diamond tones (resonance_heart_max).
HEART = [
    "..##.##..",
    ".#lw#wl#.",
    "#wlmdlmd#",
    "#mddlmdl#",
    "#lwmdddm#",
    ".#dmlwd#.",
    "..#dlm#..",
    "...#d#...",
    "....#....",
]
HEART_COLOURS = {  # cobblestone greys: dark outline, top light, light stone, mid stone, mortar
    "#": (0x1E, 0x1E, 0x1E, 255),
    "w": (0xBE, 0xBE, 0xBE, 255),
    "l": (0x9C, 0x9C, 0x9C, 255),
    "m": (0x7A, 0x7A, 0x7A, 255),
    "d": (0x52, 0x52, 0x52, 255),
}
HEART_MAX_COLOURS = {  # Vanilla diamond item tones: dark teal outline, glint, light, mid, facet shadow
    "#": (0x0E, 0x3F, 0x36, 255),
    "w": (0xD5, 0xFF, 0xF6, 255),
    "l": (0x4A, 0xED, 0xD9, 255),
    "m": (0x20, 0xC5, 0xB5, 255),
    "d": (0x11, 0x72, 0x7A, 255),
}
FIELD = [
    "..oooooooooooooooo..",
    ".oHHHHHHHHHHHHHHHHo.",
    "oHffffffffffffffffSo",
] + ["oHffffffffffffffffSo"] * 13 + [
    ".oSSSSSSSSSSSSSSSSo.",
    "..oooooooooooooooo..",
]
FIELD_COLOURS = {  # vanilla recipe_book/button and button_highlighted frame colours
    False: {"o": (0, 0, 0, 255), "H": (255, 255, 255, 255), "f": (0xC6, 0xC6, 0xC6, 255), "S": (0x55, 0x55, 0x55, 255)},
    True: {"o": (0x00, 0x07, 0x3E, 255), "H": (255, 255, 255, 255), "f": (0x88, 0x92, 0xC9, 255), "S": (0x34, 0x3E, 0x75, 255)},
}
OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/simplebuilding/textures/gui/sprites"
NINE_SLICE = {"gui": {"scaling": {"type": "nine_slice", "width": 20, "height": 18, "border": 3}}}


def paint(rows, colours) -> Image.Image:
    img = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colours:
                img.putpixel((x, y), colours[ch])
    return img


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    paint(HEART, HEART_COLOURS).save(OUT / "resonance_heart.png")
    paint(HEART, HEART_MAX_COLOURS).save(OUT / "resonance_heart_max.png")
    for highlighted, name in ((False, "resonance_field"), (True, "resonance_field_highlighted")):
        paint(FIELD, FIELD_COLOURS[highlighted]).save(OUT / f"{name}.png")
        (OUT / f"{name}.png.mcmeta").write_text(json.dumps(NINE_SLICE, indent=2) + "\n", encoding="utf-8")
    print(OUT)


if __name__ == "__main__":
    main()
