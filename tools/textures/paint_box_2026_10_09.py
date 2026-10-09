"""Paint box (owner 2026-10-09, round 4 in the evening: "sieht nicht vanilla aus"), own pixel art in Vanilla style.

An open wooden box seen slightly from above, like the Vanilla chest / bundle: the lid stands open behind, soft
shading with the light from the top left (highlight on the top-left edges, darker right and bottom edges, a dark
brown outline instead of black or hard grey areas). Inside, two rows of eight paint pans with all 16 dye colours
(one pixel each, DyeColor id order: white first, black last; N29), separated by a wooden slat and framed by the
shaded inner walls. A latch holds the front.

Tiers like the mod's other tier items: oak with an iron latch (basic), oak with iron corner fittings and a
diamond latch (reinforced), netherite and enderite (with purple specks) in the tones of the mod's netherite /
enderite backpacks.

Run: python3.12 tools/textures/paint_box_2026_10_09.py [--preview out.png]
"""
from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT_DIRS = [ROOT / "src/main/resources/assets/simplebuilding/textures/item", ROOT / "wiki/assets/textures/item"]

# O outline, h highlight, l light, m mid, d dark, D darkest (bottom), w wood between the pans, the dye symbols
# (PANS) are the pans, S/s latch light/dark, c corner fitting (tiers; plain outline on the basic box).
# Round 5 (owner 2026-10-09 late: "gefaellt noch nicht", more Vanilla item feel: clear silhouette, less detail
# noise, well visible colour dots): three proposals, CHOSEN is built into the game, --proposals previews all three.
DESIGNS = {
    # A: open tin, lid up behind, two compartments of 2x4 pans, every colour on its own between wooden ribs.
    "A": [
        "................",
        "..cOOOOOOOOOOc..",
        "..OhllllllllmO..",
        "..OmmmmmmmmmdO..",
        ".cOOOOOOOOOOOOc.",
        ".OhhhhhhhhhhhmO.",
        ".Ohw0w1ww2w3wdO.",
        ".OhwwwwwwwwwwdO.",
        ".Ohw4w5ww6w7wdO.",
        ".OhwwwwwwwwwwdO.",
        ".Ohw8w9wwAwBwdO.",
        ".OhwwwwwwwwwwdO.",
        ".OhwCwEwwFwGwdO.",
        ".OmDDDDDDDDDDDO.",
        ".cOOOOOOOOOOOOc.",
        "................",
    ],
    # B: the round 4 box simplified: lid behind, two rows of eight pans, plain front with the latch.
    "B": [
        "................",
        "..cOOOOOOOOOOc..",
        "..OhllllllllmO..",
        "..OmmmmmmmmmdO..",
        ".OOOOOOOOOOOOOO.",
        ".OhwwwwwwwwwwdO.",
        ".Ohw01234567wdO.",
        ".OhwwwwwwwwwwdO.",
        ".Ohw89ABCEFGwdO.",
        ".OhwwwwwwwwwwdO.",
        ".cOOOOOOOOOOOOc.",
        ".OhllllSSllllmO.",
        ".OlmmmmssmmmmdO.",
        ".ODDDDDDDDDDDDO.",
        ".cOOOOOOOOOOOOc.",
        "................",
    ],
    # C: a square paint tin seen from above, no lid, 4x4 big pans (2x2 each, shaded bottom right).
    "C": [
        ".cOOOOOOOOOOOOc.",
        "cOhhhhhhhhhhhhOc",
        "OhwwwwwwwwwwwwdO",
        "Ohw00w11w22w33dO",
        "Ohw0+w1+w2+w3+dO",
        "OhwwwwwwwwwwwwdO",
        "Ohw44w55w66w77dO",
        "Ohw4+w5+w6+w7+dO",
        "OhwwwwwwwwwwwwdO",
        "Ohw88w99wAAwBBdO",
        "Ohw8+w9+wA+wB+dO",
        "OhwwwwwwwwwwwwdO",
        "OhwCCwEEwFFwGGdO",
        "OhwC+wE+wF+wG+dO",
        "cODDDDDDDDDDDDOc",
        ".cOOOOOOOOOOOOc.",
    ],
}
CHOSEN = "A"
DYES = [0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
        0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21]  # DyeColor texture colours
PANS = "0123456789ABCEFG"  # one symbol per dye (no D: that is the darkest wood)
POTS = {PANS[i]: ((c >> 16) & 255, (c >> 8) & 255, c & 255) for i, c in enumerate(DYES)}
# Vanilla chest wood tones (entity/chest/normal.png), a lighter top edge and a dark brown outline.
OAK = {"O": (0x3B, 0x27, 0x09), "h": (0xC2, 0x8C, 0x3C), "l": (0xAB, 0x79, 0x2D), "m": (0xA2, 0x6B, 0x23),
       "d": (0x8F, 0x69, 0x1D), "D": (0x7F, 0x5F, 0x22), "i": (0x5C, 0x40, 0x14), "w": (0x95, 0x6C, 0x2E)}
IRON = {"S": (0xD8, 0xD8, 0xD8), "s": (0x8E, 0x8E, 0x8E)}
NETHERITE = {"O": (0x14, 0x12, 0x15), "h": (0x4B, 0x3A, 0x51), "l": (0x3E, 0x31, 0x43), "m": (0x39, 0x2E, 0x3E),
             "d": (0x2F, 0x26, 0x33), "D": (0x2A, 0x22, 0x2D), "i": (0x1C, 0x15, 0x19), "w": (0x47, 0x38, 0x4D)}
ENDERITE = {"O": (0x14, 0x12, 0x15), "h": (0x56, 0x40, 0x5F), "l": (0x49, 0x34, 0x51), "m": (0x41, 0x2F, 0x48),
            "d": (0x3A, 0x29, 0x41), "D": (0x30, 0x21, 0x36), "i": (0x1F, 0x15, 0x24), "w": (0x50, 0x35, 0x5D)}
TIERS = {
    "paint_box": {**OAK, **IRON, "c": OAK["O"]},  # no fittings: plain outline corners
    "reinforced_paint_box": {**OAK, "c": (0xC8, 0xC8, 0xC8), "S": (0x8C, 0xF4, 0xE2), "s": (0x2B, 0xC7, 0xAC)},
    "netherite_paint_box": {**NETHERITE, "c": (0x78, 0x6B, 0x7C), "S": (0xC1, 0x94, 0xB9), "s": (0x9A, 0x68, 0x99)},
    "enderite_paint_box": {**ENDERITE, "c": (0x8A, 0x5B, 0xC9), "S": (0xF4, 0xD2, 0xFF), "s": (0xC7, 0x7D, 0xFF)},
}
# Enderite: purple specks on the wood like the enderite backpack (per design, never on a pan).
SPECKS = {
    "A": [((4, 3), (0x84, 0x56, 0xA3)), ((10, 2), (0xA6, 0x7A, 0xEF)), ((3, 13), (0x88, 0x58, 0xA8)),
          ((12, 13), (0xA6, 0x7A, 0xEF))],
    "B": [((4, 3), (0x84, 0x56, 0xA3)), ((10, 2), (0xA6, 0x7A, 0xEF)), ((4, 12), (0x88, 0x58, 0xA8)),
          ((11, 11), (0xA6, 0x7A, 0xEF))],
    "C": [((1, 6), (0x84, 0x56, 0xA3)), ((14, 10), (0xA6, 0x7A, 0xEF)), ((6, 1), (0xA6, 0x7A, 0xEF)),
          ((9, 14), (0x88, 0x58, 0xA8))],
}


def build(design: str = CHOSEN) -> dict[str, Image.Image]:
    images = {}
    for name, colours in TIERS.items():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y, row in enumerate(DESIGNS[design]):
            for x, ch in enumerate(row):
                if ch == "+":  # shaded corner of a 2x2 pan: the pan's dye, a quarter darker
                    rgb = tuple(round(v * 0.72) for v in POTS[row[x - 1]])
                else:
                    rgb = POTS.get(ch) or colours.get(ch)
                if rgb:
                    img.putpixel((x, y), rgb + (255,))
        if name == "enderite_paint_box":
            for (x, y), rgb in SPECKS[design]:
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


def proposals(path: Path) -> None:
    """A/B/C side by side (4 tiers each, 12x) with 1x/2x inventory sizes and the Vanilla bundle as reference."""
    from PIL import ImageDraw
    scale, pad = 12, 12
    cell = 16 * scale + pad
    sheet = Image.new("RGBA", (pad + 5 * cell, pad + len(DESIGNS) * (cell + 16)), (0x8B, 0x8B, 0x8B, 255))
    draw = ImageDraw.Draw(sheet)
    for r, design in enumerate(DESIGNS):
        y = pad + r * (cell + 16)
        draw.text((pad, y), design + (" (eingebaut)" if design == CHOSEN else ""), fill=(0, 0, 0, 255))
        imgs = list(build(design).values())
        for i, img in enumerate(imgs):
            sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (pad + i * cell, y + 14))
            sheet.alpha_composite(img, (pad + 4 * cell + i * 20, y + 14))
            sheet.alpha_composite(img.resize((32, 32), Image.NEAREST), (pad + 4 * cell + i * 36, y + 40))
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
    print(path)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--preview", type=Path)
    parser.add_argument("--tooltip", type=Path, help="client screenshot crop appended under the preview")
    parser.add_argument("--proposals", type=Path, help="write the A/B/C proposal sheet here and nothing else")
    args = parser.parse_args()
    if args.proposals:
        proposals(args.proposals)
        return
    images = build()
    for out in OUT_DIRS:
        out.mkdir(parents=True, exist_ok=True)
        for name, img in images.items():
            img.save(out / f"{name}.png")
    if args.preview:
        preview(images, args.preview, args.tooltip)


if __name__ == "__main__":
    main()
