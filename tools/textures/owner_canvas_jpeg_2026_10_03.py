"""Usage: python tools/textures/owner_canvas_jpeg_2026_10_03.py <screenshot.jpg> <out.png> <x0 y0 x1 y1 approx canvas box>

Like owner_canvas_2026_10_03.py, for JPEG crops of the owner's Resprite canvas: the dark canvas frame is found by a
tolerant colour test, each cell's middle is sampled by its median, cells that are a neutral grey near the
checkerboard's 128/192 become transparent, and the remaining colours are clustered (any two closer than 14 in RGB
are merged into their pixel-count-weighted mean) so that tones the owner painted identically are identical again
despite the JPEG artefacts. Prints the frame, cell size and the final palette."""
import statistics
import sys

from PIL import Image


def near(c, ref, tol):
    return all(abs(c[i] - ref[i]) <= tol for i in range(3))


def is_frame(c):
    return near(c, (34, 34, 34), 12)


def inner_edge(im, start, step, fixed, axis):
    pos, seen = start, False
    for _ in range(200):
        c = im.getpixel((pos, fixed) if axis == 'x' else (fixed, pos))
        if is_frame(c):
            seen = True
        elif seen:
            return pos
        pos += step
    raise SystemExit(f'frame not found on {axis} from {start}')


def transparent(c):
    neutral = max(c) - min(c) <= 10
    return neutral and (abs(sum(c) / 3 - 128) <= 14 or abs(sum(c) / 3 - 192) <= 14)


def cluster(colours, weights, dist=14):
    groups = [[c, w] for c, w in zip(colours, weights)]
    merged = True
    while merged:
        merged = False
        for i in range(len(groups)):
            for j in range(i + 1, len(groups)):
                a, b = groups[i], groups[j]
                if sum((a[0][k] - b[0][k]) ** 2 for k in range(3)) ** 0.5 < dist:
                    w = a[1] + b[1]
                    a[0] = tuple(round((a[0][k] * a[1] + b[0][k] * b[1]) / w) for k in range(3))
                    a[1] = w
                    groups.pop(j)
                    merged = True
                    break
            if merged:
                break
    return [g[0] for g in groups]


def reconstruct(path, box):
    im = Image.open(path).convert('RGB')
    x0, y0, x1, y1 = box
    ym, xm = (y0 + y1) // 2, (x0 + x1) // 2
    left = inner_edge(im, max(0, x0 - 15), 1, ym, 'x')
    right = inner_edge(im, min(im.width - 1, x1 + 15), -1, ym, 'x')
    top = inner_edge(im, max(0, y0 - 15), 1, xm, 'y')
    bottom = inner_edge(im, min(im.height - 1, y1 + 15), -1, xm, 'y')
    cw, ch = (right - left + 1) / 16, (bottom - top + 1) / 16
    print(f'{path}: inner {left},{top} .. {right},{bottom}, cell {cw:.2f} x {ch:.2f}')
    cells = {}
    for gy in range(16):
        for gx in range(16):
            xs = range(int(left + (gx + 0.3) * cw), int(left + (gx + 0.7) * cw))
            ys = range(int(top + (gy + 0.3) * ch), int(top + (gy + 0.7) * ch))
            px = [im.getpixel((x, y)) for x in xs for y in ys]
            c = tuple(int(statistics.median(p[i] for p in px)) for i in range(3))
            if not transparent(c):
                cells[(gx, gy)] = c
    distinct = sorted(set(cells.values()))
    weights = [sum(1 for v in cells.values() if v == c) for c in distinct]
    palette = cluster(distinct, weights)
    nearest = lambda c: min(palette, key=lambda p: sum((p[k] - c[k]) ** 2 for k in range(3)))
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for p, c in cells.items():
        out.putpixel(p, nearest(c) + (255,))
    print(f'{len(distinct)} sampled colours -> {len(palette)} after clustering')
    return out, im.crop((left, top, right + 1, bottom + 1))


if __name__ == '__main__':
    tex, crop = reconstruct(sys.argv[1], tuple(int(v) for v in sys.argv[3:7]))
    tex.save(sys.argv[2])
    crop.save(sys.argv[2].replace('.png', '-crop.png'))
