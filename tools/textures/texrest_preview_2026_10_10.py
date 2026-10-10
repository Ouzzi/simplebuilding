"""Before/after preview sheets for the texture rest batch (claude-q-texrest, 2026-10-10).

Usage (repository root, Pillow):
    python tools/textures/texrest_preview_2026_10_10.py <out.png> <title> <path> [<path> ...]
Each path is drawn twice, 'vorher' from the branch base (BASE) and 'nachher' from the working tree, 8x, on a
checker. Animated strips show their first frame; 64x64 entity sheets are drawn whole at 4x.
"""
import io
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
BASE = '9cb965759'


def font(size):
    for name in ('C:/Windows/Fonts/arial.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            pass
    return ImageFont.load_default(size=size)


def load(path, before):
    try:
        if before:
            data = subprocess.run(['git', 'show', f'{BASE}:{path}'], cwd=ROOT, capture_output=True, check=True).stdout
            img = Image.open(io.BytesIO(data))
        else:
            img = Image.open(ROOT / path)
    except (OSError, subprocess.CalledProcessError):
        return None
    img = img.convert('RGBA')
    if img.height > img.width and img.height % img.width == 0:
        img = img.crop((0, 0, img.width, img.width))
    return img


def tile(img, box):
    k = max(1, box // max(img.size))
    big = img.resize((img.width * k, img.height * k), Image.Resampling.NEAREST)
    bg = Image.new('RGBA', big.size, (55, 60, 69, 255))
    d = ImageDraw.Draw(bg)
    step = max(k, 8)
    for y in range(0, big.height, step):
        for x in range(0, big.width, step):
            if (x // step + y // step) % 2:
                d.rectangle((x, y, x + step - 1, y + step - 1), fill=(65, 70, 80, 255))
    bg.alpha_composite(big)
    return bg


def sheet(out, title, paths, box=128):
    f, small = font(18), font(13)
    cols = 2
    cell_w, cell_h = box + 24, box + 22
    width = 24 + len(paths) * (cell_w * cols + 16)
    rows_per_line = max(1, min(len(paths), 1800 // (cell_w * cols + 16)))
    lines = (len(paths) + rows_per_line - 1) // rows_per_line
    width = 24 + rows_per_line * (cell_w * cols + 16)
    canvas = Image.new('RGB', (width, 60 + lines * (cell_h + 30)), '#292d35')
    d = ImageDraw.Draw(canvas)
    d.text((12, 12), title, font=f, fill='white')
    for i, path in enumerate(paths):
        x0 = 12 + (i % rows_per_line) * (cell_w * cols + 16)
        y0 = 50 + (i // rows_per_line) * (cell_h + 30)
        d.text((x0, y0), Path(path).stem[:34], font=small, fill='#d0d7e2')
        for j, before in enumerate((True, False)):
            img = load(path, before)
            if img is None:
                continue
            canvas.paste(tile(img, box), (x0 + j * cell_w, y0 + 18))
            d.text((x0 + j * cell_w, y0 + 20 + box), 'vorher' if before else 'nachher', font=small, fill='#9aa4b2')
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    canvas.save(out)
    return out


if __name__ == '__main__':
    print(sheet(sys.argv[1], sys.argv[2], sys.argv[3:]))
