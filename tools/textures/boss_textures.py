"""WP-19 End bosses (b-boss01, owner 01.11.: Void Runner A1, Brimguard hat M) and the brood hollow items: own pixel art.

    python3 tools/textures/boss_textures.py          # write PNGs + layer sources
    python3 tools/textures/boss_textures.py --check  # fail if the committed PNGs differ
    python3 tools/textures/boss_textures.py --report # hard-transition share per texture (A8b, aim ~7 %, over ~12 % too hard)

Endholz process (PRINZIPIEN A8b): each material is a soft ramp of 8-12 tones; faces get light from the top left
(top light, front mid, sides and back darker, bottom darkest), a vertical falloff and a small hash dither instead of hard
noise; hard edges only where the design needs them (eye slit, blade edge, brim rim). References looked at, never copied:
Vanilla Enderman (violet eyes on black), shulker shell / bedrock (grey grain with pits), Vanilla egg outline.

UV layouts match the Java models:
Void Runner (VoidRunnerModel, 64x64): head (0,0) 5x6x5, body (20,0) 6x11x3, legs (0,16)/(12,16) 3x13x3,
arms (24,16)/(32,16) 2x11x2, blades (40,16)/(48,16) 1x11x3, scarf wrap (0,34) 7x2x4, scarf pieces (24,34)/(38,34)/(0,42) 2x1x5.
Brimguard (BrimguardModel, 256x128): brim (0,0) 34x1x34, crown (140,0) 12x6x12, crown top (190,0) 9x5x9, band (140,20)
13x2x13, rims (140,40)/(140,44) 34x2x1 and (0,60)/(72,60) 1x2x34, body (0,40) 10x8x7, head (36,40) 8x7x8,
legs (70,40)/(88,40) 4x6x4, arms (106,40)/(120,40) 3x7x3.
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import layers  # noqa: E402
import spawn_eggs_2026_10_23 as eggs  # noqa: E402

RES = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/textures'
T = (0, 0, 0, 0)


def ramp(a, b, n):
    """n tones from a (dark) to b (light), slightly curved so the mids stay saturated."""
    out = []
    for i in range(n):
        t = i / (n - 1)
        t = t * t * (3 - 2 * t) * 0.5 + t * 0.5
        out.append(tuple(round(a[k] + (b[k] - a[k]) * t) for k in range(3)))
    return out


# Void Runner A1 (palette from the owner-approved sketch: k 1c1426, d 2c1f40, m 4b3470, p a45cff, e ff4fd8, l e9d5ff)
VIOLET = ramp((0x16, 0x10, 0x20), (0x8a, 0x5c, 0xc8), 10)
SCARF = ramp((0x4a, 0x10, 0x3c), (0xff, 0x7a, 0xe4), 9)
BLADE = ramp((0x6e, 0x6a, 0x86), (0xf2, 0xe8, 0xff), 9)
BOOT = ramp((0x16, 0x16, 0x1c), (0x58, 0x58, 0x66), 8)
EYE = [(0xff, 0x4f, 0xd8), (0xff, 0xb0, 0xf0)]
# Brimguard: bedrock hat (grain with pits, like the Shellker shell), void cloak, violet eyes
BED = ramp((0x16, 0x16, 0x18), (0xa8, 0xa8, 0xab), 12)
CLOAK = ramp((0x12, 0x0e, 0x1a), (0x66, 0x52, 0x80), 9)  # BOSS02: lighter top for Vanilla-like contrast
SKIN = ramp((0x1a, 0x14, 0x22), (0x6c, 0x5a, 0x86), 8)
BAND = ramp((0x3a, 0x12, 0x4e), (0xc0, 0x6c, 0xf0), 9)
GLOW_EYE = [(0xc9, 0x7c, 0xff), (0xf3, 0xe4, 0xff)]


def h(x, y, k=0):
    return eggs.h(x, y, k)


FACE_LIGHT = {'top': 2.2, 'front': 0.6, 'left': -0.4, 'right': 0.2, 'back': -0.8, 'bottom': -2.4}


def faces(u, v, w, hh, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, hh),
            'front': (u + d, v + d, w, hh), 'left': (u + d + w, v + d, d, hh), 'back': (u + 2 * d + w, v + d, w, hh)}


def box(img, u, v, w, hh, d, pal, base, k=0, fall=1.4, grain=0.8, fn=None):
    """Soft-shaded box: face light + vertical falloff + small dither; fn(face, lx, ly, fw, fh) may return a colour or
    a (ramp, index) override for designs (eye, seams)."""
    for name, (x0, y0, fw, fh) in faces(u, v, w, hh, d).items():
        for ly in range(fh):
            for lx in range(fw):
                gx, gy = x0 + lx, y0 + ly
                if gx >= img.width or gy >= img.height:
                    continue
                l = base + FACE_LIGHT[name]
                if name not in ('top', 'bottom') and fh > 1:
                    l += fall * (0.5 - ly / (fh - 1))
                if fw > 2:
                    l += 0.5 * (0.5 - lx / (fw - 1)) * (1 if name in ('front', 'top') else 0.4)
                # BOSS02: calm grain like Vanilla mobs - mostly 2x2 patches, little per-pixel noise
                l += ((h(gx // 2, gy // 2, k) & 7) - 3.5) / 7.0 * grain * 0.6
                l += ((h(gx, gy, k + 1) & 3) - 1.5) / 6.0 * grain * 0.3
                c = pal[max(0, min(len(pal) - 1, int(round(l))))]
                if fn is not None:
                    r = fn(name, lx, ly, fw, fh, gx, gy, l)
                    if r is not None:
                        c = r if isinstance(r[0], int) else r[0][max(0, min(len(r[0]) - 1, int(round(r[1]))))]
                img.putpixel((gx, gy), tuple(c[:3]) + (255,))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def hard_share(im):
    """Share of neighbouring opaque pixel pairs whose luminance differs by more than 40 (same figure as tex29)."""
    hard = total = 0
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            a = px[x, y]
            if not a[3]:
                continue
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < im.width and y + dy < im.height:
                    b = px[x + dx, y + dy]
                    if b[3]:
                        total += 1
                        hard += abs(lum(a) - lum(b)) > 40
    return hard / max(1, total)


# ---------------------------------------------------------------------------------------------------- Void Runner

def runner_head(face, lx, ly, fw, fh, gx, gy, l):
    if face == 'front' and ly == 3 and 0 <= lx <= fw - 1:
        return EYE[0] if lx not in (1, 3) else EYE[1]  # one slit across the face
    if face == 'front' and ly == 4:
        return (VIOLET, l - 1.5)  # shadow under the slit
    if face in ('left', 'right') and ly == 3 and lx in (0, fw - 1):
        return (SCARF, 3)  # slit wraps round a little
    return None


def runner_body(face, lx, ly, fw, fh, gx, gy, l):
    if face in ('front', 'back') and ly in (8, 9):
        return (VIOLET, l - 1.2)  # belt
    if face == 'front' and lx in (2, 3) and 1 <= ly <= 6:
        return (VIOLET, l + 1.0)  # chest ridge
    return None


def runner_leg(face, lx, ly, fw, fh, gx, gy, l):
    if face in ('front', 'back', 'left', 'right') and ly >= fh - 3:
        return (BOOT, l + 1.0)
    if face == 'bottom':
        return (BOOT, 1)
    if face in ('front', 'back', 'left', 'right') and ly == 5:
        return (VIOLET, l + 1.2)  # knee
    return None


def runner_arm(face, lx, ly, fw, fh, gx, gy, l):
    if face in ('front', 'back', 'left', 'right') and ly >= fh - 2:
        return (VIOLET, l - 1.5)  # dark gloves
    return None


def blade(face, lx, ly, fw, fh, gx, gy, l):
    if face in ('front', 'back') and ly == fh - 1:
        return BLADE[-1]  # bright tip
    if face in ('left', 'right') and lx == (fw - 1 if face == 'right' else 0):
        return (BLADE, l + 2.5)  # cutting edge catches the light
    return None


def scarf_piece(face, lx, ly, fw, fh, gx, gy, l):
    if face in ('top', 'bottom') and (ly % 3 == 2):
        return (SCARF, l - 1.5)  # woven bands
    return None


def void_runner():
    img = Image.new('RGBA', (64, 64), T)
    box(img, 0, 16, 3, 13, 3, VIOLET, 3.4, 1)
    box(img, 12, 16, 3, 13, 3, VIOLET, 3.4, 2)
    box(img, 20, 0, 6, 11, 3, VIOLET, 4.2, 3)
    box(img, 0, 0, 5, 6, 5, VIOLET, 4.4, 4)
    box(img, 24, 16, 2, 11, 2, VIOLET, 3.8, 5)
    box(img, 32, 16, 2, 11, 2, VIOLET, 3.8, 6)
    layers.snap(img, 'Grundform')
    box(img, 0, 16, 3, 13, 3, VIOLET, 3.4, 1, fn=runner_leg)
    box(img, 12, 16, 3, 13, 3, VIOLET, 3.4, 2, fn=runner_leg)
    box(img, 20, 0, 6, 11, 3, VIOLET, 4.2, 3, fn=runner_body)
    box(img, 0, 0, 5, 6, 5, VIOLET, 4.4, 4, fn=runner_head)
    box(img, 24, 16, 2, 11, 2, VIOLET, 3.8, 5, fn=runner_arm)
    box(img, 32, 16, 2, 11, 2, VIOLET, 3.8, 6, fn=runner_arm)
    layers.snap(img, 'Details')
    box(img, 40, 16, 1, 11, 3, BLADE, 4.6, 7, grain=0.6, fn=blade)
    box(img, 48, 16, 1, 11, 3, BLADE, 4.6, 8, grain=0.6, fn=blade)
    layers.snap(img, 'Klingen')
    box(img, 0, 34, 7, 2, 4, SCARF, 4.6, 9, fn=scarf_piece)
    for i, (u, v) in enumerate(((24, 34), (38, 34), (0, 42))):
        box(img, u, v, 2, 1, 5, SCARF, 4.8 - i * 0.6, 10 + i, fn=scarf_piece)
    layers.snap(img, 'Schal')
    glow = Image.new('RGBA', (64, 64), T)
    fx, fy, fw, fh = faces(0, 0, 5, 6, 5)['front']
    for lx in range(fw):
        glow.putpixel((fx + lx, fy + 3), (EYE[1] if lx in (1, 3) else EYE[0]) + (255,))
    layers.snap(glow, 'Augenschlitz')
    return img, glow


# ---------------------------------------------------------------------------------------------------- Brimguard

def bedrock(face, lx, ly, fw, fh, gx, gy, l):
    n = h(gx // 2, gy // 2, 77)
    if n < 14:
        return (BED, l - 2.2)  # pits, two pixels wide like Vanilla bedrock (BOSS02: fewer, softer)
    if n > 250:
        return (BED, l + 1.4)  # pale flecks
    return None


def brim(face, lx, ly, fw, fh, gx, gy, l):
    r = bedrock(face, lx, ly, fw, fh, gx, gy, l)
    if face == 'top':
        # radial light: the crown shades the middle, the rim catches light
        cx, cy = (fw - 1) / 2, (fh - 1) / 2
        dist = max(abs(lx - cx), abs(ly - cy)) / cx
        base = l + (dist - 0.6) * 2.0
        if dist < 0.42:
            base -= 1.5  # crown shadow
        return r if r is not None else (BED, base)
    if face == 'bottom':
        return (BED, l - 1.0 + (0 if r is None else -1))  # dark underside
    return r


def crown(face, lx, ly, fw, fh, gx, gy, l):
    return bedrock(face, lx, ly, fw, fh, gx, gy, l)


def band(face, lx, ly, fw, fh, gx, gy, l):
    if face not in ('top', 'bottom') and lx % 4 == 1:
        return (BAND, l + 1.5)  # stitched pattern
    return None


def cloak(face, lx, ly, fw, fh, gx, gy, l):
    if face in ('front', 'back') and ly == fh - 1:
        return (CLOAK, l - 1.5)  # hem
    if face == 'front' and lx == fw // 2:
        return (CLOAK, l - 1.0)  # overlap
    return None


def bhead(face, lx, ly, fw, fh, gx, gy, l):
    if face == 'front' and ly == 5 and lx in (2, 5):
        return GLOW_EYE[0]
    if face == 'front' and ly == 5 and lx in (1, 6):
        return (SKIN, 0.5)
    return None


def brimguard():
    img = Image.new('RGBA', (256, 128), T)
    box(img, 0, 40, 10, 8, 7, CLOAK, 4.0, 21, fn=cloak)
    box(img, 36, 40, 8, 7, 8, SKIN, 3.6, 22)
    box(img, 70, 40, 4, 6, 4, CLOAK, 3.0, 23)
    box(img, 88, 40, 4, 6, 4, CLOAK, 3.0, 24)
    box(img, 106, 40, 3, 7, 3, CLOAK, 3.4, 25)
    box(img, 120, 40, 3, 7, 3, CLOAK, 3.4, 26)
    layers.snap(img, 'Koerper')
    box(img, 36, 40, 8, 7, 8, SKIN, 3.6, 22, fn=bhead)
    layers.snap(img, 'Gesicht')
    box(img, 0, 0, 34, 1, 34, BED, 4.4, 31, grain=0.7, fn=brim)
    for (u, v, w, hh, d, k) in ((140, 40, 34, 2, 1, 32), (140, 44, 34, 2, 1, 33), (0, 60, 1, 2, 34, 34), (72, 60, 1, 2, 34, 35)):
        box(img, u, v, w, hh, d, BED, 5.2, k, grain=0.7, fn=crown)
    box(img, 140, 0, 12, 6, 12, BED, 4.6, 36, grain=0.7, fn=crown)
    box(img, 190, 0, 9, 5, 9, BED, 5.0, 37, grain=0.7, fn=crown)
    layers.snap(img, 'Hut')
    box(img, 140, 20, 13, 2, 13, BAND, 4.6, 38, fn=band)
    layers.snap(img, 'Hutband')
    glow = Image.new('RGBA', (256, 128), T)
    fx, fy, _, _ = faces(36, 40, 8, 7, 8)['front']
    for lx in (2, 5):
        glow.putpixel((fx + lx, fy + 5), GLOW_EYE[1] + (255,))
    layers.snap(glow, 'Augen')
    return img, glow


# ---------------------------------------------------------------------------------------------------- items

def egg_runner():
    """BOSS02: like the Vanilla enderman egg - a violet head with the glowing eye band, the pink scarf wound round the
    neck with its end flying out to the right, the pale blade sticking out at the lower left."""
    mats = {"b": (VIOLET[1:], 4, 0.6), "s": (SCARF[2:], 3, 0.5), "w": (BLADE[2:], 4, 0.3)}
    fixed = {"E": EYE[0], "F": EYE[1]}
    art = ["................",
           "......bbbb......",
           ".....bbbbbb.....",
           "....bbbbbbbb....",
           "...bbbbbbbbbb...",
           "...bEFEbbEFEb...",
           "..bbbbbbbbbbbb..",
           "..ssssssssssss..",
           "..sssssssssssss.",
           "..bbbbbbbbbbssss",
           ".wbbbbbbbbbbb.ss",
           ".wbbbbbbbbbbb...",
           ".wwbbbbbbbbbb...",
           "..w.bbbbbbbb....",
           ".....bbbbbb.....",
           "................"]
    return eggs.grid_egg(art, mats, fixed, "b", seed=41)


def egg_brim():
    """BOSS02: the bedrock hat as on the mob (crown, violet band, brim sticking out on both sides), below it the face in
    the brim's shadow with two violet eyes and the cloak."""
    mats = {"b": (BED[3:], 5, 0.7), "r": (BED[2:], 4, 0.5), "c": (CLOAK[2:], 4, 0.5), "f": (SKIN[1:], 2, 0.3),
            "v": (BAND[2:], 4, 0.3)}
    fixed = {"E": GLOW_EYE[0], "F": GLOW_EYE[1]}
    art = ["................",
           "......bbbb......",
           ".....bbbbbb.....",
           ".....bbbbbb.....",
           ".....vvvvvv.....",
           "rrbbbbbbbbbbbbrr",
           ".rrrrrrrrrrrrrr.",
           "..ffffffffffff..",
           "..ffFEffffEFff..",
           "..cffffffffffc..",
           "..cccccccccccc..",
           "..cccccccccccc..",
           "...cccccccccc...",
           "....cccccccc....",
           ".....cccccc.....",
           "................"]
    return eggs.grid_egg(art, mats, fixed, "c", seed=42)


def essence():
    """A soul-wisp in a small glass vial: cork, glass outline, a swirling violet-white wisp."""
    G = ramp((0x2a, 0x3a, 0x44), (0xd8, 0xf4, 0xff), 8)
    W = ramp((0x3c, 0x16, 0x5a), (0xf6, 0xe6, 0xff), 9)
    C = ramp((0x4a, 0x2e, 0x18), (0xb0, 0x80, 0x50), 6)
    rows = ["................",
            "......cccc......",
            "......cccc......",
            ".....gg..gg.....",
            "......g..g......",
            ".....g....g.....",
            "....g.wwww.g....",
            "...g.wwWWww.g...",
            "...g.wWWWWw.g...",
            "...g.wwWWWw.g...",
            "...g..wwWw..g...",
            "...g...ww...g...",
            "....g......g....",
            ".....gggggg.....",
            "................",
            "................"]
    img = Image.new('RGBA', (16, 16), T)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            l = (7.5 - x) * 0.25 + (8 - y) * 0.2 + ((h(x, y, 51) & 3) - 1.5) * 0.3
            if ch == 'c':
                c = C[max(0, min(5, int(round(3 + l))))]
            elif ch == 'g':
                c = G[max(0, min(7, int(round(4 + l))))]
            elif ch == 'W':
                c = W[max(0, min(8, int(round(7 + l * 0.5))))]
            else:
                c = W[max(0, min(8, int(round(4 + l))))]
            img.putpixel((x, y), c + (255,))
    # glass filling: faint tint inside the vial
    for y in range(4, 13):
        for x in range(4, 12):
            if img.getpixel((x, y))[3] == 0 and 4 <= x <= 11 and rows[y][:x].count('g') % 2 == 1:
                img.putpixel((x, y), G[5] + (110,))
    layers.snap(img, 'Grundform')
    return img


def egg_blank():
    """A plain egg with an ender pearl sheen and an amethyst sprinkle: the egg outline in pale tones."""
    mats = {"b": (ramp((0x6a, 0x64, 0x58), (0xf4, 0xf0, 0xe6), 10), 7, 1.0),
            "p": (ramp((0x0c, 0x30, 0x2c), (0x6e, 0xd8, 0xc0), 8), 4, 0.6),
            "a": (ramp((0x3c, 0x22, 0x5c), (0xd8, 0xa8, 0xff), 8), 4, 0.6)}
    rows = eggs.egg_rows("b")
    rows = eggs.paint(rows, {(6, 4): "p", (7, 4): "p", (6, 5): "p", (9, 8): "a", (10, 9): "a", (5, 10): "a", (8, 12): "p", (9, 12): "p"})
    img, _ = eggs.shade(rows, mats, {}, "b", seed=43)
    return img


# ---------------------------------------------------------------------------------------------------- output

def outputs():
    runner, runner_glow = void_runner()
    brim, brim_glow = brimguard()
    re_img, _ = egg_runner()
    be_img, _ = egg_brim()
    return {
        'entity/void_runner/void_runner.png': runner,
        'entity/void_runner/void_runner_glow.png': runner_glow,
        'entity/brimguard/brimguard.png': brim,
        'entity/brimguard/brimguard_glow.png': brim_glow,
        'item/void_runner_spawn_egg.png': re_img,
        'item/brimguard_spawn_egg.png': be_img,
        'item/mob_essence.png': essence(),
        'item/egg_blank.png': egg_blank(),
    }


def main():
    out = outputs()
    if '--report' in sys.argv:
        for rel, img in out.items():
            print(f'{rel}: hard {hard_share(img) * 100:.1f} %')
        return
    if '--check' in sys.argv:
        bad = []
        for rel, img in out.items():
            p = RES / rel
            if not p.is_file() or Image.open(p).convert('RGBA').tobytes() != img.convert('RGBA').tobytes():
                bad.append(rel)
        if bad:
            print('out of date:', ', '.join(bad))
            sys.exit(1)
        print('boss textures up to date')
        return
    for rel, img in out.items():
        p = RES / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        layers.save(img, p)
        print('wrote', p.relative_to(ROOT))


if __name__ == '__main__':
    main()
