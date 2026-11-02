"""Spawner Fragment / Chunk / Makeshift Spawner textures (b-spawn01, WF25-6). Own pixel art, no Mojang pixels.

Method (A8b, Endholz process): a ramp of 9 soft tones (dark -> light), shape first, light from the upper left, shadow lower right, a few
hard accent pixels (about 7 %) for the glint. b-tex03 (owner 02.11.): iron look like the Vanilla iron items, the fragment smaller.

  python3.12 tools/textures/spawn01_2026_11_01.py --write MODULE_TEXTURES_DIR   (also writes the .layers.json beside each PNG)"""
from __future__ import annotations

import argparse
import random
from pathlib import Path

from PIL import Image

# b-tex03 (owner 02.11.): looks like Vanilla iron - a neutral, light grey ramp (own pixels, not copied), no coloured spark.
# 'old' = the cold blue variant of b-spawn01 (kept only for the before/after preview).
RAMPS = {
    'iron': [(38, 38, 42), (62, 62, 66), (88, 88, 92), (116, 116, 120), (146, 146, 150), (174, 174, 176), (202, 202, 203), (226, 226, 226), (244, 244, 244)],
    'old': [(22, 26, 34), (36, 42, 54), (52, 60, 76), (72, 82, 100), (96, 108, 128), (124, 136, 154), (156, 168, 184), (190, 200, 212), (226, 234, 242)],
}
SPARK = {'iron': [(116, 116, 120), (226, 226, 226), (244, 244, 244)], 'old': [(30, 120, 140), (70, 200, 220), (200, 250, 255)]}
DEFAULT = 'iron'

# 16x16 item shapes: digits = ramp tone (0 dark rim .. 8 light), s = spark tones 1-3 (a b c), . = clear
FRAGMENT = [
    "................", "................", "................", "................", "......11........", ".....1351.......",
    "....135762......", "....1356762.....", "....13a77621....", ".....135662211..", "......1355221...", ".......12221....",
    "................", "................", "................", "................"]
FRAGMENT_OLD = [
    "................", "................", "................", ".......1........", "......132.......", ".....13562......",
    "....1356762.....", "....13567a62....", "...1356776662...", "...135776656621.", "....13566652211.", ".....1355522....",
    "......12221.....", ".......111......", "................", "................"]
CHUNK = [
    "................", "......11........", ".....1341.......", "....135762..11..", "....13676221352.", "...1135776613762",
    "..13557a66621662", "..1356777662.211", "..13567776621...", ".1135677766621..", ".13567c776662211", ".1355677766521..",
    "..1355666552211.", "...11255522211..", ".....1222111....", "................"]


def paint(shape, variant):
    ramp, spark = RAMPS[variant], SPARK[variant]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(shape):
        for x, c in enumerate(row):
            if c == '.':
                continue
            if c in 'abc':
                im.putpixel((x, y), spark['abc'.index(c)] + (255,))
            else:
                im.putpixel((x, y), ramp[int(c)] + (255,))
    return im


def block(variant):
    """Cube face: a dark frame, rough iron bars with uneven patches (makeshift), a faint glow in the gaps."""
    ramp, spark = RAMPS[variant], SPARK[variant]
    rng = random.Random('spawn01' + variant)
    im = Image.new('RGBA', (16, 16), ramp[0] + (255,))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            bar = x in (4, 5, 10, 11) or y in (4, 5, 10, 11)
            if edge:
                t = 2 + (1 if x < 2 or y < 2 else 0) - (1 if x > 13 or y > 13 else 0)
            elif bar:
                t = 4 + (1 if x in (4, 10) or y in (4, 10) else 0) + rng.choice((0, 0, 0, -1, 1))
            else:
                t = 1 if rng.random() < 0.8 else 2
            im.putpixel((x, y), ramp[max(0, min(8, t))] + (255,))
    for (x, y) in ((7, 7), (8, 8), (7, 8)):                 # the glint in the middle gap
        im.putpixel((x, y), spark[1] + (255,))
    im.putpixel((8, 7), spark[2] + (255,))
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):     # corner rivets, hard accents
        im.putpixel((x, y), ramp[7] + (255,))
    for (x, y) in ((2, 12), (3, 12), (3, 13), (12, 2), (13, 2)):   # mismatched patches
        im.putpixel((x, y), ramp[3] + (255,))
    return im


def sheet(variant=DEFAULT):
    return {'item/spawner_fragment.png': paint(FRAGMENT if variant != 'old' else FRAGMENT_OLD, variant), 'item/spawner_chunk.png': paint(CHUNK, variant),
            'block/makeshift_spawner.png': block(variant)}


def main():
    import sys
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import layers
    ap = argparse.ArgumentParser()
    ap.add_argument('--write', required=True)
    a = ap.parse_args()
    for rel, im in sheet().items():
        p = Path(a.write) / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        layers.save(im, p)


if __name__ == '__main__':
    main()
