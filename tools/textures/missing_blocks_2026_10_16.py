"""Simple Blocks textures (N69, 2026-10-16, docs/ai/KONZEPT-MODREF-BLOECKE-2026-10-16.md section 6).

Since W23-3 (owner 23.10.) hushed_deepslate_tiles and sculk_deepslate are Vanilla-derived too (blocks_b23_2026_10_23.py);
this file has no own pixel art left.
Vanilla-derived (built, never committed, AGENTS.md rule 7): the cracked and mossy bricks. Each starts from the Vanilla brick
texture of the client jar and adds (cracked) long cracks that run through several bricks (blocks_b23_2026_10_23.cracked,
W23-3) or (mossy) moss in the joints and a few patches on the faces. Listed in vanilla_derived.MODULE_OUTPUTS['simpleblocks'].

  python3.12 tools/textures/missing_blocks_2026_10_16.py            # writes the own PNGs + both previews
  python3.12 tools/textures/missing_blocks_2026_10_16.py --check    # are the own PNGs current?
Env: SB_CLIENT_JAR (client jar for the derived preview), SB_PREVIEW_DIR (derived preview, never committed; default
/root/previews/claude-missingblocks).
"""
import argparse
import io
import os
import random
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import blocks_b23_2026_10_23 as b23  # noqa: E402

OUT = ROOT / 'modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block'
OWN_PREVIEW = ROOT / 'docs/ai/previews/claude-missingblocks'
DERIVED_PREVIEW = Path(os.environ.get('SB_PREVIEW_DIR', '/root/previews/claude-missingblocks'))

# (new texture, Vanilla brick texture, 'cracked' | 'mossy')
AGED = [
    ('cracked_red_nether_bricks', 'red_nether_bricks', 'cracked'),
    ('cracked_tuff_bricks', 'tuff_bricks', 'cracked'), ('mossy_tuff_bricks', 'tuff_bricks', 'mossy'),
    ('cracked_mud_bricks', 'mud_bricks', 'cracked'), ('mossy_mud_bricks', 'mud_bricks', 'mossy'),
    ('cracked_end_stone_bricks', 'end_stone_bricks', 'cracked'), ('mossy_end_stone_bricks', 'end_stone_bricks', 'mossy'),
    ('mossy_deepslate_bricks', 'deepslate_bricks', 'mossy'), ('mossy_deepslate_tiles', 'deepslate_tiles', 'mossy'),
    ('cracked_cinnabar_bricks', 'cinnabar_bricks', 'cracked'), ('mossy_cinnabar_bricks', 'cinnabar_bricks', 'mossy'),
    ('cracked_sulfur_bricks', 'sulfur_bricks', 'cracked'), ('mossy_sulfur_bricks', 'sulfur_bricks', 'mossy'),
]
# Moss ramp (dark to light): the same in every family, like Vanilla's one green on every mossy block.
MOSS = [(58, 82, 36), (78, 108, 44), (100, 134, 56), (126, 160, 72)]


def own():
    """name -> 16x16 RGBA image (none since W23-3: the sculk blocks moved to blocks_b23_2026_10_23.derived)."""
    return {}


def _lum(p):
    return 0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]


def _scale(p, f):
    return tuple(max(0, min(255, int(round(c * f)))) for c in p[:3]) + (p[3],)


def cracked(src, seed):
    """W23-3 (owner: take the new cracks, deeper and running through like Vanilla): blocks_b23_2026_10_23.cracked."""
    return b23.cracked(src, seed)


MOSS_SEED = 13   # the seed of variant B on the owner's preview page (a-blockstex23b-preview.html)


def mossy(src, seed):
    """W24-1 (owner 23.10. late: moss B accepted): blocks_b23_2026_10_23.mossy variant B - Vanilla's structure (moss along
    the horizontal joints, short drips, the stone beside it a step darker), quieter and toned towards the brick."""
    return b23.mossy(src, 'B', seed=MOSS_SEED)


def mossy_old(src, seed):
    """Before W24-1: moss in the joints (the dark pixels of the brick) and a few small patches on the faces."""
    rng = random.Random(seed)
    im = src.copy()
    px = im.load()
    lums = sorted(_lum(src.getpixel((x, y))) for y in range(16) for x in range(16))
    joint = lums[len(lums) // 3]
    for y in range(16):
        for x in range(16):
            if _lum(src.getpixel((x, y))) <= joint and rng.random() < 0.8:
                px[x, y] = MOSS[rng.choice([0, 1, 1, 2])] + (255,)
    for _ in range(4):  # patches: a seed pixel with 1-3 neighbours
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(2, 4)):
            px[x % 16, y % 16] = MOSS[rng.choice([1, 2, 2, 3])] + (255,)
            x += rng.choice([-1, 0, 1])
            y += rng.choice([-1, 0, 1])
    return im


def derived(jar):
    """Vanilla-derived output (cracked/mossy bricks): never committed, vanilla_derived.py builds it. Path -> image."""
    out = {}
    with zipfile.ZipFile(jar) as z:
        for i, (name, base, kind) in enumerate(AGED):
            src = Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{base}.png'))).convert('RGBA')
            out[f'block/{name}.png'] = (cracked if kind == 'cracked' else mossy)(src, 1000 + i)
    return out


def _sheet(images, scale=8, per_row=4):
    pad = 6
    rows = (len(images) + per_row - 1) // per_row
    sheet = Image.new('RGB', (per_row * (16 * scale + pad) + pad, rows * (16 * scale + pad) + pad), (30, 30, 34))
    for i, im in enumerate(images):
        big = im.convert('RGBA').resize((16 * scale, 16 * scale), Image.NEAREST)
        sheet.paste(big, (pad + (i % per_row) * (16 * scale + pad), pad + (i // per_row) * (16 * scale + pad)), big)
    return sheet


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    a = ap.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    stale = []
    for name, im in own().items():
        buf = io.BytesIO()
        im.save(buf, format='PNG')
        path = OUT / f'{name}.png'
        if a.check:
            if not path.exists() or path.read_bytes() != buf.getvalue():
                stale.append(name)
        else:
            path.write_bytes(buf.getvalue())
    if a.check:
        if stale:
            raise SystemExit(f'missing_blocks: stale textures {stale}; run without --check')
        print('missing_blocks: own textures current')
        return
    if own():
        OWN_PREVIEW.mkdir(parents=True, exist_ok=True)
        _sheet(list(own().values()), per_row=2).save(OWN_PREVIEW / 'own_8x.png')
    jar = os.environ.get('SB_CLIENT_JAR')
    if jar and Path(jar).is_file():
        DERIVED_PREVIEW.mkdir(parents=True, exist_ok=True)
        d = derived(jar)
        # old next to new: the Vanilla brick, then the aged block (Vanilla pixels: never committed)
        pairs = []
        with zipfile.ZipFile(jar) as z:
            for name, base, _ in AGED:
                pairs.append(Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{base}.png'))).convert('RGBA'))
                pairs.append(d[f'block/{name}.png'])
        _sheet(pairs, per_row=6).save(DERIVED_PREVIEW / 'aged_8x.png')
        print('derived preview ->', DERIVED_PREVIEW / 'aged_8x.png')
    print('own textures ->', OUT)


if __name__ == '__main__':
    main()
