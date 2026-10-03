"""Usage: python tools/textures/owner_canvas_2026_10_03.py <screenshot.png> <out.png> [approx x0 y0 x1 y1 in screenshot px]

Reconstructs a 16x16 texture from a screenshot of the owner's Resprite canvas (iPad): finds the dark canvas frame
(rgb 34,34,34) around the given approximate box, divides the inner area into 16 x 16 cells and takes the median colour
of each cell's middle; the checkerboard behind the drawing (neutral greys 192/128) becomes transparent. Prints the
cell size so a skewed or wrongly found frame shows up."""
import statistics
import sys

from PIL import Image

FRAME = (34, 34, 34)


def near(c, ref, tol=6):
    return all(abs(c[i] - ref[i]) <= tol for i in range(3))


def inner_edge(im, start, step, fixed, axis):
    """Walk from `start` (outside the frame) in `step` until past the frame; return the first inner coordinate."""
    pos, seen = start, False
    for _ in range(200):
        c = im.getpixel((pos, fixed) if axis == 'x' else (fixed, pos))
        if near(c, FRAME):
            seen = True
        elif seen:
            return pos
        pos += step
    raise SystemExit(f'frame not found on {axis} from {start}')


def reconstruct(path, box):
    im = Image.open(path).convert('RGB')
    x0, y0, x1, y1 = box
    ym, xm = (y0 + y1) // 2, (x0 + x1) // 2
    left = inner_edge(im, x0 - 20, 1, ym, 'x')
    right = inner_edge(im, x1 + 20, -1, ym, 'x')
    top = inner_edge(im, y0 - 20, 1, xm, 'y')
    bottom = inner_edge(im, y1 + 20, -1, xm, 'y')
    cw, ch = (right - left + 1) / 16, (bottom - top + 1) / 16
    print(f'{path}: inner {left},{top} .. {right},{bottom}, cell {cw:.2f} x {ch:.2f}')
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for gy in range(16):
        for gx in range(16):
            xs = range(int(left + (gx + 0.3) * cw), int(left + (gx + 0.7) * cw))
            ys = range(int(top + (gy + 0.3) * ch), int(top + (gy + 0.7) * ch))
            px = [im.getpixel((x, y)) for x in xs for y in ys]
            c = tuple(int(statistics.median(p[i] for p in px)) for i in range(3))
            if c[0] == c[1] == c[2] and (near(c, (192, 192, 192), 3) or near(c, (128, 128, 128), 3)):
                continue
            out.putpixel((gx, gy), c + (255,))
    return out, im.crop((left, top, right + 1, bottom + 1))


if __name__ == '__main__':
    tex, crop = reconstruct(sys.argv[1], tuple(int(v) for v in sys.argv[3:7]))
    tex.save(sys.argv[2])
    crop.save(sys.argv[2].replace('.png', '-crop.png'))
