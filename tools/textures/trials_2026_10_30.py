"""Vanilla-derived textures of the Weathered Trials, Evil Omen and Void chain (a-trials31, queue BS27-2/3/5/6, owner picks ABN30-11: spawner B, vault B).

Owner rule (AGENTS.md 7): the pixels come from the Mojang trial spawner / vault / trial key / ominous bottle sprites of the local client jar and are
drawn at build time (vanilla_derived.py -> MODULE_OUTPUTS['simplemobs']), never committed. Only these rules live in the repo.

Method (concept 9.1): the Vanilla raster, cage shape and light distribution stay; the luminance of every pixel is mapped through a colour ramp of the
variant (weathered = verdigris, ominous = turquoise, evil = ember red, void = violet); the warm / coloured pixels of the Vanilla sprite (vault glow, ominous
cyan) go through the glow ramp. Variant B "broken open": one corner of the frame is gone (dark gap with an ember rim), a diagonal crack runs from it,
verdigris speckles sit on the metal. Variant A (patina only, no gap) stays available: B_BROKEN = False draws A.
At most 12 tones per sprite (A8b, 8-12 soft tones), the share of hard transitions is printed by --report.

  python3.12 tools/textures/trials_2026_10_30.py --report [--jar J]
  python3.12 tools/textures/trials_2026_10_30.py --preview OUT.html [--jar J]     (preview only, never committed)"""
from __future__ import annotations

import argparse
import random
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import tex29_2026_10_29 as t  # noqa: E402

B_BROKEN = True          # variant B (owner pick 30.10.); False = A, patina only
TONES = 12

# colour ramps, dark -> light
RAMPS = {
    'weathered': [(30, 56, 48), (54, 102, 84), (80, 146, 116), (112, 180, 144), (156, 206, 168)],
    'ominous': [(28, 56, 66), (48, 98, 110), (70, 142, 154), (104, 184, 194), (150, 218, 224)],
    'evil': [(36, 14, 16), (80, 30, 32), (126, 48, 46), (176, 78, 64), (222, 126, 96)],
    'void': [(18, 12, 36), (42, 28, 80), (72, 50, 124), (108, 82, 170), (162, 136, 214)],
}
GLOWS = {
    'weathered': [(30, 90, 66), (60, 150, 104), (110, 210, 150), (190, 250, 200)],
    'ominous': [(20, 90, 110), (40, 150, 180), (90, 210, 240), (180, 245, 255)],
    'evil': [(120, 20, 18), (190, 40, 30), (240, 90, 50), (255, 170, 110)],
    'void': [(86, 30, 150), (140, 60, 220), (190, 120, 250), (235, 190, 255)],
}
SPAWNER_PARTS = ('bottom', 'side_inactive', 'top_inactive', 'side_active', 'top_active', 'top_ejecting_reward')
SPAWNER_OMINOUS_PARTS = SPAWNER_PARTS[1:]
VAULT_PARTS = ('bottom', 'front_off', 'front_on', 'front_ejecting', 'side_off', 'side_on', 'top', 'top_ejecting')


def ramp_at(ramp, v):
    v = max(0.0, min(1.0, v)) * (len(ramp) - 1)
    i = min(int(v), len(ramp) - 2)
    f = v - i
    return tuple(int(round(ramp[i][c] * (1 - f) + ramp[i + 1][c] * f)) for c in range(3))


def lum(p):
    return (0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]) / 255.0


def sat(p):
    hi, lo = max(p[:3]), min(p[:3])
    return 0.0 if hi == 0 else (hi - lo) / hi


def colour_map(src: Image.Image, kind: str, ominous: bool) -> Image.Image:
    base = RAMPS['ominous' if (ominous and kind == 'weathered') else kind]
    glow = GLOWS['ominous' if (ominous and kind == 'weathered') else kind]
    # normalise the luminance range of the sprite so dark Vanilla metal does not turn black
    pix = [src.getpixel((x, y)) for y in range(src.height) for x in range(src.width) if src.getpixel((x, y))[3]]
    lo, hi = min(lum(p) for p in pix), max(lum(p) for p in pix)
    span = max(0.05, hi - lo)
    out = src.copy()
    for y in range(src.height):
        for x in range(src.width):
            p = src.getpixel((x, y))
            if not p[3]:
                continue
            v = 0.5 * (lum(p) - lo) / span + 0.5 * lum(p)   # half normalised: keeps Vanilla's contrast, no black holes
            coloured = sat(p) > 0.42 and lum(p) > 0.25
            rgb = ramp_at(glow, v) if coloured else ramp_at(base, 0.08 + 0.92 * v)
            out.putpixel((x, y), rgb + (p[3],))
    return out


def speckle(im: Image.Image, kind: str, rng: random.Random, share: float) -> Image.Image:
    """Patina (weathered) / soot (evil) / dust (void) on the metal: a few lighter or darker pixels, never on glow pixels."""
    tint = {'weathered': (96, 168, 128), 'evil': (60, 22, 22), 'void': (86, 64, 140)}[kind]
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] and rng.random() < share:
                out.putpixel((x, y), tuple((p[c] * 3 + tint[c]) // 4 for c in range(3)) + (p[3],))
    return out


def break_corner(im: Image.Image, kind: str, rng: random.Random, see_through: bool) -> Image.Image:
    """Variant B: the upper-right corner is gone (3 + 2 + 1 pixels), an ember rim around the gap and a crack towards the middle."""
    out = im.copy()
    w, h = im.size
    glow = GLOWS[kind][1]
    dark = RAMPS[kind][0]
    gap = [(w - 1, 0), (w - 2, 0), (w - 3, 0), (w - 1, 1), (w - 2, 1), (w - 1, 2)]
    rim = [(w - 4, 0), (w - 3, 1), (w - 2, 2), (w - 1, 3)]
    for x, y in gap:
        out.putpixel((x, y), (0, 0, 0, 0) if see_through else dark + (255,))
    for x, y in rim:
        out.putpixel((x, y), glow + (255,))
    x, y = w - 4, 3                                # the diagonal crack, jittered
    for _ in range(7):
        if 0 <= x < w and 0 <= y < h and out.getpixel((x, y))[3]:
            out.putpixel((x, y), dark + (255,))
        x -= 1 if rng.random() < 0.8 else 0
        y += 1 if rng.random() < 0.85 else 0
    return out


def quant(im: Image.Image, n: int = TONES) -> Image.Image:
    rgb = Image.new('RGB', im.size)
    rgb.paste(im.convert('RGB'))
    q = rgb.quantize(colors=n, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert('RGBA')
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            if im.getpixel((x, y))[3]:
                out.putpixel((x, y), q.getpixel((x, y))[:3] + (im.getpixel((x, y))[3],))
    return out


def block_texture(jar: t.Jar, source: str, kind: str, ominous: bool, seed: str, spawner: bool, side: bool) -> Image.Image:
    rng = random.Random(f'{seed}-{kind}-{ominous}')
    im = colour_map(jar.img('block/' + source), kind, ominous)
    im = speckle(im, 'weathered' if kind == 'weathered' else kind, rng, 0.04 if kind == 'weathered' else 0.03)
    if B_BROKEN and side:
        im = break_corner(im, kind, rng, see_through=spawner)
    return quant(im)


def pips(im: Image.Image, n: int, colour) -> Image.Image:
    out = im.copy()
    for i in range(n):
        out.putpixel((2 + 2 * i, 15), colour + (255,))
    return out


def derived(jar) -> dict:
    jar = jar if isinstance(jar, t.Jar) else t.Jar(jar)
    out = {}
    for kind in ('weathered', 'evil', 'void'):
        for part in SPAWNER_PARTS:
            out[f'block/{kind}_trial_spawner_{part}.png'] = block_texture(jar, f'trial_spawner_{part}', kind, False, 'sp-' + part, True, part.startswith('side'))
        for part in VAULT_PARTS:
            out[f'block/{kind}_vault_{part}.png'] = block_texture(jar, f'vault_{part}', kind, False, 'va-' + part, False, part in ('front_off', 'front_on', 'front_ejecting'))
    for part in SPAWNER_OMINOUS_PARTS:
        out[f'block/weathered_trial_spawner_{part}_ominous.png'] = block_texture(jar, f'trial_spawner_{part}_ominous', 'weathered', True, 'sp-' + part, True, part.startswith('side'))
    for part in VAULT_PARTS:
        out[f'block/weathered_vault_{part}_ominous.png'] = block_texture(jar, f'vault_{part}_ominous', 'weathered', True, 'va-' + part, False, part in ('front_off', 'front_on', 'front_ejecting'))
    for kind in ('evil', 'void'):
        out[f'mob_effect/{kind}_omen.png'] = quant(colour_map(jar.img('mob_effect/bad_omen'), kind, False))
        key = quant(colour_map(jar.img('item/ominous_trial_key'), kind, False))
        bottle = quant(colour_map(jar.img('item/ominous_bottle'), kind, False))
        mark = GLOWS[kind][3]
        for n in range(1, 6):
            out[f'item/{kind}_trial_key_{n}.png'] = pips(key, n, mark)
            out[f'item/{kind}_potion_{n}.png'] = pips(bottle, n, mark)
    return out


def paths() -> list:
    return sorted(derived(t.Jar(None)))


def _find_jar(arg):
    import vanilla_derived
    return vanilla_derived.find_jar(arg)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--jar')
    ap.add_argument('--report', action='store_true')
    ap.add_argument('--preview')
    a = ap.parse_args()
    jar = _find_jar(a.jar)
    if jar is None:
        sys.exit('no client jar (SB_CLIENT_JAR or --jar)')
    imgs = derived(str(jar))
    if a.report:
        for name, im in sorted(imgs.items()):
            print(f'{name:60s} tones {len({im.getpixel((x, y))[:3] for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3]}):3d}  hard {t.smooth_pairs(im) * 100:4.1f} %')
    if a.preview:
        import base64, io
        cells = []
        for name, im in sorted(imgs.items()):
            b = io.BytesIO(); im.resize((im.width * 6, im.height * 6), Image.NEAREST).save(b, 'PNG')
            cells.append(f'<figure><img src="data:image/png;base64,{base64.b64encode(b.getvalue()).decode()}"><figcaption>{name}</figcaption></figure>')
        Path(a.preview).write_text('<meta charset=utf-8><style>body{background:#222;color:#ddd;font:12px sans-serif}figure{display:inline-block;margin:6px}</style>' + ''.join(cells))


if __name__ == '__main__':
    main()
