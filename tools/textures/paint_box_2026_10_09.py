"""Paint box (owner 2026-10-09, round 3): a box of paint pots in four tiers, own pixel art.

The lid stands open behind, four paint pots (red, yellow, blue, green) sit in the box, a clasp holds the front. The
tiers differ in material: oak (basic), oak with iron corners and a diamond clasp (reinforced), netherite and
enderite (with purple specks), colours taken from the mod's netherite/enderite backpacks.

Run: python3.12 tools/textures/paint_box_2026_10_09.py [--preview out.png]
"""
from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT_DIRS = [ROOT / "src/main/resources/assets/simplebuilding/textures/item", ROOT / "wiki/assets/textures/item"]

ROWS = [
    "................",
    "..OOOOOOOOOOOO..",
    "..OllllllllllO..",
    "..OmmmmmmmmmmO..",
    "..OmmmmmmmmmmO..",
    ".ORROYYOOBBOGGO.",
    ".ORrDYyDDBbDGgO.",
    ".OrrDyyDDbbDggO.",
    ".OOOOOOOOOOOOOO.",
    ".OcLLLLLLLLLLcO.",
    ".OMMMMMMMMMMMMO.",
    ".OMMMMMSSMMMMMO.",
    ".OMMMMMssMMMMMO.",
    ".OcDDDDDDDDDDcO.",
    "..OOOOOOOOOOOO..",
    "................",
]
POTS = {
    "R": (0xD8, 0x3A, 0x30), "r": (0x9C, 0x24, 0x1E), "Y": (0xFE, 0xD8, 0x3D), "y": (0xC8, 0xA0, 0x20),
    "B": (0x4A, 0x6C, 0xD8), "b": (0x2C, 0x40, 0x98), "G": (0x80, 0xC7, 0x1F), "g": (0x52, 0x86, 0x14),
}
OAK = {"O": (0x4A, 0x32, 0x18), "l": (0xC8, 0x9A, 0x5E), "m": (0xA8, 0x7E, 0x48), "L": (0xC8, 0x9A, 0x5E),
       "M": (0xA8, 0x7E, 0x48), "D": (0x86, 0x62, 0x36)}
TIERS = {
    "paint_box": {**OAK, "c": OAK["M"], "S": (0xD8, 0xD8, 0xDE), "s": (0x8A, 0x8A, 0x92)},
    "reinforced_paint_box": {**OAK, "c": (0xD8, 0xD8, 0xDE), "S": (0x8C, 0xF4, 0xE2), "s": (0x2B, 0xC7, 0xAC)},
    "netherite_paint_box": {"O": (0x14, 0x12, 0x15), "l": (0x4B, 0x3A, 0x51), "m": (0x39, 0x2E, 0x3E),
                            "L": (0x4B, 0x3A, 0x51), "M": (0x39, 0x2E, 0x3E), "D": (0x2A, 0x22, 0x2D),
                            "c": (0x78, 0x6B, 0x7C), "S": (0xC1, 0x94, 0xB9), "s": (0x9A, 0x68, 0x99)},
    "enderite_paint_box": {"O": (0x14, 0x12, 0x15), "l": (0x56, 0x40, 0x5F), "m": (0x41, 0x2F, 0x48),
                           "L": (0x56, 0x40, 0x5F), "M": (0x41, 0x2F, 0x48), "D": (0x30, 0x21, 0x36),
                           "c": (0xA6, 0x7A, 0xEF), "S": (0xF4, 0xD2, 0xFF), "s": (0xC7, 0x7D, 0xFF)},
}
# Enderite specks (x, y) on lid and front, like the enderite backpack.
SPECKS = {"enderite_paint_box": [((4, 3), (0x84, 0x56, 0xA3)), ((10, 2), (0xA6, 0x7A, 0xEF)),
                                 ((4, 11), (0x88, 0x58, 0xA8)), ((11, 10), (0xA6, 0x7A, 0xEF)), ((9, 12), (0x84, 0x56, 0xA3))]}


def build() -> dict[str, Image.Image]:
    images = {}
    for name, colours in TIERS.items():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y, row in enumerate(ROWS):
            for x, ch in enumerate(row):
                rgb = POTS.get(ch) or colours.get(ch)
                if rgb:
                    img.putpixel((x, y), rgb + (255,))
        for (x, y), rgb in SPECKS.get(name, []):
            img.putpixel((x, y), rgb + (255,))
        images[name] = img
    return images


def preview(images: dict[str, Image.Image], path: Path, extra: Path | None) -> None:
    scale, pad = 16, 16
    cell = 16 * scale + pad
    sheet = Image.new("RGBA", (len(images) * cell + pad, cell + pad), (0x8B, 0x8B, 0x8B, 255))
    for i, img in enumerate(images.values()):
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (pad + i * cell, pad))
    if extra and extra.exists():
        shot = Image.open(extra).convert("RGBA")
        out = Image.new("RGBA", (max(sheet.width, shot.width), sheet.height + shot.height), (0x8B, 0x8B, 0x8B, 255))
        out.alpha_composite(sheet, (0, 0))
        out.alpha_composite(shot, (0, sheet.height))
        sheet = out
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
    print(path)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--preview", type=Path)
    parser.add_argument("--tooltip", type=Path, help="client screenshot crop appended under the preview")
    args = parser.parse_args()
    images = build()
    for out in OUT_DIRS:
        out.mkdir(parents=True, exist_ok=True)
        for name, img in images.items():
            img.save(out / f"{name}.png")
    if args.preview:
        preview(images, args.preview, args.tooltip)


if __name__ == "__main__":
    main()
