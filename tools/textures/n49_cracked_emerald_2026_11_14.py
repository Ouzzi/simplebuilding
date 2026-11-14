"""N49 (14.11.2026): cracked emerald in the emerald's shape (owner, PAL35C follow-up).

Own pixels (A8b): a step-cut gem drawn here, only the ten Vanilla emerald tones are reused (pal35c_2026_11_14.EMERALD).
Upright octagon like the emerald gem item, light left band, flat table, darker right band, two cracks with a bright chip edge.

    python3 tools/textures/n49_cracked_emerald_2026_11_14.py [--out FILE]
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from pal35c_2026_11_14 import EMERALD, TEX, rgb  # noqa: E402

# Rung per pixel: 0 = brightest .. 9 = darkest, '.' transparent.
ROWS = [
    "................",
    "................",
    "......8888......",
    ".....821378.....",
    "....82110758....",
    "...8311207569...",
    "...8312074569...",
    "...8320744569...",
    "...8307447569...",
    "...8307444769...",
    "...8374445669...",
    "....87445669....",
    ".....975669.....",
    "......9999......",
    "................",
    "................",
]


def build() -> Image.Image:
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(ROWS):
        for x, c in enumerate(row):
            if c != '.':
                img.putpixel((x, y), rgb(EMERALD[int(c)]) + (255,))
    return img


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--out', type=Path, default=TEX / 'item/cracked_emerald.png')
    a = ap.parse_args()
    build().save(a.out)
    print('wrote', a.out)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
