"""Simple Beacons textures (2026-10-13, docs/ai/PLAN-SIMPLE-BEACONS-2026-10-13.md).

Own pixel art, no Vanilla pixels: the Strong Upgrade template is an upright obsidian tablet (own shape, not the Vanilla
template outline) with a nether-star cross in the middle and two beacon-cyan glints; the Strong Beacon core is a star
glow (white centre, gold rays, lilac rim) that sits in Vanilla's glass shell on crying obsidian (the model only refers
to those Vanilla textures). Light from the top left, nothing drawn on the outermost pixel row.

Writes modules/simplebeacons/.../textures/{item/strong_upgrade_template,block/strong_beacon}.png and a 16x preview to
SB_PREVIEW_DIR (default /root/previews/beacons). Run: python3.12 tools/textures/simple_beacons_2026_10_13.py [--check]
"""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', '..', 'modules', 'simplebeacons', 'shared', 'resources', 'assets', 'simplebeacons', 'textures')
PREVIEW = os.environ.get('SB_PREVIEW_DIR', '/root/previews/beacons')

OBSIDIAN = [(14, 9, 22), (30, 19, 46), (46, 30, 70), (66, 45, 98), (94, 68, 132)]
STAR = [(132, 92, 28), (206, 162, 58), (246, 220, 124), (255, 250, 214)]
CYAN = [(70, 200, 210), (190, 252, 250)]
LILAC = [(84, 52, 118), (132, 92, 170), (184, 148, 214)]


def tablet():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    x0, x1, y0, y1 = 2, 13, 1, 14
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            corner = (x in (x0, x1)) and (y in (y0, y1))
            if corner:
                continue
            edge = x in (x0, x1) or y in (y0, y1) or ((x in (x0 + 1, x1 - 1)) and (y in (y0, y1)))
            if edge:
                px[x, y] = OBSIDIAN[0] + (255,)
                continue
            # Bevel: top/left light, bottom/right dark, field mid with a little grain.
            if y == y0 + 1 or x == x0 + 1:
                c = OBSIDIAN[4] if (x + y) % 3 else OBSIDIAN[3]
            elif y == y1 - 1 or x == x1 - 1:
                c = OBSIDIAN[1]
            else:
                c = OBSIDIAN[2] if (x * 7 + y * 3) % 5 else OBSIDIAN[3]
            px[x, y] = c + (255,)
    # Hanging slot at the top.
    for x in (7, 8):
        px[x, y0 + 1] = OBSIDIAN[0] + (255,)
    star(px, 7.5, 7.5, 4)
    px[4, 4] = CYAN[1] + (255,)
    px[11, 11] = CYAN[0] + (255,)
    px[10, 12] = CYAN[1] + (255,)
    return img


def star(px, cx, cy, arm):
    """Four-pointed star: 2 px arms tapering to a point, a diamond core; shaded from the centre out."""
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x - cx), abs(y - cy)
            inside = (dx <= 0.5 and dy <= arm + 0.5) or (dy <= 0.5 and dx <= arm + 0.5) or (dx + dy <= 2.0)
            if not inside:
                continue
            d = max(dx, dy) if (dx <= 0.5 or dy <= 0.5) else dx + dy
            if d <= 0.6:
                c = STAR[3]
            elif d <= 1.6:
                c = STAR[2]
            elif d <= arm - 0.6:
                c = STAR[1]
            else:
                c = STAR[0]
            # Light from the top left: the lower right half of the arms one step darker.
            if (x - cx) + (y - cy) > 1.5 and c != STAR[0]:
                c = STAR[STAR.index(c) - 1]
            px[x, y] = c + (255,)


def core():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 255))
    px = img.load()
    for y in range(16):
        for x in range(16):
            # The model shows u 3..13, v 2..13 (sides) / 3..13 (top): a lilac rim around that window.
            rim = x in (3, 12) or y in (2, 12)
            px[x, y] = (LILAC[0] if rim else LILAC[1]) + (255,)
    for y in range(3, 12):
        for x in range(4, 12):
            d = abs(x - 7.5) + abs(y - 7.0)
            px[x, y] = (LILAC[2] if d <= 4.0 else LILAC[1]) + (255,)
    for x, y in ((5, 4), (10, 10), (10, 4), (5, 10)):
        px[x, y] = CYAN[1] + (255,)
    star(px, 7.5, 7.5, 3)
    return img


def outputs():
    return {os.path.join(ASSETS, 'item', 'strong_upgrade_template.png'): tablet(),
            os.path.join(ASSETS, 'block', 'strong_beacon.png'): core()}


def main():
    check = '--check' in sys.argv
    stale = []
    for path, img in outputs().items():
        if check:
            if not os.path.exists(path) or Image.open(path).convert('RGBA').tobytes() != img.tobytes():
                stale.append(path)
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
    if check:
        if stale:
            raise SystemExit('stale: ' + ', '.join(stale))
        print('simple beacons textures: up to date')
        return
    os.makedirs(PREVIEW, exist_ok=True)
    imgs = list(outputs().values())
    sheet = Image.new('RGBA', (16 * 16 * 2 + 48, 16 * 16 + 40), (198, 198, 198, 255))
    for i, img in enumerate(imgs):
        sheet.alpha_composite(img.resize((256, 256), Image.Resampling.NEAREST), (16 + i * (256 + 16), 16))
    ImageDraw.Draw(sheet).text((16, 16 * 16 + 22), 'strong_upgrade_template | strong_beacon (core)', fill=(20, 20, 20, 255))
    sheet.save(os.path.join(PREVIEW, 'textures_16x.png'))
    print('preview:', os.path.join(PREVIEW, 'textures_16x.png'))


if __name__ == '__main__':
    main()
