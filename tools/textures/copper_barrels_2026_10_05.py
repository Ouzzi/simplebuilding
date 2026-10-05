"""Usage: python tools/textures/copper_barrels_2026_10_05.py <vanilla textures dir> [preview.png]

Copper barrel textures for SimpleLib (plan section 8a). From the Vanilla barrel: its wooden staves take
the copper block's ramp (same luminance ranks), the iron hoops stay dark; the reinforced tier adds a
cracked-diamond hoop band and diamond rivets (shape detail, not only a recolor), plus a copper flange
texture for the connection to the crucible. Output goes to
modules/simplelib/shared/resources/assets/simplelib/textures/block/.
"""
import os
import sys
from PIL import Image, ImageDraw

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'modules', 'simplelib', 'shared', 'resources',
                   'assets', 'simplelib', 'textures', 'block')
DIAMOND = [(26, 120, 132), (44, 182, 190), (94, 228, 214), (186, 250, 240)]


def load(name):
    return Image.open(os.path.join(V, 'block', name + '.png')).convert('RGBA').crop((0, 0, 16, 16))


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def is_wood(p):
    r, g, b = p[:3]
    return r - b > 25


def copperize(img, ramp):
    wood = sorted({p[:3] for p in img.get_flattened_data() if p[3] and is_wood(p)}, key=lum)
    if not wood:
        return img.copy()
    lo, hi = lum(wood[0]), lum(wood[-1])
    out = img.copy()
    px = out.load()
    for y in range(16):
        for x in range(16):
            p = px[x, y]
            if p[3] and is_wood(p):
                t = 0 if hi == lo else (lum(p) - lo) / (hi - lo)
                px[x, y] = (*ramp[min(len(ramp) - 1, int(round(t * (len(ramp) - 1))))], p[3])
    return out


def band(img, rows):
    px = img.load()
    for y in rows:
        for x in range(16):
            px[x, y] = (*DIAMOND[1 if x % 4 else 3], 255)


def flange(ramp):
    img = Image.new('RGBA', (16, 16), (*ramp[2], 255))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(*ramp[0], 255))
    d.rectangle([3, 3, 12, 12], outline=(*ramp[-1], 255))
    return img


def main():
    ramp = sorted({p[:3] for p in load('copper_block').get_flattened_data() if p[3]}, key=lum)
    os.makedirs(OUT, exist_ok=True)
    sheets = []
    for tier in ('copper', 'reinforced'):
        tex = {n: copperize(load('barrel_' + n), ramp) for n in ('side', 'top', 'top_open', 'bottom')}
        if tier == 'reinforced':
            band(tex['side'], (1, 14))
        tex['flange'] = flange(ramp)
        for name, img in tex.items():
            img.save(os.path.join(OUT, f'{tier}_barrel_{name}.png'))
        sheets.append((tier, tex))
    if PREVIEW:
        scale = 12
        prev = Image.new('RGBA', (5 * (16 * scale + 8) + 8, 2 * (16 * scale + 40) + 10), (40, 40, 40, 255))
        d = ImageDraw.Draw(prev)
        for row, (tier, tex) in enumerate(sheets):
            y = 10 + row * (16 * scale + 40)
            d.text((8, y), f"{'AB'[row]}: {tier}_barrel (side, top, top_open, bottom, flange)", fill=(255, 255, 255, 255))
            for col, part in enumerate(('side', 'top', 'top_open', 'bottom', 'flange')):
                big = tex[part].resize((16 * scale, 16 * scale), Image.NEAREST)
                prev.paste(big, (8 + col * (16 * scale + 8), y + 16), big)
        os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
        prev.save(PREVIEW)


if __name__ == '__main__':
    main()
