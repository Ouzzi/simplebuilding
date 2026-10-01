"""Usage: python tools/textures/dimensional_scrap_textures.py <out-dir> <vanilla textures dir>

Dimensional Scrap textures (owner 2026-10-01): vanilla ancient debris keeps its structure; its body is
mapped onto the dimension's stone ramp and its brightest swirl lines become a rift accent."""
from PIL import Image, ImageDraw
import sys, os
OUT = sys.argv[1] if len(sys.argv) > 1 else 'build/scrap-textures/'
V = sys.argv[2] if len(sys.argv) > 2 else 'build/vanilla-textures/'
VARIANTS = {
    'dimensional_scrap': ([(28, 28, 34), (42, 42, 50), (58, 58, 66), (74, 74, 82), (92, 92, 100)],
                          [(30, 140, 150), (90, 230, 220), (200, 255, 250)]),
    'nether_dimensional_scrap': ([(30, 14, 18), (48, 22, 26), (66, 30, 34), (86, 40, 42), (104, 52, 52)],
                                 [(110, 50, 170), (170, 100, 240), (230, 190, 255)]),
    'end_dimensional_scrap': ([(120, 116, 84), (150, 146, 104), (180, 176, 128), (206, 202, 150), (226, 222, 172)],
                              [(120, 50, 160), (190, 100, 230), (250, 190, 255)]),
}


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def remap(src, body, rift):
    img = src.convert('RGBA'); out = img.copy()
    px = [(x, y) for y in range(img.height) for x in range(img.width)]
    ls = sorted(lum(img.getpixel(p)) for p in px)
    cut = ls[int(len(ls) * 0.88)]  # the brightest 12 % are the swirl lines
    for p in px:
        v = lum(img.getpixel(p))
        if v >= cut:
            hi = sorted(t for t in ls if t >= cut)
            i = sum(1 for t in hi if t <= v) / len(hi)
            c = rift[min(len(rift) - 1, int(i * len(rift)))]
        else:
            lo = [t for t in ls if t < cut]
            i = sum(1 for t in lo if t <= v) / len(lo)
            c = body[min(len(body) - 1, int(i * len(body)))]
        out.putpixel(p, c + (255,))
    return out


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    side = Image.open(V + 'block/ancient_debris_side.png'); top = Image.open(V + 'block/ancient_debris_top.png')
    S = 10; tiles = [('Antiker Schrott', side), ('', top)]
    for name, (body, rift) in VARIANTS.items():
        s, t = remap(side, body, rift), remap(top, body, rift)
        s.save(OUT + f'{name}_side.png'); t.save(OUT + f'{name}_top.png')
        tiles += [(name, s), ('', t)]
    sheet = Image.new('RGBA', (10 + len(tiles) * (16 * S + 8), 16 * S + 40), (44, 44, 44, 255)); d = ImageDraw.Draw(sheet)
    for i, (n, im) in enumerate(tiles):
        x = 10 + i * (16 * S + 8)
        sheet.alpha_composite(im.convert('RGBA').resize((16 * S, 16 * S), Image.NEAREST), (x, 28)); d.text((x, 10), n, fill=(235, 235, 235, 255))
    sheet.save(OUT + 'preview_scrap.png')
    print('ok')
