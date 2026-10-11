#!/usr/bin/env python3.12
"""Isometric preview of structure templates (flat-shaded voxels, item frames as a gold marker).
  python3.12 tools/structures/render_preview.py OUT_DIR template.nbt [...]"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw
sys.path.insert(0, str(Path(__file__).parent))
import nbtmini

COLORS = {'purpur_block': (169, 125, 169), 'purpur_pillar': (171, 129, 171), 'purpur_stairs': (160, 116, 160),
          'purpur_slab': (160, 116, 160), 'end_stone': (219, 222, 158), 'end_stone_bricks': (218, 224, 162),
          'obsidian': (21, 18, 30), 'magenta_stained_glass': (178, 76, 216), 'end_rod': (240, 235, 230),
          'chest': (162, 115, 52), 'purple_wool': (122, 42, 173), 'dragon_head': (30, 30, 30)}
PARTIAL = ('stairs', 'slab', 'end_rod')


def load(path):
    d = nbtmini.loads(Path(path).read_bytes())
    pal = [p['id'].split(':')[1] for p in d['palette']]
    vox = {tuple(b['pos']): pal[b['state']] for b in d['blocks'] if pal[b['state']] not in ('air', 'structure_void')}
    frames = [tuple(e['blockPos']) for e in d['entities']]
    return vox, frames


def render(vox, frames, s=10):
    def proj(x, y, z):
        return (x - z) * s, (x + z) * s / 2 - y * s
    pts = [proj(*p) for p in vox] + [proj(p[0] + 1, p[1] + 1, p[2] + 1) for p in vox]
    minx, miny = min(p[0] for p in pts) - 2 * s, min(p[1] for p in pts) - 2 * s
    W, H = int(max(p[0] for p in pts) - minx + 3 * s), int(max(p[1] for p in pts) - miny + 3 * s)
    img = Image.new('RGB', (W, H), (34, 30, 44)); d = ImageDraw.Draw(img)

    def P(x, y, z):
        a, b = proj(x, y, z); return a - minx, b - miny
    for (x, y, z), name in sorted(vox.items(), key=lambda kv: (kv[0][0] + kv[0][2], kv[0][1], kv[0][0])):
        c = COLORS.get(name, (200, 60, 60))
        h = 0.5 if any(t in name for t in PARTIAL) else 1
        top = [P(x, y + h, z), P(x + 1, y + h, z), P(x + 1, y + h, z + 1), P(x, y + h, z + 1)]
        left = [P(x, y, z + 1), P(x + 1, y, z + 1), P(x + 1, y + h, z + 1), P(x, y + h, z + 1)]
        right = [P(x + 1, y, z), P(x + 1, y, z + 1), P(x + 1, y + h, z + 1), P(x + 1, y + h, z)]
        sh = lambda f: tuple(int(v * f) for v in c)
        for poly, f in ((left, .72), (right, .86), (top, 1.0)):
            d.polygon(poly, fill=sh(f), outline=sh(f * .8))
    for (x, y, z) in frames:
        a, b = P(x + .5, y + .5, z + .5); d.ellipse([a - s * .4, b - s * .4, a + s * .4, b + s * .4], fill=(250, 200, 40), outline=(0, 0, 0))
    return img


if __name__ == '__main__':
    out = Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
    for f in sys.argv[2:]:
        vox, frames = load(f)
        mx, mz = max(p[0] for p in vox), max(p[2] for p in vox)
        flip = lambda p: (mx - p[0], p[1], mz - p[2])   # second view from the opposite corner
        a, b = render(vox, frames), render({flip(p): n for p, n in vox.items()}, [flip(p) for p in frames])
        img = Image.new('RGB', (a.width + b.width, max(a.height, b.height)), (34, 30, 44))
        img.paste(a, (0, 0)); img.paste(b, (a.width, 0))
        img.save(out / (Path(f).stem + '.png'))
        print(out / (Path(f).stem + '.png'))
