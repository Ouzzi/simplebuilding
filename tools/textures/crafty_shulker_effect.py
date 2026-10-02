"""Usage: python tools/textures/crafty_shulker_effect.py <vanilla textures dir> [preview.png]

Crafty Shulker effect icon (owner 2026-10-02, docs/ai/PLAN-COMBAT-2026-10-02.md): vanilla item/shulker_shell pixel for
pixel on the 18x18 mob-effect canvas (one pixel of air around it, like the vanilla icons), plus two small teleport
sparkles in the purple of the shulker teleport particles. Writes
mc26_3/overlay/resources/assets/simplebuilding/textures/mob_effect/crafty_shulker.png and, if given, a preview sheet:
A = vanilla levitation (reference style), B = vanilla shulker shell (source), C = the new icon,
D = potion bottle in the effect colour (ModEffects.CRAFTY_SHULKER_COLOR), E = the icon on the dark HUD background."""
from PIL import Image, ImageDraw
import os
import sys

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'  # vanilla assets/minecraft/textures/
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
OUT = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'mob_effect',
                   'crafty_shulker.png')
COLOR = (0xC0, 0x8E, 0xD6)  # ModEffects.CRAFTY_SHULKER_COLOR
LIGHT = (243, 214, 255, 255)
MID = (196, 140, 222, 255)


def load(name):
    return Image.open(os.path.join(V, name + '.png')).convert('RGBA')


def icon():
    out = Image.new('RGBA', (18, 18), (0, 0, 0, 0))
    out.alpha_composite(load('item/shulker_shell'), (0, 2))  # shell rows 1..14 -> 3..16, cols 2..15
    # sparkle: a plus with a light centre, top right, clear of the shell outline and the canvas border
    for x, y, c in [(15, 2, LIGHT), (14, 2, MID), (16, 2, MID), (15, 1, MID), (15, 3, MID), (12, 1, MID), (16, 6, LIGHT)]:
        if out.getpixel((x, y))[3] == 0:
            out.putpixel((x, y), c)
    for x in range(18):
        assert out.getpixel((x, 0))[3] == 0 and out.getpixel((x, 17))[3] == 0, 'paints on the border'
        assert out.getpixel((0, x))[3] == 0 and out.getpixel((17, x))[3] == 0, 'paints on the border'
    return out


def tinted_potion():
    bottle = load('item/potion').copy()
    overlay = load('item/potion_overlay')
    tint = Image.new('RGBA', overlay.size)
    for y in range(overlay.height):
        for x in range(overlay.width):
            p = overlay.getpixel((x, y))
            if p[3]:
                tint.putpixel((x, y), tuple(p[i] * COLOR[i] // 255 for i in range(3)) + (p[3],))
    bottle.alpha_composite(tint)
    return bottle


def preview(made, path):
    scale = 16
    tiles = [('A', load('mob_effect/levitation')), ('B', load('item/shulker_shell')), ('C', made),
             ('D', tinted_potion())]
    dark = Image.new('RGBA', (18, 18), (40, 40, 40, 255))
    dark.alpha_composite(made)
    tiles.append(('E', dark))
    cell = 18 * scale + 24
    sheet = Image.new('RGBA', (cell * len(tiles) + 24, cell + 48), (198, 198, 198, 255))
    draw = ImageDraw.Draw(sheet)
    for i, (label, im) in enumerate(tiles):
        big = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
        x = 24 + i * cell + (18 * scale - big.width) // 2
        sheet.alpha_composite(big, (x, 40 + (18 * scale - big.height) // 2))
        draw.text((24 + i * cell, 12), label, fill=(0, 0, 0, 255))
    os.makedirs(os.path.dirname(path) or '.', exist_ok=True)
    sheet.save(path)


def main():
    made = icon()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    made.save(OUT)
    print('wrote', os.path.normpath(OUT))
    if PREVIEW:
        preview(made, PREVIEW)
        print('preview', PREVIEW)


if __name__ == '__main__':
    main()
