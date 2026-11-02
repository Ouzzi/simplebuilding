"""Usage: python3 tools/textures/ench01_preview.py <out_dir> [--vanilla <textures/block dir>]

b-ench01 (02.11.2026): Vorschau-Texturen fuer Dark Enchanter / End Enchanter, je Block zwei Varianten, eigene Pixel
(weiche Rampen nach A8b, kein Vanilla-Pixel). Schreibt NUR PNGs in <out_dir> (nichts ins Repo-Resources) und gibt
den Anteil harter Uebergaenge (Helligkeitssprung > 48 zwischen Nachbarpixeln) je Textur aus.
"""
import math, os, random, sys
from PIL import Image, ImageDraw

OUT = sys.argv[1]
VAN = sys.argv[sys.argv.index('--vanilla') + 1] if '--vanilla' in sys.argv else None
os.makedirs(OUT, exist_ok=True)
SRC = 'mc26_3/overlay/resources/assets/simplebuilding/textures/block/'


def ramp(*cols):
    return [tuple(c) for c in cols]


def smooth_noise(seed, g=4):
    r = random.Random(seed)
    grid = [[r.random() for _ in range(g)] for _ in range(g)]
    out = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            fx, fy = x * g / 16, y * g / 16
            x0, y0 = int(fx) % g, int(fy) % g
            x1, y1 = (x0 + 1) % g, (y0 + 1) % g
            tx, ty = fx - int(fx), fy - int(fy)
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            a = grid[y0][x0] * (1 - tx) + grid[y0][x1] * tx
            b = grid[y1][x0] * (1 - tx) + grid[y1][x1] * tx
            out[y][x] = a * (1 - ty) + b * ty + (r.random() - .5) * .16
    return out


def base(seed, rp, g=4):
    im = Image.new('RGBA', (16, 16))
    n = smooth_noise(seed, g)
    for y in range(16):
        for x in range(16):
            k = min(len(rp) - 1, max(0, int(n[y][x] * len(rp))))
            im.putpixel((x, y), rp[k] + (255,))
    return im


def P(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), tuple(c) + (255,))


def soften(im, f):
    # Kontrast um das Bildmittel verringern (A8b: weichere Uebergaenge, Farbton bleibt)
    px = [im.getpixel((x, y)) for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3]]
    m = [sum(p[i] for p in px) / len(px) for i in range(3)]
    for y in range(im.height):
        for x in range(im.width):
            c = im.getpixel((x, y))
            if c[3]:
                im.putpixel((x, y), tuple(int(m[i] + (c[i] - m[i]) * f) for i in range(3)) + (255,))
    return im


def top(S):
    im = base(S['seed'], S['body'])
    for y in range(16):
        for x in range(16):
            d = (abs(x - 7.5) + abs(y - 7.5)) if S['shape'] == 'rhomb' else math.hypot(x - 7.5, (y - 7.5)) * 1.25
            lim = 6.2 if S['shape'] == 'rhomb' else 6.0
            if d <= lim - 1.2:
                t = d / (lim - 1.2)
                P(im, x, y, S['cloth'][min(len(S['cloth']) - 1, int(t * len(S['cloth'])))] )
            elif d <= lim:
                P(im, x, y, S['hem'][1 if d < lim - .5 else 0])
            elif d <= lim + 1.0:  # Halo: weicher Uebergang Tuchrand -> Koerper
                a = im.getpixel((x, y)); h = S['hem'][0]
                P(im, x, y, tuple((a[i] * 2 + h[i]) // 3 for i in range(3)))
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        P(im, x, y, S['star'][-1])
    for dx, dy in ((0, -2), (0, 2), (-2, 0), (2, 0)):
        for ox, oy in ((7, 7), (8, 7), (7, 8), (8, 8)):
            if (dx and ox in (7, 8)) or (dy and oy in (7, 8)):
                pass
    for x, y in ((7, 5), (8, 5), (7, 10), (8, 10), (5, 7), (5, 8), (10, 7), (10, 8)):
        P(im, x, y, S['star'][-2])
    for x, y in ((7, 6), (8, 6), (7, 9), (8, 9), (6, 7), (6, 8), (9, 7), (9, 8)):
        P(im, x, y, S['star'][-3])
    if S.get('eye'):
        for x, y in ((7, 7), (8, 8)):
            P(im, x, y, S['eye'])
    for cx, cy in ((0, 0), (13, 0), (0, 13), (13, 13)):
        for j in range(3):
            for i in range(3):
                if (i, j) in ((2, 2),):
                    continue
                k = 3 if (i, j) == (0, 0) else 2 if i + j < 2 else 1
                P(im, cx + i, cy + j, S['corner'][k])
    return soften(im, S.get('soft', .62))


def side(S):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    b = base(S['seed'] + 1, S['body'], 3)
    for y in range(4, 16):
        for x in range(16):
            im.putpixel((x, y), b.getpixel((x, y)))
    for y in range(4, 16):  # pillars
        for x in (0, 1, 14, 15):
            P(im, x, y, S['corner'][1 if x in (0, 15) else 2])
    for x in (0, 15):
        P(im, x, 4, S['corner'][3])
    for x in range(2, 14):  # drape: V-shape cloth under a hem
        top_y = 6 + abs(x - 7.5) * 0.5
        for y in range(int(top_y), 16):
            t = (y - top_y) / (16 - top_y)
            P(im, x, y, S['cloth'][min(len(S['cloth']) - 1, int(t * len(S['cloth'])))])
        P(im, x, int(top_y), S['hem'][0])
    P(im, 7, 13, S['star'][-1]); P(im, 8, 13, S['star'][-2])
    return soften(im, S.get('soft', .62))


VARS = {
    # Dark B: Glut-Karmesin (Nether-Stoffe: Lohen-Obsidian, Karmesin)
    'dark_b': dict(seed=31, soft=.72, shape='rhomb',
        body=ramp((14, 8, 12), (24, 12, 18), (36, 17, 24), (50, 22, 28)),
        cloth=ramp((150, 52, 36), (124, 38, 36), (96, 28, 40), (72, 22, 40)),
        hem=ramp((150, 96, 30), (214, 156, 48), (246, 206, 96)),
        star=ramp((255, 150, 50), (255, 190, 80), (255, 236, 150)),
        corner=ramp((60, 16, 12), (160, 60, 20), (226, 112, 30), (255, 190, 80))),
    # End A: Purpur-Tuch auf Obsidian, Ender-Auge
    'end_a': dict(seed=47, soft=.6, shape='circle',
        body=ramp((10, 8, 18), (18, 12, 30), (28, 18, 44), (40, 26, 60)),
        cloth=ramp((176, 104, 190), (150, 80, 170), (120, 56, 146), (90, 38, 120)),
        hem=ramp((120, 110, 140), (150, 142, 172), (178, 170, 198)),
        star=ramp((110, 200, 150), (170, 235, 190), (236, 255, 236)),
        corner=ramp((50, 30, 74), (110, 70, 150), (160, 110, 200), (210, 170, 240)), eye=(30, 90, 70)),
    # End B: heller Endstein-Koerper mit tiefviolettem Tuch (Aschholz-Naehe)
    'end_b': dict(seed=59, soft=.56, shape='rhomb',
        body=ramp((122, 118, 84), (150, 146, 104), (178, 172, 124), (204, 198, 146)),
        cloth=ramp((84, 46, 120), (66, 34, 100), (50, 24, 80), (38, 18, 62)),
        hem=ramp((46, 150, 128), (80, 196, 168), (170, 240, 214)),
        star=ramp((150, 110, 210), (200, 170, 245), (250, 240, 255)),
        corner=ramp((70, 66, 46), (120, 114, 84), (190, 184, 132), (236, 230, 176))),
}
STUFF = {
    # Endholz-Regal (Aschholz-Rahmen, Endstein-Buecher) und End-Lohenobsidian-Boden
    'shelf': None, 'floor': None,
}


def shelf(seed=71):
    im = Image.new('RGBA', (16, 16))
    wood = ramp((78, 72, 62), (104, 96, 82), (128, 118, 100), (150, 140, 118))
    b = base(seed, wood, 4)
    im.paste(b)
    r = random.Random(seed)
    books = [((60, 40, 96), (96, 64, 140)), ((40, 100, 90), (70, 150, 130)), ((120, 100, 60), (170, 150, 90)), ((96, 40, 110), (150, 80, 160)), ((70, 70, 90), (110, 110, 140))]
    for row in (1, 9):
        x = 1
        while x < 15:
            w = r.choice((1, 2, 2))
            c = r.choice(books)
            for xx in range(x, min(x + w, 15)):
                for yy in range(row, row + 6):
                    P(im, xx, yy, c[0] if (xx == x) else c[1])
            x += w
    for y in (0, 7, 8, 15):
        for x in range(16):
            P(im, x, y, wood[2 if (x + y) % 2 else 1])
    return im


def floor(seed=83):
    im = base(seed, ramp((12, 8, 22), (22, 14, 38), (34, 20, 56), (46, 28, 72)), 4)
    r = random.Random(seed)
    for _ in range(3):  # weiche Adern in Ender-Gruen/Violett (Lohen-Obsidian-Verwandtschaft: Adern)
        x, y = r.randrange(2, 14), r.randrange(2, 14)
        for _ in range(7):
            P(im, x, y, (72, 190, 150) if r.random() < .6 else (150, 100, 220))
            x = max(0, min(15, x + r.choice((-1, 0, 1)))); y = max(0, min(15, y + r.choice((0, 1))))
    return im


def hard(im, thr=48):
    px = im.convert('RGBA').load(); n = h = 0; W, H = im.size
    lum = lambda p: .3 * p[0] + .59 * p[1] + .11 * p[2]
    for y in range(H):
        for x in range(W):
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < W and y + dy < H and px[x, y][3] and px[x + dx, y + dy][3]:
                    n += 1; h += abs(lum(px[x, y]) - lum(px[x + dx, y + dy])) > thr
    return round(100 * h / n, 1)


def frame0(path):
    return Image.open(path).convert('RGBA').crop((0, 0, 16, 16))


def iso(t, s, u=10, vh=10):
    W, H = 32 * u + 4, 17 * u + 12 * vh + 4
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    cx = 16 * u + 2; cy = 2
    for i in range(16):
        for j in range(16):
            X = cx + (i - j) * u; Y = cy + (i + j) * u // 2
            d.polygon([(X, Y), (X + u, Y + u // 2), (X, Y + u), (X - u, Y + u // 2)], fill=t.getpixel((i, j)))
    shade = lambda c, f: (int(c[0] * f), int(c[1] * f), int(c[2] * f), c[3])
    for k in range(12):
        for i in range(16):  # linke Flaeche (Sued)
            X = cx + (i - 16) * u; Y = cy + (i + 16) * u // 2 + 8 * 0 + k * vh + u // 2 * 0
            c = shade(s.getpixel((i, 4 + k)), .8)
            if c[3]:
                d.polygon([(X, Y + 0), (X + u, Y + u // 2), (X + u, Y + u // 2 + vh), (X, Y + vh)], fill=c)
        for j in range(16):  # rechte Flaeche (Ost)
            X = cx + (16 - j) * u; Y = cy + (16 + j) * u // 2 + k * vh
            c = shade(s.getpixel((j, 4 + k)), .62)
            if c[3]:
                d.polygon([(X - u, Y), (X, Y + u // 2 - u // 2 + u // 2), (X, Y + u // 2 + vh), (X - u, Y + vh)], fill=c)
    return im


def save(name, im, scale=1):
    if scale != 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    im.save(os.path.join(OUT, name))


res = {}
sets = {}
for n, S in VARS.items():
    sets[n] = (top(S), side(S))
sets['dark_a'] = (frame0(SRC + 'astral_enchanting_table_top.png'), frame0(SRC + 'astral_enchanting_table_side.png'))
if VAN:
    sets['vanilla'] = (frame0(VAN + '/enchanting_table_top.png'), frame0(VAN + '/enchanting_table_side.png'))
for n, (t, s) in sets.items():
    save(n + '_top_big.png', t, 12); save(n + '_side_big.png', s, 12)
    save(n + '_top.png', t); save(n + '_side.png', s)
    save(n + '_iso.png', iso(t, s))
    res[n] = (hard(t), hard(s.crop((0, 4, 16, 16))))
save('shelf.png', shelf(), 8); save('floor.png', floor(), 8)
res['shelf'] = (hard(shelf()), 0); res['floor'] = (hard(floor()), 0)
for k, v in res.items():
    print(k, 'hart oben %.1f %%, Seite %.1f %%' % v)
