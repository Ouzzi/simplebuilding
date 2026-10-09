"""Half Enderite buckets (owner N21/N28, variant A chosen 2026-10-09): one bucket of two.

Rule: like the full texture, but only the bucket opening holds liquid and its two topmost outer liquid pixels,
(4, 3) and (11, 3), show the empty bucket. The full textures' overflow over the rim (rows 5-6 outside the opening
and the drips on the body) is dropped. Every pixel outside the opening comes from the empty Enderite bucket of the
same frame, so the halves keep its shimmer animation (20 frames, same .mcmeta as the full buckets).

Reads the current full textures (water, lava, and soul lava as written by soul_lava_2026_10_09.py), so run it
again after either changes.

Run from the repository root (Pillow):
  python3.12 tools/textures/enderite_bucket_half_2026_10_09.py              write the three halves + .mcmeta
  python3.12 tools/textures/enderite_bucket_half_2026_10_09.py --check      compare only
  python3.12 tools/textures/enderite_bucket_half_2026_10_09.py --preview DIR   16x sheet empty/half/full
"""
from __future__ import annotations

import argparse
import io
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ITEM = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures/item'
FLUIDS = ('water', 'lava', 'soul_lava')

# The bucket opening (rows 2-5): what the liquid fills before it overflows the rim.
OPENING = ({(x, 2) for x in range(5, 11)} | {(x, 3) for x in range(3, 13)}
           | {(x, 4) for x in range(3, 13)} | {(x, 5) for x in range(5, 11)})
# Half full: the two topmost outer liquid pixels show the bucket.
OUTER = {(4, 3), (11, 3)}


def frames(image: Image.Image) -> list[Image.Image]:
    return [image.crop((0, 16 * i, 16, 16 * i + 16)) for i in range(image.size[1] // 16)]


def half(empty: Image.Image, full: Image.Image) -> Image.Image:
    e, f = frames(empty), frames(full)
    if len(e) != len(f):
        raise SystemExit(f'frame count differs: empty {len(e)}, full {len(f)}')
    out = Image.new('RGBA', empty.size)
    for i, (ef, ff) in enumerate(zip(e, f)):
        frame = ef.copy()
        for p in OPENING - OUTER:
            frame.putpixel(p, ff.getpixel(p))
        out.paste(frame, (0, 16 * i))
    return out


def build() -> dict[str, tuple[Image.Image, str]]:
    empty = Image.open(ITEM / 'enderite_bucket.png').convert('RGBA')
    out = {}
    for fluid in FLUIDS:
        name = f'enderite_{fluid}_bucket'
        meta = (ITEM / f'{name}.png.mcmeta').read_text(encoding='utf-8')
        out[f'{name}_half'] = (half(empty, Image.open(ITEM / f'{name}.png').convert('RGBA')), meta)
    return out


def png_bytes(image: Image.Image) -> bytes:
    buf = io.BytesIO()
    image.save(buf, 'PNG')
    return buf.getvalue()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--preview')
    args = parser.parse_args()
    built = build()
    if args.preview:
        target = Path(args.preview)
        target.mkdir(parents=True, exist_ok=True)
        empty = frames(Image.open(ITEM / 'enderite_bucket.png').convert('RGBA'))
        sheet = Image.new('RGBA', (3 * 16 * 16 + 2 * 8, len(FLUIDS) * 16 * 16 + 2 * 8), (58, 58, 58, 255))
        for row, fluid in enumerate(FLUIDS):
            name = f'enderite_{fluid}_bucket'
            cells = [empty[0], frames(built[f'{name}_half'][0])[0], frames(Image.open(ITEM / f'{name}.png').convert('RGBA'))[0]]
            for col, cell in enumerate(cells):
                sheet.alpha_composite(cell.resize((256, 256), Image.NEAREST), (col * 264, row * 264))
        sheet.save(target / 'enderite_buckets_empty_half_full.png')
        print('preview', target / 'enderite_buckets_empty_half_full.png')
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
        print('enderite half buckets up to date')
    return 0


if __name__ == '__main__':
    sys.exit(main())
