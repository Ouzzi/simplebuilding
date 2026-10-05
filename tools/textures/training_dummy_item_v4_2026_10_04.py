"""Usage: python tools/textures/training_dummy_item_v4_2026_10_04.py [preview.png] [--install A|B|C]

Training Dummy item icon, round 4 (owner 2026-10-04: "adjust the Target Dummy texture too"). The entity (carved pumpkin
head, hay torso with twine and target, round 3 A) is fine; the item icon still shows a flat orange square with a
smiley on the thin stand pole. Three hand-drawn 16x16 icons that match the model, colours from vanilla carved_pumpkin /
pumpkin_side, hay_block_side (straw + twine), target_side and the mod's straw armor stand item:
- A  pumpkin head (stem, ribs, carved eyes and jagged grin) on a hay torso as wide as the head, twine at shoulders
     and waist, a small target on the chest, two stand legs, stone plate.
- B  the straw armor stand silhouette (like the vanilla armor stand item) kept, only a smaller round pumpkin head and
     a target with twine band on the pole - same family as the Straw Armor Stand item.
- C  bust: wide pumpkin head (8 px, like the model's head = torso width), full target rings on a wide hay torso,
     one pole and the plate.
Without --install nothing in the mod changes; --install X writes item/training_dummy.png and the wiki copy."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
TEX = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures')
WIKI = os.path.join(ROOT, 'wiki', 'assets', 'textures', 'item', 'training_dummy.png')

C = {
    # carved_pumpkin / pumpkin_side
    'L': (227, 166, 75), 'O': (227, 138, 29), 'M': (196, 111, 20), 'D': (160, 86, 11), 'E': (126, 61, 14),
    'f': (45, 0, 3), 'F': (68, 19, 0),
    # stem (pumpkin_top stem tones)
    's': (79, 62, 22), 'S': (118, 98, 38),
    # hay_block_side straw + twine
    'h': (203, 182, 48), 'H': (191, 171, 49), 'j': (171, 146, 37), 'k': (148, 128, 30), 'x': (69, 58, 16),
    't': (165, 73, 44), 'T': (135, 53, 28),
    # target_side
    'w': (243, 235, 223), 'W': (235, 215, 186), 'r': (200, 47, 47), 'R': (164, 52, 52),
    # straw armor stand item (poles) + stone plate
    'a': (203, 182, 48), 'b': (171, 146, 37), 'c': (138, 115, 32), 'd': (69, 58, 16),
    'g': (176, 176, 176), 'G': (168, 168, 168), 'q': (157, 157, 157),
}

ICONS = {
    'A': [
        '.......sS.......',
        '......LOLM......',
        '.....LOLOME.....',
        '.....OfOMfE.....',
        '.....OOMOME.....',
        '.....OfffFE.....',
        '......MOME......',
        '...abttttttbd...',
        '.....hwwwkx.....',
        '.....hwrwkx.....',
        '.....Hwwwkx.....',
        '.....TtttTx.....',
        '......bd.bd.....',
        '......bd.bd.....',
        '....gGgqgGqd....',
        '....ddddddd.....',
    ],
    'B': [
        '.......S........',
        '......LOM.......',
        '.....OfMfE......',
        '.....OMOME......',
        '.....DfffE......',
        '...abttttTbd....',
        '.....wwwd.......',
        '.....wrwd.......',
        '.....wwwd.......',
        '....bTttTbd.....',
        '.....acbd.......',
        '.....abcd.......',
        '.....abcd.......',
        '....bbacbcd.....',
        '...gGgqgGqd.....',
        '....ddddddd.....',
    ],
    'C': [
        '.......sS.......',
        '.....LOLOLM.....',
        '....LOLOMOME....',
        '....OffOMffE....',
        '....OOfOMfOE....',
        '....OOMOOMOE....',
        '....OffffffE....',
        '.....DMMMDE.....',
        '..abttttttttbd..',
        '....hrrrrrkx....',
        '....hrwwwrkx....',
        '....Hrwrwrkx....',
        '....hrwwwrkx....',
        '....hrrrrrkx....',
        '...gGgqgGqgd....',
        '....dddddddd....',
    ],
}



def draw(rows):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != '.':
                img.putpixel((x, y), C[ch] + (255,))
    return img


def model_views():
    """Front and side view of the installed dummy (stuffing sheet at 2x density, stand poles, plate)."""
    sheet = Image.open(os.path.join(TEX, 'entity', 'training_dummy', 'stuffing.png')).convert('RGBA')
    leg = (190, 160, 50, 255)
    views = []
    for head, body, bw in (((16, 16, 32, 32), (40, 40, 56, 64), 16), ((32, 16, 48, 32), (56, 40, 64, 64), 8)):
        img = Image.new('RGBA', (24, 66), (0, 0, 0, 0))
        img.paste(sheet.crop(head), (4, 0))
        img.paste(sheet.crop(body), (4 + (16 - bw) // 2, 16))
        for y in range(40, 62):
            for x in ((9, 10, 13, 14) if bw == 16 else (11, 12)):
                img.putpixel((x, y), leg)
        for y in range(62, 66):
            for x in range(24):
                img.putpixel((x, y), (150, 150, 150, 255))
        views.append(img)
    return views


def slot(icon):
    """1x on an inventory slot (18x18, vanilla slot colours)."""
    s = Image.new('RGBA', (18, 18), (139, 139, 139, 255))
    d = ImageDraw.Draw(s)
    d.line((0, 0, 16, 0), fill=(55, 55, 55, 255)); d.line((0, 0, 0, 16), fill=(55, 55, 55, 255))
    d.line((17, 1, 17, 17), fill=(255, 255, 255, 255)); d.line((1, 17, 17, 17), fill=(255, 255, 255, 255))
    s.alpha_composite(icon, (1, 1))
    return s


NOTES = {
    'aktuell': ['flach, Smiley, Muetzenrand,', 'duenne Stange, 2x2-Schachbrett'],
    'A': ['Kuerbis (Stiel, Rippen, Schnitzgesicht)', 'auf Strohrumpf mit Baendern + Ziel'],
    'B': ['Stroh-Staender-Silhouette bleibt,', 'kleiner runder Kuerbis, Ziel an Stange'],
    'C': ['Buste: breiter Kuerbis = Rumpfbreite', 'wie im Modell, volle Zielringe, ein Sockel'],
}


def preview(path, icons):
    z = 16
    colw = 16 * z + 40
    views = model_views()
    vw = 4
    W = 20 + 4 * colw + 2 * 24 * vw + 40
    H = 16 * z + 150
    img = Image.new('RGBA', (W, H), (198, 198, 198, 255))
    d = ImageDraw.Draw(img)
    for i, (name, icon) in enumerate(icons):
        x = 20 + i * colw
        d.text((x, 6), name, fill=(0, 0, 0, 255))
        img.alpha_composite(icon.resize((16 * z, 16 * z), Image.NEAREST), (x, 24))
        s = slot(icon)
        img.alpha_composite(s, (x, 16 * z + 34))
        img.alpha_composite(s.resize((36, 36), Image.NEAREST), (x + 30, 16 * z + 34))
        for j, line in enumerate(NOTES[name]):
            d.text((x, 16 * z + 80 + 14 * j), line, fill=(0, 0, 0, 255))
    x = 20 + 4 * colw
    d.text((x, 6), 'Modell (eingebaut, Runde 3 A): vorn | seitlich', fill=(0, 0, 0, 255))
    for k, v in enumerate(views):
        img.alpha_composite(v.resize((v.width * vw, v.height * vw), Image.NEAREST), (x + k * (24 * vw + 10), 24))
    img.save(path)


def main():
    args = sys.argv[1:]
    install = None
    if '--install' in args:
        install = args[args.index('--install') + 1]
        args = [a for a in args if a not in ('--install', install)]
    current = Image.open(os.path.join(TEX, 'item', 'training_dummy.png')).convert('RGBA')
    icons = {k: draw(v) for k, v in ICONS.items()}
    if args:
        preview(args[0], [('aktuell', current)] + [(k, icons[k]) for k in 'ABC'])
    if install:
        icons[install].save(os.path.join(TEX, 'item', 'training_dummy.png'))
        icons[install].save(WIKI)


if __name__ == '__main__':
    main()
