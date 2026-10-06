"""Shared shimmer animation for item textures (texture round 7, owner addition 11, 2026-10-06).

A narrow diagonal glint (top left to bottom right, light from the top left like Vanilla items) runs once over the
masked pixels, then the item rests until the loop starts again. Pixels only move *up their own material ramp*
(nearest ramp tone + 2 steps in the core of the glint, + 1 at its edges; past the brightest tone: ``peak``), so the
animation never invents colors. Frame 0 is always the untouched texture, so GUIs that show the first frame keep the
owner's picked art.

Used by crucible_art_v2_2026_10_05.py (Enderite buckets), generate_textures.py (Enderite spear) and
texture_round7_2026_10_06.py (building cores).
"""
import numpy as np
from PIL import Image


def _nearest(ramp, color):
    return min(range(len(ramp)), key=lambda i: sum((int(ramp[i][k]) - int(color[k])) ** 2 for k in range(3)))


def shimmer_frame(base, ramp, mask, center, peak, width=1.0, steps=(2, 1)):
    """One frame: pixels of ``mask`` near diagonal ``x + y == center`` lifted up ``ramp``."""
    a = np.array(base.convert('RGBA'))
    h, w = mask.shape
    for y in range(h):
        for x in range(w):
            if not mask[y, x] or a[y, x, 3] == 0:
                continue
            dist = abs(x + y - center)
            if dist > width + 0.5:
                continue
            step = steps[0] if dist <= 0.5 else steps[1]
            i = _nearest(ramp, a[y, x, :3]) + step
            a[y, x, :3] = ramp[i] if i < len(ramp) else peak
    return Image.fromarray(a)


def shimmer_strip(base, ramp, mask, frames=20, sweep=8, peak=(255, 255, 255), steps=(2, 1), width=1.0):
    """Animation strip (width x height*frames): frame 0 = base, frames 1..sweep = glint pass, rest = base."""
    ys, xs = np.nonzero(mask)
    lo, hi = int((xs + ys).min()) - 1, int((xs + ys).max()) + 1
    w, h = base.size
    strip = Image.new('RGBA', (w, h * frames))
    for f in range(frames):
        if 1 <= f <= sweep:
            center = lo + (hi - lo) * (f - 1) / max(sweep - 1, 1)
            frame = shimmer_frame(base, ramp, mask, center, peak, width=width, steps=steps)
        else:
            frame = base
        strip.paste(frame, (0, h * f))
    return strip


def mcmeta(frametime):
    return {'animation': {'frametime': frametime, 'interpolate': False}}
