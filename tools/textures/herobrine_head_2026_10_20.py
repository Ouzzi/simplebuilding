"""Herobrine head textures (KONZEPT-HEROBRINE-KOPF-2026-10-19; redrawn for TEX20 H0, 2026-10-20).

Owner TEX20 H0: "wie vanilla bitte, und auch so wie herobrine. bitte nichts frei erfinden." -> the well-known Herobrine:
a Steve-like player head (dark brown hair on top, sides and back, skin face, darker nose and mouth/beard part) with plain
white eyes without pupils, nothing added (no scars, horns, extra colours, hat layer stays empty). Own pixels drawn after the
motif, no Mojang pixels read or copied. A 64x64 player skin sheet (head 8x8x8 at UV 0,0, hat layer at UV 32,0, as
SkullModel.createHumanoidHeadLayer() maps it) plus a second sheet with only the white eyes (emissive eyes pass).
Two variants (TEX21, owner choice pending):
  A  Steve-hell     (Steve-like tones, a little hair texture)          <- provisional, committed
  B  dunkler/schlicht (same face a step darker, flat hair)

Run: python3.12 tools/textures/herobrine_head_2026_10_20.py          (writes the PNGs of CHOSEN + previews)
     python3.12 tools/textures/herobrine_head_2026_10_20.py --check  (fails if the committed PNGs differ from the generator)
Previews (all variants, 16x, day and night) go to SB_PREVIEW_DIR (default docs/ai/previews/b-herobrine20, not committed).
"""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
OUT = os.path.join(ROOT, 'modules', 'simplemobheads', 'shared', 'resources', 'assets', 'simplemobheads', 'textures', 'entity', 'herobrine')
PREVIEW = os.environ.get('SB_PREVIEW_DIR', os.path.join(ROOT, 'docs', 'ai', 'previews', 'b-herobrine20'))
CHOSEN = 'A'

PAL_A = {
    'H': (74, 52, 30), 'h': (58, 40, 22), 'k': (42, 29, 15),                          # hair: light, base, dark
    's': (184, 132, 102), 'S': (160, 112, 86),                                       # skin: base, shadow
    'n': (140, 92, 68), 'b': (110, 72, 48), 'm': (86, 54, 34),                       # nose, beard, mouth
    'W': (255, 255, 255),                                                            # eyes
}
PAL_B = dict(PAL_A, H=(48, 33, 18), h=(48, 33, 18), k=(34, 23, 12), s=(166, 118, 92), S=(140, 98, 74),
             n=(122, 80, 58), b=(94, 60, 40), m=(72, 44, 28))

# Faces of the base layer, 8 rows of 8 pixels. right/left = the head's own right/left side; on the right face the last
# column touches the front, on the left face the first column does (skin sheet order: right, front, left, back).
FRONT = ['hhhhhhhh',
         'hhHhhhhh',
         'hssssssh',
         'ssssssss',
         'sWWssWWs',
         'sssnnsss',
         'ssbmmbss',
         'ssbbbbss']
RIGHT = ['hhhhhhhh',
         'hhhhhHhh',
         'khhhhhhh',
         'khhhhSss',
         'khhhSsss',
         'kkhhSsss',
         'kkhhSsss',
         'kkkhSsss']
BACK = ['hhhhhhhh',
        'hHhhhhHh',
        'hhhhhhhh',
        'hhhhhhhh',
        'hhhhhhhh',
        'khhhhhhk',
        'kkhhhhkk',
        'kkkkkkkk']
TOP = ['hhhhhhhh',
       'hHhhhhhh',
       'hhhhhHhh',
       'hhhhhhhh',
       'hhHhhhhh',
       'hhhhhhHh',
       'hhhhhhhh',
       'hhhHhhhh']
BOTTOM = ['SSSSSSSS'] * 8


def faces(pal, name):
    return {'name': name, 'pal': pal, 'front': FRONT, 'right': RIGHT, 'left': [r[::-1] for r in RIGHT], 'back': BACK,
            'top': TOP, 'bottom': BOTTOM, 'hat': {}}


VARIANTS = {'A': faces(PAL_A, 'Steve-hell'), 'B': faces(PAL_B, 'dunkler/schlicht')}

# UV origin of each face on the 64x64 sheet (base layer; the hat layer is 32 px to the right).
FACES = {'top': (8, 0), 'bottom': (16, 0), 'right': (0, 8), 'front': (8, 8), 'left': (16, 8), 'back': (24, 8)}


def check_rows(variant):
    for face in FACES:
        rows = variant[face]
        assert len(rows) == 8 and all(len(r) == 8 for r in rows), (variant['name'], face)
        assert all(c in variant['pal'] for r in rows for c in r), (variant['name'], face)


def sheets(variant):
    """(skin, eyes): the 64x64 skin sheet and the eyes-only sheet."""
    check_rows(variant)
    skin = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    eyes = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    for face, (ox, oy) in FACES.items():
        for y, row in enumerate(variant[face]):
            for x, c in enumerate(row):
                skin.putpixel((ox + x, oy + y), variant['pal'][c] + (255,))
                if c == 'W':
                    eyes.putpixel((ox + x, oy + y), variant['pal'][c] + (255,))
        for y, row in enumerate(variant['hat'].get(face, [])):
            for x, c in enumerate(row):
                if c != '.':
                    skin.putpixel((ox + 32 + x, oy + y), variant['pal'][c] + (255,))
    return skin, eyes


def scaled(img, factor):
    return img.resize((img.width * factor, img.height * factor), Image.NEAREST)


def net(skin):
    """The unfolded head (32x16) with the hat layer laid over it."""
    base = skin.crop((0, 0, 32, 16))
    hat = skin.crop((32, 0, 64, 16))
    base.alpha_composite(hat)
    return base


def night(img, eyes_img):
    """How it looks in the dark: everything at 18 % light, the eyes at full light (emissive pass)."""
    dark = Image.new('RGBA', img.size, (0, 0, 0, 0))
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            dark.putpixel((x, y), (int(r * 0.18), int(g * 0.18), int(b * 0.18), a))
    dark.alpha_composite(eyes_img)
    return dark


def front(skin):
    face = skin.crop((8, 8, 16, 16))
    face.alpha_composite(skin.crop((40, 8, 48, 16)))
    return face


def previews():
    os.makedirs(PREVIEW, exist_ok=True)
    fronts = []
    for key, variant in VARIANTS.items():
        skin, eyes = sheets(variant)
        skin.save(os.path.join(PREVIEW, f'herobrine_{key}.png'))
        eyes.save(os.path.join(PREVIEW, f'herobrine_{key}_eyes.png'))
        n = net(skin)
        sheet = Image.new('RGBA', (32 * 16, 16 * 16 * 2 + 16), (40, 40, 48, 255))
        sheet.alpha_composite(scaled(n, 16), (0, 0))
        sheet.alpha_composite(scaled(night(n, net(eyes)), 16), (0, 16 * 16 + 16))
        sheet.save(os.path.join(PREVIEW, f'herobrine_{key}_net_16x.png'))
        f = front(skin)
        fronts.append((scaled(f, 16), scaled(night(f, eyes.crop((8, 8, 16, 16))), 16)))
    overview = Image.new('RGBA', (len(fronts) * 128 + (len(fronts) + 1) * 16, 2 * 128 + 3 * 16), (40, 40, 48, 255))
    for i, (day, dark) in enumerate(fronts):
        overview.alpha_composite(day, (16 + i * 144, 16))
        overview.alpha_composite(dark, (16 + i * 144, 160))
    overview.save(os.path.join(PREVIEW, 'herobrine_fronts_16x.png'))


def main():
    skin, eyes = sheets(VARIANTS[CHOSEN])
    targets = {'herobrine.png': skin, 'herobrine_eyes.png': eyes}
    if '--check' in sys.argv:
        stale = []
        for name, img in targets.items():
            path = os.path.join(OUT, name)
            if not os.path.isfile(path) or Image.open(path).convert('RGBA').tobytes() != img.tobytes():
                stale.append(path)
        if stale:
            print('herobrine head: stale', stale)
            return 1
        print('herobrine head: current')
        return 0
    os.makedirs(OUT, exist_ok=True)
    for name, img in targets.items():
        img.save(os.path.join(OUT, name))
    previews()
    print('wrote', ', '.join(targets), '->', OUT, 'and previews', PREVIEW)
    return 0


if __name__ == '__main__':
    sys.exit(main())
