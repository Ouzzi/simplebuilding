#!/usr/bin/env python3
"""Bucket of Frog (Simple Mob Manners, queue N62): own pixel art, a metal pail with a frog peeking out of the water.

    python tools/textures/frog_bucket_2026_10_15.py            # writes the item texture + 8x preview
    python tools/textures/frog_bucket_2026_10_15.py --check    # fails if the texture differs from this map

16x16 map, light from the top left, 1 px outline (never pure black), no half transparency. No Vanilla pixels are used.
"""
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEXTURE = ROOT / "modules/simplemobmanners/shared/resources/assets/simplemobmanners/textures/item/frog_bucket.png"
PREVIEW = ROOT / "docs/ai/previews/oc-qol62/frog_bucket_x8.png"

PALETTE = {
    "o": (38, 48, 44),     # outline
    "G": (132, 204, 96),   # frog light
    "g": (86, 164, 70),    # frog mid
    "d": (52, 112, 56),    # frog dark
    "w": (240, 244, 232),  # eye white
    "k": (34, 36, 34),     # pupil
    "B": (122, 172, 244),  # water light
    "b": (66, 114, 214),   # water
    "c": (44, 82, 176),    # water dark
    "M": (206, 210, 218),  # metal light
    "m": (156, 160, 170),  # metal
    "n": (104, 108, 120),  # metal dark
}

ROWS = [
    "................",
    "....oo....oo....",
    "...owwo..owwo...",
    "...owko..okwo...",
    "..ogGGgooGGggo..",
    "..odgGggggGgdo..",
    ".oBBdddggdddBBo.",
    ".obbbbbbbbbbbbo.",
    ".ocbBBbbbbbBbco.",
    ".oMmmmmmmmmmmno.",
    "..oMmmmmmmmmno..",
    "..omMmmmmmmnno..",
    "..ommmmmmmmnno..",
    "...ommmmmmnno...",
    "...onnnnnnnno...",
    "....oooooooo....",
]


def render():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(ROWS):
        assert len(row) == 16, (y, row)
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((x, y), PALETTE[c] + (255,))
    return img


def main():
    img = render()
    if "--check" in sys.argv:
        if not TEXTURE.is_file() or Image.open(TEXTURE).convert("RGBA").tobytes() != img.tobytes():
            sys.exit(f"{TEXTURE} is out of date; run this script")
        print("frog_bucket: texture up to date")
        return
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    img.save(TEXTURE)
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    img.resize((128, 128), Image.NEAREST).save(PREVIEW)
    print("wrote", TEXTURE.relative_to(ROOT), "and", PREVIEW.relative_to(ROOT))


if __name__ == "__main__":
    main()
