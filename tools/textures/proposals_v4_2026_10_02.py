"""Usage: python tools/textures/proposals_v4_2026_10_02.py <vanilla textures dir> <preview dir>

Round 4 of the texture proposals (owner feedback on round 3, 2026-10-02):
- Erzdetektor: between round-3 B (a bit too tall) and C (a bit too flat).
- Attraktor: round-3 C, darker (three steps).
- Amethyst-Resonanzstab: rework between the current one, round-3 A and B, with a thinner grip.
- Echolot: A and B mixed on a cleaner compass frame; B (sonar) also as an animated GIF.
- Eisenstab: vanilla retexture of the copper lightning rod, shown as the 3D item next to the copper one.
- Eisenkern: between the current core and the slim round-3 version.
Writes uebersicht.png, echolot_b.gif and the 16x16 candidates into <preview dir>/png/."""
import math
import os
import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposals_v3_2026_10_02 as v3  # noqa: E402
from proposals_v3_2026_10_02 import AME, IRON, blank, grid, over, px, tex  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
OUT = sys.argv[2] if len(sys.argv) > 2 else 'build/proposals-v4/'
v3.V = V
ROOT = os.path.join(HERE, '..', '..')


# ================================================================== Erzdetektor (between B and C)
def detector_a():
    """Round-3 B one lid row lower: 14 px wide, 11 rows (B: 12, C: 11 but 16 wide)."""
    rows = [
        '................',
        '................',
        '................',
        '.....AAAAAA.....',
        '...AABBCCBBAA...',
        '..ABDGGDDDDEEF..',
        '.ABDGDDDDDDDDHF.',
        '.ABDDDDDDDDDJHF.',
        '.AEEDDDDDDDJHHF.',
        '.AEEEHDJJDHHHkF.',
        '.FgkEEHHHHHHkgF.',
        '..FgkkKKKKkkgF..',
        '...FFkKggKkFF...',
        '.....FFFFFF.....',
        '................',
        '................',
    ]
    return grid(rows, v3.DET), v3.detector_needle(7.5, 7.0, 1.0)


def detector_b():
    """Round-3 C with one more lid row: 16 px wide, 12 rows."""
    rows = [
        '................',
        '................',
        '....AAAAAAAA....',
        '..AABBBCCBBBAA..',
        '.ABDGGDDDDDDEEF.',
        '.ABDGDDDDDDDDHF.',
        'ABDDDDDDDDDDDDHF',
        'ABDDDDDDDDDDDDHF',
        'ABDDDDDDDDDDDJHF',
        'AEEDDDDDDDDDJHHF',
        '.AEEEHHDDHHHHkF.',
        '.FgkkEHHHHHHkgF.',
        '..FFgkKKKKkgFF..',
        '....FFFFFFFF....',
        '................',
        '................',
    ]
    return grid(rows, v3.DET), v3.detector_needle(7.5, 7.0, 1.0)


def detector_c():
    """14 px wide like B, 11 rows like C, the side band two rows (thinner than round-3 B)."""
    rows = [
        '................',
        '................',
        '................',
        '.....AAAAAA.....',
        '...AABBCCBBAA...',
        '..ABDGGDDDDEEF..',
        '.ABDGDDDDDDDDHF.',
        '.ABDDDDDDDDDDHF.',
        '.ABDDDDDDDDDJHF.',
        '.AEEDDDDDDDJHHF.',
        '.FgEEHDJJDHHkgF.',
        '..FgkkKKKKkkgF..',
        '...FFkKggKkFF...',
        '.....FFFFFF.....',
        '................',
        '................',
    ]
    return grid(rows, v3.DET), v3.detector_needle(7.5, 7.0, 1.0)


# ================================================================== Attraktor (round-3 C, darker)
def shade(ramp, f):
    return [tuple(round(c * f) for c in col) for col in ramp]


def attractor(f):
    tips = [(70, 70, 76), (110, 110, 116), (160, 160, 166), (210, 210, 214), (240, 240, 242)]
    return v3.attractor(shade(v3.RED, f), shade(v3.BLUE, f), shade(tips, min(1.0, f + 0.08)),
                        cx=6.6, cy=9.4, r_in=1.3, r_out=5.4, length=5.4, tip_len=2.4)


# ================================================================== Amethyst-Resonanzstab (rework, thin grip)
def thin_shaft(im, redstone=True, wrap=False):
    """Two pixel diagonal shaft like a vanilla tool handle, from the bottom left up to the collar at (8, 7)."""
    for i in range(7):
        x, y = 1 + i, 14 - i
        light, dark = IRON[6], IRON[3]
        if redstone and i == 3:
            light, dark = (255, 90, 74), (170, 20, 20)
        if wrap and i in (0, 1):
            light, dark = IRON[3], IRON[1]
        px(im, x, y, light)
        px(im, x + 1, y, dark)
    px(im, 1, 15, IRON[1])
    px(im, 2, 15, IRON[0])
    return im


def rod_a():
    """Thin grip, an iron collar, round-3 A head: big shard on the axis, a small one beside it."""
    im = grid([
        '.............o..',
        '.....o......o5o.',
        '....o5o....o54o.',
        '....o43o..o543o.',
        '.....o32oo543o..',
        '......o2oG32o...',
        '.......HGGgo....',
        '........Gg......',
    ] + ['................'] * 8, {'o': AME[1], '5': AME[5], '4': AME[4], '3': AME[3], '2': AME[2],
                                  'H': IRON[6], 'G': IRON[4], 'g': IRON[2]})
    return thin_shaft(im)


def rod_b():
    """Thin grip, round-3 B cluster (three shards) in an iron cup, tilted along the shaft."""
    im = grid([
        '...........o....',
        '..........o5o...',
        '.......o..o4o.o.',
        '......o5o.o43o5o',
        '......o43oo32o4o',
        '.......o32o32o3o',
        '.......HGHGGgoo.',
        '........Gg......',
    ] + ['................'] * 8, {'o': AME[1], '5': AME[5], '4': AME[4], '3': AME[3], '2': AME[2],
                                  'H': IRON[6], 'G': IRON[4], 'g': IRON[2]})
    return thin_shaft(im)


def rod_c():
    """Mix of A and B: one long shard on the axis flanked by two short ones, a wrapped grip end."""
    im = grid([
        '..............o.',
        '.............o5o',
        '.......o....o54o',
        '......o5o..o543o',
        '......o43oo5432o',
        '.......o32o432o.',
        '.......HGG32o3o.',
        '........Ggo.oo..',
    ] + ['................'] * 8, {'o': AME[1], '5': AME[5], '4': AME[4], '3': AME[3], '2': AME[2],
                                  'H': IRON[6], 'G': IRON[4], 'g': IRON[2]})
    return thin_shaft(im, wrap=True)


# ================================================================== Echolot (clean frame, A + B)
E = dict(v3.ECHO)
FRAME = [
    '................',
    '.....oooooo.....',
    '...ooLWLLMmoo...',
    '..oLMddddddmDO..',
    '.oLdFFFFFFFFdDO.',
    'oMdFFFFFFFFFFdDO',
    'oMdFFFFFFFFFFdDO',
    'omdFFFFFFFFFFdDO',
    'omDdFFFFFFFFdDDO',
    'OmDDddFFFFddDDkO',
    '.OLWmDddddDDmkO.',
    '..OgkMmmmmDkgO..',
    '...OOkgGGgkOO...',
    '.....OOOOOO.....',
    '................',
    '................',
]
GLASS = [(x, y) for y, row in enumerate(FRAME) for x, ch in enumerate(row) if ch == 'F']
CX, CY = 7.5, 6.5


def _ring():
    """Glass pixels touching the rim, ordered clockwise from the top (the outer echo ring)."""
    gs = set(GLASS)
    edge = [p for p in GLASS if any((p[0] + dx, p[1] + dy) not in gs for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
    return sorted(edge, key=lambda p: math.atan2(p[0] + 0.5 - CX, -(p[1] + 0.5 - CY) * 1.6) % (2 * math.pi))


RING = _ring()
INNER = [(7, 5), (8, 5), (9, 6), (8, 7), (7, 7), (6, 6)]


def echo_body(rings=True, pulse=0):
    im = grid(FRAME, E)
    if rings:
        for p in RING:
            px(im, p[0], p[1], E['f'])
        if pulse in (0, 1):
            for p in INNER:
                px(im, p[0], p[1], E['n'] if pulse == 0 else E['s'])
    return im


def echo_a():
    """Clean frame, faint echo rings behind the needle (round-3 A's needle, B's rings)."""
    im = echo_body(rings=True, pulse=3)
    for x, y, c in ((8, 4, E['t']), (8, 5, E['G']), (8, 6, E['M']), (8, 7, E['n'])):
        px(im, x, y, c)
    return im


def echo_b_frame(direction, tick):
    """Sonar: the bright arc on the outer ring points the way (direction 0..31, 0 = up, clockwise);
    the inner ring pulses outwards (tick)."""
    im = echo_body(rings=True, pulse=tick % 4)
    n = len(RING)
    i = round(direction / 32 * n) % n
    px(im, *RING[(i - 1) % n], E['G'])
    px(im, *RING[i], E['t'])
    px(im, *RING[(i + 1) % n], E['G'])
    px(im, 7, 6, E['g'] if tick % 4 == 0 else E['s'])
    px(im, 8, 6, E['g'] if tick % 4 == 0 else E['s'])
    return im


def echo_b():
    return echo_b_frame(4, 0)


def echo_c():
    """B without the inner ring, the arc three pixels long - the calmest version."""
    im = echo_body(rings=True, pulse=3)
    for k in (30, 0, 2):
        px(im, *RING[round(k / 32 * len(RING)) % len(RING)], E['G'] if k else E['t'])
    return im


def echo_gif(path):
    frames = []
    for f in range(64):
        img = echo_b_frame((f // 2) % 32, f).resize((128, 128), Image.NEAREST)
        bg = Image.new('RGBA', img.size, (139, 139, 139, 255))
        bg.alpha_composite(img)
        frames.append(bg.convert('P', palette=Image.ADAPTIVE))
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=100, loop=0)


# ================================================================== Eisenstab (3D render)
def rod_render(model, texture=None):
    sys.path.insert(0, os.path.join(ROOT, 'wiki'))
    import model_render  # noqa: E402
    if not model_render.AVAILABLE:
        return None
    assets = Path(ROOT).resolve() / 'mc26_3' / 'overlay' / 'resources' / 'assets' / 'simplebuilding'
    jar = Path.home() / '.gradle' / 'caches' / 'fabric-loom' / '26.3' / 'minecraft-client.jar'
    r = model_render.IconRenderer({'simplebuilding': [assets]}, jar)
    image = r.render_block_model(model)
    return image.resize((96, 96), Image.LANCZOS) if image else None


# ================================================================== Eisenkern (between current and slim)
def core_map(core, shift):
    """shift(x, y) -> (sx, sy): where each target pixel takes its colour from; the middle (|dx|+|dy| <= 3) stays."""
    out = blank()
    for y in range(16):
        for x in range(16):
            sx, sy = shift(x, y)
            if 0 <= sx < 16 and 0 <= sy < 16:
                p = core.getpixel((sx, sy))
                if p[3]:
                    out.putpixel((x, y), p)
    for y in range(16):
        for x in range(16):
            if abs(x - 8) + abs(y - 8) <= 3:
                p = core.getpixel((x, y))
                if p[3]:
                    out.putpixel((x, y), p)
    return out


def _in(v, c=8):
    return v - 1 if v < c else v + 1 if v > c else v


def core_a(core):
    """The current core with every point one pixel shorter (the outermost tip pixels gone), body unchanged."""
    out = core.copy()
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x - 8), abs(y - 8)
            if dx + dy >= 10 or (min(dx, dy) == 0 and max(dx, dy) >= 6):
                out.putpixel((x, y), (0, 0, 0, 0))
    return out


def core_b(core):
    """Outer ring one pixel in (everything 5+ pixels from the middle), the body as thick as now."""
    def shift(x, y):
        return (_in(x) if abs(x - 8) >= 5 else x), (_in(y) if abs(y - 8) >= 5 else y)
    return core_map(core, shift)


def core_c(core):
    """Between B and the slim round-3 core: everything 4+ pixels from the middle one pixel in."""
    def shift(x, y):
        return (_in(x) if abs(x - 8) >= 4 else x), (_in(y) if abs(y - 8) >= 4 else y)
    return core_map(core, shift)


def main():
    os.makedirs(os.path.join(OUT, 'png'), exist_ok=True)
    src_core = Image.open(os.path.join(ROOT, 'src/main/resources/assets/simplebuilding/textures/item/iron_core.png')).convert('RGBA').crop((0, 0, 16, 16))
    cur_det = over(tex('item/detector_dial'), tex('item/detector_needle_16'))
    rows = [
        ('Erzdetektor', [cur_det] + [over(d, n) for d, n in (detector_a(), detector_b(), detector_c())]),
        ('Attraktor (C dunkler)', [v3.attractor_c(), attractor(0.88), attractor(0.78), attractor(0.68)]),
        ('Amethyst-Resonanzstab', [tex('item/amethyst_lens'), rod_a(), rod_b(), rod_c()]),
        ('Echolot', [tex('item/echo_sounder_16'), echo_a(), echo_b(), echo_c()]),
        ('Eisenkern', [src_core, core_a(src_core), core_b(src_core), core_c(src_core)]),
    ]
    s, cell, left = 6, 16 * 6 + 10, 200
    sheet = Image.new('RGBA', (left + 4 * cell + 10, (len(rows) + 1) * (cell + 40) + 30), (139, 139, 139, 255))
    d = ImageDraw.Draw(sheet)
    for i, h in enumerate(['jetzt', 'A', 'B', 'C']):
        d.text((left + i * cell + 40, 8), h, fill=(255, 255, 255))
    for r, (label, variants) in enumerate(rows):
        y = 26 + r * (cell + 40)
        d.text((6, y + 40), label, fill=(255, 255, 255))
        for i, t in enumerate(variants):
            x = left + i * cell
            sheet.alpha_composite(t.resize((16 * s, 16 * s), Image.NEAREST), (x, y))
            d.rectangle((x, y + 16 * s + 4, x + 17, y + 16 * s + 21), fill=(139, 139, 139), outline=(55, 55, 55))
            sheet.alpha_composite(t, (x + 1, y + 16 * s + 5))
            sheet.alpha_composite(t.resize((32, 32), Image.NEAREST), (x + 24, y + 16 * s + 4))
        for tag, t in zip('abc', variants[1:]):
            t.save(os.path.join(OUT, 'png', f'{label.split(" ")[0].lower()}_{tag}.png'))
    y = 26 + len(rows) * (cell + 40)
    d.text((6, y + 40), 'Eisenstab (3D)', fill=(255, 255, 255))
    d.text((left, y - 2), 'Kupfer (Vanilla)', fill=(255, 255, 255))
    d.text((left + cell, y - 2), 'Eisen', fill=(255, 255, 255))
    for i, model in enumerate(('minecraft:block/lightning_rod', 'simplebuilding:block/iron_rod')):
        img = rod_render(model)
        if img:
            sheet.alpha_composite(img, (left + i * cell, y + 10))
    sheet.alpha_composite(v3.iron_rod_textures().resize((96, 96), Image.NEAREST), (left + 2 * cell, y + 10))
    d.text((left + 2 * cell, y - 2), 'Textur', fill=(255, 255, 255))
    sheet.save(os.path.join(OUT, 'uebersicht.png'))
    echo_gif(os.path.join(OUT, 'echolot_b.gif'))
    print('ok ->', OUT)


if __name__ == '__main__':
    main()
