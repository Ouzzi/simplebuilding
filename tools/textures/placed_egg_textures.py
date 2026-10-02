"""Usage: python tools/textures/placed_egg_textures.py <out-dir> <vanilla textures dir>

Placed eggs (owner 2026-10-02): one 16x16 shell texture per vanilla egg (egg, blue egg, brown egg). The shell
tones are taken from the vanilla item sprite (lightest = top light, darkest = shadow), laid out as a soft
vertical gradient with the item's speckle colour sprinkled in, so the small 3D egg reads like the item.
Output: textures/block/placed_egg_<white|blue|brown>.png."""
from PIL import Image
import os
import random
import sys

OUT = sys.argv[1] if len(sys.argv) > 1 else 'build/placed-eggs/'
V = sys.argv[2] if len(sys.argv) > 2 else 'build/vanilla-textures/'


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def tones(name):
    im = Image.open(os.path.join(V, f'item/{name}.png')).convert('RGBA')
    px = sorted({im.getpixel((x, y))[:3] for x in range(16) for y in range(16) if im.getpixel((x, y))[3] > 200}, key=lum)
    return px


def shell(name):
    t = tones(name)
    body = t[len(t) // 3:]  # skip the dark outline tones
    rnd = random.Random(name)
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            shade = 1.0 - (y / 15.0) * 0.6 - (x / 15.0) * 0.2
            c = body[max(0, min(len(body) - 1, round(shade * (len(body) - 1))))]
            if rnd.random() < 0.08:
                c = t[len(t) // 3]
            im.putpixel((x, y), c + (255,))
    return im


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for item, colour in (('egg', 'white'), ('blue_egg', 'blue'), ('brown_egg', 'brown')):
        shell(item).save(os.path.join(OUT, f'placed_egg_{colour}.png'))
