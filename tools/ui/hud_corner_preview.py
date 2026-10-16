#!/usr/bin/env python3
"""HUD panel frame corner, before (3-2-1) and after (4-2-1-1, N68), 8x, same algorithm as HudFrame.java. Output: docs/ai/previews/claude-hudcopper/."""
import os
import sys
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "..", "docs", "ai", "previews", "claude-hudcopper")
BRONZE = (0x6F, 0x5F, 0x4B)
LAYERS = [(0x1E, 0x1F, 0x23), tuple(int(v * 0.82) for v in BRONZE), tuple(int(v * 0.64) for v in BRONZE)]
FILL = tuple(int(v * 0.2) for v in BRONZE)


def frame(w, h, corner):
    n = len(LAYERS)
    img = Image.new("RGB", (w, h), (235, 235, 235))
    px = img.load()
    cor = lambda k: corner[k] if 0 <= k < len(corner) else 0
    for r in range(h):
        d = min(r, h - 1 - r)
        for i in range(n + 1):
            if d < i:
                break
            left = i + cor(d - i)
            col = LAYERS[i] if i < n else FILL
            inner = (i + 1 + cor(d - i - 1)) if (i < n and d >= i + 1) else None
            for x in range(left, (inner if inner is not None else w - left)):
                px[x, r] = col
            if inner is not None:
                for x in range(w - inner, w - left):
                    px[x, r] = col
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    # the old 3-2-1 staircase = corner offsets [3, 2, 1]
    before, after = frame(40, 28, [3, 2, 1]), frame(40, 28, [4, 2, 1, 1])
    sheet = Image.new("RGB", (2 * 40 * 8 + 24, 28 * 8 + 16), (60, 60, 66))
    sheet.paste(before.resize((320, 224), Image.NEAREST), (8, 8))
    sheet.paste(after.resize((320, 224), Image.NEAREST), (336, 8))
    sheet.save(os.path.join(OUT, "hud_corner_before_after_8x.png"))


if __name__ == "__main__":
    main()
