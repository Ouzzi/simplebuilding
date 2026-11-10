"""PAL14 (b-pal14, owner 09.11.): textures of the missing palette members (docs/ai/PLAN-B-PAL14-2026-11-09.md).

Vanilla-derived (built from the 26.3 client jar, never committed; vanilla_derived.py OUTPUTS / MODULE_OUTPUTS['simpleblocks']):
  simpleblocks   sandstone_pillar, red_sandstone_pillar (side; the ends show Vanilla's sandstone top),
                 chiseled_ice, polished_/_bricks of ice, packed ice and blue ice (the ice alpha stays: normal ice is see-through)
  simplebuilding ender_purpur_block, astral/nihil/ender_purpur_pillar (+ _top): Vanilla purpur in the colours of the kind
Own art (committed with .layers.json, --own): simplebuilding ender_end_stone = our Nihil End Stone turned to the Ender quartz hue.

Every look is a set of brightness factors over the Vanilla pixels or a palette swap (A8b: few hard steps; the share of hard
transitions is printed in the preview). Rounds (A8c) are kept as parameter sets, ROUND picks the built one:

    python3.12 tools/textures/pal14_2026_11_09.py --preview OUT.html [--jar J]   rounds + before/after rows (answer code PAL14)
    python3.12 tools/textures/pal14_2026_11_09.py --own                           writes ender_end_stone.png + .layers.json
"""
from __future__ import annotations

import argparse
import base64
import colorsys
import io
import os
import sys
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
SB_TEX = ROOT / 'src/main/resources/assets/simplebuilding/textures/block'
DESC = 'pal14_2026_11_09.derived() - Vanilla sprite as a new palette member (b-pal14)'
ICES = ('ice', 'packed_ice', 'blue_ice')
KINDS = ('astral', 'nihil', 'ender')
ROUND = 'R3'

# Round parameters (see the plan, section 8, for the verdicts).
ROUNDS = {
    'R1': dict(flute=(2, 5, 10, 13), flute_dark=0.80, flute_lit=1.00, band=False,
               pol_mix=0.35, pol_rim=(1.0, 1.0), brick_dark=0.72, brick_lit=1.00, mortar_alpha=0, chisel_deep=1.0),
    'R2': dict(flute=(3, 6, 9, 12), flute_dark=0.86, flute_lit=1.06, band=True,
               pol_mix=0.55, pol_rim=(1.08, 0.88), brick_dark=0.82, brick_lit=1.07, mortar_alpha=60, chisel_deep=1.2),
    'R3': dict(flute=(3, 6, 9, 12), flute_dark=0.88, flute_lit=1.05, band=True,
               pol_mix=0.60, pol_rim=(1.07, 0.89), brick_dark=0.84, brick_lit=1.06, mortar_alpha=70, chisel_deep=1.1),
}


# ------------------------------------------------------------------ helpers

def vanilla(jar, sprite: str) -> np.ndarray:
    with zipfile.ZipFile(jar) as z:
        im = Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{sprite}.png'))).convert('RGBA')
    return np.array(im.crop((0, 0, 16, 16)))


def _scale(a: np.ndarray, f: np.ndarray) -> np.ndarray:
    out = a.astype(float)
    out[..., :3] = np.clip(out[..., :3] * f[..., None], 0, 255)
    return np.rint(out).astype(np.uint8)


def hard_share(a: np.ndarray, thr: int = 40) -> float:
    lum = 0.299 * a[..., 0] + 0.587 * a[..., 1] + 0.114 * a[..., 2]
    h = (np.abs(np.diff(lum, axis=0)) > thr).sum() + (np.abs(np.diff(lum, axis=1)) > thr).sum()
    return float(h) / (lum.shape[0] * (lum.shape[1] - 1) * 2)


def _lum(c) -> float:
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


# ------------------------------------------------------------------ sandstone pillars

def pillar_side(cut: np.ndarray, p: dict) -> np.ndarray:
    """Vanilla cut sandstone (bands top and bottom) with vertical fluting in the body between the bands (rows 3..12)."""
    f = np.ones((16, 16))
    body = slice(3, 13) if p['band'] else slice(0, 16)
    for x in p['flute']:
        f[body, x] = p['flute_dark']
        if x + 1 < 16:
            f[body, x + 1] = p['flute_lit']
    return _scale(cut, f)


# ------------------------------------------------------------------ ice members (alpha kept)

def polished(a: np.ndarray, p: dict) -> np.ndarray:
    """Calmer surface (pulled towards the mean colour) with a lit top/left and a shaded bottom/right edge (polished stones)."""
    out = a.astype(float)
    mean = out[..., :3].reshape(-1, 3).mean(axis=0)
    out[..., :3] = out[..., :3] * (1 - p['pol_mix']) + mean * p['pol_mix']
    f = np.ones((16, 16))
    lit, dark = p['pol_rim']
    f[0, :15] = lit
    f[:15, 0] = lit
    f[15, 1:] = dark
    f[1:, 15] = dark
    return _scale(np.rint(out).astype(np.uint8), f)


def brick_mask():
    """Joint grid of 4 courses x 4 px, bricks 8 wide, every other course shifted by 4 (deepslate bricks)."""
    joint = np.zeros((16, 16), bool)
    top = np.zeros((16, 16), bool)
    for c in range(4):
        y0 = c * 4
        joint[y0 + 3, :] = True
        top[y0, :] = True
        shift = 0 if c % 2 == 0 else 4
        for x in (shift + 7, (shift + 15) % 16):
            joint[y0:y0 + 4, x] = True
    top &= ~joint
    return joint, top


def bricks(a: np.ndarray, p: dict) -> np.ndarray:
    joint, top = brick_mask()
    f = np.ones((16, 16))
    f[joint] = p['brick_dark']
    f[top] = p['brick_lit']
    out = _scale(a, f)
    if p['mortar_alpha']:
        alpha = out[..., 3].astype(int)
        alpha[joint] = np.minimum(255, alpha[joint] + p['mortar_alpha'])
        out[..., 3] = alpha.astype(np.uint8)
    return out


# The carved relief of the chiseled packed ice (blocks_b23_2026_10_23.ICE_MOTIF 'packed', B23-T3 B), on normal ice.
MOTIF = ['ffffffffffffffff', 'frrrrrrrrrrrrrsf', 'fr............sf', 'fr.dddddddddd.sf', 'fr.d.l.ll.l.d.sf',
         'fr.d.cl..lc.d.sf', 'fr.dl.cccc.ld.sf', 'fr.d.lcmmcl.d.sf', 'fr.d.lcmmcl.d.sf', 'fr.dl.cccc.ld.sf',
         'fr.d.cl..lc.d.sf', 'fr.d.l.ll.l.d.sf', 'fr.dddddddddd.sf', 'fr............sf', 'fssssssssssssssf', 'ffffffffffffffff']
ROLE = {'f': 0.92, 'r': 1.10, 's': 0.82, '.': 0.90, 'd': 1.06, 'l': 0.86, 'c': 1.08, 'm': 1.16}


def chiseled(a: np.ndarray, p: dict) -> np.ndarray:
    f = np.array([[1 + (ROLE[ch] - 1) * p['chisel_deep'] for ch in row] for row in MOTIF])
    out = _scale(a, f)
    alpha = out[..., 3].astype(int)
    frame = np.array([[ch in 'frs' for ch in row] for row in MOTIF])
    alpha[frame] = np.minimum(255, alpha[frame] + p['mortar_alpha'])
    out[..., 3] = alpha.astype(np.uint8)
    return out


# ------------------------------------------------------------------ purpur of the End kinds

def _colours(a: np.ndarray) -> list:
    return sorted({tuple(int(v) for v in px) for px in a.reshape(-1, 4) if px[3]}, key=_lum)


def ramp_map(src_colours: list, target: list):
    """Luminance-rank map: the i-th darkest source colour -> the target colour at the same quantile (no new colours)."""
    out = {}
    n, m = len(src_colours), len(target)
    for i, c in enumerate(src_colours):
        out[c] = target[min(m - 1, round(i * (m - 1) / max(1, n - 1)))]
    return out


def apply_map(a: np.ndarray, mapping: dict) -> np.ndarray:
    out = a.copy()
    for y in range(16):
        for x in range(16):
            c = tuple(int(v) for v in a[y, x])
            if c[3]:
                out[y, x] = mapping[c]
    return out


def purpur_targets(jar) -> dict:
    """kind -> target colour ramp (sorted by luminance). Astral/Nihil: their purpur blocks (recolour08 recipe), Ender: hue-turned."""
    sys.path.insert(0, str(HERE))
    import recolour08_2026_11_08 as rc
    block = vanilla(jar, 'purpur_block')
    out = {}
    with zipfile.ZipFile(jar) as z:
        for kind in ('astral', 'nihil'):
            e = next(e for e in rc.entries('simplebuilding') if e['path'] == f'block/{kind}_purpur_block.png')
            out[kind] = _colours(np.array(rc.build(e, rc._sprite(z, e['src'])).convert('RGBA')))
    ender = []
    for c in _colours(block):
        h, s, v = colorsys.rgb_to_hsv(*(x / 255 for x in c[:3]))
        r, g, b = colorsys.hsv_to_rgb(0.75, min(1.0, s * 0.95), v * 0.97)
        ender.append((round(r * 255), round(g * 255), round(b * 255), 255))
    out['ender'] = sorted(ender, key=_lum)
    return out


def purpur_set(jar) -> dict:
    """simplebuilding textures: ender_purpur_block + <kind>_purpur_pillar(_top) for the three kinds."""
    sprites = {'block': vanilla(jar, 'purpur_block'), 'pillar': vanilla(jar, 'purpur_pillar_side'), 'top': vanilla(jar, 'purpur_pillar_top')}
    union = sorted(set(_colours(sprites['block'])) | set(_colours(sprites['pillar'])) | set(_colours(sprites['top'])), key=_lum)
    out = {}
    for kind, target in purpur_targets(jar).items():
        mapping = ramp_map(union, target)
        if kind == 'ender':
            out['block/ender_purpur_block.png'] = apply_map(sprites['block'], mapping)
        out[f'block/{kind}_purpur_pillar.png'] = apply_map(sprites['pillar'], mapping)
        out[f'block/{kind}_purpur_pillar_top.png'] = apply_map(sprites['top'], mapping)
    return out


# ------------------------------------------------------------------ build hooks

def module_set(jar, p: dict) -> dict:
    out = {}
    for name, sprite in (('sandstone_pillar', 'cut_sandstone'), ('red_sandstone_pillar', 'cut_red_sandstone')):
        out[f'block/{name}.png'] = pillar_side(vanilla(jar, sprite), p)
    ice = vanilla(jar, 'ice')
    import pal16_2026_11_10 as pal16   # b-pal16 (owner PAL15: "vorher besser"): the b-pal14 crystal back, frame lit from the top, calmer
    out['block/chiseled_ice.png'] = pal16.carve_ice(ice)
    for k in ICES:
        a = vanilla(jar, k)
        out[f'block/polished_{k}.png'] = polished(a, p)
        out[f'block/{k}_bricks.png'] = bricks(a, p)
    return out


def _img(d: dict) -> dict:
    return {k: Image.fromarray(v, 'RGBA') for k, v in d.items()}


def derived(jar) -> dict:
    """simplebuilding namespace (vanilla_derived.OLDER_GENERATORS)."""
    return _img(purpur_set(Path(jar)))


def derived_module(jar, module: str) -> dict:
    """simpleblocks (vanilla_derived.MODULE_GENERATORS)."""
    return _img(module_set(Path(jar), ROUNDS[ROUND])) if module == 'simpleblocks' else {}


# ------------------------------------------------------------------ own art: Ender End Stone

def ender_end_stone() -> Image.Image:
    """Our Nihil End Stone (own art) with every colour turned to the Ender quartz hue 0.75, value and saturation kept."""
    src = Image.open(SB_TEX / 'nihil_end_stone.png').convert('RGBA')
    a = np.array(src)
    out = a.copy()
    for y in range(a.shape[0]):
        for x in range(a.shape[1]):
            r, g, b, al = (int(v) for v in a[y, x])
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            nr, ng, nb = colorsys.hsv_to_rgb(0.75, min(1.0, s * 1.1), v)
            out[y, x] = (round(nr * 255), round(ng * 255), round(nb * 255), al)
    return Image.fromarray(out, 'RGBA')


def write_own() -> None:
    sys.path.insert(0, str(HERE))
    import layers
    img = ender_end_stone()
    layers.snap(img, 'Nihil-Endstein im Enderquarz-Farbton (Grundform)')
    layers.save(img, SB_TEX / 'ender_end_stone.png')
    print('wrote', SB_TEX / 'ender_end_stone.png')


# ------------------------------------------------------------------ preview

def _data(a, k: int = 8) -> str:
    im = a if isinstance(a, Image.Image) else Image.fromarray(a, 'RGBA')
    bg = Image.new('RGBA', im.size, (0, 0, 0, 0))
    buf = io.BytesIO()
    Image.alpha_composite(bg, im.convert('RGBA')).resize((16 * k, 16 * k), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def _fig(a, cap: str, k: int = 6, hard=True) -> str:
    arr = np.array(a) if isinstance(a, Image.Image) else a
    h = f'<br><small>hart {hard_share(arr):.0%}</small>' if hard else ''
    return f'<figure><img src="{_data(a, k)}" alt=""><figcaption>{cap}{h}</figcaption></figure>'


def find_jar(explicit):
    for c in (explicit, os.environ.get('SB_CLIENT_JAR'), Path.home() / 'vanilla263/client.jar',
              Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'):
        if c and Path(c).exists():
            return Path(c)
    sys.exit('no Minecraft client jar found (--jar)')


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--own', action='store_true')
    ap.add_argument('--out')
    ap.add_argument('--jar')
    ap.add_argument('--round', default=ROUND)
    ap.add_argument('--sheet', help='PNG sheet of one round (A8c render check, 8x nearest-neighbour, next to Vanilla)')
    a = ap.parse_args()
    if a.own:
        write_own()
        return
    jar = find_jar(a.jar)
    if a.sheet:
        p = ROUNDS[a.round]
        tiles = [('vanilla cut_sandstone', vanilla(jar, 'cut_sandstone'))] + list(module_set(jar, p).items())[:2] + \
                [('vanilla ice', vanilla(jar, 'ice'))] + list(module_set(jar, p).items())[2:] + \
                [('vanilla purpur_pillar_side', vanilla(jar, 'purpur_pillar_side'))] + list(purpur_set(jar).items()) + \
                [('ender_end_stone', np.array(ender_end_stone())), ('nihil_end_stone', np.array(Image.open(SB_TEX / 'nihil_end_stone.png').convert('RGBA')))]
        S, cols = 136, 6
        sheet = Image.new('RGBA', (cols * S, ((len(tiles) + cols - 1) // cols) * (S + 30)), (110, 110, 110, 255))
        from PIL import ImageDraw
        d = ImageDraw.Draw(sheet)
        for i, (n, t) in enumerate(tiles):
            im = Image.fromarray(t, 'RGBA').resize((128, 128), Image.NEAREST)
            x, y = (i % cols) * S, (i // cols) * (S + 30)
            sheet.alpha_composite(im, (x + 4, y + 4))
            d.text((x + 4, y + 134), n.replace('block/', '').replace('.png', '')[:22], fill='white')
            d.text((x + 4, y + 146), f'hart {hard_share(t):.0%}', fill='yellow')
        sheet.save(a.sheet)
        print('wrote', a.sheet)
        return
    if a.out:
        base = Path(a.out)
        for rel, im in {**derived_module(jar, 'simpleblocks'), **derived(jar)}.items():
            p = base / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            im.save(p)
        print('wrote', base)
        return
    print(__doc__)


if __name__ == '__main__':
    main()
