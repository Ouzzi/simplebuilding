"""Shaping Saw textures (Simple Blocks, owner 17.10., principle A4b): own 16x16 pixel art, no Vanilla pixels.

Close to the Vanilla stonecutter (9 px table, round blade across the middle, same palette families), but showing the
broader use: stone AND wood on the table and a bigger, toothed, animated blade. Three variants:
  A  table half stone, half planks; copper corner caps; dark wooden legs with a copper rail; big-tooth blade, copper hub
  B  stone table with a wooden rip fence and a scale; stone sides with a wooden band; fine-tooth blade, iron hub
  C  plank table with a stone inlay around the slot; plank sides on stone feet; two-tone blade
Variant A is wired (modules/simpleblocks/.../textures/block/shaping_saw_*.png + the blade's .mcmeta).

  python3.12 tools/textures/shaping_saw_2026_10_17.py            # writes variant A + preview PNGs (SB_PREVIEW_DIR)
  python3.12 tools/textures/shaping_saw_2026_10_17.py --check    # are the wired PNGs current?
SB_PREVIEW_DIR (default /tmp/claude-0/ash/formsaege): per variant the four textures, 8x sheets and an 8x isometric render.
"""
import argparse
import io
import json
import math
import os
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block'
PREVIEW = Path(os.environ.get('SB_PREVIEW_DIR', '/tmp/claude-0/ash/formsaege'))
WIRED = 'A'

T = (0, 0, 0, 0)
STONE = [(0x5f, 0x5f, 0x5f), (0x68, 0x68, 0x68), (0x74, 0x74, 0x74), (0x7f, 0x7f, 0x7f), (0x8f, 0x8f, 0x8f), (0x9a, 0x9a, 0x9a)]
PLANK = [(0x5e, 0x48, 0x28), (0x7e, 0x62, 0x37), (0x96, 0x74, 0x41), (0xa6, 0x86, 0x4f), (0xb4, 0x92, 0x5c), (0xc0, 0x9c, 0x63)]
FRAME = [(0x3f, 0x29, 0x17), (0x4d, 0x31, 0x1c), (0x59, 0x3b, 0x24), (0x66, 0x42, 0x27), (0x75, 0x4d, 0x2e)]
COPPER = [(0x6d, 0x3a, 0x22), (0x9c, 0x54, 0x32), (0xc0, 0x6b, 0x45), (0xe0, 0x8a, 0x5e), (0xf2, 0xb0, 0x86)]
IRON = [(0x4a, 0x4a, 0x4e), (0x76, 0x76, 0x7a), (0xa4, 0xa4, 0xa8), (0xc8, 0xc8, 0xcc), (0xe4, 0xe4, 0xe8), (0xf6, 0xf6, 0xf8)]
SLOT = [(0x26, 0x26, 0x28), (0x36, 0x36, 0x3a)]


def grid(w=16, h=16, fill=T):
    return [[fill for _ in range(w)] for _ in range(h)]


def stone_noise(seed):
    """Vanilla-like stone: soft clusters of 4 greys, a few light and dark specks (own pattern)."""
    r = random.Random(seed)
    g = [[STONE[r.choice([2, 3, 3, 3, 4])] for _ in range(16)] for _ in range(16)]
    for _ in range(10):  # short horizontal streaks, like the strata of Vanilla stone
        x, y = r.randrange(16), r.randrange(16)
        c = STONE[r.choice([1, 2, 4])]
        for dx in range(r.randrange(2, 4)):
            g[y][(x + dx) % 16] = c
    for _ in range(6):
        g[r.randrange(16)][r.randrange(16)] = STONE[r.choice([0, 5])]
    return g


def plank_rows(seed, horizontal=True):
    """Boards of 4 px with a dark seam, grain streaks and nail dots (own pattern)."""
    r = random.Random(seed)
    g = grid()
    for y in range(16):
        for x in range(16):
            a, b = (y, x) if horizontal else (x, y)
            board = a // 4
            if a % 4 == 3:
                c = PLANK[0] if (b + board * 5) % 16 not in (4, 11) else PLANK[1]
            else:
                c = PLANK[[3, 4, 3, 2][(b * 3 + board * 7 + (a % 4) * 5) % 4]]
            g[y][x] = c
    for _ in range(12):  # grain
        a, b = r.randrange(16), r.randrange(16)
        if a % 4 == 3:
            continue
        for d in range(r.randrange(2, 5)):
            y, x = (a, (b + d) % 16) if horizontal else ((b + d) % 16, a)
            g[y][x] = PLANK[r.choice([2, 5])]
    return g


def put(g, x, y, c):
    if 0 <= x < 16 and 0 <= y < len(g):
        g[y][x] = c


def corner_caps(g, pal, size=2):
    for cx, cy in ((0, 0), (16 - size, 0), (0, 16 - size), (16 - size, 16 - size)):
        for y in range(size):
            for x in range(size):
                put(g, cx + x, cy + y, pal[2])
        put(g, cx + (0 if cx == 0 else size - 1), cy + (0 if cy == 0 else size - 1), pal[1])
        put(g, cx + (size - 1 if cx == 0 else 0), cy + (size - 1 if cy == 0 else 0), pal[3])


def blade_slot(g, x0=1, x1=15, rim=IRON):
    """The cut through the table (rows 7-8), dark inside, light iron lip above, darker lip below."""
    for x in range(x0, x1):
        put(g, x, 6, rim[2])
        put(g, x, 7, SLOT[0])
        put(g, x, 8, SLOT[1])
        put(g, x, 9, rim[1])
    put(g, x0 - 1, 7, rim[1]); put(g, x0 - 1, 8, rim[1])
    put(g, x1, 7, rim[3]); put(g, x1, 8, rim[2])


# ------------------------------------------------------------------ tops

def top_a():
    g = stone_noise(3)
    p = plank_rows(5, horizontal=False)
    for y in range(9, 16):
        for x in range(16):
            g[y][x] = p[y][x]
    blade_slot(g)
    corner_caps(g, COPPER)
    return g


def top_b():
    g = stone_noise(7)
    blade_slot(g)
    for x in range(1, 15):  # wooden rip fence, 2 px, light top edge
        put(g, x, 12, PLANK[4]); put(g, x, 13, PLANK[2])
    put(g, 1, 12, PLANK[1]); put(g, 14, 13, PLANK[0])
    for x in range(2, 15, 2):  # scale along the north edge
        put(g, x, 1, STONE[0]); put(g, x, 2, STONE[0] if x % 4 == 2 else STONE[1])
    corner_caps(g, IRON)
    return g


def top_c():
    g = plank_rows(9, horizontal=False)
    s = stone_noise(13)
    for y in range(4, 12):
        for x in range(1, 15):
            g[y][x] = s[y][x]
    for x in range(1, 15):
        put(g, x, 4, STONE[5]); put(g, x, 11, STONE[1])
    blade_slot(g)
    corner_caps(g, COPPER)
    return g


# ------------------------------------------------------------------ sides (rows 7-15 visible, like the stonecutter)

def side_a():
    g = grid()
    s = stone_noise(17)
    for y in range(7, 16):
        for x in range(16):
            g[y][x] = s[y][x]
    for y in range(7, 16):  # dark wooden legs
        for x in (0, 1, 14, 15):
            g[y][x] = FRAME[[2, 3, 3, 1][(y + x) % 4]]
        g[y][0] = FRAME[1]; g[y][15] = FRAME[0]
    for x in range(16):  # copper rail under the table top
        g[7][x] = COPPER[3] if x % 5 else COPPER[4]
        g[8][x] = COPPER[1]
    for x in range(2, 14):  # wooden cross bar near the foot
        g[13][x] = FRAME[3]; g[14][x] = FRAME[1]
    for x, y in ((1, 10), (14, 10)):
        g[y][x] = COPPER[3]
    return g


def side_b():
    g = grid()
    s = stone_noise(19)
    for y in range(7, 16):
        for x in range(16):
            g[y][x] = s[y][x]
    for x in range(16):
        g[7][x] = STONE[5]
        g[10][x] = PLANK[4]; g[11][x] = PLANK[2]
        g[15][x] = STONE[0]
    for y in range(7, 16):
        g[y][0] = IRON[1] if y in (7, 15) else STONE[1]
        g[y][15] = IRON[0] if y in (7, 15) else STONE[0]
    return g


def side_c():
    g = grid()
    p = plank_rows(23, horizontal=True)
    s = stone_noise(29)
    for y in range(7, 16):
        for x in range(16):
            g[y][x] = p[y][x] if y < 13 else s[y][x]
    for x in range(16):
        g[7][x] = PLANK[5]
        g[13][x] = STONE[5]; g[15][x] = STONE[0]
    for x, y in ((1, 9), (14, 9), (1, 11), (14, 11)):
        g[y][x] = COPPER[3]
    return g


def bottom():
    g = stone_noise(31)
    for i in range(16):
        for a, b in ((i, 0), (i, 15), (0, i), (15, i)):
            g[b][a] = STONE[1]
    return g


# ------------------------------------------------------------------ blade (16 x 48, 3 frames, uv rows 9-15 visible)

def blade(variant):
    teeth, hub = {'A': (8, COPPER), 'B': (12, IRON), 'C': (8, COPPER)}[variant]
    frames = []
    for f in range(3):
        g = grid()
        for y in range(9, 16):
            for x in range(1, 15):
                dx, dy = x + 0.5 - 8, y + 0.5 - 16
                d = math.hypot(dx, dy)
                ang = math.atan2(-dy, dx)  # 0..pi over the half disc
                phase = (ang / math.pi * teeth + f / 3.0) % 1.0
                if d <= 2.2:
                    c = hub[3] if d < 1.2 else hub[1]
                elif d <= 5.6:
                    if variant == 'C' and d > 4.3:
                        c = IRON[2]
                    else:
                        c = IRON[4] if (x + y + f) % 5 else IRON[3]
                    if abs(d - 3.6) < 0.5 and variant != 'B':
                        c = IRON[3]
                elif d <= 6.6:
                    c = IRON[5] if phase < 0.6 else IRON[2]
                elif d <= 7.6 and phase < 0.45:
                    c = IRON[5] if phase < 0.25 else IRON[3]
                else:
                    continue
                g[y][x] = c + (255,) if len(c) == 3 else c
        frames.append(g)
    return frames


def img(g):
    h = len(g)
    im = Image.new('RGBA', (16, h), (0, 0, 0, 0))
    for y in range(h):
        for x in range(16):
            c = g[y][x]
            im.putpixel((x, y), c if len(c) == 4 else c + (255,))
    return im


def variant(v):
    tops = {'A': top_a, 'B': top_b, 'C': top_c}
    sides = {'A': side_a, 'B': side_b, 'C': side_c}
    frames = blade(v)
    saw = Image.new('RGBA', (16, 48), (0, 0, 0, 0))
    for i, fr in enumerate(frames):
        saw.paste(img(fr), (0, 16 * i))
    side = img(sides[v]())
    return {'top': img(tops[v]()), 'side': side, 'bottom': img(bottom()), 'saw': saw}


MCMETA = {'animation': {'interpolate': False, 'frametime': 2}}


def png_bytes(im):
    buf = io.BytesIO()
    im.save(buf, 'PNG', optimize=True)
    return buf.getvalue()


# ------------------------------------------------------------------ previews

def iso(textures, s=8, frame=0):
    """Isometric render of the model: a 16x9x16 table and the blade plane across z=8 (seen from south-east)."""
    c, si = math.cos(math.radians(30)), math.sin(math.radians(30))
    W, H = int(32 * c * s) + 8, int((32 * si + 16) * s) + 8
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    ox, oy = W / 2, 16 * s + 4

    def P(x, y, z):
        return (ox + (x - z) * c * s, oy + (x + z) * si * s - y * s)

    def quad(pts, col, light):
        rgb = tuple(int(v * light) for v in col[:3])
        d.polygon([P(*p) for p in pts], fill=rgb + (255,))

    top, side = textures['top'], textures['side']
    saw = textures['saw'].crop((0, 16 * frame, 16, 16 * frame + 16))
    for v in range(16):  # top y=9
        for u in range(16):
            quad([(u, 9, v), (u + 1, 9, v), (u + 1, 9, v + 1), (u, 9, v + 1)], top.getpixel((u, v)), 1.0)
    for r in range(7, 16):  # south face z=16 and east face x=16
        y0, y1 = 16 - r, 15 - r
        for u in range(16):
            quad([(u, y0, 16), (u + 1, y0, 16), (u + 1, y1, 16), (u, y1, 16)], side.getpixel((u, r)), 0.8)
            quad([(16, y0, 16 - u), (16, y0, 15 - u), (16, y1, 15 - u), (16, y1, 16 - u)], side.getpixel((u, r)), 0.62)
    for r in range(9, 16):  # blade on z=8, south side (uv mirrored)
        y0, y1 = 16 - (r - 9), 15 - (r - 9)
        for u in range(1, 15):
            px = saw.getpixel((u, r))
            if px[3] == 0:
                continue
            quad([(u, y0, 8), (u + 1, y0, 8), (u + 1, y1, 8), (u, y1, 8)], px, 0.95)
    return im


def sheet(textures, s=8):
    order = [('top', textures['top']), ('side', textures['side']), ('bottom', textures['bottom']), ('saw', textures['saw'])]
    w = sum(16 * s + 8 for _ in order)
    im = Image.new('RGBA', (w, 48 * s), (0, 0, 0, 0))
    x = 0
    for _, t in order:
        im.paste(t.resize((t.width * s, t.height * s), Image.NEAREST), (x, 0))
        x += 16 * s + 8
    return im


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    a = ap.parse_args()
    wired = variant(WIRED)
    files = {OUT / f'shaping_saw_{k}.png': png_bytes(v) for k, v in wired.items()}
    mcmeta = (json.dumps(MCMETA, indent=2) + '\n').encode()
    files[OUT / 'shaping_saw_saw.png.mcmeta'] = mcmeta
    if a.check:
        stale = []
        for path, data in files.items():
            if not path.exists():
                stale.append(path.name)
            elif path.suffix == '.png':
                if Image.open(path).convert('RGBA').tobytes() != Image.open(io.BytesIO(data)).convert('RGBA').tobytes():
                    stale.append(path.name)
            elif path.read_bytes() != data:
                stale.append(path.name)
        if stale:
            sys.exit(f'shaping saw textures stale: {stale} (run tools/textures/shaping_saw_2026_10_17.py)')
        print('shaping saw textures current (variant %s)' % WIRED)
        return
    for path, data in files.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    for v in 'ABC':
        t = variant(v)
        for k, im in t.items():
            im.save(PREVIEW / f'{v}_{k}.png')
        sheet(t).save(PREVIEW / f'{v}_sheet.png')
        for f in range(3):
            iso(t, frame=f).save(PREVIEW / f'{v}_iso{f}.png')
    print(f'wrote variant {WIRED} to {OUT.relative_to(ROOT)} and previews A/B/C to {PREVIEW}')


if __name__ == '__main__':
    main()
