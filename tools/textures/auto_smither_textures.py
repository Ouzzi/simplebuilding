"""Usage: python tools/textures/auto_smither_textures.py <out-dir> <vanilla textures dir> [preview.png]

Auto Smither block textures (owner 2026-10-02: "Auto-Schmied analog zum Autocrafter"). Derived from vanilla:
the crafter's stone casing and redstone keep their pixels; the crafter's light planks take the smithing table's
red-brown wood (same shading ranks), and the top shows the smithing table's dark iron plate inside the crafter's
rim instead of the crafting grid. Front/side/top get the crafter's "crafting" and "triggered" states.
Output goes to mc26_3/overlay/resources/assets/simplebuilding/textures/block/."""
from PIL import Image, ImageDraw
import os
import sys

V = sys.argv[2] if len(sys.argv) > 2 else 'build/vanilla-textures/'  # vanilla assets/minecraft/textures/
OUT = sys.argv[1] if len(sys.argv) > 1 else 'build/auto-smither-textures/'
PREVIEW = sys.argv[3] if len(sys.argv) > 3 else None


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def load(name):
    return Image.open(os.path.join(V, 'block', name + '.png')).convert('RGBA').crop((0, 0, 16, 16))


def is_wood(p):
    # the crafter's planks: warm, clearly more red than blue (the casing is neutral grey, redstone is pure red)
    r, g, b = p[:3]
    return r - b > 35 and g - b > 20


def wood_ramp():
    src = load('smithing_table_front')
    # only the planks: red-brown with green under half of red (the hammer handle is orange and stays out)
    tones = sorted({p[:3] for p in src.getdata() if p[0] - p[2] > 12 and p[1] < 0.5 * p[0]}, key=lum)
    return tones


RAMP = None


def rewood(img):
    out = img.copy()
    wood = [(x, y) for y in range(16) for x in range(16) if is_wood(img.getpixel((x, y)))]
    if not wood:
        return out
    levels = sorted({lum(img.getpixel(p)) for p in wood})
    for p in wood:
        rank = levels.index(lum(img.getpixel(p))) / max(1, len(levels) - 1)
        c = RAMP[min(len(RAMP) - 1, int(round(rank * (len(RAMP) - 1))))]
        out.putpixel(p, c + (255,))
    return out


def top(state):
    rim = load('crafter_top' + state)
    plate = load('smithing_table_top')
    out = rim.copy()
    # inside the crafter's rim (2 px casing + 1 px dark frame) the crafting grid becomes the smithing iron plate
    for y in range(3, 13):
        for x in range(3, 13):
            out.putpixel((x, y), plate.getpixel((x, y)))
    return out


def build():
    global RAMP
    RAMP = wood_ramp()
    return {
        'auto_smither_front': rewood(load('crafter_north')),
        'auto_smither_front_crafting': rewood(load('crafter_north_crafting')),
        'auto_smither_side': rewood(load('crafter_south')),
        'auto_smither_side_triggered': rewood(load('crafter_south_triggered')),
        'auto_smither_top': top(''),
        'auto_smither_top_triggered': top('_triggered'),
        'auto_smither_bottom': load('crafter_bottom'),
    }


def preview(textures, path):
    scale, pad = 8, 12
    names = list(textures)
    w = len(names) * (16 * scale + pad) + pad
    sheet = Image.new('RGBA', (w, 16 * scale + 2 * pad + 14), (198, 198, 198, 255))
    draw = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        x = pad + i * (16 * scale + pad)
        sheet.paste(textures[n].resize((16 * scale, 16 * scale), Image.NEAREST), (x, pad))
        draw.text((x, pad + 16 * scale + 2), chr(ord('A') + i) + ' ' + n.replace('auto_smither_', ''), fill=(40, 40, 40, 255))
    sheet.save(path)


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    tex = build()
    for name, img in tex.items():
        img.save(os.path.join(OUT, name + '.png'))
    if PREVIEW:
        preview(tex, PREVIEW)
