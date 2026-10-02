"""Usage: python tools/textures/proposals_v10_2026_10_02.py <preview dir>

Round 10 (owner 2026-10-02): the original copper core, made a little more compact without changing much - ten
proposals, each one small, mechanical change of the old texture (no repaint)."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import cores_2026_10_02 as cores  # noqa: E402

OUT = sys.argv[1] if len(sys.argv) > 1 else 'build/proposals-v10/'
C = 8


def load(name='copper'):
    return Image.open(os.path.join(cores.SRC, f'{name}_core.png')).convert('RGBA').crop((0, 0, 16, 16))


def _in(v):
    return v - 1 if v < C else v + 1 if v > C else v


def remap(core, shift, keep_mid=3):
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            sx, sy = shift(x, y)
            if 0 <= sx < 16 and 0 <= sy < 16 and core.getpixel((sx, sy))[3]:
                out.putpixel((x, y), core.getpixel((sx, sy)))
    for y in range(16):
        for x in range(16):
            if abs(x - C) + abs(y - C) <= keep_mid and core.getpixel((x, y))[3]:
                out.putpixel((x, y), core.getpixel((x, y)))
    return out


def drop(core, test):
    out = core.copy()
    for y in range(16):
        for x in range(16):
            if test(abs(x - C), abs(y - C)):
                out.putpixel((x, y), (0, 0, 0, 0))
    return out


def axis_tips(dx, dy):
    return min(dx, dy) == 0 and max(dx, dy) >= 6


def corner_tips(dx, dy):
    return dx + dy >= 10


PROPOSALS = [
    ('Achsspitzen kürzer', lambda c: drop(c, axis_tips)),
    ('Eckspitzen kürzer', lambda c: drop(c, corner_tips)),
    ('alle Spitzen kürzer', lambda c: drop(c, lambda dx, dy: axis_tips(dx, dy) or corner_tips(dx, dy))),
    ('Ecken 1 px hinein', lambda c: remap(c, lambda x, y: (_in(x), _in(y)) if abs(x - C) >= 2 and abs(y - C) >= 2 else (x, y))),
    ('Achsen 1 px hinein', lambda c: remap(c, lambda x, y: (_in(x), y) if abs(y - C) <= 1 else (x, _in(y)) if abs(x - C) <= 1 else (x, y))),
    ('Außenring 1 px hinein', lambda c: remap(c, lambda x, y: (_in(x) if abs(x - C) >= 6 else x, _in(y) if abs(y - C) >= 6 else y))),
    ('Rand ab 5 hinein', lambda c: remap(c, lambda x, y: (_in(x) if abs(x - C) >= 5 else x, _in(y) if abs(y - C) >= 5 else y))),
    ('Ecken hinein + Achsspitzen kürzer', lambda c: drop(remap(c, lambda x, y: (_in(x), _in(y)) if abs(x - C) >= 2 and abs(y - C) >= 2 else (x, y)), axis_tips)),
    ('Rand ab 5 hinein + Eckspitzen kürzer', lambda c: drop(remap(c, lambda x, y: (_in(x) if abs(x - C) >= 5 else x, _in(y) if abs(y - C) >= 5 else y)), lambda dx, dy: dx + dy >= 9)),
    ('nur seitlich schmaler', lambda c: remap(c, lambda x, y: (_in(x) if abs(x - C) >= 5 else x, y))),
]


def main():
    os.makedirs(OUT, exist_ok=True)
    core = load()
    cell = 110
    s = Image.new('RGBA', (6 * cell + 10, 2 * (cell + 60) + 10), (139, 139, 139, 255))
    d = ImageDraw.Draw(s)
    items = [('alt', core)] + [(f'{i + 1}: {n}', f(core)) for i, (n, f) in enumerate(PROPOSALS)]
    for i, (label, im) in enumerate(items):
        col, row = (0, 0) if i == 0 else ((i - 1) % 5 + 1, (i - 1) // 5)
        x, y = 8 + col * cell, 8 + row * (cell + 60)
        d.text((x, y), label[:18], fill=(255, 255, 255))
        s.alpha_composite(im.resize((96, 96), Image.NEAREST), (x, y + 14))
        s.alpha_composite(im, (x, y + 116))
        s.alpha_composite(im.resize((32, 32), Image.NEAREST), (x + 24, y + 116))
    s.save(os.path.join(OUT, 'kupferkern-kompakter-1-10.png'))
    print('ok ->', OUT)


if __name__ == '__main__':
    main()
