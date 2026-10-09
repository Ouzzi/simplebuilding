"""Resonance field heart (inventory, beside the recipe book): a 9x9 heart in stone colours.

Run: python3.12 tools/textures/resonance_heart_2026_10_09.py
"""
from pathlib import Path
from PIL import Image

ROWS = [
    ".##...##.",
    "#ll#.#mm#",
    "#lmm#mmm#",
    "#mmmmmmd#",
    "#mmmmmdd#",
    ".#mmmdd#.",
    "..#mdd#..",
    "...#d#...",
    "....#....",
]
COLOURS = {  # stone palette: dark outline, light top-left, mid stone, shadow
    "#": (0x3A, 0x3A, 0x3A, 255),
    "l": (0xB4, 0xB4, 0xB4, 255),
    "m": (0x8B, 0x8B, 0x8B, 255),
    "d": (0x6A, 0x6A, 0x6A, 255),
}
OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/simplebuilding/textures/gui/sprites/resonance_heart.png"

def main() -> None:
    img = Image.new("RGBA", (9, 9), (0, 0, 0, 0))
    for y, row in enumerate(ROWS):
        for x, ch in enumerate(row):
            if ch in COLOURS:
                img.putpixel((x, y), COLOURS[ch])
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    print(OUT)

if __name__ == "__main__":
    main()
