"""Filled Enderite buckets, full and half (owner N21/N28/N29, round 3 2026-10-09 evening).

Full (N29): the liquid fills the bucket opening up to the rim, including the inner top rim row (like the Vanilla
axolotl / lava bucket), and nothing else: no overflow over the rim, no drips on the body.
Half (owner 2026-10-09 evening: "wie vorher", then "alle halben Eimer wie der halbe Wassereimer"): the half water
bucket of claude-q-ebucket (8c595c074), and every other half bucket with exactly its mask: the inner top rim row
and the two rim corners of row 3 stay Enderite rim (the same purples for every fluid), the two topmost outer liquid
pixels (4, 3) and (11, 3) show the bucket, the liquid fills rows 3..5 in the fluid's own tones. HALF holds that
table. Milk (queue N32, the Enderite bucket takes milk now) uses the water masks in milk whites with a creamy shine.

Every other pixel comes from the empty Enderite bucket of the same frame (enderite_bucket.png, 20 frames, a shine
sweeping over frames 1..8), so the filled buckets keep its animation; the .mcmeta is the empty bucket's. On the
filled buckets the shine runs in the colour of the content (owner 2026-10-09 evening): each shine pixel is the
frame-0 pixel blended towards the fluid's GLINT by how much brighter the empty bucket's shine is there, on the body
as well as over the liquid. The empty bucket keeps its white shine.

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
# Half (water of 8c595c074, its mask for every fluid): '.' keeps the bucket; R/r Enderite rim purples (shared by
# all fluids), e a second water tone.
HALF = {
    'water': {2: 'RRrrrr', 3: 'R.elmmee.R', 4: 'dlhhlmhmld', 5: 'dmllmd'},
    'lava': {2: 'RRrrrr', 3: 'R.hhmmhm.R', 4: 'dmhmhhmhmd', 5: 'dmhdhh'},
}
RIM = {'R': (85, 48, 153), 'r': (115, 74, 191)}
TONES['water']['e'] = (46, 88, 211)
# Milk (queue N32): the water masks in Vanilla's milk whites, same rule for half and full.
TONES['milk'] = {'d': (196, 199, 208), 'm': (222, 224, 230), 'l': (238, 240, 244), 'h': (255, 255, 255), 'e': (209, 212, 220)}
PATTERN['milk'], HALF['milk'] = PATTERN['water'], HALF['water']
PATTERN['soul_lava'], HALF['soul_lava'] = PATTERN['lava'], HALF['lava']
TONES['soul_lava'] = {k: SOUL_FOR_LAVA[v] for k, v in TONES['lava'].items()}
for tones in TONES.values():
    tones.update(RIM)
# Shine colour per content (peak of the sweep; the empty bucket's shine is near white).
GLINT = {'water': (150, 190, 255), 'lava': (255, 222, 110), 'soul_lava': (140, 238, 255), 'milk': (255, 244, 205)}
FLUIDS = ('water', 'lava', 'soul_lava', 'milk')


def frames(image: Image.Image) -> list[Image.Image]:
    return [image.crop((0, 16 * i, 16, 16 * i + 16)) for i in range(image.size[1] // 16)]


def liquid(fluid: str, half: bool) -> dict[tuple[int, int], tuple[int, int, int, int]]:
    out = {}
    table = HALF if half else PATTERN
    for y, xs in ROWS.items():
        row = table[fluid][y]
        assert len(row) == len(xs), (fluid, y)
        for x, ch in zip(xs, row):
            if ch != '.':
                out[(x, y)] = TONES[fluid][ch] + (255,)
    return out


def lum(c) -> float:
    return .299 * c[0] + .587 * c[1] + .114 * c[2]


def fill(empty: Image.Image, fluid: str, half: bool) -> Image.Image:
    pixels = liquid(fluid, half)
    base = frames(empty)[0]
    still = base.copy()
    for p, c in pixels.items():
        still.putpixel(p, c)
    glint = GLINT[fluid]
    out = Image.new('RGBA', empty.size)
    for i, frame in enumerate(frames(empty)):
        img = still.copy()
        for y in range(16):
            for x in range(16):
                shine, calm = frame.getpixel((x, y)), base.getpixel((x, y))
                if shine == calm or not shine[3]:
                    continue
                # Strength of the white shine at this pixel, 0..1; darker shine edges keep the still pixel.
                k = (lum(shine) - lum(calm)) / max(1.0, 245 - lum(calm))
                if k <= 0:
                    if (x, y) not in pixels:
                        img.putpixel((x, y), shine)
                    continue
                k = min(1.0, 1.5 * k)  # a bit stronger, so the tint carries over the purple body
                under = img.getpixel((x, y))
                img.putpixel((x, y), tuple(round(u + (g - u) * k) for u, g in zip(under[:3], glint)) + (255,))
        out.paste(img, (0, 16 * i))
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
        canvas = Image.new('RGBA', (len(built) * 72, 72), bg)
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
