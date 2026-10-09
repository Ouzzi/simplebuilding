"""Filled Enderite buckets, full and half (owner N21/N28/N29, 2026-10-09).

Full (N29): the liquid fills the bucket opening up to the rim, including the inner top rim row (like the Vanilla
axolotl / lava bucket), and nothing else: no overflow over the rim, no drips on the body.
Half (N28 variant A): the same liquid, only the two topmost outer liquid pixels (4, 3) and (11, 3) show the bucket.

Every pixel outside the opening comes from the empty Enderite bucket of the same frame (enderite_bucket.png,
20 shimmer frames), so full and half buckets keep its animation; the .mcmeta is the empty bucket's. The liquid is a
fixed table per fluid, only liquid tones (Vanilla water/lava bucket tones, soul lava via SOUL_FOR_LAVA): no rim,
crust or corner tones, so the half lava/soul lava bucket reads exactly like the half water bucket.

Replaces the liquid part of crucible_art_v2_2026_10_05.py and the hand-made water bucket of 2026-10-09.

Run from the repository root (Pillow):
  python3.12 tools/textures/enderite_bucket_fill_2026_10_09.py              write full + half buckets + .mcmeta
  python3.12 tools/textures/enderite_bucket_fill_2026_10_09.py --check      compare only
  python3.12 tools/textures/enderite_bucket_fill_2026_10_09.py --preview DIR   12x sheet empty/half/full + inventory size
"""
from __future__ import annotations

import argparse
import io
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ITEM = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures/item'
sys.path.insert(0, str(Path(__file__).resolve().parent))
from soul_lava_2026_10_09 import SOUL_FOR_LAVA  # noqa: E402

# The bucket opening incl. the inner top rim row (row 2); row 1 is the outline and stays.
ROWS = {2: range(5, 11), 3: range(3, 13), 4: range(3, 13), 5: range(5, 11)}
OPENING = {(x, y) for y, xs in ROWS.items() for x in xs}
# Half full: the two topmost outer liquid pixels show the bucket.
OUTER = {(4, 3), (11, 3)}

# Liquid per row (left to right over ROWS): d = dark, m = mid, l = light, h = highlight.
PATTERN = {
    'water': {2: 'mllhlm', 3: 'ddmlhhmmdd', 4: 'dlhhlmhmld', 5: 'dmllmd'},
    'lava': {2: 'dmhmmd', 3: 'ddhhmmhmdd', 4: 'dmhmhhmhmd', 5: 'dmmdmd'},
}
TONES = {
    'water': {'d': (35, 79, 204), 'm': (52, 95, 218), 'l': (68, 111, 233), 'h': (90, 130, 243)},
    'lava': {'d': (204, 70, 40), 'm': (227, 140, 63), 'h': (228, 210, 92)},
}
TONES['lava']['l'] = TONES['lava']['m']
PATTERN['soul_lava'] = PATTERN['lava']
TONES['soul_lava'] = {k: SOUL_FOR_LAVA[v] for k, v in TONES['lava'].items()}
FLUIDS = ('water', 'lava', 'soul_lava')


def frames(image: Image.Image) -> list[Image.Image]:
    return [image.crop((0, 16 * i, 16, 16 * i + 16)) for i in range(image.size[1] // 16)]


def liquid(fluid: str) -> dict[tuple[int, int], tuple[int, int, int, int]]:
    out = {}
    for y, xs in ROWS.items():
        row = PATTERN[fluid][y]
        assert len(row) == len(xs), (fluid, y)
        for x, ch in zip(xs, row):
            out[(x, y)] = TONES[fluid][ch] + (255,)
    return out


def fill(empty: Image.Image, fluid: str, half: bool) -> Image.Image:
    pixels = {p: c for p, c in liquid(fluid).items() if not (half and p in OUTER)}
    out = Image.new('RGBA', empty.size)
    for i, frame in enumerate(frames(empty)):
        frame = frame.copy()
        for p, c in pixels.items():
            frame.putpixel(p, c)
        out.paste(frame, (0, 16 * i))
    return out


def build() -> dict[str, tuple[Image.Image, str]]:
    empty = Image.open(ITEM / 'enderite_bucket.png').convert('RGBA')
    meta = (ITEM / 'enderite_bucket.png.mcmeta').read_text(encoding='utf-8')
    out = {}
    for fluid in FLUIDS:
        name = f'enderite_{fluid}_bucket'
        out[name] = (fill(empty, fluid, False), meta)
        out[f'{name}_half'] = (fill(empty, fluid, True), meta)
    return out


def png_bytes(image: Image.Image) -> bytes:
    buf = io.BytesIO()
    image.save(buf, 'PNG')
    return buf.getvalue()


def preview(built, target: Path) -> Path:
    target.mkdir(parents=True, exist_ok=True)
    empty = frames(Image.open(ITEM / 'enderite_bucket.png').convert('RGBA'))[0]
    s, pad, bg = 12, 8, (139, 139, 139, 255)  # inventory slot grey
    cell = 16 * s + pad
    sheet = Image.new('RGBA', (pad + 3 * cell + 3 * 18 + pad, pad + len(FLUIDS) * cell), bg)
    for row, fluid in enumerate(FLUIDS):
        name = f'enderite_{fluid}_bucket'
        cells = [empty, frames(built[f'{name}_half'][0])[0], frames(built[name][0])[0]]
        for col, cell_img in enumerate(cells):
            sheet.alpha_composite(cell_img.resize((16 * s, 16 * s), Image.NEAREST), (pad + col * cell, pad + row * cell))
            sheet.alpha_composite(cell_img, (pad + 3 * cell + col * 18, pad + row * cell))  # 1x, inventory size
    path = target / 'enderite_buckets_empty_half_full.png'
    sheet.save(path)
    # Animation of all six filled buckets (shimmer), 4x.
    gif = []
    for i in range(len(frames(Image.open(ITEM / "enderite_bucket.png")))):
        canvas = Image.new('RGBA', (6 * 72, 72), bg)
        for k, (name, (img, _)) in enumerate(built.items()):
            canvas.alpha_composite(frames(img)[i].resize((64, 64), Image.NEAREST), (4 + k * 72, 4))
        gif.append(canvas.convert('RGB'))
    gif[0].save(target / 'enderite_buckets_anim.gif', save_all=True, append_images=gif[1:], duration=100, loop=0)
    return path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--preview')
    args = parser.parse_args()
    built = build()
    if args.preview:
        print('preview', preview(built, Path(args.preview)))
        return 0
    bad = []
    for name, (image, meta) in built.items():
        png, mcmeta = ITEM / f'{name}.png', ITEM / f'{name}.png.mcmeta'
        if args.check:
            same = png.exists() and Image.open(png).convert('RGBA').tobytes() == image.tobytes() \
                and mcmeta.exists() and mcmeta.read_text(encoding='utf-8') == meta
            if not same:
                bad.append(name)
        else:
            png.write_bytes(png_bytes(image))
            mcmeta.write_text(meta, encoding='utf-8')
            print('wrote', png.relative_to(ROOT))
    if bad:
        print('out of date:', ', '.join(bad))
        return 1
    if args.check:
        print('enderite filled buckets up to date')
    return 0


if __name__ == '__main__':
    sys.exit(main())
