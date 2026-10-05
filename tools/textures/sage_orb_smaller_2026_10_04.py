"""Usage: python tools/textures/sage_orb_smaller_2026_10_04.py [preview.png] [--install A|B|C]

Sage Orb smaller (owner 2026-10-04: "Sage Orb texture too big"). Today the item is vanilla's experience orb stage 4
(12x12, bbox 2..14) - as large as the ball items (measured in the 26.3 client jar: ender pearl 13x13, fire charge /
slime ball / snowball 12x12). Three hand-drawn smaller orbs that keep its character - grey rim, bright body, the
pink inner ring and 2x2 core of vanilla's orb - in the same index colours, so the per-frame pulse tint of
ExperienceOrbRenderer (12 frames, frametime 2, round3_settled.orb_strip) works unchanged:
- A  10x10 (size of vanilla orb stage 1), full ring structure.
- B   8x8  (size of vanilla orb stage 0, between gold nugget 6x8 and the balls), rounder than stage 0.
- C   9x9  odd-sized with a single-pixel core, sits like vanilla items slightly up-left of centre.
Without --install nothing in the mod changes; --install X writes item/sage_orb.png (strip) and the wiki copy (strip, as today)."""
import io
import math
import os
import sys
import zipfile

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
JAR = r'C:\Users\o_o\.gradle\caches\fabric-loom\26.3\minecraft-client.jar'
TEX = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'item')
WIKI = os.path.join(ROOT, 'wiki', 'assets', 'textures', 'item', 'sage_orb.png')
FRAMES = 12

# vanilla experience_orb.png stage-4 colours (index 0 = dark rim ... 5 = white)
PAL = [(112, 112, 112), (160, 160, 160), (255, 144, 144), (193, 193, 193), (255, 192, 192), (255, 255, 255)]

ORBS = {
    'A': [
        '................',
        '................',
        '................',
        '.....000000.....',
        '....01133110....',
        '...0135555310...',
        '...0355445530...',
        '...0354224530...',
        '...0354224530...',
        '...0355445530...',
        '...0135555310...',
        '....01133110....',
        '.....000000.....',
        '................',
        '................',
        '................',
    ],
    'B': [
        '................',
        '................',
        '................',
        '................',
        '......0000......',
        '.....013310.....',
        '....01544510....',
        '....03422430....',
        '....03422430....',
        '....01544510....',
        '.....013310.....',
        '......0000......',
        '................',
        '................',
        '................',
        '................',
    ],
    'C': [
        '................',
        '................',
        '................',
        '................',
        '.....00000......',
        '....0133310.....',
        '...013555310....',
        '...035545530....',
        '...035424530....',
        '...035545530....',
        '...013555310....',
        '....0133310.....',
        '.....00000......',
        '................',
        '................',
        '................',
    ],
}


def jar(name):
    with zipfile.ZipFile(JAR) as z:
        return Image.open(io.BytesIO(z.read('assets/minecraft/textures/' + name + '.png'))).convert('RGBA')


def base(rows):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), PAL[int(ch)] + (255,))
    return img


def strip(b):
    """Same tint as round3_settled.orb_strip: r = (sin+1)/2, g = 1, b = (sin(+4pi/3)+1)/10."""
    out = Image.new('RGBA', (16, 16 * FRAMES), (0, 0, 0, 0))
    for f in range(FRAMES):
        rr = f / FRAMES * 4 * math.pi / 2
        r, bl = (math.sin(rr) + 1) * 0.5, (math.sin(rr + math.pi * 4 / 3) + 1) * 0.1
        for y in range(16):
            for x in range(16):
                p = b.getpixel((x, y))
                if p[3]:
                    out.putpixel((x, f * 16 + y), (round(p[0] * r), p[1], round(p[2] * bl), p[3]))
    return out


def frame(s, f):
    return s.crop((0, 16 * f, 16, 16 * f + 16))


def size(img):
    b = img.getbbox()
    return f'{b[2] - b[0]}x{b[3] - b[1]}'


def slot(icon):
    s = Image.new('RGBA', (18, 18), (139, 139, 139, 255))
    d = ImageDraw.Draw(s)
    d.line((0, 0, 16, 0), fill=(55, 55, 55, 255)); d.line((0, 0, 0, 16), fill=(55, 55, 55, 255))
    d.line((17, 1, 17, 17), fill=(255, 255, 255, 255)); d.line((1, 17, 17, 17), fill=(255, 255, 255, 255))
    s.alpha_composite(icon, (1, 1))
    return s


def preview(path, strips):
    refs = [('Enderperle', jar('item/ender_pearl')), ('Feuerkugel', jar('item/fire_charge')),
            ('Schleimball', jar('item/slime_ball')), ('Schneeball', jar('item/snowball')),
            ('Goldnugget', jar('item/gold_nugget'))]
    orb = jar('entity/experience/experience_orb')
    for i, label in ((0, 'XP-Kugel 0'), (1, 'XP-Kugel 1'), (4, 'XP-Kugel 4')):
        refs.append((label, orb.crop(((i % 4) * 16, (i // 4) * 16, (i % 4) * 16 + 16, (i // 4) * 16 + 16))))
    z = 14
    colw = 16 * z + 30
    W = 20 + 4 * colw
    H = 24 + 16 * z + 16 * 6 + 70 + 150
    img = Image.new('RGBA', (W, H), (198, 198, 198, 255))
    d = ImageDraw.Draw(img)
    for i, (name, s) in enumerate(strips):
        x = 20 + i * colw
        f0 = frame(s, 0)
        d.text((x, 6), f'{name}: {size(f0)} px', fill=(0, 0, 0, 255))
        img.alpha_composite(f0.resize((16 * z, 16 * z), Image.NEAREST), (x, 24))
        y = 24 + 16 * z + 8
        d.text((x, y), 'Puls (Frames 0, 3, 6, 9) 3x und 1x im Slot:', fill=(0, 0, 0, 255))
        for k, f in enumerate((0, 3, 6, 9)):
            img.alpha_composite(frame(s, f).resize((48, 48), Image.NEAREST), (x + k * 56, y + 16))
        y += 16 + 48 + 8
        for k, f in enumerate((0, 3, 6, 9)):
            img.alpha_composite(slot(frame(s, f)), (x + k * 24, y))
    y0 = H - 140
    d.text((20, y0), 'Vanilla 26.3 (Client-Jar, Bounding-Box gemessen), 4x und 1x im Slot:', fill=(0, 0, 0, 255))
    for i, (label, im) in enumerate(refs):
        x = 20 + i * 120
        d.text((x, y0 + 16), f'{label} {size(im)}', fill=(0, 0, 0, 255))
        img.alpha_composite(im.resize((64, 64), Image.NEAREST), (x, y0 + 32))
        img.alpha_composite(slot(im), (x + 70, y0 + 32))
    img.save(path)


def main():
    args = sys.argv[1:]
    install = None
    if '--install' in args:
        install = args[args.index('--install') + 1]
        args = [a for a in args if a not in ('--install', install)]
    current = Image.open(os.path.join(TEX, 'sage_orb.png')).convert('RGBA')
    strips = {k: strip(base(v)) for k, v in ORBS.items()}
    if args:
        preview(args[0], [('aktuell', current)] + [(k, strips[k]) for k in 'ABC'])
    if install:
        strips[install].save(os.path.join(TEX, 'sage_orb.png'))
        strips[install].save(WIKI)


if __name__ == '__main__':
    main()
