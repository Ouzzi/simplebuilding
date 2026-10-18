"""Shaping Saw textures (Simple Blocks, owner 17.10., principle A4b): the Vanilla stonecutter, retextured.

Owner decision 17.10.: same model as the stonecutter; the base gets copper highlights (the brass corner caps become
copper, the slot ends, the top edge of the wooden frame and its cross bar get copper accents) and the round blade gets light copper
highlights on its mid tone (the animated 3-frame strip stays as in Vanilla). The bottom is the Vanilla texture itself
(referenced by the model, nothing copied).

Vanilla-derived (AGENTS.md rule 7): built from the client jar, never committed. Listed in
vanilla_derived.MODULE_OUTPUTS['simpleblocks'] (generator derived(jar)) and in .gitignore.

  python3.12 tools/textures/shaping_saw_2026_10_17.py --jar <client.jar>   # previews to SB_PREVIEW_DIR (never committed)
"""
import argparse
import io
import os
import zipfile
from pathlib import Path

from PIL import Image

PREVIEW = Path(os.environ.get('SB_PREVIEW_DIR', '/tmp/claude-0/ash/formsaege'))
SRC = 'assets/minecraft/textures/block/'
# Copper ramp (dark -> light), the family of the Vanilla copper block.
COPPER = [(0x6d, 0x3a, 0x22), (0x9c, 0x54, 0x32), (0xc0, 0x6b, 0x45), (0xe0, 0x8a, 0x5e), (0xf2, 0xb0, 0x86)]
LIGHT_COPPER = (0xf0, 0xb8, 0x94)


def _lum(p):
    return 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]


def _is_brass(p):
    """The yellowish brass of the stonecutter's corner caps (red > green > blue clearly)."""
    return p[3] and p[0] - p[2] > 60 and p[1] - p[2] > 25


def _is_wood(p):
    return p[3] and p[0] - p[2] > 25 and p[1] - p[2] < 25


def _copper_for(p, lo, hi):
    """Map a pixel's brightness inside [lo, hi] onto the copper ramp (keeps Vanilla's shading)."""
    t = 0 if hi <= lo else (_lum(p) - lo) / (hi - lo)
    i = max(0, min(len(COPPER) - 1, round(t * (len(COPPER) - 1))))
    return COPPER[i] + (255,)


def top(src):
    im = src.convert('RGBA').copy()
    brass = [im.getpixel((x, y)) for y in range(16) for x in range(16) if _is_brass(im.getpixel((x, y)))]
    lo, hi = min(map(_lum, brass)), max(map(_lum, brass))
    for y in range(16):
        for x in range(16):
            p = im.getpixel((x, y))
            if _is_brass(p):
                im.putpixel((x, y), _copper_for(p, lo, hi))
    for y in (7, 8):  # copper ends of the blade slot
        for x in (2, 13):
            im.putpixel((x, y), COPPER[3 if y == 7 else 1] + (255,))
    return im


def side(src):
    im = src.convert('RGBA').copy()
    wood = [im.getpixel((x, y)) for y in range(16) for x in range(16) if _is_wood(im.getpixel((x, y)))]
    lo, hi = min(map(_lum, wood)), max(map(_lum, wood))
    for y in (7, 9):  # the top edge of the frame and its cross bar get copper rails
        for x in range(16):
            p = im.getpixel((x, y))
            if _is_wood(p):
                im.putpixel((x, y), _copper_for(p, lo, hi))
    return im


def saw(src):
    """Vanilla blade strip (grey + alpha): the mid tone of the teeth band becomes light copper, the hub copper."""
    la = src.convert('LA')
    im = Image.new('RGBA', la.size, (0, 0, 0, 0))
    for y in range(la.height):
        for x in range(la.width):
            v, a = la.getpixel((x, y))
            if not a:
                continue
            if 150 <= v <= 185:
                c = LIGHT_COPPER
            elif v < 140:
                c = COPPER[2] if v >= 120 else COPPER[1]
            else:
                c = (v, v, v)
            im.putpixel((x, y), c + (a,))
    return im


def build(read):
    """read(name) -> Image of a Vanilla block texture. Returns texture path -> image."""
    return {'block/shaping_saw_top.png': top(read('stonecutter_top.png')),
            'block/shaping_saw_side.png': side(read('stonecutter_side.png')),
            'block/shaping_saw_saw.png': saw(read('stonecutter_saw.png'))}


def derived(jar):
    """Vanilla-derived output (vanilla_derived.py, MODULE_OUTPUTS['simpleblocks']): path -> image."""
    with zipfile.ZipFile(jar) as z:
        return build(lambda name: Image.open(io.BytesIO(z.read(SRC + name))).convert('RGBA'))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--jar', default=os.environ.get('SB_CLIENT_JAR'))
    ap.add_argument('--dir', help='folder with the Vanilla block textures instead of a jar')
    a = ap.parse_args()
    if a.dir:
        out = build(lambda name: Image.open(Path(a.dir) / name).convert('RGBA'))
    else:
        out = derived(a.jar)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    for path, im in out.items():
        im.save(PREVIEW / Path(path).name)
    print(f'shaping saw previews (Vanilla-derived, never commit) -> {PREVIEW}')


if __name__ == '__main__':
    main()
