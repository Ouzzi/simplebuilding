"""Deceiver textures (own pixel art): entity skin, glowing eyes, cloth item, spawn egg.

    python tools/textures/deceiver_textures.py          # write PNGs + previews
    python tools/textures/deceiver_textures.py --check  # fail if the committed PNGs differ

UV layout (64x64) matches DeceiverModel: robe (0,0) 7x7x5, head/hood (0,12) 6x6x6, hood peak (24,12),
cloak flap (24,0), chest plate (24,18), right arm (48,0), left arm (48,12), legs (0,24) / (12,24).
"""
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs'
PREVIEW = Path('/root/previews/deceiver')

ROBE = [(0x2a, 0x24, 0x44), (0x37, 0x30, 0x58), (0x46, 0x3e, 0x6c)]
DEEP = (0x16, 0x12, 0x26)
TRIM = (0x9a, 0x84, 0x4e)
TRIM_D = (0x6e, 0x5c, 0x36)
SKIN = [(0x8f, 0x86, 0xa6), (0x9a, 0x91, 0xb0), (0xa7, 0x9d, 0xbc)]
BOOT = (0x1c, 0x18, 0x2a)
STEEL = [(0x4c, 0x50, 0x5a), (0x6c, 0x72, 0x7e), (0x8d, 0x94, 0xa2)]
FACE = (0x08, 0x06, 0x10)
EYE = [(0x9f, 0xf3, 0xff), (0xe8, 0xff, 0xff)]


def noise(x, y, k=0):
    return ((x * 73856093) ^ (y * 19349663) ^ (k * 83492791)) & 0xFF


def tone(x, y, ramp, k=0):
    n = noise(x, y, k)
    return ramp[0] if n < 70 else ramp[2] if n > 200 else ramp[1]


def fill(img, x, y, w, h, ramp, k=0):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            img.putpixel((xx, yy), tone(xx, yy, ramp, k))


def box(img, u, v, w, h, d, ramp, k=0):
    """Paint all six faces of a model box with the standard cube unwrap."""
    for (x, y, fw, fh) in [(u + d, v, w, d), (u + d + w, v, w, d), (u, v + d, d, h), (u + d, v + d, w, h),
                           (u + d + w, v + d, d, h), (u + 2 * d + w, v + d, w, h)]:
        fill(img, x, y, fw, fh, ramp, k)


def body(img):
    box(img, 0, 0, 7, 7, 5, ROBE, 1)
    # front folds and a gold hem + central clasp
    for yy in range(5, 12):
        img.putpixel((5 + 3, yy), ROBE[0])
        if yy > 7: img.putpixel((5 + 1, yy), ROBE[0]); img.putpixel((5 + 5, yy), ROBE[0])
    for xx in range(5, 12):
        img.putpixel((xx, 11), TRIM if xx % 2 == 0 else TRIM_D)
        img.putpixel((xx + 12, 11), TRIM if xx % 2 == 0 else TRIM_D)
    for xx in range(0, 5):
        img.putpixel((xx, 11), TRIM if xx % 2 else TRIM_D)
    img.putpixel((8, 5), TRIM); img.putpixel((8, 6), TRIM_D)
    # shoulder cord
    for xx in range(5, 12): img.putpixel((xx, 5), ROBE[2])
    # hood (head box): lit rim + deep shadow
    box(img, 0, 12, 6, 6, 6, ROBE, 2)
    for xx in range(6, 12):
        for yy in range(18, 24):
            img.putpixel((xx, yy), ROBE[2] if xx in (6, 11) or yy == 18 else ROBE[1])
    for xx in range(7, 11):
        for yy in range(19, 24):
            img.putpixel((xx, yy), FACE)
    img.putpixel((7, 19), ROBE[0]); img.putpixel((10, 19), ROBE[0])
    # hood peak
    box(img, 24, 12, 2, 3, 3, ROBE, 3)
    # cloak flap with gold hem
    box(img, 24, 0, 6, 8, 1, ROBE, 4)
    for xx in range(25, 31): img.putpixel((xx, 8), TRIM if xx % 2 else TRIM_D)
    for xx in range(32, 38): img.putpixel((xx, 8), TRIM if xx % 2 else TRIM_D)
    # chest plate
    box(img, 24, 18, 6, 4, 1, STEEL, 5)
    for (x, y) in [(25, 19), (30, 19), (25, 22), (30, 22)]: img.putpixel((x, y), STEEL[2])
    for xx in range(26, 30): img.putpixel((xx, 20), STEEL[0])
    # arms: wide sleeves, pale hands in the lowest two rows
    for v in (0, 12):
        box(img, 48, v, 3, 7, 3, ROBE, 6 + v)
        for (x0, w) in [(48, 3), (51, 3), (54, 3), (57, 3)]:
            for yy in (v + 3 + 5, v + 3 + 6):
                for xx in range(x0, x0 + w): img.putpixel((xx, yy), tone(xx, yy, SKIN, 9))
        for xx in range(48, 60): img.putpixel((xx, v + 3 + 4), TRIM_D)
    # legs/boots
    for u in (0, 12):
        box(img, u, 24, 3, 3, 3, [BOOT, BOOT, (0x2a, 0x25, 0x3c)], 11)


def eyes():
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    for (x, y) in [(7, 20), (7, 21), (10, 20), (10, 21)]:
        img.putpixel((x, y), EYE[0] + (255,))
    img.putpixel((7, 20), EYE[1] + (255,)); img.putpixel((10, 20), EYE[1] + (255,))
    return img


def skin():
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    body(img)
    return img


def outline_item(img, rows):
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c in PAL: img.putpixel((x, y), PAL[c] + (255,))


PAL = {'a': (0x1c, 0x17, 0x30), 'b': (0x37, 0x30, 0x58), 'c': (0x58, 0x4e, 0x86), 'd': (0x7a, 0x70, 0xb0),
       'g': TRIM, 'h': TRIM_D, 'e': EYE[0], 'w': EYE[1], 'k': (0x10, 0x0d, 0x1c)}


def cloth():
    rows = [
        "................",
        "......kkkk......",
        "....kkbbccdk....",
        "...kbbccddcck...",
        "..kbcddccbbcck..",
        ".kbccdd.wcbbbck.",
        ".kbcdcckkccbbbk.",
        ".kccbbcbbccddck.",
        ".kcbbcgcbbcdcck.",
        ".kbbcchgcccbbck.",
        ".kbcccbbbccbbbk.",
        "..kbccbbcdccbk..",
        "..kkbbbccdcbkk..",
        "...kkkbbbbkkk...",
        ".....kkkkkk.....",
        "................",
    ]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); outline_item(img, rows); return img


def egg():
    rows = [
        "................",
        "......kkkk......",
        ".....kbbccd.....",
        "....kbbcccddk...",
        "...kbbeecccddk..",
        "...kbbbcccdddk..",
        "..kbcccbbcdddck.",
        "..kbccbbbbcddck.",
        "..kbcceebccbbck.",
        "..kbbcbbbccbbck.",
        "..kabbbcccbbcck.",
        "...kabbbbbbbck..",
        "...kkabbbbbckk..",
        ".....kkaaakk....",
        "................",
        "................",
    ]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); outline_item(img, rows); return img


def outputs():
    return {
        RES / 'textures/entity/deceiver/deceiver.png': skin(),
        RES / 'textures/entity/deceiver/deceiver_eyes.png': eyes(),
        RES / 'textures/item/deceiver_cloth.png': cloth(),
        RES / 'textures/item/deceiver_spawn_egg.png': egg(),
    }


def preview(items):
    PREVIEW.mkdir(parents=True, exist_ok=True)
    sheet = Image.new('RGBA', (64 * 8 + 16 * 8 * 2 + 40, 64 * 8), (60, 60, 66, 255))
    s = items[RES / 'textures/entity/deceiver/deceiver.png'].resize((512, 512), Image.NEAREST)
    sheet.paste(s, (0, 0), s)
    e = items[RES / 'textures/entity/deceiver/deceiver_eyes.png'].resize((512, 512), Image.NEAREST)
    sheet.paste(e, (0, 0), e)
    for i, n in enumerate(['deceiver_cloth', 'deceiver_spawn_egg']):
        t = items[RES / f'textures/item/{n}.png'].resize((128, 128), Image.NEAREST)
        sheet.paste(t, (512 + 20, 20 + i * 150), t)
    sheet.save(PREVIEW / 'textures.png')


def main():
    items = outputs()
    if '--check' in sys.argv:
        bad = [str(p) for p, im in items.items() if not p.is_file() or Image.open(p).convert('RGBA').tobytes() != im.tobytes()]
        if bad:
            print('stale:', *bad, sep='\n  '); sys.exit(1)
        print('deceiver textures current'); return
    for p, im in items.items():
        p.parent.mkdir(parents=True, exist_ok=True); im.save(p)
    preview(items)


if __name__ == '__main__':
    main()
