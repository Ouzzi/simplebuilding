"""Usage: python tools/textures/training_dummy_v3_2026_10_04.py <vanilla textures dir> [preview.png] [variant]

Training Dummy look, round 3 (owner 2026-10-04: "the head should be a pumpkin", the whole dummy nicer and closer to
vanilla). The dummy's head and torso are one 128x64 sheet in the player head/body box layout at twice the texel
density (the layer declares 64x32), so every head face takes a vanilla block texture 1:1 - the head reads exactly like
a carved pumpkin worn on the head.

Variants (preview A-C, front and side view plus item icon; variant A is written into the mod, pass another letter as
third argument to switch):
- A  vanilla carved pumpkin head (carved_pumpkin front, pumpkin_side sides/back, pumpkin_top top/bottom); torso of
     hay_block_side straw (its red bindings become shoulder and waist twine) with the vanilla target block's rings
     on the chest.
- B  as A, plus a twine cord tied round the pumpkin with a knot on the left and straw tufts sticking out under it;
     torso crossed with two twine straps behind the target.
- C  jack o'lantern face (vanilla jack_o_lantern front) on the pumpkin; torso straw from hay_block_top (the cut ends)
     on the front and back, side straw on the sides, target on the chest.
Writes entity/training_dummy/stuffing.png and item/training_dummy.png into
mc26_3/overlay/resources/assets/simplebuilding/textures/."""
from PIL import Image, ImageDraw
import os
import sys

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else None
CHOICE = sys.argv[3] if len(sys.argv) > 3 else 'A'
ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
OUT = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures')
TWINE = (146, 65, 35)
TWINE_DARK = (110, 46, 24)

# Box UV (player layout, x2): head 8x8x8 at (0,0), body 8x12x4 at (16,16) in 64x32 units.
S = 2


def load(name):
    return Image.open(os.path.join(V, name + '.png')).convert('RGBA')


def head_faces(u, v, w, h, d):
    """Pixel boxes (x0, y0, x1, y1) of a box's faces at density S, for texOffs(u, v) and addBox(w, h, d)."""
    f = {
        'top': (u + d, v, u + d + w, v + d),
        'bottom': (u + d + w, v, u + d + 2 * w, v + d),
        'right': (u, v + d, u + d, v + d + h),
        'front': (u + d, v + d, u + d + w, v + d + h),
        'left': (u + d + w, v + d, u + 2 * d + w, v + d + h),
        'back': (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }
    return {k: tuple(c * S for c in box) for k, box in f.items()}


HEAD = head_faces(0, 0, 8, 8, 8)
BODY = head_faces(16, 16, 8, 12, 4)


def paste(sheet, box, img):
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    tile = Image.new('RGBA', (w, h))
    for y in range(0, h, img.height):
        for x in range(0, w, img.width):
            tile.paste(img, (x, y))
    sheet.paste(tile, (x0, y0))


def target_rings():
    """The vanilla target block's rings (its centre 12x12), background of the outer ring kept off-white."""
    return load('block/target_side').crop((2, 2, 14, 14))


def straw_front(sheet, box, straw):
    paste(sheet, box, straw)


def chest_target(sheet):
    x0, y0, _, _ = BODY['front']
    sheet.paste(target_rings(), (x0 + 2, y0 + 5))


def variant(letter):
    sheet = Image.new('RGBA', (128, 64), (0, 0, 0, 0))
    pumpkin_side = load('block/pumpkin_side')
    pumpkin_top = load('block/pumpkin_top')
    face = load('block/jack_o_lantern' if letter == 'C' else 'block/carved_pumpkin')
    for k in ('right', 'left', 'back'):
        paste(sheet, HEAD[k], pumpkin_side)
    paste(sheet, HEAD['top'], pumpkin_top)
    paste(sheet, HEAD['bottom'], pumpkin_top)
    paste(sheet, HEAD['front'], face)
    side = load('block/hay_block_side')
    top = load('block/hay_block_top')
    for k in ('right', 'left'):
        paste(sheet, BODY[k], side)
    for k in ('front', 'back'):
        paste(sheet, BODY[k], top if letter == 'C' else side)
    paste(sheet, BODY['top'], top)
    paste(sheet, BODY['bottom'], top)
    d = ImageDraw.Draw(sheet)
    if letter == 'B':
        # twine round the pumpkin, row 12 of each side face, knot on the left face
        for k in ('right', 'front', 'left', 'back'):
            x0, y0, x1, _ = HEAD[k]
            if k == 'front':
                continue
            d.line((x0, y0 + 12, x1 - 1, y0 + 12), fill=TWINE + (255,))
            d.line((x0, y0 + 13, x1 - 1, y0 + 13), fill=TWINE_DARK + (255,))
        x0, y0, _, _ = HEAD['left']
        d.rectangle((x0 + 6, y0 + 11, x0 + 8, y0 + 14), fill=TWINE_DARK + (255,))
        d.line((x0 + 7, y0 + 14, x0 + 6, y0 + 15), fill=TWINE + (255,))
        # straw tufts on the bottom face edge (seen from below/behind the jaw)
        bx0, by0, bx1, by1 = HEAD['bottom']
        for x in range(bx0, bx1, 3):
            sheet.paste(side.crop((x % 16, 4, x % 16 + 2, 6)), (x, by1 - 2))
        # crossed straps on the torso front, under the target
        fx0, fy0, fx1, fy1 = BODY['front']
        for i in range(24):
            for x in (fx0 + round(i * 15 / 23), fx1 - 1 - round(i * 15 / 23)):
                sheet.putpixel((x, fy0 + i), TWINE + (255,))
    chest_target(sheet)
    return sheet


def icon(letter, straw_item):
    """16x16 item: straw stand body from the straw item, a pumpkin head, the target on the chest."""
    out = straw_item.copy()
    for y in range(0, 6):
        for x in range(5, 11):
            out.putpixel((x, y), (0, 0, 0, 0))
    side = load('block/pumpkin_side')
    face = load('block/jack_o_lantern' if letter == 'C' else 'block/carved_pumpkin')
    head = Image.new('RGBA', (6, 6))
    # a small pumpkin in pumpkin tones (a 16 -> 6 downscale of the block loses the face), eyes and grin stamped on
    orange = side.getpixel((5, 5))
    dark = side.getpixel((1, 8))
    light = side.getpixel((8, 3))
    eye = (255, 200, 60, 255) if letter == 'C' else (52, 24, 6, 255)
    for y in range(6):
        for x in range(6):
            head.putpixel((x, y), light if y == 0 else (dark if x in (0, 5) or y == 5 else orange))
    for (x, y) in ((1, 2), (4, 2), (1, 4), (2, 4), (3, 4), (4, 4)):
        head.putpixel((x, y), eye)
    out.paste(head, (5, 0), head)
    if letter == 'B':
        for x in range(5, 11):
            out.putpixel((x, 5), TWINE + (255,))
    for (x, y), col in {(7, 6): (190, 30, 30), (8, 6): (230, 222, 205), (7, 7): (230, 222, 205), (8, 7): (190, 30, 30)}.items():
        if out.getpixel((x, y))[3]:
            out.putpixel((x, y), col + (255,))
    return out


def front_and_side(sheet, stand):
    """Preview views (texel = 1 px at S): head on the torso, the stand's straw legs and the stone plate below."""
    leg = stand.getpixel((2, 34)) if stand.getpixel((2, 34))[3] else (190, 160, 50, 255)
    def view(headbox, bodybox, body_w):
        w = 16
        img = Image.new('RGBA', (w + 8, 16 + 24 + 22 + 4), (0, 0, 0, 0))
        img.paste(sheet.crop(headbox), (4, 0))
        img.paste(sheet.crop(bodybox), (4 + (16 - body_w) // 2, 16))
        for y in range(40, 62):
            for x in ((9, 10, 13, 14) if body_w == 16 else (11, 12)):
                img.putpixel((x, y), leg)
        for y in (62, 63, 64, 65):
            for x in range(0, w + 8):
                img.putpixel((x, y), (150, 150, 150, 255))
        return img
    return view(HEAD['front'], BODY['front'], 16), view(HEAD['left'], BODY['left'], 8)


def main():
    stand = load('entity/armorstand/armorstand')
    straw_item = Image.open(os.path.join(OUT, 'item', 'straw_armor_stand.png')).convert('RGBA')
    sheets = {k: variant(k) for k in 'ABC'}
    icons = {k: icon(k, straw_item) for k in 'ABC'}
    sheets[CHOICE].save(os.path.join(OUT, 'entity', 'training_dummy', 'stuffing.png'))
    icons[CHOICE].save(os.path.join(OUT, 'item', 'training_dummy.png'))
    if PREVIEW:
        z = 6
        col = 24 * z + 16 * 12 + 60
        img = Image.new('RGBA', (3 * (2 * col) + 20, 66 * z + 64 * 3 + 80), (198, 198, 198, 255))
        dr = ImageDraw.Draw(img)
        for i, k in enumerate('ABC'):
            x = 20 + i * 2 * col
            front, side = front_and_side(sheets[k], stand)
            dr.text((x, 6), f'{k}{" (eingebaut)" if k == CHOICE else ""}: vorn | seitlich | Item', fill=(0, 0, 0, 255))
            img.alpha_composite(front.resize((front.width * z, front.height * z), Image.NEAREST), (x, 24))
            img.alpha_composite(side.resize((side.width * z, side.height * z), Image.NEAREST), (x + front.width * z + 10, 24))
            img.alpha_composite(icons[k].resize((16 * 12, 16 * 12), Image.NEAREST), (x + 2 * front.width * z + 20, 24))
            img.alpha_composite(sheets[k].resize((128 * 3, 64 * 3), Image.NEAREST), (x, 66 * z + 50))
        img.save(PREVIEW)


if __name__ == '__main__':
    main()
