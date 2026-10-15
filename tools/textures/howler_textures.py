"""Hollow Howler (Tiefenheuler) textures, own pixel art: entity skin + glow layer, Echo Core, spawn egg, Silence icon.

    python tools/textures/howler_textures.py          # write PNGs + previews (~/previews/howler)
    python tools/textures/howler_textures.py --check  # fail if the committed PNGs differ
    python tools/textures/howler_textures.py --variants [warden.png]  # proposals A/B/C -> ~/previews/howler/vanilla-vorschlaege.png

Palette from Vanilla sculk/catalyst (dark sculk, teal, cyan glow, catalyst bone) so it sits next to the blocks it is
summoned from; the shapes are drawn here. UV layout (128x128) matches HowlerModel:
legs (0,0)/(0,20) 5x13x5, body (24,0) 14x14x9, arms (72,0)/(94,0) 5x18x5, catalyst caps (0,40)/(30,40) 7x5x7,
jaw (60,40) 10x4x10, skull (0,56) 10x9x10, teeth (42,56)/(42,60) 8x1x1, ribs (62,56) 10x9x1, antennae (90,56)/(90,68) 10x10 planes (front + mirrored back).
"""
import os
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs'
PREVIEW = Path(os.environ.get('SB_PREVIEW_DIR', os.path.expanduser('~/previews')) + '/howler')

K = (0x05, 0x04, 0x12)            # outline / deepest shadow
SCULK = [(0x0d, 0x12, 0x17), (0x11, 0x1b, 0x21), (0x05, 0x2a, 0x32)]
TEAL = [(0x03, 0x41, 0x50), (0x05, 0x62, 0x5d), (0x19, 0x46, 0x48)]
CYAN = (0x00, 0x92, 0x95)
CYAN_B = (0x29, 0xdf, 0xeb)
CYAN_W = (0xb6, 0xfa, 0xff)
BONE = [(0xa2, 0xaf, 0x86), (0xbb, 0xc3, 0x9b), (0xd1, 0xd6, 0xb6)]
BONE_D = (0x81, 0x99, 0x88)
GREY = (0x40, 0x57, 0x6c)
GLOW = {CYAN, CYAN_B, CYAN_W}


def noise(x, y, k=0):
    return ((x * 73856093) ^ (y * 19349663) ^ (k * 83492791)) & 0xFF


def tone(x, y, ramp, k=0):
    n = noise(x, y, k)
    return ramp[0] if n < 80 else ramp[2] if n > 205 else ramp[1]


def faces(u, v, w, h, d):
    """Cube unwrap: name -> (x, y, width, height)."""
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


def fill(img, rect, ramp, k=0):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            img.putpixel((x, y), tone(x, y, ramp, k))


def sculk_box(img, u, v, w, h, d, k=0):
    for rect in faces(u, v, w, h, d).values():
        fill(img, rect, SCULK, k)
        x0, y0, fw, fh = rect
        # sparse teal blotches and single cyan spores like the sculk block
        for y in range(y0, y0 + fh):
            for x in range(x0, x0 + fw):
                n = noise(x, y, k + 7)
                if n < 18:
                    img.putpixel((x, y), TEAL[n % 3])
                elif n == 250 or n == 251:
                    img.putpixel((x, y), CYAN)


def vein(img, pts, bright_every=3):
    """Draw a 1 px vein through the given points (straight segments)."""
    i = 0
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        steps = max(abs(x1 - x0), abs(y1 - y0))
        for s in range(steps + 1):
            x = x0 + round((x1 - x0) * s / max(1, steps))
            y = y0 + round((y1 - y0) * s / max(1, steps))
            img.putpixel((x, y), CYAN_B if i % bright_every == 0 else CYAN)
            i += 1


def legs(img):
    for (u, v), k in (((0, 0), 1), ((0, 20), 2)):
        sculk_box(img, u, v, 5, 13, 5, k)
        f = faces(u, v, 5, 13, 5)
        x, y, w, h = f['front']
        # knee: a bony knob, and a vein running down to the foot
        for xx in range(x + 1, x + 4):
            img.putpixel((xx, y + 5), BONE[1])
        img.putpixel((x + 2, y + 4), BONE[2])
        img.putpixel((x + 2, y + 6), BONE_D)
        vein(img, [(x + 1, y + 8), (x + 2, y + 10), (x + 1, y + 12)])
        # foot: darker toe band all around
        for name in ('front', 'right', 'left', 'back'):
            fx, fy, fw, fh = f[name]
            for xx in range(fx, fx + fw):
                img.putpixel((xx, fy + fh - 1), K if (xx + k) % 2 else SCULK[0])


def body(img):
    u, v, w, h, d = 24, 0, 14, 14, 9
    sculk_box(img, u, v, w, h, d, 3)
    f = faces(u, v, w, h, d)
    x, y, fw, fh = f['front']
    # the front behind the rib plate: a glowing core between dark flesh
    for yy in range(y + 3, y + 11):
        for xx in range(x + 3, x + 11):
            img.putpixel((xx, yy), K)
    for (xx, yy) in [(x + 6, y + 6), (x + 7, y + 6), (x + 6, y + 7), (x + 7, y + 7)]:
        img.putpixel((xx, yy), CYAN_B)
    for (xx, yy) in [(x + 5, y + 6), (x + 8, y + 7), (x + 6, y + 5), (x + 7, y + 8)]:
        img.putpixel((xx, yy), CYAN)
    # veins spreading from the core over the belly and up to the shoulders
    vein(img, [(x + 1, y + 12), (x + 3, y + 11), (x + 5, y + 13)])
    vein(img, [(x + 12, y + 12), (x + 10, y + 11), (x + 9, y + 13)])
    vein(img, [(x + 1, y + 1), (x + 2, y + 3)])
    vein(img, [(x + 12, y + 1), (x + 11, y + 3)])
    # back: a ridge of bony vertebrae with veins
    bx, by, bw, bh = f['back']
    for yy in range(by + 1, by + bh - 1, 2):
        img.putpixel((bx + 6, yy), BONE[2]); img.putpixel((bx + 7, yy), BONE[1])
        img.putpixel((bx + 6, yy + 1), BONE_D); img.putpixel((bx + 7, yy + 1), K)
    vein(img, [(bx + 2, by + 3), (bx + 4, by + 6), (bx + 3, by + 10)])
    vein(img, [(bx + 11, by + 2), (bx + 10, by + 7), (bx + 11, by + 11)])
    # top: neck stump ring
    tx, ty, tw, th = f['top']
    for xx in range(tx + 3, tx + 11):
        img.putpixel((xx, ty + 1), TEAL[0]); img.putpixel((xx, ty + 7), TEAL[0])


def ribs(img):
    u, v, w, h, d = 62, 56, 10, 9, 1
    f = faces(u, v, w, h, d)
    for rect in f.values():
        fill(img, rect, [K, SCULK[0], SCULK[1]], 11)
    x, y, fw, fh = f['front']
    # four rib bars curving down from a breastbone; gaps stay dark so the core glow shows through
    for i, yy in enumerate(range(y, y + fh, 2)):
        for xx in range(x, x + fw):
            edge = xx in (x, x + fw - 1)
            if xx in (x + 4, x + 5):
                continue
            img.putpixel((xx, yy), BONE_D if edge else BONE[(xx + i) % 3])
        if yy + 1 < y + fh:
            img.putpixel((x, yy + 1), BONE_D); img.putpixel((x + fw - 1, yy + 1), BONE_D)
    for yy in range(y, y + fh):
        img.putpixel((x + 4, yy), BONE[2] if yy % 2 == 0 else BONE[1])
        img.putpixel((x + 5, yy), BONE[1] if yy % 2 == 0 else BONE_D)
    for yy in range(y + 1, y + fh, 2):
        for xx in range(x + 2, x + fw - 2):
            if xx in (x + 4, x + 5):
                continue
            mid = abs(yy - (y + 4)) <= 2 and xx in (x + 3, x + 6)
            img.putpixel((xx, yy), CYAN_B if mid and yy == y + 3 else CYAN if mid else K)
    for name in ('top', 'bottom', 'right', 'left'):
        rx, ry, rw, rh = f[name]
        for yy in range(ry, ry + rh):
            for xx in range(rx, rx + rw):
                img.putpixel((xx, yy), BONE_D)


def arms(img):
    for (u, v), k in (((72, 0), 4), ((94, 0), 5)):
        sculk_box(img, u, v, 5, 18, 5, k)
        f = faces(u, v, 5, 18, 5)
        # long bony claws at the end: bottom four rows of every side and the bottom face
        for name in ('front', 'right', 'left', 'back'):
            x, y, w, h = f[name]
            for yy in range(y + h - 4, y + h):
                for xx in range(x, x + w):
                    claw = (xx - x) % 2 == 0 or yy < y + h - 2
                    img.putpixel((xx, yy), BONE[(xx + yy) % 3] if claw else K)
            for xx in range(x, x + w):
                img.putpixel((xx, y + h - 5), BONE_D)
        bx, by, bw, bh = f['bottom']
        for yy in range(by, by + bh):
            for xx in range(bx, bx + bw):
                img.putpixel((xx, yy), BONE[0] if (xx + yy) % 2 else BONE_D)
        # a vein down the outside of the arm
        side = f['right'] if u == 72 else f['left']
        x, y, w, h = side
        vein(img, [(x + 2, y + 1), (x + 1, y + 5), (x + 3, y + 9), (x + 2, y + 12)])


def caps(img):
    """Catalyst shoulder caps: bone crown with a jagged lower edge over sculk, a bone ring with a soul spot on top."""
    for (u, v), k in (((0, 40), 6), ((30, 40), 8)):
        sculk_box(img, u, v, 7, 5, 7, k)
        f = faces(u, v, 7, 5, 7)
        for name in ('front', 'right', 'left', 'back'):
            x, y, w, h = f[name]
            for xx in range(x, x + w):
                img.putpixel((xx, y), BONE[2] if xx % 3 else BONE[1])
                img.putpixel((xx, y + 1), BONE[1] if (xx + k) % 2 else BONE_D)
                if (xx + k) % 3 == 0:
                    img.putpixel((xx, y + 2), BONE[0])
            img.putpixel((x + w // 2, y + 3), GREY)
        x, y, w, h = f['top']
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                ring = xx in (x, x + w - 1) or yy in (y, y + h - 1)
                inner = xx in (x + 1, x + w - 2) or yy in (y + 1, y + h - 2)
                if ring:
                    img.putpixel((xx, yy), BONE[1] if (xx + yy) % 2 else BONE[2])
                elif inner:
                    img.putpixel((xx, yy), BONE_D)
        img.putpixel((x + 3, y + 3), CYAN_B)
        for (dx, dy) in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            img.putpixel((x + 3 + dx, y + 3 + dy), CYAN)


def jaw(img):
    u, v, w, h, d = 60, 40, 10, 4, 10
    sculk_box(img, u, v, w, h, d, 9)
    f = faces(u, v, w, h, d)
    # inside of the mouth (top face): dark gums, glowing tongue groove - only seen when the jaws open
    x, y, fw, fh = f['top']
    for yy in range(y, y + fh):
        for xx in range(x, x + fw):
            img.putpixel((xx, yy), K if (xx + yy) % 5 else SCULK[2])
    for yy in range(y + 2, y + fh - 1):
        img.putpixel((x + 4, yy), CYAN); img.putpixel((x + 5, yy), CYAN_B if yy % 2 else CYAN)
    # chin: bone lower lip along the front edge, chipped
    x, y, fw, fh = f['front']
    for xx in range(x, x + fw):
        img.putpixel((xx, y), BONE[2] if xx % 4 else BONE_D)
        img.putpixel((xx, y + 1), BONE[1] if xx % 3 else BONE[0])
    vein(img, [(x + 2, y + 3), (x + 5, y + 2), (x + 8, y + 3)])
    for name in ('right', 'left'):
        sx, sy, sw, sh = f[name]
        for xx in range(sx, sx + sw):
            img.putpixel((xx, sy), BONE[1] if xx % 2 else BONE_D)


def teeth(img):
    for (u, v) in ((42, 56), (42, 60)):
        for rect in faces(u, v, 8, 1, 1).values():
            x, y, w, h = rect
            for yy in range(y, y + h):
                for xx in range(x, x + w):
                    img.putpixel((xx, yy), BONE[2] if xx % 2 else BONE[1])


def skull(img):
    u, v, w, h, d = 0, 56, 10, 9, 10
    f = faces(u, v, w, h, d)
    for name, rect in f.items():
        fill(img, rect, BONE, 12)
    # overgrown: sculk creeps over the top and the back of the skull
    x, y, fw, fh = f['top']
    for yy in range(y, y + fh):
        for xx in range(x, x + fw):
            if yy - y > 2 + (noise(xx, 0, 3) % 4):
                img.putpixel((xx, yy), tone(xx, yy, SCULK, 13))
            elif noise(xx, yy, 2) < 30:
                img.putpixel((xx, yy), BONE_D)
    img.putpixel((x + 3, y + 7), CYAN); img.putpixel((x + 7, y + 6), CYAN_B)
    x, y, fw, fh = f['back']
    for yy in range(y, y + fh):
        for xx in range(x, x + fw):
            if yy - y < 5 + (noise(xx, 1, 4) % 3):
                img.putpixel((xx, yy), tone(xx, yy, SCULK, 14))
    img.putpixel((x + 2, y + 2), CYAN); img.putpixel((x + 6, y + 3), CYAN_B)
    for name in ('right', 'left'):
        sx, sy, sw, sh = f[name]
        # temple hollow and a crack; sculk at the back corner
        img.putpixel((sx + 4, sy + 3), BONE_D); img.putpixel((sx + 5, sy + 3), BONE_D); img.putpixel((sx + 4, sy + 4), GREY)
        img.putpixel((sx + 6, sy + 1), GREY); img.putpixel((sx + 7, sy + 2), GREY)
        back = range(sx + sw - 3, sx + sw) if name == 'left' else range(sx, sx + 3)
        for xx in back:
            for yy in range(sy, sy + 3):
                img.putpixel((xx, yy), tone(xx, yy, SCULK, 15))
        for xx in range(sx, sx + sw):
            img.putpixel((xx, sy + sh - 1), BONE_D)
    # face: deep eye sockets with cyan pinpricks, a split nose cavity, cheek shadows, a forehead crack
    x, y, fw, fh = f['front']
    sockets = [(1, 2), (2, 2), (3, 2), (1, 3), (2, 3), (3, 3), (2, 4), (3, 4),
               (6, 2), (7, 2), (8, 2), (6, 3), (7, 3), (8, 3), (6, 4), (7, 4)]
    for (dx, dy) in sockets:
        img.putpixel((x + dx, y + dy), K)
    img.putpixel((x + 2, y + 3), CYAN_B); img.putpixel((x + 7, y + 3), CYAN_B)
    img.putpixel((x + 3, y + 3), CYAN); img.putpixel((x + 6, y + 3), CYAN)
    for (dx, dy) in ((4, 5), (5, 5), (4, 6), (5, 6)):
        img.putpixel((x + dx, y + dy), K if dy == 6 or dx == 4 else SCULK[1])
    for (dx, dy) in ((1, 5), (8, 5), (0, 6), (9, 6)):
        img.putpixel((x + dx, y + dy), BONE_D)
    for (dx, dy) in ((5, 0), (5, 1), (4, 1)):
        img.putpixel((x + dx, y + dy), GREY)
    for dx in range(fw):
        img.putpixel((x + dx, y + fh - 1), BONE_D if dx % 2 else BONE[0])
    # roof of the mouth (bottom face): dark with a faint glow, seen when the jaws open
    x, y, fw, fh = f['bottom']
    for yy in range(y, y + fh):
        for xx in range(x, x + fw):
            img.putpixel((xx, yy), K if (xx * 3 + yy) % 7 else SCULK[2])
    img.putpixel((x + 4, y + 4), CYAN); img.putpixel((x + 5, y + 5), CYAN)


def tendrils(img):
    for (u, v) in ((90, 56), (100, 56)):
        f = faces(u, v, 2, 6, 2)
        for name, rect in f.items():
            x, y, w, h = rect
            for yy in range(y, y + h):
                for xx in range(x, x + w):
                    if name == 'top':
                        img.putpixel((xx, yy), CYAN_B)
                    elif name == 'bottom':
                        img.putpixel((xx, yy), SCULK[0])
                    else:
                        t = (yy - y)
                        img.putpixel((xx, yy), CYAN_B if t == 0 else CYAN if t == 1 else TEAL[(xx + t) % 2] if t < 4 else SCULK[1])


def skin():
    img = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    legs(img); body(img); ribs(img); arms(img); caps(img); jaw(img); teeth(img); skull(img); tendrils(img)
    return img


def glow(src):
    """Emissive layer: every cyan pixel of the skin, nothing else."""
    img = Image.new('RGBA', src.size, (0, 0, 0, 0))
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            px = src.getpixel((x, y))
            if px[3] and px[:3] in GLOW:
                img.putpixel((x, y), px)
    return img


PAL = {'k': K, 'd': SCULK[1], 's': SCULK[0], 'e': SCULK[2], 'i': TEAL[0], 'f': TEAL[1], 'h': CYAN, 'c': CYAN_B,
       'w': CYAN_W, 'B': BONE[2], 'b': BONE[1], 'n': BONE[0], 'o': BONE_D, 'g': GREY}


def sprite(rows, size=16):
    assert len(rows) == size and all(len(r) == size for r in rows), [len(r) for r in rows]
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), PAL[ch] + (255,))
    return img


def egg():
    """The Howler's head: overgrown skull with glowing eyes on its sculk jaw, tendrils on both sides."""
    return sprite([
        "................",
        ".kk..........kk.",
        ".kck..kkkk..kck.",
        "..khkkBBBBkkkhk.",
        "..kfkBBBBBBbkfk.",
        "...kBBBdeBBbbk..",
        "...kBkkBBkkbbk..",
        "...kbkcbbckonk..",
        "...kbbokkobonk..",
        "...konnkknnook..",
        "...kBcBcBcBcBk..",
        "....kcBcBcBck...",
        "....kdeiedeik...",
        "....kehsfehek...",
        ".....kkdeekk....",
        "................",
    ])


def echo_core():
    """A round core of compressed echoes: teal shell, a cyan ring, a dark gap and a bright heart."""
    return sprite([
        "................",
        "......kkkk......",
        "....kkiffikk....",
        "...kiehhhheik...",
        "..kiehkddkheik..",
        "..kehkdiidkhek..",
        ".kfhkdhcchdkhfk.",
        ".kfhkichwcikhfk.",
        ".kfhkicwccikhfk.",
        ".kfhkdhcchdkhfk.",
        "..kehkdiidkhek..",
        "..kiehkddkheik..",
        "...kiehhhheik...",
        "....kkiffikk....",
        "......kkkk......",
        "................",
    ])


def silence_icon():
    """Mob effect icon 18x18: a sculk mouth sending two sound arcs, cut by a bone-white bar."""
    return sprite([
        "..................",
        "..................",
        "..............kk..",
        "........kk...kBBk.",
        ".........kh.kBBk..",
        "...kkk....kkBBk...",
        "..kiffk...kBBkh...",
        ".kiehhfk.kBBk.kh..",
        ".kehcchkkBBk..kh..",
        ".kehcchkBBkh..kh..",
        ".kiehhfBBk.kh.kh..",
        "..kiffBBk..kh.kh..",
        "...kkBBk...kh.k...",
        "....kBBk..kh..k...",
        "...kBBk..kh.......",
        "...kBk...k........",
        "....k.............",
        "..................",
    ], 18)


def outputs():
    s = skin_variant('A')  # owner 11.10.: proposal A "Warden-treu" with Warden-like antennae
    return {
        RES / 'textures/entity/hollow_howler/hollow_howler.png': s,
        RES / 'textures/entity/hollow_howler/hollow_howler_glow.png': glow(s),
        RES / 'textures/entity/hollow_howler/hollow_howler_antennae.png': antenna_glow(),
        RES / 'textures/item/echo_core.png': echo_core(),
        RES / 'textures/item/hollow_howler_spawn_egg.png': egg(),
        RES / 'textures/mob_effect/silence.png': silence_icon(),
    }


def preview(items):
    PREVIEW.mkdir(parents=True, exist_ok=True)
    sheet = Image.new('RGBA', (128 * 5 + 16 * 8 + 60, 128 * 5), (60, 60, 66, 255))
    s = items[RES / 'textures/entity/hollow_howler/hollow_howler.png'].resize((640, 640), Image.NEAREST)
    sheet.paste(s, (0, 0), s)
    for i, n in enumerate(['item/echo_core', 'item/hollow_howler_spawn_egg', 'mob_effect/silence']):
        t = items[RES / f'textures/{n}.png']
        t = t.resize((t.size[0] * 8, t.size[1] * 8), Image.NEAREST)
        sheet.paste(t, (640 + 30, 20 + i * 160), t)
    sheet.save(PREVIEW / 'textures.png')
    g = items[RES / 'textures/entity/hollow_howler/hollow_howler_glow.png'].resize((640, 640), Image.NEAREST)
    dark = Image.new('RGBA', (640, 640), (10, 10, 14, 255))
    dark.paste(g, (0, 0), g)
    dark.save(PREVIEW / 'glow.png')
    front_view(items[RES / 'textures/entity/hollow_howler/hollow_howler.png'], Image.new('RGBA', (128, 128))).save(PREVIEW / 'front.png')


def front_view(skin_img, glow_img):
    """Orthographic front view of the model (front faces at their model positions), 10x, to judge the face."""
    parts = [  # (u, v, w, h, d, x0, y0) with x0/y0 the box's left/top in model pixels (y up is negative)
        (0, 0, 5, 13, 5, -7, -13), (0, 20, 5, 13, 5, 2, -13), (24, 0, 14, 14, 9, -7, -27),
        (72, 0, 5, 18, 5, -12, -26), (94, 0, 5, 18, 5, 7, -26), (0, 40, 7, 5, 7, -13, -30), (30, 40, 7, 5, 7, 6, -30),
        (62, 56, 10, 9, 1, -5, -25), (60, 40, 10, 4, 10, -5, -31), (0, 56, 10, 9, 10, -5, -40),
        (90, 56, 10, 10, 0, -14, -47), (90, 68, 10, 10, 0, 4, -47)]
    canvas = Image.new('RGBA', (32, 48), (34, 40, 48, 255))
    for (u, v, w, h, d, x0, y0) in parts:
        fx, fy, fw, fh = faces(u, v, w, h, d)['front']
        for src in (skin_img, glow_img):
            face = src.crop((fx, fy, fx + fw, fy + fh))
            canvas.paste(face, (x0 + 16, y0 + 47), face)
    return canvas.resize((320, 480), Image.NEAREST)


# ------------------------------------------------------------------------------------------------
# Vanilla-style proposals A/B/C (owner 11.10.: closer to Warden/sculk/skeleton - Vanilla palettes,
# flat dark base, few large blotches, bone in short vertical ramps, cyan only as accents).
# Preview only (--variants); nothing here is written into the mod.
# ------------------------------------------------------------------------------------------------

W_BASE, W_DARK, W_SPECK = (0x0d, 0x12, 0x17), (0x05, 0x04, 0x12), (0x11, 0x15, 0x1a)
W_TEAL = [(0x06, 0x2e, 0x37), (0x03, 0x41, 0x50), (0x07, 0x48, 0x57)]
W_MID = (0x0b, 0x21, 0x2a)
CAT_BONE = [(0xd1, 0xd6, 0xb6), (0xbb, 0xc3, 0x9b), (0xa2, 0xaf, 0x86), (0x81, 0x99, 0x88), (0x40, 0x57, 0x6c)]
SKEL_BONE = [(0xd3, 0xd3, 0xd3), (0xbc, 0xbc, 0xbc), (0xad, 0xab, 0xad), (0x8e, 0x8d, 0x8e), (0x79, 0x76, 0x76)]
SKEL_HOLE = (0x49, 0x49, 0x49)
SC_CROSS = [(0x05, 0x2a, 0x32), (0x19, 0x46, 0x48), (0x05, 0x62, 0x5d)]

STYLES = {
    'A': dict(name='A Warden-treu', bone=CAT_BONE, skull=CAT_BONE, hole=W_DARK, blotch=1 / 45, cross=0, speck=0.05,
              eyes=True, heart=True, veins=False, overgrowth=0.45),
    'B': dict(name='B Skelett-Schädel', bone=SKEL_BONE, skull=SKEL_BONE, hole=SKEL_HOLE, blotch=1 / 60, cross=0, speck=0.04,
              eyes=True, heart=False, veins=True, overgrowth=0.0),
    'C': dict(name='C Sculk-Block', bone=CAT_BONE, skull=CAT_BONE, hole=W_DARK, blotch=0, cross=1 / 26, speck=0.30,
              eyes=True, heart=True, veins=False, overgrowth=0.7),
}


def _put(img, x, y, c):
    img.putpixel((x, y), c + (255,) if len(c) == 3 else c)


def v_flesh(img, rect, st, k):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            n = noise(x, y, k)
            _put(img, x, y, (0x11, 0x1b, 0x21) if n < 255 * st['speck'] else W_SPECK if n > 247 else W_BASE)
    area = w * h
    for i in range(int(area * st['blotch'])):  # few larger teal blotches like the Warden's
        bx = x0 + noise(i, k, 31) % max(1, w - 2); by = y0 + noise(k, i, 37) % max(1, h - 2)
        shape = [(0, 0), (1, 0), (0, 1), (1, 1), (2, 1), (1, 2)][: 3 + noise(i, i, k) % 4]
        for (dx, dy) in shape:
            if bx + dx < x0 + w and by + dy < y0 + h:
                _put(img, bx + dx, by + dy, W_TEAL[1] if (dx, dy) == (1, 1) else W_TEAL[0])
    for i in range(int(area * st['cross'])):  # sculk-block plus clusters
        cx = x0 + 1 + noise(i, k, 41) % max(1, w - 2); cy = y0 + 1 + noise(k, i, 43) % max(1, h - 2)
        centre = SC_CROSS[2] if noise(i, 3, k) < 90 else SC_CROSS[1]
        _put(img, cx, cy, centre)
        for (dx, dy) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if x0 <= cx + dx < x0 + w and y0 <= cy + dy < y0 + h:
                _put(img, cx + dx, cy + dy, SC_CROSS[0])


def v_band(img, rect, k):
    """Teal band along the bottom edge (Warden feet / sculk rim), jagged upper edge."""
    x0, y0, w, h = rect
    for x in range(x0, x0 + w):
        up = 1 + (noise(x, 0, k) % 3 == 0)
        for y in range(y0 + h - up - 1, y0 + h):
            _put(img, x, y, W_TEAL[1] if y == y0 + h - 1 else W_TEAL[0])


def v_teeth(img, rect, pal, k, from_bottom=False, lo=2, hi=5):
    """Bone in short vertical ramps per column (Warden ribs/teeth): light at the tip, darker towards the flesh."""
    x0, y0, w, h = rect
    for x in range(x0, x0 + w):
        n = lo + noise(x, 7, k) % (hi - lo + 1)
        for i in range(min(n, h)):
            c = pal[min(len(pal) - 1, i * len(pal) // max(1, n))]
            y = y0 + h - 1 - i if from_bottom else y0 + i
            _put(img, x, y, c)
        y = (y0 + h - 1 - n) if from_bottom else (y0 + n)
        if y0 <= y < y0 + h:
            _put(img, x, y, W_TEAL[0])


def v_box(img, u, v, w, h, d, st, k):
    for rect in faces(u, v, w, h, d).values():
        v_flesh(img, rect, st, k)


def skin_variant(key):
    st = STYLES[key]
    img = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    bone, sk = st['bone'], st['skull']
    # legs
    for (u, v), k in (((0, 0), 1), ((0, 20), 2)):
        v_box(img, u, v, 5, 13, 5, st, k)
        f = faces(u, v, 5, 13, 5)
        for name in ('front', 'right', 'left', 'back'):
            v_band(img, f[name], k)
        x, y, _, _ = f['front']
        for dx, c in ((1, bone[2]), (2, bone[1]), (3, bone[3])):
            _put(img, x + dx, y + 5, c)
        _put(img, x + 2, y + 4, bone[0])
    # body
    v_box(img, 24, 0, 14, 14, 9, st, 3)
    f = faces(24, 0, 14, 14, 9)
    x, y, fw, fh = f['front']
    if st['heart']:
        for (dx, dy) in ((6, 6), (7, 6), (6, 7), (7, 7)):
            _put(img, x + dx, y + dy, CYAN if (dx + dy) % 2 else CYAN_B)
    bx, by, bw, bh = f['back']
    for yy in range(by + 1, by + bh - 1, 3):  # vertebrae as short bone ramps
        for i, c in enumerate(bone[:3]):
            _put(img, bx + 6, yy + i, c); _put(img, bx + 7, yy + i, bone[min(4, i + 1)])
    if st['veins']:
        vein_c = [W_TEAL[2], (0x00, 0x92, 0x95)]
        for (a, b) in (((bx + 2, by + 3), (bx + 3, by + 9)), ((x + 2, y + 11), (x + 4, y + 13))):
            for t in range(8):
                _put(img, a[0] + (b[0] - a[0]) * t // 7, a[1] + (b[1] - a[1]) * t // 7, vein_c[t % 2])
    # rib plate: horizontal bars, light in the middle, darker at the ends (Warden ribcage)
    rf = faces(62, 56, 10, 9, 1)
    for rect in rf.values():
        v_flesh(img, rect, dict(st, blotch=0, cross=0), 11)
    x, y, fw, fh = rf['front']
    for yy in range(y, y + fh, 2):
        for xx in range(x, x + fw):
            d = abs(xx - (x + 4.5))
            _put(img, xx, yy, bone[0] if d < 1 else bone[1] if d < 2.5 else bone[2] if d < 3.5 else bone[3])
    if st['heart']:
        _put(img, x + 4, y + 3, CYAN); _put(img, x + 5, y + 5, CYAN)
    # arms: bone claws as ramps from the bottom
    for (u, v), k in (((72, 0), 4), ((94, 0), 5)):
        v_box(img, u, v, 5, 18, 5, st, k)
        f = faces(u, v, 5, 18, 5)
        for name in ('front', 'right', 'left', 'back'):
            x, y, w, h = f[name]
            v_teeth(img, (x, y + h - 6, w, 6), bone, k, from_bottom=True, lo=3, hi=5)
        x, y, w, h = f['bottom']
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                _put(img, xx, yy, bone[2] if (xx + yy) % 2 else bone[3])
    # catalyst caps: bone crown ramps over sculk, ring on top
    for (u, v), k in (((0, 40), 6), ((30, 40), 8)):
        v_box(img, u, v, 7, 5, 7, st, k)
        f = faces(u, v, 7, 5, 7)
        for name in ('front', 'right', 'left', 'back'):
            v_teeth(img, f[name], CAT_BONE, k, lo=1, hi=3)
        x, y, w, h = f['top']
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                if xx in (x, x + w - 1) or yy in (y, y + h - 1):
                    _put(img, xx, yy, CAT_BONE[1] if (xx + yy) % 3 else CAT_BONE[0])
                elif xx in (x + 1, x + w - 2) or yy in (y + 1, y + h - 2):
                    _put(img, xx, yy, CAT_BONE[3])
        _put(img, x + 3, y + 3, CYAN)
    # jaw: lower teeth along the front, dark mouth
    v_box(img, 60, 40, 10, 4, 10, st, 9)
    f = faces(60, 40, 10, 4, 10)
    v_teeth(img, f['front'], bone, 9, lo=1, hi=2)
    x, y, fw, fh = f['top']
    for yy in range(y, y + fh):
        for xx in range(x, x + fw):
            _put(img, xx, yy, W_DARK if (xx * 5 + yy) % 6 else W_MID)
    for yy in range(y + 3, y + fh - 1):
        _put(img, x + 4, yy, W_TEAL[2]); _put(img, x + 5, yy, W_TEAL[1])
    for (u, v) in ((42, 56), (42, 60)):
        for rect in faces(u, v, 8, 1, 1).values():
            for xx in range(rect[0], rect[0] + rect[2]):
                for yy in range(rect[1], rect[1] + rect[3]):
                    _put(img, xx, yy, bone[0] if xx % 2 else bone[1])
    # skull: three-tone bone like the Vanilla skeleton, light top-left, darker lower edge
    f = faces(0, 56, 10, 9, 10)
    for name, (x, y, w, h) in f.items():
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                n = noise(xx, yy, 21)
                c = sk[1] if n < 150 else sk[0] if n < 200 else sk[2]
                if yy == y + h - 1 and name != 'top':
                    c = sk[3]
                _put(img, xx, yy, c)
    for name in ('top', 'back', 'right', 'left'):  # sculk overgrowth creeping over the back of the skull
        x, y, w, h = f[name]
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                depth = {'top': (yy - y) / h, 'back': 1 - (yy - y) / h, 'right': (xx - x) / w, 'left': 1 - (xx - x) / w}[name]
                if name in ('right', 'left'):
                    depth = (1 - depth) * (1 - (yy - y) / h)
                if depth > 1 - st['overgrowth'] + (noise(xx, yy, 5) % 3) * 0.08:
                    _put(img, xx, yy, W_BASE if noise(xx, yy, 6) > 70 else W_TEAL[0])
    x, y, fw, fh = f['front']
    hole = st['hole']
    for (dx, dy) in ((1, 3), (2, 3), (3, 3), (1, 4), (2, 4), (3, 4), (6, 3), (7, 3), (8, 3), (6, 4), (7, 4), (8, 4), (2, 2), (7, 2)):
        _put(img, x + dx, y + dy, hole)
    if st['eyes']:
        _put(img, x + 2, y + 4, CYAN_B); _put(img, x + 7, y + 4, CYAN_B)
    for (dx, dy) in ((4, 5), (5, 5), (4, 6), (5, 6)):
        _put(img, x + dx, y + dy, hole if dy == 6 else sk[3])
    for dx in range(1, 9):
        _put(img, x + dx, y + fh - 1, hole if dx % 2 else sk[2])
    x, y, w, h = f['bottom']
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            _put(img, xx, yy, W_DARK)
    # antennae like the Warden's: flat forked sculk tendrils beside the skull (front and back face)
    for (u, v), flip in (((90, 56), False), ((90, 68), True)):
        paint_antenna(img, u, v, flip, glow_only=False)
    return img


ANTENNA = [  # right antenna, front face; the stem meets the skull at the lower right
    "..cc......",
    ".chT......",
    ".hTk..hc..",
    "..Tt.hT...",
    "...tTt....",
    "....tT....",
    ".....tT...",
    "......tT..",
    ".......tT.",
    "........tT",
]
ANT_PAL = {'c': CYAN_B, 'h': CYAN, 'T': W_TEAL[1], 't': W_TEAL[0], 'k': W_DARK}


def paint_antenna(img, u, v, flip, glow_only):
    """Front face at (u, v), back face at (u + 10, v) mirrored; glow_only paints the listening glow instead."""
    for face_x, mirror in ((u, flip), (u + 10, not flip)):
        for y, row in enumerate(ANTENNA):
            for x, ch in enumerate(row):
                if ch == '.':
                    continue
                c = ANT_PAL[ch]
                if glow_only:
                    c = CYAN_B if ch in 'ch' or (x + y) % 3 == 0 else CYAN
                _put(img, face_x + (9 - x if mirror else x), v + y, c)


def antenna_glow():
    """Emissive layer shown while the Howler listens (like the Warden's tendril layer)."""
    img = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    for (u, v), flip in (((90, 56), False), ((90, 68), True)):
        paint_antenna(img, u, v, flip, glow_only=True)
    return img


def side_view(skin_img):
    """Orthographic right-side view (the 'right' faces), to show the depth and the overgrown skull."""
    parts = [  # (u, v, w, h, d, z0, y0) z0 = front of the box (smaller = front)
        (0, 0, 5, 13, 5, -2, -13), (24, 0, 14, 14, 9, -4, -27), (72, 0, 5, 18, 5, -2, -26), (0, 40, 7, 5, 7, -3, -30),
        (60, 40, 10, 4, 10, -6, -31), (0, 56, 10, 9, 10, -6, -40)]
    canvas = Image.new('RGBA', (24, 48), (34, 40, 48, 255))
    for (u, v, w, h, d, z0, y0) in parts:
        fx, fy, fw, fh = faces(u, v, w, h, d)['right']
        face = skin_img.crop((fx, fy, fx + fw, fy + fh))
        canvas.paste(face, (z0 + 12, y0 + 47), face)
    return canvas.resize((240, 480), Image.NEAREST)


def warden_front(warden):
    """Rough orthographic front of the Vanilla Warden for side-by-side comparison (same 10x scale)."""
    parts = [((76, 48, 6, 13, 6), (-9, -13)), ((76, 76, 6, 13, 6), (3, -13)), ((0, 0, 18, 21, 11), (-9, -34)),
             ((44, 50, 8, 28, 8), (-17, -34)), ((0, 58, 8, 28, 8), (9, -34)), ((0, 32, 16, 16, 10), (-8, -50))]
    canvas = Image.new('RGBA', (40, 56), (34, 40, 48, 255))
    for (u, v, w, h, d), (x0, y0) in parts:
        fx, fy, fw, fh = faces(u, v, w, h, d)['front']
        face = warden.crop((fx, fy, fx + fw, fy + fh))
        canvas.paste(face, (x0 + 20, y0 + 55), face)
    rib = warden.crop((90, 11, 99, 32))
    canvas.paste(rib, (-9 + 20, -34 + 55 + 1), rib)
    canvas.paste(rib.transpose(Image.FLIP_LEFT_RIGHT), (0 + 20, -34 + 55 + 1), rib.transpose(Image.FLIP_LEFT_RIGHT))
    return canvas.resize((400, 560), Image.NEAREST)


def variants_sheet(warden_png):
    from PIL import ImageDraw
    cols = []
    for key in 'ABC':
        s = skin_variant(key)
        g = glow(s)
        col = Image.new('RGBA', (580, 1040), (60, 60, 66, 255))
        col.paste(front_view(s, g), (0, 40))
        col.paste(side_view(s), (330, 40))
        sheet = s.crop((0, 0, 116, 76)).resize((580, 380), Image.NEAREST)
        col.paste(sheet, (0, 560), sheet)
        ImageDraw.Draw(col).text((10, 10), STYLES[key]['name'], fill=(255, 255, 255, 255))
        cols.append(col)
    ref = Image.new('RGBA', (420, 1040), (60, 60, 66, 255))
    if warden_png and Path(warden_png).is_file():
        w = Image.open(warden_png).convert('RGBA')
        ref.paste(warden_front(w), (10, 40))
        t = w.crop((0, 0, 116, 90)).resize((406, 315), Image.NEAREST)
        ref.paste(t, (10, 620), t)
    ImageDraw.Draw(ref).text((10, 10), 'Vanilla Warden (Vergleich)', fill=(255, 255, 255, 255))
    out = Image.new('RGBA', (580 * 3 + 420 + 40, 1040), (40, 40, 46, 255))
    for i, c in enumerate(cols):
        out.paste(c, (i * 590, 0))
    out.paste(ref, (3 * 590 + 10, 0))
    PREVIEW.mkdir(parents=True, exist_ok=True)
    out.save(PREVIEW / 'vanilla-vorschlaege.png')
    return out



HEART_SHAPE = ['.HH..HH.', 'HHHHHHHH', 'HHHHHHHH', '.HHHHHH.', '..HHHH..', '...HH...']


def heart():
    """PROPOSAL (owner H4, not built in): emissive heart on the rib plate's front face, pulses with the heartbeat. Only this
    layer is new; hollow_howler.png and the glow layer stay as approved. Shape: outline cyan, filling bright, one white highlight."""
    img = Image.new('RGBA', (128, 128), (0, 0, 0, 0))
    x0, y0 = 62 + 1 + 1, 56 + 1 + 1     # front face of the rib plate (62,56 + depth 1), one pixel in
    rows = HEART_SHAPE
    def on(x, y): return 0 <= y < len(rows) and 0 <= x < len(rows[0]) and rows[y][x] == 'H'
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != 'H':
                continue
            edge = not all(on(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            col = CYAN if edge else CYAN_B
            if (x, y) == (1, 1):
                col = CYAN_W
            img.putpixel((x0 + x, y0 + y), col + (255,))
    return img


def heart_preview(outdir):
    """Writes hollow_howler_heart.png (the proposal) and a 8x comparison: current rib plate / heart glowing / heart at 45 % (rest of the beat)."""
    out = Path(outdir); out.mkdir(parents=True, exist_ok=True)
    skin_img, h = skin(), heart()
    h.save(out / 'hollow_howler_heart.png')
    glow_img = glow(skin_img)
    box = (60, 54, 76, 68)      # rib plate with a little margin
    def panel(level):
        base = Image.new('RGBA', skin_img.size, (10, 10, 14, 255))
        base.alpha_composite(skin_img)
        for layer, a in ((glow_img, 0.85), (h, level)):
            if a <= 0:
                continue
            lay = layer.copy()
            lay.putalpha(lay.getchannel('A').point(lambda v, a=a: int(v * a)))
            base.alpha_composite(lay)
        return base.crop(box).resize(((box[2] - box[0]) * 8, (box[3] - box[1]) * 8), Image.NEAREST)
    panels = [panel(0.0), panel(1.0), panel(0.45)]
    w, hh = panels[0].size
    sheet = Image.new('RGBA', (w * 3 + 40, hh + 20), (40, 40, 46, 255))
    for i, pn in enumerate(panels):
        sheet.paste(pn, (10 + i * (w + 10), 10))
    sheet.save(out / 'heart-vergleich.png')
    print('wrote', out)


def main():
    if '--heart-preview' in sys.argv:
        heart_preview(sys.argv[sys.argv.index('--heart-preview') + 1]); return
    if '--variants' in sys.argv:
        # optional reference: path to Vanilla's entity/warden/warden.png (never checked in)
        ref = next((a for a in sys.argv[1:] if a.endswith('.png')), None)
        variants_sheet(ref); return
    items = outputs()
    if '--check' in sys.argv:
        bad = [str(p) for p, im in items.items() if not p.is_file() or Image.open(p).convert('RGBA').tobytes() != im.tobytes()]
        if bad:
            print('stale:', *bad, sep='\n  '); sys.exit(1)
        print('howler textures current'); return
    for p, im in items.items():
        p.parent.mkdir(parents=True, exist_ok=True); im.save(p)
    preview(items)


if __name__ == '__main__':
    main()
