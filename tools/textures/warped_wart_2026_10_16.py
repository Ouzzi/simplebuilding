"""Warped wart and blue nether bricks textures (N71, 2026-10-16): own pixel art, nothing derived from Vanilla.

Own shapes in the turquoise of Vanilla's warped wart block: the item is a clump of four round warts with a stalk nub (shaded
spheres lit from the upper left, dark outline), the plant has three growth stages (sprout, young, grown: stems that carry
round caps, the grown plant with side warts), the bricks are an 8x4 brick wall like the red nether bricks with spore specks.

  python3.12 tools/textures/warped_wart_2026_10_16.py          # writes the PNGs + 8x previews (docs/ai/previews/claude-warpedwart)
  python3.12 tools/textures/warped_wart_2026_10_16.py --check  # are the PNGs current?
"""
import argparse
import io
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
LIB = ROOT / 'modules/simplelib/shared/resources/assets/simplelib/textures'
BLOCKS = ROOT / 'modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block'
PREVIEW = ROOT / 'docs/ai/previews/claude-warpedwart'

OUTLINE = (10, 48, 56, 255)
SHADE = (16, 78, 88, 255)
DARK = (20, 104, 110, 255)
MID = (22, 134, 138, 255)
LIGHT = (24, 172, 146, 255)
HIGH = (100, 232, 196, 255)
RAMP = [SHADE, DARK, MID, LIGHT, HIGH]


def blank():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def sphere(im, cx, cy, r, outline=True):
    """A shaded ball: ramp by distance from a light point at the upper left; one-pixel dark outline around the union."""
    px = im.load()
    lx, ly = cx - r * 0.35, cy - r * 0.4
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r:
                t = math.hypot(x + 0.5 - lx, y + 0.5 - ly) / (r * 1.55)
                px[x, y] = RAMP[max(0, min(4, 4 - int(t * 5)))]
    return im


def outline(im):
    px = im.load()
    marks = []
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3] and px[x + dx, y + dy] != OUTLINE
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                marks.append((x, y))
    for m in marks:
        px[m] = OUTLINE
    return im


def item():
    """A lumpy clump of five round warts of different sizes; the biggest sits in the middle, a short stalk nub below."""
    im = blank()
    for cx, cy, r in ((5.4, 5.6, 2.7), (11.0, 5.0, 2.3), (8.2, 9.2, 3.3), (3.8, 11.0, 2.0), (12.4, 11.0, 2.3)):
        part = sphere(blank(), cx, cy, r)
        im.alpha_composite(part)
    px = im.load()
    for x, y in ((7, 13), (8, 13), (7, 14), (8, 14)):
        px[x, y] = DARK if x == 7 else SHADE
    return outline(im)


def plant(stalks, caps, side=()):
    """stalks: (x, top y); caps: (cx, cy, r); the stalk is two pixels wide down to the soil line, darker on the right."""
    im = blank()
    px = im.load()
    for x, top in stalks:
        for y in range(top, 16):
            px[x, y] = MID if y % 3 else DARK
            px[x + 1, y] = SHADE
        px[x, 15] = DARK
    for cx, cy, r in caps:
        im.alpha_composite(sphere(blank(), cx, cy, r))
    for cx, cy, r in side:
        im.alpha_composite(sphere(blank(), cx, cy, r))
    return outline(im)


def stages():
    return {
        'warped_wart_stage0': plant([(3, 12), (10, 13)], [(4.0, 11.0, 1.8), (11.0, 12.0, 1.6)]),
        'warped_wart_stage1': plant([(2, 9), (7, 6), (12, 10)], [(3.0, 8.0, 2.4), (8.0, 5.0, 3.0), (13.0, 9.0, 2.2)]),
        'warped_wart_stage2': plant([(1, 8), (7, 3), (12, 7)], [(2.2, 6.6, 2.8), (8.0, 3.2, 3.4), (13.0, 5.6, 2.8)],
                                    [(5.0, 10.0, 1.9), (10.6, 11.0, 1.9)]),
    }


def bricks():
    """8x4 bricks, 1 px mortar; joints alternate (x 3/11, then 7/15) like the red nether bricks; a lit top row, a dark foot row,
    and a few bright spore specks."""
    rng = random.Random(716)
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    mortar = [(8, 40, 48, 255), (12, 54, 62, 255)]
    body = [(20, 100, 108, 255), (24, 116, 120, 255), (22, 126, 128, 255), (28, 140, 132, 255)]
    for row in range(4):
        y0 = row * 4
        joints = (3, 11) if row % 2 == 0 else (7, 15)
        for y in range(y0, y0 + 4):
            for x in range(16):
                if y == y0 + 3:
                    px[x, y] = mortar[(x + row) % 2]
                elif x in joints:
                    px[x, y] = mortar[(y + x) % 2]
                else:
                    c = body[rng.choice([0, 1, 1, 2, 2, 3])]
                    if y == y0:  # lit top row
                        c = tuple(min(255, int(v * 1.15)) for v in c[:3]) + (255,)
                    elif y == y0 + 2:  # foot shadow
                        c = tuple(int(v * 0.82) for v in c[:3]) + (255,)
                    px[x, y] = c
    for _ in range(5):  # spore specks on brick faces
        for _ in range(20):
            x, y = rng.randrange(16), rng.randrange(16)
            if y % 4 != 3 and x not in (3, 11, 7, 15):
                px[x, y] = HIGH if rng.random() < 0.5 else LIGHT
                break
    return im


def outputs():
    out = {LIB / 'item/warped_wart.png': item()}
    for name, im in stages().items():
        out[LIB / f'block/{name}.png'] = im
    out[BLOCKS / 'blue_nether_bricks.png'] = bricks()
    return out


def png(im):
    buf = io.BytesIO()
    im.save(buf, format='PNG')
    return buf.getvalue()


def sheet(images, scale=8):
    pad = 6
    out = Image.new('RGB', (len(images) * (16 * scale + pad) + pad, 16 * scale + 2 * pad), (34, 34, 40))
    for i, im in enumerate(images):
        out.paste(im.resize((16 * scale,) * 2, Image.NEAREST), (pad + i * (16 * scale + pad), pad), im.resize((16 * scale,) * 2, Image.NEAREST))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    a = ap.parse_args()
    outs = outputs()
    if a.check:
        stale = [str(p.relative_to(ROOT)) for p, im in outs.items() if not p.exists() or p.read_bytes() != png(im)]
        if stale:
            raise SystemExit(f'warped_wart: stale {stale}; run without --check')
        print('warped_wart: textures current')
        return
    for p, im in outs.items():
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(png(im))
    PREVIEW.mkdir(parents=True, exist_ok=True)
    sheet(list(outs.values())).save(PREVIEW / 'warped_wart_8x.png')
    print('wrote', len(outs), 'textures; preview', PREVIEW / 'warped_wart_8x.png')


if __name__ == '__main__':
    main()
