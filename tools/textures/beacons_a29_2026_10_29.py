"""Simple Beacons A29 (BS28b-4/5): placeholder textures by tint and the level marker icons of the beacon screen.

Placeholders until the owner picks real art (preview /root/sb-tools-a/previews/a-beacon29-preview.html): the Mighty, Evil and
Mighty Evil beacon block textures and the Evil Upgrade template are the Strong Beacon / Strong Upgrade pixels with the hue
moved (Mighty gold, Evil red, Mighty Evil dark red), with matching .layers.json palettes. The marker icons are own pixel art
(a hanging pyramid of six tiers, the first N lit for level N; red set for Evil) in Vanilla's beacon palette colours.

Run: python3.12 tools/textures/beacons_a29_2026_10_29.py [--check]
"""
import colorsys
import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / 'modules/simplebeacons/shared/resources/assets/simplebeacons/textures'
# name -> (hue degrees, saturation factor, value factor)
BLOCK_TINTS = {'mighty_beacon': (42, 1.1, 1.05), 'evil_beacon': (355, 1.3, 0.95), 'mighty_evil_beacon': (345, 1.3, 0.62)}
ITEM_TINTS = {'evil_upgrade_template': (355, 1.3, 0.95)}


def tint_rgb(rgb, hue, sat, val):
    h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    if s < 0.12:  # greys and whites stay (glass, highlights): only the value moves
        r, g, b = colorsys.hsv_to_rgb(h, s, min(1.0, v * val))
    else:
        r, g, b = colorsys.hsv_to_rgb(hue / 360, min(1.0, s * sat), min(1.0, v * val))
    return tuple(round(c * 255) for c in (r, g, b))


def tint_hex(value, spec):
    rgb = tint_rgb(tuple(int(value[i:i + 2], 16) for i in (1, 3, 5)), *spec)
    return '#%02x%02x%02x%s' % (*rgb, value[7:])


def tint_png(src, dst, spec):
    im = Image.open(src).convert('RGBA')
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (*tint_rgb((r, g, b), *spec), a)
    im.save(dst)


def tint_layers(src, dst, spec):
    data = json.loads(src.read_text(encoding='utf-8'))
    data['palette'] = [tint_hex(c, spec) for c in data['palette']]
    dst.write_text(json.dumps(data, indent=None, separators=(', ', ': ')).replace('{"format"', '{"format"'), encoding='utf-8')


# Marker palette: lit = Vanilla's green, dark = Vanilla's dark green; the red set for Evil.
GREEN = {'lit': [(175, 253, 205), (98, 245, 154), (0, 176, 56), (0, 123, 24)], 'dark': [(59, 74, 65), (45, 72, 55), (26, 59, 37), (26, 49, 31)]}
RED = {'lit': [(255, 196, 180), (255, 110, 90), (200, 40, 40), (140, 20, 30)], 'dark': [(74, 59, 59), (72, 45, 45), (59, 30, 30), (49, 26, 26)]}
STRING, LAMP_LIGHT, LAMP = (74, 72, 65), (254, 251, 96), (253, 199, 51)
W, H, TIERS = 21, 20, 6


def icon(lit, palette, sized=False):
    """One marker: string, lamp and six 2-row tiers; the first ``lit`` tiers are lit. ``sized``: only ``lit`` tiers exist (variant B)."""
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    cx = W // 2
    for y in range(3):
        im.putpixel((cx, y), (*STRING, 255))
    for x in range(cx - 1, cx + 2):
        im.putpixel((x, 3), (*LAMP_LIGHT, 255))
    im.putpixel((cx - 1, 4), (*LAMP_LIGHT, 255))
    im.putpixel((cx, 4), (*LAMP, 255))
    im.putpixel((cx + 1, 4), (*LAMP, 255))
    count = lit if sized else TIERS
    for t in range(count):
        width = 5 + 2 * t if not sized else 5 + 2 * t
        state = palette['lit'] if (sized or t < lit) else palette['dark']
        for row in range(2):
            y = 5 + 2 * t + row
            for x in range(cx - width // 2, cx + width // 2 + 1):
                shade = (x + t + row) % 2
                color = state[shade] if row == 0 else state[2 + shade]
                im.putpixel((x, y), (*color, 255))
    return im


def icon_sheet(sized=False):
    sheet = Image.new('RGBA', (W * 6, H * 2), (0, 0, 0, 0))
    for row, palette in enumerate((GREEN, RED)):
        for level in range(1, 7):
            sheet.paste(icon(level, palette, sized), ((level - 1) * W, row * H))
    return sheet


def main(check=False):
    outputs = {}
    for name, spec in BLOCK_TINTS.items():
        outputs[TEX / f'block/{name}.png'] = ('png', TEX / 'block/strong_beacon.png', spec)
        outputs[TEX / f'block/{name}.layers.json'] = ('layers', TEX / 'block/strong_beacon.layers.json', spec)
    for name, spec in ITEM_TINTS.items():
        outputs[TEX / f'item/{name}.png'] = ('png', TEX / 'item/strong_upgrade_template.png', spec)
        outputs[TEX / f'item/{name}.layers.json'] = ('layers', TEX / 'item/strong_upgrade_template.layers.json', spec)
    for dst, (kind, src, spec) in outputs.items():
        if check:
            assert dst.is_file(), dst
            continue
        (tint_png if kind == 'png' else tint_layers)(src, dst, spec)
    gui = TEX / 'gui/container/pyramid_icons.png'
    if check:
        assert gui.is_file(), gui
        return
    gui.parent.mkdir(parents=True, exist_ok=True)
    icon_sheet().save(gui)


if __name__ == '__main__':
    main('--check' in sys.argv)
