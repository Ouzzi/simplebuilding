"""Usage: python tools/textures/autonomous_crafter_textures.py <vanilla textures dir> [preview.png] [--check]

Autonomous Crafter block textures (owner 2026-10-09, queue N26: "abgewandelter Vanilla-Crafter"). Like the Auto
Smither it keeps the crafter's stone casing and plank pillars, so it reads as a crafter; its own parts:
- top: the crafter's grid in copper instead of redstone red (lit amber while crafting); the triggered state keeps
  Vanilla's red corner lights (a redstone signal stops it),
- sides: a copper arrow pointing down between the pillars - the result goes into the hopper below (lit while crafting),
- bottom: an outlet with a copper rim over the hopper.
Output: mc26_3/overlay/resources/assets/simplebuilding/textures/block/autonomous_crafter_*.png. Vanilla never checked in.
"""
import os
import sys
from PIL import Image, ImageDraw

V = sys.argv[1]
PREVIEW = next((a for a in sys.argv[2:] if a.endswith('.png')), None)
CHECK = '--check' in sys.argv
OUT = 'mc26_3/overlay/resources/assets/simplebuilding/textures/block'

COPPER = [(40, 22, 16), (70, 36, 24), (96, 50, 32), (120, 64, 40), (150, 82, 52), (186, 106, 66), (216, 132, 84)]
AMBER = [(96, 46, 20), (150, 80, 30), (206, 120, 44), (240, 168, 72), (255, 214, 120)]


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def load(name):
    return Image.open(os.path.join(V, 'block', name + '.png')).convert('RGBA').crop((0, 0, 16, 16))


def is_red(p):
    return p[0] > p[1] + 25 and abs(p[1] - p[2]) < 12


def recolor_reds(img, ramp, lo=0, hi=255):
    """Red pixels onto the ramp by their luminance rank (keeps Vanilla's shading order)."""
    out = img.copy()
    reds = sorted({img.getpixel((x, y))[:3] for y in range(16) for x in range(16) if is_red(img.getpixel((x, y)))}, key=lum)
    for y in range(16):
        for x in range(16):
            p = img.getpixel((x, y))
            if is_red(p):
                i = reds.index(p[:3])
                k = round(i * (len(ramp) - 1) / max(1, len(reds) - 1))
                out.putpixel((x, y), ramp[k] + (255,))
    return out


def lights(base, plain, triggered):
    """Vanilla's triggered lights: the pixels that differ between the plain and triggered crafter texture."""
    out = base.copy()
    for y in range(16):
        for x in range(16):
            if plain.getpixel((x, y)) != triggered.getpixel((x, y)):
                out.putpixel((x, y), triggered.getpixel((x, y)))
    return out


# Down arrow in the middle stripe (x 6..9) between the side's plank pillars.
ARROW = ['.LD.', '.LD.', '.LD.', '.LD.', '.LD.', 'LLDD', '.LD.']


def arrow(img, lit):
    out = img.copy()
    light, dark = ((255, 214, 120), (206, 120, 44)) if lit else ((186, 106, 66), (120, 64, 40))
    for r, row in enumerate(ARROW):
        for c, ch in enumerate(row):
            if ch != '.':
                out.putpixel((6 + c, 3 + r), (light if ch == 'L' else dark) + (255,))
    # a 1 px shadow under the arrow head, so it sits in the casing instead of on it
    for x in (7, 8):
        out.putpixel((x, 10), (56, 56, 56, 255))
    return out


def bottom(img):
    out = img.copy()
    for y in range(5, 11):
        for x in range(5, 11):
            edge = x in (5, 10) or y in (5, 10)
            if edge:
                c = COPPER[5] if (x == 5 or y == 5) else COPPER[3]
            else:
                c = (22, 20, 20) if (x in (6, 9) or y in (6, 9)) else (12, 10, 10)
            out.putpixel((x, y), c + (255,))
    return out


def textures():
    top = recolor_reds(load('crafter_top'), COPPER)
    side = recolor_reds(load('crafter_south'), COPPER)
    return {
        'autonomous_crafter_top': top,
        'autonomous_crafter_top_crafting': recolor_reds(load('crafter_top_crafting'), COPPER[:2] + AMBER),
        'autonomous_crafter_top_triggered': lights(top, load('crafter_top'), load('crafter_top_triggered')),
        'autonomous_crafter_side': arrow(side, False),
        'autonomous_crafter_side_crafting': arrow(side, True),
        'autonomous_crafter_side_triggered': lights(arrow(side, False), load('crafter_south'), load('crafter_south_triggered')),
        'autonomous_crafter_bottom': bottom(load('crafter_bottom')),
    }


def main():
    tex = textures()
    stale = []
    for name, img in tex.items():
        path = os.path.join(OUT, name + '.png')
        if CHECK:
            if not os.path.exists(path) or Image.open(path).convert('RGBA').tobytes() != img.tobytes():
                stale.append(path)
        else:
            os.makedirs(OUT, exist_ok=True)
            img.save(path)
    if PREVIEW:
        vanilla = [load(n) for n in ('crafter_top', 'crafter_top_crafting', 'crafter_top_triggered', 'crafter_south',
                                     'crafter_south', 'crafter_south_triggered', 'crafter_bottom')]
        s = 12
        canvas = Image.new('RGB', (40 + 7 * (16 * s + 12), 120 + 2 * (16 * s + 40)), '#20242b')
        d = ImageDraw.Draw(canvas)
        d.text((20, 10), 'Autonomer Crafter | oben Vanilla-Crafter, unten neu | 12x, ungefiltert', fill='white')
        labels = ['oben', 'oben craftet', 'oben Redstone', 'Seite', 'Seite craftet', 'Seite Redstone', 'unten']
        for i, (lab, a, b) in enumerate(zip(labels, vanilla, tex.values())):
            x = 20 + i * (16 * s + 12)
            d.text((x, 40), lab, fill='#cbd5e1')
            for row, img in enumerate((a, b)):
                big = img.resize((16 * s, 16 * s), Image.NEAREST)
                canvas.paste(big, (x, 60 + row * (16 * s + 40)), big)
        canvas.save(PREVIEW)
    if CHECK:
        print('autonomous crafter textures: ' + ('stale ' + ', '.join(stale) if stale else '7/7 current'))
        sys.exit(1 if stale else 0)
    print('autonomous crafter textures: 7 generated')


if __name__ == '__main__':
    main()
