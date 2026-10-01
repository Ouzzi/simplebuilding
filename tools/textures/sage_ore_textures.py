"""Usage: python tools/textures/sage_ore_textures.py <out-dir> <vanilla textures dir>

Sage Ore / Deepslate Sage Ore / Sage Orb textures (owner 2026-10-01). Derived from vanilla:
the ore specks of diamond ore keep their shape and shading, recolored onto an experience green ramp."""
from PIL import Image, ImageDraw
import sys
V = sys.argv[2] if len(sys.argv) > 2 else 'build/vanilla-textures/'  # vanilla assets/minecraft/textures/ from the client jar
OUT = sys.argv[1] if len(sys.argv) > 1 else 'build/sage-textures/'  # then copy into mc26_3/overlay/resources/assets/simplebuilding/textures/
# experience orb greens/yellows (dark -> light)
RAMP = [(34, 74, 20), (58, 120, 22), (102, 170, 30), (156, 214, 44), (208, 240, 92), (246, 255, 178)]


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def ore(base_name, ore_name):
    base = Image.open(V + f'block/{base_name}.png').convert('RGBA')
    src = Image.open(V + f'block/{ore_name}.png').convert('RGBA')
    out = base.copy()
    sat = lambda p: max(p[:3]) - min(p[:3])
    speck = [(x, y) for y in range(16) for x in range(16) if sat(src.getpixel((x, y))) > 30]
    # the ore texture's own stone shows outside the specks (it differs slightly from stone.png)
    out = src.copy()
    ls = sorted(lum(src.getpixel(p)) for p in speck)
    for p in speck:
        v = lum(src.getpixel(p))
        i = sum(1 for t in ls if t <= v) / len(ls)
        out.putpixel(p, RAMP[min(len(RAMP) - 1, int(i * len(RAMP)))] + (255,))
    return out


def orb():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx, cy, r = 7.5, 8.0, 5.6
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r:
                # lit from the upper left like vanilla items
                shade = 1.0 - d / r * 0.55 - ((x - cx) + (y - cy)) / (2 * r) * 0.45
                i = max(0, min(len(RAMP) - 1, int(shade * len(RAMP))))
                img.putpixel((x, y), RAMP[i] + (255,))
    # dark outline
    filled = img.copy()
    for y in range(16):
        for x in range(16):
            if filled.getpixel((x, y))[3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and filled.getpixel((x + dx, y + dy))[3]
                                                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), (22, 46, 14, 255))
    # highlight + sparkle like the owner's enderite sparkles
    for (x, y, c) in [(5, 5, RAMP[5]), (6, 5, RAMP[5]), (5, 6, RAMP[5]), (10, 10, RAMP[4]), (12, 3, (255, 255, 220)), (13, 2, (255, 255, 255))]:
        img.putpixel((x, y), c + (255,))
    return img


if __name__ == '__main__':
    import os
    os.makedirs(OUT, exist_ok=True)
    a, b, c = ore('stone', 'diamond_ore'), ore('deepslate', 'deepslate_diamond_ore'), orb()
    a.save(OUT + 'sage_ore.png'); b.save(OUT + 'deepslate_sage_ore.png'); c.save(OUT + 'sage_orb.png')
    S = 10
    refs = [('Diamanterz', Image.open(V + 'block/diamond_ore.png')), ('Weisheitserz', a),
            ('Tiefenschiefer-Diamanterz', Image.open(V + 'block/deepslate_diamond_ore.png')), ('Tiefenschiefer-Weisheitserz', b),
            ('Smaragderz', Image.open(V + 'block/emerald_ore.png')), ('Weisheitskugel', c), ('XP-Flasche', Image.open(V + 'item/experience_bottle.png'))]
    sheet = Image.new('RGBA', (10 + len(refs) * (16 * S + 12), 16 * S + 40), (44, 44, 44, 255)); d = ImageDraw.Draw(sheet)
    for i, (n, im) in enumerate(refs):
        x = 10 + i * (16 * S + 12)
        sheet.alpha_composite(im.convert('RGBA').resize((16 * S, 16 * S), Image.NEAREST), (x, 28)); d.text((x, 10), n, fill=(235, 235, 235, 255))
    sheet.save(OUT + 'preview_sage.png')
    print('ok')
