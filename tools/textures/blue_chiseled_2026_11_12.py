"""PLAN40 simpleblocks-3 (a-w1d, 12.11.2026): Chiseled Blue Nether Bricks - the accepted PAL16 flame of the chiseled red nether bricks
(owner PAL16 "red_nether_bricks ok", kept unchanged by pal20) carved into OUR OWN blue nether bricks.

Own pixels only (PRINZIPIEN A8): the source is the committed own-art blue_nether_bricks.png (warped_wart_2026_10_16.bricks()), the role
map is pal16's flame (lit from the top, exact left = right), the colours are the blue bricks' own palette plus darker shades of it
(every pixel is quantised to that ramp: no blend colours, no Mojang pixel). Committed with its .layers.json (A8 b): bricks, frame, flame.

How (per pixel): the bricks are mirrored left/right (the brick grain reads symmetric, like the Vanilla chiseled sprites), their contrast
is pulled toward the median (calm: inside the frame / on the outer ring), then every pixel is scaled by its role factor (pal16 R5 factors,
deepened per group: frame, relief of the flame, sunk field) and snapped to the nearest ramp colour.

Rounds (A8c/A8d; texrate.py light, verdicts in docs/ai/PLAN-A-W1D-2026-11-12.md, section "simpleblocks-3 (Unteragent)"):
  R1 the red recipe unchanged (deep 3.6, calm 0.25, no ramp)   orange: 20 % hard, 73 colours (blend), frame far too dark
  R2 deep 2.0, snapped to the 16 brick colours                 orange: flat 20-px field/bottom ring, flame dull
  R3 deep 2.0, calm 0.35, ramp + 2 darker shades               green but the flame barely reads in the block
  R4 per group: frame 1.6, flame 3.2, field 1.2, all mirrored  green 7 % hard; frame too weak in a wall of blue bricks
  R5 frame 2.4 (built)                                         green 10 % hard, flame and frame read at 16 px and in the wall
  R6 frame 2.8 + one more shade                                orange: 15 % hard (frame edge jumps), rejected

    python3.12 tools/textures/blue_chiseled_2026_11_12.py              write PNG + .layers.json
    python3.12 tools/textures/blue_chiseled_2026_11_12.py --check      committed PNG = generator, layer source present
    python3.12 tools/textures/blue_chiseled_2026_11_12.py --rounds DIR every round (1x, 16x), wall context, metrics; with --jar also the
                                                                       Vanilla-derived red reference (only into a DIR outside the repo)
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402
import pal16_2026_11_10 as p16  # noqa: E402

ROOT = HERE.parents[1]
TEX = ROOT / 'modules/simpleblocks/shared/resources/assets/simpleblocks/textures/block'
SRC = TEX / 'blue_nether_bricks.png'
OUT = TEX / 'chiseled_blue_nether_bricks.png'
MOTIF = 'flame'                      # pal16.MOTIFS: the chiseled red nether bricks' fire
F = p16.ROUNDS['R5']['f']            # role factors of the accepted PAL16 round
FRAME, RELIEF = set('AFZRVS'), set('HMECKL')
# deep = (frame, relief, field); mirror 'inner' = inside the frame only (pal16), 'all' = the outer ring too; shades None = no ramp
ROUNDS = {
    'R1': dict(mirror='inner', calm_in=0.25, calm_frame=1.0, deep=(3.6, 3.6, 3.6), shades=None),
    'R2': dict(mirror='inner', calm_in=0.25, calm_frame=1.0, deep=(2.0, 2.0, 2.0), shades=(1.0,)),
    'R3': dict(mirror='inner', calm_in=0.35, calm_frame=1.0, deep=(2.0, 2.0, 2.0), shades=(1.0, 0.8, 0.65)),
    'R4': dict(mirror='all', calm_in=0.3, calm_frame=0.6, deep=(1.6, 3.2, 1.2), shades=(1.0, 0.8, 0.65)),
    'R5': dict(mirror='all', calm_in=0.3, calm_frame=0.6, deep=(2.4, 3.2, 1.2), shades=(1.0, 0.8, 0.65)),
    'R6': dict(mirror='all', calm_in=0.3, calm_frame=0.6, deep=(2.8, 3.4, 1.2), shades=(1.0, 0.8, 0.65, 0.5)),
}
ROUND = 'R5'
# Layer steps (A8 b): which role groups are carved in each step.
STEPS = (('Blaue Netherziegel (gespiegelt, beruhigt)', ()), ('Rahmen', ('frame',)), ('Flamme', ('frame', 'relief', 'field')))


def source() -> np.ndarray:
    return np.array(Image.open(SRC).convert('RGBA'))


def ramp(src: np.ndarray, shades) -> np.ndarray:
    """The bricks' own colours and darker shades of them (RGB)."""
    cols = {tuple(int(v) for v in c[:3]) for c in src.reshape(-1, 4)}
    return np.array(sorted({tuple(int(round(v * k)) for v in c) for c in cols for k in shades}), float)


def carve(src: np.ndarray, p: dict, groups=('frame', 'relief', 'field')) -> np.ndarray:
    grid = p16.roles(MOTIF, 1, 'R5')
    ring0 = np.array([[ch in 'AFZ' for ch in row] for row in grid])
    a = src.astype(float)
    m = a.copy()
    m[:, 8:] = a[:, 7::-1]
    a = np.where((~ring0 if p['mirror'] == 'inner' else np.ones_like(ring0))[..., None], m, a)
    med = np.median(a[..., :3].reshape(-1, 3), axis=0)
    out = a.copy()
    df, dr, dd = p['deep']
    for y in range(16):
        for x in range(16):
            ch = grid[y][x]
            k = p['calm_frame'] if ring0[y, x] else p['calm_in']
            c = med + (a[y, x, :3] - med) * k if k < 1 else a[y, x, :3]
            group = 'frame' if ch in FRAME else 'relief' if ch in RELIEF else 'field'
            deep = {'frame': df, 'relief': dr, 'field': dd}[group] if group in groups else 0.0
            out[y, x, :3] = np.clip(c * (1 + (F[ch] - 1) * deep), 0, 255)
    if p['shades'] is None:
        return np.rint(out).astype(np.uint8)
    r = ramp(src, p['shades'])
    d = ((out[..., None, :3] - r[None, None]) ** 2).sum(-1)
    out[..., :3] = r[d.argmin(-1)]
    return np.rint(out).astype(np.uint8)


def build(rnd: str = ROUND) -> Image.Image:
    src, p = source(), ROUNDS[rnd]
    img = Image.fromarray(carve(src, p))
    for name, groups in STEPS:
        layers.snap(img, name, Image.fromarray(carve(src, p, groups)).tobytes())
    return img


def metrics(img: Image.Image) -> dict:
    import io
    import texrate
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    buf.seek(0)
    m = texrate.metrics(buf)
    light, why = texrate.rate(m)
    return {**{k: m[k] for k in ('big', 'ge6', 'top', 'hard', 'cols')}, 'light': light, 'why': why, 'mirror': round(float(p16.symmetry(img)), 2)}


def rounds(out_dir: Path, jar: str | None) -> None:
    out_dir = out_dir.resolve()
    out_dir.mkdir(parents=True, exist_ok=True)
    blue = Image.open(SRC).convert('RGBA')
    tiles = {}
    for rnd in ROUNDS:
        img = Image.fromarray(carve(source(), ROUNDS[rnd]))
        img.save(out_dir / f'{rnd}_chiseled_blue_nether_bricks.png')
        img.resize((256, 256), Image.NEAREST).save(out_dir / f'{rnd}_chiseled_blue_nether_bricks_x16.png')
        tiles[rnd] = img
        print(rnd, metrics(img))
    refs = {'blue_nether_bricks': blue}
    if jar and ROOT not in out_dir.parents and out_dir != ROOT:   # Vanilla-derived pixels never into the repo
        refs['chiseled_red_nether_bricks (Vanilla-abgeleitet, nur Vorschau)'] = Image.fromarray(
            p16.carve_vanilla(p16.p15.vanilla(jar, 'red_nether_bricks'), MOTIF, 'red_nether_bricks', 'R5'))
    for name, img in refs.items():
        print('Referenz', name, metrics(img))

    def wall(c: Image.Image) -> Image.Image:   # block context: chiseled between blue bricks, 3 x 2 blocks, 6x
        w = Image.new('RGBA', (48, 32))
        for i in range(3):
            w.paste(blue, (i * 16, 0))
            w.paste(c, (i * 16, 16))
        w.paste(c, (16, 0))
        w.paste(blue, (16, 16))
        return w.resize((288, 192), Image.NEAREST)
    row = [wall(t) for t in tiles.values()]
    sheet = Image.new('RGBA', (len(row) * 296, 192), (200, 200, 200, 255))
    for i, im in enumerate(row):
        sheet.paste(im, (i * 296, 0))
    sheet.save(out_dir / 'rounds_wall_context.png')
    cards = list(refs.values()) + list(tiles.values())
    cmp_ = Image.new('RGBA', (len(cards) * 264, 256), (200, 200, 200, 255))
    for i, im in enumerate(cards):
        cmp_.paste(im.resize((256, 256), Image.NEAREST), (i * 264, 0))
    cmp_.save(out_dir / 'rounds_compare_x16.png')
    print(f'rounds written to {out_dir} (order: {", ".join(list(refs) + list(tiles))})')


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--rounds', type=Path)
    ap.add_argument('--jar', default=str(Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'))
    a = ap.parse_args()
    if a.rounds:
        rounds(a.rounds, a.jar if Path(a.jar).exists() else None)
        return 0
    img = build()
    if a.check:
        have = Image.open(OUT).convert('RGBA') if OUT.exists() else None
        if have is None or have.tobytes() != img.convert('RGBA').tobytes() or not layers.layers_path(OUT).exists():
            print('blue_chiseled: chiseled_blue_nether_bricks.png differs from the generator (run tools/textures/blue_chiseled_2026_11_12.py)')
            return 1
        print('blue_chiseled: chiseled_blue_nether_bricks.png current')
        return 0
    layers.save(img, OUT)
    print(f'blue_chiseled: wrote {OUT.relative_to(ROOT)} + layers ({ROUND}: {metrics(img)})')
    return 0


if __name__ == '__main__':
    sys.exit(main())
