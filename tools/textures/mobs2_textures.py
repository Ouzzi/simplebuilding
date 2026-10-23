"""Shellker + Soulfire Blaze textures (Simple Mobs), own pixel art.

    python tools/textures/mobs2_textures.py          # write PNGs + previews (~/previews/mobs2)
    python tools/textures/mobs2_textures.py --check  # fail if the committed PNGs differ

Shellker: bedrock disguise (dark grey noise with black pits and pale flecks, drawn here), a violet eye that only
shows through the slit. UV layouts follow the Vanilla models: ShulkerModel 64x64 (lid 0,0 16x12x16, base 0,28
16x8x16, head 0,52 6x6x6; the lid's lower 4 rows and the base's upper 4 rows interlock like Vanilla's),
ShulkerBulletModel 64x32 (0,0 8x8x2, 0,10 2x8x8, 20,0 8x2x8), BlazeModel 64x32 (head 0,0 8x8x8, rods 0,16 2x8x2),
humanoid armor 64x32 (head 0,0 8x8x8).
"""
import os
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs'
PREVIEW = Path(os.environ.get('SB_PREVIEW_DIR', os.path.expanduser('~/previews')) + '/mobs2')
T = (0, 0, 0, 0)

# bedrock ramp: pits, dark, mid, light, flecks (own values, cooler than stone)
BED = [(0x1f, 0x1f, 0x21), (0x34, 0x34, 0x36), (0x52, 0x52, 0x55), (0x64, 0x64, 0x67), (0x8c, 0x8c, 0x8f), (0xa2, 0xa2, 0xa4)]
VIOLET = [(0x2a, 0x12, 0x3a), (0x55, 0x22, 0x77), (0x9a, 0x4c, 0xd8), (0xd9, 0xa8, 0xff), (0xf6, 0xe8, 0xff)]
# soul fire: petrol to cyan
SOUL = [(0x06, 0x1c, 0x24), (0x0b, 0x34, 0x40), (0x13, 0x55, 0x66), (0x1f, 0x86, 0x96), (0x3c, 0xc4, 0xcc), (0x8a, 0xf2, 0xf0), (0xe0, 0xff, 0xfb)]
K = (0x0c, 0x0c, 0x0e)


def h(x, y, k=0):
    v = (x * 374761393 + y * 668265263 + k * 2147483647) & 0xFFFFFFFF
    v = ((v ^ (v >> 13)) * 1274126177) & 0xFFFFFFFF
    return (v ^ (v >> 16)) & 0xFF


def faces(u, v, w, hh, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, hh),
            'front': (u + d, v + d, w, hh), 'left': (u + d + w, v + d, d, hh), 'back': (u + 2 * d + w, v + d, w, hh)}


def bedrock_face(k):
    """16x16 bedrock-like face: short horizontal runs with 1 px detail, black pits, pale flecks (tones near Vanilla's)."""
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            coarse = h((x + (y % 2)) // 2, y, k) / 255.0   # short horizontal runs, offset per row
            fine = h(x, y, k + 11) / 255.0
            v = 0.7 * coarse + 0.3 * fine
            idx = 1 if v < 0.38 else 2 if v < 0.58 else 3 if v < 0.74 else 4
            n = h(x, y, k + 23)
            if n < 16:
                idx = 0
            elif n > 248:
                idx = 5
            img.putpixel((x, y), BED[idx] + (255,))
    # a few dark cracks running diagonally, Vanilla-like irregular pits
    for sx, sy, ln in ((2 + k % 5, 3, 4), (9, 8 + k % 3, 5), (4, 12, 3)):
        for i in range(ln):
            x, y = sx + i, sy + (i // 2)
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), BED[0] + (255,))
                if y + 1 < 16 and h(x, y, k + 5) > 140:
                    img.putpixel((x, y + 1), BED[1] + (255,))
    return img


# Vanilla Shulker UV usage: which pixels of the 64x64 sheet are drawn (layout only, art is ours)
def shulker_mask(x, y):
    if y < 16:   # lid top (16..31) and lid bottom frame (32..47)
        if 16 <= x < 32:
            return True
        if y in (0, 15):
            return 32 <= x < 36 or 44 <= x < 48
        if y in (1, 2, 3, 12, 13, 14):
            return x == 32 or x == 47
        return False
    if y < 24:
        return True
    if y < 28:   # lid sides, lower 4 rows: only 4 px borders per face
        return (x % 16) < 4 or (x % 16) >= 12
    if y < 44:   # base top (16..31, frame) and base bottom (32..47)
        if 32 <= x < 48:
            return True
        if y in (28, 43):
            return 20 <= x < 28
        if 32 <= y < 40:
            return x == 16 or x == 31
        return False
    if y < 48:   # base sides, upper 4 rows: only the middle 8 px
        return 4 <= (x % 16) < 12
    if y < 52:
        return True
    if y < 58:
        return 6 <= x < 18
    return x < 24


def shellker():
    img = Image.new('RGBA', (64, 64), T)
    sides = [bedrock_face(k) for k in range(4)]
    top, bottom = bedrock_face(7), bedrock_face(9)
    # lid: top face, then the four sides (rows 0..11 of each 16x16 side)
    img.paste(top, (16, 0))
    for i in range(4):
        img.paste(sides[i].crop((0, 0, 16, 12)), (i * 16, 16))
    # base: bottom face, then the sides (rows 8..15 of the same side faces, so lid+base read as one block)
    img.paste(bottom, (32, 28))
    for i in range(4):
        img.paste(sides[i].crop((0, 8, 16, 16)), (i * 16, 44))
    # inner faces (lid bottom frame, base top frame): darker, a violet sheen inside
    for y in range(0, 16):
        for x in range(32, 48):
            img.putpixel((x, y), VIOLET[0] + (255,) if h(x, y, 3) > 90 else BED[0] + (255,))
    for y in range(28, 44):
        for x in range(16, 32):
            img.putpixel((x, y), VIOLET[0] + (255,) if h(x, y, 4) > 90 else BED[0] + (255,))
    # head 6x6x6: dark violet flesh, the eye on the front (rows 2..3 show through the slit)
    for name, (fx, fy, fw, fh) in faces(0, 52, 6, 6, 6).items():
        for y in range(fy, fy + fh):
            for x in range(fx, fx + fw):
                img.putpixel((x, y), (VIOLET[1] if h(x, y, 8) > 160 else VIOLET[0]) + (255,))
    fx, fy, _, _ = faces(0, 52, 6, 6, 6)['front']
    for x in range(fx + 1, fx + 5):
        img.putpixel((x, fy + 2), VIOLET[2] + (255,))
        img.putpixel((x, fy + 3), VIOLET[2] + (255,))
    img.putpixel((fx + 2, fy + 2), VIOLET[4] + (255,)); img.putpixel((fx + 3, fy + 2), VIOLET[3] + (255,))
    img.putpixel((fx + 2, fy + 3), VIOLET[3] + (255,)); img.putpixel((fx + 3, fy + 3), K + (255,))
    # Vanilla layout mask
    for y in range(64):
        for x in range(64):
            if not shulker_mask(x, y):
                img.putpixel((x, y), T)
    return img


def shellker_eyes():
    """Emissive layer: only the eye and a faint rim inside the slit."""
    img = Image.new('RGBA', (64, 64), T)
    fx, fy, _, _ = faces(0, 52, 6, 6, 6)['front']
    for x in range(fx + 1, fx + 5):
        img.putpixel((x, fy + 2), VIOLET[2] + (255,))
        img.putpixel((x, fy + 3), VIOLET[2] + (255,))
    img.putpixel((fx + 2, fy + 2), VIOLET[4] + (255,)); img.putpixel((fx + 3, fy + 2), VIOLET[3] + (255,))
    img.putpixel((fx + 2, fy + 3), VIOLET[3] + (255,))
    for side in ('left', 'right'):
        sx, sy, _, _ = faces(0, 52, 6, 6, 6)[side]
        img.putpixel((sx + 2, sy + 2), VIOLET[1] + (255,)); img.putpixel((sx + 3, sy + 3), VIOLET[1] + (255,))
    return img


def spark():
    img = Image.new('RGBA', (64, 32), T)
    for (u, v, w, hh, d) in ((0, 0, 8, 8, 2), (0, 10, 2, 8, 8), (20, 0, 8, 2, 8)):
        for name, (fx, fy, fw, fh) in faces(u, v, w, hh, d).items():
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    ex = min(x - fx, fx + fw - 1 - x)
                    ey = min(y - fy, fy + fh - 1 - y)
                    e = min(ex, ey)
                    c = BED[3] if e == 0 else VIOLET[2] if e == 1 else VIOLET[3] if e == 2 else VIOLET[4]
                    if e == 0 and (x + y) % 3 == 0:
                        c = BED[2]
                    img.putpixel((x, y), c + (255,))
    return img


def soulfire_blaze():
    img = Image.new('RGBA', (64, 32), T)
    f = faces(0, 0, 8, 8, 8)
    for name, (fx, fy, fw, fh) in f.items():
        for y in range(fy, fy + fh):
            for x in range(fx, fx + fw):
                n = h(x, y, 31)
                c = SOUL[2] if n < 110 else SOUL[3] if n < 210 else SOUL[4]
                if y == fy + fh - 1 or (name == 'top' and n > 235):
                    c = SOUL[1]
                img.putpixel((x, y), c + (255,))
    # face: heavy brow, glowing slit eyes, dark mouth with a cyan core glint
    fx, fy, _, _ = f['front']
    for x in range(fx, fx + 8):
        img.putpixel((x, fy + 2), SOUL[1] + (255,))
    for ex in (fx + 1, fx + 5):
        img.putpixel((ex, fy + 3), SOUL[6] + (255,)); img.putpixel((ex + 1, fy + 3), SOUL[5] + (255,))
        img.putpixel((ex, fy + 4), SOUL[0] + (255,)); img.putpixel((ex + 1, fy + 4), SOUL[0] + (255,))
    for x in range(fx + 2, fx + 6):
        img.putpixel((x, fy + 6), SOUL[0] + (255,))
    img.putpixel((fx + 3, fy + 6), SOUL[5] + (255,))
    for x in (fx, fx + 7):
        img.putpixel((x, fy + 5), SOUL[5] + (255,))
    # rods 2x8x2: banded, dark petrol shadow side, bright cyan core streak
    for name, (rx, ry, rw, rh) in faces(0, 16, 2, 8, 2).items():
        for y in range(ry, ry + rh):
            for x in range(rx, rx + rw):
                band = (y - ry) % 3
                c = SOUL[5] if band == 0 else SOUL[4] if band == 1 else SOUL[3]
                if name in ('left', 'back') and band != 0:
                    c = SOUL[2]
                if name in ('top', 'bottom'):
                    c = SOUL[6] if name == 'top' else SOUL[2]
                img.putpixel((x, y), c + (255,))
    return img


def armor_layer():
    """Humanoid equipment layer (helmet only): bedrock cap, violet visor slit on the front."""
    img = Image.new('RGBA', (64, 32), T)
    face = bedrock_face(13)
    f = faces(0, 0, 8, 8, 8)
    for name, (fx, fy, fw, fh) in f.items():
        for y in range(fy, fy + fh):
            for x in range(fx, fx + fw):
                img.putpixel((x, y), face.getpixel(((x * 3) % 16, (y * 5) % 16)))
        if name in ('front', 'left', 'right', 'back'):   # open below the cap like Vanilla helmets (face visible)
            for y in range(fy + 5, fy + fh):
                for x in range(fx, fx + fw):
                    if name == 'front' and (x in (fx, fx + fw - 1)):
                        continue
                    if name != 'front' and y < fy + 7:
                        continue
                    img.putpixel((x, y), T)
    fx, fy, fw, _ = f['front']
    for x in range(fx + 1, fx + fw - 1):
        img.putpixel((x, fy + 4), (VIOLET[2] if x in (fx + 2, fx + 5) else VIOLET[1]) + (255,))
    for x in range(fx, fx + fw):
        img.putpixel((x, fy + 3), BED[1] + (255,))
    return img


def egg(base, dark, spot, spot2):
    img = Image.new('RGBA', (16, 16), T)
    rows = {2: (6, 9), 3: (5, 10), 4: (4, 11), 5: (4, 11), 6: (3, 12), 7: (3, 12), 8: (3, 12), 9: (3, 12), 10: (3, 12), 11: (4, 11), 12: (4, 11), 13: (5, 10), 14: (6, 9)}
    for y, (a, b) in rows.items():
        for x in range(a, b + 1):
            edge = x in (a, b) or y in (2, 14) or y not in rows or not (rows.get(y - 1, (99, -1))[0] <= x <= rows.get(y - 1, (99, -1))[1]) or not (rows.get(y + 1, (99, -1))[0] <= x <= rows.get(y + 1, (99, -1))[1])
            c = dark if edge else base
            if not edge and (x - a) >= (b - a) - 1 or (not edge and y >= 12):
                c = dark
            img.putpixel((x, y), c + (255,))
    for (x, y), c in (((6, 5), spot), ((7, 5), spot), ((9, 7), spot2), ((5, 9), spot), ((8, 10), spot2), ((9, 10), spot2), ((10, 5), spot2), ((6, 4), spot2)):
        img.putpixel((x, y), c + (255,))
    img.putpixel((5, 4), (255, 255, 255, 110))
    return img


def shell_item(vanilla):
    """Shellker Shell (owner 12.10.): Vanilla's item/shulker_shell.png, recoloured - the purple shell becomes the
    Shellker's bedrock (by brightness rank, with pits and flecks on the big field) and the inner rim (the shell's
    lightest-but-one purple, the inside) shows the Shellker's violet. Vanilla-derived: built, never committed."""
    img = Image.new('RGBA', (16, 16), T)
    colors = sorted({vanilla.getpixel((x, y))[:3] for x in range(16) for y in range(16) if vanilla.getpixel((x, y))[3]},
                    key=sum)
    # Vanilla 26.3 has 7 purples: outline dark, outline light, shade, side, inner rim, field, highlight.
    ramp = [K, BED[1], BED[2], BED[4], None, 'field', BED[5]]
    if len(colors) != len(ramp):
        ramp = [BED[min(5, i * 6 // len(colors))] for i in range(len(colors))]
    for y in range(16):
        for x in range(16):
            px = vanilla.getpixel((x, y))
            if not px[3]:
                continue
            c = ramp[colors.index(px[:3])]
            if c is None:
                c = VIOLET[2] if (x + y) % 3 else VIOLET[1]
            elif c == 'field':
                n = h(x, y, 21)
                c = BED[1] if n < 30 else BED[2] if n < 80 else BED[4] if n > 225 else BED[3]
            img.putpixel((x, y), c + (255,))
    return img


def soul_recolor(vanilla):
    """Soulfire Charge / Soulfire Powder (N49): Vanilla's fire charge / blaze powder with the same pixels and outline,
    the oranges replaced by the soul-blue ramp in order of brightness. Vanilla-derived: built, never committed."""
    img = Image.new('RGBA', (16, 16), T)
    px = {(x, y): vanilla.getpixel((x, y)) for x in range(16) for y in range(16)}
    colors = sorted({c[:3] for c in px.values() if c[3]}, key=lambda c: 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2])
    for (x, y), c in px.items():
        if c[3]:
            rank = colors.index(c[:3]) * (len(SOUL) - 1) // max(1, len(colors) - 1)
            img.putpixel((x, y), SOUL[rank] + (c[3],))
    return img


def derived(jar):
    """Vanilla-derived output for tools/textures/vanilla_derived.py (module simplemobs): texture path -> image."""
    import io
    import zipfile
    with zipfile.ZipFile(jar) as z:
        vanilla = Image.open(io.BytesIO(z.read('assets/minecraft/textures/item/shulker_shell.png'))).convert('RGBA')
        charge = Image.open(io.BytesIO(z.read('assets/minecraft/textures/item/fire_charge.png'))).convert('RGBA')
        powder = Image.open(io.BytesIO(z.read('assets/minecraft/textures/item/blaze_powder.png'))).convert('RGBA')
    return {'item/shellker_shell.png': shell_item(vanilla), 'item/soul_fire_charge.png': soul_recolor(charge),
            'item/soulfire_powder.png': soul_recolor(powder)}


def helmet_item():
    """Shellker Helmet: rounded bedrock cap with a low brim and a violet visor slit (own outline, not the turtle shell)."""
    img = Image.new('RGBA', (16, 16), T)
    face = bedrock_face(17)
    rows = {3: (5, 10), 4: (3, 12), 5: (2, 13), 6: (2, 13), 7: (1, 14), 8: (1, 14), 9: (1, 14), 10: (1, 4), 11: (1, 4)}
    rows_r = {10: (11, 14), 11: (11, 14)}
    def put_row(y, a, b):
        for x in range(a, b + 1):
            edge = x in (a, b) or y == 3 or (y - 1 in rows and not (rows[y - 1][0] <= x <= rows[y - 1][1]))
            c = K if edge else face.getpixel((x, y))[:3]
            img.putpixel((x, y), c + (255,))
    for y, (a, b) in rows.items():
        put_row(y, a, b)
    for y, (a, b) in rows_r.items():
        put_row(y, a, b)
    for x in range(1, 15):
        img.putpixel((x, 12), K + (255,))
    for x in range(3, 13):   # visor slit
        img.putpixel((x, 8), (VIOLET[2] if x in (5, 6, 9, 10) else VIOLET[1]) + (255,))
    for x in range(2, 14):
        img.putpixel((x, 7), BED[1] + (255,))
    img.putpixel((6, 4), BED[5] + (255,)); img.putpixel((7, 4), BED[4] + (255,)); img.putpixel((5, 5), BED[4] + (255,))
    return img


def rod_item():
    """Soulfire Rod: a twisted petrol rod rising diagonally, cyan core, a small soul-flame wisp at the tip."""
    img = Image.new('RGBA', (16, 16), T)
    for i in range(11):
        x, y = 3 + i, 14 - i
        img.putpixel((x, y), (SOUL[5] if i % 3 == 0 else SOUL[4]) + (255,))       # lit edge
        img.putpixel((x - 1, y), (SOUL[3] if i % 3 == 0 else SOUL[2]) + (255,))   # body
        if y + 1 < 16:
            img.putpixel((x - 1, y + 1), SOUL[1] + (255,))                       # shadow edge
        if i % 3 == 1 and y + 1 < 16:
            img.putpixel((x, y + 1), SOUL[2] + (255,))                           # twist band
    for (x, y), c in (((13, 2), SOUL[6]), ((14, 1), SOUL[5]), ((12, 1), SOUL[4]), ((14, 3), SOUL[4]), ((13, 0), SOUL[3])):
        img.putpixel((x, y), c + (255,))
    return img


def gravity_icon():
    """18x18 effect icon: a heavy violet-grey weight pressing a downward arrow onto the ground."""
    img = Image.new('RGBA', (18, 18), T)
    d = ImageDraw.Draw(img)
    d.rectangle((5, 1, 12, 6), fill=BED[2] + (255,), outline=K + (255,))
    d.line((7, 2, 10, 2), fill=BED[4] + (255,))
    d.rectangle((8, 7, 9, 11), fill=VIOLET[2] + (255,))
    d.polygon([(5, 11), (12, 11), (8.5, 15)], fill=VIOLET[2] + (255,), outline=VIOLET[1] + (255,))
    d.line((2, 16, 15, 16), fill=BED[3] + (255,))
    d.line((3, 17, 14, 17), fill=BED[1] + (255,))
    return img


def outputs():
    tex = RES / 'textures'
    return {
        tex / 'entity/shellker/shellker.png': shellker(),
        tex / 'entity/shellker/shellker_eyes.png': shellker_eyes(),
        tex / 'entity/shellker/spark.png': spark(),
        tex / 'entity/soulfire_blaze/soulfire_blaze.png': soulfire_blaze(),
        tex / 'entity/equipment/humanoid/shellker.png': armor_layer(),
        tex / 'item/shellker_helmet.png': helmet_item(),
        tex / 'item/soulfire_rod.png': rod_item(),
        # item/shellker_spawn_egg.png: tools/textures/spawn_eggs_2026_10_23.py (animated, owner 23.10., TX22-2)
        tex / 'item/soulfire_blaze_spawn_egg.png': egg(SOUL[2], SOUL[1], SOUL[5], SOUL[4]),
        tex / 'mob_effect/high_gravity.png': gravity_icon(),
    }


def preview(items):
    PREVIEW.mkdir(parents=True, exist_ok=True)
    sheet = Image.new('RGBA', (1400, 760), (54, 56, 62, 255))
    d = ImageDraw.Draw(sheet)
    x = 10
    for p, im in items.items():
        if im.width > 18:
            continue
        sheet.paste(im.resize((im.width * 8, im.height * 8), Image.NEAREST), (x, 30), im.resize((im.width * 8, im.height * 8), Image.NEAREST))
        d.text((x, 10), p.stem[:18], fill=(255, 255, 255, 255))
        x += im.width * 8 + 14
    y = 200
    x = 10
    for p, im in items.items():
        if im.width <= 18:
            continue
        s = im.resize((im.width * 5, im.height * 5), Image.NEAREST)
        sheet.paste(s, (x, y + 20), s)
        d.text((x, y), p.stem, fill=(255, 255, 255, 255))
        x += s.width + 14
        if x > 1100:
            x, y = 10, y + 360
    sheet.save(PREVIEW / 'textures.png')


def main():
    items = outputs()
    if '--check' in sys.argv:
        bad = [str(p) for p, im in items.items() if not p.is_file() or Image.open(p).convert('RGBA').tobytes() != im.tobytes()]
        if bad:
            print('stale:', *bad, sep='\n  '); sys.exit(1)
        print('mobs2 textures current'); return
    for p, im in items.items():
        p.parent.mkdir(parents=True, exist_ok=True); im.save(p)
    preview(items)


if __name__ == '__main__':
    main()
