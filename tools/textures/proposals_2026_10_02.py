"""Usage: python tools/textures/proposals_2026_10_02.py <vanilla textures dir> <preview dir>

Texture proposals (owner 2026-10-02): three unanimated, vanilla-like variants each for the Velocity
Gauge, Ore Detector, Attractor, Amethyst Resonance Rod, Echo Sounder, both guide books, Deepslate Sage
Ore, Sage Orb and the six cores. Every variant starts from a vanilla texture (compass, clock, recovery
compass, spyglass, book, heart of the sea, nether star, ender eye, experience orb, deepslate diamond ore)
and keeps its outline and shading ranks; only colours and the face content change. Writes one preview
sheet per item (current | A | B | C, 8x, plus 1x) and the 16x16 candidates into <preview dir>/png/."""
from PIL import Image, ImageDraw
import os
import sys

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
OUT = sys.argv[2] if len(sys.argv) > 2 else 'build/proposals/'
ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
MOD_T = os.path.join(ROOT, 'src/main/resources/assets/simplebuilding/textures/')
MOD_O = os.path.join(ROOT, 'mc26_3/overlay/resources/assets/simplebuilding/textures/')


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def van(name):
    return Image.open(os.path.join(V, name + '.png')).convert('RGBA')


def mod(name):
    for base in (MOD_O, MOD_T):
        path = os.path.join(base, name + '.png')
        if os.path.exists(path):
            return Image.open(path).convert('RGBA').crop((0, 0, 16, 16))
    raise FileNotFoundError(name)


def shades(im):
    return sorted({im.getpixel((x, y))[:3] for x in range(im.width) for y in range(im.height) if im.getpixel((x, y))[3] > 0}, key=lum)


def ramp_of(im, n):
    px = sorted((im.getpixel((x, y))[:3] for x in range(im.width) for y in range(im.height) if im.getpixel((x, y))[3] > 200), key=lum)
    return [px[round(0.05 * (len(px) - 1) + i * 0.9 * (len(px) - 1) / max(1, n - 1))] for i in range(n)]


def recolor(im, ramp, keep=()):
    """Every colour of `im` (by brightness rank) onto `ramp`; colours in `keep` stay."""
    src = [c for c in shades(im) if c not in keep]
    tones = [ramp[round(i * (len(ramp) - 1) / max(1, len(src) - 1))] for i in range(len(src))]
    m = dict(zip(src, tones))
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] and p[:3] in m:
                out.putpixel((x, y), m[p[:3]] + (p[3],))
    return out


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def lerp_ramp(dark, light, n):
    return [mix(dark, light, i / (n - 1)) for i in range(n)]


def put(im, pts, colour):
    for x, y in pts:
        im.putpixel((x, y), tuple(colour) + (255,))


def replace(im, src, dst):
    for y in range(16):
        for x in range(16):
            p = im.getpixel((x, y))
            if p[3] and p[:3] == src:
                im.putpixel((x, y), tuple(dst) + (255,))


def clear_face(im, face, keep_ring, cx=7.5, cy=7.5, r=4.6):
    """Paint every pixel inside the dial (radius r) that is not ring/outline in the face colour."""
    for y in range(16):
        for x in range(16):
            p = im.getpixel((x, y))
            if p[3] and (x - cx) ** 2 + (y - cy) ** 2 <= r * r and p[:3] not in keep_ring:
                im.putpixel((x, y), tuple(face) + (255,))


# --- material ramps (dark -> light), from vanilla/mod items -------------------------------------
def material_ramps():
    return {
        'copper': ramp_of(van('item/copper_ingot'), 6),
        'iron': ramp_of(van('item/iron_ingot'), 6),
        'gold': ramp_of(van('item/gold_ingot'), 6),
        'diamond': ramp_of(van('item/diamond'), 6),
        'netherite': ramp_of(van('item/netherite_ingot'), 6),
        'enderite': ramp_of(mod('item/enderite_ingot'), 6),
    }


WHITE, BLACK = (255, 255, 255), (24, 23, 23)
GREEN, YELLOW, RED = (92, 196, 58), (232, 206, 52), (210, 44, 36)


# --- Velocity Gauge -----------------------------------------------------------------------------
def gauge():
    out = []
    # A: vanilla compass case, dark face, green-yellow-red tick arc, white needle to the upper right
    a = van('item/compass_00')
    clear_face(a, (47, 47, 47), {(24, 23, 23), (53, 53, 53), (94, 94, 94), (130, 130, 130), (168, 168, 168), (216, 216, 216), (255, 255, 255)})
    put(a, [(5, 7), (5, 6)], GREEN); put(a, [(6, 5), (7, 4)], YELLOW); put(a, [(9, 4), (10, 5)], RED)
    put(a, [(8, 6), (9, 5)], WHITE); put(a, [(7, 7)], (168, 168, 168)); put(a, [(6, 8)], (94, 94, 94))
    out.append(a)
    # B: compass case in copper, same dial as A
    b = recolor(van('item/compass_00'), ramp_of(van('item/copper_ingot'), 9))
    clear_face(b, (40, 30, 26), set())
    put(b, [(5, 7), (5, 6)], GREEN); put(b, [(6, 5), (7, 4)], YELLOW); put(b, [(9, 4), (10, 5)], RED)
    put(b, [(8, 6), (9, 5)], WHITE); put(b, [(7, 7)], (190, 190, 190)); put(b, [(6, 8)], (120, 90, 70))
    out.append(b)
    # C: recovery-compass case recoloured iron, speed arc as a filled band
    c = recolor(van('item/recovery_compass_00'), ramp_of(van('item/iron_ingot'), 8))
    clear_face(c, (34, 36, 40), set(), r=4.2)
    put(c, [(4, 8), (4, 7), (5, 6)], GREEN); put(c, [(6, 5), (7, 4), (8, 4)], YELLOW); put(c, [(9, 5), (10, 6)], RED)
    put(c, [(8, 7), (9, 6)], WHITE); put(c, [(7, 8)], (160, 160, 160))
    out.append(c)
    return out


# --- Ore Detector -------------------------------------------------------------------------------
def detector():
    out = []
    ring = {(24, 23, 23), (53, 53, 53), (94, 94, 94), (130, 130, 130), (168, 168, 168), (216, 216, 216), (255, 255, 255)}
    # A: compass case with a diamond crystal in the middle of a dark face
    a = van('item/compass_00')
    clear_face(a, (47, 47, 47), ring)
    put(a, [(7, 5), (8, 5)], (161, 251, 232)); put(a, [(6, 6), (7, 6), (8, 6), (9, 6)], (32, 197, 181))
    put(a, [(6, 7), (9, 7)], (14, 99, 89)); put(a, [(7, 7), (8, 7)], (161, 251, 232)); put(a, [(7, 8), (8, 8)], (14, 99, 89))
    out.append(a)
    # B: compass case in gold with three ore dots (coal, iron, diamond) on the face
    b = recolor(van('item/compass_00'), ramp_of(van('item/gold_ingot'), 9))
    clear_face(b, (52, 40, 22), set())
    put(b, [(5, 7), (6, 7)], (60, 60, 60)); put(b, [(8, 5), (9, 5)], (216, 175, 147)); put(b, [(8, 9), (9, 9)], (93, 236, 245))
    put(b, [(7, 7)], WHITE)
    out.append(b)
    # C: dowsing tool from the spyglass: copper body, diamond lens
    c = van('item/spyglass')
    lens = {(30, 101, 102), (51, 144, 144), (104, 189, 189), (255, 255, 255)}
    body = [x for x in shades(c) if x not in lens]
    for col, t in zip(sorted(body, key=lum), ramp_of(van('item/copper_ingot'), len(body))):
        replace(c, col, t)
    for col, t in zip(sorted(lens, key=lum), [(14, 99, 89), (32, 197, 181), (161, 251, 232), (255, 255, 255)]):
        replace(c, col, t)
    out.append(c)
    return out


# --- Attractor (horseshoe magnet) ---------------------------------------------------------------
MAGNET = [
    '................',
    '................',
    '....oooo.oooo...',
    '...oHHHo.oHHHo..',
    '...oTTTo.oTTTo..',
    '...oAAAo.oBBBo..',
    '...oaAAo.obBBo..',
    '...oaAAo.obBBo..',
    '...oaAAooobBBo..',
    '...oaaAAAABBbo..',
    '....oaaAAABBo...',
    '.....oaaaabo....',
    '......oooo......',
    '................',
    '................',
    '................',
]


def magnet(left, right, tip):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    pal = {'o': (36, 22, 24), 'A': left[1], 'a': left[0], 'B': right[1], 'b': right[0], 'T': tip[0], 'H': tip[1]}
    for y, row in enumerate(MAGNET):
        for x, ch in enumerate(row):
            if ch in pal:
                im.putpixel((x, y), pal[ch] + (255,))
    return im


def attractor():
    iron = ramp_of(van('item/iron_ingot'), 4)
    return [
        magnet(((150, 28, 28), (214, 52, 44)), ((150, 28, 28), (214, 52, 44)), (iron[1], iron[3])),
        magnet(((150, 28, 28), (214, 52, 44)), ((40, 64, 150), (72, 108, 214)), (iron[1], iron[3])),
        magnet((iron[0], iron[2]), (iron[0], iron[2]), ((165, 92, 60), (226, 148, 104))),
    ]


# --- Amethyst Resonance Rod ---------------------------------------------------------------------
def rod():
    out = []
    am = shades(van('item/amethyst_shard'))
    # A: spyglass body in copper, amethyst lens
    a = van('item/spyglass')
    lens = {(30, 101, 102), (51, 144, 144), (104, 189, 189), (255, 255, 255)}
    body = [x for x in shades(a) if x not in lens]
    for col, t in zip(sorted(body, key=lum), ramp_of(van('item/copper_ingot'), len(body))):
        replace(a, col, t)
    for col, t in zip(sorted(lens, key=lum), [am[0], am[2], am[4], am[6]]):
        replace(a, col, t)
    out.append(a)
    # B: stick handle (from the arrow shaft colours) with an amethyst shard head, top right
    b = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    shard = van('item/amethyst_shard').resize((9, 9), Image.NEAREST)
    for i in range(8):
        put(b, [(2 + i, 13 - i)], (137, 103, 39)); put(b, [(2 + i, 14 - i)], (40, 30, 11))
    put(b, [(9, 6), (10, 5)], (216, 216, 216))
    b.alpha_composite(shard, (7, 0))
    out.append(b)
    # C: lightning-rod style copper rod with a small amethyst cluster
    c = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cop = ramp_of(van('item/copper_ingot'), 4)
    for i in range(9):
        put(c, [(3 + i, 13 - i)], cop[2]); put(c, [(3 + i, 14 - i)], cop[0]); put(c, [(4 + i, 13 - i)], cop[1])
    put(c, [(12, 4), (11, 3), (13, 3), (12, 2), (12, 3)], am[5]); put(c, [(11, 2), (13, 2), (12, 1)], am[3]); put(c, [(13, 4), (11, 4)], am[1])
    out.append(c)
    return out


# --- Echo Sounder (a little bigger than the recovery compass) -----------------------------------
def echo():
    out = []
    base = van('item/recovery_compass_00')
    big = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    big.alpha_composite(base.resize((16, 16), Image.NEAREST))
    # A: recovery compass case scaled to the full 16 px, concentric echo rings on the face
    a = van('item/recovery_compass_00').crop((0, 1, 16, 15)).resize((16, 16), Image.NEAREST)
    clear_face(a, (11, 22, 18), {(13, 13, 13), (33, 46, 43), (71, 68, 68), (94, 94, 94), (130, 130, 130), (168, 168, 168), (255, 255, 255)}, r=5.0)
    d = ImageDraw.Draw(a)
    d.ellipse((4, 4, 11, 11), outline=(16, 82, 87)); d.ellipse((6, 6, 9, 9), outline=(19, 142, 153))
    put(a, [(7, 7), (8, 8), (7, 8), (8, 7)], (41, 223, 235))
    out.append(a)
    # B: same case, echo shard in the middle and two wave arcs to the upper right
    b = a.copy()
    clear_face(b, (11, 22, 18), {(13, 13, 13), (33, 46, 43), (71, 68, 68), (94, 94, 94), (130, 130, 130), (168, 168, 168), (255, 255, 255)}, r=5.0)
    put(b, [(6, 9), (7, 8), (8, 7)], (0, 146, 149)); put(b, [(7, 9), (8, 8), (9, 7)], (10, 80, 96)); put(b, [(8, 6), (6, 8)], (41, 223, 235))
    put(b, [(9, 4), (10, 5), (11, 6)], (19, 142, 153)); put(b, [(10, 3), (11, 4), (12, 5)], (16, 82, 87))
    out.append(b)
    # C: deepslate-dark ring with sculk-teal rim and a sonar sweep
    c = a.copy()
    rim = {(130, 130, 130): (19, 142, 153), (168, 168, 168): (20, 187, 198), (94, 94, 94): (16, 82, 87)}
    for s, t in rim.items():
        replace(c, s, t)
    clear_face(c, (8, 14, 16), {(13, 13, 13), (33, 46, 43), (71, 68, 68), (19, 142, 153), (20, 187, 198), (16, 82, 87), (255, 255, 255)}, r=5.0)
    put(c, [(8, 7), (9, 6), (10, 5)], (41, 223, 235)); put(c, [(9, 7), (10, 6)], (0, 102, 148)); put(c, [(8, 8)], WHITE)
    put(c, [(5, 10), (10, 10)], (0, 84, 121))
    out.append(c)
    return out


# --- Guide books (mod blue, vanilla brown) ------------------------------------------------------
def books():
    blue = lerp_ramp((18, 30, 72), (86, 128, 214), 6)
    brown = shades(van('item/book'))
    out = {}
    for key, cover in (('guide_book', blue), ('guide_book_vanilla_start', None)):
        variants = []
        for name in ('item/book', 'item/written_book', 'item/enchanted_book'):
            im = van(name)
            if name == 'item/enchanted_book':
                im = van('item/book')
            paper = [c for c in shades(im) if abs(c[0] - c[1]) < 8 and abs(c[1] - c[2]) < 8 and lum(c) > 80]
            leather = [c for c in shades(im) if c not in paper]
            if cover is not None:
                for c, t in zip(sorted(leather, key=lum), [cover[round(i * 5 / max(1, len(leather) - 1))] for i in range(len(leather))]):
                    replace(im, c, t)
            if name == 'item/enchanted_book':  # gold band across the cover, like the guide's bookmark
                put(im, [(7, 2), (7, 3), (7, 4), (8, 5), (8, 6), (8, 7)], (226, 172, 42)); put(im, [(6, 3), (6, 4), (7, 5), (7, 6)], (156, 102, 22))
            if name == 'item/written_book':  # small emblem on the cover
                put(im, [(7, 6), (8, 7), (6, 7), (7, 8)], (236, 196, 64)); put(im, [(7, 7)], (255, 240, 150))
            variants.append(im)
        out[key] = variants
    return out


# --- Deepslate Sage Ore -------------------------------------------------------------------------
def sage_ore():
    base = van('block/deepslate')
    ore = van('block/deepslate_diamond_ore')
    specks = [(x, y) for y in range(16) for x in range(16) if ore.getpixel((x, y))[:3] != base.getpixel((x, y))[:3]]
    speck_cols = sorted({ore.getpixel(p)[:3] for p in specks}, key=lum)
    out = []
    for dark, light in (((46, 96, 18), (214, 248, 120)), ((22, 92, 60), (150, 240, 170)), ((92, 104, 14), (246, 236, 112))):
        r = lerp_ramp(dark, light, len(speck_cols))
        m = dict(zip(speck_cols, r))
        im = ore.copy()
        for p in specks:
            im.putpixel(p, m[ore.getpixel(p)[:3]] + (255,))
        out.append(im)
    return out


# --- Sage Orb: vanilla experience orb frames ----------------------------------------------------
def sage_orb():
    sheet = Image.open(os.path.join(V, 'entity/experience/experience_orb.png')).convert('RGBA')
    frame = lambda i: sheet.crop(((i % 4) * 16, (i // 4) * 16, (i % 4) * 16 + 16, (i // 4) * 16 + 16))
    out = []
    # Vanilla tints the grey orb sprite in the renderer (red and blue pulse, green full); the item uses the
    # green-yellow middle of that pulse. A: 12 px frame, B: 10 px frame, C: 12 px frame with the warm core.
    for i in (2, 1, 4):
        f = frame(i)
        g = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for y in range(16):
            for x in range(16):
                p = f.getpixel((x, y))
                if p[3]:
                    g.putpixel((x, y), (round(p[0] * 0.55), p[1], round(p[2] * 0.12), p[3]))
        out.append(g)
    return out


# --- Cores ---------------------------------------------------------------------------------------
def cores():
    ramps = material_ramps()
    shapes = [van('item/heart_of_the_sea'), van('item/nether_star'), van('item/ender_eye')]
    return {m: [recolor(s, ramps[m]) for s in shapes] for m in ramps}


def sheet(name, current, variants, labels=('A', 'B', 'C')):
    s = 8
    w = (1 + len(variants)) * (16 * s + 16) + 16
    im = Image.new('RGBA', (w, 16 * s + 60), (198, 198, 198, 255))
    d = ImageDraw.Draw(im)
    for i, tex in enumerate([current] + variants):
        x = 16 + i * (16 * s + 16)
        if tex is not None:
            im.alpha_composite(tex.resize((16 * s, 16 * s), Image.NEAREST), (x, 8))
            im.alpha_composite(tex, (x, 16 * s + 16))
            im.alpha_composite(tex.resize((32, 32), Image.NEAREST), (x + 24, 16 * s + 16))
        d.text((x + 64, 16 * s + 36), 'jetzt' if i == 0 else labels[i - 1], fill=(40, 40, 40))
    im.save(os.path.join(OUT, name + '.png'))


def main():
    os.makedirs(os.path.join(OUT, 'png'), exist_ok=True)
    items = {
        'messuhr': (mod('item/velocity_gauge'), gauge()),
        'erzdetektor': (mod('item/detector'), detector()),
        'attraktor': (mod('item/magnet'), attractor()),
        'resonanzstab': (mod('item/amethyst_lens'), rod()),
        'echolot': (mod('item/echo_sounder_00'), echo()),
        'tiefenschiefer_weisheitserz': (mod('block/deepslate_sage_ore'), sage_ore()),
        'weisheitskugel': (mod('item/sage_orb'), sage_orb()),
    }
    for key, variants in books().items():
        items['guide_' + ('mod' if key == 'guide_book' else 'vanilla')] = (mod('item/' + key), variants)
    for m, variants in cores().items():
        items[f'kern_{m}'] = (mod(f'item/{m}_core'), variants)
    for name, (current, variants) in items.items():
        sheet(name, current, variants)
        for label, tex in zip('abc', variants):
            tex.save(os.path.join(OUT, 'png', f'{name}_{label}.png'))
    print(len(items), 'sheets ->', OUT)


if __name__ == '__main__':
    main()
