"""Concept textures for N69 (2026-10-16): three own deep-dark blocks, 16x16, own pixel art, no vanilla/foreign pixels.
Concept only, nothing is wired into the game. Writes PNGs + an 8x isometric preview board to SB_PREVIEW_DIR.
Run: python3.12 tools/textures/modref_concept_2026_10_16.py
"""
import os
import random
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
OUT = os.environ.get('SB_PREVIEW_DIR', os.path.join(ROOT, 'docs', 'ai', 'previews', 'claude-modref'))

SLATE = [(38, 40, 48), (48, 51, 60), (58, 62, 72), (70, 74, 85)]
SCULK = [(8, 20, 26), (14, 38, 46), (20, 62, 70), (38, 120, 124), (96, 214, 196)]
MOSS = [(18, 44, 40), (26, 64, 54), (38, 88, 68), (58, 118, 82), (92, 150, 104)]


def noise(rng, pal, w=16, h=16):
    return [[pal[rng.choice([0, 1, 1, 2, 2, 3])] for _ in range(w)] for _ in range(h)]


def sculk_deepslate():
    r = random.Random(11)
    p = noise(r, SLATE)
    for _ in range(5):  # sculk veins: short wandering runs, dark rim, bright core
        x, y = r.randrange(16), r.randrange(16)
        for _ in range(r.randrange(5, 9)):
            p[y % 16][x % 16] = SCULK[2]
            x += r.choice([-1, 0, 1, 1]); y += r.choice([-1, 0, 1])
        p[y % 16][x % 16] = SCULK[4]
    for _ in range(9):
        p[r.randrange(16)][r.randrange(16)] = SCULK[1]
    return p


def deepmoss_top():
    r = random.Random(23)
    p = [[MOSS[r.choice([1, 2, 2, 3])] for _ in range(16)] for _ in range(16)]
    for _ in range(14):  # fine lighter tufts, 2 px tall
        x, y = r.randrange(16), r.randrange(15)
        p[y][x] = MOSS[4]; p[y + 1][x] = MOSS[3]
    for _ in range(8):
        p[r.randrange(16)][r.randrange(16)] = MOSS[0]
    for _ in range(3):  # tiny glow spores
        p[r.randrange(16)][r.randrange(16)] = SCULK[3]
    return p


def deepmoss_side():
    r = random.Random(29)
    p = noise(r, SLATE)
    for x in range(16):  # moss overhang, ragged
        d = r.choice([3, 4, 4, 5, 6])
        for y in range(d):
            p[y][x] = MOSS[3 if y == d - 1 else r.choice([1, 2, 2])]
        p[d][x] = MOSS[0]
    return p


def hushed_tiles():
    r = random.Random(37)
    p = noise(r, SLATE)
    for i in range(16):  # grout cross + frame
        for k in (0, 7, 8, 15):
            p[k][i] = SCULK[0]; p[i][k] = SCULK[0]
    for ox, oy in ((1, 1), (9, 1), (1, 9), (9, 9)):  # four tiles, each a dished inlay
        for y in range(6):
            for x in range(6):
                edge = x in (0, 5) or y in (0, 5)
                p[oy + y][ox + x] = SLATE[3] if (edge and (x == 0 or y == 0)) else (SLATE[0] if edge else SLATE[1])
        for dx, dy in ((2, 2), (3, 3)):
            p[oy + dy][ox + dx] = SCULK[2]
        p[oy + 2][ox + 3] = SCULK[3]
    return p


def to_img(p):
    im = Image.new('RGB', (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), p[y][x])
    return im


def shade(im, f):
    return im.point(lambda v: int(v * f))


def cube(top, left, right, s=8):
    """Isometric cube from three 16x16 faces, drawn at s px per texel."""
    W = 16 * s * 2 + 8
    H = 16 * s * 2 + 8
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    cx, ty = W // 2, 4
    a = s  # texel step in x on screen
    b = s // 2  # texel step in y on screen
    h = 16 * s
    for v in range(16):
        for u in range(16):
            # top: u along right-down, v along left-down
            pts = [(u, v), (u + 1, v), (u + 1, v + 1), (u, v + 1)]
            poly = [(cx + (pu - pv) * a, ty + (pu + pv) * b) for pu, pv in pts]
            ImageDraw.Draw(img).polygon(poly, fill=top.getpixel((u, v)) + (255,))
            # left face (x from cx down-left), right face
            for face, im, sign in ((left, left, -1), (right, right, 1)):
                pass
    d = ImageDraw.Draw(img)
    for v in range(16):
        for u in range(16):
            c = left.getpixel((u, v))
            x0 = cx - 16 * a + u * a
            y0 = ty + 16 * b + u * b + v * s
            d.polygon([(x0, y0), (x0 + a, y0 + b), (x0 + a, y0 + b + s), (x0, y0 + s)], fill=c + (255,))
            c = right.getpixel((u, v))
            x0 = cx + u * a
            y0 = ty + 32 * b - u * b + v * s
            d.polygon([(x0, y0), (x0 + a, y0 - b), (x0 + a, y0 - b + s), (x0, y0 + s)], fill=c + (255,))
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    blocks = [
        ('sculk_deepslate', 'Sculk Deepslate', sculk_deepslate(), sculk_deepslate(), sculk_deepslate(),
         ['Reference (Cinch Missing Blocks): plain', 'stone-family variants, no glow.', 'Ours: slate + sculk veins, teal glow.']),
        ('deepmoss_block', 'Deepmoss Block', deepmoss_top(), deepmoss_side(), deepmoss_side(),
         ['Reference: mossy variants = green', 'speckle on gray brick.', 'Ours: dark teal moss cap over slate.']),
        ('hushed_deepslate_tiles', 'Hushed Deepslate Tiles', hushed_tiles(), hushed_tiles(), hushed_tiles(),
         ['Reference: brick/tile sets, cracked,', 'chiseled, pillar.', 'Ours: dished tiles, sculk dot inlay.']),
    ]
    for name, _, t, l, r_, _n in blocks:
        to_img(t if name != 'deepmoss_block' else l).save(os.path.join(OUT, name + '.png'))
    row = 16 * 8 * 2 + 20
    board = Image.new('RGB', (900, row * len(blocks) + 10), (30, 30, 34))
    d = ImageDraw.Draw(board)
    for i, (name, title, t, l, r_, notes) in enumerate(blocks):
        y = 10 + i * row
        d.text((14, y + 20), title, fill=(255, 255, 255))
        for k, n in enumerate(notes):
            d.text((14, y + 50 + k * 16), n, fill=(200, 200, 210))
        d.line([(430, y), (430, y + row - 20)], fill=(90, 90, 100))
        ct, cl, cr = to_img(t), shade(to_img(l), 0.8), shade(to_img(r_), 0.6)
        board.paste(cube(ct, cl, cr), (450, y), cube(ct, cl, cr))
    board.save(os.path.join(OUT, 'modref_board.png'))


if __name__ == '__main__':
    main()
