"""b-optik32 (31.10.): tiny pixel-art engine for the IG31-23 item texture proposals (Endwood process, principle A8b).

A sprite is drawn as ASCII rows: '.' is transparent, every other character names a region (a colour ramp). The engine shades
each region by itself: pixels on the lit side (top/left edge towards another region or transparency) step one tone up,
pixels on the shadow side (bottom/right) one tone down, the rim towards transparency uses the darkest tones of the SAME ramp
(no black outline), and about 12 % of the pixels get a quiet +-1 tone noise for depth. Result: 8-12 soft tones, few hard jumps.
`hard_share` is the repo measure (tex29.smooth_pairs, luminance jump > 40 between opaque neighbours).
"""
from __future__ import annotations

import colorsys
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import tex29_2026_10_29 as t29  # noqa: E402


def ramp(hexcol: str, n: int = 6, spread: float = 0.14, sat_shift: float = 0.0, hue_shift: float = 0.0) -> list[tuple[int, int, int]]:
    """n tones, dark -> light, around the base colour (index n//2). Shadows lean a little to cooler/more saturated, lights to warmer."""
    r, g, b = (int(hexcol[i:i + 2], 16) / 255 for i in (0, 2, 4))
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    out = []
    for i in range(n):
        k = (i - n // 2) / max(1, n - 1)        # -0.5 .. 0.5
        hh = (h - k * 0.04 + hue_shift) % 1.0
        ll = min(0.97, max(0.04, l + k * spread * 2))
        ss = min(1.0, max(0.0, s * (1 - k * 0.5) + sat_shift))
        rr, gg, bb = colorsys.hls_to_rgb(hh, ll, ss)
        out.append((round(rr * 255), round(gg * 255), round(bb * 255)))
    return out


def paint(rows: list[str], ramps: dict[str, list], seed: int = 1, noise: float = 0.12, flat: str = '', base: dict | None = None) -> Image.Image:
    """rows: 16 strings of 16 chars. ramps: char -> list of tones. `flat`: chars that keep their middle tone (no edge shading).
    base: char -> index of the base tone (default n//2)."""
    h, w = len(rows), len(rows[0])
    rng = random.Random(seed)
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))

    box = {}
    for yy, row in enumerate(rows):
        for xx, ch in enumerate(row):
            if ch != '.':
                b = box.setdefault(ch, [yy, yy])
                b[0], b[1] = min(b[0], yy), max(b[1], yy)

    def at(x, y):
        return rows[y][x] if 0 <= x < w and 0 <= y < h else '.'

    for y in range(h):
        for x in range(w):
            c = rows[y][x]
            if c == '.':
                continue
            R = ramps[c]
            i = (base or {}).get(c, len(R) // 2)
            if c not in flat:
                up, left, down, right = at(x, y - 1), at(x - 1, y), at(x, y + 1), at(x + 1, y)
                lit = (up != c) + (left != c)
                shade = (down != c) + (right != c)
                rim = '.' in (up, left, down, right)
                if rim:
                    i = len(R) // 2 - 1 if not (shade and not lit) else 1
                    if lit and not shade and (up != '.' and left != '.'):
                        i = len(R) // 2 + 1      # edge towards another region on the lit side: highlight, not rim
                else:
                    i += lit - shade
                    y0, y1 = box[c]
                    if y1 - y0 >= 3:           # soft vertical light: top third lighter, bottom third darker
                        t = (y - y0) / (y1 - y0)
                        i += 1 if t < 0.34 else (-1 if t > 0.66 else 0)
                if i == (base or {}).get(c, len(R) // 2) and rng.random() < noise:
                    i += rng.choice((-1, 1))
            i = max(0, min(len(R) - 1, i))
            im.putpixel((x, y), R[i] + (255,))
    return im


def tones(im: Image.Image) -> int:
    return len({p[:3] for p in im.getdata() if p[3]})


def hard_share(im: Image.Image) -> float:
    return t29.smooth_pairs(im)


def up(im: Image.Image, k: int = 8) -> Image.Image:
    return im.resize((im.width * k, im.height * k), Image.NEAREST)
